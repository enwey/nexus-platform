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
