import React, { useState, useEffect, useMemo } from "react";
import {
  Search,
  X,
  Filter,
  SlidersHorizontal,
  Calendar,
  DollarSign,
  User,
  FolderArchive,
  RefreshCw,
  Star,
} from "lucide-react";

// MOCK DE EMPRESAS PARA O AUTOCOMPLETE
const MOCK_EMPRESAS = [
  { id: 1, nome: "KGM Lan" },
  { id: 2, nome: "Natal Tech Infra" },
  { id: 3, nome: "Campos Operações" },
  { id: 4, nome: "Brasil Telecom" },
  { id: 5, nome: "Sudeste Fibra" },
  { id: 6, nome: "Tech Solutions" },
];

const STATUS_OPTIONS = [
  {
    value: "ATIVO",
    label: "Ativo",
    color: "bg-green-100 text-green-800 border-green-200",
  },
  {
    value: "EM_NEGOCIACAO",
    label: "Em Negociação",
    color: "bg-yellow-100 text-yellow-800 border-yellow-200",
  },
  {
    value: "RENOVACAO_PENDENTE",
    label: "Renovação Pendente",
    color: "bg-purple-100 text-purple-800 border-purple-200",
  },
  {
    value: "SUSPENSO",
    label: "Suspenso",
    color: "bg-rose-100 text-rose-800 border-rose-200",
  },
  {
    value: "ENCERRADO",
    label: "Encerrado",
    color: "bg-gray-100 text-gray-800 border-gray-200",
  },
];

