import request from './request'

/**
 * 通用 SSE 读取器。
 *
 * 解析后端 SSE 事件流，根据 event 名称调用对应回调。
 * 与 chat.js 中的 SSE 逻辑一致，但回调更灵活。
 *
 * @param {string} url - API 路径
 * @param {object} body - JSON 请求体
 * @param {object} handlers - 事件回调映射 { eventName: (data) => void }
 * @param {AbortSignal} signal - 取消信号
 */
async function sseRequest(url, body, handlers, signal) {
  const token = localStorage.getItem('token')
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    },
    body: JSON.stringify(body),
    signal
  })

  if (response.status === 401) {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    window.location.href = '/login'
    return
  }

  if (!response.ok) {
    const errData = await response.json().catch(() => ({}))
    const errorMsg = errData.message || '请求失败'
    if (handlers.error) {
      handlers.error({ message: errorMsg })
    }
    return
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let currentEvent = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('event:')) {
          currentEvent = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          const raw = line.slice(5).trim()
          try {
            const data = JSON.parse(raw)
            if (handlers[currentEvent]) {
              handlers[currentEvent](data)
            }
          } catch {
            // 解析失败，忽略
          }
          currentEvent = ''
        }
      }
    }
  } catch (err) {
    if (err.name === 'AbortError') {
      // 用户主动取消，正常情况
    } else if (handlers.error) {
      handlers.error({ message: err.message })
    }
  } finally {
    try { reader.releaseLock() } catch { /* ignore */ }
  }
}

// ==================== SSE 接口 ====================

/**
 * 开始模拟面试（SSE）。
 *
 * SSE 事件：
 *   greeting, session_created, question, error
 *
 * @param {string} position - 面试岗位
 * @param {object} handlers - { onGreeting, onSessionCreated, onQuestion, onError }
 * @param {AbortSignal} signal
 */
export function startInterview(position, handlers, signal) {
  return sseRequest('/api/interview/start', { position }, {
    greeting: (data) => handlers.onGreeting?.(data.message),
    session_created: (data) => handlers.onSessionCreated?.(data),
    question: (data) => handlers.onQuestion?.(data),
    error: (data) => handlers.onError?.(data.message || '启动面试失败')
  }, signal)
}

/**
 * 提交回答（SSE）。
 *
 * SSE 事件：
 *   answer_saved, question, evaluating, final_evaluation, error
 *
 * @param {number} sessionId
 * @param {string} answer
 * @param {object} handlers - { onAnswerSaved, onQuestion, onEvaluating, onFinalEvaluation, onError }
 * @param {AbortSignal} signal
 */
export function submitAnswer(sessionId, answer, handlers, signal) {
  return sseRequest('/api/interview/answer', { sessionId, answer }, {
    answer_saved: (data) => handlers.onAnswerSaved?.(data),
    question: (data) => handlers.onQuestion?.(data),
    evaluating: (data) => handlers.onEvaluating?.(data.message),
    final_evaluation: (data) => handlers.onFinalEvaluation?.(data),
    error: (data) => handlers.onError?.(data.message || '提交回答失败')
  }, signal)
}

/**
 * 终止面试（SSE）。
 *
 * SSE 事件：
 *   terminated, evaluating, final_evaluation, error
 *
 * @param {number} sessionId
 * @param {object} handlers - { onTerminated, onEvaluating, onFinalEvaluation, onError }
 * @param {AbortSignal} signal
 */
export function terminateInterview(sessionId, handlers, signal) {
  return sseRequest('/api/interview/terminate', { sessionId }, {
    terminated: (data) => handlers.onTerminated?.(data),
    evaluating: (data) => handlers.onEvaluating?.(data.message),
    final_evaluation: (data) => handlers.onFinalEvaluation?.(data),
    error: (data) => handlers.onError?.(data.message || '终止面试失败')
  }, signal)
}

// ==================== REST 接口 ====================

/**
 * 获取面试历史列表。
 */
export function getInterviewSessions() {
  return request.get('/interview/sessions')
}

/**
 * 获取某次面试的完整详情。
 */
export function getInterviewSessionDetail(sessionId) {
  return request.get(`/interview/sessions/${sessionId}`)
}
