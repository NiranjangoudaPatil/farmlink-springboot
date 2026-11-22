import { useEffect, useState } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { api, money } from '../../api'
import { useToast } from '../../toast'
import { cap } from '../../lib'

export default function AdminProductsTab() {
  const toast = useToast()
  const [products, setProducts] = useState(null)

  const load = () => api.get('/api/admin/products?size=500').then((p) => setProducts(p.content)).catch((e) => toast.error(e.message))
  useEffect(() => { load() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const toggle = async (p) => {
    try {
      await api.patch(`/api/admin/products/${p.id}/availability`, { available: !p.available })
      toast.success(p.available ? 'Product disabled' : 'Product enabled')
      load()
    } catch (e) {
      toast.error(e.message)
    }
  }

  if (!products) return <p className="muted">Loading…</p>

  return (
    <div className="card">
      <div className="card-head"><h3>All Products ({products.length})</h3></div>
      <div className="card-body table-wrap">
        {products.length === 0 ? <p className="empty">No products listed.</p> : (
          <table>
            <thead><tr><th>Product</th><th>Category</th><th>Farmer</th><th>Price</th><th>Stock</th><th>Shelf Life</th><th>Status</th><th className="right">Actions</th></tr></thead>
            <tbody>
              {products.map((p) => (
                <tr key={p.id}>
                  <td><strong>{p.name}</strong></td>
                  <td><span className="badge">{cap(p.category)}</span></td>
                  <td>{p.farmer?.fullName}</td>
                  <td>{money(p.pricePerUnit)}/{p.unit}</td>
                  <td>{p.quantityAvailable}</td>
                  <td>{p.shelfLifeHours ? `${p.shelfLifeHours}h` : '—'}</td>
                  <td><span className={`badge ${p.available ? 'badge-primary' : ''}`}>{p.available ? 'Active' : 'Disabled'}</span></td>
                  <td className="right">
                    <button className="btn btn-outline btn-sm" onClick={() => toggle(p)}>
                      {p.available ? <><EyeOff size={14} /> Disable</> : <><Eye size={14} /> Enable</>}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
