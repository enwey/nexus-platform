<template>
  <div class="games-page">
    <el-card class="panel-card">
      <template #header>
        <div class="panel-head">
          <div>
            <div class="panel-title">{{ lt('游戏资产中心', '遊戲資產中心', 'Game Asset Center') }}</div>
            <div class="panel-subtitle">{{ lt('管理主资料、素材库与运营投放资料，所有变更都会进入后端校验与留痕。', '管理主資料、素材庫與營運投放資料，所有變更都會進入後端校驗與留痕。', 'Manage metadata, media library, and operational assets with server-side validation and audit trails.') }}</div>
          </div>
          <div class="panel-actions">
            <el-input v-model.trim="keyword" clearable style="width: 240px" :placeholder="lt('搜索游戏名 / AppID', '搜尋遊戲名 / AppID', 'Search by game name / AppID')" />
            <el-button @click="loadGames">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <el-alert v-if="listError" class="page-error" type="error" :closable="false" :title="listError" />

      <el-table :data="filteredGames" v-loading="loading" @row-click="openEditor">
        <el-table-column prop="name" :label="lt('游戏名称', '遊戲名稱', 'Game')" min-width="180" />
        <el-table-column prop="appId" :label="lt('AppID', 'AppID', 'AppID')" min-width="180" />
        <el-table-column :label="lt('状态', '狀態', 'Status')" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusMeta(row.status).type">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" :label="lt('分类', '分類', 'Category')" width="140" />
        <el-table-column :label="lt('当前版本', '目前版本', 'Version')" width="120">
          <template #default="{ row }">{{ row.version || '-' }}</template>
        </el-table-column>
        <el-table-column :label="lt('最近更新', '最近更新', 'Updated')" min-width="170">
          <template #default="{ row }">{{ formatDate(row.updatedAt || row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="lt('资料完成度', '資料完成度', 'Completeness')" min-width="160">
          <template #default="{ row }">
            <el-progress :percentage="getCompleteness(row)" :stroke-width="10" />
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !filteredGames.length" :description="lt('还没有游戏资产，先上传一个游戏包再回来完善资料。', '還沒有遊戲資產，先上傳一個遊戲包再回來完善資料。', 'No game assets yet. Upload a package first and then complete the content workspace.')" />
    </el-card>

    <el-drawer v-model="drawerVisible" :title="lt('游戏资产工作台', '遊戲資產工作台', 'Game Content Workspace')" size="820px">
      <template v-if="activeGame">
        <el-tabs v-model="activeTab">
          <el-tab-pane name="basic" :label="lt('主资料', '主資料', 'Metadata')">
            <el-form :model="editForm" label-position="top" class="drawer-form">
              <el-form-item :label="lt('游戏名称', '遊戲名稱', 'Game Name')">
                <el-input v-model="editForm.name" />
              </el-form-item>
              <el-form-item :label="lt('分类', '分類', 'Category')">
                <el-select v-model="editForm.category" clearable filterable>
                  <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
              <el-form-item :label="lt('版本号', '版本號', 'Version')">
                <el-input v-model="editForm.version" />
              </el-form-item>
              <el-form-item :label="lt('图标 URL', '圖示 URL', 'Icon URL')">
                <el-input v-model="editForm.iconUrl" />
              </el-form-item>
              <el-form-item :label="lt('标签', '標籤', 'Tags')">
                <el-select v-model="editForm.tags" multiple filterable allow-create default-first-option>
                  <el-option v-for="tag in editForm.tags" :key="tag" :label="tag" :value="tag" />
                </el-select>
              </el-form-item>
              <el-form-item :label="lt('游戏简介', '遊戲簡介', 'Description')">
                <el-input v-model="editForm.description" type="textarea" :rows="5" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving" @click="handleSave">{{ lt('保存主资料', '儲存主資料', 'Save Metadata') }}</el-button>
              </el-form-item>
            </el-form>

            <el-divider />

            <div class="version-section">
              <div class="section-head">
                <div class="section-title">{{ lt('历史版本', '歷史版本', 'Version History') }}</div>
                <el-button text @click="loadVersions(activeGame.id)">{{ lt('刷新版本', '刷新版本', 'Refresh Versions') }}</el-button>
              </div>
              <el-table :data="versions" v-loading="versionsLoading" :empty-text="lt('暂无版本记录', '暫無版本記錄', 'No version records')">
                <el-table-column prop="versionName" :label="lt('版本', '版本', 'Version')" min-width="120" />
                <el-table-column :label="lt('状态', '狀態', 'Status')" width="120">
                  <template #default="{ row }">
                    <el-tag :type="getVersionMeta(row.status).type">{{ getVersionText(row.status) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="auditReason" :label="lt('审核反馈', '審核回饋', 'Review Feedback')" min-width="180" show-overflow-tooltip />
                <el-table-column :label="lt('更新时间', '更新時間', 'Updated')" min-width="170">
                  <template #default="{ row }">{{ formatDate(row.updatedAt || row.createdAt) }}</template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>

          <el-tab-pane name="assets" :label="lt('素材库', '素材庫', 'Media Library')">
            <div class="section-head">
              <div>
                <div class="section-title">{{ lt('我的游戏素材库', '我的遊戲素材庫', 'Game Media Library') }}</div>
                <div class="section-tip">{{ lt('维护截图、封面、分享图等视觉素材；删除需二次确认。', '維護截圖、封面、分享圖等視覺素材；刪除需二次確認。', 'Manage screenshots, covers, and share creatives with delete confirmation.') }}</div>
              </div>
              <div class="section-actions">
                <el-select v-model="assetGroupFilter" style="width: 160px" @change="loadAssets">
                  <el-option value="" :label="lt('全部分组', '全部分組', 'All groups')" />
                  <el-option value="LIBRARY" :label="lt('素材库', '素材庫', 'Library')" />
                  <el-option value="OPS" :label="lt('运营资产', '營運資產', 'Ops assets')" />
                </el-select>
                <el-button @click="loadAssets">{{ lt('刷新素材', '刷新素材', 'Refresh') }}</el-button>
                <el-button type="primary" @click="openAssetCreate">{{ lt('新增素材', '新增素材', 'Add Asset') }}</el-button>
              </div>
            </div>

            <el-alert v-if="assetError" class="page-error" type="error" :closable="false" :title="assetError" />

            <el-table v-if="assets.length" :data="assets" v-loading="assetsLoading">
              <el-table-column prop="title" :label="lt('名称', '名稱', 'Title')" min-width="140">
                <template #default="{ row }">{{ row.title || '-' }}</template>
              </el-table-column>
              <el-table-column :label="lt('分组', '分組', 'Group')" width="110">
                <template #default="{ row }">{{ getAssetGroupText(row.assetGroup) }}</template>
              </el-table-column>
              <el-table-column :label="lt('角色', '角色', 'Role')" width="120">
                <template #default="{ row }">{{ getAssetRoleText(row.assetRole) }}</template>
              </el-table-column>
              <el-table-column :label="lt('状态', '狀態', 'Status')" width="110">
                <template #default="{ row }">
                  <el-tag :type="getAssetStatusMeta(row.assetStatus).type">{{ getAssetStatusText(row.assetStatus) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="locale" :label="lt('语言', '語言', 'Locale')" width="110" />
              <el-table-column :label="lt('主素材', '主素材', 'Primary')" width="90">
                <template #default="{ row }">{{ row.primary ? lt('是', '是', 'Yes') : lt('否', '否', 'No') }}</template>
              </el-table-column>
              <el-table-column :label="lt('最近更新', '最近更新', 'Updated')" min-width="160">
                <template #default="{ row }">{{ formatDate(row.updatedAt || row.createdAt) }}</template>
              </el-table-column>
              <el-table-column :label="lt('操作', '操作', 'Actions')" width="180" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click.stop="openAssetEdit(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
                  <el-button link type="danger" @click.stop="handleAssetDelete(row)">{{ lt('删除', '刪除', 'Delete') }}</el-button>
                </template>
              </el-table-column>
            </el-table>

            <el-empty v-else-if="!assetsLoading" :description="lt('暂无素材，先补齐截图、封面或分享图。', '暫無素材，先補齊截圖、封面或分享圖。', 'No assets yet. Add screenshots, covers, or share creatives first.')" />
          </el-tab-pane>

          <el-tab-pane name="ops" :label="lt('运营资料', '營運資料', 'Ops Profile')">
            <div class="section-head">
              <div>
                <div class="section-title">{{ lt('运营资料与图文资产', '營運資料與圖文資產', 'Operational Profile & Story Assets') }}</div>
                <div class="section-tip">{{ lt('补齐发行文案、受众说明、支持入口和投放使用的核心信息。', '補齊發行文案、受眾說明、支援入口和投放使用的核心資訊。', 'Complete launch copy, audience notes, support entry points, and campaign-facing metadata.') }}</div>
              </div>
              <el-button @click="loadOpsProfile">{{ lt('刷新资料', '刷新資料', 'Refresh') }}</el-button>
            </div>

            <el-alert v-if="opsError" class="page-error" type="error" :closable="false" :title="opsError" />

            <div v-loading="opsLoading">
              <el-form :model="opsForm" label-position="top" class="drawer-form">
                <el-row :gutter="16">
                  <el-col :span="12">
                    <el-form-item :label="lt('工作室名称', '工作室名稱', 'Studio Name')">
                      <el-input v-model="opsForm.studioName" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item :label="lt('玩家规模文案', '玩家規模文案', 'Player Count Copy')">
                      <el-input v-model="opsForm.playerCountText" />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-form-item :label="lt('运营状态', '營運狀態', 'Operations Status')">
                  <el-select v-model="opsForm.operationsStatus">
                    <el-option value="DRAFT" :label="lt('草稿', '草稿', 'Draft')" />
                    <el-option value="READY" :label="lt('就绪', '就緒', 'Ready')" />
                    <el-option value="PUBLISHED" :label="lt('已发布', '已發佈', 'Published')" />
                    <el-option value="ARCHIVED" :label="lt('归档', '歸檔', 'Archived')" />
                  </el-select>
                </el-form-item>

                <el-form-item :label="lt('营销标题', '行銷標題', 'Marketing Tagline')">
                  <el-input v-model="opsForm.marketingTagline" />
                </el-form-item>
                <el-form-item :label="lt('营销摘要', '行銷摘要', 'Marketing Summary')">
                  <el-input v-model="opsForm.marketingSummary" type="textarea" :rows="3" />
                </el-form-item>
                <el-form-item :label="lt('亮点卖点（每行一个）', '亮點賣點（每行一個）', 'Feature Highlights (one per line)')">
                  <el-input v-model="featureHighlightsText" type="textarea" :rows="5" />
                </el-form-item>

                <el-row :gutter="16">
                  <el-col :span="12">
                    <el-form-item :label="lt('目标受众', '目標受眾', 'Target Audience')">
                      <el-input v-model="opsForm.targetAudience" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item :label="lt('支持邮箱', '支援郵箱', 'Support Email')">
                      <el-input v-model="opsForm.supportEmail" />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="16">
                  <el-col :span="12">
                    <el-form-item :label="lt('支持链接', '支援連結', 'Support URL')">
                      <el-input v-model="opsForm.supportUrl" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item :label="lt('社区链接', '社群連結', 'Community URL')">
                      <el-input v-model="opsForm.communityUrl" />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-form-item :label="lt('合规说明', '合規說明', 'Compliance Note')">
                  <el-input v-model="opsForm.complianceNote" type="textarea" :rows="3" />
                </el-form-item>

                <el-row :gutter="16">
                  <el-col :span="12">
                    <el-form-item :label="lt('分享标题', '分享標題', 'Share Title')">
                      <el-input v-model="opsForm.shareTitle" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item :label="lt('分享副标题', '分享副標題', 'Share Subtitle')">
                      <el-input v-model="opsForm.shareSubtitle" />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="16">
                  <el-col :span="12">
                    <el-form-item :label="lt('分享图 URL', '分享圖 URL', 'Share Image URL')">
                      <el-input v-model="opsForm.shareImageUrl" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item :label="lt('运行时 Banner URL', '執行時 Banner URL', 'Runtime Banner URL')">
                      <el-input v-model="opsForm.runtimeBannerUrl" />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="16">
                  <el-col :span="12">
                    <el-form-item :label="lt('运行时 Logo URL', '執行時 Logo URL', 'Runtime Logo URL')">
                      <el-input v-model="opsForm.runtimeLogoUrl" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item :label="lt('发现封面 URL', '發現封面 URL', 'Discover Cover URL')">
                      <el-input v-model="opsForm.discoverCardCoverUrl" />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-form-item :label="lt('发现 Logo URL', '發現 Logo URL', 'Discover Logo URL')">
                  <el-input v-model="opsForm.discoverCardLogoUrl" />
                </el-form-item>

                <el-form-item>
                  <el-button type="primary" :loading="opsSaving" @click="handleSaveOpsProfile">{{ lt('保存运营资料', '儲存營運資料', 'Save Ops Profile') }}</el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>

    <el-dialog v-model="assetDialogVisible" :title="editingAssetId ? lt('编辑素材', '編輯素材', 'Edit Asset') : lt('新增素材', '新增素材', 'Add Asset')" width="640px">
      <el-form :model="assetForm" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="lt('分组', '分組', 'Group')">
              <el-select v-model="assetForm.assetGroup">
                <el-option value="LIBRARY" :label="lt('素材库', '素材庫', 'Library')" />
                <el-option value="OPS" :label="lt('运营资产', '營運資產', 'Ops assets')" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="lt('角色', '角色', 'Role')">
              <el-select v-model="assetForm.assetRole">
                <el-option value="SCREENSHOT" :label="lt('截图', '截圖', 'Screenshot')" />
                <el-option value="BANNER" :label="lt('Banner', 'Banner', 'Banner')" />
                <el-option value="LOGO" :label="lt('Logo', 'Logo', 'Logo')" />
                <el-option value="ICON" :label="lt('图标', '圖示', 'Icon')" />
                <el-option value="POSTER" :label="lt('海报', '海報', 'Poster')" />
                <el-option value="COVER" :label="lt('封面', '封面', 'Cover')" />
                <el-option value="SHARE" :label="lt('分享图', '分享圖', 'Share')" />
                <el-option value="DETAIL" :label="lt('详情图', '詳情圖', 'Detail')" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="lt('媒体类型', '媒體類型', 'Media Type')">
              <el-select v-model="assetForm.mediaType">
                <el-option value="IMAGE" label="IMAGE" />
                <el-option value="VIDEO" label="VIDEO" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="lt('资产状态', '資產狀態', 'Asset Status')">
              <el-select v-model="assetForm.assetStatus">
                <el-option value="DRAFT" :label="lt('草稿', '草稿', 'Draft')" />
                <el-option value="ACTIVE" :label="lt('生效', '生效', 'Active')" />
                <el-option value="ARCHIVED" :label="lt('归档', '歸檔', 'Archived')" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="lt('名称', '名稱', 'Title')">
          <el-input v-model="assetForm.title" />
        </el-form-item>
        <el-form-item :label="lt('素材 URL', '素材 URL', 'Asset URL')">
          <el-input v-model="assetForm.url" />
        </el-form-item>
        <el-form-item :label="lt('说明', '說明', 'Description')">
          <el-input v-model="assetForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="lt('跳转文案', '跳轉文案', 'Action Title')">
              <el-input v-model="assetForm.actionTitle" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="lt('跳转链接', '跳轉連結', 'Action URL')">
              <el-input v-model="assetForm.actionUrl" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="lt('宽度', '寬度', 'Width')">
              <el-input-number v-model="assetForm.width" :min="0" :step="10" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="lt('高度', '高度', 'Height')">
              <el-input-number v-model="assetForm.height" :min="0" :step="10" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="lt('排序', '排序', 'Sort Order')">
              <el-input-number v-model="assetForm.sortOrder" :min="0" :max="999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="lt('语言', '語言', 'Locale')">
              <el-input v-model="assetForm.locale" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="lt('主素材', '主素材', 'Primary')">
              <el-switch v-model="assetForm.primary" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="assetDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="assetSaving" @click="handleSaveAsset">{{ lt('保存素材', '儲存素材', 'Save Asset') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteDeveloperGameAsset,
  getDeveloperGameAssets,
  getDeveloperGameOpsProfile,
  getDeveloperGames,
  getGameCategories,
  getGameVersions,
  updateDeveloperGameOpsProfile,
  updateGameMetadata,
  upsertDeveloperGameAsset
} from '../api'
import { useI18nLite } from '../i18n'
import { useUserStore } from '../stores/user'
import { formatDate, getGameStatusMeta } from '../utils/portal'

const { lt } = useI18nLite()
const userStore = useUserStore()
const loading = ref(false)
const listError = ref('')
const versionsLoading = ref(false)
const saving = ref(false)
const opsLoading = ref(false)
const opsSaving = ref(false)
const opsError = ref('')
const assetsLoading = ref(false)
const assetSaving = ref(false)
const assetError = ref('')
const keyword = ref('')
const games = ref([])
const categories = ref([])
const versions = ref([])
const assets = ref([])
const drawerVisible = ref(false)
const activeGame = ref(null)
const activeTab = ref('basic')
const assetGroupFilter = ref('')
const assetDialogVisible = ref(false)
const editingAssetId = ref(null)

const editForm = reactive({
  name: '',
  category: '',
  version: '',
  iconUrl: '',
  description: '',
  tags: []
})

const opsForm = reactive({
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
  targetAudience: '',
  supportEmail: '',
  supportUrl: '',
  communityUrl: '',
  complianceNote: '',
  operationsStatus: 'DRAFT'
})

const assetForm = reactive({
  assetId: null,
  versionId: null,
  assetGroup: 'LIBRARY',
  assetRole: 'SCREENSHOT',
  mediaType: 'IMAGE',
  assetStatus: 'DRAFT',
  title: '',
  description: '',
  url: '',
  actionTitle: '',
  actionUrl: '',
  width: null,
  height: null,
  sizeBytes: null,
  locale: 'zh-CN',
  sortOrder: 0,
  primary: false
})

const featureHighlightsText = ref('')

const filteredGames = computed(() => {
  const key = keyword.value.toLowerCase()
  if (!key) return games.value
  return games.value.filter((item) =>
    (item.name || '').toLowerCase().includes(key) ||
    (item.appId || '').toLowerCase().includes(key)
  )
})

const loadGames = async () => {
  loading.value = true
  listError.value = ''
  try {
    const developerId = userStore.user?.id
    if (!developerId) return
    const [gameRes, categoryRes] = await Promise.all([
      getDeveloperGames(developerId),
      getGameCategories()
    ])
    games.value = gameRes.data || []
    categories.value = (categoryRes.data || []).filter((item) => item !== 'all')
  } catch (error) {
    listError.value = error.message || lt('加载游戏列表失败', '載入遊戲列表失敗', 'Failed to load games')
  } finally {
    loading.value = false
  }
}

const loadVersions = async (gameId) => {
  versionsLoading.value = true
  try {
    const res = await getGameVersions(gameId)
    versions.value = (res.data || []).slice().sort((a, b) => new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt))
  } catch (error) {
    ElMessage.error(error.message || lt('加载版本失败', '載入版本失敗', 'Failed to load versions'))
  } finally {
    versionsLoading.value = false
  }
}

const loadAssets = async () => {
  if (!activeGame.value) return
  assetsLoading.value = true
  assetError.value = ''
  try {
    const res = await getDeveloperGameAssets(userStore.user?.id, activeGame.value.id, assetGroupFilter.value || undefined)
    assets.value = res.data || []
  } catch (error) {
    assetError.value = error.message || lt('加载素材库失败', '載入素材庫失敗', 'Failed to load asset library')
  } finally {
    assetsLoading.value = false
  }
}

const loadOpsProfile = async () => {
  if (!activeGame.value) return
  opsLoading.value = true
  opsError.value = ''
  try {
    const res = await getDeveloperGameOpsProfile(userStore.user?.id, activeGame.value.id)
    const data = res.data || {}
    Object.assign(opsForm, {
      studioName: data.studioName || '',
      playerCountText: data.playerCountText || '',
      runtimeBannerUrl: data.runtimeBannerUrl || '',
      runtimeLogoUrl: data.runtimeLogoUrl || '',
      shareTitle: data.shareTitle || '',
      shareSubtitle: data.shareSubtitle || '',
      shareImageUrl: data.shareImageUrl || '',
      discoverCardCoverUrl: data.discoverCardCoverUrl || '',
      discoverCardLogoUrl: data.discoverCardLogoUrl || '',
      marketingTagline: data.marketingTagline || '',
      marketingSummary: data.marketingSummary || '',
      targetAudience: data.targetAudience || '',
      supportEmail: data.supportEmail || '',
      supportUrl: data.supportUrl || '',
      communityUrl: data.communityUrl || '',
      complianceNote: data.complianceNote || '',
      operationsStatus: data.operationsStatus || 'DRAFT'
    })
    featureHighlightsText.value = (data.featureHighlights || []).join('\n')
  } catch (error) {
    opsError.value = error.message || lt('加载运营资料失败', '載入營運資料失敗', 'Failed to load operations profile')
  } finally {
    opsLoading.value = false
  }
}

const openEditor = async (row) => {
  activeGame.value = row
  activeTab.value = 'basic'
  assetGroupFilter.value = ''
  Object.assign(editForm, {
    name: row.name || '',
    category: row.category || '',
    version: row.version || '',
    iconUrl: row.iconUrl || '',
    description: row.description || '',
    tags: parseTags(row.tagsJson)
  })
  drawerVisible.value = true
  await Promise.all([
    loadVersions(row.id),
    loadAssets(),
    loadOpsProfile()
  ])
}

const handleSave = async () => {
  if (!activeGame.value) return
  try {
    saving.value = true
    await updateGameMetadata(activeGame.value.id, editForm)
    ElMessage.success(lt('游戏主资料已更新', '遊戲主資料已更新', 'Metadata updated'))
    await loadGames()
  } catch (error) {
    ElMessage.error(error.message || lt('保存失败', '儲存失敗', 'Save failed'))
  } finally {
    saving.value = false
  }
}

const handleSaveOpsProfile = async () => {
  if (!activeGame.value) return
  try {
    opsSaving.value = true
    await updateDeveloperGameOpsProfile(userStore.user?.id, activeGame.value.id, {
      ...opsForm,
      featureHighlights: featureHighlightsText.value.split('\n').map((item) => item.trim()).filter(Boolean)
    })
    ElMessage.success(lt('运营资料已更新', '營運資料已更新', 'Operations profile updated'))
    await loadOpsProfile()
  } catch (error) {
    ElMessage.error(error.message || lt('保存运营资料失败', '儲存營運資料失敗', 'Failed to save operations profile'))
  } finally {
    opsSaving.value = false
  }
}

const resetAssetForm = () => {
  editingAssetId.value = null
  Object.assign(assetForm, {
    assetId: null,
    versionId: null,
    assetGroup: assetGroupFilter.value || 'LIBRARY',
    assetRole: 'SCREENSHOT',
    mediaType: 'IMAGE',
    assetStatus: 'DRAFT',
    title: '',
    description: '',
    url: '',
    actionTitle: '',
    actionUrl: '',
    width: null,
    height: null,
    sizeBytes: null,
    locale: 'zh-CN',
    sortOrder: 0,
    primary: false
  })
}

const openAssetCreate = () => {
  resetAssetForm()
  assetDialogVisible.value = true
}

const openAssetEdit = (row) => {
  editingAssetId.value = row.id
  Object.assign(assetForm, {
    assetId: row.id,
    versionId: row.versionId,
    assetGroup: row.assetGroup || 'LIBRARY',
    assetRole: row.assetRole || 'SCREENSHOT',
    mediaType: row.mediaType || 'IMAGE',
    assetStatus: row.assetStatus || 'DRAFT',
    title: row.title || '',
    description: row.description || '',
    url: row.url || '',
    actionTitle: row.actionTitle || '',
    actionUrl: row.actionUrl || '',
    width: row.width,
    height: row.height,
    sizeBytes: row.sizeBytes,
    locale: row.locale || 'zh-CN',
    sortOrder: row.sortOrder || 0,
    primary: !!row.primary
  })
  assetDialogVisible.value = true
}

const handleSaveAsset = async () => {
  if (!activeGame.value) return
  try {
    assetSaving.value = true
    await upsertDeveloperGameAsset(userStore.user?.id, activeGame.value.id, assetForm)
    ElMessage.success(lt('素材已保存', '素材已儲存', 'Asset saved'))
    assetDialogVisible.value = false
    await loadAssets()
  } catch (error) {
    ElMessage.error(error.message || lt('保存素材失败', '儲存素材失敗', 'Failed to save asset'))
  } finally {
    assetSaving.value = false
  }
}

const handleAssetDelete = async (row) => {
  if (!activeGame.value) return
  try {
    const { value } = await ElMessageBox.prompt(
      lt('删除素材需要二次确认，请输入 DELETE。', '刪除素材需要二次確認，請輸入 DELETE。', 'Deleting an asset requires confirmation. Type DELETE.'),
      lt('确认删除', '確認刪除', 'Confirm delete'),
      {
        inputValue: '',
        inputPattern: /^DELETE$/i,
        inputErrorMessage: 'DELETE',
        confirmButtonText: lt('删除', '刪除', 'Delete'),
        cancelButtonText: lt('取消', '取消', 'Cancel'),
        type: 'warning'
      }
    )
    await deleteDeveloperGameAsset(userStore.user?.id, activeGame.value.id, row.id, value)
    ElMessage.success(lt('素材已删除', '素材已刪除', 'Asset deleted'))
    await loadAssets()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error.message || lt('删除素材失败', '刪除素材失敗', 'Failed to delete asset'))
  }
}

function getStatusMeta(status) {
  return getGameStatusMeta(status)
}

function getStatusText(status) {
  const dict = {
    APPROVED: lt('已通过', '已通過', 'Approved'),
    PENDING: lt('待审核', '待審核', 'Pending'),
    PROCESSING: lt('处理中', '處理中', 'Processing'),
    DRAFT: lt('草稿', '草稿', 'Draft'),
    REJECTED: lt('已驳回', '已駁回', 'Rejected')
  }
  return dict[status] || status || '-'
}

function getVersionMeta(status) {
  return getGameStatusMeta(status)
}

function getVersionText(status) {
  const dict = {
    DRAFT: lt('草稿', '草稿', 'Draft'),
    SUBMITTED: lt('已提审', '已提審', 'Submitted'),
    APPROVED: lt('已通过', '已通過', 'Approved'),
    REJECTED: lt('已驳回', '已駁回', 'Rejected'),
    PROCESSING: lt('处理中', '處理中', 'Processing')
  }
  return dict[status] || status || '-'
}

function getAssetGroupText(group) {
  return {
    LIBRARY: lt('素材库', '素材庫', 'Library'),
    OPS: lt('运营资产', '營運資產', 'Ops Assets')
  }[group] || group || '-'
}

function getAssetRoleText(role) {
  return {
    SCREENSHOT: lt('截图', '截圖', 'Screenshot'),
    BANNER: 'Banner',
    LOGO: 'Logo',
    ICON: lt('图标', '圖示', 'Icon'),
    POSTER: lt('海报', '海報', 'Poster'),
    COVER: lt('封面', '封面', 'Cover'),
    SHARE: lt('分享图', '分享圖', 'Share'),
    DETAIL: lt('详情图', '詳情圖', 'Detail')
  }[role] || role || '-'
}

function getAssetStatusMeta(status) {
  return {
    ACTIVE: { type: 'success' },
    DRAFT: { type: 'info' },
    ARCHIVED: { type: 'warning' }
  }[status] || { type: 'info' }
}

function getAssetStatusText(status) {
  return {
    ACTIVE: lt('生效', '生效', 'Active'),
    DRAFT: lt('草稿', '草稿', 'Draft'),
    ARCHIVED: lt('归档', '歸檔', 'Archived')
  }[status] || status || '-'
}

function getCompleteness(row) {
  let score = 0
  if (row.name) score += 20
  if (row.description && row.description.length >= 10) score += 20
  if (row.category) score += 20
  if (row.version) score += 20
  if (row.iconUrl) score += 20
  return score
}

function parseTags(tagsJson) {
  if (!tagsJson) return []
  try {
    const parsed = JSON.parse(tagsJson)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

onMounted(loadGames)
</script>

<style scoped>
.games-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-card {
  border-radius: 20px;
}

.panel-head,
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.panel-title,
.section-title {
  font-size: 17px;
  font-weight: 800;
  color: #101828;
}

.panel-subtitle,
.section-tip {
  margin-top: 6px;
  color: #667085;
  font-size: 13px;
}

.panel-actions,
.section-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.drawer-form {
  padding-right: 8px;
}

.version-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.page-error {
  margin-bottom: 16px;
}
</style>
