<template>
  <div class="docs-page">
    <header class="docs-header">
      <div>
        <h1>{{ lt('开发文档中心', '開發文件中心', 'Developer Docs Center') }}</h1>
        <p>{{ lt('把接入规范、提审清单和复盘案例沉淀成你团队自己的版本化文档资产。', '把接入規範、提審清單和複盤案例沉澱成你團隊自己的版本化文件資產。', 'Turn integration specs, submission checklists, and review cases into your own versioned team knowledge base.') }}</p>
      </div>
      <div class="docs-header-actions">
        <el-select v-model="docTypeFilter" style="width: 180px" @change="handleFilterChange">
          <el-option :label="lt('全部文档', '全部文件', 'All Documents')" value="" />
          <el-option :label="lt('接入指南', '接入指南', 'Guides')" value="GUIDE" />
          <el-option :label="lt('提审清单', '提審清單', 'Checklists')" value="CHECKLIST" />
          <el-option :label="lt('案例条目', '案例條目', 'Cases')" value="CASE" />
        </el-select>
        <el-button @click="openCreate('GUIDE')">{{ lt('新建指南', '新建指南', 'New Guide') }}</el-button>
        <el-button type="primary" @click="openCreate('CASE')">{{ lt('新建案例', '新建案例', 'New Case') }}</el-button>
      </div>
    </header>

    <el-alert
      v-if="docsError"
      :title="docsError"
      type="error"
      :closable="false"
      show-icon
      class="docs-alert"
    />

    <el-row :gutter="16">
      <el-col :xs="24" :lg="10">
        <el-card class="docs-list-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>{{ lt('文档列表', '文件列表', 'Articles') }}</span>
              <el-button text @click="loadDocs">{{ lt('刷新', '重新整理', 'Refresh') }}</el-button>
            </div>
          </template>

          <el-skeleton v-if="docsLoading" :rows="6" animated />
          <el-empty
            v-else-if="!documents.length"
            :description="lt('还没有文档，建议先把提审规范和常见驳回案例写下来。', '還沒有文件，建議先把提審規範和常見駁回案例寫下來。', 'No documents yet. Start by documenting submission rules and common rejection cases.')"
          />
          <div v-else class="doc-list">
            <div
              v-for="doc in documents"
              :key="doc.id"
              class="doc-item"
              :class="{ active: selectedDocId === doc.id }"
              @click="handleSelectDoc(doc)"
            >
              <div class="doc-item-head">
                <div class="doc-title">{{ doc.title }}</div>
                <el-tag size="small" :type="doc.articleStatus === 'PUBLISHED' ? 'success' : doc.articleStatus === 'ARCHIVED' ? 'info' : 'warning'">
                  {{ getDocStatusText(doc.articleStatus) }}
                </el-tag>
              </div>
              <div class="doc-meta">
                <span>{{ getDocTypeText(doc.docType) }}</span>
                <span>{{ lt('版本', '版本', 'Version') }} v{{ doc.currentVersion }}</span>
                <span>{{ formatDate(doc.updatedAt) }}</span>
              </div>
              <div class="doc-summary">{{ doc.summary || lt('暂无摘要', '暫無摘要', 'No summary') }}</div>
              <div class="doc-tags" v-if="doc.tags?.length">
                <el-tag v-for="tag in doc.tags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="14">
        <el-card class="docs-detail-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>{{ lt('文档详情', '文件詳情', 'Document Detail') }}</span>
              <div class="detail-actions">
                <el-button v-if="selectedDoc" @click="openEdit">{{ lt('编辑并生成新版本', '編輯並產生新版本', 'Edit as New Version') }}</el-button>
              </div>
            </div>
          </template>

          <el-empty
            v-if="!selectedDoc"
            :description="lt('选择左侧文档即可查看版本历史和正文内容。', '選擇左側文件即可查看版本歷史和正文內容。', 'Select a document to inspect its versions and content.')"
          />
          <template v-else>
            <div class="detail-overview">
              <div>
                <div class="detail-title">{{ selectedDoc.title }}</div>
                <div class="detail-subtitle">{{ selectedDoc.summary || lt('暂无摘要', '暫無摘要', 'No summary') }}</div>
              </div>
              <div class="detail-badges">
                <el-tag>{{ getDocTypeText(selectedDoc.docType) }}</el-tag>
                <el-tag :type="selectedDoc.articleStatus === 'PUBLISHED' ? 'success' : selectedDoc.articleStatus === 'ARCHIVED' ? 'info' : 'warning'">{{ getDocStatusText(selectedDoc.articleStatus) }}</el-tag>
              </div>
            </div>

            <el-row :gutter="16">
              <el-col :xs="24" :xl="9">
                <div class="version-panel">
                  <div class="minor-title">{{ lt('版本历史', '版本歷史', 'Version History') }}</div>
                  <el-skeleton v-if="versionsLoading" :rows="4" animated />
                  <el-empty
                    v-else-if="!versions.length"
                    :description="lt('还没有版本记录。', '還沒有版本記錄。', 'No version history yet.')"
                  />
                  <div v-else class="version-list">
                    <div
                      v-for="version in versions"
                      :key="version.id"
                      class="version-item"
                      :class="{ active: activeVersionId === version.id }"
                      @click="activeVersionId = version.id"
                    >
                      <div class="version-title">v{{ version.versionNo }} · {{ version.titleSnapshot }}</div>
                      <div class="version-desc">{{ version.changeNote || lt('未填写变更说明', '未填寫變更說明', 'No change note') }}</div>
                      <div class="version-time">{{ formatDate(version.createdAt) }}</div>
                    </div>
                  </div>
                </div>
              </el-col>

              <el-col :xs="24" :xl="15">
                <div class="content-panel">
                  <div class="minor-title">{{ lt('正文内容', '正文內容', 'Content') }}</div>
                  <el-empty
                    v-if="!activeVersion"
                    :description="lt('选择一个版本查看正文。', '選擇一個版本查看正文。', 'Pick a version to preview its content.')"
                  />
                  <template v-else>
                    <div class="content-meta">{{ activeVersion.changeNote || lt('未填写变更说明', '未填寫變更說明', 'No change note') }}</div>
                    <pre class="content-preview">{{ activeVersion.contentMarkdown }}</pre>
                  </template>
                </div>
              </el-col>
            </el-row>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      v-model="editorVisible"
      :title="editingDocId ? lt('编辑文档并生成新版本', '編輯文件並產生新版本', 'Edit Document as New Version') : lt('新建文档', '新建文件', 'Create Document')"
      width="760px"
    >
      <el-form :model="editorForm" label-position="top">
        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <el-form-item :label="lt('文档类型', '文件類型', 'Document Type')">
              <el-select v-model="editorForm.docType">
                <el-option :label="lt('接入指南', '接入指南', 'Guide')" value="GUIDE" />
                <el-option :label="lt('提审清单', '提審清單', 'Checklist')" value="CHECKLIST" />
                <el-option :label="lt('案例条目', '案例條目', 'Case')" value="CASE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item :label="lt('状态', '狀態', 'Status')">
              <el-select v-model="editorForm.articleStatus">
                <el-option :label="lt('草稿', '草稿', 'Draft')" value="DRAFT" />
                <el-option :label="lt('已发布', '已發布', 'Published')" value="PUBLISHED" />
                <el-option :label="lt('已归档', '已歸檔', 'Archived')" value="ARCHIVED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="lt('标题', '標題', 'Title')">
          <el-input v-model="editorForm.title" maxlength="80" show-word-limit />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <el-form-item :label="lt('分类', '分類', 'Category')">
              <el-input v-model="editorForm.category" maxlength="64" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item :label="lt('标签（逗号分隔）', '標籤（逗號分隔）', 'Tags (comma separated)')">
              <el-input v-model="editorForm.tagsText" maxlength="200" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="lt('摘要', '摘要', 'Summary')">
          <el-input v-model="editorForm.summary" type="textarea" :rows="2" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item :label="lt('正文（Markdown）', '正文（Markdown）', 'Content (Markdown)')">
          <el-input v-model="editorForm.contentMarkdown" type="textarea" :rows="12" maxlength="20000" show-word-limit />
        </el-form-item>
        <el-form-item :label="lt('变更说明', '變更說明', 'Change Note')">
          <el-input v-model="editorForm.changeNote" maxlength="120" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">{{ lt('取消', '取消', 'Cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ lt('保存', '儲存', 'Save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createDeveloperDoc, getDeveloperDocs, getDeveloperDocVersions, updateDeveloperDoc } from '../api'
