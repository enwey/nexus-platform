<template>
  <div class="pro-page">
    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ lt('版本审核列表', '版本審核列表', 'Version Review List') }}</div>
            <div class="panel-subtitle">{{ lt('集中处理待审核版本，从列表进入详情页完成检查、分配和审核动作。', '集中處理待審核版本，從列表進入詳情頁完成檢查、分配和審核動作。', 'Handle pending versions and move into detail pages for inspection, assignment, and review actions.') }}</div>
          </div>
          <div class="actions">
            <el-input v-model.trim="keyword" clearable style="width: 240px" :placeholder="lt('搜索游戏名 / AppID', '搜尋遊戲名 / AppID', 'Search game / AppID')" />
            <el-select v-model="statusFilter" style="width: 150px">
              <el-option :label="lt('全部状态', '全部狀態', 'All Statuses')" value="" />
              <el-option :label="lt('待审核', '待審核', 'Pending')" value="PENDING" />
              <el-option :label="lt('已通过', '已通過', 'Approved')" value="APPROVED" />
              <el-option :label="lt('已驳回', '已駁回', 'Rejected')" value="REJECTED" />
            </el-select>
            <el-select v-model="assigneeFilter" clearable style="width: 180px">
              <el-option :label="lt('全部负责人', '全部負責人', 'All Assignees')" value="" />
              <el-option v-for="item in reviewerOptions" :key="item.adminUserId" :label="`${item.username} (${item.adminUserId})`" :value="item.adminUserId" />
            </el-select>
            <el-button type="primary" plain @click="openBatchAssignDialog">{{ lt('批量分配', '批量分配', 'Batch Assign') }}</el-button>
            <el-button type="success" plain @click="batchApprove">{{ lt('批量通过', '批量通過', 'Batch Approve') }}</el-button>
            <el-button type="danger" plain @click="batchReject">{{ lt('批量驳回', '批量駁回', 'Batch Reject') }}</el-button>
            <el-button @click="loadReviews">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <div class="metric-row">
        <div class="metric-chip">{{ lt('待审核', '待審核', 'Pending') }} {{ reviewStats.pending }}</div>
        <div class="metric-chip">{{ lt('已分配', '已分配', 'Assigned') }} {{ reviewStats.assigned }}</div>
        <div class="metric-chip">{{ lt('未分配', '未分配', 'Unassigned') }} {{ reviewStats.unassigned }}</div>
        <div class="metric-chip warning">{{ lt('24h 内到期', '24h 內到期', 'Due in 24h') }} {{ reviewStats.dueSoon }}</div>
        <div class="metric-chip danger">{{ lt('积压超过 3 天', '積壓超過 3 天', 'Older than 3 days') }} {{ reviewStats.overdue }}</div>
      </div>

      <el-table :data="filteredRows" v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="52" reserve-selection />
        <el-table-column :label="lt('优先级', '優先級', 'Priority')" width="90">
          <template #default="{ row }">
            <el-tag :type="priorityType(row.queuePriority)">{{ row.queuePriority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="lt('游戏名称', '遊戲名稱', 'Game')" min-width="180" />
        <el-table-column prop="appId" label="AppID" min-width="170" />
        <el-table-column prop="version" :label="lt('版本', '版本', 'Version')" width="120" />
        <el-table-column :label="lt('状态', '狀態', 'Status')" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.gameStatus)">{{ getStatusText(row.gameStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('负责人', '負責人', 'Assignee')" width="120">
          <template #default="{ row }">{{ row.assignedReviewerId || '-' }}</template>
        </el-table-column>
        <el-table-column :label="lt('SLA 截止', 'SLA 截止', 'SLA Due')" min-width="170">
          <template #default="{ row }">{{ formatDate(row.dueAt) }}</template>
        </el-table-column>
        <el-table-column :label="lt('积压状态', '積壓狀態', 'Backlog')" width="120">
          <template #default="{ row }">
            <el-tag :type="backlogType(row.backlogLevel)">{{ backlogText(row.backlogLevel, row.overdue) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('待审时长', '待審時長', 'Pending Hours')" width="120">
          <template #default="{ row }">{{ row.pendingHours }}h</template>
        </el-table-column>
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ lt('查看详情', '查看詳情', 'View Detail') }}</el-button>
            <el-button v-if="row.gameStatus === 'PENDING'" link type="success" @click="approvePendingGame(row)">{{ lt('通过', '通過', 'Approve') }}</el-button>
            <el-button v-if="row.gameStatus === 'PENDING'" link type="danger" @click="rejectPendingGame(row)">{{ lt('驳回', '駁回', 'Reject') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="batchAssignVisible" :title="lt('批量分配审核', '批量分配審核', 'Batch Assign Reviews')" width="520px">
      <div class="assign-head">{{ lt(`已选择 ${selectedRows.length} 个待审版本`, `已選擇 ${selectedRows.length} 個待審版本`, `Selected ${selectedRows.length} pending versions`) }}</div>
      <el-form :model="batchAssignForm" label-width="120px">
        <el-form-item :label="lt('审核负责人', '審核負責人', 'Reviewer')">
          <el-select v-model="batchAssignForm.reviewerId" style="width: 100%">
            <el-option v-for="item in reviewerOptions" :key="item.adminUserId" :label="`${item.username} (${item.adminUserId})`" :value="item.adminUserId" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('分配说明', '分配說明', 'Assignment Note')">
          <el-input v-model="batchAssignForm.note" type="textarea" :rows="3" maxlength="256" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchAssignVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="batchSaving" @click="submitBatchAssign">{{ lt('确认分配', '確認分配', 'Assign') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveGame, batchOpsReview, getOpsReviewOverview, getOpsReviewers, getOpsReviews, getOpsRuleTemplates, rejectGame } from '../../api'
import { useI18nLite } from '../../i18n'
import { formatDate } from './entityPageShared'

const router = useRouter()
const { lt } = useI18nLite()

const loading = ref(false)
const batchSaving = ref(false)
const batchAssignVisible = ref(false)
const keyword = ref('')
const statusFilter = ref('')
const assigneeFilter = ref('')
const rows = ref([])
const reviewerOptions = ref([])
const selectedRows = ref([])
const ruleTemplates = ref([])
const reviewOverview = ref({
  pending: 0,
  assigned: 0,
  unassigned: 0,
  overdue: 0,
  dueSoon: 0
})
const batchAssignForm = reactive({
  reviewerId: null,
  note: ''
})

const filteredRows = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  return rows.value.filter((row) => {
    const matchesKeyword = !key
      || (row.name || '').toLowerCase().includes(key)
      || (row.appId || '').toLowerCase().includes(key)
    const matchesAssignee = !assigneeFilter.value || row.assignedReviewerId === assigneeFilter.value
    return matchesKeyword && matchesAssignee
  })
})

const reviewStats = computed(() => ({
  pending: reviewOverview.value.pending ?? 0,
  assigned: reviewOverview.value.assigned ?? 0,
  unassigned: reviewOverview.value.unassigned ?? 0,
  overdue: reviewOverview.value.overdue ?? 0,
  dueSoon: reviewOverview.value.dueSoon ?? 0
}))

const loadReviews = async () => {
  loading.value = true
  try {
    const [reviewRes, overviewRes] = await Promise.all([
      getOpsReviews({
        status: statusFilter.value || undefined,
        keyword: keyword.value || undefined
      }),
      getOpsReviewOverview()
    ])
    rows.value = reviewRes.data || []
    reviewOverview.value = overviewRes.data || reviewOverview.value
  } catch (error) {
    ElMessage.error(error.message || lt('加载审核列表失败', '載入審核列表失敗', 'Failed to load review list'))
  } finally {
    loading.value = false
  }
}

const loadReviewers = async () => {
  const res = await getOpsReviewers()
  reviewerOptions.value = Array.isArray(res.data) ? res.data : []
}

const loadRuleTemplates = async () => {
  const res = await getOpsRuleTemplates()
  ruleTemplates.value = Array.isArray(res.data) ? res.data : []
}

const handleSelectionChange = (value) => {
  selectedRows.value = Array.isArray(value) ? value : []
}

const getSelectedPendingVersionIds = () => {
  const pendingRows = selectedRows.value.filter((row) => row?.gameStatus === 'PENDING' && row?.versionId)
  if (!pendingRows.length) {
    ElMessage.warning(lt('请先选择待审核版本', '請先選擇待審核版本', 'Please select pending review versions'))
    return []
  }
  return pendingRows.map((row) => row.versionId)
}

const openBatchAssignDialog = () => {
  const versionIds = getSelectedPendingVersionIds()
  if (!versionIds.length) return
  batchAssignForm.reviewerId = reviewerOptions.value[0]?.adminUserId || null
  batchAssignForm.note = ''
  batchAssignVisible.value = true
}

const submitBatchAssign = async () => {
  const versionIds = getSelectedPendingVersionIds()
  if (!versionIds.length || !batchAssignForm.reviewerId) return
  try {
    batchSaving.value = true
    await batchOpsReview({
      versionIds,
      action: 'ASSIGN',
      reviewerId: batchAssignForm.reviewerId,
      note: batchAssignForm.note.trim()
    })
    batchAssignVisible.value = false
    ElMessage.success(lt('批量分配完成', '批量分配完成', 'Batch assignment completed'))
    await loadReviews()
  } catch (error) {
    ElMessage.error(error.message || lt('批量分配失败', '批量分配失敗', 'Batch assignment failed'))
  } finally {
    batchSaving.value = false
  }
}

const findTemplateContent = (templateType) =>
  ruleTemplates.value
    .filter((item) => item?.templateType === templateType)
    .sort((left, right) => (left?.sortOrder ?? 0) - (right?.sortOrder ?? 0))[0]?.content || ''

const promptReason = async (title, placeholder, templateType = '') => {
  const { value } = await ElMessageBox.prompt(placeholder, title, {
    confirmButtonText: lt('确认', '確認', 'Confirm'),
    cancelButtonText: lt('取消', '取消', 'Cancel'),
    inputValue: findTemplateContent(templateType),
    inputPattern: /^.{2,}$/u,
    inputErrorMessage: lt('原因至少 2 个字符', '原因至少 2 個字元', 'Reason must be at least 2 characters')
  })
  return value.trim()
}

const batchApprove = async () => {
  const versionIds = getSelectedPendingVersionIds()
  if (!versionIds.length) return
  try {
    const reason = await promptReason(
      lt('批量审核通过', '批量審核通過', 'Batch Approve'),
      lt('请输入统一通过理由', '請輸入統一通過理由', 'Enter batch approval reason'),
      'APPROVAL'
    )
    await batchOpsReview({ versionIds, action: 'APPROVE', reason })
    ElMessage.success(lt('批量审核通过完成', '批量審核通過完成', 'Batch approval completed'))
    await loadReviews()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('批量审核通过失败', '批量審核通過失敗', 'Batch approval failed'))
    }
  }
}

