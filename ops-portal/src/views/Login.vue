<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <template #header>
        <div class="auth-header">
          <h2>{{ lt('运营后台登录', '營運後台登入', 'Operations Admin Sign In') }}</h2>
          <p>{{ lt('请使用管理员账号登录，进入游戏审核工作台。', '請使用管理員帳號登入，進入遊戲審核工作台。', 'Sign in with an admin account to access the review console.') }}</p>
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
          <el-button type="primary" :loading="loading" class="full-width" @click="handleLogin">{{ lt('登录运营后台', '登入營運後台', 'Sign In to Ops Console') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api'
import { useUserStore } from '../stores/user'
import { useI18nLite } from '../i18n'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { lt } = useI18nLite()
const formRef = ref()
const loading = ref(false)

const form = reactive({ loginId: '', password: '' })
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

    if (res.data.user?.role !== 'ADMIN') {
      userStore.logout()
      ElMessage.error(lt('当前账号不是管理员，无法进入运营后台', '當前帳號不是管理員，無法進入營運後台', 'Current account is not admin and cannot access operations console'))
      return
    }

    ElMessage.success(lt('登录成功', '登入成功', 'Signed in successfully'))
    router.push(route.query.redirect || '/pro/dashboard')
  } catch (error) {
    ElMessage.error(error.message || lt('登录失败', '登入失敗', 'Sign in failed'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page { min-height: 100vh; display: grid; place-items: center; padding: 24px; }
.auth-card { width: min(420px, 100%); }
.auth-header h2 { margin: 0 0 8px; }
.auth-header p { margin: 0; color: #6b7280; }
.full-width { width: 100%; }
</style>
