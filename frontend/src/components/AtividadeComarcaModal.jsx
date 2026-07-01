import { useState } from "react";
import { X, Save, AlertCircle } from "lucide-react";
import api from "../services/api";
import EditorTextoRico from "./EditorTextoRico";
import UploadFotosEvidencia from "./UploadFotosEvidencia";
import GaleriaFotos from "./GaleriaFotos";

export default function AtividadeComarcaModal({
  comarcaId,
  atividadeExistente = null,
  onClose,
  onSalvo,
}) {
  const [dataInicio, setDataInicio] = useState(
    atividadeExistente?.dataInicio || "",
  );
  const [dataEncerramento, setDataEncerramento] = useState(
    atividadeExistente?.dataEncerramento || "",
  );
  const [descricaoAtividades, setDescricaoAtividades] = useState(
    atividadeExistente?.descricaoAtividades || "",
  );
  const [fotos, setFotos] = useState([]);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState(null);

  const emEdicao = !!atividadeExistente;

  const descricaoVazia = () => {
    const textoSemHtml = descricaoAtividades.replace(/[<>]/g, "").trim();
    return textoSemHtml.length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErro(null);

    if (!dataInicio) {
      setErro("Informe a Data de Início da atividade.");
      return;
    }

    if (descricaoVazia()) {
      setErro(
        "A Descrição das Atividades é obrigatória e não pode ficar vazia.",
      );
      return;
    }

    setSalvando(true);
    try {
      const formData = new FormData();
      formData.append("dataInicio", dataInicio);
      if (dataEncerramento)
        formData.append("dataEncerramento", dataEncerramento);
      formData.append("descricaoAtividades", descricaoAtividades);

      const campoFotos = emEdicao ? "novasFotos" : "fotos";
      fotos.forEach((foto) => formData.append(campoFotos, foto));

      const response = emEdicao
        ? await api.put(
            `/comarcas/atividades/${atividadeExistente.id}`,
            formData,
            {
              headers: { "Content-Type": "multipart/form-data" },
            },
          )
        : await api.post(`/comarcas/${comarcaId}/atividades`, formData, {
            headers: { "Content-Type": "multipart/form-data" },
          });

      onSalvo(response.data);
    } catch (err) {
      setErro(
        err.response?.data?.erro || "Erro ao salvar atividade no servidor.",
      );
      console.error(err);
    } finally {
      setSalvando(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-6 border-b border-slate-200 sticky top-0 bg-white">
          <h2 className="text-xl font-bold text-slate-800">
            {emEdicao
              ? "Editar Atividade da Comarca"
              : "Registrar Atividade na Comarca"}
          </h2>
          <button
            onClick={onClose}
            className="p-1 hover:bg-slate-100 rounded-lg"
            disabled={salvando}
          >
            <X size={22} className="text-slate-500" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {erro && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-center gap-2 text-sm">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              {erro}
            </div>
          )}

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1">
                Data de Início *
              </label>
              <input
                type="date"
                value={dataInicio}
                onChange={(e) => setDataInicio(e.target.value)}
                required
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1">
                Data de Encerramento
              </label>
              <input
                type="date"
                value={dataEncerramento}
                onChange={(e) => setDataEncerramento(e.target.value)}
                min={dataInicio || undefined}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <p className="text-xs text-slate-400 mt-1">
                Deixe em branco se ainda estiver em andamento
              </p>
            </div>
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              Descrição das Atividades *
            </label>
            <EditorTextoRico
              value={descricaoAtividades}
              onChange={setDescricaoAtividades}
              placeholder="Descreva detalhadamente o que foi realizado na comarca..."
            />
          </div>

          {emEdicao && atividadeExistente?.fotosEvidencia?.length > 0 && (
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">
                Evidências já enviadas
              </label>
              <GaleriaFotos fotos={atividadeExistente.fotosEvidencia} />
            </div>
          )}

          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              {emEdicao
                ? "Adicionar novas evidências"
                : "Evidências Fotográficas"}
            </label>
            <UploadFotosEvidencia arquivos={fotos} onChange={setFotos} />
          </div>

          <div className="flex gap-3 justify-end pt-4 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              disabled={salvando}
              className="px-6 py-2 border border-slate-300 text-slate-700 font-semibold rounded-lg hover:bg-slate-50 transition disabled:opacity-50"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={salvando}
              className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg transition flex items-center gap-2 disabled:opacity-50"
            >
              <Save size={18} />
              {salvando ? "Salvando..." : "Salvar Atividade"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
