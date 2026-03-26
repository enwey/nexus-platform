import { createRouter, createWebHistory } from 'vue-router'
import { getCurrentUser } from '../api'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  { path: '/dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { requiresAuth: true } },
  { path: '/games', name: 'Games', component: () => import('../views/Games.vue'), meta: { requiresAuth: true } },
  { path: '/games/upload', name: 'GameUpload', component: () => import('../views/GameUpload.vue'), meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

let restoringSession = null

async function ensureSession(userStore) {
  if (userStore.user || !userStore.token) {
    return Boolean(userStore.user)
  }

  if (!restoringSession) {
    restoringSession = getCurrentUser()
      .then((res) => {
        userStore.setUser(res.data)
        return true
      })
      .catch(() => {
        userStore.logout()
        return false
      })
      .finally(() => {
        restoringSession = null
      })
  }

  return restoringSession
}

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  const hasSession = await ensureSession(userStore)

  if (to.meta.requiresAuth && !hasSession) {
    return '/login'
  }

  if ((to.path === '/login' || to.path === '/register') && hasSession) {
    return '/dashboard'
  }

  return true
})

export default router

