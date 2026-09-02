# Implantacao em VPS

Este roteiro instala o RC Operations Hub em uma VPS Ubuntu/Debian com backend gerenciado pelo `systemd`, frontend servido pelo Nginx, PostgreSQL local ou privado e backup diario.

## Arquitetura

- Nginx recebe HTTP/HTTPS e entrega o frontend compilado.
- `/api/**` e `/uploads/**` sao encaminhados ao Spring Boot em `127.0.0.1:8085`.
- O backend nao fica exposto diretamente na internet.
- PostgreSQL deve aceitar conexoes somente locais ou de uma rede privada.
- Uploads e PDFs ficam em `/var/lib/poprc/uploads`.
- Releases ficam em `/opt/poprc/releases`; `/opt/poprc/current` aponta para a ativa.
- Segredos ficam apenas em `/etc/poprc/poprc.env`.

## 1. Preparar DNS e pacotes

Crie um registro DNS `A` apontando o dominio para o IP da VPS. Instale Nginx, PostgreSQL, cliente PostgreSQL, Certbot e um runtime Java 25. Node.js 22 e npm sao necessarios apenas se o frontend for compilado na propria VPS.

Confirme:

```bash
java -version
node --version
npm --version
psql --version
nginx -v
```

## 2. Criar o banco

```bash
sudo -u postgres createuser --pwprompt poprc
sudo -u postgres createdb --owner=poprc poprc
```

Use uma senha hexadecimal forte, que tambem e segura para o arquivo de ambiente:

```bash
openssl rand -hex 32
```

## 3. Obter e validar o codigo

```bash
git clone https://github.com/paulomcn/poprc.git
cd poprc
git checkout main
git pull --ff-only
```

Opcionalmente execute os testes contra um banco separado antes da publicacao. Nunca aponte testes para o banco `poprc`.

## 4. Compilar os artefatos

```bash
./mvnw --batch-mode -DskipTests package
cd frontend
npm ci
npm audit --audit-level=high
npm run build
cd ..
```

O frontend usa `/api` na mesma origem e, por isso, nao precisa receber o endereco interno do backend durante o build.

## 5. Instalar a primeira release

```bash
sudo bash deploy/scripts/install-vps.sh app.exemplo.com.br
```

Na primeira execucao o instalador cria `/etc/poprc/poprc.env` e para de forma segura. Edite o arquivo:

```bash
sudo nano /etc/poprc/poprc.env
```

Defina a URL publica exata e a credencial do banco. Preserve:

```properties
APP_SECURITY_ENABLED=true
DEV_LOGIN_ENABLED=false
ZOHO_ENABLED=false
SESSION_COOKIE_SECURE=true
SERVER_ADDRESS=127.0.0.1
```

Execute o instalador novamente. Ele valida Nginx, inicia o backend e aguarda o health check.

## 6. Ativar HTTPS

Depois que o DNS estiver resolvendo para a VPS:

```bash
sudo certbot --nginx -d app.exemplo.com.br --redirect
sudo nginx -t
sudo systemctl reload nginx
```

Mantenha no firewall apenas SSH, HTTP e HTTPS. Nao exponha `5432` nem `8085`:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
sudo ufw enable
```

## 7. Primeiro acesso ou migracao

Em banco vazio, a tela inicial permite criar o primeiro Administrador. Depois disso, o bootstrap e desativado automaticamente.

Para levar os dados locais, lembre que banco e uploads nao fazem parte do Git. Gere antes um pacote com `scripts/backup-completo.ps1`, transfira-o por um canal seguro e restaure primeiro em um banco de validacao. Somente depois da conferencia:

1. Pare `poprc-backend`.
2. Restaure o `database.dump` com `pg_restore --exit-on-error --no-owner --no-privileges`.
3. Copie `uploads/` para `/var/lib/poprc/uploads`.
4. Aplique `chown -R poprc:poprc /var/lib/poprc/uploads`.
5. Inicie o backend e execute o smoke test.

Nunca restaure diretamente sobre o banco ativo sem backup e janela de manutencao.

## 8. Backup e verificacao

O timer executa diariamente por volta de 02:30:

```bash
systemctl list-timers poprc-backup.timer
sudo systemctl start poprc-backup.service
sudo journalctl -u poprc-backup.service -n 100 --no-pager
sudo ls -lh /var/backups/poprc
```

Cada arquivo inclui dump PostgreSQL, uploads, manifesto e hashes SHA-256. Copie os backups para armazenamento externo; um backup apenas na propria VPS nao protege contra perda da maquina.

## 9. Publicar uma atualizacao

```bash
git pull --ff-only
./mvnw --batch-mode -DskipTests package
cd frontend && npm ci && npm run build && cd ..
sudo bash deploy/scripts/install-vps.sh app.exemplo.com.br
```

Para rollback, escolha uma release anterior e altere o link:

```bash
ls -1 /opt/poprc/releases
sudo ln -sfn /opt/poprc/releases/RELEASE_ANTERIOR /opt/poprc/current
sudo systemctl restart poprc-backend
sudo systemctl reload nginx
```

## 10. Smoke test

```bash
curl --fail https://app.exemplo.com.br/actuator/health
systemctl status poprc-backend --no-pager
journalctl -u poprc-backend -n 200 --no-pager
```

No navegador, valide login, logout, criação de sessão, upload de uma evidência de teste e leitura da mesma evidência. Faça também o teste em outro computador antes de liberar usuários reais.
