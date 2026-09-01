# Resultado do piloto operacional - 01/09/2026

## Escopo

Piloto local autenticado do fluxo completo:

`Contrato -> Projeto -> OS -> Vistoria -> OR -> Retirada -> Técnico -> Virada de Rede -> Devolução -> Auditoria -> As-Built -> Encerramento`

## Registros utilizados

- Contrato: `0001 - TJ-PB`
- Projeto: `#9`, período de 01/09/2026 a 02/09/2026
- Obra/comarca: `PILOTO OPERACIONAL 01` (`#9`)
- Equipe: Testonio (supervisor técnico/líder) e Trab (técnico)
- OS: `0001 - OS 09` (`#9`)
- OR: `0001 - OS 09 - OR 01` (`#13`)
- Documento final: `ENCERRAMENTO_OS` (`#1`)

## Materiais do piloto

| Material | Previsto | Retirado | Devolvido | Consumo auditado |
| --- | ---: | ---: | ---: | ---: |
| BUCHA/GESSO | 2 | 2 | 1 | 1 |
| PORCA LENTILHA | 2 | 2 | 1 | 1 |

O As-Built foi homologado com divergência controlada porque o consumo real foi inferior ao previsto. A conciliação física entre retirada, devolução e quantidade auditada foi aceita corretamente.

## Resultado das etapas

1. Projeto, comarca e equipe vinculados ao contrato: aprovado.
2. OS criada com datas e materiais obrigatórios: aprovado.
3. OR gerada automaticamente junto com a OS: aprovado.
4. Foto e assinatura da vistoria persistidas; avanço bloqueado até ambas existirem: aprovado.
5. Retirada executada com dupla assinatura e identificação de conferente/retirante: aprovado.
6. Evidência fotográfica do técnico persistida e checklist de atividades padrão salvo: aprovado.
7. Validação do supervisor e Virada de Rede com prova fotográfica: aprovado.
8. Devolução parcial assinada e saldo devolvido ao estoque: aprovado.
9. Auditoria conciliada e As-Built homologado com divergência: aprovado.
10. Documento final com três assinaturas, três eventos de log e integridade verificada: aprovado.
11. Obra e OS encerradas: aprovado.

## Rastreabilidade da OS

A OS produziu oito eventos sequenciais:

1. `OS_CRIADA`
2. `VISTORIA_LIBERADA`
3. `MATERIAIS_RETIRADOS`
4. `RELATORIO_TECNICO_ENVIADO`
5. `RELATORIO_TECNICO_APROVADO`
6. `MATERIAIS_DEVOLVIDOS`
7. `AS_BUILT_HOMOLOGADO`
8. `OBRA_ENCERRADA`

Estado final: OS `CONCLUIDA`, obra `CONCLUIDA` e projeto com As-Built `HOMOLOGADO_COM_DIVERGENCIA`.

## Estoque antes e depois

- Valor inicial homologado: `R$ 40.674,36`.
- Valor final auditado: `R$ 40.674,12`.
- Variação: `R$ 0,24`, equivalente a uma BUCHA/GESSO (`R$ 0,15`) e uma PORCA LENTILHA (`R$ 0,09`).
- Materiais ativos: 62.
- Divergências entre saldo geral e depósito: 0.
- Reservas residuais: 0.
- Abas de OR importadas da planilha: 12.
- Vínculos incompletos de OR importada: 0.

## Defeito encontrado e corrigido

O formulário permitia criar um projeto sem data inicial quando o estado do frontend perdia atualizações concorrentes. A correção aplicada:

- atualiza campos do formulário a partir do estado anterior;
- rejeita no backend projetos sem data inicial;
- rejeita data final anterior à data inicial;
- inclui teste de regressão que comprovou a falha antes da correção e passou depois dela.

## Evidências técnicas

- Foto de vistoria salva no servidor.
- Evidência do técnico salva no servidor.
- Prova de Virada de Rede salva no servidor.
- OR com três documentos arquivados ao longo do ciclo.
- PDF final retornado pelo backend com HTTP 200 e 47.289 bytes.
- Documento final com três logs de assinatura e integridade `INTEGRO`.

## Conclusão

O piloto operacional completo foi homologado localmente. Não foram encontradas divergências estruturais de estoque após o ciclo. A próxima frente recomendada é registrar testes automatizados de integração para este caminho feliz e para concorrência de retirada/devolução, seguida da homologação responsiva das telas operacionais.
