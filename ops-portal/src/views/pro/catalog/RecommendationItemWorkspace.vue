<template>
  <div class="catalog-page">
    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ mode === 'list' ? lt('推荐内容列表', '推薦內容列表', 'Recommendation Items') : detailTitle }}</div>
            <div class="panel-subtitle">{{ lt('维护推荐页图文卡片，支持编辑、排序、发布与预览。', '維護推薦頁圖文卡片，支援編輯、排序、發佈與預覽。', 'Manage editorial recommendation cards with editing, ordering, publishing, and preview.') }}</div>
          </div>
          <div class="actions">
            <el-button @click="loadWorkspace(true)">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
            <template v-if="mode === 'list'">
              <el-button :disabled="!selectedIds.length" @click="batchChangeStatus('DRAFT')">{{ lt('转草稿', '轉草稿', 'Move to Draft') }}</el-button>
              <el-button :disabled="!selectedIds.length" @click="batchChangeStatus('PAUSED')">{{ lt('批量暂停', '批量暫停', 'Batch Pause') }}</el-button>
              <el-button type="success" :disabled="!selectedIds.length" @click="batchChangeStatus('PUBLISHED')">{{ lt('批量发布', '批量發佈', 'Batch Publish') }}</el-button>
              <el-button type="primary" @click="openCreate">{{ lt('新建推荐', '新增推薦', 'Create Item') }}</el-button>
              <el-button type="success" :loading="saving" @click="saveList">{{ lt('保存排序', '保存排序', 'Save Order') }}</el-button>
            </template>
            <template v-else>
              <el-button type="danger" plain @click="removeDetail">{{ lt('删除内容', '刪除內容', 'Delete Item') }}</el-button>
              <el-button type="success" plain @click="saveAsStatus('PUBLISHED')">{{ lt('发布', '發佈', 'Publish') }}</el-button>
              <el-button plain @click="saveAsStatus('PAUSED')">{{ lt('暂停', '暫停', 'Pause') }}</el-button>
              <el-button type="primary" :loading="saving" @click="saveDetail">{{ lt('保存修改', '保存修改', 'Save Changes') }}</el-button>
            </template>
          </div>
        </div>
      </template>

      <template v-if="mode === 'list'">
        <div class="toolbar">
          <el-input v-model="filters.keyword" clearable :placeholder="lt('搜索标题、分类、AppID 或游戏名', '搜尋標題、分類、AppID 或遊戲名', 'Search by title, category, AppID, or game')" style="width: 320px" />
          <el-select v-model="filters.status" clearable :placeholder="lt('全部状态', '全部狀態', 'All statuses')" style="width: 180px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="lt(item.label[0], item.label[1], item.label[2])" :value="item.value" />
          </el-select>
        </div>
        <el-table :data="filteredList" v-loading="loading" row-key="id" @selection-change="handleSelectionChange" @row-click="openDetail">
          <el-table-column type="selection" width="48" />
          <el-table-column type="index" width="60" :label="lt('序号', '序號', 'No.')" />
          <el-table-column prop="appId" label="AppID" min-width="140" />
          <el-table-column :label="lt('游戏', '遊戲', 'Game')" min-width="180">
            <template #default="{ row }">{{ gameNameById(row.appId) }}</template>
          </el-table-column>
          <el-table-column prop="cardCategory" :label="lt('分类', '分類', 'Category')" width="140" />
          <el-table-column prop="cardTitle" :label="lt('卡片标题', '卡片標題', 'Card Title')" min-width="220" />
          <el-table-column :label="lt('投放状态', '投放狀態', 'State')" width="120">
            <template #default="{ row }"><el-tag :type="stateTagType(derivePlacementState(row))">{{ stateText(derivePlacementState(row)) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="status" :label="lt('配置状态', '配置狀態', 'Config Status')" width="120" />
          <el-table-column :label="lt('操作', '操作', 'Actions')" width="260" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="openDetail(row)">{{ lt('查看详情', '查看詳情', 'View detail') }}</el-button>
              <el-button link @click.stop="moveItem(findActualIndex(row), -1)" :disabled="findActualIndex(row) === 0">{{ lt('上移', '上移', 'Move Up') }}</el-button>
              <el-button link @click.stop="moveItem(findActualIndex(row), 1)" :disabled="findActualIndex(row) === recommendList.length - 1">{{ lt('下移', '下移', 'Move Down') }}</el-button>
              <el-button link type="danger" @click.stop="removeItem(findActualIndex(row))">{{ lt('删除', '刪除', 'Delete') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template v-else-if="detailItem">
        <el-row :gutter="16">
          <el-col :xs="24" :lg="14">
            <el-form :model="detailForm" label-width="120px">
              <el-form-item :label="lt('关联游戏', '關聯遊戲', 'Related Game')" required>
                <el-select v-model="detailForm.appId" filterable style="width: 100%">
                  <el-option v-for="item in gameOptions" :key="item.appId" :label="`${item.name} (${item.appId})`" :value="item.appId" />
                </el-select>
              </el-form-item>
              <el-form-item :label="lt('卡片分类', '卡片分類', 'Card Category')" required>
                <el-select v-model="detailForm.cardCategory" filterable style="width: 100%">
                  <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.name" />
                </el-select>
              </el-form-item>
              <el-form-item :label="lt('配置状态', '配置狀態', 'Config Status')">
                <el-select v-model="detailForm.status" style="width: 100%">
                  <el-option v-for="item in statusOptions" :key="item.value" :label="lt(item.label[0], item.label[1], item.label[2])" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item :label="lt('卡片标题', '卡片標題', 'Card Title')" required><el-input v-model="detailForm.cardTitle" /></el-form-item>
              <el-form-item :label="lt('封面图 URL', '封面圖 URL', 'Cover URL')"><el-input v-model="detailForm.coverUrl" /></el-form-item>
              <el-form-item :label="lt('按钮文案', '按鈕文案', 'Action Text')"><el-input v-model="detailForm.actionText" /></el-form-item>
              <el-form-item :label="lt('开始时间', '開始時間', 'Start At')"><el-input v-model="detailForm.startAt" type="datetime-local" /></el-form-item>
              <el-form-item :label="lt('结束时间', '結束時間', 'End At')"><el-input v-model="detailForm.endAt" type="datetime-local" /></el-form-item>
              <el-form-item :label="lt('详情标签', '詳情標籤', 'Detail Tag')"><el-input v-model="detailForm.articleTag" /></el-form-item>
              <el-form-item :label="lt('详情标题', '詳情標題', 'Detail Title')"><el-input v-model="detailForm.articleTitle" /></el-form-item>
              <el-form-item :label="lt('详情正文', '詳情正文', 'Detail Body')"><el-input v-model="detailForm.articleBody" type="textarea" :rows="8" /></el-form-item>
            </el-form>
          </el-col>
          <el-col :xs="24" :lg="10">
            <el-card shadow="never" class="preview-card">
              <template #header>{{ lt('内容预览', '內容預覽', 'Preview') }}</template>
              <div class="editorial-preview">
                <div class="preview-tag">{{ detailForm.cardCategory || '-' }}</div>
                <div class="preview-title">{{ detailForm.cardTitle || lt('卡片标题', '卡片標題', 'Card Title') }}</div>
                <div class="preview-subtitle">{{ detailForm.articleTitle || lt('详情标题', '詳情標題', 'Detail Title') }}</div>
                <div class="preview-body">{{ detailForm.articleBody || lt('图文详情会显示在这里。', '圖文詳情會顯示在這裡。', 'Editorial body will appear here.') }}</div>
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

    <el-dialog v-model="editorVisible" :title="lt('新建推荐内容', '新增推薦內容', 'Create Recommendation Item')" width="760px">
      <el-form :model="detailForm" label-width="120px">
        <el-form-item :label="lt('关联游戏', '關聯遊戲', 'Related Game')" required>
          <el-select v-model="detailForm.appId" filterable style="width: 100%">
            <el-option v-for="item in gameOptions" :key="item.appId" :label="`${item.name} (${item.appId})`" :value="item.appId" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('卡片分类', '卡片分類', 'Card Category')" required>
          <el-select v-model="detailForm.cardCategory" filterable style="width: 100%">
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('卡片标题', '卡片標題', 'Card Title')" required><el-input v-model="detailForm.cardTitle" /></el-form-item>
        <el-form-item :label="lt('详情标题', '詳情標題', 'Detail Title')"><el-input v-model="detailForm.articleTitle" /></el-form-item>
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
  cloneRecommendationItem,
  derivePlacementState,
  emptyRecommendationItem,
  fetchDiscoverPublishPreview,
  fetchDiscoverWorkspace,
  placementStatusOptions,
  saveDiscoverSection,
  createDiscoverPublishOrder
} from '../../../utils/discoverOps'
import { buildPreviewLines } from './catalogCustomShared'

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
const filters = reactive({ keyword: '', status: '' })
const detailForm = reactive(emptyRecommendationItem())
const publishForm = reactive({ effectiveAt: '', reason: '' })
const statusOptions = placementStatusOptions

const recommendList = computed(() => workspace.value?.recommendList || [])
const gameOptions = computed(() => workspace.value?.gameOptions || [])
const categoryOptions = computed(() => workspace.value?.categoryOptions || [])
const detailItem = computed(() => recommendList.value.find((item) => String(item.id) === String(props.detailId)) || null)
const detailTitle = computed(() => detailItem.value?.cardTitle || '-')
const previewLines = computed(() => buildPreviewLines(previewData.value))
const filteredList = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return recommendList.value.filter((item) => {
    const state = derivePlacementState(item)
    const matchesStatus = !filters.status || filters.status === state || filters.status === item.status
    if (!matchesStatus) return false
    if (!keyword) return true
    return [item.appId, item.cardCategory, item.cardTitle, item.articleTitle, gameNameById(item.appId)].join(' ').toLowerCase().includes(keyword)
  })
})

