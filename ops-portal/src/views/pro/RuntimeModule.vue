<template>
  <div class="pro-page">
    <el-alert v-if="loadError" type="error" :closable="false" :title="loadError" />

    <div class="overview-grid">
      <div class="overview-chip">
        <span>{{ lt('渠道规则', '渠道規則', 'Channel Rules') }}</span>
        <strong>{{ channelRules.length }}</strong>
      </div>
      <div class="overview-chip">
        <span>{{ lt('功能开关', '功能開關', 'Feature Toggles') }}</span>
        <strong>{{ featureToggles.length }}</strong>
      </div>
      <div class="overview-chip">
        <span>{{ lt('实验与灰度', '實驗與灰度', 'Experiments & Gray') }}</span>
        <strong>{{ abExperiments.length + grayReleasePlans.length }}</strong>
      </div>
      <div class="overview-chip">
        <span>{{ lt('熔断与黑名单', '熔斷與黑名單', 'Breakers & Blacklist') }}</span>
        <strong>{{ compatibilityBlacklists.length + pageCircuitBreakers.length }}</strong>
      </div>
      <div class="overview-chip">
        <span>{{ lt('Bridge 能力', 'Bridge 能力', 'Bridge APIs') }}</span>
        <strong>{{ bridgeApis.length }}</strong>
      </div>
      <div class="overview-chip">
        <span>{{ lt('接入游戏资产', '接入遊戲資產', 'Integrated Games') }}</span>
        <strong>{{ androidGames.length }}</strong>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :xl="10">
        <el-card class="panel-card">
          <template #header>{{ lt('Android 运行时参数', 'Android 運行時參數', 'Android Runtime Config') }}</template>
          <el-form :model="runtimeForm" label-position="top">
            <el-form-item label="API Base URL"><el-input v-model="runtimeForm.apiBaseUrl" /></el-form-item>
            <el-form-item label="Asset Host"><el-input v-model="runtimeForm.assetHost" /></el-form-item>
            <el-form-item :label="lt('默认语言', '預設語言', 'Default Language')"><el-input v-model="runtimeForm.defaultLanguage" /></el-form-item>
            <el-form-item :label="lt('最低支持版本', '最低支援版本', 'Minimum Supported Version')"><el-input v-model="runtimeForm.minimumSupportedVersion" /></el-form-item>
            <el-form-item :label="lt('灰度说明', '灰度說明', 'Gray Release Notes')"><el-input v-model="runtimeForm.grayReleaseDescription" type="textarea" :rows="3" maxlength="256" show-word-limit /></el-form-item>
            <el-form-item :label="lt('最大 ZIP(MB)', '最大 ZIP(MB)', 'Max ZIP (MB)')"><el-input-number v-model="runtimeForm.maxZipSizeMb" :min="1" :max="1024" style="width: 100%" /></el-form-item>
            <el-form-item :label="lt('启动路由策略', '啟動路由策略', 'Startup Route Policy')"><el-input v-model="runtimeForm.startupRoutePolicy" /></el-form-item>
            <el-form-item><el-switch v-model="runtimeForm.forceUpdateEnabled" /> {{ lt('启用强更策略', '啟用強更策略', 'Enable Force Update') }}</el-form-item>
            <el-form-item><el-switch v-model="runtimeForm.debugUseMockData" /> {{ lt('使用 Mock 数据', '使用 Mock 資料', 'Use Mock Data') }}</el-form-item>
            <el-form-item><el-switch v-model="runtimeForm.enableSyncBridge" /> {{ lt('启用同步 Bridge', '啟用同步 Bridge', 'Enable Sync Bridge') }}</el-form-item>
            <el-form-item><el-switch v-model="runtimeForm.allowCleartextTraffic" /> {{ lt('允许明文流量', '允許明文流量', 'Allow Cleartext Traffic') }}</el-form-item>
            <el-form-item><el-button type="primary" :loading="savingRuntime" @click="saveRuntime">{{ lt('保存运行时配置', '儲存運行時配置', 'Save Runtime Config') }}</el-button></el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="14">
        <el-card class="panel-card">
          <template #header>
            <div class="card-header-inline">
              <span>{{ lt('渠道灰度与强更规则', '渠道灰度與強更規則', 'Channel Rules') }}</span>
              <el-button type="primary" link @click="openChannelDialog()">{{ lt('新增渠道规则', '新增渠道規則', 'New Channel Rule') }}</el-button>
            </div>
          </template>
          <el-table :data="channelRules" :empty-text="lt('暂无渠道规则', '暫無渠道規則', 'No channel rules')" @row-click="showDetail('channel', $event)">
            <el-table-column prop="channelCode" :label="lt('渠道编码', '渠道編碼', 'Channel Code')" width="140" />
            <el-table-column prop="channelName" :label="lt('渠道名称', '渠道名稱', 'Channel Name')" min-width="160" />
            <el-table-column :label="lt('状态', '狀態', 'Status')" width="100">
              <template #default="{ row }"><el-tag :type="channelStatusType(row.status)">{{ row.status }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="minimumVersion" :label="lt('最低版本', '最低版本', 'Min Version')" width="120" />
            <el-table-column :label="lt('灰度', '灰度', 'Gray')" width="100">
              <template #default="{ row }">{{ row.grayReleaseEnabled ? `${row.trafficPercentage}%` : '-' }}</template>
            </el-table-column>
            <el-table-column :label="lt('强更', '強更', 'Force Update')" width="90">
              <template #default="{ row }"><el-tag :type="row.forceUpdateEnabled ? 'danger' : 'info'">{{ row.forceUpdateEnabled ? 'ON' : 'OFF' }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="note" :label="lt('备注', '備註', 'Note')" min-width="180" show-overflow-tooltip />
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="180">
              <template #default="{ row }">
                <el-button link @click.stop="showDetail('channel', row)">{{ lt('详情', '詳情', 'Detail') }}</el-button>
                <el-button type="primary" link @click.stop="openChannelDialog(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :xl="14">
        <el-card class="panel-card">
          <template #header>
            <div class="card-header-inline">
              <span>{{ lt('功能熔断与开关', '功能熔斷與開關', 'Feature Toggles') }}</span>
              <el-button type="primary" link @click="openFeatureDialog()">{{ lt('新增开关', '新增開關', 'New Toggle') }}</el-button>
            </div>
          </template>
          <el-table :data="featureToggles" :empty-text="lt('暂无功能开关', '暫無功能開關', 'No feature toggles')" @row-click="showDetail('feature', $event)">
            <el-table-column prop="featureKey" :label="lt('开关键', '開關鍵', 'Feature Key')" width="180" />
            <el-table-column prop="featureName" :label="lt('名称', '名稱', 'Name')" min-width="180" />
            <el-table-column :label="lt('状态', '狀態', 'Status')" width="100">
              <template #default="{ row }"><el-tag :type="featureStatusType(row.status)">{{ row.status }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="scope" :label="lt('作用域', '作用域', 'Scope')" width="130" />
            <el-table-column prop="owner" :label="lt('负责人', '負責人', 'Owner')" width="120" />
            <el-table-column prop="note" :label="lt('备注', '備註', 'Note')" min-width="180" show-overflow-tooltip />
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="180">
              <template #default="{ row }">
                <el-button link @click.stop="showDetail('feature', row)">{{ lt('详情', '詳情', 'Detail') }}</el-button>
                <el-button type="primary" link @click.stop="openFeatureDialog(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="10">
        <el-card class="panel-card">
          <template #header>
            <div class="card-header-inline">
              <span>{{ lt('AB 实验配置', 'AB 實驗配置', 'AB Experiments') }}</span>
              <el-button type="primary" link @click="openExperimentDialog()">{{ lt('新增实验', '新增實驗', 'New Experiment') }}</el-button>
            </div>
          </template>
          <el-table :data="abExperiments" :empty-text="lt('暂无 AB 实验', '暫無 AB 實驗', 'No AB experiments')" @row-click="showDetail('experiment', $event)">
            <el-table-column prop="experimentKey" :label="lt('实验键', '實驗鍵', 'Experiment Key')" width="170" />
            <el-table-column prop="experimentName" :label="lt('实验名称', '實驗名稱', 'Experiment Name')" min-width="160" />
            <el-table-column :label="lt('状态', '狀態', 'Status')" width="110">
              <template #default="{ row }"><el-tag :type="experimentStatusType(row.status)">{{ row.status }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="layerKey" :label="lt('实验层', '實驗層', 'Layer')" width="130" />
            <el-table-column :label="lt('流量', '流量', 'Traffic')" width="90">
              <template #default="{ row }">{{ row.trafficPercentage }}%</template>
            </el-table-column>
            <el-table-column :label="lt('分桶', '分桶', 'Variants')" min-width="170">
              <template #default="{ row }">{{ row.variantAName }} {{ row.variantAPercentage }}% / {{ row.variantBName }} {{ row.variantBPercentage }}%</template>
            </el-table-column>
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="180">
              <template #default="{ row }">
                <el-button link @click.stop="showDetail('experiment', row)">{{ lt('详情', '詳情', 'Detail') }}</el-button>
                <el-button type="primary" link @click.stop="openExperimentDialog(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :xl="14">
        <el-card class="panel-card">
          <template #header>
            <div class="card-header-inline">
              <span>{{ lt('精细灰度发布计划', '精細灰度發布計畫', 'Gray Release Plans') }}</span>
              <el-button type="primary" link @click="openGrayPlanDialog()">{{ lt('新增灰度计划', '新增灰度計畫', 'New Gray Plan') }}</el-button>
            </div>
          </template>
          <el-table :data="grayReleasePlans" :empty-text="lt('暂无灰度计划', '暫無灰度計畫', 'No gray release plans')" @row-click="showDetail('gray', $event)">
            <el-table-column prop="planCode" :label="lt('计划编码', '計畫編碼', 'Plan Code')" width="180" />
            <el-table-column prop="planName" :label="lt('计划名称', '計畫名稱', 'Plan Name')" min-width="160" />
            <el-table-column :label="lt('状态', '狀態', 'Status')" width="100">
              <template #default="{ row }"><el-tag :type="grayPlanStatusType(row.status)">{{ row.status }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="channelCode" :label="lt('渠道', '渠道', 'Channel')" width="120" />
            <el-table-column :label="lt('总体流量', '總體流量', 'Overall')" width="100">
              <template #default="{ row }">{{ row.overallTrafficPercentage }}%</template>
            </el-table-column>
            <el-table-column :label="lt('新老用户比', '新老用戶比', 'New / Returning')" width="140">
              <template #default="{ row }">{{ row.newUserPercentage }} / {{ row.returningUserPercentage }}</template>
            </el-table-column>
            <el-table-column :label="lt('白名单', '白名單', 'Whitelist')" width="100">
              <template #default="{ row }">{{ row.whitelistPercentage }}%</template>
            </el-table-column>
            <el-table-column prop="fallbackPolicy" :label="lt('回退策略', '回退策略', 'Fallback')" width="120" />
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="180">
              <template #default="{ row }">
                <el-button link @click.stop="showDetail('gray', row)">{{ lt('详情', '詳情', 'Detail') }}</el-button>
                <el-button type="primary" link @click.stop="openGrayPlanDialog(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="10">
        <el-card class="panel-card">
          <template #header>{{ lt('发现页推荐池', '發現頁推薦池', 'Discover Recommendation Pools') }}</template>
          <el-form :model="discoverForm" label-position="top">
            <el-form-item :label="lt('Hero 游戏 AppID', 'Hero 遊戲 AppID', 'Hero AppID')">
              <el-input v-model="discoverForm.heroAppId" />
            </el-form-item>
            <el-form-item :label="lt('大家都在玩', '大家都在玩', 'Everyone is Playing')">
              <el-select v-model="discoverForm.everyoneAppIds" multiple filterable style="width: 100%">
                <el-option v-for="item in availableGames" :key="item.appId" :label="`${item.name} (${item.appId})`" :value="item.appId" />
              </el-select>
            </el-form-item>
            <el-form-item :label="lt('新手推荐', '新手推薦', 'Newbie Picks')">
              <el-select v-model="discoverForm.newbieAppIds" multiple filterable style="width: 100%">
                <el-option v-for="item in availableGames" :key="item.appId" :label="`${item.name} (${item.appId})`" :value="item.appId" />
              </el-select>
            </el-form-item>
            <el-form-item :label="lt('排行推荐', '排行推薦', 'Ranked Picks')">
              <el-select v-model="discoverForm.rankedAppIds" multiple filterable style="width: 100%">
                <el-option v-for="item in availableGames" :key="item.appId" :label="`${item.name} (${item.appId})`" :value="item.appId" />
              </el-select>
            </el-form-item>
            <el-form-item><el-button type="success" :loading="savingDiscover" @click="saveDiscover">{{ lt('保存推荐池', '儲存推薦池', 'Save Pools') }}</el-button></el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>
            <div class="card-header-inline">
              <span>{{ lt('兼容黑名单', '相容黑名單', 'Compatibility Blacklist') }}</span>
              <el-button type="primary" link @click="openBlacklistDialog()">{{ lt('新增规则', '新增規則', 'New Rule') }}</el-button>
            </div>
          </template>
          <el-table :data="compatibilityBlacklists" :empty-text="lt('暂无兼容黑名单', '暫無相容黑名單', 'No compatibility blacklist')" @row-click="showDetail('blacklist', $event)">
            <el-table-column prop="ruleCode" :label="lt('规则编码', '規則編碼', 'Rule Code')" width="180" />
            <el-table-column :label="lt('状态', '狀態', 'Status')" width="100">
              <template #default="{ row }"><el-tag :type="row.status === 'ACTIVE' ? 'danger' : 'info'">{{ row.status }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="targetType" :label="lt('目标类型', '目標類型', 'Target Type')" width="120" />
            <el-table-column prop="targetValue" :label="lt('目标值', '目標值', 'Target Value')" min-width="160" />
            <el-table-column prop="reason" :label="lt('封禁原因', '封禁原因', 'Reason')" min-width="180" show-overflow-tooltip />
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="180">
              <template #default="{ row }">
                <el-button link @click.stop="showDetail('blacklist', row)">{{ lt('详情', '詳情', 'Detail') }}</el-button>
                <el-button type="primary" link @click.stop="openBlacklistDialog(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>
            <div class="card-header-inline">
              <span>{{ lt('页面级熔断', '頁面級熔斷', 'Page Circuit Breakers') }}</span>
              <el-button type="primary" link @click="openBreakerDialog()">{{ lt('新增熔断', '新增熔斷', 'New Breaker') }}</el-button>
            </div>
          </template>
          <el-table :data="pageCircuitBreakers" :empty-text="lt('暂无页面熔断规则', '暫無頁面熔斷規則', 'No page circuit breakers')" @row-click="showDetail('breaker', $event)">
            <el-table-column prop="pageKey" :label="lt('页面键', '頁面鍵', 'Page Key')" width="160" />
            <el-table-column prop="pageName" :label="lt('页面名称', '頁面名稱', 'Page Name')" min-width="150" />
            <el-table-column :label="lt('状态', '狀態', 'Status')" width="100">
              <template #default="{ row }"><el-tag :type="featureStatusType(row.status)">{{ row.status }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="audienceScope" :label="lt('生效范围', '生效範圍', 'Audience')" width="120" />
            <el-table-column prop="degradeMode" :label="lt('降级方式', '降級方式', 'Degrade')" width="140" />
            <el-table-column prop="reason" :label="lt('原因', '原因', 'Reason')" min-width="180" show-overflow-tooltip />
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="180">
              <template #default="{ row }">
                <el-button link @click.stop="showDetail('breaker', row)">{{ lt('详情', '詳情', 'Detail') }}</el-button>
                <el-button type="primary" link @click.stop="openBreakerDialog(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>{{ lt('Bridge 能力', 'Bridge 能力', 'Bridge APIs') }}</template>
          <el-table :data="bridgeApis" :empty-text="lt('暂无 Bridge 数据', '暫無 Bridge 資料', 'No bridge data')" @row-click="showDetail('bridge', $event)">
            <el-table-column prop="apiName" label="API" min-width="180" />
            <el-table-column prop="module" :label="lt('模块', '模組', 'Module')" width="120" />
            <el-table-column prop="supportStatus" :label="lt('支持状态', '支援狀態', 'Support')" width="140" />
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="100">
              <template #default="{ row }"><el-button link @click.stop="showDetail('bridge', row)">{{ lt('详情', '詳情', 'Detail') }}</el-button></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>{{ lt('已接入游戏资产', '已接入遊戲資產', 'Integrated Game Assets') }}</template>
          <el-table :data="androidGames" :empty-text="lt('暂无游戏资产', '暫無遊戲資產', 'No game assets')" @row-click="showDetail('asset', $event)">
            <el-table-column prop="gameName" :label="lt('游戏', '遊戲', 'Game')" min-width="180" />
            <el-table-column prop="version" :label="lt('版本', '版本', 'Version')" width="110" />
            <el-table-column :label="lt('运行时就绪', '運行時就緒', 'Runtime Ready')" width="130">
              <template #default="{ row }">
                <el-tag :type="row.runtimeReady ? 'success' : 'warning'">{{ row.runtimeReady ? lt('已就绪', '已就緒', 'Ready') : lt('待处理', '待處理', 'Pending') }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" :label="lt('审核状态', '審核狀態', 'Review')" width="120" />
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="100">
              <template #default="{ row }"><el-button link @click.stop="showDetail('asset', row)">{{ lt('详情', '詳情', 'Detail') }}</el-button></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-drawer v-model="detailVisible" :title="detailTitle" :size="detailDrawerSize">
      <template v-if="detailRecord">
        <el-descriptions :column="1" border>
          <el-descriptions-item v-for="item in detailItems" :key="item.label" :label="item.label">
            {{ item.value }}
          </el-descriptions-item>
        </el-descriptions>
        <div class="detail-actions">
          <el-button v-if="detailType === 'channel'" type="primary" @click="openChannelDialog(detailRecord)">{{ lt('编辑渠道规则', '編輯渠道規則', 'Edit Channel Rule') }}</el-button>
          <el-button v-else-if="detailType === 'feature'" type="primary" @click="openFeatureDialog(detailRecord)">{{ lt('编辑功能开关', '編輯功能開關', 'Edit Feature Toggle') }}</el-button>
          <el-button v-else-if="detailType === 'experiment'" type="primary" @click="openExperimentDialog(detailRecord)">{{ lt('编辑实验', '編輯實驗', 'Edit Experiment') }}</el-button>
          <el-button v-else-if="detailType === 'gray'" type="primary" @click="openGrayPlanDialog(detailRecord)">{{ lt('编辑灰度计划', '編輯灰度計畫', 'Edit Gray Plan') }}</el-button>
          <el-button v-else-if="detailType === 'blacklist'" type="primary" @click="openBlacklistDialog(detailRecord)">{{ lt('编辑黑名单规则', '編輯黑名單規則', 'Edit Blacklist Rule') }}</el-button>
          <el-button v-else-if="detailType === 'breaker'" type="primary" @click="openBreakerDialog(detailRecord)">{{ lt('编辑熔断规则', '編輯熔斷規則', 'Edit Breaker Rule') }}</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="channelDialogVisible" :title="editingChannelCode ? lt('编辑渠道规则', '編輯渠道規則', 'Edit Channel Rule') : lt('新增渠道规则', '新增渠道規則', 'New Channel Rule')" :width="dialogWidth('520px')">
      <el-form :model="channelForm" label-position="top">
        <el-form-item :label="lt('渠道编码', '渠道編碼', 'Channel Code')"><el-input v-model="channelForm.channelCode" :disabled="Boolean(editingChannelCode)" /></el-form-item>
        <el-form-item :label="lt('渠道名称', '渠道名稱', 'Channel Name')"><el-input v-model="channelForm.channelName" /></el-form-item>
        <el-form-item :label="lt('状态', '狀態', 'Status')">
          <el-select v-model="channelForm.status">
            <el-option label="ACTIVE" value="ACTIVE" />
            <el-option label="PAUSED" value="PAUSED" />
            <el-option label="BLOCKED" value="BLOCKED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('最低版本', '最低版本', 'Minimum Version')"><el-input v-model="channelForm.minimumVersion" /></el-form-item>
        <el-form-item :label="lt('灰度流量', '灰度流量', 'Gray Traffic')"><el-input-number v-model="channelForm.trafficPercentage" :min="0" :max="100" style="width: 100%" /></el-form-item>
        <el-form-item><el-switch v-model="channelForm.grayReleaseEnabled" /> {{ lt('启用灰度', '啟用灰度', 'Enable Gray Release') }}</el-form-item>
        <el-form-item><el-switch v-model="channelForm.forceUpdateEnabled" /> {{ lt('启用强更', '啟用強更', 'Enable Force Update') }}</el-form-item>
        <el-form-item :label="lt('备注', '備註', 'Note')"><el-input v-model="channelForm.note" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="channelDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="savingChannel" @click="saveChannel">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="featureDialogVisible" :title="editingFeatureKey ? lt('编辑功能开关', '編輯功能開關', 'Edit Feature Toggle') : lt('新增功能开关', '新增功能開關', 'New Feature Toggle')" :width="dialogWidth('520px')">
      <el-form :model="featureForm" label-position="top">
        <el-form-item :label="lt('开关键', '開關鍵', 'Feature Key')"><el-input v-model="featureForm.featureKey" :disabled="Boolean(editingFeatureKey)" /></el-form-item>
        <el-form-item :label="lt('名称', '名稱', 'Name')"><el-input v-model="featureForm.featureName" /></el-form-item>
        <el-form-item :label="lt('状态', '狀態', 'Status')">
          <el-select v-model="featureForm.status">
            <el-option label="ENABLED" value="ENABLED" />
            <el-option label="GRAY" value="GRAY" />
            <el-option label="DISABLED" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('作用域', '作用域', 'Scope')">
          <el-select v-model="featureForm.scope">
            <el-option label="GLOBAL" value="GLOBAL" />
            <el-option label="ANDROID_ONLY" value="ANDROID_ONLY" />
            <el-option label="RUNTIME_ONLY" value="RUNTIME_ONLY" />
            <el-option label="BRIDGE_ONLY" value="BRIDGE_ONLY" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('负责人', '負責人', 'Owner')"><el-input v-model="featureForm.owner" /></el-form-item>
        <el-form-item :label="lt('备注', '備註', 'Note')"><el-input v-model="featureForm.note" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="featureDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="savingFeature" @click="saveFeature">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="experimentDialogVisible" :title="editingExperimentKey ? lt('编辑 AB 实验', '編輯 AB 實驗', 'Edit AB Experiment') : lt('新增 AB 实验', '新增 AB 實驗', 'New AB Experiment')" :width="dialogWidth('620px')">
      <el-form :model="experimentForm" label-position="top">
        <el-form-item :label="lt('实验键', '實驗鍵', 'Experiment Key')"><el-input v-model="experimentForm.experimentKey" :disabled="Boolean(editingExperimentKey)" /></el-form-item>
        <el-form-item :label="lt('实验名称', '實驗名稱', 'Experiment Name')"><el-input v-model="experimentForm.experimentName" /></el-form-item>
        <el-form-item :label="lt('状态', '狀態', 'Status')">
          <el-select v-model="experimentForm.status">
            <el-option label="DRAFT" value="DRAFT" />
            <el-option label="RUNNING" value="RUNNING" />
            <el-option label="PAUSED" value="PAUSED" />
            <el-option label="ARCHIVED" value="ARCHIVED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('实验层', '實驗層', 'Layer Key')"><el-input v-model="experimentForm.layerKey" /></el-form-item>
        <el-form-item :label="lt('负责人', '負責人', 'Owner')"><el-input v-model="experimentForm.owner" /></el-form-item>
        <el-form-item :label="lt('渠道', '渠道', 'Channel Code')"><el-input v-model="experimentForm.channelCode" /></el-form-item>
        <el-form-item :label="lt('最小版本', '最小版本', 'Min Version')"><el-input v-model="experimentForm.minAppVersion" /></el-form-item>
        <el-form-item :label="lt('最大版本', '最大版本', 'Max Version')"><el-input v-model="experimentForm.maxAppVersion" /></el-form-item>
        <el-form-item :label="lt('实验流量', '實驗流量', 'Traffic Percentage')"><el-input-number v-model="experimentForm.trafficPercentage" :min="1" :max="100" style="width: 100%" /></el-form-item>
        <el-form-item :label="lt('A 版本名称', 'A 版本名稱', 'Variant A Name')"><el-input v-model="experimentForm.variantAName" /></el-form-item>
        <el-form-item :label="lt('A 版本占比', 'A 版本占比', 'Variant A %')"><el-input-number v-model="experimentForm.variantAPercentage" :min="0" :max="100" style="width: 100%" /></el-form-item>
        <el-form-item :label="lt('B 版本名称', 'B 版本名稱', 'Variant B Name')"><el-input v-model="experimentForm.variantBName" /></el-form-item>
        <el-form-item :label="lt('B 版本占比', 'B 版本占比', 'Variant B %')"><el-input-number v-model="experimentForm.variantBPercentage" :min="0" :max="100" style="width: 100%" /></el-form-item>
        <el-form-item :label="lt('实验假设', '實驗假設', 'Hypothesis')"><el-input v-model="experimentForm.hypothesis" type="textarea" :rows="2" maxlength="256" show-word-limit /></el-form-item>
        <el-form-item :label="lt('成功指标', '成功指標', 'Success Metric')"><el-input v-model="experimentForm.successMetric" maxlength="128" /></el-form-item>
        <el-form-item :label="lt('备注', '備註', 'Note')"><el-input v-model="experimentForm.note" type="textarea" :rows="2" maxlength="256" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="experimentDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="savingExperiment" @click="saveExperiment">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="grayPlanDialogVisible" :title="editingGrayPlanCode ? lt('编辑灰度计划', '編輯灰度計畫', 'Edit Gray Plan') : lt('新增灰度计划', '新增灰度計畫', 'New Gray Plan')" :width="dialogWidth('620px')">
      <el-form :model="grayPlanForm" label-position="top">
        <el-form-item :label="lt('计划编码', '計畫編碼', 'Plan Code')"><el-input v-model="grayPlanForm.planCode" :disabled="Boolean(editingGrayPlanCode)" /></el-form-item>
        <el-form-item :label="lt('计划名称', '計畫名稱', 'Plan Name')"><el-input v-model="grayPlanForm.planName" /></el-form-item>
        <el-form-item :label="lt('状态', '狀態', 'Status')">
          <el-select v-model="grayPlanForm.status">
            <el-option label="DRAFT" value="DRAFT" />
            <el-option label="ACTIVE" value="ACTIVE" />
            <el-option label="PAUSED" value="PAUSED" />
            <el-option label="STOPPED" value="STOPPED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('渠道编码', '渠道編碼', 'Channel Code')"><el-input v-model="grayPlanForm.channelCode" /></el-form-item>
        <el-form-item :label="lt('最小版本', '最小版本', 'Min Version')"><el-input v-model="grayPlanForm.minAppVersion" /></el-form-item>
        <el-form-item :label="lt('最大版本', '最大版本', 'Max Version')"><el-input v-model="grayPlanForm.maxAppVersion" /></el-form-item>
        <el-form-item :label="lt('总流量占比', '總流量占比', 'Overall Traffic %')"><el-input-number v-model="grayPlanForm.overallTrafficPercentage" :min="1" :max="100" style="width: 100%" /></el-form-item>
        <el-form-item :label="lt('新用户占比', '新用戶占比', 'New User %')"><el-input-number v-model="grayPlanForm.newUserPercentage" :min="0" :max="100" style="width: 100%" /></el-form-item>
        <el-form-item :label="lt('回流用户占比', '回流用戶占比', 'Returning User %')"><el-input-number v-model="grayPlanForm.returningUserPercentage" :min="0" :max="100" style="width: 100%" /></el-form-item>
        <el-form-item :label="lt('白名单占比', '白名單占比', 'Whitelist %')"><el-input-number v-model="grayPlanForm.whitelistPercentage" :min="0" :max="100" style="width: 100%" /></el-form-item>
        <el-form-item :label="lt('地区码', '地區碼', 'Region Code')"><el-input v-model="grayPlanForm.regionCode" /></el-form-item>
        <el-form-item :label="lt('设备档位', '設備檔位', 'Device Tier')"><el-input v-model="grayPlanForm.deviceTier" /></el-form-item>
        <el-form-item :label="lt('回退策略', '回退策略', 'Fallback Policy')">
          <el-select v-model="grayPlanForm.fallbackPolicy">
            <el-option label="ROLLBACK" value="ROLLBACK" />
            <el-option label="HOLD" value="HOLD" />
            <el-option label="FORCE_UPDATE" value="FORCE_UPDATE" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('备注', '備註', 'Note')"><el-input v-model="grayPlanForm.note" type="textarea" :rows="2" maxlength="256" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="grayPlanDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="savingGrayPlan" @click="saveGrayPlan">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="blacklistDialogVisible" :title="editingBlacklistCode ? lt('编辑兼容黑名单', '編輯相容黑名單', 'Edit Compatibility Blacklist') : lt('新增兼容黑名单', '新增相容黑名單', 'New Compatibility Blacklist')" :width="dialogWidth('560px')">
      <el-form :model="blacklistForm" label-position="top">
        <el-form-item :label="lt('规则编码', '規則編碼', 'Rule Code')"><el-input v-model="blacklistForm.ruleCode" :disabled="Boolean(editingBlacklistCode)" /></el-form-item>
        <el-form-item :label="lt('状态', '狀態', 'Status')">
          <el-select v-model="blacklistForm.status">
            <el-option label="ACTIVE" value="ACTIVE" />
            <el-option label="PAUSED" value="PAUSED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('目标类型', '目標類型', 'Target Type')">
          <el-select v-model="blacklistForm.targetType">
            <el-option label="DEVICE_MODEL" value="DEVICE_MODEL" />
            <el-option label="MANUFACTURER" value="MANUFACTURER" />
            <el-option label="CHANNEL_CODE" value="CHANNEL_CODE" />
            <el-option label="APP_VERSION" value="APP_VERSION" />
            <el-option label="SDK_INT" value="SDK_INT" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('目标值', '目標值', 'Target Value')"><el-input v-model="blacklistForm.targetValue" /></el-form-item>
        <el-form-item :label="lt('最小 App 版本', '最小 App 版本', 'Min App Version')"><el-input v-model="blacklistForm.minAppVersion" /></el-form-item>
        <el-form-item :label="lt('最大 App 版本', '最大 App 版本', 'Max App Version')"><el-input v-model="blacklistForm.maxAppVersion" /></el-form-item>
        <el-form-item :label="lt('最小 SDK', '最小 SDK', 'Min SDK')"><el-input-number v-model="blacklistForm.minSdkInt" :min="1" :max="99" style="width: 100%" /></el-form-item>
        <el-form-item :label="lt('最大 SDK', '最大 SDK', 'Max SDK')"><el-input-number v-model="blacklistForm.maxSdkInt" :min="1" :max="99" style="width: 100%" /></el-form-item>
        <el-form-item :label="lt('原因', '原因', 'Reason')"><el-input v-model="blacklistForm.reason" type="textarea" :rows="3" maxlength="256" show-word-limit /></el-form-item>
        <el-form-item :label="lt('备注', '備註', 'Note')"><el-input v-model="blacklistForm.note" type="textarea" :rows="2" maxlength="256" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="blacklistDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="savingBlacklist" @click="saveBlacklist">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="breakerDialogVisible" :title="editingBreakerKey ? lt('编辑页面熔断', '編輯頁面熔斷', 'Edit Page Breaker') : lt('新增页面熔断', '新增頁面熔斷', 'New Page Breaker')" :width="dialogWidth('560px')">
      <el-form :model="breakerForm" label-position="top">
        <el-form-item :label="lt('页面键', '頁面鍵', 'Page Key')"><el-input v-model="breakerForm.pageKey" :disabled="Boolean(editingBreakerKey)" /></el-form-item>
        <el-form-item :label="lt('页面名称', '頁面名稱', 'Page Name')"><el-input v-model="breakerForm.pageName" /></el-form-item>
        <el-form-item :label="lt('状态', '狀態', 'Status')">
          <el-select v-model="breakerForm.status">
            <el-option label="ENABLED" value="ENABLED" />
            <el-option label="GRAY" value="GRAY" />
            <el-option label="DISABLED" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('生效范围', '生效範圍', 'Audience Scope')">
          <el-select v-model="breakerForm.audienceScope">
            <el-option label="ALL" value="ALL" />
            <el-option label="GRAY_ONLY" value="GRAY_ONLY" />
            <el-option label="CHANNEL_ONLY" value="CHANNEL_ONLY" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('指定渠道', '指定渠道', 'Channel Code')"><el-input v-model="breakerForm.channelCode" /></el-form-item>
        <el-form-item :label="lt('最小 App 版本', '最小 App 版本', 'Min App Version')"><el-input v-model="breakerForm.minAppVersion" /></el-form-item>
        <el-form-item :label="lt('最大 App 版本', '最大 App 版本', 'Max App Version')"><el-input v-model="breakerForm.maxAppVersion" /></el-form-item>
        <el-form-item :label="lt('降级方式', '降級方式', 'Degrade Mode')">
          <el-select v-model="breakerForm.degradeMode">
            <el-option label="HIDE" value="HIDE" />
            <el-option label="NATIVE_FALLBACK" value="NATIVE_FALLBACK" />
            <el-option label="MAINTENANCE" value="MAINTENANCE" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('原因', '原因', 'Reason')"><el-input v-model="breakerForm.reason" type="textarea" :rows="3" maxlength="256" show-word-limit /></el-form-item>
        <el-form-item :label="lt('备注', '備註', 'Note')"><el-input v-model="breakerForm.note" type="textarea" :rows="2" maxlength="256" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="breakerDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="savingBreaker" @click="saveBreaker">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getAndroidAbExperiments,
  getAndroidBridgeApis,
  getAndroidCompatibilityBlacklists,
  getAndroidChannels,
  getAndroidConfig,
  getAndroidGrayReleasePlans,
  getAndroidFeatureToggles,
  getAndroidGameAssets,
  getAndroidPageCircuitBreakers,
  getDiscoverOpsConfig,
  updateAndroidConfig,
  updateDiscoverOpsConfig,
  upsertAndroidAbExperiment,
  upsertAndroidCompatibilityBlacklist,
  upsertAndroidChannel,
  upsertAndroidGrayReleasePlan,
  upsertAndroidPageCircuitBreaker,
  upsertAndroidFeatureToggle
} from '../../api'
import { useI18nLite } from '../../i18n'
import { useViewport } from '../../composables/useViewport'

