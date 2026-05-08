<template>
  <div class="pro-page">
    <el-card class="panel-card" v-loading="loading">
      <template #header>
        <div class="head-row">
          <div>
            <div class="breadcrumb-line">
              <el-button link @click="router.push('/pro/game-version/games/list')">{{ lt('返回游戏列表', '返回遊戲列表', 'Back to Game List') }}</el-button>
              <span>/</span>
              <span>{{ game?.name || lt('游戏详情', '遊戲詳情', 'Game Detail') }}</span>
            </div>
            <div class="panel-title">{{ game?.name || '-' }}</div>
            <div class="panel-subtitle">{{ game?.appId || '-' }}</div>
          </div>
          <div class="actions">
            <el-button @click="router.push({ name: 'OpsVersionList', query: { gameId: String(game?.id || '') } })">{{ lt('查看版本列表', '查看版本列表', 'Open Version List') }}</el-button>
            <el-button @click="openMetadataEditor">{{ lt('编辑基础信息', '編輯基礎資訊', 'Edit Metadata') }}</el-button>
            <el-button @click="openProfileEditor">{{ lt('编辑运行时资料', '編輯運行時資料', 'Edit Runtime Profile') }}</el-button>
            <el-button type="warning" @click="openVisibilityDialog">{{ lt('展示控制', '展示控制', 'Visibility Control') }}</el-button>
            <el-button :loading="loading" @click="loadDetail">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <el-empty v-if="!game" :description="lt('未找到对应游戏', '未找到對應遊戲', 'Game not found')" />
      <template v-else>
        <div class="summary-grid">
          <div class="summary-item">
            <span>{{ lt('当前状态', '當前狀態', 'Status') }}</span>
            <strong><el-tag :type="getStatusType(game.status)">{{ getStatusText(game.status) }}</el-tag></strong>
          </div>
          <div class="summary-item">
            <span>{{ lt('前端状态', '前端狀態', 'Frontend State') }}</span>
            <strong><el-tag :type="getFrontendStateType(game.frontendState)">{{ getFrontendStateText(game.frontendState) }}</el-tag></strong>
          </div>
          <div class="summary-item">
            <span>{{ lt('展示控制', '展示控制', 'Visibility') }}</span>
            <strong><el-tag :type="getVisibilityType(game.visibilityStatus)">{{ getVisibilityText(game.visibilityStatus) }}</el-tag></strong>
          </div>
          <div class="summary-item">
            <span>{{ lt('影响等级', '影響等級', 'Impact Level') }}</span>
            <strong><el-tag :type="getImpactType(game.impactLevel)">{{ getImpactText(game.impactLevel) }}</el-tag></strong>
          </div>
        </div>

        <el-descriptions :column="2" border class="section">
          <el-descriptions-item :label="lt('游戏名称', '遊戲名稱', 'Game Name')">{{ game.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="AppID">{{ game.appId || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('分类', '分類', 'Category')">{{ game.category || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('当前版本', '當前版本', 'Current Version')">{{ game.version || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('开发者 ID', '開發者 ID', 'Developer ID')">{{ game.developerId || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('需要联网', '需要連網', 'Requires Online')">{{ game.requiresOnline ? lt('是', '是', 'Yes') : lt('否', '否', 'No') }}</el-descriptions-item>
          <el-descriptions-item :label="lt('图标地址', '圖示位址', 'Icon URL')" :span="2">{{ game.iconUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('描述', '描述', 'Description')" :span="2">{{ game.description || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('标签', '標籤', 'Tags')" :span="2">
            <div class="tag-list">
              <el-tag v-for="tag in normalizeTags(game.tags)" :key="tag" size="small">{{ tag }}</el-tag>
              <span v-if="!normalizeTags(game.tags).length">-</span>
            </div>
          </el-descriptions-item>
        </el-descriptions>

        <el-card class="section" shadow="never">
          <template #header>
            <div class="section-head">
              <span>{{ lt('运行时展示资料', '運行時展示資料', 'Runtime Presentation Profile') }}</span>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item :label="lt('工作室名称', '工作室名稱', 'Studio Name')">{{ profile.studioName || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="lt('玩家数文案', '玩家數文案', 'Player Count Text')">{{ profile.playerCountText || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="lt('运行时 Banner', '運行時 Banner', 'Runtime Banner')">{{ profile.runtimeBannerUrl || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="lt('运行时 Logo', '運行時 Logo', 'Runtime Logo')">{{ profile.runtimeLogoUrl || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="lt('分享标题', '分享標題', 'Share Title')">{{ profile.shareTitle || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="lt('分享副标题', '分享副標題', 'Share Subtitle')">{{ profile.shareSubtitle || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="lt('分享图片', '分享圖片', 'Share Image')">{{ profile.shareImageUrl || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="lt('支持邮箱', '支援郵箱', 'Support Email')">{{ profile.supportEmail || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="lt('支持链接', '支援連結', 'Support URL')">{{ profile.supportUrl || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="lt('社区链接', '社群連結', 'Community URL')">{{ profile.communityUrl || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="lt('发现卡片封面', '發現卡片封面', 'Discover Cover')">{{ profile.discoverCardCoverUrl || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="lt('发现卡片 Logo', '發現卡片 Logo', 'Discover Logo')">{{ profile.discoverCardLogoUrl || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="lt('营销短句', '營銷短句', 'Marketing Tagline')" :span="2">{{ profile.marketingTagline || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="lt('营销摘要', '營銷摘要', 'Marketing Summary')" :span="2">{{ profile.marketingSummary || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card class="section" shadow="never">
          <template #header>
            <div class="section-head">
              <span>{{ lt('治理概况', '治理概況', 'Governance Overview') }}</span>
              <el-button link type="primary" @click="loadImpact">{{ lt('查看影响分析', '查看影響分析', 'Impact Analysis') }}</el-button>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item :label="lt('渠道治理', '渠道治理', 'Channel Governance')">{{ formatScopeSummary(game.governanceScope?.channelMode, game.governanceScope?.channels) }}</el-descriptions-item>
            <el-descriptions-item :label="lt('地区治理', '地區治理', 'Region Governance')">{{ formatScopeSummary(game.governanceScope?.regionMode, game.governanceScope?.regions) }}</el-descriptions-item>
            <el-descriptions-item :label="lt('版本治理', '版本治理', 'Version Governance')">{{ formatVersionGovernance(game.governanceScope) }}</el-descriptions-item>
            <el-descriptions-item :label="lt('治理备注', '治理備註', 'Governance Note')">{{ game.governanceScope?.governanceNote || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="lt('封控说明', '封控說明', 'Visibility Note')" :span="2">{{ game.visibilityReason || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div v-if="impactData" class="impact-box">
            <div class="impact-summary">{{ impactData.summary || '-' }}</div>
            <div class="impact-lines">
              <span>{{ lt('渠道命中', '渠道命中', 'Channels') }} {{ impactData.affectedChannelCount }}</span>
              <span>{{ lt('地区命中', '地區命中', 'Regions') }} {{ impactData.affectedRegionCount }}</span>
              <span>{{ lt('版本拦截', '版本攔截', 'Versions') }} {{ impactData.affectedVersionCount }}</span>
              <span>{{ lt('影响面', '影響面', 'Surfaces') }} {{ impactData.affectedSurfaceCount }}</span>
            </div>
          </div>
        </el-card>

        <el-card class="section" shadow="never">
          <template #header>
            <div class="section-head">
              <span>{{ lt('版本记录', '版本記錄', 'Version History') }}</span>
              <el-button link type="primary" @click="router.push({ name: 'OpsVersionList', query: { gameId: String(game.id) } })">{{ lt('展开版本列表', '展開版本列表', 'Open Full Version List') }}</el-button>
            </div>
          </template>
          <el-table :data="versions" :empty-text="lt('暂无版本记录', '暫無版本記錄', 'No versions')">
            <el-table-column prop="versionName" :label="lt('版本号', '版本號', 'Version')" min-width="120" />
            <el-table-column prop="status" :label="lt('状态', '狀態', 'Status')" width="140">
              <template #default="{ row }">
                <el-tag :type="getVersionStatusType(row.status)">{{ getVersionStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="lt('Manifest', 'Manifest', 'Manifest')" width="120">
              <template #default="{ row }">
                <el-tag :type="row.hostedManifestValid ? 'success' : row.hostedManifestValid === false ? 'danger' : 'info'">
                  {{ row.hostedManifestValid === true ? lt('通过', '通過', 'Passed') : row.hostedManifestValid === false ? lt('失败', '失敗', 'Failed') : lt('未知', '未知', 'Unknown') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="lt('更新时间', '更新時間', 'Updated')" min-width="170">
              <template #default="{ row }">{{ formatDate(row.updatedAt || row.createdAt) }}</template>
            </el-table-column>
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openVersionDetail(row)">{{ lt('查看详情', '查看詳情', 'View Detail') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </template>
    </el-card>

    <el-dialog v-model="metadataVisible" :title="lt('编辑游戏基础信息', '編輯遊戲基礎資訊', 'Edit Game Metadata')" width="640px">
      <el-form :model="metadataForm" label-width="120px">
        <el-form-item :label="lt('游戏名称', '遊戲名稱', 'Game Name')" required>
          <el-input v-model="metadataForm.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item :label="lt('游戏描述', '遊戲描述', 'Description')">
          <el-input v-model="metadataForm.description" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item :label="lt('图标地址', '圖示位址', 'Icon URL')">
          <el-input v-model="metadataForm.iconUrl" />
        </el-form-item>
        <el-form-item :label="lt('分类', '分類', 'Category')">
          <el-select v-model="metadataForm.category" clearable filterable style="width: 100%">
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('版本', '版本', 'Version')">
          <el-input v-model="metadataForm.version" />
        </el-form-item>
        <el-form-item :label="lt('标签', '標籤', 'Tags')">
          <el-select v-model="metadataForm.tags" multiple filterable allow-create default-first-option style="width: 100%">
            <el-option v-for="tag in tagOptions" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('需要联网', '需要連網', 'Requires Online')">
          <el-switch v-model="metadataForm.requiresOnline" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="metadataVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="savingMetadata" @click="saveMetadata">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="profileVisible" :title="lt('编辑运行时资料', '編輯運行時資料', 'Edit Runtime Profile')" width="760px">
      <el-form :model="profileForm" label-width="140px">
        <el-form-item :label="lt('工作室名称', '工作室名稱', 'Studio Name')"><el-input v-model="profileForm.studioName" /></el-form-item>
        <el-form-item :label="lt('玩家数文案', '玩家數文案', 'Player Count Text')"><el-input v-model="profileForm.playerCountText" /></el-form-item>
        <el-form-item :label="lt('运行时 Banner', '運行時 Banner', 'Runtime Banner')"><el-input v-model="profileForm.runtimeBannerUrl" /></el-form-item>
        <el-form-item :label="lt('运行时 Logo', '運行時 Logo', 'Runtime Logo')"><el-input v-model="profileForm.runtimeLogoUrl" /></el-form-item>
        <el-form-item :label="lt('分享标题', '分享標題', 'Share Title')"><el-input v-model="profileForm.shareTitle" /></el-form-item>
        <el-form-item :label="lt('分享副标题', '分享副標題', 'Share Subtitle')"><el-input v-model="profileForm.shareSubtitle" /></el-form-item>
        <el-form-item :label="lt('分享图片', '分享圖片', 'Share Image')"><el-input v-model="profileForm.shareImageUrl" /></el-form-item>
        <el-form-item :label="lt('发现卡片封面', '發現卡片封面', 'Discover Cover')"><el-input v-model="profileForm.discoverCardCoverUrl" /></el-form-item>
        <el-form-item :label="lt('发现卡片 Logo', '發現卡片 Logo', 'Discover Logo')"><el-input v-model="profileForm.discoverCardLogoUrl" /></el-form-item>
        <el-form-item :label="lt('营销短句', '營銷短句', 'Marketing Tagline')"><el-input v-model="profileForm.marketingTagline" /></el-form-item>
        <el-form-item :label="lt('营销摘要', '營銷摘要', 'Marketing Summary')"><el-input v-model="profileForm.marketingSummary" type="textarea" :rows="3" /></el-form-item>
        <el-form-item :label="lt('支持邮箱', '支援郵箱', 'Support Email')"><el-input v-model="profileForm.supportEmail" /></el-form-item>
        <el-form-item :label="lt('支持链接', '支援連結', 'Support URL')"><el-input v-model="profileForm.supportUrl" /></el-form-item>
        <el-form-item :label="lt('社区链接', '社群連結', 'Community URL')"><el-input v-model="profileForm.communityUrl" /></el-form-item>
        <el-form-item :label="lt('合规说明', '合規說明', 'Compliance Note')"><el-input v-model="profileForm.complianceNote" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="savingProfile" @click="saveProfile">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="visibilityVisible" :title="lt('前端展示控制', '前端展示控制', 'Frontend Visibility Control')" width="560px">
      <el-form :model="visibilityForm" label-width="120px">
        <el-form-item :label="lt('控制状态', '控制狀態', 'Control Status')" required>
          <el-select v-model="visibilityForm.visibilityStatus" style="width: 100%">
            <el-option :label="lt('正常展示', '正常展示', 'Visible')" value="VISIBLE" />
            <el-option :label="lt('隐藏', '隱藏', 'Hidden')" value="HIDDEN" />
            <el-option :label="lt('封禁', '封禁', 'Blocked')" value="BLOCKED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('控制原因', '控制原因', 'Reason')">
          <el-input v-model="visibilityForm.reason" type="textarea" :rows="4" maxlength="256" show-word-limit />
        </el-form-item>
        <el-form-item v-if="visibilityForm.visibilityStatus === 'BLOCKED'" :label="lt('封禁截止', '封禁截止', 'Blocked Until')">
          <el-input v-model="visibilityForm.visibilityUntil" type="datetime-local" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visibilityVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="danger" :loading="savingVisibility" @click="saveVisibility">{{ lt('确认执行', '確認執行', 'Confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getGameVersions,
  getOpsGameCategories,
  getOpsGameGovernanceImpact,
  getOpsGameProfile,
  getOpsGames,
  updateOpsGameMetadata,
  updateOpsGameProfile,
  updateOpsGameVisibility
} from '../../api'
import { useI18nLite } from '../../i18n'
import { formatDate, normalizeTags, toDateTimeLocal } from './entityPageShared'

const props = defineProps({
  gameId: {
    type: [String, Number],
    required: true
  }
})

const route = useRoute()
const router = useRouter()
const { lt } = useI18nLite()

const loading = ref(false)
const savingMetadata = ref(false)
const savingProfile = ref(false)
const savingVisibility = ref(false)
const metadataVisible = ref(false)
const profileVisible = ref(false)
const visibilityVisible = ref(false)
const game = ref(null)
const versions = ref([])
const impactData = ref(null)
const categoryOptions = ref([])
const profile = reactive({})

const metadataForm = reactive({
  name: '',
  description: '',
  iconUrl: '',
  category: '',
  tags: [],
  version: '',
  requiresOnline: false
})

const profileForm = reactive({
  studioName: '',
  playerCountText: '',
  runtimeBannerUrl: '',
  runtimeLogoUrl: '',
  shareTitle: '',
  shareSubtitle: '',
  shareImageUrl: '',
  discoverCardCoverUrl: '',
  discoverCardLogoUrl: '',
  marketingTagline: '',
  marketingSummary: '',
  featureHighlights: [],
  targetAudience: '',
  supportEmail: '',
  supportUrl: '',
  communityUrl: '',
  complianceNote: '',
  operationsStatus: ''
})

const visibilityForm = reactive({
  visibilityStatus: 'VISIBLE',
  reason: '',
  visibilityUntil: ''
})

const tagOptions = computed(() => normalizeTags(game.value?.tags))

const applyMetadata = () => {
  metadataForm.name = game.value?.name || ''
  metadataForm.description = game.value?.description || ''
  metadataForm.iconUrl = game.value?.iconUrl || ''
  metadataForm.category = game.value?.category || ''
  metadataForm.tags = normalizeTags(game.value?.tags)
  metadataForm.version = game.value?.version || ''
  metadataForm.requiresOnline = Boolean(game.value?.requiresOnline)
}

const applyProfile = (value = {}) => {
  Object.assign(profile, value || {})
  Object.assign(profileForm, {
    studioName: value?.studioName || '',
    playerCountText: value?.playerCountText || '',
    runtimeBannerUrl: value?.runtimeBannerUrl || '',
    runtimeLogoUrl: value?.runtimeLogoUrl || '',
    shareTitle: value?.shareTitle || '',
    shareSubtitle: value?.shareSubtitle || '',
    shareImageUrl: value?.shareImageUrl || '',
    discoverCardCoverUrl: value?.discoverCardCoverUrl || '',
    discoverCardLogoUrl: value?.discoverCardLogoUrl || '',
    marketingTagline: value?.marketingTagline || '',
    marketingSummary: value?.marketingSummary || '',
    featureHighlights: value?.featureHighlights || [],
    targetAudience: value?.targetAudience || '',
    supportEmail: value?.supportEmail || '',
    supportUrl: value?.supportUrl || '',
    communityUrl: value?.communityUrl || '',
    complianceNote: value?.complianceNote || '',
    operationsStatus: value?.operationsStatus || ''
  })
}

const loadDetail = async () => {
  loading.value = true
  try {
    const [gamesRes, categoriesRes] = await Promise.all([getOpsGames(), getOpsGameCategories()])
    categoryOptions.value = Array.isArray(categoriesRes.data) ? categoriesRes.data : []
    game.value = (gamesRes.data || []).find((item) => String(item.id) === String(props.gameId)) || null
    if (!game.value) return
    const [versionsRes, profileRes] = await Promise.all([
      getGameVersions(game.value.id),
      getOpsGameProfile(game.value.id).catch(() => ({ data: {} }))
    ])
    versions.value = Array.isArray(versionsRes.data) ? versionsRes.data : []
    applyMetadata()
    applyProfile(profileRes.data || {})
  } catch (error) {
    ElMessage.error(error.message || lt('加载游戏详情失败', '載入遊戲詳情失敗', 'Failed to load game detail'))
  } finally {
    loading.value = false
  }
}

const loadImpact = async () => {
  if (!game.value?.id) return
  try {
    const res = await getOpsGameGovernanceImpact(game.value.id)
    impactData.value = res.data || null
  } catch (error) {
    ElMessage.error(error.message || lt('加载影响分析失败', '載入影響分析失敗', 'Failed to load impact analysis'))
  }
}

const openMetadataEditor = () => {
  applyMetadata()
  metadataVisible.value = true
}

const saveMetadata = async () => {
  const name = metadataForm.name.trim()
  if (!name || !game.value?.id) {
    ElMessage.warning(lt('请输入游戏名称', '請輸入遊戲名稱', 'Please input game name'))
    return
  }
  savingMetadata.value = true
  try {
    await updateOpsGameMetadata(game.value.id, {
      name,
      description: metadataForm.description.trim(),
      iconUrl: metadataForm.iconUrl.trim(),
      category: metadataForm.category || '',
      tags: metadataForm.tags,
      version: metadataForm.version.trim(),
      requiresOnline: Boolean(metadataForm.requiresOnline)
    })
    metadataVisible.value = false
    ElMessage.success(lt('游戏信息已保存', '遊戲資訊已儲存', 'Game info saved'))
    await loadDetail()
  } catch (error) {
    ElMessage.error(error.message || lt('保存游戏信息失败', '儲存遊戲資訊失敗', 'Failed to save game info'))
  } finally {
    savingMetadata.value = false
  }
}

const openProfileEditor = () => {
  applyProfile(profile)
  profileVisible.value = true
}

const saveProfile = async () => {
  if (!game.value?.id) return
  savingProfile.value = true
  try {
    await updateOpsGameProfile(game.value.id, {
      ...profileForm,
      featureHighlights: profileForm.featureHighlights || [],
      targetAudience: profileForm.targetAudience || '',
      operationsStatus: profileForm.operationsStatus || ''
    })
    profileVisible.value = false
    ElMessage.success(lt('运行时资料已保存', '運行時資料已儲存', 'Runtime profile saved'))
    await loadDetail()
  } catch (error) {
    ElMessage.error(error.message || lt('保存运行时资料失败', '儲存運行時資料失敗', 'Failed to save runtime profile'))
  } finally {
    savingProfile.value = false
  }
}

const openVisibilityDialog = () => {
  visibilityForm.visibilityStatus = game.value?.visibilityStatus || 'VISIBLE'
  visibilityForm.reason = game.value?.visibilityReason || ''
  visibilityForm.visibilityUntil = toDateTimeLocal(game.value?.visibilityUntil)
  visibilityVisible.value = true
}

const saveVisibility = async () => {
  if (!game.value?.id) return
  if (visibilityForm.visibilityStatus !== 'VISIBLE' && visibilityForm.reason.trim().length < 2) {
    ElMessage.warning(lt('封控原因至少 2 个字符', '封控原因至少 2 個字元', 'Reason must be at least 2 characters'))
    return
  }
  try {
    await ElMessageBox.confirm(
      lt('确认更新前端展示控制？该操作会立即影响前端消费链路。', '確認更新前端展示控制？此操作會立即影響前端消費鏈路。', 'Confirm updating frontend visibility control? This will immediately affect frontend consumption.'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { confirmButtonText: lt('确认执行', '確認執行', 'Confirm'), cancelButtonText: lt('取消', '取消', 'Cancel'), type: 'warning' }
    )
    savingVisibility.value = true
    await updateOpsGameVisibility(game.value.id, {
      visibilityStatus: visibilityForm.visibilityStatus,
      reason: visibilityForm.reason.trim(),
      visibilityUntil: visibilityForm.visibilityUntil ? `${visibilityForm.visibilityUntil}:00` : null
    })
    visibilityVisible.value = false
    ElMessage.success(lt('展示控制已更新', '展示控制已更新', 'Visibility control updated'))
    await loadDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('更新展示控制失败', '更新展示控制失敗', 'Failed to update visibility control'))
    }
  } finally {
    savingVisibility.value = false
  }
}

const openVersionDetail = (version) => {
  router.push({ name: 'OpsVersionDetail', params: { versionId: version.id }, query: { gameId: String(game.value?.id || ''), from: route.fullPath } })
}

const getStatusText = (status) => ({
  PROCESSING: lt('处理中', '處理中', 'Processing'),
  DRAFT: lt('草稿', '草稿', 'Draft'),
  PENDING: lt('待审核', '待審核', 'Pending'),
  APPROVED: lt('已通过', '已通過', 'Approved'),
  REJECTED: lt('已驳回', '已駁回', 'Rejected')
}[status] || status || '-')

const getStatusType = (status) => ({
  PROCESSING: 'warning',
  DRAFT: 'info',
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger'
}[status] || 'info')

const getFrontendStateText = (state) => ({
  APPROVED: lt('可运营', '可營運', 'Operable'),
  PENDING: lt('待审核中', '待審核中', 'Under Review'),
  DRAFT: lt('未开放', '未開放', 'Not Open'),
  REJECTED: lt('已拦截', '已攔截', 'Blocked'),
  PROCESSING: lt('处理中', '處理中', 'Processing'),
  HIDDEN: lt('已隐藏', '已隱藏', 'Hidden')
}[state] || state || '-')

const getFrontendStateType = (state) => ({
  APPROVED: 'success',
  PENDING: 'warning',
  DRAFT: 'info',
  REJECTED: 'danger',
  PROCESSING: '',
  HIDDEN: 'warning',
  BLOCKED: 'danger',
  OPERABLE: 'success',
  UNDER_REVIEW: 'warning',
  NOT_OPEN: 'info'
}[state] || 'info')

const getVisibilityText = (status) => ({
  VISIBLE: lt('正常展示', '正常展示', 'Visible'),
  HIDDEN: lt('隐藏', '隱藏', 'Hidden'),
  BLOCKED: lt('封禁', '封禁', 'Blocked')
}[status] || status || '-')

const getVisibilityType = (status) => ({
  VISIBLE: 'success',
  HIDDEN: 'warning',
  BLOCKED: 'danger'
}[status] || 'info')

const getImpactText = (level) => ({
  NONE: lt('无影响', '無影響', 'None'),
  LOW: lt('低', '低', 'Low'),
  MEDIUM: lt('中', '中', 'Medium'),
  HIGH: lt('高', '高', 'High'),
  CRITICAL: lt('极高', '極高', 'Critical')
}[level] || level || '-')

const getImpactType = (level) => ({
  NONE: 'info',
  LOW: 'success',
  MEDIUM: 'warning',
  HIGH: 'danger',
  CRITICAL: 'danger'
}[level] || 'info')

const getVersionStatusText = (status) => ({
  DRAFT: lt('草稿', '草稿', 'Draft'),
  PROCESSING: lt('处理中', '處理中', 'Processing'),
  SUBMITTED: lt('待审核', '待審核', 'Submitted'),
  APPROVED: lt('已通过', '已通過', 'Approved'),
  REJECTED: lt('已驳回', '已駁回', 'Rejected'),
  PUBLISHED: lt('已发布', '已發布', 'Published')
}[status] || status || '-')

const getVersionStatusType = (status) => ({
  DRAFT: 'info',
  PROCESSING: 'warning',
  SUBMITTED: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  PUBLISHED: 'success'
}[status] || 'info')

const formatScopeSummary = (mode, values) => {
  if (!mode || mode === 'ALL') return lt('不限制', '不限制', 'No restriction')
  const label = mode === 'ALLOWLIST'
    ? lt('白名单', '白名單', 'Allowlist')
    : lt('黑名单', '黑名單', 'Blocklist')
  const joined = Array.isArray(values) && values.length ? values.join(', ') : lt('未配置', '未配置', 'Not set')
  return `${label}: ${joined}`
}

const formatVersionGovernance = (scope) => {
  if (!scope || !scope.versionMode || scope.versionMode === 'ALL') {
    return lt('不限制', '不限制', 'No restriction')
  }
  if (scope.versionMode === 'MIN_VERSION') {
    return `${lt('最低版本', '最低版本', 'Min')}: ${scope.versionMin || '-'}`
  }
  if (scope.versionMode === 'RANGE') {
    return `${lt('版本范围', '版本範圍', 'Range')}: ${scope.versionMin || '-'} ~ ${scope.versionMax || '-'}`
  }
  return `${lt('黑名单', '黑名單', 'Blocklist')}: ${(scope.blockedVersions || []).join(', ') || '-'}`
}

onMounted(loadDetail)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 18px; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.breadcrumb-line { display: flex; align-items: center; gap: 8px; color: #667085; font-size: 13px; margin-bottom: 8px; }
.panel-title { font-size: 20px; font-weight: 800; color: #101828; }
.panel-subtitle { margin-top: 6px; color: #667085; font-size: 13px; }
.actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.summary-item { padding: 12px 14px; border-radius: 14px; background: #f8fafc; display: flex; flex-direction: column; gap: 6px; color: #475467; font-size: 13px; }
.summary-item strong { font-size: 16px; color: #111827; }
.section { margin-top: 16px; }
.section-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.tag-list { display: flex; flex-wrap: wrap; gap: 6px; }
.impact-box { margin-top: 16px; padding: 14px 16px; border-radius: 14px; background: #f8fbff; border: 1px solid #dbe8ff; }
.impact-summary { font-weight: 700; color: #0f172a; }
.impact-lines { display: flex; flex-wrap: wrap; gap: 16px; margin-top: 8px; color: #475467; font-size: 13px; }
@media (max-width: 920px) {
  .head-row { flex-direction: column; }
  .summary-grid { grid-template-columns: 1fr 1fr; }
}
</style>
