<script lang="js" setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';
import {
  User,
  SwitchButton,
  Close,
  Operation,
  Location,
  Search, Plus, HomeFilled, Bell, Tickets, Delete,
  ChatDotRound, Top
} from '@element-plus/icons-vue';
import userlogin from "@/assets/default.png"
import { useRouter, useRoute } from 'vue-router';
import { useTokenStore } from "@/stores/token.js"
import useUserInfoStore from "@/stores/userInfo.js";
import { ElMessage, ElMessageBox, ElTooltip } from "element-plus";
import { userInfoServices } from "@/api/user.js";
import MoveMenu from "@/components/MoveMenu.vue";
import SvgIcon from "@/components/SvgIcon.vue";
import Loading from '@/components/Loading.vue'
import { useLoadingStore } from '@/stores/loading'

const route = useRoute();
const activeMenu = ref(route.path)
const userInfoStore = useUserInfoStore();
const tokenStore = useTokenStore();
const mobileMenuVisible = ref(false);

// 搜索核心响应式数据
const searchDialogVisible = ref(false);
const searchQuery = ref('');
const searchResults = ref([]);
const isSearching = ref(false);
const hasSearched = ref(false);
const pageNum = ref(1);
const total = ref(0);
const aiSearchEnabled = ref(false); // AI 智能搜索开关

// 搜索历史相关
const searchHistory = ref([]); // 存储本地搜索历史
const showSearchHistory = ref(false); // 控制历史记录显示/隐藏
const HISTORY_KEY = 'searchHistory'; // localStorage 存储键名
const MAX_HISTORY_COUNT = 10; // 最大历史记录数
// 新增：搜索容器 DOM 引用（用于判断点击是否在内部）
const searchContainerRef = ref(null);

// 读取本地搜索历史
const loadSearchHistory = () => {
  try {
    const history = localStorage.getItem(HISTORY_KEY);
    searchHistory.value = history ? JSON.parse(history) : [];
  } catch (error) {
    console.error('读取搜索历史失败:', error);
    searchHistory.value = [];
  }
};

// 添加搜索历史（去重+控制数量）
const addSearchHistory = (keyword) => {
  if (!keyword.trim()) return;

  // 去重：如果已有该关键词，先删除旧的
  searchHistory.value = searchHistory.value.filter(item => item !== keyword.trim());
  // 新增记录插入头部
  searchHistory.value.unshift(keyword.trim());
  // 控制最多10条，超出则删除最后一条（最早的）
  if (searchHistory.value.length > MAX_HISTORY_COUNT) {
    searchHistory.value = searchHistory.value.slice(0, MAX_HISTORY_COUNT);
  }
  // 保存到本地
  localStorage.setItem(HISTORY_KEY, JSON.stringify(searchHistory.value));
};

// 清空搜索历史
const clearSearchHistory = () => {
  searchHistory.value = [];
  localStorage.removeItem(HISTORY_KEY);
  ElMessage.success('搜索历史已清空');
};

// 新增：删除单条搜索历史
const deleteSingleHistory = (keyword) => {
  // 过滤掉要删除的关键词
  searchHistory.value = searchHistory.value.filter(item => item !== keyword);
  // 同步到本地存储
  localStorage.setItem(HISTORY_KEY, JSON.stringify(searchHistory.value));
  ElMessage.success('已删除该搜索记录');
};

// 点击历史记录项触发搜索
const handleHistoryItemClick = (keyword) => {
  searchQuery.value = keyword;
  handleSearch();
  // 点击历史项后主动关闭面板
  showSearchHistory.value = false;
};

// 点击外部关闭历史记录面板
const handleClickOutside = (e) => {
  // 如果面板显示，且点击的区域不在搜索容器内 → 关闭
  if (
      showSearchHistory.value &&
      searchContainerRef.value &&
      !searchContainerRef.value.contains(e.target)
  ) {
    showSearchHistory.value = false;
  }
};