const { lt } = useI18nLite()
const { isTabletOrBelow, isPhone } = useViewport()
const savingRuntime = ref(false)
const savingDiscover = ref(false)
const savingChannel = ref(false)
const savingFeature = ref(false)
const savingExperiment = ref(false)
const savingGrayPlan = ref(false)
const savingBlacklist = ref(false)
const savingBreaker = ref(false)
const loadError = ref('')
const channelDialogVisible = ref(false)
const featureDialogVisible = ref(false)
const experimentDialogVisible = ref(false)
const grayPlanDialogVisible = ref(false)
const blacklistDialogVisible = ref(false)
const breakerDialogVisible = ref(false)
const editingChannelCode = ref('')
const editingFeatureKey = ref('')
const editingExperimentKey = ref('')
const editingGrayPlanCode = ref('')
const editingBlacklistCode = ref('')
const editingBreakerKey = ref('')
const detailVisible = ref(false)
const detailType = ref('')
const detailRecord = ref(null)
const bridgeApis = ref([])
const androidGames = ref([])
const channelRules = ref([])
const featureToggles = ref([])
const abExperiments = ref([])
const grayReleasePlans = ref([])
const compatibilityBlacklists = ref([])
const pageCircuitBreakers = ref([])
const availableGames = ref([])
const discoverHero = ref({ appId: '', title: '', subtitle: '', badgeText: '', coverUrl: '' })
const discoverGameTopBanners = ref([])
const discoverTopBannerRows = ref([])
const discoverCommunityItems = ref([])
const detailDrawerSize = computed(() => (isPhone.value ? '100%' : isTabletOrBelow.value ? '72%' : '36%'))

