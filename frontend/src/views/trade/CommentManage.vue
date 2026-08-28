<script setup lang="js">
import { Edit, Delete, Search, Refresh } from '@element-plus/icons-vue'
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from "element-plus"
import {
  commentAddService,
  commentDeleteService,
  commentListService,
  commentUpdateServices
} from "@/api/comment.js"

// --- 数据模型 ---
// 核心：articleId 改为 goodsId
const goodsId = ref()
const username = ref('')
const content = ref('')
const comments = ref([])
const loading = ref(false)

// 分页数据模型
const pageNum = ref(1)
const total = ref(0)
const pageSize = ref(10)

// 获取评论分页列表数据
const CommentList = async () => {
  loading.value = true
  try {
    let params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      goodsId: goodsId.value || null, // 核心：articleId 改为 goodsId
    }
    let result = await commentListService(params)
    comments.value = result.data.items
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

CommentList()

// 分页逻辑
const onSizeChange = (size) => {
  pageSize.value = size
  CommentList()
}
const onCurrentChange = (num) => {
  pageNum.value = num
  CommentList()
}

// --- 弹窗逻辑 ---
const dialogVisible = ref(false)
const title = ref('')
// 核心：commentModel 中的 articleId 改为 goodsId
const commentModel = ref({
  goodsId: '',
  content: '',
  username: '',
  id: ''
})

// 核心：校验规则中的 articleId 改为 goodsId，文案从“文章”改为“商品”
const rules = {
  goodsId: [{ required: true, message: '请输入商品编号', trigger: 'blur' }],
  content: [{ required: true, message: '请输入评论内容', trigger: 'blur' }],
  username: [{ required: true, message: '请输入评论用户', trigger: 'blur' }]
}

const clearData = () => {
  // 核心：重置数据时改为 goodsId
  commentModel.value = { goodsId: '', content: '', username: '', id: '' }
}

const showDialog = (row) => {
  dialogVisible.value = true
  title.value = '修改评论'
  commentModel.value = { ...row } // 对象拷贝
}

// 保存（新增/修改）
const handleConfirm = async () => {
  // 核心：校验 goodsId 而非 articleId
  if (!commentModel.value.goodsId || !commentModel.value.content) {
    ElMessage.warning('主要内容不能为空')
    return
  }

  if (title.value === '新增评论') {
    await commentAddService(commentModel.value)
    ElMessage.success('添加成功')
  } else {
    await commentUpdateServices(commentModel.value)
    ElMessage.success('修改成功')
  }

  await CommentList()
  dialogVisible.value = false
}

// 删除逻辑
const deleteComment = (row) => {
  ElMessageBox.confirm('你确认要删除该评论信息吗?', '温馨提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await commentDeleteService(row.id)
    ElMessage.success('删除成功')
    await CommentList()
  })
}
</script>

<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <!-- 核心：标题从“评论管理”改为“商品评论管理” -->
        <span class="title-text">商品评论管理</span>
        <el-button type="primary" :icon="Edit" @click="dialogVisible = true; title='新增评论'; clearData()">新增评论</el-button>
      </div>
    </template>

    <div class="search-area">
      <el-form inline>
        <!-- 核心：标签从“文章编号”改为“商品编号”，v-model 改为 goodsId -->
        <el-form-item label="商品编号">
          <el-input v-model="goodsId" placeholder="输入编号" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="CommentList">搜索</el-button>
          <!-- 核心：重置时清空 goodsId 而非 articleId -->
          <el-button :icon="Refresh" @click="goodsId=''; username=''; CommentList()">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-wrapper">
      <el-table
          :data="comments"
          height="100%"
          style="width: 100%"
          v-loading="loading"
          border
          stripe
      >
        <el-table-column label="评论编号" prop="id" width="90" align="center" />

        <!-- 核心：列标签从“所属文章”改为“所属商品”，prop 改为 goodsId -->
        <el-table-column label="所属商品" prop="goodsId" width="120" align="center" />

        <el-table-column label="评论用户" prop="nickname" min-width="120" align="center" show-overflow-tooltip />

        <el-table-column label="评论内容" prop="content" min-width="250" align="center" show-overflow-tooltip />

        <el-table-column label="创建时间" prop="createTime" width="180" align="center" />

        <el-table-column label="更新时间" prop="updateTime" width="180" align="center" />

        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button :icon="Edit" type="primary" link @click="showDialog(row)">编辑</el-button>
            <el-button :icon="Delete" type="danger" link @click="deleteComment(row)">删除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无相关评论" />
        </template>
      </el-table>
    </div>

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

    <el-dialog v-model="dialogVisible" :title="title" width="420px" destroy-on-close>
      <el-form :model="commentModel" :rules="rules" label-width="90px" style="padding-right: 20px">
        <!-- 核心：标签改为“商品编号”，prop 和 v-model 改为 goodsId -->
        <el-form-item label="商品编号" prop="goodsId">
          <el-input v-model="commentModel.goodsId" placeholder="请输入关联的商品ID" />
        </el-form-item>
        <el-form-item label="评论用户" prop="username">
          <el-input v-model="commentModel.username" placeholder="请输入用户名称" />
        </el-form-item>
        <el-form-item label="评论内容" prop="content">
          <el-input
              v-model="commentModel.content"
              type="textarea"
              :rows="3"
              placeholder="请输入评论具体内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确认</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style lang="scss" scoped>
/* 整个卡片容器锁定页面高度 */
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
    .title-text { font-weight: bold; font-size: 16px; color: #303133; }
  }
}

/* 搜索区域固定高度 */
.search-area {
  flex-shrink: 0;
  margin-bottom: 12px;
}

/* 表格包装层：自适应滚动 */
.table-wrapper {
  flex: 1;
  overflow: hidden;
  margin-bottom: 12px;
}

/* 分页条固定底部 */
.pagination-area {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
}

/* 单元格样式微调 */
:deep(.el-table__cell) {
  padding: 12px 0 !important;
}

:deep(.el-table__header th) {
  background-color: #fafafa;
  color: #333;
  font-weight: bold;
}
</style>