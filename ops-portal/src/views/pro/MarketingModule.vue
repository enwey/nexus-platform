<template>
  <div class="pro-page">
    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ lt('活动与营销中心', '活動與營銷中心', 'Activity & Marketing Center') }}</div>
            <div class="panel-subtitle">{{ lt('启动广告、会场、弹窗、任务、礼包码全部分轨管理，便于后续继续扩展活动资产。', '啟動廣告、會場、彈窗、任務、禮包碼全部分軌管理，便於後續繼續擴展活動資產。', 'Launch ads, hubs, popups, tasks, and gift-code campaigns are managed in separate tracks for long-term expansion.') }}</div>
          </div>
          <div class="actions">
            <el-button :loading="loading" @click="loadData">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <div class="metric-row">
        <div class="metric-chip">{{ lt('在线广告', '在線廣告', 'Live Ads') }} {{ liveCount }}</div>
        <div class="metric-chip">{{ lt('排期广告', '排期廣告', 'Scheduled Ads') }} {{ scheduledCount }}</div>
        <div class="metric-chip">{{ lt('在线活动', '在線活動', 'Live Campaigns') }} {{ liveCampaignCount }}</div>
        <div class="metric-chip">{{ lt('排期活动', '排期活動', 'Scheduled Campaigns') }} {{ scheduledCampaignCount }}</div>
        <div class="metric-chip">{{ lt('在线弹窗', '在線彈窗', 'Live Popups') }} {{ livePopupCount }}</div>
        <div class="metric-chip">{{ lt('排期弹窗', '排期彈窗', 'Scheduled Popups') }} {{ scheduledPopupCount }}</div>
        <div class="metric-chip">{{ lt('在线任务', '在線任務', 'Live Tasks') }} {{ liveTaskCount }}</div>
        <div class="metric-chip">{{ lt('排期任务', '排期任務', 'Scheduled Tasks') }} {{ scheduledTaskCount }}</div>
        <div class="metric-chip">{{ lt('在线礼包码', '在線禮包碼', 'Live Gift Codes') }} {{ liveGiftCount }}</div>
        <div class="metric-chip">{{ lt('排期礼包码', '排期禮包碼', 'Scheduled Gift Codes') }} {{ scheduledGiftCount }}</div>
      </div>

      <el-table
        class="analytics-table"
        :data="marketingAnalytics.metrics || []"
        size="small"
        :empty-text="lt('暂无营销效果统计', '暫無營銷效果統計', 'No marketing analytics yet')"
      >
        <el-table-column prop="assetName" :label="lt('资产轨道', '資產軌道', 'Asset Track')" min-width="160" />
        <el-table-column prop="total" :label="lt('总数', '總數', 'Total')" width="80" />
        <el-table-column prop="live" :label="lt('在线', '在線', 'Live')" width="80" />
        <el-table-column prop="scheduled" :label="lt('排期中', '排期中', 'Scheduled')" width="90" />
        <el-table-column prop="paused" :label="lt('暂停/停用', '暫停/停用', 'Paused')" width="110" />
        <el-table-column prop="expired" :label="lt('过期/售罄', '過期/售罄', 'Expired')" width="110" />
        <el-table-column prop="stock" :label="lt('可用库存', '可用庫存', 'Stock')" width="100" />
        <el-table-column prop="redeemed" :label="lt('已兑换', '已兌換', 'Redeemed')" width="90" />
      </el-table>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24">
        <el-card class="panel-card">
          <template #header>
            <div class="card-header-inline">
              <span>{{ lt('弹窗活动', '彈窗活動', 'Popup Campaigns') }}</span>
              <div class="actions">
                <el-button :disabled="!selectedPopupIds.length" @click="runMarketingBatch('POPUP', 'PAUSED')">{{ lt('批量暂停', '批量暫停', 'Batch Pause') }}</el-button>
                <el-button type="success" plain :disabled="!selectedPopupIds.length" @click="runMarketingBatch('POPUP', 'PUBLISHED')">{{ lt('批量发布', '批量發佈', 'Batch Publish') }}</el-button>
                <el-button type="primary" @click="openPopupCreate">{{ lt('新建弹窗', '新增彈窗', 'Create Popup') }}</el-button>
              </div>
            </div>
          </template>
          <el-table :data="popupCampaigns" row-key="id" v-loading="loading" :empty-text="lt('暂无弹窗活动', '暫無彈窗活動', 'No popup campaigns')" @selection-change="onSelectionChange('POPUP', $event)">
            <el-table-column type="selection" width="50" />
            <el-table-column prop="popupCode" :label="lt('编码', '編碼', 'Code')" width="180" />
            <el-table-column prop="title" :label="lt('标题', '標題', 'Title')" min-width="180" />
            <el-table-column prop="triggerScene" :label="lt('触发场景', '觸發場景', 'Trigger')" width="140" />
            <el-table-column prop="audience" :label="lt('受众', '受眾', 'Audience')" width="120" />
            <el-table-column :label="lt('状态', '狀態', 'Status')" width="120">
              <template #default="{ row }"><el-tag :type="effectiveStateType(row.effectiveStatus)">{{ row.effectiveStatus }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="frequencyLimitPerDay" :label="lt('每日频控', '每日頻控', 'Daily Limit')" width="110" />
            <el-table-column :label="lt('时间窗', '時間窗', 'Schedule')" min-width="220">
              <template #default="{ row }">{{ formatRange(row.startAt, row.endAt) }}</template>
            </el-table-column>
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="240" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openPopupEdit(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
                <el-button v-if="row.status !== 'PUBLISHED'" link type="success" @click="changePopupStatus(row, 'PUBLISHED')">{{ lt('发布', '發布', 'Publish') }}</el-button>
                <el-button v-if="row.status === 'PUBLISHED'" link type="warning" @click="changePopupStatus(row, 'PAUSED')">{{ lt('暂停', '暫停', 'Pause') }}</el-button>
                <el-button v-if="row.status !== 'ARCHIVED'" link @click="changePopupStatus(row, 'ARCHIVED')">{{ lt('归档', '歸檔', 'Archive') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24">
        <el-card class="panel-card">
          <template #header>
            <div class="card-header-inline">
              <span>{{ lt('礼包码活动', '禮包碼活動', 'Gift Code Campaigns') }}</span>
              <div class="actions">
                <el-button :disabled="!selectedGiftIds.length" @click="runMarketingBatch('GIFT', 'PAUSED')">{{ lt('批量暂停', '批量暫停', 'Batch Pause') }}</el-button>
                <el-button type="success" plain :disabled="!selectedGiftIds.length" @click="runMarketingBatch('GIFT', 'PUBLISHED')">{{ lt('批量发布', '批量發佈', 'Batch Publish') }}</el-button>
                <el-button type="primary" @click="openGiftCreate">{{ lt('新建礼包码活动', '新增禮包碼活動', 'Create Gift Code Campaign') }}</el-button>
              </div>
            </div>
          </template>
          <el-table :data="giftCampaigns" row-key="id" v-loading="loading" :empty-text="lt('暂无礼包码活动', '暫無禮包碼活動', 'No gift code campaigns')" @selection-change="onSelectionChange('GIFT', $event)">
            <el-table-column type="selection" width="50" />
            <el-table-column prop="campaignCode" :label="lt('编码', '編碼', 'Code')" width="180" />
            <el-table-column prop="title" :label="lt('标题', '標題', 'Title')" min-width="180" />
            <el-table-column prop="rewardType" :label="lt('奖励类型', '獎勵類型', 'Reward Type')" width="130" />
            <el-table-column prop="rewardSummary" :label="lt('奖励说明', '獎勵說明', 'Reward Summary')" min-width="180" />
            <el-table-column :label="lt('库存', '庫存', 'Stock')" width="160">
              <template #default="{ row }">{{ row.availableStock }} / {{ row.totalStock }}</template>
            </el-table-column>
            <el-table-column :label="lt('已兑换', '已兌換', 'Redeemed')" width="100">
              <template #default="{ row }">{{ row.redeemedStock }}</template>
            </el-table-column>
            <el-table-column :label="lt('状态', '狀態', 'Status')" width="120">
              <template #default="{ row }"><el-tag :type="effectiveStateType(row.effectiveStatus)">{{ row.effectiveStatus }}</el-tag></template>
            </el-table-column>
            <el-table-column :label="lt('时间窗', '時間窗', 'Schedule')" min-width="220">
              <template #default="{ row }">{{ formatRange(row.startAt, row.endAt) }}</template>
            </el-table-column>
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="300" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openGiftEdit(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
                <el-button link @click="viewGiftCodes(row)">{{ lt('码池', '碼池', 'Codes') }}</el-button>
                <el-button v-if="row.status !== 'PUBLISHED'" link type="success" @click="changeGiftStatus(row, 'PUBLISHED')">{{ lt('发布', '發布', 'Publish') }}</el-button>
                <el-button v-if="row.status === 'PUBLISHED'" link type="warning" @click="changeGiftStatus(row, 'PAUSED')">{{ lt('暂停', '暫停', 'Pause') }}</el-button>
                <el-button v-if="row.status !== 'ARCHIVED'" link @click="changeGiftStatus(row, 'ARCHIVED')">{{ lt('归档', '歸檔', 'Archive') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24">
        <el-card class="panel-card">
          <template #header>
            <div class="card-header-inline">
              <span>{{ lt('任务活动', '任務活動', 'Task Campaigns') }}</span>
              <div class="actions">
                <el-button :disabled="!selectedTaskIds.length" @click="runMarketingBatch('TASK', 'PAUSED')">{{ lt('批量暂停', '批量暫停', 'Batch Pause') }}</el-button>
                <el-button type="success" plain :disabled="!selectedTaskIds.length" @click="runMarketingBatch('TASK', 'PUBLISHED')">{{ lt('批量发布', '批量發佈', 'Batch Publish') }}</el-button>
                <el-button type="primary" @click="openTaskCreate">{{ lt('新建任务', '新增任務', 'Create Task') }}</el-button>
              </div>
            </div>
          </template>
          <el-table :data="taskCampaigns" row-key="id" v-loading="loading" :empty-text="lt('暂无任务活动', '暫無任務活動', 'No task campaigns')" @selection-change="onSelectionChange('TASK', $event)">
            <el-table-column type="selection" width="50" />
            <el-table-column prop="taskCode" :label="lt('编码', '編碼', 'Code')" width="180" />
            <el-table-column prop="title" :label="lt('标题', '標題', 'Title')" min-width="180" />
            <el-table-column prop="taskType" :label="lt('任务类型', '任務類型', 'Task Type')" width="130" />
            <el-table-column prop="rewardType" :label="lt('奖励类型', '獎勵類型', 'Reward Type')" width="130" />
            <el-table-column prop="rewardValue" :label="lt('奖励值', '獎勵值', 'Reward Value')" min-width="140" />
            <el-table-column :label="lt('状态', '狀態', 'Status')" width="120">
              <template #default="{ row }"><el-tag :type="effectiveStateType(row.effectiveStatus)">{{ row.effectiveStatus }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="dailyLimit" :label="lt('每日上限', '每日上限', 'Daily Limit')" width="100" />
            <el-table-column :label="lt('时间窗', '時間窗', 'Schedule')" min-width="220">
              <template #default="{ row }">{{ formatRange(row.startAt, row.endAt) }}</template>
            </el-table-column>
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="240" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openTaskEdit(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
                <el-button v-if="row.status !== 'PUBLISHED'" link type="success" @click="changeTaskStatus(row, 'PUBLISHED')">{{ lt('发布', '發布', 'Publish') }}</el-button>
                <el-button v-if="row.status === 'PUBLISHED'" link type="warning" @click="changeTaskStatus(row, 'PAUSED')">{{ lt('暂停', '暫停', 'Pause') }}</el-button>
                <el-button v-if="row.status !== 'ARCHIVED'" link @click="changeTaskStatus(row, 'ARCHIVED')">{{ lt('归档', '歸檔', 'Archive') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24">
        <el-card class="panel-card">
          <template #header>
            <div class="card-header-inline">
              <span>{{ lt('活动会场', '活動會場', 'Campaign Hubs') }}</span>
              <div class="actions">
                <el-button :disabled="!selectedCampaignIds.length" @click="runMarketingBatch('CAMPAIGN', 'PAUSED')">{{ lt('批量暂停', '批量暫停', 'Batch Pause') }}</el-button>
                <el-button type="success" plain :disabled="!selectedCampaignIds.length" @click="runMarketingBatch('CAMPAIGN', 'PUBLISHED')">{{ lt('批量发布', '批量發佈', 'Batch Publish') }}</el-button>
                <el-button type="primary" @click="openCampaignCreate">{{ lt('新建活动', '新增活動', 'Create Campaign') }}</el-button>
              </div>
            </div>
          </template>
          <el-table :data="campaigns" row-key="id" v-loading="loading" :empty-text="lt('暂无活动', '暫無活動', 'No campaigns')" @selection-change="onSelectionChange('CAMPAIGN', $event)">
            <el-table-column type="selection" width="50" />
            <el-table-column prop="campaignCode" :label="lt('编码', '編碼', 'Code')" width="180" />
            <el-table-column prop="title" :label="lt('标题', '標題', 'Title')" min-width="200" />
            <el-table-column prop="campaignType" :label="lt('类型', '類型', 'Type')" width="120" />
            <el-table-column prop="audience" :label="lt('受众', '受眾', 'Audience')" width="130" />
            <el-table-column :label="lt('状态', '狀態', 'Status')" width="130">
              <template #default="{ row }"><el-tag :type="effectiveStateType(row.effectiveStatus)">{{ row.effectiveStatus }}</el-tag></template>
            </el-table-column>
            <el-table-column :label="lt('时间窗', '時間窗', 'Schedule')" min-width="220">
              <template #default="{ row }">{{ formatRange(row.startAt, row.endAt) }}</template>
            </el-table-column>
            <el-table-column prop="priority" :label="lt('优先级', '優先級', 'Priority')" width="100" />
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="240" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openCampaignEdit(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
                <el-button v-if="row.status !== 'PUBLISHED'" link type="success" @click="changeCampaignStatus(row, 'PUBLISHED')">{{ lt('发布', '發布', 'Publish') }}</el-button>
                <el-button v-if="row.status === 'PUBLISHED'" link type="warning" @click="changeCampaignStatus(row, 'PAUSED')">{{ lt('暂停', '暫停', 'Pause') }}</el-button>
                <el-button v-if="row.status !== 'ARCHIVED'" link @click="changeCampaignStatus(row, 'ARCHIVED')">{{ lt('归档', '歸檔', 'Archive') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24">
        <el-card class="panel-card">
          <template #header>
            <div class="card-header-inline">
              <span>{{ lt('启动广告', '啟動廣告', 'Launch Ads') }}</span>
              <div class="actions">
                <el-button :disabled="!selectedAdIds.length" @click="runMarketingBatch('LAUNCH_AD', 'INACTIVE')">{{ lt('批量停用', '批量停用', 'Batch Deactivate') }}</el-button>
                <el-button type="success" plain :disabled="selectedAdIds.length !== 1" @click="runMarketingBatch('LAUNCH_AD', 'ACTIVE')">{{ lt('激活选中', '激活選中', 'Activate Selected') }}</el-button>
                <el-button type="primary" @click="openCreate">{{ lt('新建启动广告', '新建啟動廣告', 'Create Launch Ad') }}</el-button>
              </div>
            </div>
          </template>
          <el-table :data="ads.items" row-key="id" v-loading="loading" :empty-text="lt('暂无启动广告', '暫無啟動廣告', 'No launch ads')" @selection-change="onSelectionChange('LAUNCH_AD', $event)">
            <el-table-column type="selection" width="50" />
            <el-table-column prop="code" :label="lt('编码', '編碼', 'Code')" min-width="180" />
            <el-table-column prop="titleZhCn" :label="lt('标题(简中)', '標題(簡中)', 'Title (zh-CN)')" min-width="180" show-overflow-tooltip />
            <el-table-column prop="imageUrl" :label="lt('图片 URL', '圖片 URL', 'Image URL')" min-width="220" show-overflow-tooltip />
            <el-table-column prop="targetUrl" :label="lt('跳转 URL', '跳轉 URL', 'Target URL')" min-width="220" show-overflow-tooltip />
            <el-table-column prop="displaySeconds" :label="lt('展示秒数', '展示秒數', 'Seconds')" width="100" />
            <el-table-column :label="lt('状态', '狀態', 'Status')" width="120">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : row.status === 'DRAFT' ? 'info' : 'warning'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="lt('生效态', '生效態', 'Effective State')" width="130">
              <template #default="{ row }">
                <el-tag :type="effectiveStateType(row.effectiveState)">{{ row.effectiveState || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="lt('投放时间窗', '投放時間窗', 'Schedule')" min-width="220">
              <template #default="{ row }">
                {{ formatRange(row.startAt, row.endAt) }}
              </template>
            </el-table-column>
            <el-table-column :label="lt('更新时间', '更新時間', 'Updated')" min-width="160">
              <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="260" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openEdit(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
                <el-button v-if="row.status !== 'ACTIVE'" link type="success" @click="activate(row)">{{ lt('激活', '激活', 'Activate') }}</el-button>
                <el-button v-if="row.status === 'ACTIVE'" link type="warning" @click="deactivate(row)">{{ lt('停用', '停用', 'Deactivate') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="campaignEditorVisible" :title="editingCampaignId ? lt('编辑活动', '編輯活動', 'Edit Campaign') : lt('新建活动', '新增活動', 'Create Campaign')" width="760px">
      <el-form :model="campaignForm" label-width="130px">
        <el-form-item :label="lt('活动类型', '活動類型', 'Campaign Type')" required>
          <el-select v-model="campaignForm.campaignType" style="width: 100%">
            <el-option label="TOPIC" value="TOPIC" />
            <el-option label="POPUP" value="POPUP" />
            <el-option label="TASK" value="TASK" />
            <el-option label="GIFT" value="GIFT" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('标题', '標題', 'Title')" required><el-input v-model="campaignForm.title" /></el-form-item>
        <el-form-item :label="lt('状态', '狀態', 'Status')">
          <el-select v-model="campaignForm.status" style="width: 100%">
            <el-option label="DRAFT" value="DRAFT" />
            <el-option label="PUBLISHED" value="PUBLISHED" />
            <el-option label="PAUSED" value="PAUSED" />
            <el-option label="ARCHIVED" value="ARCHIVED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('受众', '受眾', 'Audience')">
          <el-select v-model="campaignForm.audience" style="width: 100%">
            <el-option label="ALL" value="ALL" />
            <el-option label="NEW_USER" value="NEW_USER" />
            <el-option label="RETURNING_USER" value="RETURNING_USER" />
            <el-option label="DEVELOPER" value="DEVELOPER" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('落地页 URL', '落地頁 URL', 'Landing URL')"><el-input v-model="campaignForm.landingUrl" /></el-form-item>
        <el-form-item :label="lt('Banner URL', 'Banner URL', 'Banner URL')"><el-input v-model="campaignForm.bannerUrl" /></el-form-item>
        <el-form-item :label="lt('优先级', '優先級', 'Priority')"><el-input-number v-model="campaignForm.priority" :min="0" :max="999" style="width: 100%" /></el-form-item>
        <el-form-item :label="lt('开始时间', '開始時間', 'Start At')">
          <el-date-picker v-model="campaignForm.startAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" clearable />
        </el-form-item>
        <el-form-item :label="lt('结束时间', '結束時間', 'End At')">
          <el-date-picker v-model="campaignForm.endAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" clearable />
        </el-form-item>
        <el-form-item :label="lt('备注', '備註', 'Note')"><el-input v-model="campaignForm.note" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="campaignEditorVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="campaignSaving" @click="saveCampaign">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="popupEditorVisible" :title="editingPopupId ? lt('编辑弹窗', '編輯彈窗', 'Edit Popup') : lt('新建弹窗', '新增彈窗', 'Create Popup')" width="760px">
      <el-form :model="popupForm" label-width="130px">
        <el-form-item :label="lt('标题', '標題', 'Title')" required><el-input v-model="popupForm.title" /></el-form-item>
        <el-form-item :label="lt('状态', '狀態', 'Status')">
          <el-select v-model="popupForm.status" style="width: 100%">
            <el-option label="DRAFT" value="DRAFT" />
            <el-option label="PUBLISHED" value="PUBLISHED" />
            <el-option label="PAUSED" value="PAUSED" />
            <el-option label="ARCHIVED" value="ARCHIVED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('受众', '受眾', 'Audience')">
          <el-select v-model="popupForm.audience" style="width: 100%">
            <el-option label="ALL" value="ALL" />
            <el-option label="NEW_USER" value="NEW_USER" />
            <el-option label="RETURNING_USER" value="RETURNING_USER" />
            <el-option label="DEVELOPER" value="DEVELOPER" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('触发场景', '觸發場景', 'Trigger Scene')">
          <el-select v-model="popupForm.triggerScene" style="width: 100%">
            <el-option label="APP_LAUNCH" value="APP_LAUNCH" />
            <el-option label="TAB_ENTER" value="TAB_ENTER" />
            <el-option label="GAME_EXIT" value="GAME_EXIT" />
            <el-option label="PAY_SUCCESS" value="PAY_SUCCESS" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('落地页 URL', '落地頁 URL', 'Landing URL')"><el-input v-model="popupForm.landingUrl" /></el-form-item>
        <el-form-item :label="lt('图片 URL', '圖片 URL', 'Image URL')"><el-input v-model="popupForm.imageUrl" /></el-form-item>
        <el-form-item :label="lt('按钮文案', '按鈕文案', 'Button Text')"><el-input v-model="popupForm.buttonText" /></el-form-item>
        <el-form-item :label="lt('优先级', '優先級', 'Priority')"><el-input-number v-model="popupForm.priority" :min="0" :max="999" style="width: 100%" /></el-form-item>
        <el-form-item :label="lt('每日频控', '每日頻控', 'Daily Frequency Limit')"><el-input-number v-model="popupForm.frequencyLimitPerDay" :min="1" :max="20" style="width: 100%" /></el-form-item>
        <el-form-item :label="lt('开始时间', '開始時間', 'Start At')">
          <el-date-picker v-model="popupForm.startAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" clearable />
        </el-form-item>
        <el-form-item :label="lt('结束时间', '結束時間', 'End At')">
          <el-date-picker v-model="popupForm.endAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" clearable />
        </el-form-item>
        <el-form-item :label="lt('备注', '備註', 'Note')"><el-input v-model="popupForm.note" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="popupEditorVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="popupSaving" @click="savePopup">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="taskEditorVisible" :title="editingTaskId ? lt('编辑任务', '編輯任務', 'Edit Task') : lt('新建任务', '新增任務', 'Create Task')" width="760px">
      <el-form :model="taskForm" label-width="130px">
        <el-form-item :label="lt('标题', '標題', 'Title')" required><el-input v-model="taskForm.title" /></el-form-item>
        <el-form-item :label="lt('状态', '狀態', 'Status')">
          <el-select v-model="taskForm.status" style="width: 100%">
            <el-option label="DRAFT" value="DRAFT" />
            <el-option label="PUBLISHED" value="PUBLISHED" />
            <el-option label="PAUSED" value="PAUSED" />
            <el-option label="ARCHIVED" value="ARCHIVED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('受众', '受眾', 'Audience')">
          <el-select v-model="taskForm.audience" style="width: 100%">
            <el-option label="ALL" value="ALL" />
            <el-option label="NEW_USER" value="NEW_USER" />
            <el-option label="RETURNING_USER" value="RETURNING_USER" />
            <el-option label="DEVELOPER" value="DEVELOPER" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('任务类型', '任務類型', 'Task Type')">
          <el-select v-model="taskForm.taskType" style="width: 100%">
            <el-option label="LOGIN" value="LOGIN" />
            <el-option label="SHARE" value="SHARE" />
            <el-option label="PLAY_GAME" value="PLAY_GAME" />
            <el-option label="INVITE" value="INVITE" />
            <el-option label="PAY" value="PAY" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('奖励类型', '獎勵類型', 'Reward Type')">
          <el-select v-model="taskForm.rewardType" style="width: 100%">
            <el-option label="COUPON" value="COUPON" />
            <el-option label="POINTS" value="POINTS" />
            <el-option label="GIFT_PACK" value="GIFT_PACK" />
            <el-option label="BADGE" value="BADGE" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('奖励值', '獎勵值', 'Reward Value')"><el-input v-model="taskForm.rewardValue" /></el-form-item>
        <el-form-item :label="lt('落地页 URL', '落地頁 URL', 'Landing URL')"><el-input v-model="taskForm.landingUrl" /></el-form-item>
        <el-form-item :label="lt('优先级', '優先級', 'Priority')"><el-input-number v-model="taskForm.priority" :min="0" :max="999" style="width: 100%" /></el-form-item>
        <el-form-item :label="lt('每日上限', '每日上限', 'Daily Limit')"><el-input-number v-model="taskForm.dailyLimit" :min="1" :max="50" style="width: 100%" /></el-form-item>
        <el-form-item :label="lt('开始时间', '開始時間', 'Start At')">
          <el-date-picker v-model="taskForm.startAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" clearable />
        </el-form-item>
        <el-form-item :label="lt('结束时间', '結束時間', 'End At')">
          <el-date-picker v-model="taskForm.endAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" clearable />
        </el-form-item>
        <el-form-item :label="lt('备注', '備註', 'Note')"><el-input v-model="taskForm.note" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskEditorVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="taskSaving" @click="saveTask">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="giftEditorVisible" :title="editingGiftId ? lt('编辑礼包码活动', '編輯禮包碼活動', 'Edit Gift Code Campaign') : lt('新建礼包码活动', '新增禮包碼活動', 'Create Gift Code Campaign')" width="760px">
      <el-form :model="giftForm" label-width="130px">
        <el-form-item :label="lt('标题', '標題', 'Title')" required><el-input v-model="giftForm.title" /></el-form-item>
        <el-form-item :label="lt('状态', '狀態', 'Status')">
          <el-select v-model="giftForm.status" style="width: 100%">
            <el-option label="DRAFT" value="DRAFT" />
            <el-option label="PUBLISHED" value="PUBLISHED" />
            <el-option label="PAUSED" value="PAUSED" />
            <el-option label="ARCHIVED" value="ARCHIVED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('受众', '受眾', 'Audience')">
          <el-select v-model="giftForm.audience" style="width: 100%">
            <el-option label="ALL" value="ALL" />
            <el-option label="NEW_USER" value="NEW_USER" />
            <el-option label="RETURNING_USER" value="RETURNING_USER" />
            <el-option label="DEVELOPER" value="DEVELOPER" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('奖励类型', '獎勵類型', 'Reward Type')">
          <el-select v-model="giftForm.rewardType" style="width: 100%">
            <el-option label="GIFT_PACK" value="GIFT_PACK" />
            <el-option label="COUPON" value="COUPON" />
            <el-option label="POINTS" value="POINTS" />
            <el-option label="CASH_VOUCHER" value="CASH_VOUCHER" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('奖励说明', '獎勵說明', 'Reward Summary')" required><el-input v-model="giftForm.rewardSummary" /></el-form-item>
        <el-form-item :label="lt('礼包码前缀', '禮包碼前綴', 'Code Prefix')" required><el-input v-model="giftForm.codePrefix" /></el-form-item>
        <el-form-item :label="lt('总库存', '總庫存', 'Total Stock')"><el-input-number v-model="giftForm.totalStock" :min="1" :max="5000" style="width: 100%" /></el-form-item>
        <el-form-item :label="lt('单用户上限', '單用戶上限', 'Per-user Limit')"><el-input-number v-model="giftForm.perUserLimit" :min="1" :max="20" style="width: 100%" /></el-form-item>
        <el-form-item :label="lt('落地页 URL', '落地頁 URL', 'Landing URL')"><el-input v-model="giftForm.landingUrl" /></el-form-item>
        <el-form-item :label="lt('开始时间', '開始時間', 'Start At')">
          <el-date-picker v-model="giftForm.startAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" clearable />
        </el-form-item>
        <el-form-item :label="lt('结束时间', '結束時間', 'End At')">
          <el-date-picker v-model="giftForm.endAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" clearable />
        </el-form-item>
        <el-form-item :label="lt('备注', '備註', 'Note')"><el-input v-model="giftForm.note" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="giftEditorVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="giftSaving" @click="saveGift">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="giftCodesVisible" :title="activeGiftCampaign ? `${lt('礼包码明细', '禮包碼明細', 'Gift Codes')} · ${activeGiftCampaign.title}` : lt('礼包码明细', '禮包碼明細', 'Gift Codes')" width="820px">
      <div class="metric-row" style="margin-bottom: 12px" v-if="activeGiftCampaign">
        <div class="metric-chip">{{ lt('可用', '可用', 'Available') }} {{ activeGiftCampaign.availableStock }}</div>
        <div class="metric-chip">{{ lt('已兑换', '已兌換', 'Redeemed') }} {{ activeGiftCampaign.redeemedStock }}</div>
        <div class="metric-chip">{{ lt('前缀', '前綴', 'Prefix') }} {{ activeGiftCampaign.codePrefix }}</div>
      </div>
      <el-table :data="giftCodeEntries" :empty-text="lt('暂无礼包码明细', '暫無禮包碼明細', 'No gift codes')">
        <el-table-column prop="code" :label="lt('礼包码', '禮包碼', 'Gift Code')" min-width="220" />
        <el-table-column prop="status" :label="lt('状态', '狀態', 'Status')" width="140">
          <template #default="{ row }"><el-tag :type="row.status === 'REDEEMED' ? 'success' : row.status === 'VOID' ? 'info' : ''">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="redeemedByUserId" :label="lt('兑换人', '兌換人', 'Redeemed By')" width="120" />
        <el-table-column :label="lt('兑换时间', '兌換時間', 'Redeemed At')" width="180">
          <template #default="{ row }">{{ formatDate(row.redeemedAt) }}</template>
        </el-table-column>
        <el-table-column :label="lt('生成时间', '生成時間', 'Created At')" width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="giftCodesVisible = false">{{ lt('关闭', '關閉', 'Close') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editorVisible" :title="editingId ? lt('编辑启动广告', '編輯啟動廣告', 'Edit Launch Ad') : lt('新建启动广告', '新建啟動廣告', 'Create Launch Ad')" width="760px">
      <el-form :model="form" label-width="130px">
        <el-form-item :label="lt('图片 URL', '圖片 URL', 'Image URL')" required><el-input v-model="form.imageUrl" /></el-form-item>
        <el-form-item :label="lt('跳转 URL', '跳轉 URL', 'Target URL')" required><el-input v-model="form.targetUrl" /></el-form-item>
        <el-form-item :label="lt('展示秒数', '展示秒數', 'Display Seconds')"><el-input-number v-model="form.displaySeconds" :min="1" :max="30" style="width: 100%" /></el-form-item>
        <el-form-item :label="lt('状态', '狀態', 'Status')">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="DRAFT" value="DRAFT" />
            <el-option label="ACTIVE" value="ACTIVE" />
            <el-option label="INACTIVE" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('开始时间', '開始時間', 'Start At')">
          <el-date-picker v-model="form.startAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" clearable />
        </el-form-item>
        <el-form-item :label="lt('结束时间', '結束時間', 'End At')">
          <el-date-picker v-model="form.endAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" clearable />
        </el-form-item>
        <el-divider>{{ lt('多语言标题', '多語言標題', 'Localized Titles') }}</el-divider>
        <el-form-item label="zh-CN" required><el-input v-model="form.titleZhCn" /></el-form-item>
        <el-form-item label="zh-TW" required><el-input v-model="form.titleZhTw" /></el-form-item>
        <el-form-item label="en" required><el-input v-model="form.titleEn" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="saveAd">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  activateLaunchAd,
  batchUpdateOpsMarketingStatus,
  createLaunchAd,
  createOpsCampaign,
  createOpsGiftCodeCampaign,
  createOpsPopupCampaign,
  createOpsTaskCampaign,
  deactivateLaunchAd,
  getOpsMarketingAnalytics,
  getLaunchAds,
  getOpsCampaigns,
  getOpsGiftCodeCampaigns,
  getOpsGiftCodeEntries,
  getOpsPopupCampaigns,
  getOpsTaskCampaigns,
  updateLaunchAd,
  updateOpsCampaign,
  updateOpsCampaignStatus,
  updateOpsGiftCodeCampaign,
  updateOpsGiftCodeCampaignStatus,
  updateOpsPopupCampaign,
  updateOpsPopupCampaignStatus,
  updateOpsTaskCampaign,
  updateOpsTaskCampaignStatus
} from '../../api'
import { useI18nLite } from '../../i18n'

const { lt } = useI18nLite()
const loading = ref(false)
const saving = ref(false)
const campaignSaving = ref(false)
const popupSaving = ref(false)
const taskSaving = ref(false)
const giftSaving = ref(false)
const editorVisible = ref(false)
const campaignEditorVisible = ref(false)
const popupEditorVisible = ref(false)
const taskEditorVisible = ref(false)
const giftEditorVisible = ref(false)
const giftCodesVisible = ref(false)
const editingId = ref(null)
const editingCampaignId = ref(null)
const editingPopupId = ref(null)
const editingTaskId = ref(null)
const editingGiftId = ref(null)
const ads = reactive({ active: null, items: [] })
const campaigns = ref([])
const popupCampaigns = ref([])
const taskCampaigns = ref([])
const giftCampaigns = ref([])
const selectedCampaignIds = ref([])
const selectedPopupIds = ref([])
const selectedTaskIds = ref([])
const selectedGiftIds = ref([])
const selectedAdIds = ref([])
const marketingAnalytics = ref({
  metrics: []
})
const giftCodeEntries = ref([])
const activeGiftCampaign = ref(null)
const form = reactive({
  imageUrl: '',
  targetUrl: '',
  imageVersion: '',
  displaySeconds: 4,
  sponsorZhCn: '',
  sponsorZhTw: '',
  sponsorEn: '',
  titleZhCn: '',
  titleZhTw: '',
  titleEn: '',
  descriptionZhCn: '',
  descriptionZhTw: '',
  descriptionEn: '',
  ctaZhCn: '',
  ctaZhTw: '',
  ctaEn: '',
  footerZhCn: '',
  footerZhTw: '',
  footerEn: '',
  startAt: '',
  endAt: '',
  status: 'DRAFT'
})
const campaignForm = reactive({
  campaignType: 'TOPIC',
  title: '',
  status: 'DRAFT',
  audience: 'ALL',
  landingUrl: '',
  bannerUrl: '',
  priority: 0,
  note: '',
  startAt: '',
  endAt: ''
})
const popupForm = reactive({
  title: '',
  status: 'DRAFT',
  audience: 'ALL',
  triggerScene: 'APP_LAUNCH',
  landingUrl: '',
  imageUrl: '',
  buttonText: '',
  priority: 0,
  frequencyLimitPerDay: 1,
  note: '',
  startAt: '',
  endAt: ''
})
const taskForm = reactive({
  title: '',
  status: 'DRAFT',
  audience: 'ALL',
  taskType: 'LOGIN',
  rewardType: 'COUPON',
  rewardValue: '',
  landingUrl: '',
  priority: 0,
  dailyLimit: 1,
  note: '',
  startAt: '',
  endAt: ''
})
const giftForm = reactive({
  title: '',
  status: 'DRAFT',
  audience: 'ALL',
  rewardType: 'GIFT_PACK',
  rewardSummary: '',
  landingUrl: '',
  codePrefix: 'GIFT',
  totalStock: 100,
  perUserLimit: 1,
  note: '',
  startAt: '',
  endAt: ''
})

const liveCount = computed(() => ads.items.filter(item => item.effectiveState === 'LIVE').length)
const scheduledCount = computed(() => ads.items.filter(item => item.effectiveState === 'SCHEDULED').length)
const liveCampaignCount = computed(() => campaigns.value.filter(item => item.effectiveStatus === 'LIVE').length)
const scheduledCampaignCount = computed(() => campaigns.value.filter(item => item.effectiveStatus === 'SCHEDULED').length)
const livePopupCount = computed(() => popupCampaigns.value.filter(item => item.effectiveStatus === 'LIVE').length)
const scheduledPopupCount = computed(() => popupCampaigns.value.filter(item => item.effectiveStatus === 'SCHEDULED').length)
const liveTaskCount = computed(() => taskCampaigns.value.filter(item => item.effectiveStatus === 'LIVE').length)
const scheduledTaskCount = computed(() => taskCampaigns.value.filter(item => item.effectiveStatus === 'SCHEDULED').length)
const liveGiftCount = computed(() => giftCampaigns.value.filter(item => item.effectiveStatus === 'LIVE').length)
const scheduledGiftCount = computed(() => giftCampaigns.value.filter(item => item.effectiveStatus === 'SCHEDULED').length)

const loadData = async () => {
  loading.value = true
  try {
    const [adsRes, campaignsRes, popupRes, taskRes, giftRes, analyticsRes] = await Promise.all([getLaunchAds(), getOpsCampaigns(), getOpsPopupCampaigns(), getOpsTaskCampaigns(), getOpsGiftCodeCampaigns(), getOpsMarketingAnalytics()])
    ads.active = adsRes.data?.active || null
    ads.items = adsRes.data?.items || []
    campaigns.value = campaignsRes.data || []
    popupCampaigns.value = popupRes.data || []
    taskCampaigns.value = taskRes.data || []
    giftCampaigns.value = giftRes.data || []
    marketingAnalytics.value = analyticsRes.data || marketingAnalytics.value
  } catch (error) {
    ElMessage.error(error.message || lt('加载营销中心失败', '載入營銷中心失敗', 'Failed to load marketing center'))
  } finally {
    loading.value = false
  }
}

const onSelectionChange = (assetType, rows) => {
  const ids = rows.map((row) => row.id)
  if (assetType === 'CAMPAIGN') selectedCampaignIds.value = ids
  if (assetType === 'POPUP') selectedPopupIds.value = ids
  if (assetType === 'TASK') selectedTaskIds.value = ids
  if (assetType === 'GIFT') selectedGiftIds.value = ids
  if (assetType === 'LAUNCH_AD') selectedAdIds.value = ids
}

const selectedIdsByAssetType = (assetType) => {
  if (assetType === 'CAMPAIGN') return selectedCampaignIds.value
  if (assetType === 'POPUP') return selectedPopupIds.value
  if (assetType === 'TASK') return selectedTaskIds.value
  if (assetType === 'GIFT') return selectedGiftIds.value
  if (assetType === 'LAUNCH_AD') return selectedAdIds.value
  return []
}

const clearSelectionByAssetType = (assetType) => {
  if (assetType === 'CAMPAIGN') selectedCampaignIds.value = []
  if (assetType === 'POPUP') selectedPopupIds.value = []
  if (assetType === 'TASK') selectedTaskIds.value = []
  if (assetType === 'GIFT') selectedGiftIds.value = []
  if (assetType === 'LAUNCH_AD') selectedAdIds.value = []
}

const runMarketingBatch = async (assetType, status) => {
  const ids = selectedIdsByAssetType(assetType)
  if (!ids.length) return
  try {
    let reason = ''
    if (['PAUSED', 'ARCHIVED', 'INACTIVE'].includes(status)) {
      const result = await ElMessageBox.prompt(
        lt('请填写本次批量操作原因。', '請填寫本次批量操作原因。', 'Please provide a reason for this batch action.'),
        lt('二次确认', '二次確認', 'Secondary Confirmation'),
        {
          confirmButtonText: lt('确认执行', '確認執行', 'Confirm'),
          cancelButtonText: lt('取消', '取消', 'Cancel'),
          inputPattern: /^.{4,200}$/,
          inputErrorMessage: lt('请填写 4-200 字原因', '請填寫 4-200 字原因', 'Enter a reason between 4 and 200 characters'),
          type: 'warning'
        }
      )
      reason = result.value
    } else {
      await ElMessageBox.confirm(
        lt('该批量操作会直接影响前端可见状态，请再次确认。', '此批量操作會直接影響前端可見狀態，請再次確認。', 'This batch action will directly affect frontend visibility. Please confirm again.'),
        lt('二次确认', '二次確認', 'Secondary Confirmation'),
        { type: 'warning' }
      )
    }
    await batchUpdateOpsMarketingStatus({ assetType, ids, status, reason })
    clearSelectionByAssetType(assetType)
    ElMessage.success(lt('批量操作已完成', '批量操作已完成', 'Batch action completed'))
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('批量操作失败', '批量操作失敗', 'Batch action failed'))
    }
  }
}

const resetForm = () => {
  editingId.value = null
  Object.assign(form, {
    imageUrl: '',
    targetUrl: '',
    imageVersion: '',
    displaySeconds: 4,
    sponsorZhCn: '',
    sponsorZhTw: '',
    sponsorEn: '',
    titleZhCn: '',
    titleZhTw: '',
    titleEn: '',
    descriptionZhCn: '',
    descriptionZhTw: '',
    descriptionEn: '',
    ctaZhCn: '',
    ctaZhTw: '',
    ctaEn: '',
    footerZhCn: '',
    footerZhTw: '',
    footerEn: '',
    startAt: '',
    endAt: '',
    status: 'DRAFT'
  })
}

const resetCampaignForm = () => {
  editingCampaignId.value = null
  Object.assign(campaignForm, {
    campaignType: 'TOPIC',
    title: '',
    status: 'DRAFT',
    audience: 'ALL',
    landingUrl: '',
    bannerUrl: '',
    priority: 0,
    note: '',
    startAt: '',
    endAt: ''
  })
}

const resetPopupForm = () => {
  editingPopupId.value = null
  Object.assign(popupForm, {
    title: '',
    status: 'DRAFT',
    audience: 'ALL',
    triggerScene: 'APP_LAUNCH',
    landingUrl: '',
    imageUrl: '',
    buttonText: '',
    priority: 0,
    frequencyLimitPerDay: 1,
    note: '',
    startAt: '',
    endAt: ''
  })
}

const resetTaskForm = () => {
  editingTaskId.value = null
  Object.assign(taskForm, {
    title: '',
    status: 'DRAFT',
    audience: 'ALL',
    taskType: 'LOGIN',
    rewardType: 'COUPON',
    rewardValue: '',
    landingUrl: '',
    priority: 0,
    dailyLimit: 1,
    note: '',
    startAt: '',
    endAt: ''
  })
}

const resetGiftForm = () => {
  editingGiftId.value = null
  Object.assign(giftForm, {
    title: '',
    status: 'DRAFT',
    audience: 'ALL',
    rewardType: 'GIFT_PACK',
    rewardSummary: '',
    landingUrl: '',
    codePrefix: 'GIFT',
    totalStock: 100,
    perUserLimit: 1,
    note: '',
    startAt: '',
    endAt: ''
  })
}

const openCreate = () => {
  resetForm()
  editorVisible.value = true
}

const openEdit = (row) => {
  editingId.value = row.id
  Object.assign(form, row)
  editorVisible.value = true
}

const openCampaignCreate = () => {
  resetCampaignForm()
  campaignEditorVisible.value = true
}

const openCampaignEdit = (row) => {
  editingCampaignId.value = row.id
  Object.assign(campaignForm, row)
  campaignEditorVisible.value = true
}

const openPopupCreate = () => {
  resetPopupForm()
  popupEditorVisible.value = true
}

const openPopupEdit = (row) => {
  editingPopupId.value = row.id
  Object.assign(popupForm, row)
  popupEditorVisible.value = true
}

const openTaskCreate = () => {
  resetTaskForm()
  taskEditorVisible.value = true
}

const openTaskEdit = (row) => {
  editingTaskId.value = row.id
  Object.assign(taskForm, row)
  taskEditorVisible.value = true
}

const openGiftCreate = () => {
  resetGiftForm()
  giftEditorVisible.value = true
}

const openGiftEdit = (row) => {
  editingGiftId.value = row.id
  Object.assign(giftForm, row)
  giftEditorVisible.value = true
}

const viewGiftCodes = async (row) => {
  try {
    activeGiftCampaign.value = row
    giftCodesVisible.value = true
    const res = await getOpsGiftCodeEntries(row.id)
    giftCodeEntries.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || lt('加载礼包码失败', '載入禮包碼失敗', 'Failed to load gift codes'))
  }
}

const saveCampaign = async () => {
  if (!campaignForm.title.trim()) {
    ElMessage.warning(lt('请填写活动标题', '請填寫活動標題', 'Please enter the campaign title'))
    return
  }
  if (campaignForm.startAt && campaignForm.endAt && campaignForm.endAt < campaignForm.startAt) {
    ElMessage.warning(lt('结束时间不能早于开始时间', '結束時間不能早於開始時間', 'End time cannot be earlier than start time'))
    return
  }
  campaignSaving.value = true
  try {
    if (editingCampaignId.value) {
      await updateOpsCampaign(editingCampaignId.value, { ...campaignForm })
    } else {
      await createOpsCampaign({ ...campaignForm })
    }
    campaignEditorVisible.value = false
    ElMessage.success(lt('活动已保存', '活動已儲存', 'Campaign saved'))
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || lt('保存活动失败', '儲存活動失敗', 'Failed to save campaign'))
  } finally {
    campaignSaving.value = false
  }
}

const savePopup = async () => {
  try {
    popupSaving.value = true
    if (editingPopupId.value) {
      await updateOpsPopupCampaign(editingPopupId.value, { ...popupForm })
    } else {
      await createOpsPopupCampaign({ ...popupForm })
    }
    popupEditorVisible.value = false
    ElMessage.success(lt('弹窗活动已保存', '彈窗活動已儲存', 'Popup campaign saved'))
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || lt('保存弹窗活动失败', '儲存彈窗活動失敗', 'Failed to save popup campaign'))
  } finally {
    popupSaving.value = false
  }
}

const saveTask = async () => {
  if (!taskForm.title.trim() || !taskForm.rewardValue.trim()) {
    ElMessage.warning(lt('请填写任务标题和奖励值', '請填寫任務標題與獎勵值', 'Please enter task title and reward value'))
    return
  }
  if (taskForm.startAt && taskForm.endAt && taskForm.endAt < taskForm.startAt) {
    ElMessage.warning(lt('结束时间不能早于开始时间', '結束時間不能早於開始時間', 'End time cannot be earlier than start time'))
    return
  }
  try {
    taskSaving.value = true
    if (editingTaskId.value) {
      await updateOpsTaskCampaign(editingTaskId.value, { ...taskForm })
    } else {
      await createOpsTaskCampaign({ ...taskForm })
    }
    taskEditorVisible.value = false
    ElMessage.success(lt('任务活动已保存', '任務活動已儲存', 'Task campaign saved'))
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || lt('保存任务活动失败', '儲存任務活動失敗', 'Failed to save task campaign'))
  } finally {
    taskSaving.value = false
  }
}

const saveGift = async () => {
  if (!giftForm.title.trim() || !giftForm.rewardSummary.trim() || !giftForm.codePrefix.trim()) {
    ElMessage.warning(lt('请填写礼包码标题、奖励说明和前缀', '請填寫禮包碼標題、獎勵說明與前綴', 'Please enter gift code title, reward summary, and code prefix'))
    return
  }
  if (giftForm.startAt && giftForm.endAt && giftForm.endAt < giftForm.startAt) {
    ElMessage.warning(lt('结束时间不能早于开始时间', '結束時間不能早於開始時間', 'End time cannot be earlier than start time'))
    return
  }
  try {
    giftSaving.value = true
    if (editingGiftId.value) {
      await updateOpsGiftCodeCampaign(editingGiftId.value, { ...giftForm })
    } else {
      await createOpsGiftCodeCampaign({ ...giftForm })
    }
    giftEditorVisible.value = false
    ElMessage.success(lt('礼包码活动已保存', '禮包碼活動已儲存', 'Gift code campaign saved'))
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || lt('保存礼包码活动失败', '儲存禮包碼活動失敗', 'Failed to save gift code campaign'))
  } finally {
    giftSaving.value = false
  }
}

const changeCampaignStatus = async (row, status) => {
  try {
    await ElMessageBox.confirm(
      lt('该操作会直接改变活动在前端的可见状态，请再次确认。', '此操作會直接改變活動在前端的可見狀態，請再次確認。', 'This action will directly change campaign visibility on the frontend. Please confirm again.'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { type: 'warning' }
    )
    await updateOpsCampaignStatus(row.id, { status })
    ElMessage.success(lt('活动状态已更新', '活動狀態已更新', 'Campaign status updated'))
    await loadData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || lt('更新活动状态失败', '更新活動狀態失敗', 'Failed to update campaign status'))
  }
}

const changePopupStatus = async (row, status) => {
  try {
    await ElMessageBox.confirm(
      lt('该操作会立即影响弹窗投放状态，请再次确认。', '此操作會立即影響彈窗投放狀態，請再次確認。', 'This will immediately affect popup delivery state. Please confirm again.'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { type: 'warning' }
    )
    await updateOpsPopupCampaignStatus(row.id, { status })
    ElMessage.success(lt('弹窗状态已更新', '彈窗狀態已更新', 'Popup status updated'))
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('更新弹窗状态失败', '更新彈窗狀態失敗', 'Failed to update popup status'))
    }
  }
}

const changeTaskStatus = async (row, status) => {
  try {
    await ElMessageBox.confirm(
      lt('该操作会立即影响任务活动在前端的生效状态，请再次确认。', '此操作會立即影響任務活動在前端的生效狀態，請再次確認。', 'This will immediately affect task campaign state on the frontend. Please confirm again.'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { type: 'warning' }
    )
    await updateOpsTaskCampaignStatus(row.id, { status })
    ElMessage.success(lt('任务活动状态已更新', '任務活動狀態已更新', 'Task campaign status updated'))
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('更新任务活动状态失败', '更新任務活動狀態失敗', 'Failed to update task campaign status'))
    }
  }
}

const changeGiftStatus = async (row, status) => {
  try {
    await ElMessageBox.confirm(
      lt('该操作会立即影响礼包码在前端的可领取状态，请再次确认。', '此操作會立即影響禮包碼在前端的可領取狀態，請再次確認。', 'This will immediately affect whether gift codes can be claimed on the frontend. Please confirm again.'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { type: 'warning' }
    )
    await updateOpsGiftCodeCampaignStatus(row.id, { status })
    ElMessage.success(lt('礼包码活动状态已更新', '禮包碼活動狀態已更新', 'Gift code campaign status updated'))
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('更新礼包码活动状态失败', '更新禮包碼活動狀態失敗', 'Failed to update gift code campaign status'))
    }
  }
}

const saveAd = async () => {
  if (!form.imageUrl.trim() || !form.targetUrl.trim() || !form.titleZhCn.trim() || !form.titleZhTw.trim() || !form.titleEn.trim()) {
    ElMessage.warning(lt('请填写必填字段', '請填寫必填欄位', 'Please fill required fields'))
    return
  }
  if (form.startAt && form.endAt && form.endAt < form.startAt) {
    ElMessage.warning(lt('结束时间不能早于开始时间', '結束時間不能早於開始時間', 'End time cannot be earlier than start time'))
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateLaunchAd(editingId.value, { ...form })
    } else {
      await createLaunchAd({ ...form })
    }
    editorVisible.value = false
    ElMessage.success(lt('启动广告已保存', '啟動廣告已儲存', 'Launch ad saved'))
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || lt('保存启动广告失败', '儲存啟動廣告失敗', 'Failed to save launch ad'))
  } finally {
    saving.value = false
  }
}

const activate = async (row) => {
  try {
    await ElMessageBox.confirm(
      lt('确认激活该启动广告？激活后会替换当前线上启动广告。', '確認激活該啟動廣告？激活後會替換當前線上啟動廣告。', 'Activate this launch ad? It will replace the current live startup ad.'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { confirmButtonText: lt('确认执行', '確認執行', 'Confirm'), cancelButtonText: lt('取消', '取消', 'Cancel'), type: 'warning' }
    )
    await activateLaunchAd(row.id)
    ElMessage.success(lt('已激活', '已激活', 'Activated'))
    await loadData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || lt('激活失败', '激活失敗', 'Activation failed'))
  }
}

const deactivate = async (row) => {
  try {
    await ElMessageBox.confirm(
      lt('确认停用该启动广告？停用后前端将不再展示它。', '確認停用該啟動廣告？停用後前端將不再展示它。', 'Deactivate this launch ad? It will no longer be shown on the frontend.'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { confirmButtonText: lt('确认执行', '確認執行', 'Confirm'), cancelButtonText: lt('取消', '取消', 'Cancel'), type: 'warning' }
    )
    await deactivateLaunchAd(row.id)
    ElMessage.success(lt('已停用', '已停用', 'Deactivated'))
    await loadData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || lt('停用失败', '停用失敗', 'Deactivation failed'))
  }
}

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(date)
}

function formatRange(startAt, endAt) {
  if (!startAt && !endAt) return lt('长期有效', '長期有效', 'Always on')
  return `${formatDate(startAt)} ~ ${formatDate(endAt)}`
}

function effectiveStateType(state) {
  if (state === 'LIVE') return 'success'
  if (state === 'SCHEDULED') return 'warning'
  if (state === 'EXPIRED') return 'danger'
  if (state === 'INACTIVE' || state === 'PAUSED' || state === 'ARCHIVED') return 'info'
  return ''
}

onMounted(loadData)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 18px; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.panel-title { font-size: 17px; font-weight: 800; color: #101828; }
.panel-subtitle { margin-top: 6px; color: #667085; font-size: 13px; }
.actions { display: flex; gap: 8px; flex-wrap: wrap; }
.metric-row { display: flex; gap: 10px; flex-wrap: wrap; }
.metric-chip { padding: 8px 12px; border-radius: 999px; background: #f5f7fa; color: #344054; font-size: 13px; }
.card-header-inline { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
</style>
