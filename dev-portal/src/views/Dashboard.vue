<template>
  <div class="page-shell">
    <header class="page-header">
      <div>
        <h1>{{ lt('控制台概览', '控制台總覽', 'Dashboard Overview') }}</h1>
        <p>{{ lt('快速查看平台游戏情况，并进入上传与运营页面。', '快速查看平台遊戲情況，並進入上傳與營運頁面。', 'Quickly inspect platform game status and navigate to upload and operations pages.') }}</p>
      </div>
      <div class="page-actions">
        <el-button @click="handleLogout">{{ lt('退出登录', '登出', 'Sign Out') }}</el-button>
        <el-button type="primary" @click="$router.push('/games/upload')">{{ lt('上传游戏', '上傳遊戲', 'Upload Game') }}</el-button>
      </div>
    </header>

    <el-row :gutter="16">
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <template #header>{{ lt('游戏总数', '遊戲總數', 'Total Games') }}</template>
          <div class="metric">{{ stats.totalGames }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <template #header>{{ lt('已通过', '已通過', 'Approved') }}</template>
          <div class="metric metric-success">{{ stats.approvedGames }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <template #header>{{ lt('待审核', '待審核', 'Pending') }}</template>
          <div class="metric metric-warning">{{ stats.pendingGames }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="panel-card">
      <template #header>
        <div class="panel-header">
          <span>{{ lt('最近游戏', '最近遊戲', 'Recent Games') }}</span>
          <el-button link type="primary" @click="$router.push('/games')">{{ lt('查看全部', '查看全部', 'View All') }}</el-button>
        </div>
      </template>

      <el-table :data="recentGames" v-loading="loading" :empty-text="lt('暂无数据', '暫無資料', 'No data')">
        <el-table-column prop="name" :label="lt('游戏名称', '遊戲名稱', 'Game Name')" />
        <el-table-column prop="version" :label="lt('版本', '版本', 'Version')" width="120" />
        <el-table-column prop="status" :label="lt('状态', '狀態', 'Status')" width="140">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getGameList, logoutSession } from '../api'
import { useUserStore } from '../stores/user'
import { useI18nLite } from '../i18n'

const router = useRouter()
const userStore = useUserStore()
const { lt } = useI18nLite()
const loading = ref(false)
const recentGames = ref([])
const stats = ref({ totalGames: 0, approvedGames: 0, pendingGames: 0 })

const getStatusText = (status) => {
  const map = {
    DRAFT: lt('草稿', '草稿', 'Draft'),
    PENDING: lt('待审核', '待審核', 'Pending'),
    APPROVED: lt('已通过', '已通過', 'Approved'),
    REJECTED: lt('已拒绝', '已拒絕', 'Rejected')
  }
  return map[status] || status || lt('未知', '未知', 'Unknown')
}

const getStatusType = (status) => ({ DRAFT: 'info', PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[status] || 'info')

const loadDashboard = async () => {
  loading.value = true
  try {
    const res = await getGameList()
    const games = res.data || []
    recentGames.value = games.slice(0, 5)
    stats.value = {
      totalGames: games.length,
      approvedGames: games.filter((game) => game.status === 'APPROVED').length,
      pendingGames: games.filter((game) => game.status === 'PENDING').length
    }
  } catch (error) {
    ElMessage.error(error.message || lt('加载控制台失败', '載入控制台失敗', 'Failed to load dashboard'))
  } finally {
    loading.value = false
  }
}

const handleLogout = async () => {
  try {
    await logoutSession()
  } catch {
  } finally {
    userStore.logout()
    router.push('/login')
  }
}

onMounted(loadDashboard)
</script>

<style scoped>
.page-shell { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 24px; }
.page-header h1 { margin: 0 0 8px; }
.page-header p { margin: 0; color: #6b7280; }
.page-actions { display: flex; gap: 12px; }
.metric { font-size: 36px; font-weight: 700; color: #111827; }
.metric-success { color: #15803d; }
.metric-warning { color: #b45309; }
.panel-card { margin-top: 24px; }
.panel-header { display: flex; align-items: center; justify-content: space-between; }
</style>
