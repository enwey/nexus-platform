<template>
  <div class="config-page">
    <el-card v-loading="loading" class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ lt(...definition.title) }}</div>
            <div class="panel-subtitle">{{ lt(...definition.description) }}</div>
          </div>
          <el-button @click="load(true)">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
        </div>
      </template>

      <el-empty v-if="!loading && !detail" :description="lt('暂无配置数据', '暫無配置資料', 'No config data')" />

      <div v-else class="section-stack">
        <el-card
          v-for="section in sections"
          :key="section.key"
          shadow="never"
          class="section-card"
        >
          <template #header>{{ lt(...section.title) }}</template>
          <el-descriptions :column="descriptionColumns" border>
            <el-descriptions-item
              v-for="field in section.fields"
              :key="field.label"
              :label="Array.isArray(field.label) ? lt(...field.label) : field.label"
            >
              {{ Array.isArray(field.value) ? field.value[0] : field.value }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18nLite } from '../../../i18n'
import { getConfigDefinition } from './catalogRegistry'
import { useViewport } from '../../../composables/useViewport'

const props = defineProps({
  configKey: {
    type: String,
    required: true
  }
})

const { lt } = useI18nLite()
const { isPhone } = useViewport()
const loading = ref(false)
const detail = ref(null)

const definition = computed(() => getConfigDefinition(props.configKey))
const descriptionColumns = computed(() => (isPhone.value ? 1 : 2))
const sections = computed(() => {
  if (!definition.value || !detail.value) return []
  return definition.value.sections(detail.value)
})

const load = async () => {
  if (!definition.value) return
  loading.value = true
  try {
    detail.value = await definition.value.load()
  } catch (error) {
    ElMessage.error(error.message || lt('加载配置失败', '載入配置失敗', 'Failed to load config'))
    detail.value = null
  } finally {
    loading.value = false
  }
}

watch(() => props.configKey, load)
onMounted(load)
</script>

<style scoped>
.config-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 16px; }
.head-row { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.panel-title { font-size: 18px; font-weight: 700; color: #111827; }
.panel-subtitle { margin-top: 6px; color: #6b7280; font-size: 13px; line-height: 1.6; max-width: 720px; }
.section-stack { display: flex; flex-direction: column; gap: 16px; }
.section-card { border-radius: 14px; }
@media (max-width: 768px) {
  .head-row { flex-direction: column; }
}
</style>
