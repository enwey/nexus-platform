import request from './request'

export const login = (data) => {
  return request({
    url: '/user/login',
    method: 'post',
    data
  })
}

export const register = (data) => {
  return request({
    url: '/user/register',
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
    url: `/game/download/${appId}`,
    method: 'get',
    responseType: 'blob'
  })
}
