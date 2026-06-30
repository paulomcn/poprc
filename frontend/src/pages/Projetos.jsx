import { useState, useEffect } from "react";
import { Plus, Eye, Edit, Save, X, Briefcase, Calendar } from "lucide-react";
import api from "../services/api";
import Alert from "../components/Alert";

export default function Projetos() {
  const [projetos, setProjetos] = useState([]);
  const [contratos, setContratos] = useState([]); // Armazena contratos para o Select
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedProjeto, setSelectedProjeto] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);

  // Estado do formulário batendo com o backend
  const [formData, setFormData] = useState({
    contrato: { id: "" },
    dataInicio: "",
    dataFim: "",
    status: "EM_ANDAMENTO",
    asBuiltStatus: "PENDENTE",
  });

  useEffect(() => {
    carregarDados();
  }, []);

  const carregarDados = async () => {
    try {
      setLoading(true);
      const [resProjetos, resContratos] = await Promise.all([
        api.get("/projetos"),
        api.get("/contratos"),
      ]);
      setProjetos(resProjetos.data);
      setContratos(resContratos.data);
    } catch (err) {
      console.error("Erro ao carregar dados", err);
      setErrorMessage("Falha ao sincronizar dados com o servidor.");
    } finally {
      setLoading(false);
    }
  };

  const handleOpenModal = (projeto = null, editMode = false) => {
    setSelectedProjeto(projeto);
    setIsEditing(editMode);
    setErrorMessage(null);

    if (projeto) {
      setFormData({
        contrato: { id: projeto.contrato?.id || "" },
        dataInicio: projeto.dataInicio || "",
        dataFim: projeto.dataFim || "",
        status: projeto.status || "EM_ANDAMENTO",
        asBuiltStatus: projeto.asBuiltStatus || "PENDENTE",
      });
    } else {
      setFormData({
        contrato: { id: contratos[0]?.id || "" },
        dataInicio: "",
        dataFim: "",
        status: "EM_ANDAMENTO",
        asBuiltStatus: "PENDENTE",
      });
    }
    setModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    try {
      if (!formData.contrato.id) {
        setErrorMessage("Selecione um contrato válido!");
        return;
      }

      if (isEditing && selectedProjeto) {
        await api.put(`/projetos/${selectedProjeto.id}`, formData);
      } else {
        await api.post("/projetos", formData);
      }
      setModalOpen(false);
      carregarDados();
    } catch (err) {
      console.error("Erro ao salvar projeto", err);
      setErrorMessage(
        "Erro ao salvar projeto. Verifique os campos ou logs do Java.",
      );
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-slate-800 flex items-center gap-2">
            <Briefcase size={32} /> Projetos
          </h1>
          <p className="text-slate-600 mt-1">
            Vincule e gerencie a linha do tempo dos projetos dos contratos
          </p>
        </div>
        <button
          onClick={() => handleOpenModal(null, false)}
          className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2.5 rounded-lg font-semibold flex items-center gap-2 shadow-sm transition-colors"
        >
          <Plus size={20} /> Novo Projeto
        </button>
      </div>

      {errorMessage && <Alert type="error" message={errorMessage} />}

      {/* Grid de Cards de Projetos */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {projetos.map((p) => (
          <div
            key={p.id}
            className="bg-white p-6 rounded-xl shadow-md border border-slate-200 flex flex-col justify-between space-y-4"
          >
            <div>
              <div className="flex justify-between items-start">
                <h3 className="text-lg font-bold text-slate-800">
                  Projeto #{p.id}
                </h3>
                <span
                  className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${p.status === "CONCLUIDO" ? "bg-green-100 text-green-700" : "bg-blue-100 text-blue-700"}`}
                >
                  {p.status?.replace("_", " ")}
                </span>
              </div>
              <div className="mt-3 space-y-1.5 text-sm text-slate-600">
                <p>
                  <strong>Contrato:</strong> {p.contrato?.contrato || "---"}
                </p>
                <p>
                  <strong>Início:</strong> {p.dataInicio || "---"}
                </p>
                <p>
                  <strong>Fim:</strong> {p.dataFim || "---"}
                </p>
                <p>
                  <strong>As-Built:</strong> {p.asBuiltStatus || "PENDENTE"}
                </p>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
              <button
                onClick={() => handleOpenModal(p, false)}
                className="p-2 text-slate-500 hover:bg-slate-100 rounded-lg flex items-center gap-1 text-xs font-semibold"
              >
                <Eye size={16} /> Detalhes
              </button>
              <button
                onClick={() => handleOpenModal(p, true)}
                className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg flex items-center gap-1 text-xs font-semibold"
              >
                <Edit size={16} /> Editar
              </button>
            </div>
          </div>
        ))}
      </div>

      {/*   MODAL UNIFICADO: DETALHES / NOVO / EDITAR */}
      {modalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-md overflow-hidden">
            <div className="p-6 bg-slate-900 text-white flex justify-between items-center">
              <h2 className="text-xl font-bold">
                {isEditing
                  ? " ️ Editar Projeto"
                  : selectedProjeto
                    ? "  Detalhes do Projeto"
                    : "  Novo Projeto"}
              </h2>
              <button
                onClick={() => setModalOpen(false)}
                className="text-slate-400 hover:text-white"
              >
                <X size={24} />
              </button>
            </div>

            <form onSubmit={handleSave} className="p-6 space-y-4">
              <div>
                <label className="text-xs font-bold text-slate-600 uppercase">
                  Contrato Vinculado *
                </label>
                <select
                  disabled={selectedProjeto && !isEditing}
                  value={formData.contrato.id}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      contrato: { id: e.target.value },
                    })
                  }
                  className="w-full mt-1 p-2 border rounded-lg text-sm bg-slate-50"
                  required
                >
                  <option value="">Selecione um contrato...</option>
                  {contratos.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.contrato} - {c.cliente}
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase">
                    Data Início *
                  </label>
                  <input
                    type="date"
                    disabled={selectedProjeto && !isEditing}
                    value={formData.dataInicio}
                    onChange={(e) =>
                      setFormData({ ...formData, dataInicio: e.target.value })
                    }
                    className="w-full mt-1 p-2 border rounded-lg text-sm bg-slate-50"
                    required
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase">
                    Data Fim
                  </label>
                  <input
                    type="date"
                    disabled={selectedProjeto && !isEditing}
                    value={formData.dataFim}
                    onChange={(e) =>
                      setFormData({ ...formData, dataFim: e.target.value })
                    }
                    className="w-full mt-1 p-2 border rounded-lg text-sm bg-slate-50"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase">
                    Status *
                  </label>
                  <select
                    disabled={selectedProjeto && !isEditing}
                    value={formData.status}
                    onChange={(e) =>
                      setFormData({ ...formData, status: e.target.value })
                    }
                    className="w-full mt-1 p-2 border rounded-lg text-sm bg-slate-50"
                  >
                    <option value="EM_ANDAMENTO">Em Andamento</option>
                    <option value="CONCLUIDO">Concluído</option>
                    <option value="SUSPENSO">Suspenso</option>
                  </select>
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase">
                    Status As-Built *
                  </label>
                  <select
                    disabled={selectedProjeto && !isEditing}
                    value={formData.asBuiltStatus}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        asBuiltStatus: e.target.value,
                      })
                    }
                    className="w-full mt-1 p-2 border rounded-lg text-sm bg-slate-50"
                  >
                    <option value="PENDENTE">Pendente</option>
                    <option value="EM_REVISAO">Em Revisão</option>
                    <option value="APROVADO">Aprovado</option>
                  </select>
                </div>
              </div>

              {(!selectedProjeto || isEditing) && (
                <div className="flex justify-end pt-4 border-t">
                  <button
                    type="submit"
                    className="bg-blue-600 text-white px-6 py-2 rounded-lg font-semibold flex items-center gap-2 hover:bg-blue-700 w-full justify-center"
                  >
                    <Save size={18} /> Salvar Projeto
                  </button>
                </div>
              )}
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
