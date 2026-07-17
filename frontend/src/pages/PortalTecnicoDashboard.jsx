import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  AlertCircle,
  Briefcase,
  Bell,
  Calendar,
  CheckCircle2,
  ChevronRight,
  Clock,
  Compass,
  Loader2,
  MapPin,
  RefreshCw,
  User,
  Users,
  TimerReset
} from 'lucide-react'
import api from '../services/api'

const TECNICO_STORAGE_KEY = 'rc-tecnico-operacao-id'

const formatarData = (valor) => {
  if (!valor) return 'Data não informada'
  const data = new Date(valor)
  return Number.isNaN(data.getTime()) ? 'Data não informada' : data.toLocaleDateString('pt-BR')
}

const getMensagemErro = (erro, fallback) =>
  erro.response?.data?.erro || erro.response?.data?.message || fallback

const formatarPapel = (papel) =>
  papel === 'LIDER_EQUIPE' ? 'Supervisor técnico / Líder da equipe' : 'Técnico'

export default function PortalTecnicoDashboard() {
  const navigate = useNavigate()
  const [funcionarios, setFuncionarios] = useState([])
  const [tecnicoId, setTecnicoId] = useState('')
  const [ordens, setOrdens] = useState([])
  const [comarcas, setComarcas] = useState([])
  const [statusPonto, setStatusPonto] = useState('FORA_TURNO')
  const [ultimoPonto, setUltimoPonto] = useState(null)
  const [gpsCoords, setGpsCoords] = useState(null)
  const [notificacoes, setNotificacoes] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadingPonto, setLoadingPonto] = useState(false)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [filtroStatus, setFiltroStatus] = useState('ATIVAS')
  const [filtroPrazo, setFiltroPrazo] = useState('TODOS')

  const tecnico = funcionarios.find((funcionario) => String(funcionario.id) === String(tecnicoId)) || null

  const comarcaPorProjeto = useMemo(
    () => new Map(comarcas.filter((comarca) => comarca.projeto?.id).map((comarca) => [comarca.projeto.id, comarca])),
    [comarcas]
  )

  const ordensAtribuidas = useMemo(() => {
    if (!tecnico) return []
    return ordens
      .filter((os) =>
        os.projeto?.responsavel?.id === tecnico.id ||
        os.projeto?.equipe?.some((membro) => membro.funcionario?.id === tecnico.id),
      )
      .map((os) => {
        const comarca = comarcaPorProjeto.get(os.projeto?.id)
        const membroEquipe = os.projeto?.equipe?.find(
          (membro) => membro.funcionario?.id === tecnico.id,
        )
        const status = String(os.status || 'ABERTA')
        const finalizada = ['CONCLUIDA', 'FATURADA'].includes(status)
        const deadline = os.deadline ? new Date(os.deadline) : null
        const horasRestantes = deadline ? (deadline.getTime() - Date.now()) / 3600000 : null
        const situacaoPrazo = finalizada
          ? 'CONCLUIDA'
          : horasRestantes === null
            ? 'SEM_PRAZO'
            : horasRestantes < 0
              ? 'ATRASADA'
              : horasRestantes <= 24
                ? 'PROXIMA'
                : 'NO_PRAZO'
        return {
          ...os,
          cliente: comarca?.nomeComarca || os.contrato?.cliente || `Projeto #${os.projeto?.id || '-'}`,
          endereco: comarca?.endereco || '',
          papelNaEquipe: membroEquipe?.papel || (os.projeto?.responsavel?.id === tecnico.id ? 'LIDER_EQUIPE' : 'TECNICO'),
          situacaoPrazo,
          horasRestantes,
        }
      })
      .sort((a, b) => new Date(a.deadline || a.dataHoraInicio || 0) - new Date(b.deadline || b.dataHoraInicio || 0))
  }, [comarcaPorProjeto, ordens, tecnico])

  const ordensFiltradas = useMemo(() => ordensAtribuidas.filter((os) => {
    const statusAtivo = !['CONCLUIDA', 'FATURADA'].includes(String(os.status))
    if (filtroStatus === 'ATIVAS' && !statusAtivo) return false
    if (filtroStatus === 'CONCLUIDAS' && statusAtivo) return false
    if (filtroPrazo !== 'TODOS' && os.situacaoPrazo !== filtroPrazo) return false
    return true
  }), [filtroPrazo, filtroStatus, ordensAtribuidas])

  const resumoOrdens = useMemo(() => ({
    ativas: ordensAtribuidas.filter((os) => !['CONCLUIDA', 'FATURADA'].includes(String(os.status))).length,
    atrasadas: ordensAtribuidas.filter((os) => os.situacaoPrazo === 'ATRASADA').length,
    proximas: ordensAtribuidas.filter((os) => os.situacaoPrazo === 'PROXIMA').length,
    concluidas: ordensAtribuidas.filter((os) => os.situacaoPrazo === 'CONCLUIDA').length,
  }), [ordensAtribuidas])

  const carregarDados = async () => {
    try {
      setLoading(true)
      setErro('')
      const [funcionariosResponse, ordensResponse, comarcasResponse] = await Promise.all([
        api.get('/funcionarios'),
        api.get('/ordens-servico'),
        api.get('/comarcas')
      ])
      const funcionariosCarregados = funcionariosResponse.data || []
      setFuncionarios(funcionariosCarregados)
      setOrdens(ordensResponse.data || [])
      setComarcas(comarcasResponse.data || [])

      const salvo = localStorage.getItem(TECNICO_STORAGE_KEY)
      const tecnicoSalvoExiste = funcionariosCarregados.some((item) => String(item.id) === salvo)
      if (tecnicoSalvoExiste) setTecnicoId(salvo)
      else if (funcionariosCarregados.length === 1) setTecnicoId(String(funcionariosCarregados[0].id))
      else setTecnicoId('')
    } catch (err) {
      setFuncionarios([])
      setOrdens([])
      setComarcas([])
      setErro(getMensagemErro(err, 'Não foi possível carregar o Portal Técnico.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    carregarDados()
  }, [])

  useEffect(() => {
    if (!tecnicoId) {
      setUltimoPonto(null)
      setStatusPonto('FORA_TURNO')
      setNotificacoes([])
      return
    }

    localStorage.setItem(TECNICO_STORAGE_KEY, tecnicoId)
    Promise.all([
      api.get(`/campo/ponto/funcionario/${tecnicoId}/ultimo`),
      api.get(`/alertas/notificacoes?funcionarioId=${tecnicoId}`)
    ]).then(([pontoResponse, notificacoesResponse]) => {
        const ponto = pontoResponse.status === 204 ? null : pontoResponse.data
        setUltimoPonto(ponto)
        setStatusPonto(ponto?.tipo === 'ENTRADA' ? 'EM_TURNO' : 'FORA_TURNO')
        setNotificacoes((notificacoesResponse.data || []).filter((item) => !item.lidaEm))
      })
      .catch((err) => {
        setUltimoPonto(null)
        setStatusPonto('FORA_TURNO')
        setNotificacoes([])
        setErro(getMensagemErro(err, 'Não foi possível consultar a jornada e os alertas do técnico.'))
      })
  }, [tecnicoId])

  const selecionarTecnico = (event) => {
    setTecnicoId(event.target.value)
    setErro('')
    setSucesso('')
  }

  const registrarPontoComLocalizacao = (position) => {
    const latitude = position.coords.latitude.toFixed(6)
    const longitude = position.coords.longitude.toFixed(6)
    const tipo = statusPonto === 'EM_TURNO' ? 'SAIDA' : 'ENTRADA'

    api.post('/campo/ponto', {
      funcionarioId: Number(tecnicoId),
      tipo,
      latitude,
      longitude
    }).then((response) => {
      setUltimoPonto(response.data)
      setGpsCoords({ latitude, longitude })
      setStatusPonto(tipo === 'ENTRADA' ? 'EM_TURNO' : 'FORA_TURNO')
      setSucesso(`${tipo === 'ENTRADA' ? 'Entrada' : 'Saída'} registrada às ${new Date(response.data.dataHora).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}.`)
    }).catch((err) => {
      setErro(getMensagemErro(err, 'Não foi possível registrar o ponto.'))
    }).finally(() => setLoadingPonto(false))
  }

  const handleRegistrarPonto = () => {
    setErro('')
    setSucesso('')
    if (!tecnico) {
      setErro('Selecione o técnico antes de registrar o ponto.')
      return
    }
    if (!navigator.geolocation) {
      setErro('A geolocalização não é suportada por este dispositivo.')
      return
    }

    setLoadingPonto(true)
    navigator.geolocation.getCurrentPosition(
      registrarPontoComLocalizacao,
      (geolocationError) => {
        setLoadingPonto(false)
        setErro(
          geolocationError.code === geolocationError.PERMISSION_DENIED
            ? 'Permissão de localização negada. Ative o GPS para registrar o ponto.'
            : 'Não foi possível obter a localização atual.'
        )
      },
      { enableHighAccuracy: true, timeout: 12000, maximumAge: 0 }
    )
  }

  return (
    <div className="min-h-screen bg-slate-950 p-4 font-sans text-slate-100 sm:p-6 lg:p-8">
      <div className="mx-auto max-w-7xl space-y-6">
        <header className="flex flex-col items-start justify-between gap-4 rounded-lg border border-slate-800 bg-slate-900 p-6 shadow-xl sm:flex-row sm:items-center">
          <div>
            <p className="text-xs font-bold uppercase tracking-widest text-indigo-400">Portal de Operações</p>
            <h1 className="mt-1 text-2xl font-black tracking-tight text-white">
              {tecnico ? tecnico.nome : 'Selecione o técnico'}
            </h1>
            <p className="mt-0.5 text-xs text-slate-400">{tecnico?.funcao || 'Operação de campo'}</p>
          </div>
          <label className="w-full sm:w-80">
            <span className="mb-1 block text-[10px] font-black uppercase tracking-wide text-slate-400">
              Técnico em operação
            </span>
            <div className="relative">
              <User className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-slate-500" />
              <select
                value={tecnicoId}
                onChange={selecionarTecnico}
                className="w-full rounded-md border border-slate-700 bg-slate-950 py-2 pl-9 pr-3 text-sm text-white focus:border-indigo-500 focus:outline-none"
              >
                <option value="">Selecione...</option>
                {funcionarios.map((funcionario) => (
                  <option key={funcionario.id} value={funcionario.id}>
                    {funcionario.nome} - {funcionario.funcao || 'Técnico'}
                  </option>
                ))}
              </select>
            </div>
          </label>
        </header>

        {erro && (
          <div className="flex gap-3 rounded-lg border border-rose-500/30 bg-rose-950/40 p-4 text-sm text-rose-200">
            <AlertCircle className="h-5 w-5 shrink-0 text-rose-400" />
            <span>{erro}</span>
          </div>
        )}
        {sucesso && (
          <div className="flex gap-3 rounded-lg border border-emerald-500/30 bg-emerald-950/40 p-4 text-sm text-emerald-100">
            <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-400" />
            <span>{sucesso}</span>
          </div>
        )}

        {tecnico && notificacoes.length > 0 && (
          <section className="space-y-2 rounded-lg border border-amber-500/30 bg-amber-950/30 p-4">
            <h2 className="flex items-center gap-2 text-xs font-black uppercase tracking-wide text-amber-200">
              <Bell className="h-4 w-4" /> Alertas operacionais ({notificacoes.length})
            </h2>
            {notificacoes.slice(0, 4).map((notificacao) => (
              <div key={notificacao.id} className="rounded-md border border-amber-500/20 bg-slate-950/40 p-3">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-xs font-bold text-white">{notificacao.titulo}</span>
                  {notificacao.numeroOs && <span className="text-[10px] font-black text-amber-300">{notificacao.numeroOs}</span>}
                </div>
                <p className="mt-1 text-xs leading-relaxed text-slate-300">{notificacao.mensagem}</p>
              </div>
            ))}
          </section>
        )}

        {tecnico && (
          <section className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            <div className="rounded-lg border border-cyan-500/30 bg-cyan-950/30 p-4">
              <Briefcase className="h-4 w-4 text-cyan-300" />
              <p className="mt-3 text-2xl font-black text-white">{resumoOrdens.ativas}</p>
              <p className="text-[10px] font-bold uppercase text-cyan-200">OS ativas</p>
            </div>
            <div className="rounded-lg border border-rose-500/30 bg-rose-950/30 p-4">
              <AlertCircle className="h-4 w-4 text-rose-300" />
              <p className="mt-3 text-2xl font-black text-white">{resumoOrdens.atrasadas}</p>
              <p className="text-[10px] font-bold uppercase text-rose-200">Atrasadas</p>
            </div>
            <div className="rounded-lg border border-amber-500/30 bg-amber-950/30 p-4">
              <TimerReset className="h-4 w-4 text-amber-300" />
              <p className="mt-3 text-2xl font-black text-white">{resumoOrdens.proximas}</p>
              <p className="text-[10px] font-bold uppercase text-amber-200">Vencem em 24h</p>
            </div>
            <div className="rounded-lg border border-emerald-500/30 bg-emerald-950/30 p-4">
              <CheckCircle2 className="h-4 w-4 text-emerald-300" />
              <p className="mt-3 text-2xl font-black text-white">{resumoOrdens.concluidas}</p>
              <p className="text-[10px] font-bold uppercase text-emerald-200">Concluídas</p>
            </div>
          </section>
        )}

        <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-3">
          <aside className="space-y-4 lg:sticky lg:top-6">
            <section className="rounded-lg border border-slate-800 bg-slate-900 p-5">
              <div className="flex items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <span className={`h-3.5 w-3.5 rounded-full ${statusPonto === 'EM_TURNO' ? 'bg-emerald-500' : 'bg-rose-500'}`} />
                  <div>
                    <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Jornada</p>
                    <p className="font-bold">{statusPonto === 'EM_TURNO' ? 'Em turno' : 'Fora do turno'}</p>
                  </div>
                </div>
                <Clock className="h-5 w-5 text-indigo-400" />
              </div>
              {ultimoPonto && (
                <p className="mt-3 border-t border-slate-800 pt-3 text-xs text-slate-400">
                  Último registro: {new Date(ultimoPonto.dataHora).toLocaleString('pt-BR')}
                </p>
              )}
              {gpsCoords && (
                <p className="mt-2 flex items-center gap-1 text-[10px] text-slate-500">
                  <MapPin className="h-3 w-3" /> {gpsCoords.latitude}, {gpsCoords.longitude}
                </p>
              )}
            </section>

            <section className="flex flex-col items-center rounded-lg border border-slate-800 bg-slate-900 p-6 text-center">
              <button
                type="button"
                onClick={handleRegistrarPonto}
                disabled={loadingPonto || !tecnico}
                className={`flex h-40 w-40 flex-col items-center justify-center gap-2 rounded-full border-8 transition disabled:cursor-not-allowed disabled:opacity-40 ${
                  statusPonto === 'EM_TURNO'
                    ? 'border-rose-500 bg-rose-950 text-rose-100'
                    : 'border-emerald-500 bg-emerald-950 text-emerald-100'
                }`}
              >
                {loadingPonto ? <Loader2 className="h-8 w-8 animate-spin" /> : <Compass className="h-10 w-10" />}
                <span className="text-xs font-black uppercase">
                  {loadingPonto ? 'Validando' : statusPonto === 'EM_TURNO' ? 'Registrar saída' : 'Registrar entrada'}
                </span>
              </button>
            </section>
          </aside>

          <main className="space-y-4 lg:col-span-2">
            <div className="flex flex-col gap-3 xl:flex-row xl:items-end xl:justify-between">
              <div className="flex items-center gap-2">
                <Briefcase className="h-5 w-5 text-indigo-400" />
                <h2 className="font-black text-white">Ordens atribuídas</h2>
              </div>
              <div className="flex flex-wrap items-end gap-2">
                <label>
                  <span className="mb-1 block text-[9px] font-black uppercase text-slate-500">Situação</span>
                  <select value={filtroStatus} onChange={(e) => setFiltroStatus(e.target.value)} className="rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-xs text-white">
                    <option value="ATIVAS">Ativas</option>
                    <option value="CONCLUIDAS">Concluídas</option>
                    <option value="TODAS">Todas</option>
                  </select>
                </label>
                <label>
                  <span className="mb-1 block text-[9px] font-black uppercase text-slate-500">Prazo</span>
                  <select value={filtroPrazo} onChange={(e) => setFiltroPrazo(e.target.value)} className="rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-xs text-white">
                    <option value="TODOS">Todos</option>
                    <option value="ATRASADA">Atrasadas</option>
                    <option value="PROXIMA">Vencem em 24h</option>
                    <option value="NO_PRAZO">No prazo</option>
                    <option value="SEM_PRAZO">Sem prazo</option>
                  </select>
                </label>
                <button
                  type="button"
                  onClick={carregarDados}
                  disabled={loading}
                  className="rounded-md border border-slate-700 p-2 text-slate-400 hover:text-white disabled:opacity-50"
                  title="Atualizar dados"
                >
                  <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
                </button>
              </div>
            </div>

            {loading ? (
              <div className="flex items-center justify-center gap-2 rounded-lg border border-slate-800 bg-slate-900 p-10 text-sm text-slate-400">
                <Loader2 className="h-5 w-5 animate-spin" /> Carregando dados...
              </div>
            ) : !tecnico ? (
              <div className="rounded-lg border border-slate-800 bg-slate-900 p-10 text-center text-sm text-slate-400">
                Selecione o técnico para consultar as ordens atribuídas.
              </div>
            ) : ordensAtribuidas.length === 0 ? (
              <div className="rounded-lg border border-slate-800 bg-slate-900 p-10 text-center text-sm text-slate-400">
                Nenhuma OS vinculada a este integrante da equipe.
              </div>
            ) : ordensFiltradas.length === 0 ? (
              <div className="rounded-lg border border-slate-800 bg-slate-900 p-10 text-center text-sm text-slate-400">
                Nenhuma OS corresponde aos filtros selecionados.
              </div>
            ) : (
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                {ordensFiltradas.map((os) => (
                  <article key={os.id} className="flex flex-col justify-between rounded-lg border border-slate-800 bg-slate-900 p-5 shadow-md">
                    <div>
                      <div className="mb-3 flex items-start justify-between gap-3">
                        <div className="min-w-0">
                          <p className="truncate text-[10px] font-bold uppercase tracking-wider text-indigo-400">{os.numeroOs}</p>
                          <h3 className="mt-0.5 truncate text-sm font-bold text-white">{os.cliente}</h3>
                          <p className="mt-1 flex items-center gap-1 text-[9px] font-bold text-slate-500">
                            <User className="h-3 w-3" /> {formatarPapel(os.papelNaEquipe)}
                          </p>
                        </div>
                        <span className="shrink-0 rounded bg-slate-800 px-2 py-1 text-[9px] font-bold text-slate-300">
                          {String(os.status || 'ABERTA').replaceAll('_', ' ')}
                        </span>
                      </div>
                      <p className="mb-4 line-clamp-3 text-xs leading-relaxed text-slate-400">{os.descricao || 'Sem descrição.'}</p>
                      {os.endereco && (
                        <p className="mb-3 flex items-start gap-1 text-[10px] text-slate-500">
                          <MapPin className="mt-0.5 h-3 w-3 shrink-0" /> {os.endereco}
                        </p>
                      )}
                      <div className="mb-3 flex flex-wrap items-center gap-2">
                        <span className={`rounded px-2 py-1 text-[9px] font-black uppercase ${
                          os.situacaoPrazo === 'ATRASADA'
                            ? 'bg-rose-950 text-rose-300'
                            : os.situacaoPrazo === 'PROXIMA'
                              ? 'bg-amber-950 text-amber-300'
                              : os.situacaoPrazo === 'CONCLUIDA'
                                ? 'bg-emerald-950 text-emerald-300'
                                : 'bg-cyan-950 text-cyan-300'
                        }`}>
                          {os.situacaoPrazo === 'ATRASADA'
                            ? 'Prazo vencido'
                            : os.situacaoPrazo === 'PROXIMA'
                              ? 'Vence em até 24h'
                              : os.situacaoPrazo === 'CONCLUIDA'
                                ? 'Concluída'
                                : os.situacaoPrazo === 'SEM_PRAZO' ? 'Sem prazo' : 'No prazo'}
                        </span>
                        {os.deadline && <span className="text-[9px] text-slate-500">Limite: {new Date(os.deadline).toLocaleString('pt-BR')}</span>}
                      </div>
                      {os.projeto?.equipe?.length > 0 && (
                        <p className="mb-3 flex items-start gap-1 text-[10px] text-slate-500">
                          <Users className="mt-0.5 h-3 w-3 shrink-0" />
                          {os.projeto.equipe.map((membro) => membro.funcionario?.nome).filter(Boolean).join(', ')}
                        </p>
                      )}
                    </div>
                    <div className="mt-auto flex items-center justify-between border-t border-slate-800 pt-4">
                      <span className="flex items-center gap-1.5 text-[10px] text-slate-500">
                        <Calendar className="h-3.5 w-3.5" /> {formatarData(os.dataHoraInicio || os.dataExecucao)}
                      </span>
                      <button
                        type="button"
                        onClick={() => navigate(`/tecnico/os/${os.id}?funcionarioId=${tecnico.id}`)}
                        className="flex items-center gap-1 rounded-md bg-indigo-600 px-4 py-2 text-xs font-bold text-white hover:bg-indigo-500"
                      >
                        Acessar OS <ChevronRight className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </main>
        </div>
      </div>
    </div>
  )
}
