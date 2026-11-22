import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The backend runs on 8082 on your machine. Change BACKEND_URL if you run it on another port.
const BACKEND = process.env.BACKEND_URL || 'http://localhost:8082'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // The browser talks to Vite (same origin), Vite forwards these paths to Spring Boot. No CORS needed.
    proxy: {
      '/api': BACKEND,
      '/uploads': BACKEND,
    },
  },
})
