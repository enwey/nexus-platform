<template>
  <div class="page-shell">
    <header class="page-header">
      <div>
        <h1>{{ lt('游戏审核', '遊戲審核', 'Game Review') }}</h1>
        <p>{{ lt('查看待处理的版本并执行通过或拒绝操作。', '查看待處理版本並執行通過或拒絕操作。', 'Review pending versions and approve or reject them.') }}</p>
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
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="180" fixed="right">
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveGame, getGameList, rejectGame } from '../api'
import { useI18nLite } from '../i18n'

const games = ref([])
const loading = ref(false)
const { lt } = useI18nLite()

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

const loadGames = async () => {
  loading.value = true
  try {
    const res = await getGameList()
    games.value = (res.data || []).filter((game) => ['PENDING', 'APPROVED', 'REJECTED'].includes(game.status))
  } catch (error) {
    ElMessage.error(error.message || lt('加载审核列表失败', '載入審核列表失敗', 'Failed to load review list'))
  } finally {
    loading.value = false
  }
}

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm(
      lt(`确定通过游戏“${row.name}”吗？`, `確定通過遊戲「${row.name}」嗎？`, `Approve game "${row.name}"?`),
      lt('审核通过', '審核通過', 'Approve Review'),
      { confirmButtonText: lt('通过', '通過', 'Approve'), cancelButtonText: lt('取消', '取消', 'Cancel'), type: 'success' }
    )

    await approveGame(row.id)
    ElMessage.success(lt('审核已通过', '審核已通過', 'Approved'))
    await loadGames()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || lt('审核操作失败', '審核操作失敗', 'Review action failed'))
  }
}

const handleReject = async (row) => {
  try {
    await ElMessageBox.confirm(
      lt(`确定拒绝游戏“${row.name}”吗？`, `確定拒絕遊戲「${row.name}」嗎？`, `Reject game "${row.name}"?`),
      lt('审核拒绝', '審核拒絕', 'Reject Review'),
      { confirmButtonText: lt('拒绝', '拒絕', 'Reject'), cancelButtonText: lt('取消', '取消', 'Cancel'), type: 'warning' }
    )

    await rejectGame(row.id)
    ElMessage.success(lt('已拒绝该游戏', '已拒絕該遊戲', 'Game rejected'))
    await loadGames()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || lt('审核操作失败', '審核操作失敗', 'Review action failed'))
  }
}

onMounted(loadGames)
</script>

<style scoped>
.page-shell { padding: 24px; }
.page-header { margin-bottom: 24px; }
.page-header h1 { margin: 0 0 8px; }
.page-header p { margin: 0; color: #6b7280; }
</style>
