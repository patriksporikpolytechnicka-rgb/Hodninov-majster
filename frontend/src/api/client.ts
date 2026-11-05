import type { ApiError } from './types'

let token: string | null = null

// Kept in memory only: a page refresh logs the user out.
export function setToken(value: string | null) {
  token = value
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  if (options.body) headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(`/api${path}`, { ...options, headers })
  if (!response.ok) throw (await response.json().catch(() => ({ status: response.status }))) as ApiError
  return (response.status === 204 ? undefined : await response.json()) as T
}