const syncDetailForm = () => {
  Object.assign(detailForm, cloneRecommendationItem(detailItem.value || emptyRecommendationItem()))
}

const gameNameById = (appId) => gameOptions.value.find((item) => item.appId === appId)?.name || '-'
const stateText = (state) => ({ LIVE: lt('在线', '在線', 'Live'), SCHEDULED: lt('排期中', '排期中', 'Scheduled'), DRAFT: lt('草稿', '草稿', 'Draft'), PAUSED: lt('暂停', '暫停', 'Paused'), EXPIRED: lt('已过期', '已過期', 'Expired') }[state] || state)
const stateTagType = (state) => ({ LIVE: 'success', SCHEDULED: 'warning', DRAFT: 'info', PAUSED: '', EXPIRED: 'danger' }[state] || 'info')
const findActualIndex = (row) => recommendList.value.findIndex((item) => item === row || (item.id && item.id === row.id))

const loadWorkspace = async (force = false) => {
  loading.value = true
  try {
    workspace.value = await fetchDiscoverWorkspace(force)
    syncDetailForm()
    if (props.mode === 'detail') {
      previewData.value = await fetchDiscoverPublishPreview('COMMUNITY')
    }
  } catch (error) {
    ElMessage.error(error.message || lt('加载推荐内容失败', '載入推薦內容失敗', 'Failed to load recommendation items'))
  } finally {
    loading.value = false
  }
}

