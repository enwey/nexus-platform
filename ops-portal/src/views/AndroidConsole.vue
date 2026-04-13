<template>
  <div class="page-shell android-console">
    <header class="page-header">
      <div>
        <h1>{{ lt('Android 宿主管理台', 'Android 宿主管理台', 'Android Runtime Console') }}</h1>
        <p>{{ lt('围绕 Android 宿主运行时、Bridge 能力与游戏资源就绪状态进行统一管理。', '圍繞 Android 宿主運行時、Bridge 能力與遊戲資源就緒狀態進行統一管理。', 'Manage Android host runtime, bridge capabilities, and game asset readiness in one place.') }}</p>
      </div>
      <div class="actions">
        <el-button @click="$router.push('/discover-ops')">{{ lt('推荐数据配置', '推薦數據配置', 'Recommendation Data') }}</el-button>
        <el-button @click="$router.push('/audit/logs')">{{ lt('审计日志', '審計日誌', 'Audit Logs') }}</el-button>
        <el-button @click="$router.push('/verification-codes')">{{ lt('验证码记录', '驗證碼記錄', 'Verification Codes') }}</el-button>
        <el-button @click="$router.push('/audit')">{{ lt('返回审核台', '返回審核台', 'Back To Audit') }}</el-button>
        <el-button type="primary" :loading="loading" @click="loadConsole">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
      </div>
    </header>

    <section class="metrics">
      <el-card class="metric-card accent-indigo">
        <div class="metric-value">{{ overview.totalGames }}</div>
        <div class="metric-label">{{ lt('接入游戏总数', '接入遊戲總數', 'Total Games') }}</div>
      </el-card>
      <el-card class="metric-card accent-green">
        <div class="metric-value">{{ overview.runtimeReadyGames }}</div>
        <div class="metric-label">{{ lt('运行时就绪', '運行時就緒', 'Runtime Ready') }}</div>
      </el-card>
      <el-card class="metric-card accent-gold">
        <div class="metric-value">{{ overview.bridgeImplementedCount }}</div>
        <div class="metric-label">{{ lt('Bridge 已实装', 'Bridge 已實裝', 'Bridge Implemented') }}</div>
      </el-card>
      <el-card class="metric-card accent-rose">
        <div class="metric-value">{{ overview.bridgePartialCount }}</div>
        <div class="metric-label">{{ lt('需补强/模拟', '需補強/模擬', 'Needs Hardening') }}</div>
      </el-card>
    </section>

    <section class="summary-grid">
      <el-card class="summary-card">
        <template #header>
          <div class="card-title">{{ lt('运行时摘要', '運行時摘要', 'Runtime Summary') }}</div>
        </template>
        <div class="summary-list">
          <div class="summary-item">
            <span class="summary-label">API Base URL</span>
            <strong>{{ configForm.apiBaseUrl || '-' }}</strong>
          </div>
          <div class="summary-item">
            <span class="summary-label">{{ lt('资源域名', '資源域名', 'Asset Host') }}</span>
            <strong>{{ configForm.assetHost || '-' }}</strong>
          </div>
          <div class="summary-item">
            <span class="summary-label">{{ lt('默认语言', '預設語言', 'Default Language') }}</span>
            <strong>{{ configForm.defaultLanguage || '-' }}</strong>
          </div>
          <div class="summary-item">
            <span class="summary-label">{{ lt('同步桥接', '同步橋接', 'Sync Bridge') }}</span>
            <el-tag :type="configForm.enableSyncBridge ? 'success' : 'info'">{{ configForm.enableSyncBridge ? lt('开启', '開啟', 'Enabled') : lt('关闭', '關閉', 'Disabled') }}</el-tag>
          </div>
          <div class="summary-item">
            <span class="summary-label">{{ lt('明文流量', '明文流量', 'Cleartext') }}</span>
            <el-tag :type="configForm.allowCleartextTraffic ? 'danger' : 'success'">{{ configForm.allowCleartextTraffic ? lt('允许', '允許', 'Allowed') : lt('禁止', '禁止', 'Blocked') }}</el-tag>
          </div>
          <div class="summary-item">
            <span class="summary-label">{{ lt('最大 ZIP', '最大 ZIP', 'Max ZIP') }}</span>
            <strong>{{ configForm.maxZipSizeMb }} MB</strong>
          </div>
          <div class="summary-item">
            <span class="summary-label">{{ lt('最后更新', '最後更新', 'Last Updated') }}</span>
            <strong>{{ configMeta.updatedAt || '-' }}</strong>
          </div>
          <div class="summary-item">
            <span class="summary-label">{{ lt('更新人', '更新人', 'Updated By') }}</span>
            <strong>{{ configMeta.updatedBy || '-' }}</strong>
          </div>
        </div>
      </el-card>

      <el-card class="summary-card">
        <template #header>
          <div class="card-title">{{ lt('Bridge 覆盖面', 'Bridge 覆蓋面', 'Bridge Coverage') }}</div>
        </template>
        <div class="coverage-panel">
          <div class="coverage-row">
            <span>{{ lt('总接口数', '總介面數', 'Total APIs') }}</span>
            <strong>{{ bridgeApis.length }}</strong>
          </div>
          <div class="coverage-row">
            <span>{{ lt('真实可用', '真實可用', 'Implemented') }}</span>
            <strong>{{ bridgeCounts.implemented }}</strong>
          </div>
          <div class="coverage-row">
            <span>{{ lt('模拟返回', '模擬返回', 'Mock / Placeholder') }}</span>
            <strong>{{ bridgeCounts.mock }}</strong>
          </div>
          <div class="coverage-row">
            <span>{{ lt('支持同步', '支援同步', 'Sync Ready') }}</span>
            <strong>{{ bridgeCounts.sync }}</strong>
          </div>
          <el-progress :percentage="bridgeCoveragePercent" :stroke-width="10" :show-text="false" color="#2563eb" />
          <p class="coverage-hint">{{ lt('这组数据直接映射 Android 宿主里的 Bridge 实现情况，可用于快速识别仍是 mock 的能力。', '這組資料直接映射 Android 宿主裡的 Bridge 實作情況，可用於快速識別仍是 mock 的能力。', 'These metrics map directly to Android host bridge implementations so the team can quickly spot mock-only capabilities.') }}</p>
        </div>
      </el-card>
    </section>

    <el-card class="mt-16">
      <template #header>
        <div class="section-head">
          <div>
            <div class="card-title">{{ lt('宿主能力地图', '宿主能力地圖', 'Host Capability Map') }}</div>
            <p>{{ lt('把 Android 宿主里的关键功能按安全、交付、Bridge 和设备能力进行梳理。', '把 Android 宿主裡的關鍵功能按安全、交付、Bridge 和裝置能力進行梳理。', 'An inventory of Android host capabilities grouped by security, delivery, bridge, and device concerns.') }}</p>
          </div>
        </div>
      </template>
      <div class="capability-grid">
        <article v-for="item in hostCapabilities" :key="item.capabilityKey" class="capability-card">
          <div class="capability-head">
            <el-tag size="small" effect="dark" :type="capabilityStatusType(item.status)">{{ item.status }}</el-tag>
            <span class="capability-category">{{ item.category }}</span>
          </div>
          <h3>{{ item.displayName }}</h3>
          <p>{{ item.summary }}</p>
          <div class="capability-source">{{ lt('来源模块', '來源模組', 'Source') }}: {{ item.sourceModule }}</div>
        </article>
      </div>
    </el-card>

    <el-card class="mt-16">
      <template #header>
        <div class="section-head">
          <div>
            <div class="card-title">{{ lt('运行时配置', '運行時配置', 'Runtime Config') }}</div>
            <p>{{ lt('这里对应 Android 宿主环境配置，适合做联调、灰度和不同服务器切换。', '這裡對應 Android 宿主環境配置，適合做聯調、灰度和不同伺服器切換。', 'These settings drive the Android host runtime and are suitable for debugging, rollout, and environment switching.') }}</p>
          </div>
        </div>
      </template>
      <el-form :model="configForm" label-width="190px" class="config-form">
        <el-form-item label="API Base URL"><el-input v-model="configForm.apiBaseUrl" /></el-form-item>
        <el-form-item :label="lt('资源域名', '資源域名', 'Asset Host')"><el-input v-model="configForm.assetHost" /></el-form-item>
        <el-form-item :label="lt('默认语言', '預設語言', 'Default Language')">
          <el-select v-model="configForm.defaultLanguage">
            <el-option label="zh-TW" value="zh-TW" />
            <el-option label="zh-CN" value="zh-CN" />
            <el-option label="en" value="en" />
          </el-select>
        </el-form-item>
        <el-form-item label="WebView Mixed Content">
          <el-select v-model="configForm.webViewMixedContentMode">
            <el-option label="MIXED_CONTENT_NEVER_ALLOW" value="MIXED_CONTENT_NEVER_ALLOW" />
            <el-option label="MIXED_CONTENT_COMPATIBILITY_MODE" value="MIXED_CONTENT_COMPATIBILITY_MODE" />
            <el-option label="MIXED_CONTENT_ALWAYS_ALLOW" value="MIXED_CONTENT_ALWAYS_ALLOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="WebView Cache Mode">
          <el-select v-model="configForm.webViewCacheMode">
            <el-option label="LOAD_DEFAULT" value="LOAD_DEFAULT" />
            <el-option label="LOAD_NO_CACHE" value="LOAD_NO_CACHE" />
            <el-option label="LOAD_CACHE_ELSE_NETWORK" value="LOAD_CACHE_ELSE_NETWORK" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('启动路由策略', '啟動路由策略', 'Startup Route Policy')"><el-input v-model="configForm.startupRoutePolicy" /></el-form-item>
        <el-form-item :label="lt('ZIP 最大大小(MB)', 'ZIP 最大大小(MB)', 'Max Zip Size (MB)')"><el-input-number v-model="configForm.maxZipSizeMb" :min="20" :max="1024" /></el-form-item>
        <el-form-item :label="lt('功能开关', '功能開關', 'Feature Toggles')">
          <el-checkbox v-model="configForm.debugUseMockData">{{ lt('调试模拟数据', '偵錯模擬資料', 'Debug Mock Data') }}</el-checkbox>
          <el-checkbox v-model="configForm.enableSyncBridge">{{ lt('启用同步桥接', '啟用同步橋接', 'Enable Sync Bridge') }}</el-checkbox>
          <el-checkbox v-model="configForm.allowCleartextTraffic">{{ lt('允许明文流量', '允許明文流量', 'Allow Cleartext Traffic') }}</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingConfig" @click="handleSaveConfig">{{ lt('保存配置', '儲存配置', 'Save Config') }}</el-button>
          <span class="hint" v-if="configMeta.updatedAt">{{ lt('最近更新', '最近更新', 'Updated') }}: {{ configMeta.updatedBy || '-' }} / {{ configMeta.updatedAt }}</span>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="mt-16">
      <template #header>
        <div class="section-head">
          <div>
            <div class="card-title">{{ lt('Bridge 能力矩阵', 'Bridge 能力矩陣', 'Bridge Capability Matrix') }}</div>
            <p>{{ lt('用于识别哪些接口是真实可用，哪些还只是 mock 或占位实现。', '用於識別哪些介面是真實可用，哪些還只是 mock 或佔位實作。', 'Use this table to distinguish fully implemented bridge APIs from mock or placeholder ones.') }}</p>
          </div>
          <div class="toolbar">
            <el-input v-model="bridgeFilters.keyword" clearable style="width: 240px" :placeholder="lt('搜索 API / 模块', '搜尋 API / 模組', 'Search API / Module')" />
            <el-select v-model="bridgeFilters.status" clearable style="width: 160px" :placeholder="lt('状态筛选', '狀態篩選', 'Status')">
              <el-option value="implemented" :label="lt('已实装', '已實裝', 'Implemented')" />
              <el-option value="mock" :label="lt('模拟/占位', '模擬/佔位', 'Mock')" />
            </el-select>
            <el-select v-model="bridgeFilters.module" clearable style="width: 180px" :placeholder="lt('模块筛选', '模組篩選', 'Module')">
              <el-option v-for="module in bridgeModules" :key="module" :label="module" :value="module" />
            </el-select>
            <el-checkbox v-model="bridgeFilters.syncOnly">{{ lt('只看同步', '只看同步', 'Sync Only') }}</el-checkbox>
          </div>
        </div>
      </template>
      <el-table :data="filteredBridgeApis" v-loading="loading" :empty-text="lt('暂无 Bridge 数据', '暫無 Bridge 資料', 'No bridge capabilities')">
        <el-table-column prop="apiName" label="API" min-width="220" />
        <el-table-column prop="module" :label="lt('模块', '模組', 'Module')" min-width="160" />
        <el-table-column :label="lt('状态', '狀態', 'Status')" width="140">
          <template #default="{ row }">
            <el-tag :type="bridgeStatusType(row.supportStatus)">{{ row.supportStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('同步', '同步', 'Sync')" width="100">
          <template #default="{ row }">{{ row.syncSupported ? lt('是', '是', 'Yes') : lt('否', '否', 'No') }}</template>
        </el-table-column>
        <el-table-column prop="notes" :label="lt('说明', '說明', 'Notes')" min-width="260" show-overflow-tooltip />
      </el-table>
    </el-card>

    <el-card class="mt-16">
      <template #header>
        <div class="section-head">
          <div>
            <div class="card-title">{{ lt('游戏资源就绪状态', '遊戲資源就緒狀態', 'Game Delivery Readiness') }}</div>
            <p>{{ lt('从 Android 运行时角度判断哪些游戏已经具备下载、校验与启动条件。', '從 Android 運行時角度判斷哪些遊戲已具備下載、校驗與啟動條件。', 'Judge which games are actually ready for download, integrity checks, and runtime launch from the Android host perspective.') }}</p>
          </div>
          <div class="toolbar">
            <el-input v-model="gameFilters.keyword" clearable style="width: 240px" :placeholder="lt('搜索游戏名 / AppId', '搜尋遊戲名 / AppId', 'Search game / AppId')" />
            <el-select v-model="gameFilters.status" clearable style="width: 150px" :placeholder="lt('状态', '狀態', 'Status')">
              <el-option value="APPROVED" :label="lt('已通过', '已通過', 'Approved')" />
              <el-option value="PENDING" :label="lt('待审核', '待審核', 'Pending')" />
              <el-option value="PROCESSING" :label="lt('处理中', '處理中', 'Processing')" />
              <el-option value="REJECTED" :label="lt('已拒绝', '已拒絕', 'Rejected')" />
            </el-select>
            <el-checkbox v-model="gameFilters.problemOnly">{{ lt('只看问题项', '只看問題項', 'Problems Only') }}</el-checkbox>
          </div>
        </div>
      </template>
      <el-table :data="filteredGameAssets" v-loading="loading" :empty-text="lt('暂无游戏资源数据', '暫無遊戲資源資料', 'No game assets')">
        <el-table-column prop="appId" label="AppId" min-width="180" />
        <el-table-column prop="gameName" :label="lt('游戏名称', '遊戲名稱', 'Game Name')" min-width="180" />
        <el-table-column prop="version" :label="lt('版本', '版本', 'Version')" width="110" />
        <el-table-column :label="lt('审核状态', '審核狀態', 'Review Status')" width="130">
          <template #default="{ row }">
            <el-tag :type="gameStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('运行时就绪', '運行時就緒', 'Runtime Ready')" width="130">
          <template #default="{ row }">
            <el-tag :type="row.runtimeReady ? 'success' : 'danger'">{{ row.runtimeReady ? lt('就绪', '就緒', 'Ready') : lt('阻塞', '阻塞', 'Blocked') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('问题摘要', '問題摘要', 'Issues')" min-width="260">
          <template #default="{ row }">
            <span>{{ row.issueSummary }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" :label="lt('更新时间', '更新時間', 'Updated At')" min-width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAndroidConsole, updateAndroidConfig } from '../api'
import { useI18nLite } from '../i18n'

const { lt } = useI18nLite()

const loading = ref(false)
const savingConfig = ref(false)

const overview = reactive({
  totalGames: 0,
  approvedGames: 0,
  pendingGames: 0,
  processingGames: 0,
  rejectedGames: 0,
  runtimeReadyGames: 0,
  bridgeImplementedCount: 0,
  bridgePartialCount: 0
})

const configForm = reactive({
  apiBaseUrl: '',
  assetHost: '',
  defaultLanguage: 'zh-TW',
  debugUseMockData: true,
  enableSyncBridge: true,
  allowCleartextTraffic: false,
  webViewMixedContentMode: 'MIXED_CONTENT_NEVER_ALLOW',
  webViewCacheMode: 'LOAD_DEFAULT',
  maxZipSizeMb: 200,
  startupRoutePolicy: 'SPLASH_DIRECT_TO_MAIN'
})

const configMeta = reactive({
  updatedBy: '',
  updatedAt: ''
})

const hostCapabilities = ref([])
const bridgeApis = ref([])
const gameAssets = ref([])

const bridgeFilters = reactive({
  keyword: '',
  status: '',
  module: '',
  syncOnly: false
})

const gameFilters = reactive({
  keyword: '',
  status: '',
  problemOnly: false
})

const assignConfig = (cfg = {}) => {
  configForm.apiBaseUrl = cfg.apiBaseUrl || ''
  configForm.assetHost = cfg.assetHost || ''
  configForm.defaultLanguage = cfg.defaultLanguage || 'zh-TW'
  configForm.debugUseMockData = Boolean(cfg.debugUseMockData)
  configForm.enableSyncBridge = Boolean(cfg.enableSyncBridge)
  configForm.allowCleartextTraffic = Boolean(cfg.allowCleartextTraffic)
  configForm.webViewMixedContentMode = cfg.webViewMixedContentMode || 'MIXED_CONTENT_NEVER_ALLOW'
  configForm.webViewCacheMode = cfg.webViewCacheMode || 'LOAD_DEFAULT'
  configForm.maxZipSizeMb = Number(cfg.maxZipSizeMb || 200)
  configForm.startupRoutePolicy = cfg.startupRoutePolicy || 'SPLASH_DIRECT_TO_MAIN'
  configMeta.updatedBy = cfg.updatedBy || ''
  configMeta.updatedAt = cfg.updatedAt || ''
}

const loadConsole = async () => {
  loading.value = true
  try {
    const res = await getAndroidConsole()
    Object.assign(overview, {
      totalGames: 0,
      approvedGames: 0,
      pendingGames: 0,
      processingGames: 0,
      rejectedGames: 0,
      runtimeReadyGames: 0,
      bridgeImplementedCount: 0,
      bridgePartialCount: 0
    }, res.data?.overview || {})
    assignConfig(res.data?.config || {})
    hostCapabilities.value = res.data?.hostCapabilities || []
    bridgeApis.value = res.data?.bridgeApis || []
    gameAssets.value = (res.data?.gameAssets || []).map((item) => ({
      ...item,
      issueSummary: buildGameIssueSummary(item)
    }))
  } catch (error) {
    ElMessage.error(error.message || lt('加载 Android 管理台失败', '載入 Android 管理台失敗', 'Failed to load Android console'))
  } finally {
    loading.value = false
  }
}

const handleSaveConfig = async () => {
  savingConfig.value = true
  try {
    const res = await updateAndroidConfig({ ...configForm })
    assignConfig(res.data || {})
    ElMessage.success(lt('Android 运行时配置已保存', 'Android 運行時配置已儲存', 'Android runtime config saved'))
  } catch (error) {
    ElMessage.error(error.message || lt('保存 Android 运行时配置失败', '儲存 Android 運行時配置失敗', 'Failed to save Android runtime config'))
  } finally {
    savingConfig.value = false
  }
}

const buildGameIssueSummary = (row) => {
  const issues = []
  if (row.status !== 'APPROVED') issues.push(lt('未通过审核', '未通過審核', 'Not approved'))
  if (!row.downloadUrl) issues.push(lt('缺少下载地址', '缺少下載地址', 'Missing download URL'))
  if (!row.md5) issues.push(lt('缺少 MD5', '缺少 MD5', 'Missing MD5'))
  if (!row.runtimeReady) issues.push(lt('运行时不可启动', '運行時不可啟動', 'Runtime blocked'))
  return issues.length ? issues.join(' / ') : lt('资源完整，可进入 Android 运行时', '資源完整，可進入 Android 運行時', 'Ready for Android runtime')
}

const bridgeCounts = computed(() => ({
  implemented: bridgeApis.value.filter((item) => item.supportStatus === 'implemented').length,
  mock: bridgeApis.value.filter((item) => item.supportStatus !== 'implemented').length,
  sync: bridgeApis.value.filter((item) => item.syncSupported).length
}))

const bridgeCoveragePercent = computed(() => {
  if (!bridgeApis.value.length) return 0
  return Math.round((bridgeCounts.value.implemented / bridgeApis.value.length) * 100)
})

const bridgeModules = computed(() => [...new Set(bridgeApis.value.map((item) => item.module).filter(Boolean))])

const filteredBridgeApis = computed(() => {
  const keyword = bridgeFilters.keyword.trim().toLowerCase()
  return bridgeApis.value.filter((item) => {
    if (bridgeFilters.status && item.supportStatus !== bridgeFilters.status) return false
    if (bridgeFilters.module && item.module !== bridgeFilters.module) return false
    if (bridgeFilters.syncOnly && !item.syncSupported) return false
    if (keyword) {
      const haystack = `${item.apiName} ${item.module} ${item.notes}`.toLowerCase()
      if (!haystack.includes(keyword)) return false
    }
    return true
  })
})

const filteredGameAssets = computed(() => {
  const keyword = gameFilters.keyword.trim().toLowerCase()
  return gameAssets.value.filter((item) => {
    if (gameFilters.status && item.status !== gameFilters.status) return false
    if (gameFilters.problemOnly && item.runtimeReady) return false
    if (keyword) {
      const haystack = `${item.appId} ${item.gameName}`.toLowerCase()
      if (!haystack.includes(keyword)) return false
    }
    return true
  })
})

const capabilityStatusType = (status) => {
  if (status === 'implemented') return 'success'
  if (status === 'partial') return 'warning'
  return 'info'
}

const bridgeStatusType = (status) => {
  if (status === 'implemented') return 'success'
  if (status === 'mock') return 'warning'
  return 'info'
}

const gameStatusType = (status) => ({
  APPROVED: 'success',
  PENDING: 'warning',
  PROCESSING: 'info',
  REJECTED: 'danger'
}[status] || 'info')

onMounted(loadConsole)
</script>

<style scoped>
.page-shell {
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.12), transparent 28%),
    radial-gradient(circle at top right, rgba(245, 158, 11, 0.10), transparent 24%),
    linear-gradient(180deg, #f8fbff 0%, #f4f6fb 100%);
}

.page-header {
  margin-bottom: 20px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-header h1 {
  margin: 0 0 8px;
}

.page-header p {
  margin: 0;
  color: #5b6475;
  max-width: 860px;
}

.actions,
.toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  border: none;
  position: relative;
  overflow: hidden;
}

.metric-card::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 5px;
  opacity: 0.9;
}

.accent-indigo::before { background: linear-gradient(180deg, #2563eb, #1d4ed8); }
.accent-green::before { background: linear-gradient(180deg, #059669, #047857); }
.accent-gold::before { background: linear-gradient(180deg, #d97706, #b45309); }
.accent-rose::before { background: linear-gradient(180deg, #e11d48, #be123c); }

.metric-value {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
}

.metric-label {
  margin-top: 6px;
  color: #6b7280;
}

.summary-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: 1.25fr 0.95fr;
  gap: 16px;
}

.summary-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  padding: 14px 16px;
  border-radius: 14px;
  background: linear-gradient(180deg, #f8fafc 0%, #eef2ff 100%);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.summary-label {
  color: #6b7280;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.coverage-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.coverage-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #e5e7eb;
}

.coverage-hint {
  margin: 0;
  color: #6b7280;
  line-height: 1.6;
}

.mt-16 {
  margin-top: 16px;
}

.card-title {
  font-weight: 700;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.section-head p {
  margin: 6px 0 0;
  color: #6b7280;
}

.capability-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.capability-card {
  padding: 16px;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e5e7eb;
  min-height: 170px;
}

.capability-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.capability-card h3 {
  margin: 14px 0 10px;
  font-size: 18px;
}

.capability-card p {
  margin: 0;
  color: #5b6475;
  line-height: 1.6;
}

.capability-category,
.capability-source,
.hint {
  color: #6b7280;
  font-size: 12px;
}

.capability-source {
  margin-top: 12px;
}

.config-form :deep(.el-checkbox) {
  margin-right: 16px;
}

@media (max-width: 1200px) {
  .metrics,
  .capability-grid,
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .page-header,
  .section-head {
    flex-direction: column;
  }

  .metrics,
  .capability-grid,
  .summary-grid,
  .summary-list {
    grid-template-columns: 1fr;
  }
}
</style>
