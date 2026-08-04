# Resultado da Fase 2 - Perfis e governança de acesso

Data: 04/08/2026

Status: CONCLUIDA TECNICAMENTE; HOMOLOGACAO FISICA EM REDE PENDENTE

## Escopo entregue

- Mantida a matriz fixa com Administrador, Supervisor Técnico, Técnico, Estoque
  e Auditor.
- Confirmada a atribuição de perfil e senha pela área **Equipes e acessos**.
- Incluída visualização administrativa da matriz vigente.
- Incluído histórico das últimas 100 mudanças de acesso.
- Registrados, sem senha ou segredo, usuário responsável, funcionário afetado,
  horário, evento e valores anterior/novo quando aplicável.
- Mantida a tabela de auditoria como append-only no PostgreSQL.
- Bloqueada a desativação ou mudança de perfil do último administrador ativo.
- Alterações de perfil ou desativação invalidam a sessão afetada na próxima
  requisição.
- Criados inicializador e encerramento controlado para uso na rede local.
- Documentada rotina de acesso, perfis, backup diário e limites do modo local.

## Homologação automatizada

- Flyway V1 a V29 aplicado em banco PostgreSQL temporário limpo.
- 212 testes backend: zero falhas, zero erros e zero ignorados.
- 75 cenários de autorização por módulo e perfil aprovados.
- Duas sessões de usuários diferentes permaneceram independentes.
- Logout de um operador não encerrou a sessão do outro.
- Débitos concorrentes não consumiram o mesmo saldo duas vezes.
- Build Vite aprovado.
- Scripts PowerShell de início e parada validados pelo parser.

## Decisão sobre perfis personalizados

Perfis totalmente configuráveis não entram nesta fase. Primeiro o time deve usar
os cinco perfis fixos no piloto e identificar permissões realmente ausentes. A
decisão evita combinações inseguras e uma tela administrativa complexa antes de
existir uma necessidade operacional comprovada.

## Pendência manual

Executar um teste assistido em dois computadores da mesma rede:

1. Administrador cria ou revisa duas contas reais de teste.
2. Operador de Estoque consulta saldo e registra uma movimentação controlada.
3. Outro perfil confirma que não consegue movimentar estoque.
4. Administrador altera o perfil de uma conta e confirma o encerramento da
   sessão antiga e o registro no histórico.
5. Gerar um backup completo ao final e guardar uma cópia fora do servidor.

Essa pendência não bloqueia desenvolvimento. Ela bloqueia apenas declarar o
ambiente local como homologado para operação contínua com dados reais.
