import { useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  BarChart3,
  Building2,
  CalendarDays,
  CheckCircle2,
  Clock3,
  DollarSign,
  Eye,
  FileText,
  FilterX,
  Landmark,
  Pencil,
  Plus,
  Receipt,
  Search,
  X,
} from "lucide-react";
import Alert from "../components/Alert";
import LoadingSpinner from "../components/LoadingSpinner";
import api from "../services/api";

const hoje = () => new Date().toISOString().slice(0, 10);

const formularioInicial = {
  contratoId: "",
  projetoId: "",
  valorMedicao: "",
  servicosExecutados: "",
};

const filtrosIniciais = {
  tipoContratante: "",
  contratoId: "",
  situacao: "",
  numeroNotaFiscal: "",
  destino: "",
  dataInicio: "",
  dataFim: "",
  competencia: "",
};

const dinheiro = (valor) =>
  new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(Number(valor) || 0);

const dataBr = (valor) => {
  if (!valor) return "---";
  const [ano, mes, dia] = valor.split("-");
  return `${dia}/${mes}/${ano}`;
};

const mesBr = (valor) => {
  if (!valor) return "---";
  const [ano, mes] = valor.split("-");
  return `${mes}/${ano}`;
};

const nomeSituacao = {
  A_FATURAR: "A faturar",
  FATURADO: "A receber",
  EM_ATRASO: "Em atraso",
  PAGO: "Pago",
};

function StatusBadge({ situacao }) {
  const estilos = {
    A_FATURAR: "border-amber-200 bg-amber-50 text-amber-700",
    FATURADO: "border-blue-200 bg-blue-50 text-blue-700",
    EM_ATRASO: "border-red-200 bg-red-50 text-red-700",
    PAGO: "border-emerald-200 bg-emerald-50 text-emerald-700",
  };
  return (
    <span className={`inline-flex whitespace-nowrap rounded-full border px-2 py-1 text-xs font-bold ${estilos[situacao] || estilos.A_FATURAR}`}>
      {nomeSituacao[situacao] || situacao}
    </span>
  );
}

function AlertaFiscal({ alerta }) {
  const configuracao = {
    VENCIDO: ["Vencido", "border-red-200 bg-red-50 text-red-700"],
    VENCE_HOJE: ["Vence hoje", "border-amber-300 bg-amber-50 text-amber-800"],
    PROXIMO: ["Próximo", "border-amber-200 bg-amber-50 text-amber-700"],
    NO_PRAZO: ["No prazo", "border-emerald-200 bg-emerald-50 text-emerald-700"],
  }[alerta] || ["Sem NF", "border-slate-200 bg-slate-50 text-slate-600"];

  return (
    <span className={`inline-flex whitespace-nowrap rounded-full border px-2 py-1 text-xs font-bold ${configuracao[1]}`}>
      {configuracao[0]}
    </span>
  );
}

function Metrica({ titulo, valor, destaque = "slate", Icon }) {
  const cores = {
    slate: "text-slate-800 bg-slate-100",
    blue: "text-blue-700 bg-blue-50",
    green: "text-emerald-700 bg-emerald-50",
    red: "text-red-700 bg-red-50",
  };
  return (
    <div className="flex min-h-24 items-center justify-between rounded-lg border border-slate-200 bg-white p-4">
      <div className="min-w-0">
        <p className="text-xs font-bold uppercase text-slate-500">{titulo}</p>
        <p className="mt-1 truncate text-xl font-bold text-slate-900">{dinheiro(valor)}</p>
      </div>
      <div className={`ml-3 flex h-10 w-10 shrink-0 items-center justify-center rounded-lg ${cores[destaque]}`}>
        <Icon size={19} />
      </div>
    </div>
  );
}

