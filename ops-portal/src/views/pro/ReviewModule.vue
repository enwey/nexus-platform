<template>
  <div class="pro-page">
    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ lt('审核中心', '審核中心', 'Review Center') }}</div>
            <div class="panel-subtitle">{{ lt('集中处理待审核版本，执行版本检查、通过与驳回。', '集中處理待審核版本，執行版本檢查、通過與駁回。', 'Handle pending versions with inspection, approval, and rejection actions.') }}</div>
          </div>
          <div class="actions">
            <el-select v-model="statusFilter" style="width: 150px">
              <el-option :label="lt('全部状态', '全部狀態', 'All Statuses')" value="" />
              <el-option :label="lt('待审核', '待審核', 'Pending')" value="PENDING" />
              <el-option :label="lt('已通过', '已通過', 'Approved')" value="APPROVED" />
              <el-option :label="lt('已驳回', '已駁回', 'Rejected')" value="REJECTED" />
            </el-select>
            <el-input v-model.trim="keyword" clearable style="width: 240px" :placeholder="lt('搜索游戏名 / AppID', '搜尋遊戲名 / AppID', 'Search game / AppID')" />
            <el-select v-model="assigneeFilter" clearable style="width: 180px">
              <el-option :label="lt('全部负责人', '全部負責人', 'All Assignees')" value="" />
              <el-option v-for="item in reviewerOptions" :key="item.adminUserId" :label="`${item.username} (${item.adminUserId})`" :value="item.adminUserId" />
            </el-select>
            <el-button type="primary" plain @click="openBatchAssignDialog">{{ lt('批量分配', '批量分配', 'Batch Assign') }}</el-button>
            <el-button type="success" plain @click="batchApprove">{{ lt('批量通过', '批量通過', 'Batch Approve') }}</el-button>
            <el-button type="danger" plain @click="batchReject">{{ lt('批量驳回', '批量駁回', 'Batch Reject') }}</el-button>
            <el-button @click="loadGames">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <div class="metric-row">
        <div class="metric-chip">{{ lt('待审核', '待審核', 'Pending') }} {{ reviewStats.pending }}</div>
        <div class="metric-chip">{{ lt('已分配', '已分配', 'Assigned') }} {{ reviewStats.assigned }}</div>
        <div class="metric-chip">{{ lt('未分配', '未分配', 'Unassigned') }} {{ reviewStats.unassigned }}</div>
        <div class="metric-chip">{{ lt('已通过', '已通過', 'Approved') }} {{ reviewStats.approved }}</div>
        <div class="metric-chip">{{ lt('已驳回', '已駁回', 'Rejected') }} {{ reviewStats.rejected }}</div>
        <div class="metric-chip warning">{{ lt('24h 内到期', '24h 內到期', 'Due in 24h') }} {{ reviewStats.dueSoon }}</div>
        <div class="metric-chip danger">{{ lt('积压超过 3 天', '積壓超過 3 天', 'Older than 3 days') }} {{ reviewStats.overdue }}</div>
        <div class="metric-chip">{{ lt('平均待审时长', '平均待審時長', 'Avg Pending Hours') }} {{ reviewStats.avgPendingHours }}</div>
      </div>

      <div class="overview-grid">
        <el-card class="mini-panel">
          <template #header>{{ lt('审核员负载', '審核員負載', 'Reviewer Load') }}</template>
          <el-table :data="reviewOverview.reviewerLoads || []" :empty-text="lt('暂无审核员负载', '暫無審核員負載', 'No reviewer load')">
            <el-table-column prop="reviewerName" :label="lt('审核员', '審核員', 'Reviewer')" min-width="120" />
            <el-table-column prop="assignedPending" :label="lt('待审数', '待審數', 'Assigned')" width="90" />
            <el-table-column prop="overduePending" :label="lt('超时数', '超時數', 'Overdue')" width="90" />
          </el-table>
        </el-card>
        <el-card class="mini-panel">
          <template #header>{{ lt('积压分层', '積壓分層', 'Backlog Layers') }}</template>
          <div class="bucket-list">
            <div v-for="item in reviewOverview.backlogBuckets || []" :key="item.bucketCode" class="bucket-item">
              <div class="bucket-label">{{ item.bucketLabel }}</div>
              <div class="bucket-value">{{ item.count }}</div>
            </div>
          </div>
        </el-card>
      </div>

      <el-table :data="filteredGames" v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="52" reserve-selection />
        <el-table-column :label="lt('优先级', '優先級', 'Priority')" width="90">
          <template #default="{ row }">
            <el-tag :type="priorityType(row.queuePriority)">{{ row.queuePriority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="lt('游戏名称', '遊戲名稱', 'Game')" min-width="180" />
        <el-table-column prop="appId" label="AppID" min-width="170" />
        <el-table-column prop="developerId" :label="lt('开发者 ID', '開發者 ID', 'Developer ID')" width="120" />
        <el-table-column prop="version" :label="lt('版本', '版本', 'Version')" width="120" />
        <el-table-column :label="lt('状态', '狀態', 'Status')" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.gameStatus)">{{ getStatusText(row.gameStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('前端状态', '前端狀態', 'Frontend State')" width="140">
          <template #default="{ row }">
            <el-tag :type="getFrontendStateType(row.frontendState)">{{ getFrontendStateText(row.frontendState) }}</el-tag>
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
        <el-table-column :label="lt('更新时间', '更新時間', 'Updated')" min-width="170">
          <template #default="{ row }">{{ formatDate(row.updatedAt || row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column :label="lt('操作', '操作', 'Actions')" min-width="280" fixed="right">
          <template #default="{ row }">
            <div class="action-list">
              <el-button link type="info" @click="inspectVersion(row)">{{ lt('版本检查', '版本檢查', 'Inspect') }}</el-button>
              <el-button v-if="row.gameStatus === 'PENDING'" link type="primary" @click="openAssignDialog(row)">{{ lt('分配审核', '分配審核', 'Assign') }}</el-button>
              <el-button v-if="row.gameStatus === 'PENDING'" link type="success" @click="approvePendingGame(row)">{{ lt('审核通过', '審核通過', 'Approve') }}</el-button>
              <el-button v-if="row.gameStatus === 'PENDING'" link type="danger" @click="rejectPendingGame(row)">{{ lt('驳回', '駁回', 'Reject') }}</el-button>
              <el-button v-if="['DRAFT', 'REJECTED'].includes(row.gameStatus)" link type="warning" @click="submitForAudit(row)">{{ lt('重新提交', '重新提交', 'Resubmit') }}</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="panel-card">
      <template #header>{{ lt('复审申诉队列', '複審申訴佇列', 'Appeal Queue') }}</template>
      <el-table :data="appeals" v-loading="appealsLoading" :empty-text="lt('暂无申诉单', '暫無申訴單', 'No appeals')">
        <el-table-column prop="gameName" :label="lt('游戏名称', '遊戲名稱', 'Game')" min-width="180" />
        <el-table-column prop="appId" label="AppID" min-width="150" />
        <el-table-column prop="versionName" :label="lt('版本号', '版本號', 'Version')" width="120" />
        <el-table-column prop="appealStatus" :label="lt('申诉状态', '申訴狀態', 'Status')" width="120" />
        <el-table-column prop="appealReason" :label="lt('申诉原因', '申訴原因', 'Appeal Reason')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="rejectionSnapshot" :label="lt('原驳回原因', '原駁回原因', 'Original Rejection')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="reviewNote" :label="lt('复审备注', '複審備註', 'Review Note')" min-width="180" show-overflow-tooltip />
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="180">
          <template #default="{ row }">
            <el-button v-if="row.appealStatus === 'SUBMITTED'" type="success" link @click="decideAppeal(row, 'APPROVE')">{{ lt('通过复审', '通過複審', 'Approve Appeal') }}</el-button>
            <el-button v-if="row.appealStatus === 'SUBMITTED'" type="danger" link @click="decideAppeal(row, 'REJECT')">{{ lt('驳回复审', '駁回複審', 'Reject Appeal') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="assignVisible" :title="lt('分配审核任务', '分配審核任務', 'Assign Review Task')" width="520px">
      <div class="assign-head">{{ assignTargetLabel }}</div>
      <el-form :model="assignForm" label-width="120px">
        <el-form-item :label="lt('审核负责人', '審核負責人', 'Reviewer')">
          <el-select v-model="assignForm.reviewerId" style="width: 100%">
            <el-option v-for="item in reviewerOptions" :key="item.adminUserId" :label="`${item.username} (${item.adminUserId})`" :value="item.adminUserId" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('分配说明', '分配說明', 'Assignment Note')">
          <el-input v-model="assignForm.note" type="textarea" :rows="3" maxlength="256" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="assignSaving" @click="submitAssign">{{ lt('确认分配', '確認分配', 'Assign') }}</el-button>
      </template>
    </el-dialog>

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
import { ElMessage, ElMessageBox } from 'element-plus'
import { batchOpsReview, approveGame, assignOpsReview, decideOpsReviewAppeal, getGameVersions, getOpsReviewAppeals, getOpsReviewOverview, getOpsReviewers, getOpsReviews, getOpsRuleTemplates, rejectGame, submitGameForAudit } from '../../api'
import { useI18nLite } from '../../i18n'

const { lt } = useI18nLite()
const games = ref([])
const loading = ref(false)
const appealsLoading = ref(false)
const assignSaving = ref(false)
const batchSaving = ref(false)
const assignVisible = ref(false)
const batchAssignVisible = ref(false)
const keyword = ref('')
const statusFilter = ref('')
const assigneeFilter = ref('')
const reviewerOptions = ref([])
const appeals = ref([])
const ruleTemplates = ref([])
const reviewOverview = ref({
  pending: 0,
  assigned: 0,
  unassigned: 0,
  overdue: 0,
  dueSoon: 0,
  approved: 0,
  rejected: 0,
  pendingAppeals: 0,
  avgPendingHours: 0,
  reviewerLoads: [],
  backlogBuckets: []
})
const selectedRows = ref([])
const assignVersionId = ref(null)
const assignTargetLabel = ref('')
const assignForm = reactive({
  reviewerId: null,
  note: ''
})
const batchAssignForm = reactive({
  reviewerId: null,
  note: ''
})

const loadGames = async () => {
  loading.value = true
  try {
    const [reviewRes, overviewRes] = await Promise.all([
      getOpsReviews({
        status: statusFilter.value || undefined,
        keyword: keyword.value || undefined
      }),
      getOpsReviewOverview()
    ])
    games.value = reviewRes.data || []
    reviewOverview.value = overviewRes.data || reviewOverview.value
  } catch (error) {
    ElMessage.error(error.message || lt('加载审核列表失败', '載入審核列表失敗', 'Failed to load review list'))
  } finally {
    loading.value = false
  }
}

const loadAppeals = async () => {
  appealsLoading.value = true
  try {
    const res = await getOpsReviewAppeals()
    appeals.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || lt('加载申诉队列失败', '載入申訴佇列失敗', 'Failed to load appeal queue'))
  } finally {
    appealsLoading.value = false
  }
}

const loadReviewers = async () => {
  try {
    const res = await getOpsReviewers()
    reviewerOptions.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    ElMessage.error(error.message || lt('加载审核负责人失败', '載入審核負責人失敗', 'Failed to load reviewer list'))
  }
}

const loadRuleTemplates = async () => {
  try {
    const res = await getOpsRuleTemplates()
    ruleTemplates.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    ElMessage.error(error.message || lt('加载审核模板失败', '載入審核範本失敗', 'Failed to load review templates'))
  }
}

const filteredGames = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  return games.value.filter((game) => {
    const matchesKeyword = !key
      || (game.name || '').toLowerCase().includes(key)
      || (game.appId || '').toLowerCase().includes(key)
    const matchesAssignee = !assigneeFilter.value || game.assignedReviewerId === assigneeFilter.value
    return matchesKeyword && matchesAssignee
  })
})

