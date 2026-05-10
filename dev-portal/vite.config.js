import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolveApiBaseUrl } from './src/config/apiBaseUrl.js'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  if (mode === 'production') {
    resolveApiBaseUrl(env)
  }

  return {
    plugins: [vue()],
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes('node_modules')) return

            if (id.includes('@element-plus/icons-vue')) {
              return 'vendor-element-icons'
            }

            if (id.includes('element-plus')) {
              if (id.includes('/es/components/table') || id.includes('/es/components/table-v2')) {
                return 'vendor-ep-table'
              }

              if (
                id.includes('/es/components/form') ||
                id.includes('/es/components/input') ||
                id.includes('/es/components/select') ||
                id.includes('/es/components/upload') ||
                id.includes('/es/components/option') ||
                id.includes('/es/components/radio') ||
                id.includes('/es/components/checkbox')
              ) {
                return 'vendor-ep-form'
              }

              if (
                id.includes('/es/components/dialog') ||
                id.includes('/es/components/drawer') ||
                id.includes('/es/components/message') ||
                id.includes('/es/components/message-box') ||
                id.includes('/es/components/notification') ||
                id.includes('/es/components/popover') ||
                id.includes('/es/components/tooltip') ||
                id.includes('/es/components/dropdown')
              ) {
                return 'vendor-ep-overlay'
              }

              if (
                id.includes('/es/components/menu') ||
                id.includes('/es/components/sub-menu') ||
                id.includes('/es/components/tabs') ||
                id.includes('/es/components/breadcrumb')
              ) {
                return 'vendor-ep-navigation'
              }

              if (
                id.includes('/es/components/card') ||
                id.includes('/es/components/row') ||
                id.includes('/es/components/col') ||
                id.includes('/es/components/space') ||
                id.includes('/es/components/divider') ||
                id.includes('/es/components/container') ||
                id.includes('/es/components/header') ||
                id.includes('/es/components/main') ||
                id.includes('/es/components/footer')
              ) {
                return 'vendor-ep-layout'
              }

              if (
                id.includes('/es/components/descriptions') ||
                id.includes('/es/components/progress') ||
                id.includes('/es/components/tag') ||
                id.includes('/es/components/alert') ||
                id.includes('/es/components/skeleton') ||
                id.includes('/es/components/empty')
              ) {
                return 'vendor-ep-feedback'
              }

              return 'vendor-element-plus'
            }

            if (id.includes('vue-router')) {
              return 'vendor-vue-router'
            }

            if (id.includes('/vue/') || id.includes('/@vue/') || id.includes('pinia')) {
              return 'vendor-vue'
            }

            if (id.includes('axios')) {
              return 'vendor-network'
            }

            return 'vendor-misc'
          }
        }
      }
    }
  }
})
