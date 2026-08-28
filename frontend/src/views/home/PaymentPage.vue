<script setup lang="js">
import {ref, computed, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {ElMessage, ElMessageBox} from 'element-plus';
import {Check} from '@element-plus/icons-vue';
import {goodsDetailService} from '@/api/goods.js';
import {createOrderService, updateOrderStatusService, getOrderListService, findByOrderNoService} from '@/api/order.js';
import {addressListService, addressGetDefaultService} from '@/api/address.js';
import {userInfoServices} from '@/api/user.js';
import {useTokenStore} from '@/stores/token.js';
import useUserInfoStore from '@/stores/userInfo.js';

const route = useRoute();
const router = useRouter();
const tokenStore = useTokenStore();
const userInfoStore = useUserInfoStore();

// 商品信息
const goodsId = ref(Number(route.query.goodsId));
const goodsDetail = ref({
  id: 0,
  goodsName: '',
  goodsPic: '',
  sellPrice: 0,
  originalPrice: 0,
  sellerId: 0,
  sellerNickname: '',
  sellerAvatar: ''
});

// 地址信息（从用户信息或地址列表获取）
const selectedAddress = ref({
  id: null,
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: false
});

// 地址列表（新增：用于循环展示所有地址，可从接口获取）
const addressList = ref([]);

// 订单备注（保留逻辑，模板移除）
const remark = ref('');

// 加载状态
const loading = ref(false);
const submitting = ref(false);

// 计算总价
const totalAmount = computed(() => {
  return Number(goodsDetail.value.sellPrice || 0).toFixed(2);
});

// ========== 新增：生成唯一的requestId ==========
/**
 * 生成唯一的requestId（用于幂等性校验）
 * 格式：时间戳 + 随机数 + 用户ID（如果有）
 */
const generateRequestId = () => {
  // 13位时间戳
  const timestamp = new Date().getTime();
  // 6位随机数
  const random = Math.floor(Math.random() * 900000) + 100000;
  // 用户ID（增加唯一性）
  const userId = userInfoStore.info.id || 'guest';
  // 拼接成唯一ID
  return `REQ-${userId}-${timestamp}-${random}`;
};

// 获取用户信息
const getUserInfo = async () => {
  try {
    let result = await userInfoServices();
    userInfoStore.setInfo(result.data);
  } catch (error) {
    ElMessage.error('获取用户信息失败');
  }
};

// 获取商品详情
const getGoodsDetail = async () => {
  if (!goodsId.value) {
    ElMessage.error('商品信息不存在');
    await router.push('/');
    return;
  }

  try {
    loading.value = true;
    const res = await goodsDetailService(goodsId.value);
    if (res.data) {
      goodsDetail.value = res.data;
      const rawStatus = res.data.goodsStatus;
      const statusName = res.data.goodsStatusName;
      let status = Number(rawStatus);

      // 如果状态码是 undefined 或 NaN，则根据状态名称推断
      if (isNaN(status) && statusName) {
        const statusNameMap = {
          '在售': 1,
          '已售出': 2,
          '已下架': 3,
          '审核中': 4,
          '违规封禁': 5
        };
        status = statusNameMap[statusName] || 3; // 默认视为已下架
      }

      if (status !== 1) {
        const statusMap = {
          2: '已售出',
          3: '已下架',
          4: '审核中',
          5: '违规封禁'
        };
        const statusText = statusMap[status] || '不可购买';
        ElMessage.warning(`该商品${statusText}`);
        await router.push(`/goods/detail/${goodsId.value}`);
      }
    } else {
      ElMessage.error('商品不存在');
      await router.push('/');
    }
  } catch (error) {
    ElMessage.error('获取商品信息失败');
  } finally {
    loading.value = false;
  }
};

// 获取地址列表
const getAddressList = async () => {
  try {
    const res = await addressListService({pageNum: 1, pageSize: 100});
    if (res.data && res.data.items) {
      addressList.value = res.data.items.map(item => ({
        id: item.id,
        receiverName: item.addressName,
        receiverPhone: item.phone || '',
        province: item.province,
        city: item.city,
        district: item.district,
        detailAddress: item.detailAddr,
        fullAddress: item.fullAddress,
        isDefault: item.addressType === 1
      }));
    }
  } catch (error) {
    console.error('获取地址列表失败:', error);
  }
};

// 获取默认地址
const getDefaultAddress = async () => {
  try {
    const res = await addressGetDefaultService();
    if (res.data) {
      selectedAddress.value = {
        id: res.data.id,
        receiverName: res.data.addressName,
        receiverPhone: res.data.phone || '',
        province: res.data.province,
        city: res.data.city,
        district: res.data.district,
        detailAddress: res.data.detailAddr,
        fullAddress: res.data.fullAddress,
        isDefault: res.data.addressType === 1
      };
    }
  } catch (error) {
    console.error('获取默认地址失败:', error);
  }
};

// 选择地址
const selectAddress = (addr) => {
  if (addr) {
    selectedAddress.value = addr;
  } else {
    // 跳转到地址页面，携带来源标识和当前商品ID
    router.push({
      path: '/homes/address',
      query: {
        from: 'payment',
        goodsId: goodsId.value
      }
    });
  }
};

// 提交订单
const submitOrder = async () => {
  // 检查登录状态
  if (!tokenStore.token) {
    ElMessage.warning('请先登录');
    await router.push('/homes/login');
    return;
  }

  // 检查地址
  if (!selectedAddress.value.id) {
    ElMessageBox.confirm(
        '您还没有选择收货地址，是否先添加地址？',
        '提示',
        {
          confirmButtonText: '去添加',
          cancelButtonText: '取消',
          type: 'warning'
        }
    ).then(() => {
      router.push('/homes/address');
    });
    return;
  }

  try {
    submitting.value = true;

    // ========== 新增：生成requestId ==========
    const requestId = generateRequestId();
    console.log('生成的requestId:', requestId);

    // 构建订单创建参数
    const orderData = {
      buyerId: userInfoStore.info.id || 1, // 从用户信息获取买家ID，默认1
      sellerId: goodsDetail.value.sellerId, // 从商品详情获取卖家ID
      goodsId: goodsId.value,
      addressId: selectedAddress.value.id,
      goodsNum: 1, // 每次下单默认购买 1 件
      remark: remark.value,
      // ========== 新增：添加requestId到请求参数 ==========
      requestId: requestId
    };

    console.log('订单创建参数(含requestId):', orderData);

    const res = await createOrderService(orderData);

    if (res.code === 0) {
      const orderNo = res.data;
      ElMessage.success('订单创建成功');

      // 跳转到支付确认页面或订单详情页
      ElMessageBox.confirm(
          `订单号：${orderNo}\n请选择支付方式完成付款`,
          '订单创建成功',
          {
            confirmButtonText: '去支付',
            cancelButtonText: '查看订单',
            type: 'success'
          }
      ).then(() => {
        // 去支付 - 实际项目中这里应该调用支付接口
        handlePayment(orderNo);
      }).catch(() => {
        // 查看订单
        const targetUrl = `${window.location.origin}/homes/myBought`;
        window.open(targetUrl, '_blank')
      });
    } else {
      ElMessage.error(res.message || '订单创建失败');
    }
  } catch (error) {
    console.error('创建订单失败:', error);
    // ========== 优化：增加幂等性错误提示 ==========
    if (error.message && error.message.includes('请勿重复提交订单')) {
      ElMessage.error('请勿重复提交订单，请稍后查看订单状态');
    } else {
      ElMessage.error(error.message || '订单创建失败，请稍后重试');
    }
  } finally {
    submitting.value = false;
  }
};

// 处理支付
const handlePayment = async (orderNo) => {
  // 实际项目中这里应该调用支付接口，跳转到第三方支付页面或显示支付二维码
  ElMessage.success(`正在调起微信支付...`);

  // 模拟支付成功
  setTimeout(async () => {
    try {
      // 根据订单号查询订单ID
      const result = await findByOrderNoService(orderNo);
      const order = result.data
      if (order && order.id) {
        // 调用API更新订单状态为待发货(2)
        await updateOrderStatusService(order.id);
        ElMessage.success('支付成功，订单状态已更新');
      }
    } catch (error) {
      console.error('支付后更新订单状态失败:', error);
    }

    ElMessageBox.confirm(
        '支付成功！',
        '支付结果',
        {
          confirmButtonText: '查看订单',
          cancelButtonText: '返回商品页',
          type: 'success'
        }
    ).then(() => {
      const targetUrl = `${window.location.origin}/homes/myBought`;
      window.open(targetUrl, '_blank')
    }).catch(() => {
      router.push(`/goods/detail/${goodsId.value}`);
    });
  }, 1500);
};

// 新增：初始化数据的统一方法
const initPageData = async () => {
  // 重置商品详情（避免旧数据残留）
  goodsDetail.value = {
    id: 0,
    goodsName: '',
    goodsPic: '',
    sellPrice: 0,
    originalPrice: 0,
    sellerId: 0,
    sellerNickname: '',
    sellerAvatar: ''
  };

  // 先加载用户信息
  await getUserInfo();

  // 先加载商品详情
  await getGoodsDetail();

  // 先加载地址列表
  await getAddressList();

  // 检查是否从地址页面返回，获取选中的地址
  const savedAddress = sessionStorage.getItem('selectedPaymentAddress');
  if (savedAddress) {
    try {
      const parsedAddress = JSON.parse(savedAddress);
      console.log('从地址页面返回的选中地址:', parsedAddress);

      // 设置选中的地址
      selectedAddress.value = parsedAddress;

      // 清除已使用的地址信息
      sessionStorage.removeItem('selectedPaymentAddress');
    } catch (error) {
      console.error('解析地址信息失败:', error);
      // 如果解析失败，使用默认地址
      await getDefaultAddress();
    }
  } else {
    // 如果没有从地址页面返回，使用默认地址
    await getDefaultAddress();
  }
};

// 新增：监听路由参数变化
watch(
    () => route.query.goodsId,
    (newGoodsId) => {
      if (newGoodsId) {
        goodsId.value = Number(newGoodsId);
        // 路由参数变化时重新初始化页面数据
        initPageData();
      }
    },
    {immediate: true} // 立即执行一次，替代原来的onMounted中的逻辑
);
</script>

<template>
  <!-- 原有模板代码完全不变 -->
  <div class="payment-page">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="8" animated/>
    </div>

    <!-- 核心布局：左右分栏，对齐参考图 -->
    <div v-else class="main-container">
      <!-- 左侧主体：收货地址 + 订单信息 -->
      <div class="main-left">
        <!-- 收货地址模块：改为横向四列布局 -->
        <div class="xianyu-card address-card">
          <div class="card-header">
            <span class="card-title">收货地址</span>
            <span class="card-extra" @click="selectAddress()">管理地址</span>
          </div>

          <!-- 快递邮寄标签 -->
          <div class="delivery-tag">
            <el-icon>
              <Check/>
            </el-icon>
            <span>快递邮寄</span>
          </div>

          <!-- 地址列表：横向四列网格 -->
          <div class="address-list">
            <div
                v-for="addr in addressList"
                :key="addr.id"
                class="address-item"
                :class="{ active: selectedAddress.id === addr.id }"
                @click="selectAddress(addr)"
            >
              <div class="address-radio" :class="{ active: selectedAddress.id === addr.id }"></div>
              <div class="address-content">
                <div class="address-detail">
                  {{ addr.province }} {{ addr.city }} {{ addr.district }} {{ addr.detailAddress }}
                </div>
                <div class="receiver-info">
                  <span>{{ addr.receiverName }}</span>
                  <span>{{ addr.receiverPhone }}</span>
                </div>
              </div>
            </div>

            <!-- 兼容已有选中地址但未加载列表的情况 -->
            <div v-if="selectedAddress.id && addressList.length === 0" class="address-item active"
                 @click="selectAddress()">
              <div class="address-radio active"></div>
              <div class="address-content">
                <div class="address-detail">
                  {{ selectedAddress.province }} {{ selectedAddress.city }} {{ selectedAddress.district }}
                  {{ selectedAddress.detailAddress }}
                </div>
                <div class="receiver-info">
                  <span>{{ selectedAddress.receiverName }}</span>
                  <span>{{ selectedAddress.receiverPhone }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 订单信息模块：仅绑定接口数据 -->
        <div class="xianyu-card goods-card">
          <div class="card-header">
            <span class="card-title">订单信息</span>
          </div>
          <div class="goods-content">
            <img
                v-if="goodsDetail.goodsPic"
                :src="goodsDetail.goodsPic"
                :alt="goodsDetail.goodsName"
                class="goods-img"
            />
            <div v-else class="goods-img-placeholder">暂无图片</div>

            <div class="goods-info">
              <div class="goods-name">{{ goodsDetail.goodsName || '暂无商品名称' }}</div>
              <div class="goods-price">¥{{ Number(goodsDetail.sellPrice).toFixed(2) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧价格栏：仅绑定接口数据 -->
      <div class="main-right">
        <div class="price-card">
          <div class="price-header">价格明细</div>
          <div class="price-item">
            <span>商品总价 共1件宝贝</span>
            <span>¥{{ Number(goodsDetail.sellPrice).toFixed(2) }}</span>
          </div>
          <div class="price-item">
            <span>运费</span>
            <span>¥0.00</span>
          </div>
          <div class="price-divider"></div>
          <div class="price-total">
            <span>合计:</span>
            <span class="total-amount">¥{{ totalAmount }}</span>
          </div>

          <!-- 确认购买按钮：禁用态保护 -->
          <el-button
              type="primary"
              class="confirm-buy-btn"
              :loading="submitting"
              @click="submitOrder"
              :disabled="!selectedAddress.id || !goodsDetail.id"
          >
            确认购买
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
/* 原有样式代码完全不变 */
// 全局样式
.payment-page {
  padding-top: 100px;
  min-height: 100vh;
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Helvetica Neue", Arial, sans-serif;
}

// 顶部导航：闲鱼黄色，改为Logo和用户信息
.payment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 40px;
  background-color: #ffd600;
  color: #000;
  font-weight: 700;

  .header-logo {
    font-size: 32px;
    letter-spacing: 2px;
  }

  .header-user {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 14px;
    font-weight: 500;

    .user-avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background-color: #fff;
      background-image: url('https://via.placeholder.com/32');
      background-size: cover;
    }

    .order-entry {
      cursor: pointer;
      text-decoration: underline;
    }
  }
}

// 加载状态
.loading-container {
  padding: 32px;
  max-width: 1600px;
  margin: 0 auto;
}

// 核心分栏布局 - 增大占比，减少空白
.main-container {
  display: flex;
  gap: 24px;
  padding: 32px 48px;
  max-width: 1600px;
  margin: 0 auto;
  align-items: flex-start;

  // 左侧主体 - 增大占比和间距
  .main-left {
    flex: 1.8;
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  // 右侧价格栏 - 增大宽度
  .main-right {
    flex: 1;
    min-width: 320px;
    max-width: 400px;
  }

  // 平板端适配
  @media (max-width: 1024px) {
    padding: 24px 32px;
    gap: 20px;

    .main-right {
      min-width: 280px;
    }
  }

  // 移动端适配为单列
  @media (max-width: 768px) {
    flex-direction: column;
    padding: 20px 16px;
    gap: 20px;

    .main-right {
      max-width: 100%;
      width: 100%;
    }
  }
}

// 通用卡片样式 - 增大尺寸和内边距
.xianyu-card {
  background-color: #fff;
  border-radius: 16px;
  padding: 28px 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    .card-title {
      font-size: 18px;
      font-weight: 600;
      color: #111;
    }

    .card-extra {
      font-size: 15px;
      color: #ff4400;
      cursor: pointer;

      &:hover {
        opacity: 0.8;
      }
    }
  }
}

// 地址卡片 - 增大整体尺寸和内容占比
.address-card {
  .delivery-tag {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;
    color: #333;
    margin-bottom: 24px;

    .el-icon {
      color: #ffd600;
      font-size: 20px;
    }
  }

  .address-list {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;

    .address-item {
      border: 2px solid #e6e6e6;
      border-radius: 12px;
      padding: 24px;
      cursor: pointer;
      position: relative;
      transition: all 0.3s ease;

      &:hover {
        border-color: #ffaa80;
      }

      &.active {
        border-color: #ff9000;
        background-color: #fffbf5;
      }

      .address-radio {
        position: absolute;
        top: 16px;
        left: 16px;
        width: 20px;
        height: 20px;
        border-radius: 50%;
        border: 2px solid #999;
        background-color: #fff;

        &.active {
          border-color: #ff9000;
          background-color: #ff9000;
        }

        &.active::after {
          content: '';
          position: absolute;
          top: 3px;
          left: 3px;
          width: 10px;
          height: 10px;
          border-radius: 50%;
          background-color: #fff;
        }
      }

      .address-content {
        margin-left: 32px;

        .address-detail {
          font-size: 15px;
          color: #333;
          line-height: 1.6;
          margin-bottom: 12px;
        }

        .receiver-info {
          display: flex;
          gap: 16px;
          font-size: 15px;
          color: #666;
        }
      }
    }
  }
}

// 商品卡片 - 增大图片和文字尺寸
.goods-card {
  .goods-content {
    display: flex;
    gap: 24px;

    // 增大商品图片尺寸
    .goods-img {
      width: 100px;
      height: 100px;
      border-radius: 12px;
      object-fit: cover;
      flex-shrink: 0;
    }

    // 占位图同步增大
    .goods-img-placeholder {
      width: 100px;
      height: 100px;
      border-radius: 12px;
      background-color: #f5f5f5;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      color: #999;
      flex-shrink: 0;
    }

    .goods-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: center;
      gap: 16px;

      .goods-name {
        font-size: 18px;
        color: #111;
        line-height: 1.6;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .goods-price {
        font-size: 22px;
        color: #ff4400;
        font-weight: 600;
      }
    }
  }
}

// 右侧价格栏 - 增大尺寸和占比
.price-card {
  background-color: #fff;
  border-radius: 16px;
  padding: 32px;
  position: sticky;
  top: 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  height: fit-content;
  min-height: 380px;

  .price-header {
    font-size: 18px;
    font-weight: 600;
    color: #111;
    margin-bottom: 28px;
  }

  .price-item {
    display: flex;
    justify-content: space-between;
    font-size: 15px;
    color: #666;
    margin-bottom: 20px;

    span:first-child {
      color: #888;
    }

    span:last-child {
      color: #333;
      font-weight: 500;
    }
  }

  .price-divider {
    height: 1px;
    background-color: #e6e6e6;
    margin: 24px 0;
    border-top: 1px dashed #e6e6e6;
  }

  .price-total {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    margin-bottom: 32px;

    span:first-child {
      font-size: 16px;
      color: #111;
      font-weight: 500;
    }

    .total-amount {
      font-size: 28px;
      font-weight: 700;
      color: #ff4400;
    }
  }

  // 确认购买按钮 - 增大尺寸
  .confirm-buy-btn {
    width: 100%;
    height: 52px;
    background-color: #ff4400;
    border: none;
    border-radius: 26px;
    font-size: 17px;
    font-weight: 600;
    color: #fff;
    transition: all 0.3s ease;

    &:hover {
      background-color: #f33;
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(255, 68, 0, 0.3);
    }

    &:disabled {
      background-color: #ffaa80;
      cursor: not-allowed;
      transform: none;
      box-shadow: none;
    }
  }
}
</style>
