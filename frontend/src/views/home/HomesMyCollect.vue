<script setup lang="js">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Picture, Star, ChatDotRound } from '@element-plus/icons-vue'
import { ElMessage} from 'element-plus'
import { useTokenStore } from "@/stores/token.js"
// 导入收藏相关API
import { goodsMyCollectList, goodsDeleteCollect } from '@/api/likeCollect.js'
// 导入创建会话API（聊一聊功能）
import { createChatSessionService } from "@/api/chat.js"

const router = useRouter()
const route = useRoute()

// ========== 响应式数据 ==========
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
const collectList = ref([])
const loading = ref(false)
const tokenStore = useTokenStore()
// 新增：控制悬浮操作栏显示
const hoveredItemId = ref(null)
const isTogglingCollect = ref(false) // 防抖开关

// ========== 方法定义 ==========

// 获取我的收藏列表
const getCollectList = async () => {
  loading.value = true
  try {
    if (tokenStore.token !== '') {
      const res = await goodsMyCollectList(pageNum.value, pageSize.value)
      if (res.code === 0) {
        collectList.value = res.data.items || []
        total.value = res.data.total || 0
      } else {
        ElMessage.error('获取收藏列表失败：' + res.message)
      }
    }
  } catch (error) {
    ElMessage.error('网络异常，获取收藏失败')
  } finally {
    loading.value = false
  }
}

// 分页处理
const onSizeChange = (size) => {
  pageSize.value = size
  getCollectList()
}
const onCurrentChange = (num) => {
  pageNum.value = num
  getCollectList()
}

// 新增：鼠标进入/离开事件
const onMouseEnter = (id) => {
  hoveredItemId.value = id
}
const onMouseLeave = () => {
  hoveredItemId.value = null
}

// 取消收藏（调用提供的API）
const cancelCollect = async (item) => {
  if (isTogglingCollect.value) return
  isTogglingCollect.value = true

  try {
    await goodsDeleteCollect(item.id)
    ElMessage.success('已取消收藏')
    await getCollectList() // 刷新列表
  } catch (error) {
    if (error.response?.status === 401 || error.code === '401') {
      ElMessage.warning('请先登录后再操作')
    } else {
      ElMessage.error('取消收藏失败，请稍后重试')
    }
  } finally {
    isTogglingCollect.value = false
  }
}

// 我想要（聊一聊功能，复用详情页逻辑）
const startChat = async (item) => {
  // 1. 检查登录状态
  if (tokenStore.token === '') {
    ElMessage.warning('请先登录后再发起聊天')
    return
  }

  // 2. 获取卖家ID
  const receiverId = item.sellerId
  if (!receiverId) {
    ElMessage.error('获取卖家信息失败，无法发起聊天')
    return
  }

  try {
    // 3. 调用创建会话接口
    const res = await createChatSessionService(receiverId)
    const sessionId = res.data.id

    // 4. 跳转到聊天页面
    ElMessage.closeAll()
    await router.push({
      path: '/homes/notice',
      query: { sessionId }
    })
  } catch (error) {
    ElMessage.closeAll()
    console.error('创建聊天会话失败:', error)
    ElMessage.error('发起聊天失败，请稍后重试')
  }
}

// 跳转到商品详情
const goToDetail = (id) => {
  const targetUrl = `${window.location.origin}/goods/detail/${id}`
  window.open(targetUrl, '_blank')
}

// 路由返回刷新
watch(() => route.fullPath, async () => {
  await getCollectList()
})

// 初始化
onMounted(async () => {
  await getCollectList()
})
</script>

