<template>
  <el-config-provider :locale="elementLocale">
    <div class="app-shell"><router-view /></div>
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import zhTw from 'element-plus/es/locale/lang/zh-tw'
import en from 'element-plus/es/locale/lang/en'
import { useI18nLite } from './i18n'

const { currentLocale } = useI18nLite()

const elementLocale = computed(() => {
  if (currentLocale.value === 'zh-TW') return zhTw
  if (currentLocale.value === 'en') return en
  return zhCn
})
</script>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: #f4f6fb;
}

#app, .app-shell {
  min-height: 100vh;
}
</style>
