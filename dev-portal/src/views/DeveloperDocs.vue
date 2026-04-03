<template>
  <div class="docs-page">
    <header class="docs-header">
      <div>
        <h1>{{ lt('开发者文档', '開發者文件', 'Developer Docs') }}</h1>
        <p>{{ lt('面向小游戏开发者的完整接入文档，覆盖上传、审核、更新、回滚与多引擎发布。', '面向小遊戲開發者的完整接入文件，涵蓋上傳、審核、更新、回滾與多引擎發布。', 'Complete integration docs for mini-game developers: upload, review, update, rollback, and multi-engine publishing.') }}</p>
      </div>
      <div class="docs-header-actions">
        <el-button @click="$router.push('/games')">{{ lt('返回我的游戏', '返回我的遊戲', 'Back to My Games') }}</el-button>
        <el-button type="primary" @click="$router.push('/games/upload')">{{ lt('上传游戏', '上傳遊戲', 'Upload Game') }}</el-button>
      </div>
    </header>

    <el-row :gutter="16">
      <el-col :xs="24" :md="7" :lg="6">
        <el-card class="nav-card" shadow="never">
          <div v-for="item in sections" :key="item.id" class="nav-item" :class="{ active: currentSection === item.id }" @click="scrollTo(item.id)">
            <div class="nav-title">{{ item.title }}</div>
            <div class="nav-desc">{{ item.desc }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="17" :lg="18">
        <div class="docs-content">
          <el-card id="quick-start" class="section-card" shadow="never">
            <template #header><span>{{ lt('快速开始', '快速開始', 'Quick Start') }}</span></template>
            <p>1. {{ lt('选择引擎导出 Web/HTML5 包。', '選擇引擎導出 Web/HTML5 包。', 'Export a Web/HTML5 package from your engine.') }}</p>
            <p>2. {{ lt('确保 ZIP 根目录包含 index.html。', '確認 ZIP 根目錄包含 index.html。', 'Ensure ZIP root contains index.html.') }}</p>
            <p>3. {{ lt('上传到开发者后台并提交审核。', '上傳到開發者後台並提交審核。', 'Upload in developer portal and submit for review.') }}</p>
            <p>4. {{ lt('审核通过后即可在 Android/iOS 端分发运行。', '審核通過後即可在 Android/iOS 分發運行。', 'After approval, distribute to Android/iOS clients.') }}</p>
            <div class="actions">
              <el-button type="success" @click="downloadTemplate">{{ lt('下载模板', '下載模板', 'Download Template') }}</el-button>
              <el-button @click="$router.push('/games/upload')">{{ lt('去上传页面', '前往上傳頁', 'Go to Upload') }}</el-button>
            </div>
          </el-card>

          <el-card id="integration-spec" class="section-card" shadow="never">
            <template #header><span>{{ lt('接入规范（上传、审核、回滚）', '接入規範（上傳、審核、回滾）', 'Integration Spec (Upload / Review / Rollback)') }}</span></template>
            <p>{{ lt('已提供完整平台规范文档，建议开发者在首次接入前完整阅读。', '已提供完整平台規範文件，建議首次接入前完整閱讀。', 'A complete platform spec is provided and should be read before first integration.') }}</p>
            <p>{{ lt('重点包含：ZIP 结构规范、审核状态流转、强制更新、版本回滚流程。', '重點包含：ZIP 結構、審核狀態流轉、強制更新、版本回滾流程。', 'Includes ZIP structure, review lifecycle, force update, and rollback flow.') }}</p>
            <a :href="specDocPath" target="_blank" rel="noopener noreferrer">{{ lt('查看平台开发者接入规范文档', '查看平台開發者接入規範文件', 'View Platform Integration Spec') }}</a>
          </el-card>

          <el-card id="engine-guides" class="section-card" shadow="never">
            <template #header><span>{{ lt('引擎与框架指南', '引擎與框架指南', 'Engine & Framework Guide') }}</span></template>
            <p>{{ lt('第一梯队：Cocos Creator / LayaAir / Egret（默认推荐）', '第一梯隊：Cocos Creator / LayaAir / Egret（預設推薦）', 'Tier 1: Cocos Creator / LayaAir / Egret (recommended)') }}</p>
            <p>{{ lt('第二梯队：Unity WebGL / Godot（可支持）', '第二梯隊：Unity WebGL / Godot（可支援）', 'Tier 2: Unity WebGL / Godot (supported)') }}</p>
            <p>{{ lt('第三梯队：Phaser / PixiJS / Three.js 等轻量框架', '第三梯隊：Phaser / PixiJS / Three.js 等輕量框架', 'Tier 3: Phaser / PixiJS / Three.js and other lightweight frameworks') }}</p>
            <a :href="engineGuidePath" target="_blank" rel="noopener noreferrer">{{ lt('查看多引擎接入文档', '查看多引擎接入文件', 'View Multi-Engine Guide') }}</a>
          </el-card>

          <el-card id="api-runtime" class="section-card" shadow="never">
            <template #header><span>{{ lt('运行时 API 建议', '運行時 API 建議', 'Runtime API Recommendations') }}</span></template>
            <p>{{ lt('建议最小接入集：', '建議最小接入集：', 'Recommended minimum API set:') }}</p>
            <p><code>wx.getSystemInfoSync()</code> / <code>wx.getMenuButtonBoundingClientRect()</code></p>
            <p><code>wx.request()</code> / <code>wx.getUpdateManager()</code> / {{ lt('本地存储 API', '本地儲存 API', 'storage APIs') }}</p>
            <p>{{ lt('这样可以保证 UI 安全区适配、网络访问和热更新体验一致。', '可確保 UI 安全區適配、網路訪問與熱更新體驗一致。', 'Ensures safe-area layout, network compatibility, and OTA consistency.') }}</p>
          </el-card>

          <el-card id="release-checklist" class="section-card" shadow="never">
            <template #header><span>{{ lt('发布检查清单', '發布檢查清單', 'Release Checklist') }}</span></template>
            <p>1. {{ lt('首屏可见并可交互。', '首屏可見且可互動。', 'First screen is visible and interactive.') }}</p>
            <p>2. {{ lt('文案无乱码，简繁切换正常。', '文案無亂碼，簡繁切換正常。', 'No garbled text; language switching works.') }}</p>
            <p>3. {{ lt('胶囊区无遮挡，安全区布局正确。', '膠囊區無遮擋，安全區布局正確。', 'Capsule area is unobstructed; safe area layout is correct.') }}</p>
            <p>4. {{ lt('下载失败、断网、更新提示可触发。', '下載失敗、斷網、更新提示可觸發。', 'Download failure/offline/update prompts can be triggered.') }}</p>
            <p>5. {{ lt('回滚路径已验证到历史稳定版本。', '回滾路徑已驗證到歷史穩定版本。', 'Rollback path validated to a stable version.') }}</p>
          </el-card>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useI18nLite } from '../i18n'