const getUserInfo = async () => {
  try {
    if (tokenStore) {
      let result = await userInfoServices();
      userInfoStore.setInfo(result.data);
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
  }
}

const router = useRouter();
const lastScrollTop = ref(0);
const isScrollingDown = ref(false);

const handleScroll = () => {
  const scrollTop = window.scrollY || document.documentElement.scrollTop;
  isScrollingDown.value = scrollTop > lastScrollTop.value && scrollTop > 56;
  lastScrollTop.value = scrollTop;
};

const showBackTop = ref(false);

// 修复：原代码中handlePageScroll未定义的问题
const handlePageScroll = () => {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop || 0;
  showBackTop.value = scrollTop > 300;
};

onMounted(() => {
  if (tokenStore.token !== '') {
    getUserInfo();
  }
  // 初始化加载搜索历史
  loadSearchHistory();
  // 监听全局点击
  document.addEventListener('click', handleClickOutside);
  window.addEventListener('scroll', handlePageScroll);
  window.addEventListener('scroll', handleScroll);
});

onUnmounted(() => {
  window.removeEventListener('scroll', handlePageScroll);
  window.removeEventListener('scroll', handleScroll);
  // 移除全局点击监听（防止内存泄漏）
  document.removeEventListener('click', handleClickOutside);
});

const handleCommand = (command) => {
  if (command === 'logout') {
    ElMessageBox.confirm('你确认要退出登录吗?', '温馨提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
        .then(async () => {
          tokenStore.removeToken()
          userInfoStore.removeInfo()
          if ('caches' in window) {
            caches.keys().then((cacheNames) => {
              cacheNames.forEach((cacheName) => { caches.delete(cacheName) })
            })
          }
          await router.push('/')
          ElMessage.success('退出登录成功')
        })
        .catch(() => {
          ElMessage.info('用户取消了退出登录')
        })
  } else if(command=== 'logined') {
    const targetUrl = `${window.location.origin}/homes/${command}`;
    window.open(targetUrl, '_blank');
  } else {
    router.push('/homes/' + command)
  }
}

const isShow = ref(true)
let timer = null
onMounted(() => {
  timer = setTimeout(() => { isShow.value = false }, 4000)
})
onUnmounted(() => { clearTimeout(timer) })

const isMobile = ref(window.innerWidth <= 1200);
const drawer = ref(false);
watch(() => window.innerWidth, (newWidth) => {
  isMobile.value = newWidth <= 1200;
  if (!isMobile.value) mobileMenuVisible.value = false;
});

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

// 修改搜索执行函数：保存历史 + 跳转路由
const handleSearch = async () => {
  const keyword = searchQuery.value.trim();
  if (!keyword) return;

  // 1. 添加到搜索历史
  addSearchHistory(keyword);

  // ---> 修改点：执行搜索后主动关闭历史面板 <---
  showSearchHistory.value = false;

  // 2. 跳转到搜索结果页，携带关键词 + AI模式参数
  await router.push({
    path: '/homes/search',
    query: {
      keyword: keyword,
      ai: aiSearchEnabled.value ? '1' : '0'
    }
  });

  // 原有搜索逻辑（如果需要在新页面请求数据，可移到/homes/search组件中）
  isSearching.value = true;
  pageNum.value = 1;
};

watch(searchDialogVisible, (val) => {
  if (!val) {
    searchQuery.value = '';
    searchResults.value = [];
    hasSearched.value = false;
    total.value = 0;
  }
});

// 初始化加载状态 store
const loadingStore = useLoadingStore()
watch(
    () => router.currentRoute.value,
    (newRoute, oldRoute) => {
      if (newRoute.path !== oldRoute?.path) {
        loadingStore.setLoading(true)
      }
      const loaderTimer = setTimeout(() => {
        loadingStore.setLoading(false)
        clearTimeout(loaderTimer)
      }, 300)
    },
    { immediate: true }
)

const handleMenuSelect = (index) => {
  const whiteList = ['/homes/home', '/','/homes/dynamic'];
  if (!whiteList.includes(index) && tokenStore.token === '') {
    ElMessage.warning('请登录');
    return;
  }
  if(index==='order'){
    const targetUrl = `${window.location.origin}/homes/myBought`;
    window.open(targetUrl, '_blank');
    return;
  }
  router.push(index);
};

const handlePublishClick = () => {
  if (tokenStore.token === '') {
    ElMessage.warning('请登录');
    return;
  }
  const targetUrl = `${window.location.origin}/homes/publish`;
  window.open(targetUrl, '_blank');
};

const goToAIChat = () => {
  router.push('/homes/AIChat');
};
</script>

<template>
  <el-container class="layout-container">
    <div class="common-layout">
      <el-menu
          class="nav-bar no-select"
          mode="horizontal"
          :style="{ transform: isScrollingDown ? 'translateY(-100%)' : 'translateY(0)' }"
          :default-active="activeMenu"
          :ellipsis="false"
          @select="handleMenuSelect"
      >
        <span class="menu-icon" @click="drawer = !drawer">
          <el-icon><Operation /></el-icon>
        </span>

        <div class="nav-left">
          <el-tooltip placement="bottom" content="点击返回首页">
            <div class="logo" @click="router.push('/')">
              <SvgIcon icon-class="tag" style="width: 20px; height: 20px; margin-right: 5px;"/> 校园交易
            </div>
          </el-tooltip>

          <el-menu-item index="/homes/home"><el-icon><HomeFilled /></el-icon>商品</el-menu-item>
        </div>

        <div class="nav-center">
          <div
              class="search-container"
              ref="searchContainerRef"
          >
            <div class="custom-search-input">
              <el-icon class="search-icon"><Search /></el-icon>
              <input
                  type="text"
                  v-model="searchQuery"
                  placeholder="搜索商品..."
                  class="search-input-field"
                  @focus="showSearchHistory = true"
                  @click="showSearchHistory = true"
                  @keyup.enter="handleSearch"
              >
              <el-icon
                  v-if="searchQuery"
                  class="clear-icon"
                  @click.stop="searchQuery = ''"
              >
                <Close />
              </el-icon>
            </div>

            <!-- AI 智能搜索开关 -->
            <div
                class="ai-search-toggle"
                :class="{ active: aiSearchEnabled }"
                @click="aiSearchEnabled = !aiSearchEnabled"
                :title="aiSearchEnabled ? '当前为 AI 智能搜索' : '当前为普通搜索'"
            >
              <el-icon><ChatDotRound /></el-icon>
            </div>

            <!-- 搜索按钮 -->
            <div
                class="search-btn"
                @click="handleSearch"
            >
              <el-icon><Search /></el-icon>
              <span>搜索</span>
            </div>

            <div
                v-if="showSearchHistory"
                class="search-history-panel"
                @click.stop
            >
              <div class="history-header">
                <span>搜索历史</span>
                <el-icon
                    v-if="searchHistory.length"
                    class="clear-icon"
                    @click.stop="clearSearchHistory"
                >
                  <Delete />
                </el-icon>
              </div>

              <div v-if="!searchHistory.length" class="empty-history">
                暂无搜索记录
              </div>

              <div v-else class="history-list">
                <!-- 修改：重构历史记录项结构，添加删除按钮 -->
                <div
                    class="history-item"
                    v-for="(item, index) in searchHistory"
                    :key="index"
                >
                  <span class="history-text" @click.stop="handleHistoryItemClick(item)">
                    {{ item }}
                  </span>
                  <el-icon
                      class="single-delete-icon"
                      @click.stop="deleteSingleHistory(item)"
                  >
                    <Close />
                  </el-icon>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="nav-right">
          <el-menu-item index="order"><el-icon><Tickets /></el-icon>订单</el-menu-item>
          <el-menu-item index="/homes/notice"><el-icon><Bell /></el-icon>消息</el-menu-item>

          <div class="publish-btn" @click="handlePublishClick">
            <el-icon><Plus /></el-icon> 发布商品
          </div>

          <el-dropdown placement="bottom-end" @command="handleCommand">
            <div class="user-avatar-wrapper" @click="tokenStore.token === '' ? router.push('/homes/login') : null">
              <el-avatar :size="36" :src="userInfoStore.info.userPic ? userInfoStore.info.userPic : userlogin"/>
            </div>
            <template v-if="tokenStore.token !== ''" #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logined" :icon="User">个人主页</el-dropdown-item>
                <el-dropdown-item command="address" :icon="Location">收货地址</el-dropdown-item>
                <el-dropdown-item command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-menu>

      <el-main style="padding: 0;margin: 0;">
        <Loading />
        <router-view v-slot="{ Component, loading, route: currentRoute }">
          <template v-if="loading">
            <Loading />
          </template>
          <template v-else-if="currentRoute?.meta?.keepAlive === false">
            <component :is="Component" :key="currentRoute.fullPath" />
          </template>
          <template v-else>
            <keep-alive>
              <component :is="Component" />
            </keep-alive>
          </template>
        </router-view>
      </el-main>
    </div>
  </el-container>

  <el-drawer v-model="drawer" :with-header="true" size="50%" direction="ltr" :show-close="false" class="drawer-panel">
    <template #header>
      <span style="font-size: 1.2rem">导航</span>
      <el-button :icon="Close" style="background: none;font-size: 1.5rem;width: 30px;border: none" @click="drawer = false"/>
    </template>
    <template #default>
      <MoveMenu @update:closeDrawer="drawer = false"/>
    </template>
  </el-drawer>

  <!-- 浮动操作面板：AI 智能助手 + 回顶部 -->
  <div class="float-panel no-select" :class="{ 'is-expanded': showBackTop }">
    <div class="float-btn ai-btn" @click="goToAIChat" title="AI智能助手">
      <el-icon :size="22"><ChatDotRound /></el-icon>
      <span class="btn-label">AI</span>
    </div>
    <div class="backtop-section">
      <div class="panel-divider"></div>
      <div class="float-btn" @click="scrollToTop" title="返回顶部">
        <el-icon :size="22"><Top /></el-icon>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.float-panel {
  position: fixed;
  right: 30px;
  bottom: 120px;
  z-index: 1000;
  width: 50px;
  height: 50px;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: height 0.35s cubic-bezier(0.4, 0, 0.2, 1),
              box-shadow 0.3s ease;

  &.is-expanded {
    height: 101px;
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.13);
  }

  .float-btn {
    width: 50px;
    height: 50px;
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.2s ease;
    color: #606266;
    gap: 2px;

    &:hover {
      color: #409eff;
      background-color: #f5f7fa;
    }

    .btn-label {
      font-size: 10px;
      line-height: 1;
      font-weight: 600;
    }
  }

  .ai-btn {
    border-radius: 12px 12px 0 0;
    width: 100%;
  }

  .backtop-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 100%;
    flex-shrink: 0;
    opacity: 0;
    transform: translateY(-8px);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    pointer-events: none;
  }

  &.is-expanded .backtop-section {
    opacity: 1;
    transform: translateY(0);
    pointer-events: auto;
  }

  .panel-divider {
    width: 24px;
    height: 1px;
    background: #ebeef5;
    flex-shrink: 0;
  }
}