<template>
  <div class="my-collect-container">
    <div class="header-bar">
      <h2>我的收藏</h2>
    </div>

    <div class="empty-state-wrapper" v-if="total === 0 && !loading">
      <el-empty description="暂无收藏商品" :image-size="200">
        <el-button type="primary" size="large" @click="router.push('/')">
          去逛逛好物
        </el-button>
      </el-empty>
    </div>

    <div class="content-wrapper" v-else>
      <div class="goods-grid" v-loading="loading">
        <el-empty
            v-if="collectList.length === 0"
            description="没有收藏商品"
            style="grid-column: 1 / -1;"
        />

        <!-- 商品卡片 - 新增鼠标悬浮事件 -->
        <div
            class="goods-card"
            v-for="item in collectList"
            :key="item.id"
            @mouseenter="onMouseEnter(item.id)"
            @mouseleave="onMouseLeave"
        >
          <div class="card-img-wrapper" @click="goToDetail(item.id)">
            <el-image
                :src="item.goodsPic ? item.goodsPic.split(',')[0] : ''"
                fit="cover"
                class="goods-img"
                lazy
            >
              <template #error>
                <div class="image-slot">
                  <el-icon size="48" color="#bfbfbf"><Picture /></el-icon>
                </div>
              </template>
            </el-image>

            <!-- 新增：悬浮操作栏 -->
            <div
                class="hover-actions"
                v-if="hoveredItemId === item.id"
                @click.stop
            >
              <button class="action-btn cancel-collect" @click="cancelCollect(item)">
                <el-icon><Star /></el-icon>
                取消收藏
              </button>
              <span class="divider">|</span>
              <button class="action-btn want-chat" @click="startChat(item)">
                <el-icon><ChatDotRound /></el-icon>
                我想要
              </button>
            </div>
          </div>

          <div class="card-info">
            <div class="goods-title" :title="item.goodsName">{{ item.goodsName }}</div>

            <div class="price-row">
              <div class="price-text">
                <span class="currency">¥</span>
                <span class="amount">{{ item.sellPrice }}</span>
              </div>
              <div class="want-count">{{ item.collectCount || 0 }}人想要</div>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination-area">
        <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[12, 24, 36]"
            layout="total, sizes, prev, pager, next"
            background
            :total="total"
            @size-change="onSizeChange"
            @current-change="onCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.my-collect-container {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
  padding: 10px;
  box-sizing: border-box;

  .header-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    h2 {
      margin: 0;
      font-size: 24px;
      color: #222;
      font-weight: 600;
    }
  }
}

.empty-state-wrapper {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 240px);
  gap: 24px;
  padding-bottom: 40px;
  flex: 1;
  align-content: flex-start;
  justify-content: start;
}

.goods-card {
  width: 240px;
  height: 380px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  border: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;

  &:hover {
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  }

  .card-img-wrapper {
    position: relative;
    width: 100%;
    height: 240px;
    cursor: pointer;
    background-color: #f7f8fa;
    overflow: hidden;

    .goods-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      object-position: center;
    }

    .image-slot {
      display: flex;
      justify-content: center;
      align-items: center;
      width: 100%;
      height: 100%;
      color: #999;
      font-size: 14px;
    }

    // 新增：悬浮操作栏样式
    .hover-actions {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 40px;
      background: rgba(0, 0, 0, 0.6);
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      color: #fff;
      transition: opacity 0.2s;

      .action-btn {
        display: flex;
        align-items: center;
        gap: 4px;
        background: transparent;
        border: none;
        color: #fff;
        font-size: 14px;
        cursor: pointer;
        padding: 4px 8px;
        border-radius: 4px;
        transition: background 0.2s;

        &:hover {
          background: rgba(255, 255, 255, 0.2);
        }
      }

      .divider {
        color: rgba(255, 255, 255, 0.5);
      }
    }
  }

  .card-info {
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;

    .goods-title {
      font-size: 16px;
      color: #333;
      font-weight: 500;
      line-height: 1.5;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .price-row {
      display: flex;
      justify-content: space-between;
      align-items: baseline;

      .price-text {
        color: #ff4d4f;
        font-weight: bold;

        .currency {
          font-size: 14px;
        }

        .amount {
          font-size: 20px;
        }
      }

      .want-count {
        font-size: 13px;
        color: #999;
      }
    }
  }
}

.pagination-area {
  padding: 30px 0;
  display: flex;
  justify-content: center;
}

// 响应式适配
@media screen and (max-width: 1024px) {
  .goods-grid {
    grid-template-columns: repeat(auto-fill, 200px);
    gap: 20px;
  }

  .goods-card {
    width: 200px;
    height: 340px;

    .card-img-wrapper {
      height: 200px;
    }
  }
}

@media screen and (max-width: 768px) {
  .my-collect-container {
    padding: 20px 15px;
  }

  .goods-grid {
    grid-template-columns: repeat(auto-fill, 140px);
    gap: 15px;
  }

  .goods-card {
    width: 140px;
    height: 280px;

    .card-img-wrapper {
      height: 140px;
    }

    // 移动端适配悬浮操作栏
    .hover-actions {
      height: 36px;
      .action-btn {
        font-size: 12px;
      }
    }
  }
}
</style>