import { useEffect, useState } from 'react'
import { CheckCircle, Package, Plus, ShoppingCart, Star, TrendingUp, XCircle } from 'lucide-react'
import { api, assetUrl, money } from '../api'
import { useAuth } from '../auth'
import { useToast } from '../toast'
import { CATEGORY_LABELS, cap } from '../lib'
import Modal from '../components/Modal'
import Stars from '../components/Stars'

const EMPTY = {
  name: '', description: '', category: 'VEGETABLES', pricePerUnit: '', unit: 'kg',
  quantityAvailable: '', location: '', shelfLifeHours: 48, available: true,
}

export default function FarmerDashboard() {
  const { user } = useAuth()
  const toast = useToast()
  const [products, setProducts] = useState(null)
  const [stats, setStats] = useState(null)
  const [form, setForm] = useState(EMPTY)
  const [editingId, setEditingId] = useState(null)
  const [open, setOpen] = useState(false)
  const [file, setFile] = useState(null)
  const [saving, setSaving] = useState(false)

  // The login-time user object goes stale once an admin verifies the farmer, so ask the backend again.
  const [verified, setVerified] = useState(user.verified)

  const load = () => {
    api.get('/api/users/me').then((me) => setVerified(me.verified)).catch(() => {})
    api.get('/api/products/mine').then(setProducts).catch((e) => toast.error(e.message))
    api.get('/api/farmers/me/analytics').then(setStats).catch(() => {})
  }
  useEffect(load, []) // eslint-disable-line react-hooks/exhaustive-deps

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

  const openNew = () => { setForm(EMPTY); setEditingId(null); setFile(null); setOpen(true) }
  const openEdit = (p) => {
    setEditingId(p.id)
    setFile(null)
    setForm({
      name: p.name, description: p.description || '', category: p.category, pricePerUnit: p.pricePerUnit,
      unit: p.unit, quantityAvailable: p.quantityAvailable, location: p.location || '',
      shelfLifeHours: p.shelfLifeHours, available: p.available,
    })
    setOpen(true)
  }

  const save = async (e) => {
    e.preventDefault()
    setSaving(true)
    const body = {
      ...form,
      pricePerUnit: Number(form.pricePerUnit),
      quantityAvailable: Number(form.quantityAvailable),
      shelfLifeHours: Number(form.shelfLifeHours),
      description: form.description || null,
      location: form.location || null,
    }
    try {
      const saved = editingId ? await api.put(`/api/products/${editingId}`, body) : await api.post('/api/products', body)
      if (file) await api.upload(`/api/products/${saved.id}/image`, file) // the image is a second, multipart request
      toast.success(editingId ? 'Product updated' : 'Product added')
      setOpen(false)
      load()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setSaving(false)
    }
  }

  const remove = async (p) => {
    if (!window.confirm(`Delete ${p.name}?`)) return
    try {
      await api.del(`/api/products/${p.id}`)
      toast.success('Product removed')
      load()
    } catch (err) {
      toast.error(err.message)
    }
  }

  const cards = stats && [
    { label: 'Total orders', value: stats.totalOrders, icon: ShoppingCart, cls: 'ico-primary' },
    { label: 'Pending orders', value: stats.pendingOrders, icon: Package, cls: 'ico-secondary' },
    { label: 'Revenue', value: money(stats.revenue), icon: TrendingUp, cls: 'ico-success' },
    { label: `Rating (${stats.reviewCount})`, value: stats.reviewCount ? `${stats.averageRating.toFixed(1)} ★` : '—', icon: Star, cls: 'ico-secondary' },
  ]

  return (
    <div className="page">
      <div className="container">
        <div className="row between" style={{ marginBottom: 24 }}>
          <div>
            <h1 className="page-title" style={{ marginBottom: 4 }}>
              Farmer Dashboard
              {verified
                ? <span className="badge badge-soft"><CheckCircle size={12} /> Verified</span>
                : <span className="badge badge-danger"><XCircle size={12} /> Awaiting verification</span>}
            </h1>
            <p className="muted">Manage your products and track how you're doing</p>
          </div>
          <button className="btn" onClick={openNew} disabled={!verified} title={verified ? '' : 'Available after admin verification'}>
            <Plus size={16} /> Add Product
          </button>
        </div>

        {!verified && (
          <div className="card card-pad" style={{ marginBottom: 24, borderColor: 'hsl(36 80% 55% / .5)', background: 'hsl(36 80% 55% / .08)' }}>
            <strong>Your account is awaiting admin verification.</strong>
            <p className="small muted" style={{ marginTop: 4 }}>
              You can add products once an admin has verified your account. Make sure your farming licence number is on file so the admin can approve you.
            </p>
          </div>
        )}

        {cards && (
          <div className="stats">
            {cards.map((c) => (
              <div className="card stat" key={c.label}>
                <div className={`ico ${c.cls}`}><c.icon size={20} /></div>
                <div><p className="lbl">{c.label}</p><p className="val">{c.value}</p></div>
              </div>
            ))}
          </div>
        )}

        <h2 id="my-products" style={{ fontSize: '1.4rem', marginBottom: 16 }}>My Products</h2>
        {!products && <p className="muted">Loading…</p>}
        {products?.length === 0 && <div className="card empty">You haven't listed anything yet. Click “Add Product”.</div>}
        <div className="product-grid">
          {products?.map((p) => (
            <article className="card product" key={p.id}>
              {p.imageUrl ? <img className="pimg" src={assetUrl(p.imageUrl)} alt={p.name} /> : <div className="pimg noimg">🥬</div>}
              <div className="pbody">
                <div className="row between">
                  <span className="badge">{cap(p.category)}</span>
                  <span className={`badge ${p.available ? 'badge-soft' : 'badge-outline'}`}>{p.available ? 'Active' : 'Hidden'}</span>
                </div>
                <h3>{p.name}</h3>
                <div className="row between">
                  <span className="price">{money(p.pricePerUnit)}<small>/{p.unit}</small></span>
                  <span className="small muted">{p.quantityAvailable} in stock</span>
                </div>
                <div className="row" style={{ marginTop: 'auto' }}>
                  <button className="btn btn-outline btn-sm grow" onClick={() => openEdit(p)}>Edit</button>
                  <button className="btn btn-outline btn-sm grow" onClick={() => remove(p)}>Delete</button>
                </div>
              </div>
            </article>
          ))}
        </div>

        {stats && (
          <div className="charts" style={{ marginTop: 32 }}>
            <div className="card card-pad">
              <h3 style={{ marginBottom: 10 }}>Top products</h3>
              {stats.topProducts.length === 0 && <p className="muted small">Nothing sold yet.</p>}
              {stats.topProducts.map((t) => (
                <div className="row between small" key={t.productId} style={{ padding: '6px 0' }}>
                  <span>{t.name}</span><strong>{t.quantitySold} sold</strong>
                </div>
              ))}
            </div>
            <div className="card card-pad">
              <h3 style={{ marginBottom: 10 }}>Recent reviews</h3>
              {stats.recentReviews.length === 0 && <p className="muted small">No reviews yet.</p>}
              {stats.recentReviews.map((r) => (
                <div key={r.id} style={{ padding: '6px 0' }}>
                  <Stars value={r.rating} />
                  <p className="small muted">{r.comment || 'No comment'} — {r.reviewer?.fullName}</p>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      <Modal
        open={open}
        onClose={() => setOpen(false)}
        title={editingId ? 'Edit product' : 'Add a product'}
        footer={
          <>
            <button type="button" className="btn btn-outline" onClick={() => setOpen(false)}>Cancel</button>
            <button type="submit" form="product-form" className="btn" disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
          </>
        }
      >
        <form id="product-form" className="stack" onSubmit={save}>
          <label className="field"><span>Product Name</span><input className="input" required value={form.name} onChange={set('name')} /></label>
          <div className="form-grid">
            <label className="field"><span>Category</span>
              <select className="input" value={form.category} onChange={set('category')}>
                {Object.entries(CATEGORY_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
            </label>
            <label className="field"><span>Unit</span><input className="input" value={form.unit} onChange={set('unit')} /></label>
            <label className="field"><span>Price (₹)</span><input className="input" type="number" min="0.01" step="0.01" required value={form.pricePerUnit} onChange={set('pricePerUnit')} /></label>
            <label className="field"><span>Quantity</span><input className="input" type="number" min="0" step="0.1" required value={form.quantityAvailable} onChange={set('quantityAvailable')} /></label>
            <label className="field"><span>Location</span><input className="input" placeholder="e.g. Pune, Maharashtra" value={form.location} onChange={set('location')} /></label>
            <label className="field"><span>Shelf Life (hours)</span><input className="input" type="number" min="1" value={form.shelfLifeHours} onChange={set('shelfLifeHours')} /></label>
          </div>
          <label className="field"><span>Description</span><textarea className="input" rows={2} value={form.description} onChange={set('description')} /></label>
          <label className="field"><span>Product Image</span><input className="input" type="file" accept="image/*" onChange={(e) => setFile(e.target.files[0])} /></label>
          <label className="check"><input type="checkbox" checked={form.available} onChange={set('available')} /> Visible in marketplace</label>
        </form>
      </Modal>
    </div>
  )
}
