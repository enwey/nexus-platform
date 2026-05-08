# DEV-002 / DEV-003 我的游戏与版本发布中心

## 1. 模块 PRD

### 1.1 模块目标

让开发者在只访问自己数据的前提下完成游戏创建、资料维护、包上传、校验、提审、版本追踪。

### 1.2 用户角色

- 开发者主账号
- 有发布权限的团队成员

### 1.3 业务价值

- 降低提审门槛
- 提供稳定发布链路
- 避免重复上传、重复提审、状态错乱

### 1.4 核心动作

- 创建游戏
- 编辑主数据
- 上传 ZIP / 包体
- 校验格式、大小、Manifest
- 提交审核
- 查看驳回原因
- 查看版本历史

## 2. 数据对象设计

### 2.1 DeveloperGame

- `gameId`
- `developerId`
- `appId`
- `name`
- `description`
- `iconUrl`
- `category`
- `tags`
- `status`

### 2.2 GameVersion

- `versionId`
- `gameId`
- `versionName`
- `packageUrl`
- `packageSize`
- `packageChecksum`
- `manifestStatus`
- `reviewStatus`
- `submitNote`

### 2.3 UploadTask

- `uploadTaskId`
- `gameId`
- `developerId`
- `fileName`
- `fileSize`
- `fileType`
- `uploadStatus`
- `checksum`

## 3. 状态流转设计

### 3.1 游戏状态

- `DRAFT`
- `PENDING`
- `APPROVED`
- `REJECTED`

### 3.2 上传状态

- `UPLOADING`
- `UPLOADED`
- `CHECKING`
- `CHECK_FAILED`
- `CHECK_PASSED`

### 3.3 版本审核状态

- `DRAFT -> PENDING`
- `PENDING -> APPROVED`
- `PENDING -> REJECTED`
- `REJECTED -> PENDING`

### 3.4 禁止流转

- 未通过校验包禁止提审
- 已通过版本禁止重复提审
- 非拥有者禁止访问版本明细

## 4. 权限设计

- 仅允许访问 `developer_id = current_user.developer_id`
- 只有具备发布权限的成员能上传与提审
- 普通只读成员不可提交审核

## 5. 接口设计

- `GET /developer/games`
- `POST /developer/games`
- `PUT /developer/games/{gameId}`
- `POST /developer/games/{gameId}/upload`
- `POST /developer/games/{gameId}/submit`
- `GET /developer/games/{gameId}/versions`

校验重点：

- 上传文件格式白名单
- 文件大小上限
- 校验哈希避免重复包
- 幂等键避免重复提审
- 只允许操作自己游戏

## 6. 数据库设计

- `games`
- `game_versions`
- `upload_task`
- `upload_check_result`

## 7. 异常场景设计

- 上传 ZIP 格式非法
- 上传超过大小限制
- Manifest 缺失
- 重复点击提审
- 操作他人游戏
- 审核中版本再次编辑

## 8. 操作日志设计

- `GAME_CREATED`
- `GAME_METADATA_UPDATED`
- `PACKAGE_UPLOADED`
- `PACKAGE_CHECK_COMPLETED`
- `VERSION_SUBMITTED`

## 9. 测试用例设计

- 创建自己游戏成功
- 创建游戏参数非法失败
- 上传合法 ZIP 成功
- 上传超大文件失败
- 上传非法后缀失败
- 未通过校验包提审失败
- 重复提审幂等
- 查看他人游戏失败

## 10. 验收标准

- 开发者只能访问自己游戏
- 文件校验、状态流转、幂等控制完整
- 审核链路真实可用
- 空态 / 错态 / 加载态完整

