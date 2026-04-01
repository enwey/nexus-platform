import axios from 'axios'
import { ElMessage } from 'element-plus'

const TOKEN_KEY = 'ops_token'
const USER_KEY = 'ops_user'
const REFRESH_TOKEN_KEY = 'ops_refresh_token'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  timeout: 30000
})

let redirectingToLogin = false
let refreshPromise = null

function isAuthExpiredMessage(message = '') {
  return /登录|凭证|失效|未授权|unauthorized|token/i.test(message)
}

function redirectToLogin(message = '登录状态已失效，请重新登录') {
  if (redirectingToLogin) {
    return
  }
  redirectingToLogin = true

  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  ElMessage.warning(message)

  const current = `${window.location.pathname}${window.location.search}`
  window.location.href = `/login?redirect=${encodeURIComponent(current)}`
}

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

async function refreshAccessToken() {
  const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
  if (!refreshToken) {
    throw new Error('Missing refresh token')
  }

  if (!refreshPromise) {
    refreshPromise = axios.post(
      `${request.defaults.baseURL}/user/refresh`,
      { refreshToken },
      { timeout: request.defaults.timeout }
    ).then((response) => {
      const res = response.data
      if (!res || res.code !== 0 || !res.data?.token) {
        throw new Error(res?.message || 'Refresh failed')
      }

      localStorage.setItem(TOKEN_KEY, res.data.token)
      if (res.data.refreshToken) {
        localStorage.setItem(REFRESH_TOKEN_KEY, res.data.refreshToken)
      } else {
        localStorage.removeItem(REFRESH_TOKEN_KEY)
      }
      if (res.data.user) {
        localStorage.setItem(USER_KEY, JSON.stringify(res.data.user))
      }
      return res.data.token
    }).finally(() => {
      refreshPromise = null
    })
  }

  return refreshPromise
}

async function retryWithRefresh(config, message) {
  if (config?.skipAuthRefresh || config?._retry || !localStorage.getItem(REFRESH_TOKEN_KEY)) {
    return null
  }

  config._retry = true
  try {
    const token = await refreshAccessToken()
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
    return request(config)
  } catch {
    redirectToLogin(message)
    return null
  }
}

request.interceptors.response.use(
  async (response) => {
    const res = response.data
    if (typeof res !== 'object' || res === null) {
      return res
    }

    if (res.code === 401 || isAuthExpiredMessage(res.message)) {
      const message = res.message || '登录状态已失效，请重新登录'
      const retried = await retryWithRefresh(response.config, message)
      if (retried) {
        return retried
      }
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
  async (error) => {
    if (error?.response?.status === 401) {
      const message = typeof error?.response?.data === 'string'
        ? error.response.data
        : '登录状态已失效，请重新登录'
      const retried = await retryWithRefresh(error.config, message)
      if (retried) {
        return retried
      }
      redirectToLogin(message)
      return Promise.reject(error)
    }

    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
