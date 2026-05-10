<template>
  <div class="pro-page">
    <el-alert v-if="loadError" type="error" :closable="false" :title="loadError" />

    <div class="summary-grid">
      <div class="summary-item"><span>{{ lt('开发者总数', '開發者總數', 'Developers') }}</span><strong>{{ rows.length }}</strong></div>
      <div class="summary-item"><span>{{ lt('已认证主体', '已認證主體', 'Verified Subjects') }}</span><strong>{{ verifiedCount }}</strong></div>
      <div class="summary-item"><span>{{ lt('高风险账号', '高風險帳號', 'High Risk') }}</span><strong>{{ highRiskCount }}</strong></div>
      <div class="summary-item"><span>{{ lt('黑名单账号', '黑名單帳號', 'Blacklisted') }}</span><strong>{{ blacklistedCount }}</strong></div>
    </div>

    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ lt('开发者管理', '開發者管理', 'Developer Management') }}</div>
            <div class="panel-subtitle">{{ lt('从账号主体视角查看活跃度、审核表现和名下游戏分布。', '從帳號主體視角查看活躍度、審核表現和名下遊戲分布。', 'Inspect activity, review quality, and portfolio distribution by developer account.') }}</div>
          </div>
          <div class="actions">
            <el-input v-model.trim="keyword" clearable class="keyword-input" :placeholder="lt('搜索用户名 / 邮箱 / 游戏名', '搜尋使用者名稱 / 電子郵件 / 遊戲名', 'Search username / email / game')" />
            <el-select v-model="statusFilter" clearable class="filter-select filter-select-sm">
              <el-option :label="lt('全部状态', '全部狀態', 'All Statuses')" value="" />
              <el-option :label="lt('正常', '正常', 'Active')" value="ACTIVE" />
              <el-option :label="lt('暂停', '暫停', 'Suspended')" value="SUSPENDED" />
              <el-option :label="lt('封禁', '封禁', 'Banned')" value="BANNED" />
            </el-select>
            <el-button @click="loadDevelopers">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredRows" v-loading="loading" :empty-text="lt('暂无开发者数据', '暫無開發者資料', 'No developer accounts')" @row-click="openDeveloperDetail">
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
        <el-table-column :label="lt('档案状态', '檔案狀態', 'Profile Status')" width="130">
          <template #default="{ row }">
            <el-tag :type="profileStatusTagType(row.certificationProfileStatus)">{{ profileStatusText(row.certificationProfileStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('认证主体', '認證主體', 'Certification Subject')" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.certificationSubjectName || '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="lt('风险等级', '風險等級', 'Risk Level')" width="120">
          <template #default="{ row }">
            <el-tag :type="riskTagType(row.riskLevel)">{{ row.riskLevel || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('名单状态', '名單狀態', 'Whitelist')" width="130">
          <template #default="{ row }">
            <el-tag :type="whitelistTagType(row.whitelistStatus)">{{ whitelistText(row.whitelistStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="violationCount" :label="lt('违规次数', '違規次數', 'Violations')" width="100" />
        <el-table-column prop="governanceTag" :label="lt('治理标签', '治理標籤', 'Governance Tag')" width="140" />
        <el-table-column prop="gameCount" :label="lt('游戏数', '遊戲數', 'Games')" width="90" />
        <el-table-column prop="approvedGames" :label="lt('通过', '通過', 'Approved')" width="90" />
        <el-table-column prop="pendingGames" :label="lt('待处理', '待處理', 'Pending')" width="90" />
        <el-table-column prop="rejectedGames" :label="lt('驳回', '駁回', 'Rejected')" width="90" />
        <el-table-column prop="opsNote" :label="lt('运营备注', '營運備註', 'Ops Note')" min-width="180" show-overflow-tooltip />
        <el-table-column :label="lt('最近活跃', '最近活躍', 'Last Active')" min-width="170">
          <template #default="{ row }">{{ formatDate(row.lastGameAt || row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="lt('名下游戏', '名下遊戲', 'Games')" min-width="320">
          <template #default="{ row }">
            <div class="tag-list">
              <el-tag v-for="name in row.games" :key="name" size="small">{{ name }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link @click.stop="openDeveloperDetail(row)">{{ lt('详情', '詳情', 'Detail') }}</el-button>
            <el-button link type="primary" @click.stop="openGovernance(row)">{{ lt('治理', '治理', 'Govern') }}</el-button>
            <el-button link type="success" @click.stop="openCertification(row)">{{ lt('资质', '資質', 'Certification') }}</el-button>
            <el-button link @click.stop="openHistory(row)">{{ lt('档案', '檔案', 'History') }}</el-button>
            <el-button v-if="row.accountStatus !== 'ACTIVE'" link type="success" @click.stop="quickGovernance(row, 'ACTIVE')">{{ lt('恢复', '恢復', 'Restore') }}</el-button>
            <el-button v-if="row.accountStatus === 'ACTIVE'" link type="warning" @click.stop="quickGovernance(row, 'SUSPENDED')">{{ lt('暂停', '暫停', 'Suspend') }}</el-button>
            <el-button v-if="row.accountStatus !== 'BANNED'" link type="danger" @click.stop="quickGovernance(row, 'BANNED')">{{ lt('封禁', '封禁', 'Ban') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="detailVisible" :title="lt('开发者详情', '開發者詳情', 'Developer Detail')" :size="detailDrawerSize">
      <template v-if="detailRow">
        <el-descriptions :column="1" border>
          <el-descriptions-item :label="lt('账号', '帳號', 'Account')">{{ detailRow.username }}</el-descriptions-item>
          <el-descriptions-item :label="lt('邮箱', '電子郵件', 'Email')">{{ detailRow.email || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('账号状态', '帳號狀態', 'Account Status')">{{ statusText(detailRow.accountStatus) }}</el-descriptions-item>
          <el-descriptions-item :label="lt('资质状态', '資質狀態', 'Certification')">{{ certificationText(detailRow.certificationStatus) }}</el-descriptions-item>
          <el-descriptions-item :label="lt('档案状态', '檔案狀態', 'Profile Status')">{{ profileStatusText(detailRow.certificationProfileStatus) }}</el-descriptions-item>
          <el-descriptions-item :label="lt('认证主体', '認證主體', 'Certification Subject')">{{ detailRow.certificationSubjectName || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('风险等级', '風險等級', 'Risk Level')">{{ detailRow.riskLevel || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('名单状态', '名單狀態', 'Whitelist')">{{ whitelistText(detailRow.whitelistStatus) }}</el-descriptions-item>
          <el-descriptions-item :label="lt('治理标签', '治理標籤', 'Governance Tag')">{{ detailRow.governanceTag || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('运营备注', '營運備註', 'Ops Note')">{{ detailRow.opsNote || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('最近活跃', '最近活躍', 'Last Active')">{{ formatDate(detailRow.lastGameAt || detailRow.createdAt) }}</el-descriptions-item>
          <el-descriptions-item :label="lt('名下游戏', '名下遊戲', 'Games')">{{ (detailRow.games || []).join(' / ') || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div class="detail-actions">
          <el-button type="primary" @click="openGovernance(detailRow)">{{ lt('进入治理', '進入治理', 'Open Governance') }}</el-button>
          <el-button type="success" @click="openCertification(detailRow)">{{ lt('查看资质', '查看資質', 'View Certification') }}</el-button>
          <el-button @click="openHistory(detailRow)">{{ lt('查看档案', '查看檔案', 'View History') }}</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="governanceVisible" :title="lt('开发者治理', '開發者治理', 'Developer Governance')" :width="dialogWidth('620px')">
      <el-form :model="governanceForm" :label-width="formLabelWidth">
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
      </el-form>
      <template #footer>
        <el-button @click="governanceVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="danger" :loading="saving" @click="submitGovernance">{{ lt('确认执行', '確認執行', 'Confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="historyVisible" :title="lt('治理档案', '治理檔案', 'Governance History')" :width="dialogWidth('860px', '92%')">
      <div class="history-head">{{ historyDeveloperLabel }}</div>
      <el-table :data="historyRows" v-loading="historyLoading" :empty-text="lt('暂无治理记录', '暫無治理記錄', 'No governance records')">
        <el-table-column prop="actionType" :label="lt('动作', '動作', 'Action')" width="180" />
        <el-table-column prop="reason" :label="lt('原因', '原因', 'Reason')" min-width="220" show-overflow-tooltip />
        <el-table-column :label="lt('变更前', '變更前', 'Before')" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ summarizeSnapshot(row.beforeSnapshotJson) }}</template>
        </el-table-column>
        <el-table-column :label="lt('变更后', '變更後', 'After')" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ summarizeSnapshot(row.afterSnapshotJson) }}</template>
        </el-table-column>
        <el-table-column prop="operatorId" :label="lt('操作人', '操作人', 'Operator')" width="100" />
        <el-table-column :label="lt('时间', '時間', 'Created')" width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="certificationVisible" :title="lt('开发者资质审核', '開發者資質審核', 'Developer Certification Review')" :width="dialogWidth('920px', '92%')">
      <div class="history-head">{{ certificationDeveloperLabel }}</div>
      <div class="summary-grid cert-grid">
        <div class="summary-item">
          <span>{{ lt('档案状态', '檔案狀態', 'Profile Status') }}</span>
          <strong>{{ profileStatusText(certificationProfile.profileStatus) }}</strong>
        </div>
        <div class="summary-item">
          <span>{{ lt('资质状态', '資質狀態', 'Certification') }}</span>
          <strong>{{ certificationText(certificationProfile.certificationStatus) }}</strong>
        </div>
        <div class="summary-item">
          <span>{{ lt('最近提交', '最近提交', 'Submitted') }}</span>
          <strong>{{ formatDate(certificationProfile.submittedAt) }}</strong>
        </div>
      </div>
      <el-descriptions :column="certificationDescriptionColumns" border class="section-descriptions">
        <el-descriptions-item :label="lt('主体类型', '主體類型', 'Subject Type')">{{ subjectTypeText(certificationProfile.subjectType) }}</el-descriptions-item>
        <el-descriptions-item :label="lt('主体名称', '主體名稱', 'Subject Name')">{{ certificationProfile.subjectName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="lt('法人/负责人', '法人/負責人', 'Legal Representative')">{{ certificationProfile.legalRepresentative || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="lt('联系人', '聯絡人', 'Contact Name')">{{ certificationProfile.contactName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="lt('联系电话', '聯絡電話', 'Contact Phone')">{{ certificationProfile.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="lt('营业执照/证件号', '營業執照/證件號', 'License / ID No')">{{ certificationProfile.businessLicenseNo || certificationProfile.idDocumentNo || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="lt('开发者备注', '開發者備註', 'Developer Note')" :span="2">{{ certificationProfile.note || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="lt('驳回原因', '駁回原因', 'Reject Reason')" :span="2">{{ certificationProfile.rejectionReason || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-card shadow="never" class="section-descriptions">
        <template #header>{{ lt('资质材料链接', '資質材料連結', 'Certificate Assets') }}</template>
        <div class="tag-list">
          <el-link v-for="item in certificationProfile.certificateAssets" :key="item" :href="item" target="_blank" type="primary">{{ item }}</el-link>
          <span v-if="!(certificationProfile.certificateAssets || []).length">-</span>
        </div>
      </el-card>
      <el-form :model="certificationReviewForm" :label-width="formLabelWidth" class="section-descriptions">
        <el-form-item :label="lt('审核动作', '審核動作', 'Review Decision')">
          <el-select v-model="certificationReviewForm.decision" style="width: 100%">
            <el-option :label="lt('认证通过', '認證通過', 'Verify')" value="VERIFIED" />
            <el-option :label="lt('驳回申请', '駁回申請', 'Reject')" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('审核说明', '審核說明', 'Review Note')">
          <el-input v-model="certificationReviewForm.reason" type="textarea" :rows="3" maxlength="256" show-word-limit />
        </el-form-item>
      </el-form>
      <el-table :data="certificationReviewRows" v-loading="certificationLoading" :empty-text="lt('暂无资质审核记录', '暫無資質審核記錄', 'No certification review records')">
        <el-table-column prop="actionType" :label="lt('动作', '動作', 'Action')" width="180" />
        <el-table-column prop="afterStatus" :label="lt('结果', '結果', 'Result')" width="140">
          <template #default="{ row }">
            <el-tag :type="profileStatusTagType(row.afterStatus)">{{ profileStatusText(row.afterStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" :label="lt('原因', '原因', 'Reason')" min-width="240" show-overflow-tooltip />
        <el-table-column prop="operatorId" :label="lt('操作人', '操作人', 'Operator')" width="110" />
        <el-table-column :label="lt('时间', '時間', 'Time')" width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="certificationVisible = false">{{ lt('关闭', '關閉', 'Close') }}</el-button>
        <el-button
          type="primary"
          :loading="certificationSaving"
          :disabled="certificationProfile.profileStatus !== 'PENDING'"
          @click="submitCertificationReview"
        >
          {{ lt('提交审核结果', '提交審核結果', 'Submit Review Result') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDeveloperAccounts,
  getDeveloperCertificationProfile,
  getDeveloperCertificationReviews,
  getDeveloperGovernanceRecords,
  reviewDeveloperCertification,
  updateDeveloperGovernance
} from '../../api'
import { useI18nLite } from '../../i18n'
import { useViewport } from '../../composables/useViewport'

const { lt } = useI18nLite()
const { isTabletOrBelow, isPhone } = useViewport()
const loading = ref(false)
const saving = ref(false)
const loadError = ref('')
const keyword = ref('')
const statusFilter = ref('')
const rows = ref([])
const detailVisible = ref(false)
const detailRow = ref(null)
const governanceVisible = ref(false)
const historyVisible = ref(false)
const historyLoading = ref(false)
const historyRows = ref([])
const historyDeveloperLabel = ref('')
const certificationVisible = ref(false)
const certificationLoading = ref(false)
const certificationSaving = ref(false)
const certificationDeveloperId = ref(null)
const certificationDeveloperLabel = ref('')
const certificationReviewRows = ref([])
const governanceDeveloperId = ref(null)
const certificationProfile = reactive({
  profileStatus: 'DRAFT',
  certificationStatus: 'UNVERIFIED',
  subjectType: '',
  subjectName: '',
  legalRepresentative: '',
  contactName: '',
  contactPhone: '',
  businessLicenseNo: '',
  idDocumentNo: '',
  certificateAssets: [],
  note: '',
  rejectionReason: '',
  submittedAt: '',
  reviewedAt: ''
})
const certificationReviewForm = reactive({
  decision: 'VERIFIED',
  reason: ''
})
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
const formLabelWidth = computed(() => (isPhone.value ? '96px' : '120px'))
const certificationDescriptionColumns = computed(() => (isPhone.value ? 1 : 2))
const detailDrawerSize = computed(() => (isPhone.value ? '100%' : isTabletOrBelow.value ? '72%' : '42%'))

const dialogWidth = (desktop, tablet = '88%', mobile = '94%') => {
  if (isPhone.value) return mobile
  if (isTabletOrBelow.value) return tablet
  return desktop
}

const loadDevelopers = async () => {
  loading.value = true
  try {
    loadError.value = ''
    const res = await getDeveloperAccounts()
    rows.value = res.data || []
  } catch (error) {
    loadError.value = error.message || lt('加载开发者数据失败', '載入開發者資料失敗', 'Failed to load developer accounts')
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

const filteredRows = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  return rows.value.filter((row) => {
    const matchesStatus = !statusFilter.value || row.accountStatus === statusFilter.value
    if (!matchesStatus) return false
    if (!key) return true
    return (row.username || '').toLowerCase().includes(key) ||
      (row.email || '').toLowerCase().includes(key) ||
      (row.games || []).some((name) => (name || '').toLowerCase().includes(key))
  })
})

const verifiedCount = computed(() => rows.value.filter((row) => row.certificationStatus === 'VERIFIED').length)
const highRiskCount = computed(() => rows.value.filter((row) => ['HIGH', 'CRITICAL'].includes(row.riskLevel)).length)
const blacklistedCount = computed(() => rows.value.filter((row) => row.whitelistStatus === 'BLACKLISTED').length)

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

const whitelistTagType = (status) => ({
  STANDARD: 'info',
  WHITELISTED: 'success',
  BLACKLISTED: 'danger'
}[status] || 'info')

const openDeveloperDetail = (row) => {
  detailRow.value = row
  detailVisible.value = true
}

const openGovernance = (row) => {
  governanceDeveloperId.value = row.id
  governanceForm.accountStatus = row.accountStatus || 'ACTIVE'
  governanceForm.certificationStatus = row.certificationStatus || 'UNVERIFIED'
  governanceForm.riskLevel = row.riskLevel || 'NORMAL'
  governanceForm.whitelistStatus = row.whitelistStatus || 'STANDARD'
  governanceForm.violationCount = row.violationCount || 0
  governanceForm.governanceTag = row.governanceTag || ''
  governanceForm.opsNote = row.opsNote || ''
  governanceForm.reason = ''
  governanceVisible.value = true
}

const openHistory = async (row) => {
  historyVisible.value = true
  historyDeveloperLabel.value = `${row.username} / ${row.email || '-'}`
  historyLoading.value = true
  try {
    const res = await getDeveloperGovernanceRecords(row.id)
    historyRows.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || lt('加载治理档案失败', '載入治理檔案失敗', 'Failed to load governance history'))
  } finally {
    historyLoading.value = false
  }
}

const subjectTypeText = (value) => ({
  COMPANY: lt('企业', '企業', 'Company'),
  INDIVIDUAL: lt('个人', '個人', 'Individual')
}[value] || value || '-')

const openCertification = async (row) => {
  certificationVisible.value = true
  certificationDeveloperId.value = row.id
  certificationDeveloperLabel.value = `${row.username} / ${row.email || '-'}`
  certificationReviewForm.decision = 'VERIFIED'
  certificationReviewForm.reason = ''
  certificationLoading.value = true
  try {
    await loadCertificationDetail(row.id)
  } catch (error) {
    ElMessage.error(error.message || lt('加载资质档案失败', '載入資質檔案失敗', 'Failed to load certification profile'))
  } finally {
    certificationLoading.value = false
  }
}

const loadCertificationDetail = async (developerId) => {
  const [profileRes, reviewsRes] = await Promise.all([
    getDeveloperCertificationProfile(developerId),
    getDeveloperCertificationReviews(developerId)
  ])
  Object.assign(certificationProfile, {
    profileStatus: profileRes.data?.profileStatus || 'DRAFT',
    certificationStatus: profileRes.data?.certificationStatus || 'UNVERIFIED',
    subjectType: profileRes.data?.subjectType || '',
    subjectName: profileRes.data?.subjectName || '',
    legalRepresentative: profileRes.data?.legalRepresentative || '',
    contactName: profileRes.data?.contactName || '',
    contactPhone: profileRes.data?.contactPhone || '',
    businessLicenseNo: profileRes.data?.businessLicenseNo || '',
    idDocumentNo: profileRes.data?.idDocumentNo || '',
    certificateAssets: profileRes.data?.certificateAssets || [],
    note: profileRes.data?.note || '',
    rejectionReason: profileRes.data?.rejectionReason || '',
    submittedAt: profileRes.data?.submittedAt || '',
    reviewedAt: profileRes.data?.reviewedAt || ''
  })
  certificationReviewRows.value = reviewsRes.data || []
}

const submitCertificationReview = async () => {
  if (!certificationDeveloperId.value) return
  if (certificationReviewForm.decision === 'REJECTED' && certificationReviewForm.reason.trim().length < 2) {
    ElMessage.warning(lt('驳回原因至少 2 个字符', '駁回原因至少 2 個字元', 'Reject reason must be at least 2 characters'))
    return
  }
  try {
    await ElMessageBox.confirm(
      lt('确认提交资质审核结果？该操作会影响开发者主体认证状态。', '確認提交資質審核結果？此操作會影響開發者主體認證狀態。', 'Confirm submitting this certification review? This changes the developer certification status.'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { confirmButtonText: lt('确认提交', '確認提交', 'Confirm'), cancelButtonText: lt('取消', '取消', 'Cancel'), type: 'warning' }
    )
    certificationSaving.value = true
    await reviewDeveloperCertification(certificationDeveloperId.value, {
      decision: certificationReviewForm.decision,
      reason: certificationReviewForm.reason.trim()
    })
    ElMessage.success(lt('资质审核结果已提交', '資質審核結果已提交', 'Certification review submitted'))
    await Promise.all([
      loadDevelopers(),
      loadCertificationDetail(certificationDeveloperId.value)
    ])
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('提交资质审核失败', '提交資質審核失敗', 'Failed to submit certification review'))
    }
  } finally {
    certificationSaving.value = false
  }
}

const quickGovernance = async (row, status) => {
  const reason = status === 'ACTIVE'
    ? ''
    : await promptReason(row, status)
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

const submitGovernance = async () => {
  if (!governanceDeveloperId.value) return
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
    await updateDeveloperGovernance(governanceDeveloperId.value, {
      accountStatus: governanceForm.accountStatus,
      certificationStatus: governanceForm.certificationStatus,
      riskLevel: governanceForm.riskLevel,
      whitelistStatus: governanceForm.whitelistStatus,
      violationCount: governanceForm.violationCount,
      governanceTag: governanceForm.governanceTag.trim(),
      opsNote: governanceForm.opsNote.trim(),
      reason: governanceForm.reason.trim()
    })
    governanceVisible.value = false
    ElMessage.success(lt('开发者治理已更新', '開發者治理已更新', 'Developer governance updated'))
    await loadDevelopers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('更新开发者治理失败', '更新開發者治理失敗', 'Failed to update developer governance'))
    }
  } finally {
    saving.value = false
  }
}

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

function summarizeSnapshot(raw) {
  if (!raw) return '-'
  try {
    const json = JSON.parse(raw)
    return [
      `status:${json.accountStatus || '-'}`,
      `cert:${json.certificationStatus || '-'}`,
      `risk:${json.riskLevel || '-'}`,
      `white:${json.whitelistStatus || '-'}`,
      `viol:${json.violationCount ?? 0}`
    ].join(' | ')
  } catch {
    return raw
  }
}

onMounted(loadDevelopers)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 18px; }
.detail-actions { margin-top: 16px; display: flex; gap: 10px; flex-wrap: wrap; }
.summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
.summary-item { padding: 14px 16px; border-radius: 16px; background: #f8fafc; display: flex; flex-direction: column; gap: 6px; color: #475467; }
.summary-item strong { font-size: 20px; color: #101828; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.panel-title { font-size: 17px; font-weight: 800; color: #101828; }
.panel-subtitle { margin-top: 6px; color: #667085; font-size: 13px; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; }
.keyword-input { width: 260px; max-width: 100%; }
.filter-select { width: 180px; max-width: 100%; }
.filter-select-sm { width: 160px; }
.tag-list { display: flex; gap: 6px; flex-wrap: wrap; }
.history-head { margin-bottom: 12px; color: #344054; font-weight: 700; }
.summary-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.summary-item { padding: 12px 14px; border-radius: 14px; background: #f8fafc; display: flex; flex-direction: column; gap: 6px; color: #475467; font-size: 13px; }
.summary-item strong { font-size: 16px; color: #111827; }
.cert-grid { margin-bottom: 12px; }
.section-descriptions { margin-bottom: 14px; }
@media (max-width: 920px) {
  .head-row { flex-direction: column; }
  .actions { width: 100%; }
  .keyword-input,
  .filter-select,
  .filter-select-sm { width: 100%; }
}
</style>
