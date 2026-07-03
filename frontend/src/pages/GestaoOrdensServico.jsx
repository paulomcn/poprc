import React, { useState, useEffect } from "react";
import {
  CheckCircle,
  AlertCircle,
  Clock,
  Loader,
  Plus,
  Search,
  ChevronDown,
  X,
  Briefcase,
  FileText,
} from "lucide-react";
import api from "../services/api";
import OrdensServicoCard from "../components/OrdensServicoCard";
import StatusModal from "../components/StatusModal";

const STATUS_COLUMNS = [
  {
    value: "ABERTA",
    label: "Aberta",
    color: "bg-blue-50",
    borderColor: "border-blue-200",
  },
  {
    value: "EM_EXECUCAO",
    label: "Em Execução",
    color: "bg-yellow-50",
    borderColor: "border-yellow-200",
  },
  {
    value: "AGUARDANDO_VALIDACAO",
    label: "Aguardando Validação",
    color: "bg-purple-50",
    borderColor: "border-purple-200",
  },
  {
    value: "CONCLUIDA",
    label: "Concluída",
    color: "bg-green-50",
    borderColor: "border-green-200",
  },
  {
    value: "FATURADA",
    label: "Faturada",
    color: "bg-gray-50",
    borderColor: "border-gray-200",
  },
];

