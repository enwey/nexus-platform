<template>
  <div class="page-shell">
    <header class="page-header">
      <div>
        <h1>Runtime Ops Console</h1>
        <p>{{ lt('管理运行时配置、游戏元数据及发现页运营配置。', '管理運行時配置、遊戲中繼資料與發現頁營運配置。', 'Manage runtime config, game metadata, and discover operation settings.') }}</p>
      </div>
      <div class="actions">
        <el-button @click="$router.push('/audit')">{{ lt('返回审核台', '返回審核台', 'Back To Audit') }}</el-button>
        <el-button type="primary" @click="loadAll">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
      </div>
    </header>

    <section class="metrics">
      <el-card class="metric-card"><div class="num">{{ overview.totalGames }}</div><div class="label">{{ lt('游戏总数', '遊戲總數', 'Total Games') }}</div></el-card>
      <el-card class="metric-card"><div class="num">{{ overview.approvedGames }}</div><div class="label">{{ lt('已通过', '已通過', 'Approved') }}</div></el-card>
      <el-card class="metric-card"><div class="num">{{ overview.pendingGames }}</div><div class="label">{{ lt('待审核', '待審核', 'Pending') }}</div></el-card>
      <el-card class="metric-card"><div class="num">{{ overview.bridgeImplementedCount }}</div><div class="label">{{ lt('桥接已实现', '橋接已實作', 'Bridge Implemented') }}</div></el-card>
    </section>

    <el-card>
      <template #header><div class="card-title">{{ lt('运行时配置', '運行時配置', 'Runtime Config') }}</div></template>
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
          <span class="hint" v-if="configMeta.updatedAt">{{ lt('最后更新：', '最後更新：', 'Last updated:') }} {{ configMeta.updatedBy || '-' }} {{ configMeta.updatedAt }}</span>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="mt-16">
      <template #header><div class="card-title">{{ lt('游戏信息运营', '遊戲資訊營運', 'Game Metadata Ops') }}</div></template>
      <el-table :data="gameAssets" v-loading="loading" :empty-text="lt('暂无游戏数据', '暫無遊戲資料', 'No game assets')">
        <el-table-column prop="appId" label="AppId" min-width="170" />
        <el-table-column prop="gameName" :label="lt('游戏名称', '遊戲名稱', 'Game Name')" min-width="180" />
        <el-table-column prop="version" :label="lt('版本', '版本', 'Version')" width="100" />
        <el-table-column prop="status" :label="lt('状态', '狀態', 'Status')" width="110" />
        <el-table-column prop="category" :label="lt('分类', '分類', 'Category')" width="120"><template #default="{ row }">{{ row.category || '-' }}</template></el-table-column>
        <el-table-column :label="lt('操作', '操作', 'Action')" width="120" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="openGameEdit(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="mt-16">
      <template #header><div class="card-title">{{ lt('发现页运营配置', '發現頁營運配置', 'Discover Ops Config') }}</div></template>
      <el-form :model="discoverForm" label-width="120px">
        <el-form-item :label="lt('推荐游戏', '推薦遊戲', 'Hero Game')">
          <el-select v-model="discoverForm.heroAppId" filterable style="width:100%">
            <el-option v-for="item in discoverGameOptions" :key="item.appId" :label="`${item.name} (${item.appId})`" :value="item.appId" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('主标题', '主標題', 'Hero Title')"><el-input v-model="discoverForm.heroTitle" /></el-form-item>
        <el-form-item :label="lt('副标题', '副標題', 'Hero Subtitle')"><el-input v-model="discoverForm.heroSubtitle" /></el-form-item>
        <el-form-item :label="lt('角标', '角標', 'Hero Badge')"><el-input v-model="discoverForm.heroBadgeText" /></el-form-item>
        <el-form-item :label="lt('封面图', '封面圖', 'Hero Cover')"><el-input v-model="discoverForm.heroCoverUrl" /></el-form-item>
        <el-form-item :label="lt('热门排行', '熱門排行', 'Ranked')">
          <el-select v-model="discoverForm.rankedAppIds" multiple filterable style="width:100%"><el-option v-for="item in discoverGameOptions" :key="`rank_${item.appId}`" :label="`${item.name} (${item.appId})`" :value="item.appId" /></el-select>
        </el-form-item>
        <el-form-item :label="lt('新客必玩', '新客必玩', 'Newbie Picks')">
          <el-select v-model="discoverForm.newbieAppIds" multiple filterable style="width:100%"><el-option v-for="item in discoverGameOptions" :key="`new_${item.appId}`" :label="`${item.name} (${item.appId})`" :value="item.appId" /></el-select>
        </el-form-item>
        <el-form-item :label="lt('大家都在玩', '大家都在玩', 'Everyone Playing')">
          <el-select v-model="discoverForm.everyoneAppIds" multiple filterable style="width:100%"><el-option v-for="item in discoverGameOptions" :key="`all_${item.appId}`" :label="`${item.name} (${item.appId})`" :value="item.appId" /></el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" :loading="savingDiscover" @click="saveDiscoverConfig">{{ lt('保存运营配置', '儲存營運配置', 'Save Discover Config') }}</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card class="mt-16">
      <template #header><div class="card-title">Bridge API Capability Matrix</div></template>
      <el-table :data="bridgeApis" v-loading="loading" :empty-text="lt('暂无桥接能力数据', '暫無橋接能力資料', 'No bridge APIs')">
        <el-table-column prop="apiName" label="API" min-width="180" />
        <el-table-column prop="module" :label="lt('模块', '模組', 'Module')" min-width="140" />
        <el-table-column :label="lt('状态', '狀態', 'Status')" width="130"><template #default="{ row }"><el-tag :type="row.supportStatus === 'implemented' ? 'success' : 'warning'">{{ row.supportStatus }}</el-tag></template></el-table-column>
        <el-table-column :label="lt('同步', '同步', 'Sync')" width="90"><template #default="{ row }">{{ row.syncSupported ? lt('是', '是', 'Yes') : lt('否', '否', 'No') }}</template></el-table-column>
        <el-table-column prop="notes" :label="lt('说明', '說明', 'Notes')" min-width="220" />
      </el-table>
    </el-card>

    <el-dialog v-model="gameEditVisible" :title="lt('编辑游戏信息', '編輯遊戲資訊', 'Edit Game Metadata')" width="620px">
      <el-form :model="gameEditForm" label-width="100px">
        <el-form-item :label="lt('名称', '名稱', 'Name')"><el-input v-model="gameEditForm.name" /></el-form-item>
        <el-form-item :label="lt('分类', '分類', 'Category')"><el-select v-model="gameEditForm.category" style="width:100%"><el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" /></el-select></el-form-item>
        <el-form-item :label="lt('版本', '版本', 'Version')"><el-input v-model="gameEditForm.version" /></el-form-item>
        <el-form-item :label="lt('图标URL', '圖示 URL', 'Icon URL')"><el-input v-model="gameEditForm.iconUrl" /></el-form-item>
        <el-form-item :label="lt('标签', '標籤', 'Tags')"><el-input v-model="gameEditForm.tagsText" placeholder="tag1,tag2" /></el-form-item>
        <el-form-item :label="lt('描述', '描述', 'Description')"><el-input v-model="gameEditForm.description" type="textarea" :rows="4" /></el-form-item>
        <el-divider>{{ lt('运营资料', '營運資料', 'Ops Profile') }}</el-divider>
        <el-form-item :label="lt('工作室名', '工作室名', 'Studio')"><el-input v-model="gameOpsForm.studioName" /></el-form-item>
        <el-form-item :label="lt('玩家数文案', '玩家數文案', 'Player Count')"><el-input v-model="gameOpsForm.playerCountText" /></el-form-item>
        <el-form-item :label="lt('运行页横幅', '運行頁橫幅', 'Runtime Banner')"><el-input v-model="gameOpsForm.runtimeBannerUrl" /></el-form-item>
        <el-form-item :label="lt('运行页Logo', '運行頁 Logo', 'Runtime Logo')"><el-input v-model="gameOpsForm.runtimeLogoUrl" /></el-form-item>
        <el-form-item :label="lt('分享标题', '分享標題', 'Share Title')"><el-input v-model="gameOpsForm.shareTitle" /></el-form-item>
        <el-form-item :label="lt('分享副标题', '分享副標題', 'Share Subtitle')"><el-input v-model="gameOpsForm.shareSubtitle" /></el-form-item>
        <el-form-item :label="lt('分享图', '分享圖', 'Share Image')"><el-input v-model="gameOpsForm.shareImageUrl" /></el-form-item>
        <el-form-item :label="lt('发现卡片封面', '發現卡片封面', 'Discover Cover')"><el-input v-model="gameOpsForm.discoverCardCoverUrl" /></el-form-item>
        <el-form-item :label="lt('发现卡片Logo', '發現卡片 Logo', 'Discover Logo')"><el-input v-model="gameOpsForm.discoverCardLogoUrl" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="gameEditVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="savingGame" @click="saveGameMetadata">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getAndroidConsole,
  getGameList,
  updateAndroidConfig,
  getGameCategories,
  updateGameMetadata,
  getOpsGameProfile,
  updateOpsGameProfile,
  getDiscoverOpsConfig,
  updateDiscoverOpsConfig
} from '../api'
import { useI18nLite } from '../i18n'

