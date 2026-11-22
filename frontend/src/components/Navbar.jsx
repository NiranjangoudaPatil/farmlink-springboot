import { Link, NavLink, useNavigate } from 'react-router-dom'
import { LogOut, Package, Shield, ShoppingCart, Sprout, User } from 'lucide-react'
import { useAuth } from '../auth'
import NotificationBell from './NotificationBell'

const linkClass = ({ isActive }) => `btn btn-ghost btn-sm${isActive ? ' active' : ''}`

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  return (
    <nav className="nav">
      <div className="container nav-inner">
        <Link to="/" className="brand"><Sprout size={28} /> FarmLink</Link>

        <div className="nav-links">
          {user ? (
            <>
              {user.role === 'ADMIN' && (
                <NavLink to="/admin" className={linkClass}><Shield size={16} /><span className="label-text">Admin Portal</span></NavLink>
              )}
              {user.role === 'FARMER' && (
                <>
                  <NavLink to="/farmer/dashboard" className={linkClass}><Package size={16} /><span className="label-text">My Products</span></NavLink>
                  <NavLink to="/farmer/incoming-orders" className={linkClass}><ShoppingCart size={16} /><span className="label-text">Incoming Orders</span></NavLink>
                </>
              )}
              {user.role === 'RETAILER' && (
                <>
                  <NavLink to="/marketplace" className={linkClass}><ShoppingCart size={16} /><span className="label-text">Marketplace</span></NavLink>
                  <NavLink to="/retailer/orders" className={linkClass}><Package size={16} /><span className="label-text">My Orders</span></NavLink>
                </>
              )}

              <NotificationBell />

              <div className="user-pill">
                <User size={16} />
                <strong>{user.fullName}</strong>
                <span className="role">{user.role.toLowerCase()}</span>
              </div>
              <button className="btn btn-ghost btn-icon" title="Log out" onClick={() => { logout(); navigate('/') }}>
                <LogOut size={16} />
              </button>
            </>
          ) : (
            <>
              <Link to="/marketplace" className="btn btn-ghost btn-sm">Browse Products</Link>
              <Link to="/auth" className="btn btn-sm">Get Started</Link>
            </>
          )}
        </div>
      </div>
    </nav>
  )
}
