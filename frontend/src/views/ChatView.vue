<template>
  <div class="layout">
    <!-- ====== sidebar ====== -->
    <aside class="sidebar">
      <div class="sidebar-top">
        <h3 class="sidebar-title">面试助手</h3>
        <el-button type="primary" class="new-conv-btn" @click="handleNewConversation">
          <el-icon><Plus /></el-icon> 新对话
        </el-button>
      </div>

      <div class="conv-list" v-if="conversations.length > 0">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          :class="['conv-item', { active: conv.id === activeConvId }]"
          @click="switchConversation(conv)"
        >
          <div class="conv-title">{{ conv.title }}</div>
          <div class="conv-time">{{ fmtDate(conv.updatedAt) }}</div>
          <el-popconfirm
            title="确定删除此对话？"
            confirm-button-text="删除"
            cancel-button-text="取消"
            @confirm="handleDeleteConversation(conv)"
            @click.stop
          >
            <template #reference>
              <span class="conv-delete" @click.stop><el-icon><Delete /></el-icon></span>
            </template>
          </el-popconfirm>
        </div>
      </div>
      <div v-else class="conv-empty">暂无对话</div>

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
      <div class="msg-area" ref="msgContainer" @scroll="onScroll">
        <div v-if="loadingMsgs" class="center-state">
          <el-icon class="is-loading" :size="28"><Loading /></el-icon>
          <span>加载中...</span>
        </div>

        <div v-else-if="!activeConvId || messages.length === 0" class="center-state welcome">
          <h3>欢迎使用面试助手</h3>
          <p>点击左侧「新对话」开始模拟面试</p>
        </div>

        <template v-else>
          <div v-for="(msg, i) in messages" :key="i" :class="['msg-row', msg.role]">
            <div class="msg-bubble">
              <div v-if="msg.role === 'assistant' && !msg.failed"
                   class="msg-text markdown-body"
                   v-html="renderMd(msg.content)" />
              <div v-else class="msg-text">{{ msg.content }}</div>
              <div class="msg-meta">
                <span class="msg-time">{{ msg.time }}</span>
                <el-button
                  v-if="msg.role === 'assistant' && msg.failed"
                  text type="danger" size="small" @click="retry(i)">
                  <el-icon><RefreshRight /></el-icon>重试
                </el-button>
              </div>
            </div>
          </div>
        </template>

        <div v-if="sending" class="msg-row assistant">
          <div class="msg-bubble typing">
            <span class="dot"></span><span class="dot"></span><span class="dot"></span>
          </div>
        </div>

        <div v-if="showScrollBtn" class="scroll-btn" @click="scrollToBottom">
          <el-icon><ArrowDown /></el-icon>
        </div>
      </div>

      <footer class="footer">
        <div class="footer-left">
          <el-switch
            v-model="flashMode"
            size="small"
            active-text="Flash"
            inactive-text="Pro"
            style="--el-switch-on-color: #6366f1; --el-switch-off-color: #a5b4fc;"
          />
        </div>
        <div class="footer-right">
          <el-input
            v-model="input"
            placeholder="输入你的问题，按 Enter 发送"
            :disabled="sending || !activeConvId"
            @keyup.enter="handleSend"
          />
          <el-button v-if="!sending"
            type="primary"
            :disabled="!input.trim() || !activeConvId"
            @click="handleSend"
          >发送</el-button>
          <el-button v-else
            type="danger"
            @click="handleStop"
          >
            <el-icon><Close /></el-icon> 停止
          </el-button>
        </div>
      </footer>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Loading, RefreshRight, ArrowDown, Delete, Close } from '@element-plus/icons-vue'
import { marked } from 'marked'
import {
  sendMessageStream, createConversation, listConversations,
  getConversationMessages, deleteConversation
} from '../api/chat'

const route = useRoute()
const router = useRouter()
const msgContainer = ref(null)
const input = ref('')
const sending = ref(false)
const loadingMsgs = ref(false)
const activeConvId = ref(null)
const showScrollBtn = ref(false)
const flashMode = ref(false)
const abortController = ref(null)

const conversations = reactive([])
const messages = reactive([])

const user = JSON.parse(localStorage.getItem('user') || '{}')
const username = user.username || '用户'
const userEmail = user.email || ''
const userId = user.userId || ''

marked.setOptions({ breaks: true, gfm: true })

onMounted(async () => {
  document.title = '面试助手 - 聊天'
  await loadConversations()
  const qConvId = route.query.convId
  if (qConvId) {
    const conv = conversations.find(c => c.id === Number(qConvId))
    if (conv) switchConversation(conv)
    router.replace({ query: {} })
  }
})

