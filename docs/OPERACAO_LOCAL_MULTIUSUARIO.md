# Operação local multiusuário

## Objetivo

Usar um único computador como servidor local do RC Operations Hub. Os demais
computadores da mesma rede acessam o frontend pelo navegador e compartilham o
mesmo backend, banco PostgreSQL, estoque e histórico.

## Preparação única

1. Mantenha PostgreSQL, Java e Node.js instalados no computador servidor.
2. Configure `application-local.properties` na raiz do projeto. Não envie esse
   arquivo ao Git.
3. Garanta que cada operador tenha CPF, senha e perfil próprios em **Equipes**.
4. Libere no Firewall do Windows apenas as portas TCP `5173` e `8085` para a
   rede privada. Não exponha essas portas diretamente à internet.

## Iniciar e parar

No PowerShell, na raiz do projeto:

```powershell
.\scripts\iniciar-rede-local.ps1
```

O script mostra o endereço da rede, por exemplo `http://192.168.0.17:5173`.
Cada usuário abre esse endereço e entra com seu próprio CPF e senha.

Para encerrar somente os processos iniciados pelo script:

```powershell
.\scripts\parar-rede-local.ps1
```

Os logs ficam em `.runtime/`, fora do Git.

## Regras operacionais

- O computador servidor e o PostgreSQL devem permanecer ligados durante o uso.
- Não compartilhe uma conta entre operadores; o histórico usa o usuário da sessão.
- Administrador e Estoque podem operar o estoque. Técnico e Auditor não podem
  movimentá-lo. Supervisor consulta apenas os dados previstos na matriz vigente.
- Alterar perfil ou desativar um usuário invalida a sessão dele na próxima
  requisição. O último administrador ativo é protegido.
- Movimentações simultâneas usam controle transacional e não podem consumir o
  mesmo saldo duas vezes.

## Backup diário

Ao final do expediente, execute:

```powershell
.\scripts\backup-completo.ps1 -Database poprc
```

O pacote inclui banco e arquivos enviados. Guarde uma cópia fora do computador
servidor. A restauração de teste permanece descrita em `docs/BANCO_DESENVOLVIMENTO.md`.

## Limite deste modo

Esse modo é adequado para uso interno controlado na mesma rede. HTTPS, domínio,
monitoramento, backup automático e alta disponibilidade pertencem à fase AWS.
