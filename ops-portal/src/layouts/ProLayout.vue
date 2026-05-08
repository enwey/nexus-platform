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
        <template v-for="section in navSections" :key="section.index">
          <el-menu-item v-if="section.type === 'item'" :index="section.index">
            {{ lt(...section.label) }}
          </el-menu-item>
          <el-sub-menu v-else :index="section.index">
            <template #title>{{ lt(...section.label) }}</template>
            <el-menu-item v-for="item in section.items" :key="item.index" :index="item.index">
              {{ lt(...item.label) }}
            </el-menu-item>
          </el-sub-menu>
        </template>
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
import { proNavSections, resolveProPageTitle } from '../router/proNavigation'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { lt, currentLocale, setLocale } = useI18nLite()

const navSections = proNavSections
const activePath = computed(() => route.meta.activeMenu || route.path)

const pageTitle = computed(() => {
  const title = resolveProPageTitle(route)
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
.pro-shell { height: 100vh; display: flex; background: #f5f7fa; overflow: hidden; }
.pro-sider { width: 240px; height: 100vh; background: #001529; color: #fff; display: flex; flex-direction: column; overflow: hidden; }
.pro-brand { height: 56px; display: flex; align-items: center; padding: 0 20px; font-size: 16px; font-weight: 700; color: #fff; border-bottom: 1px solid rgba(255,255,255,0.08); }
.pro-menu { border-right: none; flex: 1; min-height: 0; overflow-y: auto; overflow-x: hidden; }
.pro-main { flex: 1; min-width: 0; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.pro-header { height: 56px; background: #fff; border-bottom: 1px solid #eef0f3; display: flex; align-items: center; justify-content: space-between; padding: 0 16px 0 20px; }
.pro-header-title { font-size: 16px; font-weight: 600; color: #1f2329; }
.pro-header-actions { display: flex; align-items: center; gap: 10px; }
.pro-user { color: #4b5563; font-size: 13px; }
.pro-content { flex: 1; min-height: 0; padding: 16px; overflow: auto; }
@media (max-width: 980px) {
  .pro-sider { width: 84px; }
  .pro-brand { justify-content: center; padding: 0; font-size: 12px; }
  :deep(.el-sub-menu__title span), :deep(.el-menu-item span) { display: none; }
}
</style>
