import React, { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  ArrowLeft,
  MapPin,
  Briefcase,
  Calendar,
  AlertCircle,
  CheckCircle2,
  Navigation,
  CheckSquare,
  Camera,
  RefreshCw
} from 'lucide-react'
import api from '../services/api'

export default function ExecutarOrdemServico() {
  const { id } = useParams() // Pega o ID da OS vindo da URL (ex: /tecnico/os/1)
  const navigate = useNavigate()

  // Estados da Página
  const [os, setOs] = useState(null)
  const [loading, setLoading] = useState(true)
  const [uploadingFoto, setUploadingFoto] = useState(false)
  const [feedbackMsg, setFeedbackMsg] = useState({ tipo: '', texto: '' })

  // Dados mocados idênticos aos da lista para caso o banco não tenha essa OS ainda
  const osFallback = {
    id: parseInt(id) || 1,
    numeroOs: `OS-2026-04${id || '2'}`,
    cliente: 'Tribunal de Justiça - Comarca Centro',
    status: 'EM_EXECUCAO',
    descricao: 'Instalação de racks de telecomunicação, cabeamento estruturado Cat6 e certificação de 24 pontos de rede.',
    endereco: 'Praça D. Pedro II, s/n - Centro, Salvador - BA',
    prioridade: 'Alta',
    dataExecucao: '2026-06-15',
    contato: 'Carlos Souza (Coordenador de TI) - (71) 99888-7766',
    tarefas: [
      { id: 1, texto: 'Vistoria técnica do local e infraestrutura', concluida: true },
      { id: 2, texto: 'Instalação física do Rack de rede de 19"', concluida: false },
      { id: 3, texto: 'Lançamento e crimpagem de cabos Cat6', concluida: false },
      { id: 4, texto: 'Certificação e testes de conectividade', concluida: false }
    ]
  }

  useEffect(() => {
    async function puxarDetalhesOS() {
      try {
        setLoading(true)
        const response = await api.get(`/ordens-servico/${id}`)
        setOs(response.data)
      } catch (err) {
        console.warn('OS não encontrada no banco, aplicando mock de segurança para testes na UI.')
        setOs(osFallback)
      } finally {
        setLoading(false)
      }
    }
    puxarDetalhesOS()
  }, [id])

  // Alternar a conclusão de uma tarefa localmente
  const handleToggleTarefa = (tarefaId) => {
    setOs(prev => ({
      ...prev,
      tarefas: prev.tarefas.map(t => t.id === tarefaId ? { ...t, concluida: !t.concluida } : t)
    }))
  }

  // Mudar status da OS direto no backend
  const handleMudarStatus = async (novoStatus) => {
    try {
      await api.put(`/ordens-servico/${id}/status`, { status: novoStatus })
      setOs(prev => ({ ...prev, status: novoStatus }))
      mostrarFeedback('sucesso', `Status da OS alterado para ${novoStatus.replace('_', ' ')}!`)
    } catch (err) {
      setOs(prev => ({ ...prev, status: novoStatus }))
      mostrarFeedback('sucesso', `[Simulado] Status alterado para ${novoStatus.replace('_', ' ')}`)
    }
  }

  // 💥 CONEXÃO REAL COM O BACKEND JAVA (Módulo 12 - Evidências Fotográficas)
  const handleUploadFotoEvidencia = async (e) => {
    const file = e.target.files[0]
    if (!file) return

    setUploadingFoto(true)
    setFeedbackMsg({ tipo: '', texto: '' })

    // Captura o GPS na hora exata do upload para enviar ao Java
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const lat = position.coords.latitude.toFixed(6)
        const lon = position.coords.longitude.toFixed(6)

        // Monta o FormData exatamente como o nosso 'MobilidadeController.java' espera!
        const formData = new FormData()
        formData.append('file', file)
        formData.append('ordemServicoId', os.id)
        formData.append('funcionarioId', 1) // Usando ID 1 do funcionário que criamos no pgAdmin
        formData.append('latitude', lat)
        formData.append('longitude', lon)

        try {
          console.log('Disparando foto real para o backend Spring Boot...')
          const response = await api.post('/campo/upload-foto', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
          })
          console.log('Resposta do Java:', response.data)
          mostrarFeedback('sucesso', 'Evidência fotográfica salva com sucesso no servidor!')
        } catch (err) {
          console.error(err)
          mostrarFeedback('erro', 'Falha ao enviar arquivo para o servidor. Verifique a API Java.')
        } finally {
          setUploadingFoto(false)
        }
      },
      (error) => {
        setUploadingFoto(false)
        mostrarFeedback('erro', 'Obrigatório ativar o GPS para registrar a evidência fotográfica da OS.')
      },
      { enableHighAccuracy: true, timeout: 8000 }
    )
  }

  const mostrarFeedback = (tipo, texto) => {
    setFeedbackMsg({ tipo, texto })
    setTimeout(() => setFeedbackMsg({ tipo: '', texto: '' }), 5000)
  }

  const abrirNoMaps = (endereco) => {
    window.open(`https://maps.google.com/?q=${encodeURIComponent(endereco)}`, '_blank')
  }

  if (loading) {
    return (
      <div className="bg-slate-950 min-h-screen flex flex-col items-center justify-center text-slate-400">
        <RefreshCw className="w-10 h-10 animate-spin text-indigo-500 mb-2" />
        <p className="text-sm font-semibold">Carregando dados da Ordem de Serviço...</p>
      </div>
    )
  }

  return (
    <div className="bg-slate-950 min-h-screen text-slate-100 p-4 sm:p-6 lg:p-8 font-sans antialiased">
      <div className="max-w-4xl mx-auto space-y-6">
        
        {/* Botão de Voltar Volante */}
        <button 
          onClick={() => navigate('/tecnico')}
          className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-slate-400 hover:text-white transition group"
        >
          <ArrowLeft className="w-4 h-4 transition-transform group-hover:-translate-x-1" />
          Voltar para o Painel
        </button>

        {/* Header Principal da OS */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 flex flex-col md:flex-row justify-between items-start md:items-center gap-4 shadow-xl">
          <div>
            <span className="text-indigo-400 text-xs font-black tracking-widest uppercase">{os.numeroOs}</span>
            <h1 className="text-xl sm:text-2xl font-black text-white tracking-tight mt-1">{os.cliente}</h1>
            <div className="flex items-center gap-1.5 text-xs text-slate-400 mt-2">
              <Calendar className="w-4 h-4 text-slate-500" />
              <span>Agendado para: {new Date(os.dataExecucao).toLocaleDateString('pt-BR')}</span>
            </div>
          </div>
          <span className={`text-xs font-black px-3 py-1 rounded-full border ${
            os.status === 'CONCLUIDA' ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' : 
            os.status === 'EM_EXECUCAO' ? 'bg-amber-500/10 text-amber-400 border-amber-500/20' : 
            'bg-indigo-500/10 text-indigo-400 border-indigo-500/20'
          }`}>
            {os.status.replace('_', ' ')}
          </span>
        </div>

        {/* Toasts de Notificação */}
        {feedbackMsg.texto && (
          <div className={`border p-4 rounded-xl flex gap-3 animate-fadeIn text-xs ${
            feedbackMsg.tipo === 'sucesso' ? 'bg-emerald-950/40 border-emerald-500/30 text-emerald-200' : 'bg-rose-950/40 border-rose-500/30 text-rose-200'
          }`}>
            {feedbackMsg.tipo === 'sucesso' ? <CheckCircle2 className="w-5 h-5 text-emerald-400 flex-shrink-0" /> : <AlertCircle className="w-5 h-5 text-rose-400 flex-shrink-0" />}
            <p className="font-semibold">{feedbackMsg.texto}</p>
          </div>
        )}

        {/* Corpo em Grid Duplo Responsivo (Fica 1 coluna no celular e 2 colunas no PC) */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          
          {/* LADO ESQUERDO: Informações e Metadados (Ocupa 2 colunas no PC) */}
          <div className="md:col-span-2 space-y-6">
            
            {/* Box de Descrição */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-3 shadow-md">
              <h3 className="text-xs font-extrabold text-white uppercase tracking-wider flex items-center gap-2">
                <Briefcase className="w-4 h-4 text-indigo-400" /> Instruções do Serviço
              </h3>
              <p className="text-sm text-slate-300 leading-relaxed bg-slate-950/40 p-4 border border-slate-800 rounded-xl">
                {os.descricao}
              </p>
            </div>

            {/* Checklist de Atividades */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-4 shadow-md">
              <h3 className="text-xs font-extrabold text-white uppercase tracking-wider">Checklist de Atividades</h3>
              <div className="space-y-2 bg-slate-950/40 p-4 border border-slate-800 rounded-xl">
                {os.tarefas.map(tarefa => (
                  <div 
                    key={tarefa.id}
                    onClick={() => handleToggleTarefa(tarefa.id)}
                    className="flex items-start gap-3 py-2.5 cursor-pointer select-none transition hover:bg-slate-900/40 px-2 rounded-lg"
                  >
                    <div className="mt-0.5">
                      {tarefa.concluida ? (
                        <CheckSquare className="w-4 h-4 text-emerald-400" />
                      ) : (
                        <div className="w-4 h-4 border border-slate-600 rounded-sm" />
                      )}
                    </div>
                    <span className={`text-sm ${tarefa.concluida ? 'line-through text-slate-500' : 'text-slate-300'}`}>
                      {tarefa.texto}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* LADO DIREITO: Endereço, Contato e Upload da Câmera */}
          <div className="md:col-span-1 space-y-6">
            
            {/* Box de Endereço */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-4 shadow-md">
              <div className="flex justify-between items-center">
                <h4 className="text-xs font-extrabold text-white uppercase tracking-wider">Localização</h4>
                <button 
                  onClick={() => abrirNoMaps(os.endereco)}
                  className="text-[10px] text-indigo-400 font-bold hover:underline flex items-center gap-1"
                >
                  <Navigation className="w-3 h-3" /> Rota GPS
                </button>
              </div>
              <div className="bg-slate-950/40 p-3.5 border border-slate-800 rounded-xl flex items-start gap-2.5">
                <MapPin className="w-4 h-4 text-rose-500 flex-shrink-0 mt-0.5" />
                <span className="text-xs text-slate-300 leading-normal">{os.endereco}</span>
              </div>
              <div className="bg-slate-950/20 p-3 border border-slate-800/60 rounded-xl text-xs text-slate-400">
                <span className="font-bold text-slate-300 block mb-1">Contato Responsável:</span>
                {os.contato}
              </div>
            </div>

            {/* Upload de Relatório Fotográfico */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-4 shadow-md">
              <h4 className="text-xs font-extrabold text-white uppercase tracking-wider">Relatório Fotográfico</h4>
              <div className="bg-slate-950/40 p-4 border border-slate-800 rounded-xl flex flex-col items-center justify-center gap-3 text-center">
                {uploadingFoto ? (
                  <div className="flex flex-col items-center gap-2 text-xs text-slate-400 py-4">
                    <RefreshCw className="w-6 h-6 text-indigo-400 animate-spin" />
                    <span>Enviando foto ao servidor...</span>
                  </div>
                ) : (
                  <>
                    <Camera className="w-8 h-8 text-slate-500" />
                    <span className="text-[11px] text-slate-400 leading-normal">
                      Tire a foto de conclusão para salvar a evidência no banco.
                    </span>
                    <label className="w-full text-center py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-200 font-bold text-xs rounded-xl cursor-pointer border border-slate-700 transition active:scale-95">
                      Capturar Câmera
                      <input 
                        type="file" 
                        accept="image/*" 
                        capture="environment" // 💥 Força os navegadores mobile a abrirem direto a câmera do celular
                        className="hidden" 
                        onChange={handleUploadFotoEvidencia}
                      />
                    </label>
                  </>
                )}
              </div>
            </div>

            {/* Ações de Status da OS */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 flex flex-col gap-2 shadow-md">
              {os.status === 'ABERTA' && (
                <button
                  onClick={() => handleMudarStatus('EM_EXECUCAO')}
                  className="w-full py-3 bg-indigo-600 hover:bg-indigo-500 active:scale-95 text-white font-bold text-xs rounded-xl transition shadow-md shadow-indigo-600/10"
                >
                  Iniciar Atendimento
                </button>
              )}
              
              {os.status === 'EM_EXECUCAO' && (
                <button
                  onClick={() => handleMudarStatus('CONCLUIDA')}
                  className="w-full py-3 bg-emerald-600 hover:bg-emerald-500 active:scale-95 text-white font-bold text-xs rounded-xl transition shadow-md shadow-emerald-600/10"
                >
                  Concluir Atendimento
                </button>
              )}

              {os.status === 'CONCLUIDA' && (
                <div className="w-full text-center py-2.5 text-xs font-bold text-emerald-400 flex items-center justify-center gap-1.5 bg-emerald-950/20 rounded-xl border border-emerald-900/30">
                  <CheckCircle2 className="w-4 h-4" />
                  OS Finalizada com Sucesso!
                </div>
              )}
            </div>

          </div>

        </div>
      </div>
    </div>
  )
}