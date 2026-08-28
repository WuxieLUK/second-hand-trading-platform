import './assets/main.scss'

import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import {createPinia} from 'pinia'
import {createPersistedState} from 'pinia-plugin-persistedstate'
import locale from "element-plus/dist/locale/zh-cn.js"
import VueLazyload from 'vue-lazyload';
import loadingImage from '@/assets/【哲风壁纸】我妻善逸-鬼灭之刃.png'
import './assets/iconfont/iconfont.js';
const app=createApp(App)
const persist = createPersistedState()
const pinia=createPinia()
pinia.use(persist)
app.use(pinia)
app.use(VueLazyload,  {
    loading: loadingImage, // 加载中的占位图
    error: loadingImage,    // 加载失败的占位图
    attempt: 1
})
app.use(router)
app.use(ElementPlus, {locale})
app.mount('#app')

