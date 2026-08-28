<script setup lang="js">
import { ref, watch, onMounted } from 'vue';
import { Camera, InfoFilled, Plus, Refresh, Upload } from '@element-plus/icons-vue';
import useUserInfoStore from "@/stores/userInfo.js";
import { useTokenStore } from '@/stores/token.js';
import { userAvatarUpdateService } from "@/api/user.js";
import { goodsOpenListService } from "@/api/goods.js"; // 商品列表接口
import { commentallListService } from "@/api/comment.js"; // 评论列表接口
import { ElMessage } from 'element-plus';
import userlogin from "@/assets/default.png";
import { useRouter } from 'vue-router';
import { getUploadUrl } from '@/utils/serviceUrls.js';

const router = useRouter();
const userInfoStore = useUserInfoStore();
const tokenStore = useTokenStore();
const activeTab = ref('baobei');

// 加载状态
const loading = ref(false);

// 头像弹窗相关
const showAvatarDialog = ref(false);
const imgUrl = ref('');

// 商品和评论数据
const myGoods = ref([]); // 我的商品列表
const commentList = ref([]); // 我的评论列表

// 监听弹窗显示，初始化头像URL
watch(showAvatarDialog, (newVal) => {
  if (newVal) imgUrl.value = userInfoStore.info?.userPic || '';
});

// 监听标签页切换，确保数据加载
watch(activeTab, (newTab) => {
  if (newTab === 'baobei' && myGoods.length === 0) {
    loadMyGoods();
  } else if (newTab === 'credit' && commentList.length === 0) {
    loadMyComments();
  }
});

// 头像上传逻辑
const uploadSuccess = (result) => {
  imgUrl.value = result.data.url;
  ElMessage.success('图片已上传，请点击确认修改保存');
};

// 更新头像
const updateAvatar = async () => {
  if (!imgUrl.value) return ElMessage.warning('请先选择并上传图片');
  try {
    let result = await userAvatarUpdateService(imgUrl.value);
    ElMessage.success(result.data ? result.data : '头像修改成功');
    // 安全更新用户信息
    if (userInfoStore.info) {
      userInfoStore.info.userPic = imgUrl.value;
    }
    showAvatarDialog.value = false;
  } catch (error) {
    console.error('头像更新失败:', error);
    ElMessage.error('修改失败，请稍后重试');
  }
};

// 加载我的商品列表
const loadMyGoods = async () => {
  if (!userInfoStore.info?.id) return;

  try {
    loading.value = true;
    const queryData = {
      sellerId: userInfoStore.info.id, // 当前用户ID作为卖家ID
      pageNum: 1,
      pageSize: 100
    };
    const res = await goodsOpenListService(queryData);
    myGoods.value = res.data.items || [];
  } catch (error) {
    console.error('加载我的商品失败:', error);
    ElMessage.error('加载商品列表失败');
  } finally {
    loading.value = false;
  }
};

// 加载我的评论列表
const loadMyComments = async () => {
  if (!userInfoStore.info?.id) return;
  try {
    loading.value = true;
    const res = await commentallListService(userInfoStore.info.id);
    commentList.value = res.data || [];
  } catch (error) {
    console.error('加载我的评论失败:', error);
    ElMessage.error('加载评论列表失败');
  } finally {
    loading.value = false;
  }
};

// 格式化时间
const formatTime = (time) => {
  if (!time) return '';
  const date = new Date(time);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).replace(/\//g, '-');
};

// 跳转到商品详情
const goToGoodsDetail = (id) => {
  router.push(`/goods/detail/${id}`);
};

// 页面挂载时加载默认数据
onMounted(() => {
  loadMyGoods(); // 默认加载商品列表
  loadMyComments() // 默认加载评论列表
});

const uploadUrl = getUploadUrl();
</script>