const reviewStats = computed(() => {
  return {
    pending: reviewOverview.value.pending ?? 0,
    assigned: reviewOverview.value.assigned ?? 0,
    unassigned: reviewOverview.value.unassigned ?? 0,
    approved: reviewOverview.value.approved ?? 0,
    rejected: reviewOverview.value.rejected ?? 0,
    overdue: reviewOverview.value.overdue ?? 0,
    dueSoon: reviewOverview.value.dueSoon ?? 0,
    avgPendingHours: reviewOverview.value.avgPendingHours ?? 0
  }
})

const openAssignDialog = (row) => {
  assignVersionId.value = row.versionId
  assignTargetLabel.value = `${row.name} / ${row.appId}`
  assignForm.reviewerId = row.assignedReviewerId || reviewerOptions.value[0]?.adminUserId || null
  assignForm.note = ''
  assignVisible.value = true
}

const handleSelectionChange = (rows) => {
  selectedRows.value = Array.isArray(rows) ? rows : []
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

const submitAssign = async () => {
  if (!assignVersionId.value || !assignForm.reviewerId) {
    ElMessage.warning(lt('请选择审核负责人', '請選擇審核負責人', 'Please select a reviewer'))
    return
  }
  try {
    assignSaving.value = true
    await assignOpsReview(assignVersionId.value, {
      reviewerId: assignForm.reviewerId,
      note: assignForm.note.trim()
    })
    assignVisible.value = false
    ElMessage.success(lt('审核任务已分配', '審核任務已分配', 'Review task assigned'))
    await loadGames()
  } catch (error) {
    ElMessage.error(error.message || lt('分配审核任务失败', '分配審核任務失敗', 'Failed to assign review task'))
  } finally {
    assignSaving.value = false
  }
}

const submitBatchAssign = async () => {
  const versionIds = getSelectedPendingVersionIds()
  if (!versionIds.length) return
  if (!batchAssignForm.reviewerId) {
    ElMessage.warning(lt('请选择审核负责人', '請選擇審核負責人', 'Please select a reviewer'))
    return
  }
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
    await loadGames()
  } catch (error) {
    ElMessage.error(error.message || lt('批量分配失败', '批量分配失敗', 'Batch assignment failed'))
  } finally {
    batchSaving.value = false
  }
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
    await loadGames()
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
    await loadGames()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('批量驳回失败', '批量駁回失敗', 'Batch rejection failed'))
    }
  }
}

