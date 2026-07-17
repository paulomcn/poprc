import { useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  Briefcase,
  Edit,
  ExternalLink,
  FileText,
  Plane,
  Plus,
  Upload,
  User,
  X,
} from "lucide-react";
import api from "../services/api";
import { buildApiFileUrl } from "../services/runtimeConfig";
import Alert from "../components/Alert";
import LoadingSpinner from "../components/LoadingSpinner";

const formularioVazio = {
  solicitacaoVeiculo: "",
  hospedagemDetalhes: "",
  adiantamentoDiarias: "",
  custoPlanejado: "",
  funcionarioId: "",
  projetoId: "",
};

export default function PainelViagensEReembolso() {
  const [viagens, setViagens] = useState([]);
  const [funcionarios, setFuncionarios] = useState([]);
  const [projetos, setProjetos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [custosReais, setCustosReais] = useState({});
  const [comprovantes, setComprovantes] = useState({});
  const [filtroProjeto, setFiltroProjeto] = useState("");
  const [filtroFuncionario, setFiltroFuncionario] = useState("");
  const [filtroStatus, setFiltroStatus] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedId, setSelectedId] = useState(null);
  const [formData, setFormData] = useState(formularioVazio);

  const carregarDados = async () => {
    try {
      setLoading(true);
      const [resViagens, resFuncionarios, resProjetos] = await Promise.all([
        api.get("/financeiro/viagens"),
        api.get("/funcionarios"),
        api.get("/projetos"),
      ]);
      setViagens(Array.isArray(resViagens.data) ? resViagens.data : []);
      setFuncionarios(resFuncionarios.data || []);
      setProjetos((resProjetos.data || []).filter((projeto) => !projeto.arquivado));
      setError(null);
    } catch (err) {
      console.error(err);
      setError("Não foi possível carregar as viagens e prestações de contas.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregarDados();
  }, []);

  const viagensFiltradas = useMemo(
    () => viagens.filter((viagem) => {
      const concluida = Boolean(viagem.prestacaoContas);
      return (
        (!filtroProjeto || String(viagem.projeto?.id) === filtroProjeto) &&
        (!filtroFuncionario || String(viagem.funcionario?.id) === filtroFuncionario) &&
        (!filtroStatus || (filtroStatus === "CONCLUIDA") === concluida)
      );
    }),
    [filtroFuncionario, filtroProjeto, filtroStatus, viagens],
  );

  const metricas = viagens.reduce(
    (resultado, viagem) => {
      resultado.planejado += Number(viagem.custoPlanejado || 0);
      resultado.realizado += Number(viagem.prestacaoContas?.custoReal || 0);
      if (viagem.prestacaoContas) resultado.concluidas += 1;
      return resultado;
    },
    { planejado: 0, realizado: 0, concluidas: 0 },
  );

  const moeda = (valor) =>
    new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(valor || 0);

  const abrirCriacao = () => {
    setSelectedId(null);
    setFormData({
      ...formularioVazio,
      funcionarioId: funcionarios[0]?.id || "",
      projetoId: projetos[0]?.id || "",
    });
    setModalOpen(true);
  };

  const abrirEdicao = (viagem) => {
    setSelectedId(viagem.id);
    setFormData({
      solicitacaoVeiculo: viagem.solicitacaoVeiculo || "",
      hospedagemDetalhes: viagem.hospedagemDetalhes || "",
      adiantamentoDiarias: viagem.adiantamentoDiarias || "",
      custoPlanejado: viagem.custoPlanejado || "",
      funcionarioId: viagem.funcionario?.id || "",
      projetoId: viagem.projeto?.id || "",
    });
    setModalOpen(true);
  };

  const salvarViagem = async (event) => {
    event.preventDefault();
    try {
      const payload = {
        ...formData,
        adiantamentoDiarias: Number(formData.adiantamentoDiarias || 0),
        custoPlanejado: Number(formData.custoPlanejado || 0),
        funcionarioId: Number(formData.funcionarioId),
        projetoId: Number(formData.projetoId),
      };
      if (selectedId) await api.put(`/financeiro/viagens/${selectedId}`, payload);
      else await api.post("/financeiro/viagens", payload);
      setSuccess(selectedId ? "Viagem atualizada." : "Viagem planejada com sucesso.");
      setModalOpen(false);
      await carregarDados();
    } catch (err) {
      setError(err.response?.data?.erro || "Não foi possível salvar a viagem.");
    }
  };

  const fecharPrestacao = async (viagemId) => {
    const custoReal = Number(custosReais[viagemId]);
    const comprovante = comprovantes[viagemId];
    if (!custoReal || custoReal <= 0 || !comprovante) {
      setError("Informe o custo real e selecione o comprovante em PDF, JPG ou PNG.");
      return;
    }
    const payload = new FormData();
    payload.append("viagemId", String(viagemId));
    payload.append("custoReal", String(custoReal));
    payload.append("comprovante", comprovante);
    try {
      await api.post("/financeiro/prestacao-contas", payload);
      setSuccess("Prestação de contas concluída e comprovante arquivado.");
      setCustosReais((atual) => ({ ...atual, [viagemId]: "" }));
      setComprovantes((atual) => ({ ...atual, [viagemId]: null }));
      await carregarDados();
    } catch (err) {
      setError(err.response?.data?.erro || "Não foi possível concluir a prestação de contas.");
    }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div className="mx-auto max-w-[1450px] space-y-6">
      <header className="flex flex-col gap-4 border-b border-slate-200 pb-5 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-xs font-bold uppercase text-cyan-700">Logística e despesas</p>
          <h1 className="mt-1 text-3xl font-bold text-slate-900">Viagens e Reembolsos</h1>
          <p className="mt-1 text-sm text-slate-500">Planejamento, execução e prestação de contas por projeto.</p>
        </div>
        <button onClick={abrirCriacao} className="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 text-sm font-bold text-white hover:bg-blue-700">
          <Plus size={18} /> Nova viagem
        </button>
      </header>

      {error && <Alert type="error" message={error} onClose={() => setError(null)} />}
      {success && <Alert type="success" message={success} onClose={() => setSuccess(null)} />}

      <section className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <div className="rounded-lg border border-slate-200 bg-white p-5"><p className="text-xs font-bold uppercase text-slate-500">Viagens abertas</p><p className="mt-2 text-3xl font-bold text-slate-900">{viagens.length - metricas.concluidas}</p></div>
        <div className="rounded-lg border border-blue-200 bg-blue-50 p-5"><p className="text-xs font-bold uppercase text-slate-500">Custo planejado</p><p className="mt-2 text-2xl font-bold text-slate-900">{moeda(metricas.planejado)}</p></div>
        <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-5"><p className="text-xs font-bold uppercase text-slate-500">Custo comprovado</p><p className="mt-2 text-2xl font-bold text-slate-900">{moeda(metricas.realizado)}</p></div>
      </section>

      <section className="grid grid-cols-1 gap-4 rounded-lg border border-slate-200 bg-white p-4 md:grid-cols-3">
        <select value={filtroProjeto} onChange={(event) => setFiltroProjeto(event.target.value)} className="h-10 rounded-lg border border-slate-300 px-3 text-sm"><option value="">Todos os projetos</option>{projetos.map((projeto) => <option key={projeto.id} value={projeto.id}>Projeto #{projeto.id} - {projeto.contrato?.contrato}</option>)}</select>
        <select value={filtroFuncionario} onChange={(event) => setFiltroFuncionario(event.target.value)} className="h-10 rounded-lg border border-slate-300 px-3 text-sm"><option value="">Todos os colaboradores</option>{funcionarios.map((funcionario) => <option key={funcionario.id} value={funcionario.id}>{funcionario.nome}</option>)}</select>
        <select value={filtroStatus} onChange={(event) => setFiltroStatus(event.target.value)} className="h-10 rounded-lg border border-slate-300 px-3 text-sm"><option value="">Todos os status</option><option value="ABERTA">Em andamento</option><option value="CONCLUIDA">Concluída</option></select>
      </section>

      <section className="space-y-4">
        {viagensFiltradas.map((viagem) => {
          const prestacao = viagem.prestacaoContas;
          const excedeu = Number(prestacao?.custoReal || 0) > Number(viagem.custoPlanejado || 0);
          const comprovanteDisponivel =
            prestacao?.caminhoNotaFiscal?.startsWith("/") ||
            prestacao?.caminhoNotaFiscal?.startsWith("http://") ||
            prestacao?.caminhoNotaFiscal?.startsWith("https://");
          return (
            <article key={viagem.id} className="rounded-lg border border-slate-200 bg-white p-5">
              <div className="flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <Plane size={19} className="text-blue-600" />
                    <h2 className="font-bold text-slate-900">{viagem.solicitacaoVeiculo || "Transporte não informado"}</h2>
                    <span className={`rounded border px-2 py-0.5 text-[10px] font-bold ${prestacao ? "border-emerald-200 bg-emerald-50 text-emerald-700" : "border-blue-200 bg-blue-50 text-blue-700"}`}>{prestacao ? "CONCLUÍDA" : "EM ANDAMENTO"}</span>
                    {!prestacao && <button onClick={() => abrirEdicao(viagem)} title="Editar viagem" className="rounded p-1.5 text-slate-400 hover:bg-slate-100 hover:text-blue-700"><Edit size={16} /></button>}
                  </div>
                  <p className="mt-2 text-sm text-slate-500">{viagem.hospedagemDetalhes || "Sem hospedagem informada"}</p>
                  <div className="mt-4 flex flex-wrap gap-x-6 gap-y-2 text-xs text-slate-600">
                    <span className="flex items-center gap-1.5"><User size={14} /> {viagem.funcionario?.nome || "Sem colaborador"}</span>
                    <span className="flex items-center gap-1.5"><Briefcase size={14} /> Projeto #{viagem.projeto?.id || "-"}</span>
                    <span>Planejado: <strong>{moeda(viagem.custoPlanejado)}</strong></span>
                    <span>Adiantado: <strong>{moeda(viagem.adiantamentoDiarias)}</strong></span>
                  </div>
                </div>

                {prestacao ? (
                  <div className={`w-full rounded-lg border p-4 xl:w-80 ${excedeu ? "border-red-200 bg-red-50" : "border-emerald-200 bg-emerald-50"}`}>
                    <p className="text-xs font-bold uppercase text-slate-500">Custo real comprovado</p>
                    <p className="mt-1 text-2xl font-bold text-slate-900">{moeda(prestacao.custoReal)}</p>
                    {comprovanteDisponivel ? (
                      <a href={buildApiFileUrl(prestacao.caminhoNotaFiscal)} target="_blank" rel="noreferrer" className="mt-3 inline-flex items-center gap-1.5 text-sm font-bold text-blue-700 hover:text-blue-900"><FileText size={16} /> Abrir comprovante <ExternalLink size={14} /></a>
                    ) : (
                      <p className="mt-3 flex items-center gap-1.5 text-xs font-semibold text-amber-700"><AlertCircle size={15} /> Comprovante legado indisponível</p>
                    )}
                  </div>
                ) : (
                  <div className="grid w-full gap-3 rounded-lg border border-slate-200 bg-slate-50 p-4 sm:grid-cols-[140px_1fr_auto] xl:w-auto xl:min-w-[520px]">
                    <label><span className="mb-1 block text-xs font-bold text-slate-500">Custo real *</span><input type="number" min="0.01" step="0.01" value={custosReais[viagem.id] || ""} onChange={(event) => setCustosReais((atual) => ({ ...atual, [viagem.id]: event.target.value }))} className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm" placeholder="0,00" /></label>
                    <label><span className="mb-1 block text-xs font-bold text-slate-500">Comprovante *</span><span className="flex h-10 cursor-pointer items-center gap-2 rounded-lg border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-600"><Upload size={16} /> <span className="truncate">{comprovantes[viagem.id]?.name || "Selecionar PDF, JPG ou PNG"}</span><input type="file" accept="application/pdf,image/jpeg,image/png" className="hidden" onChange={(event) => setComprovantes((atual) => ({ ...atual, [viagem.id]: event.target.files?.[0] || null }))} /></span></label>
                    <button onClick={() => fecharPrestacao(viagem.id)} className="mt-5 h-10 rounded-lg bg-emerald-600 px-4 text-sm font-bold text-white hover:bg-emerald-700">Concluir</button>
                  </div>
                )}
              </div>
            </article>
          );
        })}
        {viagensFiltradas.length === 0 && <div className="rounded-lg border border-dashed border-slate-300 bg-white p-10 text-center text-sm text-slate-500">Nenhuma viagem encontrada.</div>}
      </section>

      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/70 p-4">
          <div className="w-full max-w-xl overflow-hidden rounded-lg bg-white shadow-xl">
            <div className="flex items-center justify-between border-b border-slate-200 p-5"><h2 className="font-bold text-slate-900">{selectedId ? "Editar viagem" : "Planejar nova viagem"}</h2><button onClick={() => setModalOpen(false)} title="Fechar" className="rounded p-2 text-slate-500 hover:bg-slate-100"><X size={19} /></button></div>
            <form onSubmit={salvarViagem} className="space-y-4 p-5">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <label><span className="text-xs font-bold uppercase text-slate-500">Colaborador *</span><select required value={formData.funcionarioId} onChange={(event) => setFormData({ ...formData, funcionarioId: event.target.value })} className="mt-1 h-10 w-full rounded-lg border border-slate-300 px-3 text-sm"><option value="">Selecione</option>{funcionarios.map((funcionario) => <option key={funcionario.id} value={funcionario.id}>{funcionario.nome}</option>)}</select></label>
                <label><span className="text-xs font-bold uppercase text-slate-500">Projeto *</span><select required value={formData.projetoId} onChange={(event) => setFormData({ ...formData, projetoId: event.target.value })} className="mt-1 h-10 w-full rounded-lg border border-slate-300 px-3 text-sm"><option value="">Selecione</option>{projetos.map((projeto) => <option key={projeto.id} value={projeto.id}>Projeto #{projeto.id} - {projeto.contrato?.contrato}</option>)}</select></label>
                <label><span className="text-xs font-bold uppercase text-slate-500">Custo planejado *</span><input required min="0.01" step="0.01" type="number" value={formData.custoPlanejado} onChange={(event) => setFormData({ ...formData, custoPlanejado: event.target.value })} className="mt-1 h-10 w-full rounded-lg border border-slate-300 px-3 text-sm" /></label>
                <label><span className="text-xs font-bold uppercase text-slate-500">Adiantamento</span><input min="0" step="0.01" type="number" value={formData.adiantamentoDiarias} onChange={(event) => setFormData({ ...formData, adiantamentoDiarias: event.target.value })} className="mt-1 h-10 w-full rounded-lg border border-slate-300 px-3 text-sm" /></label>
              </div>
              <label className="block"><span className="text-xs font-bold uppercase text-slate-500">Transporte / veículo *</span><input required value={formData.solicitacaoVeiculo} onChange={(event) => setFormData({ ...formData, solicitacaoVeiculo: event.target.value })} className="mt-1 h-10 w-full rounded-lg border border-slate-300 px-3 text-sm" /></label>
              <label className="block"><span className="text-xs font-bold uppercase text-slate-500">Hospedagem e observações</span><textarea rows="3" value={formData.hospedagemDetalhes} onChange={(event) => setFormData({ ...formData, hospedagemDetalhes: event.target.value })} className="mt-1 w-full rounded-lg border border-slate-300 p-3 text-sm" /></label>
              <button type="submit" className="w-full rounded-lg bg-blue-600 py-2.5 text-sm font-bold text-white hover:bg-blue-700">Salvar viagem</button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