const dialogWidth = (desktop, tablet = '88%', mobile = '94%') => {
  if (isPhone.value) return mobile
  if (isTabletOrBelow.value) return tablet
  return desktop
}

const runtimeForm = reactive({
  apiBaseUrl: '',
  assetHost: '',
  defaultLanguage: 'zh-CN',
  minimumSupportedVersion: '1.0.0',
  forceUpdateEnabled: false,
  grayReleaseDescription: '',
  debugUseMockData: false,
  enableSyncBridge: true,
  allowCleartextTraffic: false,
  webViewMixedContentMode: '',
  webViewCacheMode: '',
  maxZipSizeMb: 50,
  startupRoutePolicy: ''
})

const channelForm = reactive({
  channelCode: '',
  channelName: '',
  status: 'ACTIVE',
  minimumVersion: '',
  forceUpdateEnabled: false,
  grayReleaseEnabled: false,
  trafficPercentage: 100,
  note: ''
})

const featureForm = reactive({
  featureKey: '',
  featureName: '',
  status: 'ENABLED',
  scope: 'GLOBAL',
  owner: '',
  note: ''
})

const experimentForm = reactive({
  experimentKey: '',
  experimentName: '',
  status: 'DRAFT',
  layerKey: '',
  owner: '',
  channelCode: '',
  minAppVersion: '',
  maxAppVersion: '',
  trafficPercentage: 10,
  variantAName: 'CONTROL',
  variantAPercentage: 50,
  variantBName: 'VARIANT',
  variantBPercentage: 50,
  hypothesis: '',
  successMetric: '',
  note: ''
})

