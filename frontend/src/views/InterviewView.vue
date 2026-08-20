<template>
  <div class="layout">
    <!-- ====== sidebar ====== -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-top">
        <h3 class="sidebar-title">面试助手</h3>
        <div class="sidebar-actions">
          <el-button type="primary" class="new-conv-btn" @click="handleNewConversation">
            <el-icon><Plus /></el-icon> 新对话
          </el-button>
          <el-button class="resume-btn" @click="goResume">
            <el-icon><Document /></el-icon> 简历生成
          </el-button>
          <el-button class="interview-nav-btn active-nav" @click="goInterview">
            <el-icon><ChatLineSquare /></el-icon> 模拟面试
          </el-button>
        </div>
      </div>

      <!-- 面试历史记录列表 -->
      <div class="session-list" v-if="interviewSessions.length > 0">
        <div
          v-for="sess in interviewSessions"
          :key="sess.id"
          :class="['session-item', { active: sess.id === activeSessionId }]"
          @click="viewInterviewSession(sess)"
        >
          <div class="session-title">{{ sess.position }}</div>
          <div class="session-meta">
            <span>{{ sess.answeredCount }}/{{ sess.totalQuestions }} 题</span>
            <el-tag :type="sessionStatusTag(sess.status)" size="small">
              {{ sessionStatusText(sess.status) }}
            </el-tag>
          </div>
          <div class="session-time">{{ fmtDate(sess.createdAt) }}</div>
        </div>
      </div>
      <div v-else class="conv-empty">暂无面试记录</div>

      <div class="sidebar-footer">
        <el-popover placement="top" :width="220" trigger="click">
          <template #reference>
            <span class="user-name">{{ username }}</span>
          </template>
          <div class="user-info-pop">
            <p><strong>用户名：</strong>{{ username }}</p>
            <p><strong>邮箱：</strong>{{ userEmail || '未设置' }}</p>
            <p><strong>用户ID：</strong>{{ userId }}</p>
            <el-button type="primary" size="small" @click="goAccount" style="width:100%;margin-top:8px">
              设置
            </el-button>
            <el-divider style="margin:10px 0" />
            <el-button type="danger" size="small" @click="handleLogout" style="width:100%">
              退出登录
            </el-button>
          </div>
        </el-popover>
      </div>
    </aside>

    <!-- ====== main ====== -->
    <main class="main">
      <!-- 顶栏：折叠按钮 + 状态栏 -->
      <div class="top-bar">
        <el-button text class="toggle-btn" @click="sidebarCollapsed = !sidebarCollapsed">
          <el-icon :size="18">
            <ArrowRight v-if="sidebarCollapsed" />
            <ArrowLeft v-else />
          </el-icon>
        </el-button>
        <div class="status-bar" v-if="sessionId">
          <span class="status-tag">
            <el-tag size="small" type="info">{{ sessionPosition }}</el-tag>
          </span>
          <span class="status-progress" v-if="totalQuestions > 0">
            进度：<strong>{{ answeredCount }}/{{ totalQuestions }}</strong>
          </span>
          <span class="status-indicator">
            <el-tag :type="statusTagType" size="small">{{ statusLabel }}</el-tag>
          </span>
          <el-button
            v-if="interviewState === 'in_progress' || interviewState === 'submitting'"
            type="danger"
            size="small"
            plain
            :disabled="terminating"
            @click="handleTerminate"
            style="margin-left: auto;"
          >
            <el-icon><Close /></el-icon> 终止面试
          </el-button>
        </div>
      </div>

      <!-- 消息区 -->
      <div class="msg-area" ref="msgContainer" @scroll="onScroll">
        <!-- 加载历史记录 -->
        <div v-if="loadingSession" class="center-state">
          <el-icon class="is-loading" :size="28"><Loading /></el-icon>
          <span>加载面试记录...</span>
        </div>

        <!-- 初始状态：输入岗位 -->
        <div v-else-if="interviewState === 'idle'" class="center-state welcome">
          <div class="welcome-icon">
            <el-icon :size="56"><ChatLineSquare /></el-icon>
          </div>
          <h3>模拟面试</h3>
          <p>AI 面试官将根据你的目标岗位，为你生成专属面试题</p>
          <p class="welcome-hint">请在下方输入你想面试的岗位开始</p>
        </div>

        <!-- 消息列表 -->
        <template v-else>
          <template v-for="(msg, i) in messages" :key="i">
            <!-- AI 消息 -->
            <div v-if="msg.role === 'assistant'" class="msg-row assistant">
              <div class="msg-avatar">
                <el-icon :size="22"><Service /></el-icon>
              </div>
              <div class="msg-body">
                <!-- 普通文本消息（欢迎词等） -->
                <div v-if="msg.kind === 'greeting' || msg.kind === 'info'" class="msg-bubble ai">
                  {{ msg.content }}
                </div>
                <!-- 题目卡片 -->
                <div v-else-if="msg.kind === 'question'" class="question-card">
                  <div class="question-header">
                    <span class="question-number">第 {{ msg.questionNumber }} 题</span>
                    <el-tag v-if="msg.category" size="small" type="info">{{ msg.category }}</el-tag>
                    <el-tag v-if="msg.difficulty" size="small"
                      :type="msg.difficulty === '基础' ? 'success' : msg.difficulty === '进阶' ? 'warning' : 'danger'">
                      {{ msg.difficulty }}
                    </el-tag>
                  </div>
                  <div class="question-text">{{ msg.questionText }}</div>
                </div>
                <div class="msg-time">{{ msg.time }}</div>
              </div>
            </div>

            <!-- 用户消息 -->
            <div v-else-if="msg.role === 'user'" class="msg-row user">
              <div class="msg-body">
                <div class="msg-bubble user">{{ msg.content }}</div>
                <div class="msg-time">{{ msg.time }}</div>
              </div>
              <div class="msg-avatar user-avatar">
                <el-icon :size="22"><UserFilled /></el-icon>
              </div>
            </div>
          </template>

          <!-- typing 动画 -->
          <div v-if="typing" class="msg-row assistant">
            <div class="msg-avatar">
              <el-icon :size="22"><Service /></el-icon>
            </div>
            <div class="msg-body">
              <div class="msg-bubble ai typing-bubble">
                <span class="dot"></span><span class="dot"></span><span class="dot"></span>
              </div>
            </div>
          </div>

          <!-- 评分报告卡片 -->
          <div v-if="evaluation" class="evaluation-card">
            <div class="eval-header">
              <h2>{{ interviewState === 'terminated' ? '📋 阶段性评价' : '🎉 面试完成' }}</h2>
              <div class="eval-score-big">
                <span class="score-num">{{ evaluation.overallScore }}</span>
                <span class="score-unit">/100</span>
              </div>
              <div class="score-stars">
                <span v-for="s in 5" :key="s" class="star" :class="{ filled: s <= starRating }">⭐</span>
              </div>
            </div>

            <div class="eval-section">
              <h4>📝 总体评价</h4>
              <p>{{ evaluation.overallComment }}</p>
            </div>

            <div class="eval-section" v-if="evaluation.questions && evaluation.questions.length > 0">
              <h4>📋 各题评分</h4>
              <div class="question-scores">
                <div v-for="q in evaluation.questions" :key="q.questionNumber" class="score-item">
                  <div class="score-item-header">
                    <span class="score-item-num">第{{ q.questionNumber }}题</span>
                    <span class="score-item-score" :class="scoreColorClass(q.score)">{{ q.score }}分</span>
                  </div>
                  <div class="score-item-question">{{ q.questionText }}</div>
                  <div class="score-item-answer" v-if="q.userAnswer">
                    <span class="label">你的回答：</span>{{ q.userAnswer }}
                  </div>
                  <div class="score-item-comment" v-if="q.comment">
                    <span class="label">点评：</span>{{ q.comment }}
                  </div>
                  <div class="score-item-reference" v-if="q.referenceAnswer">
                    <span class="label">参考答案：</span>{{ q.referenceAnswer }}
                  </div>
                </div>
              </div>
            </div>

            <div class="eval-section" v-if="evaluation.studyAdvice">
              <h4>📚 学习建议</h4>
              <p>{{ evaluation.studyAdvice }}</p>
            </div>

            <div class="eval-actions">
              <el-button type="primary" @click="resetInterview">
                <el-icon><Refresh /></el-icon> 再来一次
              </el-button>
              <el-button @click="goChat">返回首页</el-button>
            </div>
          </div>

          <!-- 终止确认（无评价时） -->
          <div v-if="interviewState === 'terminated' && !evaluation" class="center-state terminated-notice">
            <el-icon :size="48"><CircleCloseFilled /></el-icon>
            <h3>面试已终止</h3>
            <p>
              本次面试共 {{ totalQuestions }} 题，已完成 {{ answeredCount }} 题
              <template v-if="answeredCount === 0">，未产生评价记录</template>
            </p>
            <el-button type="primary" style="margin-top:12px" @click="resetInterview">
              <el-icon><Refresh /></el-icon> 再来一次
            </el-button>
          </div>
        </template>

        <div v-if="showScrollBtn" class="scroll-btn" @click="scrollToBottom">
          <el-icon><ArrowDown /></el-icon>
        </div>
      </div>

      <!-- 输入区 -->
      <footer class="footer">
        <!-- idle: 岗位输入 -->
        <div v-if="interviewState === 'idle'" class="input-row">
          <el-input
            v-model="positionInput"
            placeholder="请输入面试岗位，如：Java后端开发工程师"
            size="large"
            :disabled="starting"
            @keyup.enter="handleStart"
            class="position-input"
          />
          <el-button
            type="primary"
            size="large"
            :loading="starting"
            :disabled="!positionInput.trim() || starting"
            @click="handleStart"
          >
            <el-icon><VideoPlay /></el-icon> 开始面试
          </el-button>
        </div>

        <!-- in_progress / submitting: 回答输入 -->
        <div
          v-else-if="interviewState === 'in_progress' || interviewState === 'submitting'"
          class="input-row"
        >
          <el-input
            v-model="answerInput"
            placeholder="输入你的回答，按 Ctrl+Enter 发送"
            type="textarea"
            :rows="3"
            :disabled="interviewState === 'submitting'"
            @keyup.ctrl.enter="handleSubmitAnswer"
            class="answer-input"
          />
          <div class="answer-actions">
            <span class="answer-hint">Ctrl+Enter 发送</span>
            <el-button
              type="primary"
              :loading="interviewState === 'submitting'"
              :disabled="!answerInput.trim() || interviewState === 'submitting'"
              @click="handleSubmitAnswer"
            >
              <el-icon><Promotion /></el-icon> 提交回答
            </el-button>
          </div>
        </div>

        <!-- evaluating: 等待中 -->
        <div v-else-if="interviewState === 'evaluating'" class="input-row evaluating-hint">
          <el-icon class="is-loading" :size="20"><Loading /></el-icon>
          <span>AI 正在生成面试评价，请稍候...</span>
        </div>

        <!-- completed / terminated: 结束 -->
        <div
          v-else-if="interviewState === 'completed' || interviewState === 'terminated'"
          class="input-row completed-hint"
        >
          <span v-if="interviewState === 'completed'">面试已结束，查看上方评价报告</span>
          <span v-else-if="evaluation">面试已终止，查看上方阶段性评价</span>
          <span v-else>面试已终止</span>
        </div>
      </footer>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Plus, Document, Close, ArrowLeft, ArrowRight, ArrowDown,
  ChatLineSquare, Service, UserFilled, VideoPlay, Promotion, Loading,
  Refresh, CircleCloseFilled
} from '@element-plus/icons-vue'
import {
  startInterview, submitAnswer, terminateInterview,
  getInterviewSessions, getInterviewSessionDetail
} from '../api/interview'
import { createConversation } from '../api/chat'

