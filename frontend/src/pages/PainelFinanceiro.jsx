import React, { useState, useEffect } from 'react';
import { DollarSign, TrendingUp, AlertCircle, Loader2 } from 'lucide-react';
import api from '../services/api';

export default function PainelFinanceiro() {
  const [dados, setDados] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function carregarLucratividade() {
      try {
        setLoading(true);
        // Bate no endpoint consolidado que criamos no Java
        const response = await api.get('/relatorios/projeto/1/lucratividade');
        setDados(response.data);
        setError(null);
      } catch (err) {
        setError('Não foi possível carregar o relatório financeiro.');
        console.error(err);
      } finally {
        setLoading(false);
      }
    }
    carregarLucratividade();
  }, []);

  const formatarMoeda = (valor) => 
    new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor || 0);

  if (loading) return (
    <div className="min-h-screen bg-slate-950 flex justify-center items-center">
      <Loader2 className="w-10 h-10 animate-spin text-indigo-500" />
    </div>
  );

  if (error || !dados) return (
    <div className="min-h-screen bg-slate-950 p-8 flex items-center justify-center">
      <div className="bg-rose-950/40 border border-rose-500/30 text-rose-300 p-6 rounded-xl flex items-center gap-3 max-w-lg">
        <AlertCircle className="w-6 h-6 shrink-0" />
        <p>{error || 'Erro desconhecido no servidor.'}</p>
      </div>
    </div>
  );

  const getBadgeSaude = (status) => {
    if (status === 'LUCRO_SAUDAVEL') return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
    if (status === 'ALERTA_MARGEM_BAIXA') return 'bg-amber-500/10 text-amber-400 border-amber-500/20';
    return 'bg-rose-500/10 text-rose-400 border-rose-500/20';
  };

  return (
    <div className="min-h-screen bg-slate-950 p-4 md:p-8 font-sans text-slate-100">
      <div className="max-w-7xl mx-auto space-y-8">
        
        <header>
          <h1 className="text-3xl font-black tracking-tight text-white">Dashboard de Lucratividade</h1>
          <p className="text-slate-400 text-sm mt-1">Valores financeiros registrados para {dados.nomeProjeto}</p>
        </header>

        {!dados.custoMateriaisDisponivel && (
          <div className="rounded-lg border border-amber-500/30 bg-amber-500/10 p-4 text-sm text-amber-200">
            Custos de materiais ainda não são controlados e não estão incluídos nos totais, no resultado ou na margem.
          </div>
        )}

        {/* KPIs vindos direto da matemática do Java */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
            <div className="flex justify-between items-start">
              <p className="text-slate-400 text-sm font-bold uppercase tracking-wider">Total Faturado</p>
              <div className="p-2 bg-indigo-500/10 rounded-lg"><DollarSign className="w-5 h-5 text-indigo-400" /></div>
            </div>
            <p className="text-3xl font-black mt-4">{formatarMoeda(dados.totalFaturado)}</p>
          </div>
          
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
            <div className="flex justify-between items-start">
              <p className="text-slate-400 text-sm font-bold uppercase tracking-wider">Custos Registrados</p>
              <div className="p-2 bg-rose-500/10 rounded-lg"><TrendingUp className="w-5 h-5 text-rose-400" /></div>
            </div>
            <p className="text-3xl font-black mt-4 text-rose-400">{formatarMoeda(dados.custoTotalAcumulado)}</p>
            <p className="mt-2 text-xs text-slate-500">Despesas com valores efetivamente informados</p>
          </div>

          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
            <div className="flex justify-between items-start">
              <p className="text-slate-400 text-sm font-bold uppercase tracking-wider">Resultado Parcial</p>
              <div className="p-2 bg-emerald-500/10 rounded-lg"><TrendingUp className="w-5 h-5 text-emerald-400" /></div>
            </div>
            <p className="text-3xl font-black mt-4 text-emerald-400">{formatarMoeda(dados.lucroBruto)}</p>
          </div>
        </div>

        {/* Card de Saúde Financeira */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-8 shadow-xl flex flex-col items-center justify-center text-center max-w-2xl mx-auto">
          <h2 className="text-slate-400 text-sm font-bold uppercase tracking-wider mb-4">Margem Parcial</h2>
          <div className="text-7xl font-black text-white mb-6">
            {dados.margemLucro}%
          </div>
          <span className={`px-6 py-2 rounded-full border text-sm font-black tracking-widest uppercase ${getBadgeSaude(dados.saudeFinanceira)}`}>
            {dados.saudeFinanceira ? dados.saudeFinanceira.replace(/_/g, ' ') : 'SEM MOVIMENTAÇÃO'}
          </span>
        </div>

      </div>
    </div>
  );
}
