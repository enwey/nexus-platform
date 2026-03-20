<template>
  <div class="upload-container">
    <el-card>
      <template #header>
        <h2>上传游戏</h2>
      </template>
      
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="游戏名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入游戏名称" />
        </el-form-item>
        
        <el-form-item label="游戏描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入游戏描述"
          />
        </el-form-item>
        
        <el-form-item label="游戏文件" prop="file">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="1"
            accept=".zip"
            drag
          >
            <el-button type="primary">
              <el-icon><Upload /></el-icon>
              选择 ZIP 文件
            </el-button>
          </el-upload>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleUpload" :loading="uploading" style="width: 100%">
            上传
          </el-button>
          <el-button @click="$router.back()">
            取消
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { uploadGame } from '../api'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const uploadRef = ref()
const uploading = ref(false)
const fileList = ref([])

const form = reactive({
  name: '',
  description: '',
  file: null
})

const rules = {
  name: [
    { required: true, message: '请输入游戏名称', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入游戏描述', trigger: 'blur' }
  ]
}

const handleFileChange = (file) => {
  if (file && file.length > 0) {
    const selectedFile = file[0].raw
    if (!selectedFile.name.endsWith('.zip')) {
      ElMessage.error('请选择 ZIP 格式的文件')
      return
    }
    form.file = selectedFile
  }
}

const handleUpload = async () => {
  try {
    await formRef.value.validate()
    
    if (!form.file) {
      ElMessage.error('请选择游戏文件')
      return
    }
    
    uploading.value = true
    
    const formData = new FormData()
    formData.append('file', form.file)
    formData.append('name', form.name)
    formData.append('description', form.description)
    formData.append('developerId', userStore.user?.id)
    
    const res = await uploadGame(formData)
    
    if (res.code === 0) {
      ElMessage.success('上传成功，等待审核')
      router.push('/games')
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.upload-container {
  max-width: 800px;
  margin: 20px auto;
  padding: 20px;
}
</style>