const { lt } = useI18nLite()
const loading = ref(false)
const savingConfig = ref(false)
const savingDiscover = ref(false)
const savingGame = ref(false)

const categoryOptions = ref(['all', 'action', 'casual', 'rpg'])
const discoverGameOptions = ref([])

const overview = reactive({ totalGames: 0, approvedGames: 0, pendingGames: 0, processingGames: 0, rejectedGames: 0, bridgeImplementedCount: 0, bridgeStubCount: 0 })
const configForm = reactive({ apiBaseUrl: '', assetHost: '', defaultLanguage: 'zh-TW', debugUseMockData: true, enableSyncBridge: true, allowCleartextTraffic: false, webViewMixedContentMode: 'MIXED_CONTENT_NEVER_ALLOW', webViewCacheMode: 'LOAD_DEFAULT', maxZipSizeMb: 200, startupRoutePolicy: 'SPLASH_DIRECT_TO_MAIN' })
const configMeta = reactive({ updatedBy: '', updatedAt: '' })
const bridgeApis = ref([])
const gameAssets = ref([])

const discoverForm = reactive({ heroAppId: '', heroTitle: '', heroSubtitle: '', heroBadgeText: '', heroCoverUrl: '', rankedAppIds: [], newbieAppIds: [], everyoneAppIds: [] })

