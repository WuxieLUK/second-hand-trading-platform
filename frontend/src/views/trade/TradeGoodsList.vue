<script setup lang="js">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
// 修复：使用最基础的 Tools 图标（所有版本都存在）
import { Edit, Delete, Search, Refresh, Tools } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
// 导入商品相关API
import {
  goodsListService,
  goodsDeleteService,
  goodsUpdateStatusService,
  goodsCleanMilvusService // 导入清理Milvus脏数据API
} from '@/api/goods.js'
// ========== 核心修改：替换分类API ==========
import {
  shopCategoryTreeListService,
  shopCategoryChildListService
} from '@/api/shopcategory.js'

// 初始化路由
const router = useRouter()
const route = useRoute()

// ========== 响应式数据（替代Store） ==========
// 分页相关
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
// 商品列表数据
const goodsList = ref([])
// ========== 核心修改：分类数据结构调整 ==========
const parentCategoryList = ref([]) // 一级分类列表
const childCategoryList = ref([])  // 二级分类列表（根据选中的一级分类动态加载）
// 选中的商品ID（批量操作）
const selectedGoodsIds = ref([])
// 查询条件（匹配GoodsQueryDTO）
const queryForm = ref({
  goodsName: '',        // 商品名称模糊查询
  parentCategoryId: '', // 新增：一级分类ID
  categoryId: '',       // 保留：二级分类ID
  goodsStatus: '',      // 商品状态（1-在售 2-已售出 3-下架 4-审核中 5-违规封禁）
  isNew: '',            // 新旧程度
  minPrice: '',         // 最低价格
  maxPrice: '',         // 最高价格
  sellerId: ''          // 卖家ID
})

// ========== 方法定义 ==========
// ========== 核心修改：重构分类获取逻辑 ==========
// 获取一级分类列表
const getParentCategoryList = async () => {
  try {
    const res = await shopCategoryTreeListService()
    if (res.code === 0) {
      // 初始化一级分类列表
      parentCategoryList.value = res.data.map(item => ({
        id: item.id,
        categoryName: item.categoryName
      }))
    } else {
      ElMessage.error('获取一级分类列表失败：' + res.message)
    }
  } catch (error) {
    ElMessage.error('获取一级分类列表异常：' + error.message)
  }
}

// 根据选中的一级分类加载二级分类
const getChildCategoryList = async (parentId) => {
  if (!parentId) {
    childCategoryList.value = []
    queryForm.value.categoryId = '' // 清空二级分类选择
    return
  }
  try {
    const res = await shopCategoryChildListService(parentId)
    if (res.code === 0) {
      childCategoryList.value = res.data
      queryForm.value.categoryId = '' // 切换一级分类时清空二级分类
    } else {
      ElMessage.error('获取二级分类列表失败：' + res.message)
    }
  } catch (error) {
    ElMessage.error('获取二级分类列表异常：' + error.message)
  }
}

// 获取商品列表（分页+多条件筛选）
const getGoodsList = async () => {
  try {
    // 构造查询参数（匹配GoodsQueryDTO）
    const queryData = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      // 核心：仅传递二级分类ID给后端，保持接口兼容
      categoryId: queryForm.value.categoryId,
      goodsName: queryForm.value.goodsName,
      goodsStatus: queryForm.value.goodsStatus,
      isNew: queryForm.value.isNew,
      minPrice: queryForm.value.minPrice,
      maxPrice: queryForm.value.maxPrice,
      sellerId: queryForm.value.sellerId
    }
    const res = await goodsListService(queryData)
    if (res.code === 0) {
      // 适配GoodsVO数据结构，补充状态名称
      goodsList.value = res.data.items
      total.value = res.data.total
    } else {
      ElMessage.error('获取商品列表失败：' + res.message)
    }
  } catch (error) {
    ElMessage.error('获取商品列表异常：' + error.message)
  }
}

// ========== 新增：清理Milvus脏数据方法 ==========
const cleanMilvusDirtyData = async () => {
  try {
    // 二次确认防止误操作
    await ElMessageBox.confirm(
        '确认要清理Milvus脏数据吗？该操作不可逆，请谨慎执行！',
        '清理Milvus脏数据',
        {
          confirmButtonText: '确认清理',
          cancelButtonText: '取消',
          type: 'warning',
          draggable: true
        }
    )

    // 调用清理接口
    const res = await goodsCleanMilvusService()
    if (res.code === 0) {
      ElMessage.success('Milvus脏数据清理成功！')
      // 清理完成后刷新商品列表
      await getGoodsList()
    } else {
      ElMessage.error('Milvus脏数据清理失败：' + res.message)
    }
  } catch (error) {
    // 如果是用户取消操作，不提示错误
    if (error !== 'cancel') {
      ElMessage.error('Milvus脏数据清理异常：' + (error.message || '操作取消'))
    }
  }
}

// 每页条数变化
const onSizeChange = (size) => {
  pageSize.value = size
  getGoodsList()
}

