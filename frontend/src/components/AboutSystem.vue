<template>
  <div class="about-container">
    <div class="page-header">
<!--      <el-button @click="goBack" :icon="ArrowLeft" circle />-->
      <span class="title">关于 fsy-trade 二手交易平台</span>
    </div>

    <div class="content-wrapper">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="16">
          <el-card shadow="hover" class="info-card">
            <template #header>
              <div class="card-header">
                <el-icon><InfoFilled /></el-icon>
                <span>项目概览</span>
              </div>
            </template>
            <p class="desc-text">
              <strong>fsy-trade</strong> 是一款基于 <span>Vue3 + Element Plus + Node.js</span> 开发的全栈二手交易平台管理系统。
              项目从0到1完成了完整的搭建，涵盖了电商交易的核心闭环。
            </p>
            <el-divider />
            <div class="milestones">
              <el-statistic title="累计开发时长" :value="3" suffix="个月" />
              <el-statistic title="代码提交次数" :value="208" suffix="次" />
              <el-statistic title="核心功能模块" :value="12" suffix="个" />
            </div>
          </el-card>

          <el-card shadow="hover" class="info-card mt-20">
            <template #header>
              <div class="card-header">
                <el-icon><Management /></el-icon>
                <span>技术栈 & 方案</span>
              </div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="前端框架">Vue3 (Composition API)</el-descriptions-item>
              <el-descriptions-item label="构建工具">Vite</el-descriptions-item>
              <el-descriptions-item label="UI 组件库">Element Plus</el-descriptions-item>
              <el-descriptions-item label="权限控制">RBAC 角色访问控制</el-descriptions-item>
              <el-descriptions-item label="后端技术">Node.js + JWT 认证</el-descriptions-item>
              <el-descriptions-item label="设计参考">Antdv Pro 布局方案</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="8">
          <el-card shadow="hover" class="side-card status-card">
            <div class="status-box">
              <el-tag size="large" type="danger" effect="dark">持续迭代中</el-tag>
              <p class="future-text">后续将逐步优化交易流程、完善支付体系、新增数据可视化大屏等功能...</p>
            </div>
          </el-card>

          <el-card shadow="hover" class="side-card mt-20">
            <template #header>
              <span>加入交流</span>
            </template>
            <div class="community-box">
              <p>欢迎探讨技术问题：</p>
              <el-tag type="success" class="group-tag">QQ群：635887836</el-tag>
              <div class="link-btns">
                <el-link type="primary" href="https://docs.antdv-pro.com/" target="_blank">参考文档</el-link>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ArrowLeft, InfoFilled, Management } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const goBack = () => {
  // 如果有历史记录则返回，否则回首页
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/article/category')
  }
}
</script>

<style lang="scss" scoped>
.about-container {
  /* 关键：确保不出现全局滚动条 */
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;

  .page-header {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px 24px;
    background: #fff;
    border-bottom: 1px solid #ebeef5;
    .title {
      font-size: 18px;
      font-weight: bold;
      color: #303133;
    }
  }

  .content-wrapper {
    flex: 1;
    overflow-y: auto; /* 内容过多时在内部滚动，不影响全局 */
    padding: 24px;
    background-color: #f5f7fa;

    /* 解决文字换行 */
    word-break: break-all;
    white-space: normal;
  }

  .info-card {
    border-radius: 8px;
    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: bold;
    }
    .desc-text {
      font-size: 15px;
      line-height: 1.8;
      color: #606266;
      span {
        color: #409eff;
        font-weight: bold;
      }
    }
  }

  .milestones {
    display: flex;
    justify-content: space-around;
    padding: 10px 0;
  }

  .mt-20 { margin-top: 20px; }

  .status-card {
    background: linear-gradient(135deg, #fff 0%, #fff0f0 100%);
    text-align: center;
    .status-box {
      padding: 10px 0;
      .future-text {
        margin-top: 15px;
        font-size: 14px;
        color: #f56c6c;
        font-weight: bold;
      }
    }
  }

  .community-box {
    text-align: center;
    .group-tag {
      font-size: 16px;
      padding: 12px 20px;
      margin: 10px 0;
    }
    .link-btns {
      margin-top: 15px;
    }
  }
}

/* 适配移动端 */
@media (max-width: 768px) {
  .milestones {
    flex-direction: column;
    gap: 20px;
  }
}
</style>