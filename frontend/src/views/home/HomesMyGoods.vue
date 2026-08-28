<script setup lang="js">
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {Search, Refresh, Plus, Edit, DeleteFilled, Switch, Picture} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useTokenStore } from "@/stores/token.js"
// 导入商品相关API
import {
  goodsMyListService,
  goodsDeleteService,
  goodsUpdateStatusService
} from '@/api/goods.js'
// 导入分类相关API（适配层级分类）
import {
  shopCategoryTreeListService,
  shopCategoryChildListService
} from '@/api/shopcategory.js'

const router = useRouter()
const route = useRoute()

// ========== 响应式数据 ==========
const pageNum = ref(1)
const pageSize = ref(15)
const total = ref(0)
const goodsList = ref([])
// 分类相关数据
const parentCategoryList = ref([]) // 一级分类列表
const childCategoryList = ref([]) // 二级分类列表（联动加载）
const allCategoryList = ref([]) // 全量扁平分类列表（用于展示）
const loading = ref(false)
const tokenStore = useTokenStore();
// 查询条件（适配层级分类）
const queryForm = ref({
  goodsName: '',
  parentCategoryId: '', // 一级分类ID
  childCategoryId: '',  // 二级分类ID（实际筛选用）
  goodsStatus: '',
  minPrice: '',
  maxPrice: ''
})

// 判断当前是否处于搜索状态
const isSearching = computed(() => {
  return queryForm.value.goodsName !== '' ||
      queryForm.value.parentCategoryId !== '' ||
      queryForm.value.childCategoryId !== '' ||
      queryForm.value.goodsStatus !== '' ||
      queryForm.value.minPrice !== '' ||
      queryForm.value.maxPrice !== ''
})

// ========== 方法定义 ==========

// 跳转发布商品页面
const toPublish = () => {
  const targetUrl = `${window.location.origin}/homes/publish`;
  window.open(targetUrl, '_blank');
}

// 获取树形分类列表并扁平化处理
const getCategoryTreeList = async () => {
  try {
    const res = await shopCategoryTreeListService()
    if (res.code === 0) {
      // 扁平化处理：转为一维数组（用于商品分类名称展示）
      const flatCategories = []
      res.data.forEach(parent => {
        // 添加一级分类
        flatCategories.push({
          id: parent.id,
          categoryName: parent.categoryName,
          parentId: 0
        })
        // 添加二级分类（带层级名称）
        if (parent.children && parent.children.length > 0) {
          parent.children.forEach(child => {
            flatCategories.push({
              id: child.id,
              categoryName: `${parent.categoryName} > ${child.categoryName}`,
              parentId: parent.id
            })
          })
        }
      })
      allCategoryList.value = flatCategories

      // 初始化一级分类下拉列表
      parentCategoryList.value = res.data.map(item => ({
        id: item.id,
        categoryName: item.categoryName
      }))
    }
  } catch (error) {
    console.error('获取分类列表异常', error)
    ElMessage.error('获取分类列表失败')
  }
}

// 根据选中的一级分类加载二级分类
const loadChildCategories = async (parentId) => {
  if (!parentId) {
    childCategoryList.value = []
    queryForm.value.childCategoryId = '' // 清空二级分类选择
    return
  }
  try {
    const res = await shopCategoryChildListService(parentId)
    if (res.code === 0) {
      childCategoryList.value = res.data
    }
  } catch (error) {
    console.error('获取子分类异常', error)
    ElMessage.error('获取子分类失败')
  }
}

// 获取我的商品列表
const getGoodsList = async () => {
  loading.value = true
  try {
    // 构造查询参数（兼容一级/二级分类筛选）
    const queryData = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      goodsName: queryForm.value.goodsName,
      // 优先用二级分类ID，无则用一级分类ID
      categoryId: queryForm.value.childCategoryId || queryForm.value.parentCategoryId,
      goodsStatus: queryForm.value.goodsStatus,
      minPrice: queryForm.value.minPrice,
      maxPrice: queryForm.value.maxPrice
    }

    if(tokenStore.token!==''){
      const res = await goodsMyListService(queryData)
      if (res.code === 0) {
        goodsList.value = res.data.items || []
        total.value = res.data.total || 0
      } else {
        ElMessage.error('获取列表失败：' + res.message)
      }
    }
  } catch (error) {
    ElMessage.error('网络异常，获取列表失败')
  } finally {
    loading.value = false
  }
}

