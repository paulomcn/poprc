import React, { useState } from 'react'
import { X, Loader, AlertCircle, CheckCircle } from 'lucide-react'
import api from '../services/api'

const STATUS_OPTIONS = [
  { value: 'ABERTA', label: 'Aberta', color: 'text-blue-600 bg-blue-50' },
  { value: 'EM_EXECUCAO', label: 'Em Execução', color: 'text-yellow-600 bg-yellow-50' },
  {
    value: 'AGUARDANDO_VALIDACAO',
    label: 'Aguardando Validação',
    color: 'text-purple-600 bg-purple-50',
  },
  { value: 'CONCLUIDA', label: 'Concluída', color: 'text-green-600 bg-green-50' },
  { value: 'FATURADA', label: 'Faturada', color: 'text-gray-600 bg-gray-50' },
]

export default function StatusModal({ ordem, statusPermitidos = [], onClose, onStatusAtualizado }) {
  const [novoStatus, setNovoStatus] = useState(ordem.status)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [sucesso, setSucesso] = useState(false)

  const handleAtualizarStatus = async () => {
    if (novoStatus === ordem.status) {
      onClose()
      return
    }

    setLoading(true)
    setError(null)
    setSucesso(false)

    try {
      const response = await api.put(`/ordens-servico/${ordem.id}/status`, {
        status: novoStatus,
      })

      setSucesso(true)
      setTimeout(() => {
        onStatusAtualizado(response.data)
      }, 1000)
    } catch (err) {
      setError(err.response?.data?.erro || 'Erro ao atualizar status. Tente novamente.')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-lg max-w-md w-full">
        {/* Cabeçalho Modal */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200">
          <h3 className="text-lg font-bold text-gray-900">Atualizar Status da OS</h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition"
            disabled={loading}
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        {/* Conteúdo Modal */}
        <div className="p-6 space-y-4">
          {/* Info OS Atual */}
          <div className="bg-gray-50 rounded-lg p-4 mb-4">
            <p className="text-xs text-gray-500 uppercase font-semibold">OS Atual</p>
            <p className="text-sm font-bold text-gray-900 mt-1">{ordem.numeroOs}</p>
          </div>

          {/* Mensagem de Sucesso */}
          {sucesso && (
            <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg flex items-center gap-2">
              <CheckCircle className="w-5 h-5 flex-shrink-0" />
              <span className="text-sm font-medium">Status atualizado com sucesso!</span>
            </div>
          )}

          {/* Mensagem de Erro */}
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-center gap-2">
              <AlertCircle className="w-5 h-5 flex-shrink-0" />
              <span className="text-sm font-medium">{error}</span>
            </div>
          )}

          {/* Seleção de Status */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-3">
              Novo Status
            </label>
            <div className="space-y-2">
              {STATUS_OPTIONS.filter(
                (status) => status.value === ordem.status || statusPermitidos.includes(status.value),
              ).map((status) => (
                <label
                  key={status.value}
                  className="flex items-center p-3 border-2 rounded-lg cursor-pointer transition hover:border-blue-500"
                  style={{
                    borderColor: novoStatus === status.value ? '#3b82f6' : '#e5e7eb',
                    backgroundColor: novoStatus === status.value ? '#f0f9ff' : 'white',
                  }}
                >
                  <input
                    type="radio"
                    name="status"
                    value={status.value}
                    checked={novoStatus === status.value}
                    onChange={(e) => setNovoStatus(e.target.value)}
                    className="w-4 h-4 text-blue-600 cursor-pointer"
                    disabled={loading}
                  />
                  <span
                    className={`ml-3 text-sm font-medium px-3 py-1 rounded ${status.color}`}
                  >
                    {status.label}
                  </span>
                </label>
              ))}
            </div>
          </div>
        </div>

        {/* Rodapé Modal */}
        <div className="flex gap-3 p-6 border-t border-gray-200">
          <button
            onClick={onClose}
            className="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg font-medium hover:bg-gray-50 transition disabled:opacity-50"
            disabled={loading}
          >
            Cancelar
          </button>
          <button
            onClick={handleAtualizarStatus}
            className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition disabled:opacity-50 flex items-center justify-center gap-2"
            disabled={loading || novoStatus === ordem.status}
          >
            {loading && <Loader className="w-4 h-4 animate-spin" />}
            {sucesso ? 'Atualizado' : 'Atualizar'}
          </button>
        </div>
      </div>
    </div>
  )
}
