<script setup>
import { useRouter } from 'vue-router'
import Footer from "@/components/footer.vue"

const router = useRouter()

const goHome = () => {
  router.push('/')
}
</script>

<template>
  <div class="not-found-page">
    <div class="nf-container">
      <!-- 404 大标题 -->
      <div class="nf-header">
        <span class="nf-404">4</span>
        <div class="nf-circles">
          <div class="nf-circle nf-circle-1">
            <span class="nf-face">:(</span>
          </div>
        </div>
        <span class="nf-404">4</span>
      </div>

      <!-- 状态文案 -->
      <div class="nf-message">
        <h2 class="nf-title">页面走丢了</h2>
        <p class="nf-desc">
          你访问的页面可能已被删除、更名或暂时不可用<br>
          试试回到首页看看吧
        </p>
      </div>

      <!-- 操作按钮 -->
      <div class="nf-actions">
        <div class="nf-btn nf-btn-primary" @click="goHome">
          <el-icon style="margin-right: 6px"><HomeFilled /></el-icon>
          返回首页
        </div>
        <div class="nf-btn nf-btn-secondary" @click="router.back()">
          <el-icon style="margin-right: 6px"><ArrowLeft /></el-icon>
          返回上一页
        </div>
      </div>

      <!-- 底部装饰 -->
      <div class="nf-decoration">
        <div class="nf-dot nf-dot-1"></div>
        <div class="nf-dot nf-dot-2"></div>
        <div class="nf-dot nf-dot-3"></div>
        <div class="nf-dot nf-dot-4"></div>
      </div>
    </div>

    <Footer />
  </div>
</template>

<style scoped lang="scss">
.not-found-page {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 56px);
  background: #f0f2f5;
}

.nf-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px 80px;
  position: relative;
  overflow: hidden;
}

/* ========== 404 数字 + 表情圈 ========== */
.nf-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
  animation: nf-fade-down 0.6s ease both;
}

.nf-404 {
  font-size: 120px;
  font-weight: 900;
  line-height: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -8px;
  user-select: none;
}

.nf-circles {
  position: relative;
  width: 100px;
  height: 100px;
}

.nf-circle {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: nf-float 3s ease-in-out infinite;
  box-shadow: 0 8px 30px rgba(102, 126, 234, 0.35);
}

.nf-face {
  font-size: 36px;
  font-weight: 800;
  color: #fff;
  line-height: 1;
}

/* ========== 文案区 ========== */
.nf-message {
  text-align: center;
  margin-bottom: 40px;
  animation: nf-fade-up 0.6s 0.2s ease both;
}

.nf-title {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 12px;
}

.nf-desc {
  font-size: 15px;
  color: #909399;
  line-height: 1.8;
  margin: 0;
}

/* ========== 按钮 ========== */
.nf-actions {
  display: flex;
  gap: 16px;
  animation: nf-fade-up 0.6s 0.35s ease both;
}

.nf-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 28px;
  height: 46px;
  border-radius: 23px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  white-space: nowrap;
  border: none;
  outline: none;
}

.nf-btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.35);

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(102, 126, 234, 0.45);
  }

  &:active {
    transform: translateY(-1px);
  }
}

.nf-btn-secondary {
  background: #fff;
  color: #606266;
  border: 1.5px solid #e4e7ed;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  &:hover {
    border-color: #667eea;
    color: #667eea;
    transform: translateY(-3px);
    box-shadow: 0 6px 16px rgba(102, 126, 234, 0.12);
  }

  &:active {
    transform: translateY(-1px);
  }
}

/* ========== 装饰圆点 ========== */
.nf-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.nf-dot {
  position: absolute;
  border-radius: 50%;
  opacity: 0.12;
}

.nf-dot-1 {
  width: 300px;
  height: 300px;
  background: #667eea;
  top: -80px;
  left: -80px;
  animation: nf-dot-drift 8s ease-in-out infinite;
}

.nf-dot-2 {
  width: 200px;
  height: 200px;
  background: #764ba2;
  bottom: -40px;
  right: -40px;
  animation: nf-dot-drift 10s ease-in-out infinite reverse;
}

.nf-dot-3 {
  width: 120px;
  height: 120px;
  background: #ff5e78;
  top: 40%;
  left: 5%;
  animation: nf-dot-drift 12s ease-in-out infinite 2s;
}

.nf-dot-4 {
  width: 80px;
  height: 80px;
  background: #ffd21e;
  top: 20%;
  right: 10%;
  animation: nf-dot-drift 9s ease-in-out infinite 1s reverse;
}

/* ========== 动画 ========== */
@keyframes nf-fade-down {
  from { opacity: 0; transform: translateY(-30px); }
  to   { opacity: 1; transform: translateY(0); }
}

@keyframes nf-fade-up {
  from { opacity: 0; transform: translateY(30px); }
  to   { opacity: 1; transform: translateY(0); }
}

@keyframes nf-float {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-10px); }
}

@keyframes nf-dot-drift {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33%      { transform: translate(30px, -20px) scale(1.05); }
  66%      { transform: translate(-20px, 15px) scale(0.95); }
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .nf-container {
    padding: 40px 16px 60px;
  }

  .nf-404 {
    font-size: 72px;
  }

  .nf-circles, .nf-circle {
    width: 64px;
    height: 64px;
  }

  .nf-face {
    font-size: 24px;
  }

  .nf-header {
    gap: 10px;
  }

  .nf-title {
    font-size: 22px;
  }

  .nf-desc {
    font-size: 14px;
  }

  .nf-actions {
    flex-direction: column;
    width: 100%;
    max-width: 280px;
  }

  .nf-btn {
    width: 100%;
  }

  .nf-dot-1 { width: 180px; height: 180px; top: -60px; left: -60px; }
  .nf-dot-2 { width: 120px; height: 120px; }
  .nf-dot-3 { width: 80px; height: 80px; }
  .nf-dot-4 { width: 50px; height: 50px; }
}
</style>