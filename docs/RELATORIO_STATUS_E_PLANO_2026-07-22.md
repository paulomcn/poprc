# Relatorio de Status e Plano de Acao

**Projeto:** RC Operations Hub  
**Data-base:** 22/07/2026  
**Fonte:** `proximospassos.docx`, codigo atual, historico Git, migracoes, testes e documentos de homologacao.

## 1. Resumo executivo

O nucleo operacional esta funcionalmente avancado. O sistema cobre o ciclo Contrato -> Projeto -> OS -> Materiais -> OR -> Retirada -> Execucao tecnica -> Devolucao -> Auditoria -> As-Built -> Encerramento -> Faturamento. A linha de base atual foi validada com 85 testes aprovados em banco PostgreSQL descartavel e 19 migracoes Flyway aplicadas desde um schema vazio.

O projeto ainda nao esta pronto para producao. Os principais motivos sao: alteracoes atuais sem checkpoint, autenticacao suspensa, necessidade de regressao manual apos as mudancas recentes, homologacao fisica dos documentos e teste real de restauracao de backup.

**Decisao vigente:** autenticacao, CPF/senha, Zoho, perfis protegidos e CSRF permanecem no codigo, mas estao desativados no ambiente local por `APP_SECURITY_ENABLED=false`. O desenvolvimento segue com administrador de teste automatico. Essa fase sera retomada antes de qualquer piloto com usuarios reais ou publicacao na AWS.

## 2. Evidencias verificadas hoje

- 85 testes executados; nenhuma falha, erro ou teste ignorado.
- Banco `poprc_test` criado, migrado e removido automaticamente.
- 19 migracoes Flyway validadas e aplicadas do zero.
- Build Maven aprovado.
- Build Vite aprovado.
- Acesso local validado diretamente no Dashboard sem tela de login.
- Homologacao operacional de 20/07 registrada como concluida.
- PDFs aprovados digitalmente; impressao fisica A4 permanece pendente.
- Branch atual `checkpoint/fluxo-operacional`, um commit a frente do remoto e com muitas alteracoes ainda nao versionadas.

## 3. Situacao das fases anteriores

| Fase anterior | Situacao atual | Observacao |
|---|---|---|
| 1. Estabilizacao operacional | Concluida | Fallbacks simulados removidos, APIs reais, upload fisico e erros reais. |
| 2. Qualidade e homologacao | Concluida como linha de base | Fluxo completo aprovado em 20/07. As mudancas posteriores exigem regressao curta. |
| 3. Documentos e auditoria | Concluida digitalmente | PDFs, hashes, versoes e logs append-only. Impressao fisica pendente; arquivamento imutavel proprio da OR deve ser confirmado. |
| 4. Alertas operacionais | Concluida internamente | Deadline, estoque minimo e ciclo de resolucao implementados. WhatsApp nao configurado; e-mail depende do ambiente. |
| 5. Dados e banco | Avancada | Flyway V1-V19, massa dev, reset seguro, backup e restore possuem scripts. Falta evidenciar restauracao real. |
| 6. Organizacao da entrega | Parcial novamente | Checkpoints anteriores existem, mas a arvore atual esta suja e o commit local ainda nao esta no remoto. |
| 7. Seguranca e acesso | Suspensa | Codigo de perfis, CPF/senha, Zoho, sessao, CSRF e logs existe, mas a homologacao de navegador falhou. Segurança foi desativada para nao bloquear o desenvolvimento. |
| 8. Remodelacao visual | Parcial | Sidebar, header, Dashboard, Projetos, OS, Obras, Equipes e Tecnico receberam revisoes. Ainda falta consistencia visual e homologacao responsiva completa. |
| 9. AWS/publicacao | Nao iniciada | Deve ocorrer somente depois de regressao, seguranca reativada, backup testado e piloto controlado. |

## 4. Funcionalidades concluidas

### Operacao e obras

- Gestao de Obras com Vistoria, Infraestrutura e Virada de Rede.
- Fotos, assinatura manuscrita, provas de funcionamento, checklist e progresso.
- Edicao/remocao controlada de foto, assinatura e prova antes da conclusao.
- Conclusao formal da obra e filtro por Vistoria, Infra, Virada e Concluidas.
- Maquina de estados da OS, historico de transicoes e bloqueio de saltos invalidos.
- Filas e pendencias para Tecnico, Estoque, Auditoria e Administracao.

### OS, OR e estoque

- OS sequencial por contrato, datas obrigatorias e materiais definidos na criacao.
- OR automatica, multiplas ORs, dupla assinatura, retirada e devolucao.
- Ferramentas, consumiveis, depositos, bobinas/rolos, saldo local e estoque minimo.
- Vinculo de projeto nas movimentacoes.
- Concorrencia protegida com locks pessimistas, `@Version` e teste simultaneo.
- Arquivamento/restauracao de contratos, projetos, obras e OS no lugar de exclusao destrutiva.

### Equipes, tecnico e evidencias

