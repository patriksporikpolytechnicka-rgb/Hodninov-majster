import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    // Backend CORS allows http://localhost:4200 by default.
    port: 4200,
    proxy: { '/api': 'http://127.0.0.1:8080' },
  },
})
