<script lang="js" setup>
import {
  Management,
  UserFilled,
  User,
  Crop,
  EditPen,
  SwitchButton,
  CaretBottom, ChatLineRound,
  Coin, Histogram, Grid, ChatLineSquare, Link, PictureFilled, Postcard, HelpFilled, ShoppingBag,
  InfoFilled,
  Expand,
  Fold
} from '@element-plus/icons-vue'
import fsy from '@/assets/fsy.png'
import {userInfoServices} from '@/api/user.js'
import useUserInfoStore from '@/stores/userInfo.js'
import {useTokenStore} from '@/stores/token.js'
import {watch, ref, onMounted} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import {ElMessage, ElMessageBox} from "element-plus";
import { useTagsViewStore } from '@/stores/tagsView.js'
import Loading from '@/components/Loading.vue'
import { useLoadingStore } from '@/stores/loading'
import { getSwaggerUrl } from '@/utils/serviceUrls.js'

// ========== 左侧导航栏折叠状态 ==========
const isCollapse = ref(false)

// ========== 切换导航栏折叠状态 ==========
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const route = useRoute()
const router = useRouter()
// ========== 核心优化：统一管理激活的导航项（包含关于系统） ==========
const activeNavKey = ref('system') // 可选值：blog/trade/about/system/home/api

// ========== 导航类型和菜单列表 ==========
const blogMenuList = [
  { index: '/user/manage', icon: 'UserFilled', label: '用户管理' },
  { index: '/roles/manage', icon: 'User', label: '角色管理' },
  { index: '/permission/manage', icon: 'Coin', label: '权限管理' },
  {
    index: '1',
    icon: 'Histogram',
    label: '组件',
    children: [
      { index: '/calendar', icon: 'Grid', label: '日历' }
    ]
  },
  {
    index: '2',
    icon: 'UserFilled',
    label: '个人中心',
    children: [
      { index: '/user/info', icon: 'User', label: '基本资料' },
      { index: '/user/avatar', icon: 'Crop', label: '更换头像' },
      { index: '/user/resetPassword', icon: 'EditPen', label: '重置密码' }
    ]
  }
]

// 交易后台菜单列表
const tradeMenuList = [
  { index: '/trade/statistics', icon: 'Histogram', label: '仪表盘' },
  { index: '/trade/category', icon: 'Grid', label: '商品分类' },
  { index: '/trade/order', icon: 'ShoppingBag', label: '订单管理' },
  {
    index: '5',
    icon: 'Management',
    label: '商品交易',
    children: [
      { index: '/trade/goods/list', icon: 'Grid', label: '交易商品列表' },
      { index: '/trade/goods/audit', icon: 'EditPen', label: '商品发布' }
    ]
  },
  { index: '/trade/address', icon: 'Crop', label: '地址管理'},
  { index: '/trade/comment/manage', icon: 'ChatLineRound', label: '评论管理' },
]

// ========== 优化：统一的导航切换方法 ==========
const switchNav = (key) => {
  if(key!=='api'){
    activeNavKey.value = key
  }
  // 根据不同导航项跳转对应路由
  switch(key) {
    case 'system':
      router.push(blogMenuList[0].index)
      break
    case 'trade':
      router.push(tradeMenuList[0].index)
      break
    case 'about/system':
      router.push('/about/system')
      break
    case 'home':
      router.push('/')
      break
    case 'api':
      openApiDoc()
      break
  }
}

// 打开API文档
const openApiDoc = () => {
  window.open("http://localhost:5173/api/backAll/swagger-ui/index.html", '_blank')
}

// ========== 原有逻辑 ==========
const tokenStore = useTokenStore();
const userInfoStore = useUserInfoStore();
const getUserInfo = async () => {
  try { // 添加异常处理
    let result = await userInfoServices();
    userInfoStore.setInfo(result.data);
  } catch (error) {
    console.error('获取用户信息失败：', error)
  }
}
getUserInfo();

