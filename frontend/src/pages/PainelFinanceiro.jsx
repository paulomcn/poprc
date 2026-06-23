import React, { useState, useEffect } from 'react';
import { DollarSign, TrendingUp, AlertCircle, Loader2, CheckCircle2 } from 'lucide-react';
import api from '../services/api';

export default function PainelFinanceiro() {
  const [dados, setDados] = useState(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState(false);

  useEffect(() => {
    async function carregarLucratividade() {
      try {
        const response = await api.get('/relatorios/projeto/1/lucratividade');
        setDados(response.data);
      } catch (error) {
        console.warn('Erro na API, usando mock para visualização');
        // Fallback pra você ver o layout funcionando
        setDados({
          projetoId: 1,
          nomeProjeto: "Projeto Técnico 1",
          totalFaturado: 13500.00,
          custoTotalAcumulado: 1750.00,
          lucroBruto: 11750.00,
          margemLucro: 87.04,
          saudeFinanceira: "LUCRO_SAUDAVEL"
        });
        setErro(true);
      } finally {
        setLoading(false);
      }
    }
    carregarLucratividade();
  }, []);

  const formatarMoeda = (valor) => 
    new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor);

  if (loading) return (
    <div className="min-h-screen bg-slate-950 flex justify-center items-center">
      <Loader2 className="w-10 h-10 animate-spin text-indigo-500" />
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
        
        <header className="mb-8">
          <h1 className="text-3xl font-black tracking-tight text-white">Dashboard de Lucratividade</h1>
          <p className="text-slate-400 text-sm mt-1">Visão financeira consolidada do {dados.nomeProjeto}</p>
        </header>

        {erro && (
          <div className="bg-rose-950/40 border border-rose-500/30 text-rose-300 p-4 rounded-xl flex items-center gap-3 text-sm">
            <AlertCircle className="w-5 h-5" />
            Exibindo dados simulados. A conexão com o backend falhou.
          </div>
        )}

        {/* KPIs */}
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
              <p className="text-slate-400 text-sm font-bold uppercase tracking-wider">Custo Acumulado</p>
              <div className="p-2 bg-rose-500/10 rounded-lg"><TrendingUp className="w-5 h-5 text-rose-400" /></div>
            </div>
            <p className="text-3xl font-black mt-4">{formatarMoeda(dados.custoTotalAcumulado)}</p>
          </div>

          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
            <div className="flex justify-between items-start">
              <p className="text-slate-400 text-sm font-bold uppercase tracking-wider">Lucro Líquido</p>
              <div className="p-2 bg-emerald-500/10 rounded-lg"><TrendingUp className="w-5 h-5 text-emerald-400" /></div>
            </div>
            <p className="text-3xl font-black mt-4 text-emerald-400">{formatarMoeda(dados.lucroBruto)}</p>
          </div>
        </div>

        {/* Card Central - Saúde Financeira e Histórico */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-8 shadow-xl flex flex-col items-center justify-center text-center">
            <h2 className="text-slate-400 text-sm font-bold uppercase tracking-wider mb-6">Saúde Financeira do Projeto</h2>
            <div className="text-7xl font-black text-white mb-6">
              {dados.margemLucro}%
            </div>
            <span className={`px-6 py-2 rounded-full border text-sm font-black tracking-widest uppercase ${getBadgeSaude(dados.saudeFinanceira)}`}>
              {dados.saudeFinanceira.replace(/_/g, ' ')}
            </span>
          </div>

          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
            <h2 className="text-slate-100 text-lg font-bold mb-6">Histórico Recente de Faturamento</h2>
            <div className="space-y-6 relative before:absolute before:inset-0 before:ml-5 before:-translate-x-px md:before:mx-auto md:before:translate-x-0 before:h-full before:w-0.5 before:bg-gradient-to-b before:from-transparent before:via-slate-700 before:to-transparent">
              
              <div className="relative flex items-center justify-between md:justify-normal md:odd:flex-row-reverse group is-active">
                <div className="flex items-center justify-center w-10 h-10 rounded-full border-4 border-slate-900 bg-emerald-500 text-white shadow shrink-0 md:order-1 md:group-odd:-translate-x-1/2 md:group-even:translate-x-1/2 z-10">
                  <CheckCircle2 className="w-5 h-5" />
                </div>
                <div className="w-[calc(100%-4rem)] md:w-[calc(50%-2.5rem)] bg-slate-950 p-4 rounded-xl border border-slate-800 shadow">
                  <div className="flex justify-between mb-1">
                    <span className="font-bold text-emerald-400 text-sm">PAGO</span>
                    <span className="text-xs text-slate-500">Hoje</span>
                  </div>
                  <p className="text-xs text-slate-400">NF-2026-9941 quitada.</p>
                </div>
              </div>

              <div className="relative flex items-center justify-between md:justify-normal md:odd:flex-row-reverse group">
                <div className="flex items-center justify-center w-10 h-10 rounded-full border-4 border-slate-900 bg-indigo-500 text-white shadow shrink-0 md:order-1 md:group-odd:-translate-x-1/2 md:group-even:translate-x-1/2 z-10">
                  <span className="w-2 h-2 bg-white rounded-full"></span>
                </div>
                <div className="w-[calc(100%-4rem)] md:w-[calc(50%-2.5rem)] bg-slate-950 p-4 rounded-xl border border-slate-800 shadow">
                  <div className="flex justify-between mb-1">
                    <span className="font-bold text-indigo-400 text-sm">FATURADO</span>
                    <span className="text-xs text-slate-500">Ontem</span>
                  </div>
                  <p className="text-xs text-slate-400">NF emitida pelo fiscal.</p>
                </div>
              </div>

            </div>
          </div>
        </div>

      </div>
    </div>
  );
}