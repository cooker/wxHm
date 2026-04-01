const TOKEN_KEY = 'wxhm_admin_token'

export function getAdminToken() {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function setAdminToken(t) {
  if (t) sessionStorage.setItem(TOKEN_KEY, t)
  else sessionStorage.removeItem(TOKEN_KEY)
}

function loginRedirectUrl() {
  const path = window.location.pathname + window.location.search
  return '/app/admin/login?redirect=' + encodeURIComponent(path || '/app/admin')
}

/**
 * @param {string} url
 * @param {RequestInit} init
 */
export async function apiFetch(url, init = {}) {
  const headers = new Headers(init.headers || {})
  if (!headers.has('X-Requested-With')) {
    headers.set('X-Requested-With', 'XMLHttpRequest')
  }
  const token = getAdminToken()
  if (token) {
    headers.set('X-Admin-Token', token)
  }
  const res = await fetch(url, { ...init, credentials: 'include', headers })
  if (res.status === 401) {
    setAdminToken(null)
    if (!url.includes('/api/admin/login') && !url.includes('/api/admin/session')) {
      window.location.href = loginRedirectUrl()
    }
  }
  return res
}

export async function apiJson(url, init = {}) {
  const h = new Headers(init.headers || {})
  if (init.body && !(init.body instanceof FormData) && !h.has('Content-Type')) {
    h.set('Content-Type', 'application/json')
  }
  const res = await apiFetch(url, { ...init, headers: h })
  const text = await res.text()
  let data = null
  try {
    data = text ? JSON.parse(text) : null
  } catch {
    data = { raw: text }
  }
  return { ok: res.ok, status: res.status, data }
}
