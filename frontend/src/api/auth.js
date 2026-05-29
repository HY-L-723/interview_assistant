import request from './request'

export function login(username, password) {
  return request.post('/auth/login', { username, password })
}

export function register(username, password, email) {
  return request.post('/auth/register', { username, password, email })
}

export function getProfile() {
  return request.get('/user/profile')
}

export function updateProfile(data) {
  return request.put('/user/profile', data)
}
