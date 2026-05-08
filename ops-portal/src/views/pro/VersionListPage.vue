<template>
  <div class="pro-page">
    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ lt('版本列表', '版本列表', 'Version List') }}</div>
            <div class="panel-subtitle">{{ lt('统一查看游戏版本，从列表进入详情页查看包体、Manifest 和审核状态。', '統一查看遊戲版本，從列表進入詳情頁查看包體、Manifest 和審核狀態。', 'Inspect game versions and move into detail pages for package, manifest, and review status.') }}</div>
          </div>
          <div class="actions">
            <el-input v-model.trim="keyword" clearable style="width: 260px" :placeholder="lt('搜索游戏 / AppID / 版本号', '搜尋遊戲 / AppID / 版本號', 'Search game / AppID / version')" />
            <el-select v-model="statusFilter" clearable style="width: 160px">
              <el-option :label="lt('全部状态', '全部狀態', 'All Statuses')" value="" />
              <el-option v-for="item in versionStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="manifestFilter" clearable style="width: 170px">
              <el-option :label="lt('全部 Manifest 状态', '全部 Manifest 狀態', 'All Manifest States')" value="" />
              <el-option :label="lt('校验通过', '校驗通過', 'Manifest Passed')" value="passed" />
              <el-option :label="lt('校验失败', '校驗失敗', 'Manifest Failed')" value="failed" />
            </el-select>
            <el-button @click="router.push('/pro/game-version/games/list')">{{ lt('返回游戏列表', '返回遊戲列表', 'Back to Game List') }}</el-button>
            <el-button :loading="loading" @click="loadVersions">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredRows" v-loading="loading">
        <el-table-column prop="gameName" :label="lt('游戏名称', '遊戲名稱', 'Game Name')" min-width="180" />
        <el-table-column prop="appId" label="AppID" min-width="170" />
        <el-table-column prop="versionName" :label="lt('版本号', '版本號', 'Version')" width="120" />
        <el-table-column :label="lt('版本状态', '版本狀態', 'Version Status')" width="140">
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
        <el-table-column :label="lt('强更', '強更', 'Force Update')" width="100">
          <template #default="{ row }">{{ row.forceUpdate ? lt('是', '是', 'Yes') : lt('否', '否', 'No') }}</template>
        </el-table-column>
        <el-table-column :label="lt('审核说明', '審核說明', 'Audit Reason')" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.auditReason || row.submitNote || '-' }}</template>
        </el-table-column>
        <el-table-column :label="lt('更新时间', '更新時間', 'Updated')" min-width="170">
          <template #default="{ row }">{{ formatDate(row.updatedAt || row.createdAt || row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ lt('查看详情', '查看詳情', 'View Detail') }}</el-button>
            <el-button link @click="openGame(row)">{{ lt('查看游戏', '查看遊戲', 'Game') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getGameVersions, getOpsGames } from '../../api'
import { useI18nLite } from '../../i18n'
import { formatDate } from './entityPageShared'

const route = useRoute()
const router = useRouter()
const { lt } = useI18nLite()

const loading = ref(false)
const rows = ref([])
const keyword = ref('')
const statusFilter = ref('')
const manifestFilter = ref('')

const versionStatusOptions = computed(() => ([
  { value: 'DRAFT', label: lt('草稿', '草稿', 'Draft') },
  { value: 'PROCESSING', label: lt('处理中', '處理中', 'Processing') },
  { value: 'SUBMITTED', label: lt('待审核', '待審核', 'Submitted') },
  { value: 'APPROVED', label: lt('已通过', '已通過', 'Approved') },
  { value: 'REJECTED', label: lt('已驳回', '已駁回', 'Rejected') },
  { value: 'PUBLISHED', label: lt('已发布', '已發布', 'Published') }
]))

const filteredRows = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return rows.value.filter((row) => {
    const matchesKeyword = !query
      || (row.gameName || '').toLowerCase().includes(query)
      || (row.appId || '').toLowerCase().includes(query)
      || (row.versionName || '').toLowerCase().includes(query)
    const matchesStatus = !statusFilter.value || row.status === statusFilter.value
    const matchesManifest = !manifestFilter.value
      || (manifestFilter.value === 'passed' && row.hostedManifestValid === true)
      || (manifestFilter.value === 'failed' && row.hostedManifestValid === false)
    return matchesKeyword && matchesStatus && matchesManifest
  })
})

const loadVersions = async () => {
  loading.value = true
  try {
    const gamesRes = await getOpsGames()
    const scopedGameId = route.query.gameId ? String(route.query.gameId) : ''
    const games = (gamesRes.data || []).filter((game) => !scopedGameId || String(game.id) === scopedGameId)
    const versionGroups = await Promise.all(
      games.map(async (game) => {
        const res = await getGameVersions(game.id)
        return (res.data || []).map((version) => ({
          ...version,
          gameId: game.id,
          gameName: game.name,
          appId: game.appId,
          gameStatus: game.status
        }))
      })
    )
    rows.value = versionGroups.flat().sort((left, right) => {
      const leftTime = new Date(left.updatedAt || left.createdAt || left.submittedAt || 0).getTime()
      const rightTime = new Date(right.updatedAt || right.createdAt || right.submittedAt || 0).getTime()
      return rightTime - leftTime
    })
  } catch (error) {
    ElMessage.error(error.message || lt('加载版本列表失败', '載入版本列表失敗', 'Failed to load version list'))
  } finally {
    loading.value = false
  }
}

const openDetail = (row) => {
  router.push({ name: 'OpsVersionDetail', params: { versionId: row.id }, query: { gameId: String(row.gameId) } })
}

const openGame = (row) => {
  router.push({ name: 'OpsGameDetail', params: { gameId: row.gameId } })
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

onMounted(loadVersions)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 18px; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.panel-title { font-size: 17px; font-weight: 800; color: #101828; }
.panel-subtitle { margin-top: 6px; color: #667085; font-size: 13px; }
.actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
@media (max-width: 920px) { .head-row { flex-direction: column; } }
</style>