.common-layout { height: 100%; width: 100%; }

.nav-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: var(--nav-bg);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border);
  border-right: none;
  position: fixed;
  top: 0; left: 0; right: 0;
  height: 56px;
  padding: 0 20px;
  z-index: 1000;
  transition: transform 0.3s ease;

  .menu-icon { display: none; }

  .nav-left {
    display: flex;
    align-items: center;
    flex-shrink: 0;

    .logo {
      display: flex;
      align-items: center;
      font-size: 18px;
      font-weight: bold;
      cursor: pointer;
      color: var(--text);
      margin-right: 20px;
      white-space: nowrap;
    }
  }

  .nav-center {
    flex-grow: 1;
    display: flex;
    justify-content: center;
    padding: 0 20px;
    .search-container {
      width: 100%;
      max-width: 400px;
    }
  }

  .nav-right {
    display: flex;
    align-items: center;
    flex-shrink: 0;
    gap: 15px;
  }

  :deep(.el-menu-item) {
    height: 56px;
    line-height: 56px;
    padding: 0 15px;
    border-bottom: none !important;
    background-color: transparent !important;
    position: relative;
    color: var(--text) !important;

    &::after {
      content: '';
      position: absolute;
      bottom: 0; left: 50%;
      width: 0; height: 2px;
      background-color: #ff5d7d;
      transition: all 0.3s ease;
      transform: translateX(-50%);
    }

    &:hover, &.is-active {
      background-color: transparent !important;
    }

    &:hover::after, &.is-active::after {
      width: 80%;
    }
  }

  .publish-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #ff5e78;
    color: white;
    padding: 0 16px;
    height: 36px;
    border-radius: 18px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.3s ease;
    white-space: nowrap;

    .el-icon {
      margin-right: 4px;
    }

    &:hover {
      background-color: #ff4765;
      transform: translateY(-2px);
      box-shadow: 0 4px 8px rgba(255, 94, 120, 0.3);
    }
  }

  .user-avatar-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 2px;
    border-radius: 50%;
    cursor: pointer;
    transition: all 0.3s ease;
    border: 2px solid transparent;

    &:hover {
      border-color: #ff5e78;
      transform: translateY(-2px);
    }

    .el-avatar {
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
  }

  @media screen and (max-width: 1200px) {
    padding: 0 10px;
    .nav-left .el-menu-item,
    .nav-center,
    .nav-right .el-menu-item {
      display: none !important;
    }
    .menu-icon { display: block; font-size: 24px; margin-right: 15px; cursor: pointer; color: var(--text); }
  }
}

