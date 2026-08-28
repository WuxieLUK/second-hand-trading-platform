<script setup>
</script>

<template>
  <div class="footer">
    <!-- 新增顶部分割线 -->
    <div class="footer-divider"></div>

    <div class="copyright">
      <p class="copyright-title">&copy;2020-2026 &star;宇师傅. </p>
      <p class="copyright-contact">基于springboot3+vue3</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
.footer {
  height: 300px;
  display: flex;
  flex-direction: column; /* 改为纵向排列，让分割线在上，文字在下 */
  justify-content: center;
  align-items: center; /* 替换justify-items，flex布局下用align-items更合适 */
  text-align: center;
  padding: 30px;
  background-color:#333333;
  color: #7e7e7e;
  font-family: "Microsoft YaHei", "PingFang SC", sans-serif; /* 优化字体，适配不同系统 */

  // 顶部分割线样式
  .footer-divider {
    width: 80%; /* 分割线宽度，避免全屏太突兀 */
    height: 1px; /* 分割线粗细 */
    background: linear-gradient(90deg, transparent, var(--text), transparent); /* 渐变分割线，更美观 */
    margin-bottom: 40px; /* 分割线与文字的间距 */
    opacity: 0.7; /* 降低透明度，更柔和 */
  }

  .copyright {
    // 统一设置文字行高和间距
    .copyright-title {
      margin: 0 0 15px 0; /* 标题与联系方式的间距 */
      font-size: 16px; /* 主标题字体大小 */
      font-weight: 500; /* 轻微加粗，突出主标题 */
      letter-spacing: 0.5px; /* 字间距，提升可读性 */
    }

    .copyright-contact {
      margin: 0;
      font-size: 14px; /* 联系方式字体稍小 */
      opacity: 0.8; /* 透明度降低，主次区分 */
      letter-spacing: 0.3px;
    }
  }
}

// 响应式适配，小屏幕下调整样式
@media (max-width: 768px) {
  .footer {
    height: 200px;
    padding: 20px;

    .footer-divider {
      width: 90%;
      margin-bottom: 30px;
    }

    .copyright .copyright-title {
      font-size: 14px;
    }

    .copyright .copyright-contact {
      font-size: 12px;
    }
  }
}
</style>