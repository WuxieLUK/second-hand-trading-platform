<script setup lang="js">
import { computed, ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, ElInput, ElRadioGroup, ElRadio } from "element-plus";
import { useRoute, useRouter } from 'vue-router';
import {
  getOrderListService,
  getOrderDetailService,
  handleRefundService,
  sendOrderService
} from "@/api/order.js";
import useUserInfoStore from '@/stores/userInfo.js';
import { userInfoServices } from '@/api/user.js';
import { createChatSessionService } from "@/api/chat.js";
const route = useRoute();
const router = useRouter();
const userInfoStore = useUserInfoStore();

// 获取用户信息
const getUserInfo = async () => {
  try {
    let result = await userInfoServices();
    userInfoStore.setInfo(result.data);
  } catch (error) {
    ElMessage.error('获取用户信息失败');
  }
};

const orderStatus = ref('');
const orderKeyword = ref('');
const orderList = ref([]);
const loading = ref(false);

const pageNum = ref(1);
const total = ref(0);
const pageSize = ref(10);

// 状态配置
const orderStatusOptions = [
  { label: '全部', value: '' },
  { label: '待付款', value: '1' },
  { label: '待发货', value: '2' },
  { label: '待收货', value: '3' },
  { label: '已完成', value: '4' },
  { label: '已取消', value: '5' }
];

// 状态标签样式映射
const statusTagType = (status) => {
  const map = {
    1: 'warning',
    2: 'primary',
    3: 'info',
    4: 'success',
    5: 'danger'
  };
  return map[status] || 'info';
};

// 订单卡片状态文字色值映射
const getStatusClass = (status) => {
  const map = {
    1: 'status-pending-pay',
    2: 'status-pending-send',
    3: 'status-pending-receive',
    4: 'status-success',
    5: 'status-closed'
  };
  return map[status] || 'status-default';
};

// 退款状态标签样式映射
const getRefundTagType = (refundStatus) => {
  const map = {
    0: 'info',
    1: 'danger',
    2: 'success',
    3: 'warning'
  };
  return map[refundStatus] || 'info';
};

// 获取订单列表
const getOrderList = async () => {
  loading.value = true;
  try {
    if (!userInfoStore.info || !userInfoStore.info.id) {
      ElMessage.error('请先登录');
      await router.push('/homes/login');
      return;
    }
    const currentUserId = userInfoStore.info.id;

    let params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      sellerId: currentUserId,
      orderStatus: orderStatus.value !== '' ? Number(orderStatus.value) : undefined
    };

    if (orderKeyword.value) {
      params.orderNo = orderKeyword.value;
    }

    const result = await getOrderListService(params);
    orderList.value = result.data?.items || [];
    total.value = result.data?.total || 0;
  } catch (error) {
    ElMessage.error('获取订单列表失败');
  } finally {
    loading.value = false;
  }
};

const onSizeChange = (size) => {
  pageSize.value = size;
  getOrderList();
};

const onCurrentChange = (num) => {
  pageNum.value = num;
  getOrderList();
};

const syncQueryState = () => {
  const keywordFromQuery = typeof route.query.keyword === 'string'
    ? route.query.keyword.trim()
    : '';
  orderKeyword.value = keywordFromQuery;

  const statusFromQuery = route.query.status;
  if (statusFromQuery !== undefined && statusFromQuery !== null && statusFromQuery !== '') {
    const parsedStatus = Number(statusFromQuery);
    if (!isNaN(parsedStatus) && parsedStatus >= 1 && parsedStatus <= 5) {
      orderStatus.value = String(parsedStatus);
      return;
    }
  }
  orderStatus.value = '';
};

const clearKeywordQuery = () => {
  const nextQuery = { ...route.query };
  delete nextQuery.keyword;
  router.push({
    path: route.path,
    query: nextQuery
  });
};

// 退款处理弹窗相关
const refundHandleDialogVisible = ref(false);
const currentRefundOrder = ref(null);
const refundHandleForm = ref({
  orderId: null,
  handleResult: 2,
  refundRemark: ''
});

// 订单详情相关
const dialogVisible = ref(false);
const orderDetail = ref({});
const detailLoading = ref(false);

const showOrderDetail = async (row) => {
  dialogVisible.value = true;
  detailLoading.value = true;
  try {
    const result = await getOrderDetailService(row.id);
    orderDetail.value = result.data || {};
  } catch (error) {
    ElMessage.error('获取订单详情失败');
    dialogVisible.value = false;
  } finally {
    detailLoading.value = false;
  }
};

