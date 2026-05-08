<template>
  <div class="pro-page">
    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ lt('开发者列表', '開發者列表', 'Developer List') }}</div>
            <div class="panel-subtitle">{{ lt('从账号主体视角查看活跃度、审核表现和名下游戏分布，从列表进入详情页处理治理。', '從帳號主體視角查看活躍度、審核表現和名下遊戲分布，從列表進入詳情頁處理治理。', 'Inspect developer activity, review quality, and portfolio distribution before moving into detail pages for governance.') }}</div>
          </div>
          <div class="actions">
            <el-input v-model.trim="keyword" clearable style="width: 260px" :placeholder="lt('搜索用户名 / 邮箱 / 游戏名', '搜尋使用者名稱 / 電子郵件 / 遊戲名', 'Search username / email / game')" />
            <el-select v-model="statusFilter" clearable style="width: 160px">
              <el-option :label="lt('全部状态', '全部狀態', 'All Statuses')" value="" />
              <el-option :label="lt('正常', '正常', 'Active')" value="ACTIVE" />
              <el-option :label="lt('暂停', '暫停', 'Suspended')" value="SUSPENDED" />
              <el-option :label="lt('封禁', '封禁', 'Banned')" value="BANNED" />
            </el-select>
            <el-button @click="router.push('/pro/review-center/certifications/list')">{{ lt('资质审核列表', '資質審核列表', 'Certification Reviews') }}</el-button>
            <el-button :loading="loading" @click="loadDevelopers">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredRows" v-loading="loading">
        <el-table-column prop="username" :label="lt('账号', '帳號', 'Account')" min-width="160" />
        <el-table-column prop="email" :label="lt('邮箱', '電子郵件', 'Email')" min-width="220" />
        <el-table-column :label="lt('账号状态', '帳號狀態', 'Account Status')" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.accountStatus)">{{ statusText(row.accountStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('资质状态', '資質狀態', 'Certification')" width="130">
          <template #default="{ row }">
            <el-tag :type="certificationTagType(row.certificationStatus)">{{ certificationText(row.certificationStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('风险等级', '風險等級', 'Risk Level')" width="120">
          <template #default="{ row }">
            <el-tag :type="riskTagType(row.riskLevel)">{{ row.riskLevel || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="violationCount" :label="lt('违规次数', '違規次數', 'Violations')" width="100" />
        <el-table-column prop="gameCount" :label="lt('游戏数', '遊戲數', 'Games')" width="90" />
        <el-table-column prop="approvedGames" :label="lt('通过', '通過', 'Approved')" width="90" />
        <el-table-column prop="pendingGames" :label="lt('待处理', '待處理', 'Pending')" width="90" />
        <el-table-column :label="lt('最近活跃', '最近活躍', 'Last Active')" min-width="170">
          <template #default="{ row }">{{ formatDate(row.lastGameAt || row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="lt('名下游戏', '名下遊戲', 'Games')" min-width="280">
          <template #default="{ row }">
            <div class="tag-list">
              <el-tag v-for="name in row.games" :key="name" size="small">{{ name }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ lt('查看详情', '查看詳情', 'View Detail') }}</el-button>
            <el-button v-if="row.accountStatus !== 'ACTIVE'" link type="success" @click="quickGovernance(row, 'ACTIVE')">{{ lt('恢复', '恢復', 'Restore') }}</el-button>
            <el-button v-if="row.accountStatus === 'ACTIVE'" link type="warning" @click="quickGovernance(row, 'SUSPENDED')">{{ lt('暂停', '暫停', 'Suspend') }}</el-button>
            <el-button v-if="row.accountStatus !== 'BANNED'" link type="danger" @click="quickGovernance(row, 'BANNED')">{{ lt('封禁', '封禁', 'Ban') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeveloperAccounts, updateDeveloperGovernance } from '../../api'
import { useI18nLite } from '../../i18n'
import { formatDate } from './entityPageShared'

const router = useRouter()
const { lt } = useI18nLite()

const loading = ref(false)
const keyword = ref('')
const statusFilter = ref('')
const rows = ref([])

const filteredRows = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  return rows.value.filter((row) => {
    const matchesStatus = !statusFilter.value || row.accountStatus === statusFilter.value
    if (!matchesStatus) return false
    if (!key) return true
    return (row.username || '').toLowerCase().includes(key)
      || (row.email || '').toLowerCase().includes(key)
      || (row.games || []).some((name) => (name || '').toLowerCase().includes(key))
  })
})

const loadDevelopers = async () => {
  loading.value = true
  try {
    const res = await getDeveloperAccounts()
    rows.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || lt('加载开发者数据失败', '載入開發者資料失敗', 'Failed to load developer accounts'))
  } finally {
    loading.value = false
  }
}

const openDetail = (row) => {
  router.push({ name: 'OpsDeveloperDetail', params: { developerId: row.id } })
}

const promptReason = async (row, status) => {
  const label = status === 'BANNED' ? lt('封禁', '封禁', 'Ban') : lt('暂停', '暫停', 'Suspend')
  const { value } = await ElMessageBox.prompt(
    lt('请输入治理原因，至少 2 个字符', '請輸入治理原因，至少 2 個字元', 'Please input a governance reason with at least 2 characters'),
    `${label}: ${row.username}`,
    {
      confirmButtonText: lt('确认', '確認', 'Confirm'),
      cancelButtonText: lt('取消', '取消', 'Cancel'),
      inputPattern: /^.{2,}$/u,
      inputErrorMessage: lt('治理原因至少 2 个字符', '治理原因至少 2 個字元', 'Reason must be at least 2 characters')
    }
  )
  return value.trim()
}

const quickGovernance = async (row, status) => {
  const reason = status === 'ACTIVE' ? '' : await promptReason(row, status)
  try {
    loading.value = true
    await updateDeveloperGovernance(row.id, {
      accountStatus: status,
      certificationStatus: row.certificationStatus || 'UNVERIFIED',
      riskLevel: row.riskLevel || 'NORMAL',
      whitelistStatus: row.whitelistStatus || 'STANDARD',
      violationCount: row.violationCount || 0,
      governanceTag: row.governanceTag || '',
      opsNote: row.opsNote || '',
      reason
    })
    ElMessage.success(lt('开发者治理已更新', '開發者治理已更新', 'Developer governance updated'))
    await loadDevelopers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('更新开发者治理失败', '更新開發者治理失敗', 'Failed to update developer governance'))
    }
  } finally {
    loading.value = false
  }
}

