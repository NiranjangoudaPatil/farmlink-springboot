import { Link, Navigate } from 'react-router-dom'
import { ArrowRight, Leaf, Shield, TrendingUp, Users } from 'lucide-react'
import { useAuth } from '../auth'
import { homeFor } from '../lib'
import heroImage from '../assets/hero-farm.jpg'

const FEATURES = [
  { icon: Leaf, title: 'Farm Fresh', desc: 'Get produce directly from verified local farmers with guaranteed freshness.' },
  { icon: TrendingUp, title: 'Better Prices', desc: 'Eliminate middlemen costs. Farmers earn more, retailers pay less.' },
  { icon: Shield, title: 'Secure Payments', desc: 'Safe and transparent transactions with order tracking.' },
  { icon: Users, title: 'Direct Connection', desc: 'Communicate directly with farmers for custom orders and bulk deals.' },
]

export default function Landing() {
  const { user } = useAuth()
  // Logged-in users skip the landing page and go straight to their dashboard.
  if (user) return <Navigate to={homeFor(user.role)} replace />

  return (
    <>
      <section className="hero">
        <img src={heroImage} alt="Fresh farm produce" />
        <div className="container">
          <div className="hero-inner">
            <span className="hero-tag">🌾 Farm-to-Retail, Direct & Fair</span>
            <h1>Fresh From the <span>Farm</span> to Your Store</h1>
            <p className="lead">
              Connect directly with local farmers. Eliminate middlemen, get fresher produce at better prices, and support sustainable agriculture.
            </p>
            <div className="row" style={{ gap: 16 }}>
              <Link to="/auth?role=farmer" className="btn btn-lg">Join as Farmer <ArrowRight size={16} /></Link>
              <Link to="/auth?role=retailer" className="btn btn-lg btn-hero-outline">Join as Retailer <ArrowRight size={16} /></Link>
            </div>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="container">
          <h2>Why Choose FarmLink?</h2>
          <p className="lead">Our platform bridges the gap between farmers and retailers, creating a transparent and efficient agricultural marketplace.</p>
          <div className="features">
            {FEATURES.map(({ icon: Icon, title, desc }) => (
              <div className="feature" key={title}>
                <div className="ico"><Icon size={24} /></div>
                <h3>{title}</h3>
                <p className="muted">{desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="section cta">
        <div className="container">
          <h2>Ready to Get Started?</h2>
          <p className="lead" style={{ marginBottom: 32 }}>Join thousands of farmers and retailers already trading on FarmLink.</p>
          <Link to="/auth" className="btn btn-lg">Create Free Account <ArrowRight size={16} /></Link>
        </div>
      </section>

      <footer className="footer"><div className="container">© 2026 FarmLink. Connecting Farms to Retail.</div></footer>
    </>
  )
}
