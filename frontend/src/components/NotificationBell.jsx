import { useEffect, useRef, useState } from 'react'
import { Bell } from 'lucide-react'
import { api } from '../api'

export default function NotificationBell() {
  const [unread, setUnread] = useState(0)
  const [open, setOpen] = useState(false)
  const [items, setItems] = useState([])
  const box = useRef(null)

  const loadCount = () =>
    api.get('/api/notifications/unread-count').then((d) => setUnread(Object.values(d)[0] ?? 0)).catch(() => {})
  const loadList = () => api.get('/api/notifications?size=15').then((p) => setItems(p.content)).catch(() => {})

  // The backend has no push channel, so poll the unread counter every 30 seconds.
  useEffect(() => {
    loadCount()
    const t = setInterval(loadCount, 30000)
    return () => clearInterval(t)
  }, [])

  // close when clicking elsewhere
  useEffect(() => {
    const close = (e) => box.current && !box.current.contains(e.target) && setOpen(false)
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [])

  const toggle = () => {
    if (!open) loadList()
    setOpen(!open)
  }

  const markRead = async (n) => {
    if (n.read) return
    await api.patch(`/api/notifications/${n.id}/read`).catch(() => {})
    loadList()
    loadCount()
  }

  const markAll = async () => {
    await api.post('/api/notifications/read-all').catch(() => {})
    loadList()
    loadCount()
  }

  return (
    <div className="bell" ref={box}>
      <button className="btn btn-ghost btn-icon" onClick={toggle} title="Notifications">
        <Bell size={18} />
        {unread > 0 && <span className="bell-count">{unread > 9 ? '9+' : unread}</span>}
      </button>
      {open && (
        <div className="dropdown">
          <div className="dropdown-head">
            <span>Notifications</span>
            <button className="btn btn-ghost btn-sm" onClick={markAll}>Mark all read</button>
          </div>
          {items.length === 0 && <p className="empty small">No notifications yet</p>}
          {items.map((n) => (
            <div key={n.id} className={`note ${n.read ? '' : 'unread'}`} onClick={() => markRead(n)}>
              <strong className="small">{n.title}</strong>
              <p>{n.message}</p>
              <span className="muted xs">{new Date(n.createdAt).toLocaleString()}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