// ---- conversations ----

async function loadConversations() {
  try {
    const res = await listConversations()
    conversations.splice(0, conversations.length, ...(res.data || []))
    if (conversations.length > 0 && !activeConvId.value) {
      switchConversation(conversations[0])
    }
  } catch { /* handled */ }
}

async function handleNewConversation() {
  try {
    const res = await createConversation()
    const conv = res.data
    conversations.unshift(conv)
    activeConvId.value = conv.id
    messages.splice(0, messages.length)
    input.value = ''
  } catch { /* handled */ }
}

async function handleDeleteConversation(conv) {
  try {
    await deleteConversation(conv.id)
    const idx = conversations.findIndex(c => c.id === conv.id)
    if (idx >= 0) conversations.splice(idx, 1)
    if (activeConvId.value === conv.id) {
      activeConvId.value = null
      messages.splice(0, messages.length)
      if (conversations.length > 0) switchConversation(conversations[0])
    }
    ElMessage.success('已删除')
  } catch { /* handled */ }
}

async function switchConversation(conv) {
  if (conv.id === activeConvId.value) return
  activeConvId.value = conv.id
  messages.splice(0, messages.length)
  loadingMsgs.value = true
  try {
    const res = await getConversationMessages(conv.id)
    const items = res.data || []
    items.forEach(item => {
      messages.push({
        role: item.role,
        content: item.content,
        time: fmtTime(new Date(item.createdAt)),
        failed: false
      })
    })
    nextTick(() => scrollToBottom(false))
  } catch { /* handled */ } finally {
    loadingMsgs.value = false
  }
}

// ---- messaging ----

function renderMd(text) {
  return marked(text || '')
}

function fmtTime(date) {
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function fmtDate(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const yesterday = today - 86400000
  const tsTime = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  if (tsTime === today) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  if (tsTime === yesterday) return '昨天'
  return `${d.getMonth() + 1}/${d.getDate()}`
}

async function handleSend() {
  const text = input.value.trim()
  if (!text || sending.value || !activeConvId.value) return
  input.value = ''
  sending.value = true

  const convId = activeConvId.value
  const model = flashMode.value ? 'deepseek-v4-flash' : null
  abortController.value = new AbortController()

  messages.push({ role: 'user', content: text, time: fmtTime(new Date()), failed: false })
  messages.push({ role: 'assistant', content: '', time: fmtTime(new Date()), failed: false })
  scrollToBottom()

  await new Promise((resolve) => {
    sendMessageStream(text, convId, model, {
      signal: abortController.value.signal,
      onToken: (token) => {
        const idx = messages.length - 1
        messages[idx] = { ...messages[idx], content: messages[idx].content + token }
        console.log('[ChatView] onToken, content length:', messages[idx].content.length)
        scrollToBottom()
      },
      onDone: () => {
        refreshConversations()
        resolve()
      },
      onError: (err) => {
        if (err?.name === 'AbortError') {
          resolve()
          return
        }
        const idx = messages.length - 1
        if (!messages[idx].content) {
          messages[idx] = { ...messages[idx], content: '消息发送失败', failed: true }
        } else {
          messages[idx] = { ...messages[idx], failed: true }
        }
        resolve()
      }
    })
  })

  sending.value = false
  abortController.value = null
  scrollToBottom()
}

function handleStop() {
  if (abortController.value) {
    abortController.value.abort()
    sending.value = false
  }
}

async function retry(index) {
  const failedMsg = messages[index]
  if (!failedMsg || failedMsg.role !== 'assistant') return
  const prevUsers = messages.slice(0, index).filter(m => m.role === 'user')
  const lastUser = prevUsers[prevUsers.length - 1]
  if (!lastUser) return

  messages.splice(index, 1)
  sending.value = true
  abortController.value = new AbortController()
  const convId = activeConvId.value

  messages.push({ role: 'assistant', content: '', time: fmtTime(new Date()), failed: false })

  await new Promise((resolve) => {
    sendMessageStream(lastUser.content, convId,
      flashMode.value ? 'deepseek-v4-flash' : null, {
        signal: abortController.value.signal,
        onToken: (token) => {
          const idx = messages.length - 1
          messages[idx] = { ...messages[idx], content: messages[idx].content + token }
          scrollToBottom()
        },
        onDone: () => {
          refreshConversations()
          resolve()
        },
        onError: (err) => {
          if (err?.name === 'AbortError') {
            resolve()
            return
          }
          const idx = messages.length - 1
          if (!messages[idx].content) {
            messages[idx] = { ...messages[idx], content: '消息发送失败', failed: true }
          } else {
            messages[idx] = { ...messages[idx], failed: true }
          }
          resolve()
        }
      })
  })

  sending.value = false
  abortController.value = null
  scrollToBottom()
}

async function refreshConversations() {
  try {
    const res = await listConversations()
    conversations.splice(0, conversations.length, ...(res.data || []))
  } catch { /* ok */ }
}

// ---- scroll ----

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

// ---- auth ----

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
  ElMessage.success('已退出登录')
}

