# Resultado da Fase B.1 - Faturamento e Impostos

Data: 22/07/2026

## Entrega funcional

- Contratos classificados como `SETOR_PUBLICO` ou `SETOR_PRIVADO`.
- Faturamento separado visualmente por tipo de contratante.
- Servicos executados opcionais e visiveis nos detalhes.
- Emissao da NF com data de emissao e vencimento da cobranca.
- Pagamento com data persistida e situacao `PAGO`.
- Numero da NF unico dentro do contrato, protegido na aplicacao e no banco.
- Impostos derivados automaticamente da NF, sem redigitacao.
- Competencia fiscal no dia 20 do mes seguinte a emissao.
- Snapshot das aliquotas e valores fiscais no lancamento.
- Filtros por contrato, tipo, situacao, NF, destino, emissao e competencia.
- Totais, alertas de vencimento e graficos compactos nas duas abas.

## Conciliacao com a planilha

| NF | Valor | Retido 4,8% | A pagar 14,93% | Total |
|---|---:|---:|---:|---:|
| 14 | R$ 23.450,00 | R$ 1.125,60 | R$ 3.501,09 | R$ 4.626,69 |
| 15 | R$ 30.524,00 | R$ 1.465,15 | R$ 4.557,23 | R$ 6.022,38 |

O arredondamento monetario utiliza `HALF_UP` em duas casas. O total e a soma
dos dois componentes ja arredondados.

## Banco e API

- Migracao Flyway `V20__faturamento_impostos_e_tipo_contratante.sql`.
- Endpoint consolidado `GET /api/faturamentos/painel`.
- `PUT /api/faturamentos/{id}/emitir-nota` exige emissao e vencimento.
- `PUT /api/faturamentos/{id}/baixar-pagamento` exige data de pagamento.
- Registros antigos recebem classificacao publica controlada e snapshots
  fiscais quando ja possuem NF.
- NFs legadas duplicadas recebem sufixo de identificacao antes da criacao do
  indice unico, preservando o valor original no numero ajustado.

## Homologacao

- `92` testes automatizados aprovados, sem falhas ou erros.
- `20` migracoes aplicadas em banco descartavel `poprc_test`.
- Build do frontend aprovado.
- Smoke test em `poprc_phaseb1_dev`, backend `8086` e frontend `5174`.
- Desktop validado com as abas Faturamento e Impostos.
- Mobile validado em `390 x 844`, sem overflow horizontal da pagina.
- Nenhum erro registrado no console do navegador.
- Ambiente temporario e banco descartavel removidos ao final.

## Defeito adicional corrigido

O cadastro de contrato podia enviar `arquivado=null` e colidir com a restricao
do banco. O backend agora normaliza novos contratos para `arquivado=false`, com
teste automatizado especifico.

## Proxima fase

Fase C - coerencia visual e usabilidade do Dashboard, frontpage, Projetos,
Ordens de Servico, Gestao de Obras, Portal Tecnico e Auditoria.