const decideAppeal = async (row, decision) => {
  try {
    const note = await promptReason(
      decision === 'APPROVE'
        ? lt(`通过复审：${row.gameName}`, `通過複審：${row.gameName}`, `Approve Appeal: ${row.gameName}`)
        : lt(`驳回复审：${row.gameName}`, `駁回複審：${row.gameName}`, `Reject Appeal: ${row.gameName}`),
      decision === 'APPROVE'
        ? lt('请输入复审通过说明', '請輸入複審通過說明', 'Enter approval note')
        : lt('请输入复审驳回说明', '請輸入複審駁回說明', 'Enter rejection note')
    )
    await decideOpsReviewAppeal(row.id, { decision, reviewNote: note })
    ElMessage.success(decision === 'APPROVE'
      ? lt('复审已通过，版本已重新进入审核队列', '複審已通過，版本已重新進入審核佇列', 'Appeal approved and version returned to review queue')
      : lt('复审已驳回', '複審已駁回', 'Appeal rejected'))
    await Promise.all([loadGames(), loadAppeals()])
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('处理申诉失败', '處理申訴失敗', 'Failed to process appeal'))
    }
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

const submitForAudit = async (row) => {
  try {
    const reason = await promptReason(
      lt(`重新提交：${row.name}`, `重新提交：${row.name}`, `Resubmit: ${row.name}`),
      lt('请输入提审说明', '請輸入提審說明', 'Enter submission note')
    )
    await submitGameForAudit(row.gameId, reason)
    ElMessage.success(lt('已提交审核', '已提交審核', 'Submitted for review'))
    await loadGames()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('提交审核失败', '提交審核失敗', 'Failed to submit'))
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
    await loadGames()
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
    await loadGames()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('驳回失败', '駁回失敗', 'Rejection failed'))
    }
  }
}

