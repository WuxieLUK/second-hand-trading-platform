<script setup lang="js">
import { computed, ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, ElForm, ElFormItem, ElInput, ElInputNumber, ElDialog } from "element-plus";
import { useRoute, useRouter } from 'vue-router';
import {
  getOrderListService,
  getOrderDetailService,
  cancelOrderService,
  confirmReceiveOrderService,
  updateOrderStatusService,
  applyRefundService
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

// 订单状态配置
const orderStatusOptions = [
  { label: '全部', value: '' },
  { label: '待付款', value: '1' },
  { label: '待发货', value: '2' },
  { label: '待收货', value: '3' },
  { label: '已完成', value: '4' },
  { label: '已取消', value: '5' }
];

// 订单状态标签样式映射
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

// 订单状态文字样式映射
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

// 退款状态配置
const refundStatusConfig = {
  0: { text: '无退款', type: 'info' },
  1: { text: '退款中', type: 'danger' },
  2: { text: '退款成功', type: 'success' },
  3: { text: '退款失败', type: 'warning' }
};

// 获取退款状态文本
const getRefundStatusText = (refundStatus) => {
  return refundStatusConfig[refundStatus]?.text || '未知状态';
};

// 获取退款状态标签类型
const getRefundTagType = (refundStatus) => {
  return refundStatusConfig[refundStatus]?.type || 'info';
};

// 获取订单列表
const getOrderList = async () => {
  loading.value = true;
  try {
    const currentUserId = userInfoStore.info.id;
    if (!currentUserId) {
      ElMessage.error('请先登录');
      await router.push('/homes/login');
      return;
    }

    let params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      buyerId: currentUserId
    };

    if (orderStatus.value !== '') {
      params.orderStatus = Number(orderStatus.value);
    }

    if (orderKeyword.value) {
      params.orderNo = orderKeyword.value;
    }

    const result = await getOrderListService(params);
    orderList.value = result.data?.items || [];
    total.value = result.data?.total || 0;
  } catch (error) {
    ElMessage.error('获取订单列表失败');
    console.error('获取订单列表异常：', error);
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
    if (!isNaN(parsedStatus) && [1, 2, 3, 4, 5].includes(parsedStatus)) {
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

// 支付订单
const handlePay = async (row) => {
  if (row.orderStatus !== 1) {
    ElMessage.error('只有待付款订单可以支付');
    return;
  }
  if (row.refundStatus !== undefined && row.refundStatus !== 0) {
    ElMessage.error('该订单已申请退款，无法支付');
    return;
  }

  try {
    await ElMessageBox.confirm(
        `确认支付订单 ${row.orderNo} 吗？\n支付金额：¥${row.totalAmount}`,
        '确认支付',
        {
          confirmButtonText: '确认支付',
          cancelButtonText: '取消',
          type: 'info'
        }
    );

    await updateOrderStatusService(row.id);
    ElMessage.success('支付成功，等待卖家发货');
    await getOrderList();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('支付失败');
      console.error('支付异常：', error);
    }
  }
};

// 取消订单
const handleCancel = async (row) => {
  if (![1, 2].includes(row.orderStatus)) {
    ElMessage.error('只有待付款/待发货订单可以取消');
    return;
  }
  if (row.refundStatus === 1) {
    ElMessage.error('该订单正在退款中，无法取消');
    return;
  }

  try {
    await ElMessageBox.confirm(
        `确认取消订单 ${row.orderNo} 吗？`,
        '取消订单',
        {
          confirmButtonText: '确认取消',
          cancelButtonText: '返回',
          type: 'warning'
        }
    );

    await cancelOrderService(row.id);
    ElMessage.success('订单已取消');
    await getOrderList();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消订单失败');
      console.error('取消订单异常：', error);
    }
  }
};

// 确认收货
const handleConfirmReceive = async (row) => {
  if (row.orderStatus !== 3) {
    ElMessage.error('只有待收货订单可以确认收货');
    return;
  }
  if (row.refundStatus === 1) {
    ElMessage.error('该订单正在退款中，无法确认收货');
    return;
  }

  try {
    await ElMessageBox.confirm(
        `确认已收到商品吗？\n订单号：${row.orderNo}`,
        '确认收货',
        {
          confirmButtonText: '确认收货',
          cancelButtonText: '取消',
          type: 'success'
        }
    );

    await confirmReceiveOrderService(row.id);
    ElMessage.success('确认收货成功');
    await getOrderList();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('确认收货失败');
      console.error('确认收货异常：', error);
    }
  }
};

