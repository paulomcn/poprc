import { useEffect, useState } from "react";
import { AlertCircle, Briefcase, DollarSign, TrendingDown, TrendingUp } from "lucide-react";
import api from "../services/api";
import Alert from "../components/Alert";
import LoadingSpinner from "../components/LoadingSpinner";

export default function PainelFinanceiro() {
  const [projetos, setProjetos] = useState([]);
  const [projetoId, setProjetoId] = useState("");
  const [dados, setDados] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    api
      .get("/projetos")
      .then((response) => {
        const ativos = (response.data || []).filter((projeto) => !projeto.arquivado);
        setProjetos(ativos);
        setProjetoId(ativos[0]?.id ? String(ativos[0].id) : "");
        if (!ativos.length) setLoading(false);
      })
      .catch(() => {
        setError("Não foi possível carregar os projetos.");
        setLoading(false);
      });
  }, []);

  useEffect(() => {
    if (!projetoId) {
      setDados(null);
      return;
    }
    setLoading(true);
    api
      .get(`/relatorios/projeto/${projetoId}/lucratividade`)
      .then((response) => {
        setDados(response.data);
        setError(null);
      })
      .catch(() => setError("Não foi possível carregar o relatório do projeto."))
      .finally(() => setLoading(false));
  }, [projetoId]);

  const moeda = (valor) =>
    new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(valor || 0);

  const projetoSelecionado = projetos.find((projeto) => String(projeto.id) === projetoId);
  const resultadoPositivo = Number(dados?.lucroBruto || 0) >= 0;

  return (
    <div className="mx-auto max-w-[1400px] space-y-6">
      <header className="flex flex-col gap-4 border-b border-slate-200 pb-5 md:flex-row md:items-end md:justify-between">
        <div>
          <p className="text-xs font-bold uppercase text-emerald-700">Gestão financeira</p>
          <h1 className="mt-1 text-3xl font-bold text-slate-900">Lucratividade por Projeto</h1>
          <p className="mt-1 text-sm text-slate-500">Receitas e despesas efetivamente vinculadas à obra.</p>
        </div>
        <label className="block w-full md:w-96">
          <span className="mb-1.5 flex items-center gap-1.5 text-xs font-bold uppercase text-slate-500">
            <Briefcase size={14} /> Projeto analisado
          </span>
          <select
            value={projetoId}
            onChange={(event) => setProjetoId(event.target.value)}
            className="h-10 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm text-slate-700 outline-none focus:ring-2 focus:ring-blue-500"
          >
            {projetos.length === 0 && <option value="">Nenhum projeto disponível</option>}
            {projetos.map((projeto) => (
              <option key={projeto.id} value={projeto.id}>
                Projeto #{projeto.id} - {projeto.contrato?.contrato || "Sem contrato"}
              </option>
            ))}
          </select>
        </label>
      </header>

      {error && <Alert type="error" message={error} onClose={() => setError(null)} />}
      {!projetoId && !error && (
        <div className="rounded-lg border border-dashed border-slate-300 bg-white p-10 text-center text-sm text-slate-500">
          Cadastre um projeto para iniciar a análise financeira.
        </div>
      )}
      {loading && <LoadingSpinner />}

      {!loading && dados && (
        <>
          {!dados.custoMateriaisDisponivel && (
            <div className="flex gap-3 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
              <AlertCircle size={19} className="shrink-0" />
              <p>Resultado parcial: custos de materiais ainda não fazem parte deste cálculo. Receita e viagens já usam somente registros vinculados ao projeto selecionado.</p>
            </div>
          )}

          <section className="grid grid-cols-1 gap-4 md:grid-cols-3">
            <div className="rounded-lg border border-slate-200 bg-white p-5">
              <div className="flex items-center justify-between text-slate-500">
                <p className="text-xs font-bold uppercase">Receita vinculada</p>
                <DollarSign size={20} />
              </div>
              <p className="mt-4 text-3xl font-bold text-slate-900">{moeda(dados.totalFaturado)}</p>
              <p className="mt-2 text-xs text-slate-500">Medições atribuídas ao Projeto #{projetoSelecionado?.id}</p>
            </div>
            <div className="rounded-lg border border-rose-200 bg-rose-50 p-5">
              <div className="flex items-center justify-between text-rose-700">
                <p className="text-xs font-bold uppercase">Custos de viagens</p>
                <TrendingDown size={20} />
              </div>
              <p className="mt-4 text-3xl font-bold text-slate-900">{moeda(dados.totalCustoViagens)}</p>
              <p className="mt-2 text-xs text-slate-500">Prestações de contas concluídas</p>
            </div>
            <div className={`rounded-lg border p-5 ${resultadoPositivo ? "border-emerald-200 bg-emerald-50" : "border-red-200 bg-red-50"}`}>
              <div className={`flex items-center justify-between ${resultadoPositivo ? "text-emerald-700" : "text-red-700"}`}>
                <p className="text-xs font-bold uppercase">Resultado parcial</p>
                <TrendingUp size={20} />
              </div>
              <p className="mt-4 text-3xl font-bold text-slate-900">{moeda(dados.lucroBruto)}</p>
              <p className="mt-2 text-xs text-slate-500">Margem parcial de {dados.margemLucro || 0}%</p>
            </div>
          </section>

          <section className="rounded-lg border border-slate-200 bg-white p-5">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h2 className="font-bold text-slate-900">Leitura do resultado</h2>
                <p className="mt-1 text-sm text-slate-500">Situação calculada com os custos atualmente disponíveis.</p>
              </div>
              <span className={`w-fit rounded border px-3 py-1.5 text-xs font-bold ${
                dados.saudeFinanceira === "LUCRO_SAUDAVEL"
                  ? "border-emerald-200 bg-emerald-50 text-emerald-700"
                  : dados.saudeFinanceira === "ALERTA_MARGEM_BAIXA"
                    ? "border-amber-200 bg-amber-50 text-amber-700"
                    : "border-red-200 bg-red-50 text-red-700"
              }`}>
                {(dados.saudeFinanceira || "SEM_MOVIMENTACAO").replaceAll("_", " ")}
              </span>
            </div>
          </section>
        </>
      )}
    </div>
  );
}
