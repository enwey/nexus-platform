import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const STORAGE_USER_KEY = 'portal_user'
const STORAGE_TOKEN_KEY = 'token'

function readStoredUser() {
  const raw = localStorage.getItem(STORAGE_USER_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw)
  } catch {
    localStorage.removeItem(STORAGE_USER_KEY)
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const user = ref(readStoredUser())
  const token = ref(localStorage.getItem(STORAGE_TOKEN_KEY) || '')
  const isLoggedIn = computed(() => Boolean(user.value || token.value))
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  function setUser(userData) {
    user.value = userData
    if (userData) {
      localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(userData))
    } else {
      localStorage.removeItem(STORAGE_USER_KEY)
    }
  }

  function setToken(tokenValue) {
    token.value = tokenValue || ''
    if (token.value) {
      localStorage.setItem(STORAGE_TOKEN_KEY, token.value)
    } else {
      localStorage.removeItem(STORAGE_TOKEN_KEY)
    }
  }

  function setSession(userData, tokenValue = '') {
    setUser(userData)
    setToken(tokenValue)
  }

  function logout() {
    setUser(null)
    setToken('')
  }

  return {
    user,
    token,
    isLoggedIn,
    isAdmin,
    setUser,
    setToken,
    setSession,
    logout
  }
})
