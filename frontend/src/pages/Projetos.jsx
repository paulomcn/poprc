import { useState, useEffect } from 'react'
import { Plus, Briefcase } from 'lucide-react'
import api from '../services/api'
import Modal from '../components/Modal'
import LoadingSpinner from '../components/LoadingSpinner'
import Alert from '../components/Alert'

const STATUS_COLORS = {
  PLANEJAMENTO: 'bg-yellow-100 text-yellow-800',
  EM_ANDAMENTO: 'bg-blue-100 text-blue-800',
  CONCLUIDO: 'bg-green-100 text-green-800',
  SUSPENSO: 'bg-orange-100 text-orange-800',
  CANCELADO: 'bg-red-100 text-red-800'
}

export default function Projetos() {
  const [projetos, setProjetos] = useState([])
  const [contratos, setContratos] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [formData, setFormData] = useState({
    contratoId: '',
    dataInicio: '',
    dataFim: '',
    status: 'PLANEJAMENTO',
    responsavelId: ''
  })

  useEffect(() => {
    Promise.all([fetchProjetos(), fetchContratos()])
  }, [])

  const fetchProjetos = async () => {
    try {
      const response = await api.get('/projetos')
      
      // 💥 TRAVA DE SEGURANÇA
      const dadosTratados = Array.isArray(response.data)
        ? response.data
        : (response.data && Array.isArray(response.data.content) ? response.data.content : [])

      setProjetos(dadosTratados)
    } catch (err) {
      setError('Erro ao carregar projetos no servidor')
      setProjetos([])
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const fetchContratos = async () => {
    try {
      const response = await api.get('/contratos')
      
      // 💥 TRAVA DE SEGURANÇA
      const dadosTratados = Array.isArray(response.data)
        ? response.data
        : (response.data && Array.isArray(response.data.content) ? response.data.content : [])

      setContratos(dadosTratados)
    } catch (err) {
      console.error(err)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      const payload = {
        contrato: { id: parseInt(formData.contratoId) },
        dataInicio: formData.dataInicio,
        dataFim: formData.dataFim,
        status: formData.status,
        responsavel: formData.responsavelId ? { id: parseInt(formData.responsavelId) } : null
      }

      await api.post('/projetos', payload)
      setShowModal(false)
      setFormData({
        contratoId: '',
        dataInicio: '',
        dataFim: '',
        status: 'PLANEJAMENTO',
        responsavelId: ''
      })
      fetchProjetos()
    } catch (err) {
      setError('Erro ao salvar projeto')
      console.error(err)
    }
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-slate-800">Projetos</h1>
          <p className="text-slate-600 mt-2">Gerenciamento de projetos vinculados aos contratos</p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus size={20} />
          Novo Projeto
        </button>
      </div>

      {error && (
        <Alert type="error" message={error} onClose={() => setError(null)} />
      )}

      {loading ? (
        <LoadingSpinner />
      ) : !Array.isArray(projetos) || projetos.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <Briefcase size={48} className="mx-auto text-slate-400 mb-4" />
          <p className="text-slate-600">Nenhum projeto cadastrado</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {projetos.map((projeto) => (
            <div key={projeto.id} className="bg-white rounded-lg shadow hover:shadow-lg transition-shadow p-6">
              <div className="flex items-start justify-between mb-4">
                <h3 className="text-lg font-semibold text-slate-800">Projeto #{projeto.id}</h3>
                <span className={`px-3 py-1 rounded-full text-xs font-semibold ${STATUS_COLORS[projeto.status] || 'bg-slate-100'}`}>
                  {projeto.status}
                </span>
              </div>

              <div className="space-y-2 text-sm text-slate-600">
                <p>
                  <span className="font-medium">Contrato:</span> {projeto.contrato?.contrato || '-'}
                </p>
                <p>
                  <span className="font-medium">Início:</span>{' '}
                  {projeto.dataInicio ? new Date(projeto.dataInicio).toLocaleDateString('pt-BR') : '-'}
                </p>
                <p>
                  <span className="font-medium">Fim:</span>{' '}
                  {projeto.dataFim ? new Date(projeto.dataFim).toLocaleDateString('pt-BR') : '-'}
                </p>
                <p>
                  <span className="font-medium">Responsável:</span> {projeto.responsavel?.nome || '-'}
                </p>
              </div>

              <button className="w-full mt-4 px-4 py-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors font-medium">
                Ver Detalhes
              </button>
            </div>
          ))}
        </div>
      )}

      <Modal isOpen={showModal} onClose={() => setShowModal(false)} title="Novo Projeto">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Contrato *</label>
            <select
              required
              value={formData.contratoId}
              onChange={(e) => setFormData(prev => ({ ...prev, contratoId: e.target.value }))}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Selecione um contrato</option>
              {Array.isArray(contratos) && contratos.map(c => (
                <option key={c.id} value={c.id}>{c.contrato} - {c.cliente}</option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Data Início *</label>
              <input
                type="date"
                required
                value={formData.dataInicio}
                onChange={(e) => setFormData(prev => ({ ...prev, dataInicio: e.target.value }))}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Data Fim</label>
              <input
                type="date"
                value={formData.dataFim}
                onChange={(e) => setFormData(prev => ({ ...prev, dataFim: e.target.value }))}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Status *</label>
            <select
              value={formData.status}
              onChange={(e) => setFormData(prev => ({ ...prev, status: e.target.value }))}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="PLANEJAMENTO">Planejamento</option>
              <option value="EM_ANDAMENTO">Em Andamento</option>
              <option value="CONCLUIDO">Concluído</option>
              <option value="SUSPENSO">Suspenso</option>
              <option value="CANCELADO">Cancelado</option>
            </select>
          </div>

          <div className="flex gap-3 pt-4">
            <button
              type="submit"
              className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              Salvar
            </button>
            <button
              type="button"
              onClick={() => setShowModal(false)}
              className="flex-1 px-4 py-2 bg-slate-200 text-slate-800 rounded-lg hover:bg-slate-300 transition-colors"
            >
              Cancelar
            </button>
          </div>
        </form>
      </Modal>
    </div>
  )
}