const grayPlanForm = reactive({
  planCode: '',
  planName: '',
  status: 'DRAFT',
  channelCode: '',
  minAppVersion: '',
  maxAppVersion: '',
  overallTrafficPercentage: 10,
  newUserPercentage: 50,
  returningUserPercentage: 50,
  whitelistPercentage: 0,
  regionCode: '',
  deviceTier: '',
  fallbackPolicy: 'ROLLBACK',
  note: ''
})

const blacklistForm = reactive({
  ruleCode: '',
  status: 'ACTIVE',
  targetType: 'DEVICE_MODEL',
  targetValue: '',
  minAppVersion: '',
  maxAppVersion: '',
  minSdkInt: null,
  maxSdkInt: null,
  reason: '',
  note: ''
})

const breakerForm = reactive({
  pageKey: '',
  pageName: '',
  status: 'ENABLED',
  audienceScope: 'ALL',
  channelCode: '',
  minAppVersion: '',
  maxAppVersion: '',
  degradeMode: 'HIDE',
  reason: '',
  note: ''
})

const discoverForm = reactive({
  heroAppId: '',
  rankedAppIds: [],
  newbieAppIds: [],
  everyoneAppIds: []
})

const loadData = async () => {
  try {
    loadError.value = ''
    const [runtimeRes, discoverRes, bridgeRes, gamesRes, channelsRes, featuresRes, experimentRes, grayPlanRes, blacklistRes, breakerRes] = await Promise.all([
      getAndroidConfig(),
      getDiscoverOpsConfig(),
      getAndroidBridgeApis(),
      getAndroidGameAssets(),
      getAndroidChannels(),
      getAndroidFeatureToggles(),
      getAndroidAbExperiments(),
      getAndroidGrayReleasePlans(),
      getAndroidCompatibilityBlacklists(),
      getAndroidPageCircuitBreakers()
    ])
    Object.assign(runtimeForm, runtimeRes.data || {})
    bridgeApis.value = bridgeRes.data || []
    androidGames.value = gamesRes.data || []
    channelRules.value = channelsRes.data || []
    featureToggles.value = featuresRes.data || []
    abExperiments.value = experimentRes.data || []
    grayReleasePlans.value = grayPlanRes.data || []
    compatibilityBlacklists.value = blacklistRes.data || []
    pageCircuitBreakers.value = breakerRes.data || []
    availableGames.value = discoverRes.data?.availableGames || []
    discoverHero.value = discoverRes.data?.hero || discoverHero.value
    discoverGameTopBanners.value = discoverRes.data?.gameTopBanners || []
    discoverTopBannerRows.value = discoverRes.data?.discoverTopBanners || []
    discoverCommunityItems.value = discoverRes.data?.communityItems || []
    discoverForm.heroAppId = discoverRes.data?.hero?.appId || ''
    discoverForm.rankedAppIds = [...(discoverRes.data?.rankedAppIds || [])]
    discoverForm.newbieAppIds = [...(discoverRes.data?.newbieAppIds || [])]
    discoverForm.everyoneAppIds = [...(discoverRes.data?.everyoneAppIds || [])]
  } catch (error) {
    loadError.value = error.message || lt('加载运行时配置失败', '載入運行時配置失敗', 'Failed to load runtime configuration')
    ElMessage.error(loadError.value)
  }
}

