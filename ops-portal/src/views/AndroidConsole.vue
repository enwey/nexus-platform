<template>
  <div class="page-shell">
    <header class="page-header">
      <div>
        <h1>Android Console</h1>
        <p>Manage Android runtime config, bridge capability matrix, and game asset readiness.</p>
      </div>
      <div class="actions">
        <el-button @click="$router.push('/audit')">Back To Audit</el-button>
        <el-button type="primary" @click="loadAll">Refresh</el-button>
      </div>
    </header>

    <section class="metrics">
      <el-card class="metric-card"><div class="num">{{ overview.totalGames }}</div><div class="label">Total Games</div></el-card>
      <el-card class="metric-card"><div class="num">{{ overview.approvedGames }}</div><div class="label">Approved</div></el-card>
      <el-card class="metric-card"><div class="num">{{ overview.pendingGames }}</div><div class="label">Pending</div></el-card>
      <el-card class="metric-card"><div class="num">{{ overview.bridgeImplementedCount }}</div><div class="label">Bridge Implemented</div></el-card>
    </section>

    <el-card>
      <template #header>
        <div class="card-title">Android Runtime Config</div>
      </template>
      <el-form :model="configForm" label-width="190px" class="config-form">
        <el-form-item label="API Base URL">
          <el-input v-model="configForm.apiBaseUrl" />
        </el-form-item>
        <el-form-item label="Asset Host">
          <el-input v-model="configForm.assetHost" />
        </el-form-item>
        <el-form-item label="Default Language">
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
        <el-form-item label="Startup Route Policy">
          <el-input v-model="configForm.startupRoutePolicy" />
        </el-form-item>
        <el-form-item label="Max Zip Size (MB)">
          <el-input-number v-model="configForm.maxZipSizeMb" :min="20" :max="1024" />
        </el-form-item>
        <el-form-item label="Feature Toggles">
          <el-checkbox v-model="configForm.debugUseMockData">Debug Mock Data</el-checkbox>
          <el-checkbox v-model="configForm.enableSyncBridge">Enable Sync Bridge</el-checkbox>
          <el-checkbox v-model="configForm.allowCleartextTraffic">Allow Cleartext Traffic</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingConfig" @click="handleSaveConfig">Save Config</el-button>
          <span class="hint" v-if="configMeta.updatedAt">
            Last updated by {{ configMeta.updatedBy || 'unknown' }} at {{ configMeta.updatedAt }}
          </span>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="mt-16">
      <template #header>
        <div class="card-title">Bridge API Capability Matrix</div>
      </template>
      <el-table :data="bridgeApis" v-loading="loading" empty-text="No bridge APIs">
        <el-table-column prop="apiName" label="API" min-width="180" />
        <el-table-column prop="module" label="Module" min-width="140" />
        <el-table-column label="Status" width="130">
          <template #default="{ row }">
            <el-tag :type="row.supportStatus === 'implemented' ? 'success' : 'warning'">{{ row.supportStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Sync" width="90">
          <template #default="{ row }">{{ row.syncSupported ? 'Yes' : 'No' }}</template>
        </el-table-column>
        <el-table-column prop="notes" label="Notes" min-width="220" />
      </el-table>
    </el-card>

    <el-card class="mt-16">
      <template #header>
        <div class="card-title">Android Game Asset Table</div>
      </template>
      <el-table :data="gameAssets" v-loading="loading" empty-text="No game assets">
        <el-table-column prop="gameId" label="ID" width="80" />
        <el-table-column prop="appId" label="AppId" min-width="170" />
        <el-table-column prop="gameName" label="Game Name" min-width="180" />
        <el-table-column prop="version" label="Version" width="110" />
        <el-table-column prop="status" label="Status" width="120" />
        <el-table-column label="Runtime Ready" width="130">
          <template #default="{ row }">
            <el-tag :type="row.runtimeReady ? 'success' : 'info'">{{ row.runtimeReady ? 'Ready' : 'Not Ready' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="md5" label="MD5" min-width="180" show-overflow-tooltip />
        <el-table-column prop="downloadUrl" label="Download URL" min-width="260" show-overflow-tooltip />
        <el-table-column prop="updatedAt" label="Updated At" min-width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAndroidConsole, updateAndroidConfig } from '../api'

const loading = ref(false)
const savingConfig = ref(false)

const overview = reactive({
  totalGames: 0,
  approvedGames: 0,
  pendingGames: 0,
  processingGames: 0,
  rejectedGames: 0,
  bridgeImplementedCount: 0,
  bridgeStubCount: 0
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

const bridgeApis = ref([])
const gameAssets = ref([])

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

const loadAll = async () => {
  loading.value = true
  try {
    const res = await getAndroidConsole()
    Object.assign(overview, res.data?.overview || {})
    assignConfig(res.data?.config || {})
    bridgeApis.value = res.data?.bridgeApis || []
    gameAssets.value = res.data?.gameAssets || []
  } catch (error) {
    ElMessage.error(error.message || 'Failed to load android console data')
  } finally {
    loading.value = false
  }
}

const handleSaveConfig = async () => {
  savingConfig.value = true
  try {
    const res = await updateAndroidConfig({ ...configForm })
    assignConfig(res.data || {})
    ElMessage.success('Android runtime config saved')
  } catch (error) {
    ElMessage.error(error.message || 'Failed to save android config')
  } finally {
    savingConfig.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.page-shell {
  padding: 24px;
}

.page-header {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-header h1 {
  margin: 0 0 8px;
}

.page-header p {
  margin: 0;
  color: #6b7280;
}

.actions {
  display: flex;
  gap: 8px;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.metric-card .num {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}

.metric-card .label {
  color: #6b7280;
  margin-top: 4px;
}

.card-title {
  font-weight: 600;
}

.config-form :deep(.el-checkbox) {
  margin-right: 16px;
}

.hint {
  margin-left: 12px;
  color: #6b7280;
  font-size: 12px;
}

.mt-16 {
  margin-top: 16px;
}

@media (max-width: 1100px) {
  .metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
