<template>
  <div class="insights-page">
    <el-alert
      v-if="pageError"
      :title="pageError"
      type="error"
      :closable="false"
      show-icon
    />

    <el-skeleton v-if="pageLoading" :rows="8" animated />

    <template v-else>
    <el-row :gutter="16">
      <el-col :xs="24" :md="12" :xl="6" v-for="item in metricCards" :key="item.key">
        <el-card class="metric-card">
          <div class="metric-label">{{ item.label }}</div>
          <div class="metric-value">{{ item.value }}</div>
          <div class="metric-tip">{{ item.tip }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>{{ lt('经营基础指标（近 7 天）', '經營基礎指標（近 7 天）', 'Operations Overview (7 days)') }}</template>
          <el-empty
            v-if="!operationsOverview.hasData"
            :description="lt('暂时还没有安装、启动与会话数据，接入运行时上报后会在这里持续积累。', '暫時還沒有安裝、啟動與會話資料，接入運行時上報後會在這裡持續累積。', 'No install, launch, or session data yet. This board will populate after runtime telemetry is connected.')"
          />
          <el-row v-else :gutter="12">
            <el-col :xs="12" :md="8" v-for="item in operationsCards" :key="item.key">
              <div class="ops-mini-card">
                <div class="ops-mini-label">{{ item.label }}</div>
                <div class="ops-mini-value">{{ item.value }}</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>{{ lt('安装 / 启动异常', '安裝 / 啟動異常', 'Install / Launch Issues') }}</template>
          <el-empty
            v-if="!runtimeIssues.length"
            :description="lt('当前没有未解决的运行时异常。', '目前沒有未解決的運行時異常。', 'No unresolved runtime issues at the moment.')"
          />
          <el-table v-else :data="runtimeIssues">
            <el-table-column prop="gameName" :label="lt('游戏', '遊戲', 'Game')" min-width="160" />
            <el-table-column :label="lt('类型', '類型', 'Type')" width="110">
              <template #default="{ row }">{{ row.issueType === 'INSTALL' ? lt('安装异常', '安裝異常', 'Install') : lt('启动异常', '啟動異常', 'Launch') }}</template>
            </el-table-column>
            <el-table-column :label="lt('严重级别', '嚴重級別', 'Severity')" width="110">
              <template #default="{ row }"><el-tag :type="severityTag(row.severity)">{{ row.severity }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="issueMessage" :label="lt('异常说明', '異常說明', 'Issue')" min-width="220" show-overflow-tooltip />
            <el-table-column prop="impactedUsers" :label="lt('影响用户', '影響用戶', 'Users')" width="110" />
            <el-table-column :label="lt('最近出现', '最近出現', 'Latest')" min-width="160">
              <template #default="{ row }">{{ formatDate(row.lastOccurredAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :xs="24">
        <el-card class="panel-card">
          <template #header>{{ lt('经营趋势（近 7 天）', '經營趨勢（近 7 天）', 'Operations Trend (7 days)') }}</template>
          <el-empty
            v-if="!metricTrendRows.length || !operationsOverview.hasData"
            :description="lt('暂时还没有趋势数据。', '暫時還沒有趨勢資料。', 'No trend data yet.')"
          />
          <el-table v-else :data="metricTrendRows">
            <el-table-column prop="metricDate" :label="lt('日期', '日期', 'Date')" min-width="120" />
            <el-table-column prop="installs" :label="lt('安装', '安裝', 'Installs')" width="100" />
            <el-table-column prop="launches" :label="lt('启动', '啟動', 'Launches')" width="100" />
            <el-table-column prop="activeUsers" :label="lt('活跃用户', '活躍用戶', 'Active Users')" width="120" />
            <el-table-column prop="installFailures" :label="lt('安装失败', '安裝失敗', 'Install Failures')" width="120" />
            <el-table-column prop="launchFailures" :label="lt('启动失败', '啟動失敗', 'Launch Failures')" width="120" />
            <el-table-column prop="avgSessionMinutes" :label="lt('平均时长(分)', '平均時長(分)', 'Avg Session (min)')" width="140" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :xl="14">
        <el-card class="panel-card">
          <template #header>{{ lt('发布健康看板', '發布健康看板', 'Release Health Board') }}</template>
          <el-table :data="releaseRows" :empty-text="lt('暂无发布数据', '暫無發布資料', 'No release data')">
            <el-table-column prop="name" :label="lt('游戏', '遊戲', 'Game')" min-width="180" />
            <el-table-column prop="latestVersion" :label="lt('最新版本', '最新版本', 'Latest Version')" width="120" />
            <el-table-column :label="lt('前端状态', '前端狀態', 'Frontend State')" width="120">
              <template #default="{ row }">
                <el-tag :type="row.frontendMeta.type">{{ row.frontendText }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="lt('版本状态', '版本狀態', 'Version Status')" width="120">
              <template #default="{ row }">
                <el-tag :type="row.statusMeta.type">{{ row.statusText }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="lt('提审可用', '提審可用', 'Submittable')" width="110">
              <template #default="{ row }">{{ row.canSubmit ? lt('可提交', '可提交', 'Ready') : lt('不可提交', '不可提交', 'Blocked') }}</template>
            </el-table-column>
            <el-table-column prop="blockingReason" :label="lt('阻塞原因', '阻塞原因', 'Blocking Reason')" min-width="220" show-overflow-tooltip />
            <el-table-column prop="lastUpdated" :label="lt('最近变更', '最近變更', 'Last Updated')" min-width="170" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="10">
        <el-card class="panel-card">
          <template #header>{{ lt('经营建议', '經營建議', 'Recommendations') }}</template>
          <div class="recommend-list">
            <div class="recommend-item" v-for="item in recommendations" :key="item.title">
              <div class="recommend-title">{{ item.title }}</div>
              <div class="recommend-desc">{{ item.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>{{ lt('分类分布', '分類分布', 'Category Mix') }}</template>
          <el-table :data="categoryRows" :empty-text="lt('暂无分类数据', '暫無分類資料', 'No category data')">
            <el-table-column prop="category" :label="lt('分类', '分類', 'Category')" min-width="160" />
            <el-table-column prop="count" :label="lt('游戏数', '遊戲數', 'Games')" width="100" />
            <el-table-column prop="approved" :label="lt('通过数', '通過數', 'Approved')" width="100" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>{{ lt('版本质量分布', '版本品質分布', 'Version Quality') }}</template>
          <el-table :data="qualityRows" :empty-text="lt('暂无版本数据', '暫無版本資料', 'No version data')">
            <el-table-column prop="label" :label="lt('指标', '指標', 'Metric')" min-width="180" />
            <el-table-column prop="value" :label="lt('数量', '數量', 'Count')" width="120" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>{{ lt('审核结果趋势', '審核結果趨勢', 'Review Outcome Trend') }}</template>
          <el-table :data="reviewTrendRows" :empty-text="lt('暂无审核趋势数据', '暫無審核趨勢資料', 'No review trend data')">
            <el-table-column prop="period" :label="lt('月份', '月份', 'Month')" min-width="140" />
            <el-table-column prop="submitted" :label="lt('提审', '提審', 'Submitted')" width="100" />
            <el-table-column prop="approved" :label="lt('通过', '通過', 'Approved')" width="100" />
            <el-table-column prop="rejected" :label="lt('驳回', '駁回', 'Rejected')" width="100" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>{{ lt('驳回原因分布', '駁回原因分布', 'Rejection Reason Mix') }}</template>
          <el-table :data="rejectionReasonRows" :empty-text="lt('暂无驳回数据', '暫無駁回資料', 'No rejection data')">
            <el-table-column prop="reason" :label="lt('原因', '原因', 'Reason')" min-width="220" show-overflow-tooltip />
            <el-table-column prop="count" :label="lt('次数', '次數', 'Count')" width="100" />
            <el-table-column prop="latestAt" :label="lt('最近出现', '最近出現', 'Latest Seen')" min-width="170" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getDeveloperGames, getDeveloperOperationsDashboard, getDeveloperReleaseHealth, getGameVersions } from '../api'
import { useI18nLite } from '../i18n'
import { useUserStore } from '../stores/user'
import { formatDate, getFrontendStateMeta, getGameStatusMeta } from '../utils/portal'

const { lt } = useI18nLite()
const userStore = useUserStore()
const games = ref([])
const versionsByGame = ref({})
const releaseHealth = ref({ overview: null, games: [] })
const operationsDashboard = ref({ overview: null, trend: [], games: [], runtimeIssues: [] })
const pageLoading = ref(false)
const pageError = ref('')

const loadData = async () => {
  try {
    pageLoading.value = true
    pageError.value = ''
    const developerId = userStore.user?.id
    if (!developerId) return
    const [res, healthRes, dashboardRes] = await Promise.all([
      getDeveloperGames(developerId),
      getDeveloperReleaseHealth(developerId),
      getDeveloperOperationsDashboard(developerId)
    ])
    games.value = res.data || []
    releaseHealth.value = healthRes.data || { overview: null, games: [] }
    operationsDashboard.value = dashboardRes.data || { overview: null, trend: [], games: [], runtimeIssues: [] }
    const results = await Promise.all(
      games.value.map(async (game) => {
        const versionRes = await getGameVersions(game.id)
        return [game.id, versionRes.data || []]
      })
    )
    versionsByGame.value = Object.fromEntries(results)
  } catch (error) {
    pageError.value = error.message || lt('加载数据概览失败', '載入數據概覽失敗', 'Failed to load insights')
    ElMessage.error(pageError.value)
  } finally {
    pageLoading.value = false
  }
}

const allVersions = computed(() => Object.values(versionsByGame.value).flat())
const approvedGames = computed(() => games.value.filter((item) => item.status === 'APPROVED').length)
const rejectedGames = computed(() => games.value.filter((item) => item.status === 'REJECTED').length)
const approvalRate = computed(() => {
  if (!games.value.length) return '0%'
  return `${Math.round((approvedGames.value / games.value.length) * 100)}%`
})
const latestReleaseText = computed(() => {
  const latestGame = releaseHealth.value.games.slice().sort((a, b) => new Date(b.lastUpdated) - new Date(a.lastUpdated))[0]
  return latestGame ? formatDate(latestGame.lastUpdated) : '-'
})

const metricCards = computed(() => [
  { key: 'games', label: lt('游戏总数', '遊戲總數', 'Games'), value: games.value.length, tip: lt('已创建的游戏资产总量', '已建立的遊戲資產總量', 'Total created game assets') },
  { key: 'approved', label: lt('审核通过率', '審核通過率', 'Approval Rate'), value: approvalRate.value, tip: lt('以当前游戏状态估算', '以目前遊戲狀態估算', 'Estimated from current game statuses') },
  { key: 'versions', label: lt('可提审游戏', '可提審遊戲', 'Releasable Games'), value: releaseHealth.value.overview?.releasableGames ?? 0, tip: lt('满足提审条件、可直接发起审核', '滿足提審條件、可直接發起審核', 'Games that can be submitted right now') },
  { key: 'release', label: lt('最近变更', '最近變更', 'Latest Change'), value: latestReleaseText.value, tip: lt('最近一次游戏或版本更新时间', '最近一次遊戲或版本更新時間', 'Most recent game or version update') }
])

const operationsOverview = computed(() => {
  const row = operationsDashboard.value.overview || {}
  const hasData = [row.installs7d, row.launches7d, row.activeUsers7d, row.installFailures7d, row.launchFailures7d].some((item) => Number(item || 0) > 0)
  return { ...row, hasData }
})

const operationsCards = computed(() => [
  { key: 'installs', label: lt('安装', '安裝', 'Installs'), value: operationsOverview.value.installs7d ?? 0 },
  { key: 'launches', label: lt('启动', '啟動', 'Launches'), value: operationsOverview.value.launches7d ?? 0 },
  { key: 'activeUsers', label: lt('活跃用户', '活躍用戶', 'Active Users'), value: operationsOverview.value.activeUsers7d ?? 0 },
  { key: 'installFailures', label: lt('安装失败', '安裝失敗', 'Install Failures'), value: operationsOverview.value.installFailures7d ?? 0 },
  { key: 'launchFailures', label: lt('启动失败', '啟動失敗', 'Launch Failures'), value: operationsOverview.value.launchFailures7d ?? 0 },
  { key: 'avgSession', label: lt('平均时长(分)', '平均時長(分)', 'Avg Session'), value: operationsOverview.value.avgSessionMinutes7d ?? 0 }
])

const releaseRows = computed(() => releaseHealth.value.games.map((game) => {
  const frontendMeta = getFrontendStateMeta(game.frontendState)
  return {
    id: game.id,
    name: game.gameName,
    latestVersion: game.latestVersion || '-',
    statusMeta: getGameStatusMeta(game.latestVersionStatus || game.gameStatus),
    statusText: getStatusText(game.latestVersionStatus || game.gameStatus),
    frontendMeta,
    frontendText: getFrontendStateText(game.frontendState),
    canSubmit: game.canSubmit,
    blockingReason: game.blockingReason || '-',
    lastUpdated: formatDate(game.lastUpdated)
  }
}))

const runtimeIssues = computed(() => operationsDashboard.value.runtimeIssues || [])
const metricTrendRows = computed(() => (operationsDashboard.value.trend || []).map((item) => ({
  metricDate: item.metricDate,
  installs: item.installs ?? 0,
  launches: item.launches ?? 0,
  activeUsers: item.activeUsers ?? 0,
  installFailures: item.installFailures ?? 0,
  launchFailures: item.launchFailures ?? 0,
  avgSessionMinutes: item.avgSessionMinutes ?? 0
})))

const categoryRows = computed(() => {
  const bucket = new Map()
  games.value.forEach((game) => {
    const key = game.category || lt('未分类', '未分類', 'Uncategorized')
    if (!bucket.has(key)) {
      bucket.set(key, { category: key, count: 0, approved: 0 })
    }
    const row = bucket.get(key)
    row.count += 1
    if (game.status === 'APPROVED') row.approved += 1
  })
  return Array.from(bucket.values()).sort((a, b) => b.count - a.count)
})

const qualityRows = computed(() => [
  { label: lt('已提审版本', '已提審版本', 'Submitted Versions'), value: allVersions.value.filter((item) => item.status === 'SUBMITTED').length },
  { label: lt('已通过版本', '已通過版本', 'Approved Versions'), value: allVersions.value.filter((item) => item.status === 'APPROVED').length },
  { label: lt('被驳回版本', '被駁回版本', 'Rejected Versions'), value: allVersions.value.filter((item) => item.status === 'REJECTED').length },
  { label: lt('强更版本', '強更版本', 'Forced Update Versions'), value: allVersions.value.filter((item) => item.forcedUpdate).length },
  { label: lt('被平台阻断', '被平台阻斷', 'Platform-blocked Games'), value: releaseHealth.value.overview?.blockedGames ?? 0 }
])

const reviewTrendRows = computed(() => {
  const bucket = new Map()
  allVersions.value.forEach((version) => {
    const source = version.updatedAt || version.createdAt
    const date = source ? new Date(source) : null
    const period = date && !Number.isNaN(date.getTime())
      ? `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
      : lt('未分配时间', '未分配時間', 'No Time')
    if (!bucket.has(period)) {
      bucket.set(period, { period, submitted: 0, approved: 0, rejected: 0, sortKey: source || '' })
    }
    const row = bucket.get(period)
    if (version.status === 'SUBMITTED') row.submitted += 1
    if (version.status === 'APPROVED') row.approved += 1
    if (version.status === 'REJECTED') row.rejected += 1
  })
  return Array.from(bucket.values())
    .sort((a, b) => `${b.sortKey}`.localeCompare(`${a.sortKey}`))
    .slice(0, 6)
    .map(({ sortKey, ...row }) => row)
})

const rejectionReasonRows = computed(() => {
  const bucket = new Map()
  allVersions.value
    .filter((item) => item.status === 'REJECTED')
    .forEach((version) => {
      const rawReason = (version.auditReason || '').trim()
      const reason = rawReason || lt('未填写具体原因', '未填寫具體原因', 'No detailed reason')
      if (!bucket.has(reason)) {
        bucket.set(reason, { reason, count: 0, latestAt: version.updatedAt || version.createdAt || '', latestTs: version.updatedAt || version.createdAt || '' })
      }
      const row = bucket.get(reason)
      row.count += 1
      const currentTs = version.updatedAt || version.createdAt || ''
      if (`${currentTs}` > `${row.latestTs}`) {
        row.latestTs = currentTs
        row.latestAt = currentTs
      }
    })
  return Array.from(bucket.values())
    .sort((a, b) => b.count - a.count || `${b.latestTs}`.localeCompare(`${a.latestTs}`))
    .slice(0, 8)
    .map((item) => ({
      reason: item.reason,
      count: item.count,
      latestAt: formatDate(item.latestAt)
    }))
})

const recommendations = computed(() => {
  const items = []
  if (rejectedGames.value > 0) {
    items.push({
      title: lt('优先处理驳回版本', '優先處理駁回版本', 'Resolve Rejected Releases First'),
      desc: lt(`当前有 ${rejectedGames.value} 个游戏处于驳回状态，建议优先补齐素材、说明或包体规范。`, `目前有 ${rejectedGames.value} 個遊戲處於駁回狀態，建議優先補齊素材、說明或包體規範。`, `There are ${rejectedGames.value} rejected games. Fix assets, notes, or package compliance first.`)
    })
  }
  if ((releaseHealth.value.overview?.blockedGames ?? 0) > 0) {
    items.push({
      title: lt('处理发布阻塞项', '處理發布阻塞項', 'Resolve Release Blockers'),
      desc: lt(`当前有 ${releaseHealth.value.overview?.blockedGames ?? 0} 个游戏存在阻塞原因，建议优先检查前端封控、提审占用和 manifest 校验。`, `目前有 ${releaseHealth.value.overview?.blockedGames ?? 0} 個遊戲存在阻塞原因，建議優先檢查前端封控、提審佔用和 manifest 校驗。`, `There are ${releaseHealth.value.overview?.blockedGames ?? 0} blocked games. Check frontend controls, pending submissions, and manifest validation first.`)
    })
  }
  if (rejectionReasonRows.value.length > 0) {
    items.push({
      title: lt('盯紧高频驳回原因', '盯緊高頻駁回原因', 'Track Frequent Rejection Reasons'),
      desc: lt(`当前最高频的驳回原因是“${rejectionReasonRows.value[0].reason}”，建议先把对应规范整理成内部提审模板。`, `目前最高頻的駁回原因是「${rejectionReasonRows.value[0].reason}」，建議先把對應規範整理成內部提審模板。`, `The most frequent rejection reason is "${rejectionReasonRows.value[0].reason}". Turn it into an internal submission checklist first.`)
    })
  }
  if (approvalRate.value !== '0%' && Number.parseInt(approvalRate.value, 10) < 70) {
    items.push({
      title: lt('优化提审前检查', '優化提審前檢查', 'Tighten Pre-submission Checklist'),
      desc: lt('审核通过率偏低，建议把 manifest、封面素材与版本说明纳入内部必检项。', '審核通過率偏低，建議把 manifest、封面素材與版本說明納入內部必檢項。', 'Approval rate is low. Add manifest, cover assets, and release note checks to your internal checklist.')
    })
  }
  if (!items.length) {
    items.push({
      title: lt('保持稳定发布节奏', '保持穩定發布節奏', 'Keep a Stable Release Rhythm'),
      desc: lt('当前整体质量稳定，可以继续按功能批次发布，并为关键版本补充回滚预案。', '目前整體品質穩定，可以繼續按功能批次發布，並為關鍵版本補充回滾預案。', 'Quality looks stable. Keep shipping in clear batches and prepare rollback plans for critical releases.')
    })
  }
  return items
})

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

function getFrontendStateText(state) {
  const dict = {
    LIVE: lt('线上可见', '線上可見', 'Live'),
    HIDDEN: lt('前端隐藏', '前端隱藏', 'Hidden'),
    BLOCKED: lt('平台封控', '平台封控', 'Blocked')
  }
  return dict[state] || state || '-'
}

function severityTag(severity) {
  if (severity === 'HIGH' || severity === 'CRITICAL') return 'danger'
  if (severity === 'MEDIUM') return 'warning'
  return ''
}

onMounted(loadData)
</script>

<style scoped>
.insights-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-row {
  margin-top: 0;
}

.metric-card,
.panel-card {
  border-radius: 20px;
}

.ops-mini-card {
  margin-bottom: 12px;
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(180deg, #f8fafc 0%, #eef4ff 100%);
}

.ops-mini-label {
  color: #667085;
  font-size: 12px;
}

.ops-mini-value {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 800;
  color: #101828;
}

.metric-label {
  color: #667085;
  font-size: 13px;
}

.metric-value {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 800;
  color: #101828;
}

.metric-tip {
  margin-top: 10px;
  color: #98a2b3;
  font-size: 12px;
}

.recommend-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recommend-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(180deg, #f8fafc 0%, #eef4ff 100%);
}

.recommend-title {
  font-weight: 700;
  color: #0f172a;
}

.recommend-desc {
  margin-top: 8px;
  line-height: 1.6;
  color: #475467;
  font-size: 13px;
}

@media (max-width: 920px) {
  .metric-value {
    font-size: 24px;
  }

  .ops-mini-value {
    font-size: 20px;
  }
}
</style>
