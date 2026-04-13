<template>
  <div class="pro-page">
    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" :lg="6" v-for="item in metricCards" :key="item.key">
        <el-card class="pro-kpi">
          <div class="label">{{ item.label }}</div>
          <div class="value">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt-16">
      <el-col :xs="24" :lg="14">
        <el-card>
          <template #header>{{ lt('模塊導航', '模塊導航', 'Module Navigation') }}</template>
          <div class="module-grid">
            <div class="module-item" v-for="entry in moduleEntries" :key="entry.path" @click="go(entry.path)">
              <div class="module-title">{{ entry.title }}</div>
              <div class="module-desc">{{ entry.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card>
          <template #header>{{ lt('架構原則', '架構原則', 'Architecture Principles') }}</template>
          <ul class="principles">
            <li>{{ lt('模塊邊界清晰：開發者 / 短信 / 推薦 / 遊戲管理分治。', '模塊邊界清晰：開發者 / 短信 / 推薦 / 遊戲管理分治。', 'Clear module boundaries: developers / SMS / recommendations / game management.') }}</li>
            <li>{{ lt('單模塊獨立路由與狀態，避免交叉耦合。', '單模塊獨立路由與狀態，避免交叉耦合。', 'Each module owns its route and state to avoid cross-coupling.') }}</li>
            <li>{{ lt('使用 Pro 後台布局與卡片式信息層級。', '使用 Pro 後台布局與卡片式信息層級。', 'Use Pro-style layout and card-based information hierarchy.') }}</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18nLite } from '../../i18n'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const { lt } = useI18nLite()

const moduleEntries = computed(() => [
  {
    path: '/pro/developers',
    title: lt('開發者模塊', '開發者模塊', 'Developer Module'),
    desc: lt('管理開發者維度的遊戲分佈、狀態與質量。', '管理開發者維度的遊戲分佈、狀態與質量。', 'Manage game distribution, status and quality by developer.')
  },
  {
    path: '/pro/sms',
    title: lt('短信模塊', '短信模塊', 'SMS Module'),
    desc: lt('審查驗證碼發送來源、用途與風險行為。', '審查驗證碼發送來源、用途與風險行為。', 'Review verification code source, purpose and risk behavior.')
  },
  {
    path: '/pro/recommend',
    title: lt('推薦模塊', '推薦模塊', 'Recommendation Module'),
    desc: lt('配置發現與推薦位，驅動運營投放。', '配置發現與推薦位，驅動運營投放。', 'Configure discover and recommendation slots for operations.')
  },
  {
    path: '/pro/games',
    title: lt('遊戲管理模塊', '遊戲管理模塊', 'Game Management Module'),
    desc: lt('管理遊戲元數據、分類、版本與展示信息。', '管理遊戲元數據、分類、版本與展示信息。', 'Manage game metadata, categories, versions and display data.')
  }
])

const metricCards = computed(() => [
  { key: 'user', label: lt('當前用戶', '當前用戶', 'Current User'), value: userStore.user?.username || '-' },
  { key: 'role', label: lt('角色', '角色', 'Role'), value: userStore.user?.role || '-' },
  { key: 'mod', label: lt('模塊數', '模塊數', 'Module Count'), value: '4' },
  { key: 'arch', label: lt('架構', '架構', 'Architecture'), value: 'Pro' }
])

const go = (path) => router.push(path)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.mt-16 { margin-top: 16px; }
.pro-kpi .label { color: #8c8c8c; font-size: 12px; }
.pro-kpi .value { margin-top: 8px; font-size: 24px; font-weight: 700; color: #1f1f1f; }
.module-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.module-item { border: 1px solid #f0f0f0; border-radius: 8px; padding: 14px; cursor: pointer; transition: all .2s ease; }
.module-item:hover { border-color: #1677ff; box-shadow: 0 4px 16px rgba(22,119,255,0.12); }
.module-title { font-weight: 600; margin-bottom: 8px; }
.module-desc { color: #6b7280; font-size: 12px; line-height: 1.5; }
.principles { margin: 0; padding-left: 18px; color: #4b5563; line-height: 1.8; }
@media (max-width: 920px) { .module-grid { grid-template-columns: 1fr; } }
</style>
