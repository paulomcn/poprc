import React, { useState } from 'react';
import { Plane, Upload, AlertCircle, CheckCircle2 } from 'lucide-react';

export default function PainelViagensEReembolso() {
  const [viagens, setViagens] = useState([
    { id: 1, destino: 'Salvador - BA', motivo: 'Instalação Racks', custoPrevisto: 1200.00, custoReal: null, status: 'EM_ANDAMENTO' },
    { id: 2, destino: 'Camaçari - BA', motivo: 'Troca de Switch', custoPrevisto: 300.00, custoReal: 450.00, status: 'CONCLUIDA' },
  ]);

  const [inputs, setInputs] = useState({});

  const handleFechamento = (id) => {
    const valor = parseFloat(inputs[id]);
    if (!valor) return;

    setViagens(prev => prev.map(v => 
      v.id === id ? { ...v, custoReal: valor, status: 'CONCLUIDA' } : v
    ));
  };

  const format = (v) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v || 0);

  return (
    <div className="min-h-screen bg-slate-950 p-4 md:p-8 font-sans text-slate-100">
      <div className="max-w-6xl mx-auto space-y-8">
        
        <header>
          <h1 className="text-3xl font-black tracking-tight text-white flex items-center gap-3">
            <Plane className="w-8 h-8 text-indigo-400" />
            Viagens e Reembolsos
          </h1>
          <p className="text-slate-400 text-sm mt-1">Acerto de contas e controle de orçamentos (Módulos 6 e 7)</p>
        </header>

        <div className="grid grid-cols-1 gap-6">
          {viagens.map((viagem) => {
            const estourou = viagem.custoReal > viagem.custoPrevisto;
            return (
              <div key={viagem.id} className={`bg-slate-900 border ${estourou ? 'border-rose-500/50' : 'border-slate-800'} rounded-2xl p-6 shadow-xl flex flex-col md:flex-row justify-between items-start md:items-center gap-6 transition-all`}>
                
                <div className="flex-1 space-y-2">
                  <div className="flex items-center gap-3">
                    <h3 className="text-lg font-bold text-white">{viagem.destino}</h3>
                    <span className={`px-2.5 py-0.5 rounded text-[10px] font-black uppercase tracking-wider ${viagem.status === 'CONCLUIDA' ? 'bg-emerald-500/20 text-emerald-400' : 'bg-indigo-500/20 text-indigo-400'}`}>
                      {viagem.status.replace('_', ' ')}
                    </span>
                  </div>
                  <p className="text-sm text-slate-400">{viagem.motivo}</p>
                  
                  <div className="flex gap-6 pt-2">
                    <div>
                      <p className="text-xs text-slate-500 font-bold uppercase tracking-wider">Previsto</p>
                      <p className="text-base font-bold text-slate-300">{format(viagem.custoPrevisto)}</p>
                    </div>
                    {viagem.custoReal && (
                      <div>
                        <p className="text-xs text-slate-500 font-bold uppercase tracking-wider">Custo Real</p>
                        <p className={`text-base font-bold flex items-center gap-1 ${estourou ? 'text-rose-400' : 'text-emerald-400'}`}>
                          {format(viagem.custoReal)}
                          {estourou ? <AlertCircle className="w-4 h-4" /> : <CheckCircle2 className="w-4 h-4" />}
                        </p>
                      </div>
                    )}
                  </div>
                </div>

                {/* Bloco de Ação */}
                {viagem.status === 'EM_ANDAMENTO' ? (
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
                        <Upload className="w-4 h-4" /> Anexar NF
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
                      <p className="text-xs text-rose-400 font-bold max-w-[200px]">Alerta: O custo superou o planejamento em {format(viagem.custoReal - viagem.custoPrevisto)}.</p>
                    ) : (
                      <p className="text-xs text-emerald-400 font-bold flex items-center gap-2 justify-end"><CheckCircle2 className="w-4 h-4" /> Prestação Aprovada</p>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>

      </div>
    </div>
  );
}