// 分页处理
const onSizeChange = (size) => {
  pageSize.value = size
  getGoodsList()
}
const onCurrentChange = (num) => {
  pageNum.value = num
  getGoodsList()
}

// 重置查询条件
const resetSearchCondition = () => {
  queryForm.value = {
    goodsName: '',
    parentCategoryId: '',
    childCategoryId: '',
    goodsStatus: '',
    minPrice: '',
    maxPrice: ''
  }
  childCategoryList.value = []
  pageNum.value = 1
  getGoodsList()
}

// 删除单个商品
const goodsDelete = (row) => {
  ElMessageBox.confirm('确认删除该商品吗?', '提示', {
    confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'
  }).then(async () => {
    await goodsDeleteService(row.id)
    ElMessage.success('删除成功')
    await getGoodsList()
  }).catch(() => {})
}

// 标记商品售出方法
const goodsSoldOut = (row) => {
  if (row.goodsStatus === 1) {
    ElMessage.warning('该商品已标记为售出状态')
    return
  }

  if (![2, 3].includes(row.goodsStatus)) {
    ElMessage.warning('该商品状态不支持标记为售出')
    return
  }

  ElMessageBox.confirm(
      '确认将该商品标记为售出吗？标记后商品状态将变为已售罄',
      '提示',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    try {
      const res = await goodsUpdateStatusService(row.id, 1)
      if (res.code === 0) {
        ElMessage.success('商品标记为售出成功')
        await getGoodsList()
      } else {
        ElMessage.error('标记失败：' + res.message)
      }
    } catch (error) {
      ElMessage.error('网络异常，标记失败')
    }
  }).catch(() => {
    ElMessage.info('已取消标记售出操作')
  })
}

// 下架商品方法
const goodsOffShelf = (row) => {
  if (row.goodsStatus === 3) {
    ElMessage.warning('该商品已处于下架状态')
    return
  }

  if (row.goodsStatus !== 1) {
    ElMessage.warning('仅售出状态的商品可执行此操作')
    return
  }

  ElMessageBox.confirm(
      '确认将该商品下架吗？下架后商品将不再展示在商品列表中',
      '提示',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    try {
      const res = await goodsUpdateStatusService(row.id, 3)
      if (res.code === 0) {
        ElMessage.success('商品下架成功')
        await getGoodsList()
      } else {
        ElMessage.error('下架失败：' + res.message)
      }
    } catch (error) {
      ElMessage.error('网络异常，下架失败')
    }
  }).catch(() => {
    ElMessage.info('已取消下架操作')
  })
}

// 动态处理按钮点击事件
const handleStatusToggle = (row) => {
  if (row.goodsStatus === 1) {
    goodsOffShelf(row)
  } else {
    goodsSoldOut(row)
  }
}

// 跳转到编辑页面
const toEditGoods = (row) => {
  sessionStorage.setItem('editGoods', JSON.stringify(row))
  router.push({
    path: '/homes/goodsEdit',
    query: { id: row.id }
  })
}

// 路由监听，返回时刷新列表
watch(() => route.fullPath, async () => {
  await getGoodsList()
})

// 跳转到商品详情
const goToDetail = (id) => {
  const targetUrl = `${window.location.origin}/goods/detail/${id}`;
  window.open(targetUrl, '_blank');
}

// 初始化加载
onMounted(async () => {
  await getCategoryTreeList()
  await getGoodsList()
})
</script>

