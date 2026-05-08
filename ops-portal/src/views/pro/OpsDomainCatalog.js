export const domainCatalog = {
  'library-ops': {
    title: ['游戏库运营', '遊戲庫營運', 'Game Library Ops'],
    subtitle: ['围绕游戏库清单、分类与入库治理来组织运营动作。', '圍繞遊戲庫清單、分類與入庫治理來組織營運動作。', 'Organize operations around the game library, taxonomy, and intake governance.'],
    sections: [
      {
        key: 'library-governance',
        title: ['游戏库清单治理', '遊戲庫清單治理', 'Library Governance'],
        description: ['管理游戏上架状态、资料完整度、展示控制与批量治理。', '管理遊戲上架狀態、資料完整度、展示控制與批量治理。', 'Manage listing status, completeness, visibility control, and batch governance.']
      },
      {
        key: 'category-routing',
        title: ['分类与入库路由', '分類與入庫路由', 'Category Routing'],
        description: ['维护游戏库分类、分类顺序和入库标签。', '維護遊戲庫分類、分類順序和入庫標籤。', 'Maintain library categories, sort order, and intake labels.']
      }
    ]
  },
  'discover-ops': {
    title: ['发现页运营', '發現頁營運', 'Discover Ops'],
    subtitle: ['将发现 Hero、分类、Banner 和新手推荐池拆开配置。', '將發現 Hero、分類、Banner 和新手推薦池拆開配置。', 'Separate hero, categories, banners, and starter pools for discover operations.'],
    sections: [
      {
        key: 'hero-and-pools',
        title: ['新手必玩 / 大家都在玩', '新手必玩 / 大家都在玩', 'Starter & Everyone Pools'],
        description: ['配置新手必玩、大家都在玩、排行榜推荐池和 Hero AppID。', '配置新手必玩、大家都在玩、排行榜推薦池和 Hero AppID。', 'Configure newbie picks, everyone-is-playing pools, ranked pools, and hero AppID.']
      },
      {
        key: 'discover-hero',
        title: ['发现 Hero 与区块开关', '發現 Hero 與區塊開關', 'Discover Hero & Slot Controls'],
        description: ['查看发现 Hero、区块总开关、发布单与实验位。', '查看發現 Hero、區塊總開關、發佈單與實驗位。', 'Inspect discover hero, slot kill switches, release orders, and experiments.']
      },
      {
        key: 'discover-categories',
        title: ['发现分类', '發現分類', 'Discover Categories'],
        description: ['维护发现页分类、引用关系和分类排序。', '維護發現頁分類、引用關係和分類排序。', 'Maintain discover categories, references, and ordering.']
      },
      {
        key: 'library-top-banner',
        title: ['Library Top Banner', 'Library Top Banner', 'Library Top Banner'],
        description: ['管理游戏库首屏 Banner 与馆内顶部投放。', '管理遊戲庫首屏 Banner 與館內頂部投放。', 'Manage library top banners and in-library hero placements.']
      },
      {
        key: 'discover-top-banner',
        title: ['发现页 Hero Banner', '發現頁 Hero Banner', 'Discover Hero Banner'],
        description: ['管理发现页首屏 Banner、排期和状态回收。', '管理發現頁首屏 Banner、排期和狀態回收。', 'Manage discover hero banners, scheduling, and state recovery.']
      }
    ]
  },
  'recommend-content': {
    title: ['推荐内容运营', '推薦內容營運', 'Recommendation Content Ops'],
    subtitle: ['把推荐社区内容、发布单和实验位从发现运营中拆开。', '把推薦社群內容、發佈單和實驗位從發現營運中拆開。', 'Split editorial recommendation content, release orders, and experiments away from discover operations.'],
    sections: [
      {
        key: 'community-content',
        title: ['推荐社区内容', '推薦社群內容', 'Community Recommendations'],
        description: ['维护推荐列表、图文详情、状态和排序。', '維護推薦列表、圖文詳情、狀態和排序。', 'Manage recommendation lists, editorial details, status, and ordering.']
      },
      {
        key: 'publish-and-experiments',
        title: ['发布单与实验位', '發佈單與實驗位', 'Publish Orders & Experiments'],
        description: ['处理推荐变更发布单、预览、实验流量和回收。', '處理推薦變更發佈單、預覽、實驗流量和回收。', 'Handle recommendation release orders, previews, experiment traffic, and recovery.']
      }
    ]
  },
  'launch-ads': {
    title: ['启动广告', '啟動廣告', 'Launch Ads'],
    subtitle: ['围绕启动广告与营销投放总表来做列表和详情管理。', '圍繞啟動廣告與營銷投放總表來做列表和詳情管理。', 'Use launch ads and the marketing master table as the operational workspace.'],
    sections: [
      {
        key: 'launch-ads',
        title: ['启动广告与营销投放', '啟動廣告與營銷投放', 'Launch Ads & Marketing Delivery'],
        description: ['查看启动广告、活动会场、弹窗、任务与礼包码的总投放面板。', '查看啟動廣告、活動會場、彈窗、任務與禮包碼的總投放面板。', 'Inspect launch ads, campaign hubs, popups, tasks, and gift-code delivery from one panel.']
      }
    ]
  },
  'client-control': {
    title: ['客户端控制', '客戶端控制', 'Client Control'],
    subtitle: ['把版本更新控制、渠道规则、功能开关和熔断拆成控制详情。', '把版本更新控制、渠道規則、功能開關和熔斷拆成控制詳情。', 'Split runtime update rules, channel policies, toggles, and breakers into explicit control pages.'],
    sections: [
      {
        key: 'version-and-channel',
        title: ['版本更新控制 / 渠道配置', '版本更新控制 / 渠道配置', 'Version Update & Channels'],
        description: ['查看最低版本、强更策略、渠道灰度和渠道级更新规则。', '查看最低版本、強更策略、渠道灰度和渠道級更新規則。', 'Inspect minimum versions, force update policy, gray releases, and channel-level rules.']
      },
      {
        key: 'feature-and-breakers',
        title: ['功能开关 / 熔断', '功能開關 / 熔斷', 'Feature Toggles & Circuit Breakers'],
        description: ['查看功能开关、兼容黑名单和页面级熔断。', '查看功能開關、相容黑名單和頁面級熔斷。', 'Inspect feature toggles, compatibility blacklists, and page circuit breakers.']
      },
      {
        key: 'ab-and-gray',
        title: ['AB 实验 / 灰度计划', 'AB 實驗 / 灰度計畫', 'AB Experiments & Gray Plans'],
        description: ['查看 AB 实验、流量切分和精细灰度发布计划。', '查看 AB 實驗、流量切分和精細灰度發佈計畫。', 'Inspect AB experiments, traffic allocation, and fine-grained gray release plans.']
      }
    ]
  },
  'workspace-ops': {
    title: ['工单与通知', '工單與通知', 'Tickets & Notices'],
    subtitle: ['围绕客服工单和通知投放拆成两个详情工作区。', '圍繞客服工單和通知投放拆成兩個詳情工作區。', 'Split ticket handling and notice delivery into two dedicated workspaces.'],
    sections: [
      {
        key: 'tickets',
        title: ['工单处理台', '工單處理台', 'Ticket Desk'],
        description: ['处理开发者工单、消息回复和结案状态。', '處理開發者工單、訊息回覆和結案狀態。', 'Handle developer tickets, message replies, and resolution states.']
      },
      {
        key: 'notices',
        title: ['通知投放台', '通知投放台', 'Notice Delivery Desk'],
        description: ['管理系统通知、审核通知、发布通知和风险通知。', '管理系統通知、審核通知、發布通知和風險通知。', 'Manage system, review, publish, and risk notices.']
      }
    ]
  },
  'risk-logs': {
    title: ['风控与日志', '風控與日誌', 'Risk & Logs'],
    subtitle: ['把风险事件、审计日志、验证码日志拆成可回溯的详情入口。', '把風險事件、審計日誌、驗證碼日誌拆成可回溯的詳情入口。', 'Split incidents, audit logs, and verification logs into traceable detail entries.'],
    sections: [
      {
        key: 'risk-and-audit',
        title: ['风险事件 / 审计日志', '風險事件 / 審計日誌', 'Risk Incidents & Audit'],
        description: ['查看风险事件处置流、登录异常、访问规则和审计日志。', '查看風險事件處置流、登入異常、訪問規則和審計日誌。', 'Inspect incident handling, login anomalies, access rules, and audit logs.']
      },
      {
        key: 'verification-logs',
        title: ['验证码日志', '驗證碼日誌', 'Verification Logs'],
        description: ['查看验证码发送记录、失败原因和来源端。', '查看驗證碼發送記錄、失敗原因和來源端。', 'Inspect verification code delivery logs, failure reasons, and sources.']
      }
    ]
  },
  'base-config': {
    title: ['基础配置', '基礎配置', 'Base Configuration'],
    subtitle: ['把法务链接、审核模板、权限与字典配置拆成配置详情页。', '把法務連結、審核模板、權限與字典配置拆成配置詳情頁。', 'Split legal links, review templates, permissions, and dictionaries into config detail pages.'],
    sections: [
      {
        key: 'platform-settings',
        title: ['平台权限 / 审核模板', '平台權限 / 審核模板', 'Platform Permissions & Templates'],
        description: ['查看角色权限、数据范围、通知模板、字典、审批模板和敏感策略。', '查看角色權限、資料範圍、通知模板、字典、審批模板和敏感策略。', 'Inspect role permissions, data scopes, notice templates, dictionaries, approval templates, and sensitive policies.']
      },
      {
        key: 'legal-links',
        title: ['法务链接', '法務連結', 'Legal Links'],
        description: ['查看当前 Terms / Privacy 对外配置入口，便于主线继续接后台化。', '查看當前 Terms / Privacy 對外配置入口，便於主線繼續接後台化。', 'Inspect current Terms / Privacy public endpoints before mainline backend-izes them.']
      }
    ]
  }
}

export const getDomainMeta = (domainKey) => domainCatalog[domainKey]
