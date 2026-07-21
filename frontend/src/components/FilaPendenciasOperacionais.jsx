import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { AlertTriangle, ChevronRight, Clock, ListTodo, RefreshCw } from "lucide-react";
import api from "../services/api";

const prioridadeClasses = {
  CRITICA: "border-rose-200 bg-rose-50 text-rose-700",
  ALTA: "border-amber-200 bg-amber-50 text-amber-700",
  NORMAL: "border-slate-200 bg-slate-50 text-slate-600",
};

const prioridadeClassesDark = {
  CRITICA: "border-rose-500/30 bg-rose-950/30 text-rose-300",
  ALTA: "border-amber-500/30 bg-amber-950/30 text-amber-300",
  NORMAL: "border-slate-700 bg-slate-950/40 text-slate-300",
};

const formatarPrazo = (valor) => {
  if (!valor) return "Sem prazo";
  const data = new Date(valor);
  return Number.isNaN(data.getTime()) ? "Sem prazo" : data.toLocaleString("pt-BR");
};

export default function FilaPendenciasOperacionais({
  area,
  funcionarioId,
  titulo = "Fila operacional",
  limite = 6,
  dark = false,
}) {
  const [pendencias, setPendencias] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const carregar = useCallback(async () => {
    try {
      setLoading(true);
      const response = await api.get("/pendencias-operacionais", {
        params: {
          area: area || undefined,
          funcionarioId: funcionarioId || undefined,
        },
      });
      setPendencias(response.data || []);
      setError("");
    } catch (err) {
      setPendencias([]);
      setError(err.response?.data?.erro || "Não foi possível carregar as pendências.");
    } finally {
      setLoading(false);
    }
  }, [area, funcionarioId]);

  useEffect(() => {
    carregar();
    const atualizarAoFocar = () => carregar();
    window.addEventListener("focus", atualizarAoFocar);
    return () => window.removeEventListener("focus", atualizarAoFocar);
  }, [carregar]);

  const visiveis = pendencias.slice(0, limite);
  const container = dark
    ? "border-slate-800 bg-slate-900 text-slate-100"
    : "border-slate-200 bg-white text-slate-900";
  const secundaria = dark ? "text-slate-400" : "text-slate-500";
  const divisor = dark ? "divide-slate-800" : "divide-slate-100";

  return (
    <section className={`overflow-hidden rounded-lg border ${container}`}>
      <div className={`flex items-center justify-between gap-4 border-b px-4 py-3 ${dark ? "border-slate-800" : "border-slate-200"}`}>
        <div className="flex min-w-0 items-center gap-3">
          <ListTodo className="h-5 w-5 shrink-0 text-blue-500" />
          <div className="min-w-0">
            <h2 className="truncate text-sm font-bold">{titulo}</h2>
            <p className={`text-xs ${secundaria}`}>{pendencias.length} {pendencias.length === 1 ? "pendência ativa" : "pendências ativas"}</p>
          </div>
        </div>
        <button type="button" onClick={carregar} disabled={loading} className={`rounded p-2 ${dark ? "hover:bg-slate-800" : "hover:bg-slate-100"}`} title="Atualizar fila">
          <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
        </button>
      </div>

      {error ? (
        <div className="flex items-center gap-2 px-4 py-4 text-xs text-rose-500">
          <AlertTriangle className="h-4 w-4" /> {error}
        </div>
      ) : !loading && visiveis.length === 0 ? (
        <p className={`px-4 py-5 text-sm ${secundaria}`}>Nenhuma pendência para esta área.</p>
      ) : (
        <div className={`divide-y ${divisor}`}>
          {visiveis.map((item) => (
            <div key={item.id} className="flex flex-col gap-3 px-4 py-3 md:flex-row md:items-center">
              <span className={`w-fit rounded border px-2 py-1 text-[10px] font-black uppercase ${dark ? prioridadeClassesDark[item.prioridade] : prioridadeClasses[item.prioridade]}`}>
                {item.prioridade}
              </span>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
                  <p className="text-sm font-bold">{item.titulo}</p>
                  {item.numeroOs && <span className={`text-xs font-semibold ${secundaria}`}>{item.numeroOs}</span>}
                </div>
                <p className={`mt-1 line-clamp-2 text-xs ${secundaria}`}>{item.descricao}</p>
                <div className={`mt-1.5 flex flex-wrap gap-x-4 gap-y-1 text-[11px] ${secundaria}`}>
                  {item.nomeObra && <span>{item.nomeObra}</span>}
                  {item.responsavelNome && <span>Responsável: {item.responsavelNome}</span>}
                  <span className="inline-flex items-center gap-1"><Clock className="h-3 w-3" /> {formatarPrazo(item.deadline)}</span>
                </div>
              </div>
              <Link to={item.rota} className={`inline-flex h-9 shrink-0 items-center justify-center gap-1 rounded-lg px-3 text-xs font-bold ${dark ? "bg-blue-600 text-white hover:bg-blue-500" : "bg-blue-50 text-blue-700 hover:bg-blue-100"}`}>
                {item.acao} <ChevronRight className="h-4 w-4" />
              </Link>
            </div>
          ))}
        </div>
      )}
      {pendencias.length > limite && (
        <p className={`border-t px-4 py-2 text-xs ${dark ? "border-slate-800 text-slate-400" : "border-slate-100 text-slate-500"}`}>
          Mais {pendencias.length - limite} pendências não exibidas neste resumo.
        </p>
      )}
    </section>
  );
}
