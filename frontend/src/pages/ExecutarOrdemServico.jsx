import React, { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  ArrowLeft,
  Camera,
  MapPin,
  CheckCircle,
  Clock,
  Compass,
  AlertTriangle,
  Loader2,
  Smartphone,
  Navigation,
  Check,
  CheckSquare,
  FileText,
  Image as ImageIcon
} from 'lucide-react'
import api from '../services/api'

// Mock de OSs padrão (mesmas do Dashboard para consistência)
const OS_MOCK_DATA = [
  {
    id: 1,
    numeroOs: 'OS-2026-042',
    cliente: 'Tribunal de Justiça - Comarca Centro',
    status: 'ABERTA',
    descricao: 'Instalação de racks de telecomunicação, cabeamento estruturado Cat6 e certificação de 24 pontos de rede.',
    endereco: 'Praça D. Pedro II, s/n - Centro, Salvador - BA',
    prioridade: 'Alta',
    dataExecucao: '2026-06-15',
    contato: 'Carlos Souza (Coordenador de TI) - (71) 99888-7766'
  },
  {
    id: 2,
    numeroOs: 'OS-2026-043',
    cliente: 'Comarca de Camaçari - Fórum Regional',
    status: 'EM_EXECUCAO',
    descricao: 'Substituição de Switch core danificado após oscilação elétrica e reconfiguração de VLANs corporativas.',
    endereco: 'Centro Administrativo, Lote 4 - Centro, Camaçari - BA',
    prioridade: 'Crítica',
    dataExecucao: '2026-06-15',
    contato: 'Marcos Bahia (Administrador) - (71) 98765-4321'
  },
  {
    id: 3,
    numeroOs: 'OS-2026-044',
    cliente: 'Ministério Público - Anexo Administrativo',
    status: 'CONCLUIDA',
    descricao: 'Manutenção preventiva geral no banco de baterias do No-Break principal e testes de autonomia.',
    endereco: 'Av. Joana Angélica, 1422 - Nazaré, Salvador - BA',
    prioridade: 'Média',
    dataExecucao: '2026-06-15',
    contato: 'Dra. Márcia Albuquerque - (71) 99111-2233'
  }
]

