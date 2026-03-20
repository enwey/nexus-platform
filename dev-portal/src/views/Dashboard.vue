<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>游戏统计</span>
              <el-icon><TrendCharts /></el-icon>
            </div>
          </template>
          <div class="stat-content">
            <div class="stat-item">
              <div class="stat-value">{{ stats.totalGames }}</div>
              <div class="stat-label">总游戏数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ stats.approvedGames }}</div>
              <div class="stat-label">已通过</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ stats.pendingGames }}</div>
              <div class="stat-label">待审核</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>快速操作</span>
              <el-icon><Operation /></el-icon>
            </div>
          </template>
          <div class="quick-actions">
            <el-button type="primary" @click="$router.push('/games/upload')" style="width: 100%; margin-bottom: 10px;">
              <el-icon><Upload /></el-icon>
              上传游戏
            </el-button>
            <el-button type="success" @click="$router.push('/games')" style="width: 100%;">
              <el-icon><List /></el-icon>
              我的游戏
            </el-button>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>最近游戏</span>
              <el-icon><Clock /></el-icon>
            </div>
          </template>
          <el-table :data="recentGames" v-loading="loading">
            <el-table-column prop="name" label="游戏名称" />
            <el-table-column prop="version" label="版本" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="$router.push('/games')">
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getGameList } from '../api'

const router = useRouter()
const userStore = useUserStore()
const stats = ref({
  totalGames: 0,
  approvedGames: 0,
  pendingGames: 0
})
const recentGames = ref([])
const loading = ref(false)

const loadStats = async () => {
  loading.value = true
  try {
    const res = await getGameList()
    if (res.code === 0) {
      const games = res.data || []
      stats.value = {
        totalGames: games.length,
        approvedGames: games.filter(g => g.status === 'APPROVED').length,
        pendingGames: games.filter(g => g.status === 'PENDING').length
      }
      recentGames.value = games.slice(0, 5)
    }
  } catch (error) {
    console.error('加载统计失败', error)
  } finally {
    loading.value = false
  }
}

const getStatusType = (status) => {
  const map = {
    'DRAFT': 'info',
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    'DRAFT': '草稿',
    'PENDING': '待审核',
    'APPROVED': '已通过',
    'REJECTED': '已拒绝'
  }
  return map[status] || status
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-content {
  display: flex;
  justify-content: space-around;
  padding: 20px 0;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
