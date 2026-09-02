# Relatorio de Homologacao por Perfis

Data: 02/09/2026  
Versao publicada verificada: `v0.1.0+9c0fa87c`  
Ambiente: VPS, acesso HTTPS

## Resultado executivo

A camada automatizada de autorizacao e a tela publica responsiva foram
aprovadas. O aceite final continua dependendo da entrada manual com uma conta
real de cada perfil em computador e celular, sem compartilhamento de senhas no
relatorio ou em ferramentas de desenvolvimento.

## Evidencias automatizadas

| Verificacao | Resultado | Evidencia |
| --- | --- | --- |
| Matriz de permissoes do frontend | Aprovado | 23 testes do frontend, sem falhas |
| Build de producao do frontend | Aprovado | Vite concluiu a compilacao |
| Backend completo | Aprovado | 261 testes, sem falhas ou erros |
| Autorizacao por perfil e URL direta | Aprovado | 77 cenarios em `SecurityAuthorizationIntegrationTest` |
| Migracoes do banco de teste | Aprovado | 38 migracoes aplicadas em `poprc_test` |
| Login em computador | Aprovado | 1440 x 900, sem overflow horizontal |
| Login em celular | Aprovado | 390 x 844, sem overflow ou sobreposicao |
| Health check da VPS | Aprovado | HTTP 200 e estado `UP` |

Os testes de autorizacao incluem, entre outros, os seguintes bloqueios:

- Tecnico nao movimenta estoque nem cria OS.
- Tecnico nao aprova a propria execucao.
- Auditor consulta obras e documentos, mas nao altera o fluxo operacional.
- Estoque consulta comarcas e funcionarios para rastreabilidade, mas nao abre
  evidencias protegidas de campo.
- Somente Administrador consulta a auditoria de acessos.
- URLs estaticas de documentos, evidencias e comprovantes financeiros nao
  contornam a matriz de acesso.

## Aceite autenticado pendente

Execute esta tabela primeiro no computador e depois no celular. Use uma OS de
homologacao e nao uma operacao real em andamento.

| Perfil | Computador | Celular | URL proibida bloqueada | Logout | Resultado |
| --- | --- | --- | --- | --- | --- |
| Administrador | Pendente | Pendente | Pendente | Pendente | Pendente |
| Supervisor Tecnico | Pendente | Pendente | Pendente | Pendente | Pendente |
| Tecnico | Pendente | Pendente | Pendente | Pendente | Pendente |
| Estoque | Pendente | Pendente | Pendente | Pendente | Pendente |
| Auditor | Pendente | Pendente | Pendente | Pendente | Pendente |

Para cada perfil:

1. Entrar com CPF e senha da propria conta de teste.
2. Conferir os menus visiveis conforme a matriz de acesso.
3. Abrir uma pagina permitida e realizar uma leitura.
4. Tentar abrir diretamente uma URL proibida e confirmar o bloqueio.
5. Sair pelo menu do usuario e confirmar o retorno ao login.
6. Registrar apenas data, perfil, navegador, resultado e captura de tela. Nao
   registrar CPF completo ou senha.

## Pendencias nao bloqueantes

- O bundle de planilhas permanece grande e pode ser separado por carregamento
  sob demanda em uma futura rodada de desempenho.
- O npm informa duas vulnerabilidades moderadas em dependencias transitivas.
  Nao aplicar `npm audit fix --force`; tratar em atualizacao controlada.
- Zoho e permissoes dinamicas continuam adiados conforme o cronograma.

## Criterio de conclusao

A homologacao por perfis sera considerada concluida quando as cinco linhas do
aceite autenticado estiverem aprovadas em computador e celular. Ate la, a
camada automatizada esta aprovada e a camada manual permanece parcialmente
concluida.
