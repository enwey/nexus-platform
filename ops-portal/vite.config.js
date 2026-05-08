import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolveApiBaseUrl } from './src/config/apiBaseUrl.js'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  if (mode === 'production') {
    resolveApiBaseUrl(env)
  }

  return {
    plugins: [vue()]
  }
})
