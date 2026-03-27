import axios from 'axios'
import { ElMessage } from 'element-plus'

const TOKEN_KEY = 'token'
const USER_KEY = 'portal_user'
const REFRESH_TOKEN_KEY = 'refresh_token'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  timeout: 30000
})

let redirectingToLogin = false
let refreshPromise = null

function isAuthExpiredMessage(message = '') {
  return /鐧诲綍|鍑瘉|澶辨晥|鏈巿鏉億unauthorized|token/i.test(message)
}

function clearSessionStorage() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

function redirectToLogin(message = '鐧诲綍鐘舵€佸凡澶辨晥锛岃閲嶆柊鐧诲綍') {
  if (redirectingToLogin) {
    return
  }
  redirectingToLogin = true
  clearSessionStorage()
  ElMessage.warning(message)

  const current = `${window.location.pathname}${window.location.search}`
  window.location.href = `/login?redirect=${encodeURIComponent(current)}`
}

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

request.interceptors.response.use(
  async (response) => {
    const res = response.data
    if (typeof res !== 'object' || res === null) {
      return res
    }

    if (res.code === 401 || isAuthExpiredMessage(res.message)) {
      const message = res.message || '鐧诲綍鐘舵€佸凡澶辨晥锛岃閲嶆柊鐧诲綍'
      const retried = await retryWithRefresh(response.config, message)
      if (retried) {
        return retried
      }
      redirectToLogin(message)
      return Promise.reject(new Error(message))
    }

    if (res.code !== 0) {
      const message = res.message || '璇锋眰澶辫触'
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }

    return res
  },
  async (error) => {
    if (error?.response?.status === 401) {
      const message = typeof error?.response?.data === 'string'
        ? error.response.data
        : '鐧诲綍鐘舵€佸凡澶辨晥锛岃閲嶆柊鐧诲綍'
      const retried = await retryWithRefresh(error.config, message)
      if (retried) {
        return retried
      }
      redirectToLogin(message)
      return Promise.reject(error)
    }

    ElMessage.error(error.message || '缃戠粶閿欒')
    return Promise.reject(error)
  }
)

export default request
