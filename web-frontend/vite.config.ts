import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'

export default defineConfig({
  base: './',
  plugins: [
    vue(),
    AutoImport({
      imports: [
        'vue',
        'vue-router',
        'pinia'
      ]
    })
  ],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:9812',
        changeOrigin: true
      }
    }
  }
})
