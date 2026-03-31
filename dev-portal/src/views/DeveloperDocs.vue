<template>
  <div class="docs-page">
    <header class="docs-header">
      <div>
        <h1>开发者文档</h1>
        <p>面向小游戏开发者的完整接入文档，覆盖上传、审核、更新、回滚与多引擎发布。</p>
      </div>
      <div class="docs-header-actions">
        <el-button @click="$router.push('/games')">返回我的游戏</el-button>
        <el-button type="primary" @click="$router.push('/games/upload')">上传游戏</el-button>
      </div>
    </header>

    <el-row :gutter="16">
      <el-col :xs="24" :md="7" :lg="6">
        <el-card class="nav-card" shadow="never">
          <div
            v-for="item in sections"
            :key="item.id"
            class="nav-item"
            :class="{ active: currentSection === item.id }"
            @click="scrollTo(item.id)"
          >
            <div class="nav-title">{{ item.title }}</div>
            <div class="nav-desc">{{ item.desc }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="17" :lg="18">
        <div class="docs-content">
          <el-card id="quick-start" class="section-card" shadow="never">
            <template #header>
              <span>快速开始</span>
            </template>
            <p>1. 选择引擎导出 Web/HTML5 包。</p>
            <p>2. 确保 ZIP 根目录包含 <code>index.html</code>。</p>
            <p>3. 上传到开发者后台并提交审核。</p>
            <p>4. 审核通过后即可在 Android/iOS 端分发运行。</p>
            <div class="actions">
              <el-button type="success" @click="downloadTemplate">下载模板</el-button>
              <el-button @click="$router.push('/games/upload')">去上传页面</el-button>
            </div>
          </el-card>

          <el-card id="integration-spec" class="section-card" shadow="never">
            <template #header>
              <span>接入规范（上传、审核、回滚）</span>
            </template>
            <p>已提供完整平台规范文档，建议开发者在首次接入前完整阅读。</p>
            <p>重点包含：ZIP 结构规范、审核状态流转、强制更新、版本回滚流程。</p>
            <a :href="specDocPath" target="_blank" rel="noopener noreferrer">查看平台开发者接入规范文档</a>
          </el-card>

          <el-card id="engine-guides" class="section-card" shadow="never">
            <template #header>
              <span>引擎与框架指南</span>
            </template>
            <p>第一梯队：Cocos Creator / LayaAir / Egret（默认推荐）</p>
            <p>第二梯队：Unity WebGL / Godot（可支持）</p>
            <p>第三梯队：Phaser / PixiJS / Three.js 等轻量框架</p>
            <a :href="engineGuidePath" target="_blank" rel="noopener noreferrer">查看多引擎接入文档</a>
          </el-card>

          <el-card id="api-runtime" class="section-card" shadow="never">
            <template #header>
              <span>运行时 API 建议</span>
            </template>
            <p>建议最小接入集：</p>
            <p><code>wx.getSystemInfoSync()</code> / <code>wx.getMenuButtonBoundingClientRect()</code></p>
            <p><code>wx.request()</code> / <code>wx.getUpdateManager()</code> / 本地存储 API</p>
            <p>这样可以保证 UI 安全区适配、网络访问和热更新体验一致。</p>
          </el-card>

          <el-card id="release-checklist" class="section-card" shadow="never">
            <template #header>
              <span>发布检查清单</span>
            </template>
            <p>1. 首屏可见并可交互。</p>
            <p>2. 文案无乱码，简繁切换正常。</p>
            <p>3. 胶囊区无遮挡，安全区布局正确。</p>
            <p>4. 下载失败、断网、更新提示可触发。</p>
            <p>5. 回滚路径已验证到历史稳定版本。</p>
          </el-card>

          <el-card id="faq" class="section-card" shadow="never">
            <template #header>
              <span>常见问题</span>
            </template>
            <p><strong>Q:</strong> 上传后提示找不到 <code>index.html</code>？</p>
            <p><strong>A:</strong> 多数是 ZIP 打包层级错误，应打包目录内容而非外层文件夹。</p>
            <p><strong>Q:</strong> Android 运行时文字乱码？</p>
            <p><strong>A:</strong> 统一资源编码为 UTF-8，并检查字体文件与 JSON 编码。</p>
            <p><strong>Q:</strong> 新版本异常如何快速恢复？</p>
            <p><strong>A:</strong> 通过版本管理执行回滚到已审核通过的稳定版本。</p>
          </el-card>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const currentSection = ref('quick-start')

const sections = [
  { id: 'quick-start', title: '快速开始', desc: '3 分钟完成首次接入' },
  { id: 'integration-spec', title: '接入规范', desc: '上传、审核、回滚全流程' },
  { id: 'engine-guides', title: '引擎指南', desc: '按引擎查看导出与适配方式' },
  { id: 'api-runtime', title: 'API 与运行时', desc: '微信兼容 API 最小接入集' },
  { id: 'release-checklist', title: '发布检查', desc: '上线前自检清单' },
  { id: 'faq', title: '常见问题', desc: '定位高频故障与处理建议' }
]

const specDocPath = '/docs/platform-developer-integration-spec.md'
const engineGuidePath = '/docs/minigame-engine-guides.md'

const scrollTo = (id) => {
  currentSection.value = id
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
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
.docs-page {
  padding: 24px;
  background: #f6f8fb;
  min-height: 100vh;
}

.docs-header {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.docs-header h1 {
  margin: 0 0 8px;
}

.docs-header p {
  margin: 0;
  color: #667085;
}

.docs-header-actions {
  flex-shrink: 0;
  display: flex;
  gap: 8px;
}

.nav-card {
  position: sticky;
  top: 16px;
}

.nav-item {
  border: 1px solid #e4e7ec;
  border-radius: 10px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 10px;
  background: #fff;
}

.nav-item:hover {
  border-color: #409eff;
}

.nav-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.nav-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.nav-desc {
  margin-top: 4px;
  font-size: 12px;
  color: #667085;
}

.docs-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-card {
  scroll-margin-top: 12px;
}

.section-card p {
  margin: 8px 0;
  color: #374151;
  line-height: 1.6;
}

.actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

@media (max-width: 900px) {
  .docs-header {
    flex-direction: column;
  }

  .nav-card {
    position: static;
  }
}
</style>
