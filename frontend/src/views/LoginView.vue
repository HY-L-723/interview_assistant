<template>
  <div class="login-wrapper">
    <div class="login-card">
      <div class="logo-area">
        <div class="logo-icon">
          <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
            <rect width="40" height="40" rx="10" fill="#4F46E5"/>
            <path d="M12 14h16v2H12zm0 5h12v2H12zm0 5h14v2H12z" fill="#fff"/>
          </svg>
        </div>
        <h2 class="app-title">面试助手</h2>
        <p class="app-subtitle">AI 驱动的面试练习平台</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock"
            show-password size="large" @keyup.enter="handleSubmit" />
        </el-form-item>
        <el-form-item v-if="!isLogin" prop="email">
          <el-input v-model="form.email" placeholder="邮箱（选填）" :prefix-icon="Message" size="large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="submit-btn" size="large" @click="handleSubmit">
            {{ isLogin ? '登 录' : '注 册' }}
          </el-button>
        </el-form-item>
      </el-form>

      <p class="toggle-text">
        {{ isLogin ? '还没有账号？' : '已有账号？' }}
        <a href="javascript:;" @click="toggleMode">{{ isLogin ? '立即注册' : '去登录' }}</a>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { login, register } from '../api/auth'
import { createConversation } from '../api/chat'

const router = useRouter()
const formRef = ref(null)
const isLogin = ref(true)
const loading = ref(false)

const form = reactive({ username: '', password: '', email: '' })

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度 3-50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度 6-100 个字符', trigger: 'blur' }
  ]
}

function toggleMode() {
  isLogin.value = !isLogin.value
  formRef.value?.resetFields()
  form.email = ''
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    if (isLogin.value) {
      const res = await login(form.username, form.password)
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify(res.data))
      ElMessage.success('登录成功')
      const conv = await createConversation()
      router.push({ path: '/chat', query: { convId: conv.data.id } })
    } else {
      await register(form.username, form.password, form.email)
      ElMessage.success('注册成功，请登录')
      toggleMode()
    }
  } catch { /* 拦截器已处理 */ } finally { loading.value = false }
}
</script>

<style scoped>
.login-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f0f2f5;
}

.login-card {
  width: 420px;
  padding: 40px 44px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06);
}

.logo-area { text-align: center; margin-bottom: 28px; }
.logo-icon { display: inline-flex; margin-bottom: 12px; }

.app-title { margin: 0 0 6px; font-size: 22px; color: #1f2937; font-weight: 700; }
.app-subtitle { margin: 0; font-size: 13px; color: #9ca3af; }

.submit-btn { width: 100%; }

.toggle-text { text-align: center; color: #9ca3af; font-size: 13px; margin: 24px 0 0; }
.toggle-text a { color: #4F46E5; text-decoration: none; font-weight: 500; }
</style>
