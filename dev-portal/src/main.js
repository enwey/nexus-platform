import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import { getPortalApiConfigHelpText, getRuntimeApiBaseUrl } from './config/apiBaseUrl'

function renderConfigError(error) {
  const root = document.querySelector('#app')
  if (!root) return
  const helpLines = getPortalApiConfigHelpText()
  root.innerHTML = `
    <section style="min-height:100vh;display:flex;align-items:center;justify-content:center;background:#f6f8fc;padding:24px;">
      <article style="max-width:720px;background:#fff;border-radius:20px;padding:32px;box-shadow:0 18px 48px rgba(15,23,42,.12);font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;">
        <h1 style="margin:0 0 12px;font-size:28px;color:#111827;">开发者后台配置缺失</h1>
        <p style="margin:0 0 16px;color:#374151;line-height:1.7;">${error.message}</p>
        <ul style="margin:0;padding-left:20px;color:#4b5563;line-height:1.8;">
          ${helpLines.map((item) => `<li>${item}</li>`).join('')}
        </ul>
      </article>
    </section>
  `
}

async function bootstrap() {
  try {
    getRuntimeApiBaseUrl()
  } catch (error) {
    renderConfigError(error)
    return
  }

  const [{ default: router }] = await Promise.all([import('./router')])
  const app = createApp(App)

  app.use(createPinia())
  app.use(router)
  app.use(ElementPlus)
  app.mount('#app')
}

bootstrap()
