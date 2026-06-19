import { createApp } from 'vue'
import { createPinia } from 'pinia'
import {
  create,
  NButton,
  NInput,
  NInputNumber,
  NSelect,
  NForm,
  NFormItem,
  NGrid,
  NGi,
  NDataTable,
  NTag,
  NProgress,
  NMenu,
  NLayout,
  NLayoutHeader,
  NLayoutSider,
  NConfigProvider,
  NMessageProvider,
  NNotificationProvider,
  NLoadingBarProvider,
  NDialogProvider,
} from 'naive-ui'
import App from './App.vue'
import router from './router'

const naive = create({
  components: [
    NButton,
    NInput,
    NInputNumber,
    NSelect,
    NForm,
    NFormItem,
    NGrid,
    NGi,
    NDataTable,
    NTag,
    NProgress,
    NMenu,
    NLayout,
    NLayoutHeader,
    NLayoutSider,
    NConfigProvider,
    NMessageProvider,
    NNotificationProvider,
    NLoadingBarProvider,
    NDialogProvider,
  ],
})

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(naive)

app.mount('#app')
