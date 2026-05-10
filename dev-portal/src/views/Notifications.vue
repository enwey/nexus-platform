<template>
  <div class="notice-page">
    <el-card class="panel-card">
      <template #header>
        <div class="card-head">
          <span>{{ lt('平台通知', '平台通知', 'Platform Notices') }}</span>
          <el-button size="small" @click="loadNotices">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
        </div>
      </template>
      <div class="notice-summary">
        <div class="summary-pill">
          <span>{{ lt('总通知', '總通知', 'Total') }}</span>
          <strong>{{ notices.length }}</strong>
        </div>
        <div class="summary-pill">
          <span>{{ lt('发布类', '發布類', 'Publish') }}</span>
          <strong>{{ publishCount }}</strong>
        </div>
        <div class="summary-pill">
          <span>{{ lt('审核类', '審核類', 'Review') }}</span>
          <strong>{{ reviewCount }}</strong>
        </div>
      </div>
      <el-table :data="notices" v-loading="loading" :empty-text="lt('暂无通知', '暫無通知', 'No notices')">
        <el-table-column :label="lt('分类', '分類', 'Category')" width="130">
          <template #default="{ row }">
            <el-tag :type="categoryTagType(row.category)">{{ categoryText(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" :label="lt('标题', '標題', 'Title')" min-width="220" />
        <el-table-column prop="body" :label="lt('内容', '內容', 'Body')" min-width="360" show-overflow-tooltip />
        <el-table-column :label="lt('生效状态', '生效狀態', 'Effective Status')" width="130">
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
        <el-table-column :label="lt('操作', '操作', 'Action')" width="140">
          <template #default="{ row }">
            <el-button type="primary" link @click="openNotice(row)">{{ lt('查看详情', '查看詳情', 'View') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" :title="activeNotice?.title || lt('通知详情', '通知詳情', 'Notice Detail')" :width="detailDialogWidth" @close="handleCloseDetail">
      <template v-if="activeNotice">
        <el-descriptions :column="descriptionColumns" border>
          <el-descriptions-item :label="lt('分类', '分類', 'Category')">{{ categoryText(activeNotice.category) }}</el-descriptions-item>
          <el-descriptions-item :label="lt('生效状态', '生效狀態', 'Status')">{{ effectiveText(activeNotice.effectiveStatus) }}</el-descriptions-item>
          <el-descriptions-item :label="lt('开始时间', '開始時間', 'Start')">{{ formatDate(activeNotice.startAt) || '--' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('结束时间', '結束時間', 'End')">{{ formatDate(activeNotice.endAt) || '--' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('内容', '內容', 'Body')" :span="2">{{ activeNotice.body || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">{{ lt('关闭', '關閉', 'Close') }}</el-button>
        <el-button v-if="activeNotice?.actionUrl" type="primary" @click="jump(activeNotice.actionUrl)">{{ lt('打开动作链接', '打開動作連結', 'Open Action') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMyNotices } from '../api'
import { useViewport } from '../composables/useViewport'
import { useI18nLite } from '../i18n'
import { formatDate } from '../utils/portal'

const { lt } = useI18nLite()
const { isTabletOrBelow, isPhone } = useViewport()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const notices = ref([])
const detailVisible = ref(false)
const activeNotice = ref(null)

const publishCount = computed(() => notices.value.filter((item) => item.category === 'PUBLISH').length)
const reviewCount = computed(() => notices.value.filter((item) => item.category === 'REVIEW').length)
const detailDialogWidth = computed(() => (isPhone.value ? '94%' : isTabletOrBelow.value ? '88%' : '720px'))
const descriptionColumns = computed(() => (isPhone.value ? 1 : 2))

const loadNotices = async () => {
  try {
    loading.value = true
    const res = await getMyNotices()
    notices.value = res.data || []
    await syncRouteNotice()
  } catch (error) {
    ElMessage.error(error.message || lt('加载通知失败', '載入通知失敗', 'Failed to load notices'))
  } finally {
    loading.value = false
  }
}

const syncRouteNotice = async () => {
  const noticeId = Number(route.params.noticeId)
  if (!noticeId) {
    activeNotice.value = null
    detailVisible.value = false
    return
  }
  const match = notices.value.find((item) => item.id === noticeId)
  if (match) {
    activeNotice.value = match
    detailVisible.value = true
  }
}

const categoryText = (value) => {
  const map = {
    SYSTEM: lt('系统', '系統', 'System'),
    REVIEW: lt('审核', '審核', 'Review'),
    PUBLISH: lt('发布', '發布', 'Publish'),
    RISK: lt('治理', '治理', 'Governance')
  }
  return map[value] || value
}

const categoryTagType = (value) => {
  const map = {
    SYSTEM: 'info',
    REVIEW: 'warning',
    PUBLISH: 'success',
    RISK: 'danger'
  }
  return map[value] || 'info'
}

const effectiveText = (value) => {
  const map = {
    LIVE: lt('生效中', '生效中', 'Live'),
    SCHEDULED: lt('待生效', '待生效', 'Scheduled'),
    EXPIRED: lt('已过期', '已過期', 'Expired'),
    DRAFT: lt('草稿', '草稿', 'Draft'),
    PAUSED: lt('已暂停', '已暫停', 'Paused'),
    PUBLISHED: lt('已发布', '已發布', 'Published')
  }
  return map[value] || value
}

const effectiveTagType = (value) => {
  const map = {
    LIVE: 'success',
    SCHEDULED: 'warning',
    EXPIRED: 'info',
    PAUSED: 'danger'
  }
  return map[value] || 'info'
}

const jump = (url) => {
  if (url.startsWith('/')) {
    router.push(url)
    return
  }
  window.open(url, '_blank', 'noopener')
}

const openNotice = (row) => {
  if (route.params.noticeId !== String(row.id)) {
    router.push(`/support/notices/${row.id}`)
    return
  }
  activeNotice.value = row
  detailVisible.value = true
}

const handleCloseDetail = () => {
  if (route.name === 'DeveloperNoticeDetail') {
    router.push('/support/notices')
  }
}

onMounted(loadNotices)
watch(() => route.params.noticeId, async () => {
  await syncRouteNotice()
})
</script>

<style scoped>
.notice-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 20px; }
.card-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.notice-summary { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.summary-pill { padding: 14px 16px; border-radius: 16px; background: #f8fafc; display: flex; flex-direction: column; gap: 6px; color: #475467; }
.summary-pill strong { font-size: 22px; color: #111827; }
@media (max-width: 920px) {
  .notice-summary { grid-template-columns: 1fr; }
}
</style>
