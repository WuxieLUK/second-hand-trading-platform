<template>
  <svg
      :class="svgClass"
      aria-hidden="true"
      @click="handleClick"
  >
    <use :href="iconName"></use>
  </svg>
</template>

<script setup lang="js">
import { ref, computed } from 'vue';

const props = defineProps({
  iconClass: {
    type: String,
    required: true
  },
  className: {
    type: String,
    default: ''
  }
});

// 追踪点击状态
const isClicked = ref(false);

// 图标名称计算属性
const iconName = computed(() => `#${props.iconClass}`);

// 点击处理函数
const handleClick = () => {
  isClicked.value = !isClicked.value;
};

const svgClass = computed(() => {
  return props.className
      ? `${props.className}`
      : 'svg-icon';
});
</script>

<style scoped lang="scss">
.svg-icon {
  fill: gray; // 默认颜色
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  &:hover {
    fill: #f8db25;
    transform: scale(1.1);
  }
}

.icon-dianzan-active{
  fill: #f8db25;
  transition: all 0.3s ease;
  &:hover {
    fill: #f8db25;
    transform: scale(1.1);
  }
}
.icon-shoucang-active{
  border-radius: 40px;
  background-color: #f8db25;
  transition: all 0.3s ease;
  cursor: pointer;
  &:hover {
    fill: #f8db25;
    transform: scale(1.1);
  }
}
</style>