.el-menu--horizontal .el-menu-item:not(.is-disabled):focus,
.el-menu--horizontal .el-menu-item:not(.is-disabled):hover {
  background-color: transparent !important;
  outline: none;
}


.search-container {
  position: relative;
  width: 100%;
  outline: none;
  display: flex; // 改为flex布局，让输入框和按钮并排
  align-items: center;
  gap: 8px; // 输入框和按钮之间的间距
}

.custom-search-input {
  display: flex;
  align-items: center;
  width: 100%;
  height: 40px;
  background-color: #f5f7fa;
  border-radius: 20px;
  padding: 0 16px;
  transition: all 0.3s ease;
  border: 1px solid transparent;
  flex: 1; // 让输入框占据剩余空间

  &:hover {
    background-color: #eff1f5;
  }

  &:focus-within {
    border-color: #ff5e78;
    background-color: #fff;
    box-shadow: 0 0 0 2px rgba(255, 94, 120, 0.1);
  }

  .search-icon {
    color: #909399;
    font-size: 16px;
    margin-right: 8px;
    flex-shrink: 0;
  }

  .search-input-field {
    flex: 1;
    height: 100%;
    border: none;
    outline: none;
    background: transparent;
    font-size: 14px;
    color: #303133;
    caret-color: #ff5e78;

    &::placeholder {
      color: #c0c4cc;
    }
  }

  .clear-icon {
    color: #909399;
    font-size: 14px;
    cursor: pointer;
    flex-shrink: 0;
    &:hover {
      color: #606266;
    }
  }
}

