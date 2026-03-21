<template>
  <div class="page-shell">
    <header class="page-header">
      <div>
        <h1>我的游戏</h1>
        <p>查看当前账号提交的游戏版本和审核状态。</p>
      </div>
      <el-button type="primary" @click="$router.push('/games/upload')">上传游戏</el-button>
    </header>

    <el-card>
      <el-table :data="games" v-loading="loading" empty-text="暂无游戏数据">
        <el-table-column prop="name" label="游戏名称" min-width="180" />
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="version" label="版本" width="120" />
        <el-table-column prop="status" label="状态" width="140">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="200" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeveloperGames } from '../api'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const games = ref([])
const loading = ref(false)

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

const loadGames = async () => {
  if (!userStore.user?.id) {
    games.value = []
    return
  }

  loading.value = true
  try {
    const res = await getDeveloperGames(userStore.user.id)
    games.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || '加载游戏失败')
  } finally {
    loading.value = false
  }
}

const handleView = (row) => {
  ElMessageBox.alert(
    `游戏名称：${row.name}\n描述：${row.description || '暂无'}\n版本：${row.version || '暂无'}\n状态：${getStatusText(row.status)}`,
    '游戏详情',
    { confirmButtonText: '知道了' }
  )
}

const getStatusText = (status) => statusTextMap[status] || status || '未知'
const getStatusType = (status) => statusTypeMap[status] || 'info'

onMounted(loadGames)
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
</style>