<template>
  <div class="user-profile-container">
    <section class="user-banner">
      <div class="banner-bg-elements"></div>
      <div class="user-info-box">
        <div class="avatar-area" @click="showAvatarDialog = true">
          <img :src="userInfoStore.info.userPic || userlogin" alt="avatar">
          <div class="edit-overlay"><el-icon><Camera /></el-icon></div>
        </div>
        <div class="text-area">
          <h1 class="nickname">{{ userInfoStore.info.nickname || userInfoStore.info.username }}</h1>
          <div class="stats">
            <span>重庆市</span>
            <span class="divider">|</span>
            <span><b>1</b> 粉丝</span>
            <span class="divider">|</span>
            <span><b>1</b> 关注</span>
          </div>
        </div>
        <button class="edit-profile-btn" @click="router.push('/homes/userinfo')">编辑资料</button>
      </div>
    </section>

    <el-dialog v-model="showAvatarDialog" title="更换头像" width="460px" align-center destroy-on-close>
      <div class="avatar-dialog-inner">
        <div class="alert-banner">
          <el-icon><InfoFilled /></el-icon>
          <span>预览后请点击“确认修改”按钮以保存。</span>
        </div>
        <div class="preview-container no-select ">
          <el-upload
              class="inner-uploader"
              :action="uploadUrl"
              name="file"
              :headers="{ Authorization: tokenStore.token }"
              :show-file-list="false"
              :on-success="uploadSuccess"
          >
            <img v-if="imgUrl" :src="imgUrl" class="preview-img"  alt=""/>
            <el-icon v-else class="uploader-placeholder"><Plus /></el-icon>
            <div class="upload-tip-mask">
              <el-icon><Refresh /></el-icon>
              <span>更换图片</span>
            </div>
          </el-upload>
        </div>
        <div class="dialog-footer">
          <el-button @click="showAvatarDialog = false">取消</el-button>
          <el-button type="primary" :icon="Upload" @click="updateAvatar">确认修改</el-button>
        </div>
      </div>
    </el-dialog>

    <nav class="content-tabs">
      <div
          :class="['tab-item', { active: activeTab === 'baobei' }]"
          @click="activeTab = 'baobei'"
      >
        宝贝 <span>{{ myGoods.length || 0 }}</span>
      </div>
      <div
          :class="['tab-item', { active: activeTab === 'credit' }]"
          @click="activeTab = 'credit'"
      >
        信用及评价 <span>{{ commentList.length || 0 }}</span>
      </div>
    </nav>

    <!-- 商品列表区域 -->
    <div class="content-grid" v-show="activeTab === 'baobei'">
      <!-- 加载中的状态 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="4" animated />
      </div>

      <!-- 商品列表 -->
      <div
          v-for="item in myGoods"
          :key="item.id"
          class="baobei-card"
          @click="goToGoodsDetail(item.id)"
      >
        <div class="card-image">
          <img :src="item.goodsPic || userlogin" :alt="item.goodsName" />
        </div>
        <div class="card-info">
          <div class="title-wrap">
            <span class="tag-shipping" v-if="item.postageFree">包邮</span>
            <span class="title">{{ item.goodsName }}</span>
          </div>
          <div class="price-row">
            <span class="price-symbol">¥</span>
            <span class="price-num">{{ Number(item.sellPrice).toFixed(2) }}</span>
            <span class="want-count">{{ item.collectCount || 0 }}人想要</span>
          </div>
          <div class="user-tag">
            <img :src="userInfoStore.info.userPic || userlogin" alt="">
            <span>{{ userInfoStore.info.nickname || userInfoStore.info.username }}</span>
          </div>
        </div>
      </div>

      <!-- 无商品提示 -->
      <div v-if="!loading && myGoods.length === 0" class="no-data">
        <div class="empty-placeholder">
          <p>暂无在售商品</p>
        </div>
      </div>
    </div>

    <!-- 评价列表区域 -->
    <div class="content-grid comment-grid" v-show="activeTab === 'credit'">
      <!-- 加载中的状态 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="3" animated />
      </div>

      <!-- 评论列表 -->
      <div v-if="!loading && commentList.length > 0" class="comment-list">
        <div
            v-for="item in commentList"
            :key="item.id"
            class="comment-item"
        >
          <div class="comment-avatar">
            <img :src="item.userUrl || userlogin" alt="" />
          </div>
          <div class="comment-content">
            <div class="comment-header">
              <span class="comment-nickname">{{ item.nickname || '匿名用户' }}</span>
            </div>
            <div class="comment-text">{{ item.content }}</div>
            <div class="comment-time">
              {{ formatTime(item.createTime) }}
            </div>
          </div>
        </div>
      </div>

      <!-- 无评论提示 -->
      <div v-if="!loading && commentList.length === 0" class="no-data">
        <div class="empty-placeholder">
          <p>暂无信用及评价信息</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.user-profile-container {
  min-height: calc(100vh - 60px);
  background:
    radial-gradient(circle at top left, rgba(255, 224, 130, 0.18), transparent 24%),
    radial-gradient(circle at top right, rgba(110, 231, 183, 0.14), transparent 26%),
    linear-gradient(180deg, #f8fafc 0%, #f8f8f8 100%);
  padding: 20px 20px 28px;
}

.user-banner {
  max-width: 1280px;
  margin: 0 auto;
  min-height: 220px;
  background: linear-gradient(135deg, #fff9e6 0%, #fff2c2 52%, #fffdf6 100%);
  position: relative;
  padding: 36px 40px;
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 0 24px 50px rgba(15, 23, 42, 0.08);
  border: 1px solid rgba(255, 215, 64, 0.28);

  .banner-bg-elements {
    position: absolute;
    right: -30px;
    top: -20px;
    width: 280px;
    height: 280px;
    opacity: 0.7;
    border-radius: 50%;
    background:
      radial-gradient(circle, rgba(255,255,255,0.7) 0%, rgba(255,255,255,0) 68%),
      radial-gradient(circle at 30% 30%, rgba(255,215,64,0.42), rgba(255,215,64,0) 70%);
  }

  .user-info-box {
    display: flex;
    align-items: center;
    gap: 24px;
    position: relative;
    z-index: 1;
    min-height: 148px;

    .avatar-area {
      width: 108px;
      height: 108px;
      border-radius: 50%;
      border: 4px solid rgba(255, 255, 255, 0.96);
      overflow: hidden;
      cursor: pointer;
      position: relative;
      box-shadow: 0 18px 34px rgba(15, 23, 42, 0.14);

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      .edit-overlay {
        position: absolute;
        inset: 0;
        background: rgba(0, 0, 0, 0.3);
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
        opacity: 0;
        transition: opacity 0.3s ease;
      }

      &:hover .edit-overlay {
        opacity: 1;
      }
    }

    .text-area {
      flex: 1;

      .nickname {
        font-size: 34px;
        margin: 0 0 12px 0;
        color: #1f2937;
        line-height: 1.15;
      }

      .stats {
        font-size: 14px;
        color: #475569;
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 12px;
        padding: 10px 14px;
        width: fit-content;
        border-radius: 999px;
        background: rgba(255, 255, 255, 0.72);
        border: 1px solid rgba(255, 255, 255, 0.9);

        .divider {
          color: #cbd5e1;
        }

        b {
          color: #1f2937;
        }
      }
    }

    .edit-profile-btn {
      margin-left: auto;
      align-self: flex-start;
      background: rgba(15, 23, 42, 0.76);
      color: #fff;
      border: none;
      padding: 10px 18px;
      border-radius: 999px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      transition: background-color 0.3s ease;
      backdrop-filter: blur(4px);

      &:hover {
        background: rgba(0, 0, 0, 0.5);
      }
    }
  }
}

.content-tabs {
  max-width: 1280px;
  margin: 18px auto 0;
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: #fff;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 22px 22px 0 0;
  padding: 0 18px;
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.05);

  .tab-item {
    padding: 16px 18px;
    font-size: 15px;
    cursor: pointer;
    color: #666;
    position: relative;
    transition: color 0.3s ease;

    span {
      font-size: 12px;
      margin-left: 4px;
      color: #ff6a00;
    }

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 0;
      height: 3px;
      background: linear-gradient(90deg, #ffd43b, #ffb703);
      transition: width 0.3s ease;
    }

    &.active {
      color: #333;
      font-weight: bold;

      &::after {
        width: 20px;
      }
    }

    &:hover {
      color: #ffda00;
    }
  }
}

.content-grid {
  max-width: 1280px;
  margin: 0 auto;
  padding: 22px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 18px;
  min-height: 300px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-top: none;
  border-radius: 0 0 24px 24px;
  box-shadow: 0 18px 34px rgba(15, 23, 42, 0.05);

  // 评论列表特殊样式
  &.comment-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .loading-container {
    grid-column: 1 / -1;
    padding: 20px;
  }

  .no-data {
    grid-column: 1 / -1;
    display: flex;
    align-items: center;
    justify-content: center;
    height: 200px;

    .empty-placeholder {
      height: 150px;
      width: 100%;
      max-width: 500px;
      border: 1px dashed #e0e0e0;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #fafafa;

      p {
        font-size: 16px;
        color: #999;
        margin: 0;
      }
    }
  }

  .baobei-card {
    background: #fff;
    border-radius: 18px;
    overflow: hidden;
    border: 1px solid #eef2f7;
    transition: all 0.3s ease;
    cursor: pointer;
    box-shadow: 0 10px 22px rgba(15, 23, 42, 0.04);

    &:hover {
      transform: translateY(-6px);
      box-shadow: 0 16px 30px rgba(15, 23, 42, 0.08);
    }

    .card-image {
      height: 180px;
      overflow: hidden;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        transition: transform 0.3s ease;
      }

      &:hover img {
        transform: scale(1.05);
      }
    }

    .card-info {
      padding: 14px;

      .title-wrap {
        font-size: 13px;
        color: #333;
        line-height: 18px;
        margin-bottom: 8px;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;

        .tag-shipping {
          display: inline-block;
          background-color: #ffd100;
          color: #333;
          font-size: 11px;
          padding: 1px 4px;
          border-radius: 4px;
          margin-right: 4px;
          vertical-align: text-bottom;
          font-weight: bold;
        }
      }

      .price-row {
        display: flex;
        align-items: baseline;
        margin-bottom: 12px;

        .price-symbol {
          color: #ff4d4f;
          font-size: 12px;
          font-weight: bold;
        }

        .price-num {
          color: #ff4d4f;
          font-size: 18px;
          font-weight: bold;
          margin-right: 6px;
        }

        .want-count {
          color: #999;
          font-size: 11px;
        }
      }

      .user-tag {
        display: flex;
        align-items: center;
        gap: 5px;
        font-size: 11px;
        color: #999;

        img {
          width: 16px;
          height: 16px;
          border-radius: 50%;
        }
      }
    }
  }

  // 评论列表样式
  .comment-list {
    width: 100%;
    padding: 6px 0;

    .comment-item {
      display: flex;
      gap: 12px;
      padding: 18px 0;
      border-bottom: 1px solid #f1f5f9;
      width: 100%;

      &:last-child {
        border-bottom: none;
      }

      .comment-avatar {
        flex-shrink: 0;
        width: 40px;
        height: 40px;
        border-radius: 50%;
        overflow: hidden;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
      }

      .comment-content {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 8px;

        .comment-header {
          font-size: 14px;
          color: #333;
          font-weight: 500;
        }

        .comment-text {
          font-size: 14px;
          color: #333;
          line-height: 1.6;
        }

        .comment-time {
          font-size: 12px;
          color: #999;
        }
      }
    }
  }
}

