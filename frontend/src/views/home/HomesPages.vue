<script setup lang="js">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useGoodsStore } from '@/stores/goodsStore.js'
import Footer from "@/components/footer.vue";
// 引入默认图片
import defaultPic from '@/assets/【哲风壁纸】我妻善逸-鬼灭之刃.png'
// 引入推荐商品API
import { goodsRecommendByKeywordService } from '@/api/goods.js'
import useUserInfoStore from '@/stores/userInfo.js'
const router = useRouter()
const goodsStore = useGoodsStore()
const userInfoStore = useUserInfoStore();
// 筛选条件
const queryParams = ref({
  sellerId: userInfoStore.info.id || null,
  pageNum: 1,
  pageSize: 24,
  categoryId: null, // 分类ID
  minPrice: null,   // 最低价格
  maxPrice: null,   // 最高价格
  keyword: ''       // 搜索关键词
})

// 分类列表中显示父id为0的数据
const categories = computed(() => goodsStore.categories.filter(item => item.parentId === 0))
const goodsList = computed(() => goodsStore.goodsList)
const total = computed(() => goodsStore.total)
const isLoading = computed(() => goodsStore.isLoading)
// 新增：当前激活的标签
const activeTab = computed({
  get() {
    // 如果有选中的分类ID，返回对应分类名称，否则返回"猜你喜欢"
    if (queryParams.value.categoryId) {
      const activeCategory = goodsStore.categories.find(item => item.id === queryParams.value.categoryId)
      return activeCategory ? activeCategory.categoryName : '猜你喜欢'
    }
    return '猜你喜欢'
  },
  set(val) {
    // 仅用于模板绑定，实际筛选逻辑在handleCategoryChange中处理
  }
})

// 推荐模块加载状态
const moduleLoading = ref(false)

// 推荐模块数据（改为动态获取）
const moduleData = ref([
  {
    title: '衣橱捡漏',
    subtitle: '时尚美衣低价淘',
    color: 'yellow',
    keyword: '服饰,箱包', // 分类关键词（逗号分隔，匹配多个数据库分类）
    items: []
  },
  {
    title: '手机数码',
    subtitle: '热门装备省心入',
    color: 'blue',
    keyword: '手机,数码,电脑', // 分类关键词
    items: []
  },
  {
    title: '二次元',
    subtitle: '烫门新品随手入',
    color: 'green',
    keyword: '二次元,游戏,动漫', // 分类关键词
    items: []
  },
  {
    title: '省钱卡券',
    subtitle: '吃喝玩乐放心购',
    color: 'pink',
    keyword: '卡券,优惠券', // 分类关键词
    items: []
  }
])

// 获取推荐模块数据
const fetchModuleData = async () => {
  try {
    moduleLoading.value = true
    // 遍历每个推荐模块，获取对应关键词的商品
    for (const module of moduleData.value) {
      const res = await goodsRecommendByKeywordService(module.keyword)
      // 核心修复：正确解析后端返回的GoodsVO数据
      let goodsData = []
      // 后端返回结构：Result<Map> → res.data.data.goodsList
      if (res.data && Array.isArray(res.data.goodsList)) {
        goodsData = res.data.goodsList
      }
      // 转换为前端需要的格式（适配GoodsVO字段）
      module.items = goodsData.map((item, index) => ({
        id: item.id || `module-${module.color}-${index}`,
        // 取第一张图片，适配GoodsVO的imageList字段，无数据时用默认图
        image: item.goodsPic
            ? item.goodsPic
            : defaultPic,
        // 价格取sellPrice，确保是数字类型
        price: item.sellPrice || 0
      }))
    }
  } catch (error) {
    console.error('获取推荐商品失败:', error)
    // 异常时使用默认数据，保证页面正常显示
    moduleData.value.forEach(module => {
      module.items = [
        { id: 1, image: "", price: Math.floor(Math.random() * 100 + 10) },
        { id: 2, image: "", price: Math.floor(Math.random() * 100 + 10) },
        { id: 3, image: "", price: Math.floor(Math.random() * 200 + 50) }
      ]
    })
  } finally {
    moduleLoading.value = false
  }
}