import { useI18nLite } from '../i18n'
import { formatDate } from '../utils/portal'

const { lt } = useI18nLite()
const route = useRoute()
const router = useRouter()

const docsLoading = ref(false)
const docsError = ref('')
const documents = ref([])
const versions = ref([])
const versionsLoading = ref(false)
const selectedDocId = ref(null)
const activeVersionId = ref(null)
const docTypeFilter = ref('')
const editorVisible = ref(false)
const editingDocId = ref(null)
const saving = ref(false)

const editorForm = reactive({
  docType: 'GUIDE',
  articleStatus: 'DRAFT',
  title: '',
  category: '',
  summary: '',
  tagsText: '',
  contentMarkdown: '',
  changeNote: ''
})

const selectedDoc = computed(() => documents.value.find((item) => item.id === selectedDocId.value) || null)
const activeVersion = computed(() => versions.value.find((item) => item.id === activeVersionId.value) || null)

const loadDocs = async () => {
  docsLoading.value = true
  docsError.value = ''
  try {
    const res = await getDeveloperDocs(docTypeFilter.value || undefined)
    documents.value = res.data || []
    if (!documents.value.length) {
      selectedDocId.value = null
      versions.value = []
      activeVersionId.value = null
      return
    }
    const routeDocId = Number(route.params.docId)
    if (routeDocId && documents.value.some((item) => item.id === routeDocId)) {
      selectedDocId.value = routeDocId
    } else if (!documents.value.some((item) => item.id === selectedDocId.value)) {
      selectedDocId.value = documents.value[0].id
    }
    await loadVersions(selectedDocId.value)
  } catch (error) {
    docsError.value = error?.message || lt('加载文档失败', '載入文件失敗', 'Failed to load docs')
  } finally {
    docsLoading.value = false
  }
}