/* 头像弹窗样式 */
.avatar-dialog-inner {
  .alert-banner {
    display: flex;
    align-items: center;
    gap: 10px;
    background: #f1f5f9;
    padding: 12px 16px;
    border-radius: 10px;
    color: #475569;
    font-size: 13px;
    margin-bottom: 10px;
  }

  .preview-container {
    display: flex;
    justify-content: center;
    margin: 30px 0;

    .inner-uploader {
      width: 180px;
      height: 180px;
      border-radius: 50%;
      overflow: hidden;
      border: 2px dashed #dcdfe6;
      position: relative;
      cursor: pointer;

      .preview-img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      .upload-tip-mask {
        position: absolute;
        inset: 0;
        background: rgba(0,0,0,0.4);
        color: #fff;
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        opacity: 0;
        transition: opacity 0.3s;

        .el-icon {
          font-size: 24px;
          margin-bottom: 5px;
        }
      }

      &:hover .upload-tip-mask {
        opacity: 1;
      }
    }
  }

  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 20px;
  }
}

:deep(.el-dialog) {
  border-radius: 24px;
}

// 响应式适配
@media screen and (max-width: 768px) {
  .user-profile-container {
    padding: 12px;
  }

  .user-banner {
    padding: 22px 18px;
    height: auto;

    .user-info-box {
      flex-direction: column;
      align-items: flex-start;
      gap: 15px;

      .text-area {
        width: 100%;

        .nickname {
          font-size: 26px;
        }
      }

      .edit-profile-btn {
        margin-left: 0;
        width: 100%;
        text-align: center;
      }
    }
  }

  .content-grid {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    padding: 14px;
  }

  .content-tabs {
    padding: 0 10px;
    overflow-x: auto;
  }
}
</style>
