import { useState, useEffect } from 'react'
import { Plus, Users } from 'lucide-react'
import api from '../services/api'
import Modal from '../components/Modal'
import LoadingSpinner from '../components/LoadingSpinner'
import Alert from '../components/Alert'

export default function Funcionarios() {
  const [funcionarios, setFuncionarios] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [formData, setFormData] = useState({
    nome: '',
    funcao: '',
    cidade: '',
    certificacoes: [],
    documentPaths: []
  })

  useEffect(() => {
    fetchFuncionarios()
  }, [])

  const fetchFuncionarios = async () => {
    try {
      setLoading(true)
      const response = await api.get('/funcionarios')
      setFuncionarios(response.data)
      setError(null)
    } catch (err) {
      setError('Erro ao carregar funcionários')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      const payload = {
        nome: formData.nome,
        funcao: formData.funcao,
        cidade: formData.cidade,
        certificacoes: formData.certificacoes.length > 0 
          ? formData.certificacoes.split(',').map(c => c.trim())
          : [],
        documentPaths: formData.documentPaths.length > 0 
          ? formData.documentPaths.split(',').map(p => p.trim())
          : []
      }

      await api.post('/funcionarios', payload)
      setShowModal(false)
      setFormData({
        nome: '',
        funcao: '',
        cidade: '',
        certificacoes: [],
        documentPaths: []
      })
      fetchFuncionarios()
    } catch (err) {
      setError('Erro ao salvar funcionário')
      console.error(err)
    }
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-slate-800">Funcionários</h1>
          <p className="text-slate-600 mt-2">Gerenciamento de colaboradores e RH</p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus size={20} />
          Novo Funcionário
        </button>
      </div>

      {error && (
        <Alert type="error" message={error} onClose={() => setError(null)} />
      )}

      {loading ? (
        <LoadingSpinner />
      ) : funcionarios.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <Users size={48} className="mx-auto text-slate-400 mb-4" />
          <p className="text-slate-600">Nenhum funcionário cadastrado</p>
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="w-full">
            <thead className="bg-slate-50 border-b border-slate-200">
              <tr>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-700">Nome</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-700">Função</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-700">Cidade</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-700">Certificações</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-700">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {funcionarios.map((funcionario) => (
                <tr key={funcionario.id} className="hover:bg-slate-50 transition-colors">
                  <td className="px-6 py-4 text-sm text-slate-800 font-medium">{funcionario.nome}</td>
                  <td className="px-6 py-4 text-sm text-slate-600">{funcionario.funcao}</td>
                  <td className="px-6 py-4 text-sm text-slate-600">{funcionario.cidade}</td>
                  <td className="px-6 py-4 text-sm text-slate-600">
                    {funcionario.certificacoes && funcionario.certificacoes.length > 0
                      ? funcionario.certificacoes.join(', ')
                      : '-'}
                  </td>
                  <td className="px-6 py-4 text-sm">
                    <button className="text-blue-600 hover:text-blue-800 font-medium">
                      Editar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal isOpen={showModal} onClose={() => setShowModal(false)} title="Novo Funcionário">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Nome *</label>
            <input
              type="text"
              required
              value={formData.nome}
              onChange={(e) => setFormData(prev => ({ ...prev, nome: e.target.value }))}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Nome completo"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Função *</label>
            <input
              type="text"
              required
              value={formData.funcao}
              onChange={(e) => setFormData(prev => ({ ...prev, funcao: e.target.value }))}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Ex: Engenheiro de Redes"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Cidade *</label>
            <input
              type="text"
              required
              value={formData.cidade}
              onChange={(e) => setFormData(prev => ({ ...prev, cidade: e.target.value }))}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Cidade"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Certificações</label>
            <input
              type="text"
              value={formData.certificacoes}
              onChange={(e) => setFormData(prev => ({ ...prev, certificacoes: e.target.value }))}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Separadas por vírgula (ex: CCNA, MCSA)"
            />
            <p className="text-xs text-slate-500 mt-1">Separe as certificações com vírgula</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Caminhos de Documentação</label>
            <input
              type="text"
              value={formData.documentPaths}
              onChange={(e) => setFormData(prev => ({ ...prev, documentPaths: e.target.value }))}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Separados por vírgula (ex: /docs/cv, /docs/cert)"
            />
            <p className="text-xs text-slate-500 mt-1">Caminhos dos arquivos de documentação</p>
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
