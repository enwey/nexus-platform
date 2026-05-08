export const proNavSections = [
  {
    type: 'item',
    index: '/pro/dashboard',
    label: ['工作台', '工作台', 'Dashboard']
  },
  {
    type: 'group',
    index: 'game-version',
    label: ['游戏与版本', '遊戲與版本', 'Games & Versions'],
    items: [
      { index: '/pro/game-version/games/list', label: ['游戏列表', '遊戲列表', 'Game List'] },
      { index: '/pro/game-version/versions/list', label: ['版本列表', '版本列表', 'Version List'] }
    ]
  },
  {
    type: 'group',
    index: 'review-center',
    label: ['审核中心', '審核中心', 'Review Center'],
    items: [
      { index: '/pro/review-center/games/list', label: ['游戏审核', '遊戲審核', 'Game Reviews'] },
      { index: '/pro/review-center/versions/list', label: ['版本审核', '版本審核', 'Version Reviews'] },
      { index: '/pro/review-center/certifications/list', label: ['资质审核', '資質審核', 'Certification Reviews'] },
      { index: '/pro/review-center/appeals/list', label: ['申诉复审', '申訴複審', 'Appeal Reviews'] }
    ]
  },
  {
    type: 'group',
    index: 'library-ops',
    label: ['游戏库运营', '遊戲庫營運', 'Library Ops'],
    items: [
      { index: '/pro/library-ops/slots/list', label: ['运营位列表', '營運位列表', 'Slot List'] }
    ]
  },
  {
    type: 'group',
    index: 'discover-ops',
    label: ['发现页运营', '發現頁營運', 'Discover Ops'],
    items: [
      { index: '/pro/discover-ops/slots/list', label: ['运营位列表', '營運位列表', 'Discover Slot List'] },
      { index: '/pro/discover-ops/categories/list', label: ['发现分类列表', '發現分類列表', 'Category List'] },
      { index: '/pro/discover-ops/content/list', label: ['发现内容列表', '發現內容列表', 'Content List'] }
    ]
  },
  {
    type: 'group',
    index: 'recommendation-content',
    label: ['推荐内容运营', '推薦內容營運', 'Recommendation Content'],
    items: [
      { index: '/pro/recommendation-content/items/list', label: ['推荐内容列表', '推薦內容列表', 'Recommendation List'] }
    ]
  },
  {
    type: 'group',
    index: 'launch-ads',
    label: ['启动广告', '啟動廣告', 'Launch Ads'],
    items: [
      { index: '/pro/launch-ads/list', label: ['启动广告列表', '啟動廣告列表', 'Launch Ad List'] }
    ]
  },
  {
    type: 'group',
    index: 'client-control',
    label: ['客户端控制', '客戶端控制', 'Client Controls'],
    items: [
      { index: '/pro/client-control/version-policies/list', label: ['版本策略列表', '版本策略列表', 'Version Policy List'] },
      { index: '/pro/client-control/channels/list', label: ['渠道配置列表', '渠道配置列表', 'Channel Config List'] },
      { index: '/pro/client-control/feature-toggles/list', label: ['功能开关列表', '功能開關列表', 'Feature Toggle List'] },
      { index: '/pro/client-control/gray-rollouts/list', label: ['灰度配置列表', '灰度配置列表', 'Gray Rollout List'] }
    ]
  },
  {
    type: 'group',
    index: 'developer-management',
    label: ['开发者管理', '開發者管理', 'Developer Management'],
    items: [
      { index: '/pro/developer-management/developers/list', label: ['开发者列表', '開發者列表', 'Developer List'] }
    ]
  },
  {
    type: 'group',
    index: 'ticket-notice',
    label: ['工单与通知', '工單與通知', 'Tickets & Notices'],
    items: [
      { index: '/pro/ticket-notice/tickets/list', label: ['工单列表', '工單列表', 'Ticket List'] },
      { index: '/pro/ticket-notice/notices/list', label: ['通知列表', '通知列表', 'Notice List'] },
      { index: '/pro/ticket-notice/templates/list', label: ['通知模板列表', '通知模板列表', 'Notice Template List'] }
    ]
  },
  {
    type: 'group',
    index: 'risk-audit',
    label: ['风控与日志', '風控與日誌', 'Risk & Logs'],
    items: [
      { index: '/pro/risk-audit/incidents/list', label: ['风险事件列表', '風險事件列表', 'Risk Incident List'] },
      { index: '/pro/risk-audit/login-anomalies/list', label: ['登录异常列表', '登入異常列表', 'Login Anomaly List'] },
      { index: '/pro/risk-audit/audit-logs/list', label: ['审计日志列表', '審計日誌列表', 'Audit Log List'] }
    ]
  },
  {
    type: 'group',
    index: 'foundation-config',
    label: ['基础配置', '基礎配置', 'Foundation Config'],
    items: [
      { index: '/pro/foundation-config/legal', label: ['法务链接配置', '法務連結配置', 'Legal Config'] },
      { index: '/pro/foundation-config/categories/list', label: ['分类字典列表', '分類字典列表', 'Category Dictionary List'] },
      { index: '/pro/foundation-config/review-templates/list', label: ['审核模板列表', '審核模板列表', 'Review Template List'] }
    ]
  }
]

export function resolveProPageTitle(route) {
  const matchedWithTitle = [...route.matched].reverse().find((record) => record.meta?.pageTitle)
  return matchedWithTitle?.meta?.pageTitle || ['运营后台', '營運後台', 'Operations Portal']
}
