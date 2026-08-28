<script setup lang="js">
import { computed, onMounted, ref, watch } from 'vue';
import { Connection, Search, Setting, ShoppingBag, Star, User } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import Footer from "@/components/footer.vue";
import { userInfoServices } from "@/api/user.js";
import useUserInfoStore from "@/stores/userInfo.js";
import { useTokenStore } from '@/stores/token.js';

const userInfoStore = useUserInfoStore();
const tokenStore = useTokenStore();
const router = useRouter();
const route = useRoute();

const activeMenu = ref('home');
const quickQueryType = ref('bought');
const quickQueryKeyword = ref('');

const quickQueryPlaceholder = computed(() => {
  return quickQueryType.value === 'bought'
    ? '输入订单编号，查询我买到的订单'
    : '输入订单编号，查询我卖出的订单';
});

const getUserInfo = async () => {
  try {
    const result = await userInfoServices();
    userInfoStore.setInfo(result.data);
  } catch (error) {
    console.error('获取用户信息失败:', error);
  }
};

const updateActiveMenu = (fullPath) => {
  if (fullPath === '/homes/logined' || fullPath === '/homes/mycenter') {
    activeMenu.value = 'home';
  } else if (fullPath.startsWith('/homes/mygoods')) {
    activeMenu.value = 'published';
  } else if (fullPath.startsWith('/homes/mySold')) {
    activeMenu.value = 'sold';
  } else if (fullPath.startsWith('/homes/myBought')) {
    activeMenu.value = 'bought';
  } else if (fullPath.startsWith('/homes/mycollect')) {
    activeMenu.value = 'collect';
  } else if (fullPath.startsWith('/homes/userinfo')) {
    activeMenu.value = 'profile';
  } else if (fullPath.startsWith('/homes/resetPassword')) {
    activeMenu.value = 'security';
  }
};

watch(() => route.fullPath, (fullPath) => {
  updateActiveMenu(fullPath);
}, { immediate: true });

onMounted(() => {
  getUserInfo();
});

const handleLogout = () => {
  ElMessageBox.confirm('确认要退出当前账号吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    tokenStore.removeToken();
    userInfoStore.removeInfo();
    await router.push('/');
    ElMessage.success('已安全退出');
  }).catch(() => {});
};

const handleMenuClick = (index) => {
  const pathMap = {
    home: '/homes/mycenter',
    published: '/homes/mygoods',
    sold: '/homes/mySold',
    bought: '/homes/myBought',
    collect: '/homes/mycollect',
    profile: '/homes/userinfo',
    security: '/homes/resetPassword'
  };

  if (index === 'logout') {
    handleLogout();
    return;
  }

  const targetPath = pathMap[index];
  if (targetPath && route.path !== targetPath) {
    router.push(targetPath).catch((error) => {
      if (error?.name !== 'NavigationDuplicated') {
        console.error('路由跳转失败:', error);
      }
    });
  }
};

const handleQuickQuery = () => {
  const keyword = quickQueryKeyword.value.trim();
  if (!keyword) {
    ElMessage.warning('请输入订单编号');
    return;
  }

  const targetPath = quickQueryType.value === 'bought' ? '/homes/myBought' : '/homes/mySold';
  router.push({
    path: targetPath,
    query: {
      keyword
    }
  });
};
</script>

<template>
  <div class="xianyu-wrapper">
    <div class="xianyu-container">
      <aside class="xianyu-sidebar">
        <el-menu
          :model-value="activeMenu"
          @select="handleMenuClick"
          class="xianyu-menu"
          :default-active="activeMenu"
          :ellipsis="false"
        >
          <el-menu-item index="home">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </el-menu-item>

          <el-sub-menu index="trade">
            <template #title>
              <el-icon><ShoppingBag /></el-icon>
              <span>我的交易</span>
            </template>
            <el-menu-item index="published">我发布的</el-menu-item>
            <el-menu-item index="sold">我卖出的</el-menu-item>
            <el-menu-item index="bought">我买到的</el-menu-item>
          </el-sub-menu>

          <el-menu-item index="collect">
            <el-icon><Star /></el-icon>
            <span>我的收藏</span>
          </el-menu-item>

          <el-sub-menu index="account">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>账户设置</span>
            </template>
            <el-menu-item index="profile">个人资料</el-menu-item>
            <el-menu-item index="security">账号与安全</el-menu-item>
          </el-sub-menu>

          <el-menu-item index="logout" class="logout-item">
            <el-icon><Connection /></el-icon>
            <span>退出登录</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <main class="xianyu-main">
        <div class="query-panel">
          <div class="query-panel__card">
            <div class="query-panel__title">订单查询</div>
            <div class="query-panel__form">
              <el-radio-group v-model="quickQueryType" class="query-panel__type">
                <el-radio-button label="bought">我买到的</el-radio-button>
                <el-radio-button label="sold">我卖出的</el-radio-button>
              </el-radio-group>
              <el-input
                v-model="quickQueryKeyword"
                clearable
                :placeholder="quickQueryPlaceholder"
                @keyup.enter="handleQuickQuery"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-button type="warning" @click="handleQuickQuery">立即查询</el-button>
            </div>
          </div>
        </div>

        <router-view v-slot="{ Component, route: currentRoute }">
          <transition name="slide-fade" mode="out-in">
            <keep-alive>
              <component :is="Component" :key="currentRoute.fullPath" />
            </keep-alive>
          </transition>
        </router-view>
      </main>
    </div>
  </div>
  <Footer />
