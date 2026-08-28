<script setup lang="js">
import { onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import router from "@/router/index.js";
import { ElMessage, ElAvatar, ElEmpty } from 'element-plus';
import { useTokenStore } from "@/stores/token.js";
import { goodsOpenDetailSellerAllService, goodsOpenDetailSellerService } from "@/api/goods.js";
// 导入评论接口
import { commentallListService } from "@/api/comment.js";
import Footer from "@/components/footer.vue";
// 导入加载动画组件
import LoadMoreSpinner from "@/components/LoadMoreSpinner.vue";
import userlogin from "@/assets/default.png";

const loading = ref(false);
const route = useRoute();
const tokenStore = useTokenStore();
const activeTab = ref('baobei');

// 防抖开关，防止频繁点击
const isTogglingFollow = ref(false);

// 标记是否存在卖家信息
const sellerExist = ref(true);

// 卖家信息
const sellerInfo = ref({
  sellerId: 0,
  sellerNickname: '',
  sellerAvatar: '',
  publishGoodsCount: 0,
  fanCount: 5,       // 粉丝数
  followCount: 0,    // 关注数
  creditLevel: '',   // 信用等级
  bio: ''            // 个人简介
});

// 关注状态
const isFollowed = ref(false);

// 商品数据
const otherGoods = ref([]);

// 评论数据
const commentList = ref([]);

// --- 分页与下拉加载状态 ---
const pagination = ref({ pageNum: 1, pageSize: 12 }); // 每次加载12条
const hasMore = ref(true);
const isLoadingMore = ref(false);

// 节流函数 - 优化滚动加载
const throttle = (func, delay = 200) => {
  let timer = null;
  return (...args) => {
    if (!timer) {
      timer = setTimeout(() => {
        func.apply(this, args);
        timer = null;
      }, delay);
    }
  };
};

// 监听路由变化，重新加载数据
watch(() => route.params.sellerId, async (newId) => {
  const numericId = Number(newId);
  if (isNaN(numericId) || !numericId) {
    sellerExist.value = false;
    return;
  }

  sellerInfo.value.sellerId = numericId;
  sellerExist.value = true; // 重置状态
  await getSellerInfo();
}, { immediate: true });

// 切换关注状态
const toggleFollow = async () => {
  if (isTogglingFollow.value) return; // 防抖
  isTogglingFollow.value = true;

  try {
    // 未登录拦截
    if (tokenStore.token === '') {
      ElMessage.warning('请先登录后再进行关注操作');
      isTogglingFollow.value = false;
      return;
    }

    if (isFollowed.value) {
      isFollowed.value = false;
      sellerInfo.value.fanCount = Math.max(0, sellerInfo.value.fanCount - 1);
      ElMessage.success('已取消关注');
    } else {
      isFollowed.value = true;
      sellerInfo.value.fanCount++;
      ElMessage.success('关注成功');
    }
  } catch (error) {
    console.error('关注操作失败:', error);
    ElMessage.error('操作失败，请稍后重试');
  } finally {
    isTogglingFollow.value = false;
  }
};

// 格式化时间
const formatTime = (time) => {
  if (!time) return '';
  const date = new Date(time);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).replace(/\//g, '-');
};

// 获取评论列表
const loadCommentList = async () => {
  try {
    if (!sellerInfo.value.sellerId) return;
    const res = await commentallListService(sellerInfo.value.sellerId);
    commentList.value = res.data || [];
  } catch (error) {
    console.error('加载评论列表失败:', error);
  }
};

// 获取卖家详情
async function getSellerInfo() {
  try {
    loading.value = true;
    const res = await goodsOpenDetailSellerService(sellerInfo.value.sellerId);

    if (res.data) {
      sellerInfo.value = {
        ...sellerInfo.value,
        ...res.data,
        fanCount: res.data.fanCount || 5,
        followCount: res.data.followCount || 0,
        creditLevel: res.data.creditLevel || '',
        bio: res.data.bio || ''
      };

      await loadOtherGoods(true); // 传入 true 表示重置列表，重新加载第一页
      await loadCommentList();
      sellerExist.value = true;
    } else {
      sellerExist.value = false;
      ElMessage.warning('未找到该卖家的信息');
    }
  } catch (error) {
    console.error('加载卖家信息失败:', error);
    sellerExist.value = false;

    if (error.message.includes('卖家信息不存在')) {
      ElMessage.warning('该卖家不存在或已注销');
    } else {
      ElMessage.error('加载卖家信息失败，请稍后重试');
    }
  } finally {
    loading.value = false;
  }
}

// --- 分页加载商品列表 ---
const loadOtherGoods = async (isReset = false) => {
  try {
    if (!sellerInfo.value.sellerId) return;

    // 如果是重新加载，重置分页参数
    if (isReset) {
      pagination.value.pageNum = 1;
      otherGoods.value = [];
      hasMore.value = true;
    }

    // 如果没有更多数据或正在加载中，则不请求
    if (!hasMore.value || isLoadingMore.value) return;

    isLoadingMore.value = true;

    const queryData = {
      sellerId: sellerInfo.value.sellerId,
      pageNum: pagination.value.pageNum,
      pageSize: pagination.value.pageSize
    };

    const res = await goodsOpenDetailSellerAllService(queryData);
    console.log(res);
    const newItems = res.data.items || [];

    // 如果返回的数据少于 pageSize，说明到底了
    if (newItems.length < pagination.value.pageSize) {
      hasMore.value = false;
    }

    // 后端已排好序，直接将新数据追加到末尾
    otherGoods.value = [...otherGoods.value, ...newItems];
    pagination.value.pageNum++; // 为下一次请求做准备

  } catch (error) {
    console.error('加载其他商品失败:', error);
    ElMessage.error('加载商品列表失败');
  } finally {
    isLoadingMore.value = false;
  }
};

// --- 滚动触底监听逻辑（添加节流）---
const handleScroll = throttle(() => {
  // 只在“宝贝”标签页下触发加载
  if (activeTab.value !== 'baobei') return;

  const scrollHeight = document.documentElement.scrollHeight;
  const scrollTop = document.documentElement.scrollTop || document.body.scrollTop;
  const clientHeight = document.documentElement.clientHeight;

  // 当距离底部不足 150px 时，触发加载下一页
  if (scrollHeight - scrollTop - clientHeight <= 150) {
    loadOtherGoods();
  }
});

// 跳转到商品详情（增加状态拦截）
const goToGoodsDetail = (item) => {
  // 只有状态为1（在售）才允许跳转
  if (item.goodsStatus === 1) {
    router.push(`/goods/detail/${item.id}`);
  }
};

onMounted(async () => {
  if (route.params.sellerId) {
    await getSellerInfo();
  } else {
    sellerExist.value = false;
  }
  // 挂载全局滚动事件
  window.addEventListener('scroll', handleScroll);
});

onUnmounted(() => {
  // 组件销毁时移除事件，防止内存泄漏
  window.removeEventListener('scroll', handleScroll);
});
</script>

<template>
  <div class="page-wrapper">
    <div class="seller-detail-container no-select" v-if="sellerExist && !loading">
      <section class="user-banner">
        <div class="banner-bg-elements"></div>
        <div class="user-info-box">
          <div class="avatar-area">
            <ElAvatar
                :src="sellerInfo.sellerAvatar || userlogin"
                class="avatar-large"
            />
          </div>

          <div class="text-area">
            <div class="name-row">
              <h1 class="nickname">{{ sellerInfo.sellerNickname  || '未知用户' }}</h1>
              <span class="credit-badge" v-if="sellerInfo.creditLevel">🐼{{ sellerInfo.creditLevel }}</span>
            </div>
            <div class="stats-row">
              <span>{{ sellerInfo.fanCount }}粉丝</span>
              <span class="divider">|</span>
              <span>{{ sellerInfo.followCount }}关注</span>
            </div>
            <div class="bio-row" v-if="sellerInfo.bio">
              {{ sellerInfo.bio }}
            </div>
          </div>

          <div class="action-area">
            <div v-if="!isFollowed" class="wow-bubble">WOW~</div>
            <button
                :class="['follow-btn', { 'is-followed': isFollowed }]"
                @click="toggleFollow"
                :disabled="isTogglingFollow"
            >
              {{ isFollowed ? '已关注' : '关注' }}
            </button>
          </div>
        </div>
      </section>

      <div class="content-wrapper">
        <nav class="content-tabs">
          <div
              :class="['tab-item', { active: activeTab === 'baobei' }]"
              @click="activeTab = 'baobei'"
          >
            宝贝 <span class="count">{{ sellerInfo.publishGoodsCount || 0 }}</span>
          </div>
          <div
              :class="['tab-item', { active: activeTab === 'credit' }]"
              @click="activeTab = 'credit'"
          >
            信用及评价 <span class="count">{{ commentList.length || 0 }}</span>
          </div>
        </nav>

        <div class="content-grid" v-show="activeTab === 'baobei'">
          <div
              v-for="item in otherGoods"
              :key="item.id"
              class="baobei-card"
              :class="{ 'is-disabled': item.goodsStatus !== 1 }"
              @click="goToGoodsDetail(item)"
          >
            <div class="card-image">
              <img :src="item.goodsPic || userlogin" :alt="item.goodsName" />

              <div class="status-overlay" v-if="item.goodsStatus !== 1">
                <div class="stamp-box">
                  {{ item.goodsStatus === 2 ? '卖掉了' : (item.goodsStatusName || '已下架') }}
                </div>
              </div>
            </div>
            <div class="card-info">
              <div class="title-wrap">
                <span class="tag-shipping" v-if="item.postageFree">包邮</span>
                <span class="title">{{ item.goodsName }}</span>
              </div>
              <div class="price-row">
                <span class="price-symbol">¥</span>
                <span class="price-num">{{ Number(item.sellPrice).toFixed(2) }}</span>
                <span class="want-count">{{ item.collectCount || 0 }}人想要</span>
              </div>
              <div class="user-tag">
                <ElAvatar :src="item.sellerAvatar || sellerInfo.sellerAvatar || userlogin" class="avatar-16px" />
                <span>{{ item.sellerNickname || sellerInfo.sellerNickname }}</span>
              </div>
            </div>
          </div>

          <div class="no-goods" v-if="otherGoods.length === 0 && !loading && !isLoadingMore">
            <div class="empty-placeholder">
              <p>该商家暂无在售商品</p>
            </div>
          </div>

          <div class="load-more-indicator" v-if="otherGoods.length > 0">
            <LoadMoreSpinner v-if="isLoadingMore" />
            <span v-else-if="!hasMore" class="nomore-text">—— 到底啦，没有更多宝贝了 ——</span>
          </div>
        </div>

        <div class="content-grid" v-show="activeTab === 'credit'">
          <div class="comment-list" v-if="commentList.length > 0">
            <div
                v-for="item in commentList"
                :key="item.id"
                class="comment-item"
            >
              <div class="comment-avatar">
                <ElAvatar :src="item.userUrl || userlogin" :size="40" />
              </div>
              <div class="comment-content">
                <div class="comment-header">
                  <span class="comment-nickname">{{ item.nickname || '匿名用户' }}</span>
                </div>
                <div class="comment-badge">
                  <span class="comment-text">{{ item.content }}</span>
                </div>
                <div class="comment-time">
                  {{ formatTime(item.createTime) }}
                </div>
              </div>
            </div>
          </div>
          <div class="no-goods" v-else>
            <div class="empty-placeholder">
              <p>暂无信用及评价信息</p>
            </div>
          </div>
        </div>
      </div>

    </div>

    <div class="loading-container" v-if="loading">
      <div class="loading-content">
        <div class="spinner"></div>
        <p>正在加载卖家信息...</p>
      </div>
    </div>

    <div class="empty-container" v-if="!sellerExist && !loading">
      <ElEmpty
          description="该卖家不存在或已注销"
          :image-size="200"
      >
        <el-button type="primary" @click="router.back()">返回上一页</el-button>
      </ElEmpty>
    </div>
  </div>
  <div class="footer-wrapper" v-if="sellerExist && !loading">
    <Footer />
  </div>
</template>

<style lang="scss" scoped>
// 自定义头像尺寸
.avatar-16px {
  --el-avatar-size: 20px;
}

// 统一由 CSS 控制大头像大小，便于移动端自适应覆盖
.avatar-large {
  width: 80px;
  height: 80px;
  border: 3px solid #fff;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.page-wrapper {
  margin-top: 60px;
  width: 100%;
  min-height: 100vh;
  box-sizing: border-box;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  background-color: #f8f8f8;
}

.loading-container {
  width: 100%;
  height: 60vh;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;

  .loading-content {
    text-align: center;

    .spinner {
      width: 40px;
      height: 40px;
      border: 4px solid #f3f3f3;
      border-top: 4px solid #ffd100;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin: 0 auto 20px;
    }

    p {
      color: #666;
      font-size: 16px;
    }
  }
}

.empty-container {
  width: 100%;
  padding: 40px;
  box-sizing: border-box;
  display: flex;
  justify-content: center;
  align-items: center;
}

.seller-detail-container {
  width: 100%;
  min-height: calc(100vh - 80px);
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
  font-size: 14px;
  color: #333;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  border-radius: 15px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);

  .user-banner {
    height: 160px;
    background: linear-gradient(to right, #fffbf0 0%, #fff6de 100%);
    position: relative;
    overflow: hidden;
    border-radius: 0;
    display: flex;
    justify-content: center;
    align-items: center;

    .banner-bg-elements {
      position: absolute;
      right: -20px;
      bottom: -20px;
      width: 300px;
      height: 100%;
      background-color: #ffecd2;
      border-radius: 50%;
      opacity: 0.4;
      filter: blur(20px);
      z-index: 0;
    }

    .user-info-box {
      display: flex;
      align-items: flex-start;
      gap: 20px;
      position: relative;
      z-index: 1;
      width: 100%;
      padding: 0 30px;

      .avatar-area {
        width: 80px;
        height: 80px;
        border-radius: 50%;
        overflow: hidden;
        cursor: pointer;
      }

      .text-area {
        flex: 1;
        display: flex;
        flex-direction: column;
        justify-content: center;
        margin-top: 5px;

        .name-row {
          display: flex;
          align-items: center;
          gap: 10px;
          margin-bottom: 8px;

          .nickname {
            font-size: 20px;
            font-weight: bold;
            margin: 0;
            color: #333;
          }

          .credit-badge {
            background-color: #ffd100;
            color: #333;
            font-size: 11px;
            padding: 2px 8px;
            border-radius: 12px;
            font-weight: bold;
          }
        }

        .stats-row {
          font-size: 12px;
          color: #999;
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 8px;

          .divider {
            color: #ddd;
            font-size: 10px;
          }
        }

        .bio-row {
          font-size: 12px;
          color: #666;
        }
      }

      .action-area {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-top: 15px;

        .wow-bubble {
          font-size: 14px;
          color: #999;
          font-weight: bold;
          font-style: italic;
          background: #f5f5f5;
          padding: 4px 8px;
          border-radius: 12px;
        }

        .follow-btn {
          background: #ffd100;
          color: #333;
          border: none;
          padding: 10px 24px;
          border-radius: 20px;
          font-size: 14px;
          font-weight: bold;
          cursor: pointer;
          transition: all 0.2s ease;
          display: flex;
          align-items: center;
          gap: 6px;

          &:hover:not(.is-followed) {
            background: #f5c800;
          }

          &:disabled {
            opacity: 0.7;
            cursor: not-allowed;
          }

          &.is-followed {
            background: #f0f0f0;
            color: #999;
            font-weight: normal;
          }
        }
      }
    }
  }

  .content-wrapper {
    flex: 1;
    display: flex;
    flex-direction: column;
    width: 100%;
    max-width: 1500px;
    border-radius: 15px;
    overflow: hidden;
    background: #fff;
    box-shadow: 0 2px 10px rgba(0,0,0,0.03);
    margin: 10px auto;
  }

  .content-tabs {
    display: flex;
    background: #fff;
    border-bottom: 1px solid #f5f5f5;
    margin: 0;

    .tab-item {
      padding: 20px 20px 15px 20px;
      font-size: 16px;
      cursor: pointer;
      color: #666;
      position: relative;

      .count {
        font-size: 12px;
        margin-left: 2px;
        color: #999;
      }

      &.active {
        color: #333;
        font-weight: bold;

        .count {
          color: #333;
        }

        &::after {
          content: '';
          position: absolute;
          bottom: 0;
          left: 0;
          width: 100%;
          height: 3px;
          background: #ffd100;
          border-radius: 2px;
        }
      }
    }
  }

  .content-grid {
    flex: 1;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 20px;
    padding: 20px;
    align-content: flex-start;
    min-height: 300px;

    .no-goods {
      grid-column: 1 / -1;
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100%;
      min-height: 300px;

      .empty-placeholder {
        height: 200px;
        width: 100%;
        max-width: 600px;
        border: 1px dashed #e0e0e0;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #fafafa;

        p {
          font-size: 16px;
          color: #999;
          margin: 0;
        }
      }
    }

    .comment-list {
      grid-column: 1 / -1;
      width: 100%;
      padding: 10px 0;
    }

    .comment-item {
      display: flex;
      gap: 12px;
      padding: 16px 0;
      border-bottom: 1px solid #f5f5f5;

      &:last-child {
        border-bottom: none;
      }

      .comment-avatar {
        flex-shrink: 0;
      }

      .comment-content {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 8px;

        .comment-header {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 14px;
          color: #333;
        }

        .comment-badge {
          display: flex;
          align-items: center;
          gap: 8px;

          .comment-text {
            font-size: 14px;
            color: #333;
          }
        }

        .comment-time {
          font-size: 12px;
          color: #999;
        }
      }
    }
  }

  // 商品卡片
  .baobei-card {
    background: #fff;
    border-radius: 12px;
    overflow: hidden;
    transition: all 0.3s ease;
    cursor: pointer;
    position: relative;
    border: 1px solid #f0f0f0;

    &:hover {
      transform: translateY(-3px);
      box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
    }

    // 禁用状态（非在售）
    &.is-disabled {
      cursor: not-allowed;

      &:hover {
        transform: none;
        box-shadow: none;
      }

      .card-info {
        opacity: 0.6;
      }
    }

    .card-image {
      height: 220px;
      width: 100%;
      border-radius: 12px 12px 0 0;
      overflow: hidden;
      position: relative;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      // 遮罩与印章
      .status-overlay {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background-color: rgba(255, 255, 255, 0.45);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 10;

        .stamp-box {
          font-size: 20px;
          font-weight: 900;
          color: #555;
          padding: 8px 18px;
          border: 4px solid #555;
          border-radius: 6px;
          transform: rotate(-18deg);
          letter-spacing: 2px;
          background: rgba(255, 255, 255, 0.85);
          box-shadow: 0 0 0 2px rgba(255,255,255,0.4), inset 0 0 0 2px rgba(255,255,255,0.4);
          user-select: none;
        }
      }
    }

    .card-info {
      padding: 12px;

      .title-wrap {
        font-size: 14px;
        color: #333;
        line-height: 20px;
        margin-bottom: 8px;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;

        .tag-shipping {
          display: inline-block;
          background-color: #ffd100;
          color: #333;
          font-size: 11px;
          padding: 1px 4px;
          border-radius: 4px;
          margin-right: 4px;
          vertical-align: text-bottom;
          font-weight: bold;
        }

        .title {
          font-weight: 500;
        }
      }

      .price-row {
        display: flex;
        align-items: baseline;
        margin-bottom: 10px;

        .price-symbol {
          color: #ff4d4f;
          font-size: 12px;
          font-weight: bold;
        }

        .price-num {
          color: #ff4d4f;
          font-size: 18px;
          font-weight: bold;
          margin-right: 6px;
        }

        .want-count {
          color: #999;
          font-size: 11px;
        }
      }

      .user-tag {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 12px;
        color: #999;
      }
    }
  }

  .load-more-indicator {
    grid-column: 1 / -1;
    text-align: center;
    padding: 10px 0;

    .nomore-text {
      color: #ccc;
      font-size: 13px;
      display: inline-block;
      padding: 15px 0;
    }
  }
}

.footer-wrapper {
  width: 100%;
  padding: 10px 20px;
  box-sizing: border-box;
  background: #f8f8f8;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* =========================================
   📱 移动端深度适配 (Max-width: 768px)
========================================= */
@media screen and (max-width: 768px) {
  .page-wrapper {
    background-color: #f5f5f5;
    padding-top: 40px;
  }

  .seller-detail-container {
    min-height: 100vh;
    border-radius: 0; /* 移除大圆角 */
    box-shadow: none;


    .user-banner {
      height: auto;
      padding: 30px 15px 40px; /* 底部多留点空间给被遮挡的卡片层 */
      align-items: flex-start;

      .user-info-box {
        flex-direction: row; /* 横向排列更符合App习惯 */
        align-items: center;
        gap: 12px;
        padding: 0;

        .avatar-area {
          width: 60px;
          height: 60px;
          .avatar-large {
            width: 60px;
            height: 60px;
          }
        }

        .text-area {
          flex: 1;
          margin-top: 0;

          .name-row {
            margin-bottom: 4px;
            .nickname {
              font-size: 16px;
            }
            .credit-badge {
              font-size: 10px;
              padding: 1px 6px;
            }
          }

          .stats-row {
            margin-bottom: 4px;
            font-size: 11px;
          }

          .bio-row {
            display: -webkit-box;
            -webkit-line-clamp: 1;
            -webkit-box-orient: vertical;
            overflow: hidden;
            font-size: 11px;
          }
        }

        .action-area {
          margin-top: 0;
          .wow-bubble {
            display: none; /* 移动端隐藏多余装饰，节省空间 */
          }
          .follow-btn {
            padding: 6px 14px;
            font-size: 13px;
            border-radius: 15px;
          }
        }
      }
    }

    .content-wrapper {
      margin: 0;
      border-radius: 16px 16px 0 0; /* 顶部圆角，类似抽屉弹起 */
      transform: translateY(-20px); /* 向上重叠在背景上，视觉更好 */
      z-index: 2;
      position: relative;
    }

    .content-tabs {
      .tab-item {
        flex: 1; /* 均分空间 */
        text-align: center;
        padding: 14px 0;
        font-size: 15px;
      }
    }

    .content-grid {
      /* 强制两列瀑布流布局 */
      grid-template-columns: repeat(2, 1fr);
      gap: 10px;
      padding: 12px;

      .baobei-card {
        .card-image {
          height: 170px; /* 移动端适当减小图片高度 */

          .status-overlay .stamp-box {
            font-size: 16px;
            padding: 4px 12px;
          }
        }

        .card-info {
          padding: 8px;

          .title-wrap {
            font-size: 13px;
            line-height: 18px;
            margin-bottom: 6px;
          }

          .price-row {
            margin-bottom: 6px;
            .price-num {
              font-size: 16px;
            }
          }

          .user-tag {
            font-size: 11px;
            .avatar-16px {
              --el-avatar-size: 16px;
            }
          }
        }
      }

      .comment-list {
        padding: 0 4px;
      }
    }
  }

  .footer-wrapper {
    padding: 10px;
  }
}
</style>