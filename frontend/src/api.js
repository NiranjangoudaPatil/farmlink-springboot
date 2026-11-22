// One place that talks to the Spring Boot API.
// Every backend response looks like { status, data, message }; we return `data` or throw an Error(message).

const TOKEN_KEY = 'farmlink_token'

// Locally this is empty (Vite proxies /api to the backend). In production, set VITE_API_URL at build time
// to the backend's public address, e.g. https://farmlink-api.up.railway.app
const API_BASE = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '')

// Product images come back as "/uploads/x.jpg"; in production they must point at the backend too.
export const assetUrl = (u) => (u && u.startsWith('/') ? API_BASE + u : u)

export const getToken = () => localStorage.getItem(TOKEN_KEY)
export const setToken = (t) => (t ? localStorage.setItem(TOKEN_KEY, t) : localStorage.removeItem(TOKEN_KEY))

async function request(method, path, body, isForm = false) {
  const headers = {}
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  if (body && !isForm) headers['Content-Type'] = 'application/json'

  const res = await fetch(API_BASE + path, {
    method,
    headers,
    body: body ? (isForm ? body : JSON.stringify(body)) : undefined,
  })

  let json = null
  try {
    json = await res.json()
  } catch {
    /* empty body */
  }

  if (!res.ok) {
    if (res.status === 401) setToken(null) // expired / invalid token -> force a fresh login
    throw new Error(json?.message || `Request failed (${res.status})`)
  }
  return json?.data
}

export const api = {
  get: (p) => request('GET', p),
  post: (p, b) => request('POST', p, b),
  put: (p, b) => request('PUT', p, b),
  patch: (p, b) => request('PATCH', p, b),
  del: (p) => request('DELETE', p),
  upload: (p, file) => {
    const fd = new FormData()
    fd.append('file', file)
    return request('POST', p, fd, true)
  },
}

export const money = (n) => `₹${Number(n ?? 0).toFixed(2)}`