const loadVersions = async (articleId) => {
  if (!articleId) {
    versions.value = []
    activeVersionId.value = null
    return
  }
  versionsLoading.value = true
  try {
    const res = await getDeveloperDocVersions(articleId)
    versions.value = res.data || []
    activeVersionId.value = versions.value[0]?.id || null
  } catch (error) {
    docsError.value = error?.message || lt('加载版本失败', '載入版本失敗', 'Failed to load versions')
  } finally {
    versionsLoading.value = false
  }
}

const handleSelectDoc = async (doc) => {
  selectedDocId.value = doc.id
  await loadVersions(doc.id)
  if (route.params.docId !== String(doc.id)) {
    router.push(`/docs/${doc.id}`)
  }
}

const handleFilterChange = async () => {
  selectedDocId.value = null
  await loadDocs()
}

const resetEditor = () => {
  editingDocId.value = null
  editorForm.docType = 'GUIDE'
  editorForm.articleStatus = 'DRAFT'
  editorForm.title = ''
  editorForm.category = ''
  editorForm.summary = ''
  editorForm.tagsText = ''
  editorForm.contentMarkdown = ''
  editorForm.changeNote = ''
}

const openCreate = (docType) => {
  resetEditor()
  editorForm.docType = docType
  editorVisible.value = true
}

const openEdit = () => {
  if (!selectedDoc.value) return
  const currentVersion = activeVersion.value || versions.value[0]
  editingDocId.value = selectedDoc.value.id
  editorForm.docType = selectedDoc.value.docType
  editorForm.articleStatus = selectedDoc.value.articleStatus
  editorForm.title = selectedDoc.value.title
  editorForm.category = selectedDoc.value.category || ''
  editorForm.summary = selectedDoc.value.summary || ''
  editorForm.tagsText = (selectedDoc.value.tags || []).join(', ')
  editorForm.contentMarkdown = currentVersion?.contentMarkdown || ''
  editorForm.changeNote = ''
  editorVisible.value = true
}

const handleSave = async () => {
  if (!editorForm.title.trim() || editorForm.title.trim().length < 2) {
    ElMessage.error(lt('标题至少 2 个字符', '標題至少 2 個字元', 'Title must be at least 2 characters'))
    return
  }
  if (!editorForm.contentMarkdown.trim() || editorForm.contentMarkdown.trim().length < 10) {
    ElMessage.error(lt('正文至少 10 个字符', '正文至少 10 個字元', 'Content must be at least 10 characters'))
    return
  }
  const payload = {
    docType: editorForm.docType,
    articleStatus: editorForm.articleStatus,
    title: editorForm.title,
    category: editorForm.category,
    summary: editorForm.summary,
    tags: editorForm.tagsText.split(',').map((item) => item.trim()).filter(Boolean),
    contentMarkdown: editorForm.contentMarkdown,
    changeNote: editorForm.changeNote
  }
  saving.value = true
  try {
    if (editingDocId.value) {
      await updateDeveloperDoc(editingDocId.value, payload)
      ElMessage.success(lt('新版本已保存', '新版本已儲存', 'New version saved'))
    } else {
      await createDeveloperDoc(payload)
      ElMessage.success(lt('文档已创建', '文件已建立', 'Document created'))
    }
    editorVisible.value = false
    await loadDocs()
    if (documents.value.length) {
      selectedDocId.value = editingDocId.value || documents.value[0].id
      await loadVersions(selectedDocId.value)
    }
  } catch (error) {
    ElMessage.error(error?.message || lt('保存文档失败', '儲存文件失敗', 'Failed to save document'))
  } finally {
    saving.value = false
  }
}