const router = useRouter()
const msgContainer = ref(null)
const showScrollBtn = ref(false)
const sidebarCollapsed = ref(false)

// ===== 用户信息 =====
const user = JSON.parse(localStorage.getItem('user') || '{}')
const username = user.username || '用户'
const userEmail = user.email || ''
const userId = user.userId || ''

// ===== 侧边栏：面试历史记录 =====
const interviewSessions = reactive([])
const activeSessionId = ref(null)
const loadingSession = ref(false)

onMounted(async () => {
  document.title = '面试助手 - 模拟面试'
  await loadInterviewSessions()
})

/**
 * 加载面试历史记录列表。
 */
async function loadInterviewSessions() {
  try {
    const res = await getInterviewSessions()
    interviewSessions.splice(0, interviewSessions.length, ...(res.data || []))
  } catch { /* handled */ }
}

/**
 * "新对话"按钮 → 创建新对话并跳转到 AI 聊天页面。
 */
async function handleNewConversation() {
  try {
    const res = await createConversation()
    const conv = res.data
    router.push(`/chat?convId=${conv.id}`)
  } catch {
    // 如果创建失败也跳转过去，ChatView 会自行创建默认对话
    router.push('/chat')
  }
}

/**
 * 点击某条面试历史记录 → 加载并展示面试详情。
 */
