<!-- src/components/LoadMoreSpinner.vue -->
<template>
  <div class="load-more-spinner">
    <div class="spinner"></div>
    <span class="loading-text">正在加载更多宝贝...</span>
  </div>
</template>

<style scoped>
.load-more-spinner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 15px 0;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #ffd100;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.loading-text {
  color: #999;
  font-size: 14px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>