import React, { useState, useEffect } from 'react';
import { BarChart3, FileText, Briefcase, Users, Loader2 } from 'lucide-react';
import api from '../services/api';

export default function Dashboard() {
  const [estatisticas, setEstatisticas] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const puxarDadosDashboard = async () => {
      try {
        setLoading(true);
        const response = await api.get('/dashboard/stats');
        setEstatisticas(response.data);
      } catch (error) {
        console.error("Erro ao carregar dados do dashboard:", error);
      } finally {
        setLoading(false);
      }
    };

    puxarDadosDashboard();
  }, []);

  // Helper para formatar a dinheirama no padrão brasileiro
  const formatarMoeda = (valor) => {
    if (!valor) return 'R$ 0,00';
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(valor);
  };

  if (loading) {
    return (
      <div className="min-h-[50vh] flex justify-center items-center">
        <Loader2 className="w-10 h-10 animate-spin text-blue-600" />
      </div>
    );
  }

  // Mapeia os dados dinâmicos vindos do Java para os cards visuais
  const stats = [
    { 
      icon: FileText, 
      label: 'Contratos Ativos', 
      value: estatisticas?.contratosAtivos || '0', 
      color: 'bg-blue-100' 
    },
    { 
      icon: Briefcase, 
      label: 'Projetos em Andamento', 
      value: estatisticas?.projetosAndamento || '0', 
      color: 'bg-green-100' 
    },
    { 
      icon: Users, 
      label: 'Funcionários', 
      value: estatisticas?.funcionarios || '0', 
      color: 'bg-purple-100' 
    },
    { 
      icon: BarChart3, 
      label: 'Valor Total', 
      value: formatarMoeda(estatisticas?.valorTotal), 
      color: 'bg-orange-100' 
    },
  ];

  return (
    <div className="max-w-6xl">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-800">Dashboard</h1>
        <p className="text-slate-600 mt-2">Bem-vindo ao RC Operations Hub</p>
      </div>

      {/* Stats Grid Dinâmico */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {stats.map((stat, idx) => (
          <div key={idx} className="bg-white rounded-lg shadow p-6 border border-slate-100">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-slate-600 text-sm font-medium">{stat.label}</p>
                <p className="text-3xl font-bold text-slate-800 mt-2 tracking-tight">{stat.value}</p>
              </div>
              <div className={`${stat.color} p-4 rounded-lg`}>
                <stat.icon size={28} className="text-slate-700" />
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Quick Actions e Infos */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold text-slate-800 mb-4">Ações Rápidas</h2>
          <div className="space-y-3">
            <button className="w-full text-left px-4 py-3 rounded-lg bg-blue-50 hover:bg-blue-100 text-blue-700 font-medium transition-colors">
              + Novo Contrato
            </button>
            <button className="w-full text-left px-4 py-3 rounded-lg bg-green-50 hover:bg-green-100 text-green-700 font-medium transition-colors">
              + Novo Projeto
            </button>
            <button className="w-full text-left px-4 py-3 rounded-lg bg-purple-50 hover:bg-purple-100 text-purple-700 font-medium transition-colors">
              + Novo Funcionário
            </button>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold text-slate-800 mb-4">Informações do Sistema</h2>
          <div className="space-y-3 text-sm text-slate-600">
            <p><span className="font-medium">Versão:</span> 0.1.5</p>
            <p><span className="font-medium">Ambiente:</span> Desenvolvimento</p>
            <p><span className="font-medium">API:</span> <span className="text-emerald-600 font-bold">Online</span></p>
            <p><span className="font-medium">Data:</span> {new Date().toLocaleDateString('pt-BR')}</p>
          </div>
        </div>
      </div>
    </div>
  );
}