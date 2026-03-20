<template>
  <div class="audit-container">
    <el-card>
      <template #header>
        <h2>游戏审核</h2>
      </template>
      
      <el-table :data="games" v-loading="loading" stripe>
        <el-table-column prop="name" label="游戏名称" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="version" label="版本" width="100" />
        <el-table-column prop="developerId" label="开发者ID" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="success"
              size="small"
              @click="handleApprove(row)"
            >
              通过
            </el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              type="danger"
              size="small"
              @click="handleReject(row)"
            >
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getGameList, approveGame, rejectGame } from '../api'

const games = ref([])
const loading = ref(false)

const loadGames = async () => {
  loading.value = true
  try {
    const res = await getGameList()
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

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要通过游戏 "${row.name}" 吗？`,
      '审核通过',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'success'
      }
    )
    
    const res = await approveGame(row.id)
    
    if (res.code === 0) {
      ElMessage.success('审核通过')
      await loadGames()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch {
    // 用户取消
  }
}

const handleReject = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要拒绝游戏 "${row.name}" 吗？`,
      '审核拒绝',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const res = await rejectGame(row.id)
    
    if (res.code === 0) {
      ElMessage.success('已拒绝')
      await loadGames()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
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
.audit-container {
  padding: 20px;
}
</style>
