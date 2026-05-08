import {
  getAndroidChannels,
  getAndroidCompatibilityBlacklists,
  getAndroidConfig,
  getAndroidFeatureToggles,
  getAndroidGrayReleasePlans,
  getAndroidPageCircuitBreakers,
  getAuditLogs,
  getDeveloperAccounts,
  getDeveloperCertificationProfile,
  getDeveloperCertificationReviews,
  getDeveloperGovernanceRecords,
  getDiscoverCategories,
  getGameVersions,
  getLaunchAds,
  getOpsAccessControlRules,
  getOpsGameCategories,
  getOpsGameGovernanceImpact,
  getOpsGameProfile,
  getOpsGames,
  getOpsLoginRiskEvents,
  getOpsNotices,
  getOpsNoticeTemplates,
  getOpsReviewAppeals,
  getOpsReviews,
  getOpsRuleTemplates,
  getOpsTicketMessages,
  getOpsTickets,
  getRiskIncidentRecords,
  getRiskIncidents,
  getVerificationCodeLogs
} from '../../../api'
import { fetchDiscoverWorkspace } from '../../../utils/discoverOps'
import { getRuntimeApiBaseUrl } from '../../../config/apiBaseUrl'

export const tri = (zhCN, zhTW, en) => [zhCN, zhTW, en]

const activeText = tri('启用', '啟用', 'Enabled')
const disabledText = tri('停用', '停用', 'Disabled')

const keyText = (key) =>
  String(key)
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()

const asArray = (value) => (Array.isArray(value) ? value : [])
const isPrimitive = (value) => value == null || ['string', 'number', 'boolean'].includes(typeof value)

const formatDateTime = (value) => {
  if (!value) return '-'
  try {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return String(value)
    return date.toLocaleString()
  } catch {
    return String(value)
  }
}

const createPrimitiveFields = (record, excludes = []) =>
  Object.entries(record || {})
    .filter(([key, value]) => !excludes.includes(key) && isPrimitive(value))
    .map(([key, value]) => ({
      label: keyText(key),
      value: value === '' || value == null ? '-' : value
    }))

const getDisplayCategory = (item) => item.categoryName || item.category || item.cardCategory || '-'

const normalizeGameRow = (row) => ({
  ...row,
  id: row.id ?? row.gameId,
  gameId: row.id ?? row.gameId,
  displayStatus: row.gameStatus || row.status || '-',
  frontendDisplayStatus: row.frontendState || row.visibilityStatus || '-',
  displayCategory: getDisplayCategory(row),
  displayDeveloperId: row.developerId ?? '-'
})

const cache = new Map()

async function memo(key, loader, force = false) {
  if (!force && cache.has(key)) return cache.get(key)
  const value = await loader()
  cache.set(key, value)
  return value
}

function invalidate(keys = []) {
  keys.forEach((key) => cache.delete(key))
}

async function loadGames(force = false) {
  return memo('ops-games', async () => {
    const res = await getOpsGames()
    return asArray(res.data).map(normalizeGameRow)
  }, force)
}

async function loadAllVersions(force = false) {
  return memo('ops-versions', async () => {
    const games = await loadGames(force)
    const versionGroups = await Promise.all(
      games.map(async (game) => {
        try {
          const response = await getGameVersions(game.gameId)
          return asArray(response.data).map((version) => ({
            ...version,
            id: version.id ?? `${game.gameId}-${version.version || version.versionName || Math.random()}`,
            gameId: game.gameId,
            appId: game.appId,
            gameName: game.name,
            developerId: game.developerId,
            gameStatus: game.gameStatus,
            frontendState: game.frontendState
          }))
        } catch {
          return []
        }
      })
    )
    return versionGroups.flat()
  }, force)
}

async function loadVersionReviews(force = false) {
  return memo('ops-review-versions', async () => {
    const res = await getOpsReviews()
    return asArray(res.data).map((item) => ({
      ...item,
      id: item.versionId ?? item.id,
      reviewId: item.versionId ?? item.id,
      displayStatus: item.gameStatus || '-',
      displayPriority: item.queuePriority || '-'
    }))
  }, force)
}

async function loadDeveloperAccounts(force = false) {
  return memo('ops-developers', async () => {
    const res = await getDeveloperAccounts()
    return asArray(res.data).map((item) => ({
      ...item,
      id: item.id,
      developerId: item.id,
      displayStatus: item.accountStatus || '-'
    }))
  }, force)
}

async function loadDiscoverWorkspace(force = false) {
  return memo('ops-discover-workspace', async () => fetchDiscoverWorkspace(), force)
}

async function loadLibrarySlots(force = false) {
  const workspace = await loadDiscoverWorkspace(force)
  const previewMap = new Map(asArray(workspace.previews).map((item) => [item.scopeCode, item]))
  return [
    {
      id: 'NEWBIE_MUST_PLAY',
      slotCode: 'NEWBIE_MUST_PLAY',
      slotName: tri('新手必玩', '新手必玩', 'Newbie Must Play'),
      gameCount: asArray(workspace.newbieAppIds).length,
      enabled: true,
      sourceType: tri('运营精选', '營運精選', 'Curated'),
      preview: previewMap.get('DISCOVER_BANNERS') || null,
      appIds: asArray(workspace.newbieAppIds)
    },
    {
      id: 'EVERYONE_PLAYING',
      slotCode: 'EVERYONE_PLAYING',
      slotName: tri('大家都在玩', '大家都在玩', 'Everyone Playing'),
      gameCount: asArray(workspace.everyoneAppIds).length,
      enabled: true,
      sourceType: tri('精选优先 + 热度兜底', '精選優先 + 熱度兜底', 'Curated + trending fallback'),
      preview: previewMap.get('GAME_BANNERS') || null,
      appIds: asArray(workspace.everyoneAppIds)
    }
  ]
}

async function loadDiscoverSlots(force = false) {
  const workspace = await loadDiscoverWorkspace(force)
  return asArray(workspace.slotControls).map((item) => ({
    ...item,
    id: item.slotCode,
    slotName: item.name,
    enabledLabel: item.enabled ? activeText : disabledText
  }))
}

async function loadDiscoverCategoriesData(force = false) {
  return memo('ops-discover-categories', async () => {
    const res = await getDiscoverCategories()
    return asArray(res.data).map((item) => ({
      ...item,
      id: item.id,
      displayStatus: item.enabled === false ? 'DISABLED' : 'ACTIVE'
    }))
  }, force)
}

async function loadDiscoverContent(force = false) {
  const workspace = await loadDiscoverWorkspace(force)
  return [
    ...asArray(workspace.discoverTopBanners).map((item) => ({
      ...item,
      id: `discover-banner-${item.id ?? item.appId}`,
      contentType: 'DISCOVER_HERO',
      displayStatus: item.status || '-'
    })),
    ...asArray(workspace.gameTopBanners).map((item) => ({
      ...item,
      id: `library-banner-${item.id ?? item.appId}`,
      contentType: 'LIBRARY_TOP_BANNER',
      displayStatus: item.status || '-'
    }))
  ]
}

async function loadRecommendationItems(force = false) {
  const workspace = await loadDiscoverWorkspace(force)
  return asArray(workspace.recommendList).map((item) => ({
    ...item,
    id: item.id ?? `${item.appId}-${item.cardTitle}`,
    displayStatus: item.status || '-'
  }))
}