const saveList = async () => {
  saving.value = true
  try {
    workspace.value = await saveDiscoverSection({ recommendList: recommendList.value.map(cloneRecommendationItem) })
    ElMessage.success(lt('推荐内容顺序已保存', '推薦內容順序已保存', 'Recommendation order saved'))
  } catch (error) {
    ElMessage.error(error.message || lt('保存推荐内容失败', '保存推薦內容失敗', 'Failed to save recommendation items'))
  } finally {
    saving.value = false
  }
}

const batchChangeStatus = async (status) => {
  try {
    await ElMessageBox.confirm(
      lt('确认批量更新所选推荐内容状态？', '確認批量更新所選推薦內容狀態？', 'Confirm batch updating selected recommendation items?'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { type: 'warning' }
    )
    const response = await batchUpdateDiscoverContentItemsStatus({ itemIds: selectedIds.value, status })
    workspace.value = {
      ...workspace.value,
      recommendList: response.data?.communityItems || response.communityItems || workspace.value.recommendList
    }
    selectedIds.value = []
    ElMessage.success(lt('推荐内容状态已更新', '推薦內容狀態已更新', 'Recommendation status updated'))
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || lt('批量更新失败', '批量更新失敗', 'Failed to batch update'))
  }
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map((item) => item.id).filter(Boolean)
}