const detailTitle = computed(() => ({
  channel: lt('渠道规则详情', '渠道規則詳情', 'Channel Rule Detail'),
  feature: lt('功能开关详情', '功能開關詳情', 'Feature Toggle Detail'),
  experiment: lt('AB 实验详情', 'AB 實驗詳情', 'AB Experiment Detail'),
  gray: lt('灰度计划详情', '灰度計畫詳情', 'Gray Plan Detail'),
  blacklist: lt('兼容黑名单详情', '相容黑名單詳情', 'Compatibility Rule Detail'),
  breaker: lt('页面熔断详情', '頁面熔斷詳情', 'Page Breaker Detail'),
  bridge: lt('Bridge 能力详情', 'Bridge 能力詳情', 'Bridge API Detail'),
  asset: lt('游戏资产详情', '遊戲資產詳情', 'Game Asset Detail')
}[detailType.value] || lt('详情', '詳情', 'Detail')))

const detailItems = computed(() => {
  const row = detailRecord.value || {}
  const itemsByType = {
    channel: [
      ['Channel Code', row.channelCode], ['Channel Name', row.channelName], ['Status', row.status],
      ['Minimum Version', row.minimumVersion], ['Gray Release', row.grayReleaseEnabled ? `${row.trafficPercentage}%` : 'OFF'],
      ['Force Update', row.forceUpdateEnabled ? 'ON' : 'OFF'], ['Note', row.note || '-']
    ],
    feature: [
      ['Feature Key', row.featureKey], ['Feature Name', row.featureName], ['Status', row.status],
      ['Scope', row.scope], ['Owner', row.owner || '-'], ['Note', row.note || '-']
    ],
    experiment: [
      ['Experiment Key', row.experimentKey], ['Experiment Name', row.experimentName], ['Status', row.status],
      ['Layer', row.layerKey], ['Traffic', `${row.trafficPercentage || 0}%`], ['Channel', row.channelCode || '-'],
      ['Version Range', `${row.minAppVersion || '-'} ~ ${row.maxAppVersion || '-'}`],
      ['Variants', `${row.variantAName || 'A'} ${row.variantAPercentage || 0}% / ${row.variantBName || 'B'} ${row.variantBPercentage || 0}%`],
      ['Hypothesis', row.hypothesis || '-'], ['Success Metric', row.successMetric || '-'], ['Note', row.note || '-']
    ],
    gray: [
      ['Plan Code', row.planCode], ['Plan Name', row.planName], ['Status', row.status], ['Channel', row.channelCode || '-'],
      ['Version Range', `${row.minAppVersion || '-'} ~ ${row.maxAppVersion || '-'}`], ['Overall Traffic', `${row.overallTrafficPercentage || 0}%`],
      ['New / Returning', `${row.newUserPercentage || 0}% / ${row.returningUserPercentage || 0}%`], ['Whitelist', `${row.whitelistPercentage || 0}%`],
      ['Region', row.regionCode || '-'], ['Device Tier', row.deviceTier || '-'], ['Fallback', row.fallbackPolicy || '-'], ['Note', row.note || '-']
    ],
    blacklist: [
      ['Rule Code', row.ruleCode], ['Status', row.status], ['Target Type', row.targetType], ['Target Value', row.targetValue],
      ['App Version', `${row.minAppVersion || '-'} ~ ${row.maxAppVersion || '-'}`], ['SDK Range', `${row.minSdkInt ?? '-'} ~ ${row.maxSdkInt ?? '-'}`],
      ['Reason', row.reason || '-'], ['Note', row.note || '-']
    ],
    breaker: [
      ['Page Key', row.pageKey], ['Page Name', row.pageName], ['Status', row.status], ['Audience', row.audienceScope],
      ['Channel', row.channelCode || '-'], ['Version Range', `${row.minAppVersion || '-'} ~ ${row.maxAppVersion || '-'}`],
      ['Degrade Mode', row.degradeMode || '-'], ['Reason', row.reason || '-'], ['Note', row.note || '-']
    ],
    bridge: [
      ['API', row.apiName], ['Module', row.module], ['Support Status', row.supportStatus], ['Description', row.description || '-']
    ],
    asset: [
      ['Game', row.gameName], ['Version', row.version], ['Runtime Ready', row.runtimeReady ? 'YES' : 'NO'],
      ['Review Status', row.status || '-'], ['App ID', row.appId || '-'], ['Package Name', row.packageName || '-']
    ]
  }
  return (itemsByType[detailType.value] || []).map(([label, value]) => ({ label, value: value || value === 0 ? value : '-' }))
})

