import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'
import { resolve } from 'path'

export default defineConfig({
  // 本地开发保持根路径 base='/'；仅门户部署构建时用 VITE_BASE=/mss/ 注入
  base: process.env.VITE_BASE || '/',
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: {
      '@': resolve(import.meta.dirname, 'src'),
    },
  },
  server: {
    port: 3100,
    host: true,
    proxy: {
      // 本地默认接口前缀 /mss；门户部署时前端走 /mss-api，再改写到 context-path /mss
      '/mss': {
        target: 'http://localhost:8090',
        changeOrigin: true,
      },
      '/mss-api': {
        target: 'http://localhost:8090',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/mss-api/, '/mss'),
      },
    },
  },
})
