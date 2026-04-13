<template>
  <div class="pro-page">
    <el-card>
      <template #header>
        <div class="head-row">
          <span>{{ lt('推薦分類模塊', '推薦分類模塊', 'Recommendation Category Module') }}</span>
          <div class="actions">
            <el-button :loading="loading" @click="loadCategories">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
            <el-button type="primary" @click="openCreate">{{ lt('創建分類', '創建分類', 'Create Category') }}</el-button>
          </div>
        </div>
      </template>

      <el-table :data="categories" v-loading="loading">
        <el-table-column type="index" width="60" :label="lt('序號', '序號', 'No.')" />
        <el-table-column prop="name" :label="lt('分類名稱', '分類名稱', 'Category Name')" min-width="200" />
        <el-table-column prop="sortOrder" :label="lt('排序', '排序', 'Sort Order')" width="120" />
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">{{ lt('編輯', '編輯', 'Edit') }}</el-button>
            <el-button link type="danger" @click="removeCategory(row)">{{ lt('刪除', '刪除', 'Delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="editorVisible"
      :title="editingId === null ? lt('創建分類', '創建分類', 'Create Category') : lt('編輯分類', '編輯分類', 'Edit Category')"
      width="520px"
    >
      <el-form :model="editorForm" label-width="120px">
        <el-form-item :label="lt('分類名稱', '分類名稱', 'Category Name')" required>
          <el-input v-model="editorForm.name" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item :label="lt('排序值', '排序值', 'Sort Order')">
          <el-input-number v-model="editorForm.sortOrder" :min="0" :max="9999" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="confirmEdit">{{ lt('確認', '確認', 'Confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDiscoverCategories,
  createDiscoverCategory,
  updateDiscoverCategory,
  deleteDiscoverCategory
} from '../../api'
import { useI18nLite } from '../../i18n'

const { lt } = useI18nLite()

const loading = ref(false)
const saving = ref(false)
const editorVisible = ref(false)
const editingId = ref(null)
const categories = ref([])

const editorForm = reactive({
  name: '',
  sortOrder: 0
})

const resetEditor = () => {
  editorForm.name = ''
  editorForm.sortOrder = 0
}

const loadCategories = async () => {
  loading.value = true
  try {
    const res = await getDiscoverCategories()
    categories.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || lt('加載分類失敗', '加載分類失敗', 'Failed to load categories'))
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  editingId.value = null
  resetEditor()
  editorVisible.value = true
}

const openEdit = (row) => {
  editingId.value = row.id
  editorForm.name = row.name || ''
  editorForm.sortOrder = typeof row.sortOrder === 'number' ? row.sortOrder : 0
  editorVisible.value = true
}

const confirmEdit = async () => {
  const name = (editorForm.name || '').trim()
  if (!name) {
    ElMessage.warning(lt('請輸入分類名稱', '請輸入分類名稱', 'Please input category name'))
    return
  }
  saving.value = true
  try {
    const payload = {
      name,
      sortOrder: editorForm.sortOrder || 0
    }
    const res = editingId.value === null
      ? await createDiscoverCategory(payload)
      : await updateDiscoverCategory(editingId.value, payload)
    categories.value = res.data || []
    editorVisible.value = false
    ElMessage.success(lt('分類保存成功', '分類保存成功', 'Category saved'))
  } catch (error) {
    ElMessage.error(error.message || lt('分類保存失敗', '分類保存失敗', 'Failed to save category'))
  } finally {
    saving.value = false
  }
}

const removeCategory = async (row) => {
  try {
    await ElMessageBox.confirm(
      lt('刪除後不可恢復，是否繼續？', '刪除後不可恢復，是否繼續？', 'Delete this category?'),
      lt('提示', '提示', 'Notice'),
      {
        type: 'warning',
        confirmButtonText: lt('確認', '確認', 'Confirm'),
        cancelButtonText: lt('取消', '取消', 'Cancel')
      }
    )
    const res = await deleteDiscoverCategory(row.id)
    categories.value = res.data || []
    ElMessage.success(lt('分類已刪除', '分類已刪除', 'Category deleted'))
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('刪除分類失敗', '刪除分類失敗', 'Failed to delete category'))
    }
  }
}

onMounted(loadCategories)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.head-row { display: flex; justify-content: space-between; align-items: center; }
.actions { display: flex; gap: 8px; }
</style>
