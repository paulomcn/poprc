import { useState, useEffect } from 'react'
import { useParams, useNavigate, useSearchParams } from 'react-router-dom'
import {
  ArrowLeft,
  MapPin,
  Briefcase,
  Calendar,
  AlertCircle,
  CheckCircle2,
  Navigation,
  Camera,
  RefreshCw,
  Save,
  Clock,
  ListChecks,
  Users,
  Trash2,
  X,
  ExternalLink
} from 'lucide-react'
import api from '../services/api'
import { buildApiFileUrl } from '../services/runtimeConfig'

const TECNICO_STORAGE_KEY = 'rc-tecnico-operacao-id'
const MAX_FOTO_BYTES = 10 * 1024 * 1024

function getApiErrorMessage(error, fallback) {
  return error?.response?.data?.message || error?.response?.data?.erro || fallback
}

function parseChecklistIds(checklist) {
  if (!checklist) return []

  try {
    const parsed = typeof checklist === 'string' ? JSON.parse(checklist) : checklist
    if (!Array.isArray(parsed?.atividades)) return []
    return parsed.atividades
      .map((atividade) => Number(atividade.id))
      .filter((atividadeId) => Number.isFinite(atividadeId))
  } catch (err) {
    return []
  }
}

function formatDate(value) {
  if (!value) return 'Não informado'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Não informado'
  return date.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function getTemporalAlerts(os) {
  if (!os) return []
  if (['CONCLUIDA', 'FATURADA'].includes(os.status)) return []

  const now = new Date()
  const inicio = os.dataHoraInicio ? new Date(os.dataHoraInicio) : null
  const fim = os.dataHoraFim ? new Date(os.dataHoraFim) : null
  const deadline = os.deadline ? new Date(os.deadline) : null
  const alerts = []

  if (deadline && !Number.isNaN(deadline.getTime())) {
    const diffMs = deadline.getTime() - now.getTime()
    const diffHours = diffMs / (1000 * 60 * 60)

    if (diffMs < 0) {
      alerts.push({
        tipo: 'atrasada',
        titulo: 'OS atrasada',
        texto: `Prazo limite vencido em ${formatDate(os.deadline)}. Acionar operação, estoque e responsável técnico.`,
        className: 'bg-rose-950/40 border-rose-500/30 text-rose-100',
        iconClassName: 'text-rose-300'
      })
    } else if (diffHours <= 24) {
      alerts.push({
        tipo: 'prazo',
        titulo: 'OS próxima do prazo',
        texto: `Deadline em menos de 24h: ${formatDate(os.deadline)}. Priorizar fechamento das pendências.`,
        className: 'bg-amber-950/40 border-amber-500/30 text-amber-100',
        iconClassName: 'text-amber-300'
      })
    }
  }

  if (inicio && !Number.isNaN(inicio.getTime()) && now.getTime() < inicio.getTime()) {
    alerts.push({
      tipo: 'agendada',
      titulo: 'OS agendada',
      texto: `Início previsto para ${formatDate(os.dataHoraInicio)}.`,
      className: 'bg-sky-950/40 border-sky-500/30 text-sky-100',
      iconClassName: 'text-sky-300'
    })
  }

  if (fim && !Number.isNaN(fim.getTime()) && now.getTime() > fim.getTime() && os.status !== 'CONCLUIDA') {
    alerts.push({
      tipo: 'janela',
      titulo: 'Janela de execução encerrada',
      texto: `Fim previsto era ${formatDate(os.dataHoraFim)}. Validar se a OS deve ser prorrogada ou concluída.`,
      className: 'bg-orange-950/40 border-orange-500/30 text-orange-100',
      iconClassName: 'text-orange-300'
    })
  }

  return alerts
}

export default function ExecutarOrdemServico() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const funcionarioId = searchParams.get('funcionarioId') || localStorage.getItem(TECNICO_STORAGE_KEY)

  const [os, setOs] = useState(null)
  const [tecnico, setTecnico] = useState(null)
  const [evidencias, setEvidencias] = useState([])
  const [evidenciasComErro, setEvidenciasComErro] = useState({})
  const [atividadesPadrao, setAtividadesPadrao] = useState([])
  const [atividadesSelecionadas, setAtividadesSelecionadas] = useState([])
  const [loading, setLoading] = useState(true)
  const [uploadingFoto, setUploadingFoto] = useState(false)
  const [evidenciaRemovendoId, setEvidenciaRemovendoId] = useState(null)
  const [evidenciaAberta, setEvidenciaAberta] = useState(null)
  const [savingChecklist, setSavingChecklist] = useState(false)
  const [feedbackMsg, setFeedbackMsg] = useState({ tipo: '', texto: '' })
  const [erroCarregamento, setErroCarregamento] = useState('')

  const atividadesPorCategoria = atividadesPadrao.reduce((grupos, atividade) => {
    const categoria = atividade.categoria || 'Geral'
    if (!grupos[categoria]) grupos[categoria] = []
    grupos[categoria].push(atividade)
    return grupos
  }, {})

  useEffect(() => {
    async function puxarDadosDaTela() {
      try {
        setLoading(true)
        setErroCarregamento('')
        if (!funcionarioId) {
          throw new Error('Selecione o técnico no painel antes de acessar uma OS.')
        }

        const [osResponse, atividadesResponse, evidenciasResponse, funcionariosResponse, comarcasResponse] = await Promise.all([
          api.get(`/ordens-servico/${id}`),
          api.get('/atividades-padrao/ativas'),
          api.get(`/campo/evidencias/os/${id}`),
          api.get('/funcionarios'),
          api.get('/comarcas')
        ])

        const tecnicoSelecionado = (funcionariosResponse.data || [])
          .find((funcionario) => String(funcionario.id) === String(funcionarioId))
        if (!tecnicoSelecionado) {
          throw new Error('O técnico selecionado não foi encontrado.')
        }

        const ordem = osResponse.data
        const responsavelId = ordem?.projeto?.responsavel?.id
        const membroSelecionado = ordem?.projeto?.equipe?.find(
          (membro) => String(membro.funcionario?.id) === String(tecnicoSelecionado.id),
        )
        if (!membroSelecionado && (!responsavelId || String(responsavelId) !== String(tecnicoSelecionado.id))) {
          throw new Error('Esta OS não está atribuída ao técnico selecionado.')
        }

        const comarca = (comarcasResponse.data || [])
          .find((item) => item.projeto?.id === ordem.projeto?.id)
        setTecnico(tecnicoSelecionado)
        setOs({
          ...ordem,
          cliente: comarca?.nomeComarca || ordem.contrato?.cliente || ordem.projeto?.nome,
          endereco: comarca?.endereco || ordem.projeto?.endereco,
          contato: comarca?.contatoResponsavel || comarca?.gerenteResponsavel,
          papelTecnico: membroSelecionado?.papel || 'LIDER_EQUIPE',
        })
        setAtividadesSelecionadas(parseChecklistIds(ordem?.checklist))
        setAtividadesPadrao(atividadesResponse.data || [])
        setEvidencias(evidenciasResponse.data || [])
      } catch (err) {
        setOs(null)
        setTecnico(null)
        setEvidencias([])
        setAtividadesSelecionadas([])
        setAtividadesPadrao([])
        setErroCarregamento(getApiErrorMessage(err, err.message || 'Não foi possível carregar esta Ordem de Serviço.'))
      } finally {
        setLoading(false)
      }
    }
    puxarDadosDaTela()
  }, [funcionarioId, id])

  const handleToggleAtividade = (atividadeId) => {
    setAtividadesSelecionadas((prev) => {
      if (prev.includes(atividadeId)) {
        return prev.filter((idSelecionado) => idSelecionado !== atividadeId)
      }
      return [...prev, atividadeId]
    })
  }

  const handleSalvarChecklist = async () => {
    if (!tecnico) {
      mostrarFeedback('erro', 'Técnico não identificado.')
      return
    }

    const atividades = atividadesPadrao
      .filter((atividade) => atividadesSelecionadas.includes(atividade.id))
      .map((atividade) => ({
        id: atividade.id,
        nome: atividade.nome,
        categoria: atividade.categoria || 'Geral'
      }))

    const checklist = JSON.stringify({
      registradoEm: new Date().toISOString(),
      registradoPor: tecnico.nome,
      atividades
    })

    try {
      setSavingChecklist(true)
      const response = await api.put(`/ordens-servico/${id}/checklist`, { checklist })
      setOs(response.data)
      mostrarFeedback('sucesso', 'Atividades realizadas registradas na OS.')
    } catch (err) {
      mostrarFeedback('erro', getApiErrorMessage(err, 'Falha ao salvar o checklist da OS.'))
    } finally {
      setSavingChecklist(false)
    }
  }

  const handleMudarStatus = async (novoStatus) => {
    try {
      const response = await api.put(`/ordens-servico/${id}/status`, { status: novoStatus })
      setOs((prev) => ({ ...prev, ...(response.data || {}), status: novoStatus }))
      mostrarFeedback('sucesso', `Status da OS alterado para ${novoStatus.replace('_', ' ')}!`)
    } catch (err) {
      mostrarFeedback('erro', getApiErrorMessage(err, 'Falha ao atualizar o status da OS.'))
    }
  }

  const handleUploadFotoEvidencia = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    if (!tecnico) {
      mostrarFeedback('erro', 'Técnico não identificado.')
      e.target.value = ''
      return
    }
    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      mostrarFeedback('erro', 'Envie uma imagem JPG, JPEG ou PNG.')
      e.target.value = ''
      return
    }
    if (file.size > MAX_FOTO_BYTES) {
      mostrarFeedback('erro', 'A foto deve ter no máximo 10 MB.')
      e.target.value = ''
      return
    }

    setUploadingFoto(true)
    setFeedbackMsg({ tipo: '', texto: '' })

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const lat = position.coords.latitude.toFixed(6)
        const lon = position.coords.longitude.toFixed(6)

        const formData = new FormData()
        formData.append('file', file)
        formData.append('ordemServicoId', os.id)
        formData.append('funcionarioId', tecnico.id)
        formData.append('latitude', lat)
        formData.append('longitude', lon)

        try {
          await api.post('/campo/upload-foto', formData)
          const evidenciasResponse = await api.get(`/campo/evidencias/os/${os.id}`)
          setEvidencias(evidenciasResponse.data || [])
          setEvidenciasComErro({})
          mostrarFeedback('sucesso', 'Evidência fotográfica salva com sucesso no servidor!')
        } catch (err) {
          mostrarFeedback('erro', getApiErrorMessage(err, 'Falha ao enviar a evidência fotográfica.'))
        } finally {
          setUploadingFoto(false)
          e.target.value = ''
        }
      },
      () => {
        setUploadingFoto(false)
        e.target.value = ''
        mostrarFeedback('erro', 'Obrigatório ativar o GPS para registrar a evidência fotográfica da OS.')
      },
      { enableHighAccuracy: true, timeout: 8000 }
    )
  }

  const handleRemoverEvidencia = async (evidencia) => {
    if (!tecnico || !evidencia) return
    const confirmou = window.confirm('Remover esta evidência fotográfica? Esta ação apagará o arquivo enviado.')
    if (!confirmou) return

    try {
      setEvidenciaRemovendoId(evidencia.id)
      await api.delete(`/campo/evidencias/${evidencia.id}`, {
        params: { funcionarioId: tecnico.id }
      })
      setEvidencias((atuais) => atuais.filter((item) => item.id !== evidencia.id))
      setEvidenciasComErro((atuais) => {
        const proximos = { ...atuais }
        delete proximos[evidencia.id]
        return proximos
      })
      setEvidenciaAberta((atual) => atual?.id === evidencia.id ? null : atual)
      mostrarFeedback('sucesso', 'Evidência removida do relatório fotográfico.')
    } catch (err) {
      mostrarFeedback('erro', getApiErrorMessage(err, 'Não foi possível remover a evidência.'))
    } finally {
      setEvidenciaRemovendoId(null)
    }
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

  if (!os) {
    return (
      <div className="bg-slate-950 min-h-screen flex flex-col items-center justify-center px-6 text-center text-slate-300">
        <AlertCircle className="w-10 h-10 text-rose-400 mb-3" />
        <h1 className="text-lg font-black text-white">Não foi possível abrir a OS</h1>
        <p className="mt-2 max-w-lg text-sm text-slate-400">{erroCarregamento}</p>
        <button
          type="button"
          onClick={() => navigate('/tecnico')}
          className="mt-6 inline-flex items-center gap-2 rounded-md bg-indigo-600 px-4 py-2 text-xs font-bold text-white hover:bg-indigo-500"
        >
          <ArrowLeft className="h-4 w-4" /> Voltar ao painel
        </button>
      </div>
    )
  }

  const temporalAlerts = getTemporalAlerts(os)
  const cliente = os.cliente || os.contrato?.cliente || os.projeto?.nome || 'Ordem de Serviço'
  const endereco = os.endereco || os.projeto?.endereco || 'Endereço não informado'
  const contato = os.contato || 'Contato não informado'
  const ordemFinalizada = ['CONCLUIDA', 'FATURADA'].includes(os.status)
  const ordemSomenteLeitura = [
    'AGUARDANDO_VALIDACAO',
    'AGUARDANDO_DEVOLUCAO',
    'AGUARDANDO_AUDITORIA',
    'AGUARDANDO_ENCERRAMENTO',
    'CONCLUIDA',
    'FATURADA'
  ].includes(os.status)
  const evidenciasEditaveis = !ordemSomenteLeitura

  return (
    <div className="bg-slate-950 min-h-screen text-slate-100 p-4 sm:p-6 lg:p-8 font-sans antialiased">
      <div className="max-w-4xl mx-auto space-y-6">
        <button
          onClick={() => navigate('/tecnico')}
          className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-slate-400 hover:text-white transition group"
        >
          <ArrowLeft className="w-4 h-4 transition-transform group-hover:-translate-x-1" />
          Voltar para o Painel
        </button>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 flex flex-col md:flex-row justify-between items-start md:items-center gap-4 shadow-xl">
          <div>
            <span className="text-indigo-400 text-xs font-black tracking-widest uppercase">{os.numeroOs}</span>
            <h1 className="text-xl sm:text-2xl font-black text-white tracking-tight mt-1">{cliente}</h1>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs text-slate-400 mt-3">
              <span className="flex items-center gap-1.5">
                <Calendar className="w-4 h-4 text-slate-500" />
                Início: {formatDate(os.dataHoraInicio || os.dataExecucao)}
              </span>
              <span className="flex items-center gap-1.5">
                <Clock className="w-4 h-4 text-slate-500" />
                Deadline: {formatDate(os.deadline)}
              </span>
              <span className="sm:col-span-2 text-slate-500">Técnico: {tecnico?.nome}</span>
            </div>
          </div>
          <span className={`text-xs font-black px-3 py-1 rounded-full border ${
            os.status === 'CONCLUIDA' ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' :
            os.status === 'EM_EXECUCAO' ? 'bg-amber-500/10 text-amber-400 border-amber-500/20' :
            'bg-indigo-500/10 text-indigo-400 border-indigo-500/20'
          }`}>
            {(os.status || 'ABERTA').replaceAll('_', ' ')}
          </span>
        </div>

        {feedbackMsg.texto && (
          <div className={`border p-4 rounded-xl flex gap-3 animate-fadeIn text-xs ${
            feedbackMsg.tipo === 'sucesso' ? 'bg-emerald-950/40 border-emerald-500/30 text-emerald-200' : 'bg-rose-950/40 border-rose-500/30 text-rose-200'
          }`}>
            {feedbackMsg.tipo === 'sucesso' ? <CheckCircle2 className="w-5 h-5 text-emerald-400 flex-shrink-0" /> : <AlertCircle className="w-5 h-5 text-rose-400 flex-shrink-0" />}
            <p className="font-semibold">{feedbackMsg.texto}</p>
          </div>
        )}

        {temporalAlerts.length > 0 && (
          <div className="space-y-3">
            {temporalAlerts.map((alerta) => (
              <div key={alerta.tipo} className={`border p-4 rounded-xl flex gap-3 text-xs ${alerta.className}`}>
                <AlertCircle className={`w-5 h-5 flex-shrink-0 ${alerta.iconClassName}`} />
                <div>
                  <p className="font-black uppercase tracking-wide">{alerta.titulo}</p>
                  <p className="font-semibold mt-1 leading-relaxed">{alerta.texto}</p>
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="md:col-span-2 space-y-6">
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-3 shadow-md">
              <h3 className="text-xs font-extrabold text-white uppercase tracking-wider flex items-center gap-2">
                <Briefcase className="w-4 h-4 text-indigo-400" /> Instruções do Serviço
              </h3>
              <p className="text-sm text-slate-300 leading-relaxed bg-slate-950/40 p-4 border border-slate-800 rounded-xl">
                {os.descricao || 'Sem descrição técnica cadastrada.'}
              </p>
            </div>

            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-4 shadow-md">
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                <h3 className="text-xs font-extrabold text-white uppercase tracking-wider flex items-center gap-2">
                  <ListChecks className="w-4 h-4 text-indigo-400" />
                  Atividades Realizadas
                </h3>
                <button
                  onClick={handleSalvarChecklist}
                  disabled={savingChecklist || ordemSomenteLeitura}
                  className="inline-flex items-center justify-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-60 text-white font-bold text-xs rounded-xl transition"
                >
                  {savingChecklist ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                  {savingChecklist ? 'Salvando...' : 'Salvar checklist'}
                </button>
              </div>

              <div className="space-y-5 bg-slate-950/40 p-4 border border-slate-800 rounded-xl">
                {atividadesPadrao.length === 0 && (
                  <p className="text-xs text-slate-400 text-center py-4">
                    Nenhuma atividade padrão ativa cadastrada pelo Admin.
                  </p>
                )}

                {Object.entries(atividadesPorCategoria).map(([categoria, atividades]) => (
                  <section key={categoria} className="space-y-1">
                    <div className="flex items-center justify-between border-b border-slate-800 px-2 pb-2">
                      <h4 className="text-[11px] font-black uppercase text-indigo-300">
                        {categoria}
                      </h4>
                      <span className="text-[10px] font-bold text-slate-500">
                        {atividades.filter((atividade) => atividadesSelecionadas.includes(atividade.id)).length}/{atividades.length}
                      </span>
                    </div>
                    {atividades.map((atividade) => {
                      const checked = atividadesSelecionadas.includes(atividade.id)
                      return (
                        <label
                          key={atividade.id}
                          className="flex items-start gap-3 py-2.5 cursor-pointer select-none transition hover:bg-slate-900/40 px-2 rounded-lg"
                        >
                          <input
                            type="checkbox"
                            checked={checked}
                            disabled={ordemSomenteLeitura}
                            onChange={() => handleToggleAtividade(atividade.id)}
                            className="mt-0.5 h-4 w-4 rounded border-slate-600 bg-slate-950 text-indigo-500 focus:ring-indigo-500"
                          />
                          <span className={`text-sm leading-relaxed ${checked ? 'text-white' : 'text-slate-300'}`}>
                            {atividade.nome}
                          </span>
                        </label>
                      )
                    })}
                  </section>
                ))}
                {ordemSomenteLeitura && (
                  <p className="rounded-lg border border-emerald-900/50 bg-emerald-950/20 px-3 py-2 text-center text-[11px] font-semibold text-emerald-300">
                    {os.status === 'AGUARDANDO_VALIDACAO'
                      ? 'Checklist enviado para validação e disponível somente para consulta.'
                      : 'Checklist encerrado e disponível somente para consulta.'}
                  </p>
                )}
              </div>
            </div>
          </div>

          <div className="md:col-span-1 space-y-6">
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-4 shadow-md">
              <div className="flex justify-between items-center">
                <h4 className="text-xs font-extrabold text-white uppercase tracking-wider">Localização</h4>
                <button
                  onClick={() => abrirNoMaps(endereco)}
                  className="text-[10px] text-indigo-400 font-bold hover:underline flex items-center gap-1"
                >
                  <Navigation className="w-3 h-3" /> Rota GPS
                </button>
              </div>
              <div className="bg-slate-950/40 p-3.5 border border-slate-800 rounded-xl flex items-start gap-2.5">
                <MapPin className="w-4 h-4 text-rose-500 flex-shrink-0 mt-0.5" />
                <span className="text-xs text-slate-300 leading-normal">{endereco}</span>
              </div>
              <div className="bg-slate-950/20 p-3 border border-slate-800/60 rounded-xl text-xs text-slate-400">
                <span className="font-bold text-slate-300 block mb-1">Contato Responsável:</span>
                {contato}
              </div>
            </div>

            <div className="rounded-2xl border border-cyan-900/60 bg-slate-900 p-5 shadow-md">
              <h4 className="flex items-center gap-2 text-xs font-extrabold uppercase tracking-wider text-white">
                <Users className="h-4 w-4 text-cyan-400" /> Equipe em campo
              </h4>
              <p className="mt-2 text-[10px] font-bold text-cyan-300">
                Sua função: {os.papelTecnico === 'LIDER_EQUIPE' ? 'Supervisor técnico / Líder da equipe' : 'Técnico'}
              </p>
              <div className="mt-3 space-y-2">
                {(os.projeto?.equipe || []).map((membro) => (
                  <div key={membro.id} className="flex items-center justify-between gap-3 rounded-lg border border-slate-800 bg-slate-950/50 px-3 py-2">
                    <span className="truncate text-xs font-semibold text-slate-200">{membro.funcionario?.nome}</span>
                    <span className="shrink-0 text-[9px] font-bold uppercase text-slate-500">
                      {membro.papel === 'LIDER_EQUIPE' ? 'Líder' : 'Técnico'}
                    </span>
                  </div>
                ))}
                {!os.projeto?.equipe?.length && (
                  <p className="text-xs text-slate-500">Equipe legada: {os.projeto?.responsavel?.nome || tecnico?.nome}</p>
                )}
              </div>
            </div>

            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-4 shadow-md">
              <h4 className="text-xs font-extrabold text-white uppercase tracking-wider">Relatório Fotográfico</h4>
              {evidencias.length === 0 && !uploadingFoto && (
                <p className="rounded-lg border border-dashed border-slate-700 px-3 py-4 text-center text-xs text-slate-500">
                  Nenhuma evidência fotográfica enviada para esta OS.
                </p>
              )}
              {evidencias.length > 0 && (
                <div className="grid grid-cols-2 gap-2">
                  {evidencias.map((evidencia) => (
                    <div key={evidencia.id} className="relative overflow-hidden rounded-lg border border-slate-800 bg-slate-950">
                      {evidenciasComErro[evidencia.id] ? (
                        <div className="flex aspect-square flex-col items-center justify-center gap-2 p-2 text-center text-[10px] text-rose-300">
                          <AlertCircle className="h-5 w-5" />
                          <span>Arquivo salvo, mas a imagem não pôde ser exibida.</span>
                          <a
                            href={buildApiFileUrl(evidencia.caminhoArquivo)}
                            target="_blank"
                            rel="noreferrer"
                            className="font-bold text-indigo-300 underline"
                          >
                            Abrir arquivo
                          </a>
                        </div>
                      ) : (
                        <button
                          type="button"
                          onClick={() => setEvidenciaAberta(evidencia)}
                          className="group block"
                          title={`Registrada por ${evidencia.funcionarioNome || 'técnico'} em ${formatDate(evidencia.dataUpload)}`}
                        >
                          <img
                            src={`${buildApiFileUrl(evidencia.caminhoArquivo)}?v=${evidencia.id}`}
                            alt={`Evidência da OS ${os.numeroOs}`}
                            onError={() => setEvidenciasComErro((atuais) => ({ ...atuais, [evidencia.id]: true }))}
                            className="aspect-square w-full object-cover transition group-hover:opacity-80"
                          />
                        </button>
                      )}
                      <div className="flex items-center justify-between gap-1 px-2 py-1.5">
                        <span className="min-w-0 truncate text-[9px] text-slate-500">
                          {formatDate(evidencia.dataUpload)}
                        </span>
                        {evidenciasEditaveis && String(evidencia.funcionarioId) === String(tecnico?.id) && (
                          <button
                            type="button"
                            onClick={() => handleRemoverEvidencia(evidencia)}
                            disabled={evidenciaRemovendoId === evidencia.id}
                            className="shrink-0 rounded p-1 text-slate-500 hover:bg-rose-950 hover:text-rose-300 disabled:opacity-50"
                            title="Remover evidência"
                          >
                            {evidenciaRemovendoId === evidencia.id
                              ? <RefreshCw className="h-3.5 w-3.5 animate-spin" />
                              : <Trash2 className="h-3.5 w-3.5" />}
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
              {evidenciasEditaveis ? (
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
                        accept="image/jpeg,image/png"
                        capture="environment"
                        className="hidden"
                        onChange={handleUploadFotoEvidencia}
                      />
                    </label>
                  </>
                )}
              </div>
              ) : (
                <p className="rounded-lg border border-emerald-900/50 bg-emerald-950/20 px-3 py-3 text-center text-[11px] font-semibold text-emerald-300">
                  Relatório encerrado. As evidências permanecem disponíveis somente para consulta.
                </p>
              )}
            </div>

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
                  onClick={() => handleMudarStatus('AGUARDANDO_VALIDACAO')}
                  className="w-full py-3 bg-emerald-600 hover:bg-emerald-500 active:scale-95 text-white font-bold text-xs rounded-xl transition shadow-md shadow-emerald-600/10"
                >
                  Enviar para Validação
                </button>
              )}

              {os.status === 'AGUARDANDO_VALIDACAO' && (
                <div className="w-full text-center py-2.5 text-xs font-bold text-amber-300 flex items-center justify-center gap-1.5 bg-amber-950/20 rounded-xl border border-amber-900/30">
                  <Clock className="w-4 h-4" />
                  Relatório aguardando validação administrativa
                </div>
              )}

              {ordemFinalizada && (
                <div className="w-full text-center py-2.5 text-xs font-bold text-emerald-400 flex items-center justify-center gap-1.5 bg-emerald-950/20 rounded-xl border border-emerald-900/30">
                  <CheckCircle2 className="w-4 h-4" />
                  OS Finalizada com Sucesso!
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {evidenciaAberta && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 p-4" role="dialog" aria-modal="true" aria-label="Evidência fotográfica ampliada">
          <div className="w-full max-w-3xl overflow-hidden rounded-lg border border-slate-700 bg-slate-900 shadow-2xl">
            <div className="flex items-start justify-between gap-4 border-b border-slate-800 p-4">
              <div className="min-w-0">
                <p className="truncate text-sm font-bold text-white">{evidenciaAberta.funcionarioNome || 'Técnico não informado'}</p>
                <p className="mt-1 text-xs text-slate-400">{formatDate(evidenciaAberta.dataUpload)}</p>
                <p className="mt-1 text-[10px] text-slate-500">GPS: {evidenciaAberta.latitude}, {evidenciaAberta.longitude}</p>
              </div>
              <button type="button" onClick={() => setEvidenciaAberta(null)} className="rounded p-2 text-slate-400 hover:bg-slate-800 hover:text-white" title="Fechar visualização">
                <X className="h-5 w-5" />
              </button>
            </div>
            <div className="flex max-h-[70vh] items-center justify-center bg-black p-3">
              <img src={buildApiFileUrl(evidenciaAberta.caminhoArquivo)} alt={`Evidência ampliada da OS ${os.numeroOs}`} className="max-h-[66vh] max-w-full object-contain" />
            </div>
            <div className="flex flex-wrap justify-end gap-2 border-t border-slate-800 p-3">
              <a href={buildApiFileUrl(evidenciaAberta.caminhoArquivo)} target="_blank" rel="noreferrer" className="inline-flex items-center gap-2 rounded-md border border-slate-700 px-3 py-2 text-xs font-bold text-slate-200 hover:bg-slate-800">
                <ExternalLink className="h-4 w-4" /> Abrir original
              </a>
              {evidenciasEditaveis && String(evidenciaAberta.funcionarioId) === String(tecnico?.id) && (
                <button type="button" onClick={() => handleRemoverEvidencia(evidenciaAberta)} disabled={evidenciaRemovendoId === evidenciaAberta.id} className="inline-flex items-center gap-2 rounded-md bg-rose-700 px-3 py-2 text-xs font-bold text-white hover:bg-rose-600 disabled:opacity-50">
                  <Trash2 className="h-4 w-4" /> Remover evidência
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
