<template>
  <div class="pro-page">
    <el-card class="panel-card" v-loading="loading">
      <template #header>
        <div class="head-row">
          <div>
            <div class="breadcrumb-line">
              <el-button link @click="router.push('/pro/review-center/certifications/list')">{{ lt('返回资质审核列表', '返回資質審核列表', 'Back to Certification Reviews') }}</el-button>
              <span>/</span>
              <span>{{ developerLabel || lt('资质审核详情', '資質審核詳情', 'Certification Review Detail') }}</span>
            </div>
            <div class="panel-title">{{ developerLabel || '-' }}</div>
            <div class="panel-subtitle">{{ profile.subjectName || '-' }}</div>
          </div>
          <div class="actions">
            <el-button @click="openDeveloper">{{ lt('查看开发者详情', '查看開發者詳情', 'Open Developer Detail') }}</el-button>
            <el-button :loading="loading" @click="loadDetail">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <div class="summary-grid">
        <div class="summary-item">
          <span>{{ lt('档案状态', '檔案狀態', 'Profile Status') }}</span>
          <strong><el-tag :type="profileStatusTagType(profile.profileStatus)">{{ profileStatusText(profile.profileStatus) }}</el-tag></strong>
        </div>
        <div class="summary-item">
          <span>{{ lt('资质状态', '資質狀態', 'Certification') }}</span>
          <strong><el-tag :type="certificationTagType(profile.certificationStatus)">{{ certificationText(profile.certificationStatus) }}</el-tag></strong>
        </div>
        <div class="summary-item">
          <span>{{ lt('最近提交', '最近提交', 'Submitted') }}</span>
          <strong>{{ formatDate(profile.submittedAt) }}</strong>
        </div>
      </div>

      <el-descriptions :column="2" border class="section">
        <el-descriptions-item :label="lt('主体类型', '主體類型', 'Subject Type')">{{ subjectTypeText(profile.subjectType) }}</el-descriptions-item>
        <el-descriptions-item :label="lt('主体名称', '主體名稱', 'Subject Name')">{{ profile.subjectName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="lt('法人/负责人', '法人/負責人', 'Legal Representative')">{{ profile.legalRepresentative || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="lt('联系人', '聯絡人', 'Contact Name')">{{ profile.contactName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="lt('联系电话', '聯絡電話', 'Contact Phone')">{{ profile.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="lt('营业执照/证件号', '營業執照/證件號', 'License / ID No')">{{ profile.businessLicenseNo || profile.idDocumentNo || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="lt('开发者备注', '開發者備註', 'Developer Note')" :span="2">{{ profile.note || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="lt('驳回原因', '駁回原因', 'Reject Reason')" :span="2">{{ profile.rejectionReason || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-card shadow="never" class="section">
        <template #header>{{ lt('资质材料链接', '資質材料連結', 'Certificate Assets') }}</template>
        <div class="tag-list">
          <el-link v-for="item in profile.certificateAssets" :key="item" :href="item" target="_blank" type="primary">{{ item }}</el-link>
          <span v-if="!profile.certificateAssets.length">-</span>
        </div>
      </el-card>

      <el-form :model="reviewForm" label-width="120px" class="section">
        <el-form-item :label="lt('审核动作', '審核動作', 'Review Decision')">
          <el-select v-model="reviewForm.decision" style="width: 100%">
            <el-option :label="lt('认证通过', '認證通過', 'Verify')" value="VERIFIED" />
            <el-option :label="lt('驳回申请', '駁回申請', 'Reject')" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('审核说明', '審核說明', 'Review Note')">
          <el-input v-model="reviewForm.reason" type="textarea" :rows="3" maxlength="256" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="saving"
            :disabled="profile.profileStatus !== 'PENDING'"
            @click="submitReview"
          >
            {{ lt('提交审核结果', '提交審核結果', 'Submit Review Result') }}
          </el-button>
        </el-form-item>
      </el-form>

      <el-table :data="reviewRows" :empty-text="lt('暂无资质审核记录', '暫無資質審核記錄', 'No certification review records')">
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
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeveloperAccounts, getDeveloperCertificationProfile, getDeveloperCertificationReviews, reviewDeveloperCertification } from '../../api'
import { useI18nLite } from '../../i18n'
import { formatDate } from './entityPageShared'

const props = defineProps({
  reviewId: {
    type: [String, Number],
    required: true
  }
})

const router = useRouter()
const { lt } = useI18nLite()

const loading = ref(false)
const saving = ref(false)
const developerId = ref(null)
const developerLabel = ref('')
const reviewRows = ref([])
const profile = reactive({
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
const reviewForm = reactive({
  decision: 'VERIFIED',
  reason: ''
})

const loadDetail = async () => {
  loading.value = true
  try {
    developerId.value = Number(props.reviewId)
    const [accountsRes, profileRes, reviewsRes] = await Promise.all([
      getDeveloperAccounts(),
      getDeveloperCertificationProfile(developerId.value),
      getDeveloperCertificationReviews(developerId.value)
    ])
    const account = (accountsRes.data || []).find((item) => String(item.id) === String(developerId.value))
    developerLabel.value = account ? `${account.username} / ${account.email || '-'}` : String(developerId.value)
    Object.assign(profile, {
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
    reviewRows.value = reviewsRes.data || []
  } catch (error) {
    ElMessage.error(error.message || lt('加载资质审核详情失败', '載入資質審核詳情失敗', 'Failed to load certification review detail'))
  } finally {
    loading.value = false
  }
}

const submitReview = async () => {
  if (!developerId.value) return
  if (reviewForm.decision === 'REJECTED' && reviewForm.reason.trim().length < 2) {
    ElMessage.warning(lt('驳回原因至少 2 个字符', '駁回原因至少 2 個字元', 'Reject reason must be at least 2 characters'))
    return
  }
  try {
    await ElMessageBox.confirm(
      lt('确认提交资质审核结果？该操作会影响开发者主体认证状态。', '確認提交資質審核結果？此操作會影響開發者主體認證狀態。', 'Confirm submitting this certification review? This changes the developer certification status.'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { confirmButtonText: lt('确认提交', '確認提交', 'Confirm'), cancelButtonText: lt('取消', '取消', 'Cancel'), type: 'warning' }
    )
    saving.value = true
    await reviewDeveloperCertification(developerId.value, {
      decision: reviewForm.decision,
      reason: reviewForm.reason.trim()
    })
    ElMessage.success(lt('资质审核结果已提交', '資質審核結果已提交', 'Certification review submitted'))
    await loadDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('提交资质审核失败', '提交資質審核失敗', 'Failed to submit certification review'))
    }
  } finally {
    saving.value = false
  }
}

const openDeveloper = () => {
  if (developerId.value) {
    router.push({ name: 'OpsDeveloperDetail', params: { developerId: developerId.value } })
  }
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
.summary-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.summary-item { padding: 12px 14px; border-radius: 14px; background: #f8fafc; display: flex; flex-direction: column; gap: 6px; color: #475467; font-size: 13px; }
.summary-item strong { font-size: 16px; color: #111827; }
.section { margin-top: 16px; }
.tag-list { display: flex; gap: 6px; flex-wrap: wrap; }
@media (max-width: 920px) {
  .head-row { flex-direction: column; }
  .summary-grid { grid-template-columns: 1fr; }
}
</style>
