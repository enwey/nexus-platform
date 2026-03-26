<template>
  <div class="page-shell">
    <header class="page-header">
      <div>
        <h1>审计日志</h1>
        <p>查看审核动作的操作轨迹与结果。</p>
      </div>
      <div class="actions">
        <el-input-number v-model="limit" :min="10" :max="200" :step="10" />
        <el-button type="primary" @click="loadLogs">刷新</el-button>
        <el-button @click="$router.push('/audit')">返回审核台</el-button>
      </div>
    </header>

    <el-card>
      <el-table :data="logs" v-loading="loading" empty-text="暂无审计日志">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="createdAt" label="时间" min-width="180" />
        <el-table-column prop="action" label="动作" width="140" />
        <el-table-column prop="operatorId" label="操作人ID" width="120" />
        <el-table-column prop="operatorRole" label="角色" width="100" />
        <el-table-column prop="targetGameId" label="游戏ID" width="100" />
        <el-table-column prop="targetAppId" label="AppID" min-width="160" />
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'">
              {{ row.success ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="requestUri" label="请求路径" min-width="220" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuditLogs } from '../api'

const logs = ref([])
const loading = ref(false)
const limit = ref(50)

const loadLogs = async () => {
  loading.value = true
  try {
    const res = await getAuditLogs(limit.value)
    logs.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || '加载审计日志失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadLogs)
</script>

<style scoped>
.page-shell {
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-header h1 {
  margin: 0 0 8px;
}

.page-header p {
  margin: 0;
  color: #6b7280;
}

.actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>