const statusText = (status) => ({
  ACTIVE: lt('正常', '正常', 'Active'),
  SUSPENDED: lt('暂停', '暫停', 'Suspended'),
  BANNED: lt('封禁', '封禁', 'Banned')
}[status] || status || '-')

const statusTagType = (status) => ({
  ACTIVE: 'success',
  SUSPENDED: 'warning',
  BANNED: 'danger'
}[status] || 'info')

const certificationText = (status) => ({
  DRAFT: lt('草稿', '草稿', 'Draft'),
  UNVERIFIED: lt('未认证', '未認證', 'Unverified'),
  PENDING: lt('审核中', '審核中', 'Pending'),
  VERIFIED: lt('已认证', '已認證', 'Verified'),
  REJECTED: lt('已驳回', '已駁回', 'Rejected')
}[status] || status || '-')

const certificationTagType = (status) => ({
  DRAFT: 'info',
  UNVERIFIED: 'info',
  PENDING: 'warning',
  VERIFIED: 'success',
  REJECTED: 'danger'
}[status] || 'info')

const riskTagType = (status) => ({
  LOW: 'success',
  NORMAL: '',
  HIGH: 'warning',
  CRITICAL: 'danger'
}[status] || 'info')

onMounted(loadDevelopers)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 18px; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.panel-title { font-size: 17px; font-weight: 800; color: #101828; }
.panel-subtitle { margin-top: 6px; color: #667085; font-size: 13px; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; }
.tag-list { display: flex; gap: 6px; flex-wrap: wrap; }
@media (max-width: 920px) { .head-row { flex-direction: column; } }
</style>