const moveItem = (index, offset) => {
  const nextIndex = index + offset
  if (nextIndex < 0 || nextIndex >= recommendList.value.length) return
  const next = [...recommendList.value]
  ;[next[index], next[nextIndex]] = [next[nextIndex], next[index]]
  workspace.value = { ...workspace.value, recommendList: next }
}

const removeItem = async (index) => {
  try {
    await ElMessageBox.confirm(
      lt('确认删除该推荐内容？', '確認刪除此推薦內容？', 'Delete this recommendation item?'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { type: 'warning' }
    )
    const next = [...recommendList.value]
    next.splice(index, 1)
    workspace.value = { ...workspace.value, recommendList: next }
  } catch {}
}

const openCreate = () => {
  if (!categoryOptions.value.length) {
    ElMessage.warning(lt('请先创建分类', '請先建立分類', 'Please create categories first'))
    return
  }
  Object.assign(detailForm, emptyRecommendationItem(), { cardCategory: categoryOptions.value[0]?.name || '' })
  editorVisible.value = true
}

const confirmCreate = async () => {
  if (!detailForm.appId || !detailForm.cardCategory || !detailForm.cardTitle.trim()) {
    ElMessage.warning(lt('请填写完整推荐信息', '請填寫完整推薦資訊', 'Please complete required fields'))
    return
  }
  workspace.value = {
    ...workspace.value,
    recommendList: [...recommendList.value, cloneRecommendationItem(detailForm)]
  }
  editorVisible.value = false
  await saveList()
}

const openDetail = (row) => router.push({ name: 'OpsRecommendationItemDetail', params: { itemId: row.id } })

const saveDetail = async () => {
  if (!detailItem.value) return
  const next = [...recommendList.value]
  const index = findActualIndex(detailItem.value)
  next.splice(index, 1, cloneRecommendationItem(detailForm))
  saving.value = true
  try {
    workspace.value = await saveDiscoverSection({ recommendList: next })
    syncDetailForm()
    previewData.value = await fetchDiscoverPublishPreview('COMMUNITY')
    ElMessage.success(lt('推荐内容已保存', '推薦內容已保存', 'Recommendation item saved'))
  } catch (error) {
    ElMessage.error(error.message || lt('保存失败', '保存失敗', 'Failed to save'))
  } finally {
    saving.value = false
  }
}

const saveAsStatus = async (status) => {
  detailForm.status = status
  await saveDetail()
}

const removeDetail = async () => {
  const index = findActualIndex(detailItem.value)
  await removeItem(index)
  await saveList()
  router.push('/pro/recommendation-content/items/list')
}

const submitPublish = async () => {
  publishing.value = true
  try {
    await createDiscoverPublishOrder({
      scopeCode: 'COMMUNITY',
      effectiveAt: publishForm.effectiveAt ? `${publishForm.effectiveAt}:00` : null,
      reason: publishForm.reason || 'Publish from recommendation detail'
    })
    previewData.value = await fetchDiscoverPublishPreview('COMMUNITY')
    ElMessage.success(lt('发布单已创建', '發佈單已建立', 'Publish order created'))
  } catch (error) {
    ElMessage.error(error.message || lt('创建发布单失败', '建立發佈單失敗', 'Failed to create publish order'))
  } finally {
    publishing.value = false
  }
}

watch(() => props.detailId, syncDetailForm)
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
.editorial-preview { display: flex; flex-direction: column; gap: 12px; }
.preview-tag { font-size: 12px; color: #475467; }
.preview-title { font-size: 20px; font-weight: 700; color: #111827; }
.preview-subtitle { font-size: 15px; color: #1f2937; }
.preview-body { color: #6b7280; line-height: 1.7; white-space: pre-wrap; }
.preview-lines { display: flex; flex-direction: column; gap: 10px; }
.preview-line { display: flex; justify-content: space-between; gap: 16px; font-size: 13px; color: #4b5563; }
</style>
