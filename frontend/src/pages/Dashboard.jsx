import { BarChart3, FileText, Briefcase, Users } from 'lucide-react'

export default function Dashboard() {
  const stats = [
    { icon: FileText, label: 'Contratos Ativos', value: '12', color: 'bg-blue-100' },
    { icon: Briefcase, label: 'Projetos em Andamento', value: '28', color: 'bg-green-100' },
    { icon: Users, label: 'Funcionários', value: '145', color: 'bg-purple-100' },
    { icon: BarChart3, label: 'Valor Total', value: 'R$ 2.5M', color: 'bg-orange-100' },
  ]

  return (
    <div className="max-w-6xl">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-800">Dashboard</h1>
        <p className="text-slate-600 mt-2">Bem-vindo ao RC Operations Hub</p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {stats.map((stat, idx) => (
          <div key={idx} className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-slate-600 text-sm font-medium">{stat.label}</p>
                <p className="text-3xl font-bold text-slate-800 mt-2">{stat.value}</p>
              </div>
              <div className={`${stat.color} p-4 rounded-lg`}>
                <stat.icon size={28} className="text-slate-700" />
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Quick Actions */}
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
            <p>
              <span className="font-medium">Versão:</span> 0.1.5
            </p>
            <p>
              <span className="font-medium">Ambiente:</span> Desenvolvimento
            </p>
            <p>
              <span className="font-medium">API:</span> Online
            </p>
            <p>
              <span className="font-medium">Data:</span> {new Date().toLocaleDateString('pt-BR')}
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
