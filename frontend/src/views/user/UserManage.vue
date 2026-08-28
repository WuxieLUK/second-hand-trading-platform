<script setup lang="js">
import { Edit, Delete, Search, Refresh, UserFilled } from '@element-plus/icons-vue'
import { ref } from 'vue'
import { userUpdateInfoServices, userListService, userDeleteService } from '@/api/user.js'
import { ElMessage, ElMessageBox } from "element-plus"
import router from "@/router/index.js"

// --- 数据模型 ---
const username = ref("")
const email = ref('')
// 新增：手机号搜索字段
const phone = ref('')
const users = ref([])
const selectedUserIds = ref([])
const loading = ref(false)

// 分页数据模型
const pageNum = ref(1)
const total = ref(0)
const pageSize = ref(10)

// 获取列表数据
const UserList = async () => {
  loading.value = true
  try {
    let params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      username: username.value || null,
      email: email.value || null,
      // 新增：手机号搜索参数
      phone: phone.value || null
    }
    let result = await userListService(params)
    users.value = result.data.items
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

UserList()

// 分页逻辑
const onSizeChange = (size) => {
  pageSize.value = size
  UserList()
}
const onCurrentChange = (num) => {
  pageNum.value = num
  UserList()
}

// --- 弹窗逻辑 ---
const dialogVisible = ref(false)
const title = ref('编辑用户')
const userModel = ref({
  username: '',
  nickname: '',
  email: '',
  // 新增：手机号字段
  phone: '',
  id: ''
})

// 新增：手机号验证规则
const rules = {
  username: [{ required: true, message: '请输入用户名称', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入用户昵称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入用户邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号码', trigger: 'blur' }
  ]
}

// 新增：表单引用，用于编辑时的表单校验
const formRef = ref(null)

const showDialog = (row) => {
  title.value = '编辑用户'
  dialogVisible.value = true
  // 兜底处理：如果行数据没有phone字段，初始化为空
  userModel.value = {
    ...row,
    phone: row.phone || ''
  }
}

const updateUser = async () => {
  // 新增：表单校验
  try {
    await formRef.value.validate()
  } catch (error) {
    ElMessage.error('表单填写有误，请检查后重新提交')
    return
  }

  try {
    await userUpdateInfoServices(userModel.value)
    ElMessage.success('修改成功')
    await UserList()
    dialogVisible.value = false
  } catch (error) {
    ElMessage.error('修改失败，请稍后重试')
    console.error('用户修改失败：', error)
  }
}

// --- 删除逻辑 ---
const deleteUser = (row) => {
  ElMessageBox.confirm('你确认要删除该用户信息吗?', '温馨提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await userDeleteService(row.id)
    ElMessage.success('删除成功')
    await UserList()
  })
}

// 批量删除
const batchDeleteUsers = async () => {
  if (selectedUserIds.value.length === 0) return
  ElMessageBox.confirm(`确认要批量删除选中的 ${selectedUserIds.value.length} 个用户吗？`, '警告', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'danger'
  }).then(async () => {
    for (const id of selectedUserIds.value) {
      await userDeleteService(id)
    }
    ElMessage.success('批量删除成功')
    selectedUserIds.value = []
    await UserList()
  }).catch(() => {})
}

const handleSelectionChange = (selection) => {
  selectedUserIds.value = selection.map(row => row.id)
}

const RolesGrant = (row) => {
  router.push({
    path: '/roles/rolesGrant',
    query: { id: row.id, username: row.username }
  })
}
</script>

