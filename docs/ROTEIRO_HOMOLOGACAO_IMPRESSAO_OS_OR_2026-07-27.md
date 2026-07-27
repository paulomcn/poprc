# Homologacao de Impressao - OS e OR

Data de preparacao: 27/07/2026

Status: PENDENTE DE VALIDACAO FISICA

## Objetivo

Validar em papel A4 o novo modelo da Ordem de Servico (OS) e os documentos da
Ordem de Retirada (OR), garantindo legibilidade, espaco para preenchimento e
assinatura, margens corretas e ausencia de cortes.

## Pre-condicoes

- Usar o commit `ed18d9a` ou posterior.
- Abrir o documento pelo fluxo normal do sistema.
- Selecionar papel A4.
- Usar orientacao retrato.
- Imprimir em escala 100% inicialmente.
- Desativar cabecalhos e rodapes adicionados pelo navegador.
- Registrar navegador, impressora e escala usados no resultado.

## Casos obrigatorios

### HIMP-01 - OS inicial vazia

- Abrir o documento da primeira etapa sem preencher campos opcionais.
- Confirmar que o modelo completo possui seis paginas.
- Imprimir e verificar se os campos permitem preenchimento manual.
- Conferir espaco para assinaturas, datas, carimbos e observacoes.

Resultado: [ ] Aprovado  [ ] Reprovado

### HIMP-02 - OS inicial preenchida

- Preencher textos, selecoes, datas e assinaturas digitais.
- Imprimir e verificar quebra de textos longos.
- Confirmar que nenhuma assinatura, foto ou texto invade outro campo.

Resultado: [ ] Aprovado  [ ] Reprovado

### HIMP-03 - OS final vazia

- Abrir o documento de encerramento sem preencher os campos finais.
- Confirmar que o esqueleto completo permanece visivel para assinatura local.
- Verificar as secoes de conformidade, aceite e salvaguarda tecnica.

Resultado: [ ] Aprovado  [ ] Reprovado

### HIMP-04 - OS final preenchida

- Preencher o encerramento, ressalvas, responsaveis, datas e assinaturas.
- Confirmar que os dados persistidos aparecem no documento impresso.
- Verificar as secoes de rack, aceite, representante designado e assinaturas.

Resultado: [ ] Aprovado  [ ] Reprovado

### HIMP-05 - OR antes da retirada

- Gerar uma OR ainda nao executada.
- Imprimir e conferir identificacao do contrato, OS, OR e lista de materiais.
- Confirmar espaco para as assinaturas de conferencia e retirada.

Resultado: [ ] Aprovado  [ ] Reprovado

### HIMP-06 - OR concluida e devolvida

- Abrir uma OR com retirada e devolucao registradas.
- Conferir quantidades retiradas, utilizadas e devolvidas.
- Verificar nomes, timestamps e assinaturas de todos os responsaveis.

Resultado: [ ] Aprovado  [ ] Reprovado

## Criterios gerais de aprovacao

- [ ] Nenhum conteudo e cortado pelas margens fisicas.
- [ ] Nao existem paginas extras em branco.
- [ ] Titulos, tabelas e campos permanecem alinhados.
- [ ] Textos longos quebram linha sem sobreposicao.
- [ ] Fotos mantem proporcao e resolucao legivel.
- [ ] Assinaturas possuem tamanho e contraste adequados.
- [ ] Campos vazios continuam disponiveis para preenchimento manual.
- [ ] Numeros de contrato, OS e OR aparecem corretamente.
- [ ] Todas as paginas podem ser relacionadas ao mesmo documento.
- [ ] A impressao continua legivel em preto e branco.

## Registro da execucao

- Data:
- Responsavel:
- Navegador e versao:
- Impressora:
- Escala utilizada:
- Resultado geral: [ ] APROVADO  [ ] REPROVADO
- Observacoes:
- Ajustes necessarios:

## Evidencias

Anexar fotos ou digitalizacoes das paginas impressas quando houver corte,
desalinhamento, baixa legibilidade ou diferenca em relacao a tela.

