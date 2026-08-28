<script setup lang="js">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Delete, Search, Refresh, View, SuccessFilled, WarningFilled, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
// 导入订单相关API
import {
  getOrderListService,
  getOrderDetailService,
  deleteOrderService, adminUpdateStatusService
} from '@/api/order.js'

const route = useRoute()

// ========== 响应式数据 ==========
// 分页相关
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
// 订单列表数据
const orderList = ref([])
// 选中的订单ID（批量操作）
const selectedOrderIds = ref([])
// 查询条件
const queryForm = ref({
  orderNo: '',
  orderStatus: '',
  startTime: '',
  endTime: ''
})
// 订单详情抽屉相关
const detailDrawerVisible = ref(false)
const orderDetail = ref({})

// ========== 方法定义 ==========
// 获取订单列表
const getOrderList = async () => {
  try {
    const queryData = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      ...queryForm.value
    }
    const res = await getOrderListService(queryData)
    if (res.code === 0) {
      orderList.value = res.data.items
      total.value = res.data.total
    } else {
      ElMessage.error('获取订单列表失败：' + res.message)
    }
  } catch (error) {
    ElMessage.error('获取订单列表异常：' + error.message)
  }
}

// 每页条数变化
const onSizeChange = (size) => {
  pageSize.value = size
  getOrderList()
}

// 当前页码变化
const onCurrentChange = (num) => {
  pageNum.value = num
  getOrderList()
}

// 重置查询条件
const resetSearchCondition = () => {
  queryForm.value = {
    orderNo: '',
    orderStatus: '',
    startTime: '',
    endTime: ''
  }
  pageNum.value = 1
  getOrderList()
}

// 查看订单详情（抽屉展示）
const viewOrderDetail = async (row) => {
  try {
    const res = await getOrderDetailService(row.id)
    if (res.code === 0) {
      orderDetail.value = res.data
      detailDrawerVisible.value = true
    } else {
      ElMessage.error('获取订单详情失败：' + res.message)
    }
  } catch (error) {
    ElMessage.error('获取订单详情异常：' + error.message)
  }
}

// 处理订单状态切换
const handleStatusChange = async (row) => {
  try {
    await adminUpdateStatusService(row.id, row.orderStatus)
    ElMessage.success('订单状态更新成功')
  } catch (error) {
    ElMessage.error('状态更新失败：' + error.message)
    await getOrderList()
  }
}

// 取消订单
const cancelOrder = (row) => {
  ElMessageBox.confirm('你确认要删除该订单吗?', '温馨提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteOrderService(row.id)
      ElMessage.success('订单删除成功')
      await getOrderList()
    } catch (error) {
      ElMessage.error('订单删除失败：' + error.message)
    }
  }).catch(() => {
    ElMessage.info('用户取消了操作')
  })
}

// 处理表格选择变化
const handleSelectionChange = (selection) => {
  selectedOrderIds.value = selection.map(row => row.id)
}

// 监听路由变化
watch(
    () => route.fullPath,
    async () => {
      await getOrderList()
    }
)

// 初始化加载
onMounted(async () => {
  await getOrderList()
})
</script>

