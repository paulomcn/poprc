import React, { useState, useEffect } from "react";
import {
  Plane,
  Upload,
  AlertCircle,
  CheckCircle2,
  Loader2,
  Plus,
  X,
  DollarSign,
  Briefcase,
  User,
  Edit,
} from "lucide-react";
import api from "../services/api";

export default function PainelViagensEReembolso() {
  const [viagens, setViagens] = useState([]);
  const [funcionarios, setFuncionarios] = useState([]);
  const [projetos, setProjetos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [inputs, setInputs] = useState({});

  // CONTROLES DO MODAL CRUD 💥
  const [modalOpen, setModalOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [selectedId, setSelectedId] = useState(null);

  const [formData, setFormData] = useState({
    solicitacaoVeiculo: "",
    hospedagemDetalhes: "",
    adiantamentoDiarias: "",
    custoPlanejado: "",
    funcionarioId: "",
    projetoId: "",
  });

  const carregarDadosGerais = async () => {
    try {
      setLoading(true);
      const [resViagens, resFunc, resProj] = await Promise.all([
        api.get("/financeiro/viagens"),
        api.get("/funcionarios"),
        api.get("/projetos"),
      ]);
      setViagens(Array.isArray(resViagens.data) ? resViagens.data : []);
      setFuncionarios(resFunc.data || []);
      setProjetos(resProj.data || []);
      setError(null);
    } catch (err) {
      setError("Erro ao sincronizar o ecossistema de viagens e reembolsos.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregarDadosGerais();
  }, []);

  // 💾 DISPARAR ABERTURA PARA CRIAÇÃO
  const handleOpenCreate = () => {
    setIsEditing(false);
    setSelectedId(null);
    setFormData({
      solicitacaoVeiculo: "",
      hospedagemDetalhes: "",
      adiantamentoDiarias: "",
      custoPlanejado: "",
      funcionarioId: funcionarios[0]?.id || "",
      projetoId: projetos[0]?.id || "",
    });
    setModalOpen(true);
  };

  // ✏️ DISPARAR ABERTURA PARA EDIÇÃO 💥
  const handleOpenEdit = (v) => {
    setIsEditing(true);
    setSelectedId(v.id);
    setFormData({
      solicitacaoVeiculo: v.solicitacaoVeiculo || "",
      hospedagemDetalhes: v.hospedagemDetalhes || "",
      adiantamentoDiarias: v.adiantamentoDiarias || "",
      custoPlanejado: v.custoPlanejado || "",
      funcionarioId: v.funcionario?.id || "",
      projetoId: v.projeto?.id || "",
    });
    setModalOpen(true);
  };

  // 🚀 SUBMIT DO FORMULÁRIO CORRIGIDO SEM LONG() FANTASMA 💥
  const handleSaveViagem = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...formData,
        adiantamentoDiarias: parseFloat(formData.adiantamentoDiarias) || 0,
        custoPlanejado: parseFloat(formData.custoPlanejado) || 0,
        funcionarioId: formData.funcionarioId
          ? parseInt(formData.funcionarioId, 10)
          : null,
        projetoId: formData.projetoId ? parseInt(formData.projetoId, 10) : null,
      };

      if (isEditing) {
        await api.put(`/financeiro/viagens/${selectedId}`, payload);
        setSuccess("Planejamento de viagem atualizado com sucesso!");
      } else {
        await api.post("/financeiro/viagens", payload);
        setSuccess("Nova ordem de viagem lançada e salva no Postgres!");
      }
      setModalOpen(false);
      carregarDadosGerais();
    } catch (err) {
      setError("Falha ao salvar planejamento de viagem no banco.");
      console.error(err);
    }
  };

  const handleFechamento = async (viagemId) => {
    const valorReal = parseFloat(inputs[viagemId]);
    if (!valorReal || valorReal <= 0) {
      setError("Por favor, insira um valor real de custo válido.");
      return;
    }
    try {
      await api.post("/financeiro/prestacao-contas", {
        viagemId: viagemId,
        custoReal: valorReal,
        caminhoNotaFiscal: `upload_nota_fiscal_id_${viagemId}.pdf`,
      });
      setSuccess("Prestação de contas fechada e salva!");
      carregarDadosGerais();
    } catch (err) {
      setError("Erro ao fechar prestação de contas.");
    }
  };

  const format = (v) =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(v || 0);

  if (loading)
    return (
      <div className="min-h-screen bg-slate-950 flex justify-center items-center">
        <Loader2 className="w-10 h-10 animate-spin text-indigo-500" />
      </div>
    );

  return (
    <div className="min-h-screen bg-slate-950 p-4 md:p-8 font-sans text-slate-100">
      <div className="max-w-6xl mx-auto space-y-8">
        <header className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-black tracking-tight text-white flex items-center gap-3">
              <Plane className="w-8 h-8 text-indigo-400" /> Viagens e Reembolsos
            </h1>
            <p className="text-slate-400 text-sm mt-1">
              Acerto de contas operacional com o financeiro
            </p>
          </div>
          <button
            onClick={handleOpenCreate}
            className="bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-sm px-4 py-2.5 rounded-xl flex items-center gap-2 shadow-lg shadow-indigo-600/20 transition-all"
          >
            <Plus size={18} /> Nova Viagem
          </button>
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

        <div className="grid grid-cols-1 gap-6">
          {viagens.length === 0 ? (
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-8 text-center text-slate-500">
              Nenhuma viagem localizada no banco de dados.
            </div>
          ) : (
            viagens.map((viagem) => {
              const custoReal = viagem.prestacaoContas?.custoReal;
              const statusFechado = custoReal > 0;
              const estourou = custoReal > viagem.custoPlanejado;

              return (
                <div
                  key={viagem.id}
                  className={`bg-slate-900 border ${estourou ? "border-rose-500/30 bg-rose-950/10" : "border-slate-800"} rounded-2xl p-6 shadow-xl flex flex-col md:flex-row justify-between items-start md:items-center gap-6 transition-all`}
                >
                  <div className="flex-1 space-y-2">
                    <div className="flex items-center gap-3">
                      <h3 className="text-lg font-bold text-white">
                        {viagem.solicitacaoVeiculo || "Veículo Comum"}
                      </h3>
                      <span
                        className={`px-2.5 py-0.5 rounded text-[10px] font-black uppercase tracking-wider ${statusFechado ? "bg-emerald-500/20 text-emerald-400" : "bg-indigo-500/20 text-indigo-400"}`}
                      >
                        {statusFechado ? "CONCLUÍDA" : "EM ANDAMENTO"}
                      </span>
                      {/* Botão de Editar Injetado na lista 💥 */}
                      <button
                        onClick={() => handleOpenEdit(viagem)}
                        className="text-slate-500 hover:text-indigo-400 p-1 rounded transition-colors"
                      >
                        <Edit size={15} />
                      </button>
                    </div>
                    <p className="text-sm text-slate-400">
                      {viagem.hospedagemDetalhes ||
                        "Sem detalhes de hospedagem"}
                    </p>
                    <div className="flex gap-4 text-xs text-slate-500 font-medium">
                      <p className="flex items-center gap-1">
                        <User size={14} /> Colaborador:{" "}
                        {viagem.funcionario?.nome || "Não Informado"}
                      </p>
                      <p className="flex items-center gap-1">
                        <Briefcase size={14} /> Proj ID: #
                        {viagem.projeto?.id || "Global"}
                      </p>
                    </div>

                    <div className="flex gap-6 pt-2">
                      <div>
                        <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">
                          Orçamento Planejado
                        </p>
                        <p className="text-base font-bold text-slate-300">
                          {format(viagem.custoPlanejado)}
                        </p>
                      </div>
                      <div>
                        <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">
                          Diárias Adiantadas
                        </p>
                        <p className="text-base font-bold text-slate-400">
                          {format(viagem.adiantamentoDiarias)}
                        </p>
                      </div>
                      {statusFechado && (
                        <div>
                          <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">
                            Custo Real Final
                          </p>
                          <p
                            className={`text-base font-bold flex items-center gap-1 ${estourou ? "text-rose-400" : "text-emerald-400"}`}
                          >
                            {format(custoReal)}{" "}
                            {estourou ? (
                              <AlertCircle className="w-4 h-4" />
                            ) : (
                              <CheckCircle2 className="w-4 h-4" />
                            )}
                          </p>
                        </div>
                      )}
                    </div>
                  </div>

                  {!statusFechado ? (
                    <div className="w-full md:w-auto flex flex-col sm:flex-row gap-3 bg-slate-950 p-4 rounded-xl border border-slate-800">
                      <div className="flex-1">
                        <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1">
                          Custo Real (R$)
                        </label>
                        <input
                          type="number"
                          placeholder="0.00"
                          className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-indigo-500"
                          onChange={(e) =>
                            setInputs({
                              ...inputs,
                              [viagem.id]: e.target.value,
                            })
                          }
                        />
                      </div>
                      <div className="flex flex-col gap-2 justify-end">
                        <label className="flex items-center justify-center gap-2 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-bold py-2.5 px-4 rounded-lg cursor-pointer transition">
                          <Upload className="w-4 h-4" /> Nota Fiscal
                          <input type="file" className="hidden" />
                        </label>
                        <button
                          onClick={() => handleFechamento(viagem.id)}
                          className="bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold py-2.5 px-4 rounded-lg transition"
                        >
                          Fechar Viagem
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="w-full md:w-auto text-right p-4">
                      {estourou ? (
                        <p className="text-xs text-rose-400 font-bold max-w-[200px]">
                          Orçamento estourou em{" "}
                          {format(custoReal - viagem.custoPlanejado)}.
                        </p>
                      ) : (
                        <p className="text-xs text-emerald-400 font-bold flex items-center gap-2 justify-end">
                          <CheckCircle2 className="w-4 h-4" /> Prestação
                          Aprovada
                        </p>
                      )}
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* 🛑 MODAL COMPLETO CRUD COM INTEGRAÇÃO REFEITA */}
      {modalOpen && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg overflow-hidden shadow-2xl">
            <div className="p-5 border-b border-slate-800 flex justify-between items-center bg-slate-950">
              <h2 className="text-lg font-bold text-white flex items-center gap-2">
                <Plane size={20} className="text-indigo-400" />{" "}
                {isEditing
                  ? "✏️ Editar Ordem de Viagem"
                  : "Planejar Nova Viagem"}
              </h2>
              <button
                onClick={() => setModalOpen(false)}
                className="text-slate-400 hover:text-white"
              >
                <X size={22} />
              </button>
            </div>
            <form onSubmit={handleSaveViagem} className="p-6 space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                    Colaborador / Viajante *
                  </label>
                  <select
                    required
                    value={formData.funcionarioId}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        funcionarioId: e.target.value,
                      })
                    }
                    className="w-full mt-1.5 p-2.5 bg-slate-950 border border-slate-700 rounded-lg text-sm text-white outline-none focus:border-indigo-500"
                  >
                    <option value="">Selecione...</option>
                    {funcionarios.map((f) => (
                      <option key={f.id} value={f.id}>
                        {f.nome} ({f.funcao})
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                    Projeto Vinculado *
                  </label>
                  <select
                    required
                    value={formData.projetoId}
                    onChange={(e) =>
                      setFormData({ ...formData, projetoId: e.target.value })
                    }
                    className="w-full mt-1.5 p-2.5 bg-slate-950 border border-slate-700 rounded-lg text-sm text-white outline-none focus:border-indigo-500"
                  >
                    <option value="">Selecione...</option>
                    {projetos.map((p) => (
                      <option key={p.id} value={p.id}>
                        Projeto #{p.id} - {p.contrato?.cliente || "Logística"}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                    Custo Planejado (Orçamento) *
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    value={formData.custoPlanejado}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        custoPlanejado: e.target.value,
                      })
                    }
                    className="w-full mt-1.5 p-2.5 bg-slate-950 border border-slate-700 rounded-lg text-sm text-white outline-none focus:border-indigo-500"
                    placeholder="0.00"
                  />
                </div>
                <div>
                  <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                    Adiantamento de Diárias (R$)
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    value={formData.adiantamentoDiarias}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        adiantamentoDiarias: e.target.value,
                      })
                    }
                    className="w-full mt-1.5 p-2.5 bg-slate-950 border border-slate-700 rounded-lg text-sm text-white outline-none focus:border-indigo-500"
                    placeholder="0.00"
                  />
                </div>
              </div>
              <div>
                <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                  Solicitação do Veículo / Transporte *
                </label>
                <input
                  type="text"
                  required
                  value={formData.solicitacaoVeiculo}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      solicitacaoVeiculo: e.target.value,
                    })
                  }
                  className="w-full mt-1.5 p-2.5 bg-slate-950 border border-slate-700 rounded-lg text-sm text-white outline-none focus:border-indigo-500"
                  placeholder="Ex: Locação de SUV / Carro da Empresa / Reembolso Km"
                />
              </div>
              <div>
                <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                  Detalhes da Hospedagem / Acomodação
                </label>
                <textarea
                  rows="2"
                  value={formData.hospedagemDetalhes}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      hospedagemDetalhes: e.target.value,
                    })
                  }
                  className="w-full mt-1.5 p-2.5 bg-slate-950 border border-slate-700 rounded-lg text-sm text-white outline-none focus:border-indigo-500"
                  placeholder="Ex: Hotel Executive Natal..."
                ></textarea>
              </div>
              <button
                type="submit"
                className="w-full bg-indigo-600 hover:bg-indigo-500 text-white py-2.5 rounded-lg font-bold text-sm transition-colors shadow-lg shadow-indigo-600/20"
              >
                Salvar Viagem
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
