import { useEffect, useState } from 'react'
import { api, money } from '../../api'
import { useToast } from '../../toast'

export default function AdminOrdersTab() {
  const toast = useToast()
  const [orders, setOrders] = useState(null)

  useEffect(() => {
    api.get('/api/admin/orders?size=500').then((p) => setOrders(p.content)).catch((e) => toast.error(e.message))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (!orders) return <p className="muted">Loading…</p>

  return (
    <div className="card">
      <div className="card-head"><h3>All Orders ({orders.length})</h3></div>
      <div className="card-body table-wrap">
        {orders.length === 0 ? <p className="empty">No orders yet.</p> : (
          <table>
            <thead><tr><th>Order ID</th><th>Retailer</th><th>Farmer</th><th>Amount</th><th>Status</th><th>Payment</th><th>Date</th></tr></thead>
            <tbody>
              {orders.map((o) => (
                <tr key={o.id}>
                  <td style={{ fontFamily: 'monospace' }}>{o.id.slice(0, 8)}</td>
                  <td>{o.retailer?.fullName}</td>
                  <td>{o.farmer?.fullName}</td>
                  <td><strong>{money(o.totalAmount)}</strong></td>
                  <td><span className="badge">{o.status.toLowerCase()}</span></td>
                  <td>{o.paymentStatus === 'PAID' ? <span className="badge badge-paid">Paid</span> : <span className="badge badge-outline">Pending</span>}</td>
                  <td className="muted">{new Date(o.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
