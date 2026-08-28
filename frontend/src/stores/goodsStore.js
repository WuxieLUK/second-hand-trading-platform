import { defineStore } from 'pinia'
import { goodsOpenListService } from '@/api/goods.js'
import { shopCategoryOpenListService } from '@/api/shopcategory.js'

export const useGoodsStore = defineStore('goods', {
    state: () => ({
        goodsList: [],
        categories: [],
        total: 0,
        pageNum: 1,
        pageSize: 10,
        lastQueryParams: null, // 缓存上次查询参数
        isLoading: false
    }),

    getters: {
        // 是否有缓存数据
        hasCachedData: (state) => {
            return state.goodsList.length > 0 && state.lastQueryParams !== null
        }
    },

    actions: {
        // 获取商品分类列表
        async fetchCategories() {
            if (this.categories.length > 0) return this.categories
            try {
                const res = await shopCategoryOpenListService()
                this.categories = res.data
                return this.categories
            } catch (err) {
                console.error('获取分类失败:', err)
                return []
            }
        },

        // 分页查询商品（带缓存）
        async fetchGoodsList(queryParams, forceRefresh = false) {
            // 如果参数和上次一致，且不强制刷新，则直接返回缓存
            if (!forceRefresh && this.hasCachedData && JSON.stringify(queryParams) === JSON.stringify(this.lastQueryParams)) {
                return {
                    items: this.goodsList,
                    total: this.total
                }
            }

            this.isLoading = true
            try {
                const res = await goodsOpenListService(queryParams)
                this.goodsList = res.data.items
                this.total = res.data.total
                this.pageNum = queryParams.pageNum || 1
                this.pageSize = queryParams.pageSize || 10
                this.lastQueryParams = { ...queryParams } // 缓存查询参数
                return res.data
            } catch (err) {
                console.error('查询商品失败:', err)
                return { items: [], total: 0 }
            } finally {
                this.isLoading = false
            }
        },

        // 清空缓存
        clearCache() {
            this.goodsList = []
            this.total = 0
            this.lastQueryParams = null
        }
    }
})