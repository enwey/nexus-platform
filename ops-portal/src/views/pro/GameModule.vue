<template>
  <div class="pro-page">
    <el-card>
      <template #header>
        <div class="head-row">
          <span>{{ lt('游戏管理模块', '遊戲管理模塊', 'Game Management Module') }}</span>
          <div class="actions">
            <el-input
              v-model="keyword"
              clearable
              class="keyword-input"
              :placeholder="lt('搜索游戏名称或 AppID', '搜尋遊戲名稱或 AppID', 'Search by game name or AppID')"
            />
            <el-button @click="gotoCategoryModule">{{ lt('分类管理', '分類管理', 'Category Management') }}</el-button>
            <el-button :loading="loading" @click="loadGames">{{ lt('刷新', '重新整理', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredGames" v-loading="loading">
        <el-table-column type="index" width="60" :label="lt('序号', '序號', 'No.')" />
        <el-table-column prop="name" :label="lt('游戏名称', '遊戲名稱', 'Game Name')" min-width="180" />
        <el-table-column prop="appId" label="AppID" min-width="180" />
        <el-table-column prop="version" :label="lt('版本', '版本', 'Version')" width="120" />
        <el-table-column :label="lt('分类', '分類', 'Category')" min-width="140">
          <template #default="{ row }">{{ row.category || '-' }}</template>
        </el-table-column>
        <el-table-column :label="lt('状态', '狀態', 'Status')" width="140">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="260" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'DRAFT' || row.status === 'REJECTED'"
              link
              type="warning"
              @click="submitForAudit(row)"
            >
              {{ lt('提交审核', '提交審核', 'Submit for Review') }}
            </el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              link
              type="success"
              @click="approvePendingGame(row)"
            >
              {{ lt('审核通过', '審核通過', 'Approve') }}
            </el-button>
            <el-button link type="primary" @click="openEdit(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="editorVisible"
      :title="lt('编辑游戏信息', '編輯遊戲資訊', 'Edit Game Info')"
      width="640px"
    >
      <el-form :model="editorForm" label-width="120px">
        <el-form-item :label="lt('游戏名称', '遊戲名稱', 'Game Name')" required>
          <el-input v-model="editorForm.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item :label="lt('游戏描述', '遊戲描述', 'Description')">
          <el-input v-model="editorForm.description" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item :label="lt('图标地址', '圖示位址', 'Icon URL')">
          <el-input v-model="editorForm.iconUrl" />
        </el-form-item>
        <el-form-item :label="lt('分类', '分類', 'Category')">
          <el-select v-model="editorForm.category" clearable filterable style="width: 100%">
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('版本', '版本', 'Version')">
          <el-input v-model="editorForm.version" />
        </el-form-item>
        <el-form-item :label="lt('标签', '標籤', 'Tags')">
          <el-select v-model="editorForm.tags" multiple filterable allow-create default-first-option style="width: 100%">
            <el-option v-for="item in tagOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('需要联网', '需要連網', 'Requires Online')">
          <el-switch v-model="editorForm.requiresOnline" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editorVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="saveGame">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveGame,
  getGameCategories,
  getGameList,
  submitGameForAudit,
  updateGameMetadata
} from '../../api'
import { useI18nLite } from '../../i18n'

const router = useRouter()
const { lt } = useI18nLite()

const loading = ref(false)
const saving = ref(false)
const editorVisible = ref(false)
const keyword = ref('')
const games = ref([])
const categoryOptions = ref([])
const editingGameId = ref(null)

const editorForm = reactive({
  name: '',
  description: '',
  iconUrl: '',
  category: '',
  tags: [],
  version: '',
  requiresOnline: false
})

const tagOptions = computed(() => {
  const pool = new Set()
  for (const game of games.value) {
    for (const tag of normalizeTags(game.tags)) {
      pool.add(tag)
    }
  }
  return Array.from(pool)
})

const filteredGames = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  if (!query) return games.value
  return games.value.filter((game) => {
    const name = (game.name || '').toLowerCase()
    const appId = (game.appId || '').toLowerCase()
    return name.includes(query) || appId.includes(query)
  })
})