async function loadLaunchAds(force = false) {
  return memo('ops-launch-ads', async () => {
    const res = await getLaunchAds()
    return asArray(res.data?.items || res.data).map((item) => ({
      ...item,
      id: item.id,
      displayStatus: item.status || '-'
    }))
  }, force)
}

async function loadAndroidConfig(force = false) {
  return memo('ops-android-config', async () => {
    const res = await getAndroidConfig()
    return res.data || {}
  }, force)
}

async function loadChannels(force = false) {
  return memo('ops-android-channels', async () => {
    const res = await getAndroidChannels()
    return asArray(res.data)
  }, force)
}

async function loadFeatureToggles(force = false) {
  return memo('ops-android-feature-toggles', async () => {
    const res = await getAndroidFeatureToggles()
    return asArray(res.data)
  }, force)
}

async function loadCircuitBreakers(force = false) {
  return memo('ops-android-circuit-breakers', async () => {
    const res = await getAndroidPageCircuitBreakers()
    return asArray(res.data)
  }, force)
}

async function loadCompatibilityRules(force = false) {
  return memo('ops-android-compatibility', async () => {
    const res = await getAndroidCompatibilityBlacklists()
    return asArray(res.data)
  }, force)
}

async function loadGrayRollouts(force = false) {
  return memo('ops-android-gray-rollouts', async () => {
    const res = await getAndroidGrayReleasePlans()
    return asArray(res.data)
  }, force)
}

async function loadTickets(force = false) {
  return memo('ops-tickets', async () => {
    const res = await getOpsTickets()
    return asArray(res.data).map((item) => ({
      ...item,
      id: item.id,
      status: item.status || item.ticketStatus || '-',
      displayStatus: item.status || item.ticketStatus || '-'
    }))
  }, force)
}

async function loadNotices(force = false) {
  return memo('ops-notices', async () => {
    const res = await getOpsNotices()
    return asArray(res.data).map((item) => ({
      ...item,
      id: item.id,
      status: item.deliveryStatus || item.effectiveStatus || '-',
      audienceScope: item.audienceRole,
      displayStatus: item.deliveryStatus || item.effectiveStatus || '-'
    }))
  }, force)
}

async function loadNoticeTemplates(force = false) {
  return memo('ops-notice-templates', async () => {
    const res = await getOpsNoticeTemplates()
    return asArray(res.data).map((item) => ({
      ...item,
      id: item.id || item.templateCode,
      channel: item.channelType,
      languageCode: item.languageTag
    }))
  }, force)
}

async function loadRiskIncidents(force = false) {
  return memo('ops-risk-incidents', async () => {
    const res = await getRiskIncidents()
    return asArray(res.data).map((item) => ({
      ...item,
      id: item.id,
      status: item.status || '-',
      ownerUserId: item.assignee || '-',
      dueAt: item.handlingDeadline,
      displayStatus: item.status || '-'
    }))
  }, force)
}

async function loadLoginAnomalies(force = false) {
  return memo('ops-login-anomalies', async () => {
    const res = await getOpsLoginRiskEvents()
    return asArray(res.data).map((item) => ({
      ...item,
      id: item.id,
      username: item.loginId,
      ipAddress: item.clientIp,
      deviceFingerprint: item.deviceId
    }))
  }, force)
}

async function loadAccessRules(force = false) {
  return memo('ops-access-rules', async () => {
    const res = await getOpsAccessControlRules()
    return asArray(res.data).map((item) => ({
      ...item,
      id: item.id || item.ruleCode,
      ruleCode: item.ruleCode || item.id,
      displayStatus: item.status || '-'
    }))
  }, force)
}

async function loadAuditLogs(force = false) {
  return memo('ops-audit-logs', async () => {
    const res = await getAuditLogs()
    return asArray(res.data).map((item) => ({
      ...item,
      id: item.id || item.logId || `${item.action}-${item.createdAt}`
    }))
  }, force)
}

async function loadSmsLogs(force = false) {
  return memo('ops-sms-logs', async () => {
    const res = await getVerificationCodeLogs()
    return asArray(res.data).map((item) => ({
      ...item,
      id: item.id || item.logId || `${item.phone || item.mobile}-${item.createdAt}`
    }))
  }, force)
}

async function loadGameCategories(force = false) {
  return memo('ops-game-categories', async () => {
    const res = await getOpsGameCategories()
    return asArray(res.data).map((item) => ({
      ...item,
      id: item.id,
      displayStatus: item.enabled === false ? 'DISABLED' : 'ACTIVE'
    }))
  }, force)
}

async function loadReviewTemplates(force = false) {
  return memo('ops-review-templates', async () => {
    const res = await getOpsRuleTemplates()
    return asArray(res.data).map((item) => ({
      ...item,
      id: item.id,
      displayStatus: item.status || '-'
    }))
  }, force)
}

const findById = (items, id) => items.find((item) => String(item.id) === String(id)) || null

function autoSections(detail, excludes = []) {
  if (!detail) return []
  const primitiveFields = createPrimitiveFields(detail, excludes)
  const sections = primitiveFields.length
    ? [{ key: 'base', title: tri('基础信息', '基礎資訊', 'Overview'), type: 'fields', fields: primitiveFields }]
    : []

  Object.entries(detail).forEach(([key, value]) => {
    if (excludes.includes(key) || isPrimitive(value) || value == null) return
    if (Array.isArray(value)) {
      if (!value.length) return
      if (value.every((item) => isPrimitive(item))) {
        sections.push({
          key,
          title: tri(keyText(key), keyText(key), keyText(key)),
          type: 'text-list',
          items: value.map((item) => (item == null || item === '' ? '-' : String(item)))
        })
        return
      }
      const sample = value.find(Boolean) || {}
      const columns = Object.keys(sample).filter((column) => isPrimitive(sample[column])).slice(0, 6).map((column) => ({
        prop: column,
        label: tri(keyText(column), keyText(column), keyText(column))
      }))
      sections.push({
        key,
        title: tri(keyText(key), keyText(key), keyText(key)),
        type: 'table',
        columns,
        rows: value
      })
      return
    }

    sections.push({
      key,
      title: tri(keyText(key), keyText(key), keyText(key)),
      type: 'json',
      value
    })
  })
  return sections
}

