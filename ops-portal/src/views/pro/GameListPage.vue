<template>
  <div class="pro-page">
    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ lt('游戏列表', '遊戲列表', 'Game List') }}</div>
            <div class="panel-subtitle">{{ lt('统一查看平台游戏资产，从列表进入详情页处理资料、展示状态和版本。', '統一查看平台遊戲資產，從列表進入詳情頁處理資料、展示狀態和版本。', 'Inspect all game assets and move into detail pages for metadata, visibility, and version handling.') }}</div>
          </div>
          <div class="actions">
            <el-input v-model="keyword" clearable class="keyword-input" :placeholder="lt('搜索游戏名称或 AppID', '搜尋遊戲名稱或 AppID', 'Search by game name or AppID')" />
            <el-select v-model="statusFilter" clearable style="width: 150px">
              <el-option :label="lt('全部状态', '全部狀態', 'All Statuses')" value="" />
              <el-option :label="lt('草稿', '草稿', 'Draft')" value="DRAFT" />
              <el-option :label="lt('处理中', '處理中', 'Processing')" value="PROCESSING" />
              <el-option :label="lt('待审核', '待審核', 'Pending')" value="PENDING" />
              <el-option :label="lt('已通过', '已通過', 'Approved')" value="APPROVED" />
              <el-option :label="lt('已驳回', '已駁回', 'Rejected')" value="REJECTED" />
            </el-select>
            <el-select v-model="categoryFilter" clearable filterable style="width: 180px">
              <el-option :label="lt('全部分类', '全部分類', 'All Categories')" value="" />
              <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.name" />
            </el-select>
            <el-button type="primary" @click="openUploadDialog">{{ lt('直接上传游戏', '直接上傳遊戲', 'Direct Upload Game') }}</el-button>
            <el-button @click="router.push('/pro/game-version/versions/list')">{{ lt('查看版本列表', '查看版本列表', 'Open Version List') }}</el-button>
            <el-button @click="router.push('/pro/game-categories')">{{ lt('分类管理', '分類管理', 'Category Management') }}</el-button>
            <el-button :loading="loading" @click="loadGames">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <div class="status-grid">
        <div v-for="item in statusCards" :key="item.key" class="status-card" @click="statusFilter = item.filter">
          <div class="status-label">{{ item.label }}</div>
          <div class="status-value">{{ item.value }}</div>
          <div class="status-tip">{{ item.tip }}</div>
        </div>
      </div>

      <el-table :data="filteredGames" v-loading="loading">
        <el-table-column prop="name" :label="lt('游戏名称', '遊戲名稱', 'Game Name')" min-width="180" />
        <el-table-column prop="appId" label="AppID" min-width="180" />
        <el-table-column prop="version" :label="lt('当前版本', '當前版本', 'Current Version')" width="120" />
        <el-table-column :label="lt('分类', '分類', 'Category')" min-width="140">
          <template #default="{ row }">{{ row.category || '-' }}</template>
        </el-table-column>
        <el-table-column :label="lt('状态', '狀態', 'Status')" width="140">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('前端状态', '前端狀態', 'Frontend State')" width="150">
          <template #default="{ row }">
            <el-tag :type="getFrontendStateType(row.frontendState)">{{ getFrontendStateText(row.frontendState) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('展示控制', '展示控制', 'Visibility Control')" width="150">
          <template #default="{ row }">
            <el-tag :type="getVisibilityType(row.visibilityStatus)">{{ getVisibilityText(row.visibilityStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('渠道治理', '渠道治理', 'Channel Governance')" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ formatScopeSummary(row.governanceScope?.channelMode, row.governanceScope?.channels) }}</template>
        </el-table-column>
        <el-table-column :label="lt('地区治理', '地區治理', 'Region Governance')" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ formatScopeSummary(row.governanceScope?.regionMode, row.governanceScope?.regions) }}</template>
        </el-table-column>
        <el-table-column :label="lt('影响等级', '影響等級', 'Impact Level')" width="140">
          <template #default="{ row }">
            <el-tag :type="getImpactType(row.impactLevel)">{{ getImpactText(row.impactLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ lt('查看详情', '查看詳情', 'View Detail') }}</el-button>
            <el-button link @click="openVersions(row)">{{ lt('查看版本', '查看版本', 'Versions') }}</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="success" @click="approvePendingGame(row)">{{ lt('通过', '通過', 'Approve') }}</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="danger" @click="rejectPendingGame(row)">{{ lt('驳回', '駁回', 'Reject') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="uploadVisible" :title="lt('运营后台直接上传游戏', '營運後台直接上傳遊戲', 'Direct Game Upload')" width="640px">
      <el-alert
        :title="lt('该入口用于运营直接创建游戏资产并上传 ZIP 包，不依赖开发者后台。上传后会进入包处理流程。', '此入口用於營運直接建立遊戲資產並上傳 ZIP 包，不依賴開發者後台。上傳後會進入包處理流程。', 'This entry lets operations create a game asset and upload a ZIP package directly without the developer portal. The package then enters processing.')"
        type="info"
        :closable="false"
        show-icon
        class="upload-alert"
      />
      <el-form :model="uploadForm" label-width="110px" class="upload-form">
        <el-form-item :label="lt('游戏名称', '遊戲名稱', 'Game Name')" required>
          <el-input v-model.trim="uploadForm.name" maxlength="80" show-word-limit />
        </el-form-item>
        <el-form-item :label="lt('游戏描述', '遊戲描述', 'Description')">
          <el-input v-model.trim="uploadForm.description" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item :label="lt('ZIP 包', 'ZIP 包', 'ZIP Package')" required>
          <el-upload
            ref="uploadRef"
            drag
            :limit="1"
            :auto-upload="false"
            :show-file-list="true"
            accept=".zip,application/zip,application/x-zip-compressed"
            :on-change="handleUploadFileChange"
            :on-remove="handleUploadFileRemove"
          >
            <div class="upload-drag-copy">
              {{ lt('拖拽 ZIP 包到此处，或点击选择文件', '拖曳 ZIP 包到此處，或點擊選擇檔案', 'Drop a ZIP package here or click to select a file') }}
            </div>
            <template #tip>
              <div class="upload-tip">{{ lt('仅支持 ZIP 包，前端限制 512MB，服务端会继续校验格式、大小和重复提交。', '僅支援 ZIP 包，前端限制 512MB，服務端會繼續校驗格式、大小和重複提交。', 'Only ZIP packages are allowed. The frontend caps files at 512MB, and the server will continue to validate type, size, and duplicate submission.') }}</div>
            </template>
          </el-upload>
          <div v-if="uploadFileName" class="upload-selected">{{ lt('当前文件', '目前檔案', 'Current File') }}: {{ uploadFileName }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="uploadSaving" @click="submitDirectUpload">{{ lt('开始上传', '開始上傳', 'Start Upload') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveGame, getOpsGameCategories, getOpsGames, rejectGame, uploadGamePackage } from '../../api'
import { useI18nLite } from '../../i18n'

const route = useRoute()
const router = useRouter()
const { lt } = useI18nLite()

const loading = ref(false)
const uploadSaving = ref(false)
const uploadVisible = ref(false)
const uploadRef = ref(null)
const uploadRawFile = ref(null)
const uploadFileName = ref('')
const games = ref([])
const categoryOptions = ref([])
const keyword = ref('')
const statusFilter = ref('')
const categoryFilter = ref('')
const uploadForm = reactive({
  name: '',
  description: ''
})

const filteredGames = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return games.value.filter((game) => {
    const matchedKeyword = !query
      || (game.name || '').toLowerCase().includes(query)
      || (game.appId || '').toLowerCase().includes(query)
    const matchedStatus = !statusFilter.value || game.status === statusFilter.value
    const matchedCategory = !categoryFilter.value || game.category === categoryFilter.value
    return matchedKeyword && matchedStatus && matchedCategory
  })
})

const statusCards = computed(() => {
  const countBy = (status) => games.value.filter((item) => item.status === status).length
  return [
    { key: 'all', filter: '', label: lt('全部游戏', '全部遊戲', 'All Games'), value: games.value.length, tip: lt('平台已接入总量', '平台已接入總量', 'Total onboarded') },
    { key: 'pending', filter: 'PENDING', label: lt('待审核', '待審核', 'Pending'), value: countBy('PENDING'), tip: lt('需要审核处理', '需要審核處理', 'Needs review') },
    { key: 'approved', filter: 'APPROVED', label: lt('已通过', '已通過', 'Approved'), value: countBy('APPROVED'), tip: lt('可进入前端流转', '可進入前端流轉', 'Ready for frontend circulation') },
    { key: 'rejected', filter: 'REJECTED', label: lt('已驳回', '已駁回', 'Rejected'), value: countBy('REJECTED'), tip: lt('需开发者修复后重提', '需開發者修復後重提', 'Needs fixes and resubmission') }
  ]
})

const loadCategories = async () => {
  const res = await getOpsGameCategories()
  categoryOptions.value = Array.isArray(res.data) ? res.data : []
}

const loadGames = async () => {
  loading.value = true
  try {
    const res = await getOpsGames({
      status: statusFilter.value || undefined,
      category: categoryFilter.value || undefined,
      keyword: keyword.value || undefined
    })
    games.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    ElMessage.error(error.message || lt('加载游戏列表失败', '載入遊戲列表失敗', 'Failed to load game list'))
  } finally {
    loading.value = false
  }
}

const promptReason = async (title) => {
  const { value } = await ElMessageBox.prompt(
    lt('请输入审核原因，至少 2 个字符', '請輸入審核原因，至少 2 個字元', 'Please input a review reason with at least 2 characters'),
    title,
    {
      confirmButtonText: lt('确认', '確認', 'Confirm'),
      cancelButtonText: lt('取消', '取消', 'Cancel'),
      inputPattern: /^.{2,}$/u,
      inputErrorMessage: lt('审核原因至少需要 2 个字符', '審核原因至少需要 2 個字元', 'Reason must be at least 2 characters')
    }
  )
  return value.trim()
}

const approvePendingGame = async (row) => {
  try {
    const reason = await promptReason(lt(`审核通过：${row.name}`, `審核通過：${row.name}`, `Approve: ${row.name}`))
    await approveGame(row.id, reason)
    ElMessage.success(lt('审核已通过', '審核已通過', 'Approval completed'))
    await loadGames()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('审核通过失败', '審核通過失敗', 'Failed to approve game'))
    }
  }
}

const rejectPendingGame = async (row) => {
  try {
    const reason = await promptReason(lt(`驳回版本：${row.name}`, `駁回版本：${row.name}`, `Reject: ${row.name}`))
    await rejectGame(row.id, reason)
    ElMessage.success(lt('已驳回', '已駁回', 'Rejected'))
    await loadGames()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('驳回失败', '駁回失敗', 'Failed to reject'))
    }
  }
}

const openDetail = (row) => {
  router.push({ name: 'OpsGameDetail', params: { gameId: row.id } })
}

const openVersions = (row) => {
  router.push({ name: 'OpsVersionList', query: { gameId: String(row.id) } })
}

const openUploadDialog = () => {
  uploadVisible.value = true
  uploadForm.name = ''
  uploadForm.description = ''
  uploadRawFile.value = null
  uploadFileName.value = ''
  uploadRef.value?.clearFiles?.()
}

const handleUploadFileChange = (file) => {
  const raw = file?.raw || file
  if (!raw) return false
  const fileName = String(raw.name || '').toLowerCase()
  if (!fileName.endsWith('.zip')) {
    ElMessage.warning(lt('请上传 ZIP 格式文件', '請上傳 ZIP 格式檔案', 'Please upload a ZIP file'))
    uploadRef.value?.clearFiles?.()
    uploadRawFile.value = null
    uploadFileName.value = ''
    return false
  }
  if (raw.size > 512 * 1024 * 1024) {
    ElMessage.warning(lt('文件大小不能超过 512MB', '檔案大小不能超過 512MB', 'The file size must not exceed 512MB'))
    uploadRef.value?.clearFiles?.()
    uploadRawFile.value = null
    uploadFileName.value = ''
    return false
  }
  uploadRawFile.value = raw
  uploadFileName.value = raw.name || ''
  return false
}

const handleUploadFileRemove = () => {
  uploadRawFile.value = null
  uploadFileName.value = ''
}

const submitDirectUpload = async () => {
  if (uploadForm.name.trim().length < 2) {
    ElMessage.warning(lt('游戏名称至少 2 个字符', '遊戲名稱至少 2 個字元', 'Game name must be at least 2 characters'))
    return
  }
  if (!uploadRawFile.value) {
    ElMessage.warning(lt('请先选择 ZIP 包', '請先選擇 ZIP 包', 'Please select a ZIP package first'))
    return
  }
  const formData = new FormData()
  formData.append('file', uploadRawFile.value)
  formData.append('name', uploadForm.name.trim())
  if (uploadForm.description.trim()) {
    formData.append('description', uploadForm.description.trim())
  }
  try {
    uploadSaving.value = true
    const response = await uploadGamePackage(formData)
    const gameId = response?.data?.id
    ElMessage.success(lt('游戏上传任务已创建', '遊戲上傳任務已建立', 'Game upload task created'))
    uploadVisible.value = false
    await loadGames()
    if (gameId) {
      router.push({ name: 'OpsGameDetail', params: { gameId } })
    }
  } catch (error) {
    ElMessage.error(error.message || lt('直接上传失败', '直接上傳失敗', 'Direct upload failed'))
  } finally {
    uploadSaving.value = false
  }
}

const getStatusText = (status) => ({
  PROCESSING: lt('处理中', '處理中', 'Processing'),
  DRAFT: lt('草稿', '草稿', 'Draft'),
  PENDING: lt('待审核', '待審核', 'Pending'),
  APPROVED: lt('已通过', '已通過', 'Approved'),
  REJECTED: lt('已驳回', '已駁回', 'Rejected')
}[status] || status || lt('未知', '未知', 'Unknown'))

const getStatusType = (status) => ({
  PROCESSING: 'warning',
  DRAFT: 'info',
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger'
}[status] || 'info')

const getFrontendStateText = (status) => ({
  APPROVED: lt('可运营', '可營運', 'Operable'),
  PENDING: lt('待审核中', '待審核中', 'Under Review'),
  DRAFT: lt('未开放', '未開放', 'Not Open'),
  REJECTED: lt('已拦截', '已攔截', 'Blocked'),
  PROCESSING: lt('处理中', '處理中', 'Processing'),
  HIDDEN: lt('已隐藏', '已隱藏', 'Hidden')
}[status] || lt('未知', '未知', 'Unknown'))

const getFrontendStateType = (status) => ({
  APPROVED: 'success',
  PENDING: 'warning',
  DRAFT: 'info',
  REJECTED: 'danger',
  PROCESSING: '',
  HIDDEN: 'warning',
  BLOCKED: 'danger',
  OPERABLE: 'success',
  UNDER_REVIEW: 'warning',
  NOT_OPEN: 'info'
}[status] || 'info')

const getVisibilityText = (status) => ({
  VISIBLE: lt('正常展示', '正常展示', 'Visible'),
  HIDDEN: lt('隐藏', '隱藏', 'Hidden'),
  BLOCKED: lt('封禁', '封禁', 'Blocked')
}[status] || status || '-')

const getVisibilityType = (status) => ({
  VISIBLE: 'success',
  HIDDEN: 'warning',
  BLOCKED: 'danger'
}[status] || 'info')

const getImpactText = (level) => ({
  NONE: lt('无影响', '無影響', 'None'),
  LOW: lt('低', '低', 'Low'),
  MEDIUM: lt('中', '中', 'Medium'),
  HIGH: lt('高', '高', 'High'),
  CRITICAL: lt('极高', '極高', 'Critical')
}[level] || level || '-')

const getImpactType = (level) => ({
  NONE: 'info',
  LOW: 'success',
  MEDIUM: 'warning',
  HIGH: 'danger',
  CRITICAL: 'danger'
}[level] || 'info')

const formatScopeSummary = (mode, values) => {
  if (!mode || mode === 'ALL') return lt('不限制', '不限制', 'No restriction')
  const label = mode === 'ALLOWLIST'
    ? lt('白名单', '白名單', 'Allowlist')
    : lt('黑名单', '黑名單', 'Blocklist')
  const joined = Array.isArray(values) && values.length ? values.join(', ') : lt('未配置', '未配置', 'Not set')
  return `${label}: ${joined}`
}

watch(() => route.query.status, (status) => {
  statusFilter.value = typeof status === 'string' ? status : ''
}, { immediate: true })

onMounted(async () => {
  await Promise.all([loadCategories(), loadGames()])
})
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 18px; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.panel-title { font-size: 17px; font-weight: 800; color: #101828; }
.panel-subtitle { margin-top: 6px; color: #667085; font-size: 13px; }
.actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.keyword-input { width: 280px; }
.status-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; margin-bottom: 14px; }
.status-card { border: 1px solid #ebeef5; border-radius: 16px; padding: 14px 16px; cursor: pointer; background: #fafcff; }
.status-card:hover { background: #f3f8ff; }
.status-label { color: #667085; font-size: 13px; }
.status-value { margin-top: 10px; font-size: 26px; font-weight: 800; color: #101828; }
.status-tip { margin-top: 8px; color: #98a2b3; font-size: 12px; }
.upload-alert { margin-bottom: 16px; }
.upload-form { padding-top: 4px; }
.upload-drag-copy { font-size: 14px; color: #475467; }
.upload-tip { color: #98a2b3; font-size: 12px; line-height: 1.5; }
.upload-selected { margin-top: 10px; color: #344054; font-size: 13px; }
@media (max-width: 920px) { .head-row { flex-direction: column; } }
</style>
