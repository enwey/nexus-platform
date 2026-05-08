<template>
  <div class="release-page">
    <el-row :gutter="16">
      <el-col :xs="24" :xl="9">
        <el-card class="panel-card">
          <template #header>
            <div class="panel-head">
              <div>
                <div class="panel-title">{{ lt('创建游戏并上传首包', '建立遊戲並上傳首包', 'Create Game & Upload First Package') }}</div>
                <div class="panel-subtitle">{{ lt('当前后端上传接口会创建一个新游戏条目与首个版本。', '目前後端上傳介面會建立新遊戲條目與首個版本。', 'The current upload API creates a new game entry with its first version.') }}</div>
              </div>
            </div>
          </template>

          <el-alert
            :title="lt('上传规则：仅支持 ZIP，文件名不超过 128 字，包体不超过 100MB，且必须包含可通过 manifest 校验的入口文件。', '上傳規則：僅支援 ZIP，檔名不超過 128 字，包體不超過 100MB，且必須包含可通過 manifest 校驗的入口檔案。', 'Upload rules: ZIP only, filename up to 128 chars, package up to 100MB, and a manifest-valid entry file is required.')"
            type="info"
            :closable="false"
            show-icon
            class="upload-alert"
          />

          <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-position="top">
            <el-form-item :label="lt('游戏名称', '遊戲名稱', 'Game Name')" prop="name">
              <el-input v-model="uploadForm.name" :placeholder="lt('请输入游戏名称', '請輸入遊戲名稱', 'Enter game name')" />
            </el-form-item>
            <el-form-item :label="lt('版本说明', '版本說明', 'Description')" prop="description">
              <el-input v-model="uploadForm.description" type="textarea" :rows="4" :placeholder="lt('描述本次上传的核心玩法、更新点或审核说明', '描述本次上傳的核心玩法、更新點或審核說明', 'Describe the game, release highlights, or review note')" />
            </el-form-item>
            <el-form-item :label="lt('ZIP 包', 'ZIP 包', 'ZIP Package')" prop="file">
              <el-upload
                :auto-upload="false"
                :show-file-list="true"
                :limit="1"
                accept=".zip"
                :on-change="handleFileChange"
                :on-remove="handleFileRemove"
              >
                <el-button>{{ lt('选择 ZIP 文件', '選擇 ZIP 檔案', 'Choose ZIP File') }}</el-button>
              </el-upload>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="uploading" @click="handleUpload">{{ lt('上传并创建游戏', '上傳並建立遊戲', 'Upload & Create Game') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="panel-card task-card">
          <template #header>
            <div class="panel-head">
              <div>
                <div class="panel-title">{{ lt('上传异步任务面板', '上傳非同步任務面板', 'Async Upload Tasks') }}</div>
                <div class="panel-subtitle">{{ lt('追踪包体处理结果，并对失败任务执行重试。', '追蹤包體處理結果，並對失敗任務執行重試。', 'Track package processing and retry failed tasks.') }}</div>
              </div>
            </div>
          </template>

          <el-alert
            v-if="taskError"
            :title="taskError"
            type="error"
            :closable="false"
            show-icon
            class="task-alert"
          />

          <el-skeleton v-if="tasksLoading" :rows="4" animated />
          <el-empty
            v-else-if="!uploadTasks.length"
            :description="lt('还没有上传任务，创建首包后会在这里持续显示处理状态。', '還沒有上傳任務，建立首包後會在這裡持續顯示處理狀態。', 'No upload task yet. Your first package will appear here once uploaded.')"
          />
          <el-table v-else :data="uploadTasks">
            <el-table-column prop="gameName" :label="lt('游戏', '遊戲', 'Game')" min-width="160" />
            <el-table-column prop="originalFilename" :label="lt('文件名', '檔名', 'File')" min-width="160" show-overflow-tooltip />
            <el-table-column :label="lt('任务状态', '任務狀態', 'Task')" width="120">
              <template #default="{ row }">
                <el-tag :type="getTaskStatusMeta(row.taskStatus).type">{{ getTaskStatusText(row.taskStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="latestVersion" :label="lt('版本', '版本', 'Version')" width="120" />
            <el-table-column prop="failureReason" :label="lt('失败原因', '失敗原因', 'Failure Reason')" min-width="180" show-overflow-tooltip />
            <el-table-column :label="lt('重试次数', '重試次數', 'Retries')" width="100">
              <template #default="{ row }">{{ row.retryCount || 0 }}</template>
            </el-table-column>
            <el-table-column :label="lt('更新时间', '更新時間', 'Updated')" min-width="170">
              <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="120" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.taskStatus === 'FAILED'"
                  type="danger"
                  link
                  :loading="retryingTaskId === row.gameId"
                  @click="handleRetryTask(row)"
                >
                  {{ lt('失败重试', '失敗重試', 'Retry') }}
                </el-button>
                <span v-else class="muted-action">{{ lt('处理中或已完成', '處理中或已完成', 'No action needed') }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="15">
        <el-card class="panel-card">
          <template #header>
            <div class="panel-head">
              <div>
                <div class="panel-title">{{ lt('版本管理', '版本管理', 'Version Management') }}</div>
                <div class="panel-subtitle">{{ lt('查看历史版本、提交审核与回滚。', '查看歷史版本、提交審核與回滾。', 'Review version history, submit for audit, and roll back.') }}</div>
              </div>
              <el-select v-model="activeGameId" filterable clearable style="width: 220px" @change="handleGameChange">
                <el-option v-for="game in games" :key="game.id" :label="game.name" :value="game.id" />
              </el-select>
            </div>
          </template>

          <div v-if="!activeGameId" class="empty-block">
            {{ lt('请先选择一个游戏查看版本记录。', '請先選擇一個遊戲查看版本記錄。', 'Select a game to view its version history.') }}
          </div>
          <template v-else>
            <div class="version-summary">
              <el-tag>{{ lt('总版本', '總版本', 'Total') }} {{ versionHealth.total }}</el-tag>
              <el-tag type="success">{{ lt('已通过', '已通過', 'Approved') }} {{ versionHealth.approved }}</el-tag>
              <el-tag type="warning">{{ lt('提审中', '提審中', 'Submitted') }} {{ versionHealth.submitted }}</el-tag>
              <el-tag type="danger">{{ lt('被驳回', '被駁回', 'Rejected') }} {{ versionHealth.rejected }}</el-tag>
            </div>

            <el-alert
              v-if="activeReleaseHealth"
              :title="healthHeadline"
              :description="healthDescription"
              :type="activeReleaseHealth.canSubmit ? 'success' : 'warning'"
              :closable="false"
              show-icon
              class="health-alert"
            />

            <el-table :data="versions" v-loading="versionsLoading">
              <el-table-column prop="versionName" :label="lt('版本号', '版本號', 'Version')" min-width="120" />
              <el-table-column :label="lt('状态', '狀態', 'Status')" width="120">
                <template #default="{ row }">
                  <el-tag :type="getVersionStatusMeta(row.status).type">{{ getVersionStatusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column :label="lt('强更', '強更', 'Forced')" width="100">
                <template #default="{ row }">{{ row.forcedUpdate ? lt('是', '是', 'Yes') : lt('否', '否', 'No') }}</template>
              </el-table-column>
              <el-table-column prop="submitNote" :label="lt('提审说明', '提審說明', 'Submit Note')" min-width="180" show-overflow-tooltip />
              <el-table-column prop="auditReason" :label="lt('审核反馈', '審核回饋', 'Review Feedback')" min-width="180" show-overflow-tooltip />
              <el-table-column :label="lt('更新时间', '更新時間', 'Updated At')" min-width="170">
                <template #default="{ row }">{{ formatDate(row.updatedAt || row.createdAt) }}</template>
              </el-table-column>
              <el-table-column :label="lt('操作', '操作', 'Actions')" min-width="220" fixed="right">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button
                      v-if="['DRAFT', 'REJECTED'].includes(row.status)"
                      type="info"
                      link
                      @click="handleInspectPreflight(row)"
                    >
                      {{ lt('发布前检查', '發佈前檢查', 'Preflight') }}
                    </el-button>
                    <el-button
                      v-if="['DRAFT', 'REJECTED'].includes(row.status)"
                      type="primary"
                      link
                      @click="handleSubmitVersion(row)"
                    >
                      {{ lt('提交审核', '提交審核', 'Submit') }}
                    </el-button>
                    <el-button
                      v-if="row.status === 'APPROVED'"
                      type="warning"
                      link
                      @click="handleRollbackVersion(row)"
                    >
                      {{ lt('回滚为线上版本', '回滾為線上版本', 'Rollback Online') }}
                    </el-button>
                    <el-button
                      v-if="row.status === 'REJECTED'"
                      type="danger"
                      link
                      @click="handleCreateAppeal(row)"
                    >
                      {{ lt('提交申诉', '提交申訴', 'Appeal') }}
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>

            <el-divider />

            <div class="panel-title minor-title">{{ lt('审核申诉记录', '審核申訴記錄', 'Review Appeals') }}</div>
            <el-table :data="activeAppeals" :empty-text="lt('暂无申诉记录', '暫無申訴記錄', 'No appeals yet')">
              <el-table-column prop="versionName" :label="lt('版本号', '版本號', 'Version')" width="120" />
              <el-table-column prop="appealStatus" :label="lt('申诉状态', '申訴狀態', 'Appeal Status')" width="120" />
              <el-table-column prop="appealReason" :label="lt('申诉原因', '申訴原因', 'Appeal Reason')" min-width="180" show-overflow-tooltip />
              <el-table-column prop="reviewNote" :label="lt('复审反馈', '複審回饋', 'Review Note')" min-width="180" show-overflow-tooltip />
              <el-table-column :label="lt('提交时间', '提交時間', 'Submitted')" min-width="170">
                <template #default="{ row }">{{ formatDate(row.submittedAt) }}</template>
              </el-table-column>
            </el-table>
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>

  <el-dialog
    v-model="preflightVisible"
    :title="lt('发布前检查', '發佈前檢查', 'Release Preflight')"
    width="760px"
  >
    <template v-if="preflightRow">
      <div class="preflight-head">
        <el-tag :type="preflightRow.ready ? 'success' : 'danger'">
          {{ preflightRow.ready ? lt('可以提审', '可以提審', 'Ready to Submit') : lt('存在阻塞项', '存在阻塞項', 'Blocked') }}
        </el-tag>
        <span class="preflight-version">{{ preflightRow.versionName || '-' }}</span>
      </div>
      <el-alert
        :title="preflightRow.blockingReason || lt('当前版本已通过所有检查。', '目前版本已通過所有檢查。', 'The current version passed all checks.')"
        :type="preflightRow.ready ? 'success' : 'warning'"
        :closable="false"
        show-icon
        class="health-alert"
      />
      <el-table :data="preflightRow.items || []" size="small">
        <el-table-column :label="lt('检查项', '檢查項', 'Check')" min-width="200">
          <template #default="{ row }">{{ row.label }}</template>
        </el-table-column>
        <el-table-column :label="lt('结果', '結果', 'Result')" width="120">
          <template #default="{ row }">
            <el-tag :type="row.passed ? 'success' : (row.blocking ? 'danger' : 'warning')">
              {{ row.passed ? lt('通过', '通過', 'Passed') : lt('未通过', '未通過', 'Failed') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detail" :label="lt('说明', '說明', 'Detail')" min-width="300" />
      </el-table>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createReviewAppeal, getDeveloperGames, getDeveloperReleaseHealth, getDeveloperUploadTasks, getDeveloperVersionPreflight, getGameVersions, getMyReviewAppeals, retryDeveloperUploadTask, rollbackVersion, submitGameVersion, uploadGame } from '../api'
import { useI18nLite } from '../i18n'
import { useUserStore } from '../stores/user'
import { formatDate, getGameStatusMeta, summarizeVersionHealth } from '../utils/portal'

const { lt } = useI18nLite()
const userStore = useUserStore()
const uploadFormRef = ref()
const uploading = ref(false)
const versionsLoading = ref(false)
const games = ref([])
const versions = ref([])
const appeals = ref([])
const uploadTasks = ref([])
const activeGameId = ref(null)
const releaseHealthMap = ref({})
const tasksLoading = ref(false)
const taskError = ref('')
const retryingTaskId = ref(null)
const preflightVisible = ref(false)
const preflightRow = ref(null)

const uploadForm = reactive({
  name: '',
  description: '',
  file: null
})

const uploadRules = {
  name: [{ required: true, message: lt('请输入游戏名称', '請輸入遊戲名稱', 'Enter game name'), trigger: 'blur' }],
  description: [{ required: true, message: lt('请输入版本说明', '請輸入版本說明', 'Enter description'), trigger: 'blur' }],
  file: [{ required: true, message: lt('请选择 ZIP 包', '請選擇 ZIP 包', 'Choose a ZIP package'), trigger: 'change' }]
}

const versionHealth = computed(() => summarizeVersionHealth(versions.value))
const activeReleaseHealth = computed(() => releaseHealthMap.value[activeGameId.value] || null)
const activeAppeals = computed(() => appeals.value.filter((item) => item.gameId === activeGameId.value))
const healthHeadline = computed(() => {
  const row = activeReleaseHealth.value
  if (!row) return ''
  if (row.canSubmit) {
    return lt('当前版本满足提审条件', '目前版本滿足提審條件', 'Current version is ready for review')
  }
  if (row.blockingReason) {
    return lt('当前版本存在发布阻塞', '目前版本存在發布阻塞', 'Current version has release blockers')
  }
  return lt('请继续完善版本信息', '請繼續完善版本資訊', 'Keep improving release readiness')
})
const healthDescription = computed(() => {
  const row = activeReleaseHealth.value
  if (!row) return ''
  if (row.canSubmit) {
    return lt('Manifest 校验通过，且当前没有其他版本占用审核队列，可以直接提交审核。', 'Manifest 校驗通過，且目前沒有其他版本佔用審核佇列，可以直接提交審核。', 'Manifest validation passed and no other version is occupying the review queue, so you can submit now.')
  }
  return row.blockingReason || lt('当前还不满足提审条件，请先处理阻塞项。', '目前還不滿足提審條件，請先處理阻塞項。', 'This release is not ready yet. Resolve the blockers first.')
})

const loadGames = async () => {
  const developerId = userStore.user?.id
  if (!developerId) return
  const [res, healthRes] = await Promise.all([
    getDeveloperGames(developerId),
    getDeveloperReleaseHealth(developerId)
  ])
  games.value = res.data || []
  releaseHealthMap.value = Object.fromEntries((healthRes.data?.games || []).map(item => [item.gameId, item]))
  const appealRes = await getMyReviewAppeals()
  appeals.value = appealRes.data || []
  if (!activeGameId.value && games.value.length) {
    activeGameId.value = games.value[0].id
    await loadVersions(activeGameId.value)
  }
}

const loadUploadTasks = async () => {
  const developerId = userStore.user?.id
  if (!developerId) return
  tasksLoading.value = true
  taskError.value = ''
  try {
    const res = await getDeveloperUploadTasks(developerId)
    uploadTasks.value = res.data || []
  } catch (error) {
    taskError.value = error?.message || lt('加载上传任务失败', '載入上傳任務失敗', 'Failed to load upload tasks')
  } finally {
    tasksLoading.value = false
  }
}

const loadVersions = async (gameId) => {
  if (!gameId) {
    versions.value = []
    return
  }

  versionsLoading.value = true
  try {
    const res = await getGameVersions(gameId)
    versions.value = (res.data || []).slice().sort((a, b) => new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt))
  } finally {
    versionsLoading.value = false
  }
}

const handleGameChange = async (gameId) => {
  await loadVersions(gameId)
}

const handleFileChange = (file) => {
  uploadForm.file = file.raw
}

const handleFileRemove = () => {
  uploadForm.file = null
}

const handleUpload = async () => {
  try {
    await uploadFormRef.value.validate()
    const payload = new FormData()
    payload.append('name', uploadForm.name)
    payload.append('description', uploadForm.description)
    payload.append('file', uploadForm.file)

    uploading.value = true
    await uploadGame(payload)
    ElMessage.success(lt('上传成功，正在生成首个版本', '上傳成功，正在建立首個版本', 'Upload successful. First version is being processed.'))
    uploadForm.name = ''
    uploadForm.description = ''
    uploadForm.file = null
    uploadFormRef.value.resetFields()
    await Promise.all([loadGames(), loadUploadTasks()])
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    uploading.value = false
  }
}

const handleRetryTask = async (row) => {
  try {
    await ElMessageBox.confirm(
      lt('系统会重新消费原始 ZIP 包，并覆盖当前失败任务状态。', '系統會重新處理原始 ZIP 包，並覆蓋目前失敗任務狀態。', 'The original ZIP package will be processed again and the failed task state will be replaced.'),
      lt('确认重试', '確認重試', 'Confirm Retry'),
      {
        confirmButtonText: lt('继续重试', '繼續重試', 'Retry Now'),
        cancelButtonText: lt('取消', '取消', 'Cancel'),
        type: 'warning'
      }
    )
    retryingTaskId.value = row.gameId
    await retryDeveloperUploadTask(userStore.user.id, row.gameId, { confirmText: 'RETRY' })
    ElMessage.success(lt('任务已重新进入处理队列', '任務已重新進入處理佇列', 'Task has been queued for retry'))
    await Promise.all([loadUploadTasks(), loadGames()])
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || lt('重试失败', '重試失敗', 'Retry failed'))
    }
  } finally {
    retryingTaskId.value = null
  }
}

const promptSubmitConfig = async () => {
  const { value } = await ElMessageBox.prompt(
    lt('请输入提审说明，可用于补充本次改动与风险说明。', '請輸入提審說明，用於補充本次改動與風險說明。', 'Add a review note to explain release changes and risk context.'),
    lt('提交审核', '提交審核', 'Submit for Review'),
    {
      inputPlaceholder: lt('例如：修复支付回调，补齐素材版权说明', '例如：修復支付回調，補齊素材版權說明', 'Example: fixed payment callback and added copyright assets'),
      confirmButtonText: lt('提交', '提交', 'Submit'),
      cancelButtonText: lt('取消', '取消', 'Cancel')
    }
  )
  return value
}

const handleSubmitVersion = async (row) => {
  try {
    const preflight = await getDeveloperVersionPreflight(userStore.user.id, activeGameId.value, row.id, Boolean(row.forcedUpdate))
    preflightRow.value = preflight.data
    if (!preflightRow.value?.ready) {
      preflightVisible.value = true
      ElMessage.warning(preflightRow.value?.blockingReason || lt('当前版本尚未通过发布前检查', '目前版本尚未通過發佈前檢查', 'This version did not pass preflight checks'))
      return
    }
    const note = await promptSubmitConfig()
    await submitGameVersion(activeGameId.value, row.id, note || '', Boolean(row.forcedUpdate))
    ElMessage.success(lt('版本已提交审核', '版本已提交審核', 'Version submitted for review'))
    await loadVersions(activeGameId.value)
    await loadGames()
  } catch (error) {
    if (error !== 'cancel' && error?.message) {
      ElMessage.error(error.message)
    }
  }
}

const handleInspectPreflight = async (row) => {
  try {
    const res = await getDeveloperVersionPreflight(userStore.user.id, activeGameId.value, row.id, Boolean(row.forcedUpdate))
    preflightRow.value = res.data
    preflightVisible.value = true
  } catch (error) {
    ElMessage.error(error?.message || lt('加载发布前检查失败', '載入發佈前檢查失敗', 'Failed to load preflight checks'))
  }
}

const handleRollbackVersion = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      lt('请输入回滚原因，系统将把该版本设为当前线上版本。', '請輸入回滾原因，系統將把該版本設為目前線上版本。', 'Provide a rollback reason. This version will become the active online version.'),
      lt('版本回滚', '版本回滾', 'Rollback Version'),
      {
        inputPlaceholder: lt('例如：线上版本存在支付异常', '例如：線上版本存在支付異常', 'Example: current production version has payment errors'),
        confirmButtonText: lt('确认回滚', '確認回滾', 'Confirm Rollback'),
        cancelButtonText: lt('取消', '取消', 'Cancel')
      }
    )
    await rollbackVersion(activeGameId.value, row.id, value || '')
    ElMessage.success(lt('回滚成功', '回滾成功', 'Rollback successful'))
    await loadVersions(activeGameId.value)
    await loadGames()
  } catch (error) {
    if (error !== 'cancel' && error?.message) {
      ElMessage.error(error.message)
    }
  }
}

const handleCreateAppeal = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      lt('请输入申诉原因，说明为何该版本应当进入复审。', '請輸入申訴原因，說明為何該版本應進入複審。', 'Explain why this rejected version should enter re-review.'),
      lt('提交申诉', '提交申訴', 'Submit Appeal'),
      {
        confirmButtonText: lt('提交', '提交', 'Submit'),
        cancelButtonText: lt('取消', '取消', 'Cancel'),
        inputPattern: /^.{2,}$/u,
        inputErrorMessage: lt('申诉原因至少 2 个字符', '申訴原因至少 2 個字元', 'Appeal reason must be at least 2 characters')
      }
    )
    await createReviewAppeal(activeGameId.value, row.id, value.trim())
    ElMessage.success(lt('申诉已提交', '申訴已提交', 'Appeal submitted'))
    const appealRes = await getMyReviewAppeals()
    appeals.value = appealRes.data || []
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('提交申诉失败', '提交申訴失敗', 'Failed to submit appeal'))
    }
  }
}

