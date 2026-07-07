import { useState, useEffect } from "react";
import {
  AlertTriangle,
  Edit2,
  MapPin,
  User,
  Zap,
  History,
  FileText,
  Upload,
  CheckCircle2,
  ChevronRight,
  PenTool,
} from "lucide-react";
import api from "../services/api";
import Modal from "../components/Modal";
import LoadingSpinner from "../components/LoadingSpinner";
import Alert from "../components/Alert";
import HistoricoAtividadesComarca from "../components/HistoricoAtividadesComarca";

export default function GestaoComarcas() {
  const [comarcas, setComarcas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [selectedComarca, setSelectedComarca] = useState(null);
  const [comarcaHistorico, setComarcaHistorico] = useState(null);

  // 💥 Controles reativos de validação para a Etapa 1 (Vistoria)
  const [fotoVistoria, setFotoVistoria] = useState(false);
  const [assinaturaValida, setAssinaturaValida] = useState(false);

  const [formData, setFormData] = useState({
    percentualConcluido: 0,
    pendencias: "",
  });

  useEffect(() => {
    fetchComarcas();
  }, []);

  const fetchComarcas = async () => {
    try {
      setLoading(true);
      const response = await api.get("/comarcas");
      setComarcas(response.data || []);
      setError(null);
    } catch (err) {
      setError("Erro ao carregar comarcas");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // 💥 REGRA DE NEGÓCIO: Bloqueia avanço se não houver foto e assinatura digital
  const handleAvancarFase = async (comarca) => {
    if (!fotoVistoria || !assinaturaValida) {
      alert(
        "🔒 BLOQUEIO OPERACIONAL: Upload de foto da vistoria e Assinatura com o Gerente são obrigatórios para liberar a infraestrutura!",
      );
      return;
    }

    try {
      await api.patch(`/comarcas/${comarca.id}`, { etapaAtual: 2 });
      alert(
        `🚀 Sucesso! Etapa 2 (Infraestrutura) desbloqueada para a comarca de ${comarca.nomeComarca}`,
      );
      fetchComarcas();
    } catch (err) {
      console.error("Erro ao transicionar etapa", err);
    }
  };

  const handleOpenModal = (comarca) => {
    setSelectedComarca(comarca);
    setFormData({
      percentualConcluido: comarca.percentualConcluido || 0,
      pendencias: comarca.pendencias || "",
    });
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setSelectedComarca(null);
    setFormData({ percentualConcluido: 0, pendencias: "" });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selectedComarca) return;

    try {
      await api.patch(`/comarcas/${selectedComarca.id}`, {
        percentualConcluido: parseFloat(formData.percentualConcluido),
        pendencias: formData.pendencias,
      });

      setError(null);
      handleCloseModal();
      fetchComarcas();
    } catch (err) {
      setError("Erro ao atualizar comarca");
      console.error(err);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const getProgressColor = (percentage) => {
    if (percentage >= 80) return "bg-green-500";
    if (percentage >= 50) return "bg-yellow-500";
    return "bg-red-500";
  };

  if (loading) return <LoadingSpinner />;
  if (error) return <Alert type="error" message={error} />;

  return (
    <div>
      <div className="mb-8">
        <div>
          <h1 className="text-3xl font-bold text-slate-800">
            Gestão de Comarcas
          </h1>
          <p className="text-slate-600 mt-2">
            Monitore o progresso, vistorias obrigatórias e liberação de
            infraestrutura regional.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {comarcas.map((comarca) => (
          <div
            key={comarca.id}
            className="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow border border-slate-200 overflow-hidden flex flex-col justify-between"
          >
            <div
              className={`p-6 flex-1 space-y-4 ${comarca.pendencias ? "bg-red-50 border-l-4 border-l-red-500" : "bg-white"}`}
            >
              {/* Topo do Card: Identificador Único da OS + Stepper Visual de Linha do Tempo */}
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <h3 className="text-xl font-bold text-slate-800">
                    {comarca.nomeComarca}
                  </h3>
                  <div className="mt-1 inline-flex items-center gap-1 text-xs font-mono font-bold text-blue-600 bg-blue-50 px-2 py-0.5 rounded border border-blue-100">
                    <FileText size={12} /> OS:{" "}
                    {comarca.ordemServico?.numeroOs || `OS-2026-0${comarca.id}`}
                  </div>
                </div>

                {/* Indicador de Passos / Status do Fluxo Linear */}
                <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-lg text-[10px] font-black uppercase tracking-wider">
                  <span
                    className={`px-2 py-0.5 rounded ${comarca.etapaAtual !== 2 ? "bg-amber-500 text-white shadow-sm" : "bg-slate-200 text-slate-500"}`}
                  >
                    1. Vistoria
                  </span>
                  <ChevronRight size={10} className="text-slate-400" />
                  <span
                    className={`px-2 py-0.5 rounded ${comarca.etapaAtual === 2 ? "bg-emerald-500 text-white shadow-sm" : "bg-slate-200 text-slate-500"}`}
                  >
                    2. Infra
                  </span>
                </div>
              </div>

              {/* Dados Estruturados de Campo */}
              <div className="space-y-2 text-sm text-slate-700">
                <div className="flex items-start gap-3">
                  <MapPin
                    size={18}
                    className="text-blue-500 mt-0.5 flex-shrink-0"
                  />
                  <div>
                    <p className="text-xs font-bold text-slate-400 uppercase">
                      Endereço
                    </p>
                    <p className="text-slate-800 font-medium">
                      {comarca.endereco}
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-3">
                  <User
                    size={18}
                    className="text-purple-500 mt-0.5 flex-shrink-0"
                  />
                  <div>
                    <p className="text-xs font-bold text-slate-400 uppercase">
                      Juiz Responsável
                    </p>
                    <p className="text-slate-800 font-medium">
                      {comarca.juizResponsavel}
                    </p>
                  </div>
                </div>

                {/* Painel de Previsão de Materiais / Insumos Planejados */}
                <div className="flex items-start gap-3 pt-1">
                  <Zap
                    size={18}
                    className="text-yellow-500 mt-0.5 flex-shrink-0"
                  />
                  <div>
                    <p className="text-xs font-bold text-slate-400 uppercase">
                      Painel de Previsão de Materiais
                    </p>
                    <p className="text-slate-800 font-semibold">
                      {comarca.quantidadePoints || 25} pontos previstos para
                      instalação física
                    </p>
                  </div>
                </div>
              </div>

              {/* 🛑 SEÇÃO DE VISTORIA COM GERENTE COM CARDS DE UPLOAD/ASSINATURA (ETAPA 1) */}
              {(!comarca.etapaAtual || comarca.etapaAtual === 1) && (
                <div className="bg-amber-50/40 border border-amber-200/70 rounded-xl p-3 grid grid-cols-2 gap-3 text-xs font-bold tracking-wide">
                  <div
                    onClick={() => setFotoVistoria(true)}
                    className={`border border-dashed p-3 rounded-lg text-center cursor-pointer transition flex items-center justify-center gap-1 ${fotoVistoria ? "bg-emerald-50 border-emerald-300 text-emerald-800" : "bg-white hover:border-blue-400 text-slate-500"}`}
                  >
                    <Upload size={14} />{" "}
                    {fotoVistoria ? "Foto Carregada" : "Fazer Upload Foto"}
                  </div>
                  <div
                    onClick={() => setAssinaturaValida(true)}
                    className={`border border-dashed p-3 rounded-lg text-center cursor-pointer transition flex items-center justify-center gap-1 ${assinaturaValida ? "bg-emerald-50 border-emerald-300 text-emerald-800" : "bg-white hover:border-blue-400 text-slate-500"}`}
                  >
                    <PenTool size={14} />{" "}
                    {assinaturaValida ? "Termo Assinado" : "Coletar Assinatura"}
                  </div>
                </div>
              )}

              {/* Barra de Progresso Operacional */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <p className="text-xs font-bold text-slate-400 uppercase">
                    Progresso de Conclusão
                  </p>
                  <span className="text-sm font-bold text-slate-700">
                    {comarca.percentualConcluido || 0}%
                  </span>
                </div>
                <div className="w-full bg-slate-200 rounded-full h-3 overflow-hidden">
                  <div
                    className={`h-full transition-all duration-300 ${getProgressColor(comarca.percentualConcluido || 0)}`}
                    style={{
                      width: `${Math.min(comarca.percentualConcluido || 0, 100)}%`,
                    }}
                  />
                </div>
              </div>

              {comarca.pendencias && (
                <div className="bg-red-100 border border-red-300 rounded-lg p-3 text-xs text-red-800 flex items-center gap-2">
                  <AlertTriangle size={14} className="flex-shrink-0" />
                  <p>
                    <span className="font-bold">Pendência:</span>{" "}
                    {comarca.pendencias}
                  </p>
                </div>
              )}
            </div>

            {/* Ações dinâmicas com trava de avanço baseada na etapa */}
            <div className="bg-slate-50 px-6 py-3 border-t border-slate-200 flex flex-col sm:flex-row gap-2">
              {!comarca.etapaAtual || comarca.etapaAtual === 1 ? (
                <button
                  onClick={() => handleAvancarFase(comarca)}
                  className="flex-1 bg-amber-600 hover:bg-amber-700 text-white font-bold py-2 px-4 rounded-lg text-xs transition-colors shadow-sm uppercase tracking-wider"
                >
                  Homologar Vistoria e Liberar Obras
                </button>
              ) : (
                <button
                  onClick={() => handleOpenModal(comarca)}
                  className="flex-1 bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-4 rounded-lg text-xs transition-colors shadow-sm"
                >
                  Registrar Pendência/Ajustar Progresso
                </button>
              )}
              <button
                onClick={() => setComarcaHistorico(comarca)}
                className="flex-shrink-0 flex items-center justify-center gap-1 bg-white hover:bg-slate-100 border text-slate-700 font-bold py-2 px-4 rounded-lg text-xs transition-colors"
              >
                <History size={14} />
                Histórico
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Modal Ajustar Progresso */}
      <Modal
        isOpen={showModal}
        onClose={handleCloseModal}
        title={`Ajustar Progresso - ${selectedComarca?.nomeComarca}`}
      >
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              Percentual de Conclusão (%)
            </label>
            <div className="flex items-center gap-4">
              <input
                type="number"
                name="percentualConcluido"
                value={formData.percentualConcluido}
                onChange={handleInputChange}
                min="0"
                max="100"
                className="flex-1 px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <span className="text-slate-700 font-semibold">%</span>
            </div>
          </div>
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              Pendências/Observações
            </label>
            <textarea
              name="pendencias"
              value={formData.pendencias}
              onChange={handleInputChange}
              rows="4"
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
              placeholder="Descreva qualquer detalhe em campo..."
            />
          </div>
          <div className="flex gap-3 justify-end">
            <button
              type="button"
              onClick={handleCloseModal}
              className="px-5 py-2 border rounded-lg text-slate-700 font-semibold hover:bg-slate-50 transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg transition-colors"
            >
              Salvar Alterações
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal Histórico */}
      <Modal
        isOpen={!!comarcaHistorico}
        onClose={() => setComarcaHistorico(null)}
        title={`Atividades - ${comarcaHistorico?.nomeComarca || ""}`}
      >
        {comarcaHistorico && (
          <HistoricoAtividadesComarca comarcaId={comarcaHistorico.id} />
        )}
      </Modal>
    </div>
  );
}
