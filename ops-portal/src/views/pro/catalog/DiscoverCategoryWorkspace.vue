<template>
  <div class="catalog-page">
    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ mode === 'list' ? lt('发现分类列表', '發現分類列表', 'Discover Category List') : (detail?.name || '-') }}</div>
            <div class="panel-subtitle">{{ lt('管理发现页、推荐内容依赖的分类体系，并跟踪引用情况。', '管理發現頁、推薦內容依賴的分類體系，並追蹤引用情況。', 'Maintain discover categories used by discover and recommendation content.') }}</div>
          </div>
          <div class="actions">
            <el-button @click="loadCategories">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
            <el-button v-if="mode === 'list'" type="primary" @click="openCreate">{{ lt('新建分类', '新建分類', 'Create Category') }}</el-button>
            <template v-else>
              <el-button type="danger" plain @click="removeCategory(detail)">{{ lt('删除分类', '刪除分類', 'Delete Category') }}</el-button>
              <el-button type="primary" :loading="saving" @click="saveDetail">{{ lt('保存修改', '保存修改', 'Save Changes') }}</el-button>
            </template>
          </div>
        </div>
      </template>

      <template v-if="mode === 'list'">
        <el-table :data="categories" v-loading="loading" row-key="id" @row-click="openDetail">
          <el-table-column prop="name" :label="lt('分类名称', '分類名稱', 'Category Name')" min-width="220" />
          <el-table-column prop="sortOrder" :label="lt('排序', '排序', 'Sort')" width="120" />
          <el-table-column prop="usageCount" :label="lt('总引用', '總引用', 'Usage')" width="120" />
          <el-table-column prop="bannerUsageCount" :label="lt('发现引用', '發現引用', 'Discover Usage')" width="120" />
          <el-table-column prop="recommendationUsageCount" :label="lt('推荐引用', '推薦引用', 'Recommendation Usage')" width="120" />
          <el-table-column :label="lt('操作', '操作', 'Actions')" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="openDetail(row)">{{ lt('查看详情', '查看詳情', 'View detail') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template v-else-if="detail">
        <el-form :model="form" label-width="120px">
          <el-form-item :label="lt('分类名称', '分類名稱', 'Category Name')" required>
            <el-input v-model="form.name" maxlength="64" show-word-limit />
          </el-form-item>
          <el-form-item :label="lt('排序值', '排序值', 'Sort Order')">
            <el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width: 100%" />
          </el-form-item>
        </el-form>

        <div class="summary-grid">
          <div class="summary-card">
            <span>{{ lt('总引用数', '總引用數', 'Total Usage') }}</span>
            <strong>{{ detail.usageCount || 0 }}</strong>
          </div>
          <div class="summary-card">
            <span>{{ lt('发现位引用', '發現位引用', 'Discover Usage') }}</span>
            <strong>{{ detail.bannerUsageCount || 0 }}</strong>
          </div>
          <div class="summary-card">
            <span>{{ lt('推荐内容引用', '推薦內容引用', 'Recommendation Usage') }}</span>
            <strong>{{ detail.recommendationUsageCount || 0 }}</strong>
          </div>
        </div>
      </template>
    </el-card>

    <el-dialog v-model="editorVisible" :title="lt('新建分类', '新建分類', 'Create Category')" width="520px">
      <el-form :model="createForm" label-width="120px">
        <el-form-item :label="lt('分类名称', '分類名稱', 'Category Name')" required>
          <el-input v-model="createForm.name" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item :label="lt('排序值', '排序值', 'Sort Order')">
          <el-input-number v-model="createForm.sortOrder" :min="0" :max="9999" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="confirmCreate">{{ lt('创建', '建立', 'Create') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18nLite } from '../../../i18n'
import { createDiscoverCategory, deleteDiscoverCategory, getDiscoverCategories, updateDiscoverCategory } from '../../../api'
import { clearCatalogCache } from './catalogRegistry'

const props = defineProps({
  mode: { type: String, default: 'list' },
  detailId: { type: [String, Number], default: null }
})

const router = useRouter()
const { lt } = useI18nLite()
const loading = ref(false)
const saving = ref(false)
const editorVisible = ref(false)
const categories = ref([])
const form = reactive({ name: '', sortOrder: 0 })
const createForm = reactive({ name: '', sortOrder: 0 })

const detail = computed(() => categories.value.find((item) => String(item.id) === String(props.detailId)) || null)

const syncDetail = () => {
  form.name = detail.value?.name || ''
  form.sortOrder = detail.value?.sortOrder || 0
}

const loadCategories = async (force = false) => {
  loading.value = true
  try {
    if (force) clearCatalogCache()
    const res = await getDiscoverCategories()
    categories.value = res.data || []
    syncDetail()
  } catch (error) {
    ElMessage.error(error.message || lt('加载分类失败', '載入分類失敗', 'Failed to load categories'))
  } finally {
    loading.value = false
  }
}

const openDetail = (row) => router.push({ name: 'OpsDiscoverCategoryDetail', params: { categoryId: row.id } })
const openCreate = () => {
  createForm.name = ''
  createForm.sortOrder = 0
  editorVisible.value = true
}

const confirmCreate = async () => {
  if (!createForm.name.trim()) {
    ElMessage.warning(lt('请输入分类名称', '請輸入分類名稱', 'Please input category name'))
    return
  }
  saving.value = true
  try {
    const res = await createDiscoverCategory({ name: createForm.name.trim(), sortOrder: createForm.sortOrder || 0 })
    categories.value = res.data || []
    editorVisible.value = false
    ElMessage.success(lt('分类已创建', '分類已建立', 'Category created'))
  } catch (error) {
    ElMessage.error(error.message || lt('创建分类失败', '建立分類失敗', 'Failed to create category'))
  } finally {
    saving.value = false
  }
}

const saveDetail = async () => {
  if (!detail.value) return
  if (!form.name.trim()) {
    ElMessage.warning(lt('请输入分类名称', '請輸入分類名稱', 'Please input category name'))
    return
  }
  saving.value = true
  try {
    const res = await updateDiscoverCategory(detail.value.id, { name: form.name.trim(), sortOrder: form.sortOrder || 0 })
    categories.value = res.data || []
    ElMessage.success(lt('分类已保存', '分類已保存', 'Category saved'))
  } catch (error) {
    ElMessage.error(error.message || lt('保存分类失败', '保存分類失敗', 'Failed to save category'))
  } finally {
    saving.value = false
  }
}

const removeCategory = async (row) => {
  if (!row) return
  try {
    await ElMessageBox.confirm(
      lt('删除后不可恢复，并且仍有引用时会被后端拦截。确认继续？', '刪除後不可恢復，且仍有引用時會被後端攔截。確認繼續？', 'Deletion is irreversible and will be blocked when references still exist. Continue?'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { type: 'warning' }
    )
    const res = await deleteDiscoverCategory(row.id)
    categories.value = res.data || []
    if (props.mode === 'detail') {
      router.push('/pro/discover-ops/categories/list')
    }
    ElMessage.success(lt('分类已删除', '分類已刪除', 'Category deleted'))
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || lt('删除分类失败', '刪除分類失敗', 'Failed to delete category'))
  }
}

watch(() => props.detailId, syncDetail)
onMounted(() => loadCategories())
</script>

<style scoped>
.catalog-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 16px; }
.head-row { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.panel-title { font-size: 18px; font-weight: 700; color: #111827; }
.panel-subtitle { margin-top: 6px; color: #6b7280; font-size: 13px; line-height: 1.6; max-width: 760px; }
.actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.summary-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-top: 16px; }
.summary-card { border-radius: 14px; padding: 16px; background: linear-gradient(135deg, #eef4ff, #f8fbff); border: 1px solid #dbeafe; display: flex; flex-direction: column; gap: 8px; }
.summary-card span { color: #6b7280; font-size: 12px; }
.summary-card strong { color: #111827; font-size: 18px; }
</style>
