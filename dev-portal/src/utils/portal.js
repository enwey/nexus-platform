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

export function getGameStatusMeta(status) {
  const map = {
    APPROVED: { type: 'success', label: 'Approved' },
    PENDING: { type: 'warning', label: 'Pending' },
    PROCESSING: { type: 'info', label: 'Processing' },
    DRAFT: { type: '', label: 'Draft' },
    REJECTED: { type: 'danger', label: 'Rejected' },
    SUBMITTED: { type: 'warning', label: 'Submitted' }
  }

  return map[status] || { type: 'info', label: status || '-' }
}

export function summarizeVersionHealth(versions) {
  const rows = Array.isArray(versions) ? versions : []
  return {
    total: rows.length,
    approved: rows.filter((item) => item.status === 'APPROVED').length,
    submitted: rows.filter((item) => item.status === 'SUBMITTED').length,
    rejected: rows.filter((item) => item.status === 'REJECTED').length
  }
}

export function getFrontendStateMeta(state) {
  const map = {
    LIVE: { type: 'success', label: 'Live' },
    HIDDEN: { type: 'warning', label: 'Hidden' },
    BLOCKED: { type: 'danger', label: 'Blocked' }
  }
  return map[state] || { type: 'info', label: state || '-' }
}
