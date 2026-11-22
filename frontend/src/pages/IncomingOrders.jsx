import { useEffect, useState } from 'react'
import { api } from '../api'
import { useToast } from '../toast'
import OrderCard from '../components/OrderCard'

// Which buttons to SHOW for each status. OrderService enforces the real rules on the server.
const NEXT = {
  PENDING: [['CONFIRMED', 'Confirm'], ['CANCELLED', 'Reject']],
  CONFIRMED: [['SHIPPED', 'Mark Shipped'], ['CANCELLED', 'Cancel']],
  SHIPPED: [['DELIVERED', 'Mark Delivered']],
}

export default function IncomingOrders() {
  const toast = useToast()
  const [orders, setOrders] = useState(null)

  const load = () => api.get('/api/orders/incoming').then(setOrders).catch((e) => toast.error(e.message))
  useEffect(() => { load() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const change = async (o, status) => {
    try {
      await api.patch(`/api/orders/${o.id}/status`, { status })
      toast.success(`Order ${status.toLowerCase()}`)
      load()
    } catch (e) {
      toast.error(e.message) // e.g. "Insufficient stock" when confirming
    }
  }

  return (
    <div className="page">
      <div className="container" style={{ maxWidth: 900 }}>
        <h1 className="page-title">Incoming Orders</h1>
        <p className="page-sub">Orders placed by retailers for your produce</p>

        {!orders && <p className="muted">Loading…</p>}
        {orders?.length === 0 && <div className="card empty">No incoming orders yet.</div>}
        {orders?.map((o) => (
          <OrderCard
            key={o.id}
            order={o}
            who="farmer"
            actions={(NEXT[o.status] || []).map(([status, label]) => (
              <button key={status} className={`btn btn-sm ${status === 'CANCELLED' ? 'btn-outline' : ''}`} onClick={() => change(o, status)}>
                {label}
              </button>
            ))}
          />
        ))}
      </div>
    </div>
  )
}
