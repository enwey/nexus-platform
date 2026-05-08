<template>
  <div class="catalog-page">
    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ mode === 'list' ? lt('发现运营位列表', '發現營運位列表', 'Discover Slots') : detailTitle }}</div>
            <div class="panel-subtitle">{{ lt('控制前端区块总开关，并围绕已接入的发现位维护预览、发布单和实验位。', '控制前端區塊總開關，並圍繞已接入的發現位維護預覽、發佈單和實驗位。', 'Control frontend slot switches and manage preview, publish orders, and experiments for connected discover slots.') }}</div>
          </div>
          <div class="actions">
            <el-button @click="loadWorkspace">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
            <template v-if="mode === 'detail'">
              <el-button plain @click="toggleEnabled(false)">{{ lt('关闭区块', '關閉區塊', 'Disable Slot') }}</el-button>
              <el-button type="success" plain @click="toggleEnabled(true)">{{ lt('开启区块', '開啟區塊', 'Enable Slot') }}</el-button>
            </template>
          </div>
        </div>
      </template>

      <template v-if="mode === 'list'">
        <el-table :data="slotControls" row-key="slotCode" v-loading="loading" @row-click="openDetail">
          <el-table-column prop="name" :label="lt('区块', '區塊', 'Placement')" min-width="220" />
          <el-table-column prop="slotCode" :label="lt('编码', '編碼', 'Code')" min-width="180" />
          <el-table-column :label="lt('页面/位置', '頁面/位置', 'Page / Position')" min-width="180">
            <template #default="{ row }">{{ row.pageCode }} / {{ row.positionCode }}</template>
          </el-table-column>
          <el-table-column :label="lt('状态', '狀態', 'Status')" width="120">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'danger'">{{ row.enabled ? lt('开启', '開啟', 'Enabled') : lt('关闭', '關閉', 'Disabled') }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="lt('操作', '操作', 'Actions')" width="170" fixed="right">
            <template #default="{ row }">
              <el-switch :model-value="row.enabled" inline-prompt :active-text="lt('开', '開', 'On')" :inactive-text="lt('关', '關', 'Off')" @change="toggleRow(row, $event)" />
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template v-else-if="detailSlot">
        <div class="summary-grid">
          <div class="summary-card">
            <span>{{ lt('当前状态', '當前狀態', 'Current State') }}</span>
            <strong>{{ detailSlot.enabled ? lt('开启', '開啟', 'Enabled') : lt('关闭', '關閉', 'Disabled') }}</strong>
          </div>
          <div class="summary-card">
            <span>{{ lt('发布单数', '發佈單數', 'Publish Orders') }}</span>
            <strong>{{ scopedOrders.length }}</strong>
          </div>
          <div class="summary-card">
            <span>{{ lt('实验位数', '實驗位數', 'Experiments') }}</span>
            <strong>{{ scopedExperiments.length }}</strong>
          </div>
        </div>

        <el-row :gutter="16">
          <el-col :xs="24" :lg="10">
            <el-card shadow="never" class="section-card">
              <template #header>{{ lt('区块信息', '區塊資訊', 'Slot Info') }}</template>
              <el-descriptions :column="1" border>
                <el-descriptions-item :label="lt('编码', '編碼', 'Code')">{{ detailSlot.slotCode }}</el-descriptions-item>
                <el-descriptions-item :label="lt('页面', '頁面', 'Page')">{{ detailSlot.pageCode }}</el-descriptions-item>
                <el-descriptions-item :label="lt('位置', '位置', 'Position')">{{ detailSlot.positionCode }}</el-descriptions-item>
                <el-descriptions-item :label="lt('关联范围', '關聯範圍', 'Scoped Publish')">{{ scopeCode || lt('仅开关控制', '僅開關控制', 'Switch only') }}</el-descriptions-item>
              </el-descriptions>
            </el-card>
            <el-card shadow="never" class="section-card">
              <template #header>{{ lt('发布预览', '發佈預覽', 'Publish Preview') }}</template>
              <div v-if="previewLines.length" class="preview-lines">
                <div v-for="line in previewLines" :key="line[0]" class="preview-line">
                  <span>{{ line[0] }}</span>
                  <strong>{{ line[1] }}</strong>
                </div>
              </div>
              <el-empty v-else :description="lt('这个区块当前没有发布预览能力。', '這個區塊目前沒有發佈預覽能力。', 'No publish preview is available for this slot.')" />
            </el-card>
          </el-col>
          <el-col :xs="24" :lg="14">
            <el-card shadow="never" class="section-card">
              <template #header>{{ lt('创建发布单', '建立發佈單', 'Create Publish Order') }}</template>
              <el-form label-width="100px">
                <el-form-item :label="lt('生效时间', '生效時間', 'Effective At')"><el-input v-model="publishForm.effectiveAt" type="datetime-local" /></el-form-item>
                <el-form-item :label="lt('发布原因', '發佈原因', 'Reason')"><el-input v-model="publishForm.reason" type="textarea" :rows="3" /></el-form-item>
              </el-form>
              <el-button :disabled="!scopeCode" type="success" :loading="publishing" @click="submitPublish">{{ lt('创建发布单', '建立發佈單', 'Create Publish Order') }}</el-button>
            </el-card>
            <el-card shadow="never" class="section-card">
              <template #header>{{ lt('实验位管理', '實驗位管理', 'Experiment Management') }}</template>
              <el-form label-width="96px">
                <el-form-item :label="lt('实验名称', '實驗名稱', 'Experiment Name')"><el-input v-model="experimentForm.experimentName" /></el-form-item>
                <el-form-item :label="lt('流量占比', '流量佔比', 'Traffic %')"><el-input-number v-model="experimentForm.trafficPercent" :min="1" :max="100" style="width: 100%" /></el-form-item>
                <el-form-item :label="lt('开始时间', '開始時間', 'Start At')"><el-input v-model="experimentForm.startAt" type="datetime-local" /></el-form-item>
                <el-form-item :label="lt('结束时间', '結束時間', 'End At')"><el-input v-model="experimentForm.endAt" type="datetime-local" /></el-form-item>
                <el-form-item :label="lt('备注', '備註', 'Note')"><el-input v-model="experimentForm.note" type="textarea" :rows="3" /></el-form-item>
              </el-form>
              <el-button :disabled="!scopeCode" type="primary" :loading="experimentSaving" @click="saveExperiment">{{ lt('创建实验位', '建立實驗位', 'Create Experiment') }}</el-button>
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="never" class="section-card">
          <template #header>{{ lt('发布单列表', '發佈單列表', 'Publish Orders') }}</template>
          <el-table :data="scopedOrders" :empty-text="lt('暂无发布单', '暫無發佈單', 'No publish orders')">
            <el-table-column prop="orderNo" :label="lt('发布单号', '發佈單號', 'Order No.')" min-width="160" />
            <el-table-column prop="status" :label="lt('状态', '狀態', 'Status')" width="120" />
            <el-table-column prop="effectiveAt" :label="lt('生效时间', '生效時間', 'Effective At')" min-width="180" />
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="danger" :disabled="row.status !== 'SCHEDULED'" @click="cancelOrder(row)">{{ lt('取消', '取消', 'Cancel') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never" class="section-card">
          <template #header>{{ lt('实验位列表', '實驗位列表', 'Experiments') }}</template>
          <el-table :data="scopedExperiments" :empty-text="lt('暂无实验位', '暫無實驗位', 'No experiments')">
            <el-table-column prop="experimentName" :label="lt('实验名称', '實驗名稱', 'Experiment')" min-width="180" />
            <el-table-column prop="trafficPercent" :label="lt('流量', '流量', 'Traffic')" width="100">
              <template #default="{ row }">{{ row.trafficPercent }}%</template>
            </el-table-column>
            <el-table-column prop="status" :label="lt('状态', '狀態', 'Status')" width="120" />
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="160" fixed="right">
              <template #default="{ row }">
                <el-button link type="success" :disabled="row.status === 'ACTIVE'" @click="changeExperimentStatus(row, 'ACTIVE')">{{ lt('激活', '激活', 'Activate') }}</el-button>
                <el-button link :disabled="row.status === 'PAUSED'" @click="changeExperimentStatus(row, 'PAUSED')">{{ lt('暂停', '暫停', 'Pause') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18nLite } from '../../../i18n'
import { fetchDiscoverPublishPreview, fetchDiscoverWorkspace, createDiscoverPublishOrder, saveDiscoverExperiment } from '../../../utils/discoverOps'
import { batchOperateDiscover, updateDiscoverSlotControl } from '../../../api'
import { buildPreviewLines, getScopeCodeFromSlotCode } from './catalogCustomShared'

const props = defineProps({
  mode: { type: String, default: 'list' },
  detailId: { type: [String, Number], default: null }
})

const router = useRouter()
const { lt } = useI18nLite()
const loading = ref(false)
const publishing = ref(false)
const experimentSaving = ref(false)
const workspace = ref(null)
const previewData = ref(null)
const publishForm = reactive({ effectiveAt: '', reason: '' })
const experimentForm = reactive({ experimentName: '', trafficPercent: 10, startAt: '', endAt: '', note: '' })

const slotControls = computed(() => workspace.value?.slotControls || [])
const detailSlot = computed(() => slotControls.value.find((item) => String(item.slotCode) === String(props.detailId)) || null)
const detailTitle = computed(() => detailSlot.value?.name || '-')
const scopeCode = computed(() => getScopeCodeFromSlotCode(detailSlot.value?.slotCode))
const scopedOrders = computed(() => (workspace.value?.publishOrders || []).filter((item) => item.scopeCode === scopeCode.value))
const scopedExperiments = computed(() => (workspace.value?.experiments || []).filter((item) => item.scopeCode === scopeCode.value))
const previewLines = computed(() => buildPreviewLines(previewData.value))

const loadWorkspace = async () => {
  loading.value = true
  try {
    workspace.value = await fetchDiscoverWorkspace()
    if (scopeCode.value) {
      previewData.value = await fetchDiscoverPublishPreview(scopeCode.value)
    } else {
      previewData.value = null
    }
  } catch (error) {
    ElMessage.error(error.message || lt('加载发现运营位失败', '載入發現營運位失敗', 'Failed to load discover slots'))
  } finally {
    loading.value = false
  }
}

const openDetail = (row) => router.push({ name: 'OpsDiscoverSlotDetail', params: { slotId: row.slotCode } })

const toggleRow = async (row, nextEnabled) => {
  try {
    await updateDiscoverSlotControl(row.slotCode, { enabled: nextEnabled, reason: nextEnabled ? 'Enable from list' : 'Disable from list' })
    await loadWorkspace()
    ElMessage.success(nextEnabled ? lt('区块已开启', '區塊已開啟', 'Slot enabled') : lt('区块已关闭', '區塊已關閉', 'Slot disabled'))
  } catch (error) {
    ElMessage.error(error.message || lt('更新区块状态失败', '更新區塊狀態失敗', 'Failed to update slot status'))
  }
}

const toggleEnabled = async (nextEnabled) => {
  if (!detailSlot.value) return
  try {
    let reason = ''
    if (!nextEnabled) {
      const result = await ElMessageBox.prompt(
        lt('关闭区块需要填写原因。', '關閉區塊需要填寫原因。', 'Disabling a slot requires a reason.'),
        lt('二次确认', '二次確認', 'Secondary Confirmation'),
        { inputPattern: /^.{4,200}$/, inputErrorMessage: lt('请填写 4-200 字原因', '請填寫 4-200 字原因', 'Enter a reason between 4 and 200 characters'), type: 'warning' }
      )
      reason = result.value
    }
    await updateDiscoverSlotControl(detailSlot.value.slotCode, { enabled: nextEnabled, reason })
    await loadWorkspace()
    ElMessage.success(nextEnabled ? lt('区块已开启', '區塊已開啟', 'Slot enabled') : lt('区块已关闭', '區塊已關閉', 'Slot disabled'))
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('更新区块状态失败', '更新區塊狀態失敗', 'Failed to update slot status'))
    }
  }
}

const submitPublish = async () => {
  if (!scopeCode.value) return
  publishing.value = true
  try {
    await createDiscoverPublishOrder({
      scopeCode: scopeCode.value,
      effectiveAt: publishForm.effectiveAt ? `${publishForm.effectiveAt}:00` : null,
      reason: publishForm.reason || 'Publish from slot detail'
    })
    await loadWorkspace()
    ElMessage.success(lt('发布单已创建', '發佈單已建立', 'Publish order created'))
  } catch (error) {
    ElMessage.error(error.message || lt('创建发布单失败', '建立發佈單失敗', 'Failed to create publish order'))
  } finally {
    publishing.value = false
  }
}

const saveExperiment = async () => {
  if (!scopeCode.value) return
  experimentSaving.value = true
  try {
    await saveDiscoverExperiment({
      scopeCode: scopeCode.value,
      experimentName: experimentForm.experimentName,
      trafficPercent: experimentForm.trafficPercent,
      startAt: experimentForm.startAt ? `${experimentForm.startAt}:00` : null,
      endAt: experimentForm.endAt ? `${experimentForm.endAt}:00` : null,
      note: experimentForm.note,
      status: 'ACTIVE'
    })
    experimentForm.experimentName = ''
    experimentForm.trafficPercent = 10
    experimentForm.startAt = ''
    experimentForm.endAt = ''
    experimentForm.note = ''
    await loadWorkspace()
    ElMessage.success(lt('实验位已创建', '實驗位已建立', 'Experiment created'))
  } catch (error) {
    ElMessage.error(error.message || lt('创建实验位失败', '建立實驗位失敗', 'Failed to create experiment'))
  } finally {
    experimentSaving.value = false
  }
}

const cancelOrder = async (row) => {
  try {
    const result = await ElMessageBox.prompt(
      lt('取消排期发布单需要填写原因。', '取消排期發佈單需要填寫原因。', 'Please provide a reason for cancelling this publish order.'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { inputPattern: /^.{4,200}$/, inputErrorMessage: lt('请填写 4-200 字原因', '請填寫 4-200 字原因', 'Enter a reason between 4 and 200 characters'), type: 'warning' }
    )
    await batchOperateDiscover({ targetType: 'PUBLISH_ORDER', ids: [row.id], status: 'CANCELLED', reason: result.value })
    await loadWorkspace()
    ElMessage.success(lt('发布单已取消', '發佈單已取消', 'Publish order cancelled'))
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || lt('取消发布单失败', '取消發佈單失敗', 'Failed to cancel publish order'))
  }
}

const changeExperimentStatus = async (row, status) => {
  try {
    await batchOperateDiscover({ targetType: 'EXPERIMENT', ids: [row.id], status, reason: `Update experiment to ${status}` })
    await loadWorkspace()
    ElMessage.success(status === 'ACTIVE' ? lt('实验位已激活', '實驗位已激活', 'Experiment activated') : lt('实验位已暂停', '實驗位已暫停', 'Experiment paused'))
  } catch (error) {
    ElMessage.error(error.message || lt('更新实验位失败', '更新實驗位失敗', 'Failed to update experiment'))
  }
}

watch(() => props.detailId, loadWorkspace)
onMounted(loadWorkspace)
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
.preview-lines { display: flex; flex-direction: column; gap: 10px; }
.preview-line { display: flex; justify-content: space-between; gap: 16px; font-size: 13px; color: #4b5563; }
</style>
