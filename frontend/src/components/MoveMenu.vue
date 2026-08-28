<script setup lang="ts">
import {
  HomeFilled,
  ShoppingBag,
  Star,
  Bell,
} from "@element-plus/icons-vue";
import { useTokenStore } from "@/stores/token.js";
// 新增：导入用户信息store，与主导航保持一致
import {ElMessage} from "element-plus";
import { useRouter } from "vue-router";

const emit = defineEmits(["update:closeDrawer"]);
const tokenStore = useTokenStore();
const router = useRouter();

// 关闭抽屉
function isClose() {
  emit("update:closeDrawer");
}

// 权限校验 + 跳转 + 关闭抽屉
const handleMenuClick = (index: string) => {
  // 允许免登录访问的路由白名单（与主导航保持一致）
  const whiteList = ['/homes/home', '/','/homes/dynamic'];

  // 特殊处理订单跳转（与主导航逻辑一致）
  if (index === '/homes/myBought') {
    if (tokenStore.token === '') {
      ElMessage.warning('请登录');
      return;
    }
    const targetUrl = `${window.location.origin}/homes/myBought`;
    window.open(targetUrl, '_blank');
    isClose();
    return;
  }

  // 命中非白名单且没有 token 时拦截
  if (!whiteList.includes(index) && tokenStore.token === '') {
    ElMessage.warning('请登录');
    return;
  }

  // 鉴权通过，执行跳转并关闭抽屉
  router.push(index);
  isClose();
};

</script>

<template>
  <div class="mobile-menu-container">
    <el-menu
        style="width: 100%; border: none; height: 100%;"
        class="menu no-select"
    >
      <!-- 基础导航（路径与主导航完全一致） -->
      <el-menu-item index="/homes/home" @click="handleMenuClick('/homes/home')">
        <el-icon><HomeFilled /></el-icon>
        商品
      </el-menu-item>

      <!-- 需要登录的菜单（统一路径格式为 /homes/xxx） -->
      <el-menu-item index="/homes/mygoods" @click="handleMenuClick('/homes/mygoods')">
        <el-icon><ShoppingBag /></el-icon>
        我的商品
      </el-menu-item>

      <el-menu-item index="/homes/mycollect" @click="handleMenuClick('/homes/mycollect')">
        <el-icon><Star /></el-icon>
        我的收藏
      </el-menu-item>

      <!-- 通知菜单（路径与主导航一致） -->
      <el-menu-item index="/homes/notice" @click="handleMenuClick('/homes/notice')">
        <el-icon><Bell /></el-icon>
        信息
      </el-menu-item>
    </el-menu>
  </div>
</template>

<style scoped lang="scss">
.mobile-menu-container {
  height: 100%;
}

.menu {
  --el-menu-text-color: #303133;
  --el-menu-active-text-color: #ff5e78;
  --el-menu-hover-text-color: #ff5e78;

  .el-menu-item {
    height: 50px;
    line-height: 50px;
    margin: 0 10px;
    border-radius: 8px;


    &:hover {
      background-color: #f5f5f5;
    }
  }

  // 发布商品按钮特殊样式
  .publish-item {
    background-color: #ff5e78 !important;
    color: white !important;

    &:hover {
      background-color: #ff4765 !important;
    }

    .el-icon {
      color: white !important;
    }
  }

  .el-sub-menu {
    .el-sub-menu__title {
      height: 50px;
      line-height: 50px;
      margin: 0 10px;
      border-radius: 8px;

      &:hover {
        background-color: #f5f5f5;
      }
    }

    .el-menu-item {
      margin-left: 20px;
    }
  }
}
</style>