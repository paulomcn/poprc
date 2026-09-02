# Roteiro de Aceite por Perfil

Data: 02/09/2026  
Escopo: validação manual das permissões em computador e celular.

## Preparação

1. No módulo Equipes, cadastrar ou selecionar uma conta local ativa para cada perfil: Administrador, Supervisor Técnico, Técnico, Estoque e Auditor.
2. Definir uma senha temporária distinta para cada conta e concluir a troca de senha no primeiro acesso.
3. Manter uma OS de homologação com equipe, materiais e documentos de teste. Não usar uma operação real em andamento.
4. No computador, usar perfis separados do navegador ou janelas privativas para evitar que uma sessão substitua a outra.
5. No celular conectado à mesma rede, abrir o endereço de rede exibido pelo Vite, por exemplo `http://192.168.15.16:5173`.
6. Registrar CPF mascarado, perfil, dispositivo, navegador, resultado e evidência de cada teste. Nunca registrar senhas no relatório.

## Validação por perfil

| Perfil | Deve conseguir | Deve ser bloqueado |
| --- | --- | --- |
| Administrador | Acessar todos os módulos, gerenciar equipe e executar operações administrativas | Nenhum módulo funcional previsto na matriz |
| Supervisor Técnico | Acessar contratos, projetos, OS, obras, equipe e validar execução técnica | Financeiro, movimentação de estoque e homologação de auditoria |
| Técnico | Acessar as OS atribuídas, checklist, fotos, documentos permitidos e enviar execução para validação | Aprovar a própria execução, gerenciar equipe, estoque, financeiro e auditoria |
| Estoque | Acessar estoque, retirada/devolução e visualizar a lista básica de funcionários | Alterar funcionários, contratos, projetos, execução técnica, financeiro e auditoria |
| Auditor | Visualizar Gestão de Obras, evidências e documentos; acessar Auditoria e As-Built | Executar obra, editar/assinar documentos operacionais, movimentar estoque e acessar financeiro |

## Procedimento em cada dispositivo

1. Entrar com a conta do perfil.
2. Conferir se a sidebar mostra apenas os módulos permitidos.
3. Abrir cada módulo visível e executar uma operação de leitura.
4. Nas ações permitidas, realizar uma alteração controlada na OS de homologação.
5. Tentar acessar diretamente pela URL ao menos um módulo proibido e confirmar a mensagem de acesso negado.
6. Sair do perfil, confirmar o retorno à tela de login e entrar com o perfil seguinte.
7. Repetir no celular, verificando sidebar recolhida, tabelas, modais, botões e ausência de sobreposição.

## Registro do aceite

| Data | Dispositivo e navegador | Perfil | Login | Menus | Ações permitidas | Bloqueios | Layout | Evidência | Resultado |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
|  |  | Administrador |  |  |  |  |  |  | Pendente |
|  |  | Supervisor Técnico |  |  |  |  |  |  | Pendente |
|  |  | Técnico |  |  |  |  |  |  | Pendente |
|  |  | Estoque |  |  |  |  |  |  | Pendente |
|  |  | Auditor |  |  |  |  |  |  | Pendente |

## Definição de novos perfis

Antes de criar permissões configuráveis, o time deve listar funções reais que possuem responsabilidades diferentes das cinco atuais. Para cada função candidata, preencher:

| Perfil candidato | Responsabilidade | O que visualiza | O que altera | O que aprova | Justificativa para não usar perfil existente |
| --- | --- | --- | --- | --- | --- |
| Financeiro/Faturamento |  |  |  |  |  |
| Gestor de Contratos/Projetos |  |  |  |  |  |
| Diretoria/Consulta |  |  |  |  |  |

Um novo perfil só deve ser criado quando houver separação real de responsabilidade. Depois da aprovação dessa tabela, a matriz fixa pode ser ampliada e homologada. A edição dinâmica de permissões pelo Administrador continua reservada para uma fase posterior.

## Critério de aprovação

O aceite é aprovado quando os cinco perfis passarem no computador e no celular, as URLs proibidas forem bloqueadas no backend e nenhum usuário visualizar ou executar ações fora da matriz definida.
