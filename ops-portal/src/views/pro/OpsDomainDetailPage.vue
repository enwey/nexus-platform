<template>
  <div class="detail-page">
    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="breadcrumb-line">
              <el-button link type="primary" @click="goBack">{{ lt('返回列表', '返回列表', 'Back to List') }}</el-button>
              <span class="separator">/</span>
              <span>{{ domainTitle }}</span>
            </div>
            <div class="panel-title">{{ sectionTitle }}</div>
            <div class="panel-subtitle">{{ sectionDescription }}</div>
          </div>
          <el-button-group>
            <el-button
              v-for="item in domain.sections"
              :key="item.key"
              :type="item.key === sectionKey ? 'primary' : 'default'"
              @click="openSection(item.key)"
            >
              {{ lt(item.title[0], item.title[1], item.title[2]) }}
            </el-button>
          </el-button-group>
        </div>
      </template>

      <component :is="activeSection.component" v-bind="activeSection.props || {}" />
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18nLite } from '../../i18n'
import GameCategoryModule from './GameCategoryModule.vue'
import GameModule from './GameModule.vue'
import LegalConfigDetailModule from './LegalConfigDetailModule.vue'
import MarketingModule from './MarketingModule.vue'
import NoticeModule from './NoticeModule.vue'
import RecommendBannerModule from './RecommendBannerModule.vue'
import RecommendCategoryModule from './RecommendCategoryModule.vue'
import RecommendCommunityModule from './RecommendCommunityModule.vue'
import RecommendModule from './RecommendModule.vue'
import RiskModule from './RiskModule.vue'
import RuntimeModule from './RuntimeModule.vue'
import SettingsModule from './SettingsModule.vue'
import SmsModule from './SmsModule.vue'
import TicketModule from './TicketModule.vue'
import { domainCatalog } from './OpsDomainCatalog'

const props = defineProps({
  domainKey: {
    type: String,
    required: true
  }
})

const route = useRoute()
const router = useRouter()
const { lt } = useI18nLite()

const componentMap = {
  'library-governance': { component: GameModule },
  'category-routing': { component: GameCategoryModule },
  'hero-and-pools': { component: RuntimeModule },
  'discover-hero': { component: RecommendModule },
  'discover-categories': { component: RecommendCategoryModule },
  'library-top-banner': {
    component: RecommendBannerModule,
    props: {
      bannerType: 'game',
      bannerLabel: ['游戏库 Top Banner', '遊戲庫 Top Banner', 'Library Top Banner'],
      bannerDescription: [
        '用于游戏库首页、新手冷启动和馆内顶部 Banner 运营。',
        '用於遊戲庫首頁、新手冷啟動和館內頂部 Banner 營運。',
        'Used for library homepage, newbie cold-start, and in-library top-banner operations.'
      ]
    }
  },
  'discover-top-banner': {
    component: RecommendBannerModule,
    props: {
      bannerType: 'discover',
      bannerLabel: ['发现页 Hero Banner', '發現頁 Hero Banner', 'Discover Hero Banner'],
      bannerDescription: [
        '用于发现页 Hero 区、首屏焦点图和定时曝光。',
        '用於發現頁 Hero 區、首屏焦點圖和定時曝光。',
        'Used for discover hero, above-the-fold focus creatives, and scheduled exposure.'
      ]
    }
  },
  'community-content': { component: RecommendCommunityModule },
  'publish-and-experiments': { component: RecommendModule },
  'launch-ads': { component: MarketingModule },
  'version-and-channel': { component: RuntimeModule },
  'feature-and-breakers': { component: RuntimeModule },
  'ab-and-gray': { component: RuntimeModule },
  tickets: { component: TicketModule },
  notices: { component: NoticeModule },
  'risk-and-audit': { component: RiskModule },
  'verification-logs': { component: SmsModule },
  'platform-settings': { component: SettingsModule },
  'legal-links': { component: LegalConfigDetailModule }
}

const domain = computed(() => domainCatalog[props.domainKey])
const sectionKey = computed(() => String(route.params.section || domain.value.sections[0].key))
const currentSectionMeta = computed(() => domain.value.sections.find((item) => item.key === sectionKey.value) || domain.value.sections[0])
const activeSection = computed(() => componentMap[sectionKey.value] || componentMap[domain.value.sections[0].key])

const domainTitle = computed(() => lt(domain.value.title[0], domain.value.title[1], domain.value.title[2]))
const sectionTitle = computed(() => lt(currentSectionMeta.value.title[0], currentSectionMeta.value.title[1], currentSectionMeta.value.title[2]))
const sectionDescription = computed(() => lt(currentSectionMeta.value.description[0], currentSectionMeta.value.description[1], currentSectionMeta.value.description[2]))

const goBack = () => {
  router.push(`/pro/${props.domainKey}/list`)
}

const openSection = (key) => {
  if (key === sectionKey.value) return
  router.push(`/pro/${props.domainKey}/${key}`)
}
</script>

<style scoped>
.detail-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 18px; }
.head-row { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.breadcrumb-line { display: flex; align-items: center; gap: 8px; color: #667085; font-size: 13px; }
.separator { color: #98a2b3; }
.panel-title { margin-top: 6px; font-size: 18px; font-weight: 800; color: #101828; }
.panel-subtitle { margin-top: 6px; color: #667085; font-size: 13px; max-width: 720px; }
</style>
