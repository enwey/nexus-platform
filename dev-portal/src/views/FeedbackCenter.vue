<template>
  <div class="ticket-page">
    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ lt('反馈工单', '回饋工單', 'Support Tickets') }}</div>
            <div class="panel-subtitle">{{ lt('围绕审核、发布、运行时和治理问题建立正式工单。', '圍繞審核、發布、運行時和治理問題建立正式工單。', 'Open formal tickets for review, release, runtime, and governance issues.') }}</div>
          </div>
          <el-button type="primary" @click="openCreateDialog">{{ lt('新建工单', '新增工單', 'Create Ticket') }}</el-button>
        </div>
      </template>

      <el-table :data="tickets" v-loading="loading" :empty-text="lt('暂无工单', '暫無工單', 'No tickets')">
        <el-table-column prop="ticketNo" :label="lt('工单号', '工單號', 'Ticket No.')" width="180" />
        <el-table-column prop="title" :label="lt('标题', '標題', 'Title')" min-width="220" />
        <el-table-column :label="lt('类型', '類型', 'Type')" width="130">
          <template #default="{ row }"><el-tag>{{ ticketTypeText(row.ticketType) }}</el-tag></template>
        </el-table-column>
        <el-table-column :label="lt('优先级', '優先級', 'Priority')" width="110">
          <template #default="{ row }"><el-tag :type="priorityTagType(row.priority)">{{ row.priority }}</el-tag></template>
        </el-table-column>
        <el-table-column :label="lt('状态', '狀態', 'Status')" width="150">
          <template #default="{ row }"><el-tag :type="statusTagType(row.ticketStatus)">{{ statusText(row.ticketStatus) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="relatedAppId" :label="lt('关联 App', '關聯 App', 'Related App')" width="160" />
        <el-table-column :label="lt('最近回复', '最近回覆', 'Last Reply')" width="180">
          <template #default="{ row }">{{ formatDate(row.lastReplyAt || row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="140">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">{{ lt('查看', '查看', 'View') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="createVisible" :title="lt('新建反馈工单', '新增回饋工單', 'Create Ticket')" width="620px">
      <el-form :model="createForm" label-position="top">
        <el-form-item :label="lt('问题类型', '問題類型', 'Ticket Type')">
          <el-select v-model="createForm.ticketType" style="width: 100%">
            <el-option value="REVIEW" :label="lt('审核问题', '審核問題', 'Review')" />
            <el-option value="RELEASE" :label="lt('发布问题', '發布問題', 'Release')" />
            <el-option value="RUNTIME" :label="lt('运行时问题', '運行時問題', 'Runtime')" />
            <el-option value="BILLING" :label="lt('结算问题', '結算問題', 'Billing')" />
            <el-option value="GOVERNANCE" :label="lt('治理申诉', '治理申訴', 'Governance')" />
            <el-option value="OTHER" :label="lt('其他', '其他', 'Other')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('优先级', '優先級', 'Priority')">
          <el-select v-model="createForm.priority" style="width: 100%">
            <el-option value="LOW" label="LOW" />
            <el-option value="NORMAL" label="NORMAL" />
            <el-option value="HIGH" label="HIGH" />
            <el-option value="URGENT" label="URGENT" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('关联 App ID', '關聯 App ID', 'Related App ID')">
          <el-input v-model="createForm.relatedAppId" maxlength="64" />
        </el-form-item>
        <el-form-item :label="lt('标题', '標題', 'Title')">
          <el-input v-model="createForm.title" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item :label="lt('问题描述', '問題描述', 'Description')">
          <el-input v-model="createForm.content" type="textarea" :rows="6" maxlength="2000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="submitTicket">{{ lt('提交工单', '提交工單', 'Submit') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="activeTicket?.ticketNo || lt('工单详情', '工單詳情', 'Ticket Detail')" width="860px">
      <div v-if="activeTicket" class="detail-block">
        <el-descriptions :column="2" border>
          <el-descriptions-item :label="lt('标题', '標題', 'Title')">{{ activeTicket.title }}</el-descriptions-item>
          <el-descriptions-item :label="lt('状态', '狀態', 'Status')">{{ statusText(activeTicket.ticketStatus) }}</el-descriptions-item>
          <el-descriptions-item :label="lt('类型', '類型', 'Type')">{{ ticketTypeText(activeTicket.ticketType) }}</el-descriptions-item>
          <el-descriptions-item :label="lt('优先级', '優先級', 'Priority')">{{ activeTicket.priority }}</el-descriptions-item>
          <el-descriptions-item :label="lt('关联 App', '關聯 App', 'Related App')">{{ activeTicket.relatedAppId || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('结案说明', '結案說明', 'Resolution')">{{ activeTicket.resolutionSummary || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('问题描述', '問題描述', 'Description')" :span="2">{{ activeTicket.content }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <el-table :data="messages" v-loading="messageLoading" :empty-text="lt('暂无消息记录', '暫無訊息記錄', 'No messages')">
        <el-table-column prop="senderRole" :label="lt('发送方', '發送方', 'Sender')" width="140">
          <template #default="{ row }">{{ row.senderRole === 'ADMIN' ? lt('平台运营', '平台營運', 'Platform Ops') : lt('开发者', '開發者', 'Developer') }}</template>
        </el-table-column>
        <el-table-column prop="messageType" :label="lt('类型', '類型', 'Type')" width="120" />
        <el-table-column prop="content" :label="lt('内容', '內容', 'Content')" min-width="360" show-overflow-tooltip />
        <el-table-column :label="lt('时间', '時間', 'Time')" width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <div class="reply-box">
        <el-input v-model="replyText" type="textarea" :rows="3" :placeholder="lt('继续补充问题进展或回复平台追问', '繼續補充問題進展或回覆平台追問', 'Add more details or reply to platform follow-up')" />
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">{{ lt('关闭', '關閉', 'Close') }}</el-button>
        <el-button type="primary" :loading="replySaving" :disabled="!canReply" @click="submitReply">{{ lt('发送回复', '發送回覆', 'Send Reply') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createMyTicket, createMyTicketMessage, getMyTicketMessages, getMyTickets } from '../api'
import { useI18nLite } from '../i18n'
import { formatDate } from '../utils/portal'

const { lt } = useI18nLite()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const messageLoading = ref(false)
const replySaving = ref(false)
const createVisible = ref(false)
const detailVisible = ref(false)
const tickets = ref([])
const messages = ref([])
const activeTicket = ref(null)
const replyText = ref('')

const createForm = reactive({
  ticketType: 'REVIEW',
  priority: 'NORMAL',
  relatedAppId: '',
  title: '',
  content: ''
})

const canReply = computed(() => {
  return activeTicket.value && !['RESOLVED', 'CLOSED'].includes(activeTicket.value.ticketStatus)
})

const loadTickets = async () => {
  loading.value = true
  try {
    const res = await getMyTickets()
    tickets.value = res.data || []
    await syncRouteTicket()
  } catch (error) {
    ElMessage.error(error.message || lt('加载工单失败', '載入工單失敗', 'Failed to load tickets'))
  } finally {
    loading.value = false
  }
}

const syncRouteTicket = async () => {
  const ticketId = Number(route.params.ticketId)
  if (!ticketId) {
    activeTicket.value = null
    detailVisible.value = false
    return
  }
  const match = tickets.value.find((item) => item.id === ticketId)
  if (match) {
    await openDetail(match, { syncRoute: false })
  }
}

const openCreateDialog = () => {
  createForm.ticketType = 'REVIEW'
  createForm.priority = 'NORMAL'
  createForm.relatedAppId = ''
  createForm.title = ''
  createForm.content = ''
  createVisible.value = true
}

const submitTicket = async () => {
  try {
    saving.value = true
    await createMyTicket({ ...createForm })
    createVisible.value = false
    ElMessage.success(lt('工单已提交', '工單已提交', 'Ticket submitted'))
    await loadTickets()
  } catch (error) {
    ElMessage.error(error.message || lt('提交工单失败', '提交工單失敗', 'Failed to submit ticket'))
  } finally {
    saving.value = false
  }
}

const openDetail = async (row, options = {}) => {
  const { syncRoute = true } = options
  activeTicket.value = row
  detailVisible.value = true
  replyText.value = ''
  messageLoading.value = true
  try {
    const res = await getMyTicketMessages(row.id)
    messages.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || lt('加载工单消息失败', '載入工單訊息失敗', 'Failed to load ticket messages'))
  } finally {
    messageLoading.value = false
  }
  if (syncRoute && route.params.ticketId !== String(row.id)) {
    router.push(`/support/tickets/${row.id}`)
  }
}

const submitReply = async () => {
  if (!activeTicket.value) return
  try {
    replySaving.value = true
    await createMyTicketMessage(activeTicket.value.id, { content: replyText.value })
    replyText.value = ''
    ElMessage.success(lt('回复已发送', '回覆已發送', 'Reply sent'))
    await Promise.all([loadTickets(), openDetail(activeTicket.value)])
  } catch (error) {
    ElMessage.error(error.message || lt('发送回复失败', '發送回覆失敗', 'Failed to send reply'))
  } finally {
    replySaving.value = false
  }
}

const ticketTypeText = (value) => ({
  REVIEW: lt('审核问题', '審核問題', 'Review'),
  RELEASE: lt('发布问题', '發布問題', 'Release'),
  RUNTIME: lt('运行时问题', '運行時問題', 'Runtime'),
  BILLING: lt('结算问题', '結算問題', 'Billing'),
  GOVERNANCE: lt('治理申诉', '治理申訴', 'Governance'),
  OTHER: lt('其他', '其他', 'Other')
}[value] || value || '-')

const statusText = (value) => ({
  OPEN: lt('待受理', '待受理', 'Open'),
  IN_PROGRESS: lt('处理中', '處理中', 'In Progress'),
  WAITING_DEVELOPER: lt('待开发者回复', '待開發者回覆', 'Waiting for Developer'),
  RESOLVED: lt('已解决', '已解決', 'Resolved'),
  CLOSED: lt('已关闭', '已關閉', 'Closed')
}[value] || value || '-')

const statusTagType = (value) => ({
  OPEN: 'warning',
  IN_PROGRESS: 'primary',
  WAITING_DEVELOPER: 'info',
  RESOLVED: 'success',
  CLOSED: 'danger'
}[value] || 'info')

const priorityTagType = (value) => ({
  LOW: 'info',
  NORMAL: '',
  HIGH: 'warning',
  URGENT: 'danger'
}[value] || 'info')

onMounted(loadTickets)
watch(() => route.params.ticketId, async () => {
  await syncRouteTicket()
})
watch(detailVisible, (visible) => {
  if (!visible && route.name === 'DeveloperTicketDetail') {
    router.push('/support/tickets')
  }
})
</script>

<style scoped>
.ticket-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 18px; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.panel-title { font-size: 17px; font-weight: 800; color: #111827; }
.panel-subtitle { margin-top: 6px; color: #667085; font-size: 13px; }
.detail-block { margin-bottom: 14px; }
.reply-box { margin-top: 14px; }
@media (max-width: 900px) {
  .head-row { flex-direction: column; }
}
</style>
