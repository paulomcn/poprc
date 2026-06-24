import React, { useState, useEffect } from 'react';
import { Package, FileCheck, CheckCircle2, AlertTriangle, Loader2, RefreshCw } from 'lucide-react';
import api from '../services/api';

export default function AuditoriaMateriaisEAsBuilt() {
  const [dados, setDados] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Puxa a auditoria real do projeto 1 no banco de dados
  const carregarAuditoria = async () => {
    try {
      setLoading(true);
      // Batendo no endpoint de auditoria do projeto 1
      const response = await api.get('/projetos/1/auditoria');
      setDados(response.data);
      setError(null);
    } catch (err) {
      setError('Erro ao carregar a auditoria de materiais e As-Built do servidor.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregarAuditoria();
  }, []);

  // PUT para homologar o As-Built direto no banco
  const homologarAsBuilt = async () => {
    try {
      await api.put('/projetos/1/as-built/homologar');
      alert('Documentação As-Built homologada com sucesso!');
      carregarAuditoria();
    } catch (err) {
      alert('Erro ao homologar As-Built no servidor.');
      console.error(err);
    }
  };

  if (loading) return (
    <div className="min-h-screen bg-slate-950 flex justify-center items-center">
      <Loader2 className="w-10 h-10 animate-spin text-indigo-500" />
    </div>
  );

  // Fallback seguro caso o endpoint ainda não esteja criado no Java
  const materiais = dados?.materiais || [];
  const asBuiltStatus = dados?.asBuiltStatus || 'PENDENTE';

  return (
    <div className="min-h-screen bg-slate-950 p-4 md:p-8 font-sans text-slate-100">
      <div className="max-w-6xl mx-auto space-y-8">
        
        <header className="flex justify-between items-start">
          <div>
            <h1 className="text-3xl font-black tracking-tight text-white">Auditoria de Materiais & As-Built</h1>
            <p className="text-slate-400 text-sm mt-1">Módulo 15 e 16 - Conformidade de engenharia e estoque</p>
          </div>
          <button onClick={carregarAuditoria} className="p-2 bg-slate-900 border border-slate-800 rounded-xl hover:bg-slate-800 transition">
            <RefreshCw className="w-5 h-5 text-slate-400" />
          </button>
        </header>

        {error && (
          <div className="bg-rose-950/40 border border-rose-500/30 text-rose-300 p-4 rounded-xl text-sm">
            {error}
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* Coluna 1 e 2: Tabela de Materiais (Previsto vs Utilizado) */}
          <div className="lg:col-span-2 bg-slate-900 border border-slate-800 rounded-2xl shadow-xl overflow-hidden">
            <div className="p-6 border-b border-slate-800 flex items-center gap-3">
              <Package className="w-5 h-5 text-indigo-400" />
              <h2 className="font-bold text-white">Discrepância de Inventário</h2>
            </div>
            
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm whitespace-nowrap">
                <thead className="bg-slate-950/50 text-slate-400 border-b border-slate-800 uppercase text-xs font-bold tracking-wider">
                  <tr>
                    <th className="px-6 py-4">Item</th>
                    <th className="px-6 py-4 text-center">Previsto</th>
                    <th className="px-6 py-4 text-center">Utilizado</th>
                    <th className="px-6 py-4 text-right">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800">
                  {materiais.length === 0 ? (
                    <tr>
                      <td colSpan="4" className="px-6 py-8 text-center text-slate-500">
                        Nenhum material auditado para este projeto no banco.
                      </td>
                    </tr>
                  ) : (
                    materiais.map((mat, i) => {
                      const divergente = mat.utilizado > mat.previsto;
                      return (
                        <tr key={i} className="hover:bg-slate-800/50 transition-colors">
                          <td className="px-6 py-4 font-medium text-slate-300">{mat.nome}</td>
                          <td className="px-6 py-4 text-center text-slate-400 font-mono">{mat.previsto}</td>
                          <td className={`px-6 py-4 text-center font-mono font-bold ${divergente ? 'text-rose-400' : 'text-slate-200'}`}>
                            {mat.utilizado}
                          </td>
                          <td className="px-6 py-4 text-right">
                            {divergente ? (
                              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded text-xs font-bold bg-rose-500/10 text-rose-400 border border-rose-500/20">
                                <AlertTriangle className="w-3 h-3" /> Excedido
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                                <CheckCircle2 className="w-3 h-3" /> OK
                              </span>
                            )}
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Coluna 3: Status do As-Built (Engenharia) */}
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl flex flex-col justify-between space-y-6">
            <div className="space-y-4">
              <div className="flex items-center gap-3 pb-2 border-b border-slate-800">
                <FileCheck className="w-5 h-5 text-indigo-400" />
                <h2 className="font-bold text-white">Status do As-Built</h2>
              </div>
              <p className="text-sm text-slate-400 leading-relaxed">
                O arquivo As-Built valida que a engenharia executada em campo bate 100% com a planta aprovada pelo cliente.
              </p>
              
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 text-center">
                <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1">Situação Atual</span>
                <p className={`text-xl font-black tracking-widest ${asBuiltStatus === 'HOMOLOGADO' ? 'text-emerald-400' : 'text-amber-400'}`}>
                  {asBuiltStatus}
                </p>
              </div>
            </div>

            {asBuiltStatus !== 'HOMOLOGADO' ? (
              <button 
                onClick={homologarAsBuilt}
                className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-bold py-3 px-4 rounded-xl text-sm transition"
              >
                Homologar Documento
              </button>
            ) : (
              <div className="text-center p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-xs font-bold text-emerald-400 flex items-center justify-center gap-2">
                <CheckCircle2 className="w-4 h-4" /> Projeto Final Homologado
              </div>
            )}
          </div>

        </div>

      </div>
    </div>
  );
}