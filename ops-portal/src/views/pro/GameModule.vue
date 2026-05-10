<template>
  <div class="pro-page">
    <el-card>
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ panelTitle }}</div>
            <div class="panel-subtitle">{{ panelSubtitle }}</div>
          </div>
          <div class="actions">
            <el-input
              v-model="keyword"
              clearable
              class="keyword-input"
              :placeholder="lt('搜索游戏名称、AppID 或版本号', '搜尋遊戲名稱、AppID 或版本號', 'Search by game name, AppID, or version')"
            />
            <el-select v-model="statusFilter" clearable class="filter-select filter-select-sm">
              <el-option :label="lt('全部状态', '全部狀態', 'All Statuses')" value="" />
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-if="isGameMode" v-model="categoryFilter" clearable filterable class="filter-select">
              <el-option :label="lt('全部分类', '全部分類', 'All Categories')" value="" />
              <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.name" />
            </el-select>
            <template v-if="isGameMode">
              <el-button @click="gotoCategoryModule">{{ lt('分类管理', '分類管理', 'Category Management') }}</el-button>
              <el-button type="primary" @click="openCreateGameDialog">{{ lt('上传新游戏', '上傳新遊戲', 'Upload New Game') }}</el-button>
              <el-button :disabled="!selectedGameIds.length" @click="openBatchVisibility('VISIBLE')">{{ lt('批量恢复展示', '批量恢復展示', 'Batch Restore') }}</el-button>
              <el-button :disabled="!selectedGameIds.length" type="warning" @click="openBatchVisibility('HIDDEN')">{{ lt('批量隐藏', '批量隱藏', 'Batch Hide') }}</el-button>
              <el-button :disabled="!selectedGameIds.length" type="danger" @click="openBatchVisibility('BLOCKED')">{{ lt('批量封禁', '批量封禁', 'Batch Block') }}</el-button>
            </template>
            <el-button :loading="loading" @click="refreshCurrentView">{{ lt('刷新', '重新整理', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <div class="status-grid">
        <div class="status-card" v-for="item in statusCards" :key="item.key" @click="statusFilter = item.filter">
          <div class="status-label">{{ item.label }}</div>
          <div class="status-value">{{ item.value }}</div>
          <div class="status-tip">{{ item.tip }}</div>
        </div>
      </div>

      <el-alert
        v-if="isGameMode"
        class="ops-hint"
        type="info"
        :title="lt('新游戏主链路：上传 ZIP 创建游戏 -> 补充资料 -> 提交审核或直接上架。', '新遊戲主鏈路：上傳 ZIP 建立遊戲 -> 補充資料 -> 提交審核或直接上架。', 'New game chain: upload ZIP -> enrich metadata -> submit for review or direct publish.')"
        show-icon
      />
      <el-alert
        v-else
        class="ops-hint"
        type="info"
        :title="lt('新版本主链路：上传 ZIP 生成版本 -> 版本详情校验 -> 提交审核、直接上架或回滚。', '新版本主鏈路：上傳 ZIP 生成版本 -> 版本詳情校驗 -> 提交審核、直接上架或回滾。', 'New version chain: upload ZIP -> inspect version detail -> submit, direct go-live, or rollback.')"
        show-icon
      />

      <el-card v-if="selectedGameDetail && isGameMode" class="detail-card" shadow="never">
        <template #header>
          <div class="detail-head">
            <div>
              <div class="detail-title">{{ selectedGameDetail.name || '-' }}</div>
              <div class="detail-subtitle">AppID: {{ selectedGameDetail.appId || '-' }}</div>
            </div>
            <div class="detail-actions">
              <el-button @click="openVersionUploadDialog(selectedGameDetail)">{{ lt('上传新版本', '上傳新版本', 'Upload Version') }}</el-button>
              <el-button type="primary" @click="openEdit(selectedGameDetail)">{{ lt('编辑资料', '編輯資料', 'Edit Metadata') }}</el-button>
              <el-button v-if="canSubmitGame(selectedGameDetail)" type="warning" @click="submitForAudit(selectedGameDetail)">{{ lt('提交审核', '提交審核', 'Submit Review') }}</el-button>
              <el-button v-if="canDirectGoLiveGame(selectedGameDetail)" type="success" @click="directPublishLatestDraft(selectedGameDetail)">{{ lt('直接上架', '直接上架', 'Direct Go Live') }}</el-button>
              <el-button @click="closeGameDetail">{{ lt('关闭详情', '關閉詳情', 'Close Detail') }}</el-button>
            </div>
          </div>
        </template>

        <el-descriptions :column="detailDescriptionColumns" border>
          <el-descriptions-item label="AppID">{{ selectedGameDetail.appId || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('分类', '分類', 'Category')">{{ selectedGameDetail.category || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('联网', '連網', 'Online')">{{ selectedGameDetail.requiresOnline ? lt('是', '是', 'Yes') : lt('否', '否', 'No') }}</el-descriptions-item>
          <el-descriptions-item :label="lt('审核状态', '審核狀態', 'Review Status')">
            <el-tag :type="getStatusType(selectedGameDetail.status)">{{ getStatusText(selectedGameDetail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="lt('前端状态', '前端狀態', 'Frontend State')">
            <el-tag :type="getFrontendStateType(selectedGameDetail.frontendState)">{{ getFrontendStateText(selectedGameDetail.frontendState) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="lt('展示控制', '展示控制', 'Visibility')">
            <el-tag :type="getVisibilityType(selectedGameDetail.visibilityStatus)">{{ getVisibilityText(selectedGameDetail.visibilityStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="lt('版本数', '版本數', 'Versions')">{{ selectedGameDetail.versions.length }}</el-descriptions-item>
          <el-descriptions-item :label="lt('当前版本', '當前版本', 'Current Version')">{{ selectedGameDetail.version || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('运行时资料状态', '運行時資料狀態', 'Runtime Profile')">{{ selectedGameDetail.profile.operationsStatus || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('游戏描述', '遊戲描述', 'Description')" :span="3">{{ selectedGameDetail.description || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('标签', '標籤', 'Tags')" :span="3">{{ normalizeTags(selectedGameDetail.tags).join(', ') || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="subsection">
          <div class="subsection-title">{{ lt('版本记录', '版本記錄', 'Versions') }}</div>
          <el-table :data="selectedGameDetail.versions" size="small">
            <el-table-column prop="versionName" :label="lt('版本名', '版本名', 'Version Name')" min-width="160" />
            <el-table-column :label="lt('状态', '狀態', 'Status')" width="140">
              <template #default="{ row }">
                <el-tag :type="getVersionStatusType(row.status)">{{ getVersionStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="lt('Manifest', 'Manifest', 'Manifest')" min-width="180">
              <template #default="{ row }">{{ row.hostedManifestSummary || '-' }}</template>
            </el-table-column>
            <el-table-column :label="lt('更新时间', '更新時間', 'Updated At')" min-width="180">
              <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="260" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openVersionDetail(row)">{{ lt('查看详情', '查看詳情', 'View Detail') }}</el-button>
                <el-button v-if="canSubmitVersion(row)" link type="warning" @click="submitSpecificVersion(row)">{{ lt('提交审核', '提交審核', 'Submit') }}</el-button>
                <el-button v-if="canDirectGoLiveVersion(row)" link type="success" @click="directPublishVersion(row)">{{ lt('直接上架', '直接上架', 'Go Live') }}</el-button>
                <el-button v-if="row.status === 'APPROVED'" link type="info" @click="rollbackVersion(row)">{{ lt('回滚到此版本', '回滾到此版本', 'Rollback Here') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>

      <el-card v-if="selectedVersionDetail && isVersionMode" class="detail-card" shadow="never">
        <template #header>
          <div class="detail-head">
            <div>
              <div class="detail-title">{{ selectedVersionDetail.versionName || selectedVersionDetail.version || '-' }}</div>
              <div class="detail-subtitle">{{ selectedVersionDetail.gameName || '-' }} / {{ selectedVersionDetail.appId || '-' }}</div>
            </div>
            <div class="detail-actions">
              <el-button v-if="selectedVersionDetail.gameId" @click="openVersionUploadDialog({ id: selectedVersionDetail.gameId, appId: selectedVersionDetail.appId, name: selectedVersionDetail.gameName })">{{ lt('再传新版本', '再傳新版本', 'Upload Another') }}</el-button>
              <el-button v-if="canSubmitVersion(selectedVersionDetail)" type="warning" @click="submitSpecificVersion(selectedVersionDetail)">{{ lt('提交审核', '提交審核', 'Submit Review') }}</el-button>
              <el-button v-if="canDirectGoLiveVersion(selectedVersionDetail)" type="success" @click="directPublishVersion(selectedVersionDetail)">{{ lt('直接上架', '直接上架', 'Direct Go Live') }}</el-button>
              <el-button v-if="selectedVersionDetail.status === 'APPROVED'" @click="rollbackVersion(selectedVersionDetail)">{{ lt('回滚到此版本', '回滾到此版本', 'Rollback') }}</el-button>
              <el-button @click="closeVersionDetail">{{ lt('关闭详情', '關閉詳情', 'Close Detail') }}</el-button>
            </div>
          </div>
        </template>

        <el-descriptions :column="detailDescriptionColumns" border>
          <el-descriptions-item :label="lt('游戏', '遊戲', 'Game')">{{ selectedVersionDetail.gameName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="AppID">{{ selectedVersionDetail.appId || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('状态', '狀態', 'Status')">
            <el-tag :type="getVersionStatusType(selectedVersionDetail.status)">{{ getVersionStatusText(selectedVersionDetail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="lt('版本名', '版本名', 'Version Name')">{{ selectedVersionDetail.versionName || selectedVersionDetail.version || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('强更', '強更', 'Force Update')">{{ selectedVersionDetail.forcedUpdate ? lt('是', '是', 'Yes') : lt('否', '否', 'No') }}</el-descriptions-item>
          <el-descriptions-item :label="lt('Manifest 校验', 'Manifest 校驗', 'Manifest Validation')">
            <el-tag :type="selectedVersionDetail.hostedManifestValid ? 'success' : 'danger'">
              {{ selectedVersionDetail.hostedManifestValid ? lt('通过', '通過', 'Passed') : lt('未通过', '未通過', 'Failed') }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="lt('审核说明', '審核說明', 'Audit Note')" :span="3">{{ selectedVersionDetail.auditReason || selectedVersionDetail.submitNote || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="lt('Manifest 摘要', 'Manifest 摘要', 'Manifest Summary')" :span="3">{{ selectedVersionDetail.hostedManifestSummary || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-table
        v-if="isGameMode"
        :data="filteredGames"
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="52" />
        <el-table-column type="index" width="60" :label="lt('序号', '序號', 'No.')" />
        <el-table-column :label="lt('游戏名称', '遊戲名稱', 'Game Name')" min-width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="openGameDetail(row)">{{ row.name }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="appId" label="AppID" min-width="180" />
        <el-table-column prop="version" :label="lt('版本', '版本', 'Version')" width="120" />
        <el-table-column :label="lt('分类', '分類', 'Category')" min-width="140">
          <template #default="{ row }">{{ row.category || '-' }}</template>
        </el-table-column>
        <el-table-column :label="lt('状态', '狀態', 'Status')" width="140">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('前端状态', '前端狀態', 'Frontend State')" width="150">
          <template #default="{ row }">
            <el-tag :type="getFrontendStateType(row.frontendState)">{{ getFrontendStateText(row.frontendState) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('展示控制', '展示控制', 'Visibility')" width="150">
          <template #default="{ row }">
            <el-tag :type="getVisibilityType(row.visibilityStatus)">{{ getVisibilityText(row.visibilityStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('最新版本状态', '最新版本狀態', 'Latest Version')" width="150">
          <template #default="{ row }">
            <el-tag :type="getVersionStatusType(row.latestVersionStatus)">{{ getVersionStatusText(row.latestVersionStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="360" fixed="right">
          <template #default="{ row }">
            <el-button link type="info" @click="inspectVersion(row)">{{ lt('版本检查', '版本檢查', 'Inspect') }}</el-button>
            <el-button link type="primary" @click="openVersionUploadDialog(row)">{{ lt('上传版本', '上傳版本', 'Upload Version') }}</el-button>
            <el-button
              v-if="canSubmitGame(row)"
              link
              type="warning"
              @click="submitForAudit(row)"
            >
              {{ lt('提交审核', '提交審核', 'Submit') }}
            </el-button>
            <el-button
              v-if="canDirectGoLiveGame(row)"
              link
              type="success"
              @click="directPublishLatestDraft(row)"
            >
              {{ lt('直接上架', '直接上架', 'Go Live') }}
            </el-button>
            <el-button link type="danger" @click="openVisibilityControl(row)">{{ lt('展示控制', '展示控制', 'Visibility') }}</el-button>
            <el-button link type="warning" @click="openGovernanceScope(row)">{{ lt('多维治理', '多維治理', 'Governance') }}</el-button>
            <el-button link type="primary" @click="openEdit(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-table v-else :data="filteredVersions" v-loading="loading">
        <el-table-column type="index" width="60" :label="lt('序号', '序號', 'No.')" />
        <el-table-column :label="lt('版本名', '版本名', 'Version Name')" min-width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="openVersionDetail(row)">{{ row.versionName || row.version || '-' }}</el-button>
          </template>
        </el-table-column>
        <el-table-column :label="lt('游戏', '遊戲', 'Game')" min-width="180">
          <template #default="{ row }">
            <el-button link @click="openGameDetailById(row.gameId)">{{ row.gameName || '-' }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="appId" label="AppID" min-width="160" />
        <el-table-column :label="lt('版本状态', '版本狀態', 'Version Status')" width="140">
          <template #default="{ row }">
            <el-tag :type="getVersionStatusType(row.status)">{{ getVersionStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('游戏状态', '遊戲狀態', 'Game Status')" width="140">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.gameStatus)">{{ getStatusText(row.gameStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('Manifest', 'Manifest', 'Manifest')" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.hostedManifestSummary || '-' }}</template>
        </el-table-column>
        <el-table-column :label="lt('更新时间', '更新時間', 'Updated At')" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="320" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canSubmitVersion(row)" link type="warning" @click="submitSpecificVersion(row)">{{ lt('提交审核', '提交審核', 'Submit') }}</el-button>
            <el-button v-if="canDirectGoLiveVersion(row)" link type="success" @click="directPublishVersion(row)">{{ lt('直接上架', '直接上架', 'Go Live') }}</el-button>
            <el-button v-if="row.status === 'APPROVED'" link type="info" @click="rollbackVersion(row)">{{ lt('回滚', '回滾', 'Rollback') }}</el-button>
            <el-button link type="primary" @click="openVersionUploadDialog({ id: row.gameId, appId: row.appId, name: row.gameName })">{{ lt('再传版本', '再傳版本', 'Upload Another') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="createVisible" :title="lt('上传新游戏', '上傳新遊戲', 'Upload New Game')" :width="dialogWidth('680px')">
      <el-form :model="createForm" :label-width="formLabelWidth">
        <el-form-item :label="lt('ZIP 包', 'ZIP 包', 'ZIP Package')" required>
          <el-upload
            :auto-upload="false"
            :show-file-list="true"
            :limit="1"
            accept=".zip"
            :on-change="handleCreateFileChange"
            :on-remove="handleCreateFileRemove"
          >
            <el-button type="primary">{{ lt('选择 ZIP', '選擇 ZIP', 'Choose ZIP') }}</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item :label="lt('游戏名称', '遊戲名稱', 'Game Name')">
          <el-input v-model="createForm.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item :label="lt('游戏描述', '遊戲描述', 'Description')">
          <el-input v-model="createForm.description" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item :label="lt('分类', '分類', 'Category')">
          <el-select v-model="createForm.category" clearable filterable style="width: 100%">
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('标签', '標籤', 'Tags')">
          <el-select v-model="createForm.tags" multiple filterable allow-create default-first-option style="width: 100%">
            <el-option v-for="item in tagOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('需要联网', '需要連網', 'Requires Online')">
          <el-switch v-model="createForm.requiresOnline" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreateGame">{{ lt('上传并创建', '上傳並建立', 'Upload & Create') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="versionUploadVisible" :title="lt('上传游戏版本', '上傳遊戲版本', 'Upload Game Version')" :width="dialogWidth('620px')">
      <el-form :model="versionUploadForm" :label-width="formLabelWidth">
        <el-form-item :label="lt('目标游戏', '目標遊戲', 'Target Game')">
          <el-input :model-value="versionUploadForm.targetGameName" disabled />
        </el-form-item>
        <el-form-item :label="lt('ZIP 包', 'ZIP 包', 'ZIP Package')" required>
          <el-upload
            :auto-upload="false"
            :show-file-list="true"
            :limit="1"
            accept=".zip"
            :on-change="handleVersionFileChange"
            :on-remove="handleVersionFileRemove"
          >
            <el-button type="primary">{{ lt('选择 ZIP', '選擇 ZIP', 'Choose ZIP') }}</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item :label="lt('版本名', '版本名', 'Version Name')">
          <el-input v-model="versionUploadForm.versionName" :placeholder="lt('留空则自动生成，例如 1.0.2', '留空則自動生成，例如 1.0.2', 'Leave blank to auto-generate, for example 1.0.2')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="versionUploadVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="uploadingVersion" @click="submitVersionUpload">{{ lt('上传版本', '上傳版本', 'Upload Version') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="editorVisible"
      :title="lt('编辑游戏信息', '編輯遊戲資訊', 'Edit Game Info')"
      :width="dialogWidth('640px')"
    >
      <el-form :model="editorForm" :label-width="formLabelWidth">
        <el-form-item :label="lt('游戏名称', '遊戲名稱', 'Game Name')" required>
          <el-input v-model="editorForm.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item :label="lt('游戏描述', '遊戲描述', 'Description')">
          <el-input v-model="editorForm.description" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item :label="lt('图标地址', '圖示位址', 'Icon URL')">
          <el-input v-model="editorForm.iconUrl" />
        </el-form-item>
        <el-form-item :label="lt('分类', '分類', 'Category')">
          <el-select v-model="editorForm.category" clearable filterable style="width: 100%">
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('版本', '版本', 'Version')">
          <el-input v-model="editorForm.version" />
        </el-form-item>
        <el-form-item :label="lt('标签', '標籤', 'Tags')">
          <el-select v-model="editorForm.tags" multiple filterable allow-create default-first-option style="width: 100%">
            <el-option v-for="item in tagOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('需要联网', '需要連網', 'Requires Online')">
          <el-switch v-model="editorForm.requiresOnline" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="saveGame">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="visibilityVisible"
      :title="lt('前端展示控制', '前端展示控制', 'Frontend Visibility Control')"
      :width="dialogWidth('560px')"
    >
      <el-form :model="visibilityForm" :label-width="formLabelWidth">
        <el-form-item :label="lt('控制状态', '控制狀態', 'Control Status')" required>
          <el-select v-model="visibilityForm.visibilityStatus" style="width: 100%">
            <el-option :label="lt('正常展示', '正常展示', 'Visible')" value="VISIBLE" />
            <el-option :label="lt('隐藏', '隱藏', 'Hidden')" value="HIDDEN" />
            <el-option :label="lt('封禁', '封禁', 'Blocked')" value="BLOCKED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('控制原因', '控制原因', 'Reason')">
          <el-input v-model="visibilityForm.reason" type="textarea" :rows="4" maxlength="256" show-word-limit />
        </el-form-item>
        <el-form-item v-if="visibilityForm.visibilityStatus === 'BLOCKED'" :label="lt('封禁截止', '封禁截止', 'Blocked Until')">
          <el-input v-model="visibilityForm.visibilityUntil" type="datetime-local" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visibilityVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="danger" :loading="visibilitySaving" @click="submitVisibilityControl">{{ lt('确认执行', '確認執行', 'Confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="batchVisibilityVisible"
      :title="lt('批量前端展示控制', '批量前端展示控制', 'Batch Frontend Visibility')"
      :width="dialogWidth('560px')"
    >
      <el-form :model="batchVisibilityForm" :label-width="formLabelWidth">
        <el-form-item :label="lt('选中数量', '選中數量', 'Selected Count')">
          <el-input :model-value="String(selectedGameIds.length)" disabled />
        </el-form-item>
        <el-form-item :label="lt('控制状态', '控制狀態', 'Control Status')">
          <el-select v-model="batchVisibilityForm.visibilityStatus" style="width: 100%">
            <el-option :label="lt('正常展示', '正常展示', 'Visible')" value="VISIBLE" />
            <el-option :label="lt('隐藏', '隱藏', 'Hidden')" value="HIDDEN" />
            <el-option :label="lt('封禁', '封禁', 'Blocked')" value="BLOCKED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('控制原因', '控制原因', 'Reason')">
          <el-input v-model="batchVisibilityForm.reason" type="textarea" :rows="4" maxlength="256" show-word-limit />
        </el-form-item>
        <el-form-item v-if="batchVisibilityForm.visibilityStatus === 'BLOCKED'" :label="lt('封禁截止', '封禁截止', 'Blocked Until')">
          <el-input v-model="batchVisibilityForm.visibilityUntil" type="datetime-local" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchVisibilityVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="danger" :loading="batchVisibilitySaving" @click="submitBatchVisibilityControl">{{ lt('确认执行', '確認執行', 'Confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="governanceVisible"
      :title="lt('多维治理控制', '多維治理控制', 'Multidimensional Governance')"
      :width="dialogWidth('720px')"
    >
      <el-form :model="governanceForm" :label-width="governanceLabelWidth">
        <el-form-item :label="lt('渠道模式', '渠道模式', 'Channel Mode')">
          <el-select v-model="governanceForm.channelMode" style="width: 100%">
            <el-option :label="lt('不限制', '不限制', 'No Restriction')" value="ALL" />
            <el-option :label="lt('渠道白名单', '渠道白名單', 'Allowlist')" value="ALLOWLIST" />
            <el-option :label="lt('渠道黑名单', '渠道黑名單', 'Blocklist')" value="DENYLIST" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="governanceForm.channelMode !== 'ALL'" :label="lt('渠道列表', '渠道列表', 'Channels')">
          <el-select v-model="governanceForm.channels" multiple filterable allow-create default-first-option style="width: 100%">
            <el-option v-for="item in knownChannels" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('地区模式', '地區模式', 'Region Mode')">
          <el-select v-model="governanceForm.regionMode" style="width: 100%">
            <el-option :label="lt('不限制', '不限制', 'No Restriction')" value="ALL" />
            <el-option :label="lt('地区白名单', '地區白名單', 'Allowlist')" value="ALLOWLIST" />
            <el-option :label="lt('地区黑名单', '地區黑名單', 'Blocklist')" value="DENYLIST" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="governanceForm.regionMode !== 'ALL'" :label="lt('地区列表', '地區列表', 'Regions')">
          <el-select v-model="governanceForm.regions" multiple filterable allow-create default-first-option style="width: 100%">
            <el-option v-for="item in knownRegions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('版本模式', '版本模式', 'Version Mode')">
          <el-select v-model="governanceForm.versionMode" style="width: 100%">
            <el-option :label="lt('不限制', '不限制', 'No Restriction')" value="ALL" />
            <el-option :label="lt('最低版本', '最低版本', 'Minimum Version')" value="MIN_VERSION" />
            <el-option :label="lt('版本范围', '版本範圍', 'Version Range')" value="RANGE" />
            <el-option :label="lt('版本黑名单', '版本黑名單', 'Version Blocklist')" value="BLOCKLIST" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="governanceForm.versionMode === 'MIN_VERSION' || governanceForm.versionMode === 'RANGE'" :label="lt('最低版本', '最低版本', 'Min Version')">
          <el-input v-model="governanceForm.versionMin" />
        </el-form-item>
        <el-form-item v-if="governanceForm.versionMode === 'RANGE'" :label="lt('最高版本', '最高版本', 'Max Version')">
          <el-input v-model="governanceForm.versionMax" />
        </el-form-item>
        <el-form-item v-if="governanceForm.versionMode === 'BLOCKLIST'" :label="lt('拦截版本', '攔截版本', 'Blocked Versions')">
          <el-select v-model="governanceForm.blockedVersions" multiple filterable allow-create default-first-option style="width: 100%">
            <el-option v-for="item in knownVersions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('治理备注', '治理備註', 'Governance Note')">
          <el-input v-model="governanceForm.governanceNote" type="textarea" :rows="3" maxlength="256" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="governanceVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="governanceSaving" @click="submitGovernanceScope">{{ lt('保存治理规则', '儲存治理規則', 'Save Governance Rules') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../api/request'
import {
  approveGame,
  getGameVersions,
  getOpsGameCategories,
  getOpsGameGovernanceImpact,
  getOpsGameProfile,
  getOpsGames,
  rejectGame,
  submitGameForAudit,
  updateOpsGameGovernanceScope,
  updateOpsGameMetadata,
  updateOpsGameVisibility,
  updateOpsGameVisibilityBatch
} from '../../api'
import { useI18nLite } from '../../i18n'
import { useViewport } from '../../composables/useViewport'

const router = useRouter()
const route = useRoute()
const { lt } = useI18nLite()
const { isTabletOrBelow, isPhone } = useViewport()

const loading = ref(false)
const saving = ref(false)
const creating = ref(false)
const uploadingVersion = ref(false)
const visibilitySaving = ref(false)
const batchVisibilitySaving = ref(false)
const governanceSaving = ref(false)

const editorVisible = ref(false)
const createVisible = ref(false)
const versionUploadVisible = ref(false)
const visibilityVisible = ref(false)
const batchVisibilityVisible = ref(false)
const governanceVisible = ref(false)

const keyword = ref('')
const statusFilter = ref('')
const categoryFilter = ref('')
const games = ref([])
const versions = ref([])
const categoryOptions = ref([])
const selectedGameIds = ref([])
const selectedGameDetail = ref(null)
const selectedVersionDetail = ref(null)

const createFile = ref(null)
const versionUploadFile = ref(null)
const editingGameId = ref(null)
const visibilityGameId = ref(null)
const governanceGameId = ref(null)
const versionUploadGameId = ref(null)

const createForm = reactive({
  name: '',
  description: '',
  category: '',
  tags: [],
  requiresOnline: false
})

const versionUploadForm = reactive({
  targetGameName: '',
  versionName: ''
})

const editorForm = reactive({
  name: '',
  description: '',
  iconUrl: '',
  category: '',
  tags: [],
  version: '',
  requiresOnline: false
})

const visibilityForm = reactive({
  visibilityStatus: 'VISIBLE',
  reason: '',
  visibilityUntil: ''
})

const batchVisibilityForm = reactive({
  visibilityStatus: 'VISIBLE',
  reason: '',
  visibilityUntil: ''
})

const governanceForm = reactive({
  channelMode: 'ALL',
  channels: [],
  regionMode: 'ALL',
  regions: [],
  versionMode: 'ALL',
  versionMin: '',
  versionMax: '',
  blockedVersions: [],
  governanceNote: ''
})

const isVersionMode = computed(() => String(route.name || '').includes('Version'))
const isGameMode = computed(() => !isVersionMode.value)
const detailDescriptionColumns = computed(() => (isPhone.value ? 1 : isTabletOrBelow.value ? 2 : 3))
const formLabelWidth = computed(() => (isPhone.value ? '96px' : '120px'))
const governanceLabelWidth = computed(() => (isPhone.value ? '108px' : '130px'))
const currentGameId = computed(() => route.params.gameId ? Number(route.params.gameId) : null)
const currentVersionId = computed(() => route.params.versionId ? Number(route.params.versionId) : null)

const dialogWidth = (desktop, tablet = '88%', mobile = '94%') => {
  if (isPhone.value) return mobile
  if (isTabletOrBelow.value) return tablet
  return desktop
}

const panelTitle = computed(() => (
  isGameMode.value
    ? lt('游戏与版本工作台', '遊戲與版本工作台', 'Games & Versions Workbench')
    : lt('版本列表与详情', '版本列表與詳情', 'Version List & Detail')
))

const panelSubtitle = computed(() => (
  isGameMode.value
    ? lt('运营可直接上传新游戏、补充资料、提交审核或直接上架。', '營運可直接上傳新遊戲、補充資料、提交審核或直接上架。', 'Operations can upload new games, enrich metadata, submit for review, or direct go live.')
    : lt('运营可直接上传版本 ZIP、查看版本详情，并执行提交审核、直接上架和回滚。', '營運可直接上傳版本 ZIP、查看版本詳情，並執行提交審核、直接上架和回滾。', 'Operations can upload version ZIPs, inspect version detail, and trigger submit, go-live, and rollback actions.')
))

const statusOptions = computed(() => (
  isGameMode.value
    ? [
        { value: 'DRAFT', label: lt('草稿', '草稿', 'Draft') },
        { value: 'PROCESSING', label: lt('处理中', '處理中', 'Processing') },
        { value: 'PENDING', label: lt('待审核', '待審核', 'Pending') },
        { value: 'APPROVED', label: lt('已通过', '已通過', 'Approved') },
        { value: 'REJECTED', label: lt('已驳回', '已駁回', 'Rejected') }
      ]
    : [
        { value: 'PROCESSING', label: lt('处理中', '處理中', 'Processing') },
        { value: 'DRAFT', label: lt('草稿', '草稿', 'Draft') },
        { value: 'SUBMITTED', label: lt('待审核', '待審核', 'Submitted') },
        { value: 'APPROVED', label: lt('已通过', '已通過', 'Approved') },
        { value: 'REJECTED', label: lt('已驳回', '已駁回', 'Rejected') }
      ]
))

const tagOptions = computed(() => {
  const pool = new Set()
  for (const game of games.value) {
    for (const tag of normalizeTags(game.tags)) {
      pool.add(tag)
    }
  }
  return Array.from(pool)
})

const knownChannels = computed(() => collectGovernanceValues('channels'))
const knownRegions = computed(() => collectGovernanceValues('regions'))
const knownVersions = computed(() => {
  const pool = new Set(versions.value.map((item) => item.versionName || item.version).filter(Boolean))
  for (const game of games.value) {
    for (const item of game.governanceScope?.blockedVersions || []) pool.add(item)
    if (game.governanceScope?.versionMin) pool.add(game.governanceScope.versionMin)
    if (game.governanceScope?.versionMax) pool.add(game.governanceScope.versionMax)
  }
  return Array.from(pool)
})

const filteredGames = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return games.value.filter((game) => {
    const name = (game.name || '').toLowerCase()
    const appId = (game.appId || '').toLowerCase()
    const matchedKeyword = !query || name.includes(query) || appId.includes(query)
    const matchedStatus = !statusFilter.value || game.status === statusFilter.value
    const matchedCategory = !categoryFilter.value || game.category === categoryFilter.value
    return matchedKeyword && matchedStatus && matchedCategory
  })
})

const filteredVersions = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return versions.value.filter((version) => {
    const haystacks = [
      version.gameName,
      version.appId,
      version.versionName,
      version.version
    ].map((item) => String(item || '').toLowerCase())
    const matchedKeyword = !query || haystacks.some((item) => item.includes(query))
    const matchedStatus = !statusFilter.value || version.status === statusFilter.value
    return matchedKeyword && matchedStatus
  })
})

const statusCards = computed(() => {
  if (isGameMode.value) {
    const countBy = (status) => games.value.filter((item) => item.status === status).length
    return [
      { key: 'all', filter: '', label: lt('全部游戏', '全部遊戲', 'All Games'), value: games.value.length, tip: lt('平台已接入总量', '平台已接入總量', 'Total onboarded') },
      { key: 'pending', filter: 'PENDING', label: lt('待审核', '待審核', 'Pending'), value: countBy('PENDING'), tip: lt('等待审核或直接上架', '等待審核或直接上架', 'Awaiting review or direct go-live') },
      { key: 'approved', filter: 'APPROVED', label: lt('已上线', '已上線', 'Live'), value: countBy('APPROVED'), tip: lt('前端可流转', '前端可流轉', 'Ready for client circulation') },
      { key: 'processing', filter: 'PROCESSING', label: lt('处理中', '處理中', 'Processing'), value: countBy('PROCESSING'), tip: lt('ZIP 包后台处理中', 'ZIP 包後台處理中', 'ZIP is being processed') }
    ]
  }
  const countBy = (status) => versions.value.filter((item) => item.status === status).length
  return [
    { key: 'all', filter: '', label: lt('全部版本', '全部版本', 'All Versions'), value: versions.value.length, tip: lt('所有游戏版本总量', '所有遊戲版本總量', 'Total versions') },
    { key: 'draft', filter: 'DRAFT', label: lt('草稿版本', '草稿版本', 'Draft Versions'), value: countBy('DRAFT'), tip: lt('可继续补资料或提交审核', '可繼續補資料或提交審核', 'Ready for metadata or submission') },
    { key: 'submitted', filter: 'SUBMITTED', label: lt('待审核版本', '待審核版本', 'Submitted Versions'), value: countBy('SUBMITTED'), tip: lt('等待审核通过', '等待審核通過', 'Awaiting approval') },
    { key: 'approved', filter: 'APPROVED', label: lt('已通过版本', '已通過版本', 'Approved Versions'), value: countBy('APPROVED'), tip: lt('可用于回滚或当前线上', '可用於回滾或當前線上', 'Can be rolled back to or already live') }
  ]
})

function normalizeTags(tags) {
  if (Array.isArray(tags)) {
    return tags.filter(Boolean).map((tag) => String(tag).trim()).filter(Boolean)
  }
  if (typeof tags === 'string') {
    return tags.split(',').map((tag) => tag.trim()).filter(Boolean)
  }
  return []
}

function collectGovernanceValues(field) {
  const pool = new Set()
  for (const game of games.value) {
    for (const item of game.governanceScope?.[field] || []) {
      if (item) pool.add(item)
    }
  }
  return Array.from(pool)
}

function formatDateTime(value) {
  if (!value) return '-'
  try {
    return new Date(value).toLocaleString()
  } catch {
    return String(value)
  }
}

function getStatusText(status) {
  return {
    PROCESSING: lt('处理中', '處理中', 'Processing'),
    DRAFT: lt('草稿', '草稿', 'Draft'),
    PENDING: lt('待审核', '待審核', 'Pending'),
    APPROVED: lt('已通过', '已通過', 'Approved'),
    REJECTED: lt('已驳回', '已駁回', 'Rejected')
  }[status] || status || lt('未知', '未知', 'Unknown')
}

function getStatusType(status) {
  return {
    PROCESSING: 'warning',
    DRAFT: 'info',
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }[status] || 'info'
}

function getVersionStatusText(status) {
  return {
    PROCESSING: lt('处理中', '處理中', 'Processing'),
    DRAFT: lt('草稿', '草稿', 'Draft'),
    SUBMITTED: lt('待审核', '待審核', 'Submitted'),
    APPROVED: lt('已通过', '已通過', 'Approved'),
    REJECTED: lt('已驳回', '已駁回', 'Rejected')
  }[status] || status || lt('未知', '未知', 'Unknown')
}

function getVersionStatusType(status) {
  return {
    PROCESSING: 'warning',
    DRAFT: 'info',
    SUBMITTED: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }[status] || 'info'
}

function getFrontendStateText(status) {
  return {
    APPROVED: lt('可运营', '可營運', 'Operable'),
    PENDING: lt('待审核中', '待審核中', 'Under Review'),
    DRAFT: lt('未开放', '未開放', 'Not Open'),
    REJECTED: lt('已拦截', '已攔截', 'Blocked'),
    PROCESSING: lt('处理中', '處理中', 'Processing'),
    HIDDEN: lt('已隐藏', '已隱藏', 'Hidden'),
    BLOCKED: lt('已封禁', '已封禁', 'Blocked')
  }[status] || lt('未知', '未知', 'Unknown')
}

function getFrontendStateType(status) {
  return {
    APPROVED: 'success',
    PENDING: 'warning',
    DRAFT: 'info',
    REJECTED: 'danger',
    PROCESSING: '',
    HIDDEN: 'warning',
    BLOCKED: 'danger'
  }[status] || 'info'
}

function getVisibilityText(status) {
  return {
    VISIBLE: lt('正常展示', '正常展示', 'Visible'),
    HIDDEN: lt('隐藏', '隱藏', 'Hidden'),
    BLOCKED: lt('封禁', '封禁', 'Blocked')
  }[status] || status || '-'
}

function getVisibilityType(status) {
  return {
    VISIBLE: 'success',
    HIDDEN: 'warning',
    BLOCKED: 'danger'
  }[status] || 'info'
}

function latestDraftLikeVersion(game) {
  if (!selectedGameDetail.value || selectedGameDetail.value.id !== game.id) return null
  return selectedGameDetail.value.versions.find((item) => ['DRAFT', 'REJECTED'].includes(item.status))
    || selectedGameDetail.value.versions[0]
}

function canSubmitGame(game) {
  return ['DRAFT', 'REJECTED'].includes(game.status)
}

function canDirectGoLiveGame(game) {
  const version = latestDraftLikeVersion(game)
  return Boolean(version && canDirectGoLiveVersion(version))
}

function canSubmitVersion(version) {
  return ['DRAFT', 'REJECTED'].includes(version.status)
}

function canDirectGoLiveVersion(version) {
  return ['DRAFT', 'REJECTED'].includes(version.status)
}

function resetCreateForm() {
  createForm.name = ''
  createForm.description = ''
  createForm.category = ''
  createForm.tags = []
  createForm.requiresOnline = false
  createFile.value = null
}

function resetVersionUploadForm() {
  versionUploadGameId.value = null
  versionUploadForm.targetGameName = ''
  versionUploadForm.versionName = ''
  versionUploadFile.value = null
}

function resetEditor() {
  editingGameId.value = null
  editorForm.name = ''
  editorForm.description = ''
  editorForm.iconUrl = ''
  editorForm.category = ''
  editorForm.tags = []
  editorForm.version = ''
  editorForm.requiresOnline = false
}

function refreshCurrentView() {
  if (isGameMode.value) {
    return loadGames({ force: true, syncRouteDetail: true })
  }
  return loadVersions({ force: true, syncRouteDetail: true })
}

async function loadCategories() {
  try {
    const res = await getOpsGameCategories()
    categoryOptions.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    ElMessage.error(error.message || lt('加载分类失败', '載入分類失敗', 'Failed to load categories'))
  }
}

async function loadGames(options = {}) {
  loading.value = true
  try {
    const res = await getOpsGames({
      status: statusFilter.value || undefined,
      category: categoryFilter.value || undefined,
      keyword: keyword.value || undefined
    })
    games.value = Array.isArray(res.data) ? res.data : []
    if (options.syncRouteDetail !== false && currentGameId.value) {
      await loadGameDetail(currentGameId.value)
    }
  } catch (error) {
    ElMessage.error(error.message || lt('加载游戏列表失败', '載入遊戲列表失敗', 'Failed to load game list'))
  } finally {
    loading.value = false
  }
}

async function loadVersions(options = {}) {
  loading.value = true
  try {
    const gameList = games.value.length && !options.force ? games.value : (await getOpsGames().then((res) => Array.isArray(res.data) ? res.data : []))
    if (!games.value.length || options.force) {
      games.value = gameList
    }
    const versionGroups = await Promise.all(
      gameList.map(async (game) => {
        const res = await getGameVersions(game.id)
        const rows = Array.isArray(res.data) ? res.data : []
        return rows.map((version) => ({
          ...version,
          id: version.id,
          gameId: game.id,
          gameName: game.name,
          appId: game.appId,
          gameStatus: game.status,
          visibilityStatus: game.visibilityStatus,
          frontendState: game.frontendState
        }))
      })
    )
    versions.value = versionGroups.flat()
    if (options.syncRouteDetail !== false && currentVersionId.value) {
      await loadVersionDetail(currentVersionId.value)
    }
  } catch (error) {
    ElMessage.error(error.message || lt('加载版本列表失败', '載入版本列表失敗', 'Failed to load version list'))
  } finally {
    loading.value = false
  }
}

async function loadGameDetail(gameId) {
  const base = games.value.find((item) => item.id === Number(gameId))
  if (!base) {
    selectedGameDetail.value = null
    return
  }
  try {
    const [profileRes, versionsRes, impactRes] = await Promise.all([
      getOpsGameProfile(base.id),
      getGameVersions(base.id),
      getOpsGameGovernanceImpact(base.id)
    ])
    const versionRows = Array.isArray(versionsRes.data) ? versionsRes.data : []
    selectedGameDetail.value = {
      ...base,
      profile: profileRes.data || {},
      versions: versionRows,
      governanceImpact: impactRes.data || {}
    }
  } catch (error) {
    ElMessage.error(error.message || lt('加载游戏详情失败', '載入遊戲詳情失敗', 'Failed to load game detail'))
  }
}

async function loadVersionDetail(versionId) {
  const version = versions.value.find((item) => item.id === Number(versionId))
  if (!version) {
    selectedVersionDetail.value = null
    return
  }
  selectedVersionDetail.value = version
  if (currentGameId.value && (!selectedGameDetail.value || selectedGameDetail.value.id !== currentGameId.value)) {
    await loadGameDetail(currentGameId.value)
  }
}

function openGameDetail(row) {
  router.push({ name: 'OpsGameDetail', params: { gameId: row.id } })
}

function openGameDetailById(gameId) {
  if (!gameId) return
  router.push({ name: 'OpsGameDetail', params: { gameId } })
}

function closeGameDetail() {
  selectedGameDetail.value = null
  router.push({ name: 'OpsGameList' })
}

function openVersionDetail(row) {
  router.push({ name: 'OpsVersionDetail', params: { versionId: row.id } })
}

function closeVersionDetail() {
  selectedVersionDetail.value = null
  router.push({ name: 'OpsVersionList' })
}

function gotoCategoryModule() {
  router.push('/pro/foundation-config/categories/list')
}

function handleSelectionChange(rows) {
  selectedGameIds.value = Array.isArray(rows) ? rows.map((row) => row.id).filter(Boolean) : []
}

function handleCreateFileChange(file) {
  createFile.value = file.raw || file
}

function handleCreateFileRemove() {
  createFile.value = null
}

function handleVersionFileChange(file) {
  versionUploadFile.value = file.raw || file
}

function handleVersionFileRemove() {
  versionUploadFile.value = null
}

function openCreateGameDialog() {
  resetCreateForm()
  createVisible.value = true
}

function openVersionUploadDialog(game) {
  resetVersionUploadForm()
  versionUploadGameId.value = game.id
  versionUploadForm.targetGameName = `${game.name || '-'} (${game.appId || '-'})`
  versionUploadVisible.value = true
}

function openEdit(row) {
  editingGameId.value = row.id
  editorForm.name = row.name || ''
  editorForm.description = row.description || ''
  editorForm.iconUrl = row.iconUrl || ''
  editorForm.category = row.category || ''
  editorForm.tags = normalizeTags(row.tags)
  editorForm.version = row.version || ''
  editorForm.requiresOnline = Boolean(row.requiresOnline)
  editorVisible.value = true
}

function openVisibilityControl(row) {
  visibilityGameId.value = row.id
  visibilityForm.visibilityStatus = row.visibilityStatus || 'VISIBLE'
  visibilityForm.reason = row.visibilityReason || ''
  visibilityForm.visibilityUntil = row.visibilityUntil ? String(row.visibilityUntil).slice(0, 16) : ''
  visibilityVisible.value = true
}

function openBatchVisibility(status) {
  batchVisibilityForm.visibilityStatus = status
  batchVisibilityForm.reason = ''
  batchVisibilityForm.visibilityUntil = ''
  batchVisibilityVisible.value = true
}

function openGovernanceScope(row) {
  governanceGameId.value = row.id
  governanceForm.channelMode = row.governanceScope?.channelMode || 'ALL'
  governanceForm.channels = Array.isArray(row.governanceScope?.channels) ? [...row.governanceScope.channels] : []
  governanceForm.regionMode = row.governanceScope?.regionMode || 'ALL'
  governanceForm.regions = Array.isArray(row.governanceScope?.regions) ? [...row.governanceScope.regions] : []
  governanceForm.versionMode = row.governanceScope?.versionMode || 'ALL'
  governanceForm.versionMin = row.governanceScope?.versionMin || ''
  governanceForm.versionMax = row.governanceScope?.versionMax || ''
  governanceForm.blockedVersions = Array.isArray(row.governanceScope?.blockedVersions) ? [...row.governanceScope.blockedVersions] : []
  governanceForm.governanceNote = row.governanceScope?.governanceNote || ''
  governanceVisible.value = true
}

async function promptReason(title, placeholder) {
  const { value } = await ElMessageBox.prompt(
    placeholder || lt('请填写原因，至少 2 个字', '請填寫原因，至少 2 個字', 'Please input a reason with at least 2 characters'),
    title,
    {
      confirmButtonText: lt('确认', '確認', 'Confirm'),
      cancelButtonText: lt('取消', '取消', 'Cancel'),
      inputPattern: /^.{2,}$/u,
      inputErrorMessage: lt('原因至少需要 2 个字', '原因至少需要 2 個字', 'Reason must be at least 2 characters')
    }
  )
  return value.trim()
}

async function submitCreateGame() {
  if (!createFile.value) {
    ElMessage.warning(lt('请先选择 ZIP 包', '請先選擇 ZIP 包', 'Please select a ZIP package first'))
    return
  }
  creating.value = true
  try {
    const formData = new FormData()
    formData.append('file', createFile.value)
    if (createForm.name.trim()) formData.append('name', createForm.name.trim())
    if (createForm.description.trim()) formData.append('description', createForm.description.trim())
    if (createForm.category) formData.append('category', createForm.category)
    createForm.tags.forEach((tag) => formData.append('tags', tag))
    formData.append('requiresOnline', String(Boolean(createForm.requiresOnline)))
    const res = await request({
      url: '/admin/ops/games/upload',
      method: 'post',
      data: formData
    })
    createVisible.value = false
    ElMessage.success(lt('游戏已创建并进入上传处理队列', '遊戲已建立並進入上傳處理佇列', 'Game created and queued for package processing'))
    await loadGames({ force: true })
    if (res.data?.id) {
      router.push({ name: 'OpsGameDetail', params: { gameId: res.data.id } })
      await loadGameDetail(res.data.id)
    }
  } catch (error) {
    ElMessage.error(error.message || lt('上传新游戏失败', '上傳新遊戲失敗', 'Failed to upload new game'))
  } finally {
    creating.value = false
  }
}

async function submitVersionUpload() {
  if (!versionUploadGameId.value) return
  if (!versionUploadFile.value) {
    ElMessage.warning(lt('请先选择 ZIP 包', '請先選擇 ZIP 包', 'Please select a ZIP package first'))
    return
  }
  uploadingVersion.value = true
  try {
    const formData = new FormData()
    formData.append('file', versionUploadFile.value)
    if (versionUploadForm.versionName.trim()) {
      formData.append('versionName', versionUploadForm.versionName.trim())
    }
    const res = await request({
      url: `/admin/ops/games/${versionUploadGameId.value}/versions/upload`,
      method: 'post',
      data: formData
    })
    versionUploadVisible.value = false
    ElMessage.success(lt('版本已进入上传处理队列', '版本已進入上傳處理佇列', 'Version queued for package processing'))
    await Promise.all([
      loadGames({ force: true, syncRouteDetail: false }),
      loadVersions({ force: true, syncRouteDetail: false })
    ])
    if (res.data?.id) {
      router.push({ name: 'OpsVersionDetail', params: { versionId: res.data.id } })
      await loadVersionDetail(res.data.id)
    }
    if (selectedGameDetail.value?.id === versionUploadGameId.value) {
      await loadGameDetail(versionUploadGameId.value)
    }
  } catch (error) {
    ElMessage.error(error.message || lt('上传版本失败', '上傳版本失敗', 'Failed to upload version'))
  } finally {
    uploadingVersion.value = false
  }
}

async function submitForAudit(row) {
  try {
    await submitGameForAudit(row.id, lt('运营后台提交审核', '營運後台提交審核', 'Submitted from operations portal'))
    ElMessage.success(lt('已提交审核', '已提交審核', 'Submitted for review'))
    await loadGames({ force: true })
    if (selectedGameDetail.value?.id === row.id) {
      await loadGameDetail(row.id)
    }
  } catch (error) {
    ElMessage.error(error.message || lt('提交审核失败', '提交審核失敗', 'Failed to submit for review'))
  }
}

async function submitSpecificVersion(version) {
  try {
    const note = await promptReason(
      lt(`提交审核：${version.versionName || version.version}`, `提交審核：${version.versionName || version.version}`, `Submit review: ${version.versionName || version.version}`),
      lt('请填写提交说明，至少 2 个字', '請填寫提交說明，至少 2 個字', 'Please input a submission note with at least 2 characters')
    )
    await request({
      url: `/game/${version.gameId}/submit-version/${version.id}`,
      method: 'post',
      data: { note }
    })
    ElMessage.success(lt('版本已提交审核', '版本已提交審核', 'Version submitted for review'))
    await Promise.all([
      loadGames({ force: true, syncRouteDetail: false }),
      loadVersions({ force: true, syncRouteDetail: false })
    ])
    if (currentVersionId.value === version.id) {
      await loadVersionDetail(version.id)
    }
    if (selectedGameDetail.value?.id === version.gameId) {
      await loadGameDetail(version.gameId)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('版本提交审核失败', '版本提交審核失敗', 'Failed to submit version for review'))
    }
  }
}

async function directPublishLatestDraft(game) {
  const version = latestDraftLikeVersion(game)
  if (!version) {
    ElMessage.warning(lt('没有可直接上架的草稿版本', '沒有可直接上架的草稿版本', 'No draft version is available for direct go-live'))
    return
  }
  await directPublishVersion(version)
}

async function directPublishVersion(version) {
  try {
    const reason = await promptReason(
      lt(`直接上架：${version.versionName || version.version}`, `直接上架：${version.versionName || version.version}`, `Direct go-live: ${version.versionName || version.version}`),
      lt('请填写上架原因，至少 2 个字', '請填寫上架原因，至少 2 個字', 'Please input a go-live reason with at least 2 characters')
    )
    await request({
      url: `/admin/ops/games/${version.gameId}/versions/${version.id}/direct-publish`,
      method: 'post',
      data: { reason, forceUpdate: Boolean(version.forcedUpdate) }
    })
    ElMessage.success(lt('版本已直接上架', '版本已直接上架', 'Version is now live'))
    await Promise.all([
      loadGames({ force: true, syncRouteDetail: false }),
      loadVersions({ force: true, syncRouteDetail: false })
    ])
    if (currentVersionId.value === version.id) {
      await loadVersionDetail(version.id)
    }
    if (selectedGameDetail.value?.id === version.gameId) {
      await loadGameDetail(version.gameId)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('直接上架失败', '直接上架失敗', 'Failed to direct go-live'))
    }
  }
}

async function rollbackVersion(version) {
  try {
    const reason = await promptReason(
      lt(`回滚到版本：${version.versionName || version.version}`, `回滾到版本：${version.versionName || version.version}`, `Rollback to version: ${version.versionName || version.version}`),
      lt('请填写回滚原因，至少 2 个字', '請填寫回滾原因，至少 2 個字', 'Please input a rollback reason with at least 2 characters')
    )
    await request({
      url: `/game/${version.gameId}/rollback/${version.id}`,
      method: 'post',
      data: { reason }
    })
    ElMessage.success(lt('已回滚到目标版本', '已回滾到目標版本', 'Rolled back to target version'))
    await Promise.all([
      loadGames({ force: true, syncRouteDetail: false }),
      loadVersions({ force: true, syncRouteDetail: false })
    ])
    if (selectedGameDetail.value?.id === version.gameId) {
      await loadGameDetail(version.gameId)
    }
    if (currentVersionId.value === version.id) {
      await loadVersionDetail(version.id)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('回滚失败', '回滾失敗', 'Failed to rollback version'))
    }
  }
}

async function approvePendingGame(row) {
  try {
    const reason = await promptReason(lt(`审核通过：${row.name}`, `審核通過：${row.name}`, `Approve: ${row.name}`))
    await approveGame(row.id, reason)
    ElMessage.success(lt('审核已通过', '審核已通過', 'Approval completed'))
    await loadGames({ force: true })
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('审核通过失败', '審核通過失敗', 'Failed to approve game'))
    }
  }
}

async function rejectPendingGame(row) {
  try {
    const reason = await promptReason(lt(`驳回版本：${row.name}`, `駁回版本：${row.name}`, `Reject: ${row.name}`))
    await rejectGame(row.id, reason)
    ElMessage.success(lt('已驳回', '已駁回', 'Rejected'))
    await loadGames({ force: true })
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('驳回失败', '駁回失敗', 'Failed to reject'))
    }
  }
}

async function inspectVersion(row) {
  let latestVersionText = row.version || lt('暂无', '暫無', 'N/A')
  let manifestStatus = lt('暂无', '暫無', 'N/A')
  let manifestSummary = lt('暂无', '暫無', 'N/A')
  try {
    const res = await getGameVersions(row.id)
    const latest = Array.isArray(res.data) ? res.data[0] : null
    if (latest) {
      latestVersionText = latest.versionName || latestVersionText
      manifestStatus = latest.hostedManifestValid ? lt('通过', '通過', 'Passed') : lt('未通过', '未通過', 'Failed')
      manifestSummary = latest.hostedManifestSummary || manifestSummary
    }
  } catch (error) {
    manifestSummary = error.message || lt('加载版本检查结果失败', '載入版本檢查結果失敗', 'Failed to load manifest inspection result')
  }
  await ElMessageBox.alert(
    lt(
      `游戏名称：${row.name}\nAppID：${row.appId || '-'}\n状态：${getStatusText(row.status)}\n最新版本：${latestVersionText}\nManifest 检查：${manifestStatus}\nManifest 摘要：${manifestSummary}`,
      `遊戲名稱：${row.name}\nAppID：${row.appId || '-'}\n狀態：${getStatusText(row.status)}\n最新版本：${latestVersionText}\nManifest 檢查：${manifestStatus}\nManifest 摘要：${manifestSummary}`,
      `Game: ${row.name}\nAppID: ${row.appId || '-'}\nStatus: ${getStatusText(row.status)}\nLatest Version: ${latestVersionText}\nManifest Check: ${manifestStatus}\nManifest Summary: ${manifestSummary}`
    ),
    lt('版本检查', '版本檢查', 'Version Check'),
    { confirmButtonText: lt('知道了', '知道了', 'OK') }
  )
}

async function saveGame() {
  const name = editorForm.name.trim()
  if (!name) {
    ElMessage.warning(lt('请输入游戏名称', '請輸入遊戲名稱', 'Please input game name'))
    return
  }
  saving.value = true
  try {
    await updateOpsGameMetadata(editingGameId.value, {
      name,
      description: editorForm.description.trim(),
      iconUrl: editorForm.iconUrl.trim(),
      category: editorForm.category || '',
      tags: editorForm.tags,
      version: editorForm.version.trim(),
      requiresOnline: Boolean(editorForm.requiresOnline)
    })
    editorVisible.value = false
    ElMessage.success(lt('游戏信息已保存', '遊戲資訊已儲存', 'Game info saved'))
    await loadGames({ force: true })
    if (selectedGameDetail.value?.id === editingGameId.value) {
      await loadGameDetail(editingGameId.value)
    }
  } catch (error) {
    ElMessage.error(error.message || lt('保存游戏信息失败', '儲存遊戲資訊失敗', 'Failed to save game info'))
  } finally {
    saving.value = false
  }
}

async function submitVisibilityControl() {
  if (!visibilityGameId.value) return
  if (visibilityForm.visibilityStatus !== 'VISIBLE' && visibilityForm.reason.trim().length < 2) {
    ElMessage.warning(lt('封控原因至少 2 个字符', '封控原因至少 2 個字元', 'Reason must be at least 2 characters'))
    return
  }
  visibilitySaving.value = true
  try {
    await updateOpsGameVisibility(visibilityGameId.value, {
      visibilityStatus: visibilityForm.visibilityStatus,
      reason: visibilityForm.reason.trim(),
      visibilityUntil: visibilityForm.visibilityUntil ? `${visibilityForm.visibilityUntil}:00` : null
    })
    visibilityVisible.value = false
    ElMessage.success(lt('展示控制已更新', '展示控制已更新', 'Visibility control updated'))
    await loadGames({ force: true })
    if (selectedGameDetail.value?.id === visibilityGameId.value) {
      await loadGameDetail(visibilityGameId.value)
    }
  } catch (error) {
    ElMessage.error(error.message || lt('更新展示控制失败', '更新展示控制失敗', 'Failed to update visibility control'))
  } finally {
    visibilitySaving.value = false
  }
}

async function submitBatchVisibilityControl() {
  if (!selectedGameIds.value.length) return
  if (batchVisibilityForm.visibilityStatus !== 'VISIBLE' && batchVisibilityForm.reason.trim().length < 2) {
    ElMessage.warning(lt('封控原因至少 2 个字符', '封控原因至少 2 個字元', 'Reason must be at least 2 characters'))
    return
  }
  batchVisibilitySaving.value = true
  try {
    await updateOpsGameVisibilityBatch({
      gameIds: selectedGameIds.value,
      visibilityStatus: batchVisibilityForm.visibilityStatus,
      reason: batchVisibilityForm.reason.trim(),
      visibilityUntil: batchVisibilityForm.visibilityUntil ? `${batchVisibilityForm.visibilityUntil}:00` : null
    })
    batchVisibilityVisible.value = false
    selectedGameIds.value = []
    ElMessage.success(lt('批量展示控制已更新', '批量展示控制已更新', 'Batch visibility control updated'))
    await loadGames({ force: true })
  } catch (error) {
    ElMessage.error(error.message || lt('批量更新展示控制失败', '批量更新展示控制失敗', 'Failed to batch update visibility control'))
  } finally {
    batchVisibilitySaving.value = false
  }
}

async function submitGovernanceScope() {
  if (!governanceGameId.value) return
  if (governanceForm.channelMode !== 'ALL' && governanceForm.channels.length === 0) {
    ElMessage.warning(lt('请至少配置一个渠道', '請至少配置一個渠道', 'Please configure at least one channel'))
    return
  }
  if (governanceForm.regionMode !== 'ALL' && governanceForm.regions.length === 0) {
    ElMessage.warning(lt('请至少配置一个地区', '請至少配置一個地區', 'Please configure at least one region'))
    return
  }
  if (governanceForm.versionMode === 'MIN_VERSION' && !governanceForm.versionMin.trim()) {
    ElMessage.warning(lt('请填写最低版本', '請填寫最低版本', 'Please input the minimum version'))
    return
  }
  if (governanceForm.versionMode === 'RANGE' && !governanceForm.versionMin.trim() && !governanceForm.versionMax.trim()) {
    ElMessage.warning(lt('请至少填写一个版本边界', '請至少填寫一個版本邊界', 'Please provide at least one version bound'))
    return
  }
  if (governanceForm.versionMode === 'BLOCKLIST' && governanceForm.blockedVersions.length === 0) {
    ElMessage.warning(lt('请至少配置一个拦截版本', '請至少配置一個攔截版本', 'Please configure at least one blocked version'))
    return
  }
  governanceSaving.value = true
  try {
    await updateOpsGameGovernanceScope(governanceGameId.value, {
      channelMode: governanceForm.channelMode,
      channels: governanceForm.channelMode === 'ALL' ? [] : governanceForm.channels,
      regionMode: governanceForm.regionMode,
      regions: governanceForm.regionMode === 'ALL' ? [] : governanceForm.regions,
      versionMode: governanceForm.versionMode,
      versionMin: governanceForm.versionMode === 'ALL' || governanceForm.versionMode === 'BLOCKLIST' ? null : governanceForm.versionMin.trim() || null,
      versionMax: governanceForm.versionMode === 'RANGE' ? governanceForm.versionMax.trim() || null : null,
      blockedVersions: governanceForm.versionMode === 'BLOCKLIST' ? governanceForm.blockedVersions : [],
      governanceNote: governanceForm.governanceNote.trim() || null
    })
    governanceVisible.value = false
    ElMessage.success(lt('多维治理规则已更新', '多維治理規則已更新', 'Governance rules updated'))
    await loadGames({ force: true })
    if (selectedGameDetail.value?.id === governanceGameId.value) {
      await loadGameDetail(governanceGameId.value)
    }
  } catch (error) {
    ElMessage.error(error.message || lt('保存治理规则失败', '儲存治理規則失敗', 'Failed to save governance rules'))
  } finally {
    governanceSaving.value = false
  }
}

watch(
  () => route.fullPath,
  async () => {
    if (isGameMode.value) {
      selectedVersionDetail.value = null
      if (currentGameId.value) {
        if (!games.value.length) {
          await loadGames({ force: true })
        } else {
          await loadGameDetail(currentGameId.value)
        }
      } else {
        selectedGameDetail.value = null
      }
    } else {
      selectedGameDetail.value = null
      if (currentVersionId.value) {
        if (!versions.value.length) {
          await loadVersions({ force: true })
        } else {
          await loadVersionDetail(currentVersionId.value)
        }
      } else {
        selectedVersionDetail.value = null
      }
    }
  },
  { immediate: true }
)

watch(
  () => route.query.status,
  (status) => {
    statusFilter.value = typeof status === 'string' ? status : ''
  },
  { immediate: true }
)

onMounted(async () => {
  resetCreateForm()
  resetVersionUploadForm()
  resetEditor()
  await loadCategories()
  if (isGameMode.value) {
    await loadGames({ force: true })
  } else {
    await loadVersions({ force: true })
  }
})
</script>

<style scoped>
.pro-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.head-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.panel-title { font-size: 17px; font-weight: 800; color: #101828; }
.panel-subtitle { margin-top: 6px; color: #667085; font-size: 13px; }

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.keyword-input {
  width: 260px;
}

.filter-select {
  width: 180px;
  max-width: 100%;
}

.filter-select-sm {
  width: 150px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.status-card {
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 14px 16px;
  cursor: pointer;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.status-label { color: #667085; font-size: 13px; }
.status-value { margin-top: 8px; font-size: 28px; font-weight: 700; color: #111827; }
.status-tip { margin-top: 8px; color: #94a3b8; font-size: 12px; }

.ops-hint {
  margin-bottom: 16px;
}

.detail-card {
  margin-bottom: 16px;
}

.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.detail-title {
  font-size: 18px;
  font-weight: 800;
  color: #111827;
}

.detail-subtitle {
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
}

.detail-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.subsection {
  margin-top: 18px;
}

@media (max-width: 920px) {
  .head-row,
  .detail-head {
    flex-direction: column;
  }

  .actions,
  .detail-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .keyword-input,
  .filter-select,
  .filter-select-sm {
    width: 100%;
  }
}

.subsection-title {
  margin-bottom: 12px;
  font-weight: 700;
  color: #111827;
}
</style>