<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <span class="title-text">订单管理</span>
      </div>
    </template>

    <div class="search-area">
      <el-form inline :model="queryForm">
        <el-form-item label="订单编号">
          <el-input v-model="queryForm.orderNo" placeholder="请输入订单编号" clearable style="width: 200px" />
        </el-form-item>

        <el-form-item label="订单状态">
          <el-select v-model="queryForm.orderStatus" placeholder="选择状态" clearable style="width: 150px" @change="getOrderList">
            <el-option label="待付款" :value="1"></el-option>
            <el-option label="待发货" :value="2"></el-option>
            <el-option label="待收货" :value="3"></el-option>
            <el-option label="已完成" :value="4"></el-option>
            <el-option label="已取消" :value="5"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="下单时间">
          <el-date-picker v-model="queryForm.startTime" type="datetime" placeholder="开始时间" clearable style="width: 180px" />
          <span class="date-separator">-</span>
          <el-date-picker v-model="queryForm.endTime" type="datetime" placeholder="结束时间" clearable style="width: 180px" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Search" @click="getOrderList">搜索</el-button>
          <el-button :icon="Refresh" @click="resetSearchCondition">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-wrapper">
      <el-table
          :data="orderList"
          height="100%"
          style="width: 100%"
          @selection-change="handleSelectionChange"
          border
          stripe
      >
        <el-table-column type="selection" width="50" align="center" fixed="left" />
        <el-table-column label="订单ID" prop="id" width="80" align="center" show-overflow-tooltip />
        <el-table-column label="订单编号" prop="orderNo" min-width="190" align="center" show-overflow-tooltip />

        <el-table-column label="商品图片" width="110" align="center">
          <template #default="{ row }">
            <el-image
                v-if="row.goodsPic"
                :src="row.goodsPic"
                fit="cover"
                style="width: 70px; height: 45px; border-radius: 4px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); display: block; margin: 0 auto;"
                :preview-src-list="[row.goodsPic]"
                preview-title="商品图片"
                preview-teleported
            />
            <span v-else class="no-img">无图</span>
          </template>
        </el-table-column>

        <el-table-column label="商品名称" prop="goodsName" min-width="180" align="center" show-overflow-tooltip />
        <el-table-column label="金额" prop="totalAmount" width="100" align="center">
          <template #default="{ row }">
            <span class="amount-text">¥{{ row.totalAmount }}</span>
          </template>
        </el-table-column>

        <el-table-column label="订单状态" width="130" align="center">
          <template #default="scope">
            <el-select v-model="scope.row.orderStatus" size="small" style="width: 100px" @change="handleStatusChange(scope.row)">
              <el-option label="待付款" :value="1"></el-option>
              <el-option label="待发货" :value="2"></el-option>
              <el-option label="待收货" :value="3"></el-option>
              <el-option label="已完成" :value="4"></el-option>
              <el-option label="已取消" :value="5"></el-option>
            </el-select>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" prop="createTime" width="170" align="center" show-overflow-tooltip />
        <el-table-column label="更新时间" prop="updateTime" width="170" align="center" show-overflow-tooltip />

        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button :icon="View" type="primary" link @click="viewOrderDetail(row)">详情</el-button>
              <el-button :icon="Delete" type="danger" link @click="cancelOrder(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无订单记录" />
        </template>
      </el-table>
    </div>

    <div class="pagination-area">
      <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          :total="total"
          @size-change="onSizeChange"
          @current-change="onCurrentChange"
      />
    </div>

    <el-drawer
        v-model="detailDrawerVisible"
        title="订单详情"
        direction="rtl"
        size="550px"
        destroy-on-close
    >
      <div class="detail-drawer-container">

        <div class="info-card">
          <div class="card-title">产品信息</div>
          <div class="card-content">
            <div class="product-box">
              <el-image
                  v-if="orderDetail.goodsPic"
                  :src="orderDetail.goodsPic"
                  fit="contain"
                  class="drawer-goods-pic"
                  :preview-src-list="[orderDetail.goodsPic]"
              ></el-image>
              <div v-else class="drawer-no-pic">暂无商品图片</div>
            </div>
            <div class="product-text" v-if="orderDetail.goodsPrice">
              <span>单价: ￥{{ orderDetail.goodsPrice }}</span>
              <span style="margin-left: 20px;">数量: {{ orderDetail.goodsNum }}件</span>
            </div>
          </div>
        </div>

        <div class="info-card">
          <div class="card-title">订单状态</div>
          <div class="card-content status-content">
            <div class="status-row">
              <span class="label">支付状态：</span>
              <el-icon :class="orderDetail.payTime ? 'icon-success' : 'icon-warning'">
                <SuccessFilled v-if="orderDetail.payTime" />
                <WarningFilled v-else />
              </el-icon>
              <span class="value">{{ orderDetail.payTime ? '已支付' : '待支付' }}</span>
              <span class="sub-text" v-if="orderDetail.payTime">支付时间: {{ orderDetail.payTime }}</span>
            </div>

            <div class="status-row">
              <span class="label">退款状态：</span>
              <el-icon :class="orderDetail.refundStatus ? 'icon-info' : 'icon-warning'">
                <InfoFilled v-if="orderDetail.refundStatus" />
                <WarningFilled v-else />
              </el-icon>
              <span class="value">{{ orderDetail.refundStatusName || '未申请退款' }}</span>
            </div>

            <div class="status-row">
              <span class="label">退款审核：</span>
              <el-icon :class="orderDetail.refundStatus ? 'icon-success' : 'icon-warning'">
                <SuccessFilled v-if="orderDetail.refundStatus > 0" />
                <WarningFilled v-else />
              </el-icon>
              <span class="value">
                {{ orderDetail.refundStatus === null ? '未申请' : (orderDetail.refundStatus === 0 ? '待审核' : '已审核') }}
              </span>
            </div>
          </div>
        </div>

        <div class="info-card">
          <div class="card-title">附加信息</div>
          <div class="card-content">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="支付方式">{{ orderDetail.payTypeName || '暂无' }}</el-descriptions-item>
              <el-descriptions-item label="退款金额" v-if="orderDetail.refundAmount">
                <span style="color: #f56c6c; font-weight: bold;">￥{{ orderDetail.refundAmount }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="退款原因" v-if="orderDetail.refundReason">{{ orderDetail.refundReason }}</el-descriptions-item>

              <el-descriptions-item label="买家昵称/电话">{{ orderDetail.buyerNickname || '--' }} / {{ orderDetail.buyerPhone || '--' }}</el-descriptions-item>
              <el-descriptions-item label="卖家昵称/电话">{{ orderDetail.sellerNickname || '--' }} / {{ orderDetail.sellerPhone || '--' }}</el-descriptions-item>

              <el-descriptions-item label="收货地址">{{ orderDetail.address || '未填写' }}</el-descriptions-item>
              <el-descriptions-item label="订单备注">{{ orderDetail.remark || '无' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </div>

      </div>
    </el-drawer>
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
  .date-separator {
    margin: 0 8px;
    color: #c0c4cc;
  }
}

.table-wrapper {
  flex: 1;
  overflow: hidden;
  margin-bottom: 12px;

  .no-img {
    color: #909399;
    font-size: 12px;
  }

  .amount-text {
    font-weight: bold;
    color: #606266;
  }

  /* 核心：确保操作列按钮在同一行不折行 */
  .table-actions {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 12px;
    white-space: nowrap;
  }
}

.pagination-area {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}

/* ======== 抽屉内部样式 ======== */
.detail-drawer-container {
  padding: 0 10px 20px 10px;

  .info-card {
    border: 1px solid #f0f2f5;
    border-radius: 4px;
    margin-bottom: 24px;

    .card-title {
      padding: 16px;
      font-weight: bold;
      color: #333;
      border-bottom: 1px solid #f0f2f5;
      background-color: #fafafa;
    }

    .card-content {
      padding: 20px 16px;
    }
  }

  .product-box {
    display: flex;
    gap: 10px;
    margin-bottom: 12px;

    .drawer-goods-pic {
      width: 120px;
      height: 120px;
      border-radius: 6px;
      border: 1px solid #ebeef5;
      padding: 5px;
    }
    .drawer-no-pic {
      width: 120px;
      height: 120px;
      background: #f5f7fa;
      color: #909399;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
      border-radius: 6px;
    }
  }
  .product-text {
    font-size: 14px;
    color: #606266;
  }

  .status-content {
    .status-row {
      display: flex;
      align-items: center;
      margin-bottom: 16px;
      font-size: 14px;

      &:last-child {
        margin-bottom: 0;
      }

      .label {
        color: #606266;
        width: 80px;
      }

      .el-icon {
        font-size: 16px;
        margin-right: 6px;
        &.icon-success { color: #67c23a; }
        &.icon-warning { color: #e6a23c; }
        &.icon-info { color: #909399; }
      }

      .value {
        color: #303133;
        font-weight: 500;
      }

      .sub-text {
        margin-left: 16px;
        color: #909399;
        font-size: 13px;
      }
    }
  }
}

// 基础重置
:deep(.el-table__cell) {
  padding: 8px 0 !important;
}
:deep(.el-table__header th) {
  background-color: #fafafa;
  color: #333;
  font-weight: bold;
}
:deep(.el-drawer__body) {
  padding: 0;
}
:deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 20px;
  border-bottom: 1px solid #f0f2f5;
}
</style>