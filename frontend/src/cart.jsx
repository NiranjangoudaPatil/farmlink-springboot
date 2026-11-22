import { createContext, useContext, useEffect, useState } from 'react'

const CartContext = createContext(null)
export const useCart = () => useContext(CartContext)

const KEY = 'farmlink_cart'

// The cart lives in the browser only. It becomes real when the retailer places the order.
export function CartProvider({ children }) {
  const [items, setItems] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem(KEY)) || []
    } catch {
      return []
    }
  })

  useEffect(() => {
    localStorage.setItem(KEY, JSON.stringify(items))
  }, [items])

  const add = (product, quantity = 1) =>
    setItems((cur) => {
      const found = cur.find((i) => i.product.id === product.id)
      if (found) return cur.map((i) => (i === found ? { ...i, quantity: i.quantity + quantity } : i))
      return [...cur, { product, quantity }]
    })

  const setQuantity = (id, quantity) =>
    setItems((cur) => cur.map((i) => (i.product.id === id ? { ...i, quantity } : i)))

  const remove = (id) => setItems((cur) => cur.filter((i) => i.product.id !== id))
  const clear = () => setItems([])
  const total = items.reduce((s, i) => s + i.quantity * Number(i.product.pricePerUnit), 0)

  return (
    <CartContext.Provider value={{ items, add, setQuantity, remove, clear, total, count: items.length }}>
      {children}
    </CartContext.Provider>
  )
}