const gameEditVisible = ref(false)
const editingGameId = ref(null)
const gameEditForm = reactive({ name: '', category: 'all', description: '', iconUrl: '', version: '', tagsText: '' })
const gameOpsForm = reactive({
  studioName: '',
  playerCountText: '',
  runtimeBannerUrl: '',
  runtimeLogoUrl: '',
  shareTitle: '',
  shareSubtitle: '',
  shareImageUrl: '',
  discoverCardCoverUrl: '',
  discoverCardLogoUrl: ''
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

const assignDiscoverConfig = (data = {}) => {
  discoverGameOptions.value = data.availableGames || []
  discoverForm.heroAppId = data.hero?.appId || ''
  discoverForm.heroTitle = data.hero?.title || ''
  discoverForm.heroSubtitle = data.hero?.subtitle || ''
  discoverForm.heroBadgeText = data.hero?.badgeText || ''
  discoverForm.heroCoverUrl = data.hero?.coverUrl || ''
  discoverForm.rankedAppIds = [...(data.rankedAppIds || [])]
  discoverForm.newbieAppIds = [...(data.newbieAppIds || [])]
  discoverForm.everyoneAppIds = [...(data.everyoneAppIds || [])]
}

const loadAll = async () => {
  loading.value = true
  try {
    const [consoleRes, gamesRes, categoriesRes, discoverRes] = await Promise.all([
      getAndroidConsole(),
      getGameList(),
      getGameCategories().catch(() => ({ data: ['all', 'action', 'casual', 'rpg'] })),
      getDiscoverOpsConfig().catch(() => ({ data: {} }))
    ])
    Object.assign(overview, consoleRes.data?.overview || {})
    assignConfig(consoleRes.data?.config || {})
    bridgeApis.value = consoleRes.data?.bridgeApis || []
    gameAssets.value = (gamesRes.data || []).map((game) => ({ gameId: game.id, appId: game.appId, gameName: game.name, version: game.version, status: game.status, category: game.category, description: game.description, iconUrl: game.iconUrl, tagsJson: game.tagsJson }))
    categoryOptions.value = categoriesRes.data?.length ? categoriesRes.data : ['all', 'action', 'casual', 'rpg']
    assignDiscoverConfig(discoverRes.data || {})
  } catch (error) {
    ElMessage.error(error.message || lt('加载运营控制台数据失败', '載入營運控制台資料失敗', 'Failed to load runtime console data'))
  } finally {
    loading.value = false
  }
}

const handleSaveConfig = async () => {
  savingConfig.value = true
  try {
    const res = await updateAndroidConfig({ ...configForm })
    assignConfig(res.data || {})
    ElMessage.success(lt('运行时配置已保存', '運行時配置已儲存', 'Runtime config saved'))
  } catch (error) {
    ElMessage.error(error.message || lt('保存运行时配置失败', '儲存運行時配置失敗', 'Failed to save runtime config'))
  } finally {
    savingConfig.value = false
  }
}

const saveDiscoverConfig = async () => {
  savingDiscover.value = true
  try {
    const payload = {
      hero: discoverForm.heroAppId ? { appId: discoverForm.heroAppId, title: discoverForm.heroTitle, subtitle: discoverForm.heroSubtitle, badgeText: discoverForm.heroBadgeText, coverUrl: discoverForm.heroCoverUrl } : null,
      rankedAppIds: discoverForm.rankedAppIds,
      newbieAppIds: discoverForm.newbieAppIds,
      everyoneAppIds: discoverForm.everyoneAppIds
    }
    const res = await updateDiscoverOpsConfig(payload)
    assignDiscoverConfig(res.data || {})
    ElMessage.success(lt('发现页运营配置已保存', '發現頁營運配置已儲存', 'Discover ops config saved'))
  } catch (error) {
    ElMessage.error(error.message || lt('保存发现页运营配置失败', '儲存發現頁營運配置失敗', 'Failed to save discover config'))
  } finally {
    savingDiscover.value = false
  }
}

const resetGameOpsForm = () => {
  gameOpsForm.studioName = ''
  gameOpsForm.playerCountText = ''
  gameOpsForm.runtimeBannerUrl = ''
  gameOpsForm.runtimeLogoUrl = ''
  gameOpsForm.shareTitle = ''
  gameOpsForm.shareSubtitle = ''
  gameOpsForm.shareImageUrl = ''
  gameOpsForm.discoverCardCoverUrl = ''
  gameOpsForm.discoverCardLogoUrl = ''
}

const openGameEdit = async (row) => {
  editingGameId.value = row.gameId
  gameEditForm.name = row.gameName || ''
  gameEditForm.category = row.category || 'all'
  gameEditForm.description = row.description || ''
  gameEditForm.iconUrl = row.iconUrl || ''
  gameEditForm.version = row.version || ''
  gameEditForm.tagsText = row.tagsJson || ''
  resetGameOpsForm()
  try {
    const profileRes = await getOpsGameProfile(row.gameId)
    const profile = profileRes.data || {}
    gameOpsForm.studioName = profile.studioName || ''
    gameOpsForm.playerCountText = profile.playerCountText || ''
    gameOpsForm.runtimeBannerUrl = profile.runtimeBannerUrl || ''
    gameOpsForm.runtimeLogoUrl = profile.runtimeLogoUrl || ''
    gameOpsForm.shareTitle = profile.shareTitle || ''
    gameOpsForm.shareSubtitle = profile.shareSubtitle || ''
    gameOpsForm.shareImageUrl = profile.shareImageUrl || ''
    gameOpsForm.discoverCardCoverUrl = profile.discoverCardCoverUrl || ''
    gameOpsForm.discoverCardLogoUrl = profile.discoverCardLogoUrl || ''
  } catch (error) {
    ElMessage.warning(error.message || lt('未能加载运营资料，将使用空白默认值', '未能載入營運資料，將使用空白預設值', 'Failed to load ops profile, defaults are used'))
  }
  gameEditVisible.value = true
}

const saveGameMetadata = async () => {
  if (!editingGameId.value) return
  savingGame.value = true
  try {
    await Promise.all([
      updateGameMetadata(editingGameId.value, {
        name: gameEditForm.name,
        category: gameEditForm.category,
        description: gameEditForm.description,
        iconUrl: gameEditForm.iconUrl,
        version: gameEditForm.version,
        tags: (gameEditForm.tagsText || '').split(',').map((x) => x.trim()).filter(Boolean)
      }),
      updateOpsGameProfile(editingGameId.value, {
        studioName: gameOpsForm.studioName,
        playerCountText: gameOpsForm.playerCountText,
        runtimeBannerUrl: gameOpsForm.runtimeBannerUrl,
        runtimeLogoUrl: gameOpsForm.runtimeLogoUrl,
        shareTitle: gameOpsForm.shareTitle,
        shareSubtitle: gameOpsForm.shareSubtitle,
        shareImageUrl: gameOpsForm.shareImageUrl,
        discoverCardCoverUrl: gameOpsForm.discoverCardCoverUrl,
        discoverCardLogoUrl: gameOpsForm.discoverCardLogoUrl
      })
    ])
    ElMessage.success(lt('游戏信息与运营资料已更新', '遊戲資訊與營運資料已更新', 'Game metadata and ops profile updated'))
    gameEditVisible.value = false
    await loadAll()
  } catch (error) {
    ElMessage.error(error.message || lt('更新失败', '更新失敗', 'Update failed'))
  } finally {
    savingGame.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.page-shell { padding: 24px; }
.page-header { margin-bottom: 16px; display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.page-header h1 { margin: 0 0 8px; }
.page-header p { margin: 0; color: #6b7280; }
.actions { display: flex; gap: 8px; }
.metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.metric-card .num { font-size: 24px; font-weight: 700; line-height: 1.2; }
.metric-card .label { color: #6b7280; margin-top: 4px; }
.card-title { font-weight: 600; }
.config-form :deep(.el-checkbox) { margin-right: 16px; }
.hint { margin-left: 12px; color: #6b7280; font-size: 12px; }
.mt-16 { margin-top: 16px; }
@media (max-width: 1100px) { .metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>