export default function GestaoOrdensServico() {
  const [ordensServico, setOrdensServico] = useState([]);
  const [projetos, setProjetos] = useState([]); // 💥 Trocado de contratos para projetos!
  const [filteredOrdens, setFilteredOrdens] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filterCliente, setFilterCliente] = useState("");
  const [filterNumeroOS, setFilterNumeroOS] = useState("");
  const [selectedOrdem, setSelectedOrdem] = useState(null);
  const [showStatusModal, setShowStatusModal] = useState(false);

  // Modal e formulário adaptados para o vínculo duplo 💥
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [formData, setFormData] = useState({
    numeroOs: "",
    descricao: "",
    status: "ABERTA",
    projeto: { id: "" }, // 💥 Link direto com a engenharia
    contrato: { id: "" }, // 💥 Link direto com o faturamento
  });

  useEffect(() => {
    carregarDadosIniciais();
  }, []);

  useEffect(() => {
    aplicarFiltros();
  }, [ordensServico, filterCliente, filterNumeroOS]);

  const carregarDadosIniciais = async () => {
    setLoading(true);
    setError(null);
    try {
      const [resOS, resProjetos] = await Promise.all([
        api.get("/ordens-servico"),
        api.get("/projetos"), // 💥 Puxando os projetos reais do Postgres
      ]);
      setOrdensServico(resOS.data || []);
      setProjetos(resProjetos.data || []);
    } catch (err) {
      setError("Erro ao sincronizar ecossistema de Ordens de Serviço.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const aplicarFiltros = () => {
    let resultado = ordensServico;

    if (filterNumeroOS.trim()) {
      resultado = resultado.filter((ordem) =>
        ordem.numeroOs?.toLowerCase().includes(filterNumeroOS.toLowerCase()),
      );
    }

    if (filterCliente.trim()) {
      resultado = resultado.filter((ordem) =>
        ordem.contrato?.cliente
          ?.toLowerCase()
          .includes(filterCliente.toLowerCase()),
      );
    }

    setFilteredOrdens(resultado);
  };

  const handleCriarOS = async (e) => {
    e.preventDefault();
    if (
      !formData.numeroOs.trim() ||
      !formData.projeto.id ||
      !formData.contrato.id
    ) {
      alert("Por favor, selecione um projeto operacional válido.");
      return;
    }

    try {
      await api.post("/ordens-servico", formData);
      setCreateModalOpen(false);
      const response = await api.get("/ordens-servico");
      setOrdensServico(response.data || []);
    } catch (err) {
      console.error(err);
      alert("Erro ao salvar ordem de serviço vinculada no banco.");
    }
  };

  const abrirModalStatus = (ordem) => {
    setSelectedOrdem(ordem);
    setShowStatusModal(true);
  };

  const fecharModalStatus = () => {
    setShowStatusModal(false);
    setSelectedOrdem(null);
  };

  const handleStatusUpdated = (ordemAtualizada) => {
    setOrdensServico((prevOrdens) =>
      prevOrdens.map((ordem) =>
        ordem.id === ordemAtualizada.id ? ordemAtualizada : ordem,
      ),
    );
    fecharModalStatus();
  };

  const agruparPorStatus = () => {
    const agrupado = {};
    STATUS_COLUMNS.forEach((col) => {
      agrupado[col.value] = filteredOrdens.filter(
        (ordem) => (ordem.status || "ABERTA") === col.value,
      );
    });
    return agrupado;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen bg-gray-50">
        <div className="text-center">
          <Loader className="w-12 h-12 text-blue-600 animate-spin mx-auto mb-4" />
          <p className="text-gray-600 font-medium">
            Sincronizando fluxo com o Postgres...
          </p>
        </div>
      </div>
    );
  }

  const ordensPorStatus = agruparPorStatus();

  return (
    <div className="p-8 bg-gray-50 min-h-screen">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-black text-gray-900 mb-6 tracking-tight">
          Gestão de Ordens de Serviço
        </h1>

        {/* Filtros e Ações */}
        <div className="bg-white rounded-xl shadow-sm p-6 mb-6 border border-gray-100">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="relative">
              <Search className="absolute left-3 top-3 text-gray-400 w-5 h-5" />
              <input
                type="text"
                placeholder="Filtrar por nº OS..."
                value={filterNumeroOS}
                onChange={(e) => setFilterNumeroOS(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
              />
            </div>

            <div className="relative">
              <Search className="absolute left-3 top-3 text-gray-400 w-5 h-5" />
              <input
                type="text"
                placeholder="Filtrar por cliente..."
                value={filterCliente}
                onChange={(e) => setFilterCliente(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
              />
            </div>

            <button
              onClick={() => {
                const pInicial = projetos[0];
                setFormData({
                  numeroOs: "",
                  descricao: "",
                  status: "ABERTA",
                  projeto: { id: pInicial?.id || "" },
                  contrato: { id: pInicial?.contrato?.id || "" },
                });
                setCreateModalOpen(true);
              }}
              className="bg-blue-600 text-white px-6 py-2 rounded-lg font-bold hover:bg-blue-700 transition flex items-center justify-center gap-2 text-sm shadow-sm"
            >
              <Plus className="w-4 h-4" /> Nova OS
            </button>
          </div>
        </div>
      </div>

      {/* Kanban Board */}
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
        {STATUS_COLUMNS.map((coluna) => (
          <div key={coluna.value} className="space-y-4">
            <div
              className={`${coluna.color} border-2 ${coluna.borderColor} rounded-xl p-4 shadow-sm`}
            >
              <div className="flex items-center justify-between mb-2">
                <h2 className="font-bold text-gray-800 text-sm tracking-wide">
                  {coluna.label}
                </h2>
                <span className="bg-gray-200/80 text-gray-700 text-xs font-bold px-2 py-0.5 rounded-full">
                  {ordensPorStatus[coluna.value]?.length || 0}
                </span>
              </div>
              <div className="w-full h-1 bg-gray-200 rounded-full" />
            </div>

            <div className="space-y-3 min-h-[500px] bg-gray-100/40 p-2 rounded-xl border border-dashed border-gray-200">
              {ordensPorStatus[coluna.value]?.length > 0 ? (
                ordensPorStatus[coluna.value].map((ordem) => (
                  <OrdensServicoCard
                    key={ordem.id}
                    ordem={ordem}
                    onAtualizarStatus={() => abrirModalStatus(ordem)}
                  />
                ))
              ) : (
                <div className="text-center py-12 text-gray-400">
                  <p className="text-xs font-medium">Sem chamados ativos</p>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* 🛑 MODAL CORRIGIDO: SELEÇÃO OPERACIONAL POR PROJETO/COMARCA */}
      {createModalOpen && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-xl border w-full max-w-md overflow-hidden animate-fade-in">
            <div className="p-5 border-b border-gray-100 flex justify-between items-center bg-gray-50">
              <h2 className="text-base font-bold text-gray-800 flex items-center gap-2">
                <FileText size={18} className="text-blue-600" /> Abrir Nova
                Ordem de Serviço
              </h2>
              <button
                onClick={() => setCreateModalOpen(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleCriarOS} className="p-6 space-y-4">
              <div>
                <label className="text-xs font-bold text-gray-500 uppercase tracking-wider">
                  Código / Número da OS *
                </label>
                <input
                  type="text"
                  required
                  value={formData.numeroOs}
                  onChange={(e) =>
                    setFormData({ ...formData, numeroOs: e.target.value })
                  }
                  className="w-full mt-1 p-2.5 border border-gray-300 rounded-lg text-sm bg-gray-50 outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Ex: OS-2026-991"
                />
              </div>

              {/* 💥 SELETOR EVOLUÍDO: MAPEIA PROJETO, COMARCA E CONTRATO SIMULTANEAMENTE */}
              <div>
                <label className="text-xs font-bold text-gray-500 uppercase tracking-wider">
                  Projeto / Comarca Alvo *
                </label>
                <select
                  required
                  value={formData.projeto.id}
                  onChange={(e) => {
                    const projId = e.target.value;
                    const projSelecionado = projetos.find(
                      (p) => p.id == projId,
                    );
                    setFormData({
                      ...formData,
                      projeto: { id: projId },
                      contrato: { id: projSelecionado?.contrato?.id || "" },
                    });
                  }}
                  className="w-full mt-1 p-2.5 border border-gray-300 rounded-lg text-sm bg-gray-50 outline-none focus:ring-2 focus:ring-blue-500"
                >
                  {projetos.map((p) => (
                    <option key={p.id} value={p.id}>
                      Projeto #{p.id} - {p.nomeComarcaVinculada || "Infra"} (
                      {p.contrato?.cliente})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="text-xs font-bold text-gray-500 uppercase tracking-wider">
                  Descrição do Chamado Técnico *
                </label>
                <textarea
                  rows="3"
                  required
                  value={formData.descricao}
                  onChange={(e) =>
                    setFormData({ ...formData, descricao: e.target.value })
                  }
                  className="w-full mt-1 p-2.5 border border-gray-300 rounded-lg text-sm bg-gray-50 outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Descreva o escopo da manutenção ou instalação técnica..."
                ></textarea>
              </div>
              <button
                type="submit"
                className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2.5 rounded-lg font-bold text-sm transition-all shadow-sm"
              >
                Emitir Ordem de Serviço
              </button>
            </form>
          </div>
        </div>
      )}

      {/* Modal de Status */}
      {showStatusModal && selectedOrdem && (
        <StatusModal
          ordem={selectedOrdem}
          onClose={fecharModalStatus}
          onStatusAtualizado={handleStatusUpdated}
        />
      )}
    </div>
  );
}