const getVersionStatusMeta = (status) => getGameStatusMeta(status)
const getTaskStatusMeta = (status) => {
  const map = {
    PROCESSING: { type: 'warning' },
    FAILED: { type: 'danger' },
    SUCCEEDED: { type: 'success' }
  }
  return map[status] || { type: 'info' }
}
const getVersionStatusText = (status) => {
  const dict = {
    DRAFT: lt('草稿', '草稿', 'Draft'),
    SUBMITTED: lt('已提审', '已提審', 'Submitted'),
    APPROVED: lt('已通过', '已通過', 'Approved'),
    REJECTED: lt('已驳回', '已駁回', 'Rejected'),
    PROCESSING: lt('处理中', '處理中', 'Processing')
  }
  return dict[status] || status || '-'
}

const getTaskStatusText = (status) => {
  const dict = {
    PROCESSING: lt('处理中', '處理中', 'Processing'),
    FAILED: lt('处理失败', '處理失敗', 'Failed'),
    SUCCEEDED: lt('处理成功', '處理成功', 'Succeeded')
  }
  return dict[status] || status || '-'
}

onMounted(async () => {
  await Promise.all([loadGames(), loadUploadTasks()])
})
</script>

<style scoped>
.release-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.upload-alert,
.health-alert,
.task-alert {
  margin-bottom: 16px;
}

.panel-card {
  border-radius: 20px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.panel-title {
  font-size: 17px;
  font-weight: 800;
  color: #101828;
}

.panel-subtitle {
  margin-top: 6px;
  color: #667085;
  font-size: 13px;
}

.empty-block {
  padding: 24px;
  border-radius: 16px;
  background: #f8fafc;
  color: #667085;
}

.version-summary {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.table-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.task-card {
  margin-top: 16px;
}

.muted-action {
  color: #98a2b3;
  font-size: 12px;
}

.preflight-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.preflight-version {
  color: #475467;
  font-weight: 600;
}
</style>
