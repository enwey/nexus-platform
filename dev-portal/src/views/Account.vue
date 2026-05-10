<template>
  <div class="account-page">
    <el-row :gutter="16">
      <el-col :xs="24" :xl="10">
        <el-card id="profile-section" class="panel-card">
          <template #header>{{ lt('基础资料', '基礎資料', 'Profile') }}</template>
          <el-form :model="profileForm" label-position="top">
            <el-form-item :label="lt('显示名称', '顯示名稱', 'Display Name')">
              <el-input v-model="profileForm.displayName" />
            </el-form-item>
            <el-form-item :label="lt('头像地址', '頭像網址', 'Avatar URL')">
              <el-input v-model="profileForm.avatarUrl" />
            </el-form-item>
            <el-form-item :label="lt('语言偏好', '語言偏好', 'Language')">
              <el-select v-model="profileForm.languageTag">
                <el-option label="简体中文" value="zh-CN" />
                <el-option label="繁體中文" value="zh-TW" />
                <el-option label="English" value="en" />
              </el-select>
            </el-form-item>
            <el-form-item :label="lt('邮箱', '電子郵件', 'Email')">
              <el-input :model-value="profileForm.email" disabled />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingProfile" @click="handleSaveProfile">{{ lt('保存资料', '儲存資料', 'Save Profile') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card id="certification-section" class="panel-card section-gap">
          <template #header>{{ lt('开发者资质', '開發者資質', 'Developer Certification') }}</template>
          <div class="summary-grid certification-summary">
            <div class="summary-item">
              <span>{{ lt('资质状态', '資質狀態', 'Status') }}</span>
              <strong>{{ certificationStatusText(certificationForm.profileStatus || certificationForm.certificationStatus) }}</strong>
            </div>
            <div class="summary-item">
              <span>{{ lt('主体类型', '主體類型', 'Subject Type') }}</span>
              <strong>{{ subjectTypeText(certificationForm.subjectType) }}</strong>
            </div>
            <div class="summary-item">
              <span>{{ lt('最近提交', '最近提交', 'Submitted') }}</span>
              <strong>{{ formatDate(certificationForm.submittedAt) }}</strong>
            </div>
          </div>
          <div v-if="certificationForm.rejectionReason" class="danger-desc">
            {{ lt('最近驳回原因：', '最近駁回原因：', 'Latest rejection: ') }}{{ certificationForm.rejectionReason }}
          </div>
          <el-form :model="certificationForm" label-position="top" class="section-gap-sm">
            <el-form-item :label="lt('主体类型', '主體類型', 'Subject Type')">
              <el-select v-model="certificationForm.subjectType">
                <el-option :label="lt('企业', '企業', 'Company')" value="COMPANY" />
                <el-option :label="lt('个人', '個人', 'Individual')" value="INDIVIDUAL" />
              </el-select>
            </el-form-item>
            <el-form-item :label="lt('主体名称', '主體名稱', 'Subject Name')">
              <el-input v-model="certificationForm.subjectName" maxlength="128" show-word-limit />
            </el-form-item>
            <el-form-item :label="lt('法人/负责人', '法人/負責人', 'Legal Representative')">
              <el-input v-model="certificationForm.legalRepresentative" maxlength="64" show-word-limit />
            </el-form-item>
            <el-form-item :label="lt('联系人', '聯絡人', 'Contact Name')">
              <el-input v-model="certificationForm.contactName" maxlength="64" show-word-limit />
            </el-form-item>
            <el-form-item :label="lt('联系电话', '聯絡電話', 'Contact Phone')">
              <el-input v-model="certificationForm.contactPhone" maxlength="32" show-word-limit />
            </el-form-item>
            <el-form-item v-if="certificationForm.subjectType === 'COMPANY'" :label="lt('营业执照号', '營業執照號', 'Business License No')">
              <el-input v-model="certificationForm.businessLicenseNo" maxlength="64" show-word-limit />
            </el-form-item>
            <el-form-item v-else :label="lt('身份证件号', '身份證件號', 'ID Document No')">
              <el-input v-model="certificationForm.idDocumentNo" maxlength="64" show-word-limit />
            </el-form-item>
            <el-form-item :label="lt('资质材料链接', '資質材料連結', 'Certificate Asset URLs')">
              <el-input
                v-model="certificationAssetsText"
                type="textarea"
                :rows="4"
                :placeholder="lt('每行一个 https 链接，最多 6 条', '每行一個 https 連結，最多 6 條', 'One https URL per line, up to 6 entries')"
              />
            </el-form-item>
            <el-form-item :label="lt('备注', '備註', 'Note')">
              <el-input v-model="certificationForm.note" type="textarea" :rows="3" maxlength="256" show-word-limit />
            </el-form-item>
            <el-form-item>
              <el-space wrap>
                <el-button :loading="certificationSaving" @click="handleSaveCertification(false)">{{ lt('保存草稿', '儲存草稿', 'Save Draft') }}</el-button>
                <el-button type="primary" :loading="certificationSaving" @click="handleSaveCertification(true)">{{ lt('提交审核', '提交審核', 'Submit for Review') }}</el-button>
                <el-button @click="loadCertification">{{ lt('刷新状态', '刷新狀態', 'Refresh Status') }}</el-button>
              </el-space>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="14">
        <el-card id="security-section" class="panel-card">
          <template #header>{{ lt('安全设置', '安全設定', 'Security') }}</template>
          <div class="security-block">
            <div class="security-row">
              <div>
                <div class="security-title">{{ lt('修改密码', '修改密碼', 'Change Password') }}</div>
                <div class="security-desc">{{ lt('通过邮箱验证码修改当前登录密码。', '透過電子郵件驗證碼修改目前登入密碼。', 'Change your password with an email verification code.') }}</div>
              </div>
              <el-button :loading="sendingCode" @click="handleSendChangeCode">{{ lt('发送验证码', '發送驗證碼', 'Send Code') }}</el-button>
            </div>

            <el-form :model="passwordForm" label-position="top" class="password-form">
              <el-form-item :label="lt('验证码', '驗證碼', 'Verification Code')">
                <el-input v-model="passwordForm.code" />
              </el-form-item>
              <el-form-item :label="lt('新密码', '新密碼', 'New Password')">
                <el-input v-model="passwordForm.newPassword" type="password" show-password />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="changingPassword" @click="handleChangePassword">{{ lt('确认修改', '確認修改', 'Update Password') }}</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :span="24">
        <el-card id="team-section" class="panel-card">
          <template #header>
            <div class="table-head">
              <span>{{ lt('资质审核记录', '資質審核記錄', 'Certification Review Records') }}</span>
              <el-button size="small" @click="loadCertificationReviews">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
            </div>
          </template>
          <el-table :data="certificationReviews" v-loading="certificationReviewsLoading" :empty-text="lt('暂无资质审核记录', '暫無資質審核記錄', 'No certification review records')">
            <el-table-column prop="actionType" :label="lt('动作', '動作', 'Action')" width="180" />
            <el-table-column prop="afterStatus" :label="lt('结果', '結果', 'Result')" width="140">
              <template #default="{ row }">
                <el-tag :type="certificationStatusTagType(row.afterStatus)">{{ certificationStatusText(row.afterStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reason" :label="lt('原因', '原因', 'Reason')" min-width="260" show-overflow-tooltip />
            <el-table-column prop="operatorId" :label="lt('操作人', '操作人', 'Operator')" width="120" />
            <el-table-column :label="lt('时间', '時間', 'Time')" min-width="180">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :xl="14">
        <el-card id="keys-section" class="panel-card">
          <template #header>
            <div class="table-head">
              <span>{{ lt('登录设备', '登入裝置', 'Devices') }}</span>
              <el-button size="small" @click="loadDevices">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
            </div>
          </template>
          <el-table :data="devices" v-loading="devicesLoading" :empty-text="lt('暂无设备记录', '暫無裝置記錄', 'No device records')">
            <el-table-column prop="deviceName" :label="lt('设备名称', '裝置名稱', 'Device')" min-width="180" />
            <el-table-column prop="platform" :label="lt('平台', '平台', 'Platform')" width="120" />
            <el-table-column prop="clientIp" :label="lt('IP 地址', 'IP 位址', 'IP')" width="150" />
            <el-table-column :label="lt('最近登录', '最近登入', 'Last Active')" min-width="170">
              <template #default="{ row }">{{ formatDate(row.lastSeenAt) }}</template>
            </el-table-column>
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="140">
              <template #default="{ row }">
                <el-tag v-if="row.current" type="success">{{ lt('当前设备', '目前裝置', 'Current') }}</el-tag>
                <el-button v-else type="danger" link @click="handleKickDevice(row)">{{ lt('下线', '下線', 'Kick') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="device-actions">
            <el-button type="warning" plain :loading="loggingOutAll" @click="handleLogoutAll">{{ lt('下线其他设备', '下線其他裝置', 'Sign Out Other Devices') }}</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="10">
        <el-card class="panel-card danger-card">
          <template #header>{{ lt('危险操作', '危險操作', 'Danger Zone') }}</template>
          <div class="danger-title">{{ lt('注销账号', '註銷帳號', 'Terminate Account') }}</div>
          <div class="danger-desc">{{ lt('注销后会清空邮箱绑定并使当前账号失效。输入“确认注销”后才能提交。', '註銷後會清空電子郵件綁定並使目前帳號失效。輸入「確認註銷」後才能送出。', 'Termination clears email binding and invalidates this account. Enter the confirm text before submitting.') }}</div>
          <el-input v-model="confirmText" class="danger-input" :placeholder="lt('请输入：确认注销', '請輸入：確認註銷', 'Type: 确认注销')" />
          <el-button type="danger" :loading="terminating" @click="handleTerminate">{{ lt('确认注销', '確認註銷', 'Terminate Account') }}</el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :xl="14">
        <el-card class="panel-card">
          <template #header>
            <div class="table-head">
              <span>{{ lt('团队成员', '團隊成員', 'Team Members') }}</span>
              <div class="table-tools">
                <el-button size="small" @click="loadTeamMembers">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
                <el-button size="small" type="primary" @click="openTeamDialog()">{{ lt('新增成员', '新增成員', 'Add Member') }}</el-button>
              </div>
            </div>
          </template>
          <div class="summary-grid">
            <div class="summary-item">
              <span>{{ lt('总成员', '總成員', 'Members') }}</span>
              <strong>{{ teamMembers.length }}</strong>
            </div>
            <div class="summary-item">
              <span>{{ lt('活跃中', '啟用中', 'Active') }}</span>
              <strong>{{ activeMembers }}</strong>
            </div>
            <div class="summary-item">
              <span>{{ lt('待加入', '待加入', 'Invited') }}</span>
              <strong>{{ invitedMembers }}</strong>
            </div>
          </div>
          <el-table :data="teamMembers" v-loading="teamLoading" :empty-text="lt('暂无团队成员', '暫無團隊成員', 'No team members')">
            <el-table-column prop="memberName" :label="lt('成员', '成員', 'Member')" min-width="160" />
            <el-table-column prop="memberEmail" :label="lt('邮箱', '電子郵件', 'Email')" min-width="220" />
            <el-table-column :label="lt('角色', '角色', 'Role')" width="150">
              <template #default="{ row }">
                <el-tag>{{ teamRoleText(row.teamRole) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="lt('状态', '狀態', 'Status')" width="120">
              <template #default="{ row }">
                <el-tag :type="teamStatusTagType(row.memberStatus)">{{ teamStatusText(row.memberStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="lt('更新时间', '更新時間', 'Updated')" min-width="160">
              <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="200">
              <template #default="{ row }">
                <el-button type="primary" link @click="openTeamDialog(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
                <el-button v-if="row.memberStatus !== 'ACTIVE'" type="success" link @click="handleTeamStatus(row, 'ACTIVE')">{{ lt('启用', '啟用', 'Activate') }}</el-button>
                <el-button v-if="row.memberStatus !== 'DISABLED'" type="danger" link @click="handleTeamStatus(row, 'DISABLED')">{{ lt('停用', '停用', 'Disable') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="10">
        <el-card class="panel-card">
          <template #header>
            <div class="table-head">
              <span>{{ lt('API 凭证', 'API 憑證', 'API Keys') }}</span>
              <el-button size="small" type="primary" @click="apiKeyDialogVisible = true">{{ lt('创建凭证', '建立憑證', 'Create Key') }}</el-button>
            </div>
          </template>
          <div class="security-desc">{{ lt('凭证仅显示一次完整密钥，请在创建后立即保存。撤销时需要二次确认。', '憑證完整密鑰只會顯示一次，建立後請立即保存。撤銷時需要二次確認。', 'The full secret is shown only once. Save it immediately after creation. Revocation requires confirmation.') }}</div>
          <el-table :data="apiKeys" v-loading="apiKeysLoading" :empty-text="lt('暂无凭证', '暫無憑證', 'No API keys')" class="api-key-table">
            <el-table-column prop="keyName" :label="lt('名称', '名稱', 'Name')" min-width="160" />
            <el-table-column prop="accessKey" :label="lt('Access Key', 'Access Key', 'Access Key')" min-width="200" />
            <el-table-column :label="lt('权限范围', '權限範圍', 'Scopes')" min-width="200">
              <template #default="{ row }">
                <el-space wrap>
                  <el-tag v-for="scope in row.scopes" :key="scope" size="small">{{ scopeText(scope) }}</el-tag>
                </el-space>
              </template>
            </el-table-column>
            <el-table-column :label="lt('状态', '狀態', 'Status')" width="120">
              <template #default="{ row }">
                <el-tag :type="apiKeyStatusTagType(row.status)">{{ apiKeyStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="lt('过期时间', '到期時間', 'Expires')" min-width="160">
              <template #default="{ row }">{{ formatDate(row.expiresAt) || '--' }}</template>
            </el-table-column>
            <el-table-column :label="lt('操作', '操作', 'Actions')" width="150">
              <template #default="{ row }">
                <el-button v-if="row.status === 'ACTIVE'" type="primary" link @click="openRotateDialog(row)">{{ lt('轮换', '輪換', 'Rotate') }}</el-button>
                <el-button v-if="row.status === 'ACTIVE'" type="danger" link @click="openRevokeDialog(row)">{{ lt('撤销', '撤銷', 'Revoke') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-row">
      <el-col :span="24">
        <el-card class="panel-card">
          <template #header>
            <div class="table-head">
              <span>{{ lt('团队操作日志', '團隊操作日誌', 'Team Activity Logs') }}</span>
              <el-button size="small" @click="loadWorkspaceAuditLogs">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
            </div>
          </template>
          <el-table :data="workspaceAuditLogs" v-loading="workspaceAuditLoading" :empty-text="lt('暂无团队与凭证日志', '暫無團隊與憑證日誌', 'No team or API key logs')">
            <el-table-column prop="action" :label="lt('动作', '動作', 'Action')" min-width="220" />
            <el-table-column :label="lt('结果', '結果', 'Result')" width="100">
              <template #default="{ row }">
                <el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? lt('成功', '成功', 'Success') : lt('失败', '失敗', 'Failed') }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reason" :label="lt('说明', '說明', 'Reason')" min-width="260" show-overflow-tooltip />
            <el-table-column :label="lt('时间', '時間', 'Time')" min-width="180">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="teamDialogVisible" :title="editingMemberId ? lt('编辑成员', '編輯成員', 'Edit Member') : lt('新增成员', '新增成員', 'Add Member')" :width="formDialogWidth">
      <el-form :model="teamForm" label-position="top">
        <el-form-item :label="lt('成员名称', '成員名稱', 'Member Name')">
          <el-input v-model="teamForm.memberName" />
        </el-form-item>
        <el-form-item :label="lt('成员邮箱', '成員電子郵件', 'Member Email')">
          <el-input v-model="teamForm.memberEmail" />
        </el-form-item>
        <el-form-item :label="lt('团队角色', '團隊角色', 'Team Role')">
          <el-select v-model="teamForm.teamRole">
            <el-option v-for="option in teamRoleOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('成员状态', '成員狀態', 'Member Status')">
          <el-select v-model="teamForm.memberStatus">
            <el-option v-for="option in teamStatusOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('备注', '備註', 'Note')">
          <el-input v-model="teamForm.note" type="textarea" :rows="3" maxlength="256" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="teamDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="teamSaving" @click="submitTeamMember">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="apiKeyDialogVisible" :title="lt('创建 API 凭证', '建立 API 憑證', 'Create API Key')" :width="formDialogWidth">
      <el-form :model="apiKeyForm" label-position="top">
        <el-form-item :label="lt('凭证名称', '憑證名稱', 'Key Name')">
          <el-input v-model="apiKeyForm.keyName" />
        </el-form-item>
        <el-form-item :label="lt('权限范围', '權限範圍', 'Scopes')">
          <el-select v-model="apiKeyForm.scopes" multiple collapse-tags collapse-tags-tooltip>
            <el-option v-for="option in apiScopeOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('过期时间', '到期時間', 'Expires At')">
          <el-date-picker
            v-model="apiKeyForm.expiresAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :placeholder="lt('可选，默认不过期', '選填，預設不過期', 'Optional, no expiration by default')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="apiKeyDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="apiKeyCreating" @click="submitApiKey">{{ lt('创建', '建立', 'Create') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="secretDialogVisible" :title="lt('请立即保存密钥', '請立即保存密鑰', 'Save Your Secret Now')" :width="secretDialogWidth">
      <div class="secret-box">
        <div class="secret-label">Access Key</div>
        <div class="secret-value">{{ createdKey.accessKey }}</div>
        <div class="secret-label">Secret</div>
        <div class="secret-value">{{ createdKey.secret }}</div>
      </div>
      <div class="security-desc">{{ lt('关闭弹窗后将无法再次查看完整 Secret。', '關閉視窗後將無法再次查看完整 Secret。', 'The full secret cannot be viewed again after this dialog is closed.') }}</div>
      <template #footer>
        <el-button type="primary" @click="secretDialogVisible = false">{{ lt('我已保存', '我已保存', 'I Saved It') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="revokeDialogVisible" :title="lt('撤销 API 凭证', '撤銷 API 憑證', 'Revoke API Key')" :width="confirmDialogWidth">
      <div class="danger-desc">{{ lt('撤销后凭证将立刻失效，且不能恢复。请输入 REVOKE 确认。', '撤銷後憑證會立刻失效，且無法恢復。請輸入 REVOKE 確認。', 'Revoked keys stop working immediately and cannot be restored. Enter REVOKE to confirm.') }}</div>
      <el-input v-model="revokeConfirmText" class="danger-input" placeholder="REVOKE" />
      <template #footer>
        <el-button @click="revokeDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="danger" :loading="apiKeyRevoking" @click="submitRevokeApiKey">{{ lt('确认撤销', '確認撤銷', 'Confirm Revoke') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rotateDialogVisible" :title="lt('轮换 API 凭证', '輪換 API 憑證', 'Rotate API Key')" :width="formDialogWidth">
      <div class="danger-desc">{{ lt('轮换会立即生成一把新密钥，并撤销当前旧密钥。请输入 REVOKE 确认。', '輪換會立即產生一把新密鑰，並撤銷目前舊密鑰。請輸入 REVOKE 確認。', 'Rotation creates a new key immediately and revokes the current one. Enter REVOKE to confirm.') }}</div>
      <el-form :model="rotateForm" label-position="top">
        <el-form-item :label="lt('确认文本', '確認文字', 'Confirm Text')">
          <el-input v-model="rotateForm.confirmText" placeholder="REVOKE" />
        </el-form-item>
        <el-form-item :label="lt('新凭证名称', '新憑證名稱', 'New Key Name')">
          <el-input v-model="rotateForm.newKeyName" />
        </el-form-item>
        <el-form-item :label="lt('权限范围', '權限範圍', 'Scopes')">
          <el-select v-model="rotateForm.scopes" multiple collapse-tags collapse-tags-tooltip>
            <el-option v-for="option in apiScopeOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="lt('过期时间', '到期時間', 'Expires At')">
          <el-date-picker v-model="rotateForm.expiresAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rotateDialogVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="apiKeyRotating" @click="submitRotateApiKey">{{ lt('确认轮换', '確認輪換', 'Rotate') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  changePassword,
  createApiKey,
  createTeamMember,
  getApiKeys,
  getDeveloperCertification,
  getDeveloperCertificationReviews,
  getDevices,
  getProfile,
  getTeamMembers,
  getWorkspaceAuditLogs,
  kickDevice,
  logoutAll,
  rotateApiKey,
  revokeApiKey,
  saveDeveloperCertification,
  sendCode,
  terminateAccount,
  updateProfile,
  updateTeamMember,
  updateTeamMemberStatus
} from '../api'
import { useViewport } from '../composables/useViewport'
import { useI18nLite } from '../i18n'
import { useUserStore } from '../stores/user'
import { formatDate } from '../utils/portal'

const { lt } = useI18nLite()
const { isTabletOrBelow, isPhone } = useViewport()
const route = useRoute()
const userStore = useUserStore()
const savingProfile = ref(false)
const sendingCode = ref(false)
const changingPassword = ref(false)
const devicesLoading = ref(false)
const loggingOutAll = ref(false)
const terminating = ref(false)
const devices = ref([])
const confirmText = ref('')

const teamLoading = ref(false)
const teamSaving = ref(false)
const teamMembers = ref([])
const teamDialogVisible = ref(false)
const editingMemberId = ref(null)
const apiKeysLoading = ref(false)
const apiKeyCreating = ref(false)
const apiKeyRevoking = ref(false)
const apiKeyRotating = ref(false)
const apiKeys = ref([])
const apiKeyDialogVisible = ref(false)
const secretDialogVisible = ref(false)
const revokeDialogVisible = ref(false)
const rotateDialogVisible = ref(false)
const workspaceAuditLoading = ref(false)
const workspaceAuditLogs = ref([])
const certificationSaving = ref(false)
const certificationReviewsLoading = ref(false)
const certificationReviews = ref([])
const revokeTarget = ref(null)
const revokeConfirmText = ref('')
const rotateTarget = ref(null)
const formDialogWidth = computed(() => (isPhone.value ? '94%' : '520px'))
const secretDialogWidth = computed(() => (isPhone.value ? '94%' : isTabletOrBelow.value ? '88%' : '560px'))
const confirmDialogWidth = computed(() => (isPhone.value ? '92%' : '460px'))
const certificationAssetsText = ref('')
const createdKey = reactive({
  accessKey: '',
  secret: ''
})

const profileForm = reactive({
  displayName: '',
  avatarUrl: '',
  languageTag: 'zh-CN',
  email: ''
})

const passwordForm = reactive({
  code: '',
  newPassword: ''
})

const teamForm = reactive({
  memberName: '',
  memberEmail: '',
  teamRole: 'RELEASE_MANAGER',
  memberStatus: 'INVITED',
  note: ''
})

const apiKeyForm = reactive({
  keyName: '',
  scopes: ['RELEASE_READ'],
  expiresAt: ''
})

const rotateForm = reactive({
  confirmText: '',
  newKeyName: '',
  scopes: ['RELEASE_READ'],
  expiresAt: ''
})

const certificationForm = reactive({
  profileStatus: 'DRAFT',
  certificationStatus: 'UNVERIFIED',
  subjectType: 'COMPANY',
  subjectName: '',
  legalRepresentative: '',
  contactName: '',
  contactPhone: '',
  businessLicenseNo: '',
  idDocumentNo: '',
  note: '',
  rejectionReason: '',
  submittedAt: '',
  reviewedAt: ''
})

const teamRoleOptions = computed(() => [
  { value: 'ADMIN', label: lt('管理员', '管理員', 'Admin') },
  { value: 'RELEASE_MANAGER', label: lt('发布经理', '發布經理', 'Release Manager') },
  { value: 'ANALYST', label: lt('数据分析', '數據分析', 'Analyst') },
  { value: 'FINANCE', label: lt('财务', '財務', 'Finance') }
])

const teamStatusOptions = computed(() => [
  { value: 'INVITED', label: lt('待加入', '待加入', 'Invited') },
  { value: 'ACTIVE', label: lt('启用中', '啟用中', 'Active') },
  { value: 'DISABLED', label: lt('已停用', '已停用', 'Disabled') }
])

const apiScopeOptions = computed(() => [
  { value: 'PROFILE_READ', label: lt('资料读取', '資料讀取', 'Profile Read') },
  { value: 'RELEASE_READ', label: lt('发布读取', '發布讀取', 'Release Read') },
  { value: 'RELEASE_WRITE', label: lt('发布写入', '發布寫入', 'Release Write') },
  { value: 'ANALYTICS_READ', label: lt('数据读取', '數據讀取', 'Analytics Read') }
])

const activeMembers = computed(() => teamMembers.value.filter((item) => item.memberStatus === 'ACTIVE').length)
const invitedMembers = computed(() => teamMembers.value.filter((item) => item.memberStatus === 'INVITED').length)

const focusSectionMap = {
  profile: 'profile-section',
  certification: 'certification-section',
  security: 'security-section',
  team: 'team-section',
  keys: 'keys-section'
}

const scrollToFocusedSection = async () => {
  const focusSection = route.meta?.focusSection
  if (!focusSection) return
  await nextTick()
  const targetId = focusSectionMap[focusSection]
  const target = targetId ? document.getElementById(targetId) : null
  target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const resetTeamForm = () => {
  editingMemberId.value = null
  Object.assign(teamForm, {
    memberName: '',
    memberEmail: '',
    teamRole: 'RELEASE_MANAGER',
    memberStatus: 'INVITED',
    note: ''
  })
}

const loadProfile = async () => {
  const res = await getProfile()
  Object.assign(profileForm, {
    displayName: res.data?.displayName || '',
    avatarUrl: res.data?.avatarUrl || '',
    languageTag: res.data?.languageTag || 'zh-CN',
    email: res.data?.email || userStore.user?.email || ''
  })
}

const loadDevices = async () => {
  devicesLoading.value = true
  try {
    const res = await getDevices()
    devices.value = res.data || []
  } finally {
    devicesLoading.value = false
  }
}

const loadTeamMembers = async () => {
  teamLoading.value = true
  try {
    const res = await getTeamMembers()
    teamMembers.value = res.data || []
  } finally {
    teamLoading.value = false
  }
}

const loadApiKeys = async () => {
  apiKeysLoading.value = true
  try {
    const res = await getApiKeys()
    apiKeys.value = res.data || []
  } finally {
    apiKeysLoading.value = false
  }
}

const loadWorkspaceAuditLogs = async () => {
  workspaceAuditLoading.value = true
  try {
    const res = await getWorkspaceAuditLogs()
    workspaceAuditLogs.value = res.data || []
  } finally {
    workspaceAuditLoading.value = false
  }
}

const loadCertification = async () => {
  const res = await getDeveloperCertification()
  Object.assign(certificationForm, {
    profileStatus: res.data?.profileStatus || 'DRAFT',
    certificationStatus: res.data?.certificationStatus || 'UNVERIFIED',
    subjectType: res.data?.subjectType || 'COMPANY',
    subjectName: res.data?.subjectName || '',
    legalRepresentative: res.data?.legalRepresentative || '',
    contactName: res.data?.contactName || '',
    contactPhone: res.data?.contactPhone || '',
    businessLicenseNo: res.data?.businessLicenseNo || '',
    idDocumentNo: res.data?.idDocumentNo || '',
    note: res.data?.note || '',
    rejectionReason: res.data?.rejectionReason || '',
    submittedAt: res.data?.submittedAt || '',
    reviewedAt: res.data?.reviewedAt || ''
  })
  certificationAssetsText.value = (res.data?.certificateAssets || []).join('\n')
}

const loadCertificationReviews = async () => {
  certificationReviewsLoading.value = true
  try {
    const res = await getDeveloperCertificationReviews()
    certificationReviews.value = res.data || []
  } finally {
    certificationReviewsLoading.value = false
  }
}

const handleSaveProfile = async () => {
  try {
    savingProfile.value = true
    await updateProfile(profileForm)
    ElMessage.success(lt('资料已保存', '資料已儲存', 'Profile saved'))
    await loadProfile()
  } catch (error) {
    ElMessage.error(error.message || lt('保存资料失败', '儲存資料失敗', 'Failed to save profile'))
  } finally {
    savingProfile.value = false
  }
}

const handleSendChangeCode = async () => {
  try {
    sendingCode.value = true
    await sendCode({
      email: profileForm.email,
      purpose: 'CHANGE_PASSWORD',
      source: 'dev-portal',
      scene: 'DEV_PORTAL_CHANGE_PASSWORD'
    })
    ElMessage.success(lt('验证码已发送，请查收邮箱', '驗證碼已發送，請查收信箱', 'Verification code sent to your email'))
  } catch (error) {
    ElMessage.error(error.message || lt('发送验证码失败', '發送驗證碼失敗', 'Failed to send verification code'))
  } finally {
    sendingCode.value = false
  }
}

const handleChangePassword = async () => {
  try {
    changingPassword.value = true
    await changePassword({
      email: profileForm.email,
      code: passwordForm.code,
      newPassword: passwordForm.newPassword
    })
    passwordForm.code = ''
    passwordForm.newPassword = ''
    ElMessage.success(lt('密码已更新', '密碼已更新', 'Password updated'))
  } catch (error) {
    ElMessage.error(error.message || lt('修改密码失败', '修改密碼失敗', 'Failed to change password'))
  } finally {
    changingPassword.value = false
  }
}

const handleKickDevice = async (row) => {
  try {
    await kickDevice(row.deviceId)
    ElMessage.success(lt('设备已下线', '裝置已下線', 'Device signed out'))
    await loadDevices()
  } catch (error) {
    ElMessage.error(error.message || lt('操作失败', '操作失敗', 'Action failed'))
  }
}

const handleLogoutAll = async () => {
  try {
    loggingOutAll.value = true
    await logoutAll()
    ElMessage.success(lt('其他设备已下线', '其他裝置已下線', 'Other devices signed out'))
    await loadDevices()
  } catch (error) {
    ElMessage.error(error.message || lt('操作失败', '操作失敗', 'Action failed'))
  } finally {
    loggingOutAll.value = false
  }
}

const handleTerminate = async () => {
  try {
    terminating.value = true
    await terminateAccount(confirmText.value)
    userStore.logout()
    ElMessage.success(lt('账号已注销', '帳號已註銷', 'Account terminated'))
    window.location.href = '/login'
  } catch (error) {
    ElMessage.error(error.message || lt('注销失败', '註銷失敗', 'Failed to terminate account'))
  } finally {
    terminating.value = false
  }
}

const handleSaveCertification = async (submit) => {
  try {
    certificationSaving.value = true
    await saveDeveloperCertification({
      subjectType: certificationForm.subjectType,
      subjectName: certificationForm.subjectName,
      legalRepresentative: certificationForm.legalRepresentative,
      contactName: certificationForm.contactName,
      contactPhone: certificationForm.contactPhone,
      businessLicenseNo: certificationForm.businessLicenseNo,
      idDocumentNo: certificationForm.idDocumentNo,
      certificateAssets: certificationAssetsText.value.split('\n').map((item) => item.trim()).filter(Boolean),
      note: certificationForm.note,
      submit
    })
    ElMessage.success(submit
      ? lt('资质材料已提交审核', '資質材料已提交審核', 'Certification submitted for review')
      : lt('资质草稿已保存', '資質草稿已儲存', 'Certification draft saved'))
    await Promise.all([loadCertification(), loadCertificationReviews()])
  } catch (error) {
    ElMessage.error(error.message || lt('保存资质资料失败', '儲存資質資料失敗', 'Failed to save certification profile'))
  } finally {
    certificationSaving.value = false
  }
}

const openTeamDialog = (row = null) => {
  if (!row) {
    resetTeamForm()
  } else {
    editingMemberId.value = row.id
    Object.assign(teamForm, {
      memberName: row.memberName,
      memberEmail: row.memberEmail,
      teamRole: row.teamRole,
      memberStatus: row.memberStatus,
      note: row.note || ''
    })
  }
  teamDialogVisible.value = true
}

const submitTeamMember = async () => {
  try {
    teamSaving.value = true
    if (editingMemberId.value) {
      await updateTeamMember(editingMemberId.value, { ...teamForm })
    } else {
      await createTeamMember({ ...teamForm })
    }
    ElMessage.success(lt('成员已保存', '成員已儲存', 'Member saved'))
    teamDialogVisible.value = false
    resetTeamForm()
    await Promise.all([loadTeamMembers(), loadWorkspaceAuditLogs()])
  } catch (error) {
    ElMessage.error(error.message || lt('保存成员失败', '儲存成員失敗', 'Failed to save member'))
  } finally {
    teamSaving.value = false
  }
}

const handleTeamStatus = async (row, memberStatus) => {
  try {
    await updateTeamMemberStatus(row.id, { memberStatus })
    ElMessage.success(lt('成员状态已更新', '成員狀態已更新', 'Member status updated'))
    await Promise.all([loadTeamMembers(), loadWorkspaceAuditLogs()])
  } catch (error) {
    ElMessage.error(error.message || lt('更新成员状态失败', '更新成員狀態失敗', 'Failed to update member status'))
  }
}

const submitApiKey = async () => {
  try {
    apiKeyCreating.value = true
    const res = await createApiKey({
      keyName: apiKeyForm.keyName,
      scopes: apiKeyForm.scopes,
      expiresAt: apiKeyForm.expiresAt || null
    })
    createdKey.accessKey = res.data?.key?.accessKey || ''
    createdKey.secret = res.data?.secret || ''
    apiKeyDialogVisible.value = false
    secretDialogVisible.value = true
    Object.assign(apiKeyForm, {
      keyName: '',
      scopes: ['RELEASE_READ'],
      expiresAt: ''
    })
    ElMessage.success(lt('凭证已创建', '憑證已建立', 'API key created'))
    await Promise.all([loadApiKeys(), loadWorkspaceAuditLogs()])
  } catch (error) {
    ElMessage.error(error.message || lt('创建凭证失败', '建立憑證失敗', 'Failed to create API key'))
  } finally {
    apiKeyCreating.value = false
  }
}

const openRevokeDialog = (row) => {
  revokeTarget.value = row
  revokeConfirmText.value = ''
  revokeDialogVisible.value = true
}

const openRotateDialog = (row) => {
  rotateTarget.value = row
  Object.assign(rotateForm, {
    confirmText: '',
    newKeyName: `${row.keyName || 'Key'} ${lt('轮换', '輪換', 'Rotated')}`,
    scopes: Array.isArray(row.scopes) && row.scopes.length ? [...row.scopes] : ['RELEASE_READ'],
    expiresAt: row.expiresAt || ''
  })
  rotateDialogVisible.value = true
}

const submitRevokeApiKey = async () => {
  try {
    apiKeyRevoking.value = true
    await revokeApiKey(revokeTarget.value.id, { confirmText: revokeConfirmText.value })
    revokeDialogVisible.value = false
    revokeTarget.value = null
    revokeConfirmText.value = ''
    ElMessage.success(lt('凭证已撤销', '憑證已撤銷', 'API key revoked'))
    await Promise.all([loadApiKeys(), loadWorkspaceAuditLogs()])
  } catch (error) {
    ElMessage.error(error.message || lt('撤销凭证失败', '撤銷憑證失敗', 'Failed to revoke API key'))
  } finally {
    apiKeyRevoking.value = false
  }
}

const submitRotateApiKey = async () => {
  if (!rotateTarget.value?.id) return
  try {
    apiKeyRotating.value = true
    const res = await rotateApiKey(rotateTarget.value.id, {
      confirmText: rotateForm.confirmText,
      newKeyName: rotateForm.newKeyName,
      scopes: rotateForm.scopes,
      expiresAt: rotateForm.expiresAt || null
    })
    createdKey.accessKey = res.data?.key?.accessKey || ''
    createdKey.secret = res.data?.secret || ''
    rotateDialogVisible.value = false
    secretDialogVisible.value = true
    rotateTarget.value = null
    ElMessage.success(lt('凭证已轮换，旧密钥已撤销', '憑證已輪換，舊密鑰已撤銷', 'API key rotated and previous key revoked'))
    await Promise.all([loadApiKeys(), loadWorkspaceAuditLogs()])
  } catch (error) {
    ElMessage.error(error.message || lt('轮换凭证失败', '輪換憑證失敗', 'Failed to rotate API key'))
  } finally {
    apiKeyRotating.value = false
  }
}

const teamRoleText = (value) => {
  const map = {
    OWNER: lt('所有者', '所有者', 'Owner'),
    ADMIN: lt('管理员', '管理員', 'Admin'),
    RELEASE_MANAGER: lt('发布经理', '發布經理', 'Release Manager'),
    ANALYST: lt('数据分析', '數據分析', 'Analyst'),
    FINANCE: lt('财务', '財務', 'Finance')
  }
  return map[value] || value
}

const teamStatusText = (value) => {
  const map = {
    INVITED: lt('待加入', '待加入', 'Invited'),
    ACTIVE: lt('启用中', '啟用中', 'Active'),
    DISABLED: lt('已停用', '已停用', 'Disabled')
  }
  return map[value] || value
}

const teamStatusTagType = (value) => {
  const map = {
    INVITED: 'warning',
    ACTIVE: 'success',
    DISABLED: 'danger'
  }
  return map[value] || 'info'
}

const apiKeyStatusText = (value) => {
  const map = {
    ACTIVE: lt('生效中', '生效中', 'Active'),
    REVOKED: lt('已撤销', '已撤銷', 'Revoked'),
    EXPIRED: lt('已过期', '已過期', 'Expired')
  }
  return map[value] || value
}

const apiKeyStatusTagType = (value) => {
  const map = {
    ACTIVE: 'success',
    REVOKED: 'danger',
    EXPIRED: 'warning'
  }
  return map[value] || 'info'
}

const scopeText = (value) => {
  const map = {
    PROFILE_READ: lt('资料读取', '資料讀取', 'Profile Read'),
    RELEASE_READ: lt('发布读取', '發布讀取', 'Release Read'),
    RELEASE_WRITE: lt('发布写入', '發布寫入', 'Release Write'),
    ANALYTICS_READ: lt('数据读取', '數據讀取', 'Analytics Read')
  }
  return map[value] || value
}

const subjectTypeText = (value) => ({
  COMPANY: lt('企业', '企業', 'Company'),
  INDIVIDUAL: lt('个人', '個人', 'Individual')
}[value] || value || '-')

const certificationStatusText = (value) => ({
  DRAFT: lt('草稿', '草稿', 'Draft'),
  UNVERIFIED: lt('未认证', '未認證', 'Unverified'),
  PENDING: lt('审核中', '審核中', 'Pending'),
  VERIFIED: lt('已认证', '已認證', 'Verified'),
  REJECTED: lt('已驳回', '已駁回', 'Rejected')
}[value] || value || '-')

const certificationStatusTagType = (value) => ({
  DRAFT: 'info',
  UNVERIFIED: 'info',
  PENDING: 'warning',
  VERIFIED: 'success',
  REJECTED: 'danger'
}[value] || 'info')

onMounted(async () => {
  await Promise.all([loadProfile(), loadDevices(), loadTeamMembers(), loadApiKeys(), loadCertification(), loadCertificationReviews(), loadWorkspaceAuditLogs()])
  await scrollToFocusedSection()
})

watch(() => route.meta?.focusSection, async () => {
  await scrollToFocusedSection()
})
</script>

<style scoped>
.account-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-gap {
  margin-top: 16px;
}

.section-gap-sm {
  margin-top: 8px;
}

.section-row {
  margin-top: 0;
}

.panel-card {
  border-radius: 20px;
}

.security-block {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.security-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.security-title {
  font-size: 15px;
  font-weight: 700;
  color: #111827;
}

.security-desc {
  margin-top: 6px;
  color: #667085;
  font-size: 13px;
  line-height: 1.7;
}

.password-form {
  max-width: 420px;
}

.table-head,
.table-tools {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.device-actions {
  margin-top: 14px;
}

.danger-card {
  border-color: rgba(239, 68, 68, 0.18);
}

.danger-title {
  font-size: 16px;
  font-weight: 800;
  color: #b42318;
}

.danger-desc {
  margin-top: 10px;
  line-height: 1.7;
  color: #667085;
  font-size: 13px;
}

.danger-input {
  margin: 16px 0;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #475467;
  font-size: 13px;
}

.summary-item strong {
  font-size: 22px;
  color: #111827;
}

.certification-summary .summary-item strong {
  font-size: 16px;
}

.api-key-table {
  margin-top: 12px;
}

.secret-box {
  display: grid;
  gap: 10px;
  padding: 16px;
  border-radius: 18px;
  background: #0f172a;
}

.secret-label {
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  text-transform: uppercase;
}

.secret-value {
  color: #fff;
  font-family: 'SFMono-Regular', ui-monospace, Menlo, monospace;
  font-size: 13px;
  word-break: break-all;
}

@media (max-width: 920px) {
  .security-row,
  .table-head {
    flex-direction: column;
  }

  .table-tools {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .password-form {
    max-width: none;
  }
}
</style>
