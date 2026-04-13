import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCurrentUser } from '../api'
import { useUserStore } from '../stores/user'
import { ltGlobal } from '../i18n'

const routes = [
  { path: '/', redirect: '/pro/dashboard' },
  { path: '/login', name: 'OpsLogin', component: () => import('../views/Login.vue') },
  {
    path: '/pro',
    component: () => import('../layouts/ProLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: '/pro/dashboard' },
      { path: 'dashboard', name: 'ProDashboard', component: () => import('../views/pro/DashboardModule.vue') },
      { path: 'developers', name: 'ProDevelopers', component: () => import('../views/pro/DeveloperModule.vue') },
      { path: 'sms', name: 'ProSms', component: () => import('../views/pro/SmsModule.vue') },
      { path: 'recommend', name: 'ProRecommend', component: () => import('../views/pro/RecommendModule.vue') },
      { path: 'recommend-categories', name: 'ProRecommendCategories', component: () => import('../views/pro/RecommendCategoryModule.vue') },
      { path: 'game-categories', name: 'ProGameCategories', component: () => import('../views/pro/GameCategoryModule.vue') },
      { path: 'games', name: 'ProGames', component: () => import('../views/pro/GameModule.vue') }
    ]
  },
  { path: '/audit', redirect: '/pro/games' },
  { path: '/audit/logs', redirect: '/pro/dashboard' },
  { path: '/verification-codes', redirect: '/pro/sms' },
  { path: '/android-console', redirect: '/pro/dashboard' },
  { path: '/runtime-ops', redirect: '/pro/recommend' },
  { path: '/discover-ops', redirect: '/pro/recommend' },
  { path: '/android', redirect: '/pro/dashboard' }
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

  if (to.path === '/login' && hasSession) {
    return '/pro/dashboard'
  }

  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    userStore.logout()
    ElMessage.warning(ltGlobal('当前账号没有运营审核权限，请使用管理员账号登录', '當前帳號沒有營運審核權限，請使用管理員帳號登入', 'Current account does not have operations audit permission. Please sign in with an admin account.'))
    return '/login'
  }

  return true
})

export default router
