<template>
  <div class="page-shell">
    <header class="page-header">
      <div>
        <h1>游戏审核</h1>
        <p>查看待处理的版本并执行通过或拒绝操作。</p>
      </div>
    </header>

    <el-card>
      <el-table :data="games" v-loading="loading" empty-text="暂无待审核数据">
        <el-table-column prop="name" label="游戏名称" min-width="180" />
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="version" label="版本" width="120" />
        <el-table-column prop="developerId" label="开发者 ID" width="140" />
        <el-table-column prop="status" label="状态" width="140">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" type="success" size="small" @click="handleApprove(row)">
              通过
            </el-button>
            <el-button v-if="row.status === 'PENDING'" type="danger" size="small" @click="handleReject(row)">
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveGame, getGameList, rejectGame } from '../api'

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
  loading.value = true
  try {
    const res = await getGameList()
    games.value = (res.data || []).filter((game) => game.status === 'PENDING' || game.status === 'APPROVED' || game.status === 'REJECTED')
  } catch (error) {
    ElMessage.error(error.message || '加载审核列表失败')
  } finally {
    loading.value = false
  }
}

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm(`确定通过游戏“${row.name}”吗？`, '审核通过', {
      confirmButtonText: '通过',
      cancelButtonText: '取消',
      type: 'success'
    })

    await approveGame(row.id)
    ElMessage.success('审核已通过')
    await loadGames()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '审核操作失败')
    }
  }
}

const handleReject = async (row) => {
  try {
    await ElMessageBox.confirm(`确定拒绝游戏“${row.name}”吗？`, '审核拒绝', {
      confirmButtonText: '拒绝',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await rejectGame(row.id)
    ElMessage.success('已拒绝该游戏')
    await loadGames()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '审核操作失败')
    }
  }
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
