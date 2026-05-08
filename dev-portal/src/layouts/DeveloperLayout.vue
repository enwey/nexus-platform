<template>
  <div class="developer-shell">
    <aside class="developer-sider">
      <div class="brand-block">
        <div class="brand-title">Nexus Dev Console</div>
        <div class="brand-subtitle">{{ lt('开发者后台', '開發者後台', 'Developer Portal') }}</div>
      </div>

      <el-menu
        :default-active="route.path"
        class="developer-menu"
        background-color="transparent"
        text-color="rgba(255,255,255,0.78)"
        active-text-color="#ffffff"
        @select="handleSelect"
      >
        <el-menu-item index="/dashboard">{{ lt('工作台', '工作台', 'Dashboard') }}</el-menu-item>
        <el-menu-item index="/games">{{ lt('我的游戏', '我的遊戲', 'My Games') }}</el-menu-item>
        <el-menu-item index="/releases">{{ lt('版本发布', '版本發布', 'Release Center') }}</el-menu-item>
        <el-menu-item index="/insights">{{ lt('数据概览', '數據概覽', 'Insights') }}</el-menu-item>
        <el-menu-item index="/notifications">{{ lt('通知中心', '通知中心', 'Notifications') }}</el-menu-item>
        <el-menu-item index="/feedback">{{ lt('反馈工单', '回饋工單', 'Support Tickets') }}</el-menu-item>
        <el-menu-item index="/docs">{{ lt('开发文档', '開發文件', 'Docs') }}</el-menu-item>
        <el-menu-item index="/account">{{ lt('账号中心', '帳號中心', 'Account') }}</el-menu-item>
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

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { currentLocale, lt, setLocale } = useI18nLite()

const pageMeta = {
  '/dashboard': {
    title: ['开发者工作台', '開發者工作台', 'Developer Dashboard'],
    subtitle: ['跟进审核、版本和接入状态。', '追蹤審核、版本與接入狀態。', 'Track review status, releases, and onboarding progress.']
  },
  '/games': {
    title: ['我的游戏', '我的遊戲', 'My Games'],
    subtitle: ['维护游戏资料、分类、描述与版本全貌。', '維護遊戲資料、分類、描述與版本全貌。', 'Maintain game metadata, categories, descriptions, and version history.']
  },
  '/releases': {
    title: ['版本发布中心', '版本發布中心', 'Release Center'],
    subtitle: ['上传首个版本、提交审核、回滚历史版本。', '上傳首個版本、提交審核、回滾歷史版本。', 'Upload first package, submit versions for review, and roll back history.']
  },
  '/insights': {
    title: ['数据概览', '數據概覽', 'Insights'],
    subtitle: ['从审核通过率、版本分布和分类经营看整体质量。', '從審核通過率、版本分布和分類經營看整體品質。', 'See quality through approval rate, release cadence, and category mix.']
  },
  '/notifications': {
    title: ['通知中心', '通知中心', 'Notifications'],
    subtitle: ['接收平台审核、发布、治理和系统通知。', '接收平台審核、發布、治理與系統通知。', 'Receive review, release, governance, and system notices from the platform.']
  },
  '/feedback': {
    title: ['反馈工单中心', '回饋工單中心', 'Support Ticket Center'],
    subtitle: ['提交发布、审核、运行时和治理相关问题，并跟进平台处理进度。', '提交發布、審核、運行時和治理相關問題，並追蹤平台處理進度。', 'Submit release, review, runtime, and governance issues, then track platform responses.']
  },
  '/docs': {
    title: ['开发文档中心', '開發文件中心', 'Docs Center'],
    subtitle: ['接入规范、提审清单和引擎指南统一查阅。', '接入規範、提審清單和引擎指南統一查閱。', 'Access integration specs, submission checklists, and engine guides.']
  },
  '/account': {
    title: ['账号中心', '帳號中心', 'Account Center'],
    subtitle: ['维护资料、安全设置与登录设备。', '維護資料、安全設定與登入裝置。', 'Manage profile, security settings, and active devices.']
  }
}

const currentMeta = computed(() => pageMeta[route.path] || pageMeta['/dashboard'])
const pageTitle = computed(() => lt(...currentMeta.value.title))
const pageSubtitle = computed(() => lt(...currentMeta.value.subtitle))
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
  min-height: 100vh;
  display: flex;
  background:
    radial-gradient(circle at top left, rgba(38, 94, 255, 0.22), transparent 34%),
    linear-gradient(180deg, #f4f7fb 0%, #eef3f8 100%);
}

.developer-sider {
  width: 252px;
  flex-shrink: 0;
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
}

.developer-main {
  min-width: 0;
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
  flex: 1;
  padding: 22px 24px 28px;
}

@media (max-width: 980px) {
  .developer-shell {
    flex-direction: column;
  }

  .developer-sider {
    width: 100%;
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
