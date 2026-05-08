import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCurrentUser } from '../api'
import { useUserStore } from '../stores/user'
import { ltGlobal } from '../i18n'

const tri = (zhCN, zhTW, en) => [zhCN, zhTW, en]

const withOpsMeta = (pageTitle, activeMenu) => ({
  requiresAuth: true,
  requiresAdmin: true,
  pageTitle,
  activeMenu
})

const listRoute = (path, name, catalogKey, title, activeMenu) => ({
  path,
  name,
  component: () => import('../views/pro/catalog/CatalogListPage.vue'),
  props: { catalogKey },
  meta: withOpsMeta(title, activeMenu)
})

const detailRoute = (path, name, catalogKey, paramKey, title, activeMenu) => ({
  path,
  name,
  component: () => import('../views/pro/catalog/CatalogDetailPage.vue'),
  props: (route) => ({ catalogKey, detailId: route.params[paramKey] }),
  meta: withOpsMeta(title, activeMenu)
})

const configRoute = (path, name, configKey, title, activeMenu) => ({
  path,
  name,
  component: () => import('../views/pro/catalog/ConfigPage.vue'),
  props: { configKey },
  meta: withOpsMeta(title, activeMenu)
})

const GameListPage = () => import('../views/pro/GameListPage.vue')
const GameDetailPage = () => import('../views/pro/GameDetailPage.vue')
const VersionListPage = () => import('../views/pro/VersionListPage.vue')
const VersionDetailPage = () => import('../views/pro/VersionDetailPage.vue')
const VersionReviewListPage = () => import('../views/pro/VersionReviewListPage.vue')
const VersionReviewDetailPage = () => import('../views/pro/VersionReviewDetailPage.vue')
const CertificationReviewListPage = () => import('../views/pro/CertificationReviewListPage.vue')
const CertificationReviewDetailPage = () => import('../views/pro/CertificationReviewDetailPage.vue')
const DeveloperListPage = () => import('../views/pro/DeveloperListPage.vue')
const DeveloperDetailPage = () => import('../views/pro/DeveloperDetailPage.vue')
const LibraryOpsDetailPage = () => import('../views/pro/RecommendModule.vue')
const DiscoverCategoryDetailPage = () => import('../views/pro/RecommendCategoryModule.vue')
const DiscoverContentDetailPage = () => import('../views/pro/RecommendBannerModule.vue')
const RecommendationItemDetailPage = () => import('../views/pro/RecommendCommunityModule.vue')
const LaunchAdDetailPage = () => import('../views/pro/MarketingModule.vue')
const ClientControlDetailPage = () => import('../views/pro/RuntimeModule.vue')
const TicketDetailPage = () => import('../views/pro/TicketModule.vue')
const NoticeDetailPage = () => import('../views/pro/NoticeModule.vue')
const RiskDetailPage = () => import('../views/pro/RiskModule.vue')
const SmsDetailPage = () => import('../views/pro/SmsModule.vue')
const FoundationDetailPage = () => import('../views/pro/SettingsModule.vue')

