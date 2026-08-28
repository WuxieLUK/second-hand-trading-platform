import { defineStore } from 'pinia'
import { ref } from 'vue'
const categoryManageStore = defineStore('categoryInfo', () => {
    //定义状态相关的内容
    const info = ref([])

    const setInfo = (newArticle) => {
        info.value = newArticle
    }

    const removeInfo = () => {
        info.value = []
    }

    return { info, setInfo, removeInfo }
}, { persist: true })

export default categoryManageStore;