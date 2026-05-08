<template>
  <div class="pro-page">
    <el-alert v-if="loadError" type="error" :closable="false" :title="loadError" />

    <div class="summary-grid">
      <div class="summary-item"><span>{{ lt('工单总数', '工單總數', 'Tickets') }}</span><strong>{{ tickets.length }}</strong></div>
      <div class="summary-item"><span>{{ lt('处理中', '處理中', 'In Progress') }}</span><strong>{{ statusCount('IN_PROGRESS') }}</strong></div>
      <div class="summary-item"><span>{{ lt('待开发者回复', '待開發者回覆', 'Waiting Developer') }}</span><strong>{{ statusCount('WAITING_DEVELOPER') }}</strong></div>
      <div class="summary-item"><span>{{ lt('高优先级', '高優先級', 'High Priority') }}</span><strong>{{ highPriorityCount }}</strong></div>
    </div>

    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ lt('工单中心', '工單中心', 'Ticket Center') }}</div>
            <div class="panel-subtitle">{{ lt('统一处理开发者围绕审核、发布、运行时和治理的正式工单。', '統一處理開發者圍繞審核、發布、運行時和治理的正式工單。', 'Handle developer tickets for review, release, runtime, and governance in one place.') }}</div>
          </div>
          <div class="head-actions">
            <el-select v-model="statusFilter" clearable style="width: 180px">
              <el-option :label="lt('全部状态', '全部狀態', 'All Statuses')" value="" />
              <el-option value="OPEN" :label="lt('待受理', '待受理', 'Open')" />
              <el-option value="IN_PROGRESS" :label="lt('处理中', '處理中', 'In Progress')" />
              <el-option value="WAITING_DEVELOPER" :label="lt('待开发者回复', '待開發者回覆', 'Waiting for Developer')" />
              <el-option value="RESOLVED" :label="lt('已解决', '已解決', 'Resolved')" />
              <el-option value="CLOSED" :label="lt('已关闭', '已關閉', 'Closed')" />
            </el-select>
            <el-input v-model.trim="keyword" clearable style="width: 260px" :placeholder="lt('搜索工单号 / 标题 / 开发者', '搜尋工單號 / 標題 / 開發者', 'Search ticket / title / developer')" />
            <el-button @click="loadTickets">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredTickets" v-loading="loading" :empty-text="lt('暂无工单', '暫無工單', 'No tickets')" @row-click="openDetail">
        <el-table-column prop="ticketNo" :label="lt('工单号', '工單號', 'Ticket No.')" width="180" />
        <el-table-column prop="developerName" :label="lt('开发者', '開發者', 'Developer')" min-width="160" />
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
        <el-table-column prop="relatedAppId" :label="lt('关联 App', '關聯 App', 'Related App')" width="150" />
        <el-table-column prop="assigneeAdminId" :label="lt('处理人', '處理人', 'Assignee')" width="100" />
        <el-table-column :label="lt('最近回复', '最近回覆', 'Last Reply')" width="180">
          <template #default="{ row }">{{ formatDate(row.lastReplyAt || row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="180">
          <template #default="{ row }">
            <el-button link @click.stop="openDetail(row)">{{ lt('详情', '詳情', 'Detail') }}</el-button>
            <el-button type="primary" link @click.stop="openDetail(row)">{{ lt('处理', '處理', 'Handle') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" :title="activeTicket?.ticketNo || lt('工单详情', '工單詳情', 'Ticket Detail')" width="920px">
      <div v-if="activeTicket" class="detail-block">
        <div class="summary-grid">
          <div class="summary-item compact"><span>{{ lt('当前状态', '當前狀態', 'Current Status') }}</span><strong>{{ statusText(activeTicket.ticketStatus) }}</strong></div>
          <div class="summary-item compact"><span>{{ lt('优先级', '優先級', 'Priority') }}</span><strong>{{ activeTicket.priority }}</strong></div>
          <div class="summary-item compact"><span>{{ lt('处理人', '處理人', 'Assignee') }}</span><strong>{{ activeTicket.assigneeAdminId || '-' }}</strong></div>
          <div class="summary-item compact"><span>{{ lt('最近回复', '最近回覆', 'Last Reply') }}</span><strong>{{ formatDate(activeTicket.lastReplyAt || activeTicket.updatedAt) }}</strong></div>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item :label="lt('开发者', '開發者', 'Developer')">{{ activeTicket.developerName }} / {{ activeTicket.developerEmail }}</el-descriptions-item>
          <el-descriptions-item :label="lt('标题', '標題', 'Title')">{{ activeTicket.title }}</el-descriptions-item>
          <el-descriptions-item :label="lt('类型', '類型', 'Type')">{{ ticketTypeText(activeTicket.ticketType) }}</el-descriptions-item>
          <el-descriptions-item :label="lt('关联 App', '關聯 App', 'Related App')">{{ activeTicket.relatedAppId || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('问题描述', '問題描述', 'Description')" :span="2">{{ activeTicket.content }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <el-form :model="statusForm" label-width="120px" class="status-form">
        <el-form-item :label="lt('工单状态', '工單狀態', 'Ticket Status')">
          <el-select v-model="statusForm.ticketStatus" style="width: 100%">
            <el-option value="IN_PROGRESS" :label="lt('处理中', '處理中', 'In Progress')" />
            <el-option value="WAITING_DEVELOPER" :label="lt('待开发者回复', '待開發者回覆', 'Waiting for Developer')" />
            <el-option value="RESOLVED" :label="lt('已解决', '已解決', 'Resolved')" />
            <el-option value="CLOSED" :label="lt('已关闭', '已關閉', 'Closed')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('处理人 ID', '處理人 ID', 'Assignee ID')">
          <el-input-number v-model="statusForm.assigneeAdminId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="lt('结案说明', '結案說明', 'Resolution')">
          <el-input v-model="statusForm.resolutionSummary" type="textarea" :rows="3" maxlength="256" show-word-limit />
        </el-form-item>
      </el-form>
      <el-table :data="messages" v-loading="messageLoading" :empty-text="lt('暂无消息记录', '暫無訊息記錄', 'No messages')">
        <el-table-column prop="senderRole" :label="lt('发送方', '發送方', 'Sender')" width="140" />
        <el-table-column prop="messageType" :label="lt('类型', '類型', 'Type')" width="120" />
        <el-table-column prop="content" :label="lt('内容', '內容', 'Content')" min-width="360" show-overflow-tooltip />
        <el-table-column :label="lt('时间', '時間', 'Time')" width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <div class="reply-box">
        <el-input v-model="replyText" type="textarea" :rows="3" :placeholder="lt('回复开发者并同步处理进展', '回覆開發者並同步處理進展', 'Reply to developer and update handling progress')" />
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">{{ lt('关闭', '關閉', 'Close') }}</el-button>
        <el-button :loading="statusSaving" @click="submitStatus">{{ lt('更新状态', '更新狀態', 'Update Status') }}</el-button>
        <el-button type="primary" :loading="replySaving" @click="submitReply">{{ lt('发送回复', '發送回覆', 'Send Reply') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createOpsTicketMessage, getOpsTicketMessages, getOpsTickets, updateOpsTicketStatus } from '../../api'
import { useI18nLite } from '../../i18n'

const { lt } = useI18nLite()
const loading = ref(false)
const loadError = ref('')
const messageLoading = ref(false)
const statusSaving = ref(false)
const replySaving = ref(false)
const detailVisible = ref(false)
const tickets = ref([])
const messages = ref([])
const activeTicket = ref(null)
const replyText = ref('')
const statusFilter = ref('')
const keyword = ref('')

const statusForm = reactive({
  ticketStatus: 'IN_PROGRESS',
  assigneeAdminId: 1,
  resolutionSummary: ''
})

const loadTickets = async () => {
  loading.value = true
  try {
    loadError.value = ''
    const res = await getOpsTickets()
    tickets.value = res.data || []
  } catch (error) {
    loadError.value = error.message || lt('加载工单失败', '載入工單失敗', 'Failed to load tickets')
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

const filteredTickets = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  return tickets.value.filter((row) => {
    if (statusFilter.value && row.ticketStatus !== statusFilter.value) return false
    if (!key) return true
    return [row.ticketNo, row.title, row.developerName, row.developerEmail, row.relatedAppId]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(key))
  })
})

const statusCount = (status) => tickets.value.filter((item) => item.ticketStatus === status).length
const highPriorityCount = computed(() => tickets.value.filter((item) => ['HIGH', 'URGENT'].includes(item.priority)).length)

const openDetail = async (row) => {
  activeTicket.value = row
  statusForm.ticketStatus = row.ticketStatus === 'OPEN' ? 'IN_PROGRESS' : row.ticketStatus
  statusForm.assigneeAdminId = row.assigneeAdminId || 1
  statusForm.resolutionSummary = row.resolutionSummary || ''
  detailVisible.value = true
  replyText.value = ''
  messageLoading.value = true
  try {
    const res = await getOpsTicketMessages(row.id)
    messages.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || lt('加载工单消息失败', '載入工單訊息失敗', 'Failed to load ticket messages'))
  } finally {
    messageLoading.value = false
  }
}

const submitStatus = async () => {
  if (!activeTicket.value) return
  try {
    statusSaving.value = true
    await updateOpsTicketStatus(activeTicket.value.id, { ...statusForm })
    ElMessage.success(lt('工单状态已更新', '工單狀態已更新', 'Ticket status updated'))
    await loadTickets()
    const refreshed = tickets.value.find((item) => item.id === activeTicket.value.id) || activeTicket.value
    activeTicket.value = refreshed
  } catch (error) {
    ElMessage.error(error.message || lt('更新工单状态失败', '更新工單狀態失敗', 'Failed to update ticket status'))
  } finally {
    statusSaving.value = false
  }
}

const submitReply = async () => {
  if (!activeTicket.value) return
  try {
    replySaving.value = true
    await createOpsTicketMessage(activeTicket.value.id, { content: replyText.value })
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

const formatDate = (value) => {
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

onMounted(loadTickets)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 18px; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.head-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.panel-title { font-size: 17px; font-weight: 800; color: #101828; }
.panel-subtitle { margin-top: 6px; color: #667085; font-size: 13px; }
.detail-block { margin-bottom: 14px; }
.status-form { margin-bottom: 14px; }
.reply-box { margin-top: 14px; }
.summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
.summary-item { padding: 14px 16px; border-radius: 16px; background: #f8fafc; display: flex; flex-direction: column; gap: 6px; color: #475467; }
.summary-item.compact { margin-bottom: 12px; }
.summary-item strong { font-size: 18px; color: #101828; }
@media (max-width: 920px) { .head-row { flex-direction: column; } }
</style>
