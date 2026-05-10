import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

export function useViewport() {
  const width = ref(typeof window === 'undefined' ? 1280 : window.innerWidth)

  const syncWidth = () => {
    width.value = window.innerWidth
  }

  onMounted(() => {
    syncWidth()
    window.addEventListener('resize', syncWidth, { passive: true })
  })

  onBeforeUnmount(() => {
    window.removeEventListener('resize', syncWidth)
  })

  return {
    width,
    isTabletOrBelow: computed(() => width.value <= 980),
    isPhone: computed(() => width.value <= 768)
  }
}
