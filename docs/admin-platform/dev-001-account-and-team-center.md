# DEV-001 账号与团队中心

## 1. 模块 PRD

### 1.1 模块目标

管理开发者主体信息、团队成员、登录安全、上传凭证与消息接收规则。

### 1.2 用户角色

- 开发者主账号
- 团队成员
- 平台管理员

### 1.3 业务价值

- 让开发者完成企业 / 个人主体认证
- 支撑多人协作
- 控制谁可以上传、提审、查看数据

## 2. 数据对象设计

### 2.1 DeveloperAccount

- `developerId`
- `accountType`
- `companyName`
- `contactName`
- `email`
- `phone`
- `verifyStatus`
- `riskLevel`

### 2.2 TeamMember

- `memberId`
- `developerId`
- `userId`
- `role`
- `status`
- `invitedAt`
- `joinedAt`

### 2.3 ApiCredential

- `credentialId`
- `developerId`
- `credentialType`
- `accessKey`
- `status`
- `expiredAt`

## 3. 状态流转设计

- 主体认证：
  - `UNVERIFIED -> PENDING -> VERIFIED`
  - `PENDING -> REJECTED`
- 成员状态：
  - `INVITED -> ACTIVE`
  - `INVITED -> EXPIRED`
  - `ACTIVE -> DISABLED`

## 4. 权限设计

- 开发者只能看到自己的主体与团队
- 主账号可管理成员
- 成员只拥有被授予的动作权限
- 运营后台可查看全部，但开发者后台严禁跨主体读取

## 5. 接口设计

- `GET /developer/account/profile`
- `PUT /developer/account/profile`
- `GET /developer/team/members`
- `POST /developer/team/members/invite`
- `PUT /developer/team/members/{id}/role`
- `POST /developer/team/credentials`

校验重点：

- 邀请邮箱格式
- 不允许重复邀请同邮箱
- 不允许跨主体修改成员
- 不允许普通成员提升自己权限

## 6. 数据库设计

- `developer_account`
- `developer_team_member`
- `developer_api_credential`
- `developer_account_verify_log`

## 7. 异常场景设计

- 重复邀请
- 已禁用成员继续登录
- 凭证泄露后失效
- 非主账号修改团队角色
- 开发者访问其他主体数据

## 8. 操作日志设计

- `DEVELOPER_PROFILE_UPDATED`
- `TEAM_MEMBER_INVITED`
- `TEAM_MEMBER_ROLE_CHANGED`
- `API_CREDENTIAL_CREATED`
- `API_CREDENTIAL_REVOKED`

## 9. 测试用例设计

- 查看自己账号信息
- 查看他人账号信息失败
- 邀请成员成功
- 重复邀请失败
- 普通成员改角色失败
- 主账号吊销凭证成功

## 10. 验收标准

- 不存在跨开发者主体数据读取
- 所有团队与凭证操作均有日志
- 高风险操作需二次确认
- 权限边界由后端保证

