<script setup lang="js">
import { ref, onMounted, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElInput, ElAvatar } from 'element-plus'
import Footer from "@/components/footer.vue";

import { goodsRagSearchService, goodsSearchService } from '@/api/goods.js'

const router = useRouter()
const route = useRoute()

// 搜索核心数据
const goodsList = ref([])
const isLoading = ref(false)
const keyword = ref('')
const aiSearchEnabled = ref(false) // AI 智能搜索开关

// 分页（SQL 搜索使用）
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)

// 排序与筛选
const sortType = ref('综合')
const sortOrder = ref('asc')
const priceRange = ref({ min: '', max: '' })

// 计算属性：前端筛选 + 排序
const filteredGoodsList = computed(() => {
  let list = [...goodsList.value]
  // 价格区间筛选
  if (priceRange.value.min !== '') {
    const min = Number(priceRange.value.min)
    if (!isNaN(min)) list = list.filter(g => Number(g.sellPrice) >= min)
  }
  if (priceRange.value.max !== '') {
    const max = Number(priceRange.value.max)
    if (!isNaN(max)) list = list.filter(g => Number(g.sellPrice) <= max)
  }
  // 排序
  switch (sortType.value) {
    case '新发布':
      list.sort((a, b) => {
        if (a.createTime && b.createTime) return new Date(b.createTime) - new Date(a.createTime)
        return b.id - a.id
      })
      break
    case '价格':
      if (sortOrder.value === 'asc') list.sort((a, b) => Number(a.sellPrice) - Number(b.sellPrice))
      else list.sort((a, b) => Number(b.sellPrice) - Number(a.sellPrice))
      break
  }
  return list
})

// ============ SQL 搜索（主路径，零外部依赖） ============
const fetchGoodsBySQL = async () => {
  if (!keyword.value) return
  isLoading.value = true
  try {
    const response = await goodsSearchService({
      keyword: keyword.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      sortField: sortType.value === '价格' ? 'price' : 'time',
      sortOrder: sortType.value === '价格' ? sortOrder.value : 'desc',
      minPrice: priceRange.value.min || null,
      maxPrice: priceRange.value.max || null
    })
    if (response.code === 0 && response.data) {
      goodsList.value = response.data.items || []
      total.value = response.data.total || 0
      if (goodsList.value.length === 0) ElMessage.info('未找到相关商品')
    } else {
      ElMessage.error(response.message || '搜索失败')
    }
  } catch (error) {
    console.error('SQL搜索失败:', error)
    ElMessage.error('网络异常，搜索失败')
  } finally {
    isLoading.value = false
  }
}

// ============ AI 智能搜索（RAG 向量语义） ============
const fetchGoodsByRAG = async () => {
  if (!keyword.value) return
  isLoading.value = true
  try {
    const response = await goodsRagSearchService({ query: keyword.value })
    if (response.code === 0 && response.data?.goodsList) {
      goodsList.value = response.data.goodsList
      total.value = goodsList.value.length
      if (goodsList.value.length === 0) ElMessage.info('未找到相关商品')
    } else {
      ElMessage.error(response.message || '搜索商品失败')
    }
  } catch (error) {
    console.error('AI搜索失败:', error)
    ElMessage.error('AI搜索暂时不可用，已自动降级')
    // 降级到 SQL 搜索
    aiSearchEnabled.value = false
    await fetchGoodsBySQL()
  } finally {
    isLoading.value = false
  }
}

// ============ 统一搜索入口 ============
const doSearch = async () => {
  pageNum.value = 1
  goodsList.value = []
  if (aiSearchEnabled.value) {
    await fetchGoodsByRAG()
  } else {
    await fetchGoodsBySQL()
  }
}

// 切换 AI 模式
const toggleAiSearch = async () => {
  aiSearchEnabled.value = !aiSearchEnabled.value
  if (keyword.value) await doSearch()
}

// 切换排序类型
const changeSortType = (type) => {
  if (sortType.value !== type || type !== '价格') {
    sortType.value = type
    sortOrder.value = 'desc'
  } else {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  }
  // SQL 模式下自带排序，重新请求
  if (!aiSearchEnabled.value && keyword.value) {
    pageNum.value = 1
    fetchGoodsBySQL()
  }
}

