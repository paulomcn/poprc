import { useState, useEffect } from "react";
import { Plus, Eye, Edit, Save, X, Briefcase, User, Archive, RotateCcw, Trash2 } from "lucide-react";
import api from "../services/api";
import Alert from "../components/Alert";

export default function Projetos() {
  const [projetos, setProjetos] = useState([]);
  const [contratos, setContratos] = useState([]); // Armazena contratos para o Select
  const [funcionarios, setFuncionarios] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedProjeto, setSelectedProjeto] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);
  const [incluirArquivados, setIncluirArquivados] = useState(false);
  const [equipeForm, setEquipeForm] = useState([
    { funcionarioId: "", papel: "LIDER_EQUIPE", responsavelPrincipal: true },
  ]);

  // Estado do formulário batendo com o backend
  const [formData, setFormData] = useState({
    contrato: { id: "" },
    responsavel: { id: "" },
    dataInicio: "",
    dataFim: "",
    status: "EM_ANDAMENTO",
    asBuiltStatus: "PENDENTE",
    nomeComarcaVinculada: "", //  NOVO
  });

  useEffect(() => {
    carregarDados();
  }, [incluirArquivados]);

  const carregarDados = async () => {
    try {
      setLoading(true);
      const [resProjetos, resContratos, resFuncionarios] = await Promise.all([
        api.get("/projetos", { params: { incluirArquivados } }),
        api.get("/contratos"),
        api.get("/funcionarios"),
      ]);
      setProjetos(resProjetos.data);
      setContratos(resContratos.data);
      setFuncionarios(resFuncionarios.data || []);
    } catch (err) {
      console.error("Erro ao carregar dados", err);
      setErrorMessage("Falha ao sincronizar dados com o servidor.");
    } finally {
      setLoading(false);
    }
  };

  const alterarArquivamento = async (projeto) => {
    try {
      if (projeto.arquivado) {
        await api.patch(`/projetos/${projeto.id}/restaurar`);
      } else {
        const motivo = window.prompt("Informe o motivo para arquivar este projeto:");
        if (!motivo?.trim()) return;
        await api.patch(`/projetos/${projeto.id}/arquivar`, {
          usuario: "Paulo Morais",
          motivo: motivo.trim(),
        });
      }
      carregarDados();
    } catch (err) {
      alert(err.response?.data?.erro || "Não foi possível alterar o arquivamento do projeto.");
    }
  };

  const handleOpenModal = (projeto = null, editMode = false) => {
    setSelectedProjeto(projeto);
    setIsEditing(editMode);
    setErrorMessage(null);
    setModalOpen(true); // Corrigido: Abre o modal ao clicar

    if (projeto) {
      const equipeExistente = projeto.equipe?.length
        ? projeto.equipe.map((membro) => ({
            funcionarioId: String(membro.funcionario?.id || ""),
            papel: membro.papel || "TECNICO",
            responsavelPrincipal: Boolean(membro.responsavelPrincipal),
          }))
        : [{
            funcionarioId: String(projeto.responsavel?.id || ""),
            papel: "LIDER_EQUIPE",
            responsavelPrincipal: true,
          }];
      setEquipeForm(equipeExistente);
      setFormData({
        contrato: { id: projeto.contrato?.id || "" },
        responsavel: { id: projeto.responsavel?.id || "" },
        dataInicio: projeto.dataInicio || "",
        dataFim: projeto.dataFim || "",
        status: projeto.status || "EM_ANDAMENTO",
        asBuiltStatus: projeto.asBuiltStatus || "PENDENTE",
        nomeComarcaVinculada: "",
      });
    } else {
      setEquipeForm([
        { funcionarioId: "", papel: "LIDER_EQUIPE", responsavelPrincipal: true },
      ]);
      setFormData({
        contrato: { id: contratos[0]?.id || "" },
        responsavel: { id: "" },
        dataInicio: "",
        dataFim: "",
        status: "EM_ANDAMENTO",
        asBuiltStatus: "PENDENTE",
        nomeComarcaVinculada: "",
      });
    }
  };

  const handleSave = async (e) => {
    e.preventDefault();
    try {
      if (!formData.contrato.id) {
        setErrorMessage("Selecione um contrato válido!");
        return;
      }
      const membrosValidos = equipeForm.filter((membro) => membro.funcionarioId);
      const principais = membrosValidos.filter((membro) => membro.responsavelPrincipal);
      if (membrosValidos.length === 0 || principais.length !== 1) {
        setErrorMessage("Adicione a equipe e defina exatamente um responsável principal.");
        return;
      }
      if (new Set(membrosValidos.map((membro) => membro.funcionarioId)).size !== membrosValidos.length) {
        setErrorMessage("O mesmo funcionário não pode aparecer duas vezes na equipe.");
        return;
      }

      const payloadProjeto = {
        ...formData,
        responsavel: { id: principais[0].funcionarioId },
      };
      let projetoId;
      if (isEditing && selectedProjeto) {
        await api.put(`/projetos/${selectedProjeto.id}`, payloadProjeto);
        projetoId = selectedProjeto.id;
      } else {
        const response = await api.post("/projetos", payloadProjeto);
        projetoId = response.data?.projeto?.id;
      }
      await api.put(`/projetos/${projetoId}/equipe`, {
        membros: membrosValidos.map((membro) => ({
          funcionarioId: Number(membro.funcionarioId),
          papel: membro.papel,
          responsavelPrincipal: membro.responsavelPrincipal,
        })),
      });
      setModalOpen(false);
      carregarDados();
    } catch (err) {
      console.error("Erro ao salvar projeto", err);
      setErrorMessage(
        err.response?.data?.erro || "Erro ao salvar projeto. Verifique os campos informados.",
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
        <div className="flex items-center gap-3">
          <label className="flex items-center gap-2 text-xs font-bold text-slate-600">
            <input type="checkbox" checked={incluirArquivados} onChange={(e) => setIncluirArquivados(e.target.checked)} />
            Mostrar arquivados
          </label>
          <button
            onClick={() => handleOpenModal(null, false)}
            className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2.5 rounded-lg font-semibold flex items-center gap-2 shadow-sm transition-colors"
          >
            <Plus size={20} /> Novo Projeto
          </button>
        </div>
      </div>

      {errorMessage && <Alert type="error" message={errorMessage} />}

      {/* Grid de Cards de Projetos */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {projetos.map((p) => (
          <div
            key={p.id}
            className={`bg-white p-6 rounded-xl shadow-md border border-slate-200 flex flex-col justify-between space-y-4 ${p.arquivado ? "opacity-60" : ""}`}
          >
            <div>
              <div className="flex justify-between items-start">
                <h3 className="text-lg font-bold text-slate-800">
                  Projeto #{p.id}
                </h3>
                <span
                  className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${p.status === "CONCLUIDO" ? "bg-green-100 text-green-700" : "bg-blue-100 text-blue-700"}`}
                >
                  {p.arquivado ? "ARQUIVADO" : p.status?.replace("_", " ")}
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
                <p className="flex items-center gap-1.5">
                  <User size={14} className="text-slate-400" />
                  <strong>Responsável:</strong> {p.responsavel?.nome || "Não atribuído"}
                </p>
                <div className="pt-1">
                  <strong>Equipe:</strong>
                  <div className="mt-1 flex flex-wrap gap-1">
                    {(p.equipe || []).map((membro) => (
                      <span key={membro.id} className="rounded bg-slate-100 px-2 py-1 text-[10px] font-bold text-slate-600">
                        {membro.funcionario?.nome} · {membro.papel?.replaceAll("_", " ")}
                      </span>
                    ))}
                    {!p.equipe?.length && <span className="text-xs text-slate-400">Somente responsável legado</span>}
                  </div>
                </div>
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
                disabled={p.arquivado}
                className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg flex items-center gap-1 text-xs font-semibold"
              >
                <Edit size={16} /> Editar
              </button>
              <button
                onClick={() => alterarArquivamento(p)}
                title={p.arquivado ? "Restaurar projeto" : "Arquivar projeto"}
                className={`p-2 rounded-lg flex items-center gap-1 text-xs font-semibold ${p.arquivado ? "text-emerald-600 hover:bg-emerald-50" : "text-red-600 hover:bg-red-50"}`}
              >
                {p.arquivado ? <RotateCcw size={16} /> : <Archive size={16} />}
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* MODAL UNIFICADO: DETALHES / NOVO / EDITAR */}
      {modalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl overflow-hidden">
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

              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-bold text-slate-600 uppercase">
                    Equipe do projeto *
                  </label>
                  {(!selectedProjeto || isEditing) && (
                    <button
                      type="button"
                      onClick={() => setEquipeForm((prev) => [
                        ...prev,
                        { funcionarioId: "", papel: "TECNICO", responsavelPrincipal: false },
                      ])}
                      className="inline-flex items-center gap-1 rounded-md border border-blue-200 px-2 py-1 text-xs font-bold text-blue-700 hover:bg-blue-50"
                    >
                      <Plus size={13} /> Membro
                    </button>
                  )}
                </div>
                {equipeForm.map((membro, index) => (
                  <div key={index} className="grid grid-cols-1 items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 p-2 md:grid-cols-[1fr_150px_auto_auto]">
                    <select
                      disabled={selectedProjeto && !isEditing}
                      value={membro.funcionarioId}
                      onChange={(e) => setEquipeForm((prev) => prev.map((item, itemIndex) =>
                        itemIndex === index ? { ...item, funcionarioId: e.target.value } : item,
                      ))}
                      className="min-w-0 rounded-md border border-slate-300 bg-white p-2 text-sm"
                    >
                      <option value="">Selecione...</option>
                      {funcionarios.map((funcionario) => (
                        <option key={funcionario.id} value={funcionario.id}>
                          {funcionario.nome}
                        </option>
                      ))}
                    </select>
                    <select
                      disabled={selectedProjeto && !isEditing}
                      value={membro.papel}
                      onChange={(e) => setEquipeForm((prev) => prev.map((item, itemIndex) =>
                        itemIndex === index ? { ...item, papel: e.target.value } : item,
                      ))}
                      className="rounded-md border border-slate-300 bg-white p-2 text-xs font-semibold"
                    >
                      <option value="LIDER_EQUIPE">Supervisor técnico / Líder da equipe</option>
                      <option value="TECNICO">Técnico</option>
                    </select>
                    <label className="flex items-center gap-1 text-[10px] font-bold text-slate-600">
                      <input
                        type="radio"
                        name="responsavel-principal"
                        disabled={selectedProjeto && !isEditing}
                        checked={membro.responsavelPrincipal}
                        onChange={() => setEquipeForm((prev) => prev.map((item, itemIndex) => ({
                          ...item,
                          responsavelPrincipal: itemIndex === index,
                        })))}
                      />
                      Principal
                    </label>
                    {(!selectedProjeto || isEditing) && equipeForm.length > 1 && (
                      <button
                        type="button"
                        title="Remover membro"
                        onClick={() => setEquipeForm((prev) => prev.filter((_, itemIndex) => itemIndex !== index))}
                        className="rounded-md p-2 text-red-600 hover:bg-red-50"
                      >
                        <Trash2 size={15} />
                      </button>
                    )}
                  </div>
                ))}
                <p className="text-xs text-slate-400">
                  Todos os técnicos da equipe visualizarão as OS no Portal Técnico. O principal recebe alertas operacionais.
                </p>
              </div>

              {/* CAMPO ADICIONADO AQUI: Aparece apenas na criação */}
              {!selectedProjeto && (
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase">
                    Nome da Comarca (opcional)
                  </label>
                  <input
                    type="text"
                    value={formData.nomeComarcaVinculada}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        nomeComarcaVinculada: e.target.value,
                      })
                    }
                    className="w-full mt-1 p-2 border rounded-lg text-sm bg-slate-50"
                    placeholder="Se vazio, usaremos o nome do cliente do contrato"
                  />
                  <p className="text-xs text-slate-400 mt-1">
                    Esse projeto será automaticamente listado em Gestão de
                    Comarcas.
                  </p>
                </div>
              )}

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
