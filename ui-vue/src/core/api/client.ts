export interface ApiRequestOptions extends RequestInit {
  params?: Record<string, string | number | boolean | null | undefined>
}

export class ApiError extends Error {
  status?: number
  data?: unknown

  constructor(message: string, status?: number, data?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.data = data
  }
}

const getBaseUrl = () => (import.meta.env.VITE_API_BASE_URL ?? '').trim()

class ApiClient {
  private baseUrl = getBaseUrl()

  async get<T>(endpoint: string, options: ApiRequestOptions = {}) {
    return this.request<T>(endpoint, { ...options, method: 'GET' })
  }

  async post<T>(endpoint: string, data?: unknown, options: ApiRequestOptions = {}) {
    return this.request<T>(endpoint, {
      ...options,
      method: 'POST',
      body: data === undefined ? undefined : JSON.stringify(data)
    })
  }

  async put<T>(endpoint: string, data?: unknown, options: ApiRequestOptions = {}) {
    return this.request<T>(endpoint, {
      ...options,
      method: 'PUT',
      body: data === undefined ? undefined : JSON.stringify(data)
    })
  }

  async delete<T>(endpoint: string, options: ApiRequestOptions = {}) {
    return this.request<T>(endpoint, { ...options, method: 'DELETE' })
  }

  private async request<T>(endpoint: string, options: ApiRequestOptions) {
    const { params, headers: customHeaders, ...fetchOptions } = options
    const headers = new Headers(customHeaders)

    headers.set('Accept', 'application/json')
    if (fetchOptions.body !== undefined && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json')
    }

    const response = await fetch(this.buildUrl(endpoint, params), {
      ...fetchOptions,
      headers,
      credentials: 'include'
    })

    if (!response.ok) {
      const data = await this.readResponse(response)
      const message = response.status === 401
        ? 'Sessao expirada. Entre novamente.'
        : response.status === 409
          ? 'Esse registro ja existe.'
          : 'Nao foi possivel concluir a operacao.'
      throw new ApiError(message, response.status, data)
    }

    if (response.status === 204) {
      return undefined as T
    }

    return this.readResponse(response) as Promise<T>
  }

  private buildUrl(endpoint: string, params?: ApiRequestOptions['params']) {
    const url = new URL(`${this.baseUrl}${endpoint}`, window.location.origin)

    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        if (value !== null && value !== undefined) {
          url.searchParams.set(key, String(value))
        }
      })
    }

    return url.toString()
  }

  private async readResponse(response: Response) {
    const text = await response.text()
    if (!text.trim()) {
      return {}
    }

    try {
      return JSON.parse(text)
    } catch {
      return text
    }
  }
}

export const api = new ApiClient()
