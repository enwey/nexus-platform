import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  timeout: 30000
})

let redirectingToLogin = false

function isAuthExpiredMessage(message = '') {
  return /登录|凭证|失效|未授权|unauthorized|token/i.test(message)
}

function redirectToLogin(message = '登录状态已失效，请重新登录') {
  if (redirectingToLogin) {
    return
  }
  redirectingToLogin = true

  localStorage.removeItem('token')
  localStorage.removeItem('portal_user')
  ElMessage.warning(message)

  const current = `${window.location.pathname}${window.location.search}`
  window.location.href = `/login?redirect=${encodeURIComponent(current)}`
}

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response.data
    }

    const res = response.data
    if (typeof res !== 'object' || res === null) {
      return res
    }

    if (res.code === 401 || isAuthExpiredMessage(res.message)) {
      const message = res.message || '登录状态已失效，请重新登录'
      redirectToLogin(message)
      return Promise.reject(new Error(message))
    }

    if (res.code !== 0) {
      const message = res.message || '请求失败'
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }

    return res
  },
  (error) => {
    if (error?.response?.status === 401) {
      const message = typeof error?.response?.data === 'string'
        ? error.response.data
        : '登录状态已失效，请重新登录'
      redirectToLogin(message)
      return Promise.reject(error)
    }

    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request

