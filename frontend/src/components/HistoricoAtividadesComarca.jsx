import { useState, useEffect } from "react";
import { Calendar, Plus, History as HistoryIcon, Edit2 } from "lucide-react";
import api from "../services/api";
import LoadingSpinner from "./LoadingSpinner";
import GaleriaFotos from "./GaleriaFotos";
import AtividadeComarcaModal from "./AtividadeComarcaModal";

export default function HistoricoAtividadesComarca({ comarcaId }) {
  const [atividades, setAtividades] = useState([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [atividadeEmEdicao, setAtividadeEmEdicao] = useState(null);

  const carregarAtividades = async () => {
    try {
      setLoading(true);
      const response = await api.get(`/comarcas/${comarcaId}/atividades`);
      setAtividades(response.data);
      setErro(null);
    } catch (err) {
      setErro("Erro ao carregar o histórico de atividades.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregarAtividades();
  }, [comarcaId]);

  const formatarData = (data) =>
    data ? new Date(data + "T00:00:00").toLocaleDateString("pt-BR") : null;

  const handleAtividadeSalva = () => {
    setShowModal(false);
    setAtividadeEmEdicao(null);
    carregarAtividades();
  };

  const abrirNova = () => {
    setAtividadeEmEdicao(null);
    setShowModal(true);
  };

  const abrirEdicao = (atividade) => {
    setAtividadeEmEdicao(atividade);
    setShowModal(true);
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-bold text-slate-700 flex items-center gap-2">
          <HistoryIcon size={16} /> Histórico de Atividades
        </h3>
        <button
          onClick={abrirNova}
          className="flex items-center gap-1.5 text-xs font-semibold bg-blue-600 hover:bg-blue-700 text-white px-3 py-1.5 rounded-lg transition"
        >
          <Plus size={14} /> Nova Atividade
        </button>
      </div>

      {loading ? (
        <LoadingSpinner />
      ) : erro ? (
        <p className="text-sm text-red-600">{erro}</p>
      ) : atividades.length === 0 ? (
        <p className="text-sm text-slate-400 italic py-4 text-center">
          Nenhuma atividade registrada para esta comarca ainda.
        </p>
      ) : (
        <div className="space-y-4 max-h-[50vh] overflow-y-auto pr-1">
          {atividades.map((atividade) => (
            <div
              key={atividade.id}
              className="border border-slate-200 rounded-lg p-4 bg-slate-50/50"
            >
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2 text-xs font-semibold text-slate-500">
                  <Calendar size={14} />
                  <span>{formatarData(atividade.dataInicio)}</span>
                  <span>—</span>
                  <span>
                    {formatarData(atividade.dataEncerramento) || "Em andamento"}
                  </span>
                </div>
                <button
                  onClick={() => abrirEdicao(atividade)}
                  className="text-xs text-blue-600 hover:text-blue-800 font-semibold flex items-center gap-1"
                >
                  <Edit2 size={12} /> Editar
                </button>
              </div>
              <div
                className="text-sm text-slate-700 max-w-none mb-3 [&_ul]:list-disc [&_ul]:pl-5"
                dangerouslySetInnerHTML={{
                  __html: atividade.descricaoAtividades,
                }}
              />
              <GaleriaFotos fotos={atividade.fotosEvidencia} />
            </div>
          ))}
        </div>
      )}

      {showModal && (
        <AtividadeComarcaModal
          comarcaId={comarcaId}
          atividadeExistente={atividadeEmEdicao}
          onClose={() => {
            setShowModal(false);
            setAtividadeEmEdicao(null);
          }}
          onSalvo={handleAtividadeSalva}
        />
      )}
    </div>
  );
}