const inspectVersion = async (row) => {
  let latestVersionText = row.version || '-'
  let manifestStatus = lt('未知', '未知', 'Unknown')
  let manifestSummary = row.manifestSummary || '-'
  const prelaunchChecklist = findTemplateContent('PRELAUNCH')

  try {
    const res = await getGameVersions(row.gameId)
    const latest = Array.isArray(res.data) ? res.data[0] : null
    if (latest) {
      latestVersionText = latest.versionName || latestVersionText
      manifestStatus = latest.hostedManifestValid === true
        ? lt('通过', '通過', 'Passed')
        : latest.hostedManifestValid === false
          ? lt('未通过', '未通過', 'Failed')
          : manifestStatus
      manifestSummary = latest.hostedManifestSummary || manifestSummary
    }
  } catch (error) {
    manifestSummary = error.message || manifestSummary
  }

  await ElMessageBox.alert(
    lt(
      `游戏名称：${row.name}\nAppID：${row.appId || '-'}\n状态：${getStatusText(row.gameStatus)}\n最新版本：${latestVersionText}\nManifest 检查：${manifestStatus}\nManifest 摘要：${manifestSummary}\n上线前检查：${prelaunchChecklist || '-'}`,
      `遊戲名稱：${row.name}\nAppID：${row.appId || '-'}\n狀態：${getStatusText(row.gameStatus)}\n最新版本：${latestVersionText}\nManifest 檢查：${manifestStatus}\nManifest 摘要：${manifestSummary}\n上線前檢查：${prelaunchChecklist || '-'}`,
      `Game: ${row.name}\nAppID: ${row.appId || '-'}\nStatus: ${getStatusText(row.gameStatus)}\nLatest Version: ${latestVersionText}\nManifest Check: ${manifestStatus}\nManifest Summary: ${manifestSummary}\nPrelaunch Checklist: ${prelaunchChecklist || '-'}`
    ),
    lt('版本检查', '版本檢查', 'Version Inspection'),
    { confirmButtonText: lt('知道了', '知道了', 'OK') }
  )
}

