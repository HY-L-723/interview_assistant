import request from './request'
import { buildApiUrl } from '../config/app'

export function sendMessageStream(message, conversationId, model, { signal, onToken, onDone, onError }) {
  const token = localStorage.getItem('token')

  fetch(buildApiUrl('/chat'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    },
    body: JSON.stringify({ message, conversationId, model }),
    signal
  }).then(async response => {
    if (response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
      return
    }

    if (!response.ok) {
      const errData = await response.json().catch(() => ({}))
      onError(new Error(errData.message || '请求失败'))
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
            if (currentEvent === 'token') {
              onToken(typeof data === 'string' ? data : (data.content || ''))
            } else if (currentEvent === 'done') {
              onDone(data)
            } else if (currentEvent === 'error') {
              onError(new Error(data.message || '请求失败'))
            }
          } catch {
            if (currentEvent === 'token') {
              onToken(raw)
            }
          } finally {
            currentEvent = ''
          }
        }
      }
    } catch (err) {
      onError(err)
    } finally {
      reader.releaseLock()
    }
  }).catch(err => {
    onError(err)
  })
}

export function createConversation() {
  return request.post('/conversations')
}

export function listConversations() {
  return request.get('/conversations')
}

export function getConversationMessages(conversationId) {
  return request.get(`/conversations/${conversationId}/messages`)
}

export function deleteConversation(conversationId) {
  return request.delete(`/conversations/${conversationId}`)
}