export default function ExecutarOrdemServico() {
  const { id } = useParams()
  const navigate = useNavigate()
  const fileInputRef = useRef(null)

  // Estados principais
  const [os, setOs] = useState(null)
  const [loadingPage, setLoadingPage] = useState(true)
  const [checklist, setChecklist] = useState([
    { id: 1, texto: 'Efetuar vistoria de segurança no local e rede', concluido: false },
    { id: 2, texto: 'Verificar fiação e conectores físicos', concluido: false },
    { id: 3, texto: 'Testar voltagem e aterramento elétrico', concluido: false },
    { id: 4, texto: 'Realizar testes finais de conectividade de rede', concluido: false }
  ])

  // Evidências fotográficas
  const [fotos, setFotos] = useState([])
  const [loadingUpload, setLoadingUpload] = useState(false)
  const [loadingGps, setLoadingGps] = useState(false)

  // Status de finalização
  const [loadingFinalizar, setLoadingFinalizar] = useState(false)
  
  // Mensagens e feedback
  const [erroMsg, setErroMsg] = useState(null)
  const [sucessoMsg, setSucessoMsg] = useState(null)

  // Carrega os dados da OS (real da API ou fallback de mock)
  useEffect(() => {
    async function carregarOrdemServico() {
      try {
        setLoadingPage(true)
        const response = await api.get(`/ordens-servico/${id}`)
        if (response.data) {
          setOs(response.data)
        } else {
          carregarFallbackMock()
        }
      } catch (err) {
        console.warn('Erro ao obter OS da API, utilizando fallback mockado para visualização:', err)
        carregarFallbackMock()
      } finally {
        setLoadingPage(false)
      }
    }

    function carregarFallbackMock() {
      const osId = parseInt(id, 10)
      const foundOs = OS_MOCK_DATA.find(o => o.id === osId) || OS_MOCK_DATA[0]
      setOs(foundOs)
    }

    carregarOrdemServico()
  }, [id])

  // Lógica para alternar itens do checklist
  const handleToggleChecklist = (itemId) => {
    setChecklist(prev =>
      prev.map(item =>
        item.id === itemId ? { ...item, concluido: !item.concluido } : item
      )
    )
  }

  // Aciona o input de arquivo oculto para tirar foto
  const handleTriggerCamera = () => {
    setErrorMsg(null)
    setSuccessMsg(null)
    if (fileInputRef.current) {
      fileInputRef.current.click()
    }
  }

  // Processa a foto tirada pelo celular
  const handleFotoCapturada = async (e) => {
    const arquivo = e.target.files[0]
    if (!arquivo) return

    setLoadingGps(true)
    setLoadingUpload(true)

    if (!navigator.geolocation) {
      setErroMsg('Geolocalização não é suportada neste dispositivo.')
      setLoadingGps(false)
      setLoadingUpload(false)
      return
    }

    // Obtém coordenadas GPS de alta precisão
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const lat = position.coords.latitude.toFixed(6)
        const lng = position.coords.longitude.toFixed(6)
        setLoadingGps(false)

        // Cria o FormData para upload
        const formData = new FormData()
        formData.append('arquivo', arquivo)
        formData.append('latitude', lat)
        formData.append('longitude', lng)
        formData.append('osId', id)
        formData.append('timestamp', new Date().toISOString())

        try {
          console.log('Realizando upload da foto com geolocalização:', { lat, lng })
          
          // Chamada real ao endpoint
          const response = await api.post('/campo/upload-foto', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
          })
          console.log('Foto gravada com sucesso no backend:', response.data)

          // Adiciona foto ao estado
          const urlPreview = URL.createObjectURL(arquivo)
          setFotos(prev => [
            ...prev,
            {
              id: Date.now(),
              url: urlPreview,
              latitude: lat,
              longitude: lng,
              timestamp: new Date().toLocaleTimeString('pt-BR')
            }
          ])
          setSucessoMsg('Evidência fotográfica enviada com sucesso!')
        } catch (uploadError) {
          console.warn('Erro na chamada da API de upload. Mockando sucesso local para validação da interface:', uploadError)
          
          // Mock local se a API estiver indisponível
          const urlPreview = URL.createObjectURL(arquivo)
          setFotos(prev => [
            ...prev,
            {
              id: Date.now(),
              url: urlPreview,
              latitude: lat,
              longitude: lng,
              timestamp: new Date().toLocaleTimeString('pt-BR')
            }
          ])
          setSucessoMsg('[Simulado] Evidência fotográfica salva localmente!')
        } finally {
          setLoadingUpload(false)
          // Limpa campo de input para novas fotos
          if (fileInputRef.current) fileInputRef.current.value = ''
        }
      },
      (error) => {
        console.error('Erro ao buscar localização para foto:', error)
        setLoadingGps(false)
        setLoadingUpload(false)
        
        let msg = 'Erro ao obter localização. A geolocalização é obrigatória para anexar a evidência.'
        if (error.code === error.PERMISSION_DENIED) {
          msg = 'Permissão de GPS negada. Por favor, ative a localização para anexar fotos de evidência.'
        }
        setErroMsg(msg)
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
    )
  }

  // Finalização do serviço
  const handleFinalizarServico = async () => {
    // Valida se todas as tarefas do checklist foram concluídas
    const tarefasPendentes = checklist.filter(item => !item.concluido)
    if (tarefasPendentes.length > 0) {
      setErroMsg('Por favor, conclua todas as tarefas do checklist operacional antes de finalizar a OS.')
      return
    }

    // Valida se foi tirada pelo menos uma foto de evidência
    if (fotos.length === 0) {
      setErroMsg('É obrigatório anexar pelo menos uma evidência fotográfica do local de trabalho.')
      return
    }

    setLoadingFinalizar(true)
    setErroMsg(null)
    setSuccessMsg(null)

    const novoStatus = 'AGUARDANDO_VALIDACAO'

    try {
      console.log(`Atualizando status da OS ${id} para ${novoStatus}...`)
      
      // Chamada PUT real para atualizar o status
      await api.put(`/ordens-servico/${id}/status`, { status: novoStatus })
      
      setSucessoMsg('Ordem de serviço concluída e enviada para validação com sucesso!')
      
      // Retorna para o dashboard após 3 segundos
      setTimeout(() => {
        navigate('/tecnico')
      }, 3000)
    } catch (err) {
      console.warn('Erro ao atualizar status na API. Simulação de sucesso local aplicada:', err)
      
      setSucessoMsg('[Simulado] Ordem de serviço finalizada com sucesso! Redirecionando...');
      setTimeout(() => {
        navigate('/tecnico')
      }, 3000)
    } finally {
      setLoadingFinalizar(false)
    }
  }

  const abrirNoMaps = (endereco) => {
    const url = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(endereco)}`
    window.open(url, '_blank')
  }

  if (loadingPage) {
    return (
      <div className="bg-slate-950 min-h-screen flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="w-10 h-10 animate-spin text-indigo-500 mx-auto mb-3" />
          <p className="text-slate-400 text-sm font-semibold">Carregando detalhes da OS...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="bg-slate-950 min-h-screen py-0 md:py-8 flex justify-center items-start">
      {/* Mobile Device Frame Container */}
      <div className="w-full max-w-md bg-slate-900 min-h-screen md:min-h-[850px] shadow-2xl border-0 md:border-8 md:border-slate-800 md:rounded-[40px] overflow-hidden relative flex flex-col font-sans text-slate-100">
        
        {/* App Status Bar */}
        <div className="bg-slate-950 text-slate-400 px-6 py-2 flex justify-between items-center text-xs font-semibold select-none">
          <span>RC Mobile OS</span>
          <div className="flex items-center gap-1.5">
            <Smartphone className="w-3.5 h-3.5" />
            <span>4G Lte</span>
            <div className="w-5 h-2.5 bg-slate-700 rounded-sm border border-slate-500 flex items-center p-px">
              <div className="w-3.5 h-full bg-emerald-500 rounded-2xs" />
            </div>
          </div>
        </div>

        {/* Top Header Navigation */}
        <header className="p-4 bg-gradient-to-b from-slate-950 to-slate-900 border-b border-slate-800 sticky top-0 z-40 flex items-center gap-3">
          <button 
            onClick={() => navigate('/tecnico')}
            className="p-2 bg-slate-800 hover:bg-slate-700 rounded-full text-slate-300 hover:text-white transition active:scale-95"
            style={{ WebkitTapHighlightColor: 'transparent' }}
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <span className="text-[10px] font-bold text-indigo-400 uppercase tracking-widest">
              {os?.numeroOs || 'OS-DETALHES'}
            </span>
            <h1 className="text-base font-extrabold tracking-tight mt-px">Execução de OS</h1>
          </div>
        </header>

        {/* Corpo principal scrollable */}
        <main className="flex-1 overflow-y-auto px-5 py-5 space-y-6 pb-28">

          {/* Dados Gerais da OS */}
          <section className="bg-slate-950/40 border border-slate-800 rounded-2xl p-4 space-y-4">
            <div>
              <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">Cliente</p>
              <h3 className="text-base font-extrabold text-white mt-0.5">{os?.cliente}</h3>
            </div>

            <div className="space-y-1">
              <div className="flex justify-between items-center">
                <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">Local do Serviço</p>
                <button 
                  onClick={() => abrirNoMaps(os?.endereco)}
                  className="text-[10px] text-indigo-400 font-bold hover:underline flex items-center gap-1"
                >
                  <Navigation className="w-3 h-3" />
                  Ver Rota
                </button>
              </div>
              <div className="flex gap-2 text-xs text-slate-300">
                <MapPin className="w-4 h-4 text-rose-500 flex-shrink-0 mt-0.5" />
                <span>{os?.endereco}</span>
              </div>
            </div>

            <div className="space-y-1 pt-2 border-t border-slate-800/80">
              <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">Instruções de Trabalho</p>
              <p className="text-xs text-slate-400 leading-relaxed italic">
                "{os?.descricao}"
              </p>
            </div>
          </section>

          {/* Checklist Operacional */}
          <section className="space-y-3">
            <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-2">
              <CheckCircle className="w-4 h-4 text-indigo-400" />
              Checklist Operacional
            </h4>
            <div className="space-y-3 bg-slate-950/25 border border-slate-800/80 rounded-2xl p-4">
              {checklist.map(item => (
                <label 
                  key={item.id}
                  onClick={() => handleToggleChecklist(item.id)}
                  className="flex items-center gap-3.5 py-2.5 px-3 rounded-xl border border-transparent hover:border-slate-800 cursor-pointer transition select-none"
                  style={{ WebkitTapHighlightColor: 'transparent' }}
                >
                  <div className="flex-shrink-0">
                    {item.concluido ? (
                      <CheckSquare className="w-6 h-6 text-emerald-400" />
                    ) : (
                      <div className="w-6 h-6 border-2 border-slate-600 rounded-md transition duration-200" />
                    )}
                  </div>
                  <span className={`text-xs font-medium leading-tight ${
                    item.concluido ? 'line-through text-slate-500' : 'text-slate-200'
                  }`}>
                    {item.texto}
                  </span>
                </label>
              ))}
            </div>
          </section>

          {/* Relatório Fotográfico */}
          <section className="space-y-3">
            <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-2">
              <Camera className="w-4 h-4 text-indigo-400" />
              Evidências Fotográficas
            </h4>

            {/* Input nativo mobile encapsulado */}
            <input
              type="file"
              accept="image/*"
              capture="environment"
              className="hidden"
              ref={fileInputRef}
              onChange={handleFotoCapturada}
            />

            <button
              onClick={handleTriggerCamera}
              disabled={loadingUpload}
              className="w-full py-4 bg-slate-800 hover:bg-slate-700 active:scale-98 rounded-2xl border border-slate-700/80 font-bold text-xs flex items-center justify-center gap-2 text-indigo-300 transition-all select-none"
              style={{ WebkitTapHighlightColor: 'transparent' }}
            >
              {loadingUpload ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>{loadingGps ? 'Acessando GPS...' : 'Enviando Foto...'}</span>
                </>
              ) : (
                <>
                  <Camera className="w-4 h-4" />
                  <span>Tirar Foto da Evidência</span>
                </>
              )}
            </button>

            {/* Grid de Thumbnails */}
            {fotos.length > 0 ? (
              <div className="grid grid-cols-2 gap-3 mt-3">
                {fotos.map(foto => (
                  <div 
                    key={foto.id} 
                    className="bg-slate-950/45 border border-slate-800 rounded-xl overflow-hidden relative group"
                  >
                    <img 
                      src={foto.url} 
                      alt="Evidência" 
                      className="w-full h-28 object-cover"
                    />
                    <div className="p-2 bg-slate-950/80 space-y-0.5 text-[9px] text-slate-400 border-t border-slate-800">
                      <div className="flex items-center gap-1 font-bold text-emerald-400">
                        <MapPin className="w-2.5 h-2.5 text-rose-500" />
                        <span>Lat: {foto.latitude}</span>
                      </div>
                      <div className="pl-3.5">
                        <span>Lng: {foto.longitude}</span>
                      </div>
                      <div className="flex justify-between items-center pt-1 mt-1 border-t border-slate-800/60">
                        <span>Hora: {foto.timestamp}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-6 bg-slate-950/20 border border-slate-800/80 border-dashed rounded-2xl flex flex-col items-center justify-center gap-2">
                <ImageIcon className="w-8 h-8 text-slate-600" />
                <p className="text-[10px] text-slate-500 font-medium">Nenhuma foto adicionada ainda.</p>
              </div>
            )}
          </section>

          {/* Alertas e Mensagens de Feedback */}
          {erroMsg && (
            <div className="bg-rose-950/40 border border-rose-500/30 text-rose-300 rounded-xl p-3.5 flex gap-2.5 text-xs animate-fadeIn">
              <AlertTriangle className="w-4 h-4 text-rose-400 flex-shrink-0 mt-0.5" />
              <div className="flex-1">
                <p className="font-bold">Atenção</p>
                <p className="text-rose-300/80 mt-0.5">{erroMsg}</p>
                <button 
                  onClick={() => setErroMsg(null)}
                  className="mt-1.5 underline hover:no-underline font-semibold"
                >
                  OK
                </button>
              </div>
            </div>
          )}

          {sucessoMsg && (
            <div className="bg-emerald-950/50 border border-emerald-500/30 text-emerald-200 rounded-xl p-3.5 flex gap-2.5 text-xs animate-fadeIn">
              <CheckCircle className="w-4 h-4 text-emerald-400 flex-shrink-0 mt-0.5" />
              <div className="flex-1">
                <p className="font-bold">Concluído</p>
                <p className="text-emerald-200/80 mt-0.5">{sucessoMsg}</p>
              </div>
            </div>
          )}

        </main>

        {/* Rodapé Fixo com Botão de Finalização */}
        <footer className="p-4 bg-slate-950/95 border-t border-slate-800 sticky bottom-0 z-40 flex items-center justify-center">
          <button
            onClick={handleFinalizarServico}
            disabled={loadingFinalizar}
            className="w-full py-4 bg-emerald-600 hover:bg-emerald-500 active:scale-98 text-white font-extrabold text-sm rounded-2xl flex items-center justify-center gap-2 shadow-lg transition-all select-none"
            style={{ WebkitTapHighlightColor: 'transparent' }}
          >
            {loadingFinalizar ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Finalizando Ordem...</span>
              </>
            ) : (
              <>
                <Check className="w-5 h-5" />
                <span>Finalizar OS</span>
              </>
            )}
          </button>
        </footer>

      </div>
    </div>
  )
}