const batchReject = async () => {
  const versionIds = getSelectedPendingVersionIds()
  if (!versionIds.length) return
  try {
    const reason = await promptReason(
      lt('批量驳回版本', '批量駁回版本', 'Batch Reject'),
      lt('请输入统一驳回原因', '請輸入統一駁回原因', 'Enter batch rejection reason'),
      'REJECTION'
    )
    await batchOpsReview({ versionIds, action: 'REJECT', reason })
    ElMessage.success(lt('批量驳回完成', '批量駁回完成', 'Batch rejection completed'))
    await loadReviews()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('批量驳回失败', '批量駁回失敗', 'Batch rejection failed'))
    }
  }
}

const approvePendingGame = async (row) => {
  try {
    const reason = await promptReason(
      lt(`审核通过：${row.name}`, `審核通過：${row.name}`, `Approve: ${row.name}`),
      lt('请输入通过理由', '請輸入通過理由', 'Enter approval note'),
      'APPROVAL'
    )
    await approveGame(row.gameId, reason)
    ElMessage.success(lt('审核已通过', '審核已通過', 'Approved'))
    await loadReviews()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('审核通过失败', '審核通過失敗', 'Approval failed'))
    }
  }
}

const rejectPendingGame = async (row) => {
  try {
    const reason = await promptReason(
      lt(`驳回版本：${row.name}`, `駁回版本：${row.name}`, `Reject: ${row.name}`),
      lt('请输入驳回原因', '請輸入駁回原因', 'Enter rejection reason'),
      'REJECTION'
    )
    await rejectGame(row.gameId, reason)
    ElMessage.success(lt('已驳回', '已駁回', 'Rejected'))
    await loadReviews()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('驳回失败', '駁回失敗', 'Rejection failed'))
    }
  }
}