const routes = [
  { path: '/', redirect: '/pro/dashboard' },
  { path: '/login', name: 'OpsLogin', component: () => import('../views/Login.vue') },
  {
    path: '/pro',
    component: () => import('../layouts/ProLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: '/pro/dashboard' },
      {
        path: 'dashboard',
        name: 'OpsDashboard',
        component: () => import('../views/pro/DashboardModule.vue'),
        meta: withOpsMeta(tri('工作台', '工作台', 'Dashboard'), '/pro/dashboard')
      },

      {
        path: 'game-version',
        redirect: '/pro/game-version/games/list',
        children: [
          { path: 'games/list', name: 'OpsGameList', component: GameListPage, meta: withOpsMeta(tri('游戏列表', '遊戲列表', 'Game List'), '/pro/game-version/games/list') },
          { path: 'games/:gameId', name: 'OpsGameDetail', component: GameDetailPage, props: (route) => ({ gameId: route.params.gameId }), meta: withOpsMeta(tri('游戏详情', '遊戲詳情', 'Game Detail'), '/pro/game-version/games/list') },
          { path: 'versions/list', name: 'OpsVersionList', component: VersionListPage, meta: withOpsMeta(tri('版本列表', '版本列表', 'Version List'), '/pro/game-version/versions/list') },
          { path: 'versions/:versionId', name: 'OpsVersionDetail', component: VersionDetailPage, props: (route) => ({ versionId: route.params.versionId }), meta: withOpsMeta(tri('版本详情', '版本詳情', 'Version Detail'), '/pro/game-version/versions/list') }
        ]
      },

      {
        path: 'review-center',
        redirect: '/pro/review-center/versions/list',
        children: [
          listRoute('games/list', 'OpsGameReviewList', 'gameReviews', tri('游戏审核', '遊戲審核', 'Game Reviews'), '/pro/review-center/games/list'),
          detailRoute('games/:reviewId', 'OpsGameReviewDetail', 'gameReviews', 'reviewId', tri('游戏审核详情', '遊戲審核詳情', 'Game Review Detail'), '/pro/review-center/games/list'),
          { path: 'versions/list', name: 'OpsVersionReviewList', component: VersionReviewListPage, meta: withOpsMeta(tri('版本审核', '版本審核', 'Version Reviews'), '/pro/review-center/versions/list') },
          { path: 'versions/:reviewId', name: 'OpsVersionReviewDetail', component: VersionReviewDetailPage, props: (route) => ({ reviewId: route.params.reviewId }), meta: withOpsMeta(tri('版本审核详情', '版本審核詳情', 'Version Review Detail'), '/pro/review-center/versions/list') },
          { path: 'certifications/list', name: 'OpsCertificationReviewList', component: CertificationReviewListPage, meta: withOpsMeta(tri('资质审核', '資質審核', 'Certification Reviews'), '/pro/review-center/certifications/list') },
          { path: 'certifications/:reviewId', name: 'OpsCertificationReviewDetail', component: CertificationReviewDetailPage, props: (route) => ({ reviewId: route.params.reviewId }), meta: withOpsMeta(tri('资质审核详情', '資質審核詳情', 'Certification Review Detail'), '/pro/review-center/certifications/list') },
          listRoute('appeals/list', 'OpsAppealReviewList', 'appealReviews', tri('申诉复审', '申訴複審', 'Appeal Reviews'), '/pro/review-center/appeals/list'),
          detailRoute('appeals/:appealId', 'OpsAppealReviewDetail', 'appealReviews', 'appealId', tri('申诉复审详情', '申訴複審詳情', 'Appeal Review Detail'), '/pro/review-center/appeals/list')
        ]
      },

      {
        path: 'library-ops',
        redirect: '/pro/library-ops/slots/list',
        children: [
          listRoute('slots/list', 'OpsLibrarySlotList', 'librarySlots', tri('游戏库运营位列表', '遊戲庫營運位列表', 'Library Slot List'), '/pro/library-ops/slots/list'),
          { path: 'slots/:slotId', name: 'OpsLibrarySlotDetail', component: LibraryOpsDetailPage, meta: withOpsMeta(tri('游戏库运营位详情', '遊戲庫營運位詳情', 'Library Slot Detail'), '/pro/library-ops/slots/list') }
        ]
      },

      {
        path: 'discover-ops',
        redirect: '/pro/discover-ops/slots/list',
        children: [
          listRoute('slots/list', 'OpsDiscoverSlotList', 'discoverSlots', tri('发现运营位列表', '發現營運位列表', 'Discover Slot List'), '/pro/discover-ops/slots/list'),
          { path: 'slots/:slotId', name: 'OpsDiscoverSlotDetail', component: LibraryOpsDetailPage, meta: withOpsMeta(tri('发现运营位详情', '發現營運位詳情', 'Discover Slot Detail'), '/pro/discover-ops/slots/list') },
          listRoute('categories/list', 'OpsDiscoverCategoryList', 'discoverCategories', tri('发现分类列表', '發現分類列表', 'Discover Category List'), '/pro/discover-ops/categories/list'),
          { path: 'categories/:categoryId', name: 'OpsDiscoverCategoryDetail', component: DiscoverCategoryDetailPage, meta: withOpsMeta(tri('发现分类详情', '發現分類詳情', 'Discover Category Detail'), '/pro/discover-ops/categories/list') },
          listRoute('content/list', 'OpsDiscoverContentList', 'discoverContent', tri('发现内容列表', '發現內容列表', 'Discover Content List'), '/pro/discover-ops/content/list'),
          { path: 'content/:contentId', name: 'OpsDiscoverContentDetail', component: DiscoverContentDetailPage, meta: withOpsMeta(tri('发现内容详情', '發現內容詳情', 'Discover Content Detail'), '/pro/discover-ops/content/list') }
        ]
      },

      {
        path: 'recommendation-content',
        redirect: '/pro/recommendation-content/items/list',
        children: [
          listRoute('items/list', 'OpsRecommendationItemList', 'recommendationItems', tri('推荐内容列表', '推薦內容列表', 'Recommendation Item List'), '/pro/recommendation-content/items/list'),
          { path: 'items/:itemId', name: 'OpsRecommendationItemDetail', component: RecommendationItemDetailPage, meta: withOpsMeta(tri('推荐内容详情', '推薦內容詳情', 'Recommendation Item Detail'), '/pro/recommendation-content/items/list') }
        ]
      },

      {
        path: 'launch-ads',
        redirect: '/pro/launch-ads/list',
        children: [
          listRoute('list', 'OpsLaunchAdList', 'launchAds', tri('启动广告列表', '啟動廣告列表', 'Launch Ad List'), '/pro/launch-ads/list'),
          { path: ':launchAdId', name: 'OpsLaunchAdDetail', component: LaunchAdDetailPage, meta: withOpsMeta(tri('启动广告详情', '啟動廣告詳情', 'Launch Ad Detail'), '/pro/launch-ads/list') }
        ]
      },

      {
        path: 'client-control',
        redirect: '/pro/client-control/version-policies/list',
        children: [
          listRoute('version-policies/list', 'OpsVersionPolicyList', 'versionPolicies', tri('版本策略列表', '版本策略列表', 'Version Policy List'), '/pro/client-control/version-policies/list'),
          { path: 'version-policies/:policyId', name: 'OpsVersionPolicyDetail', component: ClientControlDetailPage, meta: withOpsMeta(tri('版本策略详情', '版本策略詳情', 'Version Policy Detail'), '/pro/client-control/version-policies/list') },
          listRoute('channels/list', 'OpsChannelConfigList', 'channels', tri('渠道配置列表', '渠道配置列表', 'Channel Config List'), '/pro/client-control/channels/list'),
          { path: 'channels/:channelId', name: 'OpsChannelConfigDetail', component: ClientControlDetailPage, meta: withOpsMeta(tri('渠道配置详情', '渠道配置詳情', 'Channel Config Detail'), '/pro/client-control/channels/list') },
          listRoute('feature-toggles/list', 'OpsFeatureToggleList', 'featureToggles', tri('功能开关列表', '功能開關列表', 'Feature Toggle List'), '/pro/client-control/feature-toggles/list'),
          { path: 'feature-toggles/:toggleId', name: 'OpsFeatureToggleDetail', component: ClientControlDetailPage, meta: withOpsMeta(tri('功能开关详情', '功能開關詳情', 'Feature Toggle Detail'), '/pro/client-control/feature-toggles/list') },
          listRoute('circuit-breakers/list', 'OpsCircuitBreakerList', 'circuitBreakers', tri('页面熔断列表', '頁面熔斷列表', 'Circuit Breaker List'), '/pro/client-control/circuit-breakers/list'),
          { path: 'circuit-breakers/:breakerId', name: 'OpsCircuitBreakerDetail', component: ClientControlDetailPage, meta: withOpsMeta(tri('页面熔断详情', '頁面熔斷詳情', 'Circuit Breaker Detail'), '/pro/client-control/circuit-breakers/list') },
          listRoute('compatibility/list', 'OpsCompatibilityRuleList', 'compatibilityBlocks', tri('兼容黑名单列表', '兼容黑名單列表', 'Compatibility Rule List'), '/pro/client-control/compatibility/list'),
          { path: 'compatibility/:ruleId', name: 'OpsCompatibilityRuleDetail', component: ClientControlDetailPage, meta: withOpsMeta(tri('兼容黑名单详情', '兼容黑名單詳情', 'Compatibility Rule Detail'), '/pro/client-control/compatibility/list') },
          listRoute('gray-rollouts/list', 'OpsGrayRolloutList', 'grayRollouts', tri('灰度配置列表', '灰度配置列表', 'Gray Rollout List'), '/pro/client-control/gray-rollouts/list'),
          { path: 'gray-rollouts/:rolloutId', name: 'OpsGrayRolloutDetail', component: ClientControlDetailPage, meta: withOpsMeta(tri('灰度配置详情', '灰度配置詳情', 'Gray Rollout Detail'), '/pro/client-control/gray-rollouts/list') }
        ]
      },

      {
        path: 'developer-management',
        redirect: '/pro/developer-management/developers/list',
        children: [
          { path: 'developers/list', name: 'OpsDeveloperList', component: DeveloperListPage, meta: withOpsMeta(tri('开发者列表', '開發者列表', 'Developer List'), '/pro/developer-management/developers/list') },
          { path: 'developers/:developerId', name: 'OpsDeveloperDetail', component: DeveloperDetailPage, props: (route) => ({ developerId: route.params.developerId }), meta: withOpsMeta(tri('开发者详情', '開發者詳情', 'Developer Detail'), '/pro/developer-management/developers/list') }
        ]
      },

      {
        path: 'ticket-notice',
        redirect: '/pro/ticket-notice/tickets/list',
        children: [
          listRoute('tickets/list', 'OpsTicketList', 'tickets', tri('工单列表', '工單列表', 'Ticket List'), '/pro/ticket-notice/tickets/list'),
          { path: 'tickets/:ticketId', name: 'OpsTicketDetail', component: TicketDetailPage, meta: withOpsMeta(tri('工单详情', '工單詳情', 'Ticket Detail'), '/pro/ticket-notice/tickets/list') },
          listRoute('notices/list', 'OpsNoticeList', 'notices', tri('通知列表', '通知列表', 'Notice List'), '/pro/ticket-notice/notices/list'),
          { path: 'notices/:noticeId', name: 'OpsNoticeDetail', component: NoticeDetailPage, meta: withOpsMeta(tri('通知详情', '通知詳情', 'Notice Detail'), '/pro/ticket-notice/notices/list') },
          listRoute('templates/list', 'OpsNoticeTemplateList', 'noticeTemplates', tri('通知模板列表', '通知模板列表', 'Notice Template List'), '/pro/ticket-notice/templates/list'),
          { path: 'templates/:templateId', name: 'OpsNoticeTemplateDetail', component: NoticeDetailPage, meta: withOpsMeta(tri('通知模板详情', '通知模板詳情', 'Notice Template Detail'), '/pro/ticket-notice/templates/list') }
        ]
      },

      {
        path: 'risk-audit',
        redirect: '/pro/risk-audit/incidents/list',
        children: [
          listRoute('incidents/list', 'OpsRiskIncidentList', 'incidents', tri('风险事件列表', '風險事件列表', 'Risk Incident List'), '/pro/risk-audit/incidents/list'),
          { path: 'incidents/:incidentId', name: 'OpsRiskIncidentDetail', component: RiskDetailPage, meta: withOpsMeta(tri('风险事件详情', '風險事件詳情', 'Risk Incident Detail'), '/pro/risk-audit/incidents/list') },
          listRoute('login-anomalies/list', 'OpsLoginAnomalyList', 'loginAnomalies', tri('登录异常列表', '登入異常列表', 'Login Anomaly List'), '/pro/risk-audit/login-anomalies/list'),
          { path: 'login-anomalies/:eventId', name: 'OpsLoginAnomalyDetail', component: RiskDetailPage, meta: withOpsMeta(tri('登录异常详情', '登入異常詳情', 'Login Anomaly Detail'), '/pro/risk-audit/login-anomalies/list') },
          listRoute('access-rules/list', 'OpsAccessRuleList', 'accessRules', tri('IP/设备风控规则列表', 'IP/設備風控規則列表', 'Access Rule List'), '/pro/risk-audit/access-rules/list'),
          { path: 'access-rules/:ruleId', name: 'OpsAccessRuleDetail', component: RiskDetailPage, meta: withOpsMeta(tri('IP/设备风控规则详情', 'IP/設備風控規則詳情', 'Access Rule Detail'), '/pro/risk-audit/access-rules/list') },
          listRoute('audit-logs/list', 'OpsAuditLogList', 'auditLogs', tri('审计日志列表', '審計日誌列表', 'Audit Log List'), '/pro/risk-audit/audit-logs/list'),
          { path: 'audit-logs/:logId', name: 'OpsAuditLogDetail', component: RiskDetailPage, meta: withOpsMeta(tri('审计日志详情', '審計日誌詳情', 'Audit Log Detail'), '/pro/risk-audit/audit-logs/list') },
          listRoute('sms/list', 'OpsSmsLogList', 'smsLogs', tri('短信日志列表', '短信日誌列表', 'SMS Log List'), '/pro/risk-audit/sms/list'),
          { path: 'sms/:logId', name: 'OpsSmsLogDetail', component: SmsDetailPage, meta: withOpsMeta(tri('短信日志详情', '短信日誌詳情', 'SMS Log Detail'), '/pro/risk-audit/sms/list') }
        ]
      },

      {
        path: 'foundation-config',
        redirect: '/pro/foundation-config/legal',
        children: [
          { path: 'legal', name: 'OpsLegalConfig', component: FoundationDetailPage, meta: withOpsMeta(tri('法务链接配置', '法務連結配置', 'Legal Config'), '/pro/foundation-config/legal') },
          listRoute('categories/list', 'OpsCategoryDictionaryList', 'categoryDictionary', tri('分类字典列表', '分類字典列表', 'Category Dictionary List'), '/pro/foundation-config/categories/list'),
          { path: 'categories/:categoryId', name: 'OpsCategoryDictionaryDetail', component: FoundationDetailPage, meta: withOpsMeta(tri('分类字典详情', '分類字典詳情', 'Category Dictionary Detail'), '/pro/foundation-config/categories/list') },
          listRoute('review-templates/list', 'OpsReviewTemplateList', 'reviewTemplates', tri('审核模板列表', '審核模板列表', 'Review Template List'), '/pro/foundation-config/review-templates/list'),
          { path: 'review-templates/:templateId', name: 'OpsReviewTemplateDetail', component: FoundationDetailPage, meta: withOpsMeta(tri('审核模板详情', '審核模板詳情', 'Review Template Detail'), '/pro/foundation-config/review-templates/list') },
          { path: 'upload-rules', name: 'OpsUploadRules', component: FoundationDetailPage, meta: withOpsMeta(tri('上传规则配置', '上傳規則配置', 'Upload Rules'), '/pro/foundation-config/upload-rules') }
        ]
      },

      {
        path: 'publish',
        name: 'OpsPublishCenter',
        component: () => import('../views/pro/PublishModule.vue'),
        meta: withOpsMeta(tri('发布中心', '發布中心', 'Publish Center'), '/pro/dashboard')
      }
    ]
  },

  { path: '/audit', redirect: '/pro/review-center/versions/list' },
  { path: '/audit/logs', redirect: '/pro/risk-audit/audit-logs/list' },
  { path: '/verification-codes', redirect: '/pro/risk-audit/sms/list' },
  { path: '/android-console', redirect: '/pro/client-control/version-policies/list' },
  { path: '/runtime-ops', redirect: '/pro/client-control/version-policies/list' },
  { path: '/discover-ops', redirect: '/pro/discover-ops/slots/list' },
  { path: '/android', redirect: '/pro/client-control/version-policies/list' },

  { path: '/pro/reviews', redirect: '/pro/review-center/versions/list' },
  { path: '/pro/developers', redirect: '/pro/developer-management/developers/list' },
  { path: '/pro/recommend', redirect: '/pro/discover-ops/slots/list' },
  { path: '/pro/recommend/categories', redirect: '/pro/discover-ops/categories/list' },
  { path: '/pro/recommend/game-banners', redirect: '/pro/discover-ops/content/list' },
  { path: '/pro/recommend/discover-banners', redirect: '/pro/discover-ops/content/list' },
  { path: '/pro/recommend/community', redirect: '/pro/recommendation-content/items/list' },
  { path: '/pro/recommend-categories', redirect: '/pro/discover-ops/categories/list' },
  { path: '/pro/game-categories', redirect: '/pro/foundation-config/categories/list' },
  { path: '/pro/games', redirect: '/pro/game-version/games/list' },
  { path: '/pro/marketing', redirect: '/pro/launch-ads/list' },
  { path: '/pro/notices', redirect: '/pro/ticket-notice/notices/list' },
  { path: '/pro/tickets', redirect: '/pro/ticket-notice/tickets/list' },
  { path: '/pro/runtime', redirect: '/pro/client-control/version-policies/list' },
  { path: '/pro/risk', redirect: '/pro/risk-audit/incidents/list' },
  { path: '/pro/settings', redirect: '/pro/foundation-config/legal' },
  { path: '/pro/sms', redirect: '/pro/risk-audit/sms/list' }
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

  if (to.meta.requiresAuth && !hasSession) {
    return '/login'
  }

  if (to.path === '/login' && hasSession) {
    return '/pro/dashboard'
  }

  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    userStore.logout()
    ElMessage.warning(
      ltGlobal(
        '当前账号没有运营审核权限，请使用管理员账号登录',
        '當前帳號沒有營運審核權限，請使用管理員帳號登入',
        'Current account does not have operations audit permission. Please sign in with an admin account.'
      )
    )
    return '/login'
  }

  return true
})

export default router
