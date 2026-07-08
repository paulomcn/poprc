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
  const [comarcas, setComarcas] = useState([]);
  const [selectedComarcaId, setSelectedComarcaId] = useState("");
  const [dados, setDados] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const [editModalOpen, setEditModalOpen] = useState(false);
  const [selectedMaterial, setSelectedMaterial] = useState(null);
  const [novaQuantidade, setNovaQuantidade] = useState("");

  const carregarComarcas = async () => {
    try {
      const res = await api.get("/comarcas");
      setComarcas(res.data || []);
      if (res.data && res.data.length > 0) {
        setSelectedComarcaId(res.data[0].id);
      }
    } catch (err) {
      console.error("Erro ao listar comarcas", err);
      setError("Não foi possível buscar a lista de comarcas ativas.");
    } finally {
      setLoading(false);
    }
  };

  const carregarAuditoria = async (id) => {
    if (!id) return;
    try {
      setLoading(true);
      const response = await api.get(`/comarcas/${id}/auditoria`);
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
    carregarComarcas();
  }, []);

  useEffect(() => {
    if (selectedComarcaId) {
      carregarAuditoria(selectedComarcaId);
    }
  }, [selectedComarcaId]);

  const homologarAsBuilt = async () => {
    try {
      const response = await api.patch(
        `/comarcas/${selectedComarcaId}/as-built/homologar`,
      );
      setDados(response.data);
      setError(null);
      setSuccess("Documentação As-Built homologada para esta OS.");
    } catch (err) {
      setError(
        err.response?.data?.erro ||
          "Erro ao homologar As-Built. Verifique as divergências da auditoria.",
      );
    }
  };

  const abrirModalEdicao = (material) => {
    setSelectedMaterial(material);
    setNovaQuantidade(material.utilizado);
    setEditModalOpen(true);
  };

  const salvarAjusteEstoque = async (e) => {
    e.preventDefault();
    try {
      await api.put(
        `/comarcas/materiais/${selectedMaterial.id}/auditoria`,
        null,
        { params: { quantidadeAuditada: parseInt(novaQuantidade, 10) || 0 } },
      );
      setSuccess("Divergência de material atualizada com sucesso!");
      setEditModalOpen(false);
      carregarAuditoria(selectedComarcaId);
    } catch (err) {
      setError("Erro ao atualizar dados de inventário.");
    }
  };

  const materiais = dados?.materiais || [];
  const asBuiltStatus = dados?.asBuiltStatus || "PENDENTE";
  const conciliado = Boolean(dados?.conciliado);
  const podeHomologar =
    materiais.length > 0 && conciliado && asBuiltStatus !== "HOMOLOGADO";
  const statusAsBuiltClass =
    asBuiltStatus === "HOMOLOGADO"
      ? "text-emerald-400"
      : asBuiltStatus === "DIVERGENTE"
        ? "text-rose-400"
        : "text-amber-400";
  const comarcaSelecionada = comarcas.find(
    (comarca) => String(comarca.id) === String(selectedComarcaId),
  );
  const numeroOsSelecionada =
    dados?.numeroOs || comarcaSelecionada?.ordemServico?.numeroOs || "OS não vinculada";

  if (loading && comarcas.length === 0)
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
              Módulo 15 e 16 - Conciliação por OS, comarca e execução de campo
            </p>
          </div>

          <div className="flex items-center gap-3 w-full sm:w-auto">
            <div className="flex items-center gap-2 bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-sm text-slate-300">
              <Briefcase size={16} className="text-indigo-400" />
              <select
                value={selectedComarcaId}
                onChange={(e) => setSelectedComarcaId(e.target.value)}
                className="bg-transparent border-none outline-none text-white text-xs font-bold cursor-pointer"
              >
                {comarcas.map((comarca) => (
                  <option
                    key={comarca.id}
                    value={comarca.id}
                    className="bg-slate-900 text-white"
                  >
                    {comarca.ordemServico?.numeroOs || "OS não vinculada"} -{" "}
                    {comarca.nomeComarca || "Comarca operacional"}
                  </option>
                ))}
              </select>
            </div>
            <button
              onClick={() => carregarAuditoria(selectedComarcaId)}
              className="p-2.5 bg-slate-900 border border-slate-800 rounded-xl hover:bg-slate-800 transition"
            >
              <RefreshCw className="w-4 h-4 text-slate-400" />
            </button>
          </div>
        </header>

        {error && (
          <div className="bg-rose-950/40 border border-rose-500/30 text-rose-300 p-4 rounded-xl text-sm">
            {error}
          </div>
        )}
        {success && (
          <div className="bg-emerald-950/40 border border-emerald-500/30 text-emerald-300 p-4 rounded-xl text-sm">
            {success}
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Tabela de Discrepância de Inventário com Conciliação Dinâmica */}
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
                    <th className="px-6 py-4 text-center">Previsto (A)</th>
                    <th className="px-6 py-4 text-center">Auditado (B)</th>
                    <th className="px-6 py-4 text-center">Balanço Final</th>
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
                        Nenhum material previsto para esta OS/comarca no banco.
                      </td>
                    </tr>
                  ) : (
                    materiais.map((mat, i) => {
                      const saldoDivergencia = mat.previsto - mat.utilizado;

                      return (
                        <tr
                          key={mat.id || i}
                          className="hover:bg-slate-800/50 transition-colors"
                        >
                          <td className="px-6 py-4 font-medium text-slate-300">
                            <div className="flex items-center gap-2">
                              <span>{mat.nome}</span>
                              {mat.estoqueBaixado && (
                                <span className="rounded bg-emerald-500/10 px-2 py-0.5 text-[10px] font-bold uppercase text-emerald-400 border border-emerald-500/20">
                                  Baixado
                                </span>
                              )}
                              {mat.estoqueReservado && !mat.estoqueBaixado && (
                                <span className="rounded bg-blue-500/10 px-2 py-0.5 text-[10px] font-bold uppercase text-blue-400 border border-blue-500/20">
                                  Reservado
                                </span>
                              )}
                            </div>
                          </td>
                          <td className="px-6 py-4 text-center text-slate-400 font-mono">
                            {mat.previsto}
                          </td>
                          <td className="px-6 py-4 text-center font-mono font-bold text-slate-200">
                            {mat.utilizado}
                          </td>

                          {/* Destacador visual automatizado do Saldo contábil */}
                          <td className="px-6 py-4 text-center">
                            {saldoDivergencia === 0 ? (
                              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                                <CheckCircle2 className="w-3 h-3" /> Batido
                                Perfeito
                              </span>
                            ) : saldoDivergencia > 0 ? (
                              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded text-xs font-bold bg-blue-500/10 text-blue-400 border border-blue-500/20">
                                Sobra: {saldoDivergencia} un
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded text-xs font-bold bg-rose-500/10 text-rose-400 border border-rose-500/20">
                                <AlertTriangle className="w-3 h-3" /> Falta:{" "}
                                {Math.abs(saldoDivergencia)} un
                              </span>
                            )}
                          </td>
                          <td className="px-6 py-4 text-center">
                            <button
                              onClick={() => abrirModalEdicao(mat)}
                              disabled={mat.estoqueBaixado || asBuiltStatus === "HOMOLOGADO"}
                              className="p-1.5 bg-slate-950 hover:bg-slate-800 disabled:bg-slate-900 disabled:text-slate-700 disabled:cursor-not-allowed border border-slate-800 rounded-lg text-slate-400 hover:text-indigo-400 transition-colors"
                              title={
                                mat.estoqueBaixado
                                  ? "Material já baixado no estoque"
                                  : "Ajustar quantidade auditada"
                              }
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
                A conciliação usa a OS {numeroOsSelecionada} para comparar o
                previsto na comarca com o que foi auditado em campo.
              </p>

              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 text-center">
                <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1">
                  Situação do Fechamento
                </span>
                <p className={`text-xl font-black tracking-widest ${statusAsBuiltClass}`}>
                  {asBuiltStatus}
                </p>
              </div>
            </div>

            {asBuiltStatus !== "HOMOLOGADO" ? (
              <button
                onClick={homologarAsBuilt}
                disabled={!podeHomologar}
                className="w-full bg-indigo-600 hover:bg-indigo-500 disabled:bg-slate-800 disabled:text-slate-500 disabled:cursor-not-allowed text-white font-bold py-3 px-4 rounded-xl text-sm transition shadow-lg shadow-indigo-600/10"
              >
                {materiais.length === 0
                  ? "Sem Materiais Previstos"
                  : conciliado
                    ? "Homologar As-Built"
                    : "Corrija Divergências"}
              </button>
            ) : (
              <div className="text-center p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-xs font-bold text-emerald-400 flex items-center justify-center gap-2">
                <CheckCircle2 className="w-4 h-4" /> As-Built homologado
              </div>
            )}
          </div>
        </div>
      </div>

      {/* MODAL AJUSTAR USO DE MATERIAL */}
      {editModalOpen && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-sm overflow-hidden shadow-2xl">
            <div className="p-5 border-b border-slate-800 flex justify-between items-center bg-slate-950">
              <h2 className="text-sm font-bold text-white truncate">
                Corrigir Consumo: {selectedMaterial?.nome}
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
                <label className="text-[10px] font-bold text-slate-400 uppercase block mb-1">
                  Quantidade Prevista
                </label>
                <input
                  type="text"
                  disabled
                  value={selectedMaterial?.previsto}
                  className="w-full p-2.5 bg-slate-950 border border-slate-800 rounded-lg text-sm text-slate-500 outline-none cursor-not-allowed"
                />
              </div>
              <div>
                <label className="text-[10px] font-bold text-slate-400 uppercase block mb-1">
                  Quantidade Real Utilizada *
                </label>
                <input
                  type="number"
                  required
                  value={novaQuantidade}
                  onChange={(e) => setNovaQuantidade(e.target.value)}
                  className="w-full p-2.5 bg-slate-950 border border-slate-700 rounded-lg text-sm text-white outline-none focus:border-indigo-500"
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
