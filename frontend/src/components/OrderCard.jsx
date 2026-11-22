import { money } from '../api'

const statusClass = (s) => (s === 'DELIVERED' ? 'badge-primary' : s === 'CANCELLED' ? 'badge-danger' : '')

// Shared by retailer and farmer order lists. `actions` is whatever buttons the page wants to show.
export default function OrderCard({ order, who, actions }) {
  return (
    <div className="card card-pad order-card">
      <div className="row between">
        <div>
          <h3 style={{ fontSize: '1.1rem' }}>Order #{order.id.slice(0, 8)}</h3>
          <p className="small muted">
            {who === 'farmer' ? `Retailer: ${order.retailer?.fullName}` : `Farmer: ${order.farmer?.fullName}`}
            {' • '}{new Date(order.createdAt).toLocaleDateString()}
          </p>
        </div>
        <div className="row">
          <span className={`badge ${statusClass(order.status)}`}>{order.status.toLowerCase()}</span>
          {order.paymentStatus === 'PAID'
            ? <span className="badge badge-paid">Paid</span>
            : <span className="badge badge-outline">Payment pending</span>}
        </div>
      </div>

      <ul className="order-items">
        {order.items.map((i) => (
          <li key={i.productId}>{i.productName} — {i.quantity} × {money(i.pricePerUnit)} = {money(i.totalPrice)}</li>
        ))}
      </ul>
      {order.deliveryAddress && <p className="small muted">Deliver to: {order.deliveryAddress}</p>}
      {order.notes && <p className="small muted">Note: {order.notes}</p>}

      <div className="row between" style={{ marginTop: 12 }}>
        <span className="total-line">Total: {money(order.totalAmount)}</span>
        <div className="row">{actions}</div>
      </div>
    </div>
  )
}
