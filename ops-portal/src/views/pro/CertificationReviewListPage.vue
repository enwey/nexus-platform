<template>
  <div class="pro-page">
    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ lt('资质审核列表', '資質審核列表', 'Certification Review List') }}</div>
            <div class="panel-subtitle">{{ lt('统一查看开发者资质档案，从列表进入详情页完成资质审核。', '統一查看開發者資質檔案，從列表進入詳情頁完成資質審核。', 'Inspect developer certification profiles and move into detail pages for certification review.') }}</div>
          </div>
          <div class="actions">
            <el-input v-model.trim="keyword" clearable style="width: 260px" :placeholder="lt('搜索用户名 / 邮箱 / 主体名', '搜尋使用者名稱 / 電子郵件 / 主體名', 'Search username / email / subject')" />
            <el-select v-model="profileStatusFilter" clearable style="width: 180px">
              <el-option :label="lt('全部档案状态', '全部檔案狀態', 'All Profile Statuses')" value="" />
              <el-option :label="lt('审核中', '審核中', 'Pending')" value="PENDING" />
              <el-option :label="lt('已通过', '已通過', 'Verified')" value="VERIFIED" />
              <el-option :label="lt('已驳回', '已駁回', 'Rejected')" value="REJECTED" />
              <el-option :label="lt('草稿', '草稿', 'Draft')" value="DRAFT" />
            </el-select>
            <el-button @click="router.push('/pro/developer-management/developers/list')">{{ lt('查看开发者列表', '查看開發者列表', 'Open Developer List') }}</el-button>
            <el-button :loading="loading" @click="loadRows">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredRows" v-loading="loading">
        <el-table-column prop="username" :label="lt('账号', '帳號', 'Account')" min-width="160" />
        <el-table-column prop="email" :label="lt('邮箱', '電子郵件', 'Email')" min-width="220" />
        <el-table-column :label="lt('档案状态', '檔案狀態', 'Profile Status')" width="130">
          <template #default="{ row }">
            <el-tag :type="profileStatusTagType(row.certificationProfileStatus)">{{ profileStatusText(row.certificationProfileStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('资质状态', '資質狀態', 'Certification')" width="130">
          <template #default="{ row }">
            <el-tag :type="certificationTagType(row.certificationStatus)">{{ certificationText(row.certificationStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('认证主体', '認證主體', 'Certification Subject')" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.certificationSubjectName || '-' }}</template>
        </el-table-column>
        <el-table-column :label="lt('主体类型', '主體類型', 'Subject Type')" width="140">
          <template #default="{ row }">{{ subjectTypeText(row.certificationSubjectType) }}</template>
        </el-table-column>
        <el-table-column prop="gameCount" :label="lt('名下游戏数', '名下遊戲數', 'Games')" width="100" />
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ lt('查看详情', '查看詳情', 'View Detail') }}</el-button>
            <el-button link @click="openDeveloper(row)">{{ lt('开发者详情', '開發者詳情', 'Developer') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDeveloperAccounts } from '../../api'
import { useI18nLite } from '../../i18n'

const router = useRouter()
const { lt } = useI18nLite()

const loading = ref(false)
const rows = ref([])
const keyword = ref('')
const profileStatusFilter = ref('PENDING')

const filteredRows = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  return rows.value.filter((row) => {
    const matchesStatus = !profileStatusFilter.value || row.certificationProfileStatus === profileStatusFilter.value
    if (!matchesStatus) return false
    if (!key) return true
    return (row.username || '').toLowerCase().includes(key)
      || (row.email || '').toLowerCase().includes(key)
      || (row.certificationSubjectName || '').toLowerCase().includes(key)
  })
})

const loadRows = async () => {
  loading.value = true
  try {
    const res = await getDeveloperAccounts()
    rows.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    ElMessage.error(error.message || lt('加载资质审核列表失败', '載入資質審核列表失敗', 'Failed to load certification review list'))
  } finally {
    loading.value = false
  }
}

const openDetail = (row) => {
  router.push({ name: 'OpsCertificationReviewDetail', params: { reviewId: row.id } })
}

const openDeveloper = (row) => {
  router.push({ name: 'OpsDeveloperDetail', params: { developerId: row.id } })
}

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

const profileStatusText = (status) => ({
  DRAFT: lt('草稿', '草稿', 'Draft'),
  PENDING: lt('审核中', '審核中', 'Pending'),
  VERIFIED: lt('已归档通过', '已歸檔通過', 'Verified'),
  REJECTED: lt('已驳回', '已駁回', 'Rejected')
}[status] || status || '-')

const profileStatusTagType = (status) => ({
  DRAFT: 'info',
  PENDING: 'warning',
  VERIFIED: 'success',
  REJECTED: 'danger'
}[status] || 'info')

const subjectTypeText = (value) => ({
  COMPANY: lt('企业', '企業', 'Company'),
  INDIVIDUAL: lt('个人', '個人', 'Individual')
}[value] || value || '-')

onMounted(loadRows)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 18px; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.panel-title { font-size: 17px; font-weight: 800; color: #101828; }
.panel-subtitle { margin-top: 6px; color: #667085; font-size: 13px; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; }
@media (max-width: 920px) { .head-row { flex-direction: column; } }
</style>
