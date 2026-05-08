<template>
  <div class="pro-page">
    <el-card>
      <template #header>
        <div class="head-row">
          <div>
            <div class="module-title">{{ lt('推荐运营总览', '推薦營運總覽', 'Recommendation Ops Overview') }}</div>
            <div class="module-subtitle">
              {{ lt('将分类、广告位与推荐列表分开治理，便于后续扩展更多投放位和内容模型。', '將分類、廣告位與推薦列表分開治理，便於後續擴展更多投放位和內容模型。', 'Manage categories, ad placements, and recommendation lists separately to support future expansion.') }}
            </div>
          </div>
          <el-button :loading="loading" @click="loadWorkspace">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
        </div>
      </template>

      <el-alert
        v-if="!categoryCount"
        :title="lt('当前还没有推荐分类，建议先创建分类，再维护广告位和推荐内容。', '當前還沒有推薦分類，建議先建立分類，再維護廣告位和推薦內容。', 'No recommendation categories yet. Create categories first before managing ads and recommendation content.')"
        type="warning"
        show-icon
        class="overview-alert"
      />

      <div class="overview-grid" v-loading="loading">
        <div v-for="item in modules" :key="item.path" class="module-card">
          <div class="card-name">{{ item.title }}</div>
          <div class="card-count">{{ item.count }}</div>
          <div class="card-desc">{{ item.description }}</div>
          <el-button type="primary" plain @click="router.push(item.path)">{{ lt('进入模块', '進入模組', 'Open Module') }}</el-button>
        </div>
      </div>

      <el-divider />

      <div class="ops-grid">
        <div class="ops-summary-card">
          <div class="summary-label">{{ lt('待生效发布单', '待生效發佈單', 'Pending Release Orders') }}</div>
          <div class="summary-value">{{ scheduledOrderCount }}</div>
          <div class="summary-meta">{{ lt('覆盖广告与推荐列表的定时生效任务', '涵蓋廣告與推薦列表的定時生效任務', 'Scheduled go-live tasks across banner and recommendation scopes.') }}</div>
        </div>
        <div class="ops-summary-card">
          <div class="summary-label">{{ lt('活跃实验位', '活躍實驗位', 'Active Experiments') }}</div>
          <div class="summary-value">{{ activeExperimentCount }}</div>
          <div class="summary-meta">{{ lt('用于验证不同内容组合的实验位', '用於驗證不同內容組合的實驗位', 'Experiment slots validating alternative content mixes.') }}</div>
        </div>
        <div class="ops-summary-card">
          <div class="summary-label">{{ lt('待生效内容', '待生效內容', 'Scheduled Content Items') }}</div>
          <div class="summary-value">{{ analytics.totalScheduledItems }}</div>
          <div class="summary-meta">{{ lt('已经进入排期但尚未在前端生效的推荐内容数量。', '已經進入排期但尚未在前端生效的推薦內容數量。', 'Recommendation items that are scheduled but not yet visible on the frontend.') }}</div>
        </div>
        <div class="ops-summary-card">
          <div class="summary-label">{{ lt('实验流量占比', '實驗流量佔比', 'Experiment Traffic') }}</div>
          <div class="summary-value">{{ analytics.totalExperimentTraffic }}%</div>
          <div class="summary-meta">{{ lt('当前实验位累计占用流量，用于快速回收和压降。', '當前實驗位累計佔用流量，用於快速回收和壓降。', 'Combined traffic reserved by experiments for recovery decisions.') }}</div>
        </div>
      </div>

      <el-card shadow="never" class="analytics-card">
        <template #header>
          <div class="panel-title">{{ lt('效果与排期回收', '效果與排期回收', 'Effect & Schedule Recovery') }}</div>
        </template>
        <el-table :data="analytics.scopes || []" :empty-text="lt('暂无效果数据', '暫無效果資料', 'No analytics yet')">
          <el-table-column prop="scopeName" :label="lt('范围', '範圍', 'Scope')" min-width="160" />
          <el-table-column prop="liveItems" :label="lt('在线', '在線', 'Live')" width="90" />
          <el-table-column prop="scheduledItems" :label="lt('排期中', '排期中', 'Scheduled')" width="100" />
          <el-table-column prop="pausedItems" :label="lt('暂停', '暫停', 'Paused')" width="90" />
          <el-table-column prop="expiredItems" :label="lt('过期', '過期', 'Expired')" width="90" />
          <el-table-column prop="pendingPublishOrders" :label="lt('待生效发布单', '待生效發佈單', 'Pending Orders')" width="130" />
          <el-table-column prop="activeExperiments" :label="lt('活跃实验', '活躍實驗', 'Active Exp.')" width="110" />
          <el-table-column prop="experimentTrafficPercent" :label="lt('实验流量', '實驗流量', 'Traffic')" width="100">
            <template #default="{ row }">{{ row.experimentTrafficPercent }}%</template>
          </el-table-column>
          <el-table-column :label="lt('最近排期', '最近排期', 'Next Publish')" min-width="180">
            <template #default="{ row }">{{ row.nextPublishAt || '-' }}</template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-row :gutter="16" class="ops-panels">
        <el-col :xs="24" :lg="12">
          <el-card shadow="never">
            <template #header>
              <div class="head-row">
                <div class="panel-title">{{ lt('最近发布单', '最近發佈單', 'Recent Release Orders') }}</div>
                <el-button size="small" type="warning" plain :disabled="!selectedPublishOrderIds.length" @click="batchCancelPublishOrders">
                  {{ lt('批量取消', '批量取消', 'Batch Cancel') }}
                </el-button>
              </div>
            </template>
            <el-table :data="publishOrders.slice(0, 8)" row-key="id" :empty-text="lt('暂无发布单', '暫無發佈單', 'No release orders')" @selection-change="onPublishOrderSelectionChange">
              <el-table-column type="selection" width="50" />
              <el-table-column prop="orderNo" :label="lt('发布单号', '發佈單號', 'Order No.')" min-width="150" />
              <el-table-column prop="scopeName" :label="lt('范围', '範圍', 'Scope')" min-width="140" />
              <el-table-column prop="status" :label="lt('状态', '狀態', 'Status')" width="110" />
              <el-table-column :label="lt('生效时间', '生效時間', 'Effective At')" width="180">
                <template #default="{ row }">{{ row.effectiveAt || '-' }}</template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="12">
          <el-card shadow="never">
            <template #header>
              <div class="head-row">
                <div class="panel-title">{{ lt('实验位列表', '實驗位列表', 'Experiment Slots') }}</div>
                <div class="actions">
                  <el-button size="small" plain :disabled="!selectedExperimentIds.length" @click="batchUpdateExperimentStatus('PAUSED')">
                    {{ lt('批量暂停', '批量暫停', 'Batch Pause') }}
                  </el-button>
                  <el-button size="small" type="success" plain :disabled="!selectedExperimentIds.length" @click="batchUpdateExperimentStatus('ACTIVE')">
                    {{ lt('批量激活', '批量激活', 'Batch Activate') }}
                  </el-button>
                </div>
              </div>
            </template>
            <el-table :data="experiments.slice(0, 8)" row-key="id" :empty-text="lt('暂无实验位', '暫無實驗位', 'No experiments')" @selection-change="onExperimentSelectionChange">
              <el-table-column type="selection" width="50" />
              <el-table-column prop="experimentName" :label="lt('实验名称', '實驗名稱', 'Experiment')" min-width="160" />
              <el-table-column prop="scopeCode" :label="lt('范围', '範圍', 'Scope')" width="140" />
              <el-table-column prop="trafficPercent" :label="lt('流量', '流量', 'Traffic')" width="100">
                <template #default="{ row }">{{ row.trafficPercent }}%</template>
              </el-table-column>
              <el-table-column prop="status" :label="lt('状态', '狀態', 'Status')" width="110" />
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <div class="slot-section">
        <div class="section-title">{{ lt('前端区块总开关', '前端區塊總開關', 'Frontend Placement Kill Switches') }}</div>
        <div class="section-subtitle">
          {{ lt('运营可随时关闭某个前端区块，关闭后前端不再展示该区块内容，也不会自动回退到默认内容。', '營運可隨時關閉某個前端區塊，關閉後前端不再展示該區塊內容，也不會自動回退到預設內容。', 'Operations can disable a frontend block at any time. Once disabled, the frontend stops rendering it and will not fall back to default content.') }}
        </div>
        <el-table :data="slotControls" style="margin-top: 12px" :empty-text="lt('暂无区块配置', '暫無區塊設定', 'No placement controls')">
          <el-table-column prop="name" :label="lt('区块', '區塊', 'Placement')" min-width="220" />
          <el-table-column prop="slotCode" :label="lt('编码', '編碼', 'Code')" min-width="180" />
          <el-table-column :label="lt('页面 / 位置', '頁面 / 位置', 'Page / Position')" min-width="180">
            <template #default="{ row }">{{ row.pageCode }} / {{ row.positionCode }}</template>
          </el-table-column>
          <el-table-column :label="lt('状态', '狀態', 'Status')" width="120">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'danger'">
                {{ row.enabled ? lt('开启', '開啟', 'Enabled') : lt('关闭', '關閉', 'Disabled') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="lt('操作', '操作', 'Actions')" width="180" fixed="right">
            <template #default="{ row }">
              <el-switch
                :model-value="row.enabled"
                :loading="switchingSlotCode === row.slotCode"
                inline-prompt
                :active-text="lt('开', '開', 'On')"
                :inactive-text="lt('关', '關', 'Off')"
                @change="toggleSlot(row, $event)"
              />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18nLite } from '../../i18n'
import { fetchDiscoverWorkspace } from '../../utils/discoverOps'
import { batchOperateDiscover, getDiscoverOpsAnalytics, updateDiscoverSlotControl } from '../../api'

const router = useRouter()
const { lt } = useI18nLite()
const loading = ref(false)
const switchingSlotCode = ref('')
const stats = ref({
  categoryCount: 0,
  gameBannerCount: 0,
  discoverBannerCount: 0,
  communityCount: 0
})
const slotControls = ref([])
const publishOrders = ref([])
const experiments = ref([])
const selectedPublishOrderIds = ref([])
const selectedExperimentIds = ref([])
const analytics = ref({
  totalScheduledItems: 0,
  totalExperimentTraffic: 0,
  scopes: []
})

const categoryCount = computed(() => stats.value.categoryCount)
const scheduledOrderCount = computed(() => publishOrders.value.filter((item) => item.status === 'SCHEDULED').length)
const activeExperimentCount = computed(() => experiments.value.filter((item) => item.status === 'ACTIVE').length)

const modules = computed(() => [
  {
    path: '/pro/recommend/categories',
    title: lt('推荐分类', '推薦分類', 'Recommendation Categories'),
    count: stats.value.categoryCount,
    description: lt('定义内容分类、广告标签和推荐归属。', '定義內容分類、廣告標籤和推薦歸屬。', 'Define content categories, ad tags, and recommendation ownership.')
  },
  {
    path: '/pro/recommend/game-banners',
    title: lt('游戏页广告', '遊戲頁廣告', 'Game Ads'),
    count: stats.value.gameBannerCount,
    description: lt('独立维护游戏页冷启动顶部广告位。', '獨立維護遊戲頁冷啟動頂部廣告位。', 'Manage game-page cold-start banners in a dedicated workspace.')
  },
  {
    path: '/pro/recommend/discover-banners',
    title: lt('发现页广告', '發現頁廣告', 'Discover Ads'),
    count: stats.value.discoverBannerCount,
    description: lt('独立维护发现页顶部焦点广告位。', '獨立維護發現頁頂部焦點廣告位。', 'Manage discover-page hero banners in a dedicated workspace.')
  },
  {
    path: '/pro/recommend/community',
    title: lt('推荐列表', '推薦列表', 'Recommendation Lists'),
    count: stats.value.communityCount,
    description: lt('独立维护社区推荐内容及图文详情。', '獨立維護社群推薦內容及圖文詳情。', 'Manage recommendation lists and editorial content separately.')
  }
])

const loadWorkspace = async () => {
  loading.value = true
  try {
    const workspace = await fetchDiscoverWorkspace()
    const analyticsRes = await getDiscoverOpsAnalytics()
    slotControls.value = workspace.slotControls || []
    publishOrders.value = workspace.publishOrders || []
    experiments.value = workspace.experiments || []
    analytics.value = analyticsRes.data || analytics.value
    stats.value = {
      categoryCount: workspace.categoryOptions.length,
      gameBannerCount: workspace.gameTopBanners.length,
      discoverBannerCount: workspace.discoverTopBanners.length,
      communityCount: workspace.recommendList.length
    }
  } catch (error) {
    ElMessage.error(error.message || lt('加载推荐运营总览失败', '載入推薦營運總覽失敗', 'Failed to load recommendation overview'))
  } finally {
    loading.value = false
  }
}

const toggleSlot = async (row, nextEnabled) => {
  const revert = () => {
    slotControls.value = slotControls.value.map((item) =>
      item.slotCode === row.slotCode ? { ...item, enabled: row.enabled } : item
    )
  }

  try {
    let reason = ''
    if (!nextEnabled) {
      const input = await ElMessageBox.prompt(
        lt('关闭区块需要填写原因，至少 4 个字符。', '關閉區塊需要填寫原因，至少 4 個字元。', 'Disabling a placement requires a reason of at least 4 characters.'),
        lt('二次确认', '二次確認', 'Secondary Confirmation'),
        {
          confirmButtonText: lt('确认关闭', '確認關閉', 'Disable'),
          cancelButtonText: lt('取消', '取消', 'Cancel'),
          inputPattern: /^.{4,200}$/,
          inputErrorMessage: lt('请填写 4-200 字原因', '請填寫 4-200 字原因', 'Enter a reason between 4 and 200 characters'),
          type: 'warning'
        }
      )
      reason = input.value
    } else {
      await ElMessageBox.confirm(
        lt('确认重新开启该前端区块？开启后前端会重新按配置展示内容。', '確認重新開啟該前端區塊？開啟後前端會重新按設定展示內容。', 'Re-enable this frontend placement? The frontend will render content from configuration again.'),
        lt('二次确认', '二次確認', 'Secondary Confirmation'),
        {
          confirmButtonText: lt('确认开启', '確認開啟', 'Enable'),
          cancelButtonText: lt('取消', '取消', 'Cancel'),
          type: 'warning'
        }
      )
    }

    switchingSlotCode.value = row.slotCode
    const res = await updateDiscoverSlotControl(row.slotCode, { enabled: nextEnabled, reason })
    slotControls.value = res.data?.slotControls || []
    ElMessage.success(
      nextEnabled
        ? lt('区块已开启', '區塊已開啟', 'Placement enabled')
        : lt('区块已关闭', '區塊已關閉', 'Placement disabled')
    )
  } catch (error) {
    revert()
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('更新区块状态失败', '更新區塊狀態失敗', 'Failed to update placement status'))
    }
  } finally {
    switchingSlotCode.value = ''
  }
}

