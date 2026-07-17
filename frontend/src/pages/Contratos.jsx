import { useState, useEffect, useMemo } from "react";
import {
  Plus,
  Eye,
  Edit,
  Save,
  X,
  FileText,
  Search,
  SlidersHorizontal,
  RefreshCw,
  FolderArchive,
  User,
  Calendar,
  DollarSign,
  Archive,
  RotateCcw,
} from "lucide-react";
import api from "../services/api";

const STATUS_OPTIONS = [
  { value: "ATIVO", label: "Ativo" },
  { value: "EM_NEGOCIACAO", label: "Em Negociação" },
  { value: "RENOVACAO_PENDENTE", label: "Renovação Pendente" },
  { value: "SUSPENSO", label: "Suspenso" },
  { value: "ENCERRADO", label: "Encerrado" },
];

export default function Contratos() {
  const [contratos, setContratos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedContrato, setSelectedContrato] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [showFiltrosAvancados, setShowFiltrosAvancados] = useState(false);
  const [incluirArquivados, setIncluirArquivados] = useState(false);

  // 💥 1. FILTROS CORE (Principais)
  const [recorrencia, setRecorrencia] = useState("");
  const [segmento, setSegmento] = useState("");
  const [statusSelecionados, setStatusSelecionados] = useState([]);

  // 💥 AUTOCOMPLETE PREDITIVO DE CLIENTES (Multi-seleção)
  const [buscaCliente, setBuscaCliente] = useState("");
  const [clientesSelecionados, setClientesSelecionados] = useState([]);
  const [showAutocomplete, setShowAutocomplete] = useState(false);

  // 💥 2. FILTROS AVANÇADOS
  const [filterContrato, setFilterContrato] = useState("");
  const [dataInicio, setDataInicio] = useState("");
  const [dataFim, setDataFim] = useState("");
  const [valorMin, setValorMin] = useState("");
  const [valorMax, setValorMax] = useState("");
  const [gestor, setGestor] = useState("");

  // Estado do formulário atualizado com os novos campos operacionais
  const [formData, setFormData] = useState({
    cliente: "",
    contrato: "",
    vigenciaInicio: "",
    vigenciaFim: "",
    valorGlobal: "",
    status: "ATIVO",
    escopo: "",
    recorrencia: "MENSAL",
    segmento: "PRIVADO",
    gestorResponsavel: "",
  });

  // Extrai dinamicamente a lista de sugestões com base nos clientes que já existem na base
  const listaSugestoesClientes = useMemo(() => {
    if (!buscaCliente.trim()) return [];
    const clientesUnicos = [
      ...new Set(contratos.map((c) => c.cliente).filter(Boolean)),
    ];
    return clientesUnicos.filter(
      (nome) =>
        nome.toLowerCase().includes(buscaCliente.toLowerCase()) &&
        !clientesSelecionados.includes(nome),
    );
  }, [buscaCliente, contratos, clientesSelecionados]);

  // ⚡ ENGINE DE DEBOUNCE ATUALIZADA: Monitora todos os novos filtros core e avançados
  useEffect(() => {
    setLoading(true);
    const delayDebounce = setTimeout(() => {
      carregarContratos();
    }, 350);

    return () => clearTimeout(delayDebounce);
  }, [
    clientesSelecionados,
    filterContrato,
    statusSelecionados,
    recorrencia,
    segmento,
    dataInicio,
    dataFim,
    valorMin,
    valorMax,
    gestor,
    incluirArquivados,
  ]);

  const carregarContratos = async () => {
    try {
      const res = await api.get("/contratos", {
        params: {
          cliente:
            clientesSelecionados.length > 0
              ? clientesSelecionados.join(",")
              : null,
          contrato: filterContrato.trim() || null,
          status:
            statusSelecionados.length > 0 ? statusSelecionados.join(",") : null,
          recorrencia: recorrencia || null,
          segmento: segmento || null,
          gestor: gestor.trim() || null,
          dataInicio: dataInicio || null,
          dataFim: dataFim || null,
          valorMin: valorMin || null,
          valorMax: valorMax || null,
          incluirArquivados,
        },
      });
      setContratos(res.data || []);
    } catch (err) {
      console.error("Erro ao carregar contratos filtrados", err);
    } finally {
      setLoading(false);
    }
  };

  const alterarArquivamento = async (contratoAtual) => {
    try {
      if (contratoAtual.arquivado) {
        await api.patch(`/contratos/${contratoAtual.id}/restaurar`);
      } else {
        const motivo = window.prompt("Informe o motivo para arquivar este contrato:");
        if (!motivo?.trim()) return;
        await api.patch(`/contratos/${contratoAtual.id}/arquivar`, {
          usuario: "Paulo Morais",
          motivo: motivo.trim(),
        });
      }
      carregarContratos();
    } catch (err) {
      alert(err.response?.data?.erro || "Não foi possível alterar o arquivamento do contrato.");
    }
  };

  const handleLimparFiltros = () => {
    setClientesSelecionados([]);
    setBuscaCliente("");
    setFilterContrato("");
    setStatusSelecionados([]);
    setRecorrencia("");
    setSegmento("");
    setDataInicio("");
    setDataFim("");
    setValorMin("");
    setValorMax("");
    setGestor("");
  };

  const toggleStatus = (val) => {
    setStatusSelecionados((prev) =>
      prev.includes(val) ? prev.filter((s) => s !== val) : [...prev, val],
    );
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
        recorrencia: contrato.recorrencia || "MENSAL",
        segmento: contrato.segmento || "PRIVADO",
        gestorResponsavel: contrato.gestorResponsavel || "",
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
        recorrencia: "MENSAL",
        segmento: "PRIVADO",
        gestorResponsavel: "",
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
    <div className="space-y-6" onClick={() => setShowAutocomplete(false)}>
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-black text-slate-800 flex items-center gap-2 tracking-tight">
            <FileText size={32} className="text-blue-600" /> Contratos
          </h1>
          <p className="text-slate-500 text-sm mt-0.5">
            Gerenciamento completo de minutas e vigências operacionais
          </p>
        </div>
        <div className="flex items-center gap-3">
          <label className="flex items-center gap-2 text-xs font-bold text-slate-600">
            <input
              type="checkbox"
              checked={incluirArquivados}
              onChange={(event) => setIncluirArquivados(event.target.checked)}
            />
            Mostrar arquivados
          </label>
          <button
            onClick={() => handleOpenModal(null, false)}
            className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2.5 rounded-lg font-bold flex items-center gap-2 shadow-sm transition-colors text-sm"
          >
            <Plus size={18} /> Novo Contrato
          </button>
        </div>
      </div>

      {/* 🛠️ ARQUITETURA DE FILTROS CORE E AVANÇADOS REESTRUTURADA 💥 */}
      <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm space-y-4">
        {/* FILTROS CORE (Principais) */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {/* Autocomplete de Cliente / Empresa */}
          <div className="relative">
            <label className="text-[11px] font-black text-slate-400 uppercase tracking-wider block mb-1">
              Empresa / Cliente
            </label>
            <div className="relative">
              <Search
                size={14}
                className="absolute left-3 top-3 text-slate-400"
              />
              <input
                type="text"
                placeholder="Buscar ou selecionar..."
                value={buscaCliente}
                onFocus={() => setShowAutocomplete(true)}
                onChange={(e) => setBuscaCliente(e.target.value)}
                onClick={(e) => e.stopPropagation()}
                className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-lg text-xs bg-slate-50/50 outline-none focus:ring-2 focus:ring-blue-500 font-semibold"
              />
            </div>
            {showAutocomplete && listaSugestoesClientes.length > 0 && (
              <ul
                className="absolute edit-dropdown z-30 w-full mt-1 bg-white border border-slate-200 rounded-lg shadow-xl max-h-40 overflow-y-auto text-xs font-bold text-slate-700 divide-y"
                onClick={(e) => e.stopPropagation()}
              >
                {listaSugestoesClientes.map((nome) => (
                  <li
                    key={nome}
                    onClick={() => {
                      setClientesSelecionados([...clientesSelecionados, nome]);
                      setBuscaCliente("");
                      setShowAutocomplete(false);
                    }}
                    className="p-2.5 hover:bg-blue-50 cursor-pointer transition-colors"
                  >
                    {nome}
                  </li>
                ))}
              </ul>
            )}
          </div>

          {/* Dropdown: Tipo de Recorrência */}
          <div>
            <label className="text-[11px] font-black text-slate-400 uppercase tracking-wider block mb-1">
              Tipo de Recorrência
            </label>
            <select
              value={recorrencia}
              onChange={(e) => setRecorrencia(e.target.value)}
              className="w-full p-2 border border-slate-200 rounded-lg text-xs bg-slate-50/50 outline-none font-bold text-slate-700"
            >
              <option value="">Todos os formatos</option>
              <option value="MENSAL">Mensal (Recorrente)</option>
              <option value="FIXO">Fixo (Escopo Fechado)</option>
            </select>
          </div>

          {/* Dropdown: Segmento do Cliente */}
          <div>
            <label className="text-[11px] font-black text-slate-400 uppercase tracking-wider block mb-1">
              Segmento do Cliente
            </label>
            <select
              value={segmento}
              onChange={(e) => setSegmento(e.target.value)}
              className="w-full p-2 border border-slate-200 rounded-lg text-xs bg-slate-50/50 outline-none font-bold text-slate-700"
            >
              <option value="">Todos os segmentos</option>
              <option value="PUBLICO">Público (Licitações)</option>
              <option value="PRIVADO">Privado (B2B)</option>
            </select>
          </div>

          {/* Multi-seleção de Status por Pills */}
          <div>
            <label className="text-[11px] font-black text-slate-400 uppercase tracking-wider block mb-1">
              Status do Contrato
            </label>
            <div className="flex flex-wrap gap-1 p-1 bg-slate-50 border border-slate-200 rounded-lg max-h-[34px] overflow-y-auto">
              {STATUS_OPTIONS.map((opt) => {
                const ativo = statusSelecionados.includes(opt.value);
                return (
                  <button
                    type="button"
                    key={opt.value}
                    onClick={() => toggleStatus(opt.value)}
                    className={`px-2 py-0.5 text-[10px] font-black rounded transition-all ${
                      ativo
                        ? "bg-blue-600 text-white shadow-sm"
                        : "bg-white text-slate-600 border border-slate-200 hover:bg-slate-100"
                    }`}
                  >
                    {opt.label}
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        {/* Accordion de Filtros Avançados */}
        {showFiltrosAvancados && (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-3 border-t border-slate-100 animate-fade-in text-xs font-semibold text-slate-600">
            <div>
              <label className="text-[11px] font-black text-slate-400 uppercase block mb-1 flex items-center gap-1">
                <Search size={12} /> Nº do Contrato
              </label>
              <input
                type="text"
                placeholder="Filtrar por código ou número..."
                value={filterContrato}
                onChange={(e) => setFilterContrato(e.target.value)}
                className="w-full p-2 border border-slate-200 rounded-lg text-xs bg-slate-50/30 outline-none"
              />
            </div>
            <div>
              <label className="text-[11px] font-black text-slate-400 uppercase block mb-1 flex items-center gap-1">
                <Calendar size={12} /> Janela de Vigência
              </label>
              <div className="grid grid-cols-2 gap-2">
                <input
                  type="date"
                  value={dataInicio}
                  onChange={(e) => setDataInicio(e.target.value)}
                  className="p-2 border border-slate-200 rounded-lg text-xs outline-none bg-slate-50/30"
                />
                <input
                  type="date"
                  value={dataFim}
                  onChange={(e) => setDataFim(e.target.value)}
                  className="p-2 border border-slate-200 rounded-lg text-xs outline-none bg-slate-50/30"
                />
              </div>
            </div>
            <div>
              <label className="text-[11px] font-black text-slate-400 uppercase block mb-1 flex items-center gap-1">
                <DollarSign size={12} /> Faixa de Custo Global (R$)
              </label>
              <div className="grid grid-cols-2 gap-2">
                <input
                  type="number"
                  placeholder="Mínimo"
                  value={valorMin}
                  onChange={(e) => setValorMin(e.target.value)}
                  className="p-2 border border-slate-200 rounded-lg text-xs outline-none bg-slate-50/30"
                />
                <input
                  type="number"
                  placeholder="Máximo"
                  value={valorMax}
                  onChange={(e) => setValorMax(e.target.value)}
                  className="p-2 border border-slate-200 rounded-lg text-xs outline-none bg-slate-50/30"
                />
              </div>
            </div>
            <div className="md:col-span-3">
              <label className="text-[11px] font-black text-slate-400 uppercase block mb-1 flex items-center gap-1">
                <User size={12} /> Gestor / Responsável Interno
              </label>
              <input
                type="text"
                placeholder="Digitar nome do gestor de contas..."
                value={gestor}
                onChange={(e) => setGestor(e.target.value)}
                className="w-full p-2 border border-slate-200 rounded-lg text-xs bg-slate-50/30 outline-none"
              />
            </div>
          </div>
        )}

        {/* 🏷️ NOVO SUB-PAINEL: EXIBIÇÃO DE FILTROS ATIVOS (TAGS COGNITIVAS) */}
        <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-slate-100 text-xs font-bold text-slate-500">
          <div className="flex flex-wrap items-center gap-1.5">
            <span className="text-[10px] text-slate-400 uppercase font-black tracking-wide">
              Filtros Ativos:
            </span>
            {recorrencia && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 bg-blue-50 border border-blue-100 text-blue-700 text-[10px] rounded-full">
                Recorrência: {recorrencia}{" "}
                <X
                  size={10}
                  className="cursor-pointer"
                  onClick={() => setRecorrencia("")}
                />
              </span>
            )}
            {segmento && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 bg-blue-50 border border-blue-100 text-blue-700 text-[10px] rounded-full">
                Segmento: {segmento}{" "}
                <X
                  size={10}
                  className="cursor-pointer"
                  onClick={() => setSegmento("")}
                />
              </span>
            )}
            {gestor && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 bg-blue-50 border border-blue-100 text-blue-700 text-[10px] rounded-full">
                Gestor: {gestor}{" "}
                <X
                  size={10}
                  className="cursor-pointer"
                  onClick={() => setGestor("")}
                />
              </span>
            )}
            {clientesSelecionados.map((cl) => (
              <span
                key={cl}
                className="inline-flex items-center gap-1 px-2 py-0.5 bg-slate-900 text-white text-[10px] rounded-full"
              >
                {cl}{" "}
                <X
                  size={10}
                  className="cursor-pointer text-slate-400 hover:text-white"
                  onClick={() =>
                    setClientesSelecionados((prev) =>
                      prev.filter((x) => x !== cl),
                    )
                  }
                />
              </span>
            ))}
            {statusSelecionados.map((st) => (
              <span
                key={st}
                className="inline-flex items-center gap-1 px-2 py-0.5 bg-amber-50 border border-amber-200 text-amber-800 text-[10px] rounded-full"
              >
                {st}{" "}
                <X
                  size={10}
                  className="cursor-pointer"
                  onClick={() => toggleStatus(st)}
                />
              </span>
            ))}
            {!recorrencia &&
              !segmento &&
              !gestor &&
              clientesSelecionados.length === 0 &&
              statusSelecionados.length === 0 &&
              !filterContrato &&
              !dataInicio &&
              !dataFim &&
              !valorMin &&
              !valorMax && (
                <span className="text-slate-400 italic font-medium text-[11px]">
                  Listando base completa (Sem restrições)
                </span>
              )}
          </div>

          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setShowFiltrosAvancados(!showFiltrosAvancados)}
              className="flex items-center gap-1 hover:text-blue-600 transition-colors"
            >
              <SlidersHorizontal size={13} />
              {showFiltrosAvancados
                ? "Ocultar Parâmetros Avançados"
                : "Expandir Parâmetros Avançados"}
            </button>
            <button
              type="button"
              onClick={handleLimparFiltros}
              className="flex items-center gap-1 text-rose-600 hover:text-rose-700 transition-colors"
            >
              <RefreshCw size={11} /> Limpar Filtros
            </button>
          </div>
        </div>
      </div>

      {/* Condicional de Carregamento e Empty State */}
      {loading ? (
        <div className="text-center py-12 bg-white rounded-xl border p-6 text-slate-500 font-medium">
          <RefreshCw className="w-8 h-8 text-blue-600 animate-spin mx-auto mb-2" />
          Sincronizando filtros com o Postgres...
        </div>
      ) : contratos.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-xl border border-dashed border-slate-300 max-w-sm mx-auto">
          <FolderArchive size={40} className="text-slate-300 mx-auto mb-3" />
          <h3 className="text-sm font-bold text-slate-700">
            Nenhum contrato localizado
          </h3>
          <p className="text-xs text-slate-400 max-w-xs mx-auto mt-1 mb-4 px-4">
            Tente modificar ou resetar os filtros para encontrar os registros
            desejados.
          </p>
          <button
            onClick={handleLimparFiltros}
            className="bg-slate-900 text-white text-xs font-bold px-4 py-2 rounded-lg"
          >
            Resetar Busca
          </button>
        </div>
      ) : (
        /* Tabela de Contratos */
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50/70 border-b border-slate-200 text-xs font-bold text-slate-500 uppercase">
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
                <tr
                  key={c.id}
                  className={`hover:bg-slate-50/50 transition-colors ${c.arquivado ? "opacity-60" : ""}`}
                >
                  <td className="p-4 font-semibold text-slate-800">
                    {c.cliente}
                  </td>
                  <td className="p-4 font-mono text-xs text-slate-500">
                    {c.contrato}
                  </td>
                  <td className="p-4 text-xs text-slate-600">
                    {c.vigenciaInicio
                      ? `${c.vigenciaInicio} até ${c.vigenciaFim || "---"}`
                      : "---"}
                  </td>
                  <td className="p-4 font-semibold text-slate-900">
                    {formatCurrency(c.valorGlobal)}
                  </td>
                  <td className="p-4">
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${
                        c.arquivado
                          ? "bg-slate-200 text-slate-600 border border-slate-300"
                          : c.status === "ATIVO"
                          ? "bg-green-50 text-green-700 border border-green-200"
                          : "bg-slate-100 text-slate-600 border border-slate-200"
                      }`}
                    >
                      {c.arquivado ? "ARQUIVADO" : c.status || "ATIVO"}
                    </span>
                  </td>
                  <td className="p-4 flex justify-center gap-1.5">
                    <button
                      onClick={() => handleOpenModal(c, false)}
                      className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
                    >
                      <Eye size={16} />
                    </button>
                    <button
                      onClick={() => handleOpenModal(c, true)}
                      disabled={c.arquivado}
                      className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                    >
                      <Edit size={16} />
                    </button>
                    <button
                      onClick={() => alterarArquivamento(c)}
                      title={c.arquivado ? "Restaurar contrato" : "Arquivar contrato"}
                      className={`p-2 rounded-lg transition-colors ${c.arquivado ? "text-emerald-600 hover:bg-emerald-50" : "text-red-600 hover:bg-red-50"}`}
                    >
                      {c.arquivado ? <RotateCcw size={16} /> : <Archive size={16} />}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* MODAL UNIFICADO: NOVO / EDITAR / VISUALIZAR */}
      {modalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-xl border w-full max-w-2xl overflow-hidden flex flex-col">
            <div className="p-5 bg-slate-900 text-white flex justify-between items-center">
              <h2 className="text-base font-bold">
                {isEditing
                  ? "✏️ Editar Contrato"
                  : selectedContrato
                    ? "📄 Detalhes do Contrato"
                    : "📂 Novo Contrato"}
              </h2>
              <button
                onClick={() => setModalOpen(false)}
                className="text-slate-400 hover:text-white"
              >
                <X size={20} />
              </button>
            </div>

            <form
              onSubmit={handleSave}
              className="p-6 space-y-4 overflow-y-auto max-h-[75vh]"
            >
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase tracking-wide">
                    Cliente *
                  </label>
                  <input
                    type="text"
                    disabled={selectedContrato && !isEditing}
                    value={formData.cliente}
                    onChange={(e) =>
                      setFormData({ ...formData, cliente: e.target.value })
                    }
                    className="w-full mt-1 p-2 border border-slate-200 rounded-lg text-sm bg-slate-50 disabled:opacity-70 outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase tracking-wide">
                    Número do Contrato *
                  </label>
                  <input
                    type="text"
                    disabled={selectedContrato && !isEditing}
                    value={formData.contrato}
                    onChange={(e) =>
                      setFormData({ ...formData, contrato: e.target.value })
                    }
                    className="w-full mt-1 p-2 border border-slate-200 rounded-lg text-sm bg-slate-50 disabled:opacity-70 outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase tracking-wide">
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
                    className="w-full mt-1 p-2 border border-slate-200 rounded-lg text-sm bg-slate-50 disabled:opacity-70 outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase tracking-wide">
                    Vigência Fim
                  </label>
                  <input
                    type="date"
                    disabled={selectedContrato && !isEditing}
                    value={formData.vigenciaFim}
                    onChange={(e) =>
                      setFormData({ ...formData, vigenciaFim: e.target.value })
                    }
                    className="w-full mt-1 p-2 border border-slate-200 rounded-lg text-sm bg-slate-50 disabled:opacity-70 outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase tracking-wide">
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
                    className="w-full mt-1 p-2 border border-slate-200 rounded-lg text-sm bg-slate-50 disabled:opacity-70 outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Ex: 150000.00"
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase tracking-wide">
                    Status
                  </label>
                  <select
                    disabled={selectedContrato && !isEditing}
                    value={formData.status}
                    onChange={(e) =>
                      setFormData({ ...formData, status: e.target.value })
                    }
                    className="w-full mt-1 p-2 border border-slate-200 rounded-lg text-sm bg-slate-50 disabled:opacity-70 outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="ATIVO">ATIVO</option>
                    <option value="INATIVO">INATIVO</option>
                    <option value="SUSPENSO">SUSPENSO</option>
                  </select>
                </div>

                {/* 💥 NOVOS CAMPOS OPERACIONAIS NO FORMULÁRIO DO MODAL */}
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase tracking-wide">
                    Recorrência
                  </label>
                  <select
                    disabled={selectedContrato && !isEditing}
                    value={formData.recorrencia}
                    onChange={(e) =>
                      setFormData({ ...formData, recorrencia: e.target.value })
                    }
                    className="w-full mt-1 p-2 border border-slate-200 rounded-lg text-sm bg-slate-50 disabled:opacity-70 outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="MENSAL">MENSAL</option>
                    <option value="FIXO">FIXO</option>
                  </select>
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-600 uppercase tracking-wide">
                    Segmento
                  </label>
                  <select
                    disabled={selectedContrato && !isEditing}
                    value={formData.segmento}
                    onChange={(e) =>
                      setFormData({ ...formData, segmento: e.target.value })
                    }
                    className="w-full mt-1 p-2 border border-slate-200 rounded-lg text-sm bg-slate-50 disabled:opacity-70 outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="PRIVADO">PRIVADO (B2B)</option>
                    <option value="PUBLICO">PÚBLICO</option>
                  </select>
                </div>
                <div className="md:col-span-2">
                  <label className="text-xs font-bold text-slate-600 uppercase tracking-wide">
                    Gestor do Contrato
                  </label>
                  <input
                    type="text"
                    disabled={selectedContrato && !isEditing}
                    value={formData.gestorResponsavel}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        gestorResponsavel: e.target.value,
                      })
                    }
                    placeholder="Nome do colaborador responsável..."
                    className="w-full mt-1 p-2 border border-slate-200 rounded-lg text-sm bg-slate-50 disabled:opacity-70 outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-600 uppercase tracking-wide">
                  Escopo do Contrato
                </label>
                <textarea
                  rows="3"
                  disabled={selectedContrato && !isEditing}
                  value={formData.escopo}
                  onChange={(e) =>
                    setFormData({ ...formData, escopo: e.target.value })
                  }
                  className="w-full mt-1 p-2 border border-slate-200 rounded-lg text-sm bg-slate-50 disabled:opacity-70 outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Descreva as metas e obrigações..."
                ></textarea>
              </div>

              {(!selectedContrato || isEditing) && (
                <div className="flex justify-end pt-4 border-t border-slate-100">
                  <button
                    type="submit"
                    className="bg-blue-600 text-white px-5 py-2 rounded-lg font-bold flex items-center gap-2 hover:bg-blue-700 shadow-sm text-sm"
                  >
                    <Save size={16} /> Salvar Alterações
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
