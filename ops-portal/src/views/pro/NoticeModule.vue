<template>
  <div class="pro-page">
    <el-alert v-if="loadError" type="error" :closable="false" :title="loadError" />

    <div class="summary-grid">
      <div class="summary-item"><span>{{ lt('通知总数', '通知總數', 'Notices') }}</span><strong>{{ notices.length }}</strong></div>
      <div class="summary-item"><span>{{ lt('生效中', '生效中', 'Live') }}</span><strong>{{ effectiveCount('LIVE') }}</strong></div>
      <div class="summary-item"><span>{{ lt('待生效', '待生效', 'Scheduled') }}</span><strong>{{ effectiveCount('SCHEDULED') }}</strong></div>
      <div class="summary-item"><span>{{ lt('草稿/暂停', '草稿/暫停', 'Draft / Paused') }}</span><strong>{{ draftPausedCount }}</strong></div>
    </div>

    <el-card class="panel-card">
      <template #header>
        <div class="card-head">
          <span>{{ lt('通知投放列表', '通知投放列表', 'Notice Delivery List') }}</span>
          <div class="head-actions">
            <el-select v-model="categoryFilter" clearable style="width: 140px">
              <el-option :label="lt('全部分类', '全部分類', 'All Categories')" value="" />
              <el-option label="SYSTEM" value="SYSTEM" />
              <el-option label="REVIEW" value="REVIEW" />
              <el-option label="PUBLISH" value="PUBLISH" />
              <el-option label="RISK" value="RISK" />
            </el-select>
            <el-select v-model="statusFilter" clearable style="width: 160px">
              <el-option :label="lt('全部投放态', '全部投放態', 'All Status')" value="" />
              <el-option label="LIVE" value="LIVE" />
              <el-option label="SCHEDULED" value="SCHEDULED" />
              <el-option label="DRAFT" value="DRAFT" />
              <el-option label="PAUSED" value="PAUSED" />
              <el-option label="EXPIRED" value="EXPIRED" />
            </el-select>
            <el-input v-model.trim="keyword" clearable style="width: 240px" :placeholder="lt('搜索标题 / 受众 / ID', '搜尋標題 / 受眾 / ID', 'Search title / audience / id')" />
            <el-button size="small" @click="loadNotices">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
            <el-button type="primary" size="small" @click="openCreate">{{ lt('新建通知', '新增通知', 'New Notice') }}</el-button>
          </div>
        </div>
      </template>
      <el-table :data="filteredNotices" v-loading="loading" :empty-text="lt('暂无通知', '暫無通知', 'No notices')" @row-click="openPreview">
        <el-table-column :label="lt('分类', '分類', 'Category')" width="120">
          <template #default="{ row }">
            <el-tag :type="categoryTagType(row.category)">{{ categoryText(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('受众', '受眾', 'Audience')" width="160">
          <template #default="{ row }">
            <div>{{ audienceText(row.audienceRole) }}</div>
            <div class="sub-text" v-if="row.targetUserId">UID {{ row.targetUserId }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="title" :label="lt('标题', '標題', 'Title')" min-width="220" />
        <el-table-column :label="lt('投放状态', '投放狀態', 'Delivery')" width="140">
          <template #default="{ row }">
            <el-tag :type="effectiveTagType(row.effectiveStatus)">{{ effectiveText(row.effectiveStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('开始时间', '開始時間', 'Start')" min-width="160">
          <template #default="{ row }">{{ formatDate(row.startAt) || '--' }}</template>
        </el-table-column>
        <el-table-column :label="lt('结束时间', '結束時間', 'End')" min-width="160">
          <template #default="{ row }">{{ formatDate(row.endAt) || '--' }}</template>
        </el-table-column>
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="260">
          <template #default="{ row }">
            <el-button link @click.stop="openPreview(row)">{{ lt('预览', '預覽', 'Preview') }}</el-button>
            <el-button link type="primary" @click.stop="openEdit(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
            <el-button v-if="row.deliveryStatus !== 'PUBLISHED'" link type="success" @click.stop="changeStatus(row, 'PUBLISHED')">{{ lt('发布', '發布', 'Publish') }}</el-button>
            <el-button v-if="row.deliveryStatus !== 'PAUSED'" link type="warning" @click.stop="changeStatus(row, 'PAUSED')">{{ lt('暂停', '暫停', 'Pause') }}</el-button>
            <el-button v-if="row.deliveryStatus !== 'DRAFT'" link @click.stop="changeStatus(row, 'DRAFT')">{{ lt('转草稿', '轉草稿', 'Back to Draft') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="previewVisible" :title="lt('通知详情预览', '通知詳情預覽', 'Notice Preview')" size="38%">
      <template v-if="previewRow">
        <el-descriptions :column="1" border>
          <el-descriptions-item :label="lt('分类', '分類', 'Category')">{{ categoryText(previewRow.category) }}</el-descriptions-item>
          <el-descriptions-item :label="lt('受众', '受眾', 'Audience')">{{ audienceText(previewRow.audienceRole) }}</el-descriptions-item>
          <el-descriptions-item :label="lt('投放状态', '投放狀態', 'Delivery')">{{ effectiveText(previewRow.effectiveStatus) }}</el-descriptions-item>
          <el-descriptions-item :label="lt('标题', '標題', 'Title')">{{ previewRow.title }}</el-descriptions-item>
          <el-descriptions-item :label="lt('内容', '內容', 'Body')">{{ previewRow.body }}</el-descriptions-item>
          <el-descriptions-item :label="lt('动作链接', '動作連結', 'Action URL')">{{ previewRow.actionUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('开始时间', '開始時間', 'Start At')">{{ formatDate(previewRow.startAt) || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('结束时间', '結束時間', 'End At')">{{ formatDate(previewRow.endAt) || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div class="detail-actions">
          <el-button type="primary" @click="openEdit(previewRow)">{{ lt('编辑通知', '編輯通知', 'Edit Notice') }}</el-button>
          <el-button v-if="previewRow.deliveryStatus !== 'PUBLISHED'" type="success" @click="changeStatus(previewRow, 'PUBLISHED')">{{ lt('直接发布', '直接發布', 'Publish Now') }}</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="dialogVisible" :title="editingId ? lt('编辑通知', '編輯通知', 'Edit Notice') : lt('新建通知', '新增通知', 'New Notice')" width="640px">
      <el-form :model="form" label-position="top">
        <el-form-item :label="lt('通知分类', '通知分類', 'Category')">
          <el-select v-model="form.category">
            <el-option label="SYSTEM" value="SYSTEM" />
            <el-option label="REVIEW" value="REVIEW" />
            <el-option label="PUBLISH" value="PUBLISH" />
            <el-option label="RISK" value="RISK" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('受众范围', '受眾範圍', 'Audience')">
          <el-select v-model="form.audienceRole">
            <el-option label="ALL" value="ALL" />
            <el-option label="DEVELOPER" value="DEVELOPER" />
            <el-option label="PLAYER" value="PLAYER" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.audienceRole === 'DEVELOPER'" :label="lt('目标用户 ID', '目標使用者 ID', 'Target User ID')">
          <el-input v-model.number="form.targetUserId" />
        </el-form-item>
        <el-form-item :label="lt('投放状态', '投放狀態', 'Delivery Status')">
          <el-select v-model="form.deliveryStatus">
            <el-option label="DRAFT" value="DRAFT" />
            <el-option label="PUBLISHED" value="PUBLISHED" />
            <el-option label="PAUSED" value="PAUSED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('标题', '標題', 'Title')">
          <el-input v-model="form.title" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item :label="lt('内容', '內容', 'Body')">
          <el-input v-model="form.body" type="textarea" :rows="5" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item :label="lt('动作链接', '動作連結', 'Action URL')">
          <el-input v-model="form.actionUrl" />
        </el-form-item>
        <el-form-item :label="lt('开始时间', '開始時間', 'Start At')">
          <el-date-picker v-model="form.startAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item :label="lt('结束时间', '結束時間', 'End At')">
          <el-date-picker v-model="form.endAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="submit">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createOpsNotice, getOpsNotices, updateOpsNotice, updateOpsNoticeStatus } from '../../api'
import { useI18nLite } from '../../i18n'

const { lt } = useI18nLite()
const loading = ref(false)
const loadError = ref('')
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const notices = ref([])
const previewVisible = ref(false)
const previewRow = ref(null)
const categoryFilter = ref('')
const statusFilter = ref('')
const keyword = ref('')
const form = reactive({
  audienceRole: 'DEVELOPER',
  targetUserId: null,
  category: 'SYSTEM',
  deliveryStatus: 'DRAFT',
  title: '',
  body: '',
  actionUrl: '',
  startAt: '',
  endAt: ''
})

const resetForm = () => {
  editingId.value = null
  Object.assign(form, {
    audienceRole: 'DEVELOPER',
    targetUserId: null,
    category: 'SYSTEM',
    deliveryStatus: 'DRAFT',
    title: '',
    body: '',
    actionUrl: '',
    startAt: '',
    endAt: ''
  })
}

const loadNotices = async () => {
  try {
    loading.value = true
    loadError.value = ''
    const res = await getOpsNotices()
    notices.value = res.data || []
  } catch (error) {
    loadError.value = error.message || lt('加载通知失败', '載入通知失敗', 'Failed to load notices')
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

const filteredNotices = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  return notices.value.filter((row) => {
    if (categoryFilter.value && row.category !== categoryFilter.value) return false
    if (statusFilter.value && row.effectiveStatus !== statusFilter.value) return false
    if (!key) return true
    return [row.title, row.body, row.audienceRole, row.targetUserId]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(key))
  })
})

const effectiveCount = (status) => notices.value.filter((item) => item.effectiveStatus === status).length
const draftPausedCount = computed(() => notices.value.filter((item) => ['DRAFT', 'PAUSED'].includes(item.effectiveStatus)).length)

const openCreate = () => {
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row) => {
  editingId.value = row.id
  Object.assign(form, {
    audienceRole: row.audienceRole,
    targetUserId: row.targetUserId,
    category: row.category,
    deliveryStatus: row.deliveryStatus,
    title: row.title,
    body: row.body,
    actionUrl: row.actionUrl || '',
    startAt: row.startAt,
    endAt: row.endAt
  })
  dialogVisible.value = true
}

const openPreview = (row) => {
  previewRow.value = row
  previewVisible.value = true
}

const submit = async () => {
  try {
    saving.value = true
    if (editingId.value) {
      await updateOpsNotice(editingId.value, { ...form })
    } else {
      await createOpsNotice({ ...form })
    }
    dialogVisible.value = false
    ElMessage.success(lt('通知已保存', '通知已儲存', 'Notice saved'))
    await loadNotices()
  } catch (error) {
    ElMessage.error(error.message || lt('保存通知失败', '儲存通知失敗', 'Failed to save notice'))
  } finally {
    saving.value = false
  }
}

const changeStatus = async (row, deliveryStatus) => {
  try {
    await ElMessageBox.confirm(
      lt('该操作会直接改变开发者后台的通知展示状态，请再次确认。', '此操作會直接改變開發者後台的通知展示狀態，請再次確認。', 'This will immediately change the notice visibility in the developer portal. Please confirm again.'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { type: 'warning' }
    )
    await updateOpsNoticeStatus(row.id, { deliveryStatus })
    ElMessage.success(lt('通知状态已更新', '通知狀態已更新', 'Notice status updated'))
    await loadNotices()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('更新通知状态失败', '更新通知狀態失敗', 'Failed to update notice status'))
    }
  }
}

const categoryText = (value) => ({ SYSTEM: 'SYSTEM', REVIEW: 'REVIEW', PUBLISH: 'PUBLISH', RISK: 'RISK' }[value] || value)
const categoryTagType = (value) => ({ SYSTEM: 'info', REVIEW: 'warning', PUBLISH: 'success', RISK: 'danger' }[value] || 'info')
const audienceText = (value) => ({ ALL: lt('全站', '全站', 'All'), DEVELOPER: lt('开发者', '開發者', 'Developers'), PLAYER: lt('用户', '使用者', 'Players') }[value] || value)
const effectiveText = (value) => ({ LIVE: lt('生效中', '生效中', 'Live'), SCHEDULED: lt('待生效', '待生效', 'Scheduled'), EXPIRED: lt('已过期', '已過期', 'Expired'), DRAFT: lt('草稿', '草稿', 'Draft'), PAUSED: lt('已暂停', '已暫停', 'Paused'), PUBLISHED: lt('已发布', '已發布', 'Published') }[value] || value)
const effectiveTagType = (value) => ({ LIVE: 'success', SCHEDULED: 'warning', EXPIRED: 'info', PAUSED: 'danger', DRAFT: 'info', PUBLISHED: 'success' }[value] || 'info')
const formatDate = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

onMounted(loadNotices)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 18px; }
.card-head, .head-actions { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.sub-text { color: #98a2b3; font-size: 12px; margin-top: 4px; }
.summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
.summary-item { padding: 14px 16px; border-radius: 16px; background: #f8fafc; display: flex; flex-direction: column; gap: 6px; color: #475467; }
.summary-item strong { font-size: 18px; color: #101828; }
.detail-actions { margin-top: 16px; display: flex; gap: 10px; flex-wrap: wrap; }
</style>
