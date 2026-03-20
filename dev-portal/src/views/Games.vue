<template>
  <div class="games-container">
    <el-card>
      <template #header>
        <div class="header-content">
          <h2>我的游戏</h2>
          <el-button type="primary" @click="$router.push('/games/upload')">
            <el-icon><Plus /></el-icon>
            上传游戏
          </el-button>
        </div>
      </template>
      
      <el-table :data="games" v-loading="loading" stripe>
        <el-table-column prop="name" label="游戏名称" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="version" label="版本" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">
              查看
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import { getDeveloperGames } from '../api'

const router = useRouter()
const userStore = useUserStore()
const games = ref([])
const loading = ref(false)

const loadGames = async () => {
  loading.value = true
  try {
    const res = await getDeveloperGames(userStore.user?.id)
    if (res.code === 0) {
      games.value = res.data || []
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleView = (row) => {
  ElMessageBox.alert(
    `游戏名称：${row.name}`,
    `描述：${row.description}\n版本：${row.version}\n状态：${getStatusText(row.status)}`,
    '游戏详情'
  )
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除游戏 "${row.name}" 吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    ElMessage.success('删除成功')
    await loadGames()
  } catch {
    // 用户取消
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
  loadGames()
})
</script>

<style scoped>
.games-container {
  padding: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content h2 {
  margin: 0;
}
</style>
