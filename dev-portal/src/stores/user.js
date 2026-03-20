import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('token') || '')
  const isLoggedIn = ref(!!token.value)

  function setUser(userData) {
    user.value = userData
  }

  function setToken(tokenValue) {
    token.value = tokenValue
    localStorage.setItem('token', tokenValue)
    isLoggedIn.value = !!tokenValue
  }

  function logout() {
    user.value = null
    token.value = ''
    localStorage.removeItem('token')
    isLoggedIn.value = false
  }

  return {
    user,
    token,
    isLoggedIn,
    setUser,
    setToken,
    logout
  }
})
