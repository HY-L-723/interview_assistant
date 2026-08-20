import request from './request'
import { buildApiUrl } from '../config/app'

export function generateResume(data) {
  return request.post('/resume/generate', data)
}

export function getResumeHistory() {
  return request.get('/resume/history')
}

export function uploadPhoto(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/resume/upload-photo', formData)
}

export function downloadPdfUrl(resumeId) {
  return buildApiUrl(`/resume/${resumeId}/pdf`)
}
