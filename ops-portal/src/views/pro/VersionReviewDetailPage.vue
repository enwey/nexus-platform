<template>
  <div class="pro-page">
    <el-card class="panel-card" v-loading="loading">
      <template #header>
        <div class="head-row">
          <div>
            <div class="breadcrumb-line">
              <el-button link @click="router.push('/pro/review-center/versions/list')">{{ lt('返回版本审核列表', '返回版本審核列表', 'Back to Version Reviews') }}</el-button>
              <span>/</span>
              <span>{{ reviewItem?.name || lt('版本审核详情', '版本審核詳情', 'Version Review Detail') }}</span>
            </div>
            <div class="panel-title">{{ reviewItem?.name || '-' }}</div>
            <div class="panel-subtitle">{{ reviewItem?.appId || '-' }} / {{ reviewItem?.version || '-' }}</div>
          </div>
          <div class="actions">
            <el-button type="primary" @click="openAssignDialog">{{ lt('分配审核', '分配審核', 'Assign') }}</el-button>
            <el-button v-if="reviewItem?.gameStatus === 'PENDING'" type="success" @click="approvePendingGame">{{ lt('审核通过', '審核通過', 'Approve') }}</el-button>
            <el-button v-if="reviewItem?.gameStatus === 'PENDING'" type="danger" @click="rejectPendingGame">{{ lt('驳回版本', '駁回版本', 'Reject') }}</el-button>
            <el-button @click="openGame">{{ lt('查看游戏详情', '查看遊戲詳情', 'Open Game Detail') }}</el-button>
            <el-button :loading="loading" @click="loadDetail">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <el-empty v-if="!reviewItem" :description="lt('未找到对应审核记录', '未找到對應審核記錄', 'Review record not found')" />
      <template v-else>
        <div class="summary-grid">
          <div class="summary-item">
            <span>{{ lt('审核状态', '審核狀態', 'Review Status') }}</span>
            <strong><el-tag :type="getStatusType(reviewItem.gameStatus)">{{ getStatusText(reviewItem.gameStatus) }}</el-tag></strong>
          </div>
          <div class="summary-item">
            <span>{{ lt('负责人', '負責人', 'Assignee') }}</span>
            <strong>{{ reviewItem.assignedReviewerId || '-' }}</strong>
          </div>
          <div class="summary-item">
            <span>{{ lt('SLA 截止', 'SLA 截止', 'SLA Due') }}</span>
            <strong>{{ formatDate(reviewItem.dueAt) }}</strong>
          </div>
          <div class="summary-item">
            <span>{{ lt('积压状态', '積壓狀態', 'Backlog') }}</span>
            <strong><el-tag :type="backlogType(reviewItem.backlogLevel)">{{ backlogText(reviewItem.backlogLevel, reviewItem.overdue) }}</el-tag></strong>
          </div>
        </div>

        <el-descriptions :column="2" border class="section">
          <el-descriptions-item :label="lt('游戏名称', '遊戲名稱', 'Game Name')">{{ reviewItem.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="AppID">{{ reviewItem.appId || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('开发者 ID', '開發者 ID', 'Developer ID')">{{ reviewItem.developerId || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('分类', '分類', 'Category')">{{ reviewItem.category || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('版本号', '版本號', 'Version')">{{ reviewItem.version || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('版本状态', '版本狀態', 'Version State')">{{ versionStatusText(versionDetail?.status) }}</el-descriptions-item>
          <el-descriptions-item :label="lt('提交说明', '提交說明', 'Submit Note')" :span="2">{{ reviewItem.submitNote || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('驳回原因', '駁回原因', 'Audit Reason')" :span="2">{{ reviewItem.auditReason || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('Manifest 校验', 'Manifest 校驗', 'Manifest Check')">{{ manifestStatusText(versionDetail?.hostedManifestValid ?? reviewItem.manifestValid) }}</el-descriptions-item>
          <el-descriptions-item :label="lt('强更', '強更', 'Force Update')">{{ reviewItem.forcedUpdate ? lt('是', '是', 'Yes') : lt('否', '否', 'No') }}</el-descriptions-item>
          <el-descriptions-item :label="lt('Manifest 摘要', 'Manifest 摘要', 'Manifest Summary')" :span="2">{{ versionDetail?.hostedManifestSummary || reviewItem.manifestSummary || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('上线前检查模板', '上線前檢查模板', 'Prelaunch Checklist')" :span="2">{{ prelaunchChecklist || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-card>

    <el-dialog v-model="assignVisible" :title="lt('分配审核任务', '分配審核任務', 'Assign Review Task')" width="520px">
      <div class="assign-head">{{ reviewItem?.name || '-' }}</div>
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveGame, assignOpsReview, getGameVersions, getOpsReviews, getOpsReviewers, getOpsRuleTemplates, rejectGame } from '../../api'
import { useI18nLite } from '../../i18n'
import { formatDate } from './entityPageShared'

const props = defineProps({
  reviewId: {
    type: [String, Number],
    required: true
  }
})

const router = useRouter()
const { lt } = useI18nLite()

const loading = ref(false)
const assignSaving = ref(false)
const assignVisible = ref(false)
const reviewItem = ref(null)
const versionDetail = ref(null)
const reviewerOptions = ref([])
const ruleTemplates = ref([])
const assignForm = reactive({
  reviewerId: null,
  note: ''
})

const prelaunchChecklist = computed(() =>
  ruleTemplates.value
    .filter((item) => item?.templateType === 'PRELAUNCH')
    .sort((left, right) => (left?.sortOrder ?? 0) - (right?.sortOrder ?? 0))[0]?.content || ''
)

const loadDetail = async () => {
  loading.value = true
  try {
    const [reviewsRes, reviewersRes, templatesRes] = await Promise.all([
      getOpsReviews(),
      getOpsReviewers(),
      getOpsRuleTemplates()
    ])
    reviewerOptions.value = Array.isArray(reviewersRes.data) ? reviewersRes.data : []
    ruleTemplates.value = Array.isArray(templatesRes.data) ? templatesRes.data : []
    reviewItem.value = (reviewsRes.data || []).find((item) => String(item.versionId) === String(props.reviewId)) || null
    if (!reviewItem.value) return
    assignForm.reviewerId = reviewItem.value.assignedReviewerId || reviewerOptions.value[0]?.adminUserId || null
    assignForm.note = ''
    const versionsRes = await getGameVersions(reviewItem.value.gameId)
    versionDetail.value = (versionsRes.data || []).find((item) => String(item.id) === String(reviewItem.value.versionId)) || null
  } catch (error) {
    ElMessage.error(error.message || lt('加载版本审核详情失败', '載入版本審核詳情失敗', 'Failed to load version review detail'))
  } finally {
    loading.value = false
  }
}

const openAssignDialog = () => {
  assignVisible.value = true
}

const submitAssign = async () => {
  if (!reviewItem.value?.versionId || !assignForm.reviewerId) return
  try {
    assignSaving.value = true
    await assignOpsReview(reviewItem.value.versionId, {
      reviewerId: assignForm.reviewerId,
      note: assignForm.note.trim()
    })
    assignVisible.value = false
    ElMessage.success(lt('审核任务已分配', '審核任務已分配', 'Review task assigned'))
    await loadDetail()
  } catch (error) {
    ElMessage.error(error.message || lt('分配审核任务失败', '分配審核任務失敗', 'Failed to assign review task'))
  } finally {
    assignSaving.value = false
  }
}

const promptReason = async (title, placeholder, templateType = '') => {
  const inputValue = ruleTemplates.value
    .filter((item) => item?.templateType === templateType)
    .sort((left, right) => (left?.sortOrder ?? 0) - (right?.sortOrder ?? 0))[0]?.content || ''
  const { value } = await ElMessageBox.prompt(placeholder, title, {
    confirmButtonText: lt('确认', '確認', 'Confirm'),
    cancelButtonText: lt('取消', '取消', 'Cancel'),
    inputValue,
    inputPattern: /^.{2,}$/u,
    inputErrorMessage: lt('原因至少 2 个字符', '原因至少 2 個字元', 'Reason must be at least 2 characters')
  })
  return value.trim()
}

const approvePendingGame = async () => {
  if (!reviewItem.value) return
  try {
    const reason = await promptReason(
      lt(`审核通过：${reviewItem.value.name}`, `審核通過：${reviewItem.value.name}`, `Approve: ${reviewItem.value.name}`),
      lt('请输入通过理由', '請輸入通過理由', 'Enter approval note'),
      'APPROVAL'
    )
    await approveGame(reviewItem.value.gameId, reason)
    ElMessage.success(lt('审核已通过', '審核已通過', 'Approved'))
    await loadDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('审核通过失败', '審核通過失敗', 'Approval failed'))
    }
  }
}

const rejectPendingGame = async () => {
  if (!reviewItem.value) return
  try {
    const reason = await promptReason(
      lt(`驳回版本：${reviewItem.value.name}`, `駁回版本：${reviewItem.value.name}`, `Reject: ${reviewItem.value.name}`),
      lt('请输入驳回原因', '請輸入駁回原因', 'Enter rejection reason'),
      'REJECTION'
    )
    await rejectGame(reviewItem.value.gameId, reason)
    ElMessage.success(lt('已驳回', '已駁回', 'Rejected'))
    await loadDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('驳回失败', '駁回失敗', 'Rejection failed'))
    }
  }
}

const openGame = () => {
  if (reviewItem.value?.gameId) {
    router.push({ name: 'OpsGameDetail', params: { gameId: reviewItem.value.gameId } })
  }
}

const getStatusText = (status) => ({
  PROCESSING: lt('处理中', '處理中', 'Processing'),
  DRAFT: lt('草稿', '草稿', 'Draft'),
  PENDING: lt('待审核', '待審核', 'Pending'),
  APPROVED: lt('已通过', '已通過', 'Approved'),
  REJECTED: lt('已驳回', '已駁回', 'Rejected')
}[status] || status || '-')

const getStatusType = (status) => ({ PROCESSING: 'warning', DRAFT: 'info', PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[status] || 'info')
const backlogType = (level) => ({ OVERDUE: 'danger', RISK: 'warning', NORMAL: 'success' }[level] || 'info')
const backlogText = (level, overdue) => overdue || level === 'OVERDUE' ? lt('已超时', '已超時', 'Overdue') : level === 'RISK' ? lt('临近超时', '臨近超時', 'At Risk') : lt('正常', '正常', 'On Track')

const versionStatusText = (status) => ({
  DRAFT: lt('草稿', '草稿', 'Draft'),
  PROCESSING: lt('处理中', '處理中', 'Processing'),
  SUBMITTED: lt('待审核', '待審核', 'Submitted'),
  APPROVED: lt('已通过', '已通過', 'Approved'),
  REJECTED: lt('已驳回', '已駁回', 'Rejected'),
  PUBLISHED: lt('已发布', '已發布', 'Published')
}[status] || status || '-')

const manifestStatusText = (value) => {
  if (value === true) return lt('通过', '通過', 'Passed')
  if (value === false) return lt('未通过', '未通過', 'Failed')
  return lt('未知', '未知', 'Unknown')
}

onMounted(loadDetail)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 18px; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.breadcrumb-line { display: flex; align-items: center; gap: 8px; color: #667085; font-size: 13px; margin-bottom: 8px; }
.panel-title { font-size: 20px; font-weight: 800; color: #101828; }
.panel-subtitle { margin-top: 6px; color: #667085; font-size: 13px; }
.actions { display: flex; gap: 8px; flex-wrap: wrap; }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.summary-item { padding: 12px 14px; border-radius: 14px; background: #f8fafc; display: flex; flex-direction: column; gap: 6px; color: #475467; font-size: 13px; }
.summary-item strong { font-size: 16px; color: #111827; }
.section { margin-top: 16px; }
.assign-head { margin-bottom: 12px; color: #344054; font-weight: 700; }
@media (max-width: 920px) {
  .head-row { flex-direction: column; }
  .summary-grid { grid-template-columns: 1fr 1fr; }
}
</style>
