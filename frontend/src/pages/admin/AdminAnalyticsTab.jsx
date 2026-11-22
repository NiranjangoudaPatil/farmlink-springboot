import { useEffect, useState } from 'react'
import { Package, ShoppingCart, Star, Users } from 'lucide-react'
import { Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api, money } from '../../api'
import { useToast } from '../../toast'

const COLORS = ['hsl(153,50%,27%)', 'hsl(36,80%,55%)', 'hsl(0,84%,60%)', 'hsl(200,60%,50%)', 'hsl(280,50%,50%)', 'hsl(100,40%,40%)']

export default function AdminAnalyticsTab() {
  const toast = useToast()
  const [stats, setStats] = useState(null)
  const [monthly, setMonthly] = useState([])

  useEffect(() => {
    api.get('/api/admin/analytics').then(setStats).catch((e) => toast.error(e.message))

    // Monthly chart: group the orders by month in the browser (same approach as the original app).
    api.get('/api/admin/orders?size=500').then((page) => {
      const byMonth = {}
      page.content.forEach((o) => {
        const m = new Date(o.createdAt).toLocaleDateString('en-US', { year: 'numeric', month: 'short' })
        byMonth[m] ??= { month: m, orders: 0, revenue: 0 }
        byMonth[m].orders++
        byMonth[m].revenue += Number(o.totalAmount)
      })
      setMonthly(Object.values(byMonth).reverse().slice(-6)) // API is newest-first; chart wants oldest-first
    }).catch(() => {})
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (!stats) return <p className="muted">Loading…</p>

  const byStatus = Object.entries(stats.ordersByStatus).filter(([, v]) => v > 0).map(([name, value]) => ({ name: name.toLowerCase(), value }))
  const cards = [
    { label: 'Farmers', value: stats.usersByRole.FARMER, icon: Users, cls: 'ico-primary' },
    { label: 'Retailers', value: stats.usersByRole.RETAILER, icon: Users, cls: 'ico-secondary' },
    { label: 'Products', value: stats.products, icon: Package, cls: 'ico-primary' },
    { label: 'Orders', value: stats.orders, icon: ShoppingCart, cls: 'ico-primary' },
    { label: 'Revenue', value: money(stats.revenue), icon: ShoppingCart, cls: 'ico-success' },
    { label: 'Reviews', value: stats.reviews, icon: Star, cls: 'ico-secondary' },
  ]

  return (
    <>
      <div className="stats">
        {cards.map((c) => (
          <div className="card stat" key={c.label}>
            <div className={`ico ${c.cls}`}><c.icon size={20} /></div>
            <div><p className="lbl">{c.label}</p><p className="val">{c.value}</p></div>
          </div>
        ))}
      </div>

      <div className="charts">
        <div className="card card-pad">
          <h3 style={{ marginBottom: 12 }}>Monthly Orders & Revenue</h3>
          {monthly.length === 0 ? <p className="empty">No data yet</p> : (
            <ResponsiveContainer width="100%" height={250}>
              <BarChart data={monthly}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="month" fontSize={12} />
                <YAxis fontSize={12} />
                <Tooltip />
                <Bar dataKey="orders" fill="hsl(153,50%,27%)" name="Orders" />
                <Bar dataKey="revenue" fill="hsl(36,80%,55%)" name="Revenue (₹)" />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="card card-pad">
          <h3 style={{ marginBottom: 12 }}>Orders by Status</h3>
          {byStatus.length === 0 ? <p className="empty">No data yet</p> : (
            <ResponsiveContainer width="100%" height={250}>
              <PieChart>
                <Pie data={byStatus} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label={({ name, value }) => `${name}: ${value}`}>
                  {byStatus.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>
    </>
  )
}
