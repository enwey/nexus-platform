import { computed, ref } from 'vue'

const STORAGE_KEY = 'dev_portal_locale'
const SUPPORTED = ['zh-CN', 'zh-TW', 'en']

function detectLocale() {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved && SUPPORTED.includes(saved)) return saved

  const nav = navigator.language || 'zh-CN'
  if (nav.toLowerCase().includes('tw') || nav.toLowerCase().includes('hk')) return 'zh-TW'
  if (nav.toLowerCase().startsWith('en')) return 'en'
  return 'zh-CN'
}

export const locale = ref(detectLocale())

export function setLocale(next) {
  if (!SUPPORTED.includes(next)) return
  locale.value = next
  localStorage.setItem(STORAGE_KEY, next)
}

export function useI18nLite() {
  const currentLocale = computed(() => locale.value)
  const lt = (zhCN, zhTW, en) => {
    if (currentLocale.value === 'zh-TW') return zhTW
    if (currentLocale.value === 'en') return en
    return zhCN
  }
  return { currentLocale, lt, setLocale }
}

export function ltGlobal(zhCN, zhTW, en) {
  if (locale.value === 'zh-TW') return zhTW
  if (locale.value === 'en') return en
  return zhCN
}