export const catalogRegistry = {
  games: {
    listTitle: tri('游戏列表', '遊戲列表', 'Game List'),
    listDescription: tri('查看开发者提交的全部游戏，并统一进入单游戏详情做状态治理。', '查看開發者提交的全部遊戲，並統一進入單遊戲詳情做狀態治理。', 'Inspect all submitted games and move into single-game detail for governance actions.'),
    detailTitle: tri('游戏详情', '遊戲詳情', 'Game Detail'),
    idKey: 'gameId',
    detailRouteName: 'OpsGameDetail',
    listColumns: [
      { prop: 'name', label: tri('游戏名称', '遊戲名稱', 'Game') },
      { prop: 'appId', label: tri('AppID', 'AppID', 'AppID') },
      { prop: 'displayCategory', label: tri('分类', '分類', 'Category') },
      { prop: 'displayStatus', label: tri('审核状态', '審核狀態', 'Review Status'), type: 'tag' },
      { prop: 'frontendDisplayStatus', label: tri('前端状态', '前端狀態', 'Frontend State'), type: 'tag' },
      { prop: 'displayDeveloperId', label: tri('开发者 ID', '開發者 ID', 'Developer ID') }
    ],
    loadList: loadGames,
    loadDetail: async (id) => {
      const games = await loadGames()
      const base = findById(games, id)
      if (!base) return null
      const [profileRes, versionsRes, impactRes] = await Promise.all([
        getOpsGameProfile(base.gameId),
        getGameVersions(base.gameId),
        getOpsGameGovernanceImpact(base.gameId)
      ])
      return {
        ...base,
        profile: profileRes.data || {},
        versions: asArray(versionsRes.data),
        governanceImpact: impactRes.data || {}
      }
    },
    buildSections: (detail) => [
      {
        key: 'base',
        title: tri('基础信息', '基礎資訊', 'Overview'),
        type: 'fields',
        fields: [
          { label: 'AppID', value: detail.appId || '-' },
          { label: tri('游戏名称', '遊戲名稱', 'Game'), value: detail.name || '-' },
          { label: tri('审核状态', '審核狀態', 'Review Status'), value: detail.displayStatus },
          { label: tri('前端状态', '前端狀態', 'Frontend State'), value: detail.frontendDisplayStatus },
          { label: tri('开发者 ID', '開發者 ID', 'Developer ID'), value: detail.displayDeveloperId },
          { label: tri('分类', '分類', 'Category'), value: detail.displayCategory }
        ]
      },
      {
        key: 'profile',
        title: tri('运行时展示资料', '運行時展示資料', 'Runtime Presentation'),
        type: 'fields',
        fields: createPrimitiveFields(detail.profile || {})
      },
      {
        key: 'versions',
        title: tri('版本记录', '版本記錄', 'Versions'),
        type: 'table',
        columns: [
          { prop: 'version', label: tri('版本', '版本', 'Version') },
          { prop: 'versionName', label: tri('版本名', '版本名', 'Version Name') },
          { prop: 'status', label: tri('状态', '狀態', 'Status') },
          { prop: 'updatedAt', label: tri('更新时间', '更新時間', 'Updated At'), formatter: formatDateTime }
        ],
        rows: asArray(detail.versions)
      },
      {
        key: 'impact',
        title: tri('治理影响', '治理影響', 'Governance Impact'),
        type: 'fields',
        fields: createPrimitiveFields(detail.governanceImpact || {})
      }
    ]
  },
  versions: {
    listTitle: tri('版本列表', '版本列表', 'Version List'),
    listDescription: tri('统一查看各游戏版本、线上状态和版本资料。', '統一查看各遊戲版本、線上狀態和版本資料。', 'Inspect all versions, live state, and package metadata in one queue.'),
    detailTitle: tri('版本详情', '版本詳情', 'Version Detail'),
    idKey: 'id',
    detailRouteName: 'OpsVersionDetail',
    listColumns: [
      { prop: 'gameName', label: tri('游戏', '遊戲', 'Game') },
      { prop: 'appId', label: tri('AppID', 'AppID', 'AppID') },
      { prop: 'version', label: tri('版本号', '版本號', 'Version') },
      { prop: 'status', label: tri('版本状态', '版本狀態', 'Version Status'), type: 'tag' },
      { prop: 'gameStatus', label: tri('游戏状态', '遊戲狀態', 'Game Status'), type: 'tag' }
    ],
    loadList: loadAllVersions,
    loadDetail: async (id) => {
      const versions = await loadAllVersions()
      return findById(versions, id)
    },
    buildSections: (detail) => autoSections(detail, [])
  },
  gameReviews: {
    listTitle: tri('游戏审核列表', '遊戲審核列表', 'Game Review List'),
    listDescription: tri('查看游戏级审核队列，按游戏对象进入详情。', '查看遊戲級審核佇列，按遊戲物件進入詳情。', 'Review game-level approval queue and open a single game review detail.'),
    detailTitle: tri('游戏审核详情', '遊戲審核詳情', 'Game Review Detail'),
    idKey: 'gameId',
    detailRouteName: 'OpsGameReviewDetail',
    listColumns: [
      { prop: 'name', label: tri('游戏', '遊戲', 'Game') },
      { prop: 'appId', label: tri('AppID', 'AppID', 'AppID') },
      { prop: 'displayStatus', label: tri('审核状态', '審核狀態', 'Review Status'), type: 'tag' },
      { prop: 'frontendDisplayStatus', label: tri('前端状态', '前端狀態', 'Frontend State'), type: 'tag' }
    ],
    loadList: async (force = false) => (await loadGames(force)).filter((item) => ['PENDING', 'DRAFT', 'REJECTED'].includes(String(item.displayStatus).toUpperCase())),
    loadDetail: async (id) => {
      const games = await loadGames()
      return findById(games, id)
    },
    buildSections: (detail) => autoSections(detail, [])
  },
  versionReviews: {
    listTitle: tri('版本审核列表', '版本審核列表', 'Version Review List'),
    listDescription: tri('查看版本审核队列、负责人、SLA 和审核优先级。', '查看版本審核佇列、負責人、SLA 和審核優先級。', 'Inspect version review queue, assignee, SLA, and priority.'),
    detailTitle: tri('版本审核详情', '版本審核詳情', 'Version Review Detail'),
    idKey: 'reviewId',
    detailRouteName: 'OpsVersionReviewDetail',
    listColumns: [
      { prop: 'name', label: tri('游戏', '遊戲', 'Game') },
      { prop: 'appId', label: tri('AppID', 'AppID', 'AppID') },
      { prop: 'version', label: tri('版本', '版本', 'Version') },
      { prop: 'displayStatus', label: tri('状态', '狀態', 'Status'), type: 'tag' },
      { prop: 'displayPriority', label: tri('优先级', '優先級', 'Priority'), type: 'tag' },
      { prop: 'assignedReviewerId', label: tri('负责人', '負責人', 'Assignee') }
    ],
    loadList: loadVersionReviews,
    loadDetail: async (id) => {
      const items = await loadVersionReviews()
      const detail = findById(items, id)
      if (!detail) return null
      try {
        const versionsRes = await getGameVersions(detail.gameId)
        const versionRecord = asArray(versionsRes.data).find((item) => String(item.id) === String(detail.versionId))
        return { ...detail, versionRecord }
      } catch {
        return detail
      }
    },
    buildSections: (detail) => autoSections(detail, [])
  },
  certificationReviews: {
    listTitle: tri('资质审核列表', '資質審核列表', 'Certification Review List'),
    listDescription: tri('查看开发者资质审核与档案状态。', '查看開發者資質審核與檔案狀態。', 'Inspect developer certification queue and profile status.'),
    detailTitle: tri('资质审核详情', '資質審核詳情', 'Certification Review Detail'),
    idKey: 'developerId',
    detailRouteName: 'OpsCertificationReviewDetail',
    listColumns: [
      { prop: 'username', label: tri('开发者', '開發者', 'Developer') },
      { prop: 'email', label: tri('邮箱', '電子郵件', 'Email') },
      { prop: 'certificationProfileStatus', label: tri('档案状态', '檔案狀態', 'Profile Status'), type: 'tag' },
      { prop: 'certificationStatus', label: tri('资质状态', '資質狀態', 'Certification Status'), type: 'tag' },
      { prop: 'certificationSubjectName', label: tri('主体名称', '主體名稱', 'Subject Name') }
    ],
    loadList: async (force = false) => (await loadDeveloperAccounts(force)).filter((item) => item.certificationProfileStatus || item.certificationStatus),
    loadDetail: async (id) => {
      const [accounts, profileRes, reviewsRes] = await Promise.all([
        loadDeveloperAccounts(),
        getDeveloperCertificationProfile(id),
        getDeveloperCertificationReviews(id)
      ])
      const base = findById(accounts, id)
      return {
        ...base,
        certificationProfile: profileRes.data || {},
        certificationReviews: asArray(reviewsRes.data)
      }
    },
    buildSections: (detail) => [
      ...(detail ? [{
        key: 'account',
        title: tri('开发者信息', '開發者資訊', 'Developer'),
        type: 'fields',
        fields: createPrimitiveFields(detail, ['certificationProfile', 'certificationReviews', 'games'])
      }] : []),
      {
        key: 'profile',
        title: tri('资质档案', '資質檔案', 'Certification Profile'),
        type: 'fields',
        fields: createPrimitiveFields(detail?.certificationProfile || {})
      },
      {
        key: 'reviews',
        title: tri('审核记录', '審核記錄', 'Review Records'),
        type: 'table',
        columns: [
          { prop: 'actionType', label: tri('动作', '動作', 'Action') },
          { prop: 'afterStatus', label: tri('结果', '結果', 'Result') },
          { prop: 'reason', label: tri('原因', '原因', 'Reason') },
          { prop: 'createdAt', label: tri('时间', '時間', 'Time'), formatter: formatDateTime }
        ],
        rows: asArray(detail?.certificationReviews)
      }
    ]
  },
  appealReviews: {
    listTitle: tri('申诉复审列表', '申訴複審列表', 'Appeal Review List'),
    listDescription: tri('查看被驳回版本的申诉与复审处理状态。', '查看被駁回版本的申訴與複審處理狀態。', 'Inspect appeals for rejected versions and their reconsideration status.'),
    detailTitle: tri('申诉复审详情', '申訴複審詳情', 'Appeal Review Detail'),
    idKey: 'id',
    detailRouteName: 'OpsAppealReviewDetail',
    listColumns: [
      { prop: 'gameName', label: tri('游戏', '遊戲', 'Game') },
      { prop: 'appId', label: tri('AppID', 'AppID', 'AppID') },
      { prop: 'versionName', label: tri('版本', '版本', 'Version') },
      { prop: 'appealStatus', label: tri('申诉状态', '申訴狀態', 'Appeal Status'), type: 'tag' },
      { prop: 'appealReason', label: tri('申诉原因', '申訴原因', 'Appeal Reason') }
    ],
    loadList: async (force = false) => memo('ops-review-appeals', async () => {
      const res = await getOpsReviewAppeals()
      return asArray(res.data).map((item) => ({ ...item, id: item.id }))
    }, force),
    loadDetail: async (id) => {
      const rows = await catalogRegistry.appealReviews.loadList()
      return findById(rows, id)
    },
    buildSections: (detail) => autoSections(detail, [])
  },
  librarySlots: {
    listTitle: tri('游戏库运营位列表', '遊戲庫營運位列表', 'Library Slot List'),
    listDescription: tri('管理游戏库中的“新手必玩”“大家都在玩”等运营位。', '管理遊戲庫中的「新手必玩」「大家都在玩」等營運位。', 'Manage library collections such as Newbie Must Play and Everyone Playing.'),
    detailTitle: tri('游戏库运营位详情', '遊戲庫營運位詳情', 'Library Slot Detail'),
    idKey: 'id',
    detailRouteName: 'OpsLibrarySlotDetail',
    listColumns: [
      { prop: 'slotCode', label: tri('运营位编码', '營運位編碼', 'Slot Code') },
      { prop: 'slotName', label: tri('运营位名称', '營運位名稱', 'Slot Name'), formatter: (value) => Array.isArray(value) ? value[0] : value },
      { prop: 'sourceType', label: tri('来源策略', '來源策略', 'Source Strategy'), formatter: (value) => Array.isArray(value) ? value[0] : value },
      { prop: 'gameCount', label: tri('游戏数', '遊戲數', 'Game Count') }
    ],
    loadList: loadLibrarySlots,
    loadDetail: async (id) => {
      const [slots, workspace] = await Promise.all([loadLibrarySlots(), loadDiscoverWorkspace()])
      const slot = findById(slots, id)
      if (!slot) return null
      return {
        ...slot,
        relatedGames: asArray(workspace.gameOptions).filter((game) => asArray(slot.appIds).includes(game.appId))
      }
    },
    buildSections: (detail) => [
      {
        key: 'overview',
        title: tri('运营位概览', '營運位概覽', 'Slot Overview'),
        type: 'fields',
        fields: [
          { label: tri('运营位', '營運位', 'Slot'), value: Array.isArray(detail.slotName) ? detail.slotName[0] : detail.slotName },
          { label: tri('来源策略', '來源策略', 'Strategy'), value: Array.isArray(detail.sourceType) ? detail.sourceType[0] : detail.sourceType },
          { label: tri('游戏数量', '遊戲數量', 'Game Count'), value: detail.gameCount }
        ]
      },
      {
        key: 'games',
        title: tri('配置游戏', '配置遊戲', 'Configured Games'),
        type: 'table',
        columns: [
          { prop: 'name', label: tri('游戏', '遊戲', 'Game') },
          { prop: 'appId', label: tri('AppID', 'AppID', 'AppID') },
          { prop: 'category', label: tri('分类', '分類', 'Category') }
        ],
        rows: asArray(detail.relatedGames)
      }
    ]
  },
  discoverSlots: {
    listTitle: tri('发现运营位列表', '發現營運位列表', 'Discover Slot List'),
    listDescription: tri('管理发现页与游戏库顶部的客户端运营位总开关。', '管理發現頁與遊戲庫頂部的客戶端營運位總開關。', 'Manage client-facing discover and library slot switches.'),
    detailTitle: tri('发现运营位详情', '發現營運位詳情', 'Discover Slot Detail'),
    idKey: 'id',
    detailRouteName: 'OpsDiscoverSlotDetail',
    listColumns: [
      { prop: 'slotCode', label: tri('位编码', '位編碼', 'Slot Code') },
      { prop: 'slotName', label: tri('位名称', '位名稱', 'Slot Name') },
      { prop: 'pageCode', label: tri('页面', '頁面', 'Page') },
      { prop: 'positionCode', label: tri('位置', '位置', 'Position') },
      { prop: 'enabledLabel', label: tri('状态', '狀態', 'Status'), type: 'tag', formatter: (value) => Array.isArray(value) ? value[0] : value }
    ],
    loadList: loadDiscoverSlots,
    loadDetail: async (id) => {
      const rows = await loadDiscoverSlots()
      return findById(rows, id)
    },
    buildSections: (detail) => autoSections(detail, [])
  },
  discoverCategories: {
    listTitle: tri('发现分类列表', '發現分類列表', 'Discover Category List'),
    listDescription: tri('管理发现页分类、排序和引用关系。', '管理發現頁分類、排序和引用關係。', 'Manage discover categories, ordering, and references.'),
    detailTitle: tri('发现分类详情', '發現分類詳情', 'Discover Category Detail'),
    idKey: 'id',
    detailRouteName: 'OpsDiscoverCategoryDetail',
    listColumns: [
      { prop: 'name', label: tri('分类名称', '分類名稱', 'Category Name') },
      { prop: 'sortOrder', label: tri('排序', '排序', 'Sort') },
      { prop: 'displayStatus', label: tri('状态', '狀態', 'Status'), type: 'tag' },
      { prop: 'referenceCount', label: tri('引用数', '引用數', 'References') }
    ],
    loadList: loadDiscoverCategoriesData,
    loadDetail: async (id) => {
      const rows = await loadDiscoverCategoriesData()
      return findById(rows, id)
    },
    buildSections: (detail) => autoSections(detail, [])
  },
  discoverContent: {
    listTitle: tri('发现内容列表', '發現內容列表', 'Discover Content List'),
    listDescription: tri('查看发现 Hero 与游戏库顶部 Banner 的内容资产。', '查看發現 Hero 與遊戲庫頂部 Banner 的內容資產。', 'Inspect discover hero and library top-banner content assets.'),
    detailTitle: tri('发现内容详情', '發現內容詳情', 'Discover Content Detail'),
    idKey: 'id',
    detailRouteName: 'OpsDiscoverContentDetail',
    listColumns: [
      { prop: 'contentType', label: tri('内容类型', '內容類型', 'Content Type') },
      { prop: 'title', label: tri('标题', '標題', 'Title') },
      { prop: 'appId', label: tri('AppID', 'AppID', 'AppID') },
      { prop: 'displayStatus', label: tri('状态', '狀態', 'Status'), type: 'tag' },
      { prop: 'sortOrder', label: tri('排序', '排序', 'Sort') }
    ],
    loadList: loadDiscoverContent,
    loadDetail: async (id) => {
      const rows = await loadDiscoverContent()
      return findById(rows, id)
    },
    buildSections: (detail) => autoSections(detail, [])
  },
  recommendationItems: {
    listTitle: tri('推荐内容列表', '推薦內容列表', 'Recommendation Item List'),
    listDescription: tri('管理 discover/community 的推荐内容卡片。', '管理 discover/community 的推薦內容卡片。', 'Manage recommendation cards consumed by discover/community.'),
    detailTitle: tri('推荐内容详情', '推薦內容詳情', 'Recommendation Item Detail'),
    idKey: 'id',
    detailRouteName: 'OpsRecommendationItemDetail',
    listColumns: [
      { prop: 'cardCategory', label: tri('卡片分类', '卡片分類', 'Card Category') },
      { prop: 'cardTitle', label: tri('卡片标题', '卡片標題', 'Card Title') },
      { prop: 'appId', label: tri('AppID', 'AppID', 'AppID') },
      { prop: 'displayStatus', label: tri('状态', '狀態', 'Status'), type: 'tag' },
      { prop: 'sortOrder', label: tri('排序', '排序', 'Sort') }
    ],
    loadList: loadRecommendationItems,
    loadDetail: async (id) => {
      const rows = await loadRecommendationItems()
      return findById(rows, id)
    },
    buildSections: (detail) => autoSections(detail, [])
  },
  launchAds: {
    listTitle: tri('启动广告列表', '啟動廣告列表', 'Launch Ad List'),
    listDescription: tri('管理 Android 与 iOS 启动广告的图片、文案与排期。', '管理 Android 與 iOS 啟動廣告的圖片、文案與排期。', 'Manage launch ads, schedule, and multilingual copy for Android and iOS.'),
    detailTitle: tri('启动广告详情', '啟動廣告詳情', 'Launch Ad Detail'),
    idKey: 'id',
    detailRouteName: 'OpsLaunchAdDetail',
    listColumns: [
      { prop: 'title', label: tri('标题', '標題', 'Title') },
      { prop: 'status', label: tri('状态', '狀態', 'Status'), type: 'tag' },
      { prop: 'ctaText', label: tri('CTA', 'CTA', 'CTA') },
      { prop: 'displayDurationSeconds', label: tri('展示秒数', '展示秒數', 'Duration') },
      { prop: 'startAt', label: tri('开始时间', '開始時間', 'Start At'), formatter: formatDateTime }
    ],
    loadList: loadLaunchAds,
    loadDetail: async (id) => {
      const rows = await loadLaunchAds()
      return findById(rows, id)
    },
    buildSections: (detail) => autoSections(detail, [])
  },
  versionPolicies: {
    listTitle: tri('版本策略列表', '版本策略列表', 'Version Policy List'),
    listDescription: tri('集中查看版本更新、最低版本与强更规则。', '集中查看版本更新、最低版本與強更規則。', 'Inspect minimum version, update policy, and force-update baseline.'),
    detailTitle: tri('版本策略详情', '版本策略詳情', 'Version Policy Detail'),
    idKey: 'id',
    detailRouteName: 'OpsVersionPolicyDetail',
    listColumns: [
      { prop: 'policyName', label: tri('策略名称', '策略名稱', 'Policy') },
      { prop: 'minimumSupportedVersion', label: tri('最低版本', '最低版本', 'Min Version') },
      { prop: 'forceUpdateEnabled', label: tri('强更', '強更', 'Force Update'), formatter: (value) => (value ? 'ON' : 'OFF') },
      { prop: 'grayReleaseDescription', label: tri('灰度说明', '灰度說明', 'Gray Notes') }
    ],
    loadList: async () => {
      const config = await loadAndroidConfig()
      return [{
        id: 'global-version-policy',
        policyName: tri('全局版本策略', '全域版本策略', 'Global Version Policy'),
        ...config
      }]
    },
    loadDetail: async () => {
      const rows = await catalogRegistry.versionPolicies.loadList()
      return rows[0]
    },
    buildSections: (detail) => autoSections(detail, ['policyName'])
  },
  channels: {
    listTitle: tri('渠道配置列表', '渠道配置列表', 'Channel Config List'),
    listDescription: tri('按渠道管理最小版本、灰度与强更状态。', '按渠道管理最小版本、灰度與強更狀態。', 'Manage min version, gray rollout, and force update by channel.'),
    detailTitle: tri('渠道配置详情', '渠道配置詳情', 'Channel Config Detail'),
    idKey: 'id',
    detailRouteName: 'OpsChannelConfigDetail',
    listColumns: [
      { prop: 'channelCode', label: tri('渠道编码', '渠道編碼', 'Channel Code') },
      { prop: 'channelName', label: tri('渠道名称', '渠道名稱', 'Channel Name') },
      { prop: 'status', label: tri('状态', '狀態', 'Status'), type: 'tag' },
      { prop: 'minimumVersion', label: tri('最低版本', '最低版本', 'Min Version') },
      { prop: 'trafficPercentage', label: tri('灰度流量', '灰度流量', 'Gray Traffic') }
    ],
    loadList: loadChannels,
    loadDetail: async (id) => findById(await loadChannels(), id),
    buildSections: (detail) => autoSections(detail, [])
  },
  featureToggles: {
    listTitle: tri('功能开关列表', '功能開關列表', 'Feature Toggle List'),
    listDescription: tri('集中管理客户端功能开关。', '集中管理客戶端功能開關。', 'Manage client feature flags in a dedicated list.'),
    detailTitle: tri('功能开关详情', '功能開關詳情', 'Feature Toggle Detail'),
    idKey: 'id',
    detailRouteName: 'OpsFeatureToggleDetail',
    listColumns: [
      { prop: 'featureKey', label: tri('开关键', '開關鍵', 'Feature Key') },
      { prop: 'featureName', label: tri('名称', '名稱', 'Name') },
      { prop: 'status', label: tri('状态', '狀態', 'Status'), type: 'tag' },
      { prop: 'scope', label: tri('作用域', '作用域', 'Scope') },
      { prop: 'owner', label: tri('负责人', '負責人', 'Owner') }
    ],
    loadList: loadFeatureToggles,
    loadDetail: async (id) => findById(await loadFeatureToggles(), id),
    buildSections: (detail) => autoSections(detail, [])
  },
  circuitBreakers: {
    listTitle: tri('页面熔断列表', '頁面熔斷列表', 'Page Circuit Breaker List'),
    listDescription: tri('管理客户端页面熔断策略和降级方式。', '管理客戶端頁面熔斷策略和降級方式。', 'Manage page circuit breakers and degrade modes for client pages.'),
    detailTitle: tri('页面熔断详情', '頁面熔斷詳情', 'Page Circuit Breaker Detail'),
    idKey: 'id',
    detailRouteName: 'OpsCircuitBreakerDetail',
    listColumns: [
      { prop: 'pageKey', label: tri('页面键', '頁面鍵', 'Page Key') },
      { prop: 'pageName', label: tri('页面名称', '頁面名稱', 'Page Name') },
      { prop: 'status', label: tri('状态', '狀態', 'Status'), type: 'tag' },
      { prop: 'audienceScope', label: tri('受众范围', '受眾範圍', 'Audience') },
      { prop: 'degradeMode', label: tri('降级模式', '降級模式', 'Degrade Mode') }
    ],
    loadList: loadCircuitBreakers,
    loadDetail: async (id) => findById(await loadCircuitBreakers(), id),
    buildSections: (detail) => autoSections(detail, [])
  },
  compatibilityBlocks: {
    listTitle: tri('兼容黑名单列表', '兼容黑名單列表', 'Compatibility List'),
    listDescription: tri('查看设备、版本和 SDK 的兼容限制规则。', '查看設備、版本和 SDK 的相容限制規則。', 'Inspect device, app-version, and SDK compatibility rules.'),
    detailTitle: tri('兼容黑名单详情', '兼容黑名單詳情', 'Compatibility Detail'),
    idKey: 'id',
    detailRouteName: 'OpsCompatibilityRuleDetail',
    listColumns: [
      { prop: 'ruleCode', label: tri('规则编码', '規則編碼', 'Rule Code') },
      { prop: 'targetType', label: tri('目标类型', '目標類型', 'Target Type') },
      { prop: 'targetValue', label: tri('目标值', '目標值', 'Target Value') },
      { prop: 'status', label: tri('状态', '狀態', 'Status'), type: 'tag' },
      { prop: 'reason', label: tri('原因', '原因', 'Reason') }
    ],
    loadList: loadCompatibilityRules,
    loadDetail: async (id) => findById(await loadCompatibilityRules(), id),
    buildSections: (detail) => autoSections(detail, [])
  },
  grayRollouts: {
    listTitle: tri('灰度配置列表', '灰度配置列表', 'Gray Rollout List'),
    listDescription: tri('查看渠道、版本、人群和区域灰度策略。', '查看渠道、版本、人群和區域灰度策略。', 'Inspect gray rollout plans by channel, version, audience, and region.'),
    detailTitle: tri('灰度配置详情', '灰度配置詳情', 'Gray Rollout Detail'),
    idKey: 'id',
    detailRouteName: 'OpsGrayRolloutDetail',
    listColumns: [
      { prop: 'planCode', label: tri('计划编码', '計畫編碼', 'Plan Code') },
      { prop: 'planName', label: tri('计划名称', '計畫名稱', 'Plan Name') },
      { prop: 'status', label: tri('状态', '狀態', 'Status'), type: 'tag' },
      { prop: 'channelCode', label: tri('渠道', '渠道', 'Channel') },
      { prop: 'overallTrafficPercentage', label: tri('总流量', '總流量', 'Traffic') }
    ],
    loadList: loadGrayRollouts,
    loadDetail: async (id) => findById(await loadGrayRollouts(), id),
    buildSections: (detail) => autoSections(detail, [])
  },
  developers: {
    listTitle: tri('开发者列表', '開發者列表', 'Developer List'),
    listDescription: tri('从开发者主体视角查看账号状态、资质和名下游戏。', '從開發者主體視角查看帳號狀態、資質和名下遊戲。', 'Inspect developer status, certification, and owned games by account.'),
    detailTitle: tri('开发者详情', '開發者詳情', 'Developer Detail'),
    idKey: 'developerId',
    detailRouteName: 'OpsDeveloperDetail',
    listColumns: [
      { prop: 'username', label: tri('账号', '帳號', 'Account') },
      { prop: 'email', label: tri('邮箱', '電子郵件', 'Email') },
      { prop: 'accountStatus', label: tri('账号状态', '帳號狀態', 'Account Status'), type: 'tag' },
      { prop: 'certificationStatus', label: tri('资质状态', '資質狀態', 'Certification Status'), type: 'tag' },
      { prop: 'gameCount', label: tri('游戏数', '遊戲數', 'Games') }
    ],
    loadList: loadDeveloperAccounts,
    loadDetail: async (id) => {
      const [accounts, profileRes, reviewsRes, governanceRes] = await Promise.all([
        loadDeveloperAccounts(),
        getDeveloperCertificationProfile(id),
        getDeveloperCertificationReviews(id),
        getDeveloperGovernanceRecords(id)
      ])
      const base = findById(accounts, id)
      return {
        ...base,
        certificationProfile: profileRes.data || {},
        certificationReviews: asArray(reviewsRes.data),
        governanceRecords: asArray(governanceRes.data)
      }
    },
    buildSections: (detail) => [
      {
        key: 'account',
        title: tri('账号信息', '帳號資訊', 'Account'),
        type: 'fields',
        fields: createPrimitiveFields(detail, ['certificationProfile', 'certificationReviews', 'governanceRecords', 'games'])
      },
      {
        key: 'games',
        title: tri('名下游戏', '名下遊戲', 'Owned Games'),
        type: 'text-list',
        items: asArray(detail.games).map((item) => String(item))
      },
      {
        key: 'certificationProfile',
        title: tri('资质档案', '資質檔案', 'Certification Profile'),
        type: 'fields',
        fields: createPrimitiveFields(detail.certificationProfile || {})
      },
      {
        key: 'governanceRecords',
        title: tri('治理记录', '治理記錄', 'Governance Records'),
        type: 'table',
        columns: [
          { prop: 'actionType', label: tri('动作', '動作', 'Action') },
          { prop: 'reason', label: tri('原因', '原因', 'Reason') },
          { prop: 'createdAt', label: tri('时间', '時間', 'Time'), formatter: formatDateTime }
        ],
        rows: asArray(detail.governanceRecords)
      }
    ]
  },
  tickets: {
    listTitle: tri('工单列表', '工單列表', 'Ticket List'),
    listDescription: tri('处理开发者工单并进入详情查看消息与状态。', '處理開發者工單並進入詳情查看訊息與狀態。', 'Inspect developer tickets and open a message timeline in detail.'),
    detailTitle: tri('工单详情', '工單詳情', 'Ticket Detail'),
    idKey: 'id',
    detailRouteName: 'OpsTicketDetail',
    listColumns: [
      { prop: 'title', label: tri('标题', '標題', 'Title') },
      { prop: 'ticketType', label: tri('类型', '類型', 'Type') },
      { prop: 'status', label: tri('状态', '狀態', 'Status'), type: 'tag' },
      { prop: 'developerName', label: tri('开发者', '開發者', 'Developer') },
      { prop: 'updatedAt', label: tri('更新时间', '更新時間', 'Updated At'), formatter: formatDateTime }
    ],
    loadList: loadTickets,
    loadDetail: async (id) => {
      const rows = await loadTickets()
      const base = findById(rows, id)
      if (!base) return null
      const messagesRes = await getOpsTicketMessages(id)
      return { ...base, messages: asArray(messagesRes.data) }
    },
    buildSections: (detail) => [
      ...autoSections(detail, ['messages']),
      {
        key: 'messages',
        title: tri('消息记录', '訊息記錄', 'Messages'),
        type: 'table',
        columns: [
          { prop: 'senderRole', label: tri('发送方', '發送方', 'Sender') },
          { prop: 'messageBody', label: tri('内容', '內容', 'Message') },
          { prop: 'createdAt', label: tri('时间', '時間', 'Time'), formatter: formatDateTime }
        ],
        rows: asArray(detail.messages)
      }
    ]
  },
  notices: {
    listTitle: tri('通知列表', '通知列表', 'Notice List'),
    listDescription: tri('查看发往开发者后台的通知对象。', '查看發往開發者後台的通知物件。', 'Inspect notices delivered to the developer portal.'),
    detailTitle: tri('通知详情', '通知詳情', 'Notice Detail'),
    idKey: 'id',
    detailRouteName: 'OpsNoticeDetail',
    listColumns: [
      { prop: 'title', label: tri('标题', '標題', 'Title') },
      { prop: 'category', label: tri('分类', '分類', 'Category') },
      { prop: 'status', label: tri('状态', '狀態', 'Status'), type: 'tag' },
      { prop: 'audienceScope', label: tri('目标对象', '目標對象', 'Audience') },
      { prop: 'startAt', label: tri('生效时间', '生效時間', 'Starts At'), formatter: formatDateTime }
    ],
    loadList: loadNotices,
    loadDetail: async (id) => findById(await loadNotices(), id),
    buildSections: (detail) => autoSections(detail, [])
  },
  noticeTemplates: {
    listTitle: tri('通知模板列表', '通知模板列表', 'Notice Template List'),
    listDescription: tri('维护发给开发者后台的标准通知模板。', '維護發給開發者後台的標準通知範本。', 'Maintain reusable notice templates for developer communications.'),
    detailTitle: tri('通知模板详情', '通知模板詳情', 'Notice Template Detail'),
    idKey: 'id',
    detailRouteName: 'OpsNoticeTemplateDetail',
    listColumns: [
      { prop: 'templateCode', label: tri('模板编码', '模板編碼', 'Template Code') },
      { prop: 'templateName', label: tri('模板名称', '模板名稱', 'Template Name') },
      { prop: 'channel', label: tri('渠道', '渠道', 'Channel') },
      { prop: 'languageCode', label: tri('语言', '語言', 'Language') }
    ],
    loadList: loadNoticeTemplates,
    loadDetail: async (id) => findById(await loadNoticeTemplates(), id),
    buildSections: (detail) => autoSections(detail, [])
  },
  incidents: {
    listTitle: tri('风险事件列表', '風險事件列表', 'Risk Incident List'),
    listDescription: tri('查看风险事件、负责人和处置记录。', '查看風險事件、負責人和處置記錄。', 'Inspect incidents, owners, and handling records.'),
    detailTitle: tri('风险事件详情', '風險事件詳情', 'Risk Incident Detail'),
    idKey: 'id',
    detailRouteName: 'OpsRiskIncidentDetail',
    listColumns: [
      { prop: 'incidentType', label: tri('事件类型', '事件類型', 'Type') },
      { prop: 'title', label: tri('标题', '標題', 'Title') },
      { prop: 'status', label: tri('状态', '狀態', 'Status'), type: 'tag' },
      { prop: 'ownerUserId', label: tri('负责人', '負責人', 'Owner') },
      { prop: 'dueAt', label: tri('时限', '時限', 'Due At'), formatter: formatDateTime }
    ],
    loadList: loadRiskIncidents,
    loadDetail: async (id) => {
      const rows = await loadRiskIncidents()
      const base = findById(rows, id)
      if (!base) return null
      const recordsRes = await getRiskIncidentRecords(id)
      return { ...base, records: asArray(recordsRes.data) }
    },
    buildSections: (detail) => [
      ...autoSections(detail, ['records']),
      {
        key: 'records',
        title: tri('处置记录', '處置記錄', 'Handling Records'),
        type: 'table',
        columns: [
          { prop: 'actionType', label: tri('动作', '動作', 'Action') },
          { prop: 'beforeStatus', label: tri('之前状态', '之前狀態', 'Before') },
          { prop: 'afterStatus', label: tri('之后状态', '之後狀態', 'After') },
          { prop: 'note', label: tri('说明', '說明', 'Note') },
          { prop: 'createdAt', label: tri('时间', '時間', 'Time'), formatter: formatDateTime }
        ],
        rows: asArray(detail.records)
      }
    ]
  },
  loginAnomalies: {
    listTitle: tri('登录异常列表', '登入異常列表', 'Login Anomaly List'),
    listDescription: tri('查看登录异常事件和命中的风险规则。', '查看登入異常事件和命中的風險規則。', 'Inspect anomalous login events and matched risk rules.'),
    detailTitle: tri('登录异常详情', '登入異常詳情', 'Login Anomaly Detail'),
    idKey: 'id',
    detailRouteName: 'OpsLoginAnomalyDetail',
    listColumns: [
      { prop: 'eventType', label: tri('事件类型', '事件類型', 'Event Type') },
      { prop: 'username', label: tri('账号', '帳號', 'Account') },
      { prop: 'ipAddress', label: tri('IP', 'IP', 'IP') },
      { prop: 'deviceFingerprint', label: tri('设备指纹', '設備指紋', 'Device') },
      { prop: 'createdAt', label: tri('时间', '時間', 'Time'), formatter: formatDateTime }
    ],
    loadList: loadLoginAnomalies,
    loadDetail: async (id) => findById(await loadLoginAnomalies(), id),
    buildSections: (detail) => autoSections(detail, [])
  },
  accessRules: {
    listTitle: tri('IP/设备风控规则列表', 'IP/設備風控規則列表', 'Access Rule List'),
    listDescription: tri('查看 IP / 设备风控规则与拦截策略。', '查看 IP / 設備風控規則與攔截策略。', 'Inspect access-control rules for IPs and device fingerprints.'),
    detailTitle: tri('IP/设备风控规则详情', 'IP/設備風控規則詳情', 'Access Rule Detail'),
    idKey: 'id',
    detailRouteName: 'OpsAccessRuleDetail',
    listColumns: [
      { prop: 'ruleCode', label: tri('规则编码', '規則編碼', 'Rule Code') },
      { prop: 'ruleType', label: tri('规则类型', '規則類型', 'Rule Type') },
      { prop: 'status', label: tri('状态', '狀態', 'Status'), type: 'tag' },
      { prop: 'targetValue', label: tri('目标值', '目標值', 'Target') },
      { prop: 'note', label: tri('备注', '備註', 'Note') }
    ],
    loadList: loadAccessRules,
    loadDetail: async (id) => findById(await loadAccessRules(), id),
    buildSections: (detail) => autoSections(detail, [])
  },
  auditLogs: {
    listTitle: tri('审计日志列表', '審計日誌列表', 'Audit Log List'),
    listDescription: tri('查看敏感操作、发布和封控的审计留痕。', '查看敏感操作、發布和封控的審計留痕。', 'Inspect audit trails for sensitive actions, publish, and governance.'),
    detailTitle: tri('审计日志详情', '審計日誌詳情', 'Audit Log Detail'),
    idKey: 'id',
    detailRouteName: 'OpsAuditLogDetail',
    listColumns: [
      { prop: 'action', label: tri('动作', '動作', 'Action') },
      { prop: 'operatorName', label: tri('操作人', '操作人', 'Operator') },
      { prop: 'targetType', label: tri('对象类型', '對象類型', 'Target Type') },
      { prop: 'success', label: tri('结果', '結果', 'Result'), formatter: (value) => (value ? 'SUCCESS' : 'FAILED') },
      { prop: 'createdAt', label: tri('时间', '時間', 'Time'), formatter: formatDateTime }
    ],
    loadList: loadAuditLogs,
    loadDetail: async (id) => findById(await loadAuditLogs(), id),
    buildSections: (detail) => autoSections(detail, [])
  },
  smsLogs: {
    listTitle: tri('短信日志列表', '短信日誌列表', 'SMS Log List'),
    listDescription: tri('查看验证码发送与验证日志。', '查看驗證碼發送與驗證日誌。', 'Inspect verification-code delivery and verification logs.'),
    detailTitle: tri('短信日志详情', '短信日誌詳情', 'SMS Log Detail'),
    idKey: 'id',
    detailRouteName: 'OpsSmsLogDetail',
    listColumns: [
      { prop: 'phone', label: tri('手机号', '手機號', 'Phone') },
      { prop: 'scene', label: tri('场景', '場景', 'Scene') },
      { prop: 'status', label: tri('状态', '狀態', 'Status'), type: 'tag' },
      { prop: 'verified', label: tri('已验证', '已驗證', 'Verified'), formatter: (value) => (value ? 'YES' : 'NO') },
      { prop: 'createdAt', label: tri('时间', '時間', 'Time'), formatter: formatDateTime }
    ],
    loadList: loadSmsLogs,
    loadDetail: async (id) => findById(await loadSmsLogs(), id),
    buildSections: (detail) => autoSections(detail, [])
  },
  categoryDictionary: {
    listTitle: tri('分类字典列表', '分類字典列表', 'Category Dictionary'),
    listDescription: tri('查看和维护当前项目使用的游戏分类字典。', '查看和維護當前專案使用的遊戲分類字典。', 'Inspect the game category dictionary used by current clients.'),
    detailTitle: tri('分类字典详情', '分類字典詳情', 'Category Detail'),
    idKey: 'id',
    detailRouteName: 'OpsCategoryDictionaryDetail',
    listColumns: [
      { prop: 'name', label: tri('分类名称', '分類名稱', 'Category Name') },
      { prop: 'sortOrder', label: tri('排序', '排序', 'Sort') },
      { prop: 'displayStatus', label: tri('状态', '狀態', 'Status'), type: 'tag' }
    ],
    loadList: loadGameCategories,
    loadDetail: async (id) => findById(await loadGameCategories(), id),
    buildSections: (detail) => autoSections(detail, [])
  },
  reviewTemplates: {
    listTitle: tri('审核模板列表', '審核模板列表', 'Review Template List'),
    listDescription: tri('查看审核意见、驳回原因和检查清单模板。', '查看審核意見、駁回原因和檢查清單模板。', 'Inspect review templates, reject reasons, and checklists.'),
    detailTitle: tri('审核模板详情', '審核模板詳情', 'Review Template Detail'),
    idKey: 'id',
    detailRouteName: 'OpsReviewTemplateDetail',
    listColumns: [
      { prop: 'templateCode', label: tri('模板编码', '模板編碼', 'Template Code') },
      { prop: 'name', label: tri('模板名称', '模板名稱', 'Template Name') },
      { prop: 'status', label: tri('状态', '狀態', 'Status'), type: 'tag' },
      { prop: 'templateType', label: tri('模板类型', '模板類型', 'Template Type') }
    ],
    loadList: loadReviewTemplates,
    loadDetail: async (id) => findById(await loadReviewTemplates(), id),
    buildSections: (detail) => autoSections(detail, [])
  }
}

