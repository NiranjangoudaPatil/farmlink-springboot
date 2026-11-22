import { api } from './api'

// Same formulas as the backend's DeliveryService, so warnings show instantly without a request.
export function distanceKm(lat1, lng1, lat2, lng2) {
  const R = 6371
  const rad = (d) => (d * Math.PI) / 180
  const dLat = rad(lat2 - lat1)
  const dLng = rad(lng2 - lng1)
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(dLng / 2) ** 2
  return 2 * R * Math.asin(Math.sqrt(a))
}
export const estimateHours = (km) => Math.ceil(km / 40) + 2

export const cap = (s) => (s ? s.charAt(0) + s.slice(1).toLowerCase() : '')

export const CATEGORY_LABELS = {
  VEGETABLES: '🥬 Vegetables', FRUITS: '🍎 Fruits', GRAINS: '🌾 Grains',
  DAIRY: '🥛 Dairy', HERBS: '🌿 Herbs', OTHER: '📦 Other',
}

export const homeFor = (role) => (role === 'ADMIN' ? '/admin' : role === 'FARMER' ? '/farmer/dashboard' : '/marketplace')

function loadRazorpay() {
  return new Promise((resolve, reject) => {
    if (window.Razorpay) return resolve()
    const s = document.createElement('script')
    s.src = 'https://checkout.razorpay.com/v1/checkout.js'
    s.onload = resolve
    s.onerror = () => reject(new Error('Could not load Razorpay'))
    document.body.appendChild(s)
  })
}

// Opens Razorpay checkout for the given order ids. Resolves true if paid, false if the user closed the window.
export async function payForOrders(orderIds) {
  const p = await api.post('/api/payments/create-order', { orderIds })
  await loadRazorpay()
  return new Promise((resolve, reject) => {
    const rzp = new window.Razorpay({
      key: p.razorpayKeyId,
      amount: p.amount,
      currency: p.currency,
      order_id: p.razorpayOrderId,
      name: 'FarmLink',
      handler: async (r) => {
        try {
          // The backend verifies the signature; never trust the browser's "success" alone.
          await api.post('/api/payments/verify', {
            razorpayOrderId: r.razorpay_order_id,
            razorpayPaymentId: r.razorpay_payment_id,
            razorpaySignature: r.razorpay_signature,
          })
          resolve(true)
        } catch (e) {
          reject(e)
        }
      },
      modal: { ondismiss: () => resolve(false) },
    })
    rzp.open()
  })
}