// 退款申请相关逻辑
const refundDialogVisible = ref(false);
const refundLoading = ref(false);
const currentRefundOrder = ref(null);
const refundForm = ref({
  refundAmount: 0,
  refundReason: ''
});
const refundFormRules = {
  refundAmount: [
    { required: true, message: '请输入退款金额', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '退款金额必须大于0', trigger: 'blur' }
  ],
  refundReason: [
    { required: true, message: '请填写退款原因', trigger: 'blur' },
    { min: 5, max: 200, message: '退款原因长度在5-200个字符之间', trigger: 'blur' }
  ]
};

const refundFormRef = ref(null);

// 打开退款申请弹窗
const openRefundDialog = (row) => {
  const allowRefundStatus = [2, 3, 4];
  if (!allowRefundStatus.includes(row.orderStatus)) {
    ElMessage.error('只有待发货/待收货/已完成的订单可以申请退款');
    return;
  }

  if (row.refundStatus !== undefined && row.refundStatus !== 0) {
    ElMessage.info(`该订单${getRefundStatusText(row.refundStatus)}，无法重复申请`);
    showOrderDetail(row);
    return;
  }

  currentRefundOrder.value = row;
  refundForm.value = {
    refundAmount: row.totalAmount,
    refundReason: ''
  };
  refundDialogVisible.value = true;
};

