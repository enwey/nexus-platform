<template>
  <div class="page-shell">
    <header class="page-header">
      <div>
        <h1>{{ lt('审计日志', '審計日誌', 'Audit Logs') }}</h1>
        <p>{{ lt('查看审核动作的操作轨迹与结果。', '查看審核動作的操作軌跡與結果。', 'View operation traces and outcomes of review actions.') }}</p>
      </div>
      <div class="actions">
        <el-input-number v-model="limit" :min="10" :max="200" :step="10" />
        <el-button type="primary" @click="loadLogs">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
        <el-button @click="$router.push('/runtime-ops')">Runtime Ops Console</el-button>
        <el-button @click="$router.push('/audit')">{{ lt('返回审核台', '返回審核台', 'Back to Review') }}</el-button>
      </div>
    </header>

    <el-card>
      <el-table :data="logs" v-loading="loading" :empty-text="lt('暂无审计日志', '暫無審計日誌', 'No audit logs')">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="createdAt" :label="lt('时间', '時間', 'Time')" min-width="180" />
        <el-table-column prop="action" :label="lt('动作', '動作', 'Action')" width="140" />
        <el-table-column prop="operatorId" :label="lt('操作人ID', '操作人ID', 'Operator ID')" width="120" />
        <el-table-column prop="operatorRole" :label="lt('角色', '角色', 'Role')" width="100" />
        <el-table-column prop="targetGameId" :label="lt('游戏ID', '遊戲ID', 'Game ID')" width="100" />
        <el-table-column prop="targetAppId" label="AppID" min-width="160" />
        <el-table-column :label="lt('结果', '結果', 'Result')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? lt('成功', '成功', 'Success') : lt('失败', '失敗', 'Failed') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" :label="lt('原因', '原因', 'Reason')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="requestUri" :label="lt('请求路径', '請求路徑', 'Request URI')" min-width="220" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuditLogs } from '../api'
import { useI18nLite } from '../i18n'

const logs = ref([])
const loading = ref(false)
const limit = ref(50)
const { lt } = useI18nLite()

const loadLogs = async () => {
  loading.value = true
  try {
    const res = await getAuditLogs(limit.value)
    logs.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || lt('加载审计日志失败', '載入審計日誌失敗', 'Failed to load audit logs'))
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
</style>
