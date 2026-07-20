# Homologacao fisica dos documentos

**Status:** APROVADA NA CAMADA DIGITAL em 20/07/2026. A impressao fisica em A4 permanece pendente.

Use este roteiro antes de liberar os documentos de OS e OR para operacao.

## Configuracao da impressao

- Papel: A4.
- Escala: 100% ou tamanho real.
- Desative opcoes como "Ajustar a pagina" na primeira validacao.
- Imprima inicialmente apenas um lado para facilitar a conferencia.
- Use uma impressora e o navegador que serao utilizados na operacao real.

## Documentos para conferir

- OS inicial vazia: os campos devem permitir preenchimento manual.
- OS inicial preenchida: textos e assinaturas devem permanecer legiveis.
- OS de encerramento vazia e preenchida: confira as cinco paginas.
- OR antes da retirada: quantidades retiradas e devolvidas devem ficar em branco.
- OR concluida: quantidades, responsaveis, datas e assinaturas devem aparecer.

## Criterios de aceite

- Nenhum texto, rodape, assinatura ou borda foi cortado.
- A escala permite escrever nos campos vazios com caneta.
- As quebras de pagina correspondem ao PDF exibido no sistema.
- As assinaturas manuscritas estao nitidas e identificadas.
- A OS inicial exibe "Abertura e Vistoria Tecnica Inicial".
- A OS final exibe "Encerramento, Aceite e Conformidade Tecnica".
- A OR identifica contrato, OS e sequencial da retirada corretamente.

Registre a impressora, o navegador, a data e o responsavel pela homologacao. Caso algum item seja reprovado, fotografe ou digitalize a pagina e anote qual documento e pagina apresentaram o problema.

## Resultado da camada digital - 20/07/2026

A fixture `DocumentoPdfVisualFixtureTest` gerou os quatro documentos, validou
o cabecalho PDF e a quantidade esperada de paginas. Todas as paginas foram
renderizadas em PNG a 110 DPI e inspecionadas visualmente.

| Documento | Paginas | Bytes | SHA-256 |
| --- | ---: | ---: | --- |
| OS inicial vazia | 5 | 38.916 | `e2f5f4395a9b5aa5228cfa45bad9c1059f3e2532b503f89797dfb4b2121578e0` |
| OS de encerramento preenchida | 5 | 42.622 | `6395eb3030232e5491c11ffd8315b7feff0a626d2d3d50953f11ee88bbea1dc8` |
| OR antes da retirada | 1 | 9.028 | `1817d2d8278cdfb8a48c7ebb3ea9df6aafc42eed7e5bc3b30d382eeb0c19bfdf` |
| OR concluida | 1 | 11.645 | `0592c6baa4810c5bf2ebdd24bee869916b7a6da5cf5014ecf95be6998b4b6405` |

Resultado visual digital:

- Nenhum texto, assinatura, tabela, cabecalho ou rodape foi cortado.
- Nao foram encontradas sobreposicoes, quadrados pretos ou glifos quebrados.
- Os titulos inicial e final da OS estao corretos.
- Campos vazios permanecem disponiveis para preenchimento manual.
- Dados, datas, quantidades e assinaturas aparecem na OR concluida.
- Identificacao e sequencial de contrato, OS e OR estao legiveis.

### Pendente para aceite fisico

Imprimir os quatro documentos em papel A4, escala 100%, usando o navegador e
a impressora da operacao. Registrar modelo da impressora, navegador, data e
responsavel. Essa verificacao confirma margens mecanicas, escala real para
escrita e nitidez da tinta, aspectos que nao podem ser certificados apenas pelo
arquivo digital.
