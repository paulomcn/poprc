import React, { useState } from 'react';
import { LayoutDashboard, FileText, CheckCircle, AlertTriangle } from 'lucide-react';

export default function GestaoFaturamento() {
  const [faturamentos, setFaturamentos] = useState([
    { id: 1, descricao: 'Medição Fase 1 - Infraestrutura', valor: 4500.00, status: 'A_FATURAR', nf: '-' },
    { id: 2, descricao: 'Medição Fase 2 - Ativos de Rede', valor: 8200.00, status: 'FATURADO', nf: 'NF-9941' },
    { id: 3, descricao: 'Aditivo de Cabos Extra', valor: 1200.00, status: 'EM_ATRASO', nf: 'NF-9910' },
  ]);

  const atualizarStatus = (id, novoStatus) => {
    setFaturamentos(prev => prev.map(fat => 
      fat.id === id ? { ...fat, status: novoStatus, nf: novoStatus === 'FATURADO' ? `NF-${Math.floor(Math.random() * 10000)}` : fat.nf } : fat
    ));
  };

  const getStatusBadge = (status) => {
    switch(status) {
      case 'A_FATURAR': return 'bg-slate-500/10 text-slate-400 border-slate-500/20';
      case 'FATURADO': return 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20';
      case 'PAGO': return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
      case 'EM_ATRASO': return 'bg-rose-500/10 text-rose-400 border-rose-500/20';
      default: return '';
    }
  };

  const totais = faturamentos.reduce((acc, fat) => {
    acc[fat.status] = (acc[fat.status] || 0) + fat.valor;
    return acc;
  }, {});

  const format = (v) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v || 0);

  return (
    <div className="min-h-screen bg-slate-950 p-4 md:p-8 font-sans text-slate-100">
      <div className="max-w-7xl mx-auto space-y-8">
        
        <header>
          <h1 className="text-3xl font-black tracking-tight text-white">Gestão de Faturamentos</h1>
          <p className="text-slate-400 text-sm mt-1">Módulo 14 - Controle operacional de NF e recebimentos</p>
        </header>

        {/* Cards Resumo */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[
            { label: 'A Faturar', valor: totais['A_FATURAR'], color: 'text-slate-300', icon: LayoutDashboard },
            { label: 'Faturado', valor: totais['FATURADO'], color: 'text-indigo-400', icon: FileText },
            { label: 'Pago', valor: totais['PAGO'], color: 'text-emerald-400', icon: CheckCircle },
            { label: 'Em Atraso', valor: totais['EM_ATRASO'], color: 'text-rose-400', icon: AlertTriangle }
          ].map((card, i) => (
            <div key={i} className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-lg">
              <div className="flex justify-between items-center mb-3">
                <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">{card.label}</span>
                <card.icon className={`w-4 h-4 ${card.color}`} />
              </div>
              <p className={`text-2xl font-black ${card.color}`}>{format(card.valor)}</p>
            </div>
          ))}
        </div>

        {/* Tabela */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl shadow-xl overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm whitespace-nowrap">
              <thead className="bg-slate-950/50 text-slate-400 border-b border-slate-800 uppercase text-xs font-bold tracking-wider">
                <tr>
                  <th className="px-6 py-4">Descrição da Medição</th>
                  <th className="px-6 py-4">Valor</th>
                  <th className="px-6 py-4">Nota Fiscal</th>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4 text-right">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {faturamentos.map((fat) => (
                  <tr key={fat.id} className="hover:bg-slate-800/50 transition-colors">
                    <td className="px-6 py-4 font-medium text-slate-300">{fat.descricao}</td>
                    <td className="px-6 py-4 font-bold">{format(fat.valor)}</td>
                    <td className="px-6 py-4 text-slate-500 font-mono text-xs">{fat.nf}</td>
                    <td className="px-6 py-4">
                      <span className={`px-3 py-1 rounded-full border text-[10px] font-black tracking-widest ${getStatusBadge(fat.status)}`}>
                        {fat.status.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right space-x-2">
                      {fat.status === 'A_FATURAR' && (
                        <button onClick={() => atualizarStatus(fat.id, 'FATURADO')} className="bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold py-2 px-4 rounded-lg transition">
                          Emitir NF
                        </button>
                      )}
                      {(fat.status === 'FATURADO' || fat.status === 'EM_ATRASO') && (
                        <button onClick={() => atualizarStatus(fat.id, 'PAGO')} className="bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold py-2 px-4 rounded-lg transition">
                          Dar Baixa
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

      </div>
    </div>
  );
}