function getStatusType(status) {
  return { PROCESSING: 'warning', DRAFT: 'info', PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[status] || 'info'
}

function getStatusText(status) {
  const map = {
    PROCESSING: lt('处理中', '處理中', 'Processing'),
    DRAFT: lt('草稿', '草稿', 'Draft'),
    PENDING: lt('待审核', '待審核', 'Pending'),
    APPROVED: lt('已通过', '已通過', 'Approved'),
    REJECTED: lt('已驳回', '已駁回', 'Rejected')
  }
  return map[status] || status || '-'
}

function getFrontendStateText(state) {
  return {
    OPERABLE: lt('可运营', '可營運', 'Operable'),
    UNDER_REVIEW: lt('待审核中', '待審核中', 'Under Review'),
    NOT_OPEN: lt('未开放', '未開放', 'Not Open'),
    BLOCKED: lt('已拦截', '已攔截', 'Blocked'),
    PROCESSING: lt('处理中', '處理中', 'Processing')
  }[state] || state || '-'
}

function getFrontendStateType(state) {
  return {
    OPERABLE: 'success',
    UNDER_REVIEW: 'warning',
    NOT_OPEN: 'info',
    BLOCKED: 'danger',
    PROCESSING: ''
  }[state] || 'info'
}

function priorityType(priority) {
  return { P0: 'danger', P1: 'warning', P2: 'success' }[priority] || 'info'
}

function backlogType(level) {
  return { OVERDUE: 'danger', RISK: 'warning', NORMAL: 'success' }[level] || 'info'
}

function backlogText(level, overdue) {
  if (overdue || level === 'OVERDUE') return lt('已超时', '已超時', 'Overdue')
  if (level === 'RISK') return lt('临近超时', '臨近超時', 'At Risk')
  return lt('正常', '正常', 'On Track')
}

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

onMounted(async () => {
  await Promise.all([loadGames(), loadReviewers(), loadAppeals(), loadRuleTemplates()])
})
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 18px; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.panel-title { font-size: 17px; font-weight: 800; color: #101828; }
.panel-subtitle { margin-top: 6px; color: #667085; font-size: 13px; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; }
.action-list { display: flex; gap: 8px; flex-wrap: wrap; }
.metric-row { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 14px; }
.metric-chip { padding: 8px 12px; border-radius: 999px; background: #f5f7fa; color: #344054; font-size: 13px; }
.metric-chip.danger { background: #fff1f3; color: #c01048; }
.metric-chip.warning { background: #fff7e8; color: #b54708; }
.overview-grid { display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.8fr); gap: 16px; margin-bottom: 14px; }
.mini-panel { border-radius: 16px; }
.bucket-list { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.bucket-item { border-radius: 14px; background: #f8fafc; padding: 14px; border: 1px solid #e5e7eb; }
.bucket-label { color: #667085; font-size: 12px; }
.bucket-value { margin-top: 6px; color: #101828; font-size: 24px; font-weight: 800; }
.assign-head { margin-bottom: 12px; color: #344054; font-weight: 700; }
@media (max-width: 920px) { .head-row { flex-direction: column; } .overview-grid { grid-template-columns: 1fr; } .bucket-list { grid-template-columns: 1fr; } }
</style>