// 提交退款申请
const submitRefundApply = async () => {
  if (refundFormRef.value) {
    try {
      await refundFormRef.value.validate();
    } catch (error) {
      ElMessage.error('请填写完整的退款信息');
      return;
    }
  }

  if (!refundForm.value.refundAmount || refundForm.value.refundAmount <= 0) {
    ElMessage.error('请输入有效的退款金额');
    return;
  }
  if (refundForm.value.refundAmount > currentRefundOrder.value.totalAmount) {
    ElMessage.error(`退款金额不能超过订单总额 ¥${currentRefundOrder.value.totalAmount}`);
    return;
  }
  if (!refundForm.value.refundReason.trim()) {
    ElMessage.error('请填写退款原因');
    return;
  }

  try {
    await ElMessageBox.confirm(
        `确认申请退款吗？\n订单号：${currentRefundOrder.value.orderNo}\n退款金额：¥${refundForm.value.refundAmount}\n退款原因：${refundForm.value.refundReason}`,
        '退款申请确认',
        {
          confirmButtonText: '确认申请',
          cancelButtonText: '取消',
          type: 'warning'
        }
    );

    refundLoading.value = true;

    const refundApplyDTO = {
      orderId: currentRefundOrder.value.id,
      refundAmount: refundForm.value.refundAmount,
      refundReason: refundForm.value.refundReason,
      refundRemark: ''
    };

    await applyRefundService(refundApplyDTO);

    ElMessage.success('退款申请提交成功，等待卖家处理');
    refundDialogVisible.value = false;
    await getOrderList();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('退款申请提交失败，请稍后重试');
      console.error('退款申请失败：', error);
    }
  } finally {
    refundLoading.value = false;
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

// 跳转到商品详情
const goToDetail = (id) => {
  const targetUrl = `${window.location.origin}/goods/detail/${id}`;
  window.open(targetUrl, '_blank');
}

// 发起聊天
const startChat = async (sellerId) => {
  const receiverId = sellerId;
  if (!receiverId) {
    ElMessage.error('获取卖家信息失败，无法发起聊天');
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
      // 移动端调整默认分页大小
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
      <span class="page-title">我买到的</span>
    </div>

    <!-- 顶部状态Tab栏 - 移动端优化滚动 -->
    <div class="status-tabs-wrapper">
      <el-tabs v-model="orderStatus" class="status-tabs" @tab-click="getOrderList" >
        <el-tab-pane
            v-for="item in orderStatusOptions"
            :key="item.value"
            :label="item.label"
            :name="item.value"
        >
        </el-tab-pane>
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
          <el-button type="primary" @click="$router.push('/homes/home')">去逛逛</el-button>
        </el-empty>

        <!-- 订单卡片循环 -->
        <div class="order-card" v-for="order in orderList" :key="order.id">
          <!-- 卡片头部：卖家信息 + 订单状态 + 退款状态 -->
          <div class="order-card-header">
            <div class="seller-info">
              <el-avatar :size="22" :src="order.sellerPic" v-if="order.sellerPic" />
              <el-avatar :size="22" v-else>{{ order.sellerNickname?.charAt(0) || '用' }}</el-avatar>
              <span class="seller-name">{{ order.sellerNickname }}</span>
            </div>
            <div class="status-group">
              <!-- 订单状态 -->
              <span class="order-status-text" v-if="order.orderStatus !== undefined" :class="getStatusClass(order.orderStatus)">
                {{ order.orderStatusName }}
              </span>
              <!-- 退款状态标签 -->
              <el-tag
                  v-if="order.refundStatus !== undefined && order.refundStatus !== 0"
                  size="small"
                  :type="getRefundTagType(order.refundStatus)"
                  class="refund-status-tag"
              >
                {{ order.refundStatusName}}
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

          <!-- 操作按钮区域 - 移动端优化布局 -->
          <div class="order-action-bar">
            <div class="action-btn-group">
              <!-- 联系卖家按钮 -->
              <el-button size="small" class="action-btn contact-btn" @click="startChat(order.sellerId)">
                <i class="el-icon-chat-line-round"></i> 联系卖家
              </el-button>

              <!-- 支付按钮 -->
              <el-button
                  size="small"
                  class="action-btn primary"
                  @click="handlePay(order)"
                  v-if="order.orderStatus === 1 && (order.refundStatus === undefined || order.refundStatus === 0)"
              >
                <i class="el-icon-money"></i> 付款
              </el-button>

              <!-- 取消订单 -->
              <el-button
                  size="small"
                  class="action-btn"
                  @click="handleCancel(order)"
                  v-if="order.orderStatus === 1 && order.refundStatus !== 1"
              >
                <i class="el-icon-close"></i> 取消订单
              </el-button>

              <!-- 确认收货 -->
              <el-button
                  size="small"
                  class="action-btn primary"
                  @click="handleConfirmReceive(order)"
                  v-if="order.orderStatus === 3 && order.refundStatus !== 2"
              >
                <i class="el-icon-check"></i> 确认收货
              </el-button>

              <!-- 申请退款 -->
              <el-button
                  size="small"
                  class="action-btn danger-btn"
                  @click="openRefundDialog(order)"
                  v-if="[2, 3, 4].includes(order.orderStatus) && (order.refundStatus === undefined || order.refundStatus === 0)"
              >
                <i class="el-icon-refund"></i> 申请退款
              </el-button>

              <!-- 已申请退款提示按钮 -->
              <el-button
                  size="small"
                  class="action-btn"
                  disabled
                  :type="getRefundTagType(order.refundStatus)"
                  v-else-if="order.refundStatus !== undefined && order.refundStatus !== 0"
              >
                {{ order.refundStatusName }}
              </el-button>

              <!-- 详情按钮 -->
              <el-button size="small" class="action-btn primary" @click="showOrderDetail(order)">
                <i class="el-icon-info"></i> 详情
              </el-button>

              <!-- 去评价 -->
              <el-button size="small" class="action-btn primary" v-if="order.orderStatus === 4" @click="goToDetail(order.goodsId)">
                <i class="el-icon-star-on"></i> 去评价
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
      >
      </el-pagination>
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
          <!-- 详情页退款状态 -->
          <el-descriptions-item label="退款状态" v-if="orderDetail.refundStatus !== undefined">
            <el-tag :type="getRefundTagType(orderDetail.refundStatus)">
              {{ orderDetail.refundStatusName || getRefundStatusText(orderDetail.refundStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订单金额">
            <span class="amount">¥{{ orderDetail.totalAmount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="支付方式">
            {{ orderDetail.payTypeName || '未支付' }}
          </el-descriptions-item>
          <el-descriptions-item label="卖家昵称">
            {{ orderDetail.sellerNickname || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="买家昵称">
            {{ orderDetail.buyerNickname || '-' }}
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

          <!-- 退款详情展示 -->
          <el-descriptions-item label="退款金额" v-if="orderDetail.refundAmount">
            ¥{{ orderDetail.refundAmount }}
          </el-descriptions-item>
          <el-descriptions-item label="退款原因" v-if="orderDetail.refundReason">
            {{ orderDetail.refundReason }}
          </el-descriptions-item>
          <el-descriptions-item label="退款时间" v-if="orderDetail.refundTime">
            {{ orderDetail.refundTime ? orderDetail.refundTime.replace('T', ' ').substring(0, 19) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="退款备注" v-if="orderDetail.refundRemark">
            {{ orderDetail.refundRemark }}
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
          <el-descriptions-item label="买家电话" v-if="orderDetail.buyerPhone">
            {{ orderDetail.buyerPhone }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="orderDetail.remark" class="section-title">备注信息</div>
        <div v-if="orderDetail.remark" class="remark-content">
          {{ orderDetail.remark }}
        </div>

      </div>
      <template #footer>
        <el-button size="small" @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 退款申请弹窗 - 移动端适配 -->
    <el-dialog
        v-model="refundDialogVisible"
        title="申请退款"
        width="90%"
        max-width="500px"
        :close-on-click-modal="false"
        :modal-append-to-body="false"
    >
      <el-form
          :model="refundForm"
          :rules="refundFormRules"
          ref="refundFormRef"
          label-width="90px"
          class="refund-form"
      >
        <el-form-item label="订单编号">
          <span>{{ currentRefundOrder?.orderNo || '-' }}</span>
        </el-form-item>
        <el-form-item label="订单金额">
          <span>¥{{ currentRefundOrder?.totalAmount || 0 }}</span>
        </el-form-item>
        <el-form-item label="退款金额" prop="refundAmount">
          <el-input-number
              v-model="refundForm.refundAmount"
              :min="0.01"
              :max="currentRefundOrder?.totalAmount || 0"
              :precision="2"
              style="width: 100%;"
              placeholder="请输入退款金额"
          />
        </el-form-item>
        <el-form-item label="退款原因" prop="refundReason">
          <el-input
              v-model="refundForm.refundReason"
              type="textarea"
              :rows="4"
              placeholder="请详细描述退款原因（5-200字）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="refundDialogVisible = false">取消</el-button>
        <el-button
            size="small"
            type="primary"
            @click="submitRefundApply"
            :loading="refundLoading"
        >
          提交申请
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
// 基础样式重置
* {
  box-sizing: border-box;
  touch-action: manipulation; // 优化移动端点击体验
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
  -webkit-overflow-scrolling: touch; // 移动端顺滑滚动
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

// 状态Tab栏 - 移动端优化
.status-tabs-wrapper {
  background: #fff;
  margin-bottom: 8px;
  white-space: nowrap;

  ::-webkit-scrollbar {
    display: none; // 隐藏滚动条
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

  .seller-info {
    display: flex;
    align-items: center;
    gap: 6px;

    .seller-name {
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
      display: none; // 移动端隐藏跳转输入框
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

// 退款表单样式
.refund-form {
  margin-top: 8px;

  :deep(.el-form-item) {
    margin-bottom: 12px;

    .el-form-item__label {
      font-size: 13px;
    }

    .el-form-item__content {
      line-height: 1.5;
      font-size: 13px;
    }
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
  // 大屏恢复原有样式
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
