import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { darkTheme } from 'naive-ui'
import type { BuiltInGlobalTheme } from 'naive-ui/es/themes/interface'

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(localStorage.getItem('theme') !== 'light')

  const naiveTheme = computed<BuiltInGlobalTheme | null>(() => isDark.value ? darkTheme : null)

  function toggle() {
    isDark.value = !isDark.value
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
    applyTheme()
  }

  function applyTheme() {
    document.documentElement.setAttribute('data-theme', isDark.value ? 'dark' : 'light')
  }

  function init() {
    applyTheme()
  }

  return { isDark, naiveTheme, toggle, init }
})
