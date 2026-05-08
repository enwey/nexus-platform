<template>
  <component
    :is="customComponent"
    v-if="customComponent"
    mode="list"
    :catalog-key="catalogKey"
  />
  <div v-else class="catalog-page">
    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ lt(...definition.listTitle) }}</div>
            <div class="panel-subtitle">{{ lt(...definition.listDescription) }}</div>
          </div>
          <div class="actions">
            <el-input
              v-model.trim="keyword"
              clearable
              style="width: 260px"
              :placeholder="lt('搜索关键字段', '搜尋關鍵欄位', 'Search key fields')"
            />
            <el-button @click="refresh">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <div class="summary-row">
        <div class="summary-chip">
          {{ lt('记录数', '記錄數', 'Records') }} {{ filteredRows.length }}
        </div>
        <div class="summary-chip subtle">
          {{ lt('列表点击后进入详情页统一查看关联信息与日志。', '列表點擊後進入詳情頁統一查看關聯資訊與日誌。', 'Rows open a unified detail page with relations and logs.') }}
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="filteredRows"
        row-key="id"
        :empty-text="lt('暂无数据', '暫無資料', 'No data')"
        @row-click="openDetail"
      >
        <el-table-column
          v-for="column in definition.listColumns"
          :key="column.prop"
          :prop="column.prop"
          :label="lt(...column.label)"
          :min-width="column.minWidth || 160"
          :width="column.width"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <template v-if="column.type === 'tag'">
              <el-tag :type="tagType(row[column.prop])">{{ formatCell(row, column) }}</el-tag>
            </template>
            <template v-else>
              {{ formatCell(row, column) }}
            </template>
          </template>
        </el-table-column>
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openDetail(row)">{{ lt('查看详情', '查看詳情', 'View detail') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useI18nLite } from '../../../i18n'
import { clearCatalogCache, getCatalogDefinition } from './catalogRegistry'
import { isCustomCatalogKey } from './catalogCustomShared'
import LibrarySlotWorkspace from './LibrarySlotWorkspace.vue'
import DiscoverSlotWorkspace from './DiscoverSlotWorkspace.vue'
import DiscoverCategoryWorkspace from './DiscoverCategoryWorkspace.vue'
import DiscoverContentWorkspace from './DiscoverContentWorkspace.vue'
import RecommendationItemWorkspace from './RecommendationItemWorkspace.vue'
import LaunchAdWorkspace from './LaunchAdWorkspace.vue'

const props = defineProps({
  catalogKey: {
    type: String,
    required: true
  }
})

const router = useRouter()
const { lt } = useI18nLite()
const keyword = ref('')
const rows = ref([])
const loading = ref(false)

const definition = computed(() => getCatalogDefinition(props.catalogKey))
const customComponent = computed(() => {
  if (!isCustomCatalogKey(props.catalogKey)) return null
  return {
    librarySlots: LibrarySlotWorkspace,
    discoverSlots: DiscoverSlotWorkspace,
    discoverCategories: DiscoverCategoryWorkspace,
    discoverContent: DiscoverContentWorkspace,
    recommendationItems: RecommendationItemWorkspace,
    launchAds: LaunchAdWorkspace
  }[props.catalogKey] || null
})

const searchableText = (row) => Object.values(row || {})
  .flatMap((value) => (Array.isArray(value) ? value : [value]))
  .filter((value) => value != null && ['string', 'number', 'boolean'].includes(typeof value))
  .join(' ')
  .toLowerCase()

const filteredRows = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  if (!key) return rows.value
  return rows.value.filter((row) => searchableText(row).includes(key))
})

const tagType = (value) => {
  const text = String(value || '').toUpperCase()
  if (['APPROVED', 'ACTIVE', 'PUBLISHED', 'VISIBLE', 'SUCCESS', 'ENABLED', 'VERIFIED', 'RESOLVED', 'LIVE', 'RUNNING'].includes(text)) return 'success'
  if (['PENDING', 'DRAFT', 'GRAY', 'SCHEDULED', 'OPEN', 'MITIGATING'].includes(text)) return 'warning'
  if (['REJECTED', 'BLOCKED', 'BANNED', 'FAILED', 'DISABLED', 'PAUSED', 'EXPIRED', 'CANCELLED'].includes(text)) return 'danger'
  return 'info'
}

const formatCell = (row, column) => {
  const value = row?.[column.prop]
  if (typeof column.formatter === 'function') {
    return column.formatter(value, row)
  }
  if (Array.isArray(value)) {
    return value[0] || '-'
  }
  if (value === '' || value == null) return '-'
  return value
}

const load = async (force = false) => {
  if (customComponent.value) return
  if (!definition.value) return
  loading.value = true
  try {
    if (force) {
      clearCatalogCache()
    }
    rows.value = await definition.value.loadList(force)
  } catch (error) {
    ElMessage.error(error.message || lt('加载列表失败', '載入列表失敗', 'Failed to load list'))
    rows.value = []
  } finally {
    loading.value = false
  }
}

const refresh = () => load(true)

const openDetail = (row) => {
  if (customComponent.value) return
  if (!definition.value?.detailRouteName) return
  const id = row?.[definition.value.idKey || 'id'] ?? row?.id
  if (id == null) return
  router.push({ name: definition.value.detailRouteName, params: buildDetailParams(id) })
}

const buildDetailParams = (id) => {
  switch (props.catalogKey) {
    case 'games':
    case 'gameReviews':
      return { gameId: id, reviewId: id }
    case 'versions':
      return { versionId: id }
    case 'versionReviews':
      return { reviewId: id }
    case 'certificationReviews':
    case 'developers':
      return { developerId: id, reviewId: id }
    case 'appealReviews':
      return { appealId: id }
    case 'librarySlots':
    case 'discoverSlots':
      return { slotId: id }
    case 'discoverCategories':
    case 'categoryDictionary':
      return { categoryId: id }
    case 'discoverContent':
      return { contentId: id }
    case 'recommendationItems':
      return { itemId: id }
    case 'launchAds':
      return { launchAdId: id }
    case 'versionPolicies':
      return { policyId: id }
    case 'channels':
      return { channelId: id }
    case 'featureToggles':
      return { toggleId: id }
    case 'circuitBreakers':
      return { breakerId: id }
    case 'compatibilityBlocks':
      return { ruleId: id }
    case 'grayRollouts':
      return { rolloutId: id }
    case 'tickets':
      return { ticketId: id }
    case 'notices':
      return { noticeId: id }
    case 'noticeTemplates':
    case 'reviewTemplates':
      return { templateId: id }
    case 'incidents':
      return { incidentId: id }
    case 'loginAnomalies':
      return { eventId: id }
    case 'accessRules':
      return { ruleId: id }
    case 'auditLogs':
    case 'smsLogs':
      return { logId: id }
    default:
      return { id }
  }
}

watch(() => props.catalogKey, () => {
  keyword.value = ''
  load()
})

onMounted(() => {
  load()
})
</script>

<style scoped>
.catalog-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 16px; }
.head-row { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.panel-title { font-size: 18px; font-weight: 700; color: #111827; }
.panel-subtitle { margin-top: 6px; color: #6b7280; font-size: 13px; line-height: 1.6; max-width: 760px; }
.actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.summary-row { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.summary-chip { border-radius: 999px; background: #eef4ff; color: #1d4ed8; padding: 8px 14px; font-size: 13px; font-weight: 600; }
.summary-chip.subtle { background: #f3f4f6; color: #4b5563; font-weight: 500; }
</style>
