<template>
  <div class="pro-page">
    <el-card>
      <template #header>
        <div class="head-row">
          <div>
            <div class="module-title">{{ titleText }}</div>
            <div class="module-subtitle">{{ descriptionText }}</div>
          </div>
          <div class="actions">
            <el-button :loading="loading" @click="loadWorkspace">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
            <el-button :disabled="!selectedIds.length" @click="batchChangeStatus('DRAFT')">{{ lt('转草稿', '轉草稿', 'Move to Draft') }}</el-button>
            <el-button :disabled="!selectedIds.length" @click="batchChangeStatus('PAUSED')">{{ lt('批量暂停', '批量暫停', 'Batch Pause') }}</el-button>
            <el-button type="success" :disabled="!selectedIds.length" @click="batchChangeStatus('PUBLISHED')">{{ lt('批量发布', '批量發佈', 'Batch Publish') }}</el-button>
            <el-button type="primary" @click="openCreate">{{ lt('创建广告', '建立廣告', 'Create Banner') }}</el-button>
            <el-button type="success" :loading="saving" @click="saveList">{{ lt('保存列表', '保存列表', 'Save List') }}</el-button>
          </div>
        </div>
      </template>

      <div class="toolbar">
        <el-input v-model="filters.keyword" clearable :placeholder="lt('搜索标题、AppID 或游戏名', '搜尋標題、AppID 或遊戲名', 'Search by title, AppID, or game name')" style="width: 320px" />
        <el-select v-model="filters.status" clearable :placeholder="lt('全部状态', '全部狀態', 'All statuses')" style="width: 180px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="lt(item.label[0], item.label[1], item.label[2])" :value="item.value" />
        </el-select>
        <div class="stats">
          <span>{{ lt('总数', '總數', 'Total') }} {{ bannerList.length }}</span>
          <span>{{ lt('在线', '在線', 'Live') }} {{ stateCounts.LIVE }}</span>
          <span>{{ lt('排期中', '排期中', 'Scheduled') }} {{ stateCounts.SCHEDULED }}</span>
          <span>{{ lt('草稿', '草稿', 'Draft') }} {{ stateCounts.DRAFT }}</span>
        </div>
      </div>

      <el-table :data="filteredList" v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column type="index" width="60" :label="lt('序号', '序號', 'No.')" />
        <el-table-column prop="appId" label="AppID" min-width="150" />
        <el-table-column :label="lt('游戏', '遊戲', 'Game')" min-width="180">
          <template #default="{ row }">{{ gameNameById(row.appId) }}</template>
        </el-table-column>
        <el-table-column prop="badgeText" :label="lt('角标', '角標', 'Badge')" width="120" />
        <el-table-column prop="title" :label="lt('标题', '標題', 'Title')" min-width="200" />
        <el-table-column prop="subtitle" :label="lt('副标题', '副標題', 'Subtitle')" min-width="220" />
        <el-table-column :label="lt('投放状态', '投放狀態', 'Delivery State')" width="120">
          <template #default="{ row }">
            <el-tag :type="stateTagType(derivePlacementState(row))">{{ stateText(derivePlacementState(row)) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('配置状态', '配置狀態', 'Config Status')" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : row.status === 'PAUSED' ? 'warning' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('开始时间', '開始時間', 'Start At')" width="170">
          <template #default="{ row }">{{ displayDateTime(row.startAt) }}</template>
        </el-table-column>
        <el-table-column :label="lt('结束时间', '結束時間', 'End At')" width="170">
          <template #default="{ row }">{{ displayDateTime(row.endAt) }}</template>
        </el-table-column>
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row, findActualIndex(row))">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
            <el-button link @click="moveItem(findActualIndex(row), -1)" :disabled="findActualIndex(row) === 0">{{ lt('上移', '上移', 'Move Up') }}</el-button>
            <el-button link @click="moveItem(findActualIndex(row), 1)" :disabled="findActualIndex(row) === bannerList.length - 1">{{ lt('下移', '下移', 'Move Down') }}</el-button>
            <el-button link type="danger" @click="removeItem(findActualIndex(row))">{{ lt('删除', '刪除', 'Delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editorVisible" :title="editingIndex === null ? lt('创建广告位', '建立廣告位', 'Create Banner') : lt('编辑广告位', '編輯廣告位', 'Edit Banner')" width="720px">
      <el-form :model="editorForm" label-width="130px">
        <el-form-item :label="lt('关联游戏', '關聯遊戲', 'Related Game')" required>
          <el-select v-model="editorForm.appId" filterable style="width: 100%">
            <el-option v-for="item in gameOptions" :key="item.appId" :label="`${item.name} (${item.appId})`" :value="item.appId" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('配置状态', '配置狀態', 'Config Status')">
          <el-select v-model="editorForm.status" style="width: 100%">
            <el-option v-for="item in statusOptions" :key="item.value" :label="lt(item.label[0], item.label[1], item.label[2])" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('角标', '角標', 'Badge')"><el-input v-model="editorForm.badgeText" /></el-form-item>
        <el-form-item :label="lt('标题', '標題', 'Title')"><el-input v-model="editorForm.title" /></el-form-item>
        <el-form-item :label="lt('副标题', '副標題', 'Subtitle')"><el-input v-model="editorForm.subtitle" /></el-form-item>
        <el-form-item :label="lt('封面图 URL', '封面圖 URL', 'Cover URL')"><el-input v-model="editorForm.coverUrl" /></el-form-item>
        <el-form-item :label="lt('开始时间', '開始時間', 'Start At')"><el-input v-model="editorForm.startAt" type="datetime-local" /></el-form-item>
        <el-form-item :label="lt('结束时间', '結束時間', 'End At')"><el-input v-model="editorForm.endAt" type="datetime-local" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" @click="confirmEdit">{{ lt('确认', '確認', 'Confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { batchUpdateDiscoverContentItemsStatus } from '../../api'
import { useI18nLite } from '../../i18n'
import {
  cloneTopBanner,
  derivePlacementState,
  emptyTopBanner,
  fetchDiscoverWorkspace,
  placementStatusOptions,
  saveDiscoverSection
} from '../../utils/discoverOps'

const props = defineProps({
  bannerType: { type: String, default: 'game' },
  bannerLabel: { type: Array, default: () => ['广告位管理', '廣告位管理', 'Banner Management'] },
  bannerDescription: { type: Array, default: () => ['独立维护广告位。', '獨立維護廣告位。', 'Manage banner placements independently.'] }
})

const { lt } = useI18nLite()
const loading = ref(false)
const saving = ref(false)
const editorVisible = ref(false)
const editingIndex = ref(null)
const selectedIds = ref([])
const gameOptions = ref([])
const bannerList = ref([])
const statusOptions = placementStatusOptions

const filters = reactive({
  keyword: '',
  status: ''
})

const editorForm = reactive(emptyTopBanner())

const titleText = computed(() => lt(props.bannerLabel[0], props.bannerLabel[1], props.bannerLabel[2]))
const descriptionText = computed(() => lt(props.bannerDescription[0], props.bannerDescription[1], props.bannerDescription[2]))

const filteredList = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return bannerList.value.filter((item) => {
    const state = derivePlacementState(item)
    const matchesStatus = !filters.status || state === filters.status || item.status === filters.status
    if (!matchesStatus) return false
    if (!keyword) return true
    const target = [item.appId, item.title, item.subtitle, item.badgeText, gameNameById(item.appId)].join(' ').toLowerCase()
    return target.includes(keyword)
  })
})

const stateCounts = computed(() => bannerList.value.reduce((acc, item) => {
  const state = derivePlacementState(item)
  acc[state] = (acc[state] || 0) + 1
  return acc
}, { LIVE: 0, SCHEDULED: 0, DRAFT: 0, PAUSED: 0, EXPIRED: 0 }))

const assignEditor = (item = {}) => {
  Object.assign(editorForm, cloneTopBanner(item))
}

const gameNameById = (appId) => {
  const target = gameOptions.value.find((item) => item.appId === appId)
  return target?.name || '-'
}

const displayDateTime = (value) => value || '-'

const stateText = (state) => ({
  LIVE: lt('在线', '在線', 'Live'),
  SCHEDULED: lt('排期中', '排期中', 'Scheduled'),
  DRAFT: lt('草稿', '草稿', 'Draft'),
  PAUSED: lt('暂停', '暫停', 'Paused'),
  EXPIRED: lt('已过期', '已過期', 'Expired')
}[state] || state)

const stateTagType = (state) => ({
  LIVE: 'success',
  SCHEDULED: 'warning',
  DRAFT: 'info',
  PAUSED: '',
  EXPIRED: 'danger'
}[state] || 'info')

const findActualIndex = (row) => bannerList.value.findIndex((item) => item === row || (item.id && item.id === row.id))

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map((item) => item.id).filter(Boolean)
}

const loadWorkspace = async () => {
  loading.value = true
  try {
    const workspace = await fetchDiscoverWorkspace()
    gameOptions.value = workspace.gameOptions
    bannerList.value = props.bannerType === 'discover' ? workspace.discoverTopBanners : workspace.gameTopBanners
  } catch (error) {
    ElMessage.error(error.message || lt('加载广告位失败', '載入廣告位失敗', 'Failed to load banners'))
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  editingIndex.value = null
  assignEditor(emptyTopBanner())
  editorVisible.value = true
}

const openEdit = (row, index) => {
  editingIndex.value = index
  assignEditor(row)
  editorVisible.value = true
}

const confirmEdit = () => {
  if (!editorForm.appId || !editorForm.title.trim()) {
    ElMessage.warning(lt('请填写完整广告信息', '請填寫完整廣告資訊', 'Please complete the banner fields'))
    return
  }
  const payload = cloneTopBanner(editorForm)
  if (editingIndex.value === null) {
    bannerList.value = [...bannerList.value, payload]
  } else {
    bannerList.value.splice(editingIndex.value, 1, payload)
    bannerList.value = [...bannerList.value]
  }
  editorVisible.value = false
}

const moveItem = (index, offset) => {
  const nextIndex = index + offset
  if (index < 0 || nextIndex < 0 || nextIndex >= bannerList.value.length) return
  const next = [...bannerList.value]
  ;[next[index], next[nextIndex]] = [next[nextIndex], next[index]]
  bannerList.value = next
}

const removeItem = async (index) => {
  try {
    await ElMessageBox.confirm(
      lt('确认删除该广告位？', '確認刪除此廣告位？', 'Delete this banner?'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { type: 'warning' }
    )
    bannerList.value.splice(index, 1)
    bannerList.value = [...bannerList.value]
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('删除广告位失败', '刪除廣告位失敗', 'Failed to delete banner'))
    }
  }
}

const saveList = async () => {
  saving.value = true
  try {
    const patch = props.bannerType === 'discover'
      ? { discoverTopBanners: bannerList.value.map(cloneTopBanner) }
      : { gameTopBanners: bannerList.value.map(cloneTopBanner) }
    const savedWorkspace = await saveDiscoverSection(patch)
    bannerList.value = props.bannerType === 'discover' ? savedWorkspace.discoverTopBanners : savedWorkspace.gameTopBanners
    ElMessage.success(lt('广告位列表已保存', '廣告位列表已保存', 'Banner list saved'))
  } catch (error) {
    ElMessage.error(error.message || lt('保存广告位失败', '保存廣告位失敗', 'Failed to save banners'))
  } finally {
    saving.value = false
  }
}

const batchChangeStatus = async (status) => {
  try {
    await ElMessageBox.confirm(
      lt('确认批量更新所选广告位状态？', '確認批量更新所選廣告位狀態？', 'Confirm batch updating selected banners?'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { type: 'warning' }
    )
    const savedWorkspace = await batchUpdateDiscoverContentItemsStatus({ itemIds: selectedIds.value, status })
    bannerList.value = props.bannerType === 'discover'
      ? (savedWorkspace.data?.discoverTopBanners || savedWorkspace.discoverTopBanners || [])
      : (savedWorkspace.data?.gameTopBanners || savedWorkspace.gameTopBanners || [])
    selectedIds.value = []
    ElMessage.success(lt('广告位状态已更新', '廣告位狀態已更新', 'Banner status updated'))
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('批量更新广告位失败', '批量更新廣告位失敗', 'Failed to batch update banners'))
    }
  }
}

onMounted(loadWorkspace)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.head-row { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.module-title { font-size: 18px; font-weight: 700; color: #101828; }
.module-subtitle { margin-top: 6px; color: #667085; font-size: 13px; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; }
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 16px; flex-wrap: wrap; }
.stats { display: flex; gap: 16px; color: #475467; font-size: 13px; }
</style>
