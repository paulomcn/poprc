import { useState, useEffect } from 'react'
import { Plus, Minus, Package, AlertCircle } from 'lucide-react'
import api from '../services/api'
import Modal from '../components/Modal'
import LoadingSpinner from '../components/LoadingSpinner'
import Alert from '../components/Alert'

export default function PainelEstoque() {
  const [materiais, setMateriais] = useState([])
  const [funcionarios, setFuncionarios] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [successMessage, setSuccessMessage] = useState(null)
  const [showEntradaModal, setShowEntradaModal] = useState(false)
  const [showSaidaModal, setShowSaidaModal] = useState(false)
  const [formData, setFormData] = useState({
    materialId: '',
    quantidade: '',
    funcionarioId: ''
  })

  useEffect(() => {
    fetchData()
  }, [])

  const fetchData = async () => {
    try {
      setLoading(true)
      // Buscar materiais
      const materiaisResponse = await api.get('/estoque/materiais')
      setMateriais(materiaisResponse.data)
      
      // Buscar funcionários para o select
      const funcionariosResponse = await api.get('/funcionarios')
      setFuncionarios(funcionariosResponse.data)
      
      setError(null)
    } catch (err) {
      setError('Erro ao carregar dados do estoque')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleOpenEntradaModal = () => {
    setFormData({ materialId: '', quantidade: '', funcionarioId: '' })
    setShowEntradaModal(true)
  }

  const handleOpenSaidaModal = () => {
    setFormData({ materialId: '', quantidade: '', funcionarioId: '' })
    setShowSaidaModal(true)
  }

  const handleCloseModal = () => {
    setShowEntradaModal(false)
    setShowSaidaModal(false)
    setFormData({ materialId: '', quantidade: '', funcionarioId: '' })
  }

  const handleInputChange = (e) => {
    const { name, value } = e.target
    setFormData(prev => ({
      ...prev,
      [name]: name === 'quantidade' ? value : value
    }))
  }

  const handleSubmitEntrada = async (e) => {
    e.preventDefault()
    try {
      await api.post('/estoque/entrada', {
        materialId: parseInt(formData.materialId),
        quantidade: parseInt(formData.quantidade),
        funcionarioId: parseInt(formData.funcionarioId)
      })
      
      setSuccessMessage('Entrada de material registrada com sucesso!')
      setTimeout(() => setSuccessMessage(null), 4000)
      handleCloseModal()
      fetchData()
    } catch (err) {
      if (err.response?.data?.message?.includes('Saldo insuficiente')) {
        setError('Erro: Saldo insuficiente para realizar esta saída.')
      } else {
        setError(err.response?.data?.message || 'Erro ao registrar entrada de material')
      }
      console.error(err)
    }
  }

  const handleSubmitSaida = async (e) => {
    e.preventDefault()
    try {
      await api.post('/estoque/saida', {
        materialId: parseInt(formData.materialId),
        quantidade: parseInt(formData.quantidade),
        funcionarioId: parseInt(formData.funcionarioId)
      })
      
      setSuccessMessage('Saída de material registrada com sucesso!')
      setTimeout(() => setSuccessMessage(null), 4000)
      handleCloseModal()
      fetchData()
    } catch (err) {
      if (err.response?.data?.message?.includes('Saldo insuficiente')) {
        setError('Erro: Saldo insuficiente para realizar esta saída.')
      } else {
        setError(err.response?.data?.message || 'Erro ao registrar saída de material')
      }
      console.error(err)
    }
  }

  const getMaterialName = (materialId) => {
    const material = materiais.find(m => m.id === parseInt(materialId))
    return material ? material.nome : ''
  }

  const isCriticalStock = (quantidade) => {
    return quantidade <= 5
  }

  if (loading) return <LoadingSpinner />

  return (
    <div>
      {/* Header com título e botões de ação */}
      <div className="mb-8">
        <div className="flex justify-between items-start mb-6">
          <div>
            <h1 className="text-3xl font-bold text-slate-800">Estoque de Materiais</h1>
            <p className="text-slate-600 mt-2">Gerenciamento de entrada e saída de materiais</p>
          </div>
          <div className="flex gap-3">
            <button
              onClick={handleOpenEntradaModal}
              className="flex items-center gap-2 bg-green-600 hover:bg-green-700 text-white font-semibold py-3 px-4 rounded-lg transition-colors"
            >
              <Plus size={20} />
              Entrada de Material
            </button>
            <button
              onClick={handleOpenSaidaModal}
              className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 px-4 rounded-lg transition-colors"
            >
              <Minus size={20} />
              Saída de Material
            </button>
          </div>
        </div>

        {error && <Alert type="error" message={error} />}
        {successMessage && <Alert type="success" message={successMessage} />}
      </div>

      {/* Tabela de materiais */}
      <div className="bg-white rounded-lg shadow-md overflow-hidden border border-slate-200">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-50 border-b border-slate-200">
              <tr>
                <th className="px-6 py-4 text-left text-sm font-semibold text-slate-700">Nome do Material</th>
                <th className="px-6 py-4 text-left text-sm font-semibold text-slate-700">Part Number</th>
                <th className="px-6 py-4 text-left text-sm font-semibold text-slate-700">Fabricante</th>
                <th className="px-6 py-4 text-left text-sm font-semibold text-slate-700">Fornecedor</th>
                <th className="px-6 py-4 text-left text-sm font-semibold text-slate-700">Localização</th>
                <th className="px-6 py-4 text-center text-sm font-semibold text-slate-700">Qtd. Disponível</th>
              </tr>
            </thead>
            <tbody>
              {materiais.length > 0 ? (
                materiais.map((material) => (
                  <tr
                    key={material.id}
                    className={`border-b border-slate-200 hover:bg-slate-50 transition-colors ${
                      isCriticalStock(material.quantidadeDisponivel) ? 'bg-red-50' : ''
                    }`}
                  >
                    <td className="px-6 py-4 text-sm text-slate-800">
                      <div className="flex items-center gap-2">
                        <Package size={16} className="text-slate-400" />
                        {material.nome}
                      </div>
                    </td>
                    <td className="px-6 py-4 text-sm font-mono text-slate-800">{material.partNumber}</td>
                    <td className="px-6 py-4 text-sm text-slate-800">{material.fabricante}</td>
                    <td className="px-6 py-4 text-sm text-slate-800">{material.fornecedor}</td>
                    <td className="px-6 py-4 text-sm text-slate-800">{material.localizacao}</td>
                    <td className="px-6 py-4 text-center">
                      <span
                        className={`inline-flex items-center justify-center min-w-[3rem] py-1 px-3 rounded-full font-semibold text-sm ${
                          isCriticalStock(material.quantidadeDisponivel)
                            ? 'bg-red-200 text-red-800 font-bold'
                            : 'bg-green-100 text-green-800'
                        }`}
                      >
                        {isCriticalStock(material.quantidadeDisponivel) && (
                          <AlertCircle size={14} className="mr-1" />
                        )}
                        {material.quantidadeDisponivel}
                      </span>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="6" className="px-6 py-8 text-center text-slate-500">
                    Nenhum material encontrado no estoque
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal de Entrada de Material */}
      <Modal
        isOpen={showEntradaModal}
        onClose={handleCloseModal}
        title="Entrada de Material"
      >
        <form onSubmit={handleSubmitEntrada} className="space-y-6">
          {/* Seleção do Material */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              Selecionar Material
            </label>
            <select
              name="materialId"
              value={formData.materialId}
              onChange={handleInputChange}
              required
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent"
            >
              <option value="">-- Selecione um material --</option>
              {materiais.map((material) => (
                <option key={material.id} value={material.id}>
                  {material.nome} ({material.partNumber})
                </option>
              ))}
            </select>
          </div>

          {/* Quantidade */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              Quantidade
            </label>
            <input
              type="number"
              name="quantidade"
              value={formData.quantidade}
              onChange={handleInputChange}
              min="1"
              required
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent"
              placeholder="Digite a quantidade"
            />
          </div>

          {/* Funcionário */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              Funcionário Responsável
            </label>
            <select
              name="funcionarioId"
              value={formData.funcionarioId}
              onChange={handleInputChange}
              required
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent"
            >
              <option value="">-- Selecione um funcionário --</option>
              {funcionarios.map((func) => (
                <option key={func.id} value={func.id}>
                  {func.nome}
                </option>
              ))}
            </select>
          </div>

          {/* Botões */}
          <div className="flex gap-3 justify-end pt-4">
            <button
              type="button"
              onClick={handleCloseModal}
              className="px-6 py-2 border border-slate-300 text-slate-700 font-semibold rounded-lg hover:bg-slate-50 transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="px-6 py-2 bg-green-600 hover:bg-green-700 text-white font-semibold rounded-lg transition-colors"
            >
              Registrar Entrada
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal de Saída de Material */}
      <Modal
        isOpen={showSaidaModal}
        onClose={handleCloseModal}
        title="Saída de Material"
      >
        <form onSubmit={handleSubmitSaida} className="space-y-6">
          {/* Seleção do Material */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              Selecionar Material
            </label>
            <select
              name="materialId"
              value={formData.materialId}
              onChange={handleInputChange}
              required
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="">-- Selecione um material --</option>
              {materiais.map((material) => (
                <option key={material.id} value={material.id}>
                  {material.nome} ({material.partNumber}) - Disponível: {material.quantidadeDisponivel}
                </option>
              ))}
            </select>
          </div>

          {/* Quantidade */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              Quantidade
            </label>
            <input
              type="number"
              name="quantidade"
              value={formData.quantidade}
              onChange={handleInputChange}
              min="1"
              required
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="Digite a quantidade"
            />
          </div>

          {/* Funcionário */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              Funcionário Responsável
            </label>
            <select
              name="funcionarioId"
              value={formData.funcionarioId}
              onChange={handleInputChange}
              required
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="">-- Selecione um funcionário --</option>
              {funcionarios.map((func) => (
                <option key={func.id} value={func.id}>
                  {func.nome}
                </option>
              ))}
            </select>
          </div>

          {/* Aviso de estoque crítico */}
          {formData.materialId && (
            <div className="bg-yellow-50 border border-yellow-300 rounded-lg p-4">
              <p className="text-sm text-yellow-800">
                <span className="font-semibold">Quantidade disponível:</span>{' '}
                {getMaterialName(formData.materialId) && (
                  <span>
                    {materiais.find(m => m.id === parseInt(formData.materialId))?.quantidadeDisponivel} unidades
                  </span>
                )}
              </p>
            </div>
          )}

          {/* Botões */}
          <div className="flex gap-3 justify-end pt-4">
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
              Registrar Saída
            </button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
