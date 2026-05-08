export function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

export function normalizeTags(tags) {
  if (Array.isArray(tags)) {
    return tags.filter(Boolean).map((tag) => String(tag).trim()).filter(Boolean)
  }
  if (typeof tags === 'string') {
    return tags
      .split(',')
      .map((tag) => tag.trim())
      .filter(Boolean)
  }
  return []
}

export function toDateTimeLocal(value) {
  if (!value) return ''
  return String(value).slice(0, 16)
}

export function firstBySort(items = [], selector = (item) => item?.updatedAt || item?.createdAt || '') {
  return [...items].sort((left, right) => {
    const leftTime = new Date(selector(left)).getTime() || 0
    const rightTime = new Date(selector(right)).getTime() || 0
    return rightTime - leftTime
  })
}

export function summarizeGovernanceSnapshot(raw) {
  if (!raw) return '-'
  try {
    const json = JSON.parse(raw)
    return [
      `status:${json.accountStatus || '-'}`,
      `cert:${json.certificationStatus || '-'}`,
      `risk:${json.riskLevel || '-'}`,
      `white:${json.whitelistStatus || '-'}`,
      `viol:${json.violationCount ?? 0}`
    ].join(' | ')
  } catch {
    return raw
  }
}
