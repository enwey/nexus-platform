import request from './request'

export const login = (data) => {
  return request({
    url: '/user/login',
    method: 'post',
    data
  })
}

export const refreshSession = (refreshToken) => {
  return request({
    url: '/user/refresh',
    method: 'post',
    data: { refreshToken },
    skipAuthRefresh: true
  })
}

export const logoutSession = () => {
  return request({
    url: '/user/logout',
    method: 'post',
    skipAuthRefresh: true
  })
}

export const register = (data) => {
  return request({
    url: '/user/register',
    method: 'post',
    data
  })
}

export const sendCode = (data) => {
  return request({
    url: '/user/send-code',
    method: 'post',
    data
  })
}

export const getCurrentUser = () => {
  return request({
    url: '/user/me',
    method: 'get'
  })
}

export const getGameList = () => {
  return request({
    url: '/game/list',
    method: 'get'
  })
}

export const uploadGame = (formData) => {
  return request({
    url: '/game/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const getDeveloperGames = (developerId) => {
  return request({
    url: `/game/developer/${developerId}`,
    method: 'get'
  })
}

export const getDeveloperReleaseHealth = (developerId) => {
  return request({
    url: `/game/developer/${developerId}/release-health`,
    method: 'get'
  })
}

export const getDeveloperUploadTasks = (developerId) => {
  return request({
    url: `/game/developer/${developerId}/upload-tasks`,
    method: 'get'
  })
}

export const retryDeveloperUploadTask = (developerId, gameId, data) => {
  return request({
    url: `/game/developer/${developerId}/upload-tasks/${gameId}/retry`,
    method: 'post',
    data
  })
}

export const getDeveloperOperationsDashboard = (developerId) => {
  return request({
    url: `/game/developer/${developerId}/operations-dashboard`,
    method: 'get'
  })
}

export const getGameVersions = (gameId) => {
  return request({
    url: `/game/${gameId}/versions`,
    method: 'get'
  })
}

export const getDeveloperVersionPreflight = (developerId, gameId, versionId, forceUpdate = false) => {
  return request({
    url: `/game/developer/${developerId}/games/${gameId}/versions/${versionId}/preflight`,
    method: 'get',
    params: { forceUpdate }
  })
}

export const getDeveloperGameOpsProfile = (developerId, gameId) => {
  return request({
    url: `/game/developer/${developerId}/games/${gameId}/ops-profile`,
    method: 'get'
  })
}

export const updateDeveloperGameOpsProfile = (developerId, gameId, data) => {
  return request({
    url: `/game/developer/${developerId}/games/${gameId}/ops-profile`,
    method: 'put',
    data
  })
}

export const getDeveloperGameAssets = (developerId, gameId, assetGroup) => {
  return request({
    url: `/game/developer/${developerId}/games/${gameId}/assets`,
    method: 'get',
    params: assetGroup ? { assetGroup } : undefined
  })
}

export const upsertDeveloperGameAsset = (developerId, gameId, data) => {
  return request({
    url: `/game/developer/${developerId}/games/${gameId}/assets`,
    method: 'post',
    data
  })
}

export const deleteDeveloperGameAsset = (developerId, gameId, assetId, confirmText) => {
  return request({
    url: `/game/developer/${developerId}/games/${gameId}/assets/${assetId}/delete`,
    method: 'post',
    data: { confirmText }
  })
}

export const getMyReviewAppeals = () => {
  return request({
    url: '/game/review-appeals',
    method: 'get'
  })
}

export const createReviewAppeal = (gameId, versionId, appealReason) => {
  return request({
    url: `/game/${gameId}/versions/${versionId}/review-appeals`,
    method: 'post',
    data: { appealReason }
  })
}

export const submitGameForAudit = (id, note = '', forceUpdate = false) => {
  return request({
    url: `/game/submit/${id}`,
    method: 'post',
    data: { note, forceUpdate }
  })
}

export const submitGameVersion = (gameId, versionId, note = '', forceUpdate = false) => {
  return request({
    url: `/game/${gameId}/submit-version/${versionId}`,
    method: 'post',
    data: { note, forceUpdate }
  })
}

export const rollbackVersion = (gameId, versionId, reason = '') => {
  return request({
    url: `/game/${gameId}/rollback/${versionId}`,
    method: 'post',
    data: { reason }
  })
}

export const getGameCategories = () => {
  return request({
    url: '/game/categories',
    method: 'get'
  })
}

export const updateGameMetadata = (gameId, data) => {
  return request({
    url: `/game/${gameId}/metadata`,
    method: 'put',
    data
  })
}

export const approveGame = (id) => {
  return request({
    url: `/game/approve/${id}`,
    method: 'post'
  })
}

export const rejectGame = (id) => {
  return request({
    url: `/game/reject/${id}`,
    method: 'post'
  })
}

export const downloadGame = (appId) => {
  return request({
    url: `/game/download-url/${appId}`,
    method: 'get',
    skipAuthRefresh: true
  })
}

export const getProfile = () => {
  return request({
    url: '/user/profile',
    method: 'get'
  })
}

export const getDeveloperCertification = () => {
  return request({
    url: '/user/developer-certification',
    method: 'get'
  })
}

export const saveDeveloperCertification = (data) => {
  return request({
    url: '/user/developer-certification',
    method: 'post',
    data
  })
}

export const getDeveloperCertificationReviews = () => {
  return request({
    url: '/user/developer-certification/reviews',
    method: 'get'
  })
}

export const updateProfile = (data) => {
  return request({
    url: '/user/profile',
    method: 'post',
    data
  })
}

export const getDevices = () => {
  return request({
    url: '/user/devices',
    method: 'get'
  })
}

export const kickDevice = (deviceId) => {
  return request({
    url: `/user/devices/${deviceId}/kick`,
    method: 'post'
  })
}

export const logoutAll = () => {
  return request({
    url: '/user/logout-all',
    method: 'post'
  })
}

export const changePassword = (data) => {
  return request({
    url: '/user/password/change',
    method: 'post',
    data
  })
}

export const terminateAccount = (confirmText) => {
  return request({
    url: '/user/terminate',
    method: 'post',
    data: { confirmText }
  })
}

export const getTeamMembers = () => {
  return request({
    url: '/user/team-members',
    method: 'get'
  })
}

export const createTeamMember = (data) => {
  return request({
    url: '/user/team-members',
    method: 'post',
    data
  })
}

export const updateTeamMember = (memberId, data) => {
  return request({
    url: `/user/team-members/${memberId}`,
    method: 'post',
    data
  })
}

export const updateTeamMemberStatus = (memberId, data) => {
  return request({
    url: `/user/team-members/${memberId}/status`,
    method: 'post',
    data
  })
}

export const getApiKeys = () => {
  return request({
    url: '/user/api-keys',
    method: 'get'
  })
}

export const createApiKey = (data) => {
  return request({
    url: '/user/api-keys',
    method: 'post',
    data
  })
}

export const revokeApiKey = (keyId, data) => {
  return request({
    url: `/user/api-keys/${keyId}/revoke`,
    method: 'post',
    data
  })
}

export const rotateApiKey = (keyId, data) => {
  return request({
    url: `/user/api-keys/${keyId}/rotate`,
    method: 'post',
    data
  })
}

export const getWorkspaceAuditLogs = () => {
  return request({
    url: '/user/workspace-audit-logs',
    method: 'get'
  })
}

export const getMyNotices = () => {
  return request({
    url: '/user/notices',
    method: 'get'
  })
}

export const getMyTickets = () => {
  return request({
    url: '/user/tickets',
    method: 'get'
  })
}

export const createMyTicket = (data) => {
  return request({
    url: '/user/tickets',
    method: 'post',
    data
  })
}

export const getMyTicketMessages = (ticketId) => {
  return request({
    url: `/user/tickets/${ticketId}/messages`,
    method: 'get'
  })
}

export const createMyTicketMessage = (ticketId, data) => {
  return request({
    url: `/user/tickets/${ticketId}/messages`,
    method: 'post',
    data
  })
}

export const getDeveloperDocs = (docType) => {
  return request({
    url: '/user/docs',
    method: 'get',
    params: docType ? { docType } : undefined
  })
}

export const getDeveloperDocVersions = (articleId) => {
  return request({
    url: `/user/docs/${articleId}/versions`,
    method: 'get'
  })
}

export const createDeveloperDoc = (data) => {
  return request({
    url: '/user/docs',
    method: 'post',
    data
  })
}

export const updateDeveloperDoc = (articleId, data) => {
  return request({
    url: `/user/docs/${articleId}`,
    method: 'post',
    data
  })
}
