<template>
  <div class="page-shell">
    <header class="page-header">
      <div>
        <h1>游戏审核工作台</h1>
        <p>查看待审核版本，填写审核原因后执行通过或拒绝。</p>
      </div>
      <div class="actions">
        <el-button @click="$router.push('/audit/logs')">查看审计日志</el-button>
        <el-button @click="handleLogout">退出登录</el-button>
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
        <el-table-column label="操作" width="190" fixed="right">
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveGame, getGameList, logoutSession, rejectGame } from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const games = ref([])
const loading = ref(false)

const statusTextMap = {
  PROCESSING: '处理中',
  DRAFT: '草稿',
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝'
}

const statusTypeMap = {
  PROCESSING: 'warning',
  DRAFT: 'info',
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger'
}

const loadGames = async () => {
  loading.value = true
  try {
    const res = await getGameList()
    games.value = (res.data || []).filter((game) =>
      game.status === 'PROCESSING' || game.status === 'PENDING' || game.status === 'APPROVED' || game.status === 'REJECTED'
    )
  } catch (error) {
    ElMessage.error(error.message || '加载审核列表失败')
  } finally {
    loading.value = false
  }
}

const promptReason = async (title) => {
  const { value } = await ElMessageBox.prompt('请填写审核原因（至少 2 个字符）', title, {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputPattern: /^.{2,}$/u,
    inputErrorMessage: '审核原因至少 2 个字符'
  })
  return value?.trim()
}

const handleApprove = async (row) => {
  try {
    const reason = await promptReason(`审核通过：${row.name}`)
    await approveGame(row.id, reason)
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
    const reason = await promptReason(`审核拒绝：${row.name}`)
    await rejectGame(row.id, reason)
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

const handleLogout = async () => {
  try {
    await logoutSession()
  } catch {
  } finally {
    userStore.logout()
    router.push('/login')
  }
}

onMounted(loadGames)
</script>

<style scoped>
.page-shell {
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-header h1 {
  margin: 0 0 8px;
}

.page-header p {
  margin: 0;
  color: #6b7280;
}

.actions {
  display: flex;
  gap: 8px;
}
</style>

