# Resultado da Regressao Operacional - Fase B

Data: 22/07/2026

## Escopo executado

- Ambiente de desenvolvimento isolado em banco `poprc_phaseb_dev`.
- Backend isolado na porta `8086` e frontend na porta `5174`.
- Massa dev recriada por Flyway, sem reutilizar os dados operacionais do usuario.
- Smoke test no navegador das paginas Dashboard, Gestao de Obras, Estoque,
  Portal Tecnico e Auditoria de Retirada/Devolucao.
- Criacao de OS com material de consumo, ferramenta e bobina.
- Confirmacao da OR automatica e das reservas de quantidade e metragem.
- Regressao automatizada do ciclo OS -> OR -> Retirada -> Tecnico -> Devolucao
  -> Auditoria -> As-Built -> Encerramento -> Faturamento -> Pagamento.

## Defeitos encontrados e corrigidos

1. A massa dev era carregada depois da migracao de perfis e criava todos os
   usuarios com o perfil padrao `TECNICO`. O seed agora grava e corrige de forma
   idempotente os perfis `ADMIN`, `SUPERVISOR_TECNICO` e `TECNICO`.
2. A API aceitava registrar falta de material sem nenhum item selecionado e
   podia manter a pendencia textual depois da resolucao. A validacao agora exige
   ao menos um material da propria obra e remove a pendencia quando resolvida.

## Evidencias automatizadas

- `86` testes executados.
- `0` falhas e `0` erros.
- `19` migracoes Flyway aplicadas em banco descartavel `poprc_test`.
- Cenario principal valida encerramento, emissao da NF, transicao da OS para
  `FATURADA` e baixa do faturamento como `PAGO`.
- Cenarios criticos cobrem duas ORs, sobreposicao bloqueada, dupla assinatura,
  devolucao parcial, ferramenta obrigatoria, bobina, material faltante,
  divergencia justificada, concorrencia e imutabilidade de logs.

Comando reproduzivel:

```powershell
.\scripts\test.ps1
```

## Resultado

A Fase B foi aprovada para o modo local sem autenticacao. Nao foi identificado
sucesso falso nas operacoes exercitadas: os estados, reservas, devolucoes,
pendencias, encerramento e faturamento foram conferidos contra a persistencia.

O detalhamento fiscal solicitado na planilha de referencia nao faz parte desta
aprovacao. Ele permanece separado na Fase B.1, conforme
`ESPECIFICACAO_FATURAMENTO_E_IMPOSTOS.md`.

## Proxima fase

Fase B.1 - remodelagem de Faturamento e Impostos, com setor publico/privado,
nota fiscal unica, competencia tributaria, calculos de 4,8%, 14,93% e 19,73%,
filtros, alertas, totais e testes dos calculos.
