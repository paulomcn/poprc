import React, { useState, useEffect } from 'react';
import { LayoutDashboard, FileText, CheckCircle, AlertTriangle, Loader2 } from 'lucide-react';
import api from '../services/api';

export default function GestaoFaturamento() {
  const [faturamentos, setFaturamentos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Carrega as medições direto do banco de dados
  const carregarFaturamentos = async () => {
    try {
      setLoading(true);
      const response = await api.get('/faturamentos');
      setFaturamentos(Array.isArray(response.data) ? response.data : []);
      setError(null);
    } catch (err) {
      setError('Erro ao conectar com o servidor de faturamento.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregarFaturamentos();
  }, []);

  // Dispara o PUT para emitir a Nota Fiscal no banco
  const emitirNotaFiscal = async (id) => {
    try {
      const numeroNF = `NF-2026-${Math.floor(1000 + Math.random() * 9000)}`;
      const dataVencimento = new Date();
      dataVencimento.setDate(dataVencimento.getDate() + 30); // 30 dias de prazo

      await api.put(`/faturamentos/${id}/emitir-nf`, {
        numeroNotaFiscal: numeroNF,
        dataVencimento: dataVencimento.toISOString().split('T')[0]
      });

      // Recarrega a lista com os dados atualizados do banco
      carregarFaturamentos();
    } catch (err) {
      alert('Erro ao emitir Nota Fiscal no servidor.');
      console.error(err);
    }
  };

  // Dispara o PUT para dar baixa no pagamento no banco
  const darBaixaPagamento = async (id) => {
    try {
      await api.put(`/faturamentos/${id}/baixa`);
      carregarFaturamentos();
    } catch (err) {
      alert('Erro ao dar baixa no pagamento no servidor.');
      console.error(err);
    }
  };

  const getStatusBadge = (status) => {
    switch(status) {
      case 'A_FATURAR': return 'bg-slate-500/10 text-slate-400 border-slate-500/20';
      case 'FATURADO': return 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20';
      case 'PAGO': return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
      case 'EM_ATRASO': return 'bg-rose-500/10 text-rose-400 border-rose-500/20';
      default: return 'bg-slate-500/10 text-slate-400';
    }
  };

  // Cálculos dinâmicos baseados no que veio do banco
  const totais = faturamentos.reduce((acc, fat) => {
    acc[fat.situacao] = (acc[fat.situacao] || 0) + (fat.valorMedicao || 0);
    return acc;
  }, {});

  const format = (v) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v || 0);

  if (loading) return (
    <div className="min-h-screen bg-slate-950 flex justify-center items-center">
      <Loader2 className="w-10 h-10 animate-spin text-indigo-500" />
    </div>
  );

  return (
    <div className="min-h-screen bg-slate-950 p-4 md:p-8 font-sans text-slate-100">
      <div className="max-w-7xl mx-auto space-y-8">
        
        <header>
          <h1 className="text-3xl font-black tracking-tight text-white">Gestão de Faturamentos</h1>
          <p className="text-slate-400 text-sm mt-1">Módulo 14 - Controle operacional em tempo real</p>
        </header>

        {error && (
          <div className="bg-rose-950/40 border border-rose-500/30 text-rose-300 p-4 rounded-xl text-sm">
            {error}
          </div>
        )}

        {/* Cards Resumo Calculados Dinamicamente do Banco */}
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

        {/* Tabela de Dados Reais */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl shadow-xl overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm whitespace-nowrap">
              <thead className="bg-slate-950/50 text-slate-400 border-b border-slate-800 uppercase text-xs font-bold tracking-wider">
                <tr>
                  <th className="px-6 py-4">Serviços Executados</th>
                  <th className="px-6 py-4">Valor da Medição</th>
                  <th className="px-6 py-4">Nota Fiscal</th>
                  <th className="px-6 py-4">Vencimento</th>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4 text-right">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {faturamentos.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="px-6 py-8 text-center text-slate-500">
                      Nenhum faturamento registrado no banco de dados.
                    </td>
                  </tr>
                ) : (
                  faturamentos.map((fat) => (
                    <tr key={fat.id} className="hover:bg-slate-800/50 transition-colors">
                      <td className="px-6 py-4 font-medium text-slate-300 max-w-xs truncate">{fat.servicosExecutados}</td>
                      <td className="px-6 py-4 font-bold text-white">{format(fat.valorMedicao)}</td>
                      <td className="px-6 py-4 text-indigo-400 font-mono text-xs">{fat.numeroNotaFiscal || '-'}</td>
                      <td className="px-6 py-4 text-slate-400 text-xs">
                        {fat.dataVencimento ? new Date(fat.dataVencimento + 'T00:00:00').toLocaleDateString('pt-BR') : '-'}
                      </td>
                      <td className="px-6 py-4">
                        <span className={`px-3 py-1 rounded-full border text-[10px] font-black tracking-widest ${getStatusBadge(fat.situacao)}`}>
                          {fat.situacao ? fat.situacao.replace('_', ' ') : 'S/S'}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-right space-x-2">
                        {fat.situacao === 'A_FATURAR' && (
                          <button onClick={() => emitirNotaFiscal(fat.id)} className="bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold py-2 px-4 rounded-lg transition">
                            Emitir NF
                          </button>
                        )}
                        {fat.situacao === 'FATURADO' && (
                          <button onClick={() => darBaixaPagamento(fat.id)} className="bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold py-2 px-4 rounded-lg transition">
                            Baixar Pagamento
                          </button>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

      </div>
    </div>
  );
}