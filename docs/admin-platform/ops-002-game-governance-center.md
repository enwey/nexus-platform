# OPS-002 游戏治理中心

## 1. 模块 PRD

### 1.1 模块目标

从平台视角统一管理游戏主数据、审核状态、前端可流转状态与运营前置条件。

### 1.2 用户角色

- 平台管理员
- 运营人员
- 审核主管

### 1.3 业务价值

- 统一游戏资产口径
- 让“审核状态”和“前端可消费状态”可见
- 给推荐、广告、运行时配置提供可信主数据

### 1.4 核心动作

- 查询游戏列表
- 过滤状态 / 分类 / 开发者
- 编辑主数据
- 查看版本检查结果
- 发起提审 / 驳回 / 通过
- 查看前端流转状态
- 执行前端展示封控 / 隐藏 / 解封

## 2. 数据对象设计

### 2.1 GameAsset

| 字段 | 说明 | 是否可编辑 | 是否审计 |
| --- | --- | --- | --- |
| gameId | 游戏 ID | 否 | 是 |
| appId | 平台应用 ID | 否 | 是 |
| developerId | 开发者 ID | 否 | 是 |
| name | 游戏名称 | 是 | 是 |
| description | 游戏描述 | 是 | 是 |
| iconUrl | 图标地址 | 是 | 是 |
| category | 分类 | 是 | 是 |
| tags | 标签 | 是 | 是 |
| status | 审核状态 | 否 | 是 |
| frontendState | 前端可消费状态 | 否 | 是 |
| visibilityStatus | 展示控制状态 | 是 | 是 |
| visibilityReason | 展示控制原因 | 是 | 是 |
| visibilityUntil | 临时封控截止时间 | 是 | 是 |
| version | 当前版本号 | 否 | 是 |
| requiresOnline | 是否联网 | 是 | 是 |

## 3. 状态流转设计

### 3.1 审核状态

- `DRAFT`
- `PROCESSING`
- `PENDING`
- `APPROVED`
- `REJECTED`

### 3.2 前端状态映射

- `APPROVED -> OPERABLE`
- `APPROVED + HIDDEN -> HIDDEN`
- `APPROVED + BLOCKED -> BLOCKED`
- `PENDING -> UNDER_REVIEW`
- `DRAFT -> NOT_OPEN`
- `REJECTED -> BLOCKED`
- `PROCESSING -> PROCESSING`

### 3.3 规则

- 只有 `APPROVED` 资产允许进入推荐运营
- `REJECTED` 资产必须被所有投放链路拦截
- `PENDING` 资产不可被前端消费
- `HIDDEN` 资产必须从前端列表和推荐候选池中移除
- `BLOCKED` 资产必须同时拦截前端列表、更新检查、下载和运行时取包

## 4. 权限设计

- 运营后台可查看全部游戏资产
- 开发者后台只能访问 `developer_id = current_user.id` 的数据
- 编辑主数据需要 `OPS_GAME_WRITE`
- 查看全部状态需要 `OPS_GAME_READ`

## 5. 接口设计

### 5.1 查询游戏列表

- `GET /admin/games`
- 参数：
  - `status`
  - `category`
  - `developerId`
  - `keyword`
  - `pageNo`
  - `pageSize`

### 5.2 编辑游戏主数据

- `PUT /admin/games/{gameId}/metadata`
- 校验：
  - 名称 1-100
  - 描述 0-500
  - 分类必须存在
  - 图标 URL 长度与协议合法

### 5.3 查询前端状态

- `GET /admin/games/{gameId}/state`

### 5.4 更新展示控制

- `PUT /admin/ops/games/{gameId}/visibility`
- 校验：
  - `visibilityStatus` 仅允许 `VISIBLE / HIDDEN / BLOCKED`
  - `HIDDEN / BLOCKED` 必须填写原因
  - 临时封控截止时间不能早于当前时间

## 6. 数据库设计

- 复用：
  - `games`
  - `game_versions`
  - `ops_game_profile`
- 需要确保：
  - `games.app_id` 唯一
  - `games.status` 可索引
  - `developer_id + status` 支持检索
  - `visibility_status + created_at` 支持检索

## 7. 异常场景设计

- 分类已删除但游戏仍引用
- 标签超长
- 图标 URL 非法
- 游戏不存在
- 游戏状态与版本状态不一致
- 开发者越权读取其他人游戏
- 已封控资产仍出现在前端推荐池
- 临时封控过期后未恢复正常展示

## 8. 操作日志设计

- `GAME_METADATA_UPDATED`
- `GAME_STATUS_VIEWED`
- `GAME_FRONTEND_STATE_EVALUATED`
- `GAME_VISIBILITY_UPDATE`

日志记录：

- 操作人
- 游戏 ID
- 修改字段
- 修改前 / 修改后
- 风险级别
- 请求来源

## 9. 测试用例设计

- 查询全部游戏
- 按状态过滤
- 按分类过滤
- 编辑游戏主数据成功
- 编辑分类不存在
- 编辑名称为空
- 开发者越权访问其他游戏
- 审核状态与前端状态映射正确
- 封控后前端列表不可见
- 封控后更新检查与下载被阻断
- 隐藏后推荐池候选资产不可见

## 10. 验收标准

- 运营可从列表直接看到状态治理结果
- 所有主数据修改有日志
- 只有已通过资产可进入后续运营链路
- 运营可随时封控、隐藏或恢复前端展示
- 越权访问被后端阻断
