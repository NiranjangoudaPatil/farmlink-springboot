import { createContext, useContext, useEffect, useState } from 'react'
import { api, getToken, setToken } from './api'

const AuthContext = createContext(null)
export const useAuth = () => useContext(AuthContext)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(!!getToken())

  // On page load, if a token is stored, ask the backend who we are.
  useEffect(() => {
    if (!getToken()) return
    api
      .get('/api/users/me')
      .then(setUser)
      .catch(() => setToken(null))
      .finally(() => setLoading(false))
  }, [])

  const login = async (email, password) => {
    const res = await api.post('/api/auth/login', { email, password })
    setToken(res.token)
    setUser(res.user)
    return res.user
  }

  const register = async (form) => {
    const res = await api.post('/api/auth/register', form)
    setToken(res.token)
    setUser(res.user)
    return res.user
  }

  const logout = () => {
    setToken(null)
    setUser(null)
  }

  return <AuthContext.Provider value={{ user, loading, login, register, logout }}>{children}</AuthContext.Provider>
}
