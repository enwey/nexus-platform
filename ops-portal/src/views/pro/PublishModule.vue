<template>
  <div class="pro-page">
    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ lt('发布中心', '發布中心', 'Publish Center') }}</div>
            <div class="panel-subtitle">{{ lt('集中查看待发布、已发布、暂停和封控资产，并通过发布单完成审批和上线。', '集中查看待發布、已發布、暫停與封控資產，並透過發布單完成審批與上線。', 'Review draft, published, paused, and controlled assets, then move them online through release orders.') }}</div>
          </div>
          <div class="actions">
            <el-button @click="openRollbackDialog">{{ lt('创建回滚发布单', '建立回滾發布單', 'Create Rollback Order') }}</el-button>
            <el-button :loading="executingDue" type="warning" @click="executeDueOrders">{{ lt('执行到期排期', '執行到期排期', 'Execute Due Schedule') }}</el-button>
            <el-button :loading="loading" @click="loadData">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <div class="metric-grid">
        <div class="metric-card"><div class="metric-label">{{ lt('待审核游戏', '待審核遊戲', 'Pending Games') }}</div><div class="metric-value">{{ overview.pendingGameReviews }}</div></div>
        <div class="metric-card"><div class="metric-label">{{ lt('已封控游戏', '已封控遊戲', 'Blocked Games') }}</div><div class="metric-value">{{ overview.blockedGames }}</div></div>
        <div class="metric-card"><div class="metric-label">{{ lt('已隐藏游戏', '已隱藏遊戲', 'Hidden Games') }}</div><div class="metric-value">{{ overview.hiddenGames }}</div></div>
        <div class="metric-card"><div class="metric-label">{{ lt('内容草稿', '內容草稿', 'Draft Content') }}</div><div class="metric-value">{{ overview.draftContentItems }}</div></div>
        <div class="metric-card"><div class="metric-label">{{ lt('内容已发布', '內容已發布', 'Published Content') }}</div><div class="metric-value">{{ overview.publishedContentItems }}</div></div>
        <div class="metric-card"><div class="metric-label">{{ lt('内容已暂停', '內容已暫停', 'Paused Content') }}</div><div class="metric-value">{{ overview.pausedContentItems }}</div></div>
        <div class="metric-card"><div class="metric-label">{{ lt('广告草稿', '廣告草稿', 'Draft Ads') }}</div><div class="metric-value">{{ overview.draftLaunchAds }}</div></div>
        <div class="metric-card"><div class="metric-label">{{ lt('在线启动广告', '在線啟動廣告', 'Active Launch Ads') }}</div><div class="metric-value">{{ overview.activeLaunchAds }}</div></div>
        <div class="metric-card"><div class="metric-label">{{ lt('可发布资产', '可發布資產', 'Ready Assets') }}</div><div class="metric-value">{{ overview.readyToPublishAssets }}</div></div>
        <div class="metric-card"><div class="metric-label">{{ lt('规则阻塞资产', '規則阻塞資產', 'Blocked Assets') }}</div><div class="metric-value">{{ overview.blockedByRulesAssets }}</div></div>
        <div class="metric-card"><div class="metric-label">{{ lt('排期中资产', '排期中資產', 'Scheduled Assets') }}</div><div class="metric-value">{{ overview.scheduledAssets }}</div></div>
      </div>
    </el-card>

    <el-card class="panel-card">
      <template #header>{{ lt('发布准备度队列', '發布準備度佇列', 'Release Readiness Queue') }}</template>
      <el-table :data="overview.releaseGateItems" v-loading="loading" :empty-text="lt('暂无发布准备数据', '暫無發布準備資料', 'No release readiness data')">
        <el-table-column prop="assetType" :label="lt('类型', '類型', 'Type')" width="120" />
        <el-table-column prop="assetCode" :label="lt('编码', '編碼', 'Code')" min-width="140" />
        <el-table-column prop="assetName" :label="lt('名称', '名稱', 'Name')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="currentStatus" :label="lt('当前状态', '目前狀態', 'Current Status')" min-width="150" show-overflow-tooltip />
        <el-table-column :label="lt('发布态', '發布態', 'Release State')" width="120">
          <template #default="{ row }">
            <el-tag :type="releaseStateType(row.releaseState)">{{ row.releaseState }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="recommendedAction" :label="lt('建议动作', '建議動作', 'Recommended Action')" min-width="170" />
        <el-table-column prop="blockerReason" :label="lt('阻塞原因', '阻塞原因', 'Blocker')" min-width="220" show-overflow-tooltip />
        <el-table-column prop="owner" :label="lt('归属', '歸屬', 'Owner')" min-width="120" />
        <el-table-column :label="lt('更新时间', '更新時間', 'Updated')" min-width="160">
          <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="160">
          <template #default="{ row }">
            <el-button
              v-if="row.releaseState === 'READY' && supportedAssetTypes.includes(row.assetType)"
              type="primary"
              link
              @click="openOrderDialog(row)"
            >{{ lt('创建发布单', '建立發布單', 'Create Order') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :xl="14">
        <el-card class="panel-card">
          <template #header>
            <div class="head-row">
              <span>{{ lt('发布单列表', '發布單列表', 'Release Orders') }}</span>
              <span class="light-tip">{{ lt('所有上线动作必须先入发布单，再经过审批与执行。', '所有上線動作必須先進入發布單，再經過審批與執行。', 'Every go-live action should enter the release order queue before approval and execution.') }}</span>
            </div>
          </template>
          <el-table :data="overview.publishOrders" v-loading="loading" :empty-text="lt('暂无发布单', '暫無發布單', 'No release orders')">
            <el-table-column prop="assetType" :label="lt('类型', '類型', 'Type')" width="120" />
            <el-table-column prop="assetCode" :label="lt('编码', '編碼', 'Code')" min-width="120" />
            <el-table-column prop="assetName" :label="lt('名称', '名稱', 'Name')" min-width="180" show-overflow-tooltip />
            <el-table-column prop="desiredAction" :label="lt('期望动作', '期望動作', 'Desired Action')" width="140" />
            <el-table-column :label="lt('发布方式', '發布方式', 'Rollout')" width="160">
              <template #default="{ row }">
                <span v-if="row.rolloutMode === 'GRAY'">{{ lt('灰度', '灰度', 'Gray') }} {{ row.rolloutPercent }}% / {{ row.rolloutChannel }}</span>
                <span v-else>{{ lt('全量', '全量', 'Full') }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="lt('单据状态', '單據狀態', 'Order Status')" width="130">
              <template #default="{ row }">
                <el-tag :type="orderStatusType(row.orderStatus)">{{ row.orderStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="lt('执行结果', '執行結果', 'Execution')" width="120">
              <template #default="{ row }">
                <el-tag :type="executionResultType(row.executionResult)">{{ row.executionResult || 'PENDING' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="lt('排期时间', '排期時間', 'Schedule')" min-width="160">
              <template #default="{ row }">{{ formatDate(row.scheduleAt) || '-' }}</template>
            </el-table-column>
            <el-table-column :label="lt('失败原因', '失敗原因', 'Failure Reason')" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">{{ row.failureReason || '-' }}</template>
            </el-table-column>
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="260">
              <template #default="{ row }">
                <el-button type="primary" link @click="openPreviewDialog(row)">{{ lt('预览差异', '預覽差異', 'Preview Diff') }}</el-button>
                <el-button v-if="row.orderStatus === 'SUBMITTED'" type="success" link @click="changeOrderStatus(row, row.scheduleAt ? 'SCHEDULED' : 'APPROVED')">{{ lt('审批通过', '審批通過', 'Approve') }}</el-button>
                <el-button v-if="row.orderStatus === 'SUBMITTED'" type="danger" link @click="changeOrderStatus(row, 'REJECTED')">{{ lt('驳回', '駁回', 'Reject') }}</el-button>
                <el-button v-if="row.orderStatus === 'APPROVED'" type="primary" link @click="changeOrderStatus(row, 'PUBLISHED')">{{ lt('执行上线', '執行上線', 'Publish') }}</el-button>
                <el-button v-if="row.orderStatus === 'SCHEDULED'" type="primary" link @click="changeOrderStatus(row, 'PUBLISHED')">{{ lt('立即执行', '立即執行', 'Run Now') }}</el-button>
                <el-button v-if="row.orderStatus === 'FAILED'" type="warning" link @click="changeOrderStatus(row, 'PUBLISHED')">{{ lt('失败重试', '失敗重試', 'Retry') }}</el-button>
                <el-button v-if="['SUBMITTED','APPROVED','SCHEDULED'].includes(row.orderStatus)" link @click="changeOrderStatus(row, 'CANCELLED')">{{ lt('取消', '取消', 'Cancel') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :xl="10">
        <el-card class="panel-card">
          <template #header>{{ lt('最近发布动作', '最近發布動作', 'Recent Publish Actions') }}</template>
          <el-table :data="overview.recentActions" v-loading="loading" :empty-text="lt('暂无发布动作', '暫無發布動作', 'No publish actions')">
            <el-table-column prop="action" :label="lt('动作', '動作', 'Action')" min-width="180" />
            <el-table-column prop="targetAppId" label="AppID" min-width="140" />
            <el-table-column :label="lt('结果', '結果', 'Result')" width="100">
              <template #default="{ row }">
                <el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? lt('成功', '成功', 'Success') : lt('失败', '失敗', 'Failed') }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reason" :label="lt('说明', '說明', 'Reason')" min-width="180" show-overflow-tooltip />
            <el-table-column :label="lt('时间', '時間', 'Created')" min-width="160">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="orderDialogVisible" :title="lt('创建发布单', '建立發布單', 'Create Release Order')" width="560px">
      <el-form :model="orderForm" label-position="top">
        <el-form-item :label="lt('资产类型', '資產類型', 'Asset Type')">
          <el-input :model-value="orderForm.assetType" disabled />
        </el-form-item>
        <el-form-item :label="lt('资产编码', '資產編碼', 'Asset Code')">
          <el-input :model-value="orderForm.assetCode" disabled />
        </el-form-item>
        <el-form-item :label="lt('期望动作', '期望動作', 'Desired Action')">
          <el-select v-model="orderForm.desiredAction">
            <el-option v-for="item in actionOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('发布方式', '發布方式', 'Rollout Mode')">
          <el-radio-group v-model="orderForm.rolloutMode">
            <el-radio-button label="FULL">{{ lt('全量发布', '全量發布', 'Full') }}</el-radio-button>
            <el-radio-button label="GRAY">{{ lt('灰度发布', '灰度發布', 'Gray') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <template v-if="orderForm.rolloutMode === 'GRAY'">
          <el-form-item :label="lt('灰度比例', '灰度比例', 'Rollout Percent')">
            <el-input-number v-model="orderForm.rolloutPercent" :min="1" :max="99" style="width: 100%" />
          </el-form-item>
          <el-form-item :label="lt('灰度渠道', '灰度渠道', 'Rollout Channel')">
            <el-input v-model="orderForm.rolloutChannel" :placeholder="lt('例如 ios-appstore / android-prod', '例如 ios-appstore / android-prod', 'For example ios-appstore / android-prod')" />
          </el-form-item>
        </template>
        <el-form-item :label="lt('计划执行时间', '計畫執行時間', 'Schedule At')">
          <el-date-picker
            v-model="orderForm.scheduleAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :placeholder="lt('可选，默认尽快执行', '可選，預設儘快執行', 'Optional, execute as soon as possible')"
          />
        </el-form-item>
        <el-form-item :label="lt('执行说明', '執行說明', 'Execution Note')">
          <el-input v-model="orderForm.executionNote" type="textarea" :rows="3" maxlength="256" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="orderDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="orderSaving" @click="submitOrder">{{ lt('提交发布单', '提交發布單', 'Submit Order') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rollbackDialogVisible" :title="lt('创建回滚发布单', '建立回滾發布單', 'Create Rollback Order')" width="620px">
      <el-form :model="rollbackForm" label-position="top">
        <el-form-item :label="lt('目标游戏', '目標遊戲', 'Game')">
          <el-select v-model="rollbackForm.gameId" filterable style="width: 100%" @change="handleRollbackGameChange">
            <el-option v-for="item in rollbackGames" :key="item.id" :label="`${item.name} / ${item.appId}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('回滚版本', '回滾版本', 'Rollback Version')">
          <el-select v-model="rollbackForm.versionId" filterable style="width: 100%">
            <el-option v-for="item in rollbackVersions" :key="item.id" :label="`${item.versionName} / ${item.status}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('计划执行时间', '計畫執行時間', 'Schedule At')">
          <el-date-picker v-model="rollbackForm.scheduleAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="lt('回滚说明', '回滾說明', 'Rollback Note')">
          <el-input v-model="rollbackForm.executionNote" type="textarea" :rows="3" maxlength="256" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rollbackDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="rollbackSaving" @click="submitRollbackOrder">{{ lt('提交回滚发布单', '提交回滾發布單', 'Submit Rollback Order') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewDialogVisible" :title="lt('发布差异预览', '發布差異預覽', 'Release Diff Preview')" width="860px">
      <div v-loading="previewLoading">
        <div v-if="previewData" class="preview-meta">
          <div class="metric-card">
            <div class="metric-label">{{ lt('资产', '資產', 'Asset') }}</div>
            <div class="preview-value">{{ previewData.assetName }} / {{ previewData.assetCode }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">{{ lt('动作', '動作', 'Action') }}</div>
            <div class="preview-value">{{ previewData.desiredAction }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">{{ lt('发布方式', '發布方式', 'Rollout') }}</div>
            <div class="preview-value">{{ previewData.rolloutMode === 'GRAY' ? `${lt('灰度', '灰度', 'Gray')} ${previewData.rolloutPercent}% / ${previewData.rolloutChannel}` : lt('全量发布', '全量發布', 'Full Release') }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">{{ lt('单据状态', '單據狀態', 'Order Status') }}</div>
            <div class="preview-value">{{ previewData.orderStatus }}</div>
          </div>
        </div>
        <el-alert
          v-for="(warning, index) in previewData?.riskWarnings || []"
          :key="`${index}-${warning}`"
          :title="warning"
          type="warning"
          :closable="false"
          style="margin-bottom: 10px"
        />
        <el-table :data="previewData?.fieldDiffs || []" :empty-text="lt('当前没有字段变化', '目前沒有欄位變化', 'No field changes detected')">
          <el-table-column prop="field" :label="lt('字段', '欄位', 'Field')" min-width="160" />
          <el-table-column prop="beforeValue" :label="lt('当前值', '目前值', 'Current Value')" min-width="240" show-overflow-tooltip />
          <el-table-column prop="afterValue" :label="lt('目标值', '目標值', 'Target Value')" min-width="240" show-overflow-tooltip />
        </el-table>
        <el-divider content-position="left">{{ lt('配置漂移对比', '配置漂移對比', 'Submitted vs Live Drift') }}</el-divider>
        <el-table :data="previewData?.configDriftDiffs || []" :empty-text="lt('当前线上配置与提单时一致', '目前線上配置與提單時一致', 'Live config matches the submitted snapshot')">
          <el-table-column prop="field" :label="lt('字段', '欄位', 'Field')" min-width="160" />
          <el-table-column prop="beforeValue" :label="lt('提单快照', '提單快照', 'Submitted Snapshot')" min-width="240" show-overflow-tooltip />
          <el-table-column prop="afterValue" :label="lt('当前线上', '當前線上', 'Current Live')" min-width="240" show-overflow-tooltip />
        </el-table>
        <el-row :gutter="12" style="margin-top: 14px">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>{{ lt('提单快照', '提單快照', 'Submitted Snapshot') }}</template>
              <pre class="snapshot-pre">{{ formatJson(previewData?.submittedSnapshot) }}</pre>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>{{ lt('当前线上快照', '當前線上快照', 'Current Live Snapshot') }}</template>
              <pre class="snapshot-pre">{{ formatJson(previewData?.currentSnapshot) }}</pre>
            </el-card>
          </el-col>
        </el-row>
        <el-divider content-position="left">{{ lt('执行前后快照', '執行前後快照', 'Execution Snapshots') }}</el-divider>
        <el-table :data="previewData?.executionDiffs || []" :empty-text="lt('暂无执行快照差异', '暫無執行快照差異', 'No execution snapshot diff')">
          <el-table-column prop="field" :label="lt('字段', '欄位', 'Field')" min-width="160" />
          <el-table-column prop="beforeValue" :label="lt('执行前', '執行前', 'Before Execute')" min-width="240" show-overflow-tooltip />
          <el-table-column prop="afterValue" :label="lt('执行后', '執行後', 'After Execute')" min-width="240" show-overflow-tooltip />
        </el-table>
        <el-row :gutter="12" style="margin-top: 14px">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>{{ lt('执行前快照', '執行前快照', 'Before Execution') }}</template>
              <pre class="snapshot-pre">{{ formatJson(previewData?.executionBeforeSnapshot) }}</pre>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>{{ lt('执行后快照', '執行後快照', 'After Execution') }}</template>
              <pre class="snapshot-pre">{{ formatJson(previewData?.executionAfterSnapshot) }}</pre>
            </el-card>
          </el-col>
        </el-row>
        <el-descriptions v-if="previewData?.executionReceipt || previewData?.failureReason" :column="1" border style="margin-top: 12px">
          <el-descriptions-item :label="lt('执行回执', '執行回執', 'Execution Receipt')">{{ previewData?.executionReceipt || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('失败原因', '失敗原因', 'Failure Reason')">{{ previewData?.failureReason || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="previewDialogVisible = false">{{ lt('关闭', '關閉', 'Close') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createOpsPublishOrder, executeDueOpsPublishOrders, getGameList, getGameVersions, getOpsPublishOrderPreview, getOpsPublishOverview, updateOpsPublishOrderStatus } from '../../api'
import { useI18nLite } from '../../i18n'

const { lt } = useI18nLite()
const loading = ref(false)
const executingDue = ref(false)
const orderSaving = ref(false)
const orderDialogVisible = ref(false)
const rollbackDialogVisible = ref(false)
const rollbackSaving = ref(false)
const previewDialogVisible = ref(false)
const previewLoading = ref(false)
const selectedReleaseItem = ref(null)
const previewData = ref(null)
const rollbackGames = ref([])
const rollbackVersions = ref([])
const overview = reactive({
  pendingGameReviews: 0,
  blockedGames: 0,
  hiddenGames: 0,
  draftContentItems: 0,
  publishedContentItems: 0,
  pausedContentItems: 0,
  draftLaunchAds: 0,
  activeLaunchAds: 0,
  readyToPublishAssets: 0,
  blockedByRulesAssets: 0,
  scheduledAssets: 0,
  pendingItems: [],
  releaseGateItems: [],
  publishOrders: [],
  recentActions: []
})

const orderForm = reactive({
  assetType: '',
  targetId: null,
  relatedGameId: null,
  assetCode: '',
  desiredAction: '',
  rolloutMode: 'FULL',
  rolloutPercent: 20,
  rolloutChannel: '',
  scheduleAt: '',
  executionNote: ''
})

const rollbackForm = reactive({
  gameId: null,
  versionId: null,
  scheduleAt: '',
  executionNote: ''
})

const supportedAssetTypes = ['CONTENT_ITEM', 'LAUNCH_AD', 'NOTICE']

const actionOptions = computed(() => {
  const type = orderForm.assetType
  if (type === 'CONTENT_ITEM') {
    return [
      { value: 'PUBLISH', label: lt('发布内容', '發布內容', 'Publish Content') },
      { value: 'PAUSE', label: lt('暂停内容', '暫停內容', 'Pause Content') },
      { value: 'DRAFT', label: lt('转回草稿', '轉回草稿', 'Back to Draft') }
    ]
  }
  if (type === 'LAUNCH_AD') {
    return [
      { value: 'ACTIVATE', label: lt('启用广告', '啟用廣告', 'Activate Ad') },
      { value: 'DEACTIVATE', label: lt('停用广告', '停用廣告', 'Deactivate Ad') },
      { value: 'DRAFT', label: lt('转回草稿', '轉回草稿', 'Back to Draft') }
    ]
  }
  if (type === 'NOTICE') {
    return [
      { value: 'PUBLISH', label: lt('发布通知', '發布通知', 'Publish Notice') },
      { value: 'PAUSE', label: lt('暂停通知', '暫停通知', 'Pause Notice') },
      { value: 'DRAFT', label: lt('转回草稿', '轉回草稿', 'Back to Draft') }
    ]
  }
  return []
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getOpsPublishOverview()
    Object.assign(overview, res.data || {})
  } catch (error) {
    ElMessage.error(error.message || lt('加载发布中心失败', '載入發布中心失敗', 'Failed to load publish center'))
  } finally {
    loading.value = false
  }
}

const openOrderDialog = (row) => {
  selectedReleaseItem.value = row
  orderForm.assetType = row.assetType
  orderForm.assetCode = row.assetCode
  orderForm.targetId = row.targetId
  orderForm.relatedGameId = row.gameId || null
  orderForm.desiredAction = defaultDesiredAction(row)
  orderForm.rolloutMode = 'FULL'
  orderForm.rolloutPercent = 20
  orderForm.rolloutChannel = ''
  orderForm.scheduleAt = ''
  orderForm.executionNote = ''
  orderDialogVisible.value = true
}

const defaultDesiredAction = (row) => {
  if (row.assetType === 'CONTENT_ITEM') return 'PUBLISH'
  if (row.assetType === 'NOTICE') return 'PUBLISH'
  if (row.assetType === 'LAUNCH_AD') return 'ACTIVATE'
  return ''
}

const submitOrder = async () => {
  if (orderForm.rolloutMode === 'GRAY' && !orderForm.rolloutChannel.trim()) {
    ElMessage.warning(lt('请填写灰度渠道', '請填寫灰度渠道', 'Please enter rollout channel'))
    return
  }
  try {
    orderSaving.value = true
    await createOpsPublishOrder({
      assetType: orderForm.assetType,
      targetId: orderForm.targetId,
      relatedGameId: orderForm.relatedGameId,
      desiredAction: orderForm.desiredAction,
      rolloutMode: orderForm.rolloutMode,
      rolloutPercent: orderForm.rolloutMode === 'GRAY' ? orderForm.rolloutPercent : null,
      rolloutChannel: orderForm.rolloutMode === 'GRAY' ? orderForm.rolloutChannel : null,
      scheduleAt: orderForm.scheduleAt || null,
      executionNote: orderForm.executionNote
    })
    orderDialogVisible.value = false
    ElMessage.success(lt('发布单已提交', '發布單已提交', 'Release order submitted'))
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || lt('提交发布单失败', '提交發布單失敗', 'Failed to submit release order'))
  } finally {
    orderSaving.value = false
  }
}

const openRollbackDialog = async () => {
  rollbackDialogVisible.value = true
  rollbackForm.gameId = null
  rollbackForm.versionId = null
  rollbackForm.scheduleAt = ''
  rollbackForm.executionNote = ''
  rollbackVersions.value = []
  if (rollbackGames.value.length) return
  try {
    const res = await getGameList()
    rollbackGames.value = Array.isArray(res.data) ? res.data.filter((item) => item?.status === 'APPROVED') : []
  } catch (error) {
    ElMessage.error(error.message || lt('加载可回滚游戏失败', '載入可回滾遊戲失敗', 'Failed to load rollback games'))
  }
}

const handleRollbackGameChange = async (gameId) => {
  rollbackForm.versionId = null
  rollbackVersions.value = []
  if (!gameId) return
  try {
    const res = await getGameVersions(gameId)
    rollbackVersions.value = Array.isArray(res.data) ? res.data.filter((item) => item?.status === 'APPROVED') : []
  } catch (error) {
    ElMessage.error(error.message || lt('加载可回滚版本失败', '載入可回滾版本失敗', 'Failed to load rollback versions'))
  }
}

const submitRollbackOrder = async () => {
  if (!rollbackForm.gameId || !rollbackForm.versionId) {
    ElMessage.warning(lt('请选择游戏和回滚版本', '請選擇遊戲與回滾版本', 'Please select a game and version'))
    return
  }
  try {
    rollbackSaving.value = true
    await createOpsPublishOrder({
      assetType: 'GAME_VERSION_ROLLBACK',
      targetId: rollbackForm.versionId,
      relatedGameId: rollbackForm.gameId,
      desiredAction: 'ROLLBACK_PUBLISH',
      scheduleAt: rollbackForm.scheduleAt || null,
      executionNote: rollbackForm.executionNote
    })
    rollbackDialogVisible.value = false
    ElMessage.success(lt('回滚发布单已提交', '回滾發布單已提交', 'Rollback order submitted'))
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || lt('提交回滚发布单失败', '提交回滾發布單失敗', 'Failed to submit rollback order'))
  } finally {
    rollbackSaving.value = false
  }
}

const openPreviewDialog = async (row) => {
  try {
    previewDialogVisible.value = true
    previewLoading.value = true
    const res = await getOpsPublishOrderPreview(row.id)
    previewData.value = res.data || null
  } catch (error) {
    previewDialogVisible.value = false
    ElMessage.error(error.message || lt('加载发布差异预览失败', '載入發布差異預覽失敗', 'Failed to load release diff preview'))
  } finally {
    previewLoading.value = false
  }
}

const executeDueOrders = async () => {
  try {
    await ElMessageBox.confirm(
      lt('系统会执行所有已经到点的排期发布单，请再次确认。', '系統會執行所有已到點的排期發布單，請再次確認。', 'This will execute all scheduled release orders that are due. Please confirm again.'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { type: 'warning' }
    )
    executingDue.value = true
    const res = await executeDueOpsPublishOrders()
    const count = Array.isArray(res.data) ? res.data.length : 0
    ElMessage.success(lt(`已处理 ${count} 个排期发布单`, `已處理 ${count} 個排期發布單`, `Processed ${count} scheduled orders`))
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('执行排期发布单失败', '執行排期發布單失敗', 'Failed to execute scheduled orders'))
    }
  } finally {
    executingDue.value = false
  }
}

const changeOrderStatus = async (row, orderStatus) => {
  let reason = ''
  try {
    if (orderStatus === 'REJECTED') {
      reason = await ElMessageBox.prompt(
        lt('请填写驳回原因', '請填寫駁回原因', 'Please enter the reject reason'),
        lt('驳回发布单', '駁回發布單', 'Reject Release Order'),
        { confirmButtonText: lt('确认', '確認', 'Confirm'), cancelButtonText: lt('取消', '取消', 'Cancel') }
      ).then((res) => res.value)
    } else {
      await ElMessageBox.confirm(
        lt('该操作会改变正式发布流转状态，请再次确认。', '此操作會改變正式發布流轉狀態，請再次確認。', 'This action will change the formal release workflow. Please confirm again.'),
        lt('二次确认', '二次確認', 'Secondary Confirmation'),
        { type: 'warning' }
      )
    }
    await updateOpsPublishOrderStatus(row.id, { orderStatus, reason })
    ElMessage.success(lt('发布单状态已更新', '發布單狀態已更新', 'Release order status updated'))
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('更新发布单状态失败', '更新發布單狀態失敗', 'Failed to update release order status'))
    }
  }
}

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(date)
}

function formatJson(value) {
  if (!value || !Object.keys(value).length) return '-'
  return JSON.stringify(value, null, 2)
}

function releaseStateType(state) {
  if (state === 'LIVE') return 'success'
  if (state === 'READY') return 'warning'
  if (state === 'SCHEDULED') return 'info'
  if (state === 'BLOCKED') return 'danger'
  return ''
}

function orderStatusType(status) {
  if (status === 'SUBMITTED') return 'warning'
  if (status === 'APPROVED') return 'success'
  if (status === 'SCHEDULED') return 'warning'
  if (status === 'PUBLISHED') return 'success'
  if (status === 'REJECTED') return 'danger'
  if (status === 'FAILED') return 'danger'
  if (status === 'CANCELLED') return 'info'
  return ''
}

function executionResultType(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

onMounted(loadData)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 18px; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.panel-title { font-size: 17px; font-weight: 800; color: #101828; }
.panel-subtitle, .light-tip { margin-top: 6px; color: #667085; font-size: 13px; }
.metric-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
.metric-card { border-radius: 16px; background: #f8fafc; border: 1px solid #e5e7eb; padding: 14px 16px; }
.metric-label { font-size: 13px; color: #667085; }
.metric-value { margin-top: 8px; font-size: 28px; font-weight: 800; color: #101828; }
.preview-meta { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; margin-bottom: 14px; }
.preview-value { margin-top: 8px; font-size: 15px; font-weight: 700; color: #101828; word-break: break-all; }
.snapshot-pre { margin: 0; white-space: pre-wrap; word-break: break-word; font-size: 12px; line-height: 1.5; color: #344054; }
</style>
