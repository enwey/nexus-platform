<template>
  <transition name="developer-mask-fade">
    <div v-if="isTabletOrBelow && sidebarOpen" class="developer-mask" @click="closeSidebar"></div>
  </transition>

  <div class="developer-shell">
    <aside :class="['developer-sider', { 'is-tablet': isTabletOrBelow, 'is-open': sidebarOpen || !isTabletOrBelow }]">
      <div class="brand-block">
        <div class="brand-title">Nexus Dev Console</div>
        <div class="brand-subtitle">{{ lt('开发者后台', '開發者後台', 'Developer Portal') }}</div>
      </div>

      <el-menu
        :default-active="activePath"
        class="developer-menu"
        background-color="#0b1730"
        text-color="rgba(255,255,255,0.72)"
        active-text-color="#fff"
        @select="handleSelect"
      >
        <template v-for="section in menuSections" :key="section.index">
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

    <section class="developer-main">
      <header class="developer-header">
        <div class="header-main">
          <el-button
            v-if="isTabletOrBelow"
            circle
            text
            class="menu-trigger"
            @click="toggleSidebar"
          >
            <el-icon><Menu /></el-icon>
          </el-button>
          <div>
            <div class="page-title">{{ pageTitle }}</div>
            <div class="page-subtitle">{{ pageSubtitle }}</div>
          </div>
        </div>
        <div class="header-actions">
          <el-select :model-value="currentLocale" size="small" class="locale-select" @change="setLocale">
            <el-option label="简体中文" value="zh-CN" />
            <el-option label="繁體中文" value="zh-TW" />
            <el-option label="English" value="en" />
          </el-select>
          <div class="user-badge">
            <div class="user-name">{{ userLabel }}</div>
            <div class="user-role">{{ lt('开发者账号', '開發者帳號', 'Developer account') }}</div>
          </div>
          <el-button size="small" @click="handleLogout">{{ lt('退出登录', '退出登入', 'Sign Out') }}</el-button>
        </div>
      </header>

      <main class="developer-content">
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
import { developerNavSections, resolveDeveloperPageTitle } from '../router/developerNavigation'
import { useViewport } from '../composables/useViewport'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { currentLocale, lt, setLocale } = useI18nLite()
const { isTabletOrBelow } = useViewport()
const sidebarOpen = ref(false)

const menuSections = developerNavSections
const activePath = computed(() => route.meta?.navKey || route.path)
const pageTitle = computed(() => {
  const title = resolveDeveloperPageTitle(route)
  return Array.isArray(title) ? lt(...title) : lt('开发者工作台', '開發者工作台', 'Developer Dashboard')
})
const pageSubtitle = computed(() => {
  const subtitle = route.meta?.subtitle
  return Array.isArray(subtitle) ? lt(...subtitle) : lt('跟进审核、版本和接入状态。', '追蹤審核、版本與接入狀態。', 'Track review status, releases, and onboarding progress.')
})
const userLabel = computed(() => userStore.user?.username || userStore.user?.email || 'developer')

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
    // ignore network errors during logout
  }

  userStore.logout()
  ElMessage.success(lt('已退出登录', '已退出登入', 'Signed out'))
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
.developer-shell {
  height: 100vh;
  display: flex;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(38, 94, 255, 0.22), transparent 34%),
    linear-gradient(180deg, #f4f7fb 0%, #eef3f8 100%);
}

.developer-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  z-index: 30;
}

.developer-sider {
  width: 272px;
  height: 100vh;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #11203f 0%, #0b1730 100%);
  color: #fff;
  padding: 22px 16px 18px;
  box-sizing: border-box;
  position: relative;
  z-index: 40;
}

.developer-sider.is-tablet {
  position: fixed;
  left: 0;
  top: 0;
  transform: translateX(-100%);
  transition: transform 0.24s ease;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.28);
}

.developer-sider.is-tablet.is-open {
  transform: translateX(0);
}

.brand-block {
  padding: 8px 10px 18px;
}

.brand-title {
  font-size: 19px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.brand-subtitle {
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.68);
  font-size: 13px;
}

.developer-menu {
  border-right: none;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  background: transparent;
}

.developer-main {
  min-width: 0;
  min-height: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.developer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 18px 24px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid rgba(17, 24, 39, 0.06);
}

.header-main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-width: 0;
}

.menu-trigger {
  color: #0f172a;
  font-size: 18px;
  flex-shrink: 0;
}

.page-title {
  font-size: 22px;
  font-weight: 800;
  color: #101827;
}

.page-subtitle {
  margin-top: 6px;
  color: #667085;
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.locale-select {
  width: 130px;
}

.user-badge {
  min-width: 120px;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #e4e7ec;
  border-radius: 14px;
}

.user-name {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.user-role {
  margin-top: 2px;
  font-size: 12px;
  color: #667085;
}

.developer-content {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 22px 24px 28px;
}

.developer-mask-fade-enter-active,
.developer-mask-fade-leave-active {
  transition: opacity 0.24s ease;
}

.developer-mask-fade-enter-from,
.developer-mask-fade-leave-to {
  opacity: 0;
}

@media (max-width: 980px) {
  .developer-shell {
    min-height: 100vh;
  }

  .developer-sider {
    height: 100vh;
  }

  .developer-header {
    padding: 16px 20px;
  }

  .header-actions {
    width: 100%;
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .developer-content {
    padding: 18px 20px 24px;
  }
}

@media (max-width: 768px) {
  .developer-header {
    flex-direction: column;
    align-items: flex-start;
    padding: 14px 16px;
  }

  .page-title {
    font-size: 18px;
    line-height: 1.4;
  }

  .page-subtitle {
    font-size: 12px;
    line-height: 1.5;
  }

  .header-actions {
    justify-content: flex-start;
  }

  .locale-select {
    width: 118px;
  }

  .user-badge {
    min-width: 0;
  }

  .user-role {
    display: none;
  }

  .developer-content {
    padding: 14px 16px 20px;
  }
}
</style>
