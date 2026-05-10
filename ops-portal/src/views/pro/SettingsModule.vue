<template>
  <div class="pro-page">
    <div class="summary-grid">
      <div class="summary-item"><span>{{ lt('治理模板', '治理範本', 'Governance Templates') }}</span><strong>{{ templates.length }}</strong></div>
      <div class="summary-item"><span>{{ lt('通知模板', '通知範本', 'Notice Templates') }}</span><strong>{{ noticeTemplates.length }}</strong></div>
      <div class="summary-item"><span>{{ lt('字典条目', '字典條目', 'Dictionary Entries') }}</span><strong>{{ dictionaryEntries.length }}</strong></div>
      <div class="summary-item"><span>{{ lt('敏感策略', '敏感策略', 'Sensitive Policies') }}</span><strong>{{ sensitivePolicies.length }}</strong></div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :xl="10">
        <el-card class="panel-card">
          <template #header>{{ lt('管理员资料', '管理員資料', 'Admin Profile') }}</template>
          <el-form :model="profileForm" label-position="top">
            <el-form-item :label="lt('显示名称', '顯示名稱', 'Display Name')"><el-input v-model="profileForm.displayName" /></el-form-item>
            <el-form-item :label="lt('头像地址', '頭像網址', 'Avatar URL')"><el-input v-model="profileForm.avatarUrl" /></el-form-item>
            <el-form-item :label="lt('语言偏好', '語言偏好', 'Language')">
              <el-select v-model="profileForm.languageTag">
                <el-option label="简体中文" value="zh-CN" />
                <el-option label="繁體中文" value="zh-TW" />
                <el-option label="English" value="en" />
              </el-select>
            </el-form-item>
            <el-form-item :label="lt('邮箱', '電子郵件', 'Email')"><el-input :model-value="profileForm.email" disabled /></el-form-item>
            <el-form-item><el-button type="primary" :loading="saving" @click="saveProfile">{{ lt('保存设置', '儲存設定', 'Save Settings') }}</el-button></el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="14">
        <el-card class="panel-card">
          <template #header>
            <div class="card-header-inline">
              <span>{{ lt('治理规则模板', '治理規則範本', 'Governance Templates') }}</span>
              <el-button type="primary" link @click="openTemplateCreate">{{ lt('新建模板', '新增範本', 'New Template') }}</el-button>
            </div>
          </template>
          <div class="rule-list">
            <div class="rule-item" v-for="item in templates" :key="item.id">
              <div class="rule-head">
                <div>
                  <div class="rule-title">{{ item.title }}</div>
                  <div class="rule-type">{{ item.templateType }}</div>
                </div>
                <div class="rule-actions">
                  <el-button link @click="openConfigPreview('rule-template', item)">{{ lt('详情', '詳情', 'Detail') }}</el-button>
                  <el-button link type="primary" @click="openTemplateEdit(item)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
                  <el-button link type="danger" @click="removeTemplate(item)">{{ lt('删除', '刪除', 'Delete') }}</el-button>
                </div>
              </div>
              <div class="rule-desc">{{ item.content }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="panel-card">
      <template #header>{{ lt('角色权限矩阵', '角色權限矩陣', 'Role Permission Matrix') }}</template>
      <el-table :data="permissionRows" v-loading="permissionLoading" :empty-text="lt('暂无权限矩阵', '暫無權限矩陣', 'No permission matrix')">
        <el-table-column prop="role" :label="lt('角色', '角色', 'Role')" width="140" />
        <el-table-column :label="lt('权限集合', '權限集合', 'Permissions')" min-width="720">
          <template #default="{ row }">
            <div class="permission-tags">
              <el-tag v-for="permission in row.permissions" :key="permission" size="small" type="info">{{ permission }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="140">
          <template #default="{ row }">
            <el-button v-if="row.editable" type="primary" link @click="openPermissionDialog(row)">{{ lt('编辑权限', '編輯權限', 'Edit') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="panel-card">
      <template #header>{{ lt('管理员数据权限', '管理員資料權限', 'Admin Data Scopes') }}</template>
      <el-table :data="adminDataScopes" v-loading="adminDataScopeLoading" :empty-text="lt('暂无管理员数据权限', '暫無管理員資料權限', 'No admin data scopes')">
        <el-table-column prop="username" :label="lt('管理员账号', '管理員帳號', 'Admin')" min-width="180" />
        <el-table-column prop="email" :label="lt('邮箱', '電子郵件', 'Email')" min-width="220" />
        <el-table-column :label="lt('数据范围', '資料範圍', 'Data Scope')" min-width="220">
          <template #default="{ row }">
            <el-tag>{{ dataScopeText(row.scopeCode) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('操作', '操作', 'Actions')" width="160">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDataScopeDialog(row)">{{ lt('编辑范围', '編輯範圍', 'Edit Scope') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>
            <div class="card-header-inline">
              <span>{{ lt('通知模板', '通知範本', 'Notice Templates') }}</span>
              <el-button type="primary" link @click="openNoticeTemplateCreate">{{ lt('新建模板', '新增範本', 'New Template') }}</el-button>
            </div>
          </template>
          <div class="rule-list">
            <div class="rule-item" v-for="item in noticeTemplates" :key="item.id">
              <div class="rule-head">
                <div>
                  <div class="rule-title">{{ item.templateName }}</div>
                  <div class="rule-type">{{ item.templateCode }} / {{ item.channelType }} / {{ item.languageTag }}</div>
                </div>
                <div class="rule-actions">
                  <el-button link @click="openConfigPreview('notice-template', item)">{{ lt('详情', '詳情', 'Detail') }}</el-button>
                  <el-button link type="primary" @click="openNoticeTemplateEdit(item)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
                </div>
              </div>
              <div class="rule-desc">{{ item.titleTemplate }}</div>
              <div class="rule-desc">{{ item.bodyTemplate }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :xl="12">
        <el-card class="panel-card">
          <template #header>
            <div class="card-header-inline">
              <span>{{ lt('字典配置', '字典配置', 'Dictionary Entries') }}</span>
              <el-button type="primary" link @click="openDictionaryCreate">{{ lt('新增条目', '新增條目', 'New Entry') }}</el-button>
            </div>
          </template>
          <el-table :data="dictionaryEntries" :empty-text="lt('暂无字典条目', '暫無字典條目', 'No dictionary entries')">
            <el-table-column prop="dictType" :label="lt('字典类型', '字典類型', 'Type')" min-width="140" />
            <el-table-column prop="dictKey" :label="lt('键', '鍵', 'Key')" min-width="140" />
            <el-table-column prop="dictLabel" :label="lt('标签', '標籤', 'Label')" min-width="140" />
            <el-table-column prop="dictValue" :label="lt('值', '值', 'Value')" min-width="140" show-overflow-tooltip />
            <el-table-column prop="status" :label="lt('状态', '狀態', 'Status')" width="100" />
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="100">
              <template #default="{ row }">
                <el-button link @click="openConfigPreview('dictionary', row)">{{ lt('详情', '詳情', 'Detail') }}</el-button>
                <el-button type="primary" link @click="openDictionaryEdit(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :xl="8">
        <el-card class="panel-card">
          <template #header>
            <div class="card-header-inline">
              <span>{{ lt('菜单权限配置', '菜單權限配置', 'Menu Permission Profiles') }}</span>
              <el-button type="primary" link @click="openMenuPermissionCreate">{{ lt('新增配置', '新增配置', 'New Profile') }}</el-button>
            </div>
          </template>
          <el-table :data="menuPermissions" :empty-text="lt('暂无菜单权限配置', '暫無菜單權限配置', 'No menu permission profiles')">
            <el-table-column prop="roleCode" :label="lt('角色', '角色', 'Role')" width="110" />
            <el-table-column prop="menuCode" :label="lt('菜单键', '菜單鍵', 'Menu Code')" min-width="150" />
            <el-table-column prop="menuLabel" :label="lt('菜单名称', '菜單名稱', 'Menu Label')" min-width="140" />
            <el-table-column :label="lt('启用', '啟用', 'Enabled')" width="90">
              <template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? lt('是', '是', 'Yes') : lt('否', '否', 'No') }}</el-tag></template>
            </el-table-column>
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="90">
              <template #default="{ row }">
                <el-button link @click="openConfigPreview('menu-permission', row)">{{ lt('详情', '詳情', 'Detail') }}</el-button>
                <el-button type="primary" link @click="openMenuPermissionEdit(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :xl="8">
        <el-card class="panel-card">
          <template #header>
            <div class="card-header-inline">
              <span>{{ lt('审批模板配置', '審批範本配置', 'Approval Templates') }}</span>
              <el-button type="primary" link @click="openApprovalTemplateCreate">{{ lt('新增模板', '新增範本', 'New Template') }}</el-button>
            </div>
          </template>
          <div class="rule-list">
            <div class="rule-item" v-for="item in approvalTemplates" :key="item.id">
              <div class="rule-head">
                <div>
                  <div class="rule-title">{{ item.templateName }}</div>
                  <div class="rule-type">{{ item.templateCode }} / {{ item.bizType }} / {{ item.approvalMode }}</div>
                </div>
                <div class="rule-actions">
                  <el-button link @click="openConfigPreview('approval-template', item)">{{ lt('详情', '詳情', 'Detail') }}</el-button>
                  <el-button link type="primary" @click="openApprovalTemplateEdit(item)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
                </div>
              </div>
              <div class="rule-desc">{{ item.stepConfig || '-' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :xl="8">
        <el-card class="panel-card">
          <template #header>
            <div class="card-header-inline">
              <span>{{ lt('敏感策略中心', '敏感策略中心', 'Sensitive Policy Center') }}</span>
              <el-button type="primary" link @click="openSensitivePolicyCreate">{{ lt('新增策略', '新增策略', 'New Policy') }}</el-button>
            </div>
          </template>
          <div class="rule-list">
            <div class="rule-item" v-for="item in sensitivePolicies" :key="item.id">
              <div class="rule-head">
                <div>
                  <div class="rule-title">{{ item.policyName }}</div>
                  <div class="rule-type">{{ item.policyCode }} / {{ item.scopeType }} / {{ item.riskLevel }}</div>
                </div>
                <div class="rule-actions">
                  <el-button link @click="openConfigPreview('sensitive-policy', item)">{{ lt('详情', '詳情', 'Detail') }}</el-button>
                  <el-button link type="primary" @click="openSensitivePolicyEdit(item)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
                </div>
              </div>
              <div class="rule-desc">{{ item.targetActions || '-' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-drawer v-model="configPreviewVisible" :title="configPreviewTitle" :size="previewDrawerSize">
      <template v-if="configPreviewRecord">
        <el-descriptions :column="1" border>
          <el-descriptions-item v-for="item in configPreviewItems" :key="item.label" :label="item.label">{{ item.value }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>

    <el-dialog v-model="templateDialogVisible" :title="editingTemplateId ? lt('编辑治理模板', '編輯治理範本', 'Edit Governance Template') : lt('新建治理模板', '新增治理範本', 'Create Governance Template')" :width="dialogWidth('620px')">
      <el-form :model="templateForm" :label-width="compactLabelWidth">
        <el-form-item :label="lt('模板类型', '範本類型', 'Template Type')">
          <el-select v-model="templateForm.templateType" style="width: 100%">
            <el-option label="APPROVAL" value="APPROVAL" />
            <el-option label="REJECTION" value="REJECTION" />
            <el-option label="PRELAUNCH" value="PRELAUNCH" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('标题', '標題', 'Title')">
          <el-input v-model="templateForm.title" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item :label="lt('内容', '內容', 'Content')">
          <el-input v-model="templateForm.content" type="textarea" :rows="5" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item :label="lt('排序', '排序', 'Sort Order')">
          <el-input-number v-model="templateForm.sortOrder" :min="0" :max="999" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="templateSaving" @click="submitTemplate">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permissionDialogVisible" :title="lt('编辑角色权限', '編輯角色權限', 'Edit Role Permissions')" :width="dialogWidth('760px', '92%')">
      <div class="permission-role">{{ editingRole }}</div>
      <el-checkbox-group v-model="permissionForm.permissions" class="permission-grid">
        <el-checkbox v-for="permission in availablePermissions" :key="permission" :value="permission">{{ permission }}</el-checkbox>
      </el-checkbox-group>
      <div class="permission-help">{{ lt('管理员角色会自动保留后台安全基线权限，避免误操作导致后台失控。', '管理員角色會自動保留後台安全基線權限，避免誤操作導致後台失控。', 'Admin roles always keep the backend safety-baseline permissions to prevent lockout.') }}</div>
      <template #footer>
        <el-button @click="permissionDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="permissionSaving" @click="submitPermissions">{{ lt('保存权限', '儲存權限', 'Save Permissions') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dataScopeDialogVisible" :title="lt('编辑管理员数据范围', '編輯管理員資料範圍', 'Edit Admin Data Scope')" :width="dialogWidth('520px')">
      <div class="permission-role">{{ editingAdminLabel }}</div>
      <el-select v-model="dataScopeForm.scopeCode" style="width: 100%">
        <el-option value="ALL_DEVELOPERS" :label="lt('全部开发者', '全部開發者', 'All Developers')" />
        <el-option value="HIGH_RISK_ONLY" :label="lt('仅高风险/黑名单', '僅高風險/黑名單', 'High Risk / Blacklist Only')" />
        <el-option value="PENDING_CERT_ONLY" :label="lt('仅资质审核中', '僅資質審核中', 'Pending Certification Only')" />
        <el-option value="BLOCKED_ONLY" :label="lt('仅封禁/暂停账号', '僅封禁/暫停帳號', 'Blocked Accounts Only')" />
      </el-select>
      <div class="permission-help">{{ lt('该设置由后端强制执行，开发者列表会按该管理员的数据范围过滤。', '該設定由後端強制執行，開發者列表會依該管理員的資料範圍過濾。', 'This setting is enforced by the backend and filters developer lists by the assigned admin scope.') }}</div>
      <template #footer>
        <el-button @click="dataScopeDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="dataScopeSaving" @click="submitDataScope">{{ lt('保存范围', '儲存範圍', 'Save Scope') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="noticeTemplateDialogVisible" :title="editingNoticeTemplateId ? lt('编辑通知模板', '編輯通知範本', 'Edit Notice Template') : lt('新建通知模板', '新增通知範本', 'Create Notice Template')" :width="dialogWidth('680px')">
      <el-form :model="noticeTemplateForm" :label-width="formLabelWidth">
        <el-form-item :label="lt('模板编码', '範本編碼', 'Template Code')"><el-input v-model="noticeTemplateForm.templateCode" :disabled="Boolean(editingNoticeTemplateId)" /></el-form-item>
        <el-form-item :label="lt('模板名称', '範本名稱', 'Template Name')"><el-input v-model="noticeTemplateForm.templateName" /></el-form-item>
        <el-form-item :label="lt('渠道类型', '渠道類型', 'Channel Type')">
          <el-select v-model="noticeTemplateForm.channelType" style="width: 100%">
            <el-option label="INBOX" value="INBOX" />
            <el-option label="EMAIL" value="EMAIL" />
            <el-option label="SMS" value="SMS" />
            <el-option label="PUSH" value="PUSH" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('语言', '語言', 'Language')"><el-input v-model="noticeTemplateForm.languageTag" /></el-form-item>
        <el-form-item :label="lt('标题模板', '標題範本', 'Title Template')"><el-input v-model="noticeTemplateForm.titleTemplate" maxlength="256" show-word-limit /></el-form-item>
        <el-form-item :label="lt('正文模板', '正文範本', 'Body Template')"><el-input v-model="noticeTemplateForm.bodyTemplate" type="textarea" :rows="5" maxlength="2000" show-word-limit /></el-form-item>
        <el-form-item :label="lt('状态', '狀態', 'Status')">
          <el-select v-model="noticeTemplateForm.status" style="width: 100%">
            <el-option label="ENABLED" value="ENABLED" />
            <el-option label="DISABLED" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeTemplateDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="noticeTemplateSaving" @click="submitNoticeTemplate">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dictionaryDialogVisible" :title="editingDictionaryId ? lt('编辑字典条目', '編輯字典條目', 'Edit Dictionary Entry') : lt('新建字典条目', '新增字典條目', 'Create Dictionary Entry')" :width="dialogWidth('620px')">
      <el-form :model="dictionaryForm" :label-width="formLabelWidth">
        <el-form-item :label="lt('字典类型', '字典類型', 'Type')"><el-input v-model="dictionaryForm.dictType" :disabled="Boolean(editingDictionaryId)" /></el-form-item>
        <el-form-item :label="lt('键', '鍵', 'Key')"><el-input v-model="dictionaryForm.dictKey" :disabled="Boolean(editingDictionaryId)" /></el-form-item>
        <el-form-item :label="lt('标签', '標籤', 'Label')"><el-input v-model="dictionaryForm.dictLabel" /></el-form-item>
        <el-form-item :label="lt('值', '值', 'Value')"><el-input v-model="dictionaryForm.dictValue" /></el-form-item>
        <el-form-item :label="lt('排序', '排序', 'Sort Order')"><el-input-number v-model="dictionaryForm.sortOrder" :min="0" :max="999" style="width: 100%" /></el-form-item>
        <el-form-item :label="lt('状态', '狀態', 'Status')">
          <el-select v-model="dictionaryForm.status" style="width: 100%">
            <el-option label="ENABLED" value="ENABLED" />
            <el-option label="DISABLED" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dictionaryDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="dictionarySaving" @click="submitDictionary">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="menuPermissionDialogVisible" :title="editingMenuPermissionId ? lt('编辑菜单权限配置', '編輯菜單權限配置', 'Edit Menu Permission Profile') : lt('新建菜单权限配置', '新增菜單權限配置', 'Create Menu Permission Profile')" :width="dialogWidth('620px')">
      <el-form :model="menuPermissionForm" :label-width="formLabelWidth">
        <el-form-item :label="lt('角色', '角色', 'Role')"><el-input v-model="menuPermissionForm.roleCode" :disabled="Boolean(editingMenuPermissionId)" /></el-form-item>
        <el-form-item :label="lt('菜单键', '菜單鍵', 'Menu Code')"><el-input v-model="menuPermissionForm.menuCode" :disabled="Boolean(editingMenuPermissionId)" /></el-form-item>
        <el-form-item :label="lt('菜单名称', '菜單名稱', 'Menu Label')"><el-input v-model="menuPermissionForm.menuLabel" /></el-form-item>
        <el-form-item :label="lt('排序', '排序', 'Sort Order')"><el-input-number v-model="menuPermissionForm.sortOrder" :min="0" :max="999" style="width: 100%" /></el-form-item>
        <el-form-item :label="lt('启用', '啟用', 'Enabled')"><el-switch v-model="menuPermissionForm.enabled" /></el-form-item>
        <el-form-item :label="lt('备注', '備註', 'Note')"><el-input v-model="menuPermissionForm.note" type="textarea" :rows="3" maxlength="256" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="menuPermissionDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="menuPermissionSaving" @click="submitMenuPermission">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="approvalTemplateDialogVisible" :title="editingApprovalTemplateId ? lt('编辑审批模板', '編輯審批範本', 'Edit Approval Template') : lt('新建审批模板', '新增審批範本', 'Create Approval Template')" :width="dialogWidth('680px')">
      <el-form :model="approvalTemplateForm" :label-width="wideLabelWidth">
        <el-form-item :label="lt('模板编码', '範本編碼', 'Template Code')"><el-input v-model="approvalTemplateForm.templateCode" :disabled="Boolean(editingApprovalTemplateId)" /></el-form-item>
        <el-form-item :label="lt('模板名称', '範本名稱', 'Template Name')"><el-input v-model="approvalTemplateForm.templateName" /></el-form-item>
        <el-form-item :label="lt('业务类型', '業務類型', 'Biz Type')"><el-input v-model="approvalTemplateForm.bizType" /></el-form-item>
        <el-form-item :label="lt('审批模式', '審批模式', 'Approval Mode')"><el-input v-model="approvalTemplateForm.approvalMode" /></el-form-item>
        <el-form-item :label="lt('审核角色', '審核角色', 'Reviewer Role')"><el-input v-model="approvalTemplateForm.reviewerRole" /></el-form-item>
        <el-form-item :label="lt('流程配置', '流程配置', 'Step Config')"><el-input v-model="approvalTemplateForm.stepConfig" type="textarea" :rows="4" maxlength="2000" show-word-limit /></el-form-item>
        <el-form-item :label="lt('状态', '狀態', 'Status')"><el-select v-model="approvalTemplateForm.status" style="width: 100%"><el-option label="ENABLED" value="ENABLED" /><el-option label="DISABLED" value="DISABLED" /></el-select></el-form-item>
        <el-form-item :label="lt('备注', '備註', 'Note')"><el-input v-model="approvalTemplateForm.note" type="textarea" :rows="3" maxlength="256" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approvalTemplateDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="approvalTemplateSaving" @click="submitApprovalTemplate">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="sensitivePolicyDialogVisible" :title="editingSensitivePolicyId ? lt('编辑敏感策略', '編輯敏感策略', 'Edit Sensitive Policy') : lt('新建敏感策略', '新增敏感策略', 'Create Sensitive Policy')" :width="dialogWidth('680px')">
      <el-form :model="sensitivePolicyForm" :label-width="wideLabelWidth">
        <el-form-item :label="lt('策略编码', '策略編碼', 'Policy Code')"><el-input v-model="sensitivePolicyForm.policyCode" :disabled="Boolean(editingSensitivePolicyId)" /></el-form-item>
        <el-form-item :label="lt('策略名称', '策略名稱', 'Policy Name')"><el-input v-model="sensitivePolicyForm.policyName" /></el-form-item>
        <el-form-item :label="lt('风险等级', '風險等級', 'Risk Level')"><el-select v-model="sensitivePolicyForm.riskLevel" style="width: 100%"><el-option label="LOW" value="LOW" /><el-option label="MEDIUM" value="MEDIUM" /><el-option label="HIGH" value="HIGH" /><el-option label="CRITICAL" value="CRITICAL" /></el-select></el-form-item>
        <el-form-item :label="lt('作用范围', '作用範圍', 'Scope Type')"><el-select v-model="sensitivePolicyForm.scopeType" style="width: 100%"><el-option label="ACTION" value="ACTION" /><el-option label="MODULE" value="MODULE" /><el-option label="RESOURCE" value="RESOURCE" /></el-select></el-form-item>
        <el-form-item :label="lt('目标动作', '目標動作', 'Target Actions')"><el-input v-model="sensitivePolicyForm.targetActions" type="textarea" :rows="4" maxlength="4000" show-word-limit /></el-form-item>
        <el-form-item :label="lt('状态', '狀態', 'Status')"><el-select v-model="sensitivePolicyForm.status" style="width: 100%"><el-option label="ENABLED" value="ENABLED" /><el-option label="DISABLED" value="DISABLED" /></el-select></el-form-item>
        <el-form-item :label="lt('二次确认', '二次確認', 'Confirm Required')"><el-switch v-model="sensitivePolicyForm.confirmRequired" /></el-form-item>
        <el-form-item :label="lt('审计必留', '審計必留', 'Audit Required')"><el-switch v-model="sensitivePolicyForm.auditRequired" /></el-form-item>
        <el-form-item :label="lt('备注', '備註', 'Note')"><el-input v-model="sensitivePolicyForm.note" type="textarea" :rows="3" maxlength="256" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sensitivePolicyDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="sensitivePolicySaving" @click="submitSensitivePolicy">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createOpsRuleTemplate,
  getOpsApprovalTemplates,
  getOpsDictionaryEntries,
  getOpsMenuPermissions,
  getOpsNoticeTemplates,
  getOpsSensitivePolicies,
  deleteOpsRuleTemplate,
  getOpsAdminDataScopes,
  getOpsPermissionMatrix,
  getOpsRuleTemplates,
  getProfile,
  upsertOpsApprovalTemplate,
  upsertOpsDictionaryEntry,
  upsertOpsMenuPermission,
  upsertOpsNoticeTemplate,
  upsertOpsSensitivePolicy,
  updateOpsAdminDataScope,
  updateOpsRolePermissions,
  updateOpsRuleTemplate,
  updateProfile
} from '../../api'
import { useI18nLite } from '../../i18n'
import { useViewport } from '../../composables/useViewport'

const { lt } = useI18nLite()
const { isTabletOrBelow, isPhone } = useViewport()
const saving = ref(false)
const permissionLoading = ref(false)
const permissionSaving = ref(false)
const adminDataScopeLoading = ref(false)
const dataScopeSaving = ref(false)
const permissionRows = ref([])
const availablePermissions = ref([])
const adminDataScopes = ref([])
const templates = ref([])
const noticeTemplates = ref([])
const dictionaryEntries = ref([])
const menuPermissions = ref([])
const approvalTemplates = ref([])
const sensitivePolicies = ref([])
const templateDialogVisible = ref(false)
const templateSaving = ref(false)
const noticeTemplateDialogVisible = ref(false)
const noticeTemplateSaving = ref(false)
const dictionaryDialogVisible = ref(false)
const dictionarySaving = ref(false)
const menuPermissionDialogVisible = ref(false)
const menuPermissionSaving = ref(false)
const approvalTemplateDialogVisible = ref(false)
const approvalTemplateSaving = ref(false)
const sensitivePolicyDialogVisible = ref(false)
const sensitivePolicySaving = ref(false)
const permissionDialogVisible = ref(false)
const dataScopeDialogVisible = ref(false)
const configPreviewVisible = ref(false)
const editingTemplateId = ref(null)
const editingNoticeTemplateId = ref(null)
const editingDictionaryId = ref(null)
const editingMenuPermissionId = ref(null)
const editingApprovalTemplateId = ref(null)
const editingSensitivePolicyId = ref(null)
const editingRole = ref('')
const editingAdminId = ref(null)
const editingAdminLabel = ref('')
const configPreviewType = ref('')
const compactLabelWidth = computed(() => (isPhone.value ? '92px' : '110px'))
const formLabelWidth = computed(() => (isPhone.value ? '96px' : '120px'))
const wideLabelWidth = computed(() => (isPhone.value ? '108px' : '130px'))
const previewDrawerSize = computed(() => (isPhone.value ? '100%' : isTabletOrBelow.value ? '72%' : '36%'))

const dialogWidth = (desktop, tablet = '88%', mobile = '94%') => {
  if (isPhone.value) return mobile
  if (isTabletOrBelow.value) return tablet
  return desktop
}
const configPreviewRecord = ref(null)
const profileForm = reactive({
  displayName: '',
  avatarUrl: '',
  languageTag: 'zh-CN',
  email: ''
})
const templateForm = reactive({
  templateType: 'APPROVAL',
  title: '',
  content: '',
  sortOrder: 0
})
const noticeTemplateForm = reactive({
  templateCode: '',
  templateName: '',
  channelType: 'INBOX',
  languageTag: 'zh-CN',
  titleTemplate: '',
  bodyTemplate: '',
  status: 'ENABLED'
})
const dictionaryForm = reactive({
  dictType: '',
  dictKey: '',
  dictLabel: '',
  dictValue: '',
  sortOrder: 0,
  status: 'ENABLED'
})
const menuPermissionForm = reactive({
  roleCode: '',
  menuCode: '',
  menuLabel: '',
  enabled: true,
  sortOrder: 0,
  note: ''
})
const approvalTemplateForm = reactive({
  templateCode: '',
  templateName: '',
  bizType: 'GAME_REVIEW',
  approvalMode: 'SINGLE_REVIEWER',
  reviewerRole: 'REVIEWER',
  stepConfig: '',
  status: 'ENABLED',
  note: ''
})
const sensitivePolicyForm = reactive({
  policyCode: '',
  policyName: '',
  riskLevel: 'HIGH',
  confirmRequired: true,
  auditRequired: true,
  scopeType: 'ACTION',
  targetActions: '',
  status: 'ENABLED',
  note: ''
})
const permissionForm = reactive({
  permissions: []
})
const dataScopeForm = reactive({
  scopeCode: 'HIGH_RISK_ONLY'
})

const configPreviewTitle = computed(() => ({
  'rule-template': lt('治理模板详情', '治理範本詳情', 'Governance Template Detail'),
  'notice-template': lt('通知模板详情', '通知範本詳情', 'Notice Template Detail'),
  dictionary: lt('字典条目详情', '字典條目詳情', 'Dictionary Entry Detail'),
  'menu-permission': lt('菜单权限详情', '菜單權限詳情', 'Menu Permission Detail'),
  'approval-template': lt('审批模板详情', '審批範本詳情', 'Approval Template Detail'),
  'sensitive-policy': lt('敏感策略详情', '敏感策略詳情', 'Sensitive Policy Detail')
}[configPreviewType.value] || lt('配置详情', '配置詳情', 'Configuration Detail')))

const configPreviewItems = computed(() => {
  const row = configPreviewRecord.value || {}
  const map = {
    'rule-template': [
      ['Type', row.templateType], ['Title', row.title], ['Sort Order', row.sortOrder], ['Content', row.content]
    ],
    'notice-template': [
      ['Template Code', row.templateCode], ['Template Name', row.templateName], ['Channel', row.channelType],
      ['Language', row.languageTag], ['Status', row.status], ['Title Template', row.titleTemplate], ['Body Template', row.bodyTemplate]
    ],
    dictionary: [
      ['Type', row.dictType], ['Key', row.dictKey], ['Label', row.dictLabel], ['Value', row.dictValue], ['Sort Order', row.sortOrder], ['Status', row.status]
    ],
    'menu-permission': [
      ['Role', row.roleCode], ['Menu Code', row.menuCode], ['Menu Label', row.menuLabel], ['Enabled', row.enabled ? 'YES' : 'NO'], ['Sort Order', row.sortOrder], ['Note', row.note]
    ],
    'approval-template': [
      ['Template Code', row.templateCode], ['Template Name', row.templateName], ['Biz Type', row.bizType],
      ['Approval Mode', row.approvalMode], ['Reviewer Role', row.reviewerRole], ['Status', row.status], ['Step Config', row.stepConfig], ['Note', row.note]
    ],
    'sensitive-policy': [
      ['Policy Code', row.policyCode], ['Policy Name', row.policyName], ['Scope Type', row.scopeType], ['Risk Level', row.riskLevel],
      ['Confirm Required', row.confirmRequired ? 'YES' : 'NO'], ['Audit Required', row.auditRequired ? 'YES' : 'NO'], ['Target Actions', row.targetActions], ['Status', row.status], ['Note', row.note]
    ]
  }
  return (map[configPreviewType.value] || []).map(([label, value]) => ({ label, value: value || value === 0 ? value : '-' }))
})

const openConfigPreview = (type, row) => {
  configPreviewType.value = type
  configPreviewRecord.value = row
  configPreviewVisible.value = true
}

const loadProfile = async () => {
  try {
    const res = await getProfile()
    Object.assign(profileForm, {
      displayName: res.data?.displayName || '',
      avatarUrl: res.data?.avatarUrl || '',
      languageTag: res.data?.languageTag || 'zh-CN',
      email: res.data?.email || ''
    })
  } catch (error) {
    ElMessage.error(error.message || lt('加载管理员资料失败', '載入管理員資料失敗', 'Failed to load admin profile'))
  }
}

const saveProfile = async () => {
  try {
    saving.value = true
    await updateProfile(profileForm)
    ElMessage.success(lt('设置已保存', '設定已儲存', 'Settings saved'))
  } catch (error) {
    ElMessage.error(error.message || lt('保存设置失败', '儲存設定失敗', 'Failed to save settings'))
  } finally {
    saving.value = false
  }
}

const loadPermissionMatrix = async () => {
  try {
    permissionLoading.value = true
    const res = await getOpsPermissionMatrix()
    permissionRows.value = res.data?.roles || []
    availablePermissions.value = res.data?.availablePermissions || []
  } catch (error) {
    ElMessage.error(error.message || lt('加载权限矩阵失败', '載入權限矩陣失敗', 'Failed to load permission matrix'))
  } finally {
    permissionLoading.value = false
  }
}

const loadAdminDataScopes = async () => {
  try {
    adminDataScopeLoading.value = true
    const res = await getOpsAdminDataScopes()
    adminDataScopes.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || lt('加载管理员数据权限失败', '載入管理員資料權限失敗', 'Failed to load admin data scopes'))
  } finally {
    adminDataScopeLoading.value = false
  }
}

const loadTemplates = async () => {
  try {
    const res = await getOpsRuleTemplates()
    templates.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || lt('加载治理模板失败', '載入治理範本失敗', 'Failed to load governance templates'))
  }
}

const loadNoticeTemplates = async () => {
  try {
    const res = await getOpsNoticeTemplates()
    noticeTemplates.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || lt('加载通知模板失败', '載入通知範本失敗', 'Failed to load notice templates'))
  }
}

const loadDictionaryEntries = async () => {
  try {
    const res = await getOpsDictionaryEntries()
    dictionaryEntries.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || lt('加载字典配置失败', '載入字典配置失敗', 'Failed to load dictionary entries'))
  }
}

const loadMenuPermissions = async () => {
  try {
    const res = await getOpsMenuPermissions()
    menuPermissions.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || lt('加载菜单权限配置失败', '載入菜單權限配置失敗', 'Failed to load menu permission profiles'))
  }
}

const loadApprovalTemplates = async () => {
  try {
    const res = await getOpsApprovalTemplates()
    approvalTemplates.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || lt('加载审批模板失败', '載入審批範本失敗', 'Failed to load approval templates'))
  }
}

const loadSensitivePolicies = async () => {
  try {
    const res = await getOpsSensitivePolicies()
    sensitivePolicies.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || lt('加载敏感策略失败', '載入敏感策略失敗', 'Failed to load sensitive policies'))
  }
}

const resetTemplateForm = () => {
  editingTemplateId.value = null
  templateForm.templateType = 'APPROVAL'
  templateForm.title = ''
  templateForm.content = ''
  templateForm.sortOrder = 0
}

const openTemplateCreate = () => {
  resetTemplateForm()
  templateDialogVisible.value = true
}

const openTemplateEdit = (item) => {
  editingTemplateId.value = item.id
  templateForm.templateType = item.templateType
  templateForm.title = item.title
  templateForm.content = item.content
  templateForm.sortOrder = item.sortOrder ?? 0
  templateDialogVisible.value = true
}

const submitTemplate = async () => {
  if (templateForm.title.trim().length < 2 || templateForm.content.trim().length < 4) {
    ElMessage.warning(lt('请填写完整模板信息', '請填寫完整範本資訊', 'Please complete the template fields'))
    return
  }
  try {
    templateSaving.value = true
    if (editingTemplateId.value) {
      await updateOpsRuleTemplate(editingTemplateId.value, { ...templateForm })
    } else {
      await createOpsRuleTemplate({ ...templateForm })
    }
    templateDialogVisible.value = false
    ElMessage.success(lt('治理模板已保存', '治理範本已儲存', 'Governance template saved'))
    await loadTemplates()
  } catch (error) {
    ElMessage.error(error.message || lt('保存治理模板失败', '儲存治理範本失敗', 'Failed to save governance template'))
  } finally {
    templateSaving.value = false
  }
}

const removeTemplate = async (item) => {
  try {
    await ElMessageBox.confirm(
      lt('确认删除该治理模板？删除后将不能继续用于运营审核与检查。', '確認刪除該治理範本？刪除後將不能繼續用於營運審核與檢查。', 'Delete this governance template? It will no longer be available for operations review and checklists.'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      {
        confirmButtonText: lt('确认删除', '確認刪除', 'Delete'),
        cancelButtonText: lt('取消', '取消', 'Cancel'),
        type: 'warning'
      }
    )
    await deleteOpsRuleTemplate(item.id)
    ElMessage.success(lt('治理模板已删除', '治理範本已刪除', 'Governance template deleted'))
    await loadTemplates()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || lt('删除治理模板失败', '刪除治理範本失敗', 'Failed to delete governance template'))
    }
  }
}

const openPermissionDialog = (row) => {
  editingRole.value = row.role
  permissionForm.permissions = [...(row.permissions || [])]
  permissionDialogVisible.value = true
}

const resetNoticeTemplateForm = () => {
  editingNoticeTemplateId.value = null
  Object.assign(noticeTemplateForm, {
    templateCode: '',
    templateName: '',
    channelType: 'INBOX',
    languageTag: 'zh-CN',
    titleTemplate: '',
    bodyTemplate: '',
    status: 'ENABLED'
  })
}

const openNoticeTemplateCreate = () => {
  resetNoticeTemplateForm()
  noticeTemplateDialogVisible.value = true
}

const openNoticeTemplateEdit = (item) => {
  editingNoticeTemplateId.value = item.id
  Object.assign(noticeTemplateForm, item)
  noticeTemplateDialogVisible.value = true
}

const submitNoticeTemplate = async () => {
  if (noticeTemplateForm.templateCode.trim().length < 2 || noticeTemplateForm.templateName.trim().length < 2 || noticeTemplateForm.titleTemplate.trim().length < 2 || noticeTemplateForm.bodyTemplate.trim().length < 4) {
    ElMessage.warning(lt('请填写完整通知模板信息', '請填寫完整通知範本資訊', 'Please complete the notice template'))
    return
  }
  try {
    noticeTemplateSaving.value = true
    await upsertOpsNoticeTemplate(editingNoticeTemplateId.value, { ...noticeTemplateForm })
    noticeTemplateDialogVisible.value = false
    ElMessage.success(lt('通知模板已保存', '通知範本已儲存', 'Notice template saved'))
    await loadNoticeTemplates()
  } catch (error) {
    ElMessage.error(error.message || lt('保存通知模板失败', '儲存通知範本失敗', 'Failed to save notice template'))
  } finally {
    noticeTemplateSaving.value = false
  }
}

const resetDictionaryForm = () => {
  editingDictionaryId.value = null
  Object.assign(dictionaryForm, {
    dictType: '',
    dictKey: '',
    dictLabel: '',
    dictValue: '',
    sortOrder: 0,
    status: 'ENABLED'
  })
}

const resetMenuPermissionForm = () => {
  editingMenuPermissionId.value = null
  Object.assign(menuPermissionForm, {
    roleCode: '',
    menuCode: '',
    menuLabel: '',
    enabled: true,
    sortOrder: 0,
    note: ''
  })
}

const resetApprovalTemplateForm = () => {
  editingApprovalTemplateId.value = null
  Object.assign(approvalTemplateForm, {
    templateCode: '',
    templateName: '',
    bizType: 'GAME_REVIEW',
    approvalMode: 'SINGLE_REVIEWER',
    reviewerRole: 'REVIEWER',
    stepConfig: '',
    status: 'ENABLED',
    note: ''
  })
}

const resetSensitivePolicyForm = () => {
  editingSensitivePolicyId.value = null
  Object.assign(sensitivePolicyForm, {
    policyCode: '',
    policyName: '',
    riskLevel: 'HIGH',
    confirmRequired: true,
    auditRequired: true,
    scopeType: 'ACTION',
    targetActions: '',
    status: 'ENABLED',
    note: ''
  })
}

const openDictionaryCreate = () => {
  resetDictionaryForm()
  dictionaryDialogVisible.value = true
}

const openDictionaryEdit = (item) => {
  editingDictionaryId.value = item.id
  Object.assign(dictionaryForm, item)
  dictionaryDialogVisible.value = true
}

const openMenuPermissionCreate = () => {
  resetMenuPermissionForm()
  menuPermissionDialogVisible.value = true
}

const openMenuPermissionEdit = (item) => {
  editingMenuPermissionId.value = item.id
  Object.assign(menuPermissionForm, item)
  menuPermissionDialogVisible.value = true
}

const openApprovalTemplateCreate = () => {
  resetApprovalTemplateForm()
  approvalTemplateDialogVisible.value = true
}

const openApprovalTemplateEdit = (item) => {
  editingApprovalTemplateId.value = item.id
  Object.assign(approvalTemplateForm, item)
  approvalTemplateDialogVisible.value = true
}

const openSensitivePolicyCreate = () => {
  resetSensitivePolicyForm()
  sensitivePolicyDialogVisible.value = true
}

const openSensitivePolicyEdit = (item) => {
  editingSensitivePolicyId.value = item.id
  Object.assign(sensitivePolicyForm, item)
  sensitivePolicyDialogVisible.value = true
}

const submitDictionary = async () => {
  if (dictionaryForm.dictType.trim().length < 2 || dictionaryForm.dictKey.trim().length < 1 || dictionaryForm.dictLabel.trim().length < 2) {
    ElMessage.warning(lt('请填写完整字典条目信息', '請填寫完整字典條目資訊', 'Please complete the dictionary entry'))
    return
  }
  try {
    dictionarySaving.value = true
    await upsertOpsDictionaryEntry(editingDictionaryId.value, { ...dictionaryForm })
    dictionaryDialogVisible.value = false
    ElMessage.success(lt('字典条目已保存', '字典條目已儲存', 'Dictionary entry saved'))
    await loadDictionaryEntries()
  } catch (error) {
    ElMessage.error(error.message || lt('保存字典条目失败', '儲存字典條目失敗', 'Failed to save dictionary entry'))
  } finally {
    dictionarySaving.value = false
  }
}

const submitMenuPermission = async () => {
  if (menuPermissionForm.roleCode.trim().length < 2 || menuPermissionForm.menuCode.trim().length < 2 || menuPermissionForm.menuLabel.trim().length < 2) {
    ElMessage.warning(lt('请填写完整菜单权限信息', '請填寫完整菜單權限資訊', 'Please complete the menu permission profile'))
    return
  }
  try {
    menuPermissionSaving.value = true
    await upsertOpsMenuPermission(editingMenuPermissionId.value, { ...menuPermissionForm })
    menuPermissionDialogVisible.value = false
    ElMessage.success(lt('菜单权限配置已保存', '菜單權限配置已保存', 'Menu permission profile saved'))
    await loadMenuPermissions()
  } catch (error) {
    ElMessage.error(error.message || lt('保存菜单权限配置失败', '保存菜單權限配置失敗', 'Failed to save menu permission profile'))
  } finally {
    menuPermissionSaving.value = false
  }
}

const submitApprovalTemplate = async () => {
  if (approvalTemplateForm.templateCode.trim().length < 2 || approvalTemplateForm.templateName.trim().length < 2 || approvalTemplateForm.bizType.trim().length < 2) {
    ElMessage.warning(lt('请填写完整审批模板信息', '請填寫完整審批範本資訊', 'Please complete the approval template'))
    return
  }
  try {
    approvalTemplateSaving.value = true
    await upsertOpsApprovalTemplate(editingApprovalTemplateId.value, { ...approvalTemplateForm })
    approvalTemplateDialogVisible.value = false
    ElMessage.success(lt('审批模板已保存', '審批範本已保存', 'Approval template saved'))
    await loadApprovalTemplates()
  } catch (error) {
    ElMessage.error(error.message || lt('保存审批模板失败', '保存審批範本失敗', 'Failed to save approval template'))
  } finally {
    approvalTemplateSaving.value = false
  }
}

const submitSensitivePolicy = async () => {
  if (sensitivePolicyForm.policyCode.trim().length < 2 || sensitivePolicyForm.policyName.trim().length < 2 || sensitivePolicyForm.targetActions.trim().length < 2) {
    ElMessage.warning(lt('请填写完整敏感策略信息', '請填寫完整敏感策略資訊', 'Please complete the sensitive policy'))
    return
  }
  try {
    sensitivePolicySaving.value = true
    await upsertOpsSensitivePolicy(editingSensitivePolicyId.value, { ...sensitivePolicyForm })
    sensitivePolicyDialogVisible.value = false
    ElMessage.success(lt('敏感策略已保存', '敏感策略已保存', 'Sensitive policy saved'))
    await loadSensitivePolicies()
  } catch (error) {
    ElMessage.error(error.message || lt('保存敏感策略失败', '保存敏感策略失敗', 'Failed to save sensitive policy'))
  } finally {
    sensitivePolicySaving.value = false
  }
}

const openDataScopeDialog = (row) => {
  editingAdminId.value = row.adminUserId
  editingAdminLabel.value = `${row.username} / ${row.email || '-'}`
  dataScopeForm.scopeCode = row.scopeCode || 'HIGH_RISK_ONLY'
  dataScopeDialogVisible.value = true
}

const submitPermissions = async () => {
  try {
    permissionSaving.value = true
    await updateOpsRolePermissions(editingRole.value, { permissions: permissionForm.permissions })
    permissionDialogVisible.value = false
    ElMessage.success(lt('角色权限已更新', '角色權限已更新', 'Role permissions updated'))
    await loadPermissionMatrix()
  } catch (error) {
    ElMessage.error(error.message || lt('保存角色权限失败', '儲存角色權限失敗', 'Failed to save role permissions'))
  } finally {
    permissionSaving.value = false
  }
}

const submitDataScope = async () => {
  if (!editingAdminId.value) return
  try {
    dataScopeSaving.value = true
    await updateOpsAdminDataScope(editingAdminId.value, { scopeCode: dataScopeForm.scopeCode })
    dataScopeDialogVisible.value = false
    ElMessage.success(lt('管理员数据范围已更新', '管理員資料範圍已更新', 'Admin data scope updated'))
    await loadAdminDataScopes()
  } catch (error) {
    ElMessage.error(error.message || lt('更新管理员数据范围失败', '更新管理員資料範圍失敗', 'Failed to update admin data scope'))
  } finally {
    dataScopeSaving.value = false
  }
}

const dataScopeText = (value) => ({
  ALL_DEVELOPERS: lt('全部开发者', '全部開發者', 'All Developers'),
  HIGH_RISK_ONLY: lt('仅高风险/黑名单', '僅高風險/黑名單', 'High Risk / Blacklist Only'),
  PENDING_CERT_ONLY: lt('仅资质审核中', '僅資質審核中', 'Pending Certification Only'),
  BLOCKED_ONLY: lt('仅封禁/暂停账号', '僅封禁/暫停帳號', 'Blocked Accounts Only')
}[value] || value || '-')

onMounted(() => {
  loadProfile()
  loadPermissionMatrix()
  loadAdminDataScopes()
  loadTemplates()
  loadNoticeTemplates()
  loadDictionaryEntries()
  loadMenuPermissions()
  loadApprovalTemplates()
  loadSensitivePolicies()
})
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 18px; }
.summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
.summary-item { padding: 14px 16px; border-radius: 16px; background: #f8fafc; display: flex; flex-direction: column; gap: 6px; color: #475467; }
.summary-item strong { font-size: 20px; color: #101828; }
.rule-list { display: flex; flex-direction: column; gap: 12px; }
.rule-item { padding: 14px 16px; border-radius: 16px; background: #f8fafc; }
.card-header-inline { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.rule-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.rule-title { font-weight: 700; color: #101828; }
.rule-type { margin-top: 4px; color: #98a2b3; font-size: 12px; }
.rule-desc { margin-top: 8px; color: #667085; line-height: 1.6; font-size: 13px; }
.rule-actions { display: flex; gap: 8px; }
.permission-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.permission-role { font-size: 16px; font-weight: 700; margin-bottom: 12px; color: #101828; }
.permission-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.permission-help { margin-top: 14px; color: #667085; font-size: 13px; line-height: 1.7; }
@media (max-width: 900px) {
  .card-header-inline,
  .rule-head { flex-direction: column; align-items: flex-start; }
  .rule-actions { flex-wrap: wrap; }
  .permission-grid { grid-template-columns: 1fr; }
}
</style>
