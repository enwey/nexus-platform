const PORTAL_NAME = 'dev-portal'

export const API_BASE_URL_ENV_KEYS = ['VITE_PLATFORM_API_BASE_URL', 'VITE_API_BASE_URL']

function buildMessage(detail) {
  return `[${PORTAL_NAME}] ${detail} Configure ${API_BASE_URL_ENV_KEYS.join(' or ')} in your Vite environment.`
}

function createConfigError(code, detail) {
  const error = new Error(buildMessage(detail))
  error.name = 'PortalApiConfigError'
  error.code = code
  return error
}

export function normalizeApiBaseUrl(value) {
  if (typeof value !== 'string') return ''
  return value.trim().replace(/\/+$/, '')
}

export function resolveApiBaseUrl(env = {}) {
  const rawValue = normalizeApiBaseUrl(env.VITE_PLATFORM_API_BASE_URL || env.VITE_API_BASE_URL)
  if (!rawValue) {
    throw createConfigError('PORTAL_API_BASE_URL_MISSING', 'Missing API base URL.')
  }

  if (rawValue.startsWith('/')) {
    return rawValue
  }

  try {
    const parsed = new URL(rawValue)
    if (!['http:', 'https:'].includes(parsed.protocol)) {
      throw new Error('unsupported protocol')
    }
  } catch {
    throw createConfigError(
      'PORTAL_API_BASE_URL_INVALID',
      `Invalid API base URL "${rawValue}". Use an absolute http(s) URL or a root-relative path such as /api/v1.`
    )
  }

  return rawValue
}

export function getRuntimeApiBaseUrl() {
  return resolveApiBaseUrl(import.meta.env || {})
}

export function getPortalApiConfigHelpText(locale = 'zh-CN') {
  if (locale === 'zh-TW') {
    return [
      `缺少 API 位址設定：請設定 ${API_BASE_URL_ENV_KEYS.join(' / ')}`,
      '本機開發：使用 .env.development，或複製 .env.example 為 .env.local 後重新啟動 Vite',
      '正式部署：在建置或託管環境中明確注入正式 API 位址，或使用同網域 /api/v1 反向代理'
    ]
  }

  if (locale === 'en') {
    return [
      `Missing API base URL configuration: set ${API_BASE_URL_ENV_KEYS.join(' / ')}`,
      'Local development: use .env.development, or copy .env.example to .env.local and restart Vite',
      'Production: inject the real API base URL during build/deploy, or use a same-origin /api/v1 reverse proxy'
    ]
  }

  return [
    `缺少 API 地址配置：请设置 ${API_BASE_URL_ENV_KEYS.join(' / ')}`,
    '本地开发：使用 .env.development 或复制 .env.example 为 .env.local 后重启 Vite',
    '生产部署：在构建或托管环境中显式注入正式 API 地址，或使用同域 /api/v1 反向代理'
  ]
}