const showDetail = (type, row) => {
  detailType.value = type
  detailRecord.value = row
  detailVisible.value = true
}

const saveRuntime = async () => {
  try {
    if (!runtimeForm.minimumSupportedVersion.trim()) {
      ElMessage.warning(lt('请填写最低支持版本', '請填寫最低支援版本', 'Please enter the minimum supported version'))
      return
    }
    savingRuntime.value = true
    await updateAndroidConfig(runtimeForm)
    ElMessage.success(lt('运行时配置已保存', '運行時配置已儲存', 'Runtime config saved'))
  } catch (error) {
    ElMessage.error(error.message || lt('保存运行时配置失败', '儲存運行時配置失敗', 'Failed to save runtime config'))
  } finally {
    savingRuntime.value = false
  }
}

const saveDiscover = async () => {
  try {
    savingDiscover.value = true
    await updateDiscoverOpsConfig({
      hero: { ...discoverHero.value, appId: discoverForm.heroAppId },
      rankedAppIds: discoverForm.rankedAppIds,
      newbieAppIds: discoverForm.newbieAppIds,
      everyoneAppIds: discoverForm.everyoneAppIds,
      gameTopBanners: discoverGameTopBanners.value,
      discoverTopBanners: discoverTopBannerRows.value,
      communityItems: discoverCommunityItems.value
    })
    ElMessage.success(lt('推荐池已保存', '推薦池已儲存', 'Discover pools saved'))
  } catch (error) {
    ElMessage.error(error.message || lt('保存推荐池失败', '儲存推薦池失敗', 'Failed to save discover pools'))
  } finally {
    savingDiscover.value = false
  }
}