async function viewInterviewSession(sess) {
  if (loadingSession.value) return
  if (sess.id === activeSessionId.value && sessionId.value) return

  // 如果有进行中的面试，先取消
  if (abortController) {
    abortController.abort()
    abortController = null
  }

  activeSessionId.value = sess.id
  loadingSession.value = true

  // 先清空当前内容
  resetInterviewState()

  try {
    const res = await getInterviewSessionDetail(sess.id)
    const detail = res.data

    sessionId.value = detail.id
    sessionPosition.value = detail.position
    totalQuestions.value = detail.totalQuestions || 0
    answeredCount.value = detail.answeredCount || 0

    // 从 questions 数组重建 messages
    if (detail.questions && detail.questions.length > 0) {
      detail.questions.forEach(q => {
        // AI 题目消息
        messages.push({
          role: 'assistant',
          kind: 'question',
          questionNumber: q.questionNumber,
          questionText: q.questionText,
          category: q.category || '',
          difficulty: q.difficulty || '',
          time: fmtTime(new Date(q.createdAt))
        })
        // 用户回答消息（如果有）
        if (q.userAnswer) {
          messages.push({
            role: 'user',
            kind: 'answer',
            content: q.userAnswer,
            time: fmtTime(new Date(q.answeredAt))
          })
        }
      })
    }

    // 如果有总评，构建 evaluation
    if (detail.overallScore != null || detail.overallComment) {
      evaluation.value = {
        overallScore: detail.overallScore || 0,
        overallComment: detail.overallComment || '',
        studyAdvice: detail.studyAdvice || '',
        questions: (detail.questions || [])
          .filter(q => q.score != null)
          .map(q => ({
            questionNumber: q.questionNumber,
            questionText: q.questionText,
            userAnswer: q.userAnswer || '',
            score: q.score,
            comment: q.comment || '',
            referenceAnswer: q.referenceAnswer || ''
          }))
      }
    }

    // 根据状态设置面试状态
    if (detail.status === 'COMPLETED') {
      interviewState.value = 'completed'
    } else if (detail.status === 'TERMINATED') {
      interviewState.value = 'terminated'
    } else if (detail.status === 'IN_PROGRESS') {
      interviewState.value = 'in_progress'
    } else if (detail.status === 'EVALUATING') {
      interviewState.value = 'evaluating'
    } else {
      interviewState.value = 'completed'
    }

    nextTick(() => scrollToBottom(false))
  } catch {
    ElMessage.error('加载面试记录失败')
    activeSessionId.value = null
  } finally {
    loadingSession.value = false
  }
}

