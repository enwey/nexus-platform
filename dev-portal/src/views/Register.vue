<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <template #header>
        <div class="auth-header">
          <h2>{{ lt('开发者注册', '開發者註冊', 'Developer Sign Up') }}</h2>
          <p>{{ lt('创建账号后即可管理小游戏版本和审核流程。', '建立帳號後即可管理小遊戲版本與審核流程。', 'Create an account to manage game versions and review flow.') }}</p>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item :label="lt('用户名', '使用者名稱', 'Username')" prop="username">
          <el-input v-model="form.username" :placeholder="lt('请输入用户名', '請輸入使用者名稱', 'Enter username')" />
        </el-form-item>

        <el-form-item :label="lt('密码', '密碼', 'Password')" prop="password">
          <el-input v-model="form.password" type="password" show-password :placeholder="lt('请输入密码', '請輸入密碼', 'Enter password')" />
        </el-form-item>

        <el-form-item :label="lt('确认密码', '確認密碼', 'Confirm Password')" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password :placeholder="lt('请再次输入密码', '請再次輸入密碼', 'Enter password again')" />
        </el-form-item>

        <el-form-item :label="lt('邮箱', '電子郵件', 'Email')" prop="email">
          <el-input v-model="form.email" :placeholder="lt('请输入邮箱', '請輸入電子郵件', 'Enter email')" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" class="full-width" @click="handleRegister">
            {{ lt('注册', '註冊', 'Sign Up') }}
          </el-button>
        </el-form-item>

        <div class="auth-footer">
          <span>{{ lt('已经有账号？', '已經有帳號？', 'Already have an account?') }}</span>
          <el-link type="primary" @click="$router.push('/login')">{{ lt('返回登录', '返回登入', 'Back to sign in') }}</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../api'
import { useUserStore } from '../stores/user'
import { useI18nLite } from '../i18n'

const router = useRouter()
const userStore = useUserStore()
const { lt } = useI18nLite()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: ''
})

const validateConfirmPassword = (_, value, callback) => {
  if (!value) {
    callback(new Error(lt('请再次输入密码', '請再次輸入密碼', 'Please enter password again')))
    return
  }

  if (value !== form.password) {
    callback(new Error(lt('两次输入的密码不一致', '兩次輸入的密碼不一致', 'Passwords do not match')))
    return
  }

  callback()
}

const rules = {
  username: [
    { required: true, message: lt('请输入用户名', '請輸入使用者名稱', 'Enter username'), trigger: 'blur' },
    { min: 3, max: 20, message: lt('用户名长度需为 3 到 20 个字符', '使用者名稱長度需為 3 到 20 個字元', 'Username must be 3 to 20 characters'), trigger: 'blur' }
  ],
  password: [
    { required: true, message: lt('请输入密码', '請輸入密碼', 'Enter password'), trigger: 'blur' },
    { min: 6, message: lt('密码长度不能少于 6 位', '密碼長度不能少於 6 位', 'Password must be at least 6 characters'), trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
  email: [
    { required: true, message: lt('请输入邮箱', '請輸入電子郵件', 'Enter email'), trigger: 'blur' },
    { type: 'email', message: lt('请输入正确的邮箱地址', '請輸入正確的電子郵件地址', 'Enter a valid email address'), trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  try {
    await formRef.value.validate()
    loading.value = true

    const res = await register({
      username: form.username,
      password: form.password,
      email: form.email
    })

    userStore.setSession(res.data.user, res.data.token, res.data.refreshToken)
    ElMessage.success(lt('注册成功，已自动登录', '註冊成功，已自動登入', 'Sign up successful. Signed in automatically.'))
    router.push('/dashboard')
  } catch (error) {
    ElMessage.error(error.message || lt('注册失败', '註冊失敗', 'Sign up failed'))
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
  width: min(460px, 100%);
}

.auth-header h2 {
  margin: 0 0 8px;
}

.auth-header p {
  margin: 0;
  color: #6b7280;
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
