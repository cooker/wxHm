import path from 'node:path'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

/** 与配置文件同目录解析，保证从任意 cwd 执行 build 都写入 Java 工程 static */
const __dirname = path.dirname(fileURLToPath(import.meta.url))
const javaStaticApp = path.resolve(__dirname, '../wxhm-java/src/main/resources/static/app')

export default defineConfig({
  plugins: [vue()],
  base: '/app/',
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://127.0.0.1:8092', changeOrigin: true },
      '/uploads': { target: 'http://127.0.0.1:8092', changeOrigin: true },
      '/upload': { target: 'http://127.0.0.1:8092', changeOrigin: true },
    },
  },
  build: {
    outDir: javaStaticApp,
    emptyOutDir: true,
  },
})
