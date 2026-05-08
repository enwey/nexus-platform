<template>
  <div class="legal-page">
    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ lt('法务链接配置', '法務連結配置', 'Legal Links') }}</div>
            <div class="panel-subtitle">{{ lt('当前项目法务链接仍由公共端配置对外暴露，这里提供运营可见的配置详情与主线整合说明。', '當前專案法務連結仍由公共端配置對外暴露，這裡提供營運可見的配置詳情與主線整合說明。', 'Legal links are still exposed from the public side. This page shows the current endpoints and mainline integration notes.') }}</div>
          </div>
          <el-button :loading="loading" @click="loadData">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
        </div>
      </template>

      <el-alert v-if="error" type="error" :closable="false" :title="error" class="page-alert" />

      <el-skeleton v-if="loading" :rows="6" animated />

      <template v-else>
        <el-descriptions border :column="1">
          <el-descriptions-item :label="lt('Terms URL', 'Terms URL', 'Terms URL')">
            <a v-if="legalConfig.termsUrl" :href="legalConfig.termsUrl" target="_blank" rel="noreferrer">{{ legalConfig.termsUrl }}</a>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item :label="lt('Privacy URL', 'Privacy URL', 'Privacy URL')">
            <a v-if="legalConfig.privacyUrl" :href="legalConfig.privacyUrl" target="_blank" rel="noreferrer">{{ legalConfig.privacyUrl }}</a>
            <span v-else>-</span>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <el-alert
          type="warning"
          show-icon
          :closable="false"
          :title="lt('当前这组法务链接还不是后台可写配置，仍由后端 public legal config 输出。', '當前這組法務連結還不是後台可寫配置，仍由後端 public legal config 輸出。', 'These legal links are not yet admin-writable. They still come from the backend public legal config.')"
        />

        <el-timeline class="integration-list">
          <el-timeline-item :timestamp="lt('现状', '現狀', 'Current')">
            {{ lt('运营可在这里查看线上 Terms / Privacy 的对外地址，用于审核模板、通知文案和工单答复联动。', '營運可在這裡查看線上 Terms / Privacy 的對外地址，用於審核模板、通知文案和工單答覆聯動。', 'Operations can inspect the live Terms / Privacy links here and reference them in review templates, notices, and ticket replies.') }}
          </el-timeline-item>
          <el-timeline-item :timestamp="lt('待主线整合', '待主線整合', 'Mainline Next')">
            {{ lt('后续可把 Terms / Privacy / Child Safety / EULA 统一接到平台设置中心，并补审批与审计。', '後續可把 Terms / Privacy / Child Safety / EULA 統一接到平台設定中心，並補審批與審計。', 'Mainline can later move Terms / Privacy / Child Safety / EULA into platform settings with approval and audit support.') }}
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getPublicLegalConfig } from '../../api'
import { useI18nLite } from '../../i18n'

const { lt } = useI18nLite()
const loading = ref(false)
const error = ref('')
const legalConfig = reactive({
  termsUrl: '',
  privacyUrl: ''
})

const loadData = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await getPublicLegalConfig()
    Object.assign(legalConfig, {
      termsUrl: res.data?.termsUrl || '',
      privacyUrl: res.data?.privacyUrl || ''
    })
  } catch (err) {
    error.value = err.message || lt('加载法务链接失败', '載入法務連結失敗', 'Failed to load legal links')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.legal-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card { border-radius: 18px; }
.head-row { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.panel-title { font-size: 17px; font-weight: 800; color: #101828; }
.panel-subtitle { margin-top: 6px; color: #667085; font-size: 13px; }
.page-alert { margin-bottom: 16px; }
.integration-list { margin-top: 16px; }
</style>
