import axios from 'axios'
import { API_BASE_URL, API_ORIGIN } from './runtimeConfig'

const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
})

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
