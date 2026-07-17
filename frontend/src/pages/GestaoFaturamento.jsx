import { useState, useEffect } from "react";
import {
  FileText,
  Receipt,
  CheckCircle,
  Clock,
  DollarSign,
  X,
  AlertTriangle,
  Plus,
  Edit,
  AlertCircle,
  TrendingUp,
} from "lucide-react";
import api from "../services/api";
import LoadingSpinner from "../components/LoadingSpinner";
import Alert from "../components/Alert";

export default function GestaoFaturamento() {
  const [faturamentos, setFaturamentos] = useState([]);
  const [contratos, setContratos] = useState([]);
  const [projetos, setProjetos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // 🎛️ CONTROLE DOS MODAIS
  const [crudModalOpen, setCrudModalOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [selectedId, setSelectedId] = useState(null);

  const [nfModalOpen, setNfModalOpen] = useState(false);
  const [baixaModalOpen, setBaixaModalOpen] = useState(false);
  const [numeroNF, setNumeroNF] = useState("");
  const [vencimentoNF, setVencimentoNF] = useState("");
  const [filtroContrato, setFiltroContrato] = useState("");
  const [filtroSituacao, setFiltroSituacao] = useState("");

  // FORMULÁRIO COMPLETO
  const [formData, setFormData] = useState({
    contrato: { id: "" },
    projeto: { id: "" },
    servicosExecutados: "",
    valorMedicao: "",
    dataVencimento: "",
    numeroNotaFiscal: "",
    situacao: "A_FATURAR",
  });

  useEffect(() => {
    carregarDados();
  }, []);

  const carregarDados = async () => {
    try {
      setLoading(true);
      const [resFat, resCont, resProj] = await Promise.all([
        api.get("/faturamentos"),
        api.get("/contratos"),
        api.get("/projetos"),
      ]);
      setFaturamentos(resFat.data);
      setContratos(resCont.data);
      setProjetos((resProj.data || []).filter((projeto) => !projeto.arquivado));
    } catch (err) {
      console.error(err);
      setError("Erro ao sincronizar dados financeiros com o servidor.");
    } finally {
      setLoading(false);
    }
  };

  // 📈 CÁLCULO DINÂMICO DOS CARD DE METRICAS (Padrão Dashboard Executivo)
  const metricas = faturamentos.reduce(
    (acc, curr) => {
      const valor = parseFloat(curr.valorMedicao) || 0;
      const sit = String(curr.situacao).toUpperCase();

      if (sit === "A_FATURAR") acc.aFaturar += valor;
      else if (sit === "FATURADO") acc.faturado += valor;
      else if (sit === "PAGO" || sit === "LIQUIDADO") acc.pago += valor;
      else if (sit === "EM_ATRASO") acc.emAtraso += valor;

      return acc;
    },
    { aFaturar: 0, faturado: 0, pago: 0, emAtraso: 0 },
  );

  const faturamentosFiltrados = faturamentos.filter((faturamento) => {
    const contratoOk = !filtroContrato || String(faturamento.contrato?.id) === filtroContrato;
    const situacaoOk = !filtroSituacao || String(faturamento.situacao) === filtroSituacao;
    return contratoOk && situacaoOk;
  });

  const projetosDoContrato = projetos.filter(
    (projeto) => String(projeto.contrato?.id) === String(formData.contrato.id),
  );

  const handleOpenCreate = () => {
    const contratoId = contratos[0]?.id || "";
    const primeiroProjeto = projetos.find(
      (projeto) => String(projeto.contrato?.id) === String(contratoId),
    );
    setIsEditing(false);
    setSelectedId(null);
    setFormData({
      contrato: { id: contratoId },
      projeto: { id: primeiroProjeto?.id || "" },
      servicosExecutados: "",
      valorMedicao: "",
      dataVencimento: "",
      numeroNotaFiscal: "",
      situacao: "A_FATURAR",
    });
    setCrudModalOpen(true);
  };

  const handleOpenEdit = (f) => {
    setIsEditing(true);
    setSelectedId(f.id);
    setFormData({
      contrato: { id: f.contrato?.id || "" },
      projeto: { id: f.projeto?.id || "" },
      servicosExecutados: f.servicosExecutados || "",
      valorMedicao: f.valorMedicao || "",
      dataVencimento: f.dataVencimento || "",
      numeroNotaFiscal: f.numeroNotaFiscal || "",
      situacao: f.situacao || "A_FATURAR",
    });
    setCrudModalOpen(true);
  };

  const handleSaveFaturamento = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...formData,
        valorMedicao: parseFloat(formData.valorMedicao) || 0,
      };
      if (isEditing) {
        await api.put(`/faturamentos/${selectedId}`, payload);
        setSuccess("Medição atualizada com sucesso!");
      } else {
        await api.post("/faturamentos", payload);
        setSuccess("Nova medição lançada no sistema!");
      }
      setCrudModalOpen(false);
      carregarDados();
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.erro || "Erro ao salvar dados do faturamento.");
    }
  };

  const handleEmitirNota = async (id) => {
    setSelectedId(id);
    setNumeroNF("");
    setVencimentoNF("");
    setNfModalOpen(true);
  };
  const confirmarEmitirNota = async (e) => {
    e.preventDefault();
    if (!numeroNF.trim() || !vencimentoNF) {
      setError("Informe o número da nota fiscal e a data de vencimento.");
      return;
    }
    try {
      await api.put(`/faturamentos/${selectedId}/emitir-nota`, null, {
        params: { numeroNotaFiscal: numeroNF, dataVencimento: vencimentoNF },
      });
      setSuccess("Nota Fiscal registrada corporativamente!");
      setNfModalOpen(false);
      carregarDados();
    } catch (err) {
      setError(err.response?.data?.erro || "Erro ao emitir nota.");
    }
  };

  const handleBaixarPagamento = async (id) => {
    setSelectedId(id);
    setBaixaModalOpen(true);
  };
  const confirmarBaixarPagamento = async () => {
    try {
      await api.put(`/faturamentos/${selectedId}/baixar-pagamento`);
      setSuccess("Pagamento liquidado e caixa atualizado!");
      setBaixaModalOpen(false);
      carregarDados();
    } catch (err) {
      setError(err.response?.data?.erro || "Erro ao liquidar faturamento.");
    }
  };

  const formatCurrency = (value) =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(value || 0);

  if (loading) return <LoadingSpinner />;

  return (
    <div className="space-y-6">
      {/* Header Principal */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-slate-900 flex items-center gap-2">
            <FileText size={30} className="text-blue-600" /> Gestão de Faturamento
          </h1>
          <p className="text-slate-500 text-sm mt-0.5">
            Medições, emissão de notas e recebimentos vinculados aos projetos
          </p>
        </div>
        <button
          onClick={handleOpenCreate}
          className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2.5 rounded-xl font-bold text-sm flex items-center gap-2 shadow-md shadow-blue-600/10 transition-all"
        >
          <Plus size={18} /> Nova Medição
        </button>
      </div>

      {error && (
        <Alert type="error" message={error} onClose={() => setError(null)} />
      )}
      {success && (
        <Alert
          type="success"
          message={success}
          onClose={() => setSuccess(null)}
        />
      )}

      <div className="grid grid-cols-1 gap-4 rounded-lg border border-slate-200 bg-white p-4 sm:grid-cols-2">
        <label>
          <span className="mb-1 block text-xs font-bold uppercase text-slate-500">Contrato</span>
          <select value={filtroContrato} onChange={(event) => setFiltroContrato(event.target.value)} className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm">
            <option value="">Todos os contratos</option>
            {contratos.map((contrato) => <option key={contrato.id} value={contrato.id}>{contrato.contrato} - {contrato.cliente}</option>)}
          </select>
        </label>
        <label>
          <span className="mb-1 block text-xs font-bold uppercase text-slate-500">Situação</span>
          <select value={filtroSituacao} onChange={(event) => setFiltroSituacao(event.target.value)} className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm">
            <option value="">Todas as situações</option>
            <option value="A_FATURAR">A faturar</option>
            <option value="FATURADO">Faturado</option>
            <option value="EM_ATRASO">Em atraso</option>
            <option value="PAGO">Pago</option>
          </select>
        </label>
      </div>

      {/* 📊 SEÇÃO DE METRICAS DINÂMICAS REESTILIZADAS */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white border border-slate-200 p-5 rounded-xl shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">
              A Faturar
            </p>
            <p className="text-2xl font-black text-slate-800 mt-1">
              {formatCurrency(metricas.aFaturar)}
            </p>
          </div>
          <div className="p-3 bg-slate-50 text-slate-500 rounded-lg border border-slate-100">
            <Clock size={20} />
          </div>
        </div>

        <div className="bg-white border border-slate-200 p-5 rounded-xl shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">
              Faturado (Aguardando)
            </p>
            <p className="text-2xl font-black text-blue-600 mt-1">
              {formatCurrency(metricas.faturado)}
            </p>
          </div>
          <div className="p-3 bg-blue-50 text-blue-600 rounded-lg border border-blue-100">
            <Receipt size={20} />
          </div>
        </div>

        <div className="bg-white border border-slate-200 p-5 rounded-xl shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">
              Total Recebido
            </p>
            <p className="text-2xl font-black text-emerald-600 mt-1">
              {formatCurrency(metricas.pago)}
            </p>
          </div>
          <div className="p-3 bg-emerald-50 text-emerald-600 rounded-lg border border-emerald-100">
            <CheckCircle size={20} />
          </div>
        </div>

        <div className="bg-white border border-rose-100 p-5 rounded-xl shadow-sm flex items-center justify-between bg-gradient-to-br from-white to-rose-50/20">
          <div>
            <p className="text-xs font-bold text-rose-500 uppercase tracking-wider">
              Inadimplência / Atraso
            </p>
            <p className="text-2xl font-black text-rose-600 mt-1">
              {formatCurrency(metricas.emAtraso)}
            </p>
          </div>
          <div className="p-3 bg-rose-50 text-rose-600 rounded-lg border border-rose-100">
            <AlertCircle size={20} />
          </div>
        </div>
      </div>

      {/* Tabela Principal */}
      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-500 uppercase tracking-wider">
              <th className="p-4">Contrato</th>
              <th className="p-4">Projeto</th>
              <th className="p-4">Serviços Executados</th>
              <th className="p-4">Valor</th>
              <th className="p-4">Vencimento</th>
              <th className="p-4">Nota Fiscal</th>
              <th className="p-4">Situação</th>
              <th className="p-4 text-center">Etapa do Fluxo</th>
              <th className="p-4 text-center">Ajustar</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 text-sm text-slate-700">
            {faturamentosFiltrados.map((f) => {
              const situacaoStr = String(f.situacao).toUpperCase();
              return (
                <tr
                  key={f.id}
                  className="hover:bg-slate-50/50 transition-colors"
                >
                  <td className="p-4 font-bold text-slate-800">
                    {f.contrato?.contrato || "---"}
                  </td>
                  <td className="p-4 text-xs font-semibold text-slate-600">
                    {f.projeto?.id ? `Projeto #${f.projeto.id}` : "Histórico sem projeto"}
                  </td>
                  <td className="p-4 max-w-xs truncate text-slate-500">
                    {f.servicosExecutados || "---"}
                  </td>
                  <td className="p-4 font-mono font-semibold text-slate-900">
                    {formatCurrency(f.valorMedicao)}
                  </td>
                  <td className="p-4 font-medium">
                    {f.dataVencimento || "---"}
                  </td>
                  <td className="p-4 font-mono text-xs text-slate-400">
                    {f.numeroNotaFiscal || "---"}
                  </td>
                  <td className="p-4">
                    {/* Status Tags com visibilidade premium 💥 */}
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-xs font-bold border ${
                        situacaoStr === "PAGO"
                          ? "bg-green-50 text-green-700 border-green-200"
                          : situacaoStr === "FATURADO"
                            ? "bg-indigo-50 text-indigo-700 border-indigo-200"
                            : situacaoStr === "EM_ATRASO"
                              ? "bg-rose-50 text-rose-700 border-rose-200 animate-pulse"
                              : "bg-amber-50 text-amber-700 border-amber-200"
                      }`}
                    >
                      {situacaoStr === "EM_ATRASO"
                        ? "EM ATRASO"
                        : f.situacao}
                    </span>
                  </td>
                  <td className="p-4 text-center">
                    {/* LÓGICA DO FLUXO CORRIGIDA SEM FALSOS LIQUIDADOS 💥 */}
                    {situacaoStr === "A_FATURAR" ? (
                      <button
                        onClick={() => handleEmitirNota(f.id)}
                        className="bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold px-3 py-1.5 rounded-lg shadow-sm transition-all flex items-center gap-1 mx-auto"
                      >
                        <Receipt size={13} /> Emitir NF-e
                      </button>
                    ) : situacaoStr === "FATURADO" ? (
                      <button
                        onClick={() => handleBaixarPagamento(f.id)}
                        className="bg-green-600 hover:bg-green-700 text-white text-xs font-bold px-3 py-1.5 rounded-lg shadow-sm transition-all flex items-center gap-1 mx-auto"
                      >
                        <DollarSign size={13} /> Registrar Recebimento
                      </button>
                    ) : situacaoStr === "EM_ATRASO" ? (
                      /* Se está em atraso, a ação contábil ainda é receber o dinheiro! 💥 */
                      <button
                        onClick={() => handleBaixarPagamento(f.id)}
                        className="bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold px-3 py-1.5 rounded-lg shadow-sm transition-all flex items-center gap-1 mx-auto shadow-rose-600/10"
                      >
                        <AlertTriangle size={13} /> Registrar Recebimento
                        Atrasado
                      </button>
                    ) : (
                      <span className="text-xs text-green-600 font-bold bg-green-50 px-2.5 py-1 rounded-full border border-green-200 inline-flex items-center gap-1">
                        <CheckCircle size={12} /> Liquidado
                      </span>
                    )}
                  </td>
                  <td className="p-4 text-center">
                    <button
                      onClick={() => handleOpenEdit(f)}
                      disabled={situacaoStr !== "A_FATURAR"}
                      title={situacaoStr === "A_FATURAR" ? "Editar medição" : "Medições faturadas não podem ser editadas"}
                      className="p-2 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors disabled:cursor-not-allowed disabled:opacity-30"
                    >
                      <Edit size={16} />
                    </button>
                  </td>
                </tr>
              );
            })}
            {faturamentosFiltrados.length === 0 && (
              <tr><td colSpan="9" className="p-10 text-center text-sm text-slate-500">Nenhum lançamento encontrado para os filtros selecionados.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {/* 🛑 MODAL DO CRUD: CADASTRAR / EDITAR COMPLETO */}
      {crudModalOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-lg overflow-hidden animate-fade-in">
            <div className="p-5 bg-slate-900 text-white flex justify-between items-center">
              <h2 className="text-lg font-bold">
                {isEditing ? "✏️ Editar Lançamento" : "✨ Nova Medição"}
              </h2>
              <button
                onClick={() => setCrudModalOpen(false)}
                className="text-slate-400 hover:text-white"
              >
                <X size={22} />
              </button>
            </div>
            <form onSubmit={handleSaveFaturamento} className="p-6 space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase">
                    Contrato Vinculado *
                  </label>
                  <select
                    value={formData.contrato.id}
                    onChange={(e) => {
                      const contratoId = e.target.value;
                      const primeiroProjeto = projetos.find(
                        (projeto) => String(projeto.contrato?.id) === contratoId,
                      );
                      setFormData({
                        ...formData,
                        contrato: { id: contratoId },
                        projeto: { id: primeiroProjeto?.id || "" },
                      });
                    }}
                    className="w-full mt-1 p-2.5 border rounded-lg text-sm bg-slate-50 outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  >
                    {contratos.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.contrato} - {c.cliente}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase">
                    Projeto Vinculado *
                  </label>
                  <select
                    value={formData.projeto.id}
                    onChange={(e) => setFormData({ ...formData, projeto: { id: e.target.value } })}
                    className="w-full mt-1 p-2.5 border rounded-lg text-sm bg-slate-50 outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  >
                    <option value="">Selecione o projeto</option>
                    {projetosDoContrato.map((projeto) => (
                      <option key={projeto.id} value={projeto.id}>Projeto #{projeto.id}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase">
                    Valor da Medição (R$) *
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    value={formData.valorMedicao}
                    onChange={(e) =>
                      setFormData({ ...formData, valorMedicao: e.target.value })
                    }
                    className="w-full mt-1 p-2.5 border rounded-lg text-sm bg-slate-50 outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="0.00"
                  />
                </div>
              </div>
              <div>
                <label className="text-xs font-bold text-slate-600 uppercase">
                  Descrição dos Serviços
                </label>
                <textarea
                  rows="2"
                  value={formData.servicosExecutados}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      servicosExecutados: e.target.value,
                    })
                  }
                  className="w-full mt-1 p-2.5 border rounded-lg text-sm bg-slate-50 outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Ex: Certificação de rede estruturada..."
                ></textarea>
              </div>
              <button
                type="submit"
                className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2.5 rounded-lg font-semibold text-sm transition-colors shadow-sm mt-2"
              >
                Salvar Lançamento
              </button>
            </form>
          </div>
        </div>
      )}

      {/* MODAL OPERACIONAL 1: NOTA FISCAL */}
      {nfModalOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl border w-full max-w-md overflow-hidden p-6 space-y-4 animate-fade-in">
            <h2 className="text-lg font-bold flex items-center gap-2 text-slate-800">
              <Receipt size={20} className="text-blue-600" /> Registrar Nota
              Fiscal
            </h2>
            <input
              type="text"
              required
              value={numeroNF}
              onChange={(e) => setNumeroNF(e.target.value)}
              className="w-full p-2.5 border rounded-lg text-sm bg-slate-50 outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Número da NF-e"
            />
            <label className="block">
              <span className="mb-1 block text-xs font-bold uppercase text-slate-600">Vencimento *</span>
              <input
                type="date"
                required
                value={vencimentoNF}
                onChange={(event) => setVencimentoNF(event.target.value)}
                className="w-full p-2.5 border rounded-lg text-sm bg-slate-50 outline-none focus:ring-2 focus:ring-blue-500"
              />
            </label>
            <div className="flex gap-3">
              <button
                onClick={confirmarEmitirNota}
                className="flex-1 bg-blue-600 hover:bg-blue-700 text-white py-2 rounded-lg font-semibold text-sm transition-colors"
              >
                Confirmar
              </button>
              <button
                type="button"
                onClick={() => setNfModalOpen(false)}
                className="flex-1 bg-slate-100 hover:bg-slate-200 text-slate-700 py-2 rounded-lg font-semibold text-sm transition-colors"
              >
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL OPERACIONAL 2: BAIXA DE PAGAMENTO */}
      {baixaModalOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl border w-full max-w-sm overflow-hidden text-center p-6 space-y-4 animate-fade-in">
            <div className="mx-auto w-12 h-12 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center border border-emerald-200">
              <TrendingUp size={24} />
            </div>
            <div>
              <h3 className="text-xl font-bold text-slate-800">
                Confirmar Recebimento?
              </h3>
              <p className="text-sm text-slate-500 mt-2">
                Isto confirmará a entrada de capital no banco e alterará o
                status da medição para liquidado.
              </p>
            </div>
            <div className="flex gap-3">
              <button
                onClick={confirmarBaixarPagamento}
                className="flex-1 bg-green-600 hover:bg-green-700 text-white py-2.5 rounded-lg font-semibold text-sm transition-colors"
              >
                Confirmar Entrada
              </button>
              <button
                onClick={() => setBaixaModalOpen(false)}
                className="flex-1 bg-slate-100 hover:bg-slate-200 text-slate-700 py-2.5 rounded-lg font-semibold text-sm transition-colors"
              >
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