const onPublishOrderSelectionChange = (rows) => {
  selectedPublishOrderIds.value = rows.map((row) => row.id)
}

const onExperimentSelectionChange = (rows) => {
  selectedExperimentIds.value = rows.map((row) => row.id)
}

const batchCancelPublishOrders = async () => {
  try {
    const { value } = await ElMessageBox.prompt(
      lt('批量取消排期发布单需要填写原因。', '批量取消排期發佈單需要填寫原因。', 'Please provide a reason for batch cancelling scheduled release orders.'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      {
        confirmButtonText: lt('确认取消', '確認取消', 'Confirm Cancel'),
        cancelButtonText: lt('取消', '取消', 'Cancel'),
        inputPattern: /^.{4,200}$/,
        inputErrorMessage: lt('请填写 4-200 字原因', '請填寫 4-200 字原因', 'Enter a reason between 4 and 200 characters'),
        type: 'warning'
      }
    )
    await batchOperateDiscover({
      targetType: 'PUBLISH_ORDER',
      ids: selectedPublishOrderIds.value,
      status: 'CANCELLED',
      reason: value
    })
    selectedPublishOrderIds.value = []
    ElMessage.success(lt('发布单已批量取消', '發佈單已批量取消', 'Release orders cancelled'))
    await loadWorkspace()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('批量取消发布单失败', '批量取消發佈單失敗', 'Failed to batch cancel release orders'))
    }
  }
}

