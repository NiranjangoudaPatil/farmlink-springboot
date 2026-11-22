# FarmLink – React frontend

React 18 + Vite + React Router, styled to match the original FarmLink app (cream/green theme, Playfair Display + DM Sans).
Talks to the FarmLink Spring Boot API in `../farmlink-springboot`.

## Run
1. Start the backend (port 8081 in this setup).
2. `npm install` (first time only), then `npm run dev`
3. Open http://localhost:5173

Vite proxies `/api` and `/uploads` to the backend (see `vite.config.js`). Backend on another port?
`$env:BACKEND_URL="http://localhost:8080"; npm run dev`

## Admin login
Admins can't sign up from the UI. The backend creates one on startup from environment variables:

```powershell
$env:ADMIN_EMAIL = "admin@farmlink.com"
$env:ADMIN_PASSWORD = "choose-a-strong-one"
```
Start the backend with those set, then log in at `/auth` – you land on `/admin`.

## Pages (same URLs as the original app)
| URL | Who | What |
|---|---|---|
| `/` | everyone | landing page (logged-in users are redirected to their home) |
| `/auth` | everyone | sign in / sign up tabs (`?role=farmer` or `retailer`) |
| `/marketplace` | everyone | search, categories, ratings, cart sidebar, delivery distance + shelf-life warnings |
| `/retailer/orders` | retailer | track orders, cancel, pay, rate farmer |
| `/farmer/dashboard` | farmer | stats, add/edit/delete products (with image), top products, reviews |
| `/farmer/incoming-orders` | farmer | confirm → ship → deliver, or reject |
| `/admin` | admin | Analytics (charts), Users (verify farmers), Products (enable/disable), Orders, Reviews |

## Structure
- `src/api.js` – fetch wrapper (adds the JWT, unwraps `{status,data,message}`)
- `src/auth.jsx`, `src/cart.jsx`, `src/toast.jsx` – login state, shopping cart, toast messages
- `src/lib.js` – distance/shelf-life maths and the Razorpay checkout helper
- `src/pages/` – one file per page; `src/pages/admin/` – the five admin tabs

## Differences from the original
- No map widgets (Leaflet). The delivery location uses the browser's "Use my location" instead.
- Notifications are polled every 30 s rather than pushed in real time.
