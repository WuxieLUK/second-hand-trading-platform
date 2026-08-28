<script setup>
import { articleMyCollectServices } from "@/api/article.js"
import { useTokenStore } from "@/stores/token.js"
import { ElMessage } from "element-plus";
import { ref, onMounted, computed } from "vue";
import { Calendar, ArrowRight, Sort, Reading} from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';

const tokenStore = useTokenStore();
const router = useRouter();

// --- 状态管理 ---
const myCollect = ref([]);
const loading = ref(false);
const loadingMore = ref(false);
const total = ref(0);
const queryParams = ref({
  pageNum: 1,
  pageSize: 8,
  orderBy: 'collect_time' // 默认按收藏时间排序
});

// --- 获取数据 ---
const getMyCollect = async (isLoadMore = false) => {
  if (!tokenStore.token) return;

  if (isLoadMore) loadingMore.value = true;
  else loading.value = true;

  try {
    const res = await articleMyCollectServices(queryParams.value);
    if (isLoadMore) {
      myCollect.value = [...myCollect.value, ...res.data.items];
    } else {
      myCollect.value = res.data.items;
    }
    total.value = res.data.total;
  } catch (error) {
    ElMessage.error("获取收藏列表失败");
  } finally {
    loading.value = false;
    loadingMore.value = false;
  }
};

// --- 功能函数 ---
const handleLoadMore = () => {
  queryParams.value.pageNum++;
  getMyCollect(true);
};

const toggleSort = () => {
  // 切换排序逻辑（模拟前端排序，或更改参数重新请求）
  myCollect.value.reverse();
  ElMessage.success("排序已切换");
};

const goToDetail = (id) => {
  router.push(`/homes/articleComment/${id}`);
};

// 清洗 Markdown 文本作为预览
const getPureText = (content) => {
  return content.replace(/[#*`>\s]/g, '').substring(0, 80) + '...';
};

onMounted(() => {
  getMyCollect();
});

// 是否还有更多
const hasMore = computed(() => myCollect.value.length < total.value);
</script>

<template>
  <div class="collect-wrapper no-select">
    <div class="collect-header">
      <div class="header-left">
        <span class="count-badge">共 {{ total }} 篇收藏</span>
      </div>
      <div class="header-right">
        <el-button :icon="Sort" variant="text" @click="toggleSort">
          {{ queryParams.orderBy === 'collect_time' ? '时间正序' : '时间倒序' }}
        </el-button>
      </div>
    </div>

    <div class="collect-scroll-area" v-loading="loading && queryParams.pageNum === 1">
      <div v-if="myCollect.length > 0" class="collect-list">
        <div
            v-for="item in myCollect"
            :key="item.id"
            class="modern-card"
            @click="goToDetail(item.id)"
        >
          <div class="card-body">
            <div class="text-section">
              <h3 class="title">{{ item.title }}</h3>
              <p class="excerpt">{{ getPureText(item.content) }}</p>
              <div class="meta-row">
                <span class="meta-item author">
                  <el-icon><Reading /></el-icon> 文章详情
                </span>
                <span class="meta-item date">
                  <el-icon><Calendar /></el-icon>
                  {{ item.collectTime || '刚刚' }} 收藏
                </span>
              </div>
            </div>
            <div class="image-section" v-if="item.coverImg">
              <el-image :src="item.coverImg" fit="cover" lazy>
                <template #placeholder>
                  <div class="image-slot">加载中...</div>
                </template>
              </el-image>
            </div>
          </div>
        </div>

        <div class="load-more-container" v-if="hasMore">
          <el-button
              link
              :loading="loadingMore"
              @click="handleLoadMore"
              class="load-more-btn"
          >
            {{ loadingMore ? '正在努力加载...' : '查看更多收藏' }}
            <el-icon v-if="!loadingMore"><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div v-else class="no-more">已显示全部收藏内容</div>
      </div>

      <el-empty v-else :image-size="120" description="收藏夹空空如也" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.collect-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: transparent;
}

.collect-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 4px 16px 4px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 12px;

  .count-badge {
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    background: #f0f4ff;
    padding: 4px 12px;
    border-radius: 20px;
  }
}

.collect-scroll-area {
  max-height: 65vh;
  overflow-y: auto;
  padding-right: 8px;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
}

.modern-card {
  padding: 20px 0;
  border-bottom: 1px solid var(--el-border-color-extra-light);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    .title { color: #2563eb; }
    .image-section img { transform: scale(1.05); }
  }

  .card-body {
    display: flex;
    gap: 20px;
    align-items: flex-start;
  }

  .text-section {
    flex: 1;
    min-width: 0;

    .title {
      font-size: 18px;
      font-weight: 700;
      margin: 0 0 8px 0;
      color: var(--el-text-color-primary);
      line-height: 1.4;
      display: -webkit-box;
      -webkit-line-clamp: 1;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .excerpt {
      font-size: 14px;
      color: var(--el-text-color-regular);
      line-height: 1.6;
      margin-bottom: 12px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .meta-row {
      display: flex;
      gap: 16px;
      font-size: 13px;
      color: var(--el-text-color-placeholder);

      .meta-item {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }

  .image-section {
    width: 140px;
    height: 90px;
    border-radius: 8px;
    overflow: hidden;
    flex-shrink: 0;
    border: 1px solid var(--el-border-color-extra-light);

    .el-image {
      width: 100%;
      height: 100%;
      transition: transform 0.4s ease;
    }
  }
}

.load-more-container {
  padding: 24px 0;
  display: flex;
  justify-content: center;

  .load-more-btn {
    font-size: 14px;
    color: #64748b;
    &:hover { color: #2563eb; }
  }
}

.no-more {
  text-align: center;
  padding: 20px 0;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}

/* 适配移动端 */
@media screen and (max-width: 600px) {
  .modern-card {
    padding: 16px 0;
    .card-body { flex-direction: column-reverse; gap: 12px; }
    .image-section { width: 100%; height: 160px; }
    .text-section .title { font-size: 16px; }
  }
}
</style>