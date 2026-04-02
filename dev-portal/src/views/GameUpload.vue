﻿<template>
  <div class="page-shell">
    <header class="page-header">
      <div>
        <h1>上传游戏</h1>
        <p>上传 ZIP 包并补充基础信息，上传成功后可在“我的游戏”手动提交审核。</p>
      </div>
      <div class="header-actions">
        <el-button @click="$router.push('/docs')">开发者文档</el-button>
        <el-button type="success" @click="downloadTemplate">下载模板</el-button>
      </div>
    </header>

    <el-card>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="游戏名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入游戏名称" />
        </el-form-item>

        <el-form-item label="游戏描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入游戏描述" />
        </el-form-item>

        <el-form-item label="游戏压缩包" prop="file">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :show-file-list="true"
            :limit="1"
            accept=".zip"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <template #trigger>
              <el-button type="primary">选择 ZIP 文件</el-button>
            </template>
            <template #tip>
              <div class="upload-tip">仅支持 `.zip` 文件，请确保入口文件为 `index.html`。</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="uploading" @click="handleUpload">提交审核</el-button>
          <el-button @click="$router.push('/games')">返回列表</el-button>
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

const router = useRouter()
const formRef = ref()
const uploadRef = ref()
const uploading = ref(false)

const form = reactive({
  name: '',
  description: '',
  file: null
})

const rules = {
  name: [{ required: true, message: '请输入游戏名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入游戏描述', trigger: 'blur' }],
  file: [{ required: true, message: '请上传游戏压缩包', trigger: 'change' }]
}

const handleFileChange = (uploadFile) => {
  const file = uploadFile?.raw
  if (!file) {
    form.file = null
    return
  }

  if (!file.name.endsWith('.zip')) {
    ElMessage.error('请上传 ZIP 格式文件')
    uploadRef.value?.clearFiles()
    form.file = null
    return
  }

  form.file = file
}

const handleFileRemove = () => {
  form.file = null
}

const downloadTemplate = () => {
  const link = document.createElement('a')
  link.href = '/downloads/minigame-starter.zip'
  link.download = 'minigame-starter.zip'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
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

    ElMessage.success('上传成功，请在列表中点击“提交审核”')
    router.push('/games')
  } catch (error) {
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.page-shell {
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-header h1 {
  margin: 0 0 8px;
}

.page-header p {
  margin: 0;
  color: #6b7280;
}

.header-actions {
  flex-shrink: 0;
  display: flex;
  gap: 8px;
}

.upload-tip {
  color: #6b7280;
}
</style>

