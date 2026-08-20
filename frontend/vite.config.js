import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiProxyTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:8080'

  return {
    plugins: [vue()],
    server: {
      host: env.VITE_DEV_HOST || '0.0.0.0',
      port: Number(env.VITE_DEV_PORT || 3000),
      proxy: {
        '/api': {
          target: apiProxyTarget,
          changeOrigin: true
        },
        '/uploads': {
          target: apiProxyTarget,
          changeOrigin: true
        }
      }
    }
  }
})