export const configRegistry = {
  legal: {
    title: tri('法务链接配置', '法務連結配置', 'Legal Config'),
    description: tri('移动端会读取统一法务链接，这里按当前运行时 API 地址展示真实访问地址。', '移動端會讀取統一法務連結，這裡按當前運行時 API 地址展示真實訪問地址。', 'Mobile clients read the public legal links. This page shows the real URLs resolved from the current runtime API base URL.'),
    load: async () => {
      const base = getRuntimeApiBaseUrl().replace(/\/api\/v1$/, '')
      return {
        termsUrl: `${base}/api/v1/public/legal/terms`,
        privacyUrl: `${base}/api/v1/public/legal/privacy`
      }
    },
    sections: (detail) => [{
      key: 'legal',
      title: tri('公开访问链接', '公開訪問連結', 'Public URLs'),
      type: 'fields',
      fields: [
        { label: tri('用户协议', '使用者協議', 'Terms URL'), value: detail.termsUrl },
        { label: tri('隐私协议', '隱私協議', 'Privacy URL'), value: detail.privacyUrl }
      ]
    }]
  },
  uploadRules: {
    title: tri('上传规则配置', '上傳規則配置', 'Upload Rules'),
    description: tri('当前上传规则由运行时配置提供，这里统一展示客户端和开发者后台会用到的限制。', '當前上傳規則由運行時配置提供，這裡統一展示客戶端和開發者後台會用到的限制。', 'Upload constraints are driven by runtime config. This page surfaces the active limits used by clients and the developer portal.'),
    load: async () => {
      const config = await loadAndroidConfig()
      return {
        maxZipSizeMb: config.maxZipSizeMb,
        minimumSupportedVersion: config.minimumSupportedVersion,
        startupRoutePolicy: config.startupRoutePolicy,
        allowCleartextTraffic: config.allowCleartextTraffic,
        forceUpdateEnabled: config.forceUpdateEnabled
      }
    },
    sections: (detail) => [{
      key: 'upload',
      title: tri('当前生效规则', '當前生效規則', 'Active Rules'),
      type: 'fields',
      fields: createPrimitiveFields(detail)
    }]
  }
}

export function getCatalogDefinition(catalogKey) {
  return catalogRegistry[catalogKey] || null
}

export function getConfigDefinition(configKey) {
  return configRegistry[configKey] || null
}

export function clearCatalogCache() {
  invalidate([...cache.keys()])
}