function sessionStatusTag(status) {
  const map = { COMPLETED: 'success', TERMINATED: 'danger', IN_PROGRESS: 'warning', EVALUATING: 'warning' }
  return map[status] || 'info'
}

function sessionStatusText(status) {
  const map = { COMPLETED: '已完成', TERMINATED: '已终止', IN_PROGRESS: '进行中', EVALUATING: '评价中' }
  return map[status] || status
}

// ===== 工具函数 =====
function fmtDate(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const yesterday = today - 86400000
  const tsTime = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  if (tsTime === today) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  if (tsTime === yesterday) return '昨天'
  return `${d.getMonth() + 1}/${d.getDate()} ${d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
}

function fmtTime(date) {
  return date ? date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : ''
}

// ===== 面试状态管理 =====
// idle | starting | in_progress | submitting | evaluating | completed | terminated
const interviewState = ref('idle')
const positionInput = ref('')
const answerInput = ref('')
const starting = ref(false)
const submitting = ref(false)
const terminating = ref(false)
const typing = ref(false)
const sessionId = ref(null)
const sessionPosition = ref('')
const totalQuestions = ref(0)
const answeredCount = ref(0)

const messages = reactive([])
const evaluation = ref(null)
let abortController = null

// ===== 计算属性 =====
const statusLabel = computed(() => {
  const map = {
    idle: '待开始',
    starting: '准备中',
    in_progress: '进行中',
    submitting: '评估中',
    evaluating: '生成评价',
    completed: '已完成',
    terminated: '已终止'
  }
  return map[interviewState.value] || ''
})

const statusTagType = computed(() => {
  const map = {
    idle: 'info',
    starting: 'warning',
    in_progress: 'success',
    submitting: 'warning',
    evaluating: 'warning',
    completed: 'success',
    terminated: 'danger'
  }
  return map[interviewState.value] || 'info'
})

const starRating = computed(() => {
  if (!evaluation.value) return 0
  const score = evaluation.value.overallScore || 0
  if (score >= 90) return 5
  if (score >= 75) return 4
  if (score >= 60) return 3
  if (score >= 40) return 2
  return 1
})

function scoreColorClass(score) {
  if (score >= 90) return 'score-high'
  if (score >= 75) return 'score-mid'
  if (score >= 60) return 'score-low'
  return 'score-fail'
}

// ===== 面试流程 =====

/**
 * 开始一场新的面试。
 */
async function handleStart() {
  const position = positionInput.value.trim()
  if (!position || starting.value) return

  starting.value = true
  interviewState.value = 'starting'
  typing.value = true
  abortController = new AbortController()

  await startInterview(position, {
    onGreeting: (message) => {
      typing.value = false
      messages.push({
        role: 'assistant',
        kind: 'greeting',
        content: message,
        time: fmtTime(new Date())
      })
    },
    onSessionCreated: (data) => {
      sessionId.value = data.sessionId
      sessionPosition.value = data.position
      totalQuestions.value = data.totalQuestions
      activeSessionId.value = data.sessionId
      // 刷新侧边栏历史列表
      loadInterviewSessions()
    },
    onQuestion: (data) => {
      messages.push({
        role: 'assistant',
        kind: 'question',
        questionNumber: data.questionNumber,
        questionText: data.questionText,
        category: data.category || '',
        difficulty: data.difficulty || '',
        totalQuestions: data.totalQuestions,
        time: fmtTime(new Date())
      })
      interviewState.value = 'in_progress'
      scrollToBottom()
    },
    onError: (message) => {
      ElMessage.error(message)
      interviewState.value = 'idle'
      typing.value = false
    }
  }, abortController.signal)

  starting.value = false
  abortController = null
  scrollToBottom()
}

/**
 * 提交当前题目的回答。
 */
async function handleSubmitAnswer() {
  const answer = answerInput.value.trim()
  if (!answer || interviewState.value !== 'in_progress' || submitting.value) return

  answerInput.value = ''
  submitting.value = true
  interviewState.value = 'submitting'
  typing.value = true
  abortController = new AbortController()

  // 显示用户回答消息
  messages.push({
    role: 'user',
    kind: 'answer',
    content: answer,
    time: fmtTime(new Date())
  })
  scrollToBottom()

  await submitAnswer(sessionId.value, answer, {
    onAnswerSaved: (data) => {
      answeredCount.value = data.answeredCount
      totalQuestions.value = data.totalQuestions
      typing.value = false
      messages.push({
        role: 'assistant',
        kind: 'info',
        content: `本题得分：${data.score}分。${data.comment || ''}`,
        time: fmtTime(new Date())
      })
    },
    onDecision: (data) => {
      messages.push({
        role: 'assistant',
        kind: 'info',
        content: data.action === 'CONTINUE'
          ? `${data.reason}${data.focus ? `：${data.focus}` : ''}`
          : data.reason,
        time: fmtTime(new Date())
      })
    },
    onQuestion: (data) => {
      messages.push({
        role: 'assistant',
        kind: 'question',
        questionNumber: data.questionNumber,
        questionText: data.questionText,
        category: data.category || '',
        difficulty: data.difficulty || '',
        totalQuestions: data.totalQuestions,
        time: fmtTime(new Date())
      })
      interviewState.value = 'in_progress'
      typing.value = false
      scrollToBottom()
    },
    onEvaluating: () => {
      typing.value = true
      interviewState.value = 'evaluating'
    },
    onFinalEvaluation: (data) => {
      typing.value = false
      evaluation.value = data
      interviewState.value = 'completed'
      // 刷新侧边栏（状态变为 COMPLETED）
      loadInterviewSessions()
      scrollToBottom()
    },
    onError: (message) => {
      ElMessage.error(message)
      interviewState.value = 'in_progress'
      typing.value = false
    }
  }, abortController.signal)

  submitting.value = false
  abortController = null
  scrollToBottom()
}

/**
 * 终止当前面试。
 */
async function handleTerminate() {
  if (terminating.value || !sessionId.value) return
  terminating.value = true
  typing.value = true
  abortController = new AbortController()

  await terminateInterview(sessionId.value, {
    onTerminated: (data) => {
      typing.value = false
      answeredCount.value = data.answeredCount
      interviewState.value = 'terminated'
    },
    onEvaluating: () => {
      typing.value = true
      interviewState.value = 'evaluating'
    },
    onFinalEvaluation: (data) => {
      typing.value = false
      evaluation.value = data
      interviewState.value = 'terminated'
      // 刷新侧边栏（状态变为 TERMINATED）
      loadInterviewSessions()
      scrollToBottom()
    },
    onError: (message) => {
      ElMessage.error(message)
      typing.value = false
    }
  }, abortController.signal)

  terminating.value = false
  abortController = null
  scrollToBottom()
}

/**
 * 重置面试状态，准备新一轮。
 */
function resetInterviewState() {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  positionInput.value = ''
  answerInput.value = ''
  starting.value = false
  submitting.value = false
  terminating.value = false
  typing.value = false
  sessionId.value = null
  sessionPosition.value = ''
  totalQuestions.value = 0
  answeredCount.value = 0
  messages.splice(0, messages.length)
  evaluation.value = null
}

/**
 * 重置并回到初始状态。
 */
function resetInterview() {
  resetInterviewState()
  activeSessionId.value = null
  interviewState.value = 'idle'
}

// ===== 组件卸载清理 =====
onBeforeUnmount(() => {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
})

// ===== 滚动 =====
function scrollToBottom(smooth = true) {
  nextTick(() => {
    const el = msgContainer.value
    if (el) {
      el.scrollTo({ top: el.scrollHeight, behavior: smooth ? 'smooth' : 'instant' })
      showScrollBtn.value = false
    }
  })
}

function onScroll() {
  const el = msgContainer.value
  if (!el) return
  showScrollBtn.value = el.scrollHeight - el.scrollTop - el.clientHeight > 150
}

// ===== 导航 =====
function goResume() {
  router.push('/resume')
}

function goChat() {
  router.push('/chat')
}

function goInterview() {
  // 已在当前页
}

function goAccount() {
  router.push('/account')
}

function handleLogout() {
  if (abortController) abortController.abort()
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
  ElMessage.success('已退出登录')
}
</script>

<style scoped>
/* ====== layout ====== */
.layout { display: flex; height: 100vh; }

/* ====== sidebar ====== */
.sidebar {
  width: 30%;
  min-width: 280px;
  display: flex;
  flex-direction: column;
  background: #eef2f7;
  transition: width 0.25s, min-width 0.25s, opacity 0.2s;
  overflow: hidden;
}
.sidebar.collapsed {
  width: 0;
  min-width: 0;
  opacity: 0;
}

.sidebar-top { padding: 16px 14px 12px; }
.sidebar-title { margin: 0 0 10px; font-size: 17px; color: #303133; }
.sidebar-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.new-conv-btn { flex: 1; }
.resume-btn { flex: 1; }
.interview-nav-btn {
  flex: 1 1 100%;
  --el-button-bg-color: #4F46E5;
  --el-button-border-color: #4F46E5;
  --el-button-text-color: #fff;
  --el-button-hover-bg-color: #4338CA;
  --el-button-hover-border-color: #4338CA;
  --el-button-hover-text-color: #fff;
}
.interview-nav-btn.active-nav {
  --el-button-bg-color: #4338CA;
  --el-button-border-color: #3730A3;
}

/* interview session list */
.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 10px;
}

.session-item {
  position: relative;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 6px;
  transition: background 0.15s, border-color 0.15s;
  background: #fff;
  border: 1px solid #e2e8f1;
}
.session-item:hover { background: #f0f4ff; border-color: #c4d0e8; }
.session-item.active { background: #fff; border-color: #3f76bc; }

.session-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.session-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
}
.session-time { font-size: 11px; color: #9ca3af; margin-top: 2px; }

.conv-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 13px;
}

.sidebar-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 14px;
  border-top: 1px solid #d4d4d8;
}
.user-name { font-size: 13px; color: #4b5563; cursor: pointer; }
.user-name:hover { color: #4F46E5; }

/* ====== main ====== */
.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #fff;
}

/* ====== top bar ====== */
.top-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  border-bottom: 1px solid #e5e7eb;
  background: #fafbfc;
  flex-shrink: 0;
}
.toggle-btn { color: #6b7280; flex-shrink: 0; }
.toggle-btn:hover { color: #4F46E5; }

.status-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  flex-wrap: wrap;
}
.status-tag { flex-shrink: 0; }
.status-progress { font-size: 14px; color: #374151; }
.status-indicator { flex-shrink: 0; }

/* ====== message area ====== */
.msg-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  position: relative;
}

.center-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 10px;
  color: #9ca3af;
}
.center-state.welcome h3 {
  margin: 12px 0 4px;
  color: #374151;
  font-size: 22px;
}
.center-state.welcome p {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
}
.welcome-hint {
  margin-top: 16px !important;
  color: #4F46E5 !important;
  font-weight: 500;
}
.welcome-icon {
  color: #4F46E5;
  opacity: 0.6;
}

/* messages */
.msg-row {
  display: flex;
  margin-bottom: 20px;
  gap: 10px;
}
.msg-row.assistant { justify-content: flex-start; }
.msg-row.user { justify-content: flex-end; }

.msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #4F46E5;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.msg-avatar.user-avatar {
  background: #10B981;
}

.msg-body {
  max-width: 75%;
  display: flex;
  flex-direction: column;
}

.msg-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.65;
  word-break: break-word;
}
.msg-bubble.ai {
  background: #EEF2FF;
  color: #1f2937;
  border-top-left-radius: 4px;
}
.msg-bubble.user {
  background: #10B981;
  color: #fff;
  border-top-right-radius: 4px;
  align-self: flex-end;
}

/* question card */
.question-card {
  background: #EEF2FF;
  border: 1px solid #C7D2FE;
  border-radius: 16px;
  border-top-left-radius: 4px;
  padding: 14px 18px;
}
.question-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.question-number {
  font-weight: 700;
  font-size: 13px;
  color: #4F46E5;
}
.question-text {
  font-size: 14px;
  line-height: 1.7;
  color: #1f2937;
}

.msg-time {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 4px;
  padding: 0 8px;
}
.msg-row.user .msg-time { text-align: right; }

/* typing */
.typing-bubble {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 14px 20px;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #9ca3af;
  animation: bounce 1.4s infinite both;
}
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* termination notice */
.terminated-notice {
  height: auto !important;
  padding: 60px 0;
}

/* ====== evaluation card ====== */
.evaluation-card {
  margin: 20px auto;
  max-width: 700px;
  background: #FFFBEB;
  border: 2px solid #F59E0B;
  border-radius: 20px;
  padding: 32px 28px;
  box-shadow: 0 4px 24px rgba(245, 158, 11, 0.12);
}

.eval-header {
  text-align: center;
  margin-bottom: 24px;
}
.eval-header h2 {
  margin: 0 0 12px;
  font-size: 24px;
  color: #1f2937;
}
.eval-score-big {
  margin: 8px 0;
}
.score-num {
  font-size: 56px;
  font-weight: 800;
  color: #4F46E5;
}
.score-unit {
  font-size: 20px;
  color: #6b7280;
}
.score-stars {
  font-size: 24px;
  display: flex;
  justify-content: center;
  gap: 4px;
}
.star {
  opacity: 0.25;
  transition: opacity 0.3s;
}
.star.filled {
  opacity: 1;
}

.eval-section {
  margin-bottom: 20px;
}
.eval-section h4 {
  margin: 0 0 8px;
  font-size: 15px;
  color: #374151;
}
.eval-section p {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #4b5563;
}

/* question scores */
.question-scores {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.score-item {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 14px 16px;
}
.score-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.score-item-num {
  font-weight: 600;
  font-size: 13px;
  color: #374151;
}
.score-item-score {
  font-weight: 700;
  font-size: 18px;
  padding: 2px 10px;
  border-radius: 8px;
}
.score-high { color: #059669; background: #D1FAE5; }
.score-mid { color: #D97706; background: #FEF3C7; }
.score-low { color: #DC2626; background: #FEE2E2; }
.score-fail { color: #991B1B; background: #FECACA; }

.score-item-question {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 6px;
}
.score-item-answer,
.score-item-comment,
.score-item-reference {
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 4px;
  color: #4b5563;
}
.score-item-answer .label,
.score-item-comment .label,
.score-item-reference .label {
  font-weight: 600;
  color: #374151;
}

.eval-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #FDE68A;
}

/* ====== scroll button ====== */
.scroll-btn {
  position: sticky;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #4F46E5;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.3);
  transition: transform 0.15s;
}
.scroll-btn:hover { transform: translateX(-50%) scale(1.1); }

/* ====== footer / input ====== */
.footer {
  padding: 14px 24px 18px;
  border-top: 1px solid #e5e7eb;
  background: #fafbfc;
  flex-shrink: 0;
}

.input-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
}
.position-input {
  flex: 1;
  max-width: 500px;
}
.answer-input {
  flex: 1;
}
.answer-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  flex-shrink: 0;
}
.answer-hint {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 4px;
}

.evaluating-hint,
.completed-hint {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: center;
  color: #6b7280;
  font-size: 14px;
  padding: 8px 0;
}
</style>
