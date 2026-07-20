import { useState, useEffect } from "react";
import {
  Bell,
  Mail,
  MessageSquare,
  Save,
  Play,
  ListChecks,
  Plus,
  Pencil,
  Power,
  CheckCircle2,
  Clock,
} from "lucide-react";
import api from "../services/api";
import Alert from "../components/Alert";

const atividadeFormInicial = {
  nome: "",
  categoria: "Geral",
  ordemExibicao: 0,
  ativo: true,
};

export default function ConfiguracaoNotificacoes() {
  const [settings, setSettings] = useState({
    emailGestor: "",
    whatsappGestor: "",
    alertaOsAtrasada: true,
    alertaEstoqueCritico: true,
    alertaContratoVencendo: true,
    antecedenciaOsHoras: 24,
    antecedenciaContratoDias: 30,
  });

  const [atividadesPadrao, setAtividadesPadrao] = useState([]);
  const [atividadeForm, setAtividadeForm] = useState(atividadeFormInicial);
  const [atividadeEditandoId, setAtividadeEditandoId] = useState(null);
  const [loadingTest, setLoadingTest] = useState(false);
  const [notificacoes, setNotificacoes] = useState([]);
  const [filtroNotificacoes, setFiltroNotificacoes] = useState("ATIVAS");
  const [canais, setCanais] = useState({
    emailHabilitado: false,
    emailRemetente: "",
    whatsappHabilitado: false,
    whatsappDetalhe: "",
  });
  const [successMessage, setSuccessMessage] = useState(null);
  const [errorMessage, setErrorMessage] = useState(null);

  useEffect(() => {
    loadDbSettings();
    loadAtividadesPadrao();
    loadNotificacoes();
    loadCanais();
  }, []);

  const showSuccess = (message) => {
    setSuccessMessage(message);
    setErrorMessage(null);
    setTimeout(() => setSuccessMessage(null), 3000);
  };

  const showError = (message) => {
    setErrorMessage(message);
    setSuccessMessage(null);
  };

  async function loadDbSettings() {
    try {
      const response = await api.get("/alertas/configuracoes");
      setSettings(response.data);
    } catch (err) {
      showError("Erro ao carregar configurações do servidor.");
    }
  }

  async function loadAtividadesPadrao() {
    try {
      const response = await api.get("/atividades-padrao");
      setAtividadesPadrao(response.data || []);
    } catch (err) {
      showError("Erro ao carregar atividades padrão.");
    }
  }

  async function loadNotificacoes() {
    try {
      const response = await api.get("/alertas/notificacoes");
      setNotificacoes(response.data || []);
    } catch (err) {
      showError("Erro ao carregar o histórico de notificações.");
    }
  }

  async function loadCanais() {
    try {
      const response = await api.get("/alertas/canais");
      setCanais(response.data);
    } catch (err) {
      showError("Erro ao consultar os canais de notificação.");
    }
  }

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    const numerico = ["antecedenciaOsHoras", "antecedenciaContratoDias"].includes(name);
    setSettings((prev) => ({ ...prev, [name]: numerico ? Number(value) : value }));
  };

  const handleToggleChange = (field) => {
    setSettings((prev) => ({ ...prev, [field]: !prev[field] }));
  };

  const handleAtividadeInputChange = (e) => {
    const { name, value } = e.target;
    setAtividadeForm((prev) => ({
      ...prev,
      [name]: name === "ordemExibicao" ? Number(value) : value,
    }));
  };

  const handleSaveSettings = async (e) => {
    e.preventDefault();
    try {
      await api.post("/alertas/configuracoes", settings);
      showSuccess("Configurações salvas no servidor com sucesso!");
    } catch (err) {
      showError("Falha ao salvar no banco de dados.");
    }
  };

  const handleSalvarAtividadePadrao = async (e) => {
    e.preventDefault();
    if (!atividadeForm.nome.trim()) {
      showError("Informe o nome da atividade padrão.");
      return;
    }

    try {
      if (atividadeEditandoId) {
        await api.put(`/atividades-padrao/${atividadeEditandoId}`, atividadeForm);
        showSuccess("Atividade padrão atualizada com sucesso!");
      } else {
        await api.post("/atividades-padrao", atividadeForm);
        showSuccess("Atividade padrão cadastrada com sucesso!");
      }
      setAtividadeForm(atividadeFormInicial);
      setAtividadeEditandoId(null);
      loadAtividadesPadrao();
    } catch (err) {
      showError("Falha ao salvar atividade padrão.");
    }
  };

  const handleEditarAtividade = (atividade) => {
    setAtividadeEditandoId(atividade.id);
    setAtividadeForm({
      nome: atividade.nome || "",
      categoria: atividade.categoria || "Geral",
      ordemExibicao: atividade.ordemExibicao || 0,
      ativo: atividade.ativo !== false,
    });
  };

  const handleCancelarEdicaoAtividade = () => {
    setAtividadeForm(atividadeFormInicial);
    setAtividadeEditandoId(null);
  };

  const handleToggleAtividade = async (atividadeId) => {
    try {
      await api.patch(`/atividades-padrao/${atividadeId}/toggle`);
      loadAtividadesPadrao();
    } catch (err) {
      showError("Falha ao alterar status da atividade padrão.");
    }
  };

  const handleTriggerManualAlerts = async () => {
    try {
      setLoadingTest(true);
      const response = await api.post("/alertas/disparar-todos");
      const criadas = response.data.notificacoesCriadas || 0;
      const encontradas = response.data.alertasEncontrados || 0;
      const enviados = response.data.emailsEnviados || 0;
      const falhos = response.data.emailsFalhos || 0;
      const resolvidas = response.data.notificacoesResolvidas || 0;
      const email = response.data.emailHabilitado
        ? `${enviados} e-mail(s) enviado(s)${falhos ? ` e ${falhos} com falha` : ""}`
        : "canal de e-mail desabilitado";
      showSuccess(`Varredura concluída: ${encontradas} alerta(s), ${criadas} nova(s), ${resolvidas} resolvida(s) e ${email}.`);
      await loadNotificacoes();
    } catch (err) {
      showError("Falha ao acionar o motor de alertas.");
    } finally {
      setLoadingTest(false);
    }
  };

  const handleMarcarComoLida = async (id) => {
    try {
      await api.patch(`/alertas/notificacoes/${id}/lida`);
      await loadNotificacoes();
    } catch (err) {
      showError("Falha ao marcar a notificação como lida.");
    }
  };

  const notificacoesAtivas = notificacoes.filter((notificacao) => notificacao.ativa !== false);
  const notificacoesFiltradas = filtroNotificacoes === "ATIVAS"
    ? notificacoesAtivas
    : notificacoes;

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-slate-800 flex items-center gap-2">
          <Bell className="text-slate-700" size={32} /> Configuração de
          Notificações
        </h1>
        {successMessage && (
          <div className="mt-4">
            <Alert type="success" message={successMessage} />
          </div>
        )}
        {errorMessage && (
          <div className="mt-4">
            <Alert type="error" message={errorMessage} />
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <form
          onSubmit={handleSaveSettings}
          className="bg-white p-6 rounded-xl shadow-md border border-slate-200 lg:col-span-2 space-y-6"
        >
          <h2 className="text-lg font-bold text-slate-800 border-b pb-3">
            Parâmetros de alertas
          </h2>
          <p className="text-sm text-slate-500">
            Os alertas sempre são registrados no sistema. O envio externo ocorre somente quando o canal estiver habilitado no servidor.
          </p>

          <div className="space-y-2">
            <label className="text-sm font-semibold text-slate-700 flex items-center gap-2">
              <Mail size={18} /> E-mail
              <span className={`ml-auto text-xs font-bold ${canais.emailHabilitado ? "text-emerald-600" : "text-slate-400"}`}>
                {canais.emailHabilitado ? "Habilitado" : "Desabilitado"}
              </span>
            </label>
            <input
              type="email"
              name="emailGestor"
              value={settings.emailGestor}
              onChange={handleInputChange}
              className="w-full px-4 py-2 border rounded-lg"
            />
            {canais.emailHabilitado && canais.emailRemetente && (
              <p className="text-xs text-slate-500">Remetente: {canais.emailRemetente}</p>
            )}
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold text-slate-700 flex items-center gap-2">
              <MessageSquare size={18} /> WhatsApp
              <span className="ml-auto text-xs font-bold text-slate-400">Indisponível</span>
            </label>
            <input
              type="text"
              name="whatsappGestor"
              value={settings.whatsappGestor}
              onChange={handleInputChange}
              disabled={!canais.whatsappHabilitado}
              placeholder="Aguardando integração com provedor"
              className="w-full px-4 py-2 border rounded-lg"
            />
            <p className="text-xs text-slate-500">{canais.whatsappDetalhe}</p>
          </div>

          <h2 className="text-lg font-bold text-slate-800 border-b pt-4 pb-3">
            Regras de Varredura
          </h2>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <label className="text-sm font-semibold text-slate-700">
                Avisar antes do deadline da OS
              </label>
              <div className="flex items-center gap-2">
                <input
                  type="number"
                  name="antecedenciaOsHoras"
                  min="1"
                  max="720"
                  required
                  value={settings.antecedenciaOsHoras ?? 24}
                  onChange={handleInputChange}
                  className="w-full rounded-lg border px-4 py-2"
                />
                <span className="text-sm text-slate-500">horas</span>
              </div>
            </div>
            <div className="space-y-2">
              <label className="text-sm font-semibold text-slate-700">
                Avisar antes do fim do contrato
              </label>
              <div className="flex items-center gap-2">
                <input
                  type="number"
                  name="antecedenciaContratoDias"
                  min="1"
                  max="365"
                  required
                  value={settings.antecedenciaContratoDias ?? 30}
                  onChange={handleInputChange}
                  className="w-full rounded-lg border px-4 py-2"
                />
                <span className="text-sm text-slate-500">dias</span>
              </div>
            </div>
          </div>

          {[
            "alertaOsAtrasada",
            "alertaEstoqueCritico",
            "alertaContratoVencendo",
          ].map((field) => (
            <div
              key={field}
              className="flex items-center justify-between p-4 bg-slate-50 rounded-xl border"
            >
              <span className="font-medium capitalize">
                {field.replace("alerta", "Alerta de ")}
              </span>
              <button
                type="button"
                onClick={() => handleToggleChange(field)}
                className={`w-12 h-6 rounded-full p-1 ${settings[field] ? "bg-blue-600" : "bg-slate-300"}`}
              >
                <div
                  className={`bg-white w-4 h-4 rounded-full transition-transform ${settings[field] ? "translate-x-6" : "translate-x-0"}`}
                />
              </button>
            </div>
          ))}

          <button
            type="submit"
            className="flex items-center gap-2 px-6 py-3 bg-blue-600 text-white font-semibold rounded-lg"
          >
            <Save size={18} /> Salvar Parâmetros
          </button>
        </form>

        <div className="bg-white p-6 rounded-xl shadow-md border border-slate-200 h-fit space-y-4">
          <h3 className="font-bold flex items-center gap-2">
            <Play className="text-green-600" size={18} /> Homologação
          </h3>
          <button
            onClick={handleTriggerManualAlerts}
            disabled={loadingTest}
            className="w-full py-3 bg-green-50 text-green-700 font-semibold rounded-lg disabled:opacity-60"
          >
            {loadingTest ? "Executando..." : "Disparar Varredura Manual"}
          </button>
        </div>
      </div>

      <section className="bg-white p-6 rounded-xl shadow-md border border-slate-200 space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-b border-slate-200 pb-4">
          <div>
            <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
              <Bell className="text-blue-600" size={20} /> Histórico de alertas
            </h2>
            <p className="text-sm text-slate-500 mt-1">Registros persistidos pela varredura de prazos, estoque e contratos.</p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <div className="inline-flex rounded-lg border border-slate-200 bg-slate-50 p-1">
              <button
                type="button"
                onClick={() => setFiltroNotificacoes("ATIVAS")}
                className={`px-3 py-1.5 text-xs font-bold rounded-md ${filtroNotificacoes === "ATIVAS" ? "bg-white text-blue-700 shadow-sm" : "text-slate-500"}`}
              >
                Ativas ({notificacoesAtivas.length})
              </button>
              <button
                type="button"
                onClick={() => setFiltroNotificacoes("TODAS")}
                className={`px-3 py-1.5 text-xs font-bold rounded-md ${filtroNotificacoes === "TODAS" ? "bg-white text-blue-700 shadow-sm" : "text-slate-500"}`}
              >
                Histórico ({notificacoes.length})
              </button>
            </div>
            <button type="button" onClick={loadNotificacoes} className="px-4 py-2 rounded-lg border border-slate-200 text-sm font-semibold text-slate-600 hover:bg-slate-50">
              Atualizar
            </button>
          </div>
        </div>

        <div className="space-y-2">
          {notificacoesFiltradas.length === 0 && (
            <p className="py-8 text-center text-sm text-slate-500">
              {filtroNotificacoes === "ATIVAS" ? "Nenhum alerta ativo." : "Nenhuma notificação registrada."}
            </p>
          )}
          {notificacoesFiltradas.map((notificacao) => (
            <div key={notificacao.id} className={`flex flex-col sm:flex-row sm:items-center gap-3 rounded-lg border p-4 ${notificacao.ativa === false ? "border-slate-200 bg-white" : notificacao.lidaEm ? "border-slate-200 bg-slate-50" : "border-amber-200 bg-amber-50"}`}>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <span className={`text-[10px] font-black uppercase px-2 py-1 rounded ${notificacao.severidade === "CRITICA" ? "bg-rose-100 text-rose-700" : notificacao.severidade === "ALERTA" ? "bg-amber-100 text-amber-700" : "bg-blue-100 text-blue-700"}`}>
                    {notificacao.severidade}
                  </span>
                  {notificacao.numeroOs && <span className="text-xs font-bold text-slate-600">{notificacao.numeroOs}</span>}
                  {notificacao.destinatarioNome && <span className="text-xs text-slate-500">Para: {notificacao.destinatarioNome}</span>}
                  <span className={`text-[10px] font-black uppercase px-2 py-1 rounded ${notificacao.ativa === false ? "bg-slate-100 text-slate-500" : "bg-emerald-100 text-emerald-700"}`}>
                    {notificacao.ativa === false ? "Resolvido" : "Ativo"}
                  </span>
                </div>
                <p className="mt-2 font-bold text-slate-800">{notificacao.titulo}</p>
                <p className="mt-1 text-sm text-slate-600">{notificacao.mensagem}</p>
                <p className="mt-2 flex items-center gap-1 text-xs text-slate-400">
                  <Clock size={13} /> {new Date(notificacao.criadaEm).toLocaleString("pt-BR")}
                </p>
                {notificacao.resolvidaEm && (
                  <p className="mt-1 text-xs text-slate-500">
                    Resolvido em {new Date(notificacao.resolvidaEm).toLocaleString("pt-BR")}
                    {notificacao.motivoResolucao ? `: ${notificacao.motivoResolucao}` : ""}
                  </p>
                )}
              </div>
              {!notificacao.lidaEm && (
                <button type="button" onClick={() => handleMarcarComoLida(notificacao.id)} className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg border border-emerald-200 px-3 py-2 text-xs font-bold text-emerald-700 hover:bg-emerald-50">
                  <CheckCircle2 size={16} /> Marcar como lida
                </button>
              )}
            </div>
          ))}
        </div>
      </section>

      <section className="bg-white p-6 rounded-xl shadow-md border border-slate-200 space-y-5">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-3 border-b border-slate-200 pb-4">
          <div>
            <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
              <ListChecks className="text-blue-600" size={20} />
              Atividades Padrão
            </h2>
            <p className="text-sm text-slate-500 mt-1">
              Estas opções aparecem como checklist fechado na página do técnico.
            </p>
          </div>
          {atividadeEditandoId && (
            <button
              type="button"
              onClick={handleCancelarEdicaoAtividade}
              className="text-sm font-semibold text-slate-500 hover:text-slate-800"
            >
              Cancelar edição
            </button>
          )}
        </div>

        <form
          onSubmit={handleSalvarAtividadePadrao}
          className="grid grid-cols-1 md:grid-cols-[minmax(0,1fr)_180px_120px_auto] gap-3 items-end"
        >
          <div className="space-y-1.5">
            <label className="text-xs font-bold uppercase text-slate-500">
              Nome da atividade
            </label>
            <input
              type="text"
              name="nome"
              value={atividadeForm.nome}
              onChange={handleAtividadeInputChange}
              className="w-full px-4 py-2.5 border rounded-lg text-sm"
              placeholder="Ex: Teste de conectividade"
              required
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-bold uppercase text-slate-500">
              Categoria
            </label>
            <input
              type="text"
              name="categoria"
              value={atividadeForm.categoria}
              onChange={handleAtividadeInputChange}
              className="w-full px-4 py-2.5 border rounded-lg text-sm"
              placeholder="Geral"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-bold uppercase text-slate-500">
              Ordem
            </label>
            <input
              type="number"
              min="0"
              name="ordemExibicao"
              value={atividadeForm.ordemExibicao}
              onChange={handleAtividadeInputChange}
              className="w-full px-4 py-2.5 border rounded-lg text-sm"
            />
          </div>

          <button
            type="submit"
            className="inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg"
          >
            {atividadeEditandoId ? <Save size={18} /> : <Plus size={18} />}
            {atividadeEditandoId ? "Salvar" : "Adicionar"}
          </button>
        </form>

        <div className="overflow-x-auto border border-slate-200 rounded-xl">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500">
              <tr>
                <th className="text-left px-4 py-3">Atividade</th>
                <th className="text-left px-4 py-3">Categoria</th>
                <th className="text-left px-4 py-3">Ordem</th>
                <th className="text-left px-4 py-3">Status</th>
                <th className="text-right px-4 py-3">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {atividadesPadrao.length === 0 && (
                <tr>
                  <td colSpan="5" className="px-4 py-8 text-center text-slate-500">
                    Nenhuma atividade padrão cadastrada.
                  </td>
                </tr>
              )}

              {atividadesPadrao.map((atividade) => (
                <tr key={atividade.id} className="hover:bg-slate-50">
                  <td className="px-4 py-3 font-semibold text-slate-800">
                    {atividade.nome}
                  </td>
                  <td className="px-4 py-3 text-slate-600">
                    {atividade.categoria || "Geral"}
                  </td>
                  <td className="px-4 py-3 text-slate-600">
                    {atividade.ordemExibicao || 0}
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={`inline-flex px-2.5 py-1 rounded-full text-xs font-bold ${
                        atividade.ativo
                          ? "bg-emerald-50 text-emerald-700"
                          : "bg-slate-100 text-slate-500"
                      }`}
                    >
                      {atividade.ativo ? "Ativa" : "Inativa"}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      <button
                        type="button"
                        onClick={() => handleEditarAtividade(atividade)}
                        className="p-2 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-100"
                        title="Editar atividade"
                      >
                        <Pencil size={16} />
                      </button>
                      <button
                        type="button"
                        onClick={() => handleToggleAtividade(atividade.id)}
                        className={`p-2 rounded-lg border ${
                          atividade.ativo
                            ? "border-rose-200 text-rose-600 hover:bg-rose-50"
                            : "border-emerald-200 text-emerald-600 hover:bg-emerald-50"
                        }`}
                        title={atividade.ativo ? "Desativar" : "Ativar"}
                      >
                        <Power size={16} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
