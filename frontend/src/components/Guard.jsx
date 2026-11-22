import { Navigate } from 'react-router-dom'
import { useAuth } from '../auth'
import { homeFor } from '../lib'

// Wraps a page: not logged in -> /auth, wrong role -> that user's own home. (The backend enforces this too.)
export default function Guard({ role, children }) {
  const { user, loading } = useAuth()
  if (loading) return <div className="container page muted">Loading…</div>
  if (!user) return <Navigate to="/auth" replace />
  if (role && user.role !== role) return <Navigate to={homeFor(user.role)} replace />
  return children
}
