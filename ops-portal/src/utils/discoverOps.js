import {
  getDiscoverCategories,
  getDiscoverOpsConfig,
  updateDiscoverOpsConfig
} from '../api'
import request from '../api/request'

export const DISCOVER_SCOPE_DISCOVER_BANNERS = 'DISCOVER_BANNERS'
export const DISCOVER_SCOPE_GAME_BANNERS = 'GAME_BANNERS'
export const DISCOVER_SCOPE_COMMUNITY = 'COMMUNITY'

export const emptyRecommendationItem = () => ({
  id: null,
  appId: '',
  cardCategory: '',
  cardTitle: '',
  coverUrl: '',
  articleTag: '',
  articleTitle: '',
  articleBody: '',
  actionText: '',
  status: 'DRAFT',
  startAt: '',
  endAt: '',
  sortOrder: 0
})

export const emptyTopBanner = () => ({
  id: null,
  appId: '',
  badgeText: '',
  title: '',
  subtitle: '',
  coverUrl: '',
  status: 'DRAFT',
  startAt: '',
  endAt: '',
  sortOrder: 0
})

export const cloneRecommendationItem = (item = {}) => ({
  id: item.id ?? null,
  appId: item.appId || '',
  cardCategory: item.cardCategory || '',
  cardTitle: item.cardTitle || '',
  coverUrl: item.coverUrl || '',
  articleTag: item.articleTag || '',
  articleTitle: item.articleTitle || '',
  articleBody: item.articleBody || '',
  actionText: item.actionText || '',
  status: item.status || 'DRAFT',
  startAt: item.startAt || '',
  endAt: item.endAt || '',
  sortOrder: typeof item.sortOrder === 'number' ? item.sortOrder : 0
})

export const cloneTopBanner = (item = {}) => ({
  id: item.id ?? null,
  appId: item.appId || '',
  badgeText: item.badgeText || '',
  title: item.title || '',
  subtitle: item.subtitle || '',
  coverUrl: item.coverUrl || '',
  status: item.status || 'DRAFT',
  startAt: item.startAt || '',
  endAt: item.endAt || '',
  sortOrder: typeof item.sortOrder === 'number' ? item.sortOrder : 0
})

export const placementStatusOptions = [
  { value: 'DRAFT', label: ['草稿', '草稿', 'Draft'] },
  { value: 'PUBLISHED', label: ['已发布', '已發佈', 'Published'] },
  { value: 'PAUSED', label: ['暂停', '暫停', 'Paused'] }
]

export const toDateTimeInputValue = (value) => {
  if (!value) return ''
  return String(value).slice(0, 16)
}

export const fromDateTimeInputValue = (value) => {
  if (!value) return null
  return value.length === 16 ? `${value}:00` : value
}

export const derivePlacementState = (item, now = new Date()) => {
  const status = String(item?.status || 'DRAFT').toUpperCase()
  const startAt = item?.startAt ? new Date(item.startAt) : null
  const endAt = item?.endAt ? new Date(item.endAt) : null
  if (status === 'PAUSED') return 'PAUSED'
  if (status !== 'PUBLISHED') return 'DRAFT'
  if (startAt && startAt > now) return 'SCHEDULED'
  if (endAt && endAt < now) return 'EXPIRED'
  return 'LIVE'
}

const normalizeCategoryOptions = (data = []) =>
  data.map((item) => ({
    id: item.id,
    name: item.name,
    sortOrder: item.sortOrder
  }))

export const normalizeDiscoverWorkspace = (data = {}) => ({
  hero: data.hero || null,
  rankedAppIds: [...(data.rankedAppIds || [])],
  newbieAppIds: [...(data.newbieAppIds || [])],
  everyoneAppIds: [...(data.everyoneAppIds || [])],
  recommendList: [...(data.communityItems || [])].map((item) => ({
    ...cloneRecommendationItem(item),
    startAt: toDateTimeInputValue(item.startAt),
    endAt: toDateTimeInputValue(item.endAt)
  })),
  gameTopBanners: [...(data.gameTopBanners || [])].map((item) => ({
    ...cloneTopBanner(item),
    startAt: toDateTimeInputValue(item.startAt),
    endAt: toDateTimeInputValue(item.endAt)
  })),
  discoverTopBanners: [...(data.discoverTopBanners || [])].map((item) => ({
    ...cloneTopBanner(item),
    startAt: toDateTimeInputValue(item.startAt),
    endAt: toDateTimeInputValue(item.endAt)
  })),
  gameOptions: [...(data.availableGames || [])],
  categoryOptions: normalizeCategoryOptions(data.categoryOptions || []),
  slotControls: [...(data.slotControls || [])].map((item) => ({
    slotCode: item.slotCode || '',
    name: item.name || '',
    pageCode: item.pageCode || '',
    positionCode: item.positionCode || '',
    enabled: item.enabled !== false
  })),
  previews: [...(data.previews || [])],
  publishOrders: [...(data.publishOrders || [])],
  experiments: [...(data.experiments || [])]
})