// 打开退款处理弹窗
const openRefundHandleDialog = (row) => {
  currentRefundOrder.value = row;
  refundHandleForm.value = {
    orderId: row.id,
    handleResult: 2,
    refundRemark: ''
  };
  refundHandleDialogVisible.value = true;
};

// 处理退款
const handleRefund = async () => {
  if (!refundHandleForm.value.orderId) {
    ElMessage.error('订单ID不能为空');
    return;
  }

  if (refundHandleForm.value.handleResult === 3 && !refundHandleForm.value.refundRemark.trim()) {
    ElMessage.error('驳回退款必须填写处理备注');
    return;
  }

  try {
    const handleType = refundHandleForm.value.handleResult === 2 ? '同意' : '驳回';
    await ElMessageBox.confirm(
        `确认${handleType}该订单的退款申请吗？\n订单号：${currentRefundOrder.value.orderNo}`,
        `${handleType}退款`,
        {
          confirmButtonText: `确认${handleType}`,
          cancelButtonText: '取消',
          type: refundHandleForm.value.handleResult === 2 ? 'info' : 'warning'
        }
    );

    const refundHandleDTO = {
      orderId: refundHandleForm.value.orderId,
      handleResult: refundHandleForm.value.handleResult,
      refundRemark: refundHandleForm.value.refundRemark.trim()
    };

    await handleRefundService(refundHandleDTO);

    ElMessage.success(`${handleType}退款成功`);
    refundHandleDialogVisible.value = false;
    await getOrderList();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${refundHandleForm.value.handleResult === 2 ? '同意' : '驳回'}退款失败`);
      console.error('退款处理失败：', error);
    }
  }
};

// 处理Tab切换事件
const handleTabClick = () => {
  pageNum.value = 1;
  getOrderList();
};

// 发货功能
const handleShip = async (row) => {
  if (row.refundStatus === 1) {
    ElMessage.error('该订单正在退款中，无法发货');
    return;
  }

  try {
    await ElMessageBox.confirm(
        `确认发货吗？\n订单号：${row.orderNo}\n商品：${row.goodsName}`,
        '确认发货',
        {
          confirmButtonText: '确认发货',
          cancelButtonText: '取消',
          type: 'info'
        }
    );

    await sendOrderService(row.id);
    ElMessage.success('发货成功，等待买家确认收货');
    await getOrderList();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('发货失败');
    }
  }
};

// 页面初始化
onMounted(async () => {
  await getUserInfo();
  syncQueryState();
  await getOrderList();
});

watch(
    () => [route.query.status, route.query.keyword],
    async () => {
      syncQueryState();
      pageNum.value = 1;
      await getOrderList();
    }
);

// 发起聊天
const startChat = async (buyerId) => {
  const receiverId = buyerId;
  if (!receiverId) {
    ElMessage.error('获取买家信息失败，无法发起聊天');
    return;
  }

  try {
    const res = await createChatSessionService(receiverId);
    const sessionId = res.data.id;

    ElMessage.closeAll();
    await router.push({
      path: '/homes/notice',
      query: { sessionId }
    });
  } catch (error) {
    ElMessage.closeAll();
    console.error('创建聊天会话失败:', error);
    ElMessage.error('发起聊天失败，请稍后重试');
  }
};

// 监听窗口大小变化，优化移动端体验
watch(
    () => window.innerWidth,
    (width) => {
      if (width < 768) {
        pageSize.value = 8;
      } else {
        pageSize.value = 10;
      }
    },
    { immediate: true }
);
</script>

<template>
  <div class="order-page-wrapper">
    <!-- 页面头部标题 -->
    <div class="page-header">
      <span class="page-title">我卖出的</span>
    </div>

    <!-- 顶部状态Tab栏 - 移动端滚动优化 -->
    <div class="status-tabs-wrapper">
      <el-tabs v-model="orderStatus" class="status-tabs" @tab-click="handleTabClick">
        <el-tab-pane
            v-for="item in orderStatusOptions"
            :key="item.value"
            :label="item.label"
            :name="item.value"
        />
      </el-tabs>
    </div>

    <div v-if="orderKeyword" class="query-banner">
      <span>当前按订单号查询：{{ orderKeyword }}</span>
      <el-button type="primary" link @click="clearKeywordQuery">清除筛选</el-button>
    </div>

    <!-- 订单列表卡片区域 -->
    <div class="content-wrapper">
      <div class="order-list" v-loading="loading">
        <!-- 空状态 -->
        <el-empty v-if="orderList.length === 0 && !loading" description="暂无订单记录">
          <el-button type="primary" size="small" @click="$router.push('/homes/home')">去逛逛</el-button>
        </el-empty>

        <!-- 订单卡片循环 -->
        <div class="order-card" v-for="order in orderList" :key="order.id">
          <!-- 卡片头部：买家信息 + 订单状态 + 退款状态 -->
          <div class="order-card-header">
            <div class="buyer-info">
              <el-avatar :size="22" :src="order.buyerPic" v-if="order.buyerPic" />
              <el-avatar :size="22" v-else>{{ order.buyerNickname?.charAt(0) || '买' }}</el-avatar>
              <span class="buyer-name">{{ order.buyerNickname }}</span>
            </div>
            <div class="status-group">
              <!-- 订单状态 -->
              <span class="order-status-text" :class="getStatusClass(order.orderStatus)">
                {{ order.orderStatusName }}
              </span>
              <!-- 退款状态 -->
              <el-tag
                  v-if="order.refundStatus !== undefined && order.refundStatus !== 0"
                  size="small"
                  :type="getRefundTagType(order.refundStatus)"
                  class="refund-status-tag"
              >
                {{ order.refundStatusName }}
              </el-tag>
            </div>
          </div>

          <!-- 商品信息区域 -->
          <div class="goods-content" @click="showOrderDetail(order)">
            <img :src="order.goodsPic" class="goods-cover" alt="商品图片" v-if="order.goodsPic" />
            <div class="goods-info">
              <p class="goods-name">{{ order.goodsName }}</p>
              <p class="goods-price">¥{{ order.totalAmount }}</p>
            </div>
          </div>

          <!-- 操作按钮区域 - 移动端优化 -->
          <div class="order-action-bar">
            <div class="action-btn-group">
              <!-- 联系买家按钮 -->
              <el-button size="small" class="action-btn contact-btn" @click="startChat(order.buyerId)">
                <i class="el-icon-chat-line-round"></i> 联系买家
              </el-button>

              <!-- 核心操作按钮 -->
              <el-button size="small" class="action-btn primary" @click="showOrderDetail(order)">
                <i class="el-icon-info"></i> 详情
              </el-button>

              <!-- 发货按钮 -->
              <el-button
                  size="small"
                  class="action-btn primary"
                  @click="handleShip(order)"
                  v-if="order.orderStatus === 2 && (order.refundStatus === undefined || order.refundStatus !== 2)"
              >
                <i class="el-icon-truck"></i> 发货
              </el-button>

              <!-- 退款处理按钮 -->
              <el-button
                  size="small"
                  class="action-btn danger-btn"
                  @click="openRefundHandleDialog(order)"
                  v-if="order.refundStatus === 1"
              >
                <i class="el-icon-refund"></i> 处理退款
              </el-button>

              <!-- 已处理退款状态显示 -->
              <el-button
                  size="small"
                  class="action-btn"
                  disabled
                  :type="getRefundTagType(order.refundStatus)"
                  v-else-if="order.refundStatus !== undefined && order.refundStatus !== 0 && order.refundStatus !== 1"
              >
                {{ order.refundStatusName }}
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页区域 - 移动端优化 -->
    <div class="pagination-wrapper">
      <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[5, 8, 10, 20]"
          layout="total, prev, pager, next, jumper"
          background
          :total="total"
          @size-change="onSizeChange"
          @current-change="onCurrentChange"
          :disabled="total === 0"
      />
    </div>

    <!-- 订单详情弹窗 - 移动端适配 -->
    <el-dialog
        v-model="dialogVisible"
        title="订单详情"
        width="90%"
        max-width="700px"
        destroy-on-close
        :modal-append-to-body="false"
    >
      <div v-loading="detailLoading" class="order-detail-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="订单编号">
            {{ orderDetail.orderNo }}
          </el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="statusTagType(orderDetail.orderStatus)">
              {{ orderDetail.orderStatusName }}
            </el-tag>
          </el-descriptions-item>
          <!-- 退款状态 -->
          <el-descriptions-item label="退款状态" v-if="orderDetail.refundStatus !== undefined">
            <el-tag :type="getRefundTagType(orderDetail.refundStatus)">
              {{ orderDetail.refundStatusName }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订单金额">
            <span class="amount">¥{{ orderDetail.totalAmount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="支付方式">
            {{ orderDetail.payTypeName || '未支付' }}
          </el-descriptions-item>
          <el-descriptions-item label="买家">
            {{ orderDetail.buyerNickname || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="买家电话">
            {{ orderDetail.buyerPhone || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ orderDetail.createTime ? orderDetail.createTime.replace('T', ' ').substring(0, 19) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="支付时间">
            {{ orderDetail.payTime ? orderDetail.payTime.replace('T', ' ').substring(0, 19) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="发货时间">
            {{ orderDetail.deliveryTime ? orderDetail.deliveryTime.replace('T', ' ').substring(0, 19) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="收货时间">
            {{ orderDetail.receiveTime ? orderDetail.receiveTime.replace('T', ' ').substring(0, 19) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="取消时间">
            {{ orderDetail.cancelTime ? orderDetail.cancelTime.replace('T', ' ').substring(0, 19) : '-' }}
          </el-descriptions-item>
          <!-- 退款相关字段 -->
          <el-descriptions-item label="退款金额" v-if="orderDetail.refundAmount">
            ¥{{ orderDetail.refundAmount || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="退款原因" v-if="orderDetail.refundReason">
            {{ orderDetail.refundReason || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="退款申请时间" v-if="orderDetail.refundApplyTime">
            {{ orderDetail.refundApplyTime ? orderDetail.refundApplyTime.replace('T', ' ').substring(0, 19) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="退款处理时间" v-if="orderDetail.refundHandleTime">
            {{ orderDetail.refundHandleTime ? orderDetail.refundHandleTime.replace('T', ' ').substring(0, 19) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="处理备注" v-if="orderDetail.refundRemark">
            {{ orderDetail.refundRemark || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="section-title">商品信息</div>
        <div class="goods-detail-card">
          <img v-if="orderDetail.goodsPic" :src="orderDetail.goodsPic" class="goods-detail-img" alt=""/>
          <div class="goods-detail-info">
            <div class="goods-detail-name">{{ orderDetail.goodsName }}</div>
            <div class="goods-detail-price">单价：¥{{ orderDetail.goodsPrice }}</div>
            <div class="goods-detail-num">数量：{{ orderDetail.goodsNum }}</div>
          </div>
        </div>

        <div class="section-title">收货信息</div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="收货地址">
            {{ orderDetail.address || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="orderDetail.remark" class="section-title">订单备注</div>
        <div v-if="orderDetail.remark" class="remark-content">
          {{ orderDetail.remark }}
        </div>

      </div>
      <template #footer>
        <el-button size="small" @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 退款处理弹窗 - 移动端适配 -->
    <el-dialog
        v-model="refundHandleDialogVisible"
        title="处理退款申请"
        width="90%"
        max-width="500px"
        :close-on-click-modal="false"
        :modal-append-to-body="false"
    >
      <div v-if="currentRefundOrder" class="refund-handle-content">
        <div class="refund-order-info">
          <p><strong>订单号：</strong>{{ currentRefundOrder.orderNo }}</p>
          <p><strong>商品：</strong>{{ currentRefundOrder.goodsName }}</p>
          <p><strong>退款金额：</strong>¥{{ currentRefundOrder.refundAmount || currentRefundOrder.totalAmount }}</p>
          <p><strong>买家退款原因：</strong>{{ currentRefundOrder.refundReason || '无' }}</p>
        </div>

        <el-radio-group v-model="refundHandleForm.handleResult" class="refund-radio-group">
          <el-radio :label="2" class="radio-item">同意退款（退款成功）</el-radio>
          <el-radio :label="3" class="radio-item">驳回退款（退款失败）</el-radio>
        </el-radio-group>

        <!-- 处理备注输入框 -->
        <el-form-item
            label="处理备注"
            :required="refundHandleForm.handleResult === 3"
            class="refund-reason-item"
        >
          <el-input
              v-model="refundHandleForm.refundRemark"
              type="textarea"
              :rows="4"
              :placeholder="refundHandleForm.handleResult === 3 ? '请输入驳回退款的理由（必填）' : '请输入处理备注（可选）'"
              maxlength="200"
              show-word-limit
              size="small"
          />
        </el-form-item>
      </div>

      <template #footer>
        <el-button size="small" @click="refundHandleDialogVisible = false">取消</el-button>
        <el-button
            size="small"
            type="primary"
            @click="handleRefund"
        >
          {{ refundHandleForm.handleResult === 2 ? '同意退款' : '驳回退款' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
// 基础样式重置
* {
  box-sizing: border-box;
  touch-action: manipulation;
}

.order-page-wrapper {
  width: 100%;
  min-height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
  padding-bottom: env(safe-area-inset-bottom);
}

.content-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 8px;
  margin-bottom: 8px;
  -webkit-overflow-scrolling: touch;
}

.query-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 16px;
  margin: 0 8px 8px;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 12px;
  color: #8c5a00;
  font-size: 13px;
}

// 页面标题
.page-header {
  padding: 12px 16px;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 10;

  .page-title {
    font-size: 17px;
    font-weight: 600;
    color: #333;
  }
}

// 状态Tab栏 - 移动端滚动优化
.status-tabs-wrapper {
  background: #fff;
  margin-bottom: 8px;
  white-space: nowrap;

  ::-webkit-scrollbar {
    display: none;
  }

  .status-tabs {
    :deep(.el-tabs__header) {
      margin: 0;
      border-bottom: none;
    }

    :deep(.el-tabs__nav-wrap) {
      padding: 0;
    }

    :deep(.el-tabs__nav) {
      display: inline-flex;
      width: auto;
    }

    :deep(.el-tabs__item) {
      font-size: 14px;
      color: #666;
      padding: 12px 16px;
      margin: 0;
      border-bottom: 2px solid transparent;
    }

    :deep(.el-tabs__item.is-active) {
      color: #ff6700;
      font-weight: 600;
      border-bottom-color: #ff6700;
    }

    :deep(.el-tabs__active-bar) {
      display: none;
    }

    :deep(.el-tabs__content) {
      display: none;
    }
  }
}

// 订单卡片
.order-card {
  background: #fff;
  border-radius: 12px;
  margin-bottom: 8px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
}

// 卡片头部
.order-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid #f5f5f5;

  .buyer-info {
    display: flex;
    align-items: center;
    gap: 6px;

    .buyer-name {
      font-size: 14px;
      color: #333;
      font-weight: 500;
    }
  }

  .status-group {
    display: flex;
    align-items: center;
    gap: 6px;

    .refund-status-tag {
      margin-left: 4px;
    }
  }

  .order-status-text {
    font-size: 14px;
    font-weight: 500;

    &.status-success {
      color: #ff6700;
    }

    &.status-closed {
      color: #999;
    }

    &.status-pending-pay {
      color: #e6a23c;
    }

    &.status-pending-send {
      color: #409eff;
    }

    &.status-pending-receive {
      color: #909399;
    }

    &.status-default {
      color: #666;
    }
  }
}

// 商品内容区域
.goods-content {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px;
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover {
    background-color: #fafafa;
  }

  .goods-cover {
    width: 70px;
    height: 70px;
    border-radius: 8px;
    object-fit: cover;
    flex-shrink: 0;
  }

  .goods-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    min-height: 70px;

    .goods-name {
      font-size: 14px;
      color: #333;
      line-height: 1.4;
      margin: 0;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .goods-price {
      font-size: 16px;
      font-weight: 600;
      color: #ff4400;
      margin: 0;
      text-align: right;
    }
  }
}

// 操作按钮栏 - 移动端优化
.order-action-bar {
  padding: 8px 12px;
  border-top: 1px solid #f5f5f5;

  .action-btn-group {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    justify-content: flex-end;

    .action-btn {
      border-radius: 20px;
      padding: 4px 12px;
      font-size: 12px;
      height: 28px;
      line-height: 1;
      border-color: #ddd;

      &.primary {
        background: #ffd700;
        border-color: #ffd700;
        color: #333;

        &:hover {
          background: #ffe033;
          border-color: #ffe033;
        }
      }

      &.danger-btn {
        background: #f56c6c;
        border-color: #f56c6c;
        color: #fff;

        &:hover {
          background: #f78989;
          border-color: #f78989;
        }
      }

      &.contact-btn {
        background: #e8f4ff;
        border-color: #409eff;
        color: #409eff;
      }
    }
  }
}

// 分页容器
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 12px 8px;
  background: #fff;
  border-radius: 12px;
  margin: 8px;

  :deep(.el-pagination) {
    font-size: 12px;

    .el-pagination__total {
      margin-right: 8px;
    }

    .el-pagination__jump {
      display: none;
    }
  }
}

// 详情页样式
.amount {
  color: #ff4400;
  font-weight: bold;
  font-size: 15px;
}

.order-detail-content {
  max-height: 70vh;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;

  .section-title {
    font-size: 15px;
    font-weight: bold;
    color: #303133;
    margin: 16px 0 8px 0;
    padding-left: 8px;
    border-left: 3px solid #409eff;
  }

  .goods-detail-card {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px;
    background-color: #fafafa;
    border-radius: 8px;
    margin-bottom: 8px;

    .goods-detail-img {
      width: 80px;
      height: 80px;
      border-radius: 8px;
      object-fit: cover;
      flex-shrink: 0;
    }

    .goods-detail-info {
      flex: 1;

      .goods-detail-name {
        font-size: 14px;
        font-weight: 500;
        color: #333;
        margin-bottom: 6px;
      }

      .goods-detail-price {
        font-size: 13px;
        color: #666;
        margin-bottom: 4px;
      }

      .goods-detail-num {
        font-size: 13px;
        color: #999;
      }
    }
  }

  .remark-content {
    padding: 10px;
    background-color: #fffbe6;
    border: 1px solid #ffe58f;
    border-radius: 4px;
    color: #d48806;
    font-size: 13px;
  }

}

// 退款处理弹窗样式
.refund-handle-content {
  font-size: 13px;
}

.refund-order-info {
  margin-bottom: 16px;
  padding: 10px;
  background-color: #f8f9fa;
  border-radius: 8px;

  p {
    margin: 4px 0;
    font-size: 13px;
    line-height: 1.4;
  }
}

.refund-radio-group {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;

  .radio-item {
    font-size: 14px;
  }

  :deep(.el-radio) {
    font-size: 13px;
  }
}

.refund-reason-item {
  margin-bottom: 0;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }

  :deep(.el-form-item__label) {
    font-weight: 500;
    font-size: 13px;
    padding: 0 8px 0 0;
  }

  :deep(.el-form-item__content) {
    line-height: 1.5;
  }
}

:deep(.el-descriptions) {
  font-size: 13px;

  .el-descriptions__label {
    font-weight: 500;
    width: 80px;
    padding: 10px 8px;
  }

  .el-descriptions__content {
    padding: 10px 8px;
  }
}

// 弹窗样式优化
:deep(.el-dialog) {
  border-radius: 16px;
  margin: 10vh auto 0;

  .el-dialog__header {
    padding: 12px 16px;
    border-bottom: 1px solid #f5f5f5;

    .el-dialog__title {
      font-size: 16px;
      font-weight: 600;
    }

    .el-dialog__headerbtn {
      top: 12px;
    }
  }

  .el-dialog__body {
    padding: 12px 16px;
    font-size: 14px;
    max-height: 80vh;
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
  }

  .el-dialog__footer {
    padding: 8px 16px 12px;
    border-top: 1px solid #f5f5f5;
    text-align: center;

    button {
      font-size: 14px;
      padding: 8px 20px;
      margin: 0 4px;
    }
  }
}

// 空状态优化
:deep(.el-empty) {
  padding: 40px 0;

  .el-empty__description {
    font-size: 14px;
    margin-top: 12px;
  }

  button {
    margin-top: 16px;
    font-size: 14px;
  }
}

// 适配不同屏幕尺寸
@media (max-width: 375px) {
  .query-banner {
    flex-direction: column;
    align-items: flex-start;
  }

  .goods-content .goods-cover {
    width: 60px;
    height: 60px;
  }

  .action-btn-group {
    justify-content: space-between !important;
  }

  .action-btn {
    flex: 1;
    text-align: center;
    padding: 4px 6px !important;
  }
}

@media (min-width: 768px) {
  .status-tabs-wrapper {

    :deep(.el-tabs__nav) {
      justify-content: center;
      width: 100%;
    }
  }

  .order-detail-content {
    max-height: none;
    overflow-y: visible;
  }

  :deep(.el-descriptions) {
    :nth-child(1) {
      grid-column: span 2;
    }
  }
}
</style>
