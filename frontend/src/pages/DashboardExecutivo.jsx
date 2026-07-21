import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  AlertTriangle,
  Briefcase,
  Building2,
  Calendar,
  CheckCircle,
  ChevronRight,
  ClipboardList,
  DollarSign,
  Filter,
  Layers,
  Plane,
  RefreshCw,
  TrendingUp,
} from "lucide-react";
import api from "../services/api";
import LoadingSpinner from "../components/LoadingSpinner";
import Alert from "../components/Alert";
import FilaPendenciasOperacionais from "../components/FilaPendenciasOperacionais";

const indicadorClasses = {
  neutro: "border-slate-200 bg-white text-slate-700",
  azul: "border-blue-200 bg-blue-50 text-blue-700",
  vermelho: "border-red-200 bg-red-50 text-red-700",
  verde: "border-emerald-200 bg-emerald-50 text-emerald-700",
};

function BarraDistribuicao({ itens, total }) {
  const base = Math.max(total, 1);

  return (
    <div className="space-y-4">
      <div className="flex h-3 w-full overflow-hidden rounded bg-slate-100">
        {itens.map((item) => (
          <div
            key={item.label}
            className={item.corBarra}
            style={{ width: `${(item.valor / base) * 100}%` }}
            title={`${item.label}: ${item.valor}`}
          />
        ))}
      </div>
      <div className="grid grid-cols-2 gap-x-5 gap-y-3 sm:grid-cols-4">
        {itens.map((item) => (
          <div key={item.label} className="min-w-0">
            <div className="flex items-center gap-2">
              <span className={`h-2.5 w-2.5 shrink-0 rounded-sm ${item.corBarra}`} />
              <span className="truncate text-xs font-medium text-slate-500">
                {item.label}
              </span>
            </div>
            <p className="mt-1 text-xl font-bold text-slate-900">{item.valor}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

function Indicador({ icon: Icon, label, valor, detalhe, variante = "neutro" }) {
  return (
    <div className={`min-h-32 rounded-lg border p-5 ${indicadorClasses[variante]}`}>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs font-bold uppercase text-slate-500">{label}</p>
          <p className="mt-3 text-3xl font-bold text-slate-900">{valor}</p>
        </div>
        <Icon size={22} aria-hidden="true" />
      </div>
      <p className="mt-3 text-xs text-slate-500">{detalhe}</p>
    </div>
  );
}

export default function DashboardExecutivo() {
  const [data, setData] = useState(null);
  const [contratos, setContratos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [atualizando, setAtualizando] = useState(false);
  const [error, setError] = useState(null);
  const [filtroContrato, setFiltroContrato] = useState("");
  const [dataInicio, setDataInicio] = useState("");
  const [dataFim, setDataFim] = useState("");

  useEffect(() => {
    api
      .get("/contratos")
      .then((response) => setContratos(response.data || []))
      .catch(() => setError("Não foi possível carregar os contratos para o filtro."));
  }, []);

  const fetchDashboardData = useCallback(async (exibirLoading = false) => {
    if (dataInicio && dataFim && dataInicio > dataFim) {
      setError("A data inicial não pode ser posterior à data final.");
      setLoading(false);
      return;
    }

    try {
      if (exibirLoading) setAtualizando(true);
      const response = await api.get("/dashboard/executivo", {
        params: {
          contrato: filtroContrato || undefined,
          inicio: dataInicio || undefined,
          fim: dataFim || undefined,
        },
      });
      setData(response.data);
      setError(null);
    } catch (err) {
      console.error(err);
      setError("Falha ao carregar os indicadores do painel executivo.");
    } finally {
      setLoading(false);
      setAtualizando(false);
    }
  }, [dataFim, dataInicio, filtroContrato]);

  useEffect(() => {
    const timeout = setTimeout(() => fetchDashboardData(), 250);
    return () => clearTimeout(timeout);
  }, [fetchDashboardData]);

  const formatCurrency = (value) =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
      maximumFractionDigits: 0,
    }).format(value || 0);

  const obrasAtivas =
    (data?.obrasEmVistoria || 0) +
    (data?.obrasEmInfraestrutura || 0) +
    (data?.obrasEmViradaRede || 0);
  const totalObras = obrasAtivas + (data?.totalComarcasConcluidas || 0);
  const percentualConclusao = totalObras
    ? Math.round(((data?.totalComarcasConcluidas || 0) / totalObras) * 100)
    : 0;
  const totalFinanceiro = Number(data?.valorTotalContratado || 0);
  const faturado = Number(data?.valorFaturado || 0);
  const percentualFaturado = totalFinanceiro
    ? Math.min(100, Math.round((faturado / totalFinanceiro) * 100))
    : 0;

  const etapas = useMemo(
    () => [
      { label: "Vistoria", valor: data?.obrasEmVistoria || 0, corBarra: "bg-amber-500" },
      { label: "Infraestrutura", valor: data?.obrasEmInfraestrutura || 0, corBarra: "bg-cyan-600" },
      { label: "Virada de rede", valor: data?.obrasEmViradaRede || 0, corBarra: "bg-blue-600" },
      { label: "Concluídas", valor: data?.totalComarcasConcluidas || 0, corBarra: "bg-emerald-600" },
    ],
    [data],
  );

  const statusOrdens = useMemo(
    () => [
      { label: "Abertas", valor: data?.ordensAbertas || 0, corBarra: "bg-slate-400" },
      { label: "Em execução", valor: data?.ordensEmExecucao || 0, corBarra: "bg-blue-600" },
      { label: "Concluídas", valor: data?.ordensConcluidas || 0, corBarra: "bg-emerald-600" },
    ],
    [data],
  );

  if (loading) return <LoadingSpinner />;

  return (
    <div className="mx-auto max-w-[1500px] space-y-6">
      <header className="flex flex-col gap-4 border-b border-slate-200 pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-xs font-bold uppercase text-blue-600">Visão operacional</p>
          <h1 className="mt-1 text-3xl font-bold text-slate-900">Dashboard Executivo</h1>
          <p className="mt-1 text-sm text-slate-500">
            Contratos, prazos e execução das obras em uma única leitura.
          </p>
        </div>
        <button
          type="button"
          onClick={() => fetchDashboardData(true)}
          disabled={atualizando}
          className="inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-60"
        >
          <RefreshCw size={17} className={atualizando ? "animate-spin" : ""} />
          Atualizar dados
        </button>
      </header>

      <section className="grid grid-cols-1 gap-4 rounded-lg border border-slate-200 bg-white p-4 md:grid-cols-3">
        <label className="block">
          <span className="mb-1.5 flex items-center gap-1.5 text-xs font-bold uppercase text-slate-500">
            <Filter size={14} /> Contrato
          </span>
          <select
            value={filtroContrato}
            onChange={(event) => setFiltroContrato(event.target.value)}
            className="h-10 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm text-slate-700 outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Todos os contratos</option>
            {contratos.map((contrato) => (
              <option key={contrato.id} value={contrato.contrato}>
                {contrato.contrato} - {contrato.cliente}
              </option>
            ))}
          </select>
        </label>
        <label className="block">
          <span className="mb-1.5 flex items-center gap-1.5 text-xs font-bold uppercase text-slate-500">
            <Calendar size={14} /> Início do período
          </span>
          <input
            type="date"
            value={dataInicio}
            onChange={(event) => setDataInicio(event.target.value)}
            className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm text-slate-700 outline-none focus:ring-2 focus:ring-blue-500"
          />
        </label>
        <label className="block">
          <span className="mb-1.5 flex items-center gap-1.5 text-xs font-bold uppercase text-slate-500">
            <Calendar size={14} /> Fim do período
          </span>
          <input
            type="date"
            value={dataFim}
            onChange={(event) => setDataFim(event.target.value)}
            className="h-10 w-full rounded-lg border border-slate-300 px-3 text-sm text-slate-700 outline-none focus:ring-2 focus:ring-blue-500"
          />
        </label>
      </section>

      {error && <Alert type="error" message={error} />}

      <FilaPendenciasOperacionais titulo="Pendências por área responsável" limite={8} />

      {(data?.totalComarcasEmAtraso || 0) > 0 && (
        <section className="flex flex-col gap-3 rounded-lg border border-red-200 bg-red-50 p-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-start gap-3">
            <AlertTriangle className="mt-0.5 shrink-0 text-red-600" size={20} />
            <div>
              <p className="font-bold text-red-900">Atenção aos prazos</p>
              <p className="text-sm text-red-700">
                {data.totalComarcasEmAtraso} OS {data.totalComarcasEmAtraso === 1 ? "está atrasada" : "estão atrasadas"} e exige acompanhamento.
              </p>
            </div>
          </div>
          <Link to="/ordens-servico" className="inline-flex items-center gap-1 text-sm font-bold text-red-700 hover:text-red-900">
            Ver ordens <ChevronRight size={16} />
          </Link>
        </section>
      )}

      <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Indicador icon={Briefcase} label="Contratos ativos" valor={data?.contratosAtivos || 0} detalhe="Contratos vigentes no recorte" />
        <Indicador icon={Building2} label="Obras em andamento" valor={obrasAtivas} detalhe={`${totalObras} obras acompanhadas`} variante="azul" />
        <Indicador icon={AlertTriangle} label="OS atrasadas" valor={data?.totalComarcasEmAtraso || 0} detalhe={`${data?.ordensProximasPrazo || 0} vencem nas próximas 24h`} variante={(data?.totalComarcasEmAtraso || 0) > 0 ? "vermelho" : "neutro"} />
        <Indicador icon={CheckCircle} label="Obras concluídas" valor={data?.totalComarcasConcluidas || 0} detalhe={`${percentualConclusao}% do total acompanhado`} variante="verde" />
      </section>

      <section className="grid grid-cols-1 gap-6 xl:grid-cols-5">
        <div className="rounded-lg border border-slate-200 bg-white p-5 xl:col-span-3">
          <div className="mb-6 flex items-center justify-between gap-4">
            <div>
              <h2 className="font-bold text-slate-900">Fluxo das obras</h2>
              <p className="text-xs text-slate-500">Distribuição pelas etapas operacionais</p>
            </div>
            <Link to="/obras" className="inline-flex items-center gap-1 text-sm font-semibold text-blue-700 hover:text-blue-900">
              Gestão de Obras <ChevronRight size={16} />
            </Link>
          </div>
          <BarraDistribuicao itens={etapas} total={totalObras} />
        </div>

        <div className="rounded-lg border border-slate-200 bg-white p-5 xl:col-span-2">
          <div className="mb-6 flex items-center justify-between gap-4">
            <div>
              <h2 className="font-bold text-slate-900">Ordens de serviço</h2>
              <p className="text-xs text-slate-500">{data?.totalOrdensServico || 0} ordens no recorte</p>
            </div>
            <ClipboardList size={20} className="text-slate-500" />
          </div>
          <BarraDistribuicao itens={statusOrdens} total={data?.totalOrdensServico || 0} />
        </div>
      </section>

      <section className="grid grid-cols-1 gap-6 xl:grid-cols-3">
        <div className="rounded-lg border border-slate-200 bg-white p-5 xl:col-span-2">
          <div className="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <div className="flex items-center gap-2">
                <DollarSign size={20} className="text-emerald-700" />
                <h2 className="font-bold text-slate-900">Posição financeira</h2>
              </div>
              <p className="mt-4 text-3xl font-bold text-slate-900">{formatCurrency(data?.valorFaturado)}</p>
              <p className="mt-1 text-xs text-slate-500">faturado de {formatCurrency(data?.valorTotalContratado)}</p>
            </div>
            <div className="grid grid-cols-2 gap-6 text-sm sm:text-right">
              <div>
                <p className="text-xs font-medium text-slate-500">A receber</p>
                <p className="mt-1 font-bold text-amber-700">{formatCurrency(data?.valorPendenteFaturamento)}</p>
              </div>
              <div>
                <p className="text-xs font-medium text-slate-500">Viagens aprovadas</p>
                <p className="mt-1 font-bold text-slate-800">{formatCurrency(data?.custosAcumuladosViagem)}</p>
              </div>
            </div>
          </div>
          <div className="mt-6">
            <div className="mb-2 flex justify-between text-xs font-semibold text-slate-500">
              <span>Execução financeira</span>
              <span>{percentualFaturado}%</span>
            </div>
            <div className="h-3 overflow-hidden rounded bg-slate-100">
              <div className="h-full bg-emerald-600" style={{ width: `${percentualFaturado}%` }} />
            </div>
          </div>
        </div>

        <div className="rounded-lg border border-slate-200 bg-white p-5">
          <h2 className="font-bold text-slate-900">Acessos rápidos</h2>
          <div className="mt-4 divide-y divide-slate-100">
            {[
              { to: "/ordens-servico", icon: ClipboardList, label: "Ordens de Serviço" },
              { to: "/auditoria/tecnica", icon: Layers, label: "Auditoria de retirada/devolução" },
              { to: "/financeiro/faturamento", icon: TrendingUp, label: "Gestão de faturamento" },
              { to: "/logistica/viagens", icon: Plane, label: "Viagens e reembolsos" },
            ].map((item) => (
              <Link key={item.to} to={item.to} className="flex min-h-12 items-center gap-3 py-3 text-sm font-semibold text-slate-700 hover:text-blue-700">
                <item.icon size={18} className="text-slate-400" />
                <span className="flex-1">{item.label}</span>
                <ChevronRight size={16} />
              </Link>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
