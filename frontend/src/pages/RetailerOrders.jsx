import { useEffect, useState } from 'react'
import { api } from '../api'
import { useToast } from '../toast'
import { payForOrders } from '../lib'
import OrderCard from '../components/OrderCard'
import Modal from '../components/Modal'
import Stars from '../components/Stars'

export default function RetailerOrders() {
  const toast = useToast()
  const [orders, setOrders] = useState(null)
  const [reviewing, setReviewing] = useState(null) // the order being rated
  const [rating, setRating] = useState(5)
  const [comment, setComment] = useState('')

  const load = () => api.get('/api/orders/mine').then(setOrders).catch((e) => toast.error(e.message))
  useEffect(() => { load() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const run = async (fn) => {
    try {
      await fn()
      await load()
    } catch (e) {
      toast.error(e.message)
    }
  }

  const cancel = (o) => run(async () => {
    await api.patch(`/api/orders/${o.id}/status`, { status: 'CANCELLED' })
    toast.success('Order cancelled')
  })

  const pay = (o) => run(async () => {
    const paid = await payForOrders([o.id])
    if (paid) toast.success('Payment successful!')
  })

  const submitReview = () => run(async () => {
    await api.post('/api/reviews', { orderId: reviewing.id, rating, comment: comment || null })
    toast.success('Thanks for your review!')
    setReviewing(null)
    setRating(5)
    setComment('')
  })

  return (
    <div className="page">
      <div className="container" style={{ maxWidth: 900 }}>
        <h1 className="page-title">My Orders</h1>
        <p className="page-sub">Track your orders, pay, and rate farmers</p>

        {!orders && <p className="muted">Loading…</p>}
        {orders?.length === 0 && <div className="card empty">No orders yet. Visit the marketplace to place one.</div>}
        {orders?.map((o) => (
          <OrderCard
            key={o.id}
            order={o}
            who="retailer"
            actions={
              <>
                {o.status === 'PENDING' && <button className="btn btn-sm btn-outline" onClick={() => cancel(o)}>Cancel</button>}
                {o.paymentStatus === 'UNPAID' && o.status !== 'CANCELLED' && <button className="btn btn-sm" onClick={() => pay(o)}>Pay now</button>}
                {o.status === 'DELIVERED' && <button className="btn btn-sm btn-outline" onClick={() => setReviewing(o)}>⭐ Rate farmer</button>}
              </>
            }
          />
        ))}
      </div>

      <Modal
        open={!!reviewing}
        onClose={() => setReviewing(null)}
        title={`Rate ${reviewing?.farmer?.fullName || 'farmer'}`}
        footer={
          <>
            <button className="btn btn-outline" onClick={() => setReviewing(null)}>Cancel</button>
            <button className="btn" onClick={submitReview}>Submit review</button>
          </>
        }
      >
        <div className="stack">
          <Stars value={rating} onChange={setRating} size={28} />
          <textarea className="input" rows={3} placeholder="Share your experience (optional)" value={comment} onChange={(e) => setComment(e.target.value)} />
        </div>
      </Modal>
    </div>
  )
}
