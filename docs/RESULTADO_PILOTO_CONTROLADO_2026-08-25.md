# Resultado do Piloto Operacional Controlado

Data: 25/08/2026

## Cenário

- Banco: `poprc_local` (dados autorizados como massa de teste);
- contrato: `0001`;
- projeto: `#19`;
- comarca: `TJ-PB` (`#2`);
- OS: `0001 - OS 02` (`#2`);
- OR: `0001 - OS 02 - OR 01` (`#2`);
- assinaturas e imagens: sintéticas e identificadas como homologação.

Antes da execução foi gerado e restaurado com sucesso o backup
`backups/poprc_local-completo-20260825-145940.zip`.

## Fluxo executado

1. OS criada com 4 tampas e 2 bases previstas.
2. OR automática gerada e materiais reservados.
3. Documento inicial criado, assinado pelos três papéis e arquivado em PDF.
4. Vistoria concluída com foto e assinatura.
5. Retirada executada com dupla assinatura.
6. Checklist e evidência fotográfica do técnico registrados.
7. Virada de Rede concluída com prova fotográfica.
8. Relatório técnico enviado e aprovado.
9. OR devolvida com 1 tampa e 1 base.
10. Auditoria atualizada para o consumo líquido de 3 tampas e 1 base.
11. As-Built homologado com divergência legítima entre previsto e consumido.
12. Documento final criado, assinado pelos três papéis e arquivado em PDF.
13. Obra, OS e projeto encerrados.

## Conciliação do estoque

| Material | Saldo inicial | Retirado | Devolvido | Consumido | Saldo final | Reservado final |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| TAMPA | 169 | 4 | 1 | 3 | 166 | 0 |
| BASE | 186 | 2 | 1 | 1 | 185 | 0 |

As movimentações de reserva, retirada e devolução foram registradas uma única vez para cada material.

## Documentos e evidências

- documento inicial `#3`: `REGISTRADO`, 3 logs de assinatura e integridade `INTEGRO`;
- documento final `#4`: `REGISTRADO`, 3 logs de assinatura e integridade `INTEGRO`;
- PDF inicial: 48.371 bytes;
- PDF final: 48.374 bytes;
- PDF atual da OR: 10.648 bytes;
- snapshots arquivados da OR: 3;
- evidência técnica, foto da vistoria e prova da Virada: reabertas pelo servidor, 26.840 bytes cada.

## Estados finais

- projeto: `CONCLUIDO`;
- obra: `CONCLUIDA`, 100%;
- OS: `CONCLUIDA`;
- OR: `DEVOLVIDA`;
- As-Built: `HOMOLOGADO_COM_DIVERGENCIA`;
- histórico da OS: 8 transições, da criação ao encerramento.

## Defeito encontrado e corrigido

Ao concluir a Virada de Rede, a API aceitava texto no campo destinado ao caminho da foto e podia tornar o arquivo inacessível. A regra agora aceita somente referências de upload da Virada de Rede e impede a substituição por texto.

Foi adicionado o teste `bloqueiaSubstituicaoDaFotoDaViradaDeRedePorTexto`. A classe de integração do fluxo executou 15 testes, sem falhas ou erros. O registro do piloto foi reparado e a prova fotográfica voltou a abrir normalmente.

## Resultado

Piloto operacional aprovado. Não foram encontrados outros defeitos bloqueantes na sequência OS -> OR -> retirada -> técnico -> devolução -> auditoria -> documentos -> encerramento.
