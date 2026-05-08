<template>
  <div class="pro-page">
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
      <el-col :xs="24" :xl="14">
        <el-card class="panel-card">
          <template #header>{{ lt('治理重点', '治理重點', 'Governance Priorities') }}</template>
          <div class="priority-list">
            <div class="priority-item" v-for="item in priorities" :key="item.title">
              <div class="priority-title">{{ item.title }}</div>
              <div class="priority-desc">{{ item.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="10">
        <el-card class="panel-card">
          <template #header>{{ lt('模块导航', '模組導航', 'Navigation') }}</template>
          <div class="module-grid">
            <div class="module-item" v-for="item in modules" :key="item.path" @click="router.push(item.path)">
              <div class="module-title">{{ item.title }}</div>
              <div class="module-desc">{{ item.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>{{ lt('待审核游戏', '待審核遊戲', 'Pending Reviews') }}</template>
          <el-table :data="pendingGames" :empty-text="lt('暂无待审核游戏', '暫無待審核遊戲', 'No pending games')">
            <el-table-column prop="name" :label="lt('游戏', '遊戲', 'Game')" min-width="180" />
            <el-table-column prop="developerId" :label="lt('开发者', '開發者', 'Developer')" width="120" />
            <el-table-column prop="version" :label="lt('版本', '版本', 'Version')" width="120" />
            <el-table-column :label="lt('更新时间', '更新時間', 'Updated')" min-width="170">
              <template #default="{ row }">{{ formatDate(row.updatedAt || row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>{{ lt('高活跃开发者', '高活躍開發者', 'Active Developers') }}</template>
          <el-table :data="topDevelopers" :empty-text="lt('暂无开发者数据', '暫無開發者資料', 'No developer data')">
            <el-table-column prop="username" :label="lt('账号', '帳號', 'Account')" min-width="160" />
            <el-table-column prop="gameCount" :label="lt('游戏数', '遊戲數', 'Games')" width="90" />
            <el-table-column prop="approvedGames" :label="lt('通过', '通過', 'Approved')" width="90" />
            <el-table-column :label="lt('最近活跃', '最近活躍', 'Last Active')" min-width="170">
              <template #default="{ row }">{{ formatDate(row.lastGameAt || row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :xs="24">
        <el-card class="panel-card">
          <template #header>{{ lt('状态治理看板', '狀態治理看板', 'State Governance Board') }}</template>
          <div class="module-grid">
            <div class="module-item" @click="router.push({ path: '/pro/games', query: { status: 'PENDING' } })">
              <div class="module-title">{{ lt('待审核资产', '待審核資產', 'Pending Assets') }}</div>
              <div class="module-desc">{{ lt('需要运营审核才能进入前端流转。', '需要營運審核才能進入前端流轉。', 'Requires operations approval before frontend circulation.') }}</div>
            </div>
            <div class="module-item" @click="router.push({ path: '/pro/games', query: { status: 'APPROVED' } })">
              <div class="module-title">{{ lt('已通过资产', '已通過資產', 'Approved Assets') }}</div>
              <div class="module-desc">{{ lt('可继续进入推荐、广告和前端分发流程。', '可繼續進入推薦、廣告和前端分發流程。', 'Ready for recommendation, ads, and frontend distribution.') }}</div>
            </div>
            <div class="module-item" @click="router.push({ path: '/pro/games', query: { status: 'REJECTED' } })">
              <div class="module-title">{{ lt('被拦截资产', '被攔截資產', 'Blocked Assets') }}</div>
              <div class="module-desc">{{ lt('需要重新修复与提审，避免异常内容进入前端。', '需要重新修復與提審，避免異常內容進入前端。', 'Needs fixes and resubmission to prevent bad content reaching frontend.') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDeveloperAccounts, getOpsGames, getVerificationCodeLogs } from '../../api'
import { useI18nLite } from '../../i18n'

const router = useRouter()
const { lt } = useI18nLite()
const games = ref([])
const developers = ref([])
const verificationLogs = ref([])

const loadDashboard = async () => {
  try {
    const [gameRes, developerRes, verificationRes] = await Promise.all([
      getOpsGames(),
      getDeveloperAccounts(),
      getVerificationCodeLogs({ limit: 20 })
    ])
    games.value = gameRes.data || []
    developers.value = developerRes.data || []
    verificationLogs.value = verificationRes.data || []
  } catch (error) {
    ElMessage.error(error.message || lt('加载运营总览失败', '載入營運總覽失敗', 'Failed to load operations overview'))
  }
}

const metricCards = computed(() => {
  const pending = games.value.filter((item) => item.status === 'PENDING').length
  const approved = games.value.filter((item) => item.status === 'APPROVED').length
  const rejected = games.value.filter((item) => item.status === 'REJECTED').length
  return [
    { key: 'games', label: lt('平台游戏数', '平台遊戲數', 'Games'), value: games.value.length, tip: lt('当前平台全部游戏条目', '目前平台全部遊戲條目', 'Total game entries on platform') },
    { key: 'pending', label: lt('待审核版本', '待審核版本', 'Pending Reviews'), value: pending, tip: lt('需要审核团队尽快处理', '需要審核團隊盡快處理', 'Items that need review attention') },
    { key: 'approved', label: lt('可运营资产', '可營運資產', 'Operable Assets'), value: approved, tip: lt('已通过审核，可进入前端分发', '已通過審核，可進入前端分發', 'Approved and ready for frontend circulation') },
    { key: 'rejected', label: lt('被拦截资产', '被攔截資產', 'Blocked Assets'), value: rejected, tip: lt('已被驳回，需要重新整改', '已被駁回，需要重新整改', 'Rejected and needs remediation') },
    { key: 'developers', label: lt('开发者账号', '開發者帳號', 'Developers'), value: developers.value.length, tip: lt('已注册的开发者主体数', '已註冊的開發者主體數', 'Registered developer accounts') },
    { key: 'verification', label: lt('短信/验证码日志', '短信/驗證碼日誌', 'Verification Logs'), value: verificationLogs.value.length, tip: lt(`已加载最近 ${verificationLogs.value.length} 条记录`, `已載入最近 ${verificationLogs.value.length} 條記錄`, `Loaded latest ${verificationLogs.value.length} logs`) }
  ]
})

const pendingGames = computed(() => games.value.filter((item) => item.status === 'PENDING').slice(0, 6))
const topDevelopers = computed(() => developers.value.slice().sort((a, b) => b.gameCount - a.gameCount).slice(0, 6))

const priorities = computed(() => [
  {
    title: lt('清理待审核积压', '清理待審核積壓', 'Clear Pending Queue'),
    desc: lt(`当前有 ${pendingGames.value.length} 个待审核游戏建议优先处理，避免影响开发者提审时效。`, `目前有 ${pendingGames.value.length} 個待審核遊戲建議優先處理，避免影響開發者提審時效。`, `There are ${pendingGames.value.length} pending games. Prioritize them to keep review SLAs healthy.`)
  },
  {
    title: lt('关注资产状态治理', '關注資產狀態治理', 'Watch Asset State Governance'),
    desc: lt('运营后台要重点控制哪些资产可被前端消费、哪些必须被拦截。', '營運後台要重點控制哪些資產可被前端消費、哪些必須被攔截。', 'Operations must decide which assets are eligible for frontend consumption and which must stay blocked.')
  },
  {
    title: lt('检查运行时配置与推荐位', '檢查運行時配置與推薦位', 'Review Runtime & Slots'),
    desc: lt('每次大版本前都应复核运行时配置、发现位素材与兼容参数。', '每次大版本前都應複核運行時配置、發現位素材與相容參數。', 'Before major releases, verify runtime config, discover assets, and compatibility parameters.')
  }
])

const modules = computed(() => [
  { path: '/pro/reviews', title: lt('审核中心', '審核中心', 'Review Center'), desc: lt('审核通过、驳回与版本检查。', '審核通過、駁回與版本檢查。', 'Approve, reject, and inspect versions.') },
  { path: '/pro/games', title: lt('游戏管理', '遊戲管理', 'Game Management'), desc: lt('维护主数据、分类和资料。', '維護主數據、分類和資料。', 'Maintain metadata, categories, and materials.') },
  { path: '/pro/recommend', title: lt('推荐运营', '推薦營運', 'Recommendation Ops'), desc: lt('配置发现页 Banner、推荐内容与分类。', '配置發現頁 Banner、推薦內容與分類。', 'Configure discover banners, recommendations, and categories.') },
  { path: '/pro/runtime', title: lt('运行时配置', '運行時配置', 'Runtime Config'), desc: lt('管控 Android 运行时参数与分发能力。', '管控 Android 運行時參數與分發能力。', 'Control Android runtime parameters and distribution readiness.') }
])

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

onMounted(loadDashboard)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.section-row { margin-top: 0; }
.metric-card, .panel-card { border-radius: 18px; }
.metric-label { color: #667085; font-size: 13px; }
.metric-value { margin-top: 10px; font-size: 28px; font-weight: 800; color: #101828; }
.metric-tip { margin-top: 10px; color: #98a2b3; font-size: 12px; }
.priority-list, .module-grid { display: flex; flex-direction: column; gap: 12px; }
.priority-item, .module-item { padding: 14px 16px; border-radius: 16px; background: #f8fafc; cursor: pointer; }
.priority-title, .module-title { font-weight: 700; color: #101828; }
.priority-desc, .module-desc { margin-top: 8px; color: #667085; line-height: 1.6; font-size: 13px; }
.module-item:hover { background: #eef4ff; }
@media (max-width: 920px) { .module-grid { gap: 10px; } }
</style>