const handleCommand = (command) => {
  if (command === 'logout') {
    ElMessageBox.confirm(
        '你确认要退出登录吗?',
        '温馨提示',
        {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )
        .then(async () => {
          tokenStore.removeToken()
          userInfoStore.removeInfo()
          if ('caches' in window) {
            caches.keys().then((cacheNames) => {
              cacheNames.forEach((cacheName) => {
                caches.delete(cacheName)
              })
            })
          }
          await router.push('/')
          ElMessage({
            type: 'success',
            message: '退出登录成功'
          })
        })
        .catch(() => {
          ElMessage({
            type: 'info',
            message: '用户取消了退出登录'
          })
        })
  } else {
    router.push('/user/' + command)
  }
}

const tagsViewStore = useTagsViewStore()
// 优化：标签栏逻辑（排除无布局页面）
watch(
    () => router.currentRoute.value,
    (newRoute) => {
      // 更新激活的导航项
      if (newRoute.path.startsWith('/article') || newRoute.path.startsWith('/tag') ||
          newRoute.path.startsWith('/user/manage') || newRoute.path.startsWith('/roles') ||
          newRoute.path.startsWith('/permission') || newRoute.path.startsWith('/comment') ||
          newRoute.path.startsWith('/treeholes') || newRoute.path.startsWith('/message') ||
          newRoute.path.startsWith('/friendLinks') || newRoute.path.startsWith('/photo') ||
          newRoute.path.startsWith('/calendar') || newRoute.path.startsWith('/user/info')) {
        activeNavKey.value = 'system'
      } else if (newRoute.path.startsWith('/trade')) {
        activeNavKey.value = 'trade'
      } else if (newRoute.path === '/about/system') {
        activeNavKey.value = 'about/system'
      } else if (newRoute.path === '/') {
        activeNavKey.value = 'home'
      }

      // 只有非无布局、非首页的页面才添加到标签栏
      if (newRoute.path !== '/' && !newRoute.meta.noLayout) {
        tagsViewStore.setTagsItem({
          title: newRoute.meta.title || newRoute.name,
          path: newRoute.path,
          name: newRoute.name
        })
      }
    },
    { immediate: true }
)

const handleClose = (path) => {
  const index = tagsViewStore.tagsList.findIndex(tag => tag.path === path)
  if (index === -1) return
  if (tagsViewStore.tagsList.length <= 1) {
    ElMessage.warning('这是最后一个标签页，不能关闭！')
    return
  }
  if (path === router.currentRoute.value.path) {
    const prevTag = tagsViewStore.tagsList[index - 1]
    if (prevTag) {
      router.push(prevTag.path)
    } else {
      const nextTag = tagsViewStore.tagsList[index + 1]
      if (nextTag) {
        router.push(nextTag.path)
      }
    }
  }
  tagsViewStore.delTagsItem(index)
}

const goToTag = (path) => {
  router.push(path)
}

const loadingStore = useLoadingStore()
watch(
    () => router.currentRoute.value,
    (newRoute, oldRoute) => {
      if (newRoute.path !== oldRoute?.path) {
        loadingStore.setLoading(true)
      }
      const timer = setTimeout(() => {
        loadingStore.setLoading(false)
        clearTimeout(timer)
      }, 300)
    },
    { immediate: true }
)

// 获取图标组件
const getIconComponent = (iconName) => {
  const iconMap = {
    Management, UserFilled, User, Crop, EditPen,
    Coin, Histogram, Grid, HelpFilled, ChatLineRound, ChatLineSquare,
    Link, PictureFilled, Postcard, ShoppingBag, CaretBottom, InfoFilled
  }
  return iconMap[iconName] || Grid
}

// 页面挂载时初始化激活状态
onMounted(() => {
  if (route.path === '/about/system') {
    activeNavKey.value = 'about/system'
  } else if (route.path.startsWith('/trade')) {
    activeNavKey.value = 'trade'
  } else if (route.path === '/') {
    activeNavKey.value = 'home'
  }
})
</script>

<template>
  <el-header class="header-container">
    <div class="header-left">
      <div class="collapse-trigger" @click="toggleCollapse" title="展开/收起侧边栏">
        <el-icon size="20">
          <component :is="isCollapse ? Expand : Fold" />
        </el-icon>
      </div>

      <div class="header-logo">交易后台</div>

      <nav class="header-nav">
        <!-- 关于系统（优化：添加激活样式） -->
        <a
            class="nav-link"
            :class="{ active: activeNavKey === 'about/system' }"
            @click.prevent="switchNav('about/system')"
        >
          <el-icon class="nav-icon"><InfoFilled /></el-icon>
          <span class="nav-text">关于系统</span>
        </a>

        <!-- 系统管理 -->
        <a
            class="nav-link"
            :class="{ active: activeNavKey === 'system' }"
            @click.prevent="switchNav('system')"
        >
          <el-icon class="nav-icon"><Management /></el-icon>
          <span class="nav-text">系统管理</span>
        </a>

        <!-- 交易后台 -->
        <a
            class="nav-link"
            :class="{ active: activeNavKey === 'trade' }"
            @click.prevent="switchNav('trade')"
        >
          <el-icon class="nav-icon"><ShoppingBag /></el-icon>
          <span class="nav-text">网站管理</span>
        </a>

        <!-- API接口文档 -->
        <a
            class="nav-link"
            :class="{ active: activeNavKey === 'api' }"
            @click.prevent="switchNav('api')"
        >
          <el-icon class="nav-icon"><Link /></el-icon>
          <span class="nav-text">API接口文档</span>
        </a>
        <!-- 前台首页 -->
        <a
            class="nav-link"
            :class="{ active: activeNavKey === 'home' }"
            @click.prevent="switchNav('home')"
        >
          <el-icon class="nav-icon"><Grid /></el-icon>
          <span class="nav-text">前台首页</span>
        </a>
      </nav>
    </div>

    <div class="header-user-area">
      <strong class="username">{{ userInfoStore.info.username }}</strong>
      <el-dropdown placement="bottom-end" @command="handleCommand" class="user-dropdown">
        <span class="el-dropdown__box" role="button" tabindex="0">
          <el-avatar
              :src="userInfoStore.info.userPic ? userInfoStore.info.userPic : fsy"
              size="small"
          />
          <el-icon class="caret-icon"><CaretBottom/></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="info" :icon="User">基本资料</el-dropdown-item>
            <el-dropdown-item command="avatar" :icon="Crop">更换头像</el-dropdown-item>
            <el-dropdown-item command="resetPassword" :icon="EditPen">重置密码</el-dropdown-item>
            <el-dropdown-item command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-header>

  <el-container class="layout-container" v-if="!route.meta.noLayout">
    <el-aside :width="isCollapse ? '64px' : '200px'" class="aside-container" transition="width">
      <div class="el-aside__logo"></div>
      <el-menu
          active-text-color="#2129ff"
          background-color="#fff"
          text-color="black"
          router
          :collapse="isCollapse"
          :collapse-transition="true"
      >
        <template v-if="activeNavKey === 'system'">
          <template v-for="menu in blogMenuList" :key="menu.index">
            <el-menu-item v-if="!menu.children" :index="menu.index">
              <el-icon><component :is="getIconComponent(menu.icon)" /></el-icon>
              <template #title>{{ menu.label }}</template>
            </el-menu-item>
            <el-sub-menu v-else :index="menu.index">
              <template #title>
                <el-icon><component :is="getIconComponent(menu.icon)" /></el-icon>
                <span>{{ menu.label }}</span>
              </template>
              <el-menu-item
                  v-for="child in menu.children"
                  :key="child.index"
                  :index="child.index"
              >
                <el-icon><component :is="getIconComponent(child.icon)" /></el-icon>
                <template #title>{{ child.label }}</template>
              </el-menu-item>
            </el-sub-menu>
          </template>
        </template>

        <template v-if="activeNavKey === 'trade'">
          <template v-for="menu in tradeMenuList" :key="menu.index">
            <el-menu-item v-if="!menu.children" :index="menu.index">
              <el-icon><component :is="getIconComponent(menu.icon)" /></el-icon>
              <template #title>{{ menu.label }}</template>
            </el-menu-item>
            <el-sub-menu v-else :index="menu.index">
              <template #title>
                <el-icon><component :is="getIconComponent(menu.icon)" /></el-icon>
                <span>{{ menu.label }}</span>
              </template>
              <el-menu-item
                  v-for="child in menu.children"
                  :key="child.index"
                  :index="child.index"
              >
                <el-icon><component :is="getIconComponent(child.icon)" /></el-icon>
                <template #title>{{ child.label }}</template>
              </el-menu-item>
            </el-sub-menu>
          </template>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-tabs
          v-if="tagsViewStore.tagsList.length > 0"
          class="tags-container"
          v-model="router.currentRoute.value.path"
          type="card"
          closable
          @tab-click="goToTag"
          @tab-remove="handleClose"
      >
        <el-tab-pane
            v-for="(tag) in tagsViewStore.tagsList"
            :key="tag.path"
            :label="tag.title"
            :name="tag.path"
        >
          <template #label>
            <div class="tag-item" @click="goToTag(tag.path)">
              <span>{{ tag.title }}</span>
            </div>
          </template>
        </el-tab-pane>
      </el-tabs>

        <el-main>
          <Loading />
          <router-view v-slot="{ Component, route: currentRoute }">
            <Loading v-if="loadingStore.isLoading"/>
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

      <el-footer>校园二手交易后台 ©2025 Created by 不良人fsy</el-footer>
    </el-container>
  </el-container>
  <!-- 无布局页面 -->
  <div v-else class="no-layout-page">
    <router-view />
  </div>
</template>

<style lang="scss" scoped>
.header-container {
  background-color: #090b33;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #ffffff;
  padding: 0 20px !important;
  box-sizing: border-box;

  .header-left {
    display: flex;
    align-items: center;
    gap: 15px;
    height: 100%;
  }

  .collapse-trigger {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    cursor: pointer;
    border-radius: 6px;
    transition: background-color 0.3s ease;
    color: #ffffff;

    &:hover {
      background-color: rgba(255, 255, 255, 0.12);
    }

    &:active {
      background-color: rgba(255, 255, 255, 0.2);
    }
  }

  .header-logo {
    font-size: 18px;
    font-weight: 600;
    letter-spacing: 1px;
    margin-right: 15px;
    white-space: nowrap;
  }

  .header-nav {
    display: flex;
    align-items: center;
    height: 100%;
  }

  .nav-link {
    display: flex;
    align-items: center;
    height: 50px;
    padding: 0 16px;
    color: #ffffff;
    text-decoration: none;
    transition: background-color 0.3s ease;
    font-size: 14px;
    position: relative;
    box-sizing: border-box;
    border: none;

    &:hover {
      background-color: rgba(255, 255, 255, 0.08);
      cursor: pointer;
    }

    &.active {
      background-color: rgba(255, 255, 255, 0.12);
      color: #409eff;
    }

    .nav-icon {
      font-size: 14px;
      margin-right: 8px;
    }

    .nav-text {
      font-size: 14px;
      font-weight: 500;
    }
  }

  .nav-link:active {
    background-color: rgba(255, 255, 255, 0.12);
  }

  .header-user-area {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .username {
    font-size: 14px;
    font-weight: 500;
    white-space: nowrap;
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .user-dropdown {
    display: flex;
    align-items: center;

    .el-dropdown__box {
      display: flex;
      align-items: center;
      padding: 2px 6px;
      border-radius: 20px;
      transition: background-color 0.2s;
      cursor: pointer;

      &:hover {
        background-color: rgba(255, 255, 255, 0.1);
      }

      .el-avatar {
        width: 32px !important;
        height: 32px !important;
        border: 1px solid rgba(255, 255, 255, 0.3);
      }

      .caret-icon {
        color: #f1ebeb;
        margin-left: 6px;
        font-size: 14px;
      }
    }

    .el-dropdown-menu {
      min-width: 140px;
      background-color: #111449 !important;
      border: 1px solid rgba(255, 255, 255, 0.1) !important;
      color: #ffffff !important;

      .el-dropdown-item {
        color: #ffffff !important;
        font-size: 13px;
        padding: 6px 16px !important;

        &:hover {
          background-color: rgba(255, 255, 255, 0.1) !important;
          color: #ffffff !important;
        }

        .el-icon {
          margin-right: 8px !important;
          font-size: 14px !important;
        }
      }
    }
  }
}

.tagList {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  background-color: #ffffff;
}

.layout-container {
  background-color: white;
  height: calc(100vh - 60px);

  .aside-container {
    background-color: #ffffff;
    border-right: 1px solid #e6e6e6;
    box-shadow: 1px 0 3px rgba(0, 0, 0, 0.08);
    transition: width 0.3s ease;

    .el-menu {
      border-right: none;
      height: 100%;

      .el-menu-item {
        height: 40px !important;
        line-height: 40px !important;
      }

      .el-menu-item:hover {
        color: #2129ff;
      }

      &.el-menu--collapse {
        .el-sub-menu__icon-arrow {
          display: none;
        }

        .el-menu-item__icon {
          margin-right: 0 !important;
        }
      }
    }
  }

  .el-main{
    height: calc(80vh - 36px);
    position: relative;
  }

  .el-footer {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    color: #666;
  }
}

.tags-container {
  border-bottom: 1px solid #e6e6e6;
  .tag-item {
    display: flex;
    align-items: center;
    gap: 4px;
    .close-icon {
      font-size: 12px;
      opacity: 0.7;
      &:hover {
        opacity: 1;
      }
    }
  }
  :deep(.el-tabs__nav-wrap::after)
  {
    height: 0 !important;
  }
  :deep( .el-tabs__item) {
    height: 34px;
    line-height: 34px;
    margin: 0 2px;
  }
  :deep(.el-tabs__append) {
    padding: 0 10px;
  }
}

.no-layout-page {
  min-height: calc(100vh - 50px);
  background-color: #fff;
  padding: 20px;
  box-sizing: border-box;
}
</style>
