import { useState, useEffect } from 'react'
import { Plus, FileUp } from 'lucide-react'
import api from '../services/api'
import Modal from '../components/Modal'
import LoadingSpinner from '../components/LoadingSpinner'
import Alert from '../components/Alert'

export default function Contratos() {
  const [contratos, setContratos] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [formData, setFormData] = useState({
    cliente: '',
    contrato: '',
    edital: null,
    proposta: null,
    aditivos: []
  })

  useEffect(() => {
    fetchContratos()
  }, [])

  const fetchContratos = async () => {
    try {
      setLoading(true)
      const response = await api.get('/contratos')
      setContratos(response.data)
      setError(null)
    } catch (err) {
      setError('Erro ao carregar contratos')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      const form = new FormData()
      form.append('cliente', formData.cliente)
      form.append('contrato', formData.contrato)
      if (formData.edital) form.append('edital', formData.edital)
      if (formData.proposta) form.append('proposta', formData.proposta)
      if (formData.aditivos.length > 0) {
        formData.aditivos.forEach(file => form.append('aditivos', file))
      }

      await api.post('/contratos', form, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })

      setShowModal(false)
      setFormData({ cliente: '', contrato: '', edital: null, proposta: null, aditivos: [] })
      fetchContratos()
    } catch (err) {
      setError('Erro ao salvar contrato')
      console.error(err)
    }
  }

  const handleFileChange = (e, fieldName) => {
    const files = e.target.files
    if (fieldName === 'aditivos') {
      setFormData(prev => ({ ...prev, aditivos: Array.from(files) }))
    } else {
      setFormData(prev => ({ ...prev, [fieldName]: files[0] }))
    }
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-slate-800">Contratos</h1>
          <p className="text-slate-600 mt-2">Gerenciamento de contratos e documentação</p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus size={20} />
          Novo Contrato
        </button>
      </div>

      {error && (
        <Alert type="error" message={error} onClose={() => setError(null)} />
      )}

      {loading ? (
        <LoadingSpinner />
      ) : contratos.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <FileUp size={48} className="mx-auto text-slate-400 mb-4" />
          <p className="text-slate-600">Nenhum contrato cadastrado</p>
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="w-full">
            <thead className="bg-slate-50 border-b border-slate-200">
              <tr>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-700">Cliente</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-700">Número do Contrato</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-700">Vigência</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-700">Valor Global</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-700">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {contratos.map((contrato) => (
                <tr key={contrato.id} className="hover:bg-slate-50 transition-colors">
                  <td className="px-6 py-4 text-sm text-slate-800 font-medium">{contrato.cliente}</td>
                  <td className="px-6 py-4 text-sm text-slate-600">{contrato.contrato}</td>
                  <td className="px-6 py-4 text-sm text-slate-600">
                    {contrato.vigenciaInicio && contrato.vigenciaFim
                      ? `${new Date(contrato.vigenciaInicio).toLocaleDateString('pt-BR')} a ${new Date(contrato.vigenciaFim).toLocaleDateString('pt-BR')}`
                      : '-'}
                  </td>
                  <td className="px-6 py-4 text-sm text-slate-600">
                    {contrato.valorGlobal ? `R$ ${contrato.valorGlobal.toLocaleString('pt-BR')}` : '-'}
                  </td>
                  <td className="px-6 py-4 text-sm">
                    <button className="text-blue-600 hover:text-blue-800 font-medium">
                      Detalhes
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal isOpen={showModal} onClose={() => setShowModal(false)} title="Novo Contrato">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Cliente *</label>
            <input
              type="text"
              required
              value={formData.cliente}
              onChange={(e) => setFormData(prev => ({ ...prev, cliente: e.target.value }))}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Nome do cliente"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Número do Contrato *</label>
            <input
              type="text"
              required
              value={formData.contrato}
              onChange={(e) => setFormData(prev => ({ ...prev, contrato: e.target.value }))}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Ex: CT-2024-001"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Edital</label>
            <input
              type="file"
              onChange={(e) => handleFileChange(e, 'edital')}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg"
              accept=".pdf,.doc,.docx"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Proposta</label>
            <input
              type="file"
              onChange={(e) => handleFileChange(e, 'proposta')}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg"
              accept=".pdf,.doc,.docx"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Aditivos</label>
            <input
              type="file"
              multiple
              onChange={(e) => handleFileChange(e, 'aditivos')}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg"
              accept=".pdf,.doc,.docx"
            />
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