// 新增：AI 智能搜索开关样式
.ai-search-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f0f2f5;
  color: #999;
  border: 1px solid #e4e7ed;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 18px;

  &:hover {
    background: #e8f4ff;
    border-color: #667eea;
    color: #667eea;
  }

  &.active {
    background: linear-gradient(135deg, #667eea, #764ba2);
    border-color: #667eea;
    color: #fff;
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.4);
  }
}

// 新增：搜索按钮样式
.search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background-color: #ffd21e; // 黄色背景
  color: #333;
  border: none;
  border-radius: 20px;
  padding: 0 16px;
  height: 40px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;

  &:hover {
    background-color: #ffc800;
    transform: translateY(-1px);
    box-shadow: 0 2px 6px rgba(255, 210, 30, 0.4);
  }

  .el-icon {
    font-size: 16px;
  }
}

// 适配移动端：在小屏幕下隐藏按钮文字，只保留图标
@media screen and (max-width: 768px) {
  .search-btn span {
    display: none;
  }
  .search-btn {
    padding: 0 12px;
  }
}

.search-history-panel {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  width: 100%;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  padding: 12px;
  z-index: 9999;
  max-height: 300px;
  box-sizing: border-box;
  overflow: hidden;

  .history-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 8px;
    border-bottom: 1px solid #f2f2f2;
    margin-bottom: 8px;

    span {
      font-size: 14px;
      color: #606266;
      font-weight: 500;
    }

    .clear-icon {
      font-size: 16px;
      color: #909399;
      cursor: pointer;
      &:hover {
        color: #ff5e78;
      }
    }
  }

  .empty-history {
    padding: 20px 0;
    text-align: center;
    font-size: 14px;
    color: #909399;
  }

  .history-list {
    .history-item {
      // 修改：改为flex布局，实现文字和删除按钮并排
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 10px;
      font-size: 14px;
      color: #303133;
      cursor: pointer;
      border-radius: 4px;
      transition: background-color 0.2s ease;

      .history-text {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      // 单条删除按钮样式
      .single-delete-icon {
        color: #909399;
        font-size: 12px;
        cursor: pointer;
        margin-left: 8px;
        opacity: 0; // 默认隐藏
        transition: all 0.2s ease;

        &:hover {
          color: #ff5e78;
        }
      }

      &:hover {
        background-color: #f5f7fa;

        .history-text {
          color: #ff5e78;
        }

        .single-delete-icon {
          opacity: 1; // hover时显示
        }
      }
    }
  }
}
</style>