- Equipe multifuncional por projeto com Lider de Equipe e Tecnicos.
- Atividades padrao e checklist tecnico.
- Relatorio fotografico com miniaturas, ampliacao, abertura e remocao.
- Evidencias gravadas fisicamente somente apos validacao de arquivo e imagem.
- Alertas de prazo integrados ao Portal Tecnico.

### Auditoria, documentos e gestao

- Conciliação previsto x auditado, sobra/falta e homologacao com divergencia.
- Documento inicial e final, PDF no backend, hash e historico de versoes.
- Logs de assinatura protegidos contra alteracao/exclusao no banco.
- Dashboard operacional, notificacoes, faturamento e viagens com base funcional.
- Relatorios Excel com filtros por pessoa e ajuste de largura.

## 5. Trabalho atual

- Consolidacao do modo local sem autenticacao.
- Revisao da configuracao para impedir que login/CSRF bloqueiem o desenvolvimento.
- Atualizacao deste cronograma contra o estado real do repositorio.
- Preparacao de um novo checkpoint para as mudancas de UI, perfis e seguranca suspensa.

## 6. Pendencias por prioridade

| Prioridade | Pendencia | Motivo |
|---|---|---|
| P0 - imediata | Criar checkpoint limpo das alteracoes atuais | A arvore possui muitos arquivos modificados e novos; perder esse estado teria alto impacto. |
| P0 - imediata | Regressao do fluxo principal no modo sem autenticacao | As 85 verificacoes automatizadas passaram, mas o navegador precisa validar as mudancas recentes. |
| P0 - antes de usuarios reais | Reimplementar e homologar autenticacao ponta a ponta | Hoje nao ha identidade comprovada nem protecao efetiva de rotas. |
| P1 - alta | Homologar visualmente Dashboard, Projetos, OS, Obras, Tecnico e Auditoria | O feedback aponta inconsistencias, seletores escondidos e escalabilidade ruim das listas. |
| P1 - alta | Executar e registrar uma restauracao real de backup | Scripts existem, mas recuperacao so e confiavel depois de um restore aprovado. |
| P1 - alta | Confirmar arquivo imutavel da OR e politica de retencao de uploads/PDFs | Evita perda ou alteracao de documentos historicos. |
| P1 - alta | Repetir homologacao completa com duas ORs, devolucao parcial e material faltante | Cobre os caminhos mais sujeitos a regressao de estado. |
| P2 - media | Imprimir OS, OR e encerramento em A4 | A camada digital foi aprovada; falta validar equipamento real. |
| P2 - media | Padronizar mensagens, vazios, carregamento e confirmacoes | Reduz operacoes que parecem ter funcionado sem persistencia. |
| P3 - posterior | Custos reais dos materiais | Adiado por decisao; nenhum valor ficticio deve ser reintroduzido. |
| P3 - posterior | WhatsApp, e-mail produtivo e integracoes externas | Dependem de provedor, credenciais e ambiente de producao. |
| P3 - posterior | Nuvem, ERP/CRM e ICP-Brasil | Nao sao necessarios para estabilizar o nucleo atual. |

## 7. Novo plano de acao por fases

### Fase A - Baseline e checkpoint (1-2 dias)

1. Atualizar documentacao para registrar `APP_SECURITY_ENABLED=false` no desenvolvimento.
2. Revisar o diff e separar autenticacao suspensa, UI e correcoes funcionais.
3. Rodar novamente testes e builds.
4. Criar commit e publicar a branch de checkpoint.

**Aceite:** Git limpo, branch publicada, 85 ou mais testes aprovados e builds aprovados.

### Fase B - Regressao operacional assistida (2-3 dias)

1. Resetar a massa dev de forma controlada.
2. Executar Contrato -> Projeto -> OS -> OR -> Retirada -> Tecnico -> Devolucao -> Auditoria -> As-Built -> Encerramento -> Faturamento.
3. Cobrir duas ORs, material faltante, devolucao parcial, ferramenta obrigatoria e divergencia.
4. Verificar conclusao da obra, filas e indicadores do Dashboard.

**Aceite:** nenhuma operacao apresenta sucesso sem persistir; estados e saldos coincidem com o banco; defeitos registrados com evidencia.

### Fase B.1 - Faturamento e impostos (3-5 dias)

1. Remodelar a listagem de cobrancas com contrato, NF, emissao, valor, identificacao do destino e situacao de pagamento.
2. Separar visualmente setor publico e privado, adicionando classificacao explicita ao contrato.
3. Tornar servicos executados opcionais e visiveis apenas nos detalhes.
4. Criar a aba de impostos derivada das NFs, com competencia no dia 20 do mes seguinte, 4,8% retido, 14,93% a pagar e total de 19,73%.
5. Adicionar filtros, totais, alertas de vencimento e graficos de acompanhamento.
6. Cobrir calculos, mudanca para pago, datas de virada de ano e unicidade da NF com testes de backend e frontend.

**Aceite:** os totais conciliam com a planilha de referencia; nenhuma linha fiscal e digitada duas vezes; alteracoes de pagamento persistem; filtros e alertas funcionam com dados publicos e privados.

