<template>
  <div class="page-shell">
    <header class="page-header">
      <div>
        <h1>{{ lt('我的游戏', '我的遊戲', 'My Games') }}</h1>
        <p>{{ lt('查看当前账号提交的游戏版本和审核状态，并维护分类与基础信息。', '查看當前帳號提交的遊戲版本和審核狀態，並維護分類與基礎資訊。', 'Review uploaded game versions, audit status, and maintain game metadata.') }}</p>
      </div>
      <el-button type="primary" @click="$router.push('/games/upload')">{{ lt('上传游戏', '上傳遊戲', 'Upload Game') }}</el-button>
    </header>

    <el-card>
      <el-table :data="games" v-loading="loading" :empty-text="lt('暂无游戏数据', '暫無遊戲資料', 'No games found')">
        <el-table-column prop="name" :label="lt('游戏名称', '遊戲名稱', 'Game Name')" min-width="180" />
        <el-table-column prop="category" :label="lt('分类', '分類', 'Category')" width="120">
          <template #default="{ row }">{{ row.category || '-' }}</template>
        </el-table-column>
        <el-table-column prop="description" :label="lt('描述', '描述', 'Description')" min-width="220" show-overflow-tooltip />
        <el-table-column prop="version" :label="lt('版本', '版本', 'Version')" width="120" />
        <el-table-column prop="status" :label="lt('状态', '狀態', 'Status')" width="140">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" :label="lt('创建时间', '建立時間', 'Created At')" width="200" />
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">{{ lt('查看', '查看', 'View') }}</el-button>
            <el-button link type="warning" size="small" @click="openEdit(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editVisible" :title="lt('编辑游戏信息', '編輯遊戲資訊', 'Edit Game Metadata')" width="620px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item :label="lt('游戏名称', '遊戲名稱', 'Name')"><el-input v-model="editForm.name" maxlength="120" show-word-limit /></el-form-item>
        <el-form-item :label="lt('分类', '分類', 'Category')">
          <el-select v-model="editForm.category" style="width:100%">
            <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('版本号', '版本號', 'Version')"><el-input v-model="editForm.version" :placeholder="lt('例如 1.0.3', '例如 1.0.3', 'e.g. 1.0.3')" /></el-form-item>
        <el-form-item :label="lt('图标URL', '圖示 URL', 'Icon URL')"><el-input v-model="editForm.iconUrl" placeholder="https://..." /></el-form-item>
        <el-form-item :label="lt('标签', '標籤', 'Tags')"><el-input v-model="editForm.tagsText" :placeholder="lt('多个标签用英文逗号分隔', '多個標籤用英文逗號分隔', 'Comma-separated tags')" /></el-form-item>
        <el-form-item :label="lt('描述', '描述', 'Description')"><el-input v-model="editForm.description" type="textarea" :rows="4" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeveloperGames, getGameCategories, updateGameMetadata } from '../api'
import { useUserStore } from '../stores/user'
import { useI18nLite } from '../i18n'

const userStore = useUserStore()
const { lt } = useI18nLite()
const games = ref([])
const loading = ref(false)
const saving = ref(false)
const categories = ref(['all', 'action', 'casual', 'rpg'])
const editVisible = ref(false)
const editingId = ref(null)
const editForm = reactive({ name: '', category: 'all', description: '', iconUrl: '', version: '', tagsText: '' })

const getStatusText = (status) => {
  const map = {
    DRAFT: lt('草稿', '草稿', 'Draft'),
    PENDING: lt('待审核', '待審核', 'Pending'),
    APPROVED: lt('已通过', '已通過', 'Approved'),
    REJECTED: lt('已拒绝', '已拒絕', 'Rejected')
  }
  return map[status] || status || lt('未知', '未知', 'Unknown')
}
const getStatusType = (status) => ({ DRAFT: 'info', PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[status] || 'info')

const loadGames = async () => {
  if (!userStore.user?.id) {
    games.value = []
    return
  }

  loading.value = true
  try {
    const [gamesRes, categoriesRes] = await Promise.all([
      getDeveloperGames(userStore.user.id),
      getGameCategories().catch(() => ({ data: ['all', 'action', 'casual', 'rpg'] }))
    ])
    games.value = gamesRes.data || []
    categories.value = categoriesRes.data?.length ? categoriesRes.data : ['all', 'action', 'casual', 'rpg']
  } catch (error) {
    ElMessage.error(error.message || lt('加载游戏失败', '載入遊戲失敗', 'Failed to load games'))
  } finally {
    loading.value = false
  }
}

const openEdit = (row) => {
  editingId.value = row.id
  editForm.name = row.name || ''
  editForm.category = row.category || 'all'
  editForm.description = row.description || ''
  editForm.iconUrl = row.iconUrl || ''
  editForm.version = row.version || ''
  editForm.tagsText = row.tagsJson || ''
  editVisible.value = true
}

const handleSave = async () => {
  if (!editingId.value) return
  saving.value = true
  try {
    const tags = (editForm.tagsText || '').split(',').map((x) => x.trim()).filter(Boolean)
    await updateGameMetadata(editingId.value, {
      name: editForm.name,
      category: editForm.category,
      description: editForm.description,
      iconUrl: editForm.iconUrl,
      version: editForm.version,
      tags
    })
    ElMessage.success(lt('已保存游戏信息', '已儲存遊戲資訊', 'Game metadata saved'))
    editVisible.value = false
    await loadGames()
  } catch (error) {
    ElMessage.error(error.message || lt('保存失败', '儲存失敗', 'Save failed'))
  } finally {
    saving.value = false
  }
}

const handleView = (row) => {
  ElMessageBox.alert(
    lt(
      `游戏名称：${row.name}\n分类：${row.category || '-'}\n描述：${row.description || '暂无'}\n版本：${row.version || '暂无'}\n状态：${getStatusText(row.status)}`,
      `遊戲名稱：${row.name}\n分類：${row.category || '-'}\n描述：${row.description || '暫無'}\n版本：${row.version || '暫無'}\n狀態：${getStatusText(row.status)}`,
      `Game: ${row.name}\nCategory: ${row.category || '-'}\nDescription: ${row.description || 'N/A'}\nVersion: ${row.version || 'N/A'}\nStatus: ${getStatusText(row.status)}`
    ),
    lt('游戏详情', '遊戲詳情', 'Game Details'),
    { confirmButtonText: lt('知道了', '知道了', 'OK') }
  )
}

onMounted(loadGames)
</script>

<style scoped>
.page-shell { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 24px; }
.page-header h1 { margin: 0 0 8px; }
.page-header p { margin: 0; color: #6b7280; }
</style>
