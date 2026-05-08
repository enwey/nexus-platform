<template>
  <div class="catalog-page">
    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ mode === 'list' ? lt('游戏库运营位列表', '遊戲庫營運位列表', 'Library Slot List') : detailLabel }}</div>
            <div class="panel-subtitle">
              {{ lt('围绕客户端真实的游戏库入口维护“新手必玩”和“大家都在玩”。支持手工精选、排序和回退到自动兜底逻辑。', '圍繞客戶端真實的遊戲庫入口維護「新手必玩」和「大家都在玩」。支援手工精選、排序和回退到自動兜底邏輯。', 'Manage the real library slots consumed by clients, including curated ordering and fallback-to-auto behavior.') }}
            </div>
          </div>
          <div class="actions">
            <el-button @click="loadWorkspace(true)">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
            <el-button v-if="mode === 'detail'" type="primary" :loading="saving" @click="saveSlot">{{ lt('保存配置', '保存配置', 'Save Slot') }}</el-button>
          </div>
        </div>
      </template>

      <template v-if="mode === 'list'">
        <el-table :data="slotRows" v-loading="loading" row-key="id" @row-click="openDetail">
          <el-table-column prop="slotName" :label="lt('运营位', '營運位', 'Slot')" min-width="180" />
          <el-table-column prop="sourceText" :label="lt('内容来源', '內容來源', 'Source')" min-width="220" />
          <el-table-column prop="gameCount" :label="lt('精选数', '精選數', 'Curated Count')" width="120" />
          <el-table-column :label="lt('当前策略', '當前策略', 'Current Strategy')" min-width="220">
            <template #default="{ row }">
              {{ row.gameCount ? lt('运营精选优先', '營運精選優先', 'Curated first') : lt('自动热度兜底', '自動熱度兜底', 'Automatic fallback') }}
            </template>
          </el-table-column>
          <el-table-column :label="lt('操作', '操作', 'Actions')" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="openDetail(row)">{{ lt('查看详情', '查看詳情', 'View detail') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template v-else-if="detailSlot">
        <div class="summary-grid">
          <div class="summary-card">
            <span>{{ lt('精选条目', '精選條目', 'Curated Items') }}</span>
            <strong>{{ selectedGames.length }}</strong>
          </div>
          <div class="summary-card">
            <span>{{ lt('客户端策略', '客戶端策略', 'Client Strategy') }}</span>
            <strong>{{ selectedGames.length ? lt('运营精选', '營運精選', 'Curated') : lt('自动兜底', '自動兜底', 'Fallback') }}</strong>
          </div>
          <div class="summary-card">
            <span>{{ lt('预览游戏', '預覽遊戲', 'Preview Games') }}</span>
            <strong>{{ previewGames.length }}</strong>
          </div>
        </div>

        <el-row :gutter="16">
          <el-col :xs="24" :lg="12">
            <el-card shadow="never" class="section-card">
              <template #header>{{ lt('精选配置', '精選配置', 'Curated Config') }}</template>
              <el-form label-width="120px">
                <el-form-item :label="lt('运营说明', '營運說明', 'Ops Note')">
                  <div class="field-hint">
                    {{ lt('留空时客户端自动回退到“热度聚合/最新通过游戏”。配置后按你设定的顺序展示。', '留空時客戶端自動回退到「熱度聚合/最新通過遊戲」。配置後按你設定的順序展示。', 'When empty, clients fall back to automatic trending/latest logic. Once configured, items follow this curated order.') }}
                  </div>
                </el-form-item>
                <el-form-item :label="lt('添加游戏', '新增遊戲', 'Add Game')">
                  <el-select v-model="selectedAppId" filterable clearable style="width: 100%" :placeholder="lt('选择游戏后点击添加', '選擇遊戲後點擊新增', 'Pick a game and add it')">
                    <el-option
                      v-for="item in selectableGames"
                      :key="item.appId"
                      :label="`${item.name} (${item.appId})`"
                      :value="item.appId"
                    />
                  </el-select>
                </el-form-item>
                <el-button type="primary" plain :disabled="!selectedAppId" @click="appendGame">{{ lt('添加到精选', '加入精選', 'Add to Curated') }}</el-button>
                <el-button plain @click="clearCurated">{{ lt('清空并回退自动', '清空並回退自動', 'Clear and Fallback') }}</el-button>
              </el-form>
            </el-card>
          </el-col>
          <el-col :xs="24" :lg="12">
            <el-card shadow="never" class="section-card">
              <template #header>{{ lt('前端预览', '前端預覽', 'Frontend Preview') }}</template>
              <div class="preview-stack">
                <div v-for="item in previewGames" :key="item.appId" class="preview-card">
                  <strong>{{ item.name }}</strong>
                  <span>{{ item.appId }}</span>
                </div>
                <el-empty v-if="!previewGames.length" :description="lt('当前没有可展示的精选游戏', '目前沒有可展示的精選遊戲', 'No curated games to preview')" />
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="never" class="section-card">
          <template #header>{{ lt('精选顺序', '精選順序', 'Curated Order') }}</template>
          <el-table :data="selectedGames" row-key="appId" :empty-text="lt('当前未配置精选，将由客户端自动兜底。', '當前未配置精選，將由客戶端自動兜底。', 'No curated games configured. Clients will use fallback logic.')">
            <el-table-column type="index" width="60" :label="lt('序号', '序號', 'No.')" />
            <el-table-column prop="name" :label="lt('游戏名', '遊戲名', 'Game')" min-width="220" />
            <el-table-column prop="appId" label="AppID" min-width="160" />
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="180" fixed="right">
              <template #default="{ $index }">
                <el-button link @click="moveGame($index, -1)" :disabled="$index === 0">{{ lt('上移', '上移', 'Move Up') }}</el-button>
                <el-button link @click="moveGame($index, 1)" :disabled="$index === selectedGames.length - 1">{{ lt('下移', '下移', 'Move Down') }}</el-button>
                <el-button link type="danger" @click="removeGame($index)">{{ lt('移除', '移除', 'Remove') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18nLite } from '../../../i18n'
import { clearCatalogCache } from './catalogRegistry'
import { fetchDiscoverWorkspace, saveDiscoverSection } from '../../../utils/discoverOps'

const props = defineProps({
  mode: { type: String, default: 'list' },
  detailId: { type: [String, Number], default: null }
})

const router = useRouter()
const { lt } = useI18nLite()
const loading = ref(false)
const saving = ref(false)
const workspace = ref(null)
const selectedAppId = ref('')
const selectedIds = ref([])

const slotRows = computed(() => {
  const data = workspace.value
  if (!data) return []
  return [
    {
      id: 'NEWBIE_MUST_PLAY',
      slotName: lt('新手必玩', '新手必玩', 'Newbie Must Play'),
      sourceText: lt('运营精选，空列表时自动兜底', '營運精選，空列表時自動兜底', 'Curated list with automatic fallback'),
      gameCount: (data.newbieAppIds || []).length
    },
    {
      id: 'EVERYONE_PLAYING',
      slotName: lt('大家都在玩', '大家都在玩', 'Everyone Playing'),
      sourceText: lt('运营精选优先，热度逻辑兜底', '營運精選優先，熱度邏輯兜底', 'Curated first with trending fallback'),
      gameCount: (data.everyoneAppIds || []).length
    }
  ]
})

const detailSlot = computed(() => slotRows.value.find((item) => item.id === String(props.detailId)) || null)
const detailLabel = computed(() => detailSlot.value?.slotName || '-')
const selectableGames = computed(() => {
  const used = new Set(selectedIds.value)
  return (workspace.value?.gameOptions || []).filter((item) => !used.has(item.appId))
})
const selectedGames = computed(() =>
  selectedIds.value
    .map((appId) => workspace.value?.gameOptions?.find((item) => item.appId === appId))
    .filter(Boolean)
)
const previewGames = computed(() => selectedGames.value.slice(0, 6))

const syncDetailIds = () => {
  if (!detailSlot.value || !workspace.value) return
  selectedIds.value = detailSlot.value.id === 'NEWBIE_MUST_PLAY'
    ? [...(workspace.value.newbieAppIds || [])]
    : [...(workspace.value.everyoneAppIds || [])]
}

const loadWorkspace = async (force = false) => {
  loading.value = true
  try {
    if (force) clearCatalogCache()
    workspace.value = await fetchDiscoverWorkspace()
    syncDetailIds()
  } catch (error) {
    ElMessage.error(error.message || lt('加载游戏库运营失败', '載入遊戲庫營運失敗', 'Failed to load library ops'))
  } finally {
    loading.value = false
  }
}

const openDetail = (row) => {
  router.push({ name: 'OpsLibrarySlotDetail', params: { slotId: row.id } })
}

const appendGame = () => {
  if (!selectedAppId.value) return
  selectedIds.value = [...selectedIds.value, selectedAppId.value]
  selectedAppId.value = ''
}

const moveGame = (index, offset) => {
  const nextIndex = index + offset
  if (nextIndex < 0 || nextIndex >= selectedIds.value.length) return
  const next = [...selectedIds.value]
  ;[next[index], next[nextIndex]] = [next[nextIndex], next[index]]
  selectedIds.value = next
}

const removeGame = async (index) => {
  try {
    await ElMessageBox.confirm(
      lt('确认移除该精选游戏？', '確認移除此精選遊戲？', 'Remove this curated game?'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { type: 'warning' }
    )
    const next = [...selectedIds.value]
    next.splice(index, 1)
    selectedIds.value = next
  } catch {}
}

const clearCurated = async () => {
  try {
    await ElMessageBox.confirm(
      lt('确认清空精选列表并回退到客户端自动逻辑？', '確認清空精選列表並回退到客戶端自動邏輯？', 'Clear the curated list and fall back to automatic client logic?'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { type: 'warning' }
    )
    selectedIds.value = []
  } catch {}
}

const saveSlot = async () => {
  if (!detailSlot.value) return
  saving.value = true
  try {
    workspace.value = await saveDiscoverSection(
      detailSlot.value.id === 'NEWBIE_MUST_PLAY'
        ? { newbieAppIds: [...selectedIds.value] }
        : { everyoneAppIds: [...selectedIds.value] }
    )
    syncDetailIds()
    ElMessage.success(lt('游戏库运营位已保存', '遊戲庫營運位已保存', 'Library slot saved'))
  } catch (error) {
    ElMessage.error(error.message || lt('保存失败', '保存失敗', 'Failed to save'))
  } finally {
    saving.value = false
  }
}

watch(() => props.detailId, syncDetailIds)
onMounted(() => loadWorkspace())
</script>

<style scoped>
.catalog-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card, .section-card { border-radius: 16px; }
.head-row { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.panel-title { font-size: 18px; font-weight: 700; color: #111827; }
.panel-subtitle { margin-top: 6px; color: #6b7280; font-size: 13px; line-height: 1.6; max-width: 760px; }
.actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.summary-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.summary-card { border-radius: 14px; padding: 16px; background: linear-gradient(135deg, #eef4ff, #f8fbff); border: 1px solid #dbeafe; display: flex; flex-direction: column; gap: 8px; }
.summary-card span { color: #6b7280; font-size: 12px; }
.summary-card strong { color: #111827; font-size: 18px; }
.field-hint { color: #6b7280; line-height: 1.7; }
.preview-stack { display: flex; flex-direction: column; gap: 10px; }
.preview-card { border: 1px solid #e5e7eb; border-radius: 12px; padding: 12px 14px; display: flex; flex-direction: column; gap: 4px; background: #fafafa; }
@media (max-width: 980px) {
  .summary-grid { grid-template-columns: 1fr; }
}
</style>
