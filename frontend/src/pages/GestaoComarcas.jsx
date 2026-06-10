import { useState, useEffect } from 'react'
import { AlertTriangle, Edit2, MapPin, User, Zap } from 'lucide-react'
import api from '../services/api'
import Modal from '../components/Modal'
import LoadingSpinner from '../components/LoadingSpinner'
import Alert from '../components/Alert'

export default function GestaoComarcas() {
  const [comarcas, setComarcas] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [selectedComarca, setSelectedComarca] = useState(null)
  const [formData, setFormData] = useState({
    percentualConcluido: 0,
    pendencias: ''
  })

  useEffect(() => {
    fetchComarcas()
  }, [])

  const fetchComarcas = async () => {
    try {
      setLoading(true)
      const response = await api.get('/comarcas')
      setComarcas(response.data)
      setError(null)
    } catch (err) {
      setError('Erro ao carregar comarcas')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleOpenModal = (comarca) => {
    setSelectedComarca(comarca)
    setFormData({
      percentualConcluido: comarca.percentualConcluido || 0,
      pendencias: comarca.pendencias || ''
    })
    setShowModal(true)
  }

  const handleCloseModal = () => {
    setShowModal(false)
    setSelectedComarca(null)
    setFormData({ percentualConcluido: 0, pendencias: '' })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!selectedComarca) return

    try {
      await api.patch(`/comarcas/${selectedComarca.id}`, {
        percentualConcluido: parseFloat(formData.percentualConcluido),
        pendencias: formData.pendencias
      })

      setError(null)
      handleCloseModal()
      fetchComarcas()
    } catch (err) {
      setError('Erro ao atualizar comarca')
      console.error(err)
    }
  }

  const handleInputChange = (e) => {
    const { name, value } = e.target
    setFormData(prev => ({
      ...prev,
      [name]: name === 'percentualConcluido' ? value : value
    }))
  }

  const getProgressColor = (percentage) => {
    if (percentage >= 80) return 'bg-green-500'
    if (percentage >= 50) return 'bg-yellow-500'
    return 'bg-red-500'
  }

  if (loading) return <LoadingSpinner />
  if (error) return <Alert type="error" message={error} />

  return (
    <div>
      <div className="mb-8">
        <div>
          <h1 className="text-3xl font-bold text-slate-800">Gestão de Comarcas</h1>
          <p className="text-slate-600 mt-2">Monitore o progresso e gerenciamento das comarcas</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {comarcas.map((comarca) => (
          <div
            key={comarca.id}
            className="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow border border-slate-200 overflow-hidden"
          >
            {/* Header com alerta se houver pendências */}
            <div className={`p-6 ${comarca.pendencias ? 'bg-red-50 border-l-4 border-red-500' : 'bg-white'}`}>
              <div className="flex items-start justify-between mb-4">
                <div className="flex-1">
                  <h3 className="text-xl font-semibold text-slate-800">{comarca.nomeComarca}</h3>
                </div>
                {comarca.pendencias && (
                  <div className="ml-4 animate-pulse">
                    <div className="flex items-center gap-2 bg-red-500 text-white px-3 py-1 rounded-full text-sm font-semibold">
                      <AlertTriangle size={16} />
                      <span>Alerta</span>
                    </div>
                  </div>
                )}
              </div>

              {/* Informações principais */}
              <div className="space-y-3 mb-6">
                <div className="flex items-start gap-3">
                  <MapPin size={18} className="text-blue-500 mt-0.5 flex-shrink-0" />
                  <div>
                    <p className="text-sm font-medium text-slate-600">Endereço</p>
                    <p className="text-slate-800">{comarca.endereco}</p>
                  </div>
                </div>

                <div className="flex items-start gap-3">
                  <User size={18} className="text-purple-500 mt-0.5 flex-shrink-0" />
                  <div>
                    <p className="text-sm font-medium text-slate-600">Juiz Responsável</p>
                    <p className="text-slate-800">{comarca.juizResponsavel}</p>
                  </div>
                </div>

                <div className="flex items-start gap-3">
                  <Zap size={18} className="text-yellow-500 mt-0.5 flex-shrink-0" />
                  <div>
                    <p className="text-sm font-medium text-slate-600">Quantidade de Pontos</p>
                    <p className="text-slate-800 font-semibold">{comarca.quantidadePontos || 0}</p>
                  </div>
                </div>
              </div>

              {/* Barra de progresso */}
              <div className="mb-6">
                <div className="flex items-center justify-between mb-2">
                  <p className="text-sm font-medium text-slate-600">Progresso de Conclusão</p>
                  <span className="text-sm font-semibold text-slate-700">{comarca.percentualConcluido || 0}%</span>
                </div>
                <div className="w-full bg-slate-200 rounded-full h-3 overflow-hidden">
                  <div
                    className={`h-full transition-all duration-300 ${getProgressColor(comarca.percentualConcluido || 0)}`}
                    style={{ width: `${Math.min(comarca.percentualConcluido || 0, 100)}%` }}
                  />
                </div>
              </div>

              {/* Pendências */}
              {comarca.pendencias && (
                <div className="mb-6 bg-red-100 border border-red-300 rounded-lg p-4">
                  <p className="text-sm font-semibold text-red-800 mb-2">Pendências:</p>
                  <p className="text-sm text-red-700">{comarca.pendencias}</p>
                </div>
              )}

              {/* Botão de ação */}
              <button
                onClick={() => handleOpenModal(comarca)}
                className="w-full flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-4 rounded-lg transition-colors"
              >
                <Edit2 size={18} />
                Registrar Pendência/Ajustar Progresso
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Modal para editar pendências e progresso */}
      <Modal
        isOpen={showModal}
        onClose={handleCloseModal}
        title={`Ajustar Progresso - ${selectedComarca?.nomeComarca}`}
      >
        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Percentual de Conclusão */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              Percentual de Conclusão (%)
            </label>
            <div className="flex items-center gap-4">
              <input
                type="number"
                name="percentualConcluido"
                value={formData.percentualConcluido}
                onChange={handleInputChange}
                min="0"
                max="100"
                className="flex-1 px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="0"
              />
              <span className="text-slate-700 font-semibold">%</span>
            </div>
            <div className="mt-3 bg-slate-100 rounded-lg p-3">
              <div className="w-full bg-slate-300 rounded-full h-3 overflow-hidden">
                <div
                  className={`h-full transition-all ${getProgressColor(formData.percentualConcluido)}`}
                  style={{ width: `${Math.min(formData.percentualConcluido, 100)}%` }}
                />
              </div>
            </div>
          </div>

          {/* Campo de Pendências */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              Pendências/Observações
            </label>
            <textarea
              name="pendencias"
              value={formData.pendencias}
              onChange={handleInputChange}
              rows="5"
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
              placeholder="Descreva qualquer pendência, ajuste ou observação importante..."
            />
          </div>

          {/* Botões de ação */}
          <div className="flex gap-3 justify-end">
            <button
              type="button"
              onClick={handleCloseModal}
              className="px-6 py-2 border border-slate-300 text-slate-700 font-semibold rounded-lg hover:bg-slate-50 transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg transition-colors"
            >
              Salvar Alterações
            </button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
