import axios from 'axios'
import { API_BASE_URL } from './runtimeConfig'

const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  withXSRFToken: true,
})

let reauthHandler = null

export const setReauthHandler = (handler) => {
  reauthHandler = handler
}

export const refreshCsrfToken = async () => {
  const response = await api.get('/auth/csrf', { __csrfRefresh: true })
  const token = response.data?.token
  if (token) api.defaults.headers.common['X-XSRF-TOKEN'] = token
  return token
}

export const getApiErrorMessage = (error, fallback = 'Não foi possível concluir a operação.') => {
  const data = error?.response?.data
  if (typeof data === 'string' && data.trim()) return data
  if (data?.erro) return data.erro
  if (data?.message) return data.message
  if (Array.isArray(data?.erros) && data.erros.length > 0) return data.erros.join(' ')
  if (!error?.response && error?.request) {
    return 'Não foi possível conectar ao servidor. Verifique se o backend está em execução.'
  }
  return error?.message || fallback
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config
    const responseData = error.response?.data
    const responseMessage = typeof responseData === 'string'
      ? responseData
      : responseData?.erro || responseData?.message || ''
    if (
      error.response?.status === 428 &&
      error.response?.data?.reautenticacaoNecessaria &&
      reauthHandler &&
      config &&
      !config.__reauthRetry &&
      !config.url?.includes('/auth/reauth')
    ) {
      config.__reauthRetry = true
      await reauthHandler()
      return api(config)
    }
    const sessaoExpirada = error.response?.status === 403
      && responseMessage.toLowerCase().includes('sessão de segurança expirada')
    if (sessaoExpirada && config && !config.__csrfRetry && !config.url?.includes('/auth/csrf')) {
      config.__csrfRetry = true
      try {
        const token = await refreshCsrfToken()
        if (token && config.headers) {
          config.headers['X-XSRF-TOKEN'] = token
        }
        return api(config)
      } catch {
        // A sessão pode ter expirado de fato; o fluxo abaixo encerra o acesso local.
      }
    }
    if ((error.response?.status === 401 || sessaoExpirada) && !error.config?.url?.includes('/auth/me')) {
      if (sessaoExpirada) sessionStorage.setItem('auth:reason', 'sessao')
      window.dispatchEvent(new Event('auth:unauthorized'))
    }
    return Promise.reject(error)
  }
)

export default api
