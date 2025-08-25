import { defineConfig } from 'vite'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  base: process.env.PAGES_BASE_PATH || '/',
  plugins: [
    tailwindcss(),
  ],
})