<template>
  <div class="pro-page">
    <el-card>
      <template #header>
        <div class="head-row">
          <span>{{ lt('推薦模塊', '推薦模塊', 'Recommendation Module') }}</span>
          <div class="actions">
            <el-button @click="loadConfig">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
            <el-button @click="gotoCategoryModule">{{ lt('分類管理', '分類管理', 'Category Management') }}</el-button>
            <el-button type="success" :loading="saving" @click="saveConfig">{{ lt('保存全部', '保存全部', 'Save All') }}</el-button>
          </div>
        </div>
      </template>

      <el-alert
        v-if="!categoryOptions.length"
        :title="lt('尚未創建推薦分類，請先進入分類管理創建。', '尚未創建推薦分類，請先進入分類管理創建。', 'No categories yet. Create categories first in Category Management.')"
        type="warning"
        show-icon
        class="category-alert"
      />

      <el-divider>{{ lt('遊戲頁冷啟動頂部廣告', '遊戲頁冷啟動頂部廣告', 'Game Coldstart Top Ads') }}</el-divider>
      <div class="section-head">
        <span>{{ lt('獨立板塊配置', '獨立板塊配置', 'Independent Section Config') }}</span>
        <el-button type="primary" @click="openBannerCreate('game')">{{ lt('新增廣告', '新增廣告', 'Add Banner') }}</el-button>
      </div>
      <el-table :data="gameTopBanners" v-loading="loading">
        <el-table-column type="index" width="60" :label="lt('序號', '序號', 'No.')" />
        <el-table-column prop="appId" label="AppID" min-width="170" />
        <el-table-column :label="lt('遊戲', '遊戲', 'Game')" min-width="180">
          <template #default="{ row }">{{ gameNameById(row.appId) }}</template>
        </el-table-column>
        <el-table-column prop="badgeText" :label="lt('廣告類型', '廣告類型', 'Ad Type')" width="140" />
        <el-table-column prop="title" :label="lt('標題', '標題', 'Title')" min-width="180" />
        <el-table-column prop="subtitle" :label="lt('副標題', '副標題', 'Subtitle')" min-width="180" />
        <el-table-column prop="coverUrl" :label="lt('圖片URL', '圖片URL', 'Image URL')" min-width="220" show-overflow-tooltip />
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="260" fixed="right">
          <template #default="{ $index, row }">
            <el-button link type="primary" @click="openBannerEdit('game', row, $index)">{{ lt('編輯', '編輯', 'Edit') }}</el-button>
            <el-button link @click="moveBanner('game', $index, -1)" :disabled="$index === 0">{{ lt('上移', '上移', 'Move Up') }}</el-button>
            <el-button link @click="moveBanner('game', $index, 1)" :disabled="$index === gameTopBanners.length - 1">{{ lt('下移', '下移', 'Move Down') }}</el-button>
            <el-button link type="danger" @click="removeBanner('game', $index)">{{ lt('刪除', '刪除', 'Delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-divider>{{ lt('發現頁頂部廣告', '發現頁頂部廣告', 'Discover Top Ads') }}</el-divider>
      <div class="section-head">
        <span>{{ lt('獨立板塊配置', '獨立板塊配置', 'Independent Section Config') }}</span>
        <el-button type="primary" @click="openBannerCreate('discover')">{{ lt('新增廣告', '新增廣告', 'Add Banner') }}</el-button>
      </div>
      <el-table :data="discoverTopBanners" v-loading="loading">
        <el-table-column type="index" width="60" :label="lt('序號', '序號', 'No.')" />
        <el-table-column prop="appId" label="AppID" min-width="170" />
        <el-table-column :label="lt('遊戲', '遊戲', 'Game')" min-width="180">
          <template #default="{ row }">{{ gameNameById(row.appId) }}</template>
        </el-table-column>
        <el-table-column prop="badgeText" :label="lt('廣告類型', '廣告類型', 'Ad Type')" width="140" />
        <el-table-column prop="title" :label="lt('標題', '標題', 'Title')" min-width="180" />
        <el-table-column prop="subtitle" :label="lt('副標題', '副標題', 'Subtitle')" min-width="180" />
        <el-table-column prop="coverUrl" :label="lt('圖片URL', '圖片URL', 'Image URL')" min-width="220" show-overflow-tooltip />
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="260" fixed="right">
          <template #default="{ $index, row }">
            <el-button link type="primary" @click="openBannerEdit('discover', row, $index)">{{ lt('編輯', '編輯', 'Edit') }}</el-button>
            <el-button link @click="moveBanner('discover', $index, -1)" :disabled="$index === 0">{{ lt('上移', '上移', 'Move Up') }}</el-button>
            <el-button link @click="moveBanner('discover', $index, 1)" :disabled="$index === discoverTopBanners.length - 1">{{ lt('下移', '下移', 'Move Down') }}</el-button>
            <el-button link type="danger" @click="removeBanner('discover', $index)">{{ lt('刪除', '刪除', 'Delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-divider>{{ lt('推薦內容列表', '推薦內容列表', 'Recommendation Content List') }}</el-divider>
      <div class="section-head">
        <span>{{ lt('社區推薦內容', '社區推薦內容', 'Community Recommendation Content') }}</span>
        <el-button type="primary" @click="openCreate">{{ lt('創建推薦', '創建推薦', 'Create Recommendation') }}</el-button>
      </div>
      <el-table :data="recommendList" v-loading="loading">
        <el-table-column type="index" width="60" :label="lt('序號', '序號', 'No.')" />
        <el-table-column prop="appId" label="AppID" min-width="170" />
        <el-table-column :label="lt('遊戲', '遊戲', 'Game')" min-width="180">
          <template #default="{ row }">{{ gameNameById(row.appId) }}</template>
        </el-table-column>
        <el-table-column prop="cardCategory" :label="lt('分類', '分類', 'Category')" width="140" />
        <el-table-column prop="cardTitle" :label="lt('卡片標題', '卡片標題', 'Card Title')" min-width="220" />
        <el-table-column prop="articleTitle" :label="lt('詳情標題', '詳情標題', 'Detail Title')" min-width="220" />
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="260" fixed="right">
          <template #default="{ $index, row }">
            <el-button link type="primary" @click="openEdit(row, $index)">{{ lt('編輯', '編輯', 'Edit') }}</el-button>
            <el-button link @click="moveUp($index)" :disabled="$index === 0">{{ lt('上移', '上移', 'Move Up') }}</el-button>
            <el-button link @click="moveDown($index)" :disabled="$index === recommendList.length - 1">{{ lt('下移', '下移', 'Move Down') }}</el-button>
            <el-button link type="danger" @click="removeItem($index)">{{ lt('刪除', '刪除', 'Delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="editorVisible"
      :title="editingIndex === null ? lt('創建推薦內容', '創建推薦內容', 'Create Recommendation Item') : lt('編輯推薦內容', '編輯推薦內容', 'Edit Recommendation Item')"
      width="760px"
    >
      <el-form :model="editorForm" label-width="130px">
        <el-divider>{{ lt('關聯遊戲與基礎信息', '關聯遊戲與基礎信息', 'Game & Basic Info') }}</el-divider>
        <el-form-item :label="lt('關聯遊戲', '關聯遊戲', 'Related Game')" required>
          <el-select v-model="editorForm.appId" filterable style="width: 100%">
            <el-option v-for="item in gameOptions" :key="item.appId" :label="`${item.name} (${item.appId})`" :value="item.appId" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('卡片分類', '卡片分類', 'Card Category')" required>
          <el-select v-model="editorForm.cardCategory" filterable style="width: 100%">
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('卡片標題', '卡片標題', 'Card Title')"><el-input v-model="editorForm.cardTitle" /></el-form-item>
        <el-form-item :label="lt('封面圖URL', '封面圖URL', 'Cover URL')">
          <el-input v-model="editorForm.coverUrl" />
          <div class="field-hint">{{ lt('建議比例 16:9（例如 1242x699）', '建議比例 16:9（例如 1242x699）', 'Recommended ratio 16:9 (e.g. 1242x699)') }}</div>
        </el-form-item>
        <el-form-item :label="lt('按鈕文案', '按鈕文案', 'Action Text')"><el-input v-model="editorForm.actionText" /></el-form-item>

        <el-divider>{{ lt('圖文詳情內容（端內展示）', '圖文詳情內容（端內展示）', 'Image-Text Detail') }}</el-divider>
        <el-form-item :label="lt('詳情標籤', '詳情標籤', 'Detail Tag')"><el-input v-model="editorForm.articleTag" /></el-form-item>
        <el-form-item :label="lt('詳情標題', '詳情標題', 'Detail Title')"><el-input v-model="editorForm.articleTitle" /></el-form-item>
        <el-form-item :label="lt('圖文編輯', '圖文編輯', 'Rich Content Editor')">
          <el-input
            v-model="editorForm.articleBody"
            type="textarea"
            :rows="8"
            :placeholder="lt('輸入推薦詳情內容（當前按文本展示）', '輸入推薦詳情內容（當前按文本展示）', 'Input detail content (currently rendered as text)')"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editorVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" @click="confirmEdit">{{ lt('確認', '確認', 'Confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="bannerEditorVisible"
      :title="bannerEditing.index === null ? lt('新增頂部廣告', '新增頂部廣告', 'Add Top Banner') : lt('編輯頂部廣告', '編輯頂部廣告', 'Edit Top Banner')"
      width="680px"
    >
      <el-form :model="bannerEditorForm" label-width="130px">
        <el-form-item :label="lt('關聯遊戲', '關聯遊戲', 'Related Game')" required>
          <el-select v-model="bannerEditorForm.appId" filterable style="width: 100%">
            <el-option v-for="item in gameOptions" :key="item.appId" :label="`${item.name} (${item.appId})`" :value="item.appId" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('廣告類型', '廣告類型', 'Ad Type')" required>
          <el-select v-model="bannerEditorForm.badgeText" filterable style="width: 100%">
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('標題', '標題', 'Title')">
          <el-input v-model="bannerEditorForm.title" />
        </el-form-item>
        <el-form-item :label="lt('副標題', '副標題', 'Subtitle')">
          <el-input v-model="bannerEditorForm.subtitle" />
        </el-form-item>
        <el-form-item :label="lt('圖片URL', '圖片URL', 'Image URL')">
          <el-input v-model="bannerEditorForm.coverUrl" />
          <div class="field-hint">{{ lt('建議比例 16:9（例如 1242x699）', '建議比例 16:9（例如 1242x699）', 'Recommended ratio 16:9 (e.g. 1242x699)') }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bannerEditorVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" @click="confirmBannerEdit">{{ lt('確認', '確認', 'Confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDiscoverOpsConfig, getDiscoverCategories, updateDiscoverOpsConfig } from '../../api'
import { useI18nLite } from '../../i18n'

const router = useRouter()
const { lt } = useI18nLite()
const loading = ref(false)
const saving = ref(false)
const editorVisible = ref(false)
const editingIndex = ref(null)
const gameOptions = ref([])
const recommendList = ref([])
const categoryOptions = ref([])
const gameTopBanners = ref([])
const discoverTopBanners = ref([])
const bannerEditorVisible = ref(false)

const discoverConfig = reactive({
  hero: null,
  rankedAppIds: [],
  newbieAppIds: [],
  everyoneAppIds: []
})

const bannerEditing = reactive({
  target: 'game',
  index: null
})

const editorForm = reactive({
  appId: '',
  cardCategory: '',
  cardTitle: '',
  coverUrl: '',
  articleTag: '',
  articleTitle: '',
  articleBody: '',
  actionText: ''
})

const bannerEditorForm = reactive({
  appId: '',
  badgeText: '',
  title: '',
  subtitle: '',
  coverUrl: ''
})

const emptyItem = () => ({
  appId: '',
  cardCategory: '',
  cardTitle: '',
  coverUrl: '',
  articleTag: '',
  articleTitle: '',
  articleBody: '',
  actionText: ''
})

const emptyBanner = () => ({
  appId: '',
  badgeText: '',
  title: '',
  subtitle: '',
  coverUrl: ''
})

const cloneItem = (item = {}) => ({
  appId: item.appId || '',
  cardCategory: item.cardCategory || '',
  cardTitle: item.cardTitle || '',
  coverUrl: item.coverUrl || '',
  articleTag: item.articleTag || '',
  articleTitle: item.articleTitle || '',
  articleBody: item.articleBody || '',
  actionText: item.actionText || ''
})

const cloneBanner = (item = {}) => ({
  appId: item.appId || '',
  badgeText: item.badgeText || '',
  title: item.title || '',
  subtitle: item.subtitle || '',
  coverUrl: item.coverUrl || ''
})

const assignEditor = (item = {}) => {
  Object.assign(editorForm, cloneItem(item))
}

const assignBannerEditor = (item = {}) => {
  Object.assign(bannerEditorForm, cloneBanner(item))
}

const gameNameById = (appId) => {
  const target = gameOptions.value.find((x) => x.appId === appId)
  return target?.name || '-'
}

const loadConfig = async () => {
  loading.value = true
  try {
    const [configRes, categoryRes] = await Promise.all([getDiscoverOpsConfig(), getDiscoverCategories()])
    const data = configRes.data || {}
    gameOptions.value = data.availableGames || []
    categoryOptions.value = (categoryRes.data || []).map((x) => ({
      id: x.id,
      name: x.name,
      sortOrder: x.sortOrder
    }))
    discoverConfig.hero = data.hero || null
    discoverConfig.rankedAppIds = [...(data.rankedAppIds || [])]
    discoverConfig.newbieAppIds = [...(data.newbieAppIds || [])]
    discoverConfig.everyoneAppIds = [...(data.everyoneAppIds || [])]
    recommendList.value = [...(data.communityItems || [])].map(cloneItem)
    gameTopBanners.value = [...(data.gameTopBanners || [])].map(cloneBanner)
    discoverTopBanners.value = [...(data.discoverTopBanners || [])].map(cloneBanner)
  } catch (error) {
    ElMessage.error(error.message || lt('加載推薦模塊失敗', '加載推薦模塊失敗', 'Failed to load recommendation module'))
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  if (!categoryOptions.value.length) {
    ElMessage.warning(lt('請先創建推薦分類', '請先創建推薦分類', 'Please create recommendation categories first'))
    return
  }
  editingIndex.value = null
  assignEditor(emptyItem())
  editorForm.cardCategory = categoryOptions.value[0].name
  editorVisible.value = true
}

const openEdit = (row, index) => {
  if (!categoryOptions.value.length) {
    ElMessage.warning(lt('請先創建推薦分類', '請先創建推薦分類', 'Please create recommendation categories first'))
    return
  }
  editingIndex.value = index
  assignEditor(row)
  if (!categoryOptions.value.find((x) => x.name === editorForm.cardCategory)) {
    editorForm.cardCategory = categoryOptions.value[0].name
  }
  editorVisible.value = true
}

const confirmEdit = () => {
  if (!editorForm.appId) {
    ElMessage.warning(lt('請先選擇關聯遊戲', '請先選擇關聯遊戲', 'Please select a related game'))
    return
  }
  if (!editorForm.cardCategory) {
    ElMessage.warning(lt('請先選擇卡片分類', '請先選擇卡片分類', 'Please select a category'))
    return
  }
  const payload = cloneItem(editorForm)
  if (editingIndex.value === null) {
    recommendList.value.push(payload)
  } else {
    recommendList.value.splice(editingIndex.value, 1, payload)
  }
  editorVisible.value = false
}

const removeItem = (index) => {
  recommendList.value.splice(index, 1)
}

const moveUp = (index) => {
  if (index <= 0) return
  const copy = [...recommendList.value]
  const current = copy[index]
  copy[index] = copy[index - 1]
  copy[index - 1] = current
  recommendList.value = copy
}

const moveDown = (index) => {
  if (index >= recommendList.value.length - 1) return
  const copy = [...recommendList.value]
  const current = copy[index]
  copy[index] = copy[index + 1]
  copy[index + 1] = current
  recommendList.value = copy
}

const targetBannerList = (target) => (target === 'discover' ? discoverTopBanners.value : gameTopBanners.value)

const openBannerCreate = (target) => {
  if (!categoryOptions.value.length) {
    ElMessage.warning(lt('請先創建推薦分類', '請先創建推薦分類', 'Please create recommendation categories first'))
    return
  }
  bannerEditing.target = target
  bannerEditing.index = null
  assignBannerEditor(emptyBanner())
  bannerEditorForm.badgeText = categoryOptions.value[0].name
  bannerEditorVisible.value = true
}

const openBannerEdit = (target, row, index) => {
  if (!categoryOptions.value.length) {
    ElMessage.warning(lt('請先創建推薦分類', '請先創建推薦分類', 'Please create recommendation categories first'))
    return
  }
  bannerEditing.target = target
  bannerEditing.index = index
  assignBannerEditor(row)
  if (!categoryOptions.value.find((x) => x.name === bannerEditorForm.badgeText)) {
    bannerEditorForm.badgeText = categoryOptions.value[0].name
  }
  bannerEditorVisible.value = true
}

const confirmBannerEdit = () => {
  if (!bannerEditorForm.appId) {
    ElMessage.warning(lt('請先選擇關聯遊戲', '請先選擇關聯遊戲', 'Please select a related game'))
    return
  }
  if (!bannerEditorForm.badgeText) {
    ElMessage.warning(lt('請先選擇廣告類型', '請先選擇廣告類型', 'Please select an ad type'))
    return
  }
  const list = [...targetBannerList(bannerEditing.target)]
  const payload = cloneBanner(bannerEditorForm)
  if (bannerEditing.index === null) {
    list.push(payload)
  } else {
    list.splice(bannerEditing.index, 1, payload)
  }
  if (bannerEditing.target === 'discover') {
    discoverTopBanners.value = list
  } else {
    gameTopBanners.value = list
  }
  bannerEditorVisible.value = false
}

const moveBanner = (target, index, delta) => {
  const list = [...targetBannerList(target)]
  const nextIndex = index + delta
  if (nextIndex < 0 || nextIndex >= list.length) return
  const current = list[index]
  list[index] = list[nextIndex]
  list[nextIndex] = current
  if (target === 'discover') {
    discoverTopBanners.value = list
  } else {
    gameTopBanners.value = list
  }
}

const removeBanner = (target, index) => {
  const list = [...targetBannerList(target)]
  list.splice(index, 1)
  if (target === 'discover') {
    discoverTopBanners.value = list
  } else {
    gameTopBanners.value = list
  }
}

const saveConfig = async () => {
  saving.value = true
  try {
    const discoverHero = discoverTopBanners.value[0] || null
    const payload = {
      hero: discoverHero,
      gameTopBanners: gameTopBanners.value.map(cloneBanner),
      discoverTopBanners: discoverTopBanners.value.map(cloneBanner),
      rankedAppIds: discoverConfig.rankedAppIds,
      newbieAppIds: discoverConfig.newbieAppIds,
      everyoneAppIds: discoverConfig.everyoneAppIds,
      communityItems: recommendList.value.map(cloneItem)
    }
    const res = await updateDiscoverOpsConfig(payload)
    const data = res.data || {}
    recommendList.value = [...(data.communityItems || [])].map(cloneItem)
    gameTopBanners.value = [...(data.gameTopBanners || [])].map(cloneBanner)
    discoverTopBanners.value = [...(data.discoverTopBanners || [])].map(cloneBanner)
    ElMessage.success(lt('推薦配置保存成功', '推薦配置保存成功', 'Recommendation config saved'))
  } catch (error) {
    ElMessage.error(error.message || lt('保存推薦配置失敗', '保存推薦配置失敗', 'Failed to save recommendation config'))
  } finally {
    saving.value = false
  }
}

const gotoCategoryModule = () => {
  router.push('/pro/recommend-categories')
}

onMounted(loadConfig)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.head-row { display: flex; justify-content: space-between; align-items: center; }
.actions { display: flex; gap: 8px; }
.section-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.category-alert { margin-bottom: 12px; }
.field-hint { margin-top: 4px; color: #909399; font-size: 12px; line-height: 1.4; }
</style>
