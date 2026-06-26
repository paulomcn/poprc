import { useState, useEffect } from "react";
import { Plus, Eye, Edit, Save, X, FileText } from "lucide-react";
import api from "../services/api";

export default function Contratos() {
  const [contratos, setContratos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedContrato, setSelectedContrato] = useState(null);
  const [isEditing, setIsEditing] = useState(false);

  // Estado do formulário com TODOS os campos exigidos
  const [formData, setFormData] = useState({
    cliente: "",
    contrato: "",
    vigenciaInicio: "",
    vigenciaFim: "",
    valorGlobal: "",
    status: "ATIVO",
    escopo: "",
  });

  useEffect(() => {
    carregarContratos();
  }, []);

  const carregarContratos = async () => {
    try {
      setLoading(true);
      const res = await api.get("/contratos");
      setContratos(res.data);
    } catch (err) {
      console.error("Erro ao carregar contratos", err);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenModal = (contrato = null, editMode = false) => {
    setSelectedContrato(contrato);
    setIsEditing(editMode);
    if (contrato) {
      setFormData({
        cliente: contrato.cliente || "",
        contrato: contrato.contrato || "",
        vigenciaInicio: contrato.vigenciaInicio || "",
        vigenciaFim: contrato.vigenciaFim || "",
        valorGlobal: contrato.valorGlobal || "",
        status: contrato.status || "ATIVO",
        escopo: contrato.escopo || "",
      });
    } else {
      setFormData({
        cliente: "",
        contrato: "",
        vigenciaInicio: "",
        vigenciaFim: "",
        valorGlobal: "",
        status: "ATIVO",
        escopo: "",
      });
    }
    setModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    try {
      if (isEditing && selectedContrato) {
        await api.put(`/contratos/${selectedContrato.id}`, formData);
      } else {
        await api.post("/contratos", formData);
      }
      setModalOpen(false);
      carregarContratos();
    } catch (err) {
      console.error("Erro ao salvar contrato", err);
    }
  };

  const formatCurrency = (value) => {
    if (!value) return "---";
    return new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(value);
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-slate-800 flex items-center gap-2">
            <FileText size={32} /> Contratos
          </h1>
          <p className="text-slate-600 mt-1">
            Gerenciamento completo de contratos e vigências
          </p>
        </div>
        <button
          onClick={() => handleOpenModal(null, false)}
          className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2.5 rounded-lg font-semibold flex items-center gap-2 shadow-sm transition-colors"
        >
          <Plus size={20} /> Novo Contrato
        </button>
      </div>

      {/* Tabela de Contratos */}
      <div className="bg-white rounded-xl shadow-md border border-slate-200 overflow-hidden">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-500 uppercase">
              <th className="p-4">Cliente</th>
              <th className="p-4">Número</th>
              <th className="p-4">Vigência</th>
              <th className="p-4">Valor Global</th>
              <th className="p-4">Status</th>
              <th className="p-4 text-center">Ações</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 text-sm text-slate-700">
            {contratos.map((c) => (
              <tr key={c.id} className="hover:bg-slate-50/80 transition-colors">
                <td className="p-4 font-semibold text-slate-800">
                  {c.cliente}
                </td>
                <td className="p-4 font-mono text-xs">{c.contrato}</td>
                <td className="p-4">
                  {c.vigenciaInicio
                    ? `${c.vigenciaInicio} até ${c.vigenciaFim || "---"}`
                    : "---"}
                </td>
                <td className="p-4 font-medium">
                  {formatCurrency(c.valorGlobal)}
                </td>
                <td className="p-4">
                  <span
                    className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${c.status === "ATIVO" ? "bg-green-100 text-green-700" : "bg-slate-100 text-slate-600"}`}
                  >
                    {c.status || "ATIVO"}
                  </span>
                </td>
                <td className="p-4 flex justify-center gap-2">
                  <button
                    onClick={() => handleOpenModal(c, false)}
                    className="p-2 text-slate-500 hover:bg-slate-100 rounded-lg"
                    title="Ver Detalhes"
                  >
                    <Eye size={18} />
                  </button>
                  <button
                    onClick={() => handleOpenModal(c, true)}
                    className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg"
                    title="Editar"
                  >
                    <Edit size={18} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* 🛑 MODAL UNIFICADO: NOVO / EDITAR / VISUALIZAR */}
      {modalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4 animate-fade-in">
          <div className="bg-white rounded-xl shadow-xl border w-full max-w-2xl overflow-hidden flex flex-col">
            <div className="p-6 bg-slate-900 text-white flex justify-between items-center">
              <h2 className="text-xl font-bold">
                {isEditing
                  ? "✏️ Editar Contrato"
                  : selectedContrato
                    ? "🔍 Detalhes do Contrato"
                    : "✨ Novo Contrato"}
              </h2>
              <button
                onClick={() => setModalOpen(false)}
                className="text-slate-400 hover:text-white"
              >
                <X size={24} />
              </button>
            </div>

            <form
              onSubmit={handleSave}
              className="p-6 space-y-4 overflow-y-auto max-h-[75vh]"
            >
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase">
                    Cliente
                  </label>
                  <input
                    type="text"
                    disabled={selectedContrato && !isEditing}
                    value={formData.cliente}
                    onChange={(e) =>
                      setFormData({ ...formData, cliente: e.target.value })
                    }
                    className="w-full mt-1 p-2 border rounded-lg text-sm bg-slate-50 disabled:opacity-70"
                    required
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase">
                    Número do Contrato
                  </label>
                  <input
                    type="text"
                    disabled={selectedContrato && !isEditing}
                    value={formData.contrato}
                    onChange={(e) =>
                      setFormData({ ...formData, contrato: e.target.value })
                    }
                    className="w-full mt-1 p-2 border rounded-lg text-sm bg-slate-50 disabled:opacity-70"
                    required
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase">
                    Vigência Início
                  </label>
                  <input
                    type="date"
                    disabled={selectedContrato && !isEditing}
                    value={formData.vigenciaInicio}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        vigenciaInicio: e.target.value,
                      })
                    }
                    className="w-full mt-1 p-2 border rounded-lg text-sm bg-slate-50 disabled:opacity-70"
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase">
                    Vigência Fim
                  </label>
                  <input
                    type="date"
                    disabled={selectedContrato && !isEditing}
                    value={formData.vigenciaFim}
                    onChange={(e) =>
                      setFormData({ ...formData, vigenciaFim: e.target.value })
                    }
                    className="w-full mt-1 p-2 border rounded-lg text-sm bg-slate-50 disabled:opacity-70"
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase">
                    Valor Global (R$)
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    disabled={selectedContrato && !isEditing}
                    value={formData.valorGlobal}
                    onChange={(e) =>
                      setFormData({ ...formData, valorGlobal: e.target.value })
                    }
                    className="w-full mt-1 p-2 border rounded-lg text-sm bg-slate-50 disabled:opacity-70"
                    placeholder="Ex: 150000.00"
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase">
                    Status
                  </label>
                  <select
                    disabled={selectedContrato && !isEditing}
                    value={formData.status}
                    onChange={(e) =>
                      setFormData({ ...formData, status: e.target.value })
                    }
                    className="w-full mt-1 p-2 border rounded-lg text-sm bg-slate-50 disabled:opacity-70"
                  >
                    <option value="ATIVO">ATIVO</option>
                    <option value="INATIVO">INATIVO</option>
                    <option value="SUSPENSO">SUSPENSO</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-600 uppercase">
                  Escopo do Contrato
                </label>
                <textarea
                  rows="3"
                  disabled={selectedContrato && !isEditing}
                  value={formData.escopo}
                  onChange={(e) =>
                    setFormData({ ...formData, escopo: e.target.value })
                  }
                  className="w-full mt-1 p-2 border rounded-lg text-sm bg-slate-50 disabled:opacity-70"
                  placeholder="Descreva as metas e obrigações..."
                ></textarea>
              </div>

              {(!selectedContrato || isEditing) && (
                <div className="flex justify-end pt-4 border-t">
                  <button
                    type="submit"
                    className="bg-blue-600 text-white px-6 py-2 rounded-lg font-semibold flex items-center gap-2 hover:bg-blue-700"
                  >
                    <Save size={18} /> Salvar Alterações
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
