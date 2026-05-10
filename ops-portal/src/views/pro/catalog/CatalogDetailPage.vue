<template>
  <component
    :is="customComponent"
    v-if="customComponent"
    mode="detail"
    :detail-id="detailId"
    :catalog-key="catalogKey"
  />
  <div v-else class="detail-page">
    <el-page-header class="detail-back" @back="goBack">
      <template #content>
        <div class="back-content">
          <span class="back-title">{{ lt(...definition.detailTitle) }}</span>
          <span class="back-subtitle">{{ detailLabel }}</span>
        </div>
      </template>
    </el-page-header>

    <el-card v-loading="loading" class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ detailLabel }}</div>
            <div class="panel-subtitle">{{ lt(...definition.listDescription) }}</div>
          </div>
          <div class="actions">
            <el-button @click="refresh">{{ lt('刷新详情', '刷新詳情', 'Refresh detail') }}</el-button>
            <el-button type="primary" plain @click="goBack">{{ lt('返回列表', '返回列表', 'Back to list') }}</el-button>
          </div>
        </div>
      </template>

      <el-empty
        v-if="!loading && !detail"
        :description="lt('未找到对应记录', '未找到對應記錄', 'Record not found')"
      />

      <template v-else-if="detail">
        <div class="summary-grid">
          <div
            v-for="card in summaryCards"
            :key="card.label"
            class="summary-card"
          >
            <span>{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
          </div>
        </div>

        <div class="section-stack">
          <el-card
            v-for="section in sections"
            :key="section.key"
            shadow="never"
            class="section-card"
          >
            <template #header>{{ lt(...section.title) }}</template>
            <el-descriptions v-if="section.type === 'fields'" :column="descriptionColumns" border>
              <el-descriptions-item
                v-for="field in section.fields"
                :key="field.label"
                :label="Array.isArray(field.label) ? lt(...field.label) : field.label"
              >
                {{ normalizeValue(field.value) }}
              </el-descriptions-item>
            </el-descriptions>

            <div v-else-if="section.type === 'text-list'" class="tag-list">
              <el-tag v-for="item in section.items" :key="item" size="small">{{ item }}</el-tag>
            </div>

            <el-table
              v-else-if="section.type === 'table'"
              :data="section.rows || []"
              size="small"
              :empty-text="lt('暂无数据', '暫無資料', 'No data')"
            >
              <el-table-column
                v-for="column in section.columns || []"
                :key="column.prop"
                :prop="column.prop"
                :label="lt(...column.label)"
                :min-width="column.minWidth || 150"
                show-overflow-tooltip
              >
                <template #default="{ row }">
                  {{ column.formatter ? column.formatter(row[column.prop], row) : normalizeValue(row[column.prop]) }}
                </template>
              </el-table-column>
            </el-table>

            <pre v-else-if="section.type === 'json'" class="json-block">{{ stringify(section.value) }}</pre>
          </el-card>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useI18nLite } from '../../../i18n'
import { clearCatalogCache, getCatalogDefinition } from './catalogRegistry'
import { isCustomCatalogKey } from './catalogCustomShared'
import { useViewport } from '../../../composables/useViewport'
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
  },
  detailId: {
    type: [String, Number],
    required: true
  }
})

const route = useRoute()
const router = useRouter()
const { lt } = useI18nLite()
const { isPhone } = useViewport()
const loading = ref(false)
const detail = ref(null)

const definition = computed(() => getCatalogDefinition(props.catalogKey))
const descriptionColumns = computed(() => (isPhone.value ? 1 : 2))
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

const detailLabel = computed(() => {
  const source = detail.value || {}
  return source.name || source.title || source.appId || source.slotCode || source.templateCode || source.ruleCode || source.planName || source.channelName || source.featureName || source.pageName || source.username || source.ticketNo || source.id || '-'
})

const summaryCards = computed(() => {
  const source = detail.value || {}
  return [
    { label: lt('主标识', '主標識', 'Primary ID'), value: source.appId || source.templateCode || source.ruleCode || source.id || '-' },
    { label: lt('状态', '狀態', 'Status'), value: source.status || source.displayStatus || source.accountStatus || source.appealStatus || source.profileStatus || '-' },
    { label: lt('更新时间', '更新時間', 'Updated'), value: normalizeValue(source.updatedAt || source.createdAt || source.submittedAt) }
  ]
})

const sections = computed(() => {
  if (!definition.value || !detail.value) return []
  return definition.value.buildSections(detail.value) || []
})

const normalizeValue = (value) => {
  if (value == null || value === '') return '-'
  if (Array.isArray(value)) return value.join(', ')
  if (typeof value === 'object') return stringify(value)
  return String(value)
}

const stringify = (value) => {
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

const load = async (force = false) => {
  if (customComponent.value) return
  if (!definition.value) return
  loading.value = true
  try {
    if (force) {
      clearCatalogCache()
    }
    detail.value = await definition.value.loadDetail(props.detailId)
  } catch (error) {
    ElMessage.error(error.message || lt('加载详情失败', '載入詳情失敗', 'Failed to load detail'))
    detail.value = null
  } finally {
    loading.value = false
  }
}

const refresh = () => load(true)

const goBack = () => {
  const active = route.meta.activeMenu
  if (active) {
    router.push(active)
    return
  }
  router.back()
}

watch(() => [props.catalogKey, props.detailId], () => load())

onMounted(() => load())
</script>

<style scoped>
.detail-page { display: flex; flex-direction: column; gap: 16px; }
.detail-back { padding: 4px 4px 0; }
.back-content { display: flex; flex-direction: column; gap: 4px; }
.back-title { font-size: 18px; font-weight: 700; color: #111827; }
.back-subtitle { color: #6b7280; font-size: 13px; }
.panel-card { border-radius: 16px; }
.head-row { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.panel-title { font-size: 20px; font-weight: 700; color: #111827; }
.panel-subtitle { margin-top: 6px; color: #6b7280; font-size: 13px; line-height: 1.6; max-width: 760px; }
.actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.summary-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.summary-card { border-radius: 14px; padding: 16px; background: linear-gradient(135deg, #eef4ff, #f8fbff); border: 1px solid #dbeafe; display: flex; flex-direction: column; gap: 8px; }
.summary-card span { color: #6b7280; font-size: 12px; }
.summary-card strong { color: #111827; font-size: 18px; }
.section-stack { display: flex; flex-direction: column; gap: 16px; }
.section-card { border-radius: 14px; }
.tag-list { display: flex; gap: 8px; flex-wrap: wrap; }
.json-block { margin: 0; padding: 14px; border-radius: 12px; background: #0f172a; color: #e2e8f0; overflow: auto; font-size: 12px; line-height: 1.6; }
@media (max-width: 980px) {
  .summary-grid { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .head-row { flex-direction: column; }
  .actions { width: 100%; }
}
</style>