// 重置价格筛选
const resetPriceFilter = () => {
  priceRange.value = { min: '', max: '' }
  if (!aiSearchEnabled.value && keyword.value) fetchGoodsBySQL()
}

// 分页切换
const handlePageChange = (newPage) => {
  pageNum.value = newPage
  if (!aiSearchEnabled.value && keyword.value) fetchGoodsBySQL()
}

// 初始化
onMounted(async () => {
  keyword.value = route.query.keyword || ''
  aiSearchEnabled.value = route.query.ai === '1'
  if (keyword.value) {
    await doSearch()
  }
})

// 监听路由参数
watch(() => route.query.keyword, (newKw, oldKw) => {
  if (newKw !== oldKw && newKw) {
    keyword.value = newKw
    aiSearchEnabled.value = route.query.ai === '1'
    sortType.value = '综合'
    sortOrder.value = 'asc'
    priceRange.value = { min: '', max: '' }
    doSearch()
  }
}, { immediate: true })

// 跳转到商品详情
const goToDetail = (id) => {
  router.push(`/goods/detail/${id}`)
}

// 格式化价格（保留1位小数）
const formatPrice = (price) => {
  return Number(price).toFixed(1)
}

// 处理商品图片（适配imageList，优先取第一张图）
const getGoodsImage = (goods) => {
  // 如果goodsPic有值则用，否则取imageList的第一张
  if (goods.goodsPic) return goods.goodsPic
  return goods.imageList && goods.imageList.length > 0
      ? goods.imageList[0].imageUrl
      : 'https://placeholder.pics/svg/280x200/EEEEEE/666666/暂无图片' // 占位图
}

// 适配新旧状态显示（原接口isNew=2对应9成新，这里扩展显示）
const getGoodsNewStatus = (isNew) => {
  const statusMap = {
    1: '新品',
    2: '9成新',
    3: '8成新',
    4: '7成新'
  }
  return statusMap[isNew] || '二手'
}
</script>

<template>
  <div class="page-layout no-select">
    <div class="main-content">
      <div class="goods-list-page">
        <!-- 搜索关键词 + AI 开关 -->
        <div class="search-header" v-if="keyword">
          <div class="search-keyword-tip">
            搜索“{{ keyword }}”的结果 (共 {{ total }} 件商品)
            <span v-if="aiSearchEnabled" class="ai-badge">AI 智能</span>
          </div>
          <el-switch
              v-model="aiSearchEnabled"
              class="ai-toggle-btn"
              active-text="AI 智能搜索"
              inactive-text="普通搜索"
              inline-prompt
              @change="toggleAiSearch"
          />
        </div>

        <!-- 排序与价格筛选栏 -->
        <div class="filter-bar" v-if="!isLoading && keyword">
          <div class="sort-options">
            <button class="sort-btn" :class="{ active: sortType === '综合' }" @click="changeSortType('综合')">
              综合 <span class="arrow" v-if="sortType === '综合'">▼</span>
            </button>
            <button class="sort-btn" :class="{ active: sortType === '新发布' }" @click="changeSortType('新发布')">
              新发布 <span class="arrow" v-if="sortType === '新发布'">▼</span>
            </button>
            <button class="sort-btn" :class="{ active: sortType === '价格' }" @click="changeSortType('价格')">
              价格 <span class="arrow" v-if="sortType === '价格'">{{ sortOrder === 'asc' ? '↑' : '↓' }}</span>
            </button>
          </div>
          <div class="price-filter">
            <span>¥</span>
            <el-input v-model.number="priceRange.min" placeholder="最低" size="small" style="width: 80px" type="number" min="0" />
            <span>-</span>
            <el-input v-model.number="priceRange.max" placeholder="最高" size="small" style="width: 80px" type="number" min="0" />
            <el-button size="small" type="text" @click="resetPriceFilter">重置</el-button>
          </div>
        </div>

        <div class="goods-container">
          <!-- 骨架屏 -->
          <div class="goods-grid skeleton-grid" v-show="isLoading">
            <div class="goods-card skeleton-card" v-for="i in 8" :key="i">
              <div class="goods-image-wrapper skeleton-image"></div>
              <div class="goods-info">
                <h3 class="goods-name skeleton-name"></h3>
                <div class="goods-price skeleton-price"></div>
                <div class="goods-meta">
                  <span class="stock skeleton-meta-item"></span>
                  <span class="like-count skeleton-meta-item"></span>
                </div>
                <div class="seller-info">
                  <div class="skeleton-avatar"></div>
                  <span class="seller-name skeleton-seller-name"></span>
                </div>
              </div>
            </div>
          </div>

          <!-- 商品列表 -->
          <div class="goods-grid" v-show="!isLoading">
            <div class="empty-tip" v-if="filteredGoodsList.length === 0 && !isLoading">
              未找到相关商品，请尝试更换关键词或调整筛选条件
            </div>
            <div class="goods-card" v-for="goods in filteredGoodsList" :key="goods.id" @click="goToDetail(goods.id)">
              <div class="goods-image-wrapper">
                <img :src="getGoodsImage(goods)" :alt="goods.goodsName" class="goods-image" />
                <div class="goods-tag" v-if="goods.isNew">{{ getGoodsNewStatus(goods.isNew) }}</div>
              </div>
              <div class="goods-info">
                <h3 class="goods-name">{{ goods.goodsName }}</h3>
                <div class="goods-price">¥{{ formatPrice(goods.sellPrice) }}</div>
                <div class="goods-meta">
                  <span class="stock">库存{{ goods.stock }}</span>
                  <span class="like-count">{{ goods.collectCount }}人想要</span>
                </div>
                <div class="seller-info">
                  <el-avatar :size="24" :src="goods.sellerPic || ''" />
                  <span class="seller-name">{{ goods.sellerNickname || '匿名卖家' }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 分页（仅 SQL 搜索模式） -->
          <div class="pagination-wrapper" v-if="!aiSearchEnabled && total > pageSize">
            <el-pagination
                background
                layout="prev, pager, next"
                :total="total"
                :page-size="pageSize"
                :current-page="pageNum"
                @current-change="handlePageChange"
            />
          </div>
        </div>
      </div>
    </div>
    <div class="footer-wrapper">
      <Footer />
    </div>
  </div>
</template>

<style lang="scss" scoped>
/* ================= 新增布局控制 ================= */
.page-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh; /* 页面最小高度为100%视口高度 */
}

