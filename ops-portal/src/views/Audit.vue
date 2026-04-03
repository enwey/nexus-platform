<template>
  <div class="page-shell">
    <header class="page-header">
      <div>
        <h1>{{ lt('游戏审核工作台', '遊戲審核工作台', 'Game Review Console') }}</h1>
        <p>{{ lt('查看待审核版本，填写审核原因后执行通过或拒绝。', '查看待審核版本，填寫審核原因後執行通過或拒絕。', 'Review pending versions and approve/reject with reason.') }}</p>
      </div>
      <div class="actions">
        <el-button type="primary" plain @click="$router.push('/runtime-ops')">Runtime Ops Console</el-button>
        <el-button @click="$router.push('/audit/logs')">{{ lt('查看审计日志', '查看審計日誌', 'View Audit Logs') }}</el-button>
        <el-button @click="handleLogout">{{ lt('退出登录', '登出', 'Sign Out') }}</el-button>
      </div>
    </header>

    <el-card>
      <el-table :data="games" v-loading="loading" :empty-text="lt('暂无待审核数据', '暫無待審核資料', 'No pending review items')">
        <el-table-column prop="name" :label="lt('游戏名称', '遊戲名稱', 'Game Name')" min-width="180" />
        <el-table-column prop="description" :label="lt('描述', '描述', 'Description')" min-width="220" show-overflow-tooltip />
        <el-table-column prop="version" :label="lt('版本', '版本', 'Version')" width="120" />
        <el-table-column prop="developerId" :label="lt('开发者 ID', '開發者 ID', 'Developer ID')" width="140" />
        <el-table-column prop="status" :label="lt('状态', '狀態', 'Status')" width="140">
          <template #default="{ row }"><el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="190" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" type="success" size="small" @click="handleApprove(row)">{{ lt('通过', '通過', 'Approve') }}</el-button>
            <el-button v-if="row.status === 'PENDING'" type="danger" size="small" @click="handleReject(row)">{{ lt('拒绝', '拒絕', 'Reject') }}</el-button>
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
import { useI18nLite } from '../i18n'

const router = useRouter()
const userStore = useUserStore()
const { lt } = useI18nLite()
const games = ref([])
const loading = ref(false)

const getStatusText = (status) => {
  const map = {
    PROCESSING: lt('处理中', '處理中', 'Processing'),
    DRAFT: lt('草稿', '草稿', 'Draft'),
    PENDING: lt('待审核', '待審核', 'Pending'),
    APPROVED: lt('已通过', '已通過', 'Approved'),
    REJECTED: lt('已拒绝', '已拒絕', 'Rejected')
  }
  return map[status] || status || lt('未知', '未知', 'Unknown')
}
const getStatusType = (status) => ({ PROCESSING: 'warning', DRAFT: 'info', PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[status] || 'info')

const loadGames = async () => {
  loading.value = true
  try {
    const res = await getGameList()
    games.value = (res.data || []).filter((game) => ['PROCESSING', 'PENDING', 'APPROVED', 'REJECTED'].includes(game.status))
  } catch (error) {
    ElMessage.error(error.message || lt('加载审核列表失败', '載入審核列表失敗', 'Failed to load review list'))
  } finally {
    loading.value = false
  }
}

const promptReason = async (title) => {
  const { value } = await ElMessageBox.prompt(
    lt('请填写审核原因（至少 2 个字符）', '請填寫審核原因（至少 2 個字元）', 'Please provide review reason (at least 2 characters)'),
    title,
    {
      confirmButtonText: lt('确认', '確認', 'Confirm'),
      cancelButtonText: lt('取消', '取消', 'Cancel'),
      inputPattern: /^.{2,}$/u,
      inputErrorMessage: lt('审核原因至少 2 个字符', '審核原因至少 2 個字元', 'Reason must be at least 2 characters')
    }
  )
  return value?.trim()
}

const handleApprove = async (row) => {
  try {
    const reason = await promptReason(lt(`审核通过：${row.name}`, `審核通過：${row.name}`, `Approve: ${row.name}`))
    await approveGame(row.id, reason)
    ElMessage.success(lt('审核已通过', '審核已通過', 'Approved'))
    await loadGames()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || lt('审核操作失败', '審核操作失敗', 'Review action failed'))
  }
}

const handleReject = async (row) => {
  try {
    const reason = await promptReason(lt(`审核拒绝：${row.name}`, `審核拒絕：${row.name}`, `Reject: ${row.name}`))
    await rejectGame(row.id, reason)
    ElMessage.success(lt('已拒绝该游戏', '已拒絕該遊戲', 'Game rejected'))
    await loadGames()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || lt('审核操作失败', '審核操作失敗', 'Review action failed'))
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

onMounted(loadGames)
</script>

<style scoped>
.page-shell { padding: 24px; }
.page-header { margin-bottom: 24px; display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.page-header h1 { margin: 0 0 8px; }
.page-header p { margin: 0; color: #6b7280; }
.actions { display: flex; gap: 8px; }
</style>
