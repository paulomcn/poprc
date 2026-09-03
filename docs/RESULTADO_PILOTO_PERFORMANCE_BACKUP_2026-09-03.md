# Piloto operacional, performance e backup - 03/09/2026

## Resultado do piloto automatizado

O roteiro `scripts/test-pilot.ps1` foi ampliado para incluir o ciclo HTTP completo,
concorrencia de retirada/devolucao e consistencia das reservas locais. A execucao
usa o banco temporario `poprc_pilot_test`, uploads isolados em `target/test-uploads`
e remove o banco ao terminar. Nenhum dado da VPS ou do estoque operacional e alterado.

Resultado: 115 testes executados, sem falhas, erros ou testes ignorados.

O escopo inclui:

- contrato, projeto, equipe, OS e OR automatica;
- documento inicial, vistoria, fotos e assinaturas;
- retirada, checklist e evidencia do tecnico;
- validacao do supervisor e Virada de Rede;
- devolucao, auditoria, As-Built, documento final e encerramento;
- autorizacao por perfil, integridade documental e concorrencia de estoque.

## Otimizacao do frontend

As paginas protegidas e o layout passaram a ser carregados sob demanda por rota.
O JavaScript inicial de producao caiu de aproximadamente 810 kB para 184 kB antes
da compressao, reducao de cerca de 77%. O ExcelJS permanece em um arquivo isolado:
ele so e transferido quando o usuario usa importacao ou exportacao de planilhas.

A build agora aplica um limite de 300 kB ao arquivo JavaScript inicial. Uma futura
alteracao que volte a incluir paginas ou bibliotecas pesadas na entrada falhara no CI.

## Backup imediato na VPS

Depois de publicar esta revisao, executar:

```bash
cd /opt/poprc/current
sudo test -r /etc/poprc/poprc.env
sudo env POPRC_ENV_FILE=/etc/poprc/poprc.env bash deploy/scripts/backup-vps.sh
sudo ls -lht /var/backups/poprc | head
```

O arquivo `poprc-AAAAMMDDTHHMMSSZ.tar.gz` contem o dump PostgreSQL, uploads,
manifesto com a revisao Git e hashes SHA-256. O script valida os hashes antes de
finalizar o pacote e testa a estrutura do arquivo compactado.

Para ativar o backup diario, sem reinstalar o backend legado:

```bash
cd /opt/poprc/current
sudo install -o root -g root -m 0750 deploy/scripts/backup-vps.sh /usr/local/sbin/poprc-backup
sudo install -o root -g root -m 0644 deploy/systemd/poprc-backup.service /etc/systemd/system/poprc-backup.service
sudo install -o root -g root -m 0644 deploy/systemd/poprc-backup.timer /etc/systemd/system/poprc-backup.timer
sudo systemctl daemon-reload
sudo systemctl enable --now poprc-backup.timer
sudo systemctl start poprc-backup.service
sudo systemctl status poprc-backup.service --no-pager
sudo systemctl list-timers poprc-backup.timer --no-pager
```

O backup local protege contra erro operacional, mas nao contra perda total da VPS.
Uma copia periodica deve ser enviada a armazenamento externo com acesso restrito.

## Proxima validacao

O proximo passo de homologacao e um ciclo assistido pela interface com usuarios
reais de cada perfil. Ele deve usar uma OS identificada como piloto, quantidades
pequenas e horario combinado. Antes do inicio, confirmar o backup acima e evitar
qualquer importacao ou movimentacao paralela do mesmo material.

Para otimizar o backend com seguranca, primeiro coletar memoria, CPU e tempos de
resposta da VPS. Limites de heap, pool de conexoes ou CPU nao devem ser escolhidos
sem conhecer a memoria total e os demais servicos hospedados na maquina.
