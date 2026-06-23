import React, { useState, useEffect } from 'react';
import { Plane, Upload, AlertCircle, CheckCircle2, Loader2 } from 'lucide-react';
import api from '../services/api';

export default function PainelViagensEReembolso() {
  const [viagens, setViagens] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [inputs, setInputs] = useState({});

  const carregarViagens = async () => {
    try {
      setLoading(true);
      // 💥 ROTA ATUALIZADA
      const response = await api.get('/financeiro/viagens');
      setViagens(Array.isArray(response.data) ? response.data : []);
      setError(null);
    } catch (err) {
      setError('Erro ao carregar a listagem de viagens.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregarViagens();
  }, []);

  const handleFechamento = async (viagemId) => {
    const valorReal = parseFloat(inputs[viagemId]);
    if (!valorReal || valorReal <= 0) {
      alert('Por favor, insira um valor válido.');
      return;
    }

    try {
      // 💥 INTEGRADO COM PRESTACAOCONTAS DTO DO BACKEND
      await api.post('/financeiro/prestacao-contas', {
        viagemId: viagemId,
        custoReal: valorReal,
        caminhoNotaFiscal: "upload_nota_fiscal_id_" + viagemId + ".pdf"
      });

      alert('Prestação de contas enviada com sucesso!');
      carregarViagens();
    } catch (err) {
      alert('Erro ao salvar prestação de contas.');
      console.error(err);
    }
  };

  const format = (v) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v || 0);

  if (loading) return (
    <div className="min-h-screen bg-slate-950 flex justify-center items-center">
      <Loader2 className="w-10 h-10 animate-spin text-indigo-500" />
    </div>
  );

  return (
    <div className="min-h-screen bg-slate-950 p-4 md:p-8 font-sans text-slate-100">
      <div className="max-w-6xl mx-auto space-y-8">
        
        <header>
          <h1 className="text-3xl font-black tracking-tight text-white flex items-center gap-3">
            <Plane className="w-8 h-8 text-indigo-400" />
            Viagens e Reembolsos
          </h1>
          <p className="text-slate-400 text-sm mt-1">Acerto de contas operacional com o financeiro</p>
        </header>

        {error && (
          <div className="bg-rose-950/40 border border-rose-500/30 text-rose-300 p-4 rounded-xl text-sm">
            {error}
          </div>
        )}

        <div className="grid grid-cols-1 gap-6">
          {viagens.length === 0 ? (
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-8 text-center text-slate-500">
              Nenhuma viagem localizada no banco de dados.
            </div>
          ) : (
            viagens.map((viagem) => {
              const custoReal = viagem.prestacaoContas?.custoReal;
              const statusFechado = custoReal > 0;
              const estourou = custoReal > viagem.custoPlanejado;

              return (
                <div key={viagem.id} className={`bg-slate-900 border ${estourou ? 'border-rose-500/50' : 'border-slate-800'} rounded-2xl p-6 shadow-xl flex flex-col md:flex-row justify-between items-start md:items-center gap-6 transition-all`}>
                  
                  <div className="flex-1 space-y-2">
                    <div className="flex items-center gap-3">
                      {/* Mapeado para os campos do seu DTO real */}
                      <h3 className="text-lg font-bold text-white">{viagem.solicitacaoVeiculo || 'Veículo Próprio / Outros'}</h3>
                      <span className={`px-2.5 py-0.5 rounded text-[10px] font-black uppercase tracking-wider ${statusFechado ? 'bg-emerald-500/20 text-emerald-400' : 'bg-indigo-500/20 text-indigo-400'}`}>
                        {statusFechado ? 'CONCLUÍDA' : 'EM ANDAMENTO'}
                      </span>
                    </div>
                    <p className="text-sm text-slate-400">{viagem.hospedagemDetalhes || 'Sem detalhes de hospedagem'}</p>
                    
                    <div className="flex gap-6 pt-2">
                      <div>
                        <p className="text-xs text-slate-500 font-bold uppercase tracking-wider">Orçamento Planejado</p>
                        <p className="text-base font-bold text-slate-300">{format(viagem.custoPlanejado)}</p>
                      </div>
                      {statusFechado && (
                        <div>
                          <p className="text-xs text-slate-500 font-bold uppercase tracking-wider">Custo Real</p>
                          <p className={`text-base font-bold flex items-center gap-1 ${estourou ? 'text-rose-400' : 'text-emerald-400'}`}>
                            {format(custoReal)}
                            {estourou ? <AlertCircle className="w-4 h-4" /> : <CheckCircle2 className="w-4 h-4" />}
                          </p>
                        </div>
                      )}
                    </div>
                  </div>

                  {!statusFechado ? (
                    <div className="w-full md:w-auto flex flex-col sm:flex-row gap-3 bg-slate-950 p-4 rounded-xl border border-slate-800">
                      <div className="flex-1">
                        <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1">Custo Real (R$)</label>
                        <input 
                          type="number" 
                          placeholder="0.00"
                          className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-indigo-500"
                          onChange={(e) => setInputs({...inputs, [viagem.id]: e.target.value})}
                        />
                      </div>
                      <div className="flex flex-col gap-2 justify-end">
                        <label className="flex items-center justify-center gap-2 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-bold py-2.5 px-4 rounded-lg cursor-pointer transition">
                          <Upload className="w-4 h-4" /> Nota Fiscal
                          <input type="file" className="hidden" />
                        </label>
                        <button 
                          onClick={() => handleFechamento(viagem.id)}
                          className="bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold py-2.5 px-4 rounded-lg transition"
                        >
                          Fechar Viagem
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="w-full md:w-auto text-right p-4">
                      {estourou ? (
                        <p className="text-xs text-rose-400 font-bold max-w-[200px]">Alerta de Estouro: O orçamento planejado estourou em {format(custoReal - viagem.custoPlanejado)}.</p>
                      ) : (
                        <p className="text-xs text-emerald-400 font-bold flex items-center gap-2 justify-end"><CheckCircle2 className="w-4 h-4" /> Prestação Aprovada</p>
                      )}
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>

      </div>
    </div>
  );
}