<template>
  <div class="page-shell">
    <header class="page-header">
      <div>
        <h1>控制台概览</h1>
        <p>快速查看平台游戏情况，并进入上传与运营页面。</p>
      </div>
      <div class="page-actions">
        <el-button @click="handleLogout">退出登录</el-button>
        <el-button type="primary" @click="$router.push('/games/upload')">上传游戏</el-button>
      </div>
    </header>

    <el-row :gutter="16">
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <template #header>游戏总数</template>
          <div class="metric">{{ stats.totalGames }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <template #header>已通过</template>
          <div class="metric metric-success">{{ stats.approvedGames }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <template #header>待审核</template>
          <div class="metric metric-warning">{{ stats.pendingGames }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="panel-card">
      <template #header>
        <div class="panel-header">
          <span>最近游戏</span>
          <el-button link type="primary" @click="$router.push('/games')">查看全部</el-button>
        </div>
      </template>

      <el-table :data="recentGames" v-loading="loading" empty-text="暂无数据">
        <el-table-column prop="name" label="游戏名称" />
        <el-table-column prop="version" label="版本" width="120" />
        <el-table-column prop="status" label="状态" width="140">
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
import { getGameList } from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const recentGames = ref([])
const stats = ref({
  totalGames: 0,
  approvedGames: 0,
  pendingGames: 0
})

const statusTextMap = {
  DRAFT: '草稿',
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝'
}

const statusTypeMap = {
  DRAFT: 'info',
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger'
}

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
    ElMessage.error(error.message || '加载控制台失败')
  } finally {
    loading.value = false
  }
}

const getStatusText = (status) => statusTextMap[status] || status || '未知'
const getStatusType = (status) => statusTypeMap[status] || 'info'

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

onMounted(loadDashboard)
</script>

<style scoped>
.page-shell {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0 0 8px;
}

.page-header p {
  margin: 0;
  color: #6b7280;
}

.page-actions {
  display: flex;
  gap: 12px;
}

.metric {
  font-size: 36px;
  font-weight: 700;
  color: #111827;
}

.metric-success {
  color: #15803d;
}

.metric-warning {
  color: #b45309;
}

.panel-card {
  margin-top: 24px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
