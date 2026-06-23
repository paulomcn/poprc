import React, { useState } from 'react';
import { Layers, FileText, Upload, Clock, Package } from 'lucide-react';

export default function AuditoriaMateriaisEAsBuilt() {
  const [aba, setAba] = useState('materiais');

  const materiais = [
    { id: 1, nome: 'Cabo de Rede Cat6 Nexans', previsto: 50, utilizado: 35 },
    { id: 2, nome: 'Conector RJ45 Furukawa', previsto: 100, utilizado: 105 }, // Estourou 5
  ];

  const asBuiltDocs = [
    { id: 1, nome: 'diagrama_comarca_centro.vsdx', tipo: 'VISIO', versao: 2, log: 'Versão 2 atualizada em 23/06/2026 por Paulo Morais.\nVersão 1 criada em 20/06/2026 por Sistema.' },
    { id: 2, nome: 'planta_baixa_racks.dwg', tipo: 'DWG', versao: 1, log: 'Versão 1 criada em 21/06/2026 por Sistema.' },
  ];

  return (
    <div className="min-h-screen bg-slate-950 p-4 md:p-8 font-sans text-slate-100">
      <div className="max-w-6xl mx-auto space-y-8">
        
        <header>
          <h1 className="text-3xl font-black tracking-tight text-white">Auditoria Técnica</h1>
          <p className="text-slate-400 text-sm mt-1">Materiais de Projeto e Engenharia As-Built (Módulos 11 e 13)</p>
        </header>

        {/* Tabs de Navegação */}
        <div className="flex border-b border-slate-800">
          <button 
            onClick={() => setAba('materiais')}
            className={`flex items-center gap-2 px-6 py-3 text-sm font-bold border-b-2 transition-colors ${aba === 'materiais' ? 'border-indigo-500 text-indigo-400' : 'border-transparent text-slate-500 hover:text-slate-300'}`}
          >
            <Package className="w-4 h-4" /> Balanço de Materiais
          </button>
          <button 
            onClick={() => setAba('engenharia')}
            className={`flex items-center gap-2 px-6 py-3 text-sm font-bold border-b-2 transition-colors ${aba === 'engenharia' ? 'border-indigo-500 text-indigo-400' : 'border-transparent text-slate-500 hover:text-slate-300'}`}
          >
            <Layers className="w-4 h-4" /> Engenharia As-Built
          </button>
        </div>

        {/* Conteúdo Aba Materiais */}
        {aba === 'materiais' && (
          <div className="bg-slate-900 border border-slate-800 rounded-2xl shadow-xl overflow-hidden animate-in fade-in slide-in-from-bottom-4">
            <table className="w-full text-left text-sm whitespace-nowrap">
              <thead className="bg-slate-950/50 text-slate-400 border-b border-slate-800 uppercase text-xs font-bold tracking-wider">
                <tr>
                  <th className="px-6 py-4">Material</th>
                  <th className="px-6 py-4 text-center">Previsto</th>
                  <th className="px-6 py-4 text-center">Utilizado</th>
                  <th className="px-6 py-4 text-center">Saldo</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {materiais.map((mat) => {
                  const saldo = mat.previsto - mat.utilizado;
                  return (
                    <tr key={mat.id} className="hover:bg-slate-800/50 transition-colors">
                      <td className="px-6 py-4 font-medium text-slate-300">{mat.nome}</td>
                      <td className="px-6 py-4 text-center text-slate-400">{mat.previsto} un</td>
                      <td className="px-6 py-4 text-center font-bold text-white">{mat.utilizado} un</td>
                      <td className="px-6 py-4 text-center">
                        <span className={`px-3 py-1 rounded-full text-xs font-black ${saldo < 0 ? 'bg-rose-500/20 text-rose-400' : 'bg-emerald-500/20 text-emerald-400'}`}>
                          {saldo > 0 ? `+${saldo}` : saldo} un
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Conteúdo Aba Engenharia As-Built */}
        {aba === 'engenharia' && (
          <div className="grid grid-cols-1 gap-6 animate-in fade-in slide-in-from-bottom-4">
            {asBuiltDocs.map((doc) => (
              <div key={doc.id} className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl flex flex-col lg:flex-row gap-6">
                
                {/* Info do Arquivo */}
                <div className="flex-1 space-y-4">
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-3">
                      <div className="p-3 bg-indigo-500/10 rounded-xl"><FileText className="w-6 h-6 text-indigo-400" /></div>
                      <div>
                        <h3 className="text-base font-bold text-white">{doc.nome}</h3>
                        <p className="text-xs font-bold text-slate-500 tracking-widest mt-1 uppercase">Formato: {doc.tipo}</p>
                      </div>
                    </div>
                    <span className="bg-indigo-600 text-white text-[10px] font-black px-3 py-1 rounded-full tracking-widest">
                      V{doc.versao}
                    </span>
                  </div>

                  <div className="flex gap-3">
                    <button className="bg-slate-800 hover:bg-slate-700 text-white text-xs font-bold py-2 px-4 rounded-lg transition">
                      Visualizar Arquivo
                    </button>
                    <label className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold py-2 px-4 rounded-lg cursor-pointer transition">
                      <Upload className="w-4 h-4" /> Atualizar Versão
                      <input type="file" className="hidden" />
                    </label>
                  </div>
                </div>

                {/* Log de Histórico */}
                <div className="lg:w-1/3 bg-slate-950 border border-slate-800 p-4 rounded-xl">
                  <h4 className="flex items-center gap-2 text-xs font-bold text-slate-500 uppercase tracking-wider mb-3">
                    <Clock className="w-4 h-4" /> Histórico de Versões
                  </h4>
                  <div className="text-xs text-slate-400 whitespace-pre-wrap font-mono leading-relaxed">
                    {doc.log}
                  </div>
                </div>

              </div>
            ))}
          </div>
        )}

      </div>
    </div>
  );
}