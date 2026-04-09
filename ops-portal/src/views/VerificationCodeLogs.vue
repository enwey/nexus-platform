<template>
  <div class="page-shell">
    <header class="page-header">
      <div>
        <h1>{{ lt('验证码记录', '驗證碼記錄', 'Verification Code Logs') }}</h1>
        <p>{{ lt('查看验证码由誰申請、在哪裡申請，以及用途與驗證碼內容。', '查看驗證碼由誰申請、在哪裡申請，以及用途與驗證碼內容。', 'Track who requested verification codes, where they were requested, and their purpose.') }}</p>
      </div>
      <div class="actions">
        <el-button type="primary" @click="loadLogs">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
        <el-button @click="$router.push('/audit/logs')">{{ lt('审计日志', '審計日誌', 'Audit Logs') }}</el-button>
        <el-button @click="$router.push('/audit')">{{ lt('返回审核台', '返回審核台', 'Back to Review') }}</el-button>
      </div>
    </header>

    <el-card class="filter-card">
      <div class="filters">
        <el-input v-model.trim="filters.email" clearable style="width: 240px" :placeholder="lt('邮箱（精确匹配）', '郵箱（精確匹配）', 'Email (exact)')" />
        <el-select v-model="filters.purpose" clearable style="width: 180px" :placeholder="lt('用途', '用途', 'Purpose')">
          <el-option label="REGISTER" value="REGISTER" />
          <el-option label="RESET_PASSWORD" value="RESET_PASSWORD" />
          <el-option label="CHANGE_PASSWORD" value="CHANGE_PASSWORD" />
        </el-select>
        <el-input v-model.trim="filters.source" clearable style="width: 180px" :placeholder="lt('来源端', '來源端', 'Source')" />
        <el-input-number v-model="filters.limit" :min="20" :max="500" :step="20" />
        <el-button type="primary" @click="loadLogs">{{ lt('查询', '查詢', 'Query') }}</el-button>
      </div>
    </el-card>

    <el-card>
      <el-table :data="logs" v-loading="loading" :empty-text="lt('暂无验证码记录', '暫無驗證碼記錄', 'No verification code logs')">
        <el-table-column prop="createdAt" :label="lt('时间', '時間', 'Time')" min-width="172" />
        <el-table-column prop="account" :label="lt('邮箱', '郵箱', 'Email')" min-width="220" />
        <el-table-column prop="purpose" :label="lt('用途', '用途', 'Purpose')" width="170" />
        <el-table-column prop="debugCode" :label="lt('验证码', '驗證碼', 'Code')" width="120" />
        <el-table-column :label="lt('状态', '狀態', 'Status')" width="110">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'">
              {{ row.success ? lt('成功', '成功', 'Success') : lt('失败', '失敗', 'Failed') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestSource" :label="lt('来源端', '來源端', 'Source')" min-width="130" />
        <el-table-column prop="requestScene" :label="lt('来源场景', '來源場景', 'Scene')" min-width="170" />
        <el-table-column prop="requestIp" :label="lt('来源IP', '來源 IP', 'Source IP')" min-width="130" />
        <el-table-column prop="requestUri" :label="lt('请求路径', '請求路徑', 'Request URI')" min-width="170" show-overflow-tooltip />
        <el-table-column prop="requesterUserId" :label="lt('请求用户ID', '請求用戶ID', 'Requester ID')" width="120" />
        <el-table-column prop="requesterRole" :label="lt('请求角色', '請求角色', 'Requester Role')" width="120" />
        <el-table-column prop="failureReason" :label="lt('失败原因', '失敗原因', 'Failure Reason')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="userAgent" label="User-Agent" min-width="240" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getVerificationCodeLogs } from '../api'
import { useI18nLite } from '../i18n'

const { lt } = useI18nLite()
const loading = ref(false)
const logs = ref([])
const filters = reactive({
  email: '',
  purpose: '',
  source: '',
  limit: 100
})

const loadLogs = async () => {
  loading.value = true
  try {
    const res = await getVerificationCodeLogs({
      limit: filters.limit,
      email: filters.email || undefined,
      purpose: filters.purpose || undefined,
      source: filters.source || undefined
    })
    logs.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || lt('加载验证码记录失败', '載入驗證碼記錄失敗', 'Failed to load verification code logs'))
  } finally {
    loading.value = false
  }
}

onMounted(loadLogs)
</script>

<style scoped>
.page-shell { padding: 24px; }
.page-header { margin-bottom: 24px; display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.page-header h1 { margin: 0 0 8px; }
.page-header p { margin: 0; color: #6b7280; }
.actions { display: flex; gap: 8px; align-items: center; }
.filter-card { margin-bottom: 16px; }
.filters { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
</style>
