import { defineStore } from 'pinia'
import { articleCateListService, articleListService } from '@/api/article.js'

export const useArticleStore = defineStore('articlePage', {
    state: () => ({
        // 文章分类数据
        categorys: [],
        // 搜索条件
        categoryId: '',
        state: '',
        // 文章列表数据
        articles: [],
        // 分页参数
        pageNum: 1,
        pageSize: 3,
        total: 20,
        // 选中的文章ID（批量删除用）
        selectedArticleIds: []
    }),
    actions: {
        // 重置搜索条件
        resetSearchCondition() {
            this.categoryId = ''
            this.state = ''
            this.pageNum = 1 // 重置搜索时回到第一页
        },
        // 设置选中的文章ID
        setSelectedArticleIds(ids) {
            this.selectedArticleIds = ids
        },
        // 获取文章分类列表
        async fetchArticleCategoryList() {
            try {
                const result = await articleCateListService()
                this.categorys = result.data
            } catch (error) {
                console.error('获取文章分类失败：', error)
            }
        },
        // 获取文章列表（核心方法）
        async fetchArticleList() {
            try {
                const params = {
                    pageNum: this.pageNum,
                    pageSize: this.pageSize,
                    categoryId: this.categoryId ? this.categoryId : null,
                    state: this.state ? this.state : null
                }
                const result = await articleListService(params)
                this.articles = result.data.items
                this.total = result.data.total

                // 映射分类名称
                this.articles.forEach(article => {
                    const category = this.categorys.find(c => c.id === article.categoryId)
                    article.categoryName = category ? category.categoryName : ''
                })

                // 处理分页边界：当前页无数据且页码>1时，回退到上一页并重新请求
                if (this.articles.length === 0 && this.pageNum > 1) {
                    this.pageNum--
                    await this.fetchArticleList();
                }
            } catch (error) {
                console.error('获取文章列表失败：', error)
                throw error // 抛出错误，让组件捕获处理
            }
        },
        // 分页参数更新
        updatePageNum(num) {
            this.pageNum = num
        },
        updatePageSize(size) {
            this.pageSize = size
        }
    }
})