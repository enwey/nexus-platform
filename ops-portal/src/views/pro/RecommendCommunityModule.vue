<template>
  <div class="pro-page">
    <el-card>
      <template #header>
        <div class="head-row">
          <div>
            <div class="module-title">{{ lt('推荐列表管理', '推薦列表管理', 'Recommendation List Management') }}</div>
            <div class="module-subtitle">
              {{ lt('独立维护推荐卡片列表和图文详情内容，和广告位分开治理。', '獨立維護推薦卡片列表和圖文詳情內容，與廣告位分開治理。', 'Manage recommendation cards and editorial detail content separately from ad placements.') }}
            </div>
          </div>
          <div class="actions">
            <el-button :loading="loading" @click="loadWorkspace">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
            <el-button @click="router.push('/pro/recommend/categories')">{{ lt('管理分类', '管理分類', 'Manage Categories') }}</el-button>
            <el-button :disabled="!selectedIds.length" @click="batchChangeStatus('DRAFT')">{{ lt('转草稿', '轉草稿', 'Move to Draft') }}</el-button>
            <el-button :disabled="!selectedIds.length" @click="batchChangeStatus('PAUSED')">{{ lt('批量暂停', '批量暫停', 'Batch Pause') }}</el-button>
            <el-button type="success" :disabled="!selectedIds.length" @click="batchChangeStatus('PUBLISHED')">{{ lt('批量发布', '批量發佈', 'Batch Publish') }}</el-button>
            <el-button type="primary" @click="openCreate">{{ lt('创建推荐', '建立推薦', 'Create Recommendation') }}</el-button>
            <el-button type="success" :loading="saving" @click="saveList">{{ lt('保存列表', '保存列表', 'Save List') }}</el-button>
          </div>
        </div>
      </template>

      <el-alert
        v-if="!categoryOptions.length"
        :title="lt('当前没有推荐分类，推荐列表需要依赖分类，请先创建分类。', '當前沒有推薦分類，推薦列表需要依賴分類，請先建立分類。', 'No recommendation categories yet. Recommendation lists depend on categories, so create categories first.')"
        type="warning"
        show-icon
        class="category-alert"
      />

      <div class="toolbar">
        <el-input
          v-model="filters.keyword"
          clearable
          :placeholder="lt('搜索标题、分类、AppID 或游戏名', '搜尋標題、分類、AppID 或遊戲名', 'Search by title, category, AppID, or game name')"
          style="width: 320px"
        />
        <el-select v-model="filters.status" clearable :placeholder="lt('全部状态', '全部狀態', 'All statuses')" style="width: 180px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="lt(item.label[0], item.label[1], item.label[2])" :value="item.value" />
        </el-select>
        <div class="stats">
          <span>{{ lt('总数', '總數', 'Total') }} {{ recommendList.length }}</span>
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
        <el-table-column prop="cardCategory" :label="lt('分类', '分類', 'Category')" width="140" />
        <el-table-column prop="cardTitle" :label="lt('卡片标题', '卡片標題', 'Card Title')" min-width="200" />
        <el-table-column :label="lt('投放状态', '投放狀態', 'Delivery State')" width="120">
          <template #default="{ row }">
            <el-tag :type="stateTagType(derivePlacementState(row))">{{ stateText(derivePlacementState(row)) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('配置状态', '配置狀態', 'Config Status')" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : row.status === 'PAUSED' ? 'warning' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('开始时间', '開始時間', 'Start At')" width="170">
          <template #default="{ row }">{{ displayDateTime(row.startAt) }}</template>
        </el-table-column>
        <el-table-column :label="lt('结束时间', '結束時間', 'End At')" width="170">
          <template #default="{ row }">{{ displayDateTime(row.endAt) }}</template>
        </el-table-column>
        <el-table-column prop="articleTitle" :label="lt('详情标题', '詳情標題', 'Detail Title')" min-width="220" />
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row, findActualIndex(row))">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
            <el-button link @click="moveItem(findActualIndex(row), -1)" :disabled="findActualIndex(row) === 0">{{ lt('上移', '上移', 'Move Up') }}</el-button>
            <el-button link @click="moveItem(findActualIndex(row), 1)" :disabled="findActualIndex(row) === recommendList.length - 1">{{ lt('下移', '下移', 'Move Down') }}</el-button>
            <el-button link type="danger" @click="removeItem(findActualIndex(row))">{{ lt('删除', '刪除', 'Delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="editorVisible"
      :title="editingIndex === null ? lt('创建推荐内容', '建立推薦內容', 'Create Recommendation Item') : lt('编辑推荐内容', '編輯推薦內容', 'Edit Recommendation Item')"
      width="760px"
    >
      <el-form :model="editorForm" label-width="130px">
        <el-divider>{{ lt('关联游戏与基础信息', '關聯遊戲與基礎資訊', 'Game & Basic Info') }}</el-divider>
        <el-form-item :label="lt('关联游戏', '關聯遊戲', 'Related Game')" required>
          <el-select v-model="editorForm.appId" filterable style="width: 100%">
            <el-option v-for="item in gameOptions" :key="item.appId" :label="`${item.name} (${item.appId})`" :value="item.appId" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('卡片分类', '卡片分類', 'Card Category')" required>
          <el-select v-model="editorForm.cardCategory" filterable style="width: 100%">
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('配置状态', '配置狀態', 'Config Status')">
          <el-select v-model="editorForm.status" style="width: 100%">
            <el-option v-for="item in statusOptions" :key="item.value" :label="lt(item.label[0], item.label[1], item.label[2])" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('卡片标题', '卡片標題', 'Card Title')"><el-input v-model="editorForm.cardTitle" /></el-form-item>
        <el-form-item :label="lt('封面图 URL', '封面圖 URL', 'Cover URL')">
          <el-input v-model="editorForm.coverUrl" />
          <div class="field-hint">{{ lt('建议比例 16:9，例如 1242x699。', '建議比例 16:9，例如 1242x699。', 'Recommended ratio is 16:9, such as 1242x699.') }}</div>
        </el-form-item>
        <el-form-item :label="lt('按钮文案', '按鈕文案', 'Action Text')"><el-input v-model="editorForm.actionText" /></el-form-item>
        <el-form-item :label="lt('开始时间', '開始時間', 'Start At')"><el-input v-model="editorForm.startAt" type="datetime-local" /></el-form-item>
        <el-form-item :label="lt('结束时间', '結束時間', 'End At')"><el-input v-model="editorForm.endAt" type="datetime-local" /></el-form-item>

        <el-divider>{{ lt('图文详情内容', '圖文詳情內容', 'Editorial Detail') }}</el-divider>
        <el-form-item :label="lt('详情标签', '詳情標籤', 'Detail Tag')"><el-input v-model="editorForm.articleTag" /></el-form-item>
        <el-form-item :label="lt('详情标题', '詳情標題', 'Detail Title')"><el-input v-model="editorForm.articleTitle" /></el-form-item>
        <el-form-item :label="lt('详情正文', '詳情正文', 'Detail Body')">
          <el-input v-model="editorForm.articleBody" type="textarea" :rows="8" :placeholder="lt('输入推荐详情内容。', '輸入推薦詳情內容。', 'Input recommendation detail content.')" />
        </el-form-item>
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
import { useRouter } from 'vue-router'
import { useI18nLite } from '../../i18n'
import { batchUpdateDiscoverContentItemsStatus } from '../../api'
import {
  cloneRecommendationItem,
  derivePlacementState,
  emptyRecommendationItem,
  fetchDiscoverWorkspace,
  placementStatusOptions,
  saveDiscoverSection
} from '../../utils/discoverOps'

const router = useRouter()
const { lt } = useI18nLite()
const loading = ref(false)
const saving = ref(false)
const editorVisible = ref(false)
const editingIndex = ref(null)
const selectedIds = ref([])
const gameOptions = ref([])
const categoryOptions = ref([])
const recommendList = ref([])
const statusOptions = placementStatusOptions

const filters = reactive({
  keyword: '',
  status: ''
})

const editorForm = reactive(emptyRecommendationItem())

const filteredList = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return recommendList.value.filter((item) => {
    const state = derivePlacementState(item)
    const matchesStatus = !filters.status || state === filters.status || item.status === filters.status
    if (!matchesStatus) return false
    if (!keyword) return true
    const target = [item.appId, item.cardCategory, item.cardTitle, item.articleTitle, gameNameById(item.appId)].join(' ').toLowerCase()
    return target.includes(keyword)
  })
})

const stateCounts = computed(() => recommendList.value.reduce((acc, item) => {
  const state = derivePlacementState(item)
  acc[state] = (acc[state] || 0) + 1
  return acc
}, { LIVE: 0, SCHEDULED: 0, DRAFT: 0, PAUSED: 0, EXPIRED: 0 }))

const assignEditor = (item = {}) => {
  Object.assign(editorForm, cloneRecommendationItem(item))
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

const findActualIndex = (row) => recommendList.value.findIndex((item) => item === row || (item.id && item.id === row.id))
const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map((item) => item.id).filter(Boolean)
}

const loadWorkspace = async () => {
  loading.value = true
  try {
    const workspace = await fetchDiscoverWorkspace()
    gameOptions.value = workspace.gameOptions
    categoryOptions.value = workspace.categoryOptions
    recommendList.value = workspace.recommendList
  } catch (error) {
    ElMessage.error(error.message || lt('加载推荐列表失败', '載入推薦列表失敗', 'Failed to load recommendation list'))
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  if (!categoryOptions.value.length) {
    ElMessage.warning(lt('请先创建分类', '請先建立分類', 'Please create categories first'))
    return
  }
  editingIndex.value = null
  assignEditor(emptyRecommendationItem())
  editorForm.cardCategory = categoryOptions.value[0].name
  editorVisible.value = true
}

const openEdit = (row, index) => {
  if (!categoryOptions.value.length) {
    ElMessage.warning(lt('请先创建分类', '請先建立分類', 'Please create categories first'))
    return
  }
  editingIndex.value = index
  assignEditor(row)
  if (!categoryOptions.value.find((item) => item.name === editorForm.cardCategory)) {
    editorForm.cardCategory = categoryOptions.value[0].name
  }
  editorVisible.value = true
}

const confirmEdit = () => {
  if (!editorForm.appId || !editorForm.cardCategory || !editorForm.cardTitle.trim()) {
    ElMessage.warning(lt('请填写完整推荐信息', '請填寫完整推薦資訊', 'Please complete the recommendation fields'))
    return
  }
  const payload = cloneRecommendationItem(editorForm)
  if (editingIndex.value === null) {
    recommendList.value = [...recommendList.value, payload]
  } else {
    recommendList.value.splice(editingIndex.value, 1, payload)
    recommendList.value = [...recommendList.value]
  }
  editorVisible.value = false
}

const moveItem = (index, offset) => {
  const nextIndex = index + offset
  if (index < 0 || nextIndex < 0 || nextIndex >= recommendList.value.length) return
  const next = [...recommendList.value]
  ;[next[index], next[nextIndex]] = [next[nextIndex], next[index]]
  recommendList.value = next
}

const removeItem = async (index) => {
  try {
    await ElMessageBox.confirm(
      lt('确认删除该推荐内容？', '確認刪除此推薦內容？', 'Delete this recommendation item?'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { type: 'warning' }
    )
    recommendList.value.splice(index, 1)
    recommendList.value = [...recommendList.value]
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('删除推荐内容失败', '刪除推薦內容失敗', 'Failed to delete recommendation item'))
    }
  }
}

const saveList = async () => {
  saving.value = true
  try {
    const savedWorkspace = await saveDiscoverSection({ recommendList: recommendList.value.map(cloneRecommendationItem) })
    recommendList.value = savedWorkspace.recommendList
    ElMessage.success(lt('推荐列表已保存', '推薦列表已保存', 'Recommendation list saved'))
  } catch (error) {
    ElMessage.error(error.message || lt('保存推荐列表失败', '保存推薦列表失敗', 'Failed to save recommendation list'))
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
    recommendList.value = response.data?.communityItems || response.communityItems || []
    selectedIds.value = []
    ElMessage.success(lt('推荐内容状态已更新', '推薦內容狀態已更新', 'Recommendation item status updated'))
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('批量更新推荐内容失败', '批量更新推薦內容失敗', 'Failed to batch update recommendation items'))
    }
  }
}

onMounted(loadWorkspace)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.module-title { font-size: 18px; font-weight: 700; color: #101828; }
.module-subtitle { margin-top: 6px; color: #667085; font-size: 13px; line-height: 1.6; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; }
.category-alert { margin-bottom: 16px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 16px; flex-wrap: wrap; }
.stats { display: flex; gap: 16px; color: #475467; font-size: 13px; }
.field-hint { margin-top: 6px; color: #98a2b3; font-size: 12px; }
</style>
