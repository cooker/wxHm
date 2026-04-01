import { createApp } from 'vue'
import NutUI from '@nutui/nutui'
import '@nutui/nutui/dist/style.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(NutUI)
app.use(router)
app.mount('#app')
