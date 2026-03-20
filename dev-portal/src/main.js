import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router'
import App from './App.vue'

/**
 * 创建Vue应用实例
 */
const app = createApp(App)

/**
 * 配置并使用Pinia状态管理
 */
app.use(createPinia())

/**
 * 配置并使用Vue Router
 */
app.use(router)

/**
 * 配置并使用Element Plus UI组件库
 */
app.use(ElementPlus)

/**
 * 挂载应用到DOM
 */
app.mount('#app')
