import { computed, reactive } from 'vue'
import { ApiError } from '@/core/api/client'
import { authApi } from '@/modules/auth/api/auth.api'
import type { AuthCredentials, AuthUser } from '@/modules/auth/types'

interface SessionState {
  user: AuthUser | null
  initialized: boolean
  loading: boolean
  error: string
}

export const sessionState = reactive<SessionState>({
  user: null,
  initialized: false,
  loading: false,
  error: ''
})

export const isAuthenticated = computed(() => sessionState.user !== null)

let initPromise: Promise<void> | null = null

export async function initSession(force = false) {
  if (sessionState.initialized && !force) {
    return
  }

  if (initPromise && !force) {
    return initPromise
  }

  initPromise = (async () => {
    sessionState.loading = true
    sessionState.error = ''

    try {
      sessionState.user = await authApi.me()
    } catch (error) {
      sessionState.user = null
      if (!(error instanceof ApiError && error.status === 401)) {
        sessionState.error = normalizeError(error)
      }
    } finally {
      sessionState.initialized = true
      sessionState.loading = false
      initPromise = null
    }
  })()

  return initPromise
}

export async function login(credentials: AuthCredentials) {
  return authenticate(() => authApi.login(credentials))
}

export async function register(credentials: AuthCredentials) {
  return authenticate(() => authApi.register(credentials))
}

export async function logout() {
  sessionState.loading = true
  sessionState.error = ''

  try {
    await authApi.logout()
  } catch {
  } finally {
    sessionState.user = null
    sessionState.initialized = true
    sessionState.loading = false
  }
}

async function authenticate(action: () => Promise<AuthUser>) {
  sessionState.loading = true
  sessionState.error = ''

  try {
    sessionState.user = await action()
    sessionState.initialized = true
    return sessionState.user
  } catch (error) {
    sessionState.user = null
    sessionState.error = normalizeError(error)
    throw error
  } finally {
    sessionState.loading = false
  }
}

export function normalizeError(error: unknown) {
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }

  return 'Algo deu errado. Tente novamente.'
}
