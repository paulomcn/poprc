# Operação local multiusuário

## Primeiro uso com banco limpo

Para iniciar a operação real sem os dados usados durante o desenvolvimento:

```powershell
.\scripts\preparar-ambiente-local-limpo.ps1
```

O comando cria o banco `poprc_local`, mantém o banco anterior intacto e atualiza somente
`application-local.properties`. Ao abrir o sistema pela primeira vez, a tela de login pede o
cadastro do primeiro administrador. Esse cadastro inicial deixa de existir automaticamente após
o primeiro usuário ser criado.

Se `poprc_local` já existir, o script não apaga dados. Para recriá-lo, use `-Recreate`; será exigida
uma confirmação textual e um backup completo do banco e dos uploads será criado antes da remoção.
Os arquivos de upload permanecem no diretório original, embora o novo banco não tenha referências
a eles. Exclua arquivos antigos somente depois de validar e guardar o pacote de backup.

## Importação por nota fiscal

Na página **Estoque**, use **Importar NF** para enviar XML ou PDF. O sistema primeiro apresenta uma
prévia editável, permite vincular linhas a materiais existentes ou criar novos itens e somente
altera o saldo após a confirmação. XML de NF-e é a fonte mais confiável. PDFs variam por fornecedor
e sempre precisam de conferência; PDFs digitalizados podem exigir o preenchimento manual das linhas.

O botão **Histórico** separa notas fiscais e planilhas. Em **Notas fiscais** é possível conferir os
itens processados, materiais criados ou vinculados e baixar o XML/PDF original arquivado.

## Remoção de materiais

O ícone de lixeira remove o material das listas operacionais, mas preserva notas fiscais,
movimentações, OS e auditorias antigas. A remoção exige saldo disponível, metragem e reservas
zerados e registra o usuário e o horário da ação. Como é uma operação sensível, o sistema pode
solicitar a senha novamente antes de confirmar.

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
.\scripts\backup-completo.ps1 -Database poprc_local
```

O pacote inclui banco e arquivos enviados. Guarde uma cópia fora do computador
servidor. A restauração de teste permanece descrita em `docs/BANCO_DESENVOLVIMENTO.md`.

## Limite deste modo

Esse modo é adequado para uso interno controlado na mesma rede. HTTPS, domínio,
monitoramento, backup automático e alta disponibilidade pertencem à fase AWS.
