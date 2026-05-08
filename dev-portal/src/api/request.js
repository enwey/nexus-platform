import axios from 'axios'
import { ElMessage } from 'element-plus'
import { ltGlobal } from '../i18n'
import { getRuntimeApiBaseUrl } from '../config/apiBaseUrl'

const TOKEN_KEY = 'token'
const USER_KEY = 'portal_user'
const REFRESH_TOKEN_KEY = 'refresh_token'

function safeGet(key) {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

function safeSet(key, value) {
  try {
    localStorage.setItem(key, value)
  } catch {
    // Ignore storage write failures in private mode.
  }
}

function safeRemove(key) {
  try {
    localStorage.removeItem(key)
  } catch {
    // Ignore storage removal failures in private mode.
  }
}

const request = axios.create({
  timeout: 30000
})

request.defaults.baseURL = getRuntimeApiBaseUrl()

let redirectingToLogin = false
let refreshPromise = null
const recentIdempotencyKeys = new Map()

const msgAuthExpired = () => ltGlobal('登录状态已失效，请重新登录', '登入狀態已失效，請重新登入', 'Session expired. Please sign in again.')
const msgRequestFailed = () => ltGlobal('请求失败', '請求失敗', 'Request failed')
const msgNetworkError = () => ltGlobal('网络错误', '網路錯誤', 'Network error')

function isAuthExpiredMessage(message = '') {
  return /登录|憑證|失效|未授權|unauthorized|token/i.test(message)
}

function createRequestError(payload, fallbackMessage) {
  const message = payload?.error?.userMessage || payload?.message || fallbackMessage
  const error = new Error(message)
  error.code = payload?.code
  error.errorCode = payload?.error?.errorCode
  error.details = payload?.error?.details
  error.requestId = payload?.error?.requestId
  error.retryable = payload?.error?.retryable
  return error
}

function createIdempotencyKey() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `idem_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
}

function buildIdempotencyFingerprint(config) {
  const method = (config?.method || 'get').toUpperCase()
  const url = config?.url || ''
  let body = ''
  if (config?.data instanceof FormData) {
    body = '[form-data]'
  } else if (typeof config?.data === 'string') {
    body = config.data
  } else if (config?.data != null) {
    try {
      body = JSON.stringify(config.data)
    } catch {
      body = '[unserializable]'
    }
  }
  return `${method}|${url}|${body}`
}

function resolveIdempotencyKey(config) {
  const method = (config?.method || 'get').toUpperCase()
  if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) return null
  const fingerprint = buildIdempotencyFingerprint(config)
  const now = Date.now()
  const existing = recentIdempotencyKeys.get(fingerprint)
  if (existing && existing.expiresAt > now) {
    return existing.key
  }
  const key = createIdempotencyKey()
  recentIdempotencyKeys.set(fingerprint, { key, expiresAt: now + 5000 })
  return key
}

function clearSessionStorage() {
  safeRemove(TOKEN_KEY)
  safeRemove(USER_KEY)
  safeRemove(REFRESH_TOKEN_KEY)
}

function redirectToLogin(message = msgAuthExpired()) {
  if (redirectingToLogin) return
  redirectingToLogin = true
  clearSessionStorage()
  ElMessage.warning(message)

  const current = `${window.location.pathname}${window.location.search}`
  window.location.href = `/login?redirect=${encodeURIComponent(current)}`
}

async function refreshAccessToken() {
  const refreshToken = safeGet(REFRESH_TOKEN_KEY)
  if (!refreshToken) throw new Error('Missing refresh token')

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

      safeSet(TOKEN_KEY, res.data.token)
      if (res.data.refreshToken) {
        safeSet(REFRESH_TOKEN_KEY, res.data.refreshToken)
      } else {
        safeRemove(REFRESH_TOKEN_KEY)
      }
      if (res.data.user) {
        safeSet(USER_KEY, JSON.stringify(res.data.user))
      }
      return res.data.token
    }).finally(() => {
      refreshPromise = null
    })
  }

  return refreshPromise
}

async function retryWithRefresh(config, message) {
  if (config?.skipAuthRefresh || config?._retry || !safeGet(REFRESH_TOKEN_KEY)) {
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
    const token = safeGet(TOKEN_KEY)
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    config.headers = config.headers || {}
    if (!config.headers['X-Idempotency-Key']) {
      const idempotencyKey = resolveIdempotencyKey(config)
      if (idempotencyKey) {
        config.headers['X-Idempotency-Key'] = idempotencyKey
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  async (response) => {
    const res = response.data
    if (typeof res !== 'object' || res === null) return res

    if (res.code === 401 || isAuthExpiredMessage(res.message)) {
      const message = res?.error?.userMessage || res.message || msgAuthExpired()
      const retried = await retryWithRefresh(response.config, message)
      if (retried) return retried
      redirectToLogin(message)
      return Promise.reject(createRequestError(res, message))
    }

    if (res.code !== 0) {
      const message = res?.error?.userMessage || res.message || msgRequestFailed()
      ElMessage.error(message)
      return Promise.reject(createRequestError(res, message))
    }

    return res
  },
  async (error) => {
    if (error?.response?.status === 401) {
      const message = typeof error?.response?.data === 'string'
        ? error.response.data
        : error?.response?.data?.error?.userMessage || msgAuthExpired()
      const retried = await retryWithRefresh(error.config, message)
      if (retried) return retried
      redirectToLogin(message)
      return Promise.reject(error)
    }

    const normalizedError = error?.response?.data && typeof error.response.data === 'object'
      ? createRequestError(error.response.data, error.message || msgNetworkError())
      : error
    ElMessage.error(normalizedError?.message || msgNetworkError())
    return Promise.reject(normalizedError)
  }
)

export default request
