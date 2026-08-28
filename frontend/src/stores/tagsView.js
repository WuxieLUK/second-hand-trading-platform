import { defineStore } from 'pinia'

export const useTagsViewStore = defineStore('tagsView', {
    state: () => ({
        tagsList: [] // 存放所有tag标签的数组
    }),
    actions: {
        // 删除一个tag
        delTagsItem(index) {
            this.tagsList.splice(index, 1)
        },
        // 添加一个tag
        setTagsItem(data) {
            // 避免重复添加
            const isExist = this.tagsList.some(tag => tag.path === data.path)
            if (!isExist) {
                this.tagsList.push(data)
            }
        },
        // 清除其他tag
        clearTagsOther(keepTags) {
            this.tagsList = keepTags
        },
        // 清除所有tag
        clearTags() {
            this.tagsList = []
        }
    }
})