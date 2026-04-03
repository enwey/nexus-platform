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

export const getAuditLogs = (limit = 50) =>
  request({
    url: '/audit/logs',
    method: 'get',
    params: { limit }
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
