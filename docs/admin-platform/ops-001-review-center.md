# OPS-001 审核中心

## 1. 模块 PRD

### 1.1 模块目标

统一管理平台游戏 / 版本的审核生命周期，确保只有合法、合规、可运行的资产进入前端流转。

### 1.2 用户角色

- 平台管理员
- 审核员
- 审核主管

### 1.3 业务价值

- 保障前端内容安全
- 保证审核口径统一
- 减少开发者反复提审成本
- 支撑审核 SLA 与积压治理

### 1.4 核心页面 / 动作

- 待审核列表
- 已通过列表
- 已驳回列表
- 审核分配台
- SLA 积压看板
- 审核详情页
- 版本检查结果弹层 / 详情页
- 审核通过
- 审核驳回
- 分配审核人
- 重新提审记录查看

### 1.5 非目标范围

- 不负责开发者上传
- 不负责推荐投放配置
- 不负责客户端运行时参数

## 2. 数据对象设计

### 2.1 ReviewTask

| 字段 | 说明 | 是否可编辑 | 是否审计 |
| --- | --- | --- | --- |
| reviewTaskId | 审核任务 ID | 否 | 是 |
| gameId | 游戏 ID | 否 | 是 |
| versionId | 版本 ID | 否 | 是 |
| developerId | 开发者 ID | 否 | 是 |
| reviewStatus | 审核状态 | 是 | 是 |
| submitReason | 提审说明 | 否 | 是 |
| reviewReason | 审核意见 | 是 | 是 |
| manifestCheckResult | 包校验结果 | 否 | 是 |
| assignedReviewerId | 审核人 | 是 | 是 |
| assignedAt | 分配时间 | 否 | 是 |
| dueAt | SLA 截止时间 | 否 | 是 |
| overdueFlag | 是否积压超时 | 否 | 是 |
| submittedAt | 提审时间 | 否 | 是 |
| reviewedAt | 审核时间 | 否 | 是 |

### 2.2 ReviewStatus

- `DRAFT`
- `PENDING`
- `APPROVED`
- `REJECTED`
- `WITHDRAWN`

## 3. 状态流转设计

### 3.1 允许流转

- `DRAFT -> PENDING`
- `REJECTED -> PENDING`
- `PENDING -> APPROVED`
- `PENDING -> REJECTED`
- `PENDING -> WITHDRAWN`
- `PENDING -> PENDING`（仅允许分配审核人，不改变审核状态）

### 3.2 禁止流转

- `APPROVED -> PENDING`
- `APPROVED -> REJECTED`
- `REJECTED -> APPROVED`
- `WITHDRAWN -> APPROVED`

### 3.3 流转副作用

- `PENDING -> APPROVED`
  - 写审核日志
  - 更新游戏 / 版本可流转状态
  - 允许进入运营投放链路
- `PENDING -> REJECTED`
  - 写审核日志
  - 通知开发者
  - 锁定当前版本不可继续流转
- `PENDING -> PENDING`（分配审核人）
  - 写分配日志
  - 更新审核负责人
  - 更新分配时间
  - 进入 SLA 跟踪

## 4. 权限设计

### 4.1 角色

- `OPS_ADMIN`
- `OPS_REVIEWER`
- `OPS_REVIEW_LEAD`

### 4.2 权限矩阵

| 动作 | 审核员 | 审核主管 | 管理员 |
| --- | --- | --- | --- |
| 查看审核列表 | 是 | 是 | 是 |
| 查看审核详情 | 是 | 是 | 是 |
| 通过审核 | 是 | 是 | 是 |
| 驳回审核 | 是 | 是 | 是 |
| 重新分配审核人 | 否 | 是 | 是 |
| 查看全部审核日志 | 否 | 是 | 是 |
| 查看 SLA 积压 | 是 | 是 | 是 |

### 4.3 数据权限

- 运营后台可看全平台审核任务
- 开发者后台只能看到自己的提审结果

## 5. 接口设计

### 5.1 查询审核列表

- `GET /admin/reviews`
- 参数：
  - `status`
  - `keyword`
  - `developerId`
  - `pageNo`
  - `pageSize`
- 校验：
  - `pageSize <= 100`
  - `status` 必须在枚举内

### 5.2 通过审核

- `POST /admin/reviews/{reviewTaskId}/approve`
- 参数：
  - `reason`
  - `operatorToken`
- 校验：
  - 审核状态必须为 `PENDING`
  - 审核意见长度 2-500
  - 幂等键或版本号校验，避免重复通过

### 5.3 驳回审核

- `POST /admin/reviews/{reviewTaskId}/reject`
- 参数：
  - `reason`
  - `operatorToken`
- 校验：
  - 状态必须为 `PENDING`
  - 驳回原因必填

### 5.4 分配审核任务

- `POST /admin/ops/reviews/{versionId}/assign`
- 参数：
  - `reviewerId`
  - `note`
- 校验：
  - 版本状态必须为 `SUBMITTED`
  - `reviewerId` 必须为管理员账号
  - 不允许对不存在版本分配
  - 分配动作必须写日志

## 6. 数据库设计

### 6.1 表

- `review_task`
- `review_task_log`
- `review_check_result`
- `game_versions.assigned_reviewer_id`
- `game_versions.assigned_at`

### 6.2 关键字段

- `review_task`
  - `id`
  - `game_id`
  - `version_id`
  - `developer_id`
  - `status`
  - `submit_reason`
  - `review_reason`
  - `assigned_reviewer_id`
  - `submitted_at`
  - `reviewed_at`
  - `created_at`
  - `updated_at`

### 6.3 索引

- `idx_review_task_status_submitted_at`
- `idx_review_task_game_version`
- `uk_review_task_version_pending`

## 7. 异常场景设计

- 重复点击通过 / 驳回
- 审核时版本已被撤回
- 审核时包检查结果为空
- 审核任务不存在
- 非法状态流转
- 非审核角色调用接口
- 审核分配到不存在的管理员
- 待审任务长时间未分配

## 8. 操作日志设计

- 日志事件：
- `REVIEW_APPROVED`
- `REVIEW_REJECTED`
- `REVIEW_REASSIGNED`
- `REVIEW_SLA_OVERDUE`
- 记录字段：
  - 操作人
  - 审核任务 ID
  - 游戏 / 版本 ID
  - 操作前状态
  - 操作后状态
  - 原因
  - 请求 IP
  - 请求 Trace ID

## 9. 测试用例设计

- 正常通过审核
- 正常驳回审核
- 已通过任务再次通过
- 已驳回任务直接通过
- 非审核员调用通过接口
- 空原因驳回
- 重复提交审批请求
- 审核日志是否完整
- 审核分配是否合法
- SLA 超时任务是否能正确识别

## 10. 验收标准

- 审核列表支持分页、筛选、搜索
- 非法状态流转全部被后端拒绝
- 所有审核动作均写操作日志
- 审核任务可分配负责人并展示 SLA 截止
- 高风险操作二次确认
- 开发者看不到其他开发者提审任务
- 重复提交不会生成重复结果
