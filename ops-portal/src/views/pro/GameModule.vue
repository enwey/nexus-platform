<template>
  <div class="pro-page">
    <el-card>
      <template #header>
        <div class="head-row">
          <span>{{ lt('遊戲管理模塊', '遊戲管理模塊', 'Game Management Module') }}</span>
          <div class="actions">
            <el-button @click="gotoCategoryModule">{{ lt('分類管理', '分類管理', 'Category Management') }}</el-button>
            <el-button type="primary" @click="loadGames">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <div class="filters">
        <el-input v-model.trim="keyword" clearable style="width: 280px" :placeholder="lt('按名稱 / AppID 搜索', '按名稱 / AppID 搜索', 'Search by name / AppID')" />
      </div>

      <el-table :data="filteredGames" v-loading="loading" style="margin-top: 12px">
        <el-table-column prop="name" :label="lt('遊戲名', '遊戲名', 'Game Name')" min-width="200" />
        <el-table-column prop="appId" label="AppID" min-width="180" />
        <el-table-column prop="version" :label="lt('版本', '版本', 'Version')" width="100" />
        <el-table-column prop="status" :label="lt('狀態', '狀態', 'Status')" width="120" />
        <el-table-column prop="category" :label="lt('分類', '分類', 'Category')" width="120" />
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">{{ lt('編輯', '編輯', 'Edit') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editVisible" :title="lt('編輯遊戲', '編輯遊戲', 'Edit Game')" width="620px">
      <el-form :model="editForm" label-width="120px">
        <el-form-item :label="lt('名稱', '名稱', 'Name')"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item :label="lt('分類', '分類', 'Category')">
          <el-select v-model="editForm.category" style="width: 100%">
            <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('版本', '版本', 'Version')"><el-input v-model="editForm.version" /></el-form-item>
        <el-form-item :label="lt('圖標', '圖標', 'Icon URL')"><el-input v-model="editForm.iconUrl" /></el-form-item>
        <el-form-item :label="lt('描述', '描述', 'Description')"><el-input v-model="editForm.description" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="saveGame">{{ lt('保存', '保存', 'Save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getGameCategories, getGameList, updateGameMetadata } from '../../api'
import { useI18nLite } from '../../i18n'

const router = useRouter()
const { lt } = useI18nLite()
const loading = ref(false)
const saving = ref(false)
const games = ref([])
const keyword = ref('')
const categories = ref(['all'])
const editVisible = ref(false)
const editingId = ref(null)
const editForm = reactive({
  name: '',
  category: 'all',
  version: '',
  iconUrl: '',
  description: ''
})

const filteredGames = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  if (!key) return games.value
  return games.value.filter((g) => {
    const nameHit = (g.name || '').toLowerCase().includes(key)
    const appIdHit = (g.appId || '').toLowerCase().includes(key)
    return nameHit || appIdHit
  })
})

const loadGames = async () => {
  loading.value = true
  try {
    const [gamesRes, categoriesRes] = await Promise.all([
      getGameList(),
      getGameCategories().catch(() => ({ data: categories.value }))
    ])
    games.value = gamesRes.data || []
    categories.value = categoriesRes.data?.length ? categoriesRes.data : categories.value
  } catch (error) {
    ElMessage.error(error.message || lt('加載遊戲管理模塊失敗', '加載遊戲管理模塊失敗', 'Failed to load game management module'))
  } finally {
    loading.value = false
  }
}

const openEdit = (row) => {
  editingId.value = row.id
  editForm.name = row.name || ''
  editForm.category = row.category || 'all'
  editForm.version = row.version || ''
  editForm.iconUrl = row.iconUrl || ''
  editForm.description = row.description || ''
  editVisible.value = true
}

const saveGame = async () => {
  if (!editingId.value) return
  saving.value = true
  try {
    await updateGameMetadata(editingId.value, {
      name: editForm.name,
      category: editForm.category,
      version: editForm.version,
      iconUrl: editForm.iconUrl,
      description: editForm.description
    })
    ElMessage.success(lt('遊戲更新成功', '遊戲更新成功', 'Game updated'))
    editVisible.value = false
    await loadGames()
  } catch (error) {
    ElMessage.error(error.message || lt('遊戲更新失敗', '遊戲更新失敗', 'Failed to update game'))
  } finally {
    saving.value = false
  }
}

const gotoCategoryModule = () => {
  router.push('/pro/game-categories')
}

onMounted(loadGames)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.head-row { display: flex; justify-content: space-between; align-items: center; }
.actions { display: flex; gap: 8px; }
.filters { display: flex; gap: 8px; align-items: center; }
</style>
