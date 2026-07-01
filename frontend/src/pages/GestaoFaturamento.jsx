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
} from "lucide-react";
import api from "../services/api";
import LoadingSpinner from "../components/LoadingSpinner";
import Alert from "../components/Alert";

export default function GestaoFaturamento() {
  const [faturamentos, setFaturamentos] = useState([]);
  const [contratos, setContratos] = useState([]);
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

  // FORMULÁRIO COMPLETO DO CRUD
  const [formData, setFormData] = useState({
    contrato: { id: "" },
    servicosExecutados: "",
    valorMedicao: "",
    dataVencimento: "",
    numeroNotaFiscal: "",
    situacao: "PENDENTE",
  });

  useEffect(() => {
    carregarDados();
  }, []);

  const carregarDados = async () => {
    try {
      setLoading(true);
      const [resFat, resCont] = await Promise.all([
        api.get("/faturamentos"),
        api.get("/contratos"),
      ]);
      setFaturamentos(resFat.data);
      setContratos(resCont.data);
    } catch (err) {
      console.error(err);
      setError("Erro ao sincronizar dados financeiros com o servidor.");
    } finally {
      setLoading(false);
    }
  };

  // 💾 ABRIR MODAL PARA NOVO CADASTRO
  const handleOpenCreate = () => {
    setIsEditing(false);
    setSelectedId(null);
    setFormData({
      contrato: { id: contratos[0]?.id || "" },
      servicosExecutados: "",
      valorMedicao: "",
      dataVencimento: "",
      numeroNotaFiscal: "",
      situacao: "PENDENTE",
    });
    setCrudModalOpen(true);
  };

  // ✏️ ABRIR MODAL PARA EDIÇÃO GERAL
  const handleOpenEdit = (f) => {
    setIsEditing(true);
    setSelectedId(f.id);
    setFormData({
      contrato: { id: f.contrato?.id || "" },
      servicosExecutados: f.servicosExecutados || "",
      valorMedicao: f.valorMedicao || "",
      dataVencimento: f.dataVencimento || "",
      numeroNotaFiscal: f.numeroNotaFiscal || "",
      situacao: f.situacao || "PENDENTE",
    });
    setCrudModalOpen(true);
  };

  // 🚀 DISPARAR SALVAMENTO GERAL (POST OU PUT)
  const handleSaveFaturamento = async (e) => {
    e.preventDefault();
    try {
      if (isEditing) {
        await api.put(`/faturamentos/${selectedId}`, formData);
        setSuccess("Medição atualizada com sucesso!");
      } else {
        await api.post("/faturamentos", formData);
        setSuccess("Nova medição lançada no sistema!");
      }
      setCrudModalOpen(false);
      carregarDados();
    } catch (err) {
      console.error(err);
      setError("Erro ao salvar dados do faturamento.");
    }
  };

  const handleEmitirNota = async (id) => {
    setSelectedId(id);
    setNumeroNF("");
    setNfModalOpen(true);
  };
  const confirmarEmitirNota = async (e) => {
    e.preventDefault();
    try {
      await api.put(`/faturamentos/${selectedId}/emitir-nota`, null, {
        params: { numeroNotaFiscal: numeroNF },
      });
      setSuccess("Nota Fiscal registrada!");
      setNfModalOpen(false);
      carregarDados();
    } catch (err) {
      setError("Erro ao emitir nota.");
    }
  };

  const handleBaixarPagamento = async (id) => {
    setSelectedId(id);
    setBaixaModalOpen(true);
  };
  const confirmarBaixarPagamento = async () => {
    try {
      await api.put(`/faturamentos/${selectedId}/baixar-pagamento`);
      setSuccess("Pagamento liquidado!");
      setBaixaModalOpen(false);
      carregarDados();
    } catch (err) {
      setError("Erro ao liquidar faturamento.");
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
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-slate-800 flex items-center gap-2">
            <FileText size={32} /> Gestão de Faturamentos
          </h1>
          <p className="text-slate-600 mt-1">
            Lançamentos de medições, emissão de NF-e e fluxo de caixa
          </p>
        </div>
        <button
          onClick={handleOpenCreate}
          className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2.5 rounded-lg font-semibold flex items-center gap-2 shadow-sm transition-colors"
        >
          <Plus size={20} /> Nova Medição
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

      {/* Tabela Principal */}
      <div className="bg-white rounded-xl shadow-md border border-slate-200 overflow-hidden">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-500 uppercase">
              <th className="p-4">Contrato</th>
              <th className="p-4">Serviços Executados</th>
              <th className="p-4">Valor</th>
              <th className="p-4">Vencimento</th>
              <th className="p-4">Nota Fiscal</th>
              <th className="p-4">Situação</th>
              <th className="p-4 text-center">Ações Operacionais</th>
              <th className="p-4 text-center">Ajustar</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 text-sm text-slate-700">
            {faturamentos.map((f) => {
              const situacaoStr = String(f.situacao).toUpperCase();
              return (
                <tr
                  key={f.id}
                  className="hover:bg-slate-50/80 transition-colors"
                >
                  <td className="p-4 font-semibold text-slate-800">
                    {f.contrato?.contrato || "---"}
                  </td>
                  <td className="p-4 max-w-xs truncate">
                    {f.servicosExecutados || "---"}
                  </td>
                  <td className="p-4 font-mono font-medium text-slate-900">
                    {formatCurrency(f.valorMedicao)}
                  </td>
                  <td className="p-4">{f.dataVencimento || "---"}</td>
                  <td className="p-4 font-mono text-xs text-slate-500">
                    {f.numeroNotaFiscal || "🔄 Aguardando"}
                  </td>
                  <td className="p-4">
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-xs font-bold flex items-center gap-1 w-fit ${
                        situacaoStr === "PAGO"
                          ? "bg-green-100 text-green-700"
                          : situacaoStr === "FATURADO"
                            ? "bg-blue-100 text-blue-700"
                            : "bg-amber-100 text-amber-700"
                      }`}
                    >
                      {f.situacao}
                    </span>
                  </td>
                  <td className="p-4 text-center">
                    {situacaoStr === "PENDENTE" ? (
                      <button
                        onClick={() => handleEmitirNota(f.id)}
                        className="bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold px-3 py-1.5 rounded-lg shadow-sm"
                      >
                        Emitir NF-e
                      </button>
                    ) : situacaoStr === "FATURADO" ? (
                      <button
                        onClick={() => handleBaixarPagamento(f.id)}
                        className="bg-green-600 hover:bg-green-700 text-white text-xs font-bold px-3 py-1.5 rounded-lg flex items-center gap-1 mx-auto shadow-sm"
                      >
                        <DollarSign size={14} /> Baixar Pagamento
                      </button>
                    ) : (
                      <span className="text-xs text-green-600 font-bold bg-green-50 px-2.5 py-1 rounded-full border border-green-200">
                        💰 Liquidado
                      </span>
                    )}
                  </td>
                  <td className="p-4 text-center">
                    <button
                      onClick={() => handleOpenEdit(f)}
                      className="p-2 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                    >
                      <Edit size={16} />
                    </button>
                  </td>
                </tr>
              );
            })}
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
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        contrato: { id: e.target.value },
                      })
                    }
                    className="w-full mt-1 p-2.5 border rounded-lg text-sm bg-slate-50"
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
                    className="w-full mt-1 p-2.5 border rounded-lg text-sm bg-slate-50"
                    placeholder="0.00"
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase">
                    Data Vencimento *
                  </label>
                  <input
                    type="date"
                    required
                    value={formData.dataVencimento}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        dataVencimento: e.target.value,
                      })
                    }
                    className="w-full mt-1 p-2.5 border rounded-lg text-sm bg-slate-50"
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase">
                    Situação / Status
                  </label>
                  <select
                    value={formData.situacao}
                    onChange={(e) =>
                      setFormData({ ...formData, situacao: e.target.value })
                    }
                    className="w-full mt-1 p-2.5 border rounded-lg text-sm bg-slate-50"
                  >
                    <option value="PENDENTE">PENDENTE</option>
                    <option value="FATURADO">FATURADO</option>
                    <option value="PAGO">PAGO</option>
                  </select>
                </div>
              </div>
              <div>
                <label className="text-xs font-bold text-slate-600 uppercase">
                  Número da Nota (Opcional)
                </label>
                <input
                  type="text"
                  value={formData.numeroNotaFiscal}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      numeroNotaFiscal: e.target.value,
                    })
                  }
                  className="w-full mt-1 p-2.5 border rounded-lg text-sm bg-slate-50"
                  placeholder="Apenas se já faturado"
                />
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
                  className="w-full mt-1 p-2.5 border rounded-lg text-sm bg-slate-50"
                  placeholder="Ex: Execução de cabeamento estruturado e as-built da comarca..."
                ></textarea>
              </div>
              <button
                type="submit"
                className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2.5 rounded-lg font-semibold text-sm transition-colors shadow-sm"
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
          <div className="bg-white rounded-xl shadow-2xl border w-full max-w-md overflow-hidden p-6 space-y-4">
            <h2 className="text-lg font-bold flex items-center gap-2">
              <Receipt size={20} /> Registrar Nota Fiscal
            </h2>
            <input
              type="text"
              required
              value={numeroNF}
              onChange={(e) => setNumeroNF(e.target.value)}
              className="w-full p-2.5 border rounded-lg text-sm bg-slate-50"
              placeholder="Número da NF-e"
            />
            <div className="flex gap-3">
              <button
                onClick={confirmarEmitirNota}
                className="flex-1 bg-blue-600 text-white py-2 rounded-lg font-semibold text-sm"
              >
                Confirmar
              </button>
              <button
                type="button"
                onClick={() => setNfModalOpen(false)}
                className="flex-1 bg-slate-100 text-slate-700 py-2 rounded-lg font-semibold text-sm"
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
          <div className="bg-white rounded-xl shadow-2xl border w-full max-w-sm overflow-hidden text-center p-6 space-y-4">
            <div className="mx-auto w-12 h-12 bg-amber-100 text-amber-600 rounded-full flex items-center justify-center border border-amber-200">
              <AlertTriangle size={26} />
            </div>
            <div>
              <h3 className="text-xl font-bold text-slate-800">
                Confirmar Recebimento?
              </h3>
              <p className="text-sm text-slate-500 mt-2">
                Isto dará baixa definitiva no faturamento e atualizará o fluxo
                de caixa corporativo.
              </p>
            </div>
            <div className="flex gap-3">
              <button
                onClick={confirmarBaixarPagamento}
                className="flex-1 bg-green-600 text-white py-2.5 rounded-lg font-semibold text-sm"
              >
                Sim, Dar Baixa!
              </button>
              <button
                onClick={() => setBaixaModalOpen(false)}
                className="flex-1 bg-slate-100 text-slate-700 py-2.5 rounded-lg font-semibold text-sm"
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
