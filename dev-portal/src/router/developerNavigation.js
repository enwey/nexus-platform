export const developerNavSections = [
  {
    type: 'item',
    index: '/dashboard',
    label: ['工作台', '工作台', 'Dashboard']
  },
  {
    type: 'group',
    index: 'games',
    label: ['我的游戏', '我的遊戲', 'My Games'],
    items: [
      { index: '/games/list', label: ['游戏列表', '遊戲列表', 'Game List'] }
    ]
  },
  {
    type: 'group',
    index: 'releases',
    label: ['版本发布', '版本發布', 'Release Center'],
    items: [
      { index: '/releases/list', label: ['版本列表', '版本列表', 'Release List'] }
    ]
  },
  {
    type: 'group',
    index: 'reviews',
    label: ['审核与资质', '審核與資質', 'Reviews & Certification'],
    items: [
      { index: '/reviews/certification', label: ['资质审核记录', '資質審核記錄', 'Certification Reviews'] },
      { index: '/reviews/appeals', label: ['申诉复审', '申訴複審', 'Appeals'] }
    ]
  },
  {
    type: 'group',
    index: 'support',
    label: ['通知与工单', '通知與工單', 'Notices & Tickets'],
    items: [
      { index: '/support/notices', label: ['平台通知', '平台通知', 'Notices'] },
      { index: '/support/tickets', label: ['工单列表', '工單列表', 'Tickets'] }
    ]
  },
  {
    type: 'group',
    index: 'workspace',
    label: ['团队与账号', '團隊與帳號', 'Team & Account'],
    items: [
      { index: '/workspace/account', label: ['账号资料', '帳號資料', 'Account'] },
      { index: '/workspace/team', label: ['团队成员', '團隊成員', 'Team'] },
      { index: '/workspace/keys', label: ['API 凭证', 'API 憑證', 'API Keys'] }
    ]
  },
  {
    type: 'group',
    index: 'health',
    label: ['数据与健康', '數據與健康', 'Insights & Health'],
    items: [
      { index: '/health/overview', label: ['健康看板', '健康看板', 'Health Board'] }
    ]
  },
  {
    type: 'group',
    index: 'docs',
    label: ['文档中心', '文件中心', 'Docs Center'],
    items: [
      { index: '/docs/list', label: ['文档列表', '文件列表', 'Document List'] }
    ]
  }
]

export function resolveDeveloperPageTitle(route) {
  const matchedWithTitle = [...route.matched].reverse().find((record) => record.meta?.title)
  return matchedWithTitle?.meta?.title || ['开发者后台', '開發者後台', 'Developer Portal']
}
