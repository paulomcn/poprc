import React, { useState, useEffect } from "react";
import {
  Package,
  FileCheck,
  CheckCircle2,
  AlertTriangle,
  Loader2,
  RefreshCw,
  Briefcase,
  Edit2,
  X,
} from "lucide-react";
import api from "../services/api";

export default function AuditoriaMateriaisEAsBuilt() {
  const [projetos, setProjetos] = useState([]);
  const [selectedProjetoId, setSelectedProjetoId] = useState("");
  const [dados, setDados] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Estados para o Modal de Edição de Quantidade 💥
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [selectedMaterial, setSelectedMaterial] = useState(null);
  const [novaQuantidade, setNovaQuantidade] = useState("");

  // 1. Carrega os projetos disponíveis para popular o seletor do topo
  const carregarProjetos = async () => {
    try {
      const res = await api.get("/projetos");
      setProjetos(res.data || []);
      if (res.data && res.data.length > 0) {
        setSelectedProjetoId(res.data[0].id);
      }
    } catch (err) {
      console.error("Erro ao listar projetos", err);
      setError("Não foi possível buscar a lista de projetos ativos.");
    }
  };

  // 2. Carrega a auditoria do projeto selecionado no dropdown
  const carregarAuditoria = async (id) => {
    if (!id) return;
    try {
      setLoading(true);
      const response = await api.get(`/projetos/${id}/auditoria`);
      setDados(response.data);
      setError(null);
    } catch (err) {
      setError("Erro ao carregar os dados de engenharia do servidor.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregarProjetos();
  }, []);

  useEffect(() => {
    if (selectedProjetoId) {
      carregarAuditoria(selectedProjetoId);
    }
  }, [selectedProjetoId]);

  // 🚀 PUT para homologar o As-Built
  const homologarAsBuilt = async () => {
    try {
      await api.put(`/projetos/${selectedProjetoId}/as-built/homologar`);
      setSuccess(
        "Documentação Técnica As-Built homologada em nível de produção!",
      );
      carregarAuditoria(selectedProjetoId);
    } catch (err) {
      setError("Erro ao processar homologação de engenharia.");
    }
  };

  // ✏️ Abre o modal para atualizar a quantidade utilizada de um item
  const abrirModalEdicao = (material) => {
    setSelectedMaterial(material);
    setNovaQuantidade(material.utilizado);
    setEditModalOpen(true);
  };

  // 💾 Envia o ajuste de inventário para o Postgres
  const salvarAjusteEstoque = async (e) => {
    e.preventDefault();
    try {
      await api.put(
        `/projetos/${selectedProjetoId}/materiais/${selectedMaterial.id}`,
        null,
        {
          params: { quantidadeUtilizada: parseInt(novaQuantidade, 10) || 0 },
        },
      );
      setSuccess("Divergência de material atualizada com sucesso!");
      setEditModalOpen(false);
      carregarAuditoria(selectedProjetoId);
    } catch (err) {
      setError("Erro ao atualizar dados de inventário.");
    }
  };

  const materiais = dados?.materiais || []; // Chave sincronizada perfeitamente com o Java! 💥
  const asBuiltStatus = dados?.asBuiltStatus || "PENDENTE";

  if (loading && projetos.length === 0)
    return (
      <div className="min-h-screen bg-slate-950 flex justify-center items-center">
        <Loader2 className="w-10 h-10 animate-spin text-indigo-500" />
      </div>
    );

  return (
    <div className="min-h-screen bg-slate-950 p-4 md:p-8 font-sans text-slate-100">
      <div className="max-w-6xl mx-auto space-y-8">
        <header className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <h1 className="text-3xl font-black tracking-tight text-white flex items-center gap-2">
              Auditoria de Materiais & As-Built
            </h1>
            <p className="text-slate-400 text-sm mt-0.5">
              Módulo 15 e 16 - Controle de conformidade técnica e engenharia
              reversa
            </p>
          </div>

          {/* SELETOR DE PROJETOS DINÂMICO INJETADO NO TOPO 💥 */}
          <div className="flex items-center gap-3 w-full sm:w-auto">
            <div className="flex items-center gap-2 bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-sm text-slate-300">
              <Briefcase size={16} className="text-indigo-400" />
              <select
                value={selectedProjetoId}
                onChange={(e) => setSelectedProjetoId(e.target.value)}
                className="bg-transparent border-none outline-none text-white text-xs font-bold cursor-pointer"
              >
                {projetos.map((p) => (
                  <option
                    key={p.id}
                    value={p.id}
                    className="bg-slate-900 text-white"
                  >
                    Projeto #{p.id} - Comarca{" "}
                    {p.nomeComarcaVinculada || "Operacional"}
                  </option>
                ))}
              </select>
            </div>
            <button
              onClick={() => carregarAuditoria(selectedProjetoId)}
              className="p-2.5 bg-slate-900 border border-slate-800 rounded-xl hover:bg-slate-800 transition"
            >
              <RefreshCw className="w-4 h-4 text-slate-400" />
            </button>
          </div>
        </header>

        {error && (
          <div className="bg-rose-950/40 border border-rose-500/30 text-rose-300 p-4 rounded-xl text-sm animate-fade-in">
            {error}
          </div>
        )}
        {success && (
          <div className="bg-emerald-950/40 border border-emerald-500/30 text-emerald-300 p-4 rounded-xl text-sm animate-fade-in">
            {success}
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Tabela de Discrepância de Inventário */}
          <div className="lg:col-span-2 bg-slate-900 border border-slate-800 rounded-2xl shadow-xl overflow-hidden">
            <div className="p-6 border-b border-slate-800 flex items-center gap-3">
              <Package className="w-5 h-5 text-indigo-400" />
              <h2 className="font-bold text-white">
                Discrepância de Inventário
              </h2>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm whitespace-nowrap">
                <thead className="bg-slate-950/50 text-slate-400 border-b border-slate-800 uppercase text-xs font-bold tracking-wider">
                  <tr>
                    <th className="px-6 py-4">Item</th>
                    <th className="px-6 py-4 text-center">Previsto</th>
                    <th className="px-6 py-4 text-center">Utilizado</th>
                    <th className="px-6 py-4 text-center">Status</th>
                    <th className="px-6 py-4 text-center">Ajustar</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800">
                  {materiais.length === 0 ? (
                    <tr>
                      <td
                        colSpan="5"
                        className="px-6 py-8 text-center text-slate-500"
                      >
                        Nenhum insumo ou material auditado para este projeto no
                        banco.
                      </td>
                    </tr>
                  ) : (
                    materiais.map((mat, i) => {
                      const divergente = mat.utilizado > mat.previsto;
                      return (
                        <tr
                          key={i}
                          className="hover:bg-slate-800/50 transition-colors"
                        >
                          <td className="px-6 py-4 font-medium text-slate-300">
                            {mat.nome}
                          </td>
                          <td className="px-6 py-4 text-center text-slate-400 font-mono">
                            {mat.previsto}
                          </td>
                          <td
                            className={`px-6 py-4 text-center font-mono font-bold ${divergente ? "text-rose-400" : "text-slate-200"}`}
                          >
                            {mat.utilizado}
                          </td>
                          <td className="px-6 py-4 text-center">
                            {divergente ? (
                              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded text-xs font-bold bg-rose-500/10 text-rose-400 border border-rose-500/20">
                                <AlertTriangle className="w-3 h-3" /> Excedido
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                                <CheckCircle2 className="w-3 h-3" /> OK
                              </span>
                            )}
                          </td>
                          {/* Ação de Ajuste adicionada 💥 */}
                          <td className="px-6 py-4 text-center">
                            <button
                              onClick={() => abrirModalEdicao(mat)}
                              className="p-1.5 bg-slate-950 hover:bg-slate-800 border border-slate-800 rounded-lg text-slate-400 hover:text-indigo-400 transition-colors"
                            >
                              <Edit2 size={14} />
                            </button>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Card do As-Built */}
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl flex flex-col justify-between space-y-6">
            <div className="space-y-4">
              <div className="flex items-center gap-3 pb-2 border-b border-slate-800">
                <FileCheck className="w-5 h-5 text-indigo-400" />
                <h2 className="font-bold text-white">Status do As-Built</h2>
              </div>
              <p className="text-sm text-slate-400 leading-relaxed">
                O arquivo As-Built valida que a engenharia executada em campo
                bate 100% com a planta aprovada pelo cliente.
              </p>

              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 text-center">
                <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1">
                  Situação Atual
                </span>
                <p
                  className={`text-xl font-black tracking-widest ${asBuiltStatus === "HOMOLOGADO" ? "text-emerald-400" : "text-amber-400"}`}
                >
                  {asBuiltStatus}
                </p>
              </div>
            </div>

            {asBuiltStatus !== "HOMOLOGADO" ? (
              <button
                onClick={homologarAsBuilt}
                className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-bold py-3 px-4 rounded-xl text-sm transition shadow-lg shadow-indigo-600/10"
              >
                Homologar Documento
              </button>
            ) : (
              <div className="text-center p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-xs font-bold text-emerald-400 flex items-center justify-center gap-2">
                <CheckCircle2 className="w-4 h-4" /> Projeto Final Homologado
              </div>
            )}
          </div>
        </div>
      </div>

      {/* 🛑 MODAL CYBER-DARK: AJUSTAR USO DE MATERIAL */}
      {editModalOpen && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-sm overflow-hidden shadow-2xl animate-fade-in">
            <div className="p-5 border-b border-slate-800 flex justify-between items-center bg-slate-950">
              <h2 className="text-sm font-bold text-white truncate">
                ✏️ Corrigir Consumo: {selectedMaterial?.nome}
              </h2>
              <button
                onClick={() => setEditModalOpen(false)}
                className="text-slate-400 hover:text-white"
              >
                <X size={20} />
              </button>
            </div>
            <form onSubmit={salvarAjusteEstoque} className="p-6 space-y-4">
              <div>
                <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1">
                  Quantidade Prevista em Escopo
                </label>
                <input
                  type="text"
                  disabled
                  value={selectedMaterial?.previsto}
                  className="w-full p-2.5 bg-slate-950 border border-slate-800 rounded-lg text-sm text-slate-500 outline-none cursor-not-allowed"
                />
              </div>
              <div>
                <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1">
                  Quantidade Real Utilizada em Campo *
                </label>
                <input
                  type="number"
                  required
                  value={novaQuantidade}
                  onChange={(e) => setNovaQuantidade(e.target.value)}
                  className="w-full p-2.5 bg-slate-950 border border-slate-700 rounded-lg text-sm text-white outline-none focus:border-indigo-500"
                  placeholder="0"
                />
              </div>
              <button
                type="submit"
                className="w-full bg-indigo-600 hover:bg-indigo-500 text-white py-2.5 rounded-lg font-bold text-sm transition-colors shadow-lg"
              >
                Salvar Ajuste Contábil
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