const getStatusText = (status) => {
  const map = {
    PROCESSING: lt('处理中', '處理中', 'Processing'),
    DRAFT: lt('草稿', '草稿', 'Draft'),
    PENDING: lt('待审核', '待審核', 'Pending'),
    APPROVED: lt('已通过', '已通過', 'Approved'),
    REJECTED: lt('已驳回', '已駁回', 'Rejected')
  }
  return map[status] || status || lt('未知', '未知', 'Unknown')
}

const getStatusType = (status) => ({
  PROCESSING: 'warning',
  DRAFT: 'info',
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger'
}[status] || 'info')

function normalizeTags(tags) {
  if (Array.isArray(tags)) {
    return tags.filter(Boolean).map((tag) => String(tag).trim()).filter(Boolean)
  }
  if (typeof tags === 'string') {
    return tags
      .split(',')
      .map((tag) => tag.trim())
      .filter(Boolean)
  }
  return []
}

function resetEditor() {
  editingGameId.value = null
  editorForm.name = ''
  editorForm.description = ''
  editorForm.iconUrl = ''
  editorForm.category = ''
  editorForm.tags = []
  editorForm.version = ''
  editorForm.requiresOnline = false
}

async function loadCategories() {
  try {
    const res = await getGameCategories()
    categoryOptions.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    ElMessage.error(error.message || lt('加载分类失败', '載入分類失敗', 'Failed to load categories'))
  }
}

async function loadGames() {
  loading.value = true
  try {
    const res = await getGameList()
    games.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    ElMessage.error(error.message || lt('加载游戏列表失败', '載入遊戲列表失敗', 'Failed to load game list'))
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  editingGameId.value = row.id
  editorForm.name = row.name || ''
  editorForm.description = row.description || ''
  editorForm.iconUrl = row.iconUrl || ''
  editorForm.category = row.category || ''
  editorForm.tags = normalizeTags(row.tags)
  editorForm.version = row.version || ''
  editorForm.requiresOnline = Boolean(row.requiresOnline)
  editorVisible.value = true
}

async function promptReason(title) {
  const { value } = await ElMessageBox.prompt(
    lt('請填寫審核原因，至少 2 個字', '請填寫審核原因，至少 2 個字', 'Please input a review reason with at least 2 characters'),
    title,
    {
      confirmButtonText: lt('确认', '確認', 'Confirm'),
      cancelButtonText: lt('取消', '取消', 'Cancel'),
      inputPattern: /^.{2,}$/u,
      inputErrorMessage: lt('审核原因至少需要 2 个字', '審核原因至少需要 2 個字', 'Reason must be at least 2 characters')
    }
  )
  return value.trim()
}

async function submitForAudit(row) {
  try {
    await submitGameForAudit(row.id, lt('运营后台提交审核', '營運後台提交審核', 'Submitted from operations portal'))
    ElMessage.success(lt('已提交审核', '已提交審核', 'Submitted for review'))
    await loadGames()
  } catch (error) {
    ElMessage.error(error.message || lt('提交审核失败', '提交審核失敗', 'Failed to submit for review'))
  }
}

async function approvePendingGame(row) {
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

async function saveGame() {
  const name = editorForm.name.trim()
  if (!name) {
    ElMessage.warning(lt('请输入游戏名称', '請輸入遊戲名稱', 'Please input game name'))
    return
  }

  saving.value = true
  try {
    await updateGameMetadata(editingGameId.value, {
      name,
      description: editorForm.description.trim(),
      iconUrl: editorForm.iconUrl.trim(),
      category: editorForm.category || '',
      tags: editorForm.tags,
      version: editorForm.version.trim(),
      requiresOnline: Boolean(editorForm.requiresOnline)
    })
    ElMessage.success(lt('游戏信息已保存', '遊戲資訊已儲存', 'Game info saved'))
    editorVisible.value = false
    await loadGames()
  } catch (error) {
    ElMessage.error(error.message || lt('保存游戏信息失败', '儲存遊戲資訊失敗', 'Failed to save game info'))
  } finally {
    saving.value = false
  }
}

function gotoCategoryModule() {
  router.push('/pro/game-categories')
}

onMounted(async () => {
  resetEditor()
  await Promise.all([loadCategories(), loadGames()])
})
</script>

<style scoped>
.pro-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.keyword-input {
  width: 280px;
}
</style>
