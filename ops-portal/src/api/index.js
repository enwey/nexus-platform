import request from './request'

export const login = (data) =>
  request({
    url: '/user/login',
    method: 'post',
    data
  })

export const refreshSession = (refreshToken) =>
  request({
    url: '/user/refresh',
    method: 'post',
    data: { refreshToken },
    skipAuthRefresh: true
  })

export const logoutSession = () =>
  request({
    url: '/user/logout',
    method: 'post',
    skipAuthRefresh: true
  })

export const getCurrentUser = () =>
  request({
    url: '/user/me',
    method: 'get'
  })

export const getPublicLegalConfig = () =>
  request({
    url: '/public/legal/config',
    method: 'get'
  })

export const getProfile = () =>
  request({
    url: '/user/profile',
    method: 'get'
  })

export const updateProfile = (data) =>
  request({
    url: '/user/profile',
    method: 'post',
    data
  })

export const getGameList = () =>
  request({
    url: '/game/list',
    method: 'get'
  })

export const uploadGamePackage = (formData) =>
  request({
    url: '/game/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })

export const getOpsGames = (params = {}) =>
  request({
    url: '/admin/ops/games',
    method: 'get',
    params
  })

export const getOpsGameProfile = (gameId) =>
  request({
    url: `/admin/ops/game-profile/${gameId}`,
    method: 'get'
  })

export const updateOpsGameProfile = (gameId, data) =>
  request({
    url: `/admin/ops/game-profile/${gameId}`,
    method: 'put',
    data
  })

export const getOpsReviews = (params = {}) =>
  request({
    url: '/admin/ops/reviews',
    method: 'get',
    params
  })

export const getOpsReviewOverview = () =>
  request({
    url: '/admin/ops/reviews/overview',
    method: 'get'
  })

export const getOpsReviewers = () =>
  request({
    url: '/admin/ops/reviewers',
    method: 'get'
  })

export const getOpsReviewAppeals = () =>
  request({
    url: '/admin/ops/review-appeals',
    method: 'get'
  })

export const decideOpsReviewAppeal = (appealId, data) =>
  request({
    url: `/admin/ops/review-appeals/${appealId}/decision`,
    method: 'post',
    data
  })

export const assignOpsReview = (versionId, data) =>
  request({
    url: `/admin/ops/reviews/${versionId}/assign`,
    method: 'post',
    data
  })

export const batchOpsReview = (data) =>
  request({
    url: '/admin/ops/reviews/batch',
    method: 'post',
    data
  })

export const getGameVersions = (gameId) =>
  request({
    url: `/game/${gameId}/versions`,
    method: 'get'
  })

export const getGameCategories = () =>
  request({
    url: '/game/categories',
    method: 'get'
  })

export const getOpsGameCategories = () =>
  request({
    url: '/admin/ops/game-categories',
    method: 'get'
  })

export const createOpsGameCategory = (data) =>
  request({
    url: '/admin/ops/game-categories',
    method: 'post',
    data
  })

export const updateOpsGameCategory = (id, data) =>
  request({
    url: `/admin/ops/game-categories/${id}`,
    method: 'put',
    data
  })

export const deleteOpsGameCategory = (id) =>
  request({
    url: `/admin/ops/game-categories/${id}`,
    method: 'delete'
  })

export const updateGameMetadata = (gameId, data) =>
  request({
    url: `/game/${gameId}/metadata`,
    method: 'put',
    data
  })

export const updateOpsGameMetadata = (gameId, data) =>
  request({
    url: `/admin/ops/games/${gameId}/metadata`,
    method: 'put',
    data
  })

export const updateOpsGameVisibility = (gameId, data) =>
  request({
    url: `/admin/ops/games/${gameId}/visibility`,
    method: 'put',
    data
  })

export const updateOpsGameVisibilityBatch = (data) =>
  request({
    url: '/admin/ops/games/visibility/batch',
    method: 'put',
    data
  })

export const updateOpsGameGovernanceScope = (gameId, data) =>
  request({
    url: `/admin/ops/games/${gameId}/governance-scope`,
    method: 'put',
    data
  })

export const getOpsGameGovernanceImpact = (gameId) =>
  request({
    url: `/admin/ops/games/${gameId}/governance-impact`,
    method: 'get'
  })

export const approveGame = (id, reason) =>
  request({
    url: `/game/approve/${id}`,
    method: 'post',
    data: { reason }
  })

export const rejectGame = (id, reason) =>
  request({
    url: `/game/reject/${id}`,
    method: 'post',
    data: { reason }
  })

export const submitGameForAudit = (id, note = '') =>
  request({
    url: `/game/submit/${id}`,
    method: 'post',
    data: { note }
  })

export const submitGameVersionForAudit = (gameId, versionId, note = '', forceUpdate) =>
  request({
    url: `/game/${gameId}/submit-version/${versionId}`,
    method: 'post',
    data: {
      note,
      ...(typeof forceUpdate === 'boolean' ? { forceUpdate } : {})
    }
  })

export const getAuditLogs = (params = {}) =>
  request({
    url: '/audit/logs',
    method: 'get',
    params
  })

export const getRiskIncidents = () =>
  request({
    url: '/admin/ops/risk-incidents',
    method: 'get'
  })

export const getOpsLoginRiskEvents = () =>
  request({
    url: '/admin/ops/login-risk/events',
    method: 'get'
  })

export const getOpsAccessControlRules = () =>
  request({
    url: '/admin/ops/login-risk/rules',
    method: 'get'
  })

export const upsertOpsAccessControlRule = (data) =>
  request({
    url: '/admin/ops/login-risk/rules',
    method: 'post',
    data
  })

export const getOpsRiskRules = () =>
  request({
    url: '/admin/ops/login-risk/risk-rules',
    method: 'get'
  })

export const upsertOpsRiskRule = (data) =>
  request({
    url: '/admin/ops/login-risk/risk-rules',
    method: 'post',
    data
  })

export const createRiskIncident = (data) =>
  request({
    url: '/admin/ops/risk-incidents',
    method: 'post',
    data
  })

export const updateRiskIncidentStatus = (id, data) =>
  request({
    url: `/admin/ops/risk-incidents/${id}/status`,
    method: 'put',
    data
  })

export const getRiskIncidentRecords = (id) =>
  request({
    url: `/admin/ops/risk-incidents/${id}/records`,
    method: 'get'
  })

export const getOpsTickets = () =>
  request({
    url: '/admin/ops/tickets',
    method: 'get'
  })

export const getOpsTicketMessages = (ticketId) =>
  request({
    url: `/admin/ops/tickets/${ticketId}/messages`,
    method: 'get'
  })

export const updateOpsTicketStatus = (ticketId, data) =>
  request({
    url: `/admin/ops/tickets/${ticketId}/status`,
    method: 'put',
    data
  })

export const createOpsTicketMessage = (ticketId, data) =>
  request({
    url: `/admin/ops/tickets/${ticketId}/messages`,
    method: 'post',
    data
  })

export const getVerificationCodeLogs = (params = {}) =>
  request({
    url: '/admin/ops/verification-codes',
    method: 'get',
    params
  })

export const getDeveloperAccounts = () =>
  request({
    url: '/admin/ops/accounts/developers',
    method: 'get'
  })

export const updateDeveloperGovernance = (developerId, data) =>
  request({
    url: `/admin/ops/accounts/developers/${developerId}/governance`,
    method: 'put',
    data
  })

export const getDeveloperGovernanceRecords = (developerId) =>
  request({
    url: `/admin/ops/accounts/developers/${developerId}/governance-records`,
    method: 'get'
  })

export const getDeveloperCertificationProfile = (developerId) =>
  request({
    url: `/admin/ops/accounts/developers/${developerId}/certification-profile`,
    method: 'get'
  })

export const getDeveloperCertificationReviews = (developerId) =>
  request({
    url: `/admin/ops/accounts/developers/${developerId}/certification-reviews`,
    method: 'get'
  })

export const reviewDeveloperCertification = (developerId, data) =>
  request({
    url: `/admin/ops/accounts/developers/${developerId}/certification-review`,
    method: 'put',
    data
  })

export const getAndroidConsole = () =>
  request({
    url: '/admin/android/console',
    method: 'get'
  })

export const getAndroidConfig = () =>
  request({
    url: '/admin/android/config',
    method: 'get'
  })

export const updateAndroidConfig = (data) =>
  request({
    url: '/admin/android/config',
    method: 'put',
    data
  })

export const getAndroidBridgeApis = () =>
  request({
    url: '/admin/android/bridge/apis',
    method: 'get'
  })

export const getAndroidGameAssets = () =>
  request({
    url: '/admin/android/games',
    method: 'get'
  })

export const getAndroidChannels = () =>
  request({
    url: '/admin/android/channels',
    method: 'get'
  })

export const upsertAndroidChannel = (data) =>
  request({
    url: '/admin/android/channels',
    method: 'post',
    data
  })

export const getAndroidFeatureToggles = () =>
  request({
    url: '/admin/android/feature-toggles',
    method: 'get'
  })

export const upsertAndroidFeatureToggle = (data) =>
  request({
    url: '/admin/android/feature-toggles',
    method: 'post',
    data
  })

export const getAndroidCompatibilityBlacklists = () =>
  request({
    url: '/admin/android/compatibility-blacklists',
    method: 'get'
  })

export const upsertAndroidCompatibilityBlacklist = (data) =>
  request({
    url: '/admin/android/compatibility-blacklists',
    method: 'post',
    data
  })

export const getAndroidPageCircuitBreakers = () =>
  request({
    url: '/admin/android/page-circuit-breakers',
    method: 'get'
  })

export const upsertAndroidPageCircuitBreaker = (data) =>
  request({
    url: '/admin/android/page-circuit-breakers',
    method: 'post',
    data
  })

export const getAndroidAbExperiments = () =>
  request({
    url: '/admin/android/ab-experiments',
    method: 'get'
  })

export const upsertAndroidAbExperiment = (data) =>
  request({
    url: '/admin/android/ab-experiments',
    method: 'post',
    data
  })

export const getAndroidGrayReleasePlans = () =>
  request({
    url: '/admin/android/gray-release-plans',
    method: 'get'
  })

export const upsertAndroidGrayReleasePlan = (data) =>
  request({
    url: '/admin/android/gray-release-plans',
    method: 'post',
    data
  })

export const getOpsPublishOverview = () =>
  request({
    url: '/admin/ops/publish/overview',
    method: 'get'
  })

export const getOpsPermissionMatrix = () =>
  request({
    url: '/admin/ops/settings/permission-matrix',
    method: 'get'
  })

export const getOpsAdminDataScopes = () =>
  request({
    url: '/admin/ops/settings/admin-data-scopes',
    method: 'get'
  })

export const getOpsNoticeTemplates = () =>
  request({
    url: '/admin/ops/settings/notice-templates',
    method: 'get'
  })

export const upsertOpsNoticeTemplate = (id, data) =>
  request({
    url: id ? `/admin/ops/settings/notice-templates/${id}` : '/admin/ops/settings/notice-templates',
    method: 'put',
    data
  })

export const getOpsDictionaryEntries = () =>
  request({
    url: '/admin/ops/settings/dictionary-entries',
    method: 'get'
  })

export const upsertOpsDictionaryEntry = (id, data) =>
  request({
    url: id ? `/admin/ops/settings/dictionary-entries/${id}` : '/admin/ops/settings/dictionary-entries',
    method: 'put',
    data
  })

export const getOpsMenuPermissions = () =>
  request({
    url: '/admin/ops/settings/menu-permissions',
    method: 'get'
  })

export const upsertOpsMenuPermission = (id, data) =>
  request({
    url: id ? `/admin/ops/settings/menu-permissions/${id}` : '/admin/ops/settings/menu-permissions',
    method: 'put',
    data
  })

export const getOpsApprovalTemplates = () =>
  request({
    url: '/admin/ops/settings/approval-templates',
    method: 'get'
  })

export const upsertOpsApprovalTemplate = (id, data) =>
  request({
    url: id ? `/admin/ops/settings/approval-templates/${id}` : '/admin/ops/settings/approval-templates',
    method: 'put',
    data
  })

export const getOpsSensitivePolicies = () =>
  request({
    url: '/admin/ops/settings/sensitive-policies',
    method: 'get'
  })

export const upsertOpsSensitivePolicy = (id, data) =>
  request({
    url: id ? `/admin/ops/settings/sensitive-policies/${id}` : '/admin/ops/settings/sensitive-policies',
    method: 'put',
    data
  })

export const updateOpsRolePermissions = (roleCode, data) =>
  request({
    url: `/admin/ops/settings/permission-matrix/${roleCode}`,
    method: 'put',
    data
  })

export const updateOpsAdminDataScope = (adminUserId, data) =>
  request({
    url: `/admin/ops/settings/admin-data-scopes/${adminUserId}`,
    method: 'put',
    data
  })

export const getOpsRuleTemplates = () =>
  request({
    url: '/admin/ops/settings/rule-templates',
    method: 'get'
  })

export const createOpsRuleTemplate = (data) =>
  request({
    url: '/admin/ops/settings/rule-templates',
    method: 'post',
    data
  })

export const updateOpsRuleTemplate = (id, data) =>
  request({
    url: `/admin/ops/settings/rule-templates/${id}`,
    method: 'put',
    data
  })

export const deleteOpsRuleTemplate = (id) =>
  request({
    url: `/admin/ops/settings/rule-templates/${id}`,
    method: 'delete'
  })

export const getLaunchAds = () =>
  request({
    url: '/admin/launch-ads',
    method: 'get'
  })

export const createLaunchAd = (data) =>
  request({
    url: '/admin/launch-ads',
    method: 'post',
    data
  })

export const updateLaunchAd = (id, data) =>
  request({
    url: `/admin/launch-ads/${id}`,
    method: 'put',
    data
  })

export const activateLaunchAd = (id) =>
  request({
    url: `/admin/launch-ads/${id}/activate`,
    method: 'post'
  })

export const deactivateLaunchAd = (id) =>
  request({
    url: `/admin/launch-ads/${id}/deactivate`,
    method: 'post'
  })

export const getOpsMarketingAnalytics = () =>
  request({
    url: '/admin/ops/marketing/analytics',
    method: 'get'
  })

export const batchUpdateOpsMarketingStatus = (data) =>
  request({
    url: '/admin/ops/marketing/batch-status',
    method: 'post',
    data
  })

export const getOpsCampaigns = () =>
  request({
    url: '/admin/ops/campaigns',
    method: 'get'
  })

export const createOpsCampaign = (data) =>
  request({
    url: '/admin/ops/campaigns',
    method: 'post',
    data
  })

export const updateOpsCampaign = (id, data) =>
  request({
    url: `/admin/ops/campaigns/${id}`,
    method: 'put',
    data
  })

export const updateOpsCampaignStatus = (id, data) =>
  request({
    url: `/admin/ops/campaigns/${id}/status`,
    method: 'post',
    data
  })

export const getOpsPopupCampaigns = () =>
  request({
    url: '/admin/ops/popup-campaigns',
    method: 'get'
  })

export const createOpsPopupCampaign = (data) =>
  request({
    url: '/admin/ops/popup-campaigns',
    method: 'post',
    data
  })

export const updateOpsPopupCampaign = (id, data) =>
  request({
    url: `/admin/ops/popup-campaigns/${id}`,
    method: 'put',
    data
  })

export const updateOpsPopupCampaignStatus = (id, data) =>
  request({
    url: `/admin/ops/popup-campaigns/${id}/status`,
    method: 'post',
    data
  })

export const getOpsTaskCampaigns = () =>
  request({
    url: '/admin/ops/task-campaigns',
    method: 'get'
  })

export const createOpsTaskCampaign = (data) =>
  request({
    url: '/admin/ops/task-campaigns',
    method: 'post',
    data
  })

export const updateOpsTaskCampaign = (id, data) =>
  request({
    url: `/admin/ops/task-campaigns/${id}`,
    method: 'put',
    data
  })

export const updateOpsTaskCampaignStatus = (id, data) =>
  request({
    url: `/admin/ops/task-campaigns/${id}/status`,
    method: 'post',
    data
  })

export const getOpsGiftCodeCampaigns = () =>
  request({
    url: '/admin/ops/gift-code-campaigns',
    method: 'get'
  })

export const getOpsGiftCodeEntries = (id) =>
  request({
    url: `/admin/ops/gift-code-campaigns/${id}/codes`,
    method: 'get'
  })

export const createOpsGiftCodeCampaign = (data) =>
  request({
    url: '/admin/ops/gift-code-campaigns',
    method: 'post',
    data
  })

export const updateOpsGiftCodeCampaign = (id, data) =>
  request({
    url: `/admin/ops/gift-code-campaigns/${id}`,
    method: 'put',
    data
  })

export const updateOpsGiftCodeCampaignStatus = (id, data) =>
  request({
    url: `/admin/ops/gift-code-campaigns/${id}/status`,
    method: 'put',
    data
  })

export const getDiscoverOpsConfig = () =>
  request({
    url: '/admin/ops/discover/config',
    method: 'get'
  })

export const updateDiscoverOpsConfig = (data) =>
  request({
    url: '/admin/ops/discover/config',
    method: 'put',
    data
  })

export const getDiscoverOpsAnalytics = () =>
  request({
    url: '/admin/ops/discover/analytics',
    method: 'get'
  })

export const batchUpdateDiscoverContentItemsStatus = (data) =>
  request({
    url: '/admin/ops/discover/content-items/batch-status',
    method: 'post',
    data
  })

export const updateDiscoverSlotControl = (slotCode, data) =>
  request({
    url: `/admin/ops/discover/slots/${slotCode}`,
    method: 'put',
    data
  })

export const batchOperateDiscover = (data) =>
  request({
    url: '/admin/ops/discover/batch-operation',
    method: 'post',
    data
  })

export const getDiscoverPublishOrders = () =>
  request({
    url: '/admin/ops/discover/publish-orders',
    method: 'get'
  })

export const createDiscoverPublishOrder = (data) =>
  request({
    url: '/admin/ops/discover/publish-orders',
    method: 'post',
    data
  })

export const updateDiscoverPublishOrderStatus = (orderId, data) =>
  request({
    url: `/admin/ops/discover/publish-orders/${orderId}/status`,
    method: 'put',
    data
  })

export const getDiscoverPublishPreview = (scopeCode) =>
  request({
    url: `/admin/ops/discover/publish-preview/${scopeCode}`,
    method: 'get'
  })

export const getDiscoverExperiments = () =>
  request({
    url: '/admin/ops/discover/experiments',
    method: 'get'
  })

export const createDiscoverExperiment = (data) =>
  request({
    url: '/admin/ops/discover/experiments',
    method: 'post',
    data
  })

export const updateDiscoverExperimentStatus = (experimentId, data) =>
  request({
    url: `/admin/ops/discover/experiments/${experimentId}/status`,
    method: 'put',
    data
  })

export const getDiscoverCategories = () =>
  request({
    url: '/admin/ops/discover/categories',
    method: 'get'
  })

export const createDiscoverCategory = (data) =>
  request({
    url: '/admin/ops/discover/categories',
    method: 'post',
    data
  })

export const updateDiscoverCategory = (id, data) =>
  request({
    url: `/admin/ops/discover/categories/${id}`,
    method: 'put',
    data
  })

export const deleteDiscoverCategory = (id) =>
  request({
    url: `/admin/ops/discover/categories/${id}`,
    method: 'delete'
  })

export const getOpsNotices = () =>
  request({
    url: '/admin/ops/notices',
    method: 'get'
  })

export const createOpsNotice = (data) =>
  request({
    url: '/admin/ops/notices',
    method: 'post',
    data
  })

export const updateOpsNotice = (noticeId, data) =>
  request({
    url: `/admin/ops/notices/${noticeId}`,
    method: 'post',
    data
  })

export const updateOpsNoticeStatus = (noticeId, data) =>
  request({
    url: `/admin/ops/notices/${noticeId}/status`,
    method: 'post',
    data
  })

export const getOpsPublishOrders = () =>
  request({
    url: '/admin/ops/publish/orders',
    method: 'get'
  })

export const createOpsPublishOrder = (data) =>
  request({
    url: '/admin/ops/publish/orders',
    method: 'post',
    data
  })

export const getOpsPublishOrderPreview = (orderId) =>
  request({
    url: `/admin/ops/publish/orders/${orderId}/preview`,
    method: 'get'
  })

export const updateOpsPublishOrderStatus = (orderId, data) =>
  request({
    url: `/admin/ops/publish/orders/${orderId}/status`,
    method: 'post',
    data
  })

export const executeDueOpsPublishOrders = () =>
  request({
    url: '/admin/ops/publish/orders/execute-due',
    method: 'post'
  })
