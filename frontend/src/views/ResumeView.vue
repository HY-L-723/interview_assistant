<template>
  <div class="resume-layout">
    <div class="resume-topbar">
      <el-button text @click="goBack"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
      <h2>AI 简历生成</h2>
    </div>

    <div class="resume-body">
      <!-- ====== 表单 ====== -->
      <div class="form-card">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">

          <!-- 基本信息 -->
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="姓名" prop="name">
                <el-input v-model="form.name" placeholder="请输入姓名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="求职意向">
                <el-input v-model="form.targetPosition" placeholder="如：Java 高级工程师" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="电话">
                <el-input v-model="form.phone" placeholder="请输入电话" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="邮箱">
                <el-input v-model="form.email" placeholder="请输入邮箱" />
              </el-form-item>
            </el-col>
          </el-row>

          <!-- 照片上传 -->
          <el-form-item label="个人照片">
            <div class="photo-area">
              <div v-if="form.photoUrl" class="photo-preview">
                <img :src="form.photoUrl" alt="照片预览" />
                <el-button type="danger" size="small" circle class="photo-remove"
                  @click="removePhoto"><el-icon><Delete /></el-icon></el-button>
              </div>
              <el-upload v-else
                :show-file-list="false"
                :before-upload="handlePhotoUpload"
                accept="image/*"
                drag
              >
                <el-icon :size="32"><Plus /></el-icon>
                <div>点击或拖拽上传照片</div>
              </el-upload>
            </div>
          </el-form-item>

          <!-- 教育背景 -->
          <div class="section-block">
            <div class="section-header">
              <span class="section-title">教育背景</span>
              <el-button type="primary" link size="small" @click="addEducation">
                <el-icon><Plus /></el-icon> 添加
              </el-button>
            </div>
            <div v-if="form.educationList.length === 0" class="section-empty">
              暂无，点击"添加"补充教育背景
            </div>
            <div v-for="(item, i) in form.educationList" :key="'edu' + i" class="entry-card">
              <el-row :gutter="12">
                <el-col :span="6">
                  <el-input v-model="item.school" placeholder="学校名称" size="small" />
                </el-col>
                <el-col :span="5">
                  <el-input v-model="item.degree" placeholder="学位/专业" size="small" />
                </el-col>
                <el-col :span="5">
                  <el-input v-model="item.startDate" placeholder="开始时间" size="small" />
                </el-col>
                <el-col :span="5">
                  <el-input v-model="item.endDate" placeholder="结束时间" size="small" />
                </el-col>
                <el-col :span="3">
                  <el-button type="danger" link size="small" @click="removeEducation(i)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-col>
              </el-row>
            </div>
          </div>

          <!-- 工作经历 -->
          <div class="section-block">
            <div class="section-header">
              <span class="section-title">工作经历</span>
              <el-button type="primary" link size="small" @click="addExperience">
                <el-icon><Plus /></el-icon> 添加
              </el-button>
            </div>
            <div v-if="form.experienceList.length === 0" class="section-empty">
              暂无，点击"添加"补充工作经历
            </div>
            <div v-for="(item, i) in form.experienceList" :key="'exp' + i" class="entry-card">
              <el-row :gutter="12">
                <el-col :span="5">
                  <el-input v-model="item.company" placeholder="公司名称" size="small" />
                </el-col>
                <el-col :span="4">
                  <el-input v-model="item.role" placeholder="职位" size="small" />
                </el-col>
                <el-col :span="5">
                  <el-input v-model="item.startDate" placeholder="开始时间" size="small" />
                </el-col>
                <el-col :span="5">
                  <el-input v-model="item.endDate" placeholder="结束时间" size="small" />
                </el-col>
                <el-col :span="3">
                  <el-button type="danger" link size="small" @click="removeExperience(i)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-col>
              </el-row>
              <el-input v-model="item.description" placeholder="工作描述（如：负责后端系统设计与开发...）"
                size="small" style="margin-top:8px" />
            </div>
          </div>

          <!-- 项目经历 -->
          <div class="section-block">
            <div class="section-header">
              <span class="section-title">项目经历</span>
              <el-button type="primary" link size="small" @click="addProject">
                <el-icon><Plus /></el-icon> 添加
              </el-button>
            </div>
            <div v-if="form.projectList.length === 0" class="section-empty">
              暂无，点击"添加"补充项目经历
            </div>
            <div v-for="(item, i) in form.projectList" :key="'proj' + i" class="entry-card">
              <el-row :gutter="12">
                <el-col :span="6">
                  <el-input v-model="item.name" placeholder="项目名称" size="small" />
                </el-col>
                <el-col :span="15">
                  <el-input v-model="item.technologies" placeholder="使用技术（如：Spring Boot、Vue）" size="small" />
                </el-col>
                <el-col :span="3">
                  <el-button type="danger" link size="small" @click="removeProject(i)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-col>
              </el-row>
              <el-input v-model="item.description" placeholder="项目描述（如：实现了什么功能，解决了什么问题...）"
                size="small" style="margin-top:8px" />
            </div>
          </div>

          <!-- 技能特长 -->
          <div class="section-block">
            <div class="section-header">
              <span class="section-title">技能特长</span>
            </div>
            <div class="skill-tags">
              <el-tag v-for="(item, i) in form.skillList" :key="'skill' + i"
                closable @close="removeSkill(i)" size="large" class="skill-tag">
                {{ item.name }}
              </el-tag>
            </div>
            <div class="skill-input-row">
              <el-input v-model="newSkillName" placeholder="输入技能名称"
                size="small" style="width:200px" @keyup.enter="addSkill" ref="skillInputRef" />
              <el-button size="small" @click="addSkill" :disabled="!newSkillName.trim()">
                <el-icon><Plus /></el-icon> 添加
              </el-button>
            </div>
          </div>

          <!-- 操作按钮 -->
          <el-form-item style="margin-top:16px">
            <el-button type="primary" :loading="generating" @click="handleGenerate" size="large">
              <el-icon><MagicStick /></el-icon> 生成简历
            </el-button>
            <el-button @click="resetForm" :disabled="generating">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- ====== 结果 ====== -->
      <div v-if="result" class="result-card">
        <div class="result-header">
          <h3>生成结果</h3>
          <div class="result-actions">
            <el-button type="primary" size="small" @click="downloadPdf">
              <el-icon><Download /></el-icon> 导出 PDF
            </el-button>
            <el-button text size="small" @click="copyResult">
              <el-icon><CopyDocument /></el-icon> 复制
            </el-button>
          </div>
        </div>
        <div class="markdown-body resume-content" v-html="renderMd(result.generatedContent)" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, MagicStick, CopyDocument, Plus, Delete, Download } from '@element-plus/icons-vue'
