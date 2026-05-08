<template>
  <div class="dashboard-page">
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
      <el-col :xs="24" :xl="16">
        <el-card class="panel-card">
          <template #header>
            <div class="panel-head">
              <div>
                <div class="panel-title">{{ lt('待处理事项', '待處理事項', 'Action Queue') }}</div>
                <div class="panel-subtitle">{{ lt('把提审、驳回、待补充材料的事项集中处理。', '把提審、駁回、待補充資料的事項集中處理。', 'Focus on submissions, rejections, and items needing more materials.') }}</div>
              </div>
              <el-button text @click="$router.push('/releases')">{{ lt('前往发布中心', '前往發布中心', 'Open Release Center') }}</el-button>
            </div>
          </template>
          <el-table :data="actionRows" :empty-text="lt('当前没有待处理事项', '目前沒有待處理事項', 'No pending actions')">
            <el-table-column prop="gameName" :label="lt('游戏', '遊戲', 'Game')" min-width="180" />
            <el-table-column prop="version" :label="lt('版本', '版本', 'Version')" width="120" />
            <el-table-column :label="lt('状态', '狀態', 'Status')" width="120">
              <template #default="{ row }">
                <el-tag :type="row.statusMeta.type">{{ row.statusText }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" :label="lt('处理建议', '處理建議', 'Recommendation')" min-width="220" />
            <el-table-column prop="updatedAt" :label="lt('最近更新时间', '最近更新時間', 'Updated At')" min-width="170" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="8">
        <el-card class="panel-card">
          <template #header>{{ lt('接入进度', '接入進度', 'Onboarding Progress') }}</template>
          <div class="progress-list">
            <div class="progress-item" v-for="item in progressItems" :key="item.title">
              <div class="progress-row">
                <span class="progress-title">{{ item.title }}</span>
                <span class="progress-value">{{ item.value }}</span>
              </div>
              <el-progress :percentage="item.percent" :stroke-width="10" :show-text="false" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>{{ lt('最近游戏', '最近遊戲', 'Recent Games') }}</template>
          <div class="game-list">
            <div class="game-item" v-for="game in recentGames" :key="game.id">
              <div>
                <div class="game-name">{{ game.name }}</div>
                <div class="game-meta">{{ game.category || lt('未分类', '未分類', 'Uncategorized') }} · {{ formatDate(game.updatedAt || game.createdAt) }}</div>
              </div>
              <el-tag :type="getStatusMeta(game.status).type">{{ getStatusText(game.status) }}</el-tag>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>{{ lt('平台提醒', '平台提醒', 'Portal Notes') }}</template>
          <div class="notice-list">
            <div class="notice-item" v-for="item in notices" :key="item.title">
              <div class="notice-title">{{ item.title }}</div>
              <div class="notice-desc">{{ item.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getDeveloperGames, getGameVersions } from '../api'
import { useI18nLite } from '../i18n'
import { useUserStore } from '../stores/user'
import { formatDate, getGameStatusMeta } from '../utils/portal'

const { lt } = useI18nLite()
const userStore = useUserStore()
const games = ref([])
const versionsByGame = ref({})

const loadDashboard = async () => {
  try {
    const developerId = userStore.user?.id
    if (!developerId) return
    const res = await getDeveloperGames(developerId)
    games.value = res.data || []
    const versionEntries = await Promise.all(
      games.value.map(async (game) => {
        const versionRes = await getGameVersions(game.id)
        return [game.id, versionRes.data || []]
      })
    )
    versionsByGame.value = Object.fromEntries(versionEntries)
  } catch (error) {
    ElMessage.error(error.message || lt('加载工作台失败', '載入工作台失敗', 'Failed to load dashboard'))
  }
}

const metricCards = computed(() => {
  const totalGames = games.value.length
  const approvedGames = games.value.filter((item) => item.status === 'APPROVED').length
  const pendingGames = games.value.filter((item) => ['PENDING', 'PROCESSING', 'DRAFT'].includes(item.status)).length
  const totalVersions = Object.values(versionsByGame.value).flat().length
  return [
    { key: 'games', label: lt('游戏资产', '遊戲資產', 'Game Assets'), value: totalGames, tip: lt('当前账号下的全部游戏条目', '目前帳號下的全部遊戲條目', 'All games under this account') },
    { key: 'approved', label: lt('已通过', '已通過', 'Approved'), value: approvedGames, tip: lt('已处于可上线状态的游戏', '已處於可上線狀態的遊戲', 'Games ready for distribution') },
    { key: 'pending', label: lt('待推进', '待推進', 'Need Attention'), value: pendingGames, tip: lt('含草稿、处理中和待审核状态', '含草稿、處理中和待審核狀態', 'Draft, processing, and pending states') },
    { key: 'versions', label: lt('版本累计', '版本累計', 'Version Count'), value: totalVersions, tip: lt('累计生成的版本记录数', '累計產生的版本記錄數', 'Total version records generated') }
  ]
})

const actionRows = computed(() => {
  const rows = []
  games.value.forEach((game) => {
    const versions = versionsByGame.value[game.id] || []
    const targetVersion = versions.slice().sort((a, b) => new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt))[0]
    if (!targetVersion) return
    if (['DRAFT', 'REJECTED', 'SUBMITTED'].includes(targetVersion.status)) {
      rows.push({
        gameName: game.name,
        version: targetVersion.versionName,
        statusMeta: getGameStatusMeta(targetVersion.status),
        statusText: getVersionStatusText(targetVersion.status),
        message: targetVersion.status === 'REJECTED'
          ? (targetVersion.auditReason || lt('查看驳回原因并重新提审', '查看駁回原因並重新提審', 'Review rejection reason and resubmit'))
          : targetVersion.status === 'SUBMITTED'
            ? lt('等待运营审核结果', '等待營運審核結果', 'Waiting for operations review')
            : lt('建议补充说明后提交审核', '建議補充說明後提交審核', 'Add more release context before submission'),
        updatedAt: formatDate(targetVersion.updatedAt || targetVersion.createdAt)
      })
    }
  })
  return rows.slice().sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt))
})

