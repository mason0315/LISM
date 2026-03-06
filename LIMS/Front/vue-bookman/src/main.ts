
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import 'element-plus/dist/index.css'
import ElementPlus from 'element-plus'
import LogoAndSearch from '@/components/LogoAndSearch.vue'
import './assets/styles/global.css'

// 导入Font Awesome
import { library } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
// 注册Font Awesome全局组件
app.component('FontAwesomeIcon', FontAwesomeIcon)
app.mount('#app')
export default {
  components: {
    LogoAndSearch
  }
}