<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <span class="title-text">用户管理</span>
      </div>
    </template>

    <div class="search-area">
      <el-form inline>
        <el-form-item label="用户名称">
          <el-input v-model="username" placeholder="输入名称搜索" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="用户邮箱">
          <el-input v-model="email" placeholder="输入邮箱搜索" clearable style="width: 200px" />
        </el-form-item>
        <!-- 新增：手机号搜索框 -->
        <el-form-item label="手机号码">
          <el-input
              v-model="phone"
              placeholder="输入手机号搜索"
              clearable
              style="width: 200px"
              maxlength="11"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="UserList">搜索</el-button>
          <el-button :icon="Refresh" @click="username=''; email=''; phone=''; UserList()">重置</el-button>
          <el-button
              type="danger"
              :icon="Delete"
              @click="batchDeleteUsers"
              :disabled="selectedUserIds.length === 0"
          >
            批量删除 ({{ selectedUserIds.length }})
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-wrapper">
      <el-table
          :data="users"
          height="100%"
          style="width: 100%"
          v-loading="loading"
          @selection-change="handleSelectionChange"
          border
          stripe
      >
        <el-table-column type="selection" width="55" align="center" />

        <el-table-column label="编号" width="80" prop="id" align="center" />

        <el-table-column label="用户头像" width="130" align="center">
          <template #default="{row}">
            <div class="avatar-container">
              <el-image
                  class="rect-avatar"
                  :src="row.userPic"
                  :preview-src-list="[row.userPic]"
                  :initial-index="0"
                  fit="cover"
                  preview-teleported
              >
                <template #error>
                  <div class="error-slot">
                    {{ row.username?.charAt(0) }}
                  </div>
                </template>
              </el-image>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="登录名称" prop="username" min-width="120" align="center" show-overflow-tooltip />
        <el-table-column label="用户昵称" prop="nickname" min-width="120" align="center" show-overflow-tooltip />
        <el-table-column label="电子邮箱" prop="email" min-width="180" align="center" show-overflow-tooltip />
        <!-- 新增：手机号表格列 -->
        <el-table-column label="手机号码" prop="phone" min-width="130" align="center" show-overflow-tooltip />

        <el-table-column label="创建时间" width="180" align="center">
          <template #default="{ row }">
            {{ row.createTime ? row.createTime.replace('T', ' ') : '-' }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button :icon="Edit" type="primary" link @click="showDialog(row)">编辑</el-button>
            <el-button :icon="Delete" type="danger" link @click="deleteUser(row)">删除</el-button>
            <el-button :icon="UserFilled" type="warning" link @click="RolesGrant(row)">角色授权</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="未检索到用户信息" />
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

    <el-dialog v-model="dialogVisible" :title="title" width="420px" destroy-on-close>
      <!-- 新增：表单引用 -->
      <el-form
          ref="formRef"
          :model="userModel"
          :rules="rules"
          label-width="80px"
          style="padding-right: 20px"
      >
        <el-form-item label="登录名" prop="username">
          <el-input v-model="userModel.username" placeholder="请输入登录名称" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="userModel.nickname" placeholder="请输入用户昵称" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userModel.email" placeholder="请输入邮箱地址" />
        </el-form-item>
        <!-- 新增：手机号编辑项 -->
        <el-form-item label="手机号" prop="phone">
          <el-input
              v-model="userModel.phone"
              placeholder="请输入11位手机号码"
              maxlength="11"
              show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="updateUser">保存修改</el-button>
      </template>
    </el-dialog>
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
    .title-text { font-weight: bold; font-size: 16px; color: #303133; }
  }
}

.search-area {
  flex-shrink: 0;
  margin-bottom: 12px;
}

.table-wrapper {
  flex: 1;
  overflow: hidden;
  margin-bottom: 12px;
}

.avatar-container {
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 修改后的图片样式 */
.rect-avatar {
  width: 65px;
  height: 38px;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  cursor: pointer; /* 增加手指指针，提示可点击 */
}

/* 错误占位样式（模拟原 el-avatar 效果） */
.error-slot {
  width: 100%;
  height: 100%;
  background-color: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  border-radius: 6px;
}

.pagination-area {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
}

:deep(.el-table__cell) {
  padding: 12px 0 !important;
}

:deep(.el-table__header th) {
  background-color: #fafafa;
  color: #333;
  font-weight: bold;
}
</style>