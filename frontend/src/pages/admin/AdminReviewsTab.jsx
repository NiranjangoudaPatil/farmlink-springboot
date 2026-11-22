import { useEffect, useState } from 'react'
import { api } from '../../api'
import { useToast } from '../../toast'
import Stars from '../../components/Stars'

export default function AdminReviewsTab() {
  const toast = useToast()
  const [reviews, setReviews] = useState(null)

  useEffect(() => {
    api.get('/api/admin/reviews?size=500').then((p) => setReviews(p.content)).catch((e) => toast.error(e.message))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (!reviews) return <p className="muted">Loading…</p>

  return (
    <div className="card">
      <div className="card-head"><h3>All Reviews ({reviews.length})</h3></div>
      <div className="card-body table-wrap">
        {reviews.length === 0 ? <p className="empty">No reviews yet.</p> : (
          <table>
            <thead><tr><th>Reviewer</th><th>Farmer</th><th>Rating</th><th>Comment</th><th>Date</th></tr></thead>
            <tbody>
              {reviews.map((r) => (
                <tr key={r.id}>
                  <td><strong>{r.reviewer?.fullName}</strong></td>
                  <td>{r.farmer?.fullName}</td>
                  <td><Stars value={r.rating} /></td>
                  <td style={{ maxWidth: 240 }}>{r.comment || '—'}</td>
                  <td className="muted">{new Date(r.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
