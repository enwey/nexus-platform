<template>
  <div class="pro-page">
    <el-alert v-if="loadError" type="error" :closable="false" :title="loadError" />

    <div class="summary-grid">
      <div class="summary-item"><span>{{ lt('日志总数', '日誌總數', 'Logs') }}</span><strong>{{ logs.length }}</strong></div>
      <div class="summary-item"><span>{{ lt('发送成功', '發送成功', 'Success') }}</span><strong>{{ successCount }}</strong></div>
      <div class="summary-item"><span>{{ lt('发送失败', '發送失敗', 'Failed') }}</span><strong>{{ failedCount }}</strong></div>
      <div class="summary-item"><span>{{ lt('注册用途', '註冊用途', 'Register') }}</span><strong>{{ registerCount }}</strong></div>
    </div>

    <el-card>
      <template #header>
        <div class="head-row">
          <span>{{ lt('短信模塊', '短信模塊', 'SMS Module') }}</span>
          <el-button type="primary" @click="loadLogs">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
        </div>
      </template>

      <div class="filters">
        <el-input
          v-model.trim="filters.email"
          clearable
          style="width: 240px"
          :placeholder="lt('郵箱', '郵箱', 'Email')"
        />
        <el-select
          v-model="filters.purpose"
          clearable
          style="width: 180px"
          :placeholder="lt('用途', '用途', 'Purpose')"
        >
          <el-option label="REGISTER" value="REGISTER" />
          <el-option label="RESET_PASSWORD" value="RESET_PASSWORD" />
          <el-option label="CHANGE_PASSWORD" value="CHANGE_PASSWORD" />
        </el-select>
        <el-input
          v-model.trim="filters.source"
          clearable
          style="width: 160px"
          :placeholder="lt('來源端', '來源端', 'Source')"
        />
        <el-input-number v-model="filters.limit" :min="20" :max="500" :step="20" />
        <el-button @click="loadLogs">{{ lt('查詢', '查詢', 'Query') }}</el-button>
      </div>

      <el-table :data="logs" v-loading="loading" style="margin-top: 12px" :empty-text="lt('暂无验证码日志', '暫無驗證碼日誌', 'No verification logs')" @row-click="openDetail">
        <el-table-column prop="createdAt" :label="lt('時間', '時間', 'Time')" min-width="170" />
        <el-table-column prop="account" :label="lt('郵箱', '郵箱', 'Email')" min-width="220" />
        <el-table-column prop="purpose" :label="lt('用途', '用途', 'Purpose')" width="150" />
        <el-table-column prop="debugCode" :label="lt('驗證碼', '驗證碼', 'Code')" width="110" />
        <el-table-column :label="lt('狀態', '狀態', 'Status')" width="110">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'">
              {{ row.success ? lt('成功', '成功', 'Success') : lt('失敗', '失敗', 'Failed') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestSource" :label="lt('來源端', '來源端', 'Source')" width="120" />
        <el-table-column prop="requestIp" label="IP" width="130" />
        <el-table-column prop="failureReason" :label="lt('失敗原因', '失敗原因', 'Failure Reason')" min-width="200" show-overflow-tooltip />
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="100">
          <template #default="{ row }"><el-button link @click.stop="openDetail(row)">{{ lt('详情', '詳情', 'Detail') }}</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="detailVisible" :title="lt('验证码日志详情', '驗證碼日誌詳情', 'Verification Log Detail')" size="36%">
      <template v-if="detailRow">
        <el-descriptions :column="1" border>
          <el-descriptions-item :label="lt('时间', '時間', 'Time')">{{ detailRow.createdAt || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('邮箱', '郵箱', 'Email')">{{ detailRow.account || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('用途', '用途', 'Purpose')">{{ detailRow.purpose || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('验证码', '驗證碼', 'Code')">{{ detailRow.debugCode || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('状态', '狀態', 'Status')">{{ detailRow.success ? lt('成功', '成功', 'Success') : lt('失败', '失敗', 'Failed') }}</el-descriptions-item>
          <el-descriptions-item :label="lt('来源端', '來源端', 'Source')">{{ detailRow.requestSource || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('请求 IP', '請求 IP', 'Request IP')">{{ detailRow.requestIp || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('失败原因', '失敗原因', 'Failure Reason')">{{ detailRow.failureReason || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getVerificationCodeLogs } from '../../api'
import { useI18nLite } from '../../i18n'

const { lt } = useI18nLite()
const loading = ref(false)
const loadError = ref('')
const logs = ref([])
const detailVisible = ref(false)
const detailRow = ref(null)
const filters = reactive({
  email: '',
  purpose: '',
  source: '',
  limit: 100
})

const loadLogs = async () => {
  loading.value = true
  try {
    loadError.value = ''
    const res = await getVerificationCodeLogs({
      limit: filters.limit,
      email: filters.email || undefined,
      purpose: filters.purpose || undefined,
      source: filters.source || undefined
    })
    logs.value = res.data || []
  } catch (error) {
    loadError.value = error.message || lt('加載短信模塊失敗', '加載短信模塊失敗', 'Failed to load SMS module')
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

const successCount = computed(() => logs.value.filter((item) => item.success).length)
const failedCount = computed(() => logs.value.filter((item) => !item.success).length)
const registerCount = computed(() => logs.value.filter((item) => item.purpose === 'REGISTER').length)

const openDetail = (row) => {
  detailRow.value = row
  detailVisible.value = true
}

onMounted(loadLogs)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.head-row { display: flex; justify-content: space-between; align-items: center; }
.filters { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
.summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
.summary-item { padding: 14px 16px; border-radius: 16px; background: #f8fafc; display: flex; flex-direction: column; gap: 6px; color: #475467; }
.summary-item strong { font-size: 18px; color: #101828; }
</style>
