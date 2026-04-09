import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const STORAGE_USER_KEY = 'ops_user'
const STORAGE_TOKEN_KEY = 'ops_token'
const STORAGE_REFRESH_TOKEN_KEY = 'ops_refresh_token'

function safeGet(key) {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

function safeSet(key, value) {
  try {
    localStorage.setItem(key, value)
  } catch {
    // Ignore storage write failures in private mode.
  }
}

function safeRemove(key) {
  try {
    localStorage.removeItem(key)
  } catch {
    // Ignore storage removal failures in private mode.
  }
}

function readStoredUser() {
  const raw = safeGet(STORAGE_USER_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw)
  } catch {
    safeRemove(STORAGE_USER_KEY)
    return null
  }
}

export const useUserStore = defineStore('ops-user', () => {
  const user = ref(readStoredUser())
  const token = ref(safeGet(STORAGE_TOKEN_KEY) || '')
  const refreshToken = ref(safeGet(STORAGE_REFRESH_TOKEN_KEY) || '')
  const isLoggedIn = computed(() => Boolean(user.value || token.value))
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  function setUser(userData) {
    user.value = userData
    if (userData) {
      safeSet(STORAGE_USER_KEY, JSON.stringify(userData))
    } else {
      safeRemove(STORAGE_USER_KEY)
    }
  }

  function setToken(tokenValue) {
    token.value = tokenValue || ''
    if (token.value) {
      safeSet(STORAGE_TOKEN_KEY, token.value)
    } else {
      safeRemove(STORAGE_TOKEN_KEY)
    }
  }

  function setRefreshToken(refreshTokenValue) {
    refreshToken.value = refreshTokenValue || ''
    if (refreshToken.value) {
      safeSet(STORAGE_REFRESH_TOKEN_KEY, refreshToken.value)
    } else {
      safeRemove(STORAGE_REFRESH_TOKEN_KEY)
    }
  }

  function setSession(userData, tokenValue = '', refreshTokenValue = '') {
    setUser(userData)
    setToken(tokenValue)
    setRefreshToken(refreshTokenValue)
  }

  function logout() {
    setUser(null)
    setToken('')
    setRefreshToken('')
  }

  return {
    user,
    token,
    refreshToken,
    isLoggedIn,
    isAdmin,
    setUser,
    setToken,
    setRefreshToken,
    setSession,
    logout
  }
})
