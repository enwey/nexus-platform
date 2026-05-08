import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCurrentUser } from '../api'
import { ltGlobal } from '../i18n'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  {
    path: '/',
    component: () => import('../layouts/DeveloperLayout.vue'),
    meta: { requiresAuth: true, requiresDeveloper: true },
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue') },
      { path: 'games', name: 'Games', component: () => import('../views/Games.vue') },
      { path: 'releases', name: 'ReleaseCenter', component: () => import('../views/ReleaseCenter.vue') },
      { path: 'insights', name: 'Insights', component: () => import('../views/Insights.vue') },
      { path: 'notifications', name: 'Notifications', component: () => import('../views/Notifications.vue') },
      { path: 'feedback', name: 'FeedbackCenter', component: () => import('../views/FeedbackCenter.vue') },
      { path: 'docs', name: 'DeveloperDocs', component: () => import('../views/DeveloperDocs.vue') },
      { path: 'account', name: 'Account', component: () => import('../views/Account.vue') }
    ]
  },
  { path: '/games/upload', redirect: '/releases' }
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
  const canUseDeveloperPortal = userStore.user?.role === 'DEVELOPER' || userStore.user?.role === 'ADMIN'

  if (to.meta.requiresAuth && !hasSession) {
    return '/login'
  }

  if (to.meta.requiresDeveloper && hasSession && !canUseDeveloperPortal) {
    userStore.logout()
    ElMessage.warning(ltGlobal(
      '当前账号没有开发者后台权限，请使用开发者账号登录',
      '當前帳號沒有開發者後台權限，請使用開發者帳號登入',
      'Current account does not have developer portal access. Please sign in with a developer account.'
    ))
    return '/login'
  }

  if ((to.path === '/login' || to.path === '/register') && hasSession && canUseDeveloperPortal) {
    return '/dashboard'
  }

  return true
})

export default router
