const PORTAL_NAME = 'ops-portal'

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

export function getPortalApiConfigHelpText() {
  return [
    `缺少 API 地址配置：请设置 ${API_BASE_URL_ENV_KEYS.join(' / ')}`,
    '本地开发：使用 .env.development 或复制 .env.example 为 .env.local 后重启 Vite',
    '生产部署：在构建或托管环境中显式注入正式 API 地址，或使用同域 /api/v1 反向代理'
  ]
}