// 当前页码变化
const onCurrentChange = (num) => {
  pageNum.value = num
  getGoodsList()
}

// 重置查询条件
const resetSearchCondition = () => {
  queryForm.value = {
    goodsName: '',
    parentCategoryId: '', // 重置一级分类
    categoryId: '',       // 重置二级分类
    goodsStatus: '',
    isNew: '',
    minPrice: '',
    maxPrice: '',
    sellerId: ''
  }
  pageNum.value = 1
  // 清空二级分类列表
  childCategoryList.value = []
  getGoodsList()
}

// 删除单个商品
const goodsDelete = (row) => {
  ElMessageBox.confirm('你确认要删除该商品信息吗?', '温馨提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await goodsDeleteService(row.id)
      ElMessage.success('删除成功')
      await getGoodsList()
    } catch (error) {
      ElMessage.error('删除失败：' + error.message)
    }
  }).catch(() => {
    ElMessage.info('用户取消了删除')
  })
}

// 批量删除商品
const batchDeleteGoods = async () => {
  if (selectedGoodsIds.value.length === 0) {
    ElMessage.warning('请选择要删除的商品')
    return
  }

  ElMessageBox.confirm(
      `确认要删除选中的 ${selectedGoodsIds.value.length} 个商品吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'danger'
      }
  ).then(async () => {
    try {
      // 批量调用删除接口
      for (const id of selectedGoodsIds.value) {
        await goodsDeleteService(id)
      }
      ElMessage.success('批量删除成功')
      selectedGoodsIds.value = []
      await getGoodsList()
    } catch (error) {
      ElMessage.error('批量删除失败：' + error.message)
    }
  })
}

// 处理表格选择变化
const handleSelectionChange = (selection) => {
  selectedGoodsIds.value = selection.map(row => row.id)
}

// 跳转到编辑商品页面
const toEditGoods = (row) => {
  sessionStorage.setItem('editGoods', JSON.stringify(row))
  router.push({
    path: '/trade/goods/edit',
    query: { id: row.id }
  })
}

// 处理商品状态切换
const handleStatusChange = async (row) => {
  try {
    await goodsUpdateStatusService(row.id, row.goodsStatus)
    ElMessage.success('状态更新成功')
  } catch (error) {
    // 状态回滚
    const oldStatus = row.goodsStatus
    row.goodsStatus = oldStatus === 1 ? 2 : 1 // 示例回滚逻辑，可根据实际需求调整
    ElMessage.error('状态更新失败：' + error.message)
  }
}

// ========== 核心修改：监听一级分类变化 ==========
watch(
    () => queryForm.value.parentCategoryId,
    async (newVal) => {
      await getChildCategoryList(newVal)
    },
    { immediate: false }
)

// 监听路由变化（编辑/新增返回后刷新列表）
watch(
    () => route.fullPath,
    async (_newPath, oldPath) => {
      if (oldPath?.includes('/goods/edit') || oldPath?.includes('/goods/add')) {
        await getGoodsList()
      }
    }
)

// 初始化加载
onMounted(async () => {
  await getParentCategoryList() // 修改：加载一级分类
  await getGoodsList()
})
</script>

<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <span class="title-text">商品管理</span>
      </div>
    </template>

    <!-- 搜索区域 -->
    <div class="search-area">
      <el-form inline :model="queryForm">
        <el-form-item label="商品名称">
          <el-input
              v-model="queryForm.goodsName"
              placeholder="请输入商品名称"
              clearable
              style="width: 200px"
          ></el-input>
        </el-form-item>

        <!-- ========== 核心修改：分类筛选区域 ========== -->
        <el-form-item label="一级分类">
          <el-select
              v-model="queryForm.parentCategoryId"
              placeholder="选择一级分类"
              clearable
              style="width: 180px"
          >
            <el-option
                v-for="c in parentCategoryList"
                :key="c.id"
                :label="c.categoryName"
                :value="c.id"
            ></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="二级分类">
          <el-select
              v-model="queryForm.categoryId"
              placeholder="选择二级分类"
              clearable
              style="width: 180px"
              @change="getGoodsList"
              :disabled="!queryForm.parentCategoryId"
          >
            <el-option
                v-for="c in childCategoryList"
                :key="c.id"
                :label="c.categoryName"
                :value="c.id"
            ></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="商品状态">
          <el-select
              v-model="queryForm.goodsStatus"
              placeholder="选择状态"
              clearable
              style="width: 150px"
              @change="getGoodsList"
          >
            <el-option label="在售" :value="1"></el-option>
            <el-option label="已售出" :value="2"></el-option>
            <el-option label="下架" :value="3"></el-option>
            <el-option label="审核中" :value="4"></el-option>
            <el-option label="违规封禁" :value="5"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="价格区间">
          <el-input
              v-model="queryForm.minPrice"
              placeholder="最低价格"
              clearable
              style="width: 100px"
              type="number"
          ></el-input>
          <span class="price-separator">-</span>
          <el-input
              v-model="queryForm.maxPrice"
              placeholder="最高价格"
              clearable
              style="width: 100px"
              type="number"
          ></el-input>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Search" @click="getGoodsList">搜索</el-button>
          <el-button :icon="Refresh" @click="resetSearchCondition">重置</el-button>
          <el-button
              type="danger"
              :icon="Delete"
              @click="batchDeleteGoods"
              :disabled="selectedGoodsIds.length === 0"
          >
            批量删除 ({{ selectedGoodsIds.length }})
          </el-button>
          <!-- ========== 最终修复：使用 Tools 图标 ========== -->
          <el-button
              type="warning"
              :icon="Tools"
              @click="cleanMilvusDirtyData"
              style="margin-left: 10px"
          >
            清理Milvus脏数据
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格区域 -->
    <div class="table-wrapper">
      <el-table
          :data="goodsList"
          height="100%"
          style="width: 100%"
          @selection-change="handleSelectionChange"
          border
          stripe
      >
        <el-table-column type="selection" width="55" align="center"></el-table-column>
        <el-table-column label="编号" width="80" prop="id" align="center"></el-table-column>

        <!-- 商品图片 -->
        <el-table-column label="商品图片" width="130" align="center">
          <template #default="{ row }">
            <el-image
                :src="row.goodsPic ? row.goodsPic.split(',')[0] : ''"
                fit="cover"
                style="width: 80px; height: 45px; border-radius: 6px; box-shadow: 0 2px 4px rgba(0,0,0,0.1)"
                :preview-src-list="row.goodsPic ? row.goodsPic.split(',') : []"
                preview-teleported
            >
              <template #error><div class="image-slot">无图</div></template>
            </el-image>
          </template>
        </el-table-column>

        <!-- 商品名称 -->
        <el-table-column label="商品名称" prop="goodsName" min-width="180" align="center" show-overflow-tooltip></el-table-column>

        <!-- 商品分类 -->
        <el-table-column label="商品分类" prop="categoryName" width="100" align="center"></el-table-column>

        <!-- 售卖价格 -->
        <el-table-column label="售卖价格" prop="sellPrice" width="100" align="center">
          <template #default="{ row }">¥{{ row.sellPrice }}</template>
        </el-table-column>

        <!-- 库存列 - 新增 -->
        <el-table-column label="库存" prop="stock" width="100" align="center">
          <template #default="{ row }">
            <span :class="row.stock <= 0 ? 'stock-out' : 'stock-normal'">
              {{ row.stock || 0 }}
            </span>
          </template>
        </el-table-column>

        <!-- 新旧程度 -->
        <el-table-column label="新旧程度" prop="isNewName" width="100" align="center"></el-table-column>

        <!-- 商品状态 -->
        <el-table-column label="商品状态" width="120" align="center">
          <template #default="scope">
            <el-select
                v-model="scope.row.goodsStatus"
                size="small"
                @change="handleStatusChange(scope.row)"
            >
              <el-option label="在售" :value="1"></el-option>
              <el-option label="已售罄" :value="2"></el-option>
              <el-option label="下架" :value="3"></el-option>
              <el-option label="审核中" :value="4"></el-option>
              <el-option label="违规封禁" :value="5"></el-option>
            </el-select>
          </template>
        </el-table-column>

        <!-- 创建时间 -->
        <el-table-column label="创建时间" prop="createTime" width="160" align="center"></el-table-column>

        <!-- 操作列 -->
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button :icon="Edit" type="primary" link @click="toEditGoods(row)">编辑</el-button>
            <el-button :icon="Delete" type="danger" link @click="goodsDelete(row)">删除</el-button>
          </template>
        </el-table-column>

        <!-- 空数据提示 -->
        <template #empty>
          <el-empty description="暂无商品记录" />
        </template>
      </el-table>
    </div>

    <!-- 分页区域 -->
    <div class="pagination-area">
      <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          :total="total"
          @size-change="onSizeChange"
          @current-change="onCurrentChange"
      />
    </div>
  </el-card>
</template>

<style lang="scss" scoped>
.page-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;

  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    flex: 1;
    overflow: hidden;
    padding: 16px;
  }

  .header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    .title-text {
      font-weight: bold;
      font-size: 16px;
      color: #303133;
    }
  }
}

.search-area {
  flex-shrink: 0;
  margin-bottom: 12px;

  .price-separator {
    margin: 0 8px;
    color: #c0c4cc;
  }

}

.table-wrapper {
  flex: 1;
  overflow: hidden;
  margin-bottom: 12px;
}

.pagination-area {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
}

/* 库存样式优化 */
.stock-out {
  color: #f56c6c;
  font-weight: bold;
}
.stock-normal {
  color: #67c23a;
}

:deep(.el-table__cell) {
  padding: 10px 0 !important;
}

:deep(.el-table__header th) {
  background-color: #fafafa;
  color: #333;
  font-weight: bold;
}

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
  font-size: 12px;
}
</style>