// 初始化加载
onMounted(async () => {
  await goodsStore.fetchCategories()
  await fetchGoods()
  // 加载推荐模块数据
  await fetchModuleData()
})

// 查询商品
const fetchGoods = async (forceRefresh = false) => {
  await goodsStore.fetchGoodsList(queryParams.value, forceRefresh)
}

// 切换分类
const handleCategoryChange = (categoryId) => {
  queryParams.value.categoryId = categoryId
  queryParams.value.pageNum = 1
  fetchGoods()
}

// 页码变化
const onCurrentChange = (pageNum) => {
  queryParams.value.pageNum = pageNum
  fetchGoods()
}

// 跳转到商品详情
const goToDetail = (id) => {
  router.push(`/goods/detail/${id}`)
}

// 格式化价格
const formatPrice = (price) => {
  return Number(price).toFixed(1)
}

// 新增：处理标签栏的分类点击（清空分类筛选）
const handleGuessLikeClick = () => {
  queryParams.value.categoryId = null
  queryParams.value.pageNum = 1
  fetchGoods()
}

// 新增：处理左侧细分类目的点击事件
const handleCategoryItemClick = async (keyword) => {
  await router.push({
    path: '/homes/search',
    query: { keyword: keyword } // URL参数：/homes/search?keyword=xxx
  });
}

