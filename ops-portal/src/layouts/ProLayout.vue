<template>
  <div class="pro-shell">
    <transition name="pro-mask-fade">
      <div v-if="isTabletOrBelow && sidebarOpen" class="pro-mask" @click="closeSidebar"></div>
    </transition>

    <aside :class="['pro-sider', { 'is-tablet': isTabletOrBelow, 'is-open': sidebarOpen || !isTabletOrBelow }]">
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
        <div class="pro-header-main">
          <el-button
            v-if="isTabletOrBelow"
            circle
            text
            class="pro-menu-trigger"
            @click="toggleSidebar"
          >
            <el-icon><Menu /></el-icon>
          </el-button>
          <div class="pro-header-title">{{ pageTitle }}</div>
        </div>
        <div class="pro-header-actions">
          <el-select :model-value="currentLocale" size="small" class="locale-select" @change="setLocale">
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
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Menu } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { logoutSession } from '../api'
import { useI18nLite } from '../i18n'
import { useUserStore } from '../stores/user'
import { proNavSections, resolveProPageTitle } from '../router/proNavigation'
import { useViewport } from '../composables/useViewport'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { lt, currentLocale, setLocale } = useI18nLite()
const { isTabletOrBelow } = useViewport()
const sidebarOpen = ref(false)

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
  if (isTabletOrBelow.value) {
    sidebarOpen.value = false
  }
}

const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value
}

const closeSidebar = () => {
  sidebarOpen.value = false
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

watch(() => route.path, () => {
  if (isTabletOrBelow.value) {
    sidebarOpen.value = false
  }
})

watch(isTabletOrBelow, (next) => {
  if (!next) {
    sidebarOpen.value = false
  }
})
</script>

<style scoped>
.pro-shell { height: 100vh; display: flex; background: #f5f7fa; overflow: hidden; }
.pro-mask { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.45); z-index: 30; }
.pro-sider { width: 240px; height: 100vh; background: #001529; color: #fff; display: flex; flex-direction: column; overflow: hidden; position: relative; z-index: 40; flex-shrink: 0; }
.pro-sider.is-tablet { position: fixed; left: 0; top: 0; transform: translateX(-100%); transition: transform 0.24s ease; box-shadow: 0 18px 48px rgba(15, 23, 42, 0.28); }
.pro-sider.is-tablet.is-open { transform: translateX(0); }
.pro-brand { height: 56px; display: flex; align-items: center; padding: 0 20px; font-size: 16px; font-weight: 700; color: #fff; border-bottom: 1px solid rgba(255,255,255,0.08); }
.pro-menu { border-right: none; flex: 1; min-height: 0; overflow-y: auto; overflow-x: hidden; }
.pro-main { flex: 1; min-width: 0; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.pro-header { height: 56px; background: #fff; border-bottom: 1px solid #eef0f3; display: flex; align-items: center; justify-content: space-between; padding: 0 16px 0 20px; }
.pro-header-main { display: flex; align-items: center; gap: 8px; min-width: 0; }
.pro-header-title { font-size: 16px; font-weight: 600; color: #1f2329; }
.pro-header-actions { display: flex; align-items: center; gap: 10px; min-width: 0; }
.pro-menu-trigger { color: #1f2329; font-size: 18px; }
.locale-select { width: 130px; }
.pro-user { color: #4b5563; font-size: 13px; }
.pro-content { flex: 1; min-height: 0; padding: 16px; overflow: auto; }
.pro-mask-fade-enter-active,
.pro-mask-fade-leave-active { transition: opacity 0.24s ease; }
.pro-mask-fade-enter-from,
.pro-mask-fade-leave-to { opacity: 0; }
@media (max-width: 980px) {
  .pro-header { padding: 0 12px; gap: 12px; }
  .pro-header-actions { flex-wrap: wrap; justify-content: flex-end; }
  .pro-content { padding: 12px; }
}
@media (max-width: 768px) {
  .pro-header { height: auto; min-height: 56px; align-items: flex-start; padding-top: 10px; padding-bottom: 10px; }
  .pro-header-title { font-size: 15px; line-height: 1.4; }
  .pro-header-actions { gap: 8px; }
  .locale-select { width: 118px; }
  .pro-user { display: none; }
  .pro-content { padding: 10px; }
}
</style>
