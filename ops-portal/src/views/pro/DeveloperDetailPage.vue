<template>
  <div class="pro-page">
    <el-card class="panel-card" v-loading="loading">
      <template #header>
        <div class="head-row">
          <div>
            <div class="breadcrumb-line">
              <el-button link @click="router.push('/pro/developer-management/developers/list')">{{ lt('返回开发者列表', '返回開發者列表', 'Back to Developer List') }}</el-button>
              <span>/</span>
              <span>{{ developer?.username || lt('开发者详情', '開發者詳情', 'Developer Detail') }}</span>
            </div>
            <div class="panel-title">{{ developer?.username || '-' }}</div>
            <div class="panel-subtitle">{{ developer?.email || '-' }}</div>
          </div>
          <div class="actions">
            <el-button @click="openCertification">{{ lt('资质审核详情', '資質審核詳情', 'Certification Detail') }}</el-button>
            <el-button :loading="loading" @click="loadDetail">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <el-empty v-if="!developer" :description="lt('未找到对应开发者', '未找到對應開發者', 'Developer not found')" />
      <template v-else>
        <div class="summary-grid">
          <div class="summary-item">
            <span>{{ lt('账号状态', '帳號狀態', 'Account Status') }}</span>
            <strong><el-tag :type="statusTagType(developer.accountStatus)">{{ statusText(developer.accountStatus) }}</el-tag></strong>
          </div>
          <div class="summary-item">
            <span>{{ lt('资质状态', '資質狀態', 'Certification') }}</span>
            <strong><el-tag :type="certificationTagType(developer.certificationStatus)">{{ certificationText(developer.certificationStatus) }}</el-tag></strong>
          </div>
          <div class="summary-item">
            <span>{{ lt('风险等级', '風險等級', 'Risk Level') }}</span>
            <strong><el-tag :type="riskTagType(developer.riskLevel)">{{ developer.riskLevel || '-' }}</el-tag></strong>
          </div>
          <div class="summary-item">
            <span>{{ lt('名下游戏数', '名下遊戲數', 'Games') }}</span>
            <strong>{{ developer.gameCount }}</strong>
          </div>
        </div>

        <el-descriptions :column="2" border class="section">
          <el-descriptions-item :label="lt('账号', '帳號', 'Account')">{{ developer.username || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('邮箱', '電子郵件', 'Email')">{{ developer.email || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('认证主体', '認證主體', 'Certification Subject')">{{ developer.certificationSubjectName || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('主体类型', '主體類型', 'Subject Type')">{{ subjectTypeText(developer.certificationSubjectType) }}</el-descriptions-item>
          <el-descriptions-item :label="lt('名单状态', '名單狀態', 'Whitelist')">{{ whitelistText(developer.whitelistStatus) }}</el-descriptions-item>
          <el-descriptions-item :label="lt('违规次数', '違規次數', 'Violations')">{{ developer.violationCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item :label="lt('治理标签', '治理標籤', 'Governance Tag')">{{ developer.governanceTag || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('最近活跃', '最近活躍', 'Last Active')">{{ formatDate(developer.lastGameAt || developer.createdAt) }}</el-descriptions-item>
          <el-descriptions-item :label="lt('运营备注', '營運備註', 'Ops Note')" :span="2">{{ developer.opsNote || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-card shadow="never" class="section">
          <template #header>{{ lt('开发者治理', '開發者治理', 'Developer Governance') }}</template>
          <el-form :model="governanceForm" label-width="120px">
            <el-form-item :label="lt('账号状态', '帳號狀態', 'Account Status')" required>
              <el-select v-model="governanceForm.accountStatus" style="width: 100%">
                <el-option :label="lt('正常', '正常', 'Active')" value="ACTIVE" />
                <el-option :label="lt('暂停', '暫停', 'Suspended')" value="SUSPENDED" />
                <el-option :label="lt('封禁', '封禁', 'Banned')" value="BANNED" />
              </el-select>
            </el-form-item>
            <el-form-item :label="lt('资质状态', '資質狀態', 'Certification')">
              <el-select v-model="governanceForm.certificationStatus" style="width: 100%">
                <el-option :label="lt('未认证', '未認證', 'Unverified')" value="UNVERIFIED" />
                <el-option :label="lt('审核中', '審核中', 'Pending')" value="PENDING" />
                <el-option :label="lt('已认证', '已認證', 'Verified')" value="VERIFIED" />
                <el-option :label="lt('已驳回', '已駁回', 'Rejected')" value="REJECTED" />
              </el-select>
            </el-form-item>
            <el-form-item :label="lt('风险等级', '風險等級', 'Risk Level')">
              <el-select v-model="governanceForm.riskLevel" style="width: 100%">
                <el-option label="LOW" value="LOW" />
                <el-option label="NORMAL" value="NORMAL" />
                <el-option label="HIGH" value="HIGH" />
                <el-option label="CRITICAL" value="CRITICAL" />
              </el-select>
            </el-form-item>
            <el-form-item :label="lt('名单状态', '名單狀態', 'Whitelist')">
              <el-select v-model="governanceForm.whitelistStatus" style="width: 100%">
                <el-option :label="lt('标准', '標準', 'Standard')" value="STANDARD" />
                <el-option :label="lt('白名单', '白名單', 'Whitelisted')" value="WHITELISTED" />
                <el-option :label="lt('黑名单', '黑名單', 'Blacklisted')" value="BLACKLISTED" />
              </el-select>
            </el-form-item>
            <el-form-item :label="lt('违规次数', '違規次數', 'Violations')">
              <el-input-number v-model="governanceForm.violationCount" :min="0" :max="9999" style="width: 100%" />
            </el-form-item>
            <el-form-item :label="lt('治理标签', '治理標籤', 'Governance Tag')">
              <el-input v-model="governanceForm.governanceTag" maxlength="64" show-word-limit />
            </el-form-item>
            <el-form-item :label="lt('运营备注', '營運備註', 'Ops Note')">
              <el-input v-model="governanceForm.opsNote" type="textarea" :rows="3" maxlength="256" show-word-limit />
            </el-form-item>
            <el-form-item :label="lt('操作原因', '操作原因', 'Reason')">
              <el-input v-model="governanceForm.reason" type="textarea" :rows="3" maxlength="256" show-word-limit />
            </el-form-item>
            <el-form-item>
              <el-button type="danger" :loading="saving" @click="submitGovernance">{{ lt('确认执行', '確認執行', 'Confirm') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="section">
          <template #header>{{ lt('名下游戏', '名下遊戲', 'Games') }}</template>
          <div class="tag-list">
            <el-tag v-for="name in developer.games || []" :key="name" size="small">{{ name }}</el-tag>
            <span v-if="!(developer.games || []).length">-</span>
          </div>
        </el-card>

        <el-card shadow="never" class="section">
          <template #header>{{ lt('治理档案', '治理檔案', 'Governance History') }}</template>
          <el-table :data="historyRows" :empty-text="lt('暂无治理记录', '暫無治理記錄', 'No governance records')">
            <el-table-column prop="actionType" :label="lt('动作', '動作', 'Action')" width="180" />
            <el-table-column prop="reason" :label="lt('原因', '原因', 'Reason')" min-width="220" show-overflow-tooltip />
            <el-table-column :label="lt('变更前', '變更前', 'Before')" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ summarizeGovernanceSnapshot(row.beforeSnapshotJson) }}</template>
            </el-table-column>
            <el-table-column :label="lt('变更后', '變更後', 'After')" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ summarizeGovernanceSnapshot(row.afterSnapshotJson) }}</template>
            </el-table-column>
            <el-table-column prop="operatorId" :label="lt('操作人', '操作人', 'Operator')" width="100" />
            <el-table-column :label="lt('时间', '時間', 'Created')" width="180">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDeveloperAccounts,
  getDeveloperGovernanceRecords,
  updateDeveloperGovernance
} from '../../api'
import { useI18nLite } from '../../i18n'
import { formatDate, summarizeGovernanceSnapshot } from './entityPageShared'

const props = defineProps({
  developerId: {
    type: [String, Number],
    required: true
  }
})

const router = useRouter()
const { lt } = useI18nLite()

const loading = ref(false)
const saving = ref(false)
const developer = ref(null)
const historyRows = ref([])
const governanceForm = reactive({
  accountStatus: 'ACTIVE',
  certificationStatus: 'UNVERIFIED',
  riskLevel: 'NORMAL',
  whitelistStatus: 'STANDARD',
  violationCount: 0,
  governanceTag: '',
  opsNote: '',
  reason: ''
})

const loadDetail = async () => {
  loading.value = true
  try {
    const [accountsRes, historyRes] = await Promise.all([
      getDeveloperAccounts(),
      getDeveloperGovernanceRecords(props.developerId)
    ])
    developer.value = (accountsRes.data || []).find((item) => String(item.id) === String(props.developerId)) || null
    historyRows.value = historyRes.data || []
    if (developer.value) {
      governanceForm.accountStatus = developer.value.accountStatus || 'ACTIVE'
      governanceForm.certificationStatus = developer.value.certificationStatus || 'UNVERIFIED'
      governanceForm.riskLevel = developer.value.riskLevel || 'NORMAL'
      governanceForm.whitelistStatus = developer.value.whitelistStatus || 'STANDARD'
      governanceForm.violationCount = developer.value.violationCount || 0
      governanceForm.governanceTag = developer.value.governanceTag || ''
      governanceForm.opsNote = developer.value.opsNote || ''
      governanceForm.reason = ''
    }
  } catch (error) {
    ElMessage.error(error.message || lt('加载开发者详情失败', '載入開發者詳情失敗', 'Failed to load developer detail'))
  } finally {
    loading.value = false
  }
}

const submitGovernance = async () => {
  if (!developer.value?.id) return
  if (governanceForm.accountStatus !== 'ACTIVE' && governanceForm.reason.trim().length < 2) {
    ElMessage.warning(lt('治理原因至少 2 个字符', '治理原因至少 2 個字元', 'Reason must be at least 2 characters'))
    return
  }
  try {
    await ElMessageBox.confirm(
      lt('确认更新该开发者的治理状态？该操作会影响登录与后台操作。', '確認更新該開發者的治理狀態？此操作會影響登入與後台操作。', 'Confirm updating this developer governance status? This affects login and backend access.'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { confirmButtonText: lt('确认执行', '確認執行', 'Confirm'), cancelButtonText: lt('取消', '取消', 'Cancel'), type: 'warning' }
    )
    saving.value = true
    await updateDeveloperGovernance(developer.value.id, {
      accountStatus: governanceForm.accountStatus,
      certificationStatus: governanceForm.certificationStatus,
      riskLevel: governanceForm.riskLevel,
      whitelistStatus: governanceForm.whitelistStatus,
      violationCount: governanceForm.violationCount,
      governanceTag: governanceForm.governanceTag.trim(),
      opsNote: governanceForm.opsNote.trim(),
      reason: governanceForm.reason.trim()
    })
    ElMessage.success(lt('开发者治理已更新', '開發者治理已更新', 'Developer governance updated'))
    await loadDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('更新开发者治理失败', '更新開發者治理失敗', 'Failed to update developer governance'))
    }
  } finally {
    saving.value = false
  }
}

const openCertification = () => {
  if (developer.value?.id) {
    router.push({ name: 'OpsCertificationReviewDetail', params: { reviewId: developer.value.id } })
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

const whitelistText = (status) => ({
  STANDARD: lt('标准', '標準', 'Standard'),
  WHITELISTED: lt('白名单', '白名單', 'Whitelisted'),
  BLACKLISTED: lt('黑名单', '黑名單', 'Blacklisted')
}[status] || status || '-')

const subjectTypeText = (value) => ({
  COMPANY: lt('企业', '企業', 'Company'),
  INDIVIDUAL: lt('个人', '個人', 'Individual')
}[value] || value || '-')

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
.tag-list { display: flex; gap: 6px; flex-wrap: wrap; }
@media (max-width: 920px) {
  .head-row { flex-direction: column; }
  .summary-grid { grid-template-columns: 1fr 1fr; }
}
</style>
