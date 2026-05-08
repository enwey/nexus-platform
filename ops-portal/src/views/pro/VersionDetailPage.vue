<template>
  <div class="pro-page">
    <el-card class="panel-card" v-loading="loading">
      <template #header>
        <div class="head-row">
          <div>
            <div class="breadcrumb-line">
              <el-button link @click="goBack">{{ lt('返回版本列表', '返回版本列表', 'Back to Version List') }}</el-button>
              <span>/</span>
              <span>{{ version?.versionName || lt('版本详情', '版本詳情', 'Version Detail') }}</span>
            </div>
            <div class="panel-title">{{ version?.versionName || '-' }}</div>
            <div class="panel-subtitle">{{ game?.name || '-' }} / {{ game?.appId || '-' }}</div>
          </div>
          <div class="actions">
            <el-button @click="openGame">{{ lt('查看游戏详情', '查看遊戲詳情', 'Open Game Detail') }}</el-button>
            <el-button v-if="game?.status === 'PENDING'" type="primary" @click="openReviewDetail">{{ lt('进入审核详情', '進入審核詳情', 'Open Review Detail') }}</el-button>
            <el-button :loading="loading" @click="loadDetail">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <el-empty v-if="!version" :description="lt('未找到对应版本', '未找到對應版本', 'Version not found')" />
      <template v-else>
        <div class="summary-grid">
          <div class="summary-item">
            <span>{{ lt('版本状态', '版本狀態', 'Version Status') }}</span>
            <strong><el-tag :type="getVersionStatusType(version.status)">{{ getVersionStatusText(version.status) }}</el-tag></strong>
          </div>
          <div class="summary-item">
            <span>{{ lt('Manifest', 'Manifest', 'Manifest') }}</span>
            <strong>
              <el-tag :type="version.hostedManifestValid ? 'success' : version.hostedManifestValid === false ? 'danger' : 'info'">
                {{ version.hostedManifestValid === true ? lt('通过', '通過', 'Passed') : version.hostedManifestValid === false ? lt('失败', '失敗', 'Failed') : lt('未知', '未知', 'Unknown') }}
              </el-tag>
            </strong>
          </div>
          <div class="summary-item">
            <span>{{ lt('强更', '強更', 'Force Update') }}</span>
            <strong>{{ version.forceUpdate ? lt('是', '是', 'Yes') : lt('否', '否', 'No') }}</strong>
          </div>
          <div class="summary-item">
            <span>{{ lt('更新时间', '更新時間', 'Updated') }}</span>
            <strong>{{ formatDate(version.updatedAt || version.createdAt || version.submittedAt) }}</strong>
          </div>
        </div>

        <el-descriptions :column="2" border class="section">
          <el-descriptions-item :label="lt('游戏名称', '遊戲名稱', 'Game Name')">{{ game?.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="AppID">{{ game?.appId || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('版本号', '版本號', 'Version')">{{ version.versionName || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('版本状态', '版本狀態', 'Status')">{{ getVersionStatusText(version.status) }}</el-descriptions-item>
          <el-descriptions-item :label="lt('提交说明', '提交說明', 'Submit Note')" :span="2">{{ version.submitNote || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('审核原因', '審核原因', 'Audit Reason')" :span="2">{{ version.auditReason || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('Manifest 摘要', 'Manifest 摘要', 'Manifest Summary')" :span="2">{{ version.hostedManifestSummary || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-card class="section" shadow="never">
          <template #header>
            <div class="section-head">
              <span>{{ lt('同游戏版本记录', '同遊戲版本記錄', 'Sibling Versions') }}</span>
            </div>
          </template>
          <el-table :data="siblingVersions" :empty-text="lt('暂无同游戏版本', '暫無同遊戲版本', 'No sibling versions')">
            <el-table-column prop="versionName" :label="lt('版本号', '版本號', 'Version')" min-width="120" />
            <el-table-column :label="lt('状态', '狀態', 'Status')" width="140">
              <template #default="{ row }">
                <el-tag :type="getVersionStatusType(row.status)">{{ getVersionStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="lt('更新时间', '更新時間', 'Updated')" min-width="170">
              <template #default="{ row }">{{ formatDate(row.updatedAt || row.createdAt || row.submittedAt) }}</template>
            </el-table-column>
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="130">
              <template #default="{ row }">
                <el-button v-if="String(row.id) !== String(version.id)" link type="primary" @click="switchVersion(row)">{{ lt('查看', '查看', 'View') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getGameVersions, getOpsGames } from '../../api'
import { useI18nLite } from '../../i18n'
import { formatDate } from './entityPageShared'

const props = defineProps({
  versionId: {
    type: [String, Number],
    required: true
  }
})

const route = useRoute()
const router = useRouter()
const { lt } = useI18nLite()

const loading = ref(false)
const game = ref(null)
const version = ref(null)
const siblingVersions = ref([])

const loadDetail = async () => {
  loading.value = true
  try {
    const explicitGameId = route.query.gameId ? String(route.query.gameId) : ''
    const gamesRes = await getOpsGames()
    const games = gamesRes.data || []

    if (explicitGameId) {
      game.value = games.find((item) => String(item.id) === explicitGameId) || null
    }

    if (!game.value) {
      for (const item of games) {
        const res = await getGameVersions(item.id)
        const match = (res.data || []).find((entry) => String(entry.id) === String(props.versionId))
        if (match) {
          game.value = item
          siblingVersions.value = res.data || []
          version.value = match
          break
        }
      }
      return
    }

    const versionsRes = await getGameVersions(game.value.id)
    siblingVersions.value = Array.isArray(versionsRes.data) ? versionsRes.data : []
    version.value = siblingVersions.value.find((item) => String(item.id) === String(props.versionId)) || null
  } catch (error) {
    ElMessage.error(error.message || lt('加载版本详情失败', '載入版本詳情失敗', 'Failed to load version detail'))
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  if (route.query.from) {
    router.push(String(route.query.from))
    return
  }
  router.push({ name: 'OpsVersionList', query: game.value?.id ? { gameId: String(game.value.id) } : undefined })
}

const openGame = () => {
  if (game.value?.id) {
    router.push({ name: 'OpsGameDetail', params: { gameId: game.value.id } })
  }
}

const openReviewDetail = () => {
  if (version.value?.id) {
    router.push({ name: 'OpsVersionReviewDetail', params: { reviewId: version.value.id } })
  }
}

const switchVersion = (row) => {
  router.push({ name: 'OpsVersionDetail', params: { versionId: row.id }, query: game.value?.id ? { gameId: String(game.value.id) } : undefined })
}

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

onMounted(loadDetail)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 18px; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.breadcrumb-line { display: flex; align-items: center; gap: 8px; color: #667085; font-size: 13px; margin-bottom: 8px; }
.panel-title { font-size: 20px; font-weight: 800; color: #101828; }
.panel-subtitle { margin-top: 6px; color: #667085; font-size: 13px; }
.actions { display: flex; gap: 8px; flex-wrap: wrap; }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.summary-item { padding: 12px 14px; border-radius: 14px; background: #f8fafc; display: flex; flex-direction: column; gap: 6px; color: #475467; font-size: 13px; }
.summary-item strong { font-size: 16px; color: #111827; }
.section { margin-top: 16px; }
.section-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
@media (max-width: 920px) {
  .head-row { flex-direction: column; }
  .summary-grid { grid-template-columns: 1fr 1fr; }
}
</style>