// 处理 Banner "去看看" 点击 — 跳转到"猜你喜欢"商品列表
const handleBannerClick = () => {
  queryParams.value.categoryId = null
  queryParams.value.pageNum = 1
  fetchGoods()
  // 平滑滚动到商品区域
  setTimeout(() => {
    const el = document.querySelector('.bottom-module')
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, 100)
}

// 静态左侧分类数据（拆分更细，支持点击）
const staticCategories = ref([
  {
    icon: '📱',
    name: '手机 / 数码 / 电脑',
    subItems: [
      { name: '手机', keyword: '手机' },
      { name: '数码', keyword: '数码' },
      { name: '电脑', keyword: '电脑' }
    ]
  },
  {
    icon: '👕',
    name: '服饰 / 箱包 / 运动',
    subItems: [
      { name: '服饰', keyword: '服饰' },
      { name: '箱包', keyword: '箱包' },
      { name: '运动', keyword: '运动' }
    ]
  },
  {
    icon: '🎮',
    name: '技能 / 卡券 / 潮玩',
    subItems: [
      { name: '技能', keyword: '技能' },
      { name: '卡券', keyword: '卡券' },
      { name: '潮玩', keyword: '潮玩' }
    ]
  },
  {
    icon: '👶',
    name: '母婴 / 美妆 / 个护',
    subItems: [
      { name: '母婴', keyword: '母婴' },
      { name: '美妆', keyword: '美妆' },
      { name: '个护', keyword: '个护' }
    ]
  },
  {
    icon: '🏠',
    name: '家具 / 家电 / 家装',
    subItems: [
      { name: '家具', keyword: '家具' },
      { name: '家电', keyword: '家电' },
      { name: '家装', keyword: '家装' }
    ]
  },
  {
    icon: '💎',
    name: '文玩 / 珠宝 / 礼品',
    subItems: [
      { name: '文玩', keyword: '文玩' },
      { name: '珠宝', keyword: '珠宝' },
      { name: '礼品', keyword: '礼品' }
    ]
  },
  {
    icon: '🍔',
    name: '食品 / 宠物 / 花卉',
    subItems: [
      { name: '食品', keyword: '食品' },
      { name: '宠物', keyword: '宠物' },
      { name: '花卉', keyword: '花卉' }
    ]
  },
  {
    icon: '📚',
    name: '图书 / 游戏 / 音像',
    subItems: [
      { name: '图书', keyword: '图书' },
      { name: '游戏', keyword: '游戏' },
      { name: '音像', keyword: '音像' }
    ]
  },
  {
    icon: '🚗',
    name: '汽车 / 电动车 / 租房',
    subItems: [
      { name: '汽车', keyword: '汽车' },
      { name: '电动车', keyword: '电动车' },
      { name: '租房', keyword: '租房' }
    ]
  },
  {
    icon: '🔧',
    name: '五金 / 设备 / 农牧',
    subItems: [
      { name: '五金', keyword: '五金' },
      { name: '设备', keyword: '设备' },
      { name: '农牧', keyword: '农牧' }
    ]
  }
])
</script>

<template>
  <div class="page-layout">
    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 第一大模块：分类 + 轮播 + 推荐模块 -->
      <div class="top-module">
        <!-- 左侧静态分类菜单（支持细分类目点击） -->
        <aside class="sidebar">
          <ul class="category-menu">
            <li
                v-for="(cat, index) in staticCategories"
                :key="index"
                class="static-category-item"
            >
              <span class="icon">{{ cat.icon }}</span>
              <div class="category-content">
                <div class="sub-items">
                  <span
                      v-for="(subItem, subIndex) in cat.subItems"
                      :key="subIndex"
                      class="main-name"
                      @click.stop="handleCategoryItemClick(subItem.keyword)"
                  >
                    {{ subItem.name }}
                    <span v-if="subIndex !== cat.subItems.length - 1">/</span>
                  </span>
                </div>
              </div>
            </li>
          </ul>
        </aside>

        <!-- 右侧轮播 + 推荐模块 -->
        <div class="top-right-content">
          <!-- 顶部轮播/活动区 -->
          <div class="banner-section">
            <div class="banner-item orange">
              <div class="banner-content">
                <div class="banner-text">
                  <h2>校园抄底好物</h2>
                  <p>超绝性价比 <span class="highlight">1</span> 省到底</p>
                </div>
                <div class="banner-character">
                  <svg viewBox="0 0 120 120" width="110" height="110" xmlns="http://www.w3.org/2000/svg">
                    <!-- 学生人物主体 -->
                    <circle cx="60" cy="35" r="22" fill="rgba(255,255,255,0.2)"/>
                    <path d="M38,62 Q38,56 44,54 L76,54 Q82,56 82,62 L82,94 Q82,98 78,98 L42,98 Q38,98 38,94Z" fill="rgba(255,255,255,0.15)"/>
                    <!-- 书包 -->
                    <rect x="74" y="64" width="18" height="26" rx="4" fill="rgba(255,255,255,0.3)"/>
                    <path d="M83,64 L83,58" stroke="rgba(255,255,255,0.4)" stroke-width="2" stroke-linecap="round"/>
                    <!-- 笑脸 -->
                    <circle cx="52" cy="33" r="3" fill="rgba(255,255,255,0.7)"/>
                    <circle cx="68" cy="33" r="3" fill="rgba(255,255,255,0.7)"/>
                    <path d="M52,42 Q60,50 68,42" stroke="rgba(255,255,255,0.7)" stroke-width="2.5" fill="none" stroke-linecap="round"/>
                    <!-- 价格标签装饰 -->
                    <circle cx="28" cy="48" r="12" fill="rgba(255,255,255,0.18)"/>
                    <text x="28" y="53" text-anchor="middle" fill="rgba(255,255,255,0.85)" font-size="14" font-weight="bold">¥</text>
                  </svg>
                </div>
                <el-button type="primary" size="small" class="banner-button" @click="handleBannerClick">去看看 ></el-button>
              </div>
            </div>
          </div>

          <!-- 推荐模块区 -->
          <div class="module-section">
            <div
                class="module-card"
                :class="module.color"
                v-for="(module, idx) in moduleData"
                :key="idx"
                @click="handleCategoryItemClick(module.title)"
            >
              <div class="module-header" >
                <div class="module-title-wrapper">
                  <h3 class="module-title">{{ module.title }} <span class="arrow">➤</span></h3>
                  <span class="module-subtitle">{{ module.subtitle }}</span>
                </div>
                <div class="module-icon">
                  <span class="icon" v-if="module.color === 'yellow'">👟</span>
                  <span class="icon" v-else-if="module.color === 'blue'">📸</span>
                  <span class="icon" v-else-if="module.color === 'green'">🦆</span>
                  <span class="icon" v-else-if="module.color === 'pink'">👝</span>
                </div>
              </div>
              <!-- 推荐模块加载状态 -->
              <div v-if="moduleLoading" class="module-items skeleton-items">
                <div class="skeleton-item" v-for="i in 3" :key="i">
                  <div class="skeleton-img"></div>
                  <div class="skeleton-price"></div>
                </div>
              </div>
              <!-- 推荐模块商品列表 -->
              <div v-else class="module-items">
                <div class="module-item" v-for="item in module.items" :key="item.id" @click.stop="goToDetail(item.id)">
                  <!-- 核心修改：设置图片默认值，无数据时显示导入的默认图 -->
                  <img
                      :src="item.image || defaultPic"
                      :alt="`商品${item.id}`"
                      class="module-item-img"
                  />
                  <span class="price">¥{{ formatPrice(item.price) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 第二大模块：猜你喜欢 + 商品列表 -->
      <div class="bottom-module">
        <!-- 猜你喜欢标签栏 - 核心修改部分 -->
        <div class="tab-section">
          <!-- 猜你喜欢标签 -->
          <div
              :class="{ active: activeTab === '猜你喜欢' }"
              class="tab-item"
              @click="handleGuessLikeClick"
          >
            猜你喜欢
          </div>
          <!-- 动态渲染分类标签 -->
          <div
              v-for="cat in categories"
              :key="`tab-${cat.id}`"
              :class="{ active: activeTab === cat.categoryName }"
              class="tab-item"
              @click="handleCategoryChange(cat.id)"
          >
            {{ cat.categoryName }}
          </div>
        </div>

        <!-- 商品列表 -->
        <div class="goods-container">
          <!-- 自定义商品骨架屏 -->
          <div class="goods-grid skeleton-grid" v-show="isLoading">
            <div class="goods-card skeleton-card" v-for="i in 12" :key="i">
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

          <!-- 实际商品列表 -->
          <div class="goods-grid" v-show="!isLoading">
            <div
                class="goods-card"
                v-for="goods in goodsList"
                :key="goods.id"
                @click="goToDetail(goods.id)"
            >
              <div class="goods-image-wrapper">
                <img :src="goods.goodsPic || defaultPic" :alt="goods.goodsName" class="goods-image" />
                <div class="goods-tag" v-if="goods.isNew === 1">新品</div>
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
                  <span class="seller-name">{{ goods.sellerNickname }}</span>
                </div>
              </div>
            </div>
          </div>

          <el-pagination
              v-model:current-page="queryParams.pageNum"
              v-model:page-size="queryParams.pageSize"
              :total="total"
              layout="prev, pager, next"
              background
              @current-change="onCurrentChange"
              style="margin-top: 20px; justify-content: center"
          />
        </div>
      </div>
    </main>

    <!-- 底部 -->
    <div class="footer-wrapper">
      <Footer />
    </div>
  </div>
</template>

<style lang="scss" scoped>
/* ========== 全局 ========== */
.page-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  padding-top: 50px;
  background: #f0f2f5;
}

.main-content {
  flex: 1;
  max-width: 1600px;
  margin: 0 auto;
  width: 100%;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ========== 顶部大模块：分类侧栏 + 轮播 + 推荐 ========== */
.top-module {
  display: flex;
  gap: 16px;
  width: 100%;
  height: 400px;
  box-sizing: border-box;
  align-items: stretch;
}

/* --- 侧边分类菜单（玻璃质感） --- */
.sidebar {
  width: 190px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(12px);
  border-radius: 20px;
  padding: 8px 0;
  height: 100%;
  overflow-y: auto;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.6);

  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.08); border-radius: 4px; }

  .category-menu {
    list-style: none;
    margin: 0;
    padding: 0;

    .static-category-item {
      padding: 7px 14px;
      font-size: 13px;
      color: #333;
      display: flex;
      align-items: flex-start;
      gap: 8px;
      transition: background 0.2s;
      border-left: 3px solid transparent;

      &:hover {
        background: rgba(102, 126, 234, 0.06);
        border-left-color: #667eea;

        .main-name { color: #667eea; }
      }

      .icon { font-size: 14px; flex-shrink: 0; margin-top: 1px; }
      .category-content { flex: 1; }

      .sub-items {
        display: flex;
        flex-wrap: wrap;
        gap: 2px 4px;

        .main-name {
          font-size: 12px;
          color: #555;
          cursor: pointer;
          transition: color 0.2s;
          &:hover { color: #f58c14; }
        }
      }
    }
  }
}

/* --- 右侧区域 --- */
.top-right-content {
  flex: 1;
  display: flex;
  gap: 14px;
  height: 100%;
}

/* --- Banner 活动区（潮玩渐变） --- */
.banner-section {
  width: 280px;
  height: 100%;
  border-radius: 20px;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;

  .banner-item {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;

    &.orange {
      background: linear-gradient(145deg, #ff8a00 0%, #ff4d00 100%);
    }

    .banner-content {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      padding: 24px 20px;
      box-sizing: border-box;
      position: relative;

      &::after {
        content: '';
        position: absolute;
        top: -40px;
        right: -40px;
        width: 160px;
        height: 160px;
        background: rgba(255,255,255,0.08);
        border-radius: 50%;
      }

      .banner-text {
        position: relative;
        z-index: 1;
        h2 {
          font-size: 24px;
          font-weight: 800;
          margin-bottom: 8px;
          line-height: 1.2;
          letter-spacing: 1px;
        }
        p {
          font-size: 14px;
          opacity: 0.92;
          margin: 0;
          .highlight {
            display: inline-block;
            background: #fff;
            color: #ff4d00;
            width: 26px; height: 26px;
            border-radius: 50%;
            text-align: center;
            line-height: 26px;
            font-weight: 800;
            font-size: 14px;
          }
        }
      }

      .banner-character {
        position: relative;
        z-index: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        flex: 1;
        min-height: 0;
        svg {
          filter: drop-shadow(0 4px 12px rgba(0,0,0,0.1));
          animation: character-float 3s ease-in-out infinite;
        }
      }

      @keyframes character-float {
        0%, 100% { transform: translateY(0); }
        50% { transform: translateY(-6px); }
      }

      .banner-button {
        border-radius: 30px;
        height: 40px;
        font-size: 14px;
        font-weight: 700;
        background: rgba(255,255,255,0.95);
        color: #ff4d00;
        border: none;
        letter-spacing: 1px;
        transition: all 0.3s;
        position: relative;
        z-index: 1;
        &:hover {
          background: #fff;
          transform: scale(1.03);
          box-shadow: 0 6px 20px rgba(255, 77, 0, 0.35);
        }
      }
    }
  }
}

/* ========== 推荐模块（潮玩风格 · 横向滚动） ========== */
.module-section {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
  gap: 12px;
  flex: 1;
  height: 100%;

  .module-card {
    border-radius: 18px;
    padding: 14px 12px 10px;
    display: flex;
    flex-direction: column;
    height: 100%;
    box-sizing: border-box;
    position: relative;
    overflow: hidden;
    transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.3s;

    &:hover {
      transform: translateY(-3px);
      cursor: pointer;
      .module-title .arrow { transform: translateX(4px); opacity: 0.8; }
    }

    // 每个模块的装饰圆
    &::before {
      content: '';
      position: absolute;
      top: -20px;
      right: -20px;
      width: 80px;
      height: 80px;
      border-radius: 50%;
      opacity: 0.15;
      transition: transform 0.4s;
    }
    &:hover::before {
      transform: scale(1.2);
    }

    // 入场动画
    animation: card-enter 0.5s ease both;
    @for $i from 1 through 4 {
      &:nth-child(#{$i}) { animation-delay: $i * 0.08s; }
    }

    // 各主题色
    &.yellow {
      background: linear-gradient(135deg, #fff7e6 0%, #ffefd5 100%);
      border: 1.5px solid #ffe0a8;
      &::before { background: #ff9f43; }
      .module-gradient-bar { background: linear-gradient(90deg, #ff9f43, #ffb56b); }
    }
    &.blue {
      background: linear-gradient(135deg, #e8f4fd 0%, #d4edfc 100%);
      border: 1.5px solid #b8dff5;
      &::before { background: #2d86c9; }
      .module-gradient-bar { background: linear-gradient(90deg, #2d86c9, #5da8e0); }
    }
    &.green {
      background: linear-gradient(135deg, #e6f9ee 0%, #d0f3df 100%);
      border: 1.5px solid #a8e4c0;
      &::before { background: #20b26c; }
      .module-gradient-bar { background: linear-gradient(90deg, #20b26c, #4fd18b); }
    }
    &.pink {
      background: linear-gradient(135deg, #fde8f0 0%, #fcd6e6 100%);
      border: 1.5px solid #f7b8d0;
      &::before { background: #d63384; }
      .module-gradient-bar { background: linear-gradient(90deg, #d63384, #e668a3); }
    }

    // 渐变标题条
    .module-gradient-bar {
      height: 3px;
      border-radius: 2px;
      margin-bottom: 8px;
      width: 40px;
    }

    .module-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      .module-title-wrapper {
        .module-title {
          font-size: 15px;
          font-weight: 800;
          margin: 0;
          letter-spacing: 0.5px;
          display: flex;
          align-items: center;
          gap: 4px;
          .arrow {
            font-size: 12px;
            opacity: 0.5;
            display: inline-block;
            transition: transform 0.3s, opacity 0.3s;
          }
        }
        .module-subtitle {
          font-size: 11px;
          color: #888;
          letter-spacing: 0.3px;
        }
      }

      .module-icon {
        width: 34px; height: 34px;
        background: rgba(255,255,255,0.7);
        backdrop-filter: blur(4px);
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;
      }
    }

    // 横向滚动商品区（潮玩核心）
    .module-items {
      display: flex;
      gap: 10px;
      overflow-x: auto;
      flex: 1;
      padding: 2px 0 4px;
      scroll-snap-type: x mandatory;

      &::-webkit-scrollbar { display: none; }
      -ms-overflow-style: none;
      scrollbar-width: none;

      .module-item {
        flex-shrink: 0;
        width: 82px;
        text-align: center;
        transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
        scroll-snap-align: start;
        position: relative;
        animation: item-enter 0.4s ease both;

        @for $j from 1 through 10 {
          &:nth-child(#{$j}) { animation-delay: $j * 0.04s; }
        }

        &:hover {
          transform: translateY(-4px) scale(1.04);

          .module-item-img {
            box-shadow: 0 8px 20px rgba(0,0,0,0.15);
          }
        }

        .module-item-img {
          width: 80px;
          height: 80px;
          object-fit: cover;
          border-radius: 14px;
          margin-bottom: 4px;
          box-shadow: 0 2px 8px rgba(0,0,0,0.06);
          transition: box-shadow 0.3s, transform 0.3s;
          background: #fff;
        }

        .price {
          font-size: 12px;
          font-weight: 700;
          color: #ff4d4f;
          display: block;
          line-height: 1.3;
        }
      }
    }

    // 骨架屏
    .skeleton-items {
      display: flex;
      gap: 10px;
      flex: 1;
      .skeleton-item {
        flex-shrink: 0;
        width: 80px;
        .skeleton-img {
          width: 80px; height: 80px;
          border-radius: 14px;
          background: linear-gradient(90deg, rgba(255,255,255,0.3) 25%, rgba(255,255,255,0.6) 50%, rgba(255,255,255,0.3) 75%);
          background-size: 200% 100%;
          animation: skeleton-loading 1.5s infinite;
          margin-bottom: 4px;
        }
        .skeleton-price {
          width: 40px; height: 12px;
          margin: 0 auto;
          border-radius: 4px;
          background: linear-gradient(90deg, rgba(255,255,255,0.3) 25%, rgba(255,255,255,0.6) 50%, rgba(255,255,255,0.3) 75%);
          background-size: 200% 100%;
          animation: skeleton-loading 1.5s infinite;
        }
      }
    }
  }
}

/* ========== 猜你喜欢 + 商品列表 ========== */
.bottom-module {
  display: flex;
  flex-direction: column;
  gap: 18px;
  width: 100%;
  background: #fff;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
}

.tab-section {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
  &::-webkit-scrollbar { height: 3px; }
  &::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.08); border-radius: 4px; }

  .tab-item {
    padding: 8px 20px;
    background: #f5f6f8;
    border-radius: 30px;
    font-size: 14px;
    font-weight: 500;
    color: #444;
    cursor: pointer;
    white-space: nowrap;
    transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
    border: 1px solid transparent;

    &:hover {
      background: #eef0f5;
      border-color: #dde0e8;
    }

    &.active {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
      font-weight: 600;
      border-color: transparent;
      box-shadow: 0 4px 14px rgba(102, 126, 234, 0.3);
    }
  }
}

/* 商品网格 */
.goods-container {
  .goods-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 20px;

    .goods-card {
      background: #fff;
      border-radius: 16px;
      overflow: hidden;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
      transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.35s;
      cursor: pointer;
      border: 1px solid rgba(0,0,0,0.03);

      &:hover {
        transform: translateY(-6px);
        box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1);
      }

      .goods-image-wrapper {
        position: relative;
        width: 100%;
        height: 240px;
        overflow: hidden;
        background: #f0f2f5;

        .goods-image {
          width: 100%;
          height: 100%;
          object-fit: cover;
          transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
        }

        &:hover .goods-image {
          transform: scale(1.08);
        }

        .goods-tag {
          position: absolute;
          top: 12px;
          left: 12px;
          background: linear-gradient(135deg, #ff6b6b, #ee5a24);
          color: #fff;
          font-size: 11px;
          padding: 4px 10px;
          border-radius: 20px;
          font-weight: 700;
          box-shadow: 0 2px 8px rgba(238, 90, 36, 0.3);
        }
      }

      .goods-info {
        padding: 16px;

        .goods-name {
          font-size: 15px;
          font-weight: 600;
          margin-bottom: 10px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          color: #222;
        }

        .goods-price {
          font-size: 22px;
          color: #ff4d4f;
          font-weight: 800;
          margin-bottom: 12px;
          display: flex;
          align-items: baseline;
          gap: 1px;
          &::before {
            content: '¥';
            font-size: 14px;
            font-weight: 700;
          }
        }

        .goods-meta {
          display: flex;
          justify-content: space-between;
          font-size: 12px;
          color: #999;
          margin-bottom: 12px;

          .stock, .like-count {
            background: #f5f6f8;
            padding: 3px 10px;
            border-radius: 20px;
            font-size: 11px;
          }
        }

        .seller-info {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 12px;
          color: #777;

          .el-avatar { border: 2px solid #f0f2f5; }
          .seller-name {
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            max-width: 100px;
          }
        }
      }
    }
  }

  // 骨架屏
  .skeleton-grid .skeleton-card {
    cursor: default;
    &:hover { transform: none; box-shadow: 0 2px 12px rgba(0,0,0,0.05); }

    .skeleton-image {
      width: 100%; height: 240px;
      background: linear-gradient(90deg, #f0f2f5 25%, #e6e8ec 50%, #f0f2f5 75%);
      background-size: 200% 100%;
      animation: skeleton-loading 1.5s infinite;
    }
    .skeleton-name {
      width: 80%; height: 20px;
      margin-bottom: 12px;
      border-radius: 4px;
      background: linear-gradient(90deg, #f0f2f5 25%, #e6e8ec 50%, #f0f2f5 75%);
      background-size: 200% 100%;
      animation: skeleton-loading 1.5s infinite;
    }
    .skeleton-price {
      width: 60%; height: 24px;
      margin-bottom: 12px;
      border-radius: 4px;
      background: linear-gradient(90deg, #f0f2f5 25%, #e6e8ec 50%, #f0f2f5 75%);
      background-size: 200% 100%;
      animation: skeleton-loading 1.5s infinite;
    }
    .skeleton-meta-item {
      width: 70px; height: 18px;
      border-radius: 9px;
      display: inline-block;
      background: linear-gradient(90deg, #f0f2f5 25%, #e6e8ec 50%, #f0f2f5 75%);
      background-size: 200% 100%;
      animation: skeleton-loading 1.5s infinite;
    }
    .skeleton-avatar {
      width: 24px; height: 24px; border-radius: 50%;
      background: linear-gradient(90deg, #f0f2f5 25%, #e6e8ec 50%, #f0f2f5 75%);
      background-size: 200% 100%;
      animation: skeleton-loading 1.5s infinite;
    }
    .skeleton-seller-name {
      width: 80px; height: 16px; border-radius: 4px; display: inline-block;
      background: linear-gradient(90deg, #f0f2f5 25%, #e6e8ec 50%, #f0f2f5 75%);
      background-size: 200% 100%;
      animation: skeleton-loading 1.5s infinite;
    }
  }
}

/* 模块卡片入场动画 */
@keyframes card-enter {
  from { opacity: 0; transform: translateY(20px) scale(0.95); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

/* 商品项入场动画 */
@keyframes item-enter {
  from { opacity: 0; transform: translateX(-12px); }
  to   { opacity: 1; transform: translateX(0); }
}

@keyframes skeleton-loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 底部 */
.footer-wrapper {
  width: 100%;
  flex-shrink: 0;
  background: #fff;
  border-top: 1px solid #e5e5e5;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .main-content { padding: 12px; gap: 16px; }

  .top-module {
    flex-direction: column;
    gap: 16px;
    padding: 0;
    height: auto;
  }

  .sidebar {
    width: 100%;
    height: auto;
    .category-menu {
      display: flex;
      overflow-x: auto;
      flex-wrap: nowrap;
      .static-category-item {
        white-space: nowrap;
        min-width: 130px;
        border-left: none;
        border-bottom: 2px solid transparent;
        &:hover { border-left-color: transparent; border-bottom-color: #667eea; }
      }
    }
  }

  .top-right-content { flex-direction: column; height: auto; }
  .banner-section { width: 100%; height: 180px; }
  .module-section { grid-template-columns: 1fr; gap: 14px; height: auto; }
  .module-section .module-card { min-height: 160px; }

  .bottom-module { padding: 16px; }
  .goods-container .goods-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 12px;
    .goods-card {
      .goods-image-wrapper { height: 160px; }
      .goods-info { padding: 12px; }
      .goods-name { font-size: 14px; }
      .goods-price { font-size: 18px; }
      .skeleton-image { height: 160px; }
    }
  }
}
</style>