<template>
  <div class="pro-page">
    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ lt('风控与审计', '風控與審計', 'Risk and Audit') }}</div>
        <div class="panel-subtitle">{{ lt('跟踪敏感操作、失败动作与验证码发送状态，并把风险事件推进成可分派、可升级、可留档的处置流。', '追蹤敏感操作、失敗動作與驗證碼發送狀態，並把風險事件推進成可分派、可升級、可留檔的處置流。', 'Track sensitive operations, failed actions, and verification-code delivery, while turning risk incidents into assignable, traceable handling flows.') }}</div>
          </div>
          <div class="actions">
            <el-select v-model="auditFilters.success" clearable style="width: 140px">
              <el-option :label="lt('全部结果', '全部結果', 'All Results')" :value="undefined" />
              <el-option :label="lt('成功', '成功', 'Success')" :value="true" />
              <el-option :label="lt('失败', '失敗', 'Failed')" :value="false" />
            </el-select>
            <el-select v-model="auditFilters.action" clearable filterable style="width: 180px">
              <el-option :label="lt('全部动作', '全部動作', 'All Actions')" value="" />
              <el-option v-for="item in actionOptions" :key="item" :label="item" :value="item" />
            </el-select>
            <el-input v-model.trim="auditFilters.targetAppId" clearable style="width: 180px" :placeholder="lt('按 AppID 搜索', '按 AppID 搜尋', 'Search by AppID')" />
            <el-button size="small" @click="openAccessRuleCreate">{{ lt('新增访问规则', '新增訪問規則', 'New Access Rule') }}</el-button>
            <el-button size="small" :loading="loading" @click="loadData">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
          </div>
        </div>
      </template>

      <div class="metric-row">
        <div class="metric-chip">{{ lt('审计总数', '審計總數', 'Audit Rows') }} {{ auditStats.total }}</div>
        <div class="metric-chip">{{ lt('成功操作', '成功操作', 'Successful') }} {{ auditStats.success }}</div>
        <div class="metric-chip danger">{{ lt('失败操作', '失敗操作', 'Failed') }} {{ auditStats.failed }}</div>
        <div class="metric-chip warning">{{ lt('高风险动作', '高風險動作', 'Sensitive Actions') }} {{ auditStats.sensitive }}</div>
        <div class="metric-chip">{{ lt('登录异常', '登入異常', 'Login Anomalies') }} {{ loginRiskStats.anomalyCount }}</div>
        <div class="metric-chip warning">{{ lt('活跃访问规则', '活躍訪問規則', 'Active Access Rules') }} {{ loginRiskStats.activeRuleCount }}</div>
        <div class="metric-chip">{{ lt('活跃行为规则', '活躍行為規則', 'Active Behavior Rules') }} {{ loginRiskStats.activeBehaviorRuleCount }}</div>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24">
        <el-card class="panel-card">
          <template #header>
            <div class="head-row">
              <span>{{ lt('风险事件台', '風險事件台', 'Risk Incident Desk') }}</span>
              <el-button type="primary" link @click="openIncidentCreate">{{ lt('登记事件', '登記事件', 'Create Incident') }}</el-button>
            </div>
          </template>
          <el-table :data="riskIncidents" v-loading="loading" :empty-text="lt('暂无风险事件', '暫無風險事件', 'No risk incidents')">
            <el-table-column prop="incidentType" :label="lt('类型', '類型', 'Type')" width="140" />
            <el-table-column prop="severity" :label="lt('等级', '等級', 'Severity')" width="120">
              <template #default="{ row }">
                <el-tag :type="severityType(row.severity)">{{ row.severity }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" :label="lt('状态', '狀態', 'Status')" width="120">
              <template #default="{ row }">
                <el-tag :type="incidentStatusType(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" :label="lt('标题', '標題', 'Title')" min-width="180" show-overflow-tooltip />
            <el-table-column prop="targetAppId" label="AppID" min-width="140" />
            <el-table-column prop="assignee" :label="lt('负责人', '負責人', 'Assignee')" width="140" />
            <el-table-column prop="ownerNote" :label="lt('处理备注', '處理備註', 'Owner Note')" min-width="180" show-overflow-tooltip />
            <el-table-column :label="lt('处理时限', '處理時限', 'Deadline')" min-width="160">
              <template #default="{ row }">{{ formatDate(row.handlingDeadline) }}</template>
            </el-table-column>
            <el-table-column :label="lt('更新时间', '更新時間', 'Updated')" min-width="160">
              <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="240" fixed="right">
              <template #default="{ row }">
                <el-button link type="warning" @click="openIncidentStatus(row, 'MITIGATING')">{{ lt('处理中', '處理中', 'Mitigate') }}</el-button>
                <el-button link type="success" @click="openIncidentStatus(row, 'RESOLVED')">{{ lt('关闭', '關閉', 'Resolve') }}</el-button>
                <el-button link @click="openIncidentHistory(row)">{{ lt('记录', '記錄', 'Records') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>
            <div class="head-row">
              <span>{{ lt('登录异常监控', '登入異常監控', 'Login Anomaly Monitor') }}</span>
              <span class="table-tip">{{ lt('识别高频失败、可疑成功和锁定事件。', '識別高頻失敗、可疑成功和鎖定事件。', 'Identify repeated failures, suspicious successes, and lockouts.') }}</span>
            </div>
          </template>
          <el-table :data="loginRiskEvents" v-loading="loading" :empty-text="lt('暂无登录风险事件', '暫無登入風險事件', 'No login risk events')">
            <el-table-column prop="eventType" :label="lt('事件', '事件', 'Event')" width="150" />
            <el-table-column prop="severity" :label="lt('等级', '等級', 'Severity')" width="110">
              <template #default="{ row }">
                <el-tag :type="severityType(row.severity)">{{ row.severity }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="result" :label="lt('结果', '結果', 'Result')" width="110">
              <template #default="{ row }">
                <el-tag :type="row.result === 'SUCCESS' ? 'success' : row.result === 'FAILED' ? 'danger' : 'info'">{{ row.result }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="loginId" :label="lt('账号', '帳號', 'Login ID')" min-width="160" show-overflow-tooltip />
            <el-table-column prop="clientIp" label="IP" min-width="140" />
            <el-table-column prop="deviceId" :label="lt('设备指纹', '設備指紋', 'Device Fingerprint')" min-width="180" show-overflow-tooltip />
            <el-table-column prop="reason" :label="lt('原因', '原因', 'Reason')" min-width="180" show-overflow-tooltip />
            <el-table-column :label="lt('时间', '時間', 'Created')" min-width="170">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>
            <div class="head-row">
              <span>{{ lt('IP / 设备风控规则', 'IP / 設備風控規則', 'IP / Device Access Rules') }}</span>
              <el-button type="primary" link @click="openAccessRuleCreate">{{ lt('新增规则', '新增規則', 'Add Rule') }}</el-button>
            </div>
          </template>
          <el-table :data="accessRules" v-loading="loading" :empty-text="lt('暂无访问规则', '暫無訪問規則', 'No access rules')">
            <el-table-column prop="ruleType" :label="lt('规则类型', '規則類型', 'Rule Type')" width="150" />
            <el-table-column prop="targetValue" :label="lt('目标值', '目標值', 'Target Value')" min-width="180" show-overflow-tooltip />
            <el-table-column prop="status" :label="lt('状态', '狀態', 'Status')" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'warning' : 'info'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="riskLevel" :label="lt('风险等级', '風險等級', 'Risk Level')" width="110">
              <template #default="{ row }">
                <el-tag :type="severityType(row.riskLevel)">{{ row.riskLevel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="note" :label="lt('备注', '備註', 'Note')" min-width="180" show-overflow-tooltip />
            <el-table-column :label="lt('到期时间', '到期時間', 'Expires')" min-width="170">
              <template #default="{ row }">{{ formatDate(row.expiresAt) }}</template>
            </el-table-column>
            <el-table-column prop="updatedBy" :label="lt('更新人', '更新人', 'Updated By')" width="140" />
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="110" fixed="right">
              <template #default="{ row }">
                <el-button link @click="openAccessRuleEdit(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>
            <div class="head-row">
              <span>{{ lt('高频行为规则', '高頻行為規則', 'High-frequency Risk Rules') }}</span>
              <el-button type="primary" link @click="openRiskRuleEdit(riskRules[0] || null)">{{ lt('管理规则', '管理規則', 'Manage Rules') }}</el-button>
            </div>
          </template>
          <el-table :data="riskRules" v-loading="loading" :empty-text="lt('暂无规则配置', '暫無規則配置', 'No risk rules')">
            <el-table-column prop="ruleCode" :label="lt('规则编码', '規則編碼', 'Rule Code')" width="180" />
            <el-table-column prop="ruleName" :label="lt('规则名称', '規則名稱', 'Rule Name')" min-width="180" />
            <el-table-column prop="enabled" :label="lt('启用', '啟用', 'Enabled')" width="90">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? lt('是', '是', 'Yes') : lt('否', '否', 'No') }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="thresholdCount" :label="lt('阈值', '閾值', 'Threshold')" width="90" />
            <el-table-column prop="windowMinutes" :label="lt('窗口(分)', '窗口(分)', 'Window (m)')" width="110" />
            <el-table-column prop="actionType" :label="lt('动作', '動作', 'Action')" width="110" />
            <el-table-column prop="severity" :label="lt('等级', '等級', 'Severity')" width="110">
              <template #default="{ row }">
                <el-tag :type="severityType(row.severity)">{{ row.severity }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link @click="openRiskRuleEdit(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>
            <div class="head-row">
              <span>{{ lt('审计日志', '審計日誌', 'Audit Logs') }}</span>
              <span class="table-tip">{{ lt('敏感状态变更必须可追溯', '敏感狀態變更必須可追溯', 'Sensitive state changes must be traceable') }}</span>
            </div>
          </template>
          <el-table :data="auditLogs" v-loading="loading" :empty-text="lt('暂无审计日志', '暫無審計日誌', 'No audit logs')">
            <el-table-column prop="action" :label="lt('动作', '動作', 'Action')" min-width="150" />
            <el-table-column :label="lt('结果', '結果', 'Result')" width="100">
              <template #default="{ row }">
                <el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? lt('成功', '成功', 'Success') : lt('失败', '失敗', 'Failed') }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="lt('快照类型', '快照類型', 'Snapshot Type')" min-width="140">
              <template #default="{ row }">{{ row.snapshotType || '-' }}</template>
            </el-table-column>
            <el-table-column :label="lt('变更字段', '變更欄位', 'Changed Fields')" width="110">
              <template #default="{ row }">{{ row.fieldDiffs?.length || 0 }}</template>
            </el-table-column>
            <el-table-column prop="targetAppId" label="AppID" min-width="150" />
            <el-table-column prop="targetGameId" :label="lt('游戏 ID', '遊戲 ID', 'Game ID')" width="110" />
            <el-table-column prop="operatorId" :label="lt('操作人', '操作人', 'Operator')" width="110" />
            <el-table-column prop="reason" :label="lt('原因', '原因', 'Reason')" min-width="180" show-overflow-tooltip />
            <el-table-column prop="requestUri" :label="lt('请求路径', '請求路徑', 'Request URI')" min-width="180" show-overflow-tooltip />
            <el-table-column :label="lt('时间', '時間', 'Created')" min-width="170">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link :disabled="!row.fieldDiffs?.length && !hasSnapshot(row)" @click="openAuditDetail(row)">{{ lt('查看变更', '查看變更', 'Inspect') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>{{ lt('验证码日志', '驗證碼日誌', 'Verification Logs') }}</template>
          <el-table :data="verificationLogs" v-loading="loading" :empty-text="lt('暂无验证码日志', '暫無驗證碼日誌', 'No verification logs')">
            <el-table-column prop="email" :label="lt('邮箱', '電子郵件', 'Email')" min-width="180" />
            <el-table-column prop="purpose" :label="lt('用途', '用途', 'Purpose')" width="140" />
            <el-table-column prop="source" :label="lt('来源', '來源', 'Source')" width="130" />
            <el-table-column :label="lt('发送结果', '發送結果', 'Result')" width="120">
              <template #default="{ row }">
                <el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? lt('成功', '成功', 'Success') : lt('失败', '失敗', 'Failed') }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="lt('时间', '時間', 'Created')" min-width="170">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="incidentDialogVisible" :title="lt('登记风险事件', '登記風險事件', 'Create Risk Incident')" width="620px">
      <el-form :model="incidentForm" label-width="110px">
        <el-form-item :label="lt('事件类型', '事件類型', 'Incident Type')">
          <el-select v-model="incidentForm.incidentType" style="width: 100%">
            <el-option label="ACCOUNT_RISK" value="ACCOUNT_RISK" />
            <el-option label="CONTENT_RISK" value="CONTENT_RISK" />
            <el-option label="RUNTIME_RISK" value="RUNTIME_RISK" />
            <el-option label="SECURITY_RISK" value="SECURITY_RISK" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('严重等级', '嚴重等級', 'Severity')">
          <el-select v-model="incidentForm.severity" style="width: 100%">
            <el-option label="LOW" value="LOW" />
            <el-option label="MEDIUM" value="MEDIUM" />
            <el-option label="HIGH" value="HIGH" />
            <el-option label="CRITICAL" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('标题', '標題', 'Title')">
          <el-input v-model="incidentForm.title" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item :label="lt('描述', '描述', 'Description')">
          <el-input v-model="incidentForm.description" type="textarea" :rows="5" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="AppID">
          <el-input v-model="incidentForm.targetAppId" maxlength="64" />
        </el-form-item>
        <el-form-item :label="lt('负责人', '負責人', 'Assignee')">
          <el-input v-model="incidentForm.assignee" maxlength="128" />
        </el-form-item>
        <el-form-item :label="lt('处理时限', '處理時限', 'Deadline')">
          <el-date-picker v-model="incidentForm.handlingDeadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" clearable />
        </el-form-item>
        <el-form-item :label="lt('处理备注', '處理備註', 'Owner Note')">
          <el-input v-model="incidentForm.ownerNote" type="textarea" :rows="3" maxlength="256" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="incidentDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="incidentSaving" @click="submitIncident">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="incidentHistoryVisible" :title="lt('风险事件记录', '風險事件記錄', 'Risk Incident Records')" width="860px">
      <div class="table-tip">{{ incidentHistoryTitle }}</div>
      <el-table :data="incidentHistoryRows" v-loading="incidentHistoryLoading" :empty-text="lt('暂无处理记录', '暫無處理記錄', 'No records')">
        <el-table-column prop="actionType" :label="lt('动作', '動作', 'Action')" width="160" />
        <el-table-column prop="fromStatus" :label="lt('前状态', '前狀態', 'From')" width="100" />
        <el-table-column prop="toStatus" :label="lt('后状态', '後狀態', 'To')" width="100" />
        <el-table-column prop="note" :label="lt('处理纪要', '處理紀要', 'Note')" min-width="260" show-overflow-tooltip />
        <el-table-column prop="operatorId" :label="lt('操作人', '操作人', 'Operator')" width="100" />
        <el-table-column :label="lt('时间', '時間', 'Created')" width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="auditDetailVisible" :title="lt('审计变更详情', '審計變更詳情', 'Audit Change Detail')" width="980px">
      <div v-if="auditDetailRow">
        <div class="table-tip" style="margin-bottom: 12px">
          {{ auditDetailRow.action }} / {{ auditDetailRow.snapshotType || '-' }} / {{ formatDate(auditDetailRow.createdAt) }}
        </div>
        <el-table :data="auditDetailRow.fieldDiffs || []" :empty-text="lt('没有字段变化', '沒有欄位變化', 'No field changes')">
          <el-table-column prop="field" :label="lt('字段', '欄位', 'Field')" min-width="200" />
          <el-table-column prop="beforeValue" :label="lt('变更前', '變更前', 'Before')" min-width="220" show-overflow-tooltip />
          <el-table-column prop="afterValue" :label="lt('变更后', '變更後', 'After')" min-width="220" show-overflow-tooltip />
          <el-table-column prop="changeType" :label="lt('类型', '類型', 'Type')" width="120" />
        </el-table>
        <el-row :gutter="12" style="margin-top: 14px">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>{{ lt('变更前快照', '變更前快照', 'Before Snapshot') }}</template>
              <pre class="snapshot-pre">{{ formatJson(auditDetailRow.beforeSnapshot) }}</pre>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>{{ lt('变更后快照', '變更後快照', 'After Snapshot') }}</template>
              <pre class="snapshot-pre">{{ formatJson(auditDetailRow.afterSnapshot) }}</pre>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-dialog>

    <el-dialog v-model="accessRuleDialogVisible" :title="accessRuleDialogMode === 'edit' ? lt('编辑访问规则', '編輯訪問規則', 'Edit Access Rule') : lt('新增访问规则', '新增訪問規則', 'Create Access Rule')" width="620px">
      <el-form :model="accessRuleForm" label-width="120px">
        <el-form-item :label="lt('规则类型', '規則類型', 'Rule Type')">
          <el-select v-model="accessRuleForm.ruleType" style="width: 100%">
            <el-option label="IP_BLOCK" value="IP_BLOCK" />
            <el-option label="DEVICE_BLOCK" value="DEVICE_BLOCK" />
            <el-option label="IP_WATCH" value="IP_WATCH" />
            <el-option label="DEVICE_WATCH" value="DEVICE_WATCH" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('目标值', '目標值', 'Target Value')">
          <el-input v-model="accessRuleForm.targetValue" maxlength="128" />
        </el-form-item>
        <el-form-item :label="lt('状态', '狀態', 'Status')">
          <el-select v-model="accessRuleForm.status" style="width: 100%">
            <el-option label="ACTIVE" value="ACTIVE" />
            <el-option label="INACTIVE" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('风险等级', '風險等級', 'Risk Level')">
          <el-select v-model="accessRuleForm.riskLevel" style="width: 100%">
            <el-option label="LOW" value="LOW" />
            <el-option label="MEDIUM" value="MEDIUM" />
            <el-option label="HIGH" value="HIGH" />
            <el-option label="CRITICAL" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('到期时间', '到期時間', 'Expires At')">
          <el-date-picker v-model="accessRuleForm.expiresAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" clearable />
        </el-form-item>
        <el-form-item :label="lt('备注', '備註', 'Note')">
          <el-input v-model="accessRuleForm.note" type="textarea" :rows="3" maxlength="256" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="accessRuleDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="accessRuleSaving" @click="submitAccessRule">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="riskRuleDialogVisible" :title="lt('编辑高频行为规则', '編輯高頻行為規則', 'Edit High-frequency Risk Rule')" width="620px">
      <el-form :model="riskRuleForm" label-width="120px">
        <el-form-item :label="lt('规则编码', '規則編碼', 'Rule Code')">
          <el-input v-model="riskRuleForm.ruleCode" :disabled="riskRuleDialogMode === 'edit'" maxlength="64" />
        </el-form-item>
        <el-form-item :label="lt('规则名称', '規則名稱', 'Rule Name')">
          <el-input v-model="riskRuleForm.ruleName" maxlength="128" />
        </el-form-item>
        <el-form-item :label="lt('是否启用', '是否啟用', 'Enabled')">
          <el-switch v-model="riskRuleForm.enabled" />
        </el-form-item>
        <el-form-item :label="lt('风险等级', '風險等級', 'Risk Level')">
          <el-select v-model="riskRuleForm.severity" style="width: 100%">
            <el-option label="LOW" value="LOW" />
            <el-option label="MEDIUM" value="MEDIUM" />
            <el-option label="HIGH" value="HIGH" />
            <el-option label="CRITICAL" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('阈值', '閾值', 'Threshold')">
          <el-input-number v-model="riskRuleForm.thresholdCount" :min="1" :max="999" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="lt('窗口分钟', '窗口分鐘', 'Window Minutes')">
          <el-input-number v-model="riskRuleForm.windowMinutes" :min="1" :max="1440" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="lt('动作类型', '動作類型', 'Action Type')">
          <el-select v-model="riskRuleForm.actionType" style="width: 100%">
            <el-option label="ALERT" value="ALERT" />
            <el-option label="LOCK" value="LOCK" />
            <el-option label="ESCALATE" value="ESCALATE" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('备注', '備註', 'Note')">
          <el-input v-model="riskRuleForm.note" type="textarea" :rows="3" maxlength="256" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="riskRuleDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="riskRuleSaving" @click="submitRiskRule">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createRiskIncident,
  getAuditLogs,
  getOpsAccessControlRules,
  getOpsLoginRiskEvents,
  getOpsRiskRules,
  getRiskIncidentRecords,
  getRiskIncidents,
  getVerificationCodeLogs,
  updateRiskIncidentStatus,
  upsertOpsAccessControlRule,
  upsertOpsRiskRule
} from '../../api'
import { useI18nLite } from '../../i18n'

const { lt } = useI18nLite()
const loading = ref(false)
const auditLogs = ref([])
const verificationLogs = ref([])
const riskIncidents = ref([])
const loginRiskEvents = ref([])
const accessRules = ref([])
const riskRules = ref([])
const incidentDialogVisible = ref(false)
const incidentSaving = ref(false)
const incidentHistoryVisible = ref(false)
const incidentHistoryLoading = ref(false)
const incidentHistoryRows = ref([])
const incidentHistoryTitle = ref('')
const auditDetailVisible = ref(false)
const auditDetailRow = ref(null)
const accessRuleDialogVisible = ref(false)
const accessRuleDialogMode = ref('create')
const accessRuleSaving = ref(false)
const riskRuleDialogVisible = ref(false)
const riskRuleDialogMode = ref('create')
const riskRuleSaving = ref(false)
const auditFilters = reactive({
  action: '',
  success: undefined,
  targetAppId: ''
})
const incidentForm = reactive({
  incidentType: 'CONTENT_RISK',
  severity: 'MEDIUM',
  title: '',
  description: '',
  targetAppId: '',
  ownerNote: '',
  assignee: '',
  handlingDeadline: ''
})
const accessRuleForm = reactive({
  ruleType: 'IP_BLOCK',
  targetValue: '',
  status: 'ACTIVE',
  riskLevel: 'HIGH',
  note: '',
  expiresAt: ''
})
const riskRuleForm = reactive({
  ruleCode: '',
  ruleName: '',
  enabled: true,
  severity: 'HIGH',
  thresholdCount: 1,
  windowMinutes: 15,
  actionType: 'ALERT',
  note: ''
})

const actionOptions = [
  'GAME_SUBMIT',
  'GAME_SUBMIT_VERSION',
  'GAME_APPROVE',
  'GAME_REJECT',
  'GAME_ROLLBACK',
  'GAME_METADATA_UPDATE'
]

const sensitiveActions = new Set(['GAME_APPROVE', 'GAME_REJECT', 'GAME_ROLLBACK'])

const auditStats = computed(() => ({
  total: auditLogs.value.length,
  success: auditLogs.value.filter((item) => item.success).length,
  failed: auditLogs.value.filter((item) => !item.success).length,
  sensitive: auditLogs.value.filter((item) => sensitiveActions.has(item.action)).length
}))

const loginRiskStats = computed(() => ({
  anomalyCount: loginRiskEvents.value.filter((item) => item.eventType !== 'LOGIN_SUCCESS').length,
  activeRuleCount: accessRules.value.filter((item) => item.status === 'ACTIVE').length,
  activeBehaviorRuleCount: riskRules.value.filter((item) => item.enabled).length
}))

const loadData = async () => {
  loading.value = true
  try {
    const [riskRes, auditRes, verificationRes, eventRes, ruleRes, riskRuleRes] = await Promise.all([
      getRiskIncidents(),
      getAuditLogs({
        limit: 80,
        action: auditFilters.action || undefined,
        success: auditFilters.success,
        targetAppId: auditFilters.targetAppId || undefined
      }),
      getVerificationCodeLogs({ limit: 50 }),
      getOpsLoginRiskEvents(),
      getOpsAccessControlRules(),
      getOpsRiskRules()
    ])
    riskIncidents.value = riskRes.data || []
    auditLogs.value = auditRes.data || []
    verificationLogs.value = verificationRes.data || []
    loginRiskEvents.value = eventRes.data || []
    accessRules.value = ruleRes.data || []
    riskRules.value = riskRuleRes.data || []
  } catch (error) {
    ElMessage.error(error.message || lt('加载风险与审计数据失败', '載入風險與審計資料失敗', 'Failed to load risk and audit data'))
  } finally {
    loading.value = false
  }
}

const openIncidentCreate = () => {
  incidentForm.incidentType = 'CONTENT_RISK'
  incidentForm.severity = 'MEDIUM'
  incidentForm.title = ''
  incidentForm.description = ''
  incidentForm.targetAppId = ''
  incidentForm.ownerNote = ''
  incidentForm.assignee = ''
  incidentForm.handlingDeadline = ''
  incidentDialogVisible.value = true
}

const submitIncident = async () => {
  if (incidentForm.title.trim().length < 2 || incidentForm.description.trim().length < 4) {
    ElMessage.warning(lt('请填写完整事件信息', '請填寫完整事件資訊', 'Please complete the incident fields'))
    return
  }
  try {
    incidentSaving.value = true
    await createRiskIncident({ ...incidentForm })
    incidentDialogVisible.value = false
    ElMessage.success(lt('风险事件已登记', '風險事件已登記', 'Risk incident created'))
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || lt('登记风险事件失败', '登記風險事件失敗', 'Failed to create risk incident'))
  } finally {
    incidentSaving.value = false
  }
}

const openIncidentStatus = async (row, nextStatus) => {
  try {
    const prompt = await ElMessageBox.prompt(
      lt('请填写处理备注，用于记录本次状态推进。', '請填寫處理備註，用於記錄本次狀態推進。', 'Please enter an owner note to record this status change.'),
      lt('更新风险事件', '更新風險事件', 'Update Risk Incident'),
      {
        confirmButtonText: lt('确认', '確認', 'Confirm'),
        cancelButtonText: lt('取消', '取消', 'Cancel'),
        inputPlaceholder: lt('例如：已通知相关负责人处理', '例如：已通知相關負責人處理', 'Example: owner notified and mitigation in progress')
      }
    )
    const resolutionSummary = nextStatus === 'RESOLVED' ? prompt.value || '' : ''
    await updateRiskIncidentStatus(row.id, {
      status: nextStatus,
      ownerNote: prompt.value || '',
      assignee: row.assignee || '',
      resolutionSummary
    })
    ElMessage.success(lt('风险事件状态已更新', '風險事件狀態已更新', 'Risk incident updated'))
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('更新风险事件失败', '更新風險事件失敗', 'Failed to update risk incident'))
    }
  }
}

const openIncidentHistory = async (row) => {
  incidentHistoryVisible.value = true
  incidentHistoryTitle.value = `${row.title} / ${row.targetAppId || '-'}`
  incidentHistoryLoading.value = true
  try {
    const res = await getRiskIncidentRecords(row.id)
    incidentHistoryRows.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || lt('加载风险事件记录失败', '載入風險事件記錄失敗', 'Failed to load incident records'))
  } finally {
    incidentHistoryLoading.value = false
  }
}

const openAuditDetail = (row) => {
  auditDetailRow.value = row
  auditDetailVisible.value = true
}

const openAccessRuleCreate = () => {
  accessRuleDialogMode.value = 'create'
  accessRuleForm.ruleType = 'IP_BLOCK'
  accessRuleForm.targetValue = ''
  accessRuleForm.status = 'ACTIVE'
  accessRuleForm.riskLevel = 'HIGH'
  accessRuleForm.note = ''
  accessRuleForm.expiresAt = ''
  accessRuleDialogVisible.value = true
}

const openAccessRuleEdit = (row) => {
  accessRuleDialogMode.value = 'edit'
  accessRuleForm.ruleType = row.ruleType
  accessRuleForm.targetValue = row.targetValue
  accessRuleForm.status = row.status
  accessRuleForm.riskLevel = row.riskLevel
  accessRuleForm.note = row.note || ''
  accessRuleForm.expiresAt = row.expiresAt || ''
  accessRuleDialogVisible.value = true
}

const submitAccessRule = async () => {
  if ((accessRuleForm.targetValue || '').trim().length < 2) {
    ElMessage.warning(lt('请填写有效的目标值', '請填寫有效的目標值', 'Please enter a valid target value'))
    return
  }
  try {
    accessRuleSaving.value = true
    await upsertOpsAccessControlRule({
      ...accessRuleForm,
      targetValue: accessRuleForm.targetValue.trim()
    })
    accessRuleDialogVisible.value = false
    ElMessage.success(lt('访问规则已保存', '訪問規則已保存', 'Access rule saved'))
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || lt('保存访问规则失败', '保存訪問規則失敗', 'Failed to save access rule'))
  } finally {
    accessRuleSaving.value = false
  }
}

const openRiskRuleEdit = (row) => {
  riskRuleDialogMode.value = row ? 'edit' : 'create'
  riskRuleForm.ruleCode = row?.ruleCode || ''
  riskRuleForm.ruleName = row?.ruleName || ''
  riskRuleForm.enabled = row?.enabled ?? true
  riskRuleForm.severity = row?.severity || 'HIGH'
  riskRuleForm.thresholdCount = row?.thresholdCount || 1
  riskRuleForm.windowMinutes = row?.windowMinutes || 15
  riskRuleForm.actionType = row?.actionType || 'ALERT'
  riskRuleForm.note = row?.note || ''
  riskRuleDialogVisible.value = true
}

const submitRiskRule = async () => {
  if ((riskRuleForm.ruleCode || '').trim().length < 4 || (riskRuleForm.ruleName || '').trim().length < 2) {
    ElMessage.warning(lt('请填写完整规则信息', '請填寫完整規則資訊', 'Please complete the risk rule fields'))
    return
  }
  try {
    riskRuleSaving.value = true
    await upsertOpsRiskRule({
      ...riskRuleForm,
      ruleCode: riskRuleForm.ruleCode.trim().toUpperCase(),
      ruleName: riskRuleForm.ruleName.trim()
    })
    riskRuleDialogVisible.value = false
    ElMessage.success(lt('高频行为规则已保存', '高頻行為規則已保存', 'High-frequency risk rule saved'))
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || lt('保存高频行为规则失败', '保存高頻行為規則失敗', 'Failed to save high-frequency risk rule'))
  } finally {
    riskRuleSaving.value = false
  }
}

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