import { marked } from 'marked'
import { generateResume, uploadPhoto, downloadPdfUrl } from '../api/resume'

const router = useRouter()
const formRef = ref(null)
const generating = ref(false)
const result = ref(null)
const newSkillName = ref('')
const skillInputRef = ref(null)

const form = reactive({
  name: '',
  phone: '',
  email: '',
  photoUrl: '',
  targetPosition: '',
  educationList: [],
  experienceList: [],
  projectList: [],
  skillList: []
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

marked.setOptions({ breaks: true, gfm: true })

function renderMd(text) {
  return marked(text || '')
}

// ---- 照片上传 ----
async function handlePhotoUpload(file) {
  try {
    const res = await uploadPhoto(file)
    form.photoUrl = res.data
    ElMessage.success('照片上传成功')
  } catch {
    ElMessage.error('照片上传失败')
  }
  return false // 阻止 el-upload 默认上传
}

function removePhoto() {
  form.photoUrl = ''
}

// ---- 教育背景 ----
function addEducation() {
  form.educationList.push({ school: '', degree: '', startDate: '', endDate: '' })
}

function removeEducation(i) {
  form.educationList.splice(i, 1)
}

// ---- 工作经历 ----
function addExperience() {
  form.experienceList.push({ company: '', role: '', description: '', startDate: '', endDate: '' })
}

function removeExperience(i) {
  form.experienceList.splice(i, 1)
}

// ---- 项目经历 ----
function addProject() {
  form.projectList.push({ name: '', description: '', technologies: '' })
}

function removeProject(i) {
  form.projectList.splice(i, 1)
}

// ---- 技能 ----
function addSkill() {
  const name = newSkillName.value.trim()
  if (!name) return
  form.skillList.push({ name })
  newSkillName.value = ''
  nextTick(() => skillInputRef.value?.focus())
}

function removeSkill(i) {
  form.skillList.splice(i, 1)
}

// ---- 格式化各区块为文本 ----
function formatEducation() {
  return form.educationList
    .map(e => `${e.school || '?'} | ${e.degree || '?'} | ${e.startDate || '?'} - ${e.endDate || '?'}`)
    .join('\n')
}

function formatExperience() {
  return form.experienceList
    .map(e => `${e.company || '?'} | ${e.role || '?'} | ${e.startDate || '?'} - ${e.endDate || '?'}\n  ${e.description || ''}`)
    .join('\n')
}

function formatProjects() {
  return form.projectList
    .map(p => `${p.name || '?'}\n  技术栈: ${p.technologies || '?'}\n  描述: ${p.description || ''}`)
    .join('\n')
}

function formatSkills() {
  return form.skillList.map(s => s.name).join('、')
}

// ---- 重置 ----
function resetForm() {
  formRef.value?.resetFields()
  form.name = ''
  form.phone = ''
  form.email = ''
  form.photoUrl = ''
  form.targetPosition = ''
  form.educationList = []
  form.experienceList = []
  form.projectList = []
  form.skillList = []
  result.value = null
}

// ---- 生成 ----
async function handleGenerate() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  generating.value = true
  try {
    const data = { name: form.name }
    if (form.phone) data.phone = form.phone
    if (form.email) data.email = form.email
    if (form.photoUrl) data.photoUrl = form.photoUrl
    if (form.targetPosition) data.targetPosition = form.targetPosition

    const eduStr = formatEducation()
    if (eduStr) data.education = eduStr

    const expStr = formatExperience()
    if (expStr) data.experience = expStr

    const projStr = formatProjects()
    if (projStr) data.projects = projStr

    const skillStr = formatSkills()
    if (skillStr) data.skills = skillStr

    const res = await generateResume(data)
    result.value = res.data
    ElMessage.success('简历生成成功')
  } catch {
    /* handled */
  } finally {
    generating.value = false
  }
}

// ---- 复制 ----
function copyResult() {
  if (result.value?.generatedContent) {
    navigator.clipboard.writeText(result.value.generatedContent)
    ElMessage.success('已复制到剪贴板')
  }
}

// ---- PDF 下载 ----
function downloadPdf() {
  if (!result.value?.id) return
  const token = localStorage.getItem('token')
  const url = downloadPdfUrl(result.value.id)
  // 用 fetch 带 token 下载
  fetch(url, { headers: { Authorization: 'Bearer ' + token } })
    .then(res => {
      if (!res.ok) throw new Error('下载失败')
      return res.blob()
    })
    .then(blob => {
      const a = document.createElement('a')
      a.href = URL.createObjectURL(blob)
      a.download = 'resume.pdf'
      a.click()
      URL.revokeObjectURL(a.href)
      ElMessage.success('PDF 下载开始')
    })
    .catch(() => ElMessage.error('PDF 下载失败'))
}

function goBack() {
  router.push('/chat')
}
</script>

<style scoped>
.resume-layout { min-height: 100vh; background: #f0f2f5; }

.resume-topbar {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 24px; background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  position: sticky; top: 0; z-index: 10;
}
.resume-topbar h2 { margin: 0; font-size: 18px; color: #1f2937; }

.resume-body { max-width: 900px; margin: 24px auto; padding: 0 16px; }

.form-card {
  background: #fff; padding: 28px 32px; border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}

/* 照片 */
.photo-area { width: 120px; }
.photo-preview { position: relative; width: 100px; height: 120px; }
.photo-preview img {
  width: 100%; height: 100%; object-fit: cover; border-radius: 8px; border: 1px solid #e5e7eb;
}
.photo-remove { position: absolute; top: -8px; right: -8px; }

/* 动态区块 */
.section-block { margin-top: 8px; padding: 12px 0; border-top: 1px dashed #e5e7eb; }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.section-title { font-size: 14px; font-weight: 600; color: #374151; }
.section-empty { color: #9ca3af; font-size: 13px; padding: 8px 0; }

.entry-card {
  background: #f9fafb; padding: 12px; border-radius: 8px; margin-bottom: 8px;
  border: 1px solid #e5e7eb;
}

/* 技能 */
.skill-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.skill-tag { cursor: default; }
.skill-input-row { margin-top: 8px; display: flex; gap: 8px; align-items: center; }

/* 结果 */
.result-card {
  margin-top: 24px; background: #fff; padding: 24px 32px;
  border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}
.result-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.result-header h3 { margin: 0; font-size: 16px; color: #1f2937; }
.result-actions { display: flex; gap: 8px; align-items: center; }

.resume-content { font-size: 14px; line-height: 1.8; color: #374151; }
.resume-content :deep(h1), .resume-content :deep(h2), .resume-content :deep(h3) {
  margin: 12px 0 6px; color: #1f2937;
}
.resume-content :deep(ul), .resume-content :deep(ol) { padding-left: 20px; }
.resume-content :deep(li) { margin-bottom: 4px; }
.resume-content :deep(pre) {
  background: #f3f4f6; padding: 12px 16px; border-radius: 8px; overflow-x: auto;
}
.resume-content :deep(strong) { font-weight: 600; }
.resume-content :deep(hr) { border: none; border-top: 1px solid #e5e7eb; margin: 16px 0; }
</style>
