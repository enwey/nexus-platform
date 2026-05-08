<template>
  <div class="catalog-page">
    <el-card class="panel-card">
      <template #header>
        <div class="head-row">
          <div>
            <div class="panel-title">{{ mode === 'list' ? lt('启动广告列表', '啟動廣告列表', 'Launch Ads') : detailTitle }}</div>
            <div class="panel-subtitle">{{ lt('管理 iOS / Android 已接入的开屏广告，支持保存、激活、停用和排期预览。', '管理 iOS / Android 已接入的開屏廣告，支援保存、激活、停用和排期預覽。', 'Manage startup ads consumed by iOS and Android with save, activate, deactivate, and schedule preview.') }}</div>
          </div>
          <div class="actions">
            <el-button @click="loadData">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
            <template v-if="mode === 'list'">
              <el-button :disabled="!selectedIds.length" @click="runBatch('INACTIVE')">{{ lt('批量停用', '批量停用', 'Batch Deactivate') }}</el-button>
              <el-button type="success" plain :disabled="selectedIds.length !== 1" @click="runBatch('ACTIVE')">{{ lt('激活选中', '激活選中', 'Activate Selected') }}</el-button>
              <el-button type="primary" @click="openCreate">{{ lt('新建启动广告', '新建啟動廣告', 'Create Launch Ad') }}</el-button>
            </template>
            <template v-else>
              <el-button plain @click="deactivate(detailItem)">{{ lt('停用', '停用', 'Deactivate') }}</el-button>
              <el-button type="success" plain @click="activate(detailItem)">{{ lt('激活', '激活', 'Activate') }}</el-button>
              <el-button type="primary" :loading="saving" @click="saveAd">{{ lt('保存修改', '保存修改', 'Save Changes') }}</el-button>
            </template>
          </div>
        </div>
      </template>

      <template v-if="mode === 'list'">
        <el-table :data="ads" row-key="id" v-loading="loading" @selection-change="handleSelectionChange" @row-click="openDetail">
          <el-table-column type="selection" width="48" />
          <el-table-column prop="titleZhCn" :label="lt('标题(简中)', '標題(簡中)', 'Title (zh-CN)')" min-width="220" />
          <el-table-column prop="imageUrl" :label="lt('图片 URL', '圖片 URL', 'Image URL')" min-width="220" show-overflow-tooltip />
          <el-table-column :label="lt('排期', '排期', 'Schedule')" min-width="220">
            <template #default="{ row }">{{ formatRange(row.startAt, row.endAt) }}</template>
          </el-table-column>
          <el-table-column prop="status" :label="lt('配置状态', '配置狀態', 'Status')" width="120" />
          <el-table-column :label="lt('生效状态', '生效狀態', 'Effective State')" width="140">
            <template #default="{ row }"><el-tag :type="effectiveStateType(row.effectiveState || row.effectiveStatus)">{{ row.effectiveState || row.effectiveStatus || '-' }}</el-tag></template>
          </el-table-column>
          <el-table-column :label="lt('操作', '操作', 'Actions')" width="220" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="openDetail(row)">{{ lt('查看详情', '查看詳情', 'View detail') }}</el-button>
              <el-button link @click.stop="openDetail(row)">{{ lt('编辑', '編輯', 'Edit') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template v-else-if="detailItem">
        <el-row :gutter="16">
          <el-col :xs="24" :lg="14">
            <el-form :model="form" label-width="120px">
              <el-form-item :label="lt('图片 URL', '圖片 URL', 'Image URL')" required><el-input v-model="form.imageUrl" /></el-form-item>
              <el-form-item :label="lt('跳转 URL', '跳轉 URL', 'Target URL')" required><el-input v-model="form.targetUrl" /></el-form-item>
              <el-form-item :label="lt('显示秒数', '顯示秒數', 'Display Seconds')"><el-input-number v-model="form.displaySeconds" :min="1" :max="15" style="width: 100%" /></el-form-item>
              <el-form-item :label="lt('状态', '狀態', 'Status')">
                <el-select v-model="form.status" style="width: 100%">
                  <el-option label="DRAFT" value="DRAFT" />
                  <el-option label="ACTIVE" value="ACTIVE" />
                  <el-option label="INACTIVE" value="INACTIVE" />
                </el-select>
              </el-form-item>
              <el-form-item :label="lt('开始时间', '開始時間', 'Start At')"><el-input v-model="form.startAt" type="datetime-local" /></el-form-item>
              <el-form-item :label="lt('结束时间', '結束時間', 'End At')"><el-input v-model="form.endAt" type="datetime-local" /></el-form-item>
              <el-divider>{{ lt('多语言文案', '多語言文案', 'Localized Copy') }}</el-divider>
              <el-form-item label="zh-CN" required><el-input v-model="form.titleZhCn" /></el-form-item>
              <el-form-item label="zh-TW" required><el-input v-model="form.titleZhTw" /></el-form-item>
              <el-form-item label="en" required><el-input v-model="form.titleEn" /></el-form-item>
              <el-form-item :label="lt('描述(简中)', '描述(簡中)', 'Description (zh-CN)')"><el-input v-model="form.descriptionZhCn" /></el-form-item>
              <el-form-item :label="lt('按钮(简中)', '按鈕(簡中)', 'CTA (zh-CN)')"><el-input v-model="form.ctaZhCn" /></el-form-item>
            </el-form>
          </el-col>
          <el-col :xs="24" :lg="10">
            <el-card shadow="never" class="preview-card">
              <template #header>{{ lt('开屏预览', '開屏預覽', 'Launch Preview') }}</template>
              <div class="launch-preview">
                <div class="preview-badge">{{ form.sponsorZhCn || 'Sponsor' }}</div>
                <div class="preview-main-title">{{ form.titleZhCn || lt('启动广告标题', '啟動廣告標題', 'Launch Ad Title') }}</div>
                <div class="preview-desc">{{ form.descriptionZhCn || lt('启动广告描述会显示在这里。', '啟動廣告描述會顯示在這裡。', 'Launch ad description will appear here.') }}</div>
                <div class="preview-footer">{{ form.ctaZhCn || lt('立即查看', '立即查看', 'Open Now') }}</div>
              </div>
            </el-card>
            <el-card shadow="never" class="preview-card">
              <template #header>{{ lt('当前状态', '當前狀態', 'Current State') }}</template>
              <div class="preview-lines">
                <div class="preview-line"><span>{{ lt('配置状态', '配置狀態', 'Config Status') }}</span><strong>{{ form.status || '-' }}</strong></div>
                <div class="preview-line"><span>{{ lt('生效状态', '生效狀態', 'Effective State') }}</span><strong>{{ detailItem.effectiveState || detailItem.effectiveStatus || '-' }}</strong></div>
                <div class="preview-line"><span>{{ lt('排期', '排期', 'Schedule') }}</span><strong>{{ formatRange(form.startAt, form.endAt) }}</strong></div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </template>
    </el-card>

    <el-dialog v-model="editorVisible" :title="lt('新建启动广告', '新建啟動廣告', 'Create Launch Ad')" width="760px">
      <el-form :model="form" label-width="120px">
        <el-form-item :label="lt('图片 URL', '圖片 URL', 'Image URL')" required><el-input v-model="form.imageUrl" /></el-form-item>
        <el-form-item :label="lt('跳转 URL', '跳轉 URL', 'Target URL')" required><el-input v-model="form.targetUrl" /></el-form-item>
        <el-form-item label="zh-CN" required><el-input v-model="form.titleZhCn" /></el-form-item>
        <el-form-item label="zh-TW" required><el-input v-model="form.titleZhTw" /></el-form-item>
        <el-form-item label="en" required><el-input v-model="form.titleEn" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" @click="confirmCreate">{{ lt('创建并保存', '建立並保存', 'Create & Save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18nLite } from '../../../i18n'
import { activateLaunchAd, batchUpdateOpsMarketingStatus, createLaunchAd, deactivateLaunchAd, getLaunchAds, updateLaunchAd } from '../../../api'
import { effectiveStateType, formatDateTime } from './catalogCustomShared'

const props = defineProps({
  mode: { type: String, default: 'list' },
  detailId: { type: [String, Number], default: null }
})

const router = useRouter()
const { lt } = useI18nLite()
const loading = ref(false)
const saving = ref(false)
const editorVisible = ref(false)
const selectedIds = ref([])
const ads = ref([])
const form = reactive({
  imageUrl: '', targetUrl: '', imageVersion: '', displaySeconds: 4,
  sponsorZhCn: '', sponsorZhTw: '', sponsorEn: '',
  titleZhCn: '', titleZhTw: '', titleEn: '',
  descriptionZhCn: '', descriptionZhTw: '', descriptionEn: '',
  ctaZhCn: '', ctaZhTw: '', ctaEn: '',
  footerZhCn: '', footerZhTw: '', footerEn: '',
  startAt: '', endAt: '', status: 'DRAFT'
})

const detailItem = computed(() => ads.value.find((item) => String(item.id) === String(props.detailId)) || null)
const detailTitle = computed(() => detailItem.value?.titleZhCn || '-')
const normalizeDateInput = (value) => (value ? String(value).slice(0, 16) : '')

const resetForm = () => Object.assign(form, {
  imageUrl: '', targetUrl: '', imageVersion: '', displaySeconds: 4,
  sponsorZhCn: '', sponsorZhTw: '', sponsorEn: '',
  titleZhCn: '', titleZhTw: '', titleEn: '',
  descriptionZhCn: '', descriptionZhTw: '', descriptionEn: '',
  ctaZhCn: '', ctaZhTw: '', ctaEn: '',
  footerZhCn: '', footerZhTw: '', footerEn: '',
  startAt: '', endAt: '', status: 'DRAFT'
})

const syncDetail = () => {
  if (!detailItem.value) return
  Object.assign(form, {
    ...detailItem.value,
    startAt: normalizeDateInput(detailItem.value.startAt),
    endAt: normalizeDateInput(detailItem.value.endAt)
  })
}

const formatRange = (startAt, endAt) => {
  if (!startAt && !endAt) return lt('长期有效', '長期有效', 'Always on')
  return `${formatDateTime(startAt)} ~ ${formatDateTime(endAt)}`
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getLaunchAds()
    ads.value = res.data?.items || res.data || []
    syncDetail()
  } catch (error) {
    ElMessage.error(error.message || lt('加载启动广告失败', '載入啟動廣告失敗', 'Failed to load launch ads'))
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map((item) => item.id)
}

const openDetail = (row) => router.push({ name: 'OpsLaunchAdDetail', params: { launchAdId: row.id } })
const openCreate = () => { resetForm(); editorVisible.value = true }
const saveAd = async () => {
  if (!form.imageUrl.trim() || !form.targetUrl.trim() || !form.titleZhCn.trim() || !form.titleZhTw.trim() || !form.titleEn.trim()) {
    ElMessage.warning(lt('请填写必填字段', '請填寫必填欄位', 'Please fill required fields'))
    return
  }
  if (form.startAt && form.endAt && form.endAt < form.startAt) {
    ElMessage.warning(lt('结束时间不能早于开始时间', '結束時間不能早於開始時間', 'End time cannot be earlier than start time'))
    return
  }
  saving.value = true
  try {
    await updateLaunchAd(detailItem.value.id, { ...form })
    await loadData()
    ElMessage.success(lt('启动广告已保存', '啟動廣告已保存', 'Launch ad saved'))
  } catch (error) {
    ElMessage.error(error.message || lt('保存启动广告失败', '保存啟動廣告失敗', 'Failed to save launch ad'))
  } finally {
    saving.value = false
  }
}

const confirmCreate = async () => {
  saving.value = true
  try {
    await createLaunchAd({ ...form })
    editorVisible.value = false
    await loadData()
    ElMessage.success(lt('启动广告已创建', '啟動廣告已建立', 'Launch ad created'))
  } catch (error) {
    ElMessage.error(error.message || lt('创建启动广告失败', '建立啟動廣告失敗', 'Failed to create launch ad'))
  } finally {
    saving.value = false
  }
}

const activate = async (row) => {
  if (!row) return
  try {
    await ElMessageBox.confirm(
      lt('确认激活该启动广告？激活后会替换当前线上启动广告。', '確認激活該啟動廣告？激活後會替換當前線上啟動廣告。', 'Activate this launch ad? It will replace the live startup ad.'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { type: 'warning' }
    )
    await activateLaunchAd(row.id)
    await loadData()
    ElMessage.success(lt('启动广告已激活', '啟動廣告已激活', 'Launch ad activated'))
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || lt('激活失败', '激活失敗', 'Activation failed'))
  }
}

const deactivate = async (row) => {
  if (!row) return
  try {
    await ElMessageBox.confirm(
      lt('确认停用该启动广告？停用后前端将不再展示它。', '確認停用該啟動廣告？停用後前端將不再展示它。', 'Deactivate this launch ad? It will no longer be shown on the frontend.'),
      lt('二次确认', '二次確認', 'Secondary Confirmation'),
      { type: 'warning' }
    )
    await deactivateLaunchAd(row.id)
    await loadData()
    ElMessage.success(lt('启动广告已停用', '啟動廣告已停用', 'Launch ad deactivated'))
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || lt('停用失败', '停用失敗', 'Deactivation failed'))
  }
}

const runBatch = async (status) => {
  try {
    await batchUpdateOpsMarketingStatus({ assetType: 'LAUNCH_AD', ids: selectedIds.value, status, reason: `Batch ${status.toLowerCase()} from launch ad list` })
    selectedIds.value = []
    await loadData()
    ElMessage.success(lt('批量操作已完成', '批量操作已完成', 'Batch action completed'))
  } catch (error) {
    ElMessage.error(error.message || lt('批量操作失败', '批量操作失敗', 'Batch action failed'))
  }
}

watch(() => props.detailId, syncDetail)
onMounted(loadData)
</script>

<style scoped>
.catalog-page { display: flex; flex-direction: column; gap: 16px; }
.panel-card, .preview-card { border-radius: 16px; }
.head-row { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.panel-title { font-size: 18px; font-weight: 700; color: #111827; }
.panel-subtitle { margin-top: 6px; color: #6b7280; font-size: 13px; line-height: 1.6; max-width: 760px; }
.actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.launch-preview { min-height: 220px; border-radius: 16px; background: linear-gradient(145deg, #111827, #374151); color: #fff; padding: 20px; display: flex; flex-direction: column; justify-content: flex-end; gap: 10px; }
.preview-badge { font-size: 12px; opacity: .75; }
.preview-main-title { font-size: 24px; font-weight: 700; }
.preview-desc { font-size: 13px; opacity: .85; line-height: 1.6; }
.preview-footer { display: inline-flex; padding: 8px 12px; border-radius: 999px; background: rgba(255,255,255,.14); width: fit-content; }
.preview-lines { display: flex; flex-direction: column; gap: 10px; }
.preview-line { display: flex; justify-content: space-between; gap: 16px; font-size: 13px; color: #4b5563; }
</style>
