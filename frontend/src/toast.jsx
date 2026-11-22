import { createContext, useCallback, useContext, useState } from 'react'

const ToastContext = createContext(null)

// const toast = useToast();  toast.success('Saved'); toast.error('Oops'); toast.info('FYI')
export const useToast = () => useContext(ToastContext)

export function ToastProvider({ children }) {
  const [items, setItems] = useState([])

  const push = useCallback((type, message) => {
    const id = Math.random()
    setItems((cur) => [...cur, { id, type, message }])
    setTimeout(() => setItems((cur) => cur.filter((t) => t.id !== id)), 3500)
  }, [])

  const api = {
    success: (m) => push('success', m),
    error: (m) => push('error', m),
    info: (m) => push('info', m),
  }

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toasts">
        {items.map((t) => <div key={t.id} className={`toast ${t.type}`}>{t.message}</div>)}
      </div>
    </ToastContext.Provider>
  )
}
