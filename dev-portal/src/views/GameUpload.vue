<template>
  <div class="page-shell">
    <header class="page-header">
      <div>
        <h1>{{ lt('上传游戏', '上傳遊戲', 'Upload Game') }}</h1>
        <p>{{ lt('上传 ZIP 包并补充基础信息，提交后进入审核流程。', '上傳 ZIP 並補充基礎資訊，提交後進入審核流程。', 'Upload a ZIP and complete metadata to submit for review.') }}</p>
      </div>
    </header>

    <el-card>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item :label="lt('游戏名称', '遊戲名稱', 'Game Name')" prop="name">
          <el-input v-model="form.name" :placeholder="lt('请输入游戏名称', '請輸入遊戲名稱', 'Enter game name')" />
        </el-form-item>

        <el-form-item :label="lt('游戏描述', '遊戲描述', 'Description')" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" :placeholder="lt('请输入游戏描述', '請輸入遊戲描述', 'Enter game description')" />
        </el-form-item>

        <el-form-item :label="lt('游戏压缩包', '遊戲壓縮包', 'Game ZIP')" prop="file">
          <el-upload ref="uploadRef" :auto-upload="false" :show-file-list="true" :limit="1" accept=".zip" :on-change="handleFileChange" :on-remove="handleFileRemove">
            <template #trigger>
              <el-button type="primary">{{ lt('选择 ZIP 文件', '選擇 ZIP 檔案', 'Select ZIP File') }}</el-button>
            </template>
            <template #tip>
              <div class="upload-tip">{{ lt('仅支持 .zip 文件，请确保入口文件为 index.html。', '僅支援 .zip，請確認入口檔案為 index.html。', 'Only .zip is supported. Ensure entry file is index.html.') }}</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="uploading" @click="handleUpload">{{ lt('提交审核', '提交審核', 'Submit For Review') }}</el-button>
          <el-button @click="$router.push('/games')">{{ lt('返回列表', '返回列表', 'Back to List') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { uploadGame } from '../api'
import { useI18nLite } from '../i18n'

const router = useRouter()
const { lt } = useI18nLite()
const formRef = ref()
const uploadRef = ref()
const uploading = ref(false)

const form = reactive({ name: '', description: '', file: null })

const rules = {
  name: [{ required: true, message: lt('请输入游戏名称', '請輸入遊戲名稱', 'Enter game name'), trigger: 'blur' }],
  description: [{ required: true, message: lt('请输入游戏描述', '請輸入遊戲描述', 'Enter game description'), trigger: 'blur' }],
  file: [{ required: true, message: lt('请上传游戏压缩包', '請上傳遊戲壓縮包', 'Please upload game ZIP'), trigger: 'change' }]
}

const handleFileChange = (uploadFile) => {
  const file = uploadFile?.raw
  if (!file) {
    form.file = null
    return
  }

  if (!file.name.endsWith('.zip')) {
    ElMessage.error(lt('请上传 ZIP 格式文件', '請上傳 ZIP 格式檔案', 'Please upload a ZIP file'))
    uploadRef.value?.clearFiles()
    form.file = null
    return
  }

  form.file = file
}

const handleFileRemove = () => {
  form.file = null
}

const handleUpload = async () => {
  try {
    await formRef.value.validate()
    uploading.value = true

    const formData = new FormData()
    formData.append('file', form.file)
    formData.append('name', form.name)
    formData.append('description', form.description)

    await uploadGame(formData)

    ElMessage.success(lt('上传成功，已提交审核', '上傳成功，已提交審核', 'Upload successful, submitted for review'))
    router.push('/games')
  } catch (error) {
    ElMessage.error(error.message || lt('上传失败', '上傳失敗', 'Upload failed'))
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.page-shell { padding: 24px; }
.page-header { margin-bottom: 24px; }
.page-header h1 { margin: 0 0 8px; }
.page-header p { margin: 0; color: #6b7280; }
.upload-tip { color: #6b7280; }
</style>
