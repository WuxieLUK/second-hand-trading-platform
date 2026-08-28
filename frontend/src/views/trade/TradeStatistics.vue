<template>
  <div class="dashboard-container">
    <!-- 顶部统计卡片 -->
    <div class="stats-cards">
      <el-card class="stats-card">
        <div class="card-content">
          <div class="label">用户数量</div>
          <div class="value">{{ stats.userCount || 0 }}</div>
        </div>
      </el-card>
      <el-card class="stats-card">
        <div class="card-content">
          <div class="label">闲置商品数量</div>
          <div class="value">{{ stats.goodsCount || 0 }}</div>
        </div>
      </el-card>
    </div>

    <!-- 饼图 -->
    <el-card class="chart-card">
      <template #header>
        <div class="card-header">
          <span>不同商品分类闲置商品数量统计</span>
        </div>
      </template>
      <div ref="pieChartRef" class="pie-chart"></div>
    </el-card>
  </div>
</template>

<script setup lang="js">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
// 引入封装的仪表盘API接口
import { getDashboardStatsService } from '@/api/dashboard.js'

const pieChartRef = ref(null)
let pieChart = null
// 定义resizeHandler变量，让onUnmounted能访问到
let resizeHandler = null
const stats = ref({
  userCount: 0,
  goodsCount: 0,
  categoryStats: []
})

// 获取后端数据
const fetchStats = async () => {
  try {
    const res = await getDashboardStatsService()
    if (res.code === 0) {
      const processedData = handleCategoryData(res.data.categoryStats)
      stats.value = {
        ...res.data,
        categoryStats: processedData
      }
      renderPieChart()
    } else {
      console.warn('获取统计数据失败', res.message || res.msg)
    }
  } catch (err) {
    console.error('获取统计数据接口异常', err)
    stats.value = {
      userCount: 0,
      goodsCount: 0,
      categoryStats: []
    }
    renderPieChart() // 空数据也渲染图表，避免页面空白
  }
}

// 处理分类数据：过滤空数据、去空格、补充配色
const handleCategoryData = (categoryList) => {
  const colorList = [
    '#4066b2', '#88b864', '#e6b84c', '#d65454', '#64a8b8',
    '#447858', '#e6783c', '#784488', '#d664a8', '#4056b2',
    '#9966cc', '#ff6b6b', '#4ecdc4', '#f9c74f', '#70d6ff',
    '#ff758f', '#ff9770', '#9775fa', '#57cc99', '#80ed99'
  ]

  return categoryList
      .filter(item => item.count > 0)
      .map((item, index) => ({
        categoryName: item.categoryName.trim(),
        count: item.count,
        color: colorList[index % colorList.length]
      }))
}

// 渲染饼图
const renderPieChart = () => {
  if (!pieChartRef.value) return

  // 销毁旧实例，避免内存泄漏
  if (pieChart) {
    pieChart.dispose()
  }

  // 初始化ECharts实例
  pieChart = echarts.init(pieChartRef.value)

  const { categoryStats } = stats.value
  const pieData = categoryStats.length > 0
      ? categoryStats.map(item => ({
        name: item.categoryName,
        value: item.count,
        itemStyle: { color: item.color }
      }))
      : [{ name: '暂无数据', value: 1, itemStyle: { color: '#999999' } }]

  const option = {
    title: {
      text: '不同商品分类闲置商品数量统计',
      left: 'center',
      textStyle: { fontSize: 14 }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 件 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle',
      data: categoryStats.map(item => item.categoryName) || ['暂无数据'],
      textStyle: { fontSize: 12 }
    },
    series: [
      {
        name: '分类统计',
        type: 'pie',
        radius: ['30%', '70%'],
        center: ['60%', '55%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          fontSize: 12,
          formatter: '{b}\n{c}件'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          }
        },
        data: pieData
      }
    ]
  }

  pieChart.setOption(option)

  // 先移除旧的监听，避免重复绑定
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
  }
  // 重新定义resizeHandler
  resizeHandler = () => pieChart.resize()
  window.addEventListener('resize', resizeHandler)
}

// 组件挂载时加载数据
onMounted(() => {
  fetchStats()
})

// 关键修复：将onUnmounted移到setup顶级作用域
onUnmounted(() => {
  // 移除窗口大小监听
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
  }
  // 销毁ECharts实例，彻底释放内存
  if (pieChart) {
    pieChart.dispose()
    pieChart = null
  }
})
</script>

<style scoped lang="scss">
.dashboard-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.stats-card {
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.card-content {
  padding: 16px;
}

.label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.chart-card {
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  margin-top: 20px;
}

.card-header {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.pie-chart {
  width: 100%;
  height: 500px;
}
</style>