const progressItems = computed(() => {
  const total = Math.max(games.value.length, 1)
  const approved = games.value.filter((item) => item.status === 'APPROVED').length
  const withCategory = games.value.filter((item) => item.category).length
  const withDescription = games.value.filter((item) => item.description && item.description.length >= 10).length
  return [
    { title: lt('审核通过率', '審核通過率', 'Approval Rate'), value: `${Math.round((approved / total) * 100)}%`, percent: Math.round((approved / total) * 100) },
    { title: lt('分类完善率', '分類完善率', 'Category Coverage'), value: `${Math.round((withCategory / total) * 100)}%`, percent: Math.round((withCategory / total) * 100) },
    { title: lt('资料完整率', '資料完整率', 'Metadata Completeness'), value: `${Math.round((withDescription / total) * 100)}%`, percent: Math.round((withDescription / total) * 100) }
  ]
})

const recentGames = computed(() => games.value.slice().sort((a, b) => new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt)).slice(0, 5))
const notices = computed(() => [
  {
    title: lt('提审前检查 ZIP 结构', '提審前檢查 ZIP 結構', 'Check ZIP Structure Before Submission'),
    desc: lt('确保入口文件、资源路径与 manifest 摘要一致，能显著降低驳回率。', '確保入口檔案、資源路徑與 manifest 摘要一致，可明顯降低駁回率。', 'Make sure entry files, asset paths, and manifest summary are aligned to reduce rejections.')
  },
  {
    title: lt('关键版本准备回滚方案', '關鍵版本準備回滾方案', 'Prepare Rollback for Critical Releases'),
    desc: lt('涉及支付、登录或资源加载链路时，建议保留一版稳定历史版本。', '涉及支付、登入或資源載入鏈路時，建議保留一版穩定歷史版本。', 'Keep a stable previous version when a release touches payment, login, or asset loading.')
  },
  {
    title: lt('持续补全文档中心', '持續補全文檔中心', 'Keep Docs Center Updated'),
    desc: lt('团队协作时，把引擎导出步骤和提审清单沉淀到文档里，能大幅减少返工。', '團隊協作時，把引擎匯出步驟和提審清單沉澱到文件裡，能大幅減少返工。', 'Document engine export steps and submission checklists to reduce team rework.')
  }
])

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

function getVersionStatusText(status) {
  const dict = {
    DRAFT: lt('草稿', '草稿', 'Draft'),
    SUBMITTED: lt('已提审', '已提審', 'Submitted'),
    APPROVED: lt('已通过', '已通過', 'Approved'),
    REJECTED: lt('已驳回', '已駁回', 'Rejected'),
    PROCESSING: lt('处理中', '處理中', 'Processing')
  }
  return dict[status] || status || '-'
}

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard-page {
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

.metric-label {
  font-size: 13px;
  color: #667085;
}

.metric-value {
  margin-top: 10px;
  font-size: 30px;
  line-height: 1;
  font-weight: 800;
  color: #0f172a;
}

.metric-tip {
  margin-top: 12px;
  color: #98a2b3;
  font-size: 12px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.panel-title {
  font-size: 16px;
  font-weight: 800;
  color: #101828;
}

.panel-subtitle {
  margin-top: 6px;
  color: #667085;
  font-size: 13px;
}

.progress-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.progress-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.progress-title,
.progress-value {
  font-size: 13px;
  color: #475467;
}

.game-list,
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.game-item,
.notice-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: #f8fafc;
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
}

.game-name,
.notice-title {
  font-weight: 700;
  color: #101828;
}

.game-meta,
.notice-desc {
  margin-top: 6px;
  font-size: 12px;
  color: #667085;
  line-height: 1.6;
}

@media (max-width: 920px) {
  .panel-head,
  .game-item,
  .notice-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