const getDocTypeText = (docType) => {
  const dict = {
    GUIDE: lt('接入指南', '接入指南', 'Guide'),
    CHECKLIST: lt('提审清单', '提審清單', 'Checklist'),
    CASE: lt('案例条目', '案例條目', 'Case')
  }
  return dict[docType] || docType || '-'
}

const getDocStatusText = (status) => {
  const dict = {
    DRAFT: lt('草稿', '草稿', 'Draft'),
    PUBLISHED: lt('已发布', '已發布', 'Published'),
    ARCHIVED: lt('已归档', '已歸檔', 'Archived')
  }
  return dict[status] || status || '-'
}

onMounted(loadDocs)
watch(() => route.params.docId, async (docId) => {
  if (!documents.value.length) return
  const numericDocId = Number(docId)
  if (!numericDocId) return
  const target = documents.value.find((item) => item.id === numericDocId)
  if (target && selectedDocId.value !== numericDocId) {
    selectedDocId.value = numericDocId
    await loadVersions(numericDocId)
  }
})
</script>

<style scoped>
.docs-page { padding: 24px; background: #f6f8fb; min-height: 100vh; }
.docs-header { margin-bottom: 16px; display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.docs-header h1 { margin: 0 0 8px; }
.docs-header p { margin: 0; color: #667085; }
.docs-header-actions { flex-shrink: 0; display: flex; gap: 8px; flex-wrap: wrap; }
.docs-alert { margin-bottom: 16px; }
.docs-list-card, .docs-detail-card { border-radius: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.doc-list { display: flex; flex-direction: column; gap: 12px; }
.doc-item { border: 1px solid #e4e7ec; border-radius: 14px; padding: 14px 16px; cursor: pointer; background: #fff; transition: all 0.2s ease; }
.doc-item:hover { border-color: #3b82f6; box-shadow: 0 8px 24px rgba(59, 130, 246, 0.08); }
.doc-item.active { border-color: #3b82f6; background: #eef4ff; }
.doc-item-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.doc-title { font-size: 15px; font-weight: 700; color: #111827; }
.doc-meta { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 6px; color: #667085; font-size: 12px; }
.doc-summary { margin-top: 8px; color: #475467; line-height: 1.6; font-size: 13px; }
.doc-tags { margin-top: 10px; display: flex; gap: 6px; flex-wrap: wrap; }
.detail-overview { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; margin-bottom: 16px; }
.detail-title { font-size: 18px; font-weight: 800; color: #101828; }
.detail-subtitle { margin-top: 8px; color: #667085; line-height: 1.6; }
.detail-badges { display: flex; gap: 8px; flex-wrap: wrap; }
.minor-title { font-size: 14px; font-weight: 700; color: #111827; margin-bottom: 10px; }
.version-list { display: flex; flex-direction: column; gap: 10px; }
.version-item { border: 1px solid #e4e7ec; border-radius: 12px; padding: 12px; cursor: pointer; transition: all 0.2s ease; background: #fff; }
.version-item.active { border-color: #2563eb; background: #eff6ff; }
.version-title { font-size: 14px; font-weight: 700; color: #111827; }
.version-desc { margin-top: 6px; color: #667085; font-size: 12px; line-height: 1.5; }
.version-time { margin-top: 8px; color: #98a2b3; font-size: 12px; }
.content-panel, .version-panel { min-height: 320px; }
.content-meta { margin-bottom: 12px; color: #667085; font-size: 13px; }
.content-preview { margin: 0; padding: 16px; border-radius: 14px; background: #0f172a; color: #e2e8f0; white-space: pre-wrap; line-height: 1.7; max-height: 520px; overflow: auto; }
@media (max-width: 900px) {
  .docs-header { flex-direction: column; }
  .detail-overview { flex-direction: column; }
}
</style>