export const fetchDiscoverWorkspace = async () => {
  const [configRes, categoryRes] = await Promise.all([
    getDiscoverOpsConfig(),
    getDiscoverCategories()
  ])
  const workspace = normalizeDiscoverWorkspace(configRes.data || {})
  return {
    ...workspace,
    categoryOptions: normalizeCategoryOptions(categoryRes.data || workspace.categoryOptions)
  }
}

const buildDiscoverPayload = (workspace) => ({
  hero: workspace.discoverTopBanners[0]
    ? {
        appId: workspace.discoverTopBanners[0].appId,
        title: workspace.discoverTopBanners[0].title,
        subtitle: workspace.discoverTopBanners[0].subtitle,
        badgeText: workspace.discoverTopBanners[0].badgeText,
        coverUrl: workspace.discoverTopBanners[0].coverUrl
      }
    : workspace.hero || null,
  gameTopBanners: workspace.gameTopBanners.map((item, index) => ({
    ...cloneTopBanner(item),
    sortOrder: index,
    startAt: fromDateTimeInputValue(item.startAt),
    endAt: fromDateTimeInputValue(item.endAt)
  })),
  discoverTopBanners: workspace.discoverTopBanners.map((item, index) => ({
    ...cloneTopBanner(item),
    sortOrder: index,
    startAt: fromDateTimeInputValue(item.startAt),
    endAt: fromDateTimeInputValue(item.endAt)
  })),
  rankedAppIds: [...workspace.rankedAppIds],
  newbieAppIds: [...workspace.newbieAppIds],
  everyoneAppIds: [...workspace.everyoneAppIds],
  communityItems: workspace.recommendList.map((item, index) => ({
    ...cloneRecommendationItem(item),
    sortOrder: index,
    startAt: fromDateTimeInputValue(item.startAt),
    endAt: fromDateTimeInputValue(item.endAt)
  }))
})

export const saveDiscoverSection = async (patch = {}) => {
  const currentRes = await getDiscoverOpsConfig()
  const currentWorkspace = normalizeDiscoverWorkspace(currentRes.data || {})
  const nextWorkspace = {
    ...currentWorkspace,
    ...patch
  }
  const saveRes = await updateDiscoverOpsConfig(buildDiscoverPayload(nextWorkspace))
  return normalizeDiscoverWorkspace(saveRes.data || {})
}

export const getScopePreview = (workspace, scopeCode) =>
  (workspace?.previews || []).find((item) => item.scopeCode === scopeCode) || null

export const getScopeOrders = (workspace, scopeCode) =>
  (workspace?.publishOrders || []).filter((item) => item.scopeCode === scopeCode)

export const getScopeExperiments = (workspace, scopeCode) =>
  (workspace?.experiments || []).filter((item) => item.scopeCode === scopeCode)

export const fetchDiscoverPublishPreview = (scopeCode) =>
  request({
    url: `/admin/ops/discover/publish-preview/${scopeCode}`,
    method: 'get'
  }).then((res) => res.data || null)

export const createDiscoverPublishOrder = (data) =>
  request({
    url: '/admin/ops/discover/publish-orders',
    method: 'post',
    data
  }).then((res) => normalizeDiscoverWorkspace(res.data || {}))

export const cancelDiscoverPublishOrder = (id, reason = '') =>
  request({
    url: `/admin/ops/discover/publish-orders/${id}/status`,
    method: 'put',
    data: { status: 'CANCELLED', reason }
  }).then((res) => normalizeDiscoverWorkspace(res.data || {}))

export const saveDiscoverExperiment = (data) =>
  request({
    url: '/admin/ops/discover/experiments',
    method: 'post',
    data
  }).then((res) => normalizeDiscoverWorkspace(res.data || {}))

export const updateDiscoverExperimentStatus = (id, status) =>
  request({
    url: `/admin/ops/discover/experiments/${id}/status`,
    method: 'put',
    data: { status }
  }).then((res) => normalizeDiscoverWorkspace(res.data || {}))
