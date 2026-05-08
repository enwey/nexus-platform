<template>
  <div class="catalog-page">
    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ mode === 'list' ? lt('发现内容列表', '發現內容列表', 'Discover Content') : detailTitle }}</div>
            <div class="panel-subtitle">{{ lt('维护发现页 Hero 与游戏库顶部 Banner，两类内容独立排序并可分别发布。', '維護發現頁 Hero 與遊戲庫頂部 Banner，兩類內容獨立排序並可分別發佈。', 'Manage discover hero banners and library top banners with independent ordering and publishing.') }}</div>
          </div>
          <div class="actions">
            <el-button @click="loadWorkspace(true)">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
            <template v-if="mode === 'list'">
              <el-button :disabled="!selectedIds.length" @click="batchChangeStatus('DRAFT')">{{ lt('转草稿', '轉草稿', 'Move to Draft') }}</el-button>
              <el-button :disabled="!selectedIds.length" @click="batchChangeStatus('PAUSED')">{{ lt('批量暂停', '批量暫停', 'Batch Pause') }}</el-button>
              <el-button type="success" :disabled="!selectedIds.length" @click="batchChangeStatus('PUBLISHED')">{{ lt('批量发布', '批量發佈', 'Batch Publish') }}</el-button>
              <el-button type="primary" @click="openCreate">{{ lt('新建内容', '新增內容', 'Create Content') }}</el-button>
              <el-button type="success" :loading="saving" @click="saveLists">{{ lt('保存排序', '保存排序', 'Save Order') }}</el-button>
            </template>
            <template v-else>
              <el-button type="danger" plain @click="removeDetail">{{ lt('删除内容', '刪除內容', 'Delete') }}</el-button>
              <el-button type="success" plain @click="saveAsStatus('PUBLISHED')">{{ lt('发布', '發佈', 'Publish') }}</el-button>
              <el-button plain @click="saveAsStatus('PAUSED')">{{ lt('暂停', '暫停', 'Pause') }}</el-button>
              <el-button type="primary" :loading="saving" @click="saveDetail">{{ lt('保存修改', '保存修改', 'Save Changes') }}</el-button>
            </template>
          </div>
        </div>
      </template>

      <template v-if="mode === 'list'">
        <div class="toolbar">
          <el-input v-model="filters.keyword" clearable :placeholder="lt('搜索标题、AppID 或游戏名', '搜尋標題、AppID 或遊戲名', 'Search by title, AppID, or game')" style="width: 320px" />
          <el-select v-model="filters.contentType" clearable :placeholder="lt('全部类型', '全部類型', 'All types')" style="width: 200px">
            <el-option v-for="item in contentTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <el-table :data="filteredList" v-loading="loading" row-key="id" @selection-change="handleSelectionChange" @row-click="openDetail">
          <el-table-column type="selection" width="48" />
          <el-table-column prop="contentTypeLabel" :label="lt('内容类型', '內容類型', 'Type')" width="160" />
          <el-table-column prop="appId" label="AppID" min-width="140" />
          <el-table-column :label="lt('游戏', '遊戲', 'Game')" min-width="180">
            <template #default="{ row }">{{ gameNameById(row.appId) }}</template>
          </el-table-column>
          <el-table-column prop="title" :label="lt('标题', '標題', 'Title')" min-width="220" />
          <el-table-column prop="badgeText" :label="lt('角标', '角標', 'Badge')" width="120" />
          <el-table-column :label="lt('投放状态', '投放狀態', 'State')" width="120">
            <template #default="{ row }"><el-tag :type="stateTagType(derivePlacementState(row))">{{ stateText(derivePlacementState(row)) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="status" :label="lt('配置状态', '配置狀態', 'Config Status')" width="120" />
          <el-table-column :label="lt('操作', '操作', 'Actions')" width="260" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="openDetail(row)">{{ lt('查看详情', '查看詳情', 'View detail') }}</el-button>
              <el-button link @click.stop="moveItem(row, -1)">{{ lt('上移', '上移', 'Move Up') }}</el-button>
              <el-button link @click.stop="moveItem(row, 1)">{{ lt('下移', '下移', 'Move Down') }}</el-button>
              <el-button link type="danger" @click.stop="removeByRow(row)">{{ lt('删除', '刪除', 'Delete') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template v-else-if="detailItem">
        <el-row :gutter="16">
          <el-col :xs="24" :lg="14">
            <el-form :model="detailForm" label-width="120px">
              <el-form-item :label="lt('内容类型', '內容類型', 'Type')" required>
                <el-select v-model="detailForm.contentType" style="width: 100%">
                  <el-option v-for="item in contentTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item :label="lt('关联游戏', '關聯遊戲', 'Related Game')" required>
                <el-select v-model="detailForm.appId" filterable style="width: 100%">
                  <el-option v-for="item in gameOptions" :key="item.appId" :label="`${item.name} (${item.appId})`" :value="item.appId" />
                </el-select>
              </el-form-item>
              <el-form-item :label="lt('配置状态', '配置狀態', 'Config Status')">
                <el-select v-model="detailForm.status" style="width: 100%">
                  <el-option v-for="item in statusOptions" :key="item.value" :label="lt(item.label[0], item.label[1], item.label[2])" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item :label="lt('角标', '角標', 'Badge')"><el-input v-model="detailForm.badgeText" /></el-form-item>
              <el-form-item :label="lt('标题', '標題', 'Title')" required><el-input v-model="detailForm.title" /></el-form-item>
              <el-form-item :label="lt('副标题', '副標題', 'Subtitle')"><el-input v-model="detailForm.subtitle" /></el-form-item>
              <el-form-item :label="lt('封面图 URL', '封面圖 URL', 'Cover URL')"><el-input v-model="detailForm.coverUrl" /></el-form-item>
              <el-form-item :label="lt('开始时间', '開始時間', 'Start At')"><el-input v-model="detailForm.startAt" type="datetime-local" /></el-form-item>
              <el-form-item :label="lt('结束时间', '結束時間', 'End At')"><el-input v-model="detailForm.endAt" type="datetime-local" /></el-form-item>
            </el-form>
          </el-col>
          <el-col :xs="24" :lg="10">
            <el-card shadow="never" class="preview-card">
              <template #header>{{ lt('展示预览', '展示預覽', 'Display Preview') }}</template>
              <div class="hero-preview">
                <div class="hero-badge">{{ detailForm.badgeText || lt('角标', '角標', 'Badge') }}</div>
                <div class="hero-title">{{ detailForm.title || lt('标题', '標題', 'Title') }}</div>
                <div class="hero-subtitle">{{ detailForm.subtitle || lt('副标题', '副標題', 'Subtitle') }}</div>
              </div>
            </el-card>
            <el-card shadow="never" class="preview-card">
              <template #header>{{ lt('发布预览', '發佈預覽', 'Publish Preview') }}</template>
              <div v-if="previewLines.length" class="preview-lines">
                <div v-for="line in previewLines" :key="line[0]" class="preview-line">
                  <span>{{ line[0] }}</span>
                  <strong>{{ line[1] }}</strong>
                </div>
              </div>
              <el-empty v-else :description="lt('暂无发布预览', '暫無發佈預覽', 'No publish preview')" />
            </el-card>
            <el-card shadow="never" class="preview-card">
              <template #header>{{ lt('快速发布', '快速發佈', 'Quick Publish') }}</template>
              <el-form label-width="96px">
                <el-form-item :label="lt('生效时间', '生效時間', 'Effective At')">
                  <el-input v-model="publishForm.effectiveAt" type="datetime-local" />
                </el-form-item>
                <el-form-item :label="lt('发布原因', '發佈原因', 'Reason')">
                  <el-input v-model="publishForm.reason" type="textarea" :rows="3" />
                </el-form-item>
              </el-form>
              <el-button type="success" :loading="publishing" @click="submitPublish">{{ lt('创建发布单', '建立發佈單', 'Create Publish Order') }}</el-button>
            </el-card>
          </el-col>
        </el-row>
      </template>
    </el-card>

    <el-dialog v-model="editorVisible" :title="lt('新建发现内容', '新增發現內容', 'Create Discover Content')" width="720px">
      <el-form :model="detailForm" label-width="120px">
        <el-form-item :label="lt('内容类型', '內容類型', 'Type')" required>
          <el-select v-model="detailForm.contentType" style="width: 100%">
            <el-option v-for="item in contentTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('关联游戏', '關聯遊戲', 'Related Game')" required>
          <el-select v-model="detailForm.appId" filterable style="width: 100%">
            <el-option v-for="item in gameOptions" :key="item.appId" :label="`${item.name} (${item.appId})`" :value="item.appId" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('标题', '標題', 'Title')" required><el-input v-model="detailForm.title" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" @click="confirmCreate">{{ lt('创建并保存', '建立並保存', 'Create & Save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18nLite } from '../../../i18n'
import { batchUpdateDiscoverContentItemsStatus } from '../../../api'
import {
  cloneTopBanner,
  derivePlacementState,
  emptyTopBanner,
  fetchDiscoverPublishPreview,
  fetchDiscoverWorkspace,
  placementStatusOptions,
  saveDiscoverSection,
  createDiscoverPublishOrder
} from '../../../utils/discoverOps'
import { buildPreviewLines, getScopeCodeFromContentType } from './catalogCustomShared'

const props = defineProps({
  mode: { type: String, default: 'list' },
  detailId: { type: [String, Number], default: null }
})

const router = useRouter()
const { lt } = useI18nLite()
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const editorVisible = ref(false)
const selectedIds = ref([])
const workspace = ref(null)
const previewData = ref(null)
const filters = reactive({ keyword: '', contentType: '' })
const detailForm = reactive({ ...emptyTopBanner(), contentType: 'DISCOVER_HERO' })
const publishForm = reactive({ effectiveAt: '', reason: '' })
const statusOptions = placementStatusOptions
const contentTypeOptions = [
  { value: 'DISCOVER_HERO', label: lt('发现页 Hero', '發現頁 Hero', 'Discover Hero') },
  { value: 'LIBRARY_TOP_BANNER', label: lt('游戏库顶部 Banner', '遊戲庫頂部 Banner', 'Library Top Banner') }
]

const gameOptions = computed(() => workspace.value?.gameOptions || [])
const contentList = computed(() => [
  ...(workspace.value?.discoverTopBanners || []).map((item) => ({ ...item, id: `discover-banner-${item.id ?? item.appId}`, contentType: 'DISCOVER_HERO', contentTypeLabel: lt('发现页 Hero', '發現頁 Hero', 'Discover Hero') })),
  ...(workspace.value?.gameTopBanners || []).map((item) => ({ ...item, id: `library-banner-${item.id ?? item.appId}`, contentType: 'LIBRARY_TOP_BANNER', contentTypeLabel: lt('游戏库顶部 Banner', '遊戲庫頂部 Banner', 'Library Top Banner') }))
])
const detailItem = computed(() => contentList.value.find((item) => String(item.id) === String(props.detailId)) || null)
const detailTitle = computed(() => detailItem.value?.title || '-')
const previewLines = computed(() => buildPreviewLines(previewData.value))
const filteredList = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return contentList.value.filter((item) => {
    const matchesType = !filters.contentType || item.contentType === filters.contentType
    if (!matchesType) return false
    if (!keyword) return true
    return [item.title, item.subtitle, item.badgeText, item.appId, gameNameById(item.appId)].join(' ').toLowerCase().includes(keyword)
  })
})

const syncDetailForm = async () => {
  if (!detailItem.value) return
  Object.assign(detailForm, cloneTopBanner(detailItem.value), { contentType: detailItem.value.contentType })
  const scopeCode = getScopeCodeFromContentType(detailItem.value.contentType)
  previewData.value = scopeCode ? await fetchDiscoverPublishPreview(scopeCode) : null
}

const gameNameById = (appId) => gameOptions.value.find((item) => item.appId === appId)?.name || '-'
const stateText = (state) => ({ LIVE: lt('在线', '在線', 'Live'), SCHEDULED: lt('排期中', '排期中', 'Scheduled'), DRAFT: lt('草稿', '草稿', 'Draft'), PAUSED: lt('暂停', '暫停', 'Paused'), EXPIRED: lt('已过期', '已過期', 'Expired') }[state] || state)
const stateTagType = (state) => ({ LIVE: 'success', SCHEDULED: 'warning', DRAFT: 'info', PAUSED: '', EXPIRED: 'danger' }[state] || 'info')

const loadWorkspace = async () => {
  loading.value = true
  try {
    workspace.value = await fetchDiscoverWorkspace()
    if (props.mode === 'detail') await syncDetailForm()
  } catch (error) {
    ElMessage.error(error.message || lt('加载发现内容失败', '載入發現內容失敗', 'Failed to load discover content'))
  } finally {
    loading.value = false
  }
}

const saveLists = async () => {
  saving.value = true
  try {
    workspace.value = await saveDiscoverSection({
      discoverTopBanners: workspace.value.discoverTopBanners.map(cloneTopBanner),
      gameTopBanners: workspace.value.gameTopBanners.map(cloneTopBanner)
    })
    ElMessage.success(lt('发现内容排序已保存', '發現內容排序已保存', 'Discover content order saved'))
  } catch (error) {
    ElMessage.error(error.message || lt('保存排序失败', '保存排序失敗', 'Failed to save order'))
  } finally {
    saving.value = false
  }
}

const updateCollectionByType = (contentType, updater) => {
  const key = contentType === 'DISCOVER_HERO' ? 'discoverTopBanners' : 'gameTopBanners'
  workspace.value = { ...workspace.value, [key]: updater([...(workspace.value?.[key] || [])]) }
}

const moveItem = (row, offset) => {
  const key = row.contentType === 'DISCOVER_HERO' ? 'discoverTopBanners' : 'gameTopBanners'
  const list = [...(workspace.value?.[key] || [])]
  const index = list.findIndex((item) => (item.id && row.id?.includes(item.id)) || item.appId === row.appId)
  const nextIndex = index + offset
  if (index < 0 || nextIndex < 0 || nextIndex >= list.length) return
  ;[list[index], list[nextIndex]] = [list[nextIndex], list[index]]
  workspace.value = { ...workspace.value, [key]: list }
}

const removeByRow = async (row) => {
  try {
    await ElMessageBox.confirm(
      lt('确认删除该发现内容？', '確認刪除此發現內容？', 'Delete this discover content?'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { type: 'warning' }
    )
    updateCollectionByType(row.contentType, (list) => list.filter((item) => !((item.id && row.id?.includes(item.id)) || item.appId === row.appId)))
  } catch {}
}

const batchChangeStatus = async (status) => {
  try {
    await batchUpdateDiscoverContentItemsStatus({ itemIds: selectedIds.value, status })
    await loadWorkspace()
    selectedIds.value = []
    ElMessage.success(lt('发现内容状态已更新', '發現內容狀態已更新', 'Discover content status updated'))
  } catch (error) {
    ElMessage.error(error.message || lt('更新状态失败', '更新狀態失敗', 'Failed to update status'))
  }
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map((item) => item.id).filter(Boolean)
}

const openCreate = () => {
  Object.assign(detailForm, emptyTopBanner(), { contentType: 'DISCOVER_HERO' })
  editorVisible.value = true
}

const confirmCreate = async () => {
  if (!detailForm.appId || !detailForm.title.trim()) {
    ElMessage.warning(lt('请填写完整内容信息', '請填寫完整內容資訊', 'Please complete required fields'))
    return
  }
  updateCollectionByType(detailForm.contentType, (list) => [...list, cloneTopBanner(detailForm)])
  editorVisible.value = false
  await saveLists()
}

const openDetail = (row) => router.push({ name: 'OpsDiscoverContentDetail', params: { contentId: row.id } })

const saveDetail = async () => {
  if (!detailItem.value) return
  const previousType = detailItem.value.contentType
  const nextPayload = cloneTopBanner(detailForm)
  if (previousType === detailForm.contentType) {
    updateCollectionByType(previousType, (list) => {
      const index = list.findIndex((item) => (item.id && detailItem.value.id?.includes(item.id)) || item.appId === detailItem.value.appId)
      const next = [...list]
      next.splice(index, 1, nextPayload)
      return next
    })
  } else {
    updateCollectionByType(previousType, (list) => list.filter((item) => !((item.id && detailItem.value.id?.includes(item.id)) || item.appId === detailItem.value.appId)))
    updateCollectionByType(detailForm.contentType, (list) => [...list, nextPayload])
  }
  await saveLists()
  if (previousType !== detailForm.contentType) {
    const prefix = detailForm.contentType === 'DISCOVER_HERO' ? 'discover-banner-' : 'library-banner-'
    router.replace({ name: 'OpsDiscoverContentDetail', params: { contentId: `${prefix}${detailForm.appId}` } })
  }
  await syncDetailForm()
}

const saveAsStatus = async (status) => {
  detailForm.status = status
  await saveDetail()
}

const removeDetail = async () => {
  await removeByRow(detailItem.value)
  await saveLists()
  router.push('/pro/discover-ops/content/list')
}

const submitPublish = async () => {
  const scopeCode = getScopeCodeFromContentType(detailForm.contentType)
  if (!scopeCode) return
  publishing.value = true
  try {
    await createDiscoverPublishOrder({
      scopeCode,
      effectiveAt: publishForm.effectiveAt ? `${publishForm.effectiveAt}:00` : null,
      reason: publishForm.reason || 'Publish from discover content detail'
    })
    previewData.value = await fetchDiscoverPublishPreview(scopeCode)
    ElMessage.success(lt('发布单已创建', '發佈單已建立', 'Publish order created'))
  } catch (error) {
    ElMessage.error(error.message || lt('创建发布单失败', '建立發佈單失敗', 'Failed to create publish order'))
  } finally {
    publishing.value = false
  }
}

watch(() => props.detailId, () => { if (props.mode === 'detail') syncDetailForm() })
onMounted(() => loadWorkspace())
</script>

<style scoped>
.catalog-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card, .preview-card { border-radius: 16px; }
.head-row { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.panel-title { font-size: 18px; font-weight: 700; color: #111827; }
.panel-subtitle { margin-top: 6px; color: #6b7280; font-size: 13px; line-height: 1.6; max-width: 760px; }
.actions, .toolbar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.toolbar { margin-bottom: 16px; }
.hero-preview { min-height: 200px; border-radius: 16px; background: linear-gradient(135deg, #0f172a, #334155); color: #fff; padding: 20px; display: flex; flex-direction: column; justify-content: flex-end; gap: 8px; }
.hero-badge { font-size: 12px; opacity: .8; }
.hero-title { font-size: 24px; font-weight: 700; }
.hero-subtitle { font-size: 14px; opacity: .9; }
.preview-lines { display: flex; flex-direction: column; gap: 10px; }
.preview-line { display: flex; justify-content: space-between; gap: 16px; font-size: 13px; color: #4b5563; }
</style>
