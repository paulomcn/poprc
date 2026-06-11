import React, { useState, useEffect } from 'react'
import {
  CheckCircle,
  AlertCircle,
  Clock,
  Loader,
  Plus,
  Search,
  ChevronDown,
} from 'lucide-react'
import api from "../services/api";
import OrdensServicoCard from "../components/OrdensServicoCard";
import StatusModal from "../components/StatusModal"; // Alterado de ./ para ../components/

const STATUS_COLUMNS = [
  { value: 'ABERTA', label: 'Aberta', color: 'bg-blue-50', borderColor: 'border-blue-200' },
  { value: 'EM_EXECUCAO', label: 'Em Execução', color: 'bg-yellow-50', borderColor: 'border-yellow-200' },
  {
    value: 'AGUARDANDO_VALIDACAO',
    label: 'Aguardando Validação',
    color: 'bg-purple-50',
    borderColor: 'border-purple-200',
  },
  { value: 'CONCLUIDA', label: 'Concluída', color: 'bg-green-50', borderColor: 'border-green-200' },
  { value: 'FATURADA', label: 'Faturada', color: 'bg-gray-50', borderColor: 'border-gray-200' },
]

export default function GestaoOrdensServico() {
  const [ordensServico, setOrdensServico] = useState([])
  const [filteredOrdens, setFilteredOrdens] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [filterCliente, setFilterCliente] = useState('')
  const [filterNumeroOS, setFilterNumeroOS] = useState('')
  const [selectedOrdem, setSelectedOrdem] = useState(null)
  const [showStatusModal, setShowStatusModal] = useState(false)

  useEffect(() => {
    carregarOrdensServico()
  }, [])

  useEffect(() => {
    aplicarFiltros()
  }, [ordensServico, filterCliente, filterNumeroOS])

  const carregarOrdensServico = async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await api.get('/ordens-servico')
      setOrdensServico(response.data)
    } catch (err) {
      setError('Erro ao carregar ordens de serviço. Tente novamente.')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const aplicarFiltros = () => {
    let resultado = ordensServico

    if (filterNumeroOS.trim()) {
      resultado = resultado.filter((ordem) =>
        ordem.numeroOs.toLowerCase().includes(filterNumeroOS.toLowerCase())
      )
    }

    if (filterCliente.trim()) {
      resultado = resultado.filter((ordem) =>
        ordem.contrato?.cliente?.toLowerCase().includes(filterCliente.toLowerCase())
      )
    }

    setFilteredOrdens(resultado)
  }

  const abrirModalStatus = (ordem) => {
    setSelectedOrdem(ordem)
    setShowStatusModal(true)
  }

  const fecharModalStatus = () => {
    setShowStatusModal(false)
    setSelectedOrdem(null)
  }

  const handleStatusAtualizado = (ordemAtualizada) => {
    setOrdensServico((prevOrdens) =>
      prevOrdens.map((ordem) => (ordem.id === ordemAtualizada.id ? ordemAtualizada : ordem))
    )
    fecharModalStatus()
  }

  const agruparPorStatus = () => {
    const agrupado = {}
    STATUS_COLUMNS.forEach((col) => {
      agrupado[col.value] = filteredOrdens.filter((ordem) => ordem.status === col.value)
    })
    return agrupado
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen bg-gray-50">
        <div className="text-center">
          <Loader className="w-12 h-12 text-blue-600 animate-spin mx-auto mb-4" />
          <p className="text-gray-600 font-medium">Carregando ordens de serviço...</p>
        </div>
      </div>
    )
  }

  const ordensPorStatus = agruparPorStatus()

  return (
    <div className="p-8 bg-gray-50 min-h-screen">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-6">Gestão de Ordens de Serviço</h1>

        {/* Filtros e Ações */}
        <div className="bg-white rounded-lg shadow-sm p-6 mb-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {/* Filtro Número OS */}
            <div className="relative">
              <Search className="absolute left-3 top-3 text-gray-400 w-5 h-5" />
              <input
                type="text"
                placeholder="Filtrar por nº OS..."
                value={filterNumeroOS}
                onChange={(e) => setFilterNumeroOS(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            {/* Filtro Cliente */}
            <div className="relative">
              <Search className="absolute left-3 top-3 text-gray-400 w-5 h-5" />
              <input
                type="text"
                placeholder="Filtrar por cliente..."
                value={filterCliente}
                onChange={(e) => setFilterCliente(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            {/* Botão Nova OS */}
            <button className="bg-blue-600 text-white px-6 py-2 rounded-lg font-medium hover:bg-blue-700 transition flex items-center justify-center gap-2">
              <Plus className="w-5 h-5" />
              Nova OS
            </button>
          </div>

          {/* Info de Filtros Ativos */}
          {(filterNumeroOS || filterCliente) && (
            <div className="mt-4 text-sm text-gray-600">
              Filtros ativos: {filterNumeroOS && `OS: "${filterNumeroOS}"`}{' '}
              {filterCliente && `Cliente: "${filterCliente}"`}
            </div>
          )}
        </div>

        {/* Mensagem de Erro */}
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-6 py-4 rounded-lg flex items-center gap-3 mb-6">
            <AlertCircle className="w-5 h-5 flex-shrink-0" />
            <span>{error}</span>
            <button
              onClick={carregarOrdensServico}
              className="ml-auto text-red-700 underline hover:no-underline font-medium"
            >
              Tentar novamente
            </button>
          </div>
        )}
      </div>

      {/* Kanban Board */}
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
        {STATUS_COLUMNS.map((coluna) => (
          <div key={coluna.value} className="space-y-4">
            {/* Cabeçalho Coluna */}
            <div className={`${coluna.color} border-2 ${coluna.borderColor} rounded-lg p-4`}>
              <div className="flex items-center justify-between mb-2">
                <h2 className="font-bold text-gray-900">{coluna.label}</h2>
                <span className="bg-gray-200 text-gray-700 text-xs font-semibold px-2 py-1 rounded">
                  {ordensPorStatus[coluna.value].length}
                </span>
              </div>
              <div className="w-full h-1 bg-gray-300 rounded" />
            </div>

            {/* Cards da Coluna */}
            <div className="space-y-3 min-h-96">
              {ordensPorStatus[coluna.value].length > 0 ? (
                ordensPorStatus[coluna.value].map((ordem) => (
                  <OrdensServicoCard
                    key={ordem.id}
                    ordem={ordem}
                    onAtualizarStatus={() => abrirModalStatus(ordem)}
                  />
                ))
              ) : (
                <div className="text-center py-8 text-gray-400">
                  <p className="text-sm">Nenhuma OS nesta coluna</p>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* Modal de Atualização de Status */}
      {showStatusModal && selectedOrdem && (
        <StatusModal
          ordem={selectedOrdem}
          onClose={fecharModalStatus}
          onStatusAtualizado={handleStatusAtualizado}
        />
      )}
    </div>
  )
}
