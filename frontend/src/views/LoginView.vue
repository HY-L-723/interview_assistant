<template>
  <div class="login-wrapper">
    <section class="brand-panel">
      <div class="brand-mark"><el-icon><Briefcase /></el-icon></div>
      <div class="brand-copy">
        <span class="eyebrow">AI INTERVIEW STUDIO</span>
        <h1>让每一次练习，<br><span>都更接近理想 Offer</span></h1>
        <p>从简历优化到模拟面试，用 AI 陪你梳理表达、发现盲点，带着准备和自信走进面试现场。</p>
      </div>
      <div class="feature-list">
        <div><strong>01</strong><span>智能对话训练<small>针对岗位持续追问</small></span></div>
        <div><strong>02</strong><span>即时反馈建议<small>优化你的回答结构</small></span></div>
        <div><strong>03</strong><span>个性化面试准备<small>沉淀每一次成长</small></span></div>
      </div>
      <p class="brand-foot">INTERVIEW ASSISTANT · YOUR AI CAREER PARTNER</p>
    </section>
    <div class="login-card">
      <div class="logo-area">
        <span class="welcome-label">{{ isLogin ? 'WELCOME BACK' : 'GET STARTED' }}</span>
        <h2 class="app-title">{{ isLogin ? '欢迎回来' : '创建你的账号' }}</h2>
        <p class="app-subtitle">{{ isLogin ? '登录后继续你的面试准备' : '开启专属于你的 AI 面试训练' }}</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" size="large" />
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
import { User, Lock, Message, Briefcase } from '@element-plus/icons-vue'
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
  display: grid;
  grid-template-columns: minmax(440px, 1.08fr) minmax(420px, .92fr);
  min-height: 100vh;
  background: #f8fafc;
  color: #172033;
  overflow: hidden;
}

.brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: clamp(48px, 7vw, 110px);
  background:
    radial-gradient(circle at 82% 18%, rgba(112, 219, 204, .34), transparent 28%),
    radial-gradient(circle at 8% 92%, rgba(181, 221, 113, .28), transparent 30%),
    linear-gradient(145deg, #ecfbfc 0%, #effbf1 52%, #f4f8de 100%);
}
.brand-panel::after {
  content: ''; position: absolute; width: 420px; height: 420px; right: -210px; bottom: -210px;
  border: 1px solid rgba(31, 88, 90, .08); border-radius: 50%; box-shadow: 0 0 0 54px rgba(255,255,255,.16);
}
.brand-mark { position: absolute; top: 42px; left: clamp(48px, 7vw, 110px); display: grid; place-items: center; width: 44px; height: 44px; border-radius: 14px; background: #183c3f; color: #fff; font-size: 21px; box-shadow: 0 10px 24px rgba(24,60,63,.18); }
.eyebrow, .welcome-label { color: #267c78; font-size: 12px; font-weight: 800; letter-spacing: .18em; }
.brand-copy h1 { margin: 20px 0; font-size: clamp(38px, 4vw, 62px); line-height: 1.14; letter-spacing: -.04em; color: #173235; }
.brand-copy h1 span { color: #287d78; }
.brand-copy p { max-width: 610px; color: #5f7476; font-size: 16px; line-height: 1.9; }
.feature-list { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; margin-top: 50px; }
.feature-list div { display: flex; gap: 12px; padding-top: 16px; border-top: 1px solid rgba(33,91,89,.18); }
.feature-list strong { color: #3d9890; font-size: 12px; }
.feature-list span { color: #294b4d; font-size: 14px; font-weight: 700; }
.feature-list small { display: block; margin-top: 5px; color: #7a9292; font-size: 11px; font-weight: 400; }
.brand-foot { position: absolute; bottom: 28px; margin: 0; color: #8ca09d; font-size: 10px; letter-spacing: .12em; }
.login-card {
  align-self: center;
  justify-self: center;
  width: min(420px, calc(100% - 64px));
  padding: 48px;
  background: rgba(255,255,255,.9);
  border: 1px solid #edf0f3;
  border-radius: 24px;
  box-shadow: 0 24px 70px rgba(31, 45, 61, .09);
}

.logo-area { margin-bottom: 34px; }

.app-title { margin: 10px 0 8px; font-size: 30px; color: #172033; font-weight: 750; letter-spacing: -.03em; }
.app-subtitle { margin: 0; font-size: 14px; color: #8993a3; }
:deep(.el-input__wrapper) { min-height: 48px; border-radius: 12px; background: #f7f9fb; box-shadow: 0 0 0 1px #e8ecf1 inset; }
:deep(.el-input__wrapper.is-focus) { background: #fff; box-shadow: 0 0 0 1px #338d88 inset, 0 0 0 4px rgba(51,141,136,.08); }
:deep(.el-form-item) { margin-bottom: 20px; }

.submit-btn { width: 100%; height: 48px; border: 0; border-radius: 12px; background: #183c3f; font-weight: 700; letter-spacing: .12em; box-shadow: 0 10px 22px rgba(24,60,63,.18); }
.submit-btn:hover { background: #24585a; }

.toggle-text { text-align: center; color: #9ca3af; font-size: 13px; margin: 24px 0 0; }
.toggle-text a { color: #267c78; text-decoration: none; font-weight: 700; }
@media (max-width: 900px) {
  .login-wrapper { grid-template-columns: 1fr; background: linear-gradient(145deg, #ecfbfc, #f4f8de); }
  .brand-panel { display: none; }
  .login-card { margin: 32px auto; width: min(420px, calc(100% - 40px)); box-sizing: border-box; padding: 36px 28px; }
}
</style>
