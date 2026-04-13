<template>
  <div class="pro-shell">
    <aside class="pro-sider">
      <div class="pro-brand">Nexus Ops Pro</div>
      <el-menu
        :default-active="activePath"
        class="pro-menu"
        background-color="#001529"
        text-color="rgba(255,255,255,0.72)"
        active-text-color="#fff"
        @select="handleSelect"
      >
        <el-menu-item index="/pro/dashboard">{{ lt('工作台', '工作台', 'Dashboard') }}</el-menu-item>
        <el-sub-menu index="module_group">
          <template #title>{{ lt('業務模塊', '業務模塊', 'Business Modules') }}</template>
          <el-menu-item index="/pro/developers">{{ lt('開發者模塊', '開發者模塊', 'Developer Module') }}</el-menu-item>
          <el-menu-item index="/pro/sms">{{ lt('短信模塊', '短信模塊', 'SMS Module') }}</el-menu-item>
          <el-menu-item index="/pro/recommend">{{ lt('推薦模塊', '推薦模塊', 'Recommendation Module') }}</el-menu-item>
          <el-menu-item index="/pro/recommend-categories">{{ lt('推薦分類模塊', '推薦分類模塊', 'Recommendation Categories') }}</el-menu-item>
          <el-menu-item index="/pro/game-categories">{{ lt('遊戲分類管理', '遊戲分類管理', 'Game Categories') }}</el-menu-item>
          <el-menu-item index="/pro/games">{{ lt('遊戲管理模塊', '遊戲管理模塊', 'Game Management Module') }}</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </aside>

    <section class="pro-main">
      <header class="pro-header">
        <div class="pro-header-title">{{ pageTitle }}</div>
        <div class="pro-header-actions">
          <el-select :model-value="currentLocale" size="small" style="width: 130px" @change="setLocale">
            <el-option label="简体中文" value="zh-CN" />
            <el-option label="繁體中文" value="zh-TW" />
            <el-option label="English" value="en" />
          </el-select>
          <span class="pro-user">{{ userStore.user?.username || userStore.user?.email || 'admin' }}</span>
          <el-button size="small" @click="handleLogout">{{ lt('退出登錄', '退出登錄', 'Sign Out') }}</el-button>
        </div>
      </header>
      <main class="pro-content">
        <router-view />
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { logoutSession } from '../api'
import { useI18nLite } from '../i18n'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { lt, currentLocale, setLocale } = useI18nLite()

const activePath = computed(() => route.path)

const pageTitleMap = {
  '/pro/dashboard': ['運營總覽', '運營總覽', 'Operations Overview'],
  '/pro/developers': ['開發者模塊', '開發者模塊', 'Developer Module'],
  '/pro/sms': ['短信模塊', '短信模塊', 'SMS Module'],
  '/pro/recommend': ['推薦模塊', '推薦模塊', 'Recommendation Module'],
  '/pro/recommend-categories': ['推薦分類模塊', '推薦分類模塊', 'Recommendation Categories'],
  '/pro/game-categories': ['遊戲分類管理', '遊戲分類管理', 'Game Categories'],
  '/pro/games': ['遊戲管理模塊', '遊戲管理模塊', 'Game Management Module']
}

const pageTitle = computed(() => {
  const title = pageTitleMap[route.path] || pageTitleMap['/pro/dashboard']
  return lt(title[0], title[1], title[2])
})

const handleSelect = (path) => {
  if (path && path !== route.path) {
    router.push(path)
  }
}

const handleLogout = async () => {
  try {
    await logoutSession()
  } catch {
    // ignore
  }
  userStore.logout()
  ElMessage.success(lt('已退出登錄', '已退出登錄', 'Signed out'))
  router.push('/login')
}
</script>

<style scoped>
.pro-shell { min-height: 100vh; display: flex; background: #f5f7fa; }
.pro-sider { width: 240px; background: #001529; color: #fff; display: flex; flex-direction: column; }
.pro-brand { height: 56px; display: flex; align-items: center; padding: 0 20px; font-size: 16px; font-weight: 700; color: #fff; border-bottom: 1px solid rgba(255,255,255,0.08); }
.pro-menu { border-right: none; flex: 1; }
.pro-main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.pro-header { height: 56px; background: #fff; border-bottom: 1px solid #eef0f3; display: flex; align-items: center; justify-content: space-between; padding: 0 16px 0 20px; }
.pro-header-title { font-size: 16px; font-weight: 600; color: #1f2329; }
.pro-header-actions { display: flex; align-items: center; gap: 10px; }
.pro-user { color: #4b5563; font-size: 13px; }
.pro-content { flex: 1; padding: 16px; overflow: auto; }
@media (max-width: 980px) {
  .pro-sider { width: 84px; }
  .pro-brand { justify-content: center; padding: 0; font-size: 12px; }
  :deep(.el-sub-menu__title span), :deep(.el-menu-item span) { display: none; }
}
</style>
