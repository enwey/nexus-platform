import {
  DISCOVER_SCOPE_COMMUNITY,
  DISCOVER_SCOPE_DISCOVER_BANNERS,
  DISCOVER_SCOPE_GAME_BANNERS,
  fromDateTimeInputValue,
  toDateTimeInputValue
} from '../../../utils/discoverOps'

export const customCatalogKeys = new Set([
  'librarySlots',
  'discoverSlots',
  'discoverCategories',
  'discoverContent',
  'recommendationItems',
  'launchAds'
])

export const isCustomCatalogKey = (catalogKey) => customCatalogKeys.has(catalogKey)

export const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString()
}

export const toDateInputValue = (value) => toDateTimeInputValue(value)
export const fromDateInputValue = (value) => fromDateTimeInputValue(value)

export const effectiveStateType = (state) => {
  if (state === 'LIVE') return 'success'
  if (state === 'SCHEDULED') return 'warning'
  if (state === 'EXPIRED') return 'danger'
  if (['PAUSED', 'INACTIVE', 'ARCHIVED'].includes(state)) return 'info'
  return ''
}

export const discoverScopeOptions = [
  { value: DISCOVER_SCOPE_DISCOVER_BANNERS, label: 'Discover Banners' },
  { value: DISCOVER_SCOPE_GAME_BANNERS, label: 'Library Banners' },
  { value: DISCOVER_SCOPE_COMMUNITY, label: 'Community Recommendations' }
]

export const getScopeCodeFromSlotCode = (slotCode) => {
  switch (slotCode) {
    case 'DISCOVER_HERO':
    case 'DISCOVER_RANK':
      return DISCOVER_SCOPE_DISCOVER_BANNERS
    case 'LIBRARY_COLDSTART_HERO':
    case 'LIBRARY_TOP_BANNER':
      return DISCOVER_SCOPE_GAME_BANNERS
    case 'COMMUNITY_TODAY':
      return DISCOVER_SCOPE_COMMUNITY
    default:
      return null
  }
}

export const getScopeCodeFromContentType = (contentType) => {
  switch (contentType) {
    case 'DISCOVER_HERO':
      return DISCOVER_SCOPE_DISCOVER_BANNERS
    case 'LIBRARY_TOP_BANNER':
      return DISCOVER_SCOPE_GAME_BANNERS
    default:
      return null
  }
}

export const buildPreviewLines = (preview) => {
  if (!preview) return []
  return [
    ['Scope', preview.scopeName || preview.scopeCode || '-'],
    ['Draft Count', preview.draftCount ?? '-'],
    ['Live Count', preview.liveCount ?? '-'],
    ['Changed Items', preview.changedCount ?? '-'],
    ['Added Items', preview.addedCount ?? '-'],
    ['Removed Items', preview.removedCount ?? '-'],
    ['Next Publish', formatDateTime(preview.nextScheduledAt)]
  ]
}