</template>

<style lang="scss" scoped>
.xianyu-wrapper {
  background-color: #f6f6f6;
  min-height: 100vh;
  padding: 20px 0;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}

.xianyu-container {
  width: 1500px;
  margin: 30px auto;
  display: flex;
  gap: 20px;
}

.xianyu-sidebar {
  width: 180px;
  background: #fff;
  border-radius: 12px;
  padding: 15px 0;
  height: fit-content;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  box-sizing: border-box;

  :deep(.xianyu-menu) {
    border-right: none;
    background: transparent;

    .el-menu-item {
      height: auto;
      padding: 12px 20px;
      line-height: normal;
      font-size: 14px;
      color: #333;
      margin: 0;
      border-left: 4px solid transparent;
      transition: all 0.3s ease;

      &:hover {
        background-color: #f8f8f8;
        color: #ffda00;
      }

      &.is-active {
        background: #f8f8f8;
        font-weight: bold;
        color: #ffda00;
        border-left-color: #ffda00;
      }

      .el-icon {
        font-size: 18px;
        color: #666;
        margin-right: 10px;
        transition: color 0.3s ease;
      }

      &:hover .el-icon,
      &.is-active .el-icon {
        color: #ffda00;
      }
    }

    .el-sub-menu {
      .el-sub-menu__title {
        height: auto;
        padding: 12px 20px;
        line-height: normal;
        font-size: 14px;
        color: #333;
        border-left: 4px solid transparent;
        transition: all 0.3s ease;

        &:hover {
          background-color: #f8f8f8;
          color: #ffda00;
        }

        .el-icon {
          font-size: 18px;
          color: #666;
          margin-right: 10px;
          transition: color 0.3s ease;
        }

        &:hover .el-icon {
          color: #ffda00;
        }
      }

      .el-menu-item {
        padding: 8px 20px 8px 48px;
        font-size: 13px;
        color: #666;
        border-left: none;

        &:hover {
          color: #ffda00;
          background-color: #fafafa;
        }

        &.is-active {
          color: #ffda00;
          font-weight: 500;
          background-color: transparent;
          border-left: none;
        }
      }
    }

    .logout-item {
      color: #ff4d4f !important;
      margin-top: 20px;

      .el-icon {
        color: #ff4d4f !important;
      }

      &:hover {
        background-color: #fff2f0 !important;
        color: #ff4d4f !important;
      }
    }
  }
}

.xianyu-main {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  min-height: 600px;
  box-sizing: border-box;
}

.query-panel {
  padding: 20px 24px 0;

  .query-panel__card {
    padding: 18px 20px;
    border-radius: 18px;
    background: linear-gradient(135deg, #fff8df 0%, #fff1c7 100%);
    border: 1px solid #f4dfb3;
  }

  .query-panel__title {
    font-size: 16px;
    font-weight: 700;
    color: #4f3b00;
    margin-bottom: 14px;
  }

  .query-panel__form {
    display: grid;
    grid-template-columns: auto 1fr auto;
    gap: 12px;
    align-items: center;
  }

  .query-panel__type {
    white-space: nowrap;
  }
}

:deep(.slide-fade-enter-from) {
  opacity: 0;
  transform: translateX(10px);
}

:deep(.slide-fade-enter-active) {
  transition: all 0.3s ease;
}

:deep(.slide-fade-leave-to) {
  opacity: 0;
  transform: translateX(-10px);
}

:deep(.slide-fade-leave-active) {
  transition: all 0.2s ease;
}

@media screen and (max-width: 1560px) {
  .xianyu-container {
    width: calc(100% - 40px);
  }
}

@media screen and (max-width: 1024px) {
  .xianyu-container {
    gap: 15px;
  }

  .xianyu-sidebar {
    width: 150px;
  }

  .query-panel {
    .query-panel__form {
      grid-template-columns: 1fr;
    }
  }
}

@media screen and (max-width: 768px) {
  .xianyu-wrapper {
    padding: 10px 0;
  }

  .xianyu-container {
    width: 100%;
    padding: 0 15px;
    margin: 10px auto;
    flex-direction: column;
    gap: 15px;
    box-sizing: border-box;
  }

  .xianyu-sidebar {
    width: 100%;
    padding: 10px 0;

    :deep(.xianyu-menu) {
      .el-menu-item,
      .el-sub-menu__title {
        padding: 10px 20px;
      }

      .el-sub-menu .el-menu-item {
        padding: 8px 20px 8px 48px;
      }

      .logout-item {
        margin-top: 10px;
        border-top: 1px solid #f0f0f0;
      }
    }
  }

  .xianyu-main {
    min-height: 500px;
    width: 100%;
  }

  .query-panel {
    padding: 15px 15px 0;
  }
}

@media screen and (max-width: 480px) {
  .xianyu-container {
    padding: 0 10px;
  }

  .xianyu-sidebar {
    :deep(.xianyu-menu) {
      .el-menu-item,
      .el-sub-menu__title {
        padding: 10px 15px;
      }

      .el-sub-menu .el-menu-item {
        padding: 8px 15px 8px 40px;
      }
    }
  }
}
</style>
