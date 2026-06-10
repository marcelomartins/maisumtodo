import { api } from '@/core/api/client'
import type { AuthCredentials, AuthUser } from '../types'

export const authApi = {
  me() {
    return api.get<AuthUser>('/api/auth/me')
  },

  login(credentials: AuthCredentials) {
    return api.post<AuthUser>('/api/auth/login', credentials)
  },

  register(credentials: AuthCredentials) {
    return api.post<AuthUser>('/api/auth/register', credentials)
  },

  logout() {
    return api.post<void>('/api/auth/logout')
  }
}
