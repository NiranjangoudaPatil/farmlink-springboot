import { useEffect, useMemo, useState } from 'react'
import { AlertTriangle, CreditCard, MapPin, Search, ShoppingCart, Trash2 } from 'lucide-react'
import { api, assetUrl, money } from '../api'
import { useAuth } from '../auth'
import { useCart } from '../cart'
import { useToast } from '../toast'
import { CATEGORY_LABELS, cap, distanceKm, estimateHours, payForOrders } from '../lib'
import Modal from '../components/Modal'
import Stars from '../components/Stars'

const LOC_KEY = 'farmlink_delivery_location'

export default function Marketplace() {
  const { user } = useAuth()
  const cart = useCart()
  const toast = useToast()
  const isRetailer = user?.role === 'RETAILER'

  const [products, setProducts] = useState(null)
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('')
  const [ratings, setRatings] = useState({})       // farmerId -> { average, count }
  const [qty, setQty] = useState({})               // productId -> quantity typed in the card
  const [placing, setPlacing] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)    // bump to re-fetch products (stock changes after an order)

  const [location, setLocation] = useState(() => {
    try { return JSON.parse(localStorage.getItem(LOC_KEY)) } catch { return null }
  })
  const [address, setAddress] = useState(location?.address || '')

  const [warning, setWarning] = useState(null)     // { product, quantity, text }
  const [reviewFor, setReviewFor] = useState(null) // { farmer, reviews }

  // Load products (search/category are handled by the backend), with a small debounce.
  useEffect(() => {
    const params = new URLSearchParams({ size: 100 })
    if (search.trim()) params.set('q', search.trim())
    if (category) params.set('category', category)
    const t = setTimeout(() => {
      api.get(`/api/products?${params}`).then((p) => setProducts(p.content)).catch((e) => toast.error(e.message))
    }, 250)
    return () => clearTimeout(t)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search, category, reloadKey])

  // Fetch each farmer's average rating once.
  useEffect(() => {
    if (!products) return
    const ids = [...new Set(products.map((p) => p.farmer?.id).filter(Boolean))].filter((id) => !(id in ratings))
    ids.forEach((id) =>
      api.get(`/api/farmers/${id}/rating`).then((r) => setRatings((cur) => ({ ...cur, [id]: r }))).catch(() => {}),
    )
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [products])

  const saveLocation = (loc) => {
    setLocation(loc)
    localStorage.setItem(LOC_KEY, JSON.stringify(loc))
  }

  const useMyLocation = () => {
    if (!navigator.geolocation) return toast.error('Geolocation is not supported by this browser')
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        saveLocation({ lat: pos.coords.latitude, lng: pos.coords.longitude, address: address || 'My current location' })
        toast.success('Delivery location set!')
      },
      () => toast.error('Could not get your location. Allow location access and try again.'),
    )
  }

  const distanceTo = (p) =>
    location && p.latitude != null && p.longitude != null ? distanceKm(location.lat, location.lng, p.latitude, p.longitude) : null

  const tryAdd = (p) => {
    const quantity = Number(qty[p.id] || 1)
    const inCart = cart.items.find((i) => i.product.id === p.id)?.quantity || 0
    if (inCart + quantity > Number(p.quantityAvailable)) {
      return toast.error(`Only ${p.quantityAvailable} ${p.unit} of ${p.name} available${inCart ? ` (${inCart} already in your cart)` : ''}`)
    }
    const d = distanceTo(p)
    if (d != null && p.shelfLifeHours && estimateHours(d) >= p.shelfLifeHours * 0.8) {
      // Same rule as the backend's delivery check: warn when ETA uses up 80%+ of the shelf life.
      return setWarning({
        product: p,
        quantity,
        text: `${p.name} is about ${d.toFixed(0)} km away (about ${estimateHours(d)}h delivery) but its shelf life is only ${p.shelfLifeHours}h. It may not arrive fresh.`,
      })
    }
    cart.add(p, quantity)
    toast.success(`Added ${p.name} to cart`)
  }

  const openReviews = async (p) => {
    try {
      const page = await api.get(`/api/farmers/${p.farmer.id}/reviews?size=20`)
      setReviewFor({ farmer: p.farmer.fullName, reviews: page.content })
    } catch (e) {
      toast.error(e.message)
    }
  }

  const placeOrder = async () => {
    setPlacing(true)
    try {
      const orders = await api.post('/api/orders', {
        items: cart.items.map((i) => ({ productId: i.product.id, quantity: i.quantity })),
        deliveryAddress: location?.address || address || null,
        deliveryLatitude: location?.lat ?? null,
        deliveryLongitude: location?.lng ?? null,
      })
      cart.clear()
      toast.success(`Order placed (${orders.length} order${orders.length > 1 ? 's' : ''})`)
      try {
        const paid = await payForOrders(orders.map((o) => o.id))
        if (paid) toast.success('Payment successful!')
        else toast.info('Order placed but payment pending. You can pay later from My Orders.')
      } catch {
        toast.info('Order placed but payment pending. You can pay later from My Orders.')
      }
    } catch (e) {
      toast.error(e.message)
    } finally {
      setPlacing(false)
      setReloadKey((k) => k + 1) // stock was reduced (or someone else bought it): show the real numbers
    }
  }

  const list = useMemo(() => products || [], [products])

  return (
    <div className="page">
      <div className="container">
        <h1 className="page-title">Marketplace</h1>
        <p className="page-sub">Browse fresh produce directly from local farmers</p>

        {isRetailer && (
          <div className="card loc-bar">
            <MapPin size={18} color="var(--primary)" />
            <input className="input" style={{ flex: 1, minWidth: 200 }} placeholder="Delivery address (optional)"
              value={address} onChange={(e) => setAddress(e.target.value)}
              onBlur={() => location && saveLocation({ ...location, address })} />
            <button className="btn btn-outline btn-sm" onClick={useMyLocation}>
              {location ? 'Update my location' : 'Use my location'}
            </button>
            {location && <span className="badge badge-soft">📍 location set</span>}
          </div>
        )}

        <div className="row" style={{ gap: 16, marginBottom: 28 }}>
          <div className="input-icon">
            <Search size={16} />
            <input className="input" placeholder="Search products..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          <select className="input" style={{ width: 190 }} value={category} onChange={(e) => setCategory(e.target.value)}>
            <option value="">All Categories</option>
            {Object.entries(CATEGORY_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </div>

        <div className={isRetailer ? 'market-grid' : ''}>
          <div>
            {!products && <p className="muted">Loading…</p>}
            {products && list.length === 0 && (
              <div className="card empty">No products found. Try adjusting your filters.</div>
            )}
            <div className="product-grid">
              {list.map((p) => {
                const d = distanceTo(p)
                const rating = ratings[p.farmer?.id]
                const lowShelf = p.shelfLifeHours && p.shelfLifeHours <= 48
                const risky = d != null && p.shelfLifeHours && estimateHours(d) >= p.shelfLifeHours * 0.8
                return (
                  <article className="card product" key={p.id}>
                    {p.imageUrl ? <img className="pimg" src={assetUrl(p.imageUrl)} alt={p.name} /> : <div className="pimg noimg">🥬</div>}
                    <div className="pbody">
                      <div className="row between">
                        <span className="badge">{cap(p.category)}</span>
                        {d != null && <span className="badge badge-outline">📍 {d.toFixed(0)} km</span>}
                      </div>
                      <h3>{p.name}</h3>
                      {rating?.count > 0 && (
                        <button className="link-rating" onClick={() => openReviews(p)}>
                          ⭐ {rating.average.toFixed(1)} ({rating.count} reviews)
                        </button>
                      )}
                      <p className="small muted">by {p.farmer?.fullName}{p.farmer?.verified && ' ✔'}</p>
                      {p.description && <p className="small muted">{p.description}</p>}
                      <div className="row between">
                        <span className="price">{money(p.pricePerUnit)}<small>/{p.unit}</small></span>
                        <span className={`small ${Number(p.quantityAvailable) <= 0 ? 'err-text' : 'muted'}`}>
                          {Number(p.quantityAvailable) <= 0 ? 'Out of stock' : `${p.quantityAvailable} available`}
                        </span>
                      </div>
                      {p.location && <p className="xs muted row" style={{ gap: 4 }}><MapPin size={12} />{p.location}</p>}
                      {lowShelf && <p className="warn-line"><AlertTriangle size={12} />Shelf life: {p.shelfLifeHours}h</p>}
                      {risky && <p className="warn-line"><AlertTriangle size={12} />Delivery may exceed shelf life</p>}

                      <div style={{ marginTop: 'auto' }}>
                        {isRetailer && Number(p.quantityAvailable) <= 0 && (
                          <button className="btn btn-sm btn-full" disabled>Out of stock</button>
                        )}
                        {isRetailer && Number(p.quantityAvailable) > 0 && (
                          <>
                            <input className="input" type="number" min="1" max={p.quantityAvailable} style={{ marginBottom: 8 }}
                              value={qty[p.id] || 1} onChange={(e) => setQty({ ...qty, [p.id]: Number(e.target.value) })} />
                            <button className="btn btn-sm btn-full" onClick={() => tryAdd(p)}>
                              <ShoppingCart size={16} /> Add to Cart
                            </button>
                          </>
                        )}
                        {!user && <p className="center xs muted">Sign in as retailer to order</p>}
                      </div>
                    </div>
                  </article>
                )
              })}
            </div>
          </div>

          {isRetailer && (
            <aside className="sticky">
              <div className="card">
                <div className="card-head"><h3 className="row"><ShoppingCart size={18} /> Cart ({cart.items.length})</h3></div>
                <div className="card-body">
                  {cart.items.length === 0 ? (
                    <p className="small muted">Your cart is empty</p>
                  ) : (
                    <>
                      <div className="stack" style={{ marginBottom: 14 }}>
                        {cart.items.map(({ product: p, quantity }) => (
                          <div className="cart-line" key={p.id}>
                            <div>
                              <p style={{ fontWeight: 500 }}>{p.name}</p>
                              <p className="muted">{quantity} × {money(p.pricePerUnit)}</p>
                            </div>
                            <div className="row" style={{ gap: 4 }}>
                              <strong>{money(quantity * Number(p.pricePerUnit))}</strong>
                              <button className="btn btn-ghost btn-icon" onClick={() => cart.remove(p.id)} title="Remove"><Trash2 size={14} /></button>
                            </div>
                          </div>
                        ))}
                      </div>
                      <div style={{ borderTop: '1px solid var(--border)', paddingTop: 12 }}>
                        <div className="row between total-line" style={{ marginBottom: 12 }}>
                          <span>Total</span><span>{money(cart.total)}</span>
                        </div>
                        <button className="btn btn-full" onClick={placeOrder} disabled={placing}>
                          <CreditCard size={16} /> {placing ? 'Processing...' : 'Pay & Place Order'}
                        </button>
                      </div>
                    </>
                  )}
                </div>
              </div>
            </aside>
          )}
        </div>
      </div>

      <Modal
        open={!!warning}
        onClose={() => setWarning(null)}
        title="⚠️ Delivery Warning"
        footer={
          <>
            <button className="btn btn-outline" onClick={() => setWarning(null)}>Cancel</button>
            <button className="btn" onClick={() => { cart.add(warning.product, warning.quantity); toast.success('Added to cart'); setWarning(null) }}>Add Anyway</button>
          </>
        }
      >
        <p className="muted">{warning?.text}</p>
      </Modal>

      <Modal open={!!reviewFor} onClose={() => setReviewFor(null)} title={`Reviews - ${reviewFor?.farmer}`}
        footer={<button className="btn btn-outline" onClick={() => setReviewFor(null)}>Close</button>}>
        <div className="stack">
          {reviewFor?.reviews.length === 0 && <p className="muted">No reviews yet.</p>}
          {reviewFor?.reviews.map((r) => (
            <div key={r.id} style={{ borderBottom: '1px solid var(--border)', paddingBottom: 8 }}>
              <Stars value={r.rating} />
              <p className="small muted">{r.comment || 'No comment'} — {r.reviewer?.fullName}</p>
            </div>
          ))}
        </div>
      </Modal>
    </div>
  )
}
