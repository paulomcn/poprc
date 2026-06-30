import { useState, useEffect } from "react";
import { Plus, Users } from "lucide-react";
import api from "../services/api";
import Modal from "../components/Modal";
import LoadingSpinner from "../components/LoadingSpinner";
import Alert from "../components/Alert";

export default function Funcionarios() {
  const [funcionarios, setFuncionarios] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showModal, setShowModal] = useState(false);

  //   ESTADOS NOVOS PARA CONTROLAR A EDIÇÃO
  const [isEditing, setIsEditing] = useState(false);
  const [currentId, setCurrentId] = useState(null);

  // Ajustado para string para facilitar o split na hora de salvar
  const [formData, setFormData] = useState({
    nome: "",
    funcao: "",
    cidade: "",
    certificacoes: "",
    documentPaths: "",
  });

  useEffect(() => {
    fetchFuncionarios();
  }, []);

  const fetchFuncionarios = async () => {
    try {
      setLoading(true);
      const response = await api.get("/funcionarios");
      setFuncionarios(response.data);
      setError(null);
    } catch (err) {
      setError("Erro ao carregar funcionários");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  //   FUNÇÃO NOVA: Abre o modal limpo para CRIAR
  const handleNew = () => {
    setIsEditing(false);
    setCurrentId(null);
    setFormData({
      nome: "",
      funcao: "",
      cidade: "",
      certificacoes: "",
      documentPaths: "",
    });
    setShowModal(true);
  };

  //   FUNÇÃO NOVA: Abre o modal preenchido para EDITAR
  const handleEdit = (funcionario) => {
    setIsEditing(true);
    setCurrentId(funcionario.id);
    setFormData({
      nome: funcionario.nome || "",
      funcao: funcionario.funcao || "",
      cidade: funcionario.cidade || "",
      // Junta as arrays em string separada por vírgula para exibir no input
      certificacoes: funcionario.certificacoes
        ? funcionario.certificacoes.join(", ")
        : "",
      documentPaths: funcionario.documentPaths
        ? funcionario.documentPaths.join(", ")
        : "",
    });
    setShowModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        nome: formData.nome,
        funcao: formData.funcao,
        cidade: formData.cidade,
        // Converte as strings de volta para array antes de mandar pro Java
        certificacoes: formData.certificacoes
          ? String(formData.certificacoes)
              .split(",")
              .map((c) => c.trim())
              .filter(Boolean)
          : [],
        documentPaths: formData.documentPaths
          ? String(formData.documentPaths)
              .split(",")
              .map((p) => p.trim())
              .filter(Boolean)
          : [],
      };

      //   MÁGICA ACONTECENDO AQUI: Se for edição, faz PUT. Se for novo, faz POST.
      if (isEditing) {
        await api.put(`/funcionarios/${currentId}`, payload);
      } else {
        await api.post("/funcionarios", payload);
      }

      setShowModal(false);
      fetchFuncionarios();
    } catch (err) {
      setError("Erro ao salvar funcionário");
      console.error(err);
    }
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-slate-800">Funcionários</h1>
          <p className="text-slate-600 mt-2">
            Gerenciamento de colaboradores e RH
          </p>
        </div>
        <button
          onClick={handleNew}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus size={20} />
          Novo Funcionário
        </button>
      </div>

      {error && (
        <Alert type="error" message={error} onClose={() => setError(null)} />
      )}

      {loading ? (
        <LoadingSpinner />
      ) : funcionarios.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <Users size={48} className="mx-auto text-slate-400 mb-4" />
          <p className="text-slate-600">Nenhum funcionário cadastrado</p>
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="w-full">
            <thead className="bg-slate-50 border-b border-slate-200">
              <tr>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-700">
                  Nome
                </th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-700">
                  Função
                </th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-700">
                  Cidade
                </th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-700">
                  Certificações
                </th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-700">
                  Ações
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {funcionarios.map((funcionario) => (
                <tr
                  key={funcionario.id}
                  className="hover:bg-slate-50 transition-colors"
                >
                  <td className="px-6 py-4 text-sm text-slate-800 font-medium">
                    {funcionario.nome}
                  </td>
                  <td className="px-6 py-4 text-sm text-slate-600">
                    {funcionario.funcao}
                  </td>
                  <td className="px-6 py-4 text-sm text-slate-600">
                    {funcionario.cidade}
                  </td>
                  <td className="px-6 py-4 text-sm text-slate-600">
                    {funcionario.certificacoes &&
                    funcionario.certificacoes.length > 0
                      ? funcionario.certificacoes.join(", ")
                      : "-"}
                  </td>
                  <td className="px-6 py-4 text-sm">
                    {/*   AGORA O BOTÃO DE EDITAR CHAMA A FUNÇÃO */}
                    <button
                      onClick={() => handleEdit(funcionario)}
                      className="text-blue-600 hover:text-blue-800 font-medium"
                    >
                      Editar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/*   O TÍTULO DO MODAL MUDA DINAMICAMENTE */}
      <Modal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        title={isEditing ? "Editar Funcionário" : "Novo Funcionário"}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Nome *
            </label>
            <input
              type="text"
              required
              value={formData.nome}
              onChange={(e) =>
                setFormData((prev) => ({ ...prev, nome: e.target.value }))
              }
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Nome completo"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Função *
            </label>
            <input
              type="text"
              required
              value={formData.funcao}
              onChange={(e) =>
                setFormData((prev) => ({ ...prev, funcao: e.target.value }))
              }
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Ex: Engenheiro de Redes"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Cidade *
            </label>
            <input
              type="text"
              required
              value={formData.cidade}
              onChange={(e) =>
                setFormData((prev) => ({ ...prev, cidade: e.target.value }))
              }
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Cidade"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Certificações
            </label>
            <input
              type="text"
              value={formData.certificacoes}
              onChange={(e) =>
                setFormData((prev) => ({
                  ...prev,
                  certificacoes: e.target.value,
                }))
              }
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Separadas por vírgula (ex: CCNA, MCSA)"
            />
            <p className="text-xs text-slate-500 mt-1">
              Separe as certificações com vírgula
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Caminhos de Documentação
            </label>
            <input
              type="text"
              value={formData.documentPaths}
              onChange={(e) =>
                setFormData((prev) => ({
                  ...prev,
                  documentPaths: e.target.value,
                }))
              }
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Separados por vírgula (ex: /docs/cv, /docs/cert)"
            />
            <p className="text-xs text-slate-500 mt-1">
              Caminhos dos arquivos de documentação
            </p>
          </div>

          <div className="flex gap-3 pt-4">
            <button
              type="submit"
              className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              Salvar
            </button>
            <button
              type="button"
              onClick={() => setShowModal(false)}
              className="flex-1 px-4 py-2 bg-slate-200 text-slate-800 rounded-lg hover:bg-slate-300 transition-colors"
            >
              Cancelar
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