const openChannelDialog = (row = null) => {
  editingChannelCode.value = row?.channelCode || ''
  Object.assign(channelForm, row || {
    channelCode: '',
    channelName: '',
    status: 'ACTIVE',
    minimumVersion: '',
    forceUpdateEnabled: false,
    grayReleaseEnabled: false,
    trafficPercentage: 100,
    note: ''
  })
  channelDialogVisible.value = true
}

const saveChannel = async () => {
  try {
    savingChannel.value = true
    await upsertAndroidChannel({ ...channelForm })
    channelDialogVisible.value = false
    ElMessage.success(lt('渠道规则已保存', '渠道規則已儲存', 'Channel rule saved'))
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || lt('保存渠道规则失败', '儲存渠道規則失敗', 'Failed to save channel rule'))
  } finally {
    savingChannel.value = false
  }
}

const openFeatureDialog = (row = null) => {
  editingFeatureKey.value = row?.featureKey || ''
  Object.assign(featureForm, row || {
    featureKey: '',
    featureName: '',
    status: 'ENABLED',
    scope: 'GLOBAL',
    owner: '',
    note: ''
  })
  featureDialogVisible.value = true
}

const saveFeature = async () => {
  try {
    savingFeature.value = true
    await upsertAndroidFeatureToggle({ ...featureForm })
    featureDialogVisible.value = false
    ElMessage.success(lt('功能开关已保存', '功能開關已儲存', 'Feature toggle saved'))
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || lt('保存功能开关失败', '儲存功能開關失敗', 'Failed to save feature toggle'))
  } finally {
    savingFeature.value = false
  }
}

