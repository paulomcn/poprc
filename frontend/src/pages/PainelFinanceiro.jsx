import { useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  Briefcase,
  Download,
  DollarSign,
  FileText,
  TrendingDown,
  TrendingUp,
} from "lucide-react";
import api from "../services/api";
import Alert from "../components/Alert";
import LoadingSpinner from "../components/LoadingSpinner";

const moeda = (valor) =>
  new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(
    Number(valor) || 0,
  );

const nomeStatus = (status) =>
  String(status || "SEM_STATUS").replaceAll("_", " ");

const classeResultado = (valor) =>
  Number(valor || 0) >= 0 ? "text-emerald-700" : "text-red-700";

export default function PainelFinanceiro() {
  const [contratos, setContratos] = useState([]);
  const [projetos, setProjetos] = useState([]);
  const [ordensServico, setOrdensServico] = useState([]);
  const [contratoId, setContratoId] = useState("");
  const [projetoId, setProjetoId] = useState("");
  const [ordemServicoId, setOrdemServicoId] = useState("");
  const [dados, setDados] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    Promise.all([
      api.get("/contratos"),
      api.get("/projetos"),
      api.get("/ordens-servico"),
    ])
      .then(([contratosResponse, projetosResponse, ordensResponse]) => {
        const contratosAtivos = (contratosResponse.data || []).filter(
          (contrato) => !contrato.arquivado,
        );
        setContratos(contratosAtivos);
        setProjetos((projetosResponse.data || []).filter((projeto) => !projeto.arquivado));
        setOrdensServico((ordensResponse.data || []).filter((ordem) => !ordem.arquivado));
        setContratoId(contratosAtivos[0]?.id ? String(contratosAtivos[0].id) : "");
        if (!contratosAtivos.length) setLoading(false);
      })
      .catch(() => {
        setError("Não foi possível carregar contratos, projetos e ordens de serviço.");
        setLoading(false);
      });
  }, []);

  const projetosFiltrados = useMemo(
    () =>
      projetos.filter(
        (projeto) => String(projeto.contrato?.id || "") === contratoId,
      ),
    [contratos, contratoId, projetos],
  );

  const ordensFiltradas = useMemo(
    () =>
      ordensServico.filter((ordem) => {
        const mesmoContrato = String(ordem.contrato?.id || "") === contratoId;
        const mesmoProjeto = !projetoId
          || String(ordem.projeto?.id || "") === projetoId;
        return mesmoContrato && mesmoProjeto;
      }),
    [contratoId, ordemServicoId, ordensServico, projetoId],
  );

  useEffect(() => {
    if (!contratoId) {
      setDados(null);
      return;
    }
    setLoading(true);
    api
      .get("/relatorios/lucratividade", {
        params: {
          contratoId,
          projetoId: projetoId || null,
          ordemServicoId: ordemServicoId || null,
        },
      })
      .then((response) => {
        setDados(response.data);
        setError(null);
      })
      .catch(() => setError("Não foi possível carregar o relatório financeiro."))
      .finally(() => setLoading(false));
  }, [contratoId, ordemServicoId, projetoId]);

  const selecionarContrato = (id) => {
    setContratoId(id);
    setProjetoId("");
    setOrdemServicoId("");
  };

  const selecionarProjeto = (id) => {
    setProjetoId(id);
    setOrdemServicoId("");
  };

  const exportarRelatorio = async () => {
    if (!dados) return;
    const ExcelJS = (await import("exceljs")).default;
    const workbook = new ExcelJS.Workbook();
    workbook.creator = "RC Operations Hub";
    workbook.created = new Date();

    const resumo = workbook.addWorksheet("Resumo");
    resumo.columns = [
      { header: "Indicador", key: "indicador", width: 34 },
      { header: "Valor", key: "valor", width: 22 },
    ];
    [
      ["Contrato", dados.numeroContrato],
      ["Projeto", dados.nomeProjeto],
      ["Ordem de Serviço", dados.numeroOs || "Todas"],
      ["Receita vinculada", Number(dados.totalFaturado || 0)],
      ["Custo de materiais", Number(dados.totalCustoMateriais || 0)],
      ["Custo de viagens", Number(dados.totalCustoViagens || 0)],
      ["Custo total", Number(dados.custoTotalAcumulado || 0)],
      ["Resultado operacional", Number(dados.lucroBruto || 0)],
      ["Margem (%)", Number(dados.margemLucro || 0)],
    ].forEach(([indicador, valor]) => resumo.addRow({ indicador, valor }));
    resumo.getColumn("valor").numFmt = 'R$ #,##0.00';
    resumo.getCell("B10").numFmt = '0.00"%"';

    const porOs = workbook.addWorksheet("Por OS", {
      views: [{ state: "frozen", ySplit: 1 }],
    });
    porOs.columns = [
      { header: "OS", key: "os", width: 30 },
      { header: "Projeto", key: "projeto", width: 14 },
      { header: "Status", key: "status", width: 24 },
      { header: "Receita", key: "receita", width: 18 },
      { header: "Materiais consumidos", key: "materiais", width: 22 },
      { header: "Resultado operacional", key: "resultado", width: 22 },
      { header: "Margem", key: "margem", width: 14 },
      { header: "Qualidade do custo", key: "qualidade", width: 24 },
    ];
    (dados.ordensServico || []).forEach((ordem) => {
      porOs.addRow({
        os: ordem.numeroOs,
        projeto: ordem.projetoId ? `Projeto #${ordem.projetoId}` : "Sem projeto",
        status: nomeStatus(ordem.status),
        receita: Number(ordem.totalFaturado || 0),
        materiais: Number(ordem.totalCustoMateriais || 0),
        resultado: Number(ordem.lucroOperacional || 0),
        margem: Number(ordem.margemLucro || 0),
        qualidade: !ordem.custoMateriaisDisponivel
          ? "Custo não informado"
          : ordem.custoMateriaisEstimado ? "Histórico estimado" : "Custo registrado",
      });
    });
    ["receita", "materiais", "resultado"].forEach((coluna) => {
      porOs.getColumn(coluna).numFmt = 'R$ #,##0.00';
    });
    porOs.getColumn("margem").numFmt = '0.00"%"';
    [resumo, porOs].forEach((planilha) => {
      planilha.getRow(1).font = { bold: true, color: { argb: "FFFFFFFF" } };
      planilha.getRow(1).fill = {
        type: "pattern",
        pattern: "solid",
        fgColor: { argb: "FF1E293B" },
      };
      planilha.getRow(1).alignment = { vertical: "middle", wrapText: true };
      planilha.autoFilter = {
        from: "A1",
        to: `${planilha.getColumn(planilha.columnCount).letter}1`,
      };
    });

    const buffer = await workbook.xlsx.writeBuffer();
    const url = URL.createObjectURL(
      new Blob([buffer], {
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      }),
    );
    const link = document.createElement("a");
    link.href = url;
    link.download = `lucratividade-${dados.numeroContrato || "contrato"}.xlsx`
      .replaceAll(" ", "-")
      .toLowerCase();
    link.click();
    URL.revokeObjectURL(url);
  };

  const resultadoPositivo = Number(dados?.lucroBruto || 0) >= 0;
  const temLancamentosSemOs =
    Number(dados?.receitaSemOrdemServico || 0) !== 0
    || Number(dados?.custoMateriaisSemOrdemServico || 0) !== 0;

  return (
    <div className="mx-auto max-w-[1480px] space-y-6">
      <header className="border-b border-slate-200 pb-5">
        <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
          <div>
            <p className="text-xs font-bold uppercase text-emerald-700">Gestão financeira</p>
            <h1 className="mt-1 text-3xl font-bold text-slate-900">Lucratividade Operacional</h1>
            <p className="mt-1 text-sm text-slate-500">
              Receita, materiais consumidos e despesas vinculadas ao fluxo da obra.
            </p>
          </div>
          <button
            type="button"
            onClick={exportarRelatorio}
            disabled={!dados}
            className="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-slate-800 px-4 text-sm font-bold text-white hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-40"
          >
            <Download size={17} /> Exportar Excel
          </button>
        </div>

        <div className="mt-5 grid gap-3 lg:grid-cols-3">
          <label className="block">
            <span className="mb-1.5 flex items-center gap-1.5 text-xs font-bold uppercase text-slate-500">
              <Briefcase size={14} /> Contrato
            </span>
            <select
              value={contratoId}
              onChange={(event) => selecionarContrato(event.target.value)}
              className="h-10 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm text-slate-700 outline-none focus:ring-2 focus:ring-blue-500"
            >
              {contratos.length === 0 && <option value="">Nenhum contrato disponível</option>}
              {contratos.map((contrato) => (
                <option key={contrato.id} value={contrato.id}>
                  {contrato.contrato} - {contrato.cliente}
                </option>
              ))}
            </select>
          </label>
          <label className="block">
            <span className="mb-1.5 block text-xs font-bold uppercase text-slate-500">Projeto</span>
            <select
              value={projetoId}
              onChange={(event) => selecionarProjeto(event.target.value)}
              className="h-10 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm text-slate-700 outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Todos os projetos</option>
              {projetosFiltrados.map((projeto) => (
                <option key={projeto.id} value={projeto.id}>Projeto #{projeto.id}</option>
              ))}
            </select>
          </label>
          <label className="block">
            <span className="mb-1.5 block text-xs font-bold uppercase text-slate-500">Ordem de Serviço</span>
            <select
              value={ordemServicoId}
              onChange={(event) => setOrdemServicoId(event.target.value)}
              className="h-10 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm text-slate-700 outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Todas as OS</option>
              {ordensFiltradas.map((ordem) => (
                <option key={ordem.id} value={ordem.id}>{ordem.numeroOs}</option>
              ))}
            </select>
          </label>
        </div>
      </header>

      {error && <Alert type="error" message={error} onClose={() => setError(null)} />}
      {!contratoId && !error && (
        <div className="rounded-lg border border-dashed border-slate-300 bg-white p-10 text-center text-sm text-slate-500">
          Cadastre um contrato para iniciar a análise financeira.
        </div>
      )}
      {loading && <LoadingSpinner />}

      {!loading && dados && (
        <>
          {!dados.custoMateriaisDisponivel && (
            <Aviso classe="border-amber-200 bg-amber-50 text-amber-800">
              Resultado parcial: há retiradas com custo médio não informado. Cadastre o valor do material no estoque para valorizar as próximas movimentações.
            </Aviso>
          )}
          {dados.custoMateriaisDisponivel && dados.custoMateriaisEstimado && (
            <Aviso classe="border-blue-200 bg-blue-50 text-blue-800">
              Movimentações anteriores ao histórico de custos usam o custo médio atual como estimativa. Novas entradas e retiradas preservam o valor da operação.
            </Aviso>
          )}
          {Number(dados.custoViagensNaoAlocado || 0) > 0 && (
            <Aviso classe="border-slate-300 bg-slate-50 text-slate-700">
              Existem {moeda(dados.custoViagensNaoAlocado)} em viagens do projeto que não foram rateadas nesta OS porque a viagem ainda não possui vínculo direto com uma Ordem de Serviço.
            </Aviso>
          )}
          {temLancamentosSemOs && !ordemServicoId && (
            <Aviso classe="border-slate-300 bg-slate-50 text-slate-700">
              O total inclui {moeda(dados.receitaSemOrdemServico)} de receita e {moeda(dados.custoMateriaisSemOrdemServico)} de materiais sem OS identificada.
            </Aviso>
          )}

          <section className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
            <Metrica
              titulo="Receita vinculada"
              valor={dados.totalFaturado}
              detalhe={dados.numeroOs || dados.nomeProjeto}
              Icon={DollarSign}
            />
            <Metrica
              titulo="Materiais consumidos"
              valor={dados.totalCustoMateriais}
              detalhe="Retiradas menos devoluções"
              Icon={TrendingDown}
              classe="border-orange-200 bg-orange-50 text-orange-700"
            />
            <Metrica
              titulo="Custos de viagens"
              valor={dados.totalCustoViagens}
              detalhe={ordemServicoId ? "Não rateados por OS" : "Prestações de contas"}
              Icon={TrendingDown}
              classe="border-rose-200 bg-rose-50 text-rose-700"
            />
            <Metrica
              titulo={dados.resultadoFinanceiroParcial ? "Resultado parcial" : "Resultado operacional"}
              valor={dados.lucroBruto}
              detalhe={`Margem de ${dados.margemLucro || 0}%`}
              Icon={TrendingUp}
              classe={resultadoPositivo
                ? "border-emerald-200 bg-emerald-50 text-emerald-700"
                : "border-red-200 bg-red-50 text-red-700"}
            />
          </section>

          <section className="overflow-hidden rounded-lg border border-slate-200 bg-white">
            <div className="flex flex-col gap-3 border-b border-slate-200 p-5 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h2 className="flex items-center gap-2 font-bold text-slate-900">
                  <FileText size={18} /> Composição por Ordem de Serviço
                </h2>
                <p className="mt-1 text-sm text-slate-500">
                  Viagens permanecem no consolidado do projeto ou contrato.
                </p>
              </div>
              <div className="text-right">
                <p className="text-xs uppercase text-slate-500">Custo operacional acumulado</p>
                <p className="font-bold text-slate-900">{moeda(dados.custoTotalAcumulado)}</p>
              </div>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[920px] text-left text-sm">
                <thead className="bg-slate-50 text-xs font-bold uppercase text-slate-500">
                  <tr>
                    <th className="px-5 py-3">OS</th>
                    <th className="px-5 py-3">Status</th>
                    <th className="px-5 py-3 text-right">Receita</th>
                    <th className="px-5 py-3 text-right">Materiais</th>
                    <th className="px-5 py-3 text-right">Resultado</th>
                    <th className="px-5 py-3 text-right">Margem</th>
                    <th className="px-5 py-3">Qualidade do custo</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {(dados.ordensServico || []).map((ordem) => (
                    <tr key={ordem.ordemServicoId} className="hover:bg-slate-50">
                      <td className="px-5 py-3">
                        <p className="font-semibold text-slate-900">{ordem.numeroOs}</p>
                        <p className="text-xs text-slate-500">Projeto #{ordem.projetoId}</p>
                      </td>
                      <td className="px-5 py-3 text-xs font-semibold text-slate-600">
                        {nomeStatus(ordem.status)}
                      </td>
                      <td className="px-5 py-3 text-right font-semibold">{moeda(ordem.totalFaturado)}</td>
                      <td className="px-5 py-3 text-right">{moeda(ordem.totalCustoMateriais)}</td>
                      <td className={`px-5 py-3 text-right font-bold ${classeResultado(ordem.lucroOperacional)}`}>
                        {moeda(ordem.lucroOperacional)}
                      </td>
                      <td className="px-5 py-3 text-right">{ordem.margemLucro || 0}%</td>
                      <td className="px-5 py-3">
                        <span className={`rounded px-2 py-1 text-xs font-semibold ${
                          !ordem.custoMateriaisDisponivel
                            ? "bg-amber-100 text-amber-800"
                            : ordem.custoMateriaisEstimado
                              ? "bg-blue-100 text-blue-800"
                              : "bg-emerald-100 text-emerald-800"
                        }`}>
                          {!ordem.custoMateriaisDisponivel
                            ? "Custo não informado"
                            : ordem.custoMateriaisEstimado ? "Estimado" : "Registrado"}
                        </span>
                      </td>
                    </tr>
                  ))}
                  {(dados.ordensServico || []).length === 0 && (
                    <tr>
                      <td colSpan="7" className="px-5 py-8 text-center text-slate-400">
                        Nenhuma OS encontrada para este recorte.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>

          <section className="flex flex-col gap-4 rounded-lg border border-slate-200 bg-white p-5 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="font-bold text-slate-900">Leitura do resultado</h2>
              <p className="mt-1 text-sm text-slate-500">
                Resultado calculado sobre registros efetivamente vinculados ao recorte.
              </p>
            </div>
            <span className={`w-fit rounded border px-3 py-1.5 text-xs font-bold ${
              dados.saudeFinanceira === "LUCRO_SAUDAVEL"
                ? "border-emerald-200 bg-emerald-50 text-emerald-700"
                : dados.saudeFinanceira === "ALERTA_MARGEM_BAIXA"
                  ? "border-amber-200 bg-amber-50 text-amber-700"
                  : dados.saudeFinanceira === "SEM_MOVIMENTACAO"
                    ? "border-slate-200 bg-slate-50 text-slate-600"
                    : "border-red-200 bg-red-50 text-red-700"
            }`}>
              {nomeStatus(dados.saudeFinanceira)}
            </span>
          </section>
        </>
      )}
    </div>
  );
}

function Aviso({ classe, children }) {
  return (
    <div className={`flex gap-3 rounded-lg border p-4 text-sm ${classe}`}>
      <AlertCircle size={19} className="shrink-0" />
      <p>{children}</p>
    </div>
  );
}

function Metrica({ titulo, valor, detalhe, Icon, classe = "border-slate-200 bg-white text-slate-500" }) {
  return (
    <div className={`rounded-lg border p-5 ${classe}`}>
      <div className="flex items-center justify-between">
        <p className="text-xs font-bold uppercase">{titulo}</p>
        <Icon size={20} />
      </div>
      <p className="mt-4 text-3xl font-bold text-slate-900">{moeda(valor)}</p>
      <p className="mt-2 text-xs text-slate-500">{detalhe}</p>
    </div>
  );
}
