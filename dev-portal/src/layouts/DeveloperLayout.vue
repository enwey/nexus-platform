<template>
  <div class="developer-shell">
    <aside class="developer-sider">
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
        <div>
          <div class="page-title">{{ pageTitle }}</div>
          <div class="page-subtitle">{{ pageSubtitle }}</div>
        </div>
        <div class="header-actions">
          <el-select :model-value="currentLocale" size="small" style="width: 130px" @change="setLocale">
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
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { logoutSession } from '../api'
import { useI18nLite } from '../i18n'
import { useUserStore } from '../stores/user'
import { developerNavSections, resolveDeveloperPageTitle } from '../router/developerNavigation'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { currentLocale, lt, setLocale } = useI18nLite()

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
</script>

<style scoped>
.developer-shell {
  height: 100vh;
  display: flex;
  background:
    radial-gradient(circle at top left, rgba(38, 94, 255, 0.22), transparent 34%),
    linear-gradient(180deg, #f4f7fb 0%, #eef3f8 100%);
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

@media (max-width: 980px) {
  .developer-shell {
    height: auto;
    min-height: 100vh;
    flex-direction: column;
  }

  .developer-sider {
    width: 100%;
    height: auto;
  }

  .developer-menu {
    overflow: visible;
  }

  :deep(.el-sub-menu__title span),
  :deep(.el-menu-item span) {
    display: none;
  }

  .developer-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
