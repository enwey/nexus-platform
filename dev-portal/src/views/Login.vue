<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <template #header>
        <div class="auth-header">
          <h2>{{ lt('开发者登录', '開發者登入', 'Developer Sign In') }}</h2>
          <p>{{ lt('登录后可查看游戏、上传版本和进入运营工作台。', '登入後可查看遊戲、上傳版本並進入營運工作台。', 'Sign in to view games, upload versions, and access operation consoles.') }}</p>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item :label="lt('账号 / 邮箱', '帳號 / 電子郵件', 'Username / Email')" prop="loginId">
          <el-input v-model="form.loginId" :placeholder="lt('请输入账号或邮箱', '請輸入帳號或電子郵件', 'Enter username or email')" />
        </el-form-item>

        <el-form-item :label="lt('密码', '密碼', 'Password')" prop="password">
          <el-input v-model="form.password" type="password" show-password :placeholder="lt('请输入密码', '請輸入密碼', 'Enter password')" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" class="full-width" @click="handleLogin">
            {{ lt('登录', '登入', 'Sign In') }}
          </el-button>
        </el-form-item>

        <div class="auth-footer">
          <span>{{ lt('还没有账号？', '還沒有帳號？', 'No account yet?') }}</span>
          <el-link type="primary" @click="$router.push('/register')">{{ lt('立即注册', '立即註冊', 'Create one') }}</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api'
import { useUserStore } from '../stores/user'
import { useI18nLite } from '../i18n'

const router = useRouter()
const userStore = useUserStore()
const { lt } = useI18nLite()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  loginId: '',
  password: ''
})

const rules = {
  loginId: [{ required: true, message: lt('请输入账号或邮箱', '請輸入帳號或電子郵件', 'Enter username or email'), trigger: 'blur' }],
  password: [{ required: true, message: lt('请输入密码', '請輸入密碼', 'Enter password'), trigger: 'blur' }]
}

const handleLogin = async () => {
  try {
    await formRef.value.validate()
    loading.value = true

    const res = await login({
      email: form.loginId,
      username: form.loginId,
      password: form.password
    })
    userStore.setSession(res.data.user, res.data.token, res.data.refreshToken)

    ElMessage.success(lt('登录成功', '登入成功', 'Signed in successfully'))
    router.push('/dashboard')
  } catch (error) {
    ElMessage.error(error.message || lt('登录失败', '登入失敗', 'Sign in failed'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: linear-gradient(135deg, #f4f7fb 0%, #dfe9f3 100%);
}

.auth-card {
  width: min(420px, 100%);
}

.auth-header h2 {
  margin: 0 0 8px;
}

.auth-header p {
  margin: 0;
  color: #6b7280;
  line-height: 1.5;
}

.full-width {
  width: 100%;
}

.auth-footer {
  display: flex;
  justify-content: center;
  gap: 8px;
  color: #6b7280;
}
</style>
