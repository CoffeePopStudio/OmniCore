import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import { fileURLToPath, URL } from 'node:url'
import { readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'

const __dirname = fileURLToPath(new URL('.', import.meta.url))

export default defineConfig({
  base: './',
  plugins: [
    vue(),
    AutoImport({
      imports: [
        'vue',
        'vue-router',
        'pinia',
      ],
      dts: true,
      include: [
        /\.[tj]sx?$/,
        /\.vue$/,
        /\.vue\?vue/,
      ],
      exclude: [
        /node_modules/,
        /\.git/,
      ],
    }),
    {
      name: 'web-version',
      closeBundle() {
        const pkgPath = resolve(__dirname, 'package.json')
        const pkg = JSON.parse(readFileSync(pkgPath, 'utf-8'))
        const version = {
          version: pkg.version,
          buildTime: new Date().toISOString(),
        }
        const outPath = resolve(__dirname, '../src/main/resources/web/web-version.json')
        writeFileSync(outPath, JSON.stringify(version, null, 2))
      },
    },
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    outDir: '../src/main/resources/web',
    emptyOutDir: true,
    rollupOptions: {
      output: {
        manualChunks(id: string) {
          if (id.includes('naive-ui')) return 'naive-ui'
          if (id.includes('node_modules/vue')) return 'vue-vendor'
        },
      },
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:9812',
        changeOrigin: true,
      },
    },
  },
})
