<template>
  <div class="pro-page">
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

      <el-table :data="logs" v-loading="loading" style="margin-top: 12px">
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
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getVerificationCodeLogs } from '../../api'
import { useI18nLite } from '../../i18n'

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
    ElMessage.error(error.message || lt('加載短信模塊失敗', '加載短信模塊失敗', 'Failed to load SMS module'))
  } finally {
    loading.value = false
  }
}

onMounted(loadLogs)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.head-row { display: flex; justify-content: space-between; align-items: center; }
.filters { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
</style>
