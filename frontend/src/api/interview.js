import request from './request'
import { buildApiUrl } from '../config/app'

async function sseRequest(path, body, handlers, signal) {
  const token = localStorage.getItem('token')
  const response = await fetch(buildApiUrl(path), {
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
    handlers.error?.({ message: errData.message || '请求失败' })
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
          continue
        }

        if (!line.startsWith('data:')) {
          continue
        }

        const raw = line.slice(5).trim()
        try {
          const data = JSON.parse(raw)
          handlers[currentEvent]?.(data)
        } catch {
          handlers.error?.({ message: '服务端响应解析失败' })
        } finally {
          currentEvent = ''
        }
      }
    }
  } catch (err) {
    if (err.name !== 'AbortError') {
      handlers.error?.({ message: err.message })
    }
  } finally {
    try {
      reader.releaseLock()
    } catch {
      // Ignore repeated release in browser edge cases.
    }
  }
}

export function startInterview(position, handlers, signal) {
  return sseRequest('/interview/start', { position }, {
    greeting: data => handlers.onGreeting?.(data.message),
    session_created: data => handlers.onSessionCreated?.(data),
    question: data => handlers.onQuestion?.(data),
    error: data => handlers.onError?.(data.message || '启动面试失败')
  }, signal)
}

export function submitAnswer(sessionId, answer, handlers, signal) {
  return sseRequest('/interview/answer', { sessionId, answer }, {
    answer_saved: data => handlers.onAnswerSaved?.(data),
    interview_decision: data => handlers.onDecision?.(data),
    question: data => handlers.onQuestion?.(data),
    evaluating: data => handlers.onEvaluating?.(data.message),
    final_evaluation: data => handlers.onFinalEvaluation?.(data),
    error: data => handlers.onError?.(data.message || '提交回答失败')
  }, signal)
}

export function terminateInterview(sessionId, handlers, signal) {
  return sseRequest('/interview/terminate', { sessionId }, {
    terminated: data => handlers.onTerminated?.(data),
    evaluating: data => handlers.onEvaluating?.(data.message),
    final_evaluation: data => handlers.onFinalEvaluation?.(data),
    error: data => handlers.onError?.(data.message || '终止面试失败')
  }, signal)
}

export function getInterviewSessions() {
  return request.get('/interview/sessions')
}

export function getInterviewSessionDetail(sessionId) {
  return request.get(`/interview/sessions/${sessionId}`)
}
