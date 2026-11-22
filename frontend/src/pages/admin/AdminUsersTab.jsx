import { useEffect, useState } from 'react'
import { CheckCircle, XCircle } from 'lucide-react'
import { api } from '../../api'
import { useToast } from '../../toast'

export default function AdminUsersTab() {
  const toast = useToast()
  const [users, setUsers] = useState(null)

  const load = () => api.get('/api/admin/users?size=500').then((p) => setUsers(p.content)).catch((e) => toast.error(e.message))
  useEffect(() => { load() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const verify = async (u, verified) => {
    try {
      await api.patch(`/api/admin/users/${u.id}/verification`, { verified })
      toast.success(verified ? 'User verified!' : 'Verification revoked')
      load()
    } catch (e) {
      toast.error(e.message)
    }
  }

  if (!users) return <p className="muted">Loading…</p>
  const farmers = users.filter((u) => u.role === 'FARMER')
  const retailers = users.filter((u) => u.role === 'RETAILER')

  return (
    <div className="stack" style={{ gap: 24 }}>
      <div className="card">
        <div className="card-head"><h3>Farmers ({farmers.length})</h3></div>
        <div className="card-body table-wrap">
          {farmers.length === 0 ? <p className="empty">No farmers registered.</p> : (
            <table>
              <thead><tr><th>Name</th><th>Email</th><th>Location</th><th>Phone</th><th>License</th><th>Status</th><th className="right">Actions</th></tr></thead>
              <tbody>
                {farmers.map((f) => (
                  <tr key={f.id}>
                    <td><strong>{f.fullName}</strong></td>
                    <td>{f.email}</td>
                    <td>{f.location || '—'}</td>
                    <td>{f.phone || '—'}</td>
                    <td>{f.licenseNumber ? <code className="tag">{f.licenseNumber}</code> : <em className="muted">N/A</em>}</td>
                    <td>
                      {f.verified
                        ? <span className="badge badge-soft"><CheckCircle size={12} /> Verified</span>
                        : <span className="badge badge-danger"><XCircle size={12} /> Unverified</span>}
                    </td>
                    <td className="right">
                      {f.verified
                        ? <button className="btn btn-outline btn-sm" onClick={() => verify(f, false)}>Revoke</button>
                        : <button className="btn btn-sm" onClick={() => {
                            // The original app blocked this; here the admin may override, after a warning.
                            if (!f.licenseNumber && !window.confirm(`${f.fullName} has no licence number on file. Verify anyway?`)) return
                            verify(f, true)
                          }}><CheckCircle size={14} /> Verify</button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      <div className="card">
        <div className="card-head"><h3>Retailers ({retailers.length})</h3></div>
        <div className="card-body table-wrap">
          {retailers.length === 0 ? <p className="empty">No retailers registered.</p> : (
            <table>
              <thead><tr><th>Name</th><th>Email</th><th>Location</th><th>Phone</th><th>Status</th><th className="right">Actions</th></tr></thead>
              <tbody>
                {retailers.map((r) => (
                  <tr key={r.id}>
                    <td><strong>{r.fullName}</strong></td>
                    <td>{r.email}</td>
                    <td>{r.location || '—'}</td>
                    <td>{r.phone || '—'}</td>
                    <td>{r.verified ? <span className="badge badge-soft"><CheckCircle size={12} /> Active</span> : <span className="badge">Pending</span>}</td>
                    <td className="right">
                      {r.verified
                        ? <button className="btn btn-outline btn-sm" onClick={() => verify(r, false)}>Deactivate</button>
                        : <button className="btn btn-sm" onClick={() => verify(r, true)}>Activate</button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  )
}
