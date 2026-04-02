﻿<template>
  <div class="page-shell">
    <header class="page-header">
      <div>
        <h1>我的游戏</h1>
        <p>查看当前账号提交的游戏版本与审核状态。</p>
      </div>
      <div class="header-actions">
        <el-button @click="$router.push('/docs')">开发者文档</el-button>
        <el-button type="primary" @click="$router.push('/games/upload')">上传游戏</el-button>
      </div>
    </header>

    <el-card>
      <el-table :data="games" v-loading="loading" empty-text="暂无游戏数据">
        <el-table-column prop="name" label="游戏名称" min-width="180" />
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="version" label="版本" width="120" />
        <el-table-column prop="status" label="状态" width="140">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="200" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
            <el-button link type="info" size="small" @click="openVersionDialog(row)">版本</el-button>
            <el-button
              v-if="row.status === 'DRAFT' || row.status === 'REJECTED'"
              link
              type="success"
              size="small"
              @click="handleSubmit(row)"
            >
              提交审核
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="versionDialogVisible"
      width="860px"
      :title="selectedGame ? `${selectedGame.name} - 版本历史` : '版本历史'"
    >
      <el-table :data="versions" v-loading="versionLoading" empty-text="暂无版本数据">
        <el-table-column prop="versionName" label="版本号" width="120" />
        <el-table-column label="线上" width="100">
          <template #default="{ row }">
            <el-tag v-if="isCurrentOnlineVersion(row)" type="success" size="small">当前线上</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getVersionStatusType(row.status)">{{ getVersionStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="md5" label="MD5" min-width="240" show-overflow-tooltip />
        <el-table-column prop="submitNote" label="提审备注" min-width="160" show-overflow-tooltip />
        <el-table-column prop="auditReason" label="审核说明" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'DRAFT' || row.status === 'REJECTED'"
              link
              type="success"
              size="small"
              @click="handleSubmitVersion(row)"
            >
              提交审核
            </el-button>
            <el-button
              v-if="row.status === 'APPROVED' && !isCurrentOnlineVersion(row)"
              link
              type="warning"
              size="small"
              @click="handleRollbackVersion(row)"
            >
              回滚发布
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDeveloperGames,
  getGameVersions,
  rollbackGameVersion,
  submitGameForAudit,
  submitGameVersionForAudit
} from '../api'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const games = ref([])
const loading = ref(false)
const versionDialogVisible = ref(false)
const versionLoading = ref(false)
const selectedGame = ref(null)
const versions = ref([])

const statusTextMap = {
  PROCESSING: '处理中',
  DRAFT: '草稿',
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}

const statusTypeMap = {
  PROCESSING: 'warning',
  DRAFT: 'info',
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger'
}

const versionStatusTextMap = {
  PROCESSING: '处理中',
  DRAFT: '草稿',
  SUBMITTED: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}

const versionStatusTypeMap = {
  PROCESSING: 'warning',
  DRAFT: 'info',
  SUBMITTED: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger'
}

const loadGames = async () => {
  if (!userStore.user?.id) {
    games.value = []
    return
  }

  loading.value = true
  try {
    const res = await getDeveloperGames(userStore.user.id)
    games.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || '加载游戏失败')
  } finally {
    loading.value = false
  }
}

const handleView = (row) => {
  ElMessageBox.alert(
    `游戏名称：${row.name}\n描述：${row.description || '暂无'}\n版本：${row.version || '暂无'}\n状态：${getStatusText(row.status)}`,
    '游戏详情',
    { confirmButtonText: '知道了' }
  )
}

const handleSubmit = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('可选：填写本次提审备注', `提交审核：${row.name}`, {
      confirmButtonText: '提交',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：修复关键崩溃，补充隐私弹窗'
    })
    await submitGameForAudit(row.id, value || '')
    ElMessage.success('已提交审核')
    await loadGames()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '提交审核失败')
    }
  }
}

const openVersionDialog = async (row) => {
  selectedGame.value = row
  versionDialogVisible.value = true
  versionLoading.value = true
  versions.value = []
  try {
    const res = await getGameVersions(row.id)
    versions.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || '加载版本历史失败')
  } finally {
    versionLoading.value = false
  }
}

const handleSubmitVersion = async (versionRow) => {
  if (!selectedGame.value) return
  try {
    const { value } = await ElMessageBox.prompt('可选：填写本次提审备注', `提交版本 ${versionRow.versionName}`, {
      confirmButtonText: '提交',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：修复登录异常'
    })
    await submitGameVersionForAudit(selectedGame.value.id, versionRow.id, value || '')
    ElMessage.success('版本已提交审核')
    await openVersionDialog(selectedGame.value)
    await loadGames()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '版本提审失败')
    }
  }
}

const handleRollbackVersion = async (versionRow) => {
  if (!selectedGame.value) return
  try {
    const { value } = await ElMessageBox.prompt(
      `将线上版本回滚到 ${versionRow.versionName}，可选填写回滚原因`,
      '确认回滚发布',
      {
        confirmButtonText: '确认回滚',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：新版本出现稳定性问题'
      }
    )
    await rollbackGameVersion(selectedGame.value.id, versionRow.id, value || '')
    ElMessage.success('已回滚到指定版本并发布')
    await loadGames()
    const refreshed = games.value.find((game) => game.id === selectedGame.value.id)
    if (refreshed) {
      selectedGame.value = refreshed
    }
    await openVersionDialog(selectedGame.value)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '回滚发布失败')
    }
  }
}

const getStatusText = (status) => statusTextMap[status] || status || '未知'
const getStatusType = (status) => statusTypeMap[status] || 'info'
const getVersionStatusText = (status) => versionStatusTextMap[status] || status || '未知'
const getVersionStatusType = (status) => versionStatusTypeMap[status] || 'info'
const isCurrentOnlineVersion = (versionRow) =>
  !!selectedGame.value &&
  selectedGame.value.status === 'APPROVED' &&
  selectedGame.value.version === versionRow.versionName

onMounted(loadGames)
</script>

<style scoped>
.page-shell {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0 0 8px;
}

.page-header p {
  margin: 0;
  color: #6b7280;
}

.header-actions {
  display: flex;
  gap: 8px;
}
</style>