const openDetail = (row) => {
  router.push({ name: 'OpsVersionReviewDetail', params: { reviewId: row.versionId } })
}

const getStatusText = (status) => ({
  PROCESSING: lt('处理中', '處理中', 'Processing'),
  DRAFT: lt('草稿', '草稿', 'Draft'),
  PENDING: lt('待审核', '待審核', 'Pending'),
  APPROVED: lt('已通过', '已通過', 'Approved'),
  REJECTED: lt('已驳回', '已駁回', 'Rejected')
}[status] || status || '-')

const getStatusType = (status) => ({ PROCESSING: 'warning', DRAFT: 'info', PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[status] || 'info')
const priorityType = (priority) => ({ P0: 'danger', P1: 'warning', P2: 'success' }[priority] || 'info')
const backlogType = (level) => ({ OVERDUE: 'danger', RISK: 'warning', NORMAL: 'success' }[level] || 'info')
const backlogText = (level, overdue) => overdue || level === 'OVERDUE' ? lt('已超时', '已超時', 'Overdue') : level === 'RISK' ? lt('临近超时', '臨近超時', 'At Risk') : lt('正常', '正常', 'On Track')

onMounted(async () => {
  await Promise.all([loadReviews(), loadReviewers(), loadRuleTemplates()])
})
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 18px; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.panel-title { font-size: 17px; font-weight: 800; color: #101828; }
.panel-subtitle { margin-top: 6px; color: #667085; font-size: 13px; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; }
.metric-row { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 14px; }
.metric-chip { padding: 8px 12px; border-radius: 999px; background: #f5f7fa; color: #344054; font-size: 13px; }
.metric-chip.danger { background: #fff1f3; color: #c01048; }
.metric-chip.warning { background: #fff7e8; color: #b54708; }
.assign-head { margin-bottom: 12px; color: #344054; font-weight: 700; }
@media (max-width: 920px) { .head-row { flex-direction: column; } }
</style>
