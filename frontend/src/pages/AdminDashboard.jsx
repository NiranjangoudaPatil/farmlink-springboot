import { useState } from 'react'
import { Shield } from 'lucide-react'
import AdminAnalyticsTab from './admin/AdminAnalyticsTab'
import AdminUsersTab from './admin/AdminUsersTab'
import AdminProductsTab from './admin/AdminProductsTab'
import AdminOrdersTab from './admin/AdminOrdersTab'
import AdminReviewsTab from './admin/AdminReviewsTab'

const TABS = [
  ['analytics', 'Analytics', AdminAnalyticsTab],
  ['users', 'Users', AdminUsersTab],
  ['products', 'Products', AdminProductsTab],
  ['orders', 'Orders', AdminOrdersTab],
  ['reviews', 'Reviews', AdminReviewsTab],
]

export default function AdminDashboard() {
  const [tab, setTab] = useState('analytics')
  const Current = TABS.find(([key]) => key === tab)[2]

  return (
    <div className="page">
      <div className="container">
        <h1 className="page-title"><Shield size={30} color="var(--primary)" /> Admin Portal</h1>
        <p className="page-sub">Manage users, products, orders, and platform analytics</p>

        <div className="tabs" style={{ marginBottom: 24 }}>
          {TABS.map(([key, label]) => (
            <button key={key} className={`tab ${tab === key ? 'active' : ''}`} onClick={() => setTab(key)}>{label}</button>
          ))}
        </div>

        <Current />
      </div>
    </div>
  )
}