const batchUpdateExperimentStatus = async (status) => {
  try {
    await ElMessageBox.confirm(
      status === 'ACTIVE'
        ? lt('确认批量激活所选实验位？', '確認批量激活所選實驗位？', 'Activate the selected experiments?')
        : lt('确认批量暂停所选实验位？', '確認批量暫停所選實驗位？', 'Pause the selected experiments?'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { type: 'warning' }
    )
    await batchOperateDiscover({
      targetType: 'EXPERIMENT',
      ids: selectedExperimentIds.value,
      status,
      reason: status === 'ACTIVE' ? 'Batch activate from overview' : 'Batch pause from overview'
    })
    selectedExperimentIds.value = []
    ElMessage.success(
      status === 'ACTIVE'
        ? lt('实验位已批量激活', '實驗位已批量激活', 'Experiments activated')
        : lt('实验位已批量暂停', '實驗位已批量暫停', 'Experiments paused')
    )
    await loadWorkspace()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('批量更新实验位失败', '批量更新實驗位失敗', 'Failed to batch update experiments'))
    }
  }
}

onMounted(loadWorkspace)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.module-title { font-size: 16px; font-weight: 600; color: #1f2329; }
.module-subtitle { margin-top: 6px; color: #667085; font-size: 13px; line-height: 1.6; max-width: 780px; }
.overview-alert { margin-bottom: 16px; }
.overview-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; }
.module-card { border: 1px solid #ebeef5; border-radius: 16px; padding: 20px; background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%); }
.card-name { font-size: 15px; font-weight: 600; color: #1f2329; }
.card-count { margin-top: 12px; font-size: 32px; line-height: 1; font-weight: 700; color: #1677ff; }
.card-desc { min-height: 44px; margin: 14px 0 18px; color: #667085; font-size: 13px; line-height: 1.6; }
.ops-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; margin: 16px 0; }
.ops-summary-card { border: 1px solid #ebeef5; border-radius: 14px; padding: 16px; background: #fafcff; }
.summary-label { color: #667085; font-size: 13px; }
.summary-value { margin-top: 8px; font-size: 28px; font-weight: 700; color: #1f2329; }
.summary-meta { margin-top: 8px; color: #909399; font-size: 12px; line-height: 1.5; }
.analytics-card { margin-bottom: 16px; }
.ops-panels { margin-bottom: 18px; }
.panel-title { font-size: 14px; font-weight: 600; color: #1f2329; }
.slot-section { margin-top: 8px; }
.section-title { font-size: 15px; font-weight: 600; color: #1f2329; }
.section-subtitle { margin-top: 6px; color: #667085; font-size: 13px; line-height: 1.6; max-width: 860px; }
.actions { display: flex; gap: 8px; flex-wrap: wrap; }
</style>
