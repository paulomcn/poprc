import axios from 'axios'
import { API_BASE_URL, API_ORIGIN } from './runtimeConfig'

const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
})

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
  (error) => {
    if (error.response?.status === 401) {
      window.location.href = `${API_ORIGIN}/login`
    }
    return Promise.reject(error)
  }
)

export default api
