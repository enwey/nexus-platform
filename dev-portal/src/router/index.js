import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCurrentUser } from '../api'
import { ltGlobal } from '../i18n'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  {
    path: '/',
    component: () => import('../layouts/DeveloperLayout.vue'),
    meta: { requiresAuth: true, requiresDeveloper: true },
    children: [
      {
        path: 'dashboard',
        name: 'DeveloperDashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: {
          navKey: '/dashboard',
          title: ['开发者工作台', '開發者工作台', 'Developer Dashboard'],
          subtitle: ['跟进审核、版本和接入状态。', '追蹤審核、版本與接入狀態。', 'Track review status, releases, and onboarding progress.']
        }
      },

      { path: 'games', redirect: '/games/list' },
      {
        path: 'games/list',
        name: 'DeveloperGamesList',
        component: () => import('../views/Games.vue'),
        meta: {
          navKey: '/games/list',
          title: ['我的游戏', '我的遊戲', 'My Games'],
          subtitle: ['管理游戏主资料、素材库、运行时展示资料和运营资料。', '管理遊戲主資料、素材庫、運行時展示資料和營運資料。', 'Manage game metadata, media library, runtime display assets, and ops profile.']
        }
      },
      {
        path: 'games/:gameId',
        name: 'DeveloperGameDetail',
        component: () => import('../views/Games.vue'),
        meta: {
          navKey: '/games/list',
          title: ['游戏详情', '遊戲詳情', 'Game Detail'],
          subtitle: ['通过详情工作台维护单个游戏的完整内容资产。', '透過詳情工作台維護單個遊戲的完整內容資產。', 'Use the detail workspace to maintain a single game asset end to end.']
        }
      },

      { path: 'releases', redirect: '/releases/list' },
      {
        path: 'releases/list',
        name: 'DeveloperReleaseList',
        component: () => import('../views/ReleaseCenter.vue'),
        meta: {
          navKey: '/releases/list',
          title: ['版本发布', '版本發布', 'Release Center'],
          subtitle: ['上传首包、管理版本、查看异步处理任务并发起提审。', '上傳首包、管理版本、查看非同步處理任務並發起提審。', 'Upload the first package, manage versions, inspect async tasks, and submit for review.']
        }
      },
      {
        path: 'releases/game/:gameId',
        name: 'DeveloperReleaseDetail',
        component: () => import('../views/ReleaseCenter.vue'),
        meta: {
          navKey: '/releases/list',
          title: ['版本详情', '版本詳情', 'Release Detail'],
          subtitle: ['按游戏查看版本记录、发布前检查、申诉与回滚能力。', '按遊戲查看版本記錄、發佈前檢查、申訴與回滾能力。', 'Inspect version history, preflight checks, appeals, and rollback by game.']
        }
      },

      { path: 'reviews', redirect: '/reviews/certification' },
      {
        path: 'reviews/certification',
        name: 'DeveloperReviewCertification',
        component: () => import('../views/Account.vue'),
        meta: {
          navKey: '/reviews/certification',
          focusSection: 'certification',
          title: ['审核与资质', '審核與資質', 'Reviews & Certification'],
          subtitle: ['查看资质状态、审核记录与平台审核结果。', '查看資質狀態、審核記錄與平台審核結果。', 'Review certification status, review records, and platform decisions.']
        }
      },
      {
        path: 'reviews/appeals',
        name: 'DeveloperReviewAppeals',
        component: () => import('../views/ReleaseCenter.vue'),
        meta: {
          navKey: '/reviews/certification',
          focusPanel: 'appeals',
          title: ['申诉记录', '申訴記錄', 'Appeal Records'],
          subtitle: ['跟进被驳回版本的申诉状态和复审反馈。', '追蹤被駁回版本的申訴狀態和複審回饋。', 'Track appeal status and re-review feedback for rejected versions.']
        }
      },

      { path: 'support', redirect: '/support/notices' },
      {
        path: 'support/notices',
        name: 'DeveloperNoticeList',
        component: () => import('../views/Notifications.vue'),
        meta: {
          navKey: '/support/notices',
          title: ['通知与工单', '通知與工單', 'Notices & Tickets'],
          subtitle: ['接收平台通知、查看生效状态并进入工单处理。', '接收平台通知、查看生效狀態並進入工單處理。', 'Receive platform notices, inspect their status, and move into support workflows.']
        }
      },
      {
        path: 'support/notices/:noticeId',
        name: 'DeveloperNoticeDetail',
        component: () => import('../views/Notifications.vue'),
        meta: {
          navKey: '/support/notices',
          title: ['通知详情', '通知詳情', 'Notice Detail'],
          subtitle: ['查看单条平台通知的完整内容和动作链接。', '查看單條平台通知的完整內容和動作連結。', 'Inspect a single platform notice and its action link.']
        }
      },
      {
        path: 'support/tickets',
        name: 'DeveloperTicketList',
        component: () => import('../views/FeedbackCenter.vue'),
        meta: {
          navKey: '/support/notices',
          title: ['工单列表', '工單列表', 'Ticket List'],
          subtitle: ['集中处理发布、审核、运行时和治理问题单。', '集中處理發布、審核、運行時和治理問題單。', 'Manage release, review, runtime, and governance support tickets.']
        }
      },
      {
        path: 'support/tickets/:ticketId',
        name: 'DeveloperTicketDetail',
        component: () => import('../views/FeedbackCenter.vue'),
        meta: {
          navKey: '/support/notices',
          title: ['工单详情', '工單詳情', 'Ticket Detail'],
          subtitle: ['查看处理记录、补充材料并继续回复平台。', '查看處理記錄、補充材料並繼續回覆平台。', 'Inspect handling history, add more context, and reply to platform follow-up.']
        }
      },

      { path: 'workspace', redirect: '/workspace/account' },
      {
        path: 'workspace/account',
        name: 'DeveloperWorkspaceAccount',
        component: () => import('../views/Account.vue'),
        meta: {
          navKey: '/workspace/account',
          focusSection: 'profile',
          title: ['团队与账号', '團隊與帳號', 'Team & Account'],
          subtitle: ['维护个人资料、安全设置和账号基础信息。', '維護個人資料、安全設定和帳號基礎資訊。', 'Manage profile, security settings, and account basics.']
        }
      },
      {
        path: 'workspace/team',
        name: 'DeveloperWorkspaceTeam',
        component: () => import('../views/Account.vue'),
        meta: {
          navKey: '/workspace/account',
          focusSection: 'team',
          title: ['团队成员', '團隊成員', 'Team Members'],
          subtitle: ['管理成员角色、启停状态和团队操作日志。', '管理成員角色、啟停狀態和團隊操作日誌。', 'Manage member roles, active status, and team activity logs.']
        }
      },
      {
        path: 'workspace/keys',
        name: 'DeveloperWorkspaceKeys',
        component: () => import('../views/Account.vue'),
        meta: {
          navKey: '/workspace/account',
          focusSection: 'keys',
          title: ['API 凭证', 'API 憑證', 'API Keys'],
          subtitle: ['创建、轮换和撤销上传与发布使用的 API 凭证。', '建立、輪換和撤銷上傳與發布使用的 API 憑證。', 'Create, rotate, and revoke API keys for upload and release workflows.']
        }
      },

      {
        path: 'health',
        redirect: '/health/overview'
      },
      {
        path: 'health/overview',
        name: 'DeveloperHealthOverview',
        component: () => import('../views/Insights.vue'),
        meta: {
          navKey: '/health/overview',
          title: ['数据与健康', '數據與健康', 'Insights & Health'],
          subtitle: ['查看发布健康、经营趋势、运行时异常和审核结果变化。', '查看發布健康、經營趨勢、運行時異常和審核結果變化。', 'Track release health, operating trends, runtime issues, and review outcomes.']
        }
      },

      { path: 'docs', redirect: '/docs/list' },
      {
        path: 'docs/list',
        name: 'DeveloperDocsList',
        component: () => import('../views/DeveloperDocs.vue'),
        meta: {
          navKey: '/docs/list',
          title: ['文档中心', '文件中心', 'Docs Center'],
          subtitle: ['沉淀接入指南、提审清单和案例复盘，并保留版本历史。', '沉澱接入指南、提審清單和案例複盤，並保留版本歷史。', 'Preserve integration guides, submission checklists, and case studies with version history.']
        }
      },
      {
        path: 'docs/:docId',
        name: 'DeveloperDocDetail',
        component: () => import('../views/DeveloperDocs.vue'),
        meta: {
          navKey: '/docs/list',
          title: ['文档详情', '文件詳情', 'Document Detail'],
          subtitle: ['查看单篇文档的正文、版本历史和变更说明。', '查看單篇文件的正文、版本歷史和變更說明。', 'Inspect a document body, its version history, and change notes.']
        }
      }
    ]
  },

  { path: '/games/upload', redirect: '/releases/list' },
  { path: '/games', redirect: '/games/list' },
  { path: '/releases', redirect: '/releases/list' },
  { path: '/insights', redirect: '/health/overview' },
  { path: '/notifications', redirect: '/support/notices' },
  { path: '/feedback', redirect: '/support/tickets' },
  { path: '/docs', redirect: '/docs/list' },
  { path: '/account', redirect: '/workspace/account' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

let restoringSession = null

async function ensureSession(userStore) {
  if (userStore.user || !userStore.token) {
    return Boolean(userStore.user)
  }

  if (!restoringSession) {
    restoringSession = getCurrentUser()
      .then((res) => {
        userStore.setUser(res.data)
        return true
      })
      .catch(() => {
        userStore.logout()
        return false
      })
      .finally(() => {
        restoringSession = null
      })
  }

  return restoringSession
}

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  const hasSession = await ensureSession(userStore)
  const canUseDeveloperPortal = userStore.user?.role === 'DEVELOPER' || userStore.user?.role === 'ADMIN'

  if (to.meta.requiresAuth && !hasSession) {
    return '/login'
  }

  if (to.meta.requiresDeveloper && hasSession && !canUseDeveloperPortal) {
    userStore.logout()
    ElMessage.warning(ltGlobal(
      '当前账号没有开发者后台权限，请使用开发者账号登录',
      '當前帳號沒有開發者後台權限，請使用開發者帳號登入',
      'Current account does not have developer portal access. Please sign in with a developer account.'
    ))
    return '/login'
  }

  if ((to.path === '/login' || to.path === '/register') && hasSession && canUseDeveloperPortal) {
    return '/dashboard'
  }

  return true
})

export default router