function Barras({ titulo, dados, formatarValor = dinheiro }) {
  const maximo = Math.max(...dados.map((item) => item.valor), 0);
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-4">
      <h2 className="mb-4 flex items-center gap-2 text-sm font-bold text-slate-800">
        <BarChart3 size={17} className="text-blue-600" /> {titulo}
      </h2>
      <div className="space-y-3">
        {dados.map((item) => (
          <div key={item.rotulo} className="grid grid-cols-[72px_1fr_auto] items-center gap-3 text-xs">
            <span className="font-semibold text-slate-600">{item.rotulo}</span>
            <div className="h-2.5 overflow-hidden rounded-full bg-slate-100">
              <div
                className="h-full rounded-full bg-blue-600"
                style={{ width: `${maximo ? Math.max((item.valor / maximo) * 100, 3) : 0}%` }}
              />
            </div>
            <span className="min-w-24 text-right font-semibold text-slate-700">{formatarValor(item.valor)}</span>
          </div>
        ))}
        {dados.length === 0 && <p className="py-5 text-center text-sm text-slate-500">Sem dados para o período.</p>}
      </div>
    </section>
  );
}

export default function GestaoFaturamento() {
  const [faturamentos, setFaturamentos] = useState([]);
  const [contratos, setContratos] = useState([]);
  const [projetos, setProjetos] = useState([]);
  const [aba, setAba] = useState("faturamento");
  const [filtros, setFiltros] = useState(filtrosIniciais);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const [formModal, setFormModal] = useState(false);
  const [editandoId, setEditandoId] = useState(null);
  const [formData, setFormData] = useState(formularioInicial);
  const [detalhes, setDetalhes] = useState(null);
  const [notaModal, setNotaModal] = useState(null);
  const [notaData, setNotaData] = useState({ numero: "", emissao: hoje(), vencimento: "" });
  const [pagamentoModal, setPagamentoModal] = useState(null);
  const [dataPagamento, setDataPagamento] = useState(hoje());

  const carregarDados = async () => {
    try {
      setLoading(true);
      const [resFaturamentos, resContratos, resProjetos] = await Promise.all([
        api.get("/faturamentos/painel"),
        api.get("/contratos"),
        api.get("/projetos"),
      ]);
      setFaturamentos(resFaturamentos.data || []);
      setContratos(resContratos.data || []);
      setProjetos((resProjetos.data || []).filter((projeto) => !projeto.arquivado));
    } catch (err) {
      setError(err.response?.data?.erro || "Não foi possível carregar os dados financeiros.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregarDados();
  }, []);

  const faturamentosFiltrados = useMemo(
    () =>
      faturamentos.filter((item) => {
        const dataReferencia = item.dataEmissao || "";
        return (
          (!filtros.tipoContratante || item.tipoContratante === filtros.tipoContratante) &&
          (!filtros.contratoId || String(item.contratoId) === filtros.contratoId) &&
          (!filtros.situacao || item.situacao === filtros.situacao) &&
          (!filtros.numeroNotaFiscal ||
            String(item.numeroNotaFiscal || "").toLowerCase().includes(filtros.numeroNotaFiscal.toLowerCase())) &&
          (!filtros.destino || String(item.destino || "").toLowerCase().includes(filtros.destino.toLowerCase())) &&
          (!filtros.dataInicio || (dataReferencia && dataReferencia >= filtros.dataInicio)) &&
          (!filtros.dataFim || (dataReferencia && dataReferencia <= filtros.dataFim)) &&
          (!filtros.competencia || String(item.competenciaFiscal || "").startsWith(filtros.competencia))
        );
      }),
    [faturamentos, filtros],
  );

  const impostosFiltrados = faturamentosFiltrados.filter((item) => item.numeroNotaFiscal);

  const metricasFaturamento = useMemo(
    () =>
      faturamentosFiltrados.reduce(
        (acc, item) => {
          const valor = Number(item.valorMedicao) || 0;
          acc.total += valor;
          if (["FATURADO", "EM_ATRASO"].includes(item.situacao)) acc.receber += valor;
          if (item.situacao === "PAGO") acc.pago += valor;
          if (item.situacao === "EM_ATRASO") acc.atraso += valor;
          return acc;
        },
        { total: 0, receber: 0, pago: 0, atraso: 0 },
      ),
    [faturamentosFiltrados],
  );

  const metricasImpostos = useMemo(
    () =>
      impostosFiltrados.reduce(
        (acc, item) => {
          acc.base += Number(item.valorMedicao) || 0;
          acc.retido += Number(item.impostoRetido) || 0;
          acc.pagar += Number(item.impostoPagar) || 0;
          acc.total += Number(item.impostoTotal) || 0;
          return acc;
        },
        { base: 0, retido: 0, pagar: 0, total: 0 },
      ),
    [impostosFiltrados],
  );

  const faturamentoMensal = useMemo(() => {
    const grupos = new Map();
    impostosFiltrados.forEach((item) => {
      const chave = item.dataEmissao?.slice(0, 7);
      if (chave) grupos.set(chave, (grupos.get(chave) || 0) + Number(item.valorMedicao || 0));
    });
    return [...grupos.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .slice(-6)
      .map(([rotulo, valor]) => ({ rotulo: mesBr(rotulo), valor }));
  }, [impostosFiltrados]);

  const impostosPorCompetencia = useMemo(() => {
    const grupos = new Map();
    impostosFiltrados.forEach((item) => {
      const chave = item.competenciaFiscal?.slice(0, 7);
      if (chave) grupos.set(chave, (grupos.get(chave) || 0) + Number(item.impostoTotal || 0));
    });
    return [...grupos.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .slice(-6)
      .map(([rotulo, valor]) => ({ rotulo: mesBr(rotulo), valor }));
  }, [impostosFiltrados]);

  const projetosDoContrato = projetos.filter(
    (projeto) => String(projeto.contrato?.id) === String(formData.contratoId),
  );

  const abrirNovo = () => {
    const contratoId = contratos[0]?.id ? String(contratos[0].id) : "";
    const projeto = projetos.find((item) => String(item.contrato?.id) === contratoId);
    setEditandoId(null);
    setFormData({ ...formularioInicial, contratoId, projetoId: projeto ? String(projeto.id) : "" });
    setFormModal(true);
  };

  const abrirEdicao = (item) => {
    setEditandoId(item.id);
    setFormData({
      contratoId: String(item.contratoId || ""),
      projetoId: String(item.projetoId || ""),
      valorMedicao: item.valorMedicao || "",
      servicosExecutados: item.servicosExecutados || "",
    });
    setFormModal(true);
  };

  const salvarLancamento = async (event) => {
    event.preventDefault();
    try {
      const payload = {
        contrato: { id: Number(formData.contratoId) },
        projeto: { id: Number(formData.projetoId) },
        valorMedicao: Number(formData.valorMedicao),
        servicosExecutados: formData.servicosExecutados.trim() || null,
      };
      if (editandoId) await api.put(`/faturamentos/${editandoId}`, payload);
      else await api.post("/faturamentos", payload);
      setSuccess(editandoId ? "Lançamento atualizado." : "Lançamento criado.");
      setFormModal(false);
      await carregarDados();
    } catch (err) {
      setError(err.response?.data?.erro || "Não foi possível salvar o lançamento.");
    }
  };

  const abrirNota = (item) => {
    setNotaModal(item);
    setNotaData({ numero: "", emissao: hoje(), vencimento: "" });
  };

  const emitirNota = async (event) => {
    event.preventDefault();
    try {
      await api.put(`/faturamentos/${notaModal.id}/emitir-nota`, null, {
        params: {
          numeroNotaFiscal: notaData.numero.trim(),
          dataEmissao: notaData.emissao,
          dataVencimento: notaData.vencimento,
        },
      });
      setSuccess("Nota fiscal registrada e impostos calculados.");
      setNotaModal(null);
      await carregarDados();
    } catch (err) {
      setError(err.response?.data?.erro || "Não foi possível registrar a nota fiscal.");
    }
  };

  const abrirPagamento = (item) => {
    setPagamentoModal(item);
    setDataPagamento(hoje());
  };

  const registrarPagamento = async (event) => {
    event.preventDefault();
    try {
      await api.put(`/faturamentos/${pagamentoModal.id}/baixar-pagamento`, null, {
        params: { dataPagamento },
      });
      setSuccess("Pagamento registrado com data e situação atualizadas.");
      setPagamentoModal(null);
      await carregarDados();
    } catch (err) {
      setError(err.response?.data?.erro || "Não foi possível registrar o pagamento.");
    }
  };

  const limparFiltros = () => setFiltros(filtrosIniciais);

  if (loading) return <LoadingSpinner />;

  return (
    <div className="space-y-5">
      <header className="flex flex-col justify-between gap-4 border-b border-slate-200 pb-4 sm:flex-row sm:items-center">
        <div>
          <h1 className="flex items-center gap-2 text-2xl font-bold text-slate-900">
            <FileText size={24} className="text-blue-600" /> Gestão de Faturamento
          </h1>
          <p className="mt-1 text-sm text-slate-500">Cobranças, recebimentos e obrigações fiscais por nota.</p>
        </div>
        <button onClick={abrirNovo} className="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 text-sm font-bold text-white hover:bg-blue-700">
          <Plus size={17} /> Novo lançamento
        </button>
      </header>

      {error && <Alert type="error" message={error} onClose={() => setError(null)} />}
      {success && <Alert type="success" message={success} onClose={() => setSuccess(null)} />}

      <div className="inline-flex rounded-lg border border-slate-200 bg-slate-100 p-1">
        <button onClick={() => setAba("faturamento")} className={`flex h-9 items-center gap-2 rounded-md px-4 text-sm font-bold ${aba === "faturamento" ? "bg-white text-blue-700 shadow-sm" : "text-slate-600"}`}>
          <Receipt size={16} /> Faturamento
        </button>
        <button onClick={() => setAba("impostos")} className={`flex h-9 items-center gap-2 rounded-md px-4 text-sm font-bold ${aba === "impostos" ? "bg-white text-blue-700 shadow-sm" : "text-slate-600"}`}>
          <Landmark size={16} /> Impostos
        </button>
      </div>

      <section className="rounded-lg border border-slate-200 bg-white p-4">
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
          <label>
            <span className="mb-1 block text-xs font-bold uppercase text-slate-500">Tipo</span>
            <select value={filtros.tipoContratante} onChange={(e) => setFiltros({ ...filtros, tipoContratante: e.target.value })} className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm">
              <option value="">Público e privado</option>
              <option value="SETOR_PUBLICO">Setor público</option>
              <option value="SETOR_PRIVADO">Setor privado</option>
            </select>
          </label>
          <label>
            <span className="mb-1 block text-xs font-bold uppercase text-slate-500">Contrato</span>
            <select value={filtros.contratoId} onChange={(e) => setFiltros({ ...filtros, contratoId: e.target.value })} className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm">
              <option value="">Todos os contratos</option>
              {contratos.map((contrato) => <option key={contrato.id} value={contrato.id}>{contrato.contrato} - {contrato.cliente}</option>)}
            </select>
          </label>
          {aba === "faturamento" ? (
            <label>
              <span className="mb-1 block text-xs font-bold uppercase text-slate-500">Situação</span>
              <select value={filtros.situacao} onChange={(e) => setFiltros({ ...filtros, situacao: e.target.value })} className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm">
                <option value="">Todas</option>
                <option value="A_FATURAR">A faturar</option>
                <option value="FATURADO">A receber</option>
                <option value="EM_ATRASO">Em atraso</option>
                <option value="PAGO">Pago</option>
              </select>
            </label>
          ) : (
            <label>
              <span className="mb-1 block text-xs font-bold uppercase text-slate-500">Competência</span>
              <input type="month" value={filtros.competencia} onChange={(e) => setFiltros({ ...filtros, competencia: e.target.value })} className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm" />
            </label>
          )}
          <label>
            <span className="mb-1 block text-xs font-bold uppercase text-slate-500">Nota fiscal</span>
            <div className="relative">
              <Search size={15} className="absolute left-3 top-3 text-slate-400" />
              <input value={filtros.numeroNotaFiscal} onChange={(e) => setFiltros({ ...filtros, numeroNotaFiscal: e.target.value })} placeholder="Buscar número" className="h-10 w-full rounded-lg border border-slate-300 pl-9 pr-3 text-sm" />
            </div>
          </label>
          <label>
            <span className="mb-1 block text-xs font-bold uppercase text-slate-500">Comarca ou cliente</span>
            <input value={filtros.destino} onChange={(e) => setFiltros({ ...filtros, destino: e.target.value })} placeholder="Buscar destino" className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm" />
          </label>
          <label>
            <span className="mb-1 block text-xs font-bold uppercase text-slate-500">Emissão inicial</span>
            <input type="date" value={filtros.dataInicio} onChange={(e) => setFiltros({ ...filtros, dataInicio: e.target.value })} className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm" />
          </label>
          <label>
            <span className="mb-1 block text-xs font-bold uppercase text-slate-500">Emissão final</span>
            <input type="date" value={filtros.dataFim} onChange={(e) => setFiltros({ ...filtros, dataFim: e.target.value })} className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm" />
          </label>
          <button onClick={limparFiltros} className="mt-auto inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-slate-300 px-3 text-sm font-bold text-slate-700 hover:bg-slate-50">
            <FilterX size={16} /> Limpar filtros
          </button>
        </div>
      </section>

      {aba === "faturamento" ? (
        <>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <Metrica titulo="Total lançado" valor={metricasFaturamento.total} Icon={DollarSign} />
            <Metrica titulo="A receber" valor={metricasFaturamento.receber} destaque="blue" Icon={Clock3} />
            <Metrica titulo="Pago" valor={metricasFaturamento.pago} destaque="green" Icon={CheckCircle2} />
            <Metrica titulo="Em atraso" valor={metricasFaturamento.atraso} destaque="red" Icon={AlertTriangle} />
          </div>

          <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
            <Barras titulo="Faturamento mensal" dados={faturamentoMensal} />
            <section className="rounded-lg border border-slate-200 bg-white p-4">
              <h2 className="mb-4 flex items-center gap-2 text-sm font-bold text-slate-800"><DollarSign size={17} className="text-blue-600" /> Pago x a receber</h2>
              <div className="space-y-4">
                {[["Pago", metricasFaturamento.pago, "bg-emerald-600"], ["A receber", metricasFaturamento.receber, "bg-blue-600"]].map(([rotulo, valor, cor]) => {
                  const base = metricasFaturamento.pago + metricasFaturamento.receber;
                  return (
                    <div key={rotulo}>
                      <div className="mb-1 flex justify-between text-xs font-semibold text-slate-600"><span>{rotulo}</span><span>{dinheiro(valor)}</span></div>
                      <div className="h-3 overflow-hidden rounded-full bg-slate-100"><div className={`h-full rounded-full ${cor}`} style={{ width: `${base ? (valor / base) * 100 : 0}%` }} /></div>
                    </div>
                  );
                })}
              </div>
            </section>
          </div>

          <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
            <table className="w-full min-w-[1040px] text-left text-sm">
              <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase text-slate-500">
                <tr><th className="p-3">Contrato</th><th className="p-3">NF</th><th className="p-3">Emissão</th><th className="p-3">Valor</th><th className="p-3">Comarca / Cliente</th><th className="p-3">Vencimento</th><th className="p-3">Situação</th><th className="p-3 text-right">Ações</th></tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {faturamentosFiltrados.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50">
                    <td className="p-3"><div className="font-bold text-slate-800">{item.numeroContrato}</div><div className="text-xs text-slate-500">{item.tipoContratante === "SETOR_PRIVADO" ? "Privado" : "Público"}</div></td>
                    <td className="p-3 font-mono text-xs">{item.numeroNotaFiscal || "---"}</td>
                    <td className="p-3">{dataBr(item.dataEmissao)}</td>
                    <td className="p-3 font-semibold">{dinheiro(item.valorMedicao)}</td>
                    <td className="p-3"><div className="flex items-center gap-2"><Building2 size={15} className="text-slate-400" /><span>{item.destino}</span></div></td>
                    <td className="p-3">{dataBr(item.dataVencimento)}</td>
                    <td className="p-3"><StatusBadge situacao={item.situacao} /></td>
                    <td className="p-3"><div className="flex justify-end gap-1">
                      <button onClick={() => setDetalhes(item)} title="Ver detalhes" aria-label="Ver detalhes" className="rounded-md p-2 text-slate-500 hover:bg-slate-100"><Eye size={16} /></button>
                      {item.situacao === "A_FATURAR" && <button onClick={() => abrirEdicao(item)} title="Editar lançamento" aria-label="Editar lançamento" className="rounded-md p-2 text-blue-600 hover:bg-blue-50"><Pencil size={16} /></button>}
                      {item.situacao === "A_FATURAR" && <button onClick={() => abrirNota(item)} title="Registrar nota fiscal" aria-label="Registrar nota fiscal" className="rounded-md p-2 text-blue-600 hover:bg-blue-50"><Receipt size={16} /></button>}
                      {["FATURADO", "EM_ATRASO"].includes(item.situacao) && <button onClick={() => abrirPagamento(item)} title="Registrar pagamento" aria-label="Registrar pagamento" className="rounded-md p-2 text-emerald-600 hover:bg-emerald-50"><DollarSign size={16} /></button>}
                    </div></td>
                  </tr>
                ))}
                {faturamentosFiltrados.length === 0 && <tr><td colSpan="8" className="p-10 text-center text-slate-500">Nenhum lançamento encontrado.</td></tr>}
              </tbody>
            </table>
          </div>
        </>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <Metrica titulo="Base das NFs" valor={metricasImpostos.base} Icon={FileText} />
            <Metrica titulo="Retido 4,8%" valor={metricasImpostos.retido} destaque="blue" Icon={Landmark} />
            <Metrica titulo="A pagar 14,93%" valor={metricasImpostos.pagar} destaque="red" Icon={CalendarDays} />
            <Metrica titulo="Total de impostos" valor={metricasImpostos.total} destaque="slate" Icon={DollarSign} />
          </div>
          <Barras titulo="Impostos por competência" dados={impostosPorCompetencia} />
          <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
            <table className="w-full min-w-[1100px] text-left text-sm">
              <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase text-slate-500">
                <tr><th className="p-3">Contrato</th><th className="p-3">NF</th><th className="p-3">Emissão</th><th className="p-3">Competência</th><th className="p-3">Valor</th><th className="p-3">Retido 4,8%</th><th className="p-3">A pagar 14,93%</th><th className="p-3">Total</th><th className="p-3">Alerta</th></tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {impostosFiltrados.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50">
                    <td className="p-3 font-bold text-slate-800">{item.numeroContrato}</td><td className="p-3 font-mono text-xs">{item.numeroNotaFiscal}</td><td className="p-3">{dataBr(item.dataEmissao)}</td><td className="p-3 font-semibold">{dataBr(item.competenciaFiscal)}</td><td className="p-3">{dinheiro(item.valorMedicao)}</td><td className="p-3">{dinheiro(item.impostoRetido)}</td><td className="p-3">{dinheiro(item.impostoPagar)}</td><td className="p-3 font-bold">{dinheiro(item.impostoTotal)}</td><td className="p-3"><AlertaFiscal alerta={item.alertaFiscal} /></td>
                  </tr>
                ))}
                {impostosFiltrados.length === 0 && <tr><td colSpan="9" className="p-10 text-center text-slate-500">Nenhuma nota fiscal encontrada.</td></tr>}
              </tbody>
            </table>
          </div>
        </>
      )}

      {formModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-xl overflow-hidden rounded-lg bg-white shadow-xl">
            <div className="flex items-center justify-between border-b border-slate-200 p-4"><h2 className="font-bold text-slate-900">{editandoId ? "Editar lançamento" : "Novo lançamento"}</h2><button onClick={() => setFormModal(false)} aria-label="Fechar" className="rounded-md p-1 text-slate-500 hover:bg-slate-100"><X size={19} /></button></div>
            <form onSubmit={salvarLancamento} className="space-y-4 p-5">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <label><span className="mb-1 block text-xs font-bold uppercase text-slate-500">Contrato *</span><select required value={formData.contratoId} onChange={(e) => { const contratoId = e.target.value; const projeto = projetos.find((item) => String(item.contrato?.id) === contratoId); setFormData({ ...formData, contratoId, projetoId: projeto ? String(projeto.id) : "" }); }} className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm"><option value="">Selecione</option>{contratos.map((item) => <option key={item.id} value={item.id}>{item.contrato} - {item.cliente}</option>)}</select></label>
                <label><span className="mb-1 block text-xs font-bold uppercase text-slate-500">Projeto / comarca *</span><select required value={formData.projetoId} onChange={(e) => setFormData({ ...formData, projetoId: e.target.value })} className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm"><option value="">Selecione</option>{projetosDoContrato.map((item) => <option key={item.id} value={item.id}>Projeto #{item.id}{item.nomeComarcaVinculada ? ` - ${item.nomeComarcaVinculada}` : ""}</option>)}</select></label>
              </div>
              <label><span className="mb-1 block text-xs font-bold uppercase text-slate-500">Valor *</span><input required min="0.01" step="0.01" type="number" value={formData.valorMedicao} onChange={(e) => setFormData({ ...formData, valorMedicao: e.target.value })} className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm" /></label>
              <label><span className="mb-1 block text-xs font-bold uppercase text-slate-500">Serviços executados</span><textarea rows="3" value={formData.servicosExecutados} onChange={(e) => setFormData({ ...formData, servicosExecutados: e.target.value })} className="w-full rounded-lg border border-slate-300 p-3 text-sm" /></label>
              <div className="flex justify-end gap-2 border-t border-slate-100 pt-4"><button type="button" onClick={() => setFormModal(false)} className="h-10 rounded-lg border border-slate-300 px-4 text-sm font-bold text-slate-700">Cancelar</button><button type="submit" className="h-10 rounded-lg bg-blue-600 px-4 text-sm font-bold text-white hover:bg-blue-700">Salvar</button></div>
            </form>
          </div>
        </div>
      )}

      {notaModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-md rounded-lg bg-white shadow-xl">
            <div className="flex items-center justify-between border-b border-slate-200 p-4"><h2 className="font-bold text-slate-900">Registrar nota fiscal</h2><button onClick={() => setNotaModal(null)} aria-label="Fechar" className="rounded-md p-1 text-slate-500 hover:bg-slate-100"><X size={19} /></button></div>
            <form onSubmit={emitirNota} className="space-y-4 p-5">
              <label><span className="mb-1 block text-xs font-bold uppercase text-slate-500">Número da NF *</span><input required value={notaData.numero} onChange={(e) => setNotaData({ ...notaData, numero: e.target.value })} className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm" /></label>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2"><label><span className="mb-1 block text-xs font-bold uppercase text-slate-500">Emissão *</span><input required type="date" value={notaData.emissao} onChange={(e) => setNotaData({ ...notaData, emissao: e.target.value })} className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm" /></label><label><span className="mb-1 block text-xs font-bold uppercase text-slate-500">Vencimento *</span><input required min={notaData.emissao} type="date" value={notaData.vencimento} onChange={(e) => setNotaData({ ...notaData, vencimento: e.target.value })} className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm" /></label></div>
              <div className="rounded-lg border border-blue-100 bg-blue-50 p-3 text-sm text-blue-800">Os impostos serão calculados e vinculados automaticamente a esta NF.</div>
              <div className="flex justify-end gap-2"><button type="button" onClick={() => setNotaModal(null)} className="h-10 rounded-lg border border-slate-300 px-4 text-sm font-bold text-slate-700">Cancelar</button><button type="submit" className="h-10 rounded-lg bg-blue-600 px-4 text-sm font-bold text-white">Registrar NF</button></div>
            </form>
          </div>
        </div>
      )}

      {pagamentoModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-sm rounded-lg bg-white shadow-xl">
            <div className="flex items-center justify-between border-b border-slate-200 p-4"><h2 className="font-bold text-slate-900">Registrar pagamento</h2><button onClick={() => setPagamentoModal(null)} aria-label="Fechar" className="rounded-md p-1 text-slate-500 hover:bg-slate-100"><X size={19} /></button></div>
            <form onSubmit={registrarPagamento} className="space-y-4 p-5"><p className="text-sm text-slate-600">NF {pagamentoModal.numeroNotaFiscal} · {dinheiro(pagamentoModal.valorMedicao)}</p><label><span className="mb-1 block text-xs font-bold uppercase text-slate-500">Data do pagamento *</span><input required min={pagamentoModal.dataEmissao} max={hoje()} type="date" value={dataPagamento} onChange={(e) => setDataPagamento(e.target.value)} className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm" /></label><div className="flex justify-end gap-2"><button type="button" onClick={() => setPagamentoModal(null)} className="h-10 rounded-lg border border-slate-300 px-4 text-sm font-bold text-slate-700">Cancelar</button><button type="submit" className="h-10 rounded-lg bg-emerald-600 px-4 text-sm font-bold text-white">Confirmar</button></div></form>
          </div>
        </div>
      )}

      {detalhes && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-2xl overflow-hidden rounded-lg bg-white shadow-xl">
            <div className="flex items-center justify-between border-b border-slate-200 p-4"><h2 className="font-bold text-slate-900">Detalhes do lançamento</h2><button onClick={() => setDetalhes(null)} aria-label="Fechar" className="rounded-md p-1 text-slate-500 hover:bg-slate-100"><X size={19} /></button></div>
            <div className="grid grid-cols-1 gap-px bg-slate-200 sm:grid-cols-2">
              {[["Contrato", detalhes.numeroContrato], ["Destino", detalhes.destino], ["Nota fiscal", detalhes.numeroNotaFiscal || "Não emitida"], ["Situação", nomeSituacao[detalhes.situacao]], ["Emissão", dataBr(detalhes.dataEmissao)], ["Pagamento", dataBr(detalhes.dataPagamento)], ["Valor", dinheiro(detalhes.valorMedicao)], ["Total de impostos", dinheiro(detalhes.impostoTotal)]].map(([rotulo, valor]) => <div key={rotulo} className="bg-white p-4"><p className="text-xs font-bold uppercase text-slate-500">{rotulo}</p><p className="mt-1 text-sm font-semibold text-slate-800">{valor}</p></div>)}
            </div>
            <div className="border-t border-slate-200 p-4"><p className="text-xs font-bold uppercase text-slate-500">Serviços executados</p><p className="mt-2 whitespace-pre-wrap text-sm text-slate-700">{detalhes.servicosExecutados || "Não informado"}</p></div>
          </div>
        </div>
      )}
    </div>
  );
}