function hasSnapshot(row) {
  return Boolean(
    (row?.beforeSnapshot && Object.keys(row.beforeSnapshot).length) ||
    (row?.afterSnapshot && Object.keys(row.afterSnapshot).length)
  )
}

function formatJson(value) {
  if (!value || !Object.keys(value).length) return '-'
  return JSON.stringify(value, null, 2)
}

function severityType(value) {
  if (value === 'CRITICAL') return 'danger'
  if (value === 'HIGH') return 'warning'
  if (value === 'MEDIUM') return 'info'
  return ''
}

function incidentStatusType(value) {
  if (value === 'OPEN') return 'danger'
  if (value === 'MITIGATING') return 'warning'
  if (value === 'RESOLVED') return 'success'
  return ''
}

onMounted(loadData)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 18px; }
.head-row { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.panel-title { font-size: 17px; font-weight: 800; color: #101828; }
.panel-subtitle { margin-top: 6px; color: #667085; font-size: 13px; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; }
.metric-row { display: flex; gap: 10px; flex-wrap: wrap; }
.metric-chip { padding: 8px 12px; border-radius: 999px; background: #f5f7fa; color: #344054; font-size: 13px; }
.metric-chip.danger { background: #fff1f3; color: #c01048; }
.metric-chip.warning { background: #fff7ed; color: #b54708; }
.table-tip { color: #98a2b3; font-size: 12px; }
.snapshot-pre { margin: 0; white-space: pre-wrap; word-break: break-word; font-size: 12px; line-height: 1.5; color: #344054; }
</style>
