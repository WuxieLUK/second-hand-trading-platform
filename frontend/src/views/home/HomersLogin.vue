<script setup lang="ts">
</script>

<template>
  <div style="width: auto;height: 100vh;overflow: hidden;display: flex">
    <!-- 左边区域 -->
    <div class="left-card">
      <img :style="{ width: '100%', height: '100%', objectFit: 'cover', }"
           src="../../assets/shopping.png" alt="背景图片加载中..."/>
    </div>
    <div class="welcome-title">
      <div>欢迎来到我的二手交易平台</div>
      <div style="margin-top: 10px">在这里你可以买到一切你想要的东西</div>
    </div>
    <!-- 右边区域 -->
    <div class="right-card">
      <router-view v-slot="{Component}">
        <transition name="el-fade-in-linear" mode="out-in">
          <component :is="Component"/>
        </transition>
      </router-view>
    </div>
  </div>
</template>

<style scoped lang="scss">
.left-card{
  flex: 1;
  background-color: black;
  overflow: hidden;
  /* 确保图片容器没有额外的内边距或边框 */
  padding: 0;
}

.right-card {
  width: 400px;
  z-index: 1;
  background-color: var(--el-bg-color);
  overflow: hidden;
}

@media screen and (max-width: 600px) {
  .left-card {
    display: none;
  }
  .right-card{
    width: 100vw;
  }
}

.welcome-title {
  position: absolute;
  bottom: 30px;
  left: 30px;
  color: white;
  text-shadow: 0 0 10px white;
}

.welcome-title div:first-child {
  font-size: 30px;
  font-weight: bold;
}
</style>