### Fase C - Coerencia visual e usabilidade (3-5 dias)

1. Definir um unico sistema visual claro para sidebar, header, tabelas, cards e modais.
2. Revisar Dashboard e frontpage.
3. Revisar Projetos, Ordens de Servico e Gestao de Obras.
4. Revisar Area do Tecnico e Auditoria, dando destaque aos seletores de projeto/OS.
5. Testar listas pequenas, grandes, vazias e com erro.

**Aceite:** screenshots aprovados em desktop e celular; sem sobreposicao, overflow ou controles escondidos; tarefas principais executadas sem orientacao externa.

### Fase D - Documentos e recuperacao (2-4 dias)

1. Imprimir os quatro cenarios documentais em A4.
2. Confirmar arquivamento imutavel da OR e retencao de fotos/PDFs.
3. Gerar backup, restaurar em `_restore_test` e abrir o sistema contra a copia.
4. Registrar versao Flyway, contagens e responsavel pelo teste.

**Aceite:** impressao fisica assinada, restore comprovado e documentos recuperaveis.

### Fase E - Seguranca final isolada (5-8 dias)

1. Retomar a autenticacao em branch propria.
2. Escolher oficialmente CPF+senha local, Zoho opcional ou combinacao dos dois.
3. Corrigir ciclo SPA de sessao/CSRF e persistencia de sessao em reinicios.
4. Proteger APIs e entidades por perfil e atribuicao.
5. Remover o bypass de administrador e reativar `APP_SECURITY_ENABLED=true`.
6. Criar testes E2E reais de login, primeira troca, logout, expiracao, 401 e 403.

**Aceite:** fluxo validado em navegador real, reinicio do backend, dois computadores da rede e todos os perfis. Nenhuma liberacao baseada apenas em teste unitario/API.

### Fase F - Pre-producao AWS (3-5 dias)

1. Preparar HTTPS, dominio, CORS restrito e variaveis de ambiente.
2. Definir PostgreSQL, armazenamento persistente de uploads/PDFs e backup automatico.
3. Configurar logs, health check e monitoramento.
4. Executar migracao e smoke test em ambiente limpo.

**Aceite:** ambiente reproduzivel, restore testado, segredos fora do codigo e checklist de rollback.

### Fase G - Piloto controlado (1-2 semanas)

1. Liberar para poucos usuarios e poucas OS reais.
2. Monitorar erros, tempo por etapa, estoque, arquivos e feedback de campo.
3. Corrigir defeitos antes de ampliar o uso.

**Aceite:** ciclo real concluido sem ajuste manual de banco e sem perda de evidencia.

## 8. Homologacoes obrigatorias

| Homologacao | Quando | Evidencia |
|---|---|---|
| Suite automatizada isolada | Todo checkpoint | Relatorio Maven com total, falhas e 19 migracoes. |
| Build frontend/backend | Todo checkpoint | Saida de build sem erro. |
| Smoke de paginas | Toda mudanca de UI/API | Dashboard, Projetos, OS, Obras, Estoque, Tecnico e Auditoria carregando. |
| Fluxo ponta a ponta | Fases B, E e F | Roteiro preenchido com IDs de Contrato, OS e OR. |
| Concorrencia | Mudanca em estoque/OR | Duas retiradas simultaneas; apenas uma pode consumir o mesmo saldo. |
| Responsividade | Fase C | Capturas desktop e celular sem overflow. |
| Documentos | Fase D | PDFs vazios/preenchidos e quatro impressoes A4. |
| Backup/restore | Fases D e F | Backup, restore, Flyway e abertura do sistema comprovados. |
| Seguranca E2E | Fase E | Login, troca, logout, expiracao, reinicio e perfis em navegador real. |

## 9. Medidas para evitar novos bugs

1. Nenhuma tela pode exibir sucesso antes de receber confirmacao persistida do backend.
2. Erros devem mostrar a mensagem real da API e preservar o formulario para nova tentativa.
3. Toda mudanca de estado deve ter regra no backend, transacao e historico; o frontend apenas solicita.
4. Novas tabelas e colunas entram somente por Flyway; migracoes aplicadas nunca sao editadas.
5. Alteracoes em estoque exigem teste de concorrencia e idempotencia.
6. Uploads exigem validacao de MIME/conteudo, gravacao atomica e teste de exclusao/retencao.
7. Cada fase termina com Git limpo, testes, build, smoke de navegador e documento de resultado.
8. Segurança deve ser testada no mesmo navegador e topologia de rede usados pelo usuario, nao somente via chamadas diretas de API.
9. Funcionalidades externas devem permanecer explicitamente `NAO_CONFIGURADO`, nunca simulando envio.
10. Bugs encontrados na homologacao devem gerar teste automatizado antes do fechamento.

## 10. Proxima acao recomendada

Executar a **Fase A - Baseline e checkpoint**. Ela reduz o risco imediato, registra a decisao de adiar autenticacao e cria uma base confiavel para a regressao operacional. Depois, seguir para a Fase B e implementar o modulo fiscal na Fase B.1, antes da revisao visual ampla da Fase C.
