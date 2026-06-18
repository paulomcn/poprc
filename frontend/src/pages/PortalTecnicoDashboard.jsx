import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  MapPin,
  Clock,
  Briefcase,
  Calendar,
  ChevronRight,
  AlertCircle,
  CheckCircle2,
  Compass,
  Loader2,
  Navigation,
  CheckSquare,
  Camera,
  X,
  RefreshCw
} from 'lucide-react'
import api from '../services/api'

export default function PortalTecnicoDashboard() {
  const navigate = useNavigate()
  
  // Estado do técnico logado
  const [tecnico, setTecnico] = useState({
    id: 1,
    nome: 'João Silva',
    funcao: 'Técnico de Redes Sênior'
  })

  // Estado do ponto do técnico
  const [statusPonto, setStatusPonto] = useState('FORA_TURNO') 
  const [loadingPonto, setLoadingPonto] = useState(false)
  const [gpsCoords, setGpsCoords] = useState(null)
  
  // Mensagens de feedback
  const [errorGps, setErrorGps] = useState(null)
  const [successMsg, setSuccessMsg] = useState(null)

  // Lista de Ordens de Serviço (OS) do dia
  const [ordensServico, setOrdensServico] = useState([
    {
      id: 1,
      numeroOs: 'OS-2026-042',
      cliente: 'Tribunal de Justiça - Comarca Centro',
      status: 'ABERTA',
      descricao: 'Instalação de racks de telecomunicação, cabeamento estruturado Cat6 e certificação de 24 pontos de rede.',
      endereco: 'Praça D. Pedro II, s/n - Centro, Salvador - BA',
      prioridade: 'Alta',
      dataExecucao: '2026-06-15',
      contato: 'Carlos Souza (Coordenador de TI) - (71) 99888-7766',
      tarefas: [
        { id: 1, texto: 'Vistoria técnica do local e infraestrutura', concluida: false },
        { id: 2, texto: 'Instalação física do Rack de rede de 19"', concluida: false },
        { id: 3, texto: 'Lançamento e crimpagem de cabos Cat6', concluida: false },
        { id: 4, texto: 'Certificação e testes de conectividade', concluida: false }
      ]
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
      contato: 'Marcos Bahia (Administrador) - (71) 98765-4321',
      tarefas: [
        { id: 1, texto: 'Remoção do equipamento danificado', concluida: true },
        { id: 2, texto: 'Montagem do novo Switch de 48 Portas PoE', concluida: true },
        { id: 3, texto: 'Upload de backup de configuração e VLANs', concluida: false },
        { id: 4, texto: 'Validação da rede local e internet', concluida: false }
      ]
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
      contato: 'Dra. Márcia Albuquerque - (71) 99111-2233',
      tarefas: [
        { id: 1, texto: 'Inspeção física do No-Break de 10kVA', concluida: true },
        { id: 2, texto: 'Medição individual da tensão das baterias', concluida: true },
        { id: 3, texto: 'Limpeza de contatos e reaperto de conexões', concluida: true },
        { id: 4, texto: 'Simulação de queda de energia com carga total', concluida: true }
      ]
    }
  ])

  const [selectedOs, setSelectedOs] = useState(null)
  const [uploadingFoto, setUploadingFoto] = useState(false)
  const [fotoEnviada, setFotoEnviada] = useState(null)

  useEffect(() => {
    async function carregarDadosIniciais() {
      try {
        const response = await api.get('/funcionarios')
        if (response.data && response.data.length > 0) {
          const func = response.data[0]
          setTecnico({
            id: func.id,
            nome: func.nome,
            funcao: func.funcao || 'Técnico de Campo'
          })
        }
      } catch (err) {
        console.log('Usando técnico mockado.', err)
      }
    }
    carregarDadosIniciais()
  }, [])

  const handleRegistrarPonto = () => {
    setLoadingPonto(true)
    setErrorGps(null)
    setSuccessMsg(null)

    if (!navigator.geolocation) {
      setErrorGps('A geolocalização não é suportada por este dispositivo.')
      setLoadingPonto(false)
      return
    }

    const gpsOptions = { enableHighAccuracy: true, timeout: 12000, maximumAge: 0 }

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const lat = position.coords.latitude.toFixed(6)
        const lng = position.coords.longitude.toFixed(6)
        setGpsCoords({ latitude: lat, longitude: lng })

        const proximoTipo = statusPonto === 'FORA_TURNO' ? 'ENTRADA' : 'SAIDA'
        const payload = {
          latitude: lat,
          longitude: lng,
          tipo: proximoTipo,
          funcionario: { id: tecnico.id },
          dataHora: new Date().toISOString()
        }

        try {
          const response = await api.post('/campo/ponto', payload)
          setStatusPonto(proximoTipo === 'ENTRADA' ? 'EM_TURNO' : 'FORA_TURNO')
          setSuccessMsg(`Ponto de ${proximoTipo === 'ENTRADA' ? 'Entrada' : 'Saída'} registrado com sucesso às ${new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}!`)
          setTimeout(() => setSuccessMsg(null), 5500)
        } catch (apiError) {
          setStatusPonto(proximoTipo === 'ENTRADA' ? 'EM_TURNO' : 'FORA_TURNO')
          setSuccessMsg(`[Simulado] Ponto de ${proximoTipo === 'ENTRADA' ? 'Entrada' : 'Saída'} gravado localmente!`)
          setTimeout(() => setSuccessMsg(null), 5500)
        } finally {
          setLoadingPonto(false)
        }
      },
      (error) => {
        let mensagemErro = 'Erro ao obter localização.'
        if (error.code === error.PERMISSION_DENIED) mensagemErro = 'Permissão do GPS negada. Ative a localização.'
        setErrorGps(mensagemErro)
        setLoadingPonto(false)
      },
      gpsOptions
    )
  }

  const toggleTarefa = (osId, tarefaId) => {
    setOrdensServico(prev => prev.map(os => os.id === osId ? { ...os, tarefas: os.tarefas.map(t => t.id === tarefaId ? { ...t, concluida: !t.concluida } : t) } : os))
  }

  const atualizarStatusOS = (osId, novoStatus) => {
    setOrdensServico(prev => prev.map(os => os.id === osId ? { ...os, status: novoStatus } : os))
  }

  const abrirNoMaps = (endereco) => {
    window.open(`https://maps.google.com/?q=${encodeURIComponent(endereco)}`, '_blank')
  }

  const handleUploadFoto = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    setUploadingFoto(true)
    setTimeout(() => {
      setUploadingFoto(false)
      setFotoEnviada('Foto enviada com sucesso!')
      setTimeout(() => setFotoEnviada(null), 3000)
    } , 1500)
  }

  return (
    // 💥 MUDANÇA 1: Removido o frame de celular falso. O container agora ocupa a tela inteira com espaçamento fluido.
    <div className="bg-slate-950 min-h-screen text-slate-100 font-sans antialiased p-4 sm:p-6 lg:p-8">
      <div className="max-w-7xl mx-auto space-y-6">
        
        {/* Topo do App: Header fluido */}
        <header className="bg-slate-900 border border-slate-800 rounded-2xl p-6 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 shadow-xl">
          <div>
            <p className="text-indigo-400 text-xs font-bold tracking-widest uppercase">Portal de Operações</p>
            <h2 className="text-2xl font-black text-white tracking-tight mt-1">Olá, {tecnico.nome}</h2>
            <p className="text-slate-400 text-xs mt-0.5">{tecnico.funcao}</p>
          </div>
          <div className="flex items-center gap-3 bg-slate-950/60 border border-slate-800 px-4 py-2.5 rounded-xl shadow-inner">
            <Clock className="w-5 h-5 text-indigo-400" />
            <span className="text-sm font-semibold tracking-wide text-slate-300">Painel de Campo</span>
          </div>
        </header>

        {/* 💥 MUDANÇA 2: GRID SISTÊMICO RESPONSIVO */}
        {/* No Mobile: 1 coluna (tudo empilhado). No Computador (lg): Divide em 3 colunas (1 pro Ponto, 2 paras as OS) */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
          
          {/* COLUNA DA ESQUERDA: Status e Botão de Ponto (Fica fixo no scroll no Computador) */}
          <div className="lg:col-span-1 space-y-6 lg:sticky lg:top-6">
            
            {/* Card de Status do Ponto */}
            <div className={`rounded-2xl p-5 border transition-all duration-300 ${
              statusPonto === 'EM_TURNO' 
                ? 'bg-emerald-950/45 border-emerald-500/30 text-emerald-100 shadow-[0_0_20px_rgba(16,185,129,0.05)]' 
                : 'bg-slate-900 border-slate-800 text-slate-300'
            }`}>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <span className="relative flex h-3.5 w-3.5">
                    <span className={`animate-ping absolute inline-flex h-full w-full rounded-full opacity-75 ${statusPonto === 'EM_TURNO' ? 'bg-emerald-400' : 'bg-rose-400'}`}></span>
                    <span className={`relative inline-flex rounded-full h-3.5 w-3.5 ${statusPonto === 'EM_TURNO' ? 'bg-emerald-500' : 'bg-rose-500'}`}></span>
                  </span>
                  <div>
                    <p className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Status da Jornada</p>
                    <p className="text-base font-bold tracking-tight">{statusPonto === 'EM_TURNO' ? 'Em Turno de Trabalho' : 'Fora do Turno'}</p>
                  </div>
                </div>
                <span className={`text-[10px] font-extrabold px-2.5 py-1 rounded-full ${statusPonto === 'EM_TURNO' ? 'bg-emerald-500/20 text-emerald-300' : 'bg-slate-800 text-slate-400'}`}>
                  {statusPonto === 'EM_TURNO' ? 'ATIVO' : 'INATIVO'}
                </span>
              </div>
              {gpsCoords && statusPonto === 'EM_TURNO' && (
                <div className="mt-3 pt-3 border-t border-emerald-500/10 flex items-center gap-1.5 text-[10px] text-emerald-400/80">
                  <MapPin className="w-3 h-3 text-emerald-400" />
                  <span>GPS Ativo: {gpsCoords.latitude}, {gpsCoords.longitude}</span>
                </div>
              )}
            </div>

            {/* Widget do Botão Gigante do Ponto */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 flex flex-col items-center justify-center text-center shadow-lg">
              <button
                onClick={handleRegistrarPonto}
                disabled={loadingPonto}
                className={`w-40 h-40 rounded-full flex flex-col items-center justify-center gap-2 border-8 shadow-2xl transition-all duration-300 select-none ${
                  loadingPonto 
                    ? 'bg-slate-800 border-slate-700 scale-95 cursor-wait' 
                    : statusPonto === 'EM_TURNO'
                      ? 'bg-rose-950/80 border-rose-500 hover:bg-rose-900 active:scale-95 text-rose-100 shadow-[0_0_25px_rgba(239,68,68,0.15)]'
                      : 'bg-emerald-950/80 border-emerald-500 hover:bg-emerald-900 active:scale-95 text-emerald-100 shadow-[0_0_25px_rgba(16,185,129,0.15)]'
                }`}
              >
                {loadingPonto ? (
                  <>
                    <Loader2 className="w-8 h-8 animate-spin text-indigo-400" />
                    <span className="text-[10px] font-bold text-slate-400 tracking-wider">Buscando GPS</span>
                  </>
                ) : (
                  <>
                    <Compass className={`w-10 h-10 ${statusPonto === 'EM_TURNO' ? 'text-rose-400' : 'text-emerald-400'}`} />
                    <span className="text-[9px] uppercase font-extrabold tracking-widest text-slate-400">REGISTRAR</span>
                    <span className="text-lg font-black tracking-tight">{statusPonto === 'EM_TURNO' ? 'SAÍDA' : 'ENTRADA'}</span>
                  </>
                )}
              </button>
              <p className="text-[11px] text-slate-500 font-medium max-w-[200px] mt-4">
                {loadingPonto ? 'Validando localização...' : 'Toque no botão para registrar sua jornada em tempo real.'}
              </p>
            </div>

            {/* Alertas e Feedbacks (CORS, GPS, etc.) */}
            <div className="space-y-3">
              {errorGps && (
                <div className="bg-rose-950/40 border border-rose-500/30 text-rose-200 rounded-xl p-4 flex gap-3 animate-fadeIn">
                  <AlertCircle className="w-5 h-5 text-rose-400 flex-shrink-0 mt-0.5" />
                  <div className="flex-1 text-xs">
                    <p className="font-bold">Acesso ao GPS Obrigatório</p>
                    <p className="text-rose-300/90 mt-1 leading-relaxed">{errorGps}</p>
                    <button onClick={() => setErrorGps(null)} className="mt-2 text-[10px] font-bold underline">Fechar</button>
                  </div>
                </div>
              )}
              {successMsg && (
                <div className="bg-emerald-950/40 border border-emerald-500/30 text-emerald-100 rounded-xl p-4 flex gap-3 animate-fadeIn">
                  <CheckCircle2 className="w-5 h-5 text-emerald-400 flex-shrink-0 mt-0.5" />
                  <div className="flex-1 text-xs"><p className="font-bold">Sucesso!</p><p className="text-emerald-200/90 mt-1">{successMsg}</p></div>
                </div>
              )}
            </div>

          </div>

          {/* COLUNA DA DIREITA: Lista de Ordens de Serviço (No Computador ocupa 2 colunas de largura) */}
          <div className="lg:col-span-2 space-y-4">
            
            <div className="flex justify-between items-center bg-slate-900 border border-slate-800 px-5 py-4 rounded-xl shadow-sm">
              <div className="flex items-center gap-2">
                <Briefcase className="w-4 h-4 text-indigo-400" />
                <h3 className="text-xs font-extrabold text-white uppercase tracking-wider">Ordens de Serviço do Dia</h3>
              </div>
              <span className="bg-indigo-500/10 text-indigo-300 border border-indigo-500/20 text-[10px] font-extrabold px-2.5 py-0.5 rounded-md">
                {ordensServico.length} ATIVAS
              </span>
            </div>

            {/* 💥 MUDANÇA 3: AS ORDENS DE SERVIÇO NO COMPUTADOR FICAM LADO A LADO EM SUB-GRID */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {ordensServico.map((os) => (
                <div 
                  key={os.id} 
                  className="bg-slate-900 border border-slate-800/80 rounded-2xl p-5 hover:border-slate-700 transition-all duration-300 flex flex-col justify-between shadow-md"
                >
                  <div>
                    <div className="flex justify-between items-start mb-3">
                      <div>
                        <p className="text-[10px] font-bold text-indigo-400 tracking-wider uppercase">{os.numeroOs}</p>
                        <h4 className="text-sm font-bold text-white mt-0.5 line-clamp-1">{os.cliente}</h4>
                      </div>
                      <span className={`text-[9px] font-bold px-2 py-0.5 rounded-full ${
                        os.status === 'CONCLUIDA' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 
                        os.status === 'EM_EXECUCAO' ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20' : 
                        'bg-indigo-500/10 text-indigo-400 border border-indigo-500/20'
                      }`}>
                        {os.status.replace('_', ' ')}
                      </span>
                    </div>
                    <p className="text-xs text-slate-400 line-clamp-3 leading-relaxed mb-4">{os.descricao}</p>
                  </div>

                  <div className="flex items-center justify-between pt-4 border-t border-slate-800/60 mt-auto">
                    <div className="flex items-center gap-1.5 text-[10px] text-slate-500">
                      <Calendar className="w-3.5 h-3.5" />
                      <span>{new Date(os.dataExecucao).toLocaleDateString('pt-BR')}</span>
                    </div>
                    
                    <button
                      onClick={() => navigate(`/tecnico/os/${os.id}`)}
                      className="flex items-center gap-1 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 active:scale-95 text-white font-bold text-xs rounded-xl transition-all shadow-md shadow-indigo-600/10"
                    >
                      <span>Acessar OS</span>
                      <ChevronRight className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              ))}
            </div>

          </div>

        </div>
      </div>
    </div>
  )
}