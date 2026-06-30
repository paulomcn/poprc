import { useState, useEffect } from "react";
import {
  Briefcase,
  DollarSign,
  CheckCircle,
  AlertTriangle,
  Plane,
  Percent,
  TrendingUp,
  Filter,
  Calendar,
} from "lucide-react";
import api from "../services/api";
import LoadingSpinner from "../components/LoadingSpinner";
import Alert from "../components/Alert";

export default function DashboardExecutivo() {
  //  ️ CONTROLE DE PERFIL (Mude para 'CLIENTE' para testar a trava visual do prompt!)
  const [userRole, setUserRole] = useState("DIRETORIA");

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  //  ️ ESTADOS DOS FILTROS (Requisito do Prompt 4/4)
  const [filtroContrato, setFiltroContrato] = useState("");
  const [dataInicio, setDataInicio] = useState("");
  const [dataFim, setDataFim] = useState("");

  useEffect(() => {
    fetchDashboardData();
  }, [filtroContrato, dataInicio, dataFim]); // Recarrega se o filtro mudar

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      // Passa os filtros via query params para a API (opcional no seu backend atual)
      const response = await api.get("/dashboard/executivo", {
        params: { contrato: filtroContrato, inicio: dataInicio, fim: dataFim },
      });
      setData(response.data);
      setError(null);
    } catch (err) {
      setError("Falha ao carregar os indicadores estratégicos.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (value) => {
    return new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(value || 0);
  };

  if (loading) return <LoadingSpinner />;

  const total = data?.valorTotalContratado || 1;
  const faturadoPercent = ((data?.valorFaturado || 0) / total) * 100;

  return (
    <div className="space-y-8">
      {/* Header e Toggle de Perfil para testes de homologação */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-slate-50 p-4 rounded-xl border border-slate-200">
        <div>
          <h1 className="text-3xl font-bold text-slate-800 flex items-center gap-2">
            <TrendingUp className="text-slate-700" size={32} /> Dashboard
            Executivo
          </h1>
          <p className="text-slate-600 mt-1">
            Visão analítica macro do sistema
          </p>
        </div>

        {/* Simulador de Perfil para você testar na hora */}
        <div className="flex items-center gap-2 bg-white p-2 rounded-lg border shadow-sm w-fit">
          <span className="text-xs font-bold text-slate-500 uppercase px-2">
            Visualizar como:
          </span>
          <button
            onClick={() => setUserRole("DIRETORIA")}
            className={`px-3 py-1 text-xs font-bold rounded ${userRole === "DIRETORIA" ? "bg-slate-800 text-white" : "bg-slate-100 text-slate-600"}`}
          >
            Diretoria
          </button>
          <button
            onClick={() => setUserRole("CLIENTE")}
            className={`px-3 py-1 text-xs font-bold rounded ${userRole === "CLIENTE" ? "bg-blue-600 text-white" : "bg-slate-100 text-slate-600"}`}
          >
            Cliente
          </button>
        </div>
      </div>

      {/*  ️ BARRA DE FILTROS DO TOPO (Requisito da Parte 4/4) */}
      <div className="bg-white p-4 rounded-xl shadow-sm border border-slate-200 grid grid-cols-1 md:grid-cols-3 gap-4 items-end">
        <div className="space-y-1">
          <label className="text-xs font-bold text-slate-500 uppercase flex items-center gap-1">
            <Filter size={14} /> Filtrar por Contrato
          </label>
          <select
            value={filtroContrato}
            onChange={(e) => setFiltroContrato(e.target.value)}
            className="w-full text-sm px-3 py-2 border rounded-lg bg-slate-50 focus:outline-none focus:ring-2 focus:ring-slate-400"
          >
            <option value="">Todos os Contratos</option>
            <option value="CT-2026-001">Contrato TJ-RN</option>
            <option value="CT-2026-002">Contrato TRT-21</option>
          </select>
        </div>

        <div className="space-y-1">
          <label className="text-xs font-bold text-slate-500 uppercase flex items-center gap-1">
            <Calendar size={14} /> Data Início
          </label>
          <input
            type="date"
            value={dataInicio}
            onChange={(e) => setDataInicio(e.target.value)}
            className="w-full text-sm px-3 py-2 border rounded-lg bg-slate-50 focus:outline-none"
          />
        </div>

        <div className="space-y-1">
          <label className="text-xs font-bold text-slate-500 uppercase flex items-center gap-1">
            <Calendar size={14} /> Data Fim
          </label>
          <input
            type="date"
            value={dataFim}
            onChange={(e) => setDataFim(e.target.value)}
            className="w-full text-sm px-3 py-2 border rounded-lg bg-slate-50 focus:outline-none"
          />
        </div>
      </div>

      {error && <Alert type="error" message={error} />}

      {/* Grid Principal de KPIs */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {/* Card 1: Contratos (Visível para Ambos) */}
        <div className="bg-white p-6 rounded-xl shadow-md border border-slate-200 flex items-center justify-between">
          <div className="space-y-2">
            <p className="text-sm font-semibold text-slate-500 uppercase">
              Contratos Ativos
            </p>
            <h3 className="text-3xl font-bold text-slate-800">
              {data?.contratosAtivos}
            </h3>
          </div>
          <div className="p-4 bg-slate-100 text-slate-700 rounded-lg">
            <Briefcase size={24} />
          </div>
        </div>

        {/* Card 2: Valor Global (Visível para Ambos) */}
        <div className="bg-white p-6 rounded-xl shadow-md border border-slate-200 flex items-center justify-between">
          <div className="space-y-2">
            <p className="text-sm font-semibold text-slate-500 uppercase">
              Total Alocado
            </p>
            <h3 className="text-2xl font-bold text-slate-800">
              {formatCurrency(data?.valorTotalContratado)}
            </h3>
          </div>
          <div className="p-4 bg-green-50 text-green-700 rounded-lg">
            <DollarSign size={24} />
          </div>
        </div>

        {/*  ️ REGRAS DO PROMPT: ESCONDE SE FOR PERFIL 'CLIENTE' */}
        {userRole === "DIRETORIA" ? (
          <>
            {/* Card 3: Faturado Global (Só Diretoria) */}
            <div className="bg-white p-6 rounded-xl shadow-md border border-slate-200 flex items-center justify-between">
              <div className="space-y-2">
                <p className="text-sm font-semibold text-slate-500 uppercase">
                  Faturado Global RC
                </p>
                <h3 className="text-2xl font-bold text-green-600">
                  {formatCurrency(data?.valorFaturado)}
                </h3>
              </div>
              <div className="p-4 bg-green-100/50 text-green-600 rounded-lg">
                <Percent size={24} />
              </div>
            </div>

            {/* Card 4: Custos de Viagem (Só Diretoria) */}
            <div className="bg-white p-6 rounded-xl shadow-md border border-slate-200 flex items-center justify-between">
              <div className="space-y-2">
                <p className="text-sm font-semibold text-slate-500 uppercase">
                  Custos Viagem Internos
                </p>
                <h3 className="text-2xl font-bold text-red-600">
                  {formatCurrency(data?.custosAcumuladosViagem)}
                </h3>
              </div>
              <div className="p-4 bg-red-50 text-red-600 rounded-lg">
                <Plane size={24} />
              </div>
            </div>
          </>
        ) : (
          /* Placeholder visual para o Cliente não ver buraco na tela, mantendo o grid limpo */
          <div className="lg:col-span-2 bg-gradient-to-r from-blue-50 to-indigo-50 p-6 rounded-xl border border-blue-100 flex items-center justify-center">
            <p className="text-sm font-medium text-blue-700 text-center">
              {" "}
              Indicadores internos de rentabilidade e custos restritos à gestão
              RC.
            </p>
          </div>
        )}
      </div>

      {/* Seção Inferior: Saúde Financeira (Condicional) e Comarcas */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Gráfico/Barra de Progresso de Faturamento (Só Diretoria vê) */}
        <div
          className={`bg-white p-6 rounded-xl shadow-md border border-slate-200 lg:col-span-2 space-y-6 ${userRole !== "DIRETORIA" && "opacity-40 pointer-events-none"}`}
        >
          <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
            Saúde do Faturamento {userRole !== "DIRETORIA" && " (Restrito)"}
          </h2>

          <div className="space-y-2">
            <div className="flex justify-between text-sm font-medium">
              <span className="text-green-600">
                Faturado ({faturadoPercent.toFixed(1)}%)
              </span>
              <span className="text-amber-600">Pendente</span>
            </div>
            <div className="w-full bg-slate-100 h-5 rounded-full overflow-hidden flex border">
              <div
                style={{ width: `${faturadoPercent}%` }}
                className="bg-green-500 h-full"
              ></div>
              <div className="flex-1 bg-amber-400 h-full"></div>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4 pt-4 border-t text-sm">
            <div>
              <p className="text-xs font-semibold text-slate-400 uppercase">
                Realizado
              </p>
              <p className="font-bold text-slate-700">
                {userRole === "DIRETORIA"
                  ? formatCurrency(data?.valorFaturado)
                  : "---"}
              </p>
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-400 uppercase">
                A Receber
              </p>
              <p className="font-bold text-slate-700">
                {userRole === "DIRETORIA"
                  ? formatCurrency(data?.valorPendenteFaturamento)
                  : "---"}
              </p>
            </div>
          </div>
        </div>

        {/* Status de Comarcas / SLAs (Ambos Vêem - Foco total do Cliente) */}
        <div className="bg-white p-6 rounded-xl shadow-md border border-slate-200 space-y-6 flex flex-col justify-between">
          <div>
            <h2 className="text-lg font-bold text-slate-800 mb-4">
              Status de Entregas & SLA
            </h2>
            <div className="space-y-4">
              <div className="flex items-center justify-between p-3 bg-slate-50 border rounded-lg">
                <div className="flex items-center gap-3 text-slate-700 text-sm font-medium">
                  <CheckCircle className="text-green-500" size={20} />
                  <span>Comarcas Concluídas</span>
                </div>
                <span className="text-lg font-bold text-slate-800">
                  {data?.totalComarcasConcluidas}
                </span>
              </div>

              <div className="flex items-center justify-between p-3 bg-red-50/50 border rounded-lg">
                <div className="flex items-center gap-3 text-slate-700 text-sm font-medium">
                  <AlertTriangle className="text-red-500" size={20} />
                  <span>Comarcas em Atraso</span>
                </div>
                <span className="text-sm font-bold text-red-600 bg-red-100 px-2.5 py-0.5 rounded-full">
                  {data?.totalComarcasEmAtraso} Fora do Prazo
                </span>
              </div>
            </div>
          </div>

          <button
            onClick={fetchDashboardData}
            className="w-full mt-4 py-2.5 bg-slate-700 hover:bg-slate-800 text-white font-semibold text-sm rounded-lg transition-colors"
          >
            Atualizar Painel
          </button>
        </div>
      </div>
    </div>
  );
}