.main-content {
  flex: 1; /* 无数据时自动拉伸，将底部区域挤到下方 */
  display: flex;
  flex-direction: column;
}

.footer-wrapper {
  width: 100%;
  flex-shrink: 0; /* 确保底部不会被挤压变形 */
}

/* ================= 新增筛选栏样式 ================= */
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  flex-wrap: wrap;
  gap: 16px;
}

.sort-options {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.sort-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 20px;
  background: #f5f7fa;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: #ecf5ff;
    border-color: #409eff;
    color: #409eff;
  }

  &.active {
    background: #409eff;
    border-color: #409eff;
    color: #fff;
  }

  .arrow {
    font-size: 12px;
    transition: transform 0.2s ease;
  }
}

.price-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;

  span {
    font-weight: 500;
  }
}

/* ================= 搜索头部 + AI 开关 ================= */
.search-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 0 10px;
  flex-wrap: wrap;
  gap: 12px;
}

.search-keyword-tip {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-badge {
  display: inline-block;
  font-size: 12px;
  font-weight: 500;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  padding: 2px 10px;
  border-radius: 12px;
}

.ai-toggle-btn {
  --el-switch-on-color: #667eea;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding-bottom: 20px;
}

.empty-tip {
  text-align: center;
  padding: 60px 0;
  color: #999;
  font-size: 16px;
}

/* ================= 自定义骨架屏样式 ================= */
.skeleton-grid {
  .skeleton-card {
    cursor: default;
    &:hover {
      transform: none;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    .skeleton-image {
      width: 100%;
      height: 200px;
      background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
      background-size: 200% 100%;
      animation: skeleton-loading 1.5s infinite;
      border-radius: 12px 12px 0 0;
    }

    .skeleton-name {
      width: 80%;
      height: 20px;
      background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
      background-size: 200% 100%;
      animation: skeleton-loading 1.5s infinite;
      border-radius: 4px;
      margin-bottom: 8px;
    }

    .skeleton-price {
      width: 60%;
      height: 24px;
      background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
      background-size: 200% 100%;
      animation: skeleton-loading 1.5s infinite;
      border-radius: 4px;
      margin-bottom: 8px;
    }

    .skeleton-meta-item {
      width: 70px;
      height: 18px;
      background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
      background-size: 200% 100%;
      animation: skeleton-loading 1.5s infinite;
      border-radius: 9px;
      display: inline-block;
    }

    .skeleton-avatar {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
      background-size: 200% 100%;
      animation: skeleton-loading 1.5s infinite;
    }

    .skeleton-seller-name {
      width: 80px;
      height: 16px;
      background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
      background-size: 200% 100%;
      animation: skeleton-loading 1.5s infinite;
      border-radius: 4px;
      display: inline-block;
    }
  }
}

/* 骨架屏动画 */
@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* ================================================ */

/* 原有样式 */
.goods-list-page {
  max-width: 1200px;
  width: 100%;
  margin: 30px auto;
  padding-top: 70px;
}

.filter-section {
  margin-bottom: 24px;

  .filter-title {
    font-size: 30px;
    font-weight: bold;
    margin-bottom: 16px;
  }

  .filter-item {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;

    .filter-label {
      font-size: 14px;
      color: #666;
    }

    .category-btn-group {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }

    .category-btn {
      border-radius: 20px !important;
      padding: 4px 16px !important;
      transition: all 0.2s ease !important;

      &.el-button--primary {
        background-color: #409eff !important;
        border-color: #409eff !important;
        color: #fff !important;

        &:hover {
          background-color: #66b1ff !important;
          border-color: #66b1ff !important;
        }
      }

      &.el-button--default {
        background-color: #f5f7fa !important;
        border-color: #e4e7ed !important;
        color: #606266 !important;

        &:hover {
          background-color: #e4e7ed !important;
          border-color: #dcdfe6 !important;
          color: #303133 !important;
        }
      }
    }
  }
}

.goods-container {
  .goods-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;

    .goods-card {
      background: #fff;
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      transition: all 0.3s ease;
      cursor: pointer;

      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 8px 16px rgba(0, 0, 0, 0.12);
      }

      .goods-image-wrapper {
        position: relative;
        width: 100%;
        height: 200px;
        overflow: hidden;

        .goods-image {
          width: 100%;
          height: 100%;
          object-fit: cover;
          transition: transform 0.3s ease;
        }

        &:hover .goods-image {
          transform: scale(1.05);
        }

        .goods-tag {
          position: absolute;
          top: 8px;
          left: 8px;
          background: #ff4d4f;
          color: #fff;
          font-size: 12px;
          padding: 2px 6px;
          border-radius: 4px;
        }
      }

      .goods-info {
        padding: 12px;

        .goods-name {
          font-size: 16px;
          font-weight: 600;
          margin-bottom: 8px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .goods-price {
          font-size: 18px;
          color: #ff4d4f;
          font-weight: bold;
          margin-bottom: 8px;
        }

        .goods-meta {
          display: flex;
          justify-content: space-between;
          font-size: 12px;
          color: #999;
          margin-bottom: 12px;

          .stock, .like-count {
            background: #f5f5f5;
            padding: 2px 6px;
            border-radius: 4px;
          }
        }

        .seller-info {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 12px;
          color: #666;

          .seller-name {
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }
      }
    }
  }
}

/* 响应式适配 */
@media (max-width: 768px) {

  .page-layout {
    padding-top: 30px;
  }
  .goods-list-page {
    padding: 12px;
  }

  /* 筛选栏响应式 */
  .filter-bar {
    flex-direction: column;
    align-items: flex-start;
    padding: 12px;
  }

  .sort-options {
    width: 100%;
    justify-content: space-between;
  }

  .sort-btn {
    padding: 4px 8px;
    font-size: 12px;
  }

  .price-filter {
    width: 100%;
    justify-content: flex-start;
  }

  .filter-section .filter-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;

    .category-btn-group {
      width: 100%;
    }

    .category-btn {
      padding: 2px 12px !important;
      font-size: 12px !important;
    }
  }

  .goods-container .goods-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 12px;

    .goods-card {
      .goods-image-wrapper {
        height: 140px;
      }

      .goods-info {
        padding: 8px;

        .goods-name {
          font-size: 14px;
        }

        .goods-price {
          font-size: 16px;
        }
      }

      /* 响应式骨架屏调整 */
      .skeleton-image {
        height: 140px !important;
      }
    }
  }
}
</style>