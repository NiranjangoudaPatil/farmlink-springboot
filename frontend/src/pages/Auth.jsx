import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Sprout } from 'lucide-react'
import { useAuth } from '../auth'
import { useToast } from '../toast'
import { homeFor } from '../lib'

export default function Auth() {
  const { login, register } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [params] = useSearchParams()

  const [tab, setTab] = useState(params.get('role') ? 'signup' : 'login')
  const [role, setRole] = useState(params.get('role') === 'farmer' ? 'FARMER' : 'RETAILER')
  const [busy, setBusy] = useState(false)
  const [loginForm, setLoginForm] = useState({ email: '', password: '' })
  const [signup, setSignup] = useState({ fullName: '', email: '', password: '', location: '', licenseNumber: '' })

  const set = (k) => (e) => setSignup({ ...signup, [k]: e.target.value })

  const doLogin = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      const u = await login(loginForm.email, loginForm.password)
      toast.success('Login successful!')
      navigate(homeFor(u.role))
    } catch (err) {
      toast.error(err.message)
    } finally {
      setBusy(false)
    }
  }

  const doSignup = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      const u = await register({
        role,
        fullName: signup.fullName,
        email: signup.email,
        password: signup.password,
        location: signup.location || null,
        licenseNumber: role === 'FARMER' ? signup.licenseNumber || null : null,
      })
      toast.success('Account created!')
      navigate(homeFor(u.role))
    } catch (err) {
      toast.error(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-wrap">
      <div className="card auth-card card-pad">
        <div className="center" style={{ marginBottom: 20 }}>
          <div className="auth-logo"><Sprout size={24} /></div>
          <h2>Welcome to FarmLink</h2>
          <p className="muted small" style={{ marginTop: 4 }}>Sign in or create an account to start trading</p>
        </div>

        <div className="tabs full">
          <button className={`tab ${tab === 'login' ? 'active' : ''}`} onClick={() => setTab('login')}>Sign In</button>
          <button className={`tab ${tab === 'signup' ? 'active' : ''}`} onClick={() => setTab('signup')}>Sign Up</button>
        </div>

        {tab === 'login' ? (
          <form className="stack" style={{ paddingTop: 18 }} onSubmit={doLogin}>
            <label className="field"><span>Email</span>
              <input className="input" type="email" required value={loginForm.email} onChange={(e) => setLoginForm({ ...loginForm, email: e.target.value })} />
            </label>
            <label className="field"><span>Password</span>
              <input className="input" type="password" required value={loginForm.password} onChange={(e) => setLoginForm({ ...loginForm, password: e.target.value })} />
            </label>
            <button className="btn btn-full" disabled={busy}>{busy ? 'Signing in...' : 'Sign In'}</button>
          </form>
        ) : (
          <form className="stack" style={{ paddingTop: 18 }} onSubmit={doSignup}>
            <div className="field">
              <span>I am a</span>
              <div className="role-pick">
                <button type="button" className={`btn ${role === 'FARMER' ? '' : 'btn-outline'}`} onClick={() => setRole('FARMER')}>🌾 Farmer</button>
                <button type="button" className={`btn ${role === 'RETAILER' ? '' : 'btn-outline'}`} onClick={() => setRole('RETAILER')}>🏪 Retailer</button>
              </div>
            </div>
            <label className="field"><span>Full Name</span><input className="input" required value={signup.fullName} onChange={set('fullName')} /></label>
            <label className="field"><span>Email</span><input className="input" type="email" required value={signup.email} onChange={set('email')} /></label>
            <label className="field"><span>Password</span><input className="input" type="password" required minLength={6} value={signup.password} onChange={set('password')} /></label>
            <label className="field"><span>Location (optional)</span><input className="input" placeholder="e.g. Maharashtra, India" value={signup.location} onChange={set('location')} /></label>
            {role === 'FARMER' && (
              <label className="field"><span>Farming License Number</span>
                <input className="input" placeholder="e.g. FARM-MH-2026-XXXXX" required value={signup.licenseNumber} onChange={set('licenseNumber')} />
                <span className="muted xs" style={{ fontWeight: 400 }}>Required for verification by admin</span>
              </label>
            )}
            <button className="btn btn-full" disabled={busy}>{busy ? 'Creating account...' : 'Create Account'}</button>
          </form>
        )}
      </div>
    </div>
  )
}
