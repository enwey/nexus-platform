import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCurrentUser } from '../api'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'OpsLogin', component: () => import('../views/Login.vue') },
  { path: '/audit', name: 'OpsAudit', component: () => import('../views/Audit.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/audit/logs', name: 'OpsAuditLogs', component: () => import('../views/AuditLogs.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/android', name: 'OpsAndroid', component: () => import('../views/AndroidConsole.vue'), meta: { requiresAuth: true, requiresAdmin: true } }
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
    return '/audit'
  }

  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    userStore.logout()
    ElMessage.warning('当前账号没有运营审核权限，请使用管理员账号登录')
    return '/login'
  }

  return true
})

export default router