const { lt } = useI18nLite()
const currentSection = ref('quick-start')

const sections = [
  { id: 'quick-start', title: lt('快速开始', '快速開始', 'Quick Start'), desc: lt('3 分钟完成首次接入', '3 分鐘完成首次接入', 'Finish first integration in 3 minutes') },
  { id: 'integration-spec', title: lt('接入规范', '接入規範', 'Integration Spec'), desc: lt('上传、审核、回滚全流程', '上傳、審核、回滾全流程', 'Upload, review, and rollback flow') },
  { id: 'engine-guides', title: lt('引擎指南', '引擎指南', 'Engine Guide'), desc: lt('按引擎查看导出与适配方式', '按引擎查看導出與適配方式', 'Export and adaptation by engine') },
  { id: 'api-runtime', title: lt('API 与运行时', 'API 與運行時', 'API & Runtime'), desc: lt('微信兼容 API 最小接入集', '微信相容 API 最小接入集', 'Minimal WeChat-compatible API set') },
  { id: 'release-checklist', title: lt('发布检查', '發布檢查', 'Release Checklist'), desc: lt('上线前自检清单', '上線前自檢清單', 'Pre-release self-check list') }
]

const specDocPath = '/docs/platform-developer-integration-spec.md'
const engineGuidePath = '/docs/minigame-engine-guides.md'

const scrollTo = (id) => {
  currentSection.value = id
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const downloadTemplate = () => {
  const link = document.createElement('a')
  link.href = '/downloads/minigame-starter.zip'
  link.download = 'minigame-starter.zip'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}
</script>

<style scoped>
.docs-page { padding: 24px; background: #f6f8fb; min-height: 100vh; }
.docs-header { margin-bottom: 16px; display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.docs-header h1 { margin: 0 0 8px; }
.docs-header p { margin: 0; color: #667085; }
.docs-header-actions { flex-shrink: 0; display: flex; gap: 8px; }
.nav-card { position: sticky; top: 16px; }
.nav-item { border: 1px solid #e4e7ec; border-radius: 10px; padding: 10px 12px; cursor: pointer; transition: all 0.2s ease; margin-bottom: 10px; background: #fff; }
.nav-item:hover { border-color: #409eff; }
.nav-item.active { border-color: #409eff; background: #ecf5ff; }
.nav-title { font-size: 14px; font-weight: 600; color: #111827; }
.nav-desc { margin-top: 4px; font-size: 12px; color: #667085; }
.docs-content { display: flex; flex-direction: column; gap: 12px; }
.section-card { scroll-margin-top: 12px; }
.section-card p { margin: 8px 0; color: #374151; line-height: 1.6; }
.actions { margin-top: 12px; display: flex; gap: 8px; }
@media (max-width: 900px) {
  .docs-header { flex-direction: column; }
  .nav-card { position: static; }
}
</style>
