<template>
  <div class="account-wrapper">
    <div class="account-card">
      <div class="card-header">
        <el-button text @click="goBack"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
        <h2>账户设置</h2>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" v-loading="loading">
        <el-divider content-position="left">基本信息</el-divider>

        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>

        <el-form-item label="注册时间" v-if="profile.createdAt">
          <span class="info-text">{{ fmtDate(profile.createdAt) }}</span>
        </el-form-item>

        <el-divider content-position="left">修改密码</el-divider>

        <el-form-item label="当前密码" prop="currentPassword">
          <el-input v-model="form.currentPassword" type="password" placeholder="如需修改密码，请输入当前密码" show-password />
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" placeholder="请输入新密码（至少6位）" show-password />
        </el-form-item>

        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getProfile, updateProfile } from '../api/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)

const profile = reactive({
  userId: null,
  username: '',
  email: '',
  createdAt: null
})

const form = reactive({
  username: '',
  email: '',
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (form.newPassword && !value) {
    callback(new Error('请确认新密码'))
  } else if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  email: [
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  newPassword: [
    { min: 6, max: 100, message: '密码长度 6-100 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getProfile()
    const p = res.data
    Object.assign(profile, p)
    form.username = p.username
    form.email = p.email || ''
  } catch { /* handled */ } finally { loading.value = false }
})

function resetForm() {
  form.email = profile.email || ''
  form.currentPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  formRef.value?.resetFields()
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const payload = {}
  if (form.email !== (profile.email || '')) {
    payload.email = form.email
  }
  if (form.newPassword) {
    if (!form.currentPassword) {
      ElMessage.warning('修改密码需要提供当前密码')
      return
    }
    payload.currentPassword = form.currentPassword
    payload.newPassword = form.newPassword
  }

  if (Object.keys(payload).length === 0) {
    ElMessage.info('没有需要修改的内容')
    return
  }

  saving.value = true
  try {
    const res = await updateProfile(payload)
    const p = res.data
    Object.assign(profile, p)
    form.email = p.email || ''
    form.currentPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
    // Update localStorage user info
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    user.email = p.email
    localStorage.setItem('user', JSON.stringify(user))
    formRef.value?.resetFields()
    ElMessage.success('修改成功')
  } catch { /* handled */ } finally { saving.value = false }
}

function fmtDate(ts) {
  if (!ts) return ''
  return new Date(ts).toLocaleDateString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

function goBack() {
  router.push('/chat')
}
</script>

<style scoped>
.account-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f0f2f5;
}

.account-card {
  width: 520px;
  padding: 32px 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
  color: #1f2937;
}

.info-text {
  color: #6b7280;
  font-size: 13px;
}
</style>