export default function FiltrosContratos({ onFiltrar, totalResultados = 0 }) {
  // Estados dos Filtros Core
  const [recorrencia, setRecorrencia] = useState("");
  const [segmento, setSegmento] = useState("");
  const [statusSelecionados, setStatusSelecionados] = useState([]);

  // Estados do Autocomplete de Empresas
  const [buscaEmpresa, setBuscaEmpresa] = useState("");
  const [empresasSelecionadas, setEmpresasSelecionadas] = useState([]);
  const [showAutocomplete, setShowAutocomplete] = useState(false);

  // Estados dos Filtros Avançados
  const [showAvancados, setShowAvancados] = useState(false);
  const [isMobileOpen, setIsMobileOpen] = useState(false);
  const [dataInicio, setDataInicio] = useState("");
  const [dataFim, setDataFim] = useState("");
  const [valorMin, setValorMin] = useState("");
  const [valorMax, setValorMax] = useState("");
  const [gestor, setGestor] = useState("");

  // Sugestões do Autocomplete
  const sugestoesEmpresa = useMemo(() => {
    if (!buscaEmpresa.trim()) return [];
    return MOCK_EMPRESAS.filter(
      (emp) =>
        emp.nome.toLowerCase().includes(buscaEmpresa.toLowerCase()) &&
        !empresasSelecionadas.some((sel) => sel.id === emp.id),
    );
  }, [buscaEmpresa, empresasSelecionadas]);

  const handleLimparTodos = () => {
    setRecorrencia("");
    setSegmento("");
    setStatusSelecionados([]);
    setBuscaEmpresa("");
    setEmpresasSelecionadas([]);
    setDataInicio("");
    setDataFim("");
    setValorMin("");
    setValorMax("");
    setGestor("");
  };

  // Dispara a busca estruturada sempre que um filtro mudar (Debounce simplificado)
  useEffect(() => {
    const params = {
      recorrencia,
      segmento,
      gestor,
      dataInicio,
      dataFim,
      valorMin,
      valorMax,
      status: statusSelecionados,
      empresas: empresasSelecionadas.map((e) => e.id),
    };
    onFiltrar(params);
  }, [
    recorrencia,
    segmento,
    statusSelecionados,
    empresasSelecionadas,
    dataInicio,
    dataFim,
    valorMin,
    valorMax,
    gestor,
  ]);

  const toggleStatus = (val) => {
    setStatusSelecionados((prev) =>
      prev.includes(val) ? prev.filter((item) => item !== val) : [...prev, val],
    );
  };

  const removerEmpresa = (id) => {
    setEmpresasSelecionadas((prev) => prev.filter((e) => e.id !== id));
  };

  // Renderiza a estrutura interna dos inputs para reutilizar no Desktop e Mobile
  const RenderInputsFiltro = () => (
    <div className="space-y-6">
      {/* Grid Core */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div>
          <label className="text-xs font-bold text-slate-500 uppercase tracking-wider block mb-1">
            Recorrência
          </label>
          <select
            value={recorrencia}
            onChange={(e) => setRecorrencia(e.target.value)}
            className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Todos os tipos</option>
            <option value="MENSAL">Mensal (Recorrente)</option>
            <option value="FIXO">Fixo (Escopo Fechado)</option>
          </select>
        </div>

        <div>
          <label className="text-xs font-bold text-slate-500 uppercase tracking-wider block mb-1">
            Segmento
          </label>
          <select
            value={segmento}
            onChange={(e) => setSegmento(e.target.value)}
            className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Todos os segmentos</option>
            <option value="PUBLICO">Público (Licitações)</option>
            <option value="PRIVADO">Privado (B2B)</option>
          </select>
        </div>

        {/* Autocomplete Empresa */}
        <div className="relative">
          <label className="text-xs font-bold text-slate-500 uppercase tracking-wider block mb-1">
            Empresa / Cliente
          </label>
          <div className="relative">
            <Search
              size={16}
              className="absolute left-3 top-3.5 text-slate-400"
            />
            <input
              type="text"
              value={buscaEmpresa}
              onFocus={() => setShowAutocomplete(true)}
              onChange={(e) => setBuscaEmpresa(e.target.value)}
              placeholder="Digitar nome da empresa..."
              className="w-full pl-9 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          {showAutocomplete && sugestoesEmpresa.length > 0 && (
            <ul className="absolute z-30 w-full mt-1 bg-white border border-slate-200 rounded-lg shadow-xl max-h-48 overflow-y-auto divide-y divide-slate-100">
              {sugestoesEmpresa.map((emp) => (
                <li
                  key={emp.id}
                  onClick={() => {
                    setEmpresasSelecionadas([...empresasSelecionadas, emp]);
                    setBuscaEmpresa("");
                    setShowAutocomplete(false);
                  }}
                  className="p-3 text-sm hover:bg-blue-50 cursor-pointer text-slate-700 font-medium transition-colors"
                >
                  {emp.nome}
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* Multi-select Status Dropdown / Row */}
        <div>
          <label className="text-xs font-bold text-slate-500 uppercase tracking-wider block mb-1">
            Status do Contrato
          </label>
          <div className="flex flex-wrap gap-1.5 p-1 bg-slate-50 border border-slate-200 rounded-lg max-h-[42px] overflow-y-auto">
            {STATUS_OPTIONS.map((opt) => {
              const ativo = statusSelecionados.includes(opt.value);
              return (
                <button
                  type="button"
                  key={opt.value}
                  onClick={() => toggleStatus(opt.value)}
                  className={`px-2 py-1 text-[11px] font-bold rounded-md transition-all border ${
                    ativo
                      ? "bg-blue-600 text-white border-blue-700 shadow-sm"
                      : "bg-white text-slate-600 border-slate-200 hover:bg-slate-100"
                  }`}
                >
                  {opt.label}
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Botão Expansor Avançado (Desktop Only) */}
      <div className="hidden md:flex items-center justify-between border-t border-slate-100 pt-4">
        <button
          type="button"
          onClick={() => setShowAvancados(!showAvancados)}
          className="flex items-center gap-1.5 text-xs font-bold text-slate-600 hover:text-blue-600 transition-colors"
        >
          <SlidersHorizontal size={14} />
          {showAvancados
            ? "Ocultar Filtros Avançados"
            : "Exibir Filtros Avançados"}
        </button>
      </div>

      {/* Seção Filtros Avançados */}
      {(showAvancados || isMobileOpen) && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-2 border-t border-slate-100 md:border-none animate-fade-in">
          <div>
            <label className="text-xs font-bold text-slate-500 uppercase tracking-wider block mb-1 flex items-center gap-1">
              <Calendar size={12} /> Período de Vigência
            </label>
            <div className="grid grid-cols-2 gap-2">
              <input
                type="date"
                value={dataInicio}
                onChange={(e) => setDataInicio(e.target.value)}
                className="p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs outline-none focus:ring-2 focus:ring-blue-500"
              />
              <input
                type="date"
                value={dataFim}
                onChange={(e) => setDataFim(e.target.value)}
                className="p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          <div>
            <label className="text-xs font-bold text-slate-500 uppercase tracking-wider block mb-1 flex items-center gap-1">
              <DollarSign size={12} /> Faixa de Valor (R$)
            </label>
            <div className="grid grid-cols-2 gap-2">
              <input
                type="number"
                placeholder="Min"
                value={valorMin}
                onChange={(e) => setValorMin(e.target.value)}
                className="p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs outline-none focus:ring-2 focus:ring-blue-500"
              />
              <input
                type="number"
                placeholder="Max"
                value={valorMax}
                onChange={(e) => setValorMax(e.target.value)}
                className="p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          <div>
            <label className="text-xs font-bold text-slate-500 uppercase tracking-wider block mb-1 flex items-center gap-1">
              <User size={12} /> Gestor do Contrato
            </label>
            <select
              value={gestor}
              onChange={(e) => setGestor(e.target.value)}
              className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Todos os gestores</option>
              <option value="1">Paulo Morais</option>
              <option value="2">Aline Silva</option>
              <option value="3">Rodrigo Lima</option>
            </select>
          </div>
        </div>
      )}
    </div>
  );

  return (
    <div
      className="bg-white rounded-xl border border-slate-200/80 shadow-sm p-6 mb-6"
      onClick={() => setShowAutocomplete(false)}
    >
      {/* Mobile Toggle Button */}
      <div className="flex md:hidden items-center justify-between mb-4">
        <span className="text-sm font-bold text-slate-800">
          {totalResultados} contratos encontrados
        </span>
        <button
          onClick={() => setIsMobileOpen(true)}
          className="flex items-center gap-2 bg-slate-900 text-white px-4 py-2 rounded-lg text-xs font-bold"
        >
          <Filter size={14} /> Filtrar Base
        </button>
      </div>

      {/* Renderizador Desktop */}
      <div className="hidden md:block">
        <RenderInputsFiltro />
      </div>

      {/* BARRA DE FILTROS ATIVOS (TAGS) */}
      <div className="flex flex-wrap items-center justify-between gap-3 mt-4 pt-4 border-t border-slate-100">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs text-slate-400 font-medium">
            Filtros ativos:
          </span>

          {recorrencia && (
            <span className="inline-flex items-center gap-1 px-2.5 py-1 bg-blue-50 border border-blue-100 text-blue-700 text-xs font-semibold rounded-full">
              Recorrência: {recorrencia}{" "}
              <X
                size={12}
                className="cursor-pointer"
                onClick={() => setRecorrencia("")}
              />
            </span>
          )}
          {segmento && (
            <span className="inline-flex items-center gap-1 px-2.5 py-1 bg-blue-50 border border-blue-100 text-blue-700 text-xs font-semibold rounded-full">
              Segmento: {segmento}{" "}
              <X
                size={12}
                className="cursor-pointer"
                onClick={() => setSegmento("")}
              />
            </span>
          )}
          {gestor && (
            <span className="inline-flex items-center gap-1 px-2.5 py-1 bg-blue-50 border border-blue-100 text-blue-700 text-xs font-semibold rounded-full">
              Gestor ID: {gestor}{" "}
              <X
                size={12}
                className="cursor-pointer"
                onClick={() => setGestor("")}
              />
            </span>
          )}
          {(dataInicio || dataFim) && (
            <span className="inline-flex items-center gap-1 px-2.5 py-1 bg-blue-50 border border-blue-100 text-blue-700 text-xs font-semibold rounded-full">
              Vigência Modificada{" "}
              <X
                size={12}
                className="cursor-pointer"
                onClick={() => {
                  setDataInicio("");
                  setDataFim("");
                }}
              />
            </span>
          )}
          {(valorMin || valorMax) && (
            <span className="inline-flex items-center gap-1 px-2.5 py-1 bg-blue-50 border border-blue-100 text-blue-700 text-xs font-semibold rounded-full">
              Filtro de Custo{" "}
              <X
                size={12}
                className="cursor-pointer"
                onClick={() => {
                  setValorMin("");
                  setValorMax("");
                }}
              />
            </span>
          )}

          {empresasSelecionadas.map((emp) => (
            <span
              key={emp.id}
              className="inline-flex items-center gap-1 px-2.5 py-1 bg-slate-900 text-white text-xs font-semibold rounded-full"
            >
              {emp.nome}{" "}
              <X
                size={12}
                className="cursor-pointer text-slate-300 hover:text-white"
                onClick={() => removerEmpresa(emp.id)}
              />
            </span>
          ))}

          {statusSelecionados.map((st) => (
            <span
              key={st}
              className="inline-flex items-center gap-1 px-2.5 py-1 bg-amber-50 border border-amber-200 text-amber-800 text-xs font-semibold rounded-full"
            >
              Status: {st}{" "}
              <X
                size={12}
                className="cursor-pointer"
                onClick={() => toggleStatus(st)}
              />
            </span>
          ))}

          {!recorrencia &&
            !segmento &&
            !gestor &&
            !dataInicio &&
            !dataFim &&
            !valorMin &&
            !valorMax &&
            empresasSelecionadas.length === 0 &&
            statusSelecionados.length === 0 && (
              <span className="text-xs text-slate-400 italic font-medium">
                Nenhum (Listando base completa)
              </span>
            )}
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            className="p-1.5 text-slate-400 hover:text-amber-500 rounded-lg transition-colors border border-transparent hover:border-slate-200"
            title="Salvar essa busca como favorita"
          >
            <Star size={16} />
          </button>
          <button
            type="button"
            onClick={handleLimparTodos}
            className="text-xs font-bold text-rose-600 hover:text-rose-700 flex items-center gap-1 transition-colors"
          >
            <RefreshCw size={12} /> Limpar Filtros
          </button>
        </div>
      </div>

      {/* GAVETA / DRAWER MOBILE (Responsividade) */}
      {isMobileOpen && (
        <div className="fixed inset-0 z-50 flex justify-end md:hidden">
          <div
            className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm"
            onClick={() => setIsMobileOpen(false)}
          />
          <div className="relative w-full max-w-sm bg-white h-full p-6 shadow-2xl flex flex-col justify-between overflow-y-auto animate-slide-in">
            <div className="space-y-6">
              <div className="flex justify-between items-center border-b border-slate-100 pb-3">
                <h3 className="font-black text-slate-800 text-lg flex items-center gap-2">
                  <Filter className="text-blue-600" size={18} /> Filtrar
                  Contratos
                </h3>
                <button
                  onClick={() => setIsMobileOpen(false)}
                  className="text-slate-400 p-1"
                >
                  <X size={22} />
                </button>
              </div>
              <RenderInputsFiltro />
            </div>
            <button
              onClick={() => setIsMobileOpen(false)}
              className="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-3 rounded-xl text-sm shadow-md mt-6 tracking-wide"
            >
              Aplicar Filtros Operacionais
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

// COMPONENTE DO EMPTY STATE INTEGRADO
export function EmptyStateContratos({ onReset }) {
  return (
    <div className="text-center py-16 px-4 bg-white rounded-xl border border-dashed border-slate-300 shadow-inner max-w-md mx-auto my-8 animate-fade-in">
      <div className="bg-slate-50 p-4 rounded-full w-14 h-14 flex items-center justify-center mx-auto mb-4 border border-slate-100 text-slate-400">
        <FolderArchive size={28} />
      </div>
      <h3 className="text-base font-black text-slate-800 mb-1">
        Apagão nos resultados!
      </h3>
      <p className="text-xs text-slate-500 max-w-xs mx-auto mb-5 leading-relaxed">
        Nenhum contrato ativo, suspenso ou encerrado bate com essa combinação de
        filtros. Tente flexibilizar os valores ou as datas informadas.
      </p>
      <button
        onClick={onReset}
        className="bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold px-5 py-2.5 rounded-lg transition shadow-sm"
      >
        Resetar Parâmetros de Busca
      </button>
    </div>
  );
}