const openExperimentDialog = (row = null) => {
  editingExperimentKey.value = row?.experimentKey || ''
  Object.assign(experimentForm, row || {
    experimentKey: '',
    experimentName: '',
    status: 'DRAFT',
    layerKey: '',
    owner: '',
    channelCode: '',
    minAppVersion: '',
    maxAppVersion: '',
    trafficPercentage: 10,
    variantAName: 'CONTROL',
    variantAPercentage: 50,
    variantBName: 'VARIANT',
    variantBPercentage: 50,
    hypothesis: '',
    successMetric: '',
    note: ''
  })
  experimentDialogVisible.value = true
}

const saveExperiment = async () => {
  try {
    if ((experimentForm.variantAPercentage || 0) + (experimentForm.variantBPercentage || 0) !== 100) {
      ElMessage.warning(lt('AB 分桶占比之和必须为 100', 'AB 分桶占比之和必須為 100', 'Variant split must sum to 100'))
      return
    }
    savingExperiment.value = true
    await upsertAndroidAbExperiment({ ...experimentForm })
    experimentDialogVisible.value = false
    ElMessage.success(lt('AB 实验已保存', 'AB 實驗已儲存', 'AB experiment saved'))
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || lt('保存 AB 实验失败', '儲存 AB 實驗失敗', 'Failed to save AB experiment'))
  } finally {
    savingExperiment.value = false
  }
}

const openGrayPlanDialog = (row = null) => {
  editingGrayPlanCode.value = row?.planCode || ''
  Object.assign(grayPlanForm, row || {
    planCode: '',
    planName: '',
    status: 'DRAFT',
    channelCode: '',
    minAppVersion: '',
    maxAppVersion: '',
    overallTrafficPercentage: 10,
    newUserPercentage: 50,
    returningUserPercentage: 50,
    whitelistPercentage: 0,
    regionCode: '',
    deviceTier: '',
    fallbackPolicy: 'ROLLBACK',
    note: ''
  })
  grayPlanDialogVisible.value = true
}

const saveGrayPlan = async () => {
  try {
    if ((grayPlanForm.newUserPercentage || 0) + (grayPlanForm.returningUserPercentage || 0) !== 100) {
      ElMessage.warning(lt('新老用户占比之和必须为 100', '新老用戶占比之和必須為 100', 'New and returning user split must sum to 100'))
      return
    }
    if ((grayPlanForm.whitelistPercentage || 0) > (grayPlanForm.overallTrafficPercentage || 0)) {
      ElMessage.warning(lt('白名单占比不能高于总流量占比', '白名單占比不能高於總流量占比', 'Whitelist percentage cannot exceed overall traffic'))
      return
    }
    savingGrayPlan.value = true
    await upsertAndroidGrayReleasePlan({ ...grayPlanForm })
    grayPlanDialogVisible.value = false
    ElMessage.success(lt('灰度计划已保存', '灰度計畫已儲存', 'Gray release plan saved'))
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || lt('保存灰度计划失败', '儲存灰度計畫失敗', 'Failed to save gray release plan'))
  } finally {
    savingGrayPlan.value = false
  }
}

const openBlacklistDialog = (row = null) => {
  editingBlacklistCode.value = row?.ruleCode || ''
  Object.assign(blacklistForm, row || {
    ruleCode: '',
    status: 'ACTIVE',
    targetType: 'DEVICE_MODEL',
    targetValue: '',
    minAppVersion: '',
    maxAppVersion: '',
    minSdkInt: null,
    maxSdkInt: null,
    reason: '',
    note: ''
  })
  blacklistDialogVisible.value = true
}

const saveBlacklist = async () => {
  try {
    savingBlacklist.value = true
    await upsertAndroidCompatibilityBlacklist({ ...blacklistForm })
    blacklistDialogVisible.value = false
    ElMessage.success(lt('兼容黑名单已保存', '相容黑名單已儲存', 'Compatibility blacklist saved'))
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || lt('保存兼容黑名单失败', '儲存相容黑名單失敗', 'Failed to save compatibility blacklist'))
  } finally {
    savingBlacklist.value = false
  }
}

const openBreakerDialog = (row = null) => {
  editingBreakerKey.value = row?.pageKey || ''
  Object.assign(breakerForm, row || {
    pageKey: '',
    pageName: '',
    status: 'ENABLED',
    audienceScope: 'ALL',
    channelCode: '',
    minAppVersion: '',
    maxAppVersion: '',
    degradeMode: 'HIDE',
    reason: '',
    note: ''
  })
  breakerDialogVisible.value = true
}

const saveBreaker = async () => {
  try {
    savingBreaker.value = true
    await upsertAndroidPageCircuitBreaker({ ...breakerForm })
    breakerDialogVisible.value = false
    ElMessage.success(lt('页面熔断规则已保存', '頁面熔斷規則已儲存', 'Page breaker saved'))
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || lt('保存页面熔断失败', '儲存頁面熔斷失敗', 'Failed to save page breaker'))
  } finally {
    savingBreaker.value = false
  }
}

const channelStatusType = (status) => ({ ACTIVE: 'success', PAUSED: 'warning', BLOCKED: 'danger' }[status] || 'info')
const featureStatusType = (status) => ({ ENABLED: 'success', GRAY: 'warning', DISABLED: 'danger' }[status] || 'info')
const experimentStatusType = (status) => ({ RUNNING: 'success', PAUSED: 'warning', DRAFT: 'info', ARCHIVED: 'danger' }[status] || 'info')
const grayPlanStatusType = (status) => ({ ACTIVE: 'success', PAUSED: 'warning', DRAFT: 'info', STOPPED: 'danger' }[status] || 'info')

onMounted(loadData)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.section-row { margin-top: 0; }
.panel-card { border-radius: 18px; }
.card-header-inline { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.overview-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; }
.overview-chip { padding: 14px 16px; border-radius: 16px; background: #f8fafc; display: flex; flex-direction: column; gap: 6px; color: #475467; }
.overview-chip strong { font-size: 20px; color: #101828; }
.detail-actions { margin-top: 16px; display: flex; gap: 10px; flex-wrap: wrap; }
@media (max-width: 920px) {
  .card-header-inline { flex-direction: column; align-items: flex-start; }
}
</style>