<template>
  <div class="my-goods-container">
    <div class="header-bar">
      <h2>我发布的</h2>
      <el-button type="primary" :icon="Plus" @click="toPublish" class="publish-btn">
        发布新商品
      </el-button>
    </div>

    <div class="empty-state-wrapper" v-if="total === 0 && !isSearching && !loading">
      <el-empty description="暂无商品信息" :image-size="200">
        <el-button type="primary" size="large" @click="toPublish">
          发布我的第一个商品
        </el-button>
      </el-empty>
    </div>

    <div class="content-wrapper" v-else>
      <div class="search-area">
        <el-form inline :model="queryForm" class="filter-form">
          <el-form-item label="名称">
            <el-input v-model="queryForm.goodsName" placeholder="商品名称" clearable style="width: 150px"/>
          </el-form-item>

          <!-- 一级分类筛选 -->
          <el-form-item label="一级分类">
            <el-select
                v-model="queryForm.parentCategoryId"
                placeholder="全部分类"
                clearable
                style="width: 120px"
                @change="(val) => {
                loadChildCategories(val);
                queryForm.childCategoryId = '';
                getGoodsList();
              }"
            >
              <el-option v-for="c in parentCategoryList" :key="c.id" :label="c.categoryName" :value="c.id"/>
            </el-select>
          </el-form-item>

          <!-- 二级分类筛选（有数据时显示） -->
          <el-form-item label="二级分类" v-if="childCategoryList.length > 0">
            <el-select
                v-model="queryForm.childCategoryId"
                placeholder="全部子类"
                clearable
                style="width: 120px"
                @change="getGoodsList"
            >
              <el-option v-for="c in childCategoryList" :key="c.id" :label="c.categoryName" :value="c.id"/>
            </el-select>
          </el-form-item>

          <el-form-item label="状态">
            <el-select v-model="queryForm.goodsStatus" placeholder="全部" clearable style="width: 100px" @change="getGoodsList">
              <el-option label="在售" :value="1"></el-option>
              <el-option label="已售罄" :value="2"></el-option>
              <el-option label="下架" :value="3"></el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="价格区间">
            <el-input
                v-model="queryForm.minPrice"
                placeholder="最低价格"
                clearable
                style="width: 80px"
                type="number"
            />
            <span style="margin: 0 5px">-</span>
            <el-input
                v-model="queryForm.maxPrice"
                placeholder="最高价格"
                clearable
                style="width: 80px"
                type="number"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :icon="Search" @click="getGoodsList">搜索</el-button>
            <el-button :icon="Refresh" @click="resetSearchCondition">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="goods-grid" v-loading="loading">
        <el-empty v-if="goodsList.length === 0" description="没有找到符合条件的商品" style="grid-column: 1 / -1;"></el-empty>

        <div class="goods-card" v-for="item in goodsList" :key="item.id">
          <div class="card-img-wrapper" @click="goToDetail(item.id)">
            <el-image
                :src="item.goodsPic ? item.goodsPic.split(',')[0] : ''"
                fit="cover"
                class="goods-img"
                lazy
            >
              <template #error>
                <div class="image-slot">
                  <el-icon size="48" color="#bfbfbf"><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <span class="status-badge" :class="'status-' + item.goodsStatus">
              {{ item.goodsStatus === 1 ? '在售' : (item.goodsStatus === 2 ? '已出' : '下架') }}
            </span>
          </div>

          <div class="card-info">
            <!-- 商品分类层级展示 -->
            <div class="goods-category" v-if="item.categoryId">
              {{ allCategoryList.find(c => c.id === item.categoryId)?.categoryName || '未知分类' }}
            </div>
            <div class="goods-title" :title="item.goodsName">{{ item.goodsName }}</div>

            <div class="price-row">
              <div class="price-text">
                <span class="currency">¥</span>
                <span class="amount">{{ item.sellPrice }}</span>
              </div>
              <div class="want-count">{{ item.collectCount || 0 }}人想要</div>
            </div>

            <div class="action-row">
              <button class="custom-btn edit-btn" @click="toEditGoods(item)"><el-icon><Edit /></el-icon>编辑</button>
              <button
                  class="custom-btn"
                  @click="handleStatusToggle(item)"
                  :class="['sold-out-btn']"
              >
                <el-icon><Switch /></el-icon>
                {{ item.goodsStatus === 1 ? '下架' : '标记售出' }}
              </button>
              <button class="custom-btn delete-btn" @click="goodsDelete(item)"><el-icon><DeleteFilled /></el-icon>删除</button>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination-area">
        <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[12, 24, 36]"
            layout="total, sizes, prev, pager, next"
            background
            :total="total"
            @size-change="onSizeChange"
            @current-change="onCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.my-goods-container {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
  padding: 10px;
  box-sizing: border-box;

  .header-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    h2 { margin: 0; font-size: 24px; color: #222; font-weight: 600; }
    .publish-btn { background-color: #409eff; border-radius: 4px; padding: 10px 20px; font-size: 14px; }
  }
}

