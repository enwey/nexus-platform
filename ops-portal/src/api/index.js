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

export const getGameList = () =>
  request({
    url: '/game/list',
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

export const getAuditLogs = (limit = 50) =>
  request({
    url: '/audit/logs',
    method: 'get',
    params: { limit }
  })

export const getVerificationCodeLogs = (params = {}) =>
  request({
    url: '/admin/ops/verification-codes',
    method: 'get',
    params
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
