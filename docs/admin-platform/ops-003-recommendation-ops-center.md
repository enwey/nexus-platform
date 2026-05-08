# OPS-003 推荐运营中心

## 1. 模块 PRD

### 1.1 模块目标

管理前端推荐分类、广告位、推荐内容及其展示状态，确保前端呈现可控、可审计、可排期。

### 1.2 用户角色

- 平台管理员
- 运营人员
- 审核主管

### 1.3 业务价值

- 保证前端推荐内容可控
- 支撑排期、暂停、下线
- 支撑分类治理与投放位扩展

### 1.4 核心子模块

- 推荐分类
- 游戏页广告位
- 发现页广告位
- 推荐列表
- 总览看板

## 2. 数据对象设计

### 2.1 RecommendCategory

- `categoryId`
- `name`
- `sortOrder`
- `usageCount`
- `bannerUsageCount`
- `recommendationUsageCount`

### 2.2 PlacementItem

- `itemId`
- `slotCode`
- `gameId`
- `badgeText`
- `title`
- `subtitle`
- `coverUrl`
- `status`
- `startAt`
- `endAt`
- `sortOrder`

### 2.3 RecommendationItem

- `itemId`
- `slotCode`
- `gameId`
- `cardCategory`
- `cardTitle`
- `coverUrl`
- `articleTag`
- `articleTitle`
- `articleBody`
- `actionText`
- `status`
- `startAt`
- `endAt`
- `sortOrder`

## 3. 状态流转设计

### 3.1 推荐项状态

- `DRAFT`
- `PUBLISHED`
- `PAUSED`
- 派生展示状态：
  - `LIVE`
  - `SCHEDULED`
  - `EXPIRED`

### 3.2 规则

- `PUBLISHED` 且在时间窗内才可前端展示
- `PAUSED` 必须立即停止展示
- `DRAFT` 不允许前端展示
- 只有 `APPROVED` 游戏可被加入广告位与推荐列表

## 4. 权限设计

- 查看推荐运营：`OPS_RECOMMEND_READ`
- 编辑分类：`OPS_RECOMMEND_CATEGORY_WRITE`
- 编辑广告位：`OPS_RECOMMEND_SLOT_WRITE`
- 编辑推荐列表：`OPS_RECOMMEND_CONTENT_WRITE`
- 删除操作必须管理员或主管权限

## 5. 接口设计

- `GET /admin/ops/discover/config`
- `PUT /admin/ops/discover/config`
- `POST /admin/ops/discover/content-items/batch-status`
- `GET /admin/ops/discover/categories`
- `POST /admin/ops/discover/categories`
- `PUT /admin/ops/discover/categories/{id}`
- `DELETE /admin/ops/discover/categories/{id}`

接口校验：

- 分类名唯一
- 广告位状态必须在枚举内
- `endAt >= startAt`
- 引用分类必须存在
- 引用游戏必须是已通过状态
- 批量状态操作必须校验 item 是否存在、是否属于受管投放位、是否满足发布时间窗规则

## 6. 数据库设计

- 复用：
  - `ops_discover_category`
  - `ops_content_slot`
  - `ops_content_item`
- 必须保障：
  - `slot_id + sort_order` 可排序
  - `status + start_at + end_at` 支持前端查询
  - 分类删除前校验引用

## 7. 异常场景设计

- 分类被广告 / 推荐项引用后删除
- 排期结束时间早于开始时间
- 重复保存导致顺序错乱
- 暂停项仍被前端展示
- 引用了未审核通过游戏

## 8. 操作日志设计

- `RECOMMEND_CATEGORY_CREATED`
- `RECOMMEND_CATEGORY_UPDATED`
- `RECOMMEND_CATEGORY_DELETED`
- `PLACEMENT_ITEM_SAVED`
- `PLACEMENT_ITEM_STATUS_CHANGED`
- `RECOMMEND_ITEM_SAVED`
- `DISCOVER_CONFIG_SAVE`
- `DISCOVER_ITEM_BATCH_STATUS`

## 9. 测试用例设计

- 创建分类成功
- 分类重名失败
- 被引用分类删除失败
- 创建广告位并排期成功
- 结束时间早于开始时间失败
- 暂停后不再展示
- 只有已通过游戏可进入推荐位
- 批量发布未审核通过游戏失败
- 批量操作后生成审计日志

## 10. 验收标准

- 推荐分类、广告位、推荐列表完全分治
- 状态、排期、暂停能力完整
- 支持批量发布、批量暂停、转草稿，并有二次确认
- 所有敏感操作有日志
- 非法资产不能进入前端展示链路
