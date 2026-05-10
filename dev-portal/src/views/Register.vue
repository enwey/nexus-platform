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
        <el-form-item :label="lt('邮箱', '電子郵件', 'Email')" prop="email">
          <el-input v-model="form.email" :placeholder="lt('请输入邮箱', '請輸入電子郵件', 'Enter email')" />
        </el-form-item>

        <el-form-item :label="lt('验证码', '驗證碼', 'Verification Code')" prop="code">
          <div class="code-row">
            <el-input v-model="form.code" :placeholder="lt('请输入验证码', '請輸入驗證碼', 'Enter verification code')" />
            <el-button :loading="sendingCode" :disabled="codeCountdown > 0" @click="handleSendCode">
              {{ codeCountdown > 0 ? `${codeCountdown}s` : lt('获取验证码', '獲取驗證碼', 'Get Code') }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item :label="lt('密码', '密碼', 'Password')" prop="password">
          <el-input v-model="form.password" type="password" show-password :placeholder="lt('请输入密码', '請輸入密碼', 'Enter password')" />
        </el-form-item>

        <el-form-item :label="lt('确认密码', '確認密碼', 'Confirm Password')" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password :placeholder="lt('请再次输入密码', '請再次輸入密碼', 'Enter password again')" />
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
import { onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register, sendCode } from '../api'
import { useUserStore } from '../stores/user'
import { useI18nLite } from '../i18n'

const router = useRouter()
const userStore = useUserStore()
const { lt } = useI18nLite()
const formRef = ref()
const loading = ref(false)
const sendingCode = ref(false)
const codeCountdown = ref(0)
let countdownTimer = null

const form = reactive({
  code: '',
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
  password: [
    { required: true, message: lt('请输入密码', '請輸入密碼', 'Enter password'), trigger: 'blur' },
    { min: 8, message: lt('密码长度不能少于 8 位', '密碼長度不能少於 8 位', 'Password must be at least 8 characters'), trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
  code: [{ required: true, message: lt('请输入验证码', '請輸入驗證碼', 'Enter verification code'), trigger: 'blur' }],
  email: [
    { required: true, message: lt('请输入邮箱', '請輸入電子郵件', 'Enter email'), trigger: 'blur' },
    { type: 'email', message: lt('请输入正确的邮箱地址', '請輸入正確的電子郵件地址', 'Enter a valid email address'), trigger: 'blur' }
  ]
}

const clearCountdown = () => {
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
    countdownTimer = null
  }
}

const startCountdown = () => {
  clearCountdown()
  codeCountdown.value = 60
  countdownTimer = window.setInterval(() => {
    codeCountdown.value -= 1
    if (codeCountdown.value <= 0) {
      clearCountdown()
    }
  }, 1000)
}

const handleSendCode = async () => {
  if (!form.email) {
    ElMessage.warning(lt('请先输入邮箱', '請先輸入電子郵件', 'Please enter email first'))
    return
  }

  try {
    sendingCode.value = true
    await sendCode({
      email: form.email,
      purpose: 'REGISTER',
      source: 'dev-portal',
      scene: 'DEV_PORTAL_REGISTER'
    })
    ElMessage.success(lt('验证码已发送，请查收邮箱', '驗證碼已發送，請查收信箱', 'Verification code sent. Please check your email.'))
    startCountdown()
  } catch (error) {
    ElMessage.error(error.message || lt('发送验证码失败', '發送驗證碼失敗', 'Failed to send verification code'))
  } finally {
    sendingCode.value = false
  }
}

const handleRegister = async () => {
  try {
    await formRef.value.validate()
    loading.value = true

    const res = await register({
      password: form.password,
      email: form.email,
      code: form.code,
      accountType: 'DEVELOPER'
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

onBeforeUnmount(() => {
  clearCountdown()
})
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

.code-row {
  display: flex;
  gap: 12px;
}

.code-row .el-input {
  flex: 1;
}

.auth-footer {
  display: flex;
  justify-content: center;
  gap: 8px;
  color: #6b7280;
}

@media (max-width: 520px) {
  .auth-page {
    padding: 16px;
  }

  .code-row {
    flex-direction: column;
  }

  .code-row :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }

  .auth-footer {
    flex-wrap: wrap;
  }
}
</style>
