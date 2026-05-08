<template>
  <div class="domain-page">
    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ lt(domain.title[0], domain.title[1], domain.title[2]) }}</div>
            <div class="panel-subtitle">{{ lt(domain.subtitle[0], domain.subtitle[1], domain.subtitle[2]) }}</div>
          </div>
          <el-button :loading="loading" @click="loadSummary">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
        </div>
      </template>

      <el-alert v-if="error" type="error" :closable="false" :title="error" class="page-alert" />

      <el-table :data="sectionRows" v-loading="loading">
        <el-table-column prop="title" :label="lt('配置域', '配置域', 'Section')" min-width="220" />
        <el-table-column prop="description" :label="lt('说明', '說明', 'Description')" min-width="320" />
        <el-table-column :label="lt('当前规模', '當前規模', 'Current Volume')" width="140">
          <template #default="{ row }">{{ row.countLabel }}</template>
        </el-table-column>
        <el-table-column :label="lt('当前结构', '當前結構', 'Structure')" width="210">
          <template #default>
            <el-tag type="info">List -> Detail</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="lt('操作', '操作', 'Action')" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" @click="openDetail(row.key)">{{ lt('进入详情页', '進入詳情頁', 'Open Detail') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18nLite } from '../../i18n'
import {
  getAndroidAbExperiments,
  getAndroidChannels,
  getAndroidFeatureToggles,
  getAndroidGrayReleasePlans,
  getAndroidPageCircuitBreakers,
  getAuditLogs,
  getDiscoverCategories,
  getDiscoverOpsConfig,
  getLaunchAds,
  getOpsGameCategories,
  getOpsGames,
  getOpsNotices,
  getOpsPublishOrders,
  getRiskIncidents,
  getOpsTickets,
  getOpsRuleTemplates,
  getPublicLegalConfig
} from '../../api'
import { domainCatalog } from './OpsDomainCatalog'

const props = defineProps({
  domainKey: {
    type: String,
    required: true
  }
})

const router = useRouter()
const { lt } = useI18nLite()
const loading = ref(false)
const error = ref('')
const summaryMap = ref({})

const domain = computed(() => domainCatalog[props.domainKey])

const sectionRows = computed(() =>
  (domain.value?.sections || []).map((section) => ({
    key: section.key,
    title: lt(section.title[0], section.title[1], section.title[2]),
    description: lt(section.description[0], section.description[1], section.description[2]),
    countLabel: summaryMap.value[section.key] ?? '-'
  }))
)

const domainLoaders = {
  async 'library-ops'() {
    const [gamesRes, categoriesRes] = await Promise.all([getOpsGames(), getOpsGameCategories()])
    return {
      'library-governance': `${(gamesRes.data || []).length} ${lt('款游戏', '款遊戲', 'games')}`,
      'category-routing': `${(categoriesRes.data || []).length} ${lt('个分类', '個分類', 'categories')}`
    }
  },
  async 'discover-ops'() {
    const [configRes, categoriesRes] = await Promise.all([getDiscoverOpsConfig(), getDiscoverCategories()])
    const data = configRes.data || {}
    return {
      'hero-and-pools': `${(data.newbieAppIds || []).length}/${(data.everyoneAppIds || []).length}/${(data.rankedAppIds || []).length}`,
      'discover-hero': `${(data.slotControls || []).length} ${lt('个区块', '個區塊', 'slots')}`,
      'discover-categories': `${(categoriesRes.data || []).length} ${lt('个分类', '個分類', 'categories')}`,
      'library-top-banner': `${(data.gameTopBanners || []).length} Banner`,
      'discover-top-banner': `${(data.discoverTopBanners || []).length} Banner`
    }
  },
  async 'recommend-content'() {
    const [configRes, publishRes] = await Promise.all([getDiscoverOpsConfig(), getOpsPublishOrders()])
    const data = configRes.data || {}
    return {
      'community-content': `${(data.communityItems || []).length} ${lt('条内容', '條內容', 'items')}`,
      'publish-and-experiments': `${(publishRes.data || []).length} ${lt('个发布单', '個發佈單', 'orders')}`
    }
  },
  async 'launch-ads'() {
    const res = await getLaunchAds()
    const items = Array.isArray(res.data) ? res.data : res.data?.items || []
    return {
      'launch-ads': `${items.length} ${lt('条投放', '條投放', 'deliveries')}`
    }
  },
  async 'client-control'() {
    const [channelsRes, togglesRes, breakerRes, abRes, grayRes] = await Promise.all([
      getAndroidChannels(),
      getAndroidFeatureToggles(),
      getAndroidPageCircuitBreakers(),
      getAndroidAbExperiments(),
      getAndroidGrayReleasePlans()
    ])
    return {
      'version-and-channel': `${(channelsRes.data || []).length} ${lt('条渠道规则', '條渠道規則', 'channel rules')}`,
      'feature-and-breakers': `${(togglesRes.data || []).length + (breakerRes.data || []).length} ${lt('条控制规则', '條控制規則', 'controls')}`,
      'ab-and-gray': `${(abRes.data || []).length + (grayRes.data || []).length} ${lt('条实验/灰度计划', '條實驗/灰度計畫', 'experiments / plans')}`
    }
  },
  async 'workspace-ops'() {
    const [ticketsRes, noticesRes] = await Promise.all([getOpsTickets(), getOpsNotices()])
    return {
      tickets: `${(ticketsRes.data || []).length} ${lt('张工单', '張工單', 'tickets')}`,
      notices: `${(noticesRes.data || []).length} ${lt('条通知', '條通知', 'notices')}`
    }
  },
  async 'risk-logs'() {
    const [incidentsRes, auditRes] = await Promise.all([getRiskIncidents(), getAuditLogs({ limit: 50 })])
    return {
      'risk-and-audit': `${(incidentsRes.data || []).length}/${(auditRes.data || []).length}`,
      'verification-logs': lt('使用详情页查看', '使用詳情頁查看', 'Use detail page')
    }
  },
  async 'base-config'() {
    const [templatesRes, legalRes] = await Promise.all([getOpsRuleTemplates(), getPublicLegalConfig()])
    return {
      'platform-settings': `${(templatesRes.data || []).length} ${lt('条规则模板', '條規則模板', 'rule templates')}`,
      'legal-links': legalRes.data?.termsUrl || legalRes.data?.privacyUrl ? lt('已接线', '已接線', 'Connected') : lt('未接线', '未接線', 'Disconnected')
    }
  }
}

const loadSummary = async () => {
  loading.value = true
  error.value = ''
  try {
    const loader = domainLoaders[props.domainKey]
    summaryMap.value = loader ? await loader() : {}
  } catch (err) {
    error.value = err.message || lt('加载配置域概览失败', '載入配置域概覽失敗', 'Failed to load domain summary')
  } finally {
    loading.value = false
  }
}

const openDetail = (sectionKey) => {
  router.push(`/pro/${props.domainKey}/${sectionKey}`)
}

onMounted(loadSummary)
</script>

<style scoped>
.domain-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 18px; }
.head-row { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.panel-title { font-size: 17px; font-weight: 800; color: #101828; }
.panel-subtitle { margin-top: 6px; color: #667085; font-size: 13px; }
.page-alert { margin-bottom: 16px; }
</style>