.empty-state-wrapper {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.search-area {
  margin-bottom: 30px;
  padding: 16px 20px;
  background: #fcfcfc;
  border: 1px solid #f0f0f0;
  border-radius: 8px;

  .filter-form {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    align-items: flex-end;
  }

  :deep(.el-form-item) {
    margin-bottom: 0;
    margin-right: 20px;
  }
}

// 核心修改：固定卡片网格布局和尺寸
.goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 240px); // 固定列宽
  gap: 20px;
  padding-bottom: 40px;
  flex: 1;
  align-content: flex-start;
  justify-content: start; // 左对齐，保证卡片排列整齐
}

.goods-card {
  width: 240px; // 固定卡片宽度
  height: 420px; // 固定卡片高度
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  border: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  justify-content: space-between; // 内容垂直分布

  &:hover {
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
    transform: translateY(-4px);
  }

  // 核心修改：固定图片容器尺寸
  .card-img-wrapper {
    position: relative;
    width: 100%;
    height: 240px; // 固定图片容器高度
    cursor: pointer;
    background-color: #f7f8fa;
    overflow: hidden; // 防止图片溢出

    .goods-img {
      width: 100%;
      height: 100%;
      object-fit: cover; // 保持图片比例并填充容器
      object-position: center; // 图片居中显示
    }

    .image-slot {
      display: flex;
      justify-content: center;
      align-items: center;
      width: 100%;
      height: 100%;
      color: #999;
      font-size: 14px;
    }

    .status-badge {
      position: absolute;
      top: 12px;
      right: 12px;
      padding: 4px 10px;
      border-radius: 6px;
      font-size: 12px;
      color: #fff;
      font-weight: 500;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      &.status-1 { background: #67c23a; }
      &.status-2 { background: #f56c6c; }
      &.status-3 { background: #909399; }
    }
  }

  .card-info {
    padding: 16px;
    display: flex;
    flex-direction: column;
    flex: 1; // 填充剩余空间
    justify-content: space-between; // 内容均匀分布

    // 新增：分类名称样式
    .goods-category {
      font-size: 12px;
      color: #999;
      margin-bottom: 4px;
    }

    .goods-title {
      font-size: 16px;
      color: #333;
      font-weight: 500;
      line-height: 1.5;
      margin-bottom: 12px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      height: 48px; // 固定标题高度
    }

    .price-row {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      margin-bottom: 16px;

      .price-text {
        color: #ff4d4f;
        font-weight: bold;
        .currency { font-size: 14px; }
        .amount { font-size: 22px; }
      }

      .want-count { font-size: 13px; color: #999; }
    }

    .action-row {
      display: flex;
      gap: 12px;

      .custom-btn {
        flex: 1;
        border: none;
        padding: 8px 0;
        border-radius: 6px;
        font-size: 14px;
        cursor: pointer;
        color: #fff;
        font-weight: 500;
        transition: background 0.2s;

        &:active { transform: scale(0.97); }

        &.disabled {
          background-color: #ccc !important;
          cursor: not-allowed;
          transform: none !important;
          opacity: 0.7;
        }
      }

      .edit-btn { background-color: #79af53; &:hover { background-color: #6a9a48; } }
      .sold-out-btn { background-color: #e6a23c; &:hover { background-color: #d4912f; } }
      .delete-btn { background-color: #7d848e; &:hover { background-color: #6c737c; } }
    }
  }
}

.pagination-area {
  padding: 30px 0;
  display: flex;
  justify-content: center;
}

// 移动端适配样式 - 保持卡片比例一致
@media screen and (max-width: 1024px) {
  .my-goods-container {
    padding: 40px 50px;

    .header-bar {
      margin-bottom: 20px;
      h2 { font-size: 20px; }
      .publish-btn { padding: 8px 16px; font-size: 13px; }
    }
  }

  .search-area {
    padding: 14px 16px;
    margin-bottom: 24px;

    :deep(.el-form-item) {
      margin-right: 15px;
    }

    :deep(.el-input) {
      width: 130px !important;
    }

    :deep(.el-select) {
      width: 100px !important;
    }
  }

  .goods-grid {
    grid-template-columns: repeat(auto-fill, 200px); // 适配平板尺寸
    gap: 20px;
  }

  .goods-card {
    width: 200px; // 适配平板尺寸
    height: 380px; // 适配平板尺寸

    .card-img-wrapper {
      height: 200px; // 适配平板尺寸
    }

    .card-info {
      padding: 14px;

      .goods-title {
        font-size: 15px;
        margin-bottom: 10px;
        height: 45px;
      }

      .price-row {
        margin-bottom: 14px;

        .price-text {
          .amount { font-size: 20px; }
        }
      }

      .action-row {
        gap: 10px;

        .custom-btn {
          padding: 7px 0;
          font-size: 13px;
        }
      }
    }
  }
}

@media screen and (max-width: 768px) {
  .my-goods-container {
    padding: 20px 15px;
    min-height: 100vh;

    .header-bar {
      margin-bottom: 16px;
      h2 { font-size: 18px; }
      .publish-btn {
        padding: 6px 12px;
        font-size: 12px;
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }

  .empty-state-wrapper {
    :deep(.el-empty) {
      :deep(.el-empty-image) {
        width: 150px !important;
        height: 150px !important;
      }
      :deep(.el-empty-description) {
        font-size: 14px;
      }
      :deep(.el-button) {
        padding: 8px 16px;
        font-size: 14px;
      }
    }
  }

  .search-area {
    padding: 12px 14px;
    margin-bottom: 20px;

    .filter-form {
      gap: 8px;
    }

    :deep(.el-form-item) {
      margin-right: 0;
      margin-bottom: 10px !important;
      width: calc(50% - 4px);
    }

    :deep(.el-form-item:last-child) {
      width: 100%;
      display: flex;
      justify-content: center;
      gap: 10px;
    }

    :deep(.el-input) {
      width: 100% !important;
    }

    :deep(.el-select) {
      width: 100% !important;
    }

    :deep(.el-form-item__label) {
      font-size: 12px;
      padding-right: 5px;
    }
  }

  .goods-grid {
    grid-template-columns: repeat(auto-fill, 140px); // 适配手机尺寸
    gap: 15px;
    padding-bottom: 30px;
  }

  .goods-card {
    width: 140px; // 适配手机尺寸
    height: 300px; // 适配手机尺寸

    .card-img-wrapper {
      height: 140px; // 适配手机尺寸

      .status-badge {
        top: 8px;
        right: 8px;
        padding: 2px 8px;
        font-size: 11px;
      }
    }

    .card-info {
      padding: 12px;

      .goods-category {
        font-size: 11px;
      }

      .goods-title {
        font-size: 14px;
        margin-bottom: 8px;
        height: 40px;
        line-height: 1.4;
      }

      .price-row {
        margin-bottom: 12px;

        .price-text {
          .currency { font-size: 12px; }
          .amount { font-size: 18px; }
        }

        .want-count {
          font-size: 12px;
        }
      }

      .action-row {
        gap: 8px;

        .custom-btn {
          padding: 6px 0;
          font-size: 12px;
          border-radius: 4px;
        }
      }
    }
  }

  .pagination-area {
    padding: 20px 0 10px;

    :deep(.el-pagination) {
      font-size: 12px;

      :deep(.el-pagination__sizes) {
        display: none;
      }

      :deep(.el-pager li) {
        padding: 0 8px;
      }
    }
  }
}

@media screen and (max-width: 480px) {
  .goods-grid {
    grid-template-columns: repeat(auto-fill, 120px); // 适配小屏手机
    gap: 10px;
  }

  .goods-card {
    width: 120px; // 适配小屏手机
    height: 260px; // 适配小屏手机

    .card-img-wrapper {
      height: 120px; // 适配小屏手机
    }

    .card-info {
      .goods-title {
        font-size: 13px;
        height: 36px;
      }

      .price-row {
        .price-text {
          .amount { font-size: 16px; }
        }
      }
    }
  }
}
</style>