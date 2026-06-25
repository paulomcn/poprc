import { useState, useEffect } from 'react'
import { Bell, Mail, MessageSquare, Save, Play, ShieldAlert, Package, FileText } from 'lucide-react'
import api from '../services/api'
import Alert from '../components/Alert'

export default function ConfiguracaoNotificacoes() {
  const [settings, setSettings] = useState({
    emailGestor: '',
    whatsappGestor: '',
    alertaOsAtrasada: true,
    alertaEstoqueCritico: true,
    alertaContratoVencendo: true,
  })

  const [loadingTest, setLoadingTest] = useState(false)
  const [successMessage, setSuccessMessage] = useState(null)
  const [errorMessage, setErrorMessage] = useState(null)

  // 💥 CARREGA DO BANCO REAL
  useEffect(() => {
    async function loadDbSettings() {
      try {
        const response = await api.get('/alertas/configuracoes')
        setSettings(response.data)
      } catch (err) {
        setErrorMessage('Erro ao carregar configurações do servidor.')
      }
    }
    loadDbSettings()
  }, [])

  const handleInputChange = (e) => {
    const { name, value } = e.target
    setSettings(prev => ({ ...prev, [name]: value }))
  }

  const handleToggleChange = (field) => {
    setSettings(prev => ({ ...prev, [field]: !prev[field] }))
  }

  // 💥 SALVA NO BANCO REAL
  const handleSaveSettings = async (e) => {
    e.preventDefault()
    try {
      await api.post('/alertas/configuracoes', settings)
      setSuccessMessage('Configurações salvas no servidor com sucesso!')
      setTimeout(() => setSuccessMessage(null), 3000)
    } catch (err) {
      setErrorMessage('Falha ao salvar no banco de dados.')
    }
  }

  const handleTriggerManualAlerts = async () => {
    try {
      setLoadingTest(true)
      const response = await api.post('/alertas/disparar-todos')
      if (response.data.status === 'sucesso') {
        setSuccessMessage('Varredura forçada! Verifique o console do servidor.')
      }
    } catch (err) {
      setErrorMessage('Falha ao acionar o motor de alertas.')
    } finally {
      setLoadingTest(false)
      setTimeout(() => setSuccessMessage(null), 5000)
    }
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-slate-800 flex items-center gap-2">
          <Bell className="text-slate-700" size={32} /> Configuração de Notificações
        </h1>
        {successMessage && <div className="mt-4"><Alert type="success" message={successMessage} /></div>}
        {errorMessage && <div className="mt-4"><Alert type="error" message={errorMessage} /></div>}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <form onSubmit={handleSaveSettings} className="bg-white p-6 rounded-xl shadow-md border border-slate-200 lg:col-span-2 space-y-6">
          <h2 className="text-lg font-bold text-slate-800 border-b pb-3">Destinatários</h2>
          
          <div className="space-y-2">
            <label className="text-sm font-semibold text-slate-700 flex items-center gap-2"><Mail size={18} /> E-mail</label>
            <input type="email" name="emailGestor" value={settings.emailGestor} onChange={handleInputChange} className="w-full px-4 py-2 border rounded-lg" required />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold text-slate-700 flex items-center gap-2"><MessageSquare size={18} /> WhatsApp</label>
            <input type="text" name="whatsappGestor" value={settings.whatsappGestor} onChange={handleInputChange} className="w-full px-4 py-2 border rounded-lg" required />
          </div>

          <h2 className="text-lg font-bold text-slate-800 border-b pt-4 pb-3">Regras de Varredura</h2>
          
          {['alertaOsAtrasada', 'alertaEstoqueCritico', 'alertaContratoVencendo'].map((field) => (
            <div key={field} className="flex items-center justify-between p-4 bg-slate-50 rounded-xl border">
              <span className="font-medium capitalize">{field.replace('alerta', 'Alerta de ')}</span>
              <button type="button" onClick={() => handleToggleChange(field)} className={`w-12 h-6 rounded-full p-1 ${settings[field] ? 'bg-blue-600' : 'bg-slate-300'}`}>
                <div className={`bg-white w-4 h-4 rounded-full transition-transform ${settings[field] ? 'translate-x-6' : 'translate-x-0'}`} />
              </button>
            </div>
          ))}

          <button type="submit" className="flex items-center gap-2 px-6 py-3 bg-blue-600 text-white font-semibold rounded-lg">
            <Save size={18} /> Salvar Parâmetros
          </button>
        </form>

        <div className="bg-white p-6 rounded-xl shadow-md border border-slate-200 h-fit space-y-4">
          <h3 className="font-bold flex items-center gap-2"><Play className="text-green-600" size={18} /> Homologação</h3>
          <button onClick={handleTriggerManualAlerts} disabled={loadingTest} className="w-full py-3 bg-green-50 text-green-700 font-semibold rounded-lg">
            {loadingTest ? 'Executando...' : 'Disparar Varredura Manual'}
          </button>
        </div>
      </div>
    </div>
  )
}