function goAccount() {
  router.push('/account')
}
</script>

<style scoped>
.layout { display: flex; height: 100vh; }

/* ====== sidebar ====== */
.sidebar {
  width: 20%;
  min-width: 240px;
  display: flex;
  flex-direction: column;
  background: #e8e8ea;
}

.sidebar-top { padding: 16px 14px 12px; }
.sidebar-title { margin: 0 0 10px; font-size: 17px; color: #303133; }
.new-conv-btn { width: 100%; }

.conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}

.conv-item {
  position: relative;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 3px;
  transition: background 0.15s;
}
.conv-item:hover { background: #d4d4d8; }
.conv-item.active { background: #c7d2fe; }

.conv-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-right: 20px;
}
.conv-time { font-size: 11px; color: #9ca3af; margin-top: 2px; }

.conv-delete {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0;
  font-size: 14px;
  color: #ef4444;
  transition: opacity 0.15s;
}
.conv-item:hover .conv-delete { opacity: 1; }
.conv-delete:hover { color: #dc2626; }

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
.center-state.welcome h3 { margin: 0; color: #374151; }
.center-state.welcome p { margin: 0; font-size: 14px; }

/* messages */
.msg-row { display: flex; margin-bottom: 16px; }
.msg-row.user { justify-content: flex-end; }
.msg-row.assistant { justify-content: flex-start; }

.msg-bubble {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}
.msg-row.user .msg-bubble {
  background: #4F46E5;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.msg-row.assistant .msg-bubble {
  background: #f3f4f6;
  color: #1f2937;
  border-bottom-left-radius: 4px;
}

.msg-meta { display: flex; align-items: center; justify-content: flex-end; gap: 8px; margin-top: 6px; }
.msg-time { font-size: 12px; opacity: 0.7; }

/* markdown */
.markdown-body :deep(p) { margin: 0 0 8px; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }
.markdown-body :deep(pre) {
  background: #1e1e2e;
  color: #cdd6f4;
  padding: 12px 16px;
  border-radius: 8px;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.5;
  margin: 8px 0;
}
.markdown-body :deep(code) {
  background: rgba(0,0,0,0.06);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}
.markdown-body :deep(pre code) {
  background: none;
  padding: 0;
  border-radius: 0;
}
.markdown-body :deep(ul), .markdown-body :deep(ol) { padding-left: 20px; margin: 6px 0; }
.markdown-body :deep(li) { margin-bottom: 4px; }
.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3),
.markdown-body :deep(h4) { margin: 10px 0 6px; }
.markdown-body :deep(strong) { font-weight: 600; }
.markdown-body :deep(blockquote) {
  border-left: 3px solid #6366f1;
  padding-left: 12px;
  margin: 8px 0;
  color: #6b7280;
}
.markdown-body :deep(table) { border-collapse: collapse; margin: 8px 0; width: 100%; }
.markdown-body :deep(th), .markdown-body :deep(td) {
  border: 1px solid #d1d5db;
  padding: 6px 10px;
  text-align: left;
  font-size: 13px;
}
.markdown-body :deep(th) { background: #f3f4f6; font-weight: 600; }

/* typing */
.msg-bubble.typing { display: flex; gap: 6px; padding: 14px 18px; }
.dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: #9ca3af;
  animation: blink 1.4s infinite ease-in-out;
}
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
  0%, 80%, 100% { opacity: 0.3; }
  40% { opacity: 1; }
}

.scroll-btn {
  position: sticky; bottom: 8px; left: 50%; transform: translateX(-50%);
  width: 36px; height: 36px; border-radius: 50%;
  background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.12);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; color: #6b7280;
}
.scroll-btn:hover { box-shadow: 0 4px 14px rgba(0,0,0,0.18); }

/* footer */
.footer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 24px;
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
  background: #fff;
}
.footer-left { flex-shrink: 0; }
.footer-right { display: flex; gap: 10px; flex: 1; }
</style>
