# Matriz de Permissões e Homologação

Data: 02/09/2026  
Escopo: autorização por perfil, identidade auditável e transições da Ordem de Serviço.

## Perfis ativos

| Perfil | Responsabilidade principal |
| --- | --- |
| Administrador | Administração integral, cadastros, operação e gestão financeira |
| Supervisor técnico | Contratos, projetos, OS, obras, equipe e validação técnica |
| Técnico | Execução das OS atribuídas, checklist, evidências e envio para validação |
| Estoque | Cadastro, entrada, retirada, devolução e rastreabilidade dos materiais |
| Auditor | Conciliação, As-Built e consulta das evidências permitidas |

## Matriz funcional do frontend

| Módulo/ação | Administrador | Supervisor | Técnico | Estoque | Auditor |
| --- | :---: | :---: | :---: | :---: | :---: |
| Dashboard executivo | Sim | Sim | Não | Não | Não |
| Contratos e projetos | Sim | Sim | Não | Não | Não |
| Visualizar funcionários | Sim | Sim | Não | Sim | Não |
| Gerenciar funcionários | Sim | Não | Não | Não | Não |
| Visualizar OS | Sim | Sim | Sim | Não | Não |
| Gerenciar OS | Sim | Sim | Não | Não | Não |
| Visualizar obras | Sim | Sim | Sim | Não | Sim |
| Executar obras | Sim | Sim | Sim | Não | Não |
| Gerenciar obras | Sim | Sim | Não | Não | Não |
| Estoque | Sim | Não | Não | Sim | Não |
| Retirada e devolução | Sim | Não | Não | Sim | Não |
| Auditoria e As-Built | Sim | Não | Não | Não | Sim |
| Financeiro | Sim | Não | Não | Não | Não |
| Atividades padrão | Sim | Não | Não | Não | Não |
| Portal técnico | Sim | Sim | Sim | Não | Não |
| Editar documentos da obra | Sim | Sim | Sim | Não | Não |

## Regras reforçadas nesta homologação

1. A identidade de criação, invalidação e registro de assinatura dos documentos vem exclusivamente da sessão autenticada.
2. O header cliente `X-Usuario-Atual` foi removido do backend, frontend e configuração CORS.
3. O perfil Técnico pode somente enviar uma OS executada para `AGUARDANDO_VALIDACAO`.
4. Aprovação, rejeição e avanço após a validação permanecem sob responsabilidade de Administrador ou Supervisor Técnico.
5. O responsável gravado no histórico da mudança de status vem da sessão e ignora nomes enviados no corpo da requisição.
6. O técnico continua limitado às OS vinculadas aos projetos em que participa ou pelos quais é responsável.
7. O Auditor pode consultar documentos permitidos, mas não pode criar, alterar, invalidar ou assinar documentos.

## Cobertura automatizada

- Matriz HTTP de leitura e escrita por perfil no backend.
- Bloqueio de movimentação de estoque para Técnico.
- Bloqueio de operação de campo para Auditor.
- Bloqueio de arquivos financeiros e evidências fora do perfil permitido.
- Bloqueio da aprovação da própria execução pelo Técnico via chamada direta.
- Origem autenticada do responsável no histórico da OS.
- Recusa de documento sem identidade autenticada.
- Matriz completa de menus e ações do frontend, incluindo negação de perfil desconhecido.
- Imutabilidade do log de assinaturas e verificação de integridade documental.

## Pendências de aceite

- Executar o roteiro manual com uma conta real de cada perfil em computador e celular.
- Confirmar com o time se Estoque deve continuar visualizando a lista básica de funcionários para rastreabilidade.
- Confirmar se Auditor deve visualizar Gestão de Obras ou somente Auditoria e documentos associados.
- Definir os novos perfis antes de transformar a matriz fixa em permissões configuráveis pelo Administrador.
- Manter Zoho e permissões dinâmicas para uma fase posterior, conforme decisão já registrada no cronograma.

## Critério de conclusão

Esta etapa fica tecnicamente concluída quando as suítes de backend e frontend estiverem verdes. O aceite operacional permanece pendente até o teste manual dos cinco perfis em dispositivos reais.

## Resultado automatizado

- Backend: 261 testes aprovados no banco isolado `poprc_test`.
- Frontend: 23 testes aprovados.
- Build de produção do frontend: concluído com sucesso.
- Resultado: homologação técnica automatizada aprovada; aceite manual em dispositivos reais pendente.
