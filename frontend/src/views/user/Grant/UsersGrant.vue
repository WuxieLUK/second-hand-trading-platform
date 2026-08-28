<script setup lang="js">
import { Edit, Delete, Search, Refresh, Plus } from '@element-plus/icons-vue'
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from "element-plus"
import { userRolesListService, userRolesDelete, userRolesAdd } from "@/api/roles.js"
import { allUserList } from "@/api/user.js"

// --- 基础数据与路由 ---
const route = useRoute()
const userId = ref()
const roleId = ref(route.query.roleId)
const roleNameParam = ref(route.query.roleName) // 路由传参的角色名称
const loading = ref(false)

// 列表数据
const userRoles = ref([])
const users = ref([])

// 分页数据
const pageNum = ref(1)
const total = ref(0)
const pageSize = ref(10)

// --- 数据获取逻辑 ---

// 获取所有用户列表（用于下拉框选择）
const usersList = async () => {
  let result = await allUserList()
  users.value = result.data
}

// 获取已授权该角色的用户分页列表
const UserRolesList = async () => {
  loading.value = true
  try {
    let params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      userId: userId.value || null,
      roleId: roleId.value || null
    }

    // 确保用户基础数据已加载
    await usersList()

    let result = await userRolesListService(params)
    let items = result.data.items

    // 数据聚合：将 userId 映射为用户名
    userRoles.value = items.map(item => {
      const user = users.value.find(u => String(item.userId) === String(u.id))
      return {
        ...item,
        userRoleName: user ? user.username : '未知用户'
      }
    })
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

// 初始化
UserRolesList()

// 分页逻辑
const onSizeChange = (size) => {
  pageSize.value = size
  UserRolesList()
}
const onCurrentChange = (num) => {
  pageNum.value = num
  UserRolesList()
}

// --- 弹窗逻辑 ---
const dialogVisible = ref(false)
const title = ref('')
const userRolesModel = ref({
  userId: '',
  roleId: ''
})

const clearData = () => {
  userRolesModel.value = {
    roleId: roleId.value,
    userId: '',
    userRoleId: ''
  }
}

const handleAddUserRoles = async () => {
  if (!userRolesModel.value.userId) {
    ElMessage.warning('请选择要授权的用户')
    return
  }
  await userRolesAdd(userRolesModel.value)
  ElMessage.success('用户授权成功')
  await UserRolesList()
  dialogVisible.value = false
}

// --- 删除逻辑 ---
const deleteUsers = (row) => {
  ElMessageBox.confirm('你确认要取消该用户的角色授权吗?', '温馨提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await userRolesDelete(row.userRoleId)
    ElMessage.success('取消授权成功')
    await UserRolesList()
  }).catch(() => {})
}
</script>

<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <span class="title-text">角色管理 / <small>授权用户列表</small></span>
        <el-button type="primary" :icon="Plus" @click="dialogVisible = true; title='授权用户'; clearData()">授权用户</el-button>
      </div>
    </template>

    <div class="search-area">
      <el-form inline>
        <el-form-item label="用户筛选">
          <el-select placeholder="请选择用户" v-model="userId" clearable style="width: 200px">
            <el-option
                v-for="c in users"
                :key="c.id"
                :label="c.username"
                :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="UserRolesList">搜索</el-button>
          <el-button :icon="Refresh" @click="userId=''; UserRolesList()">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-wrapper">
      <el-table
          :data="userRoles"
          height="100%"
          style="width: 100%"
          v-loading="loading"
          border
          stripe
      >
        <el-table-column label="编号" width="90" prop="userRoleId" align="center" />

        <el-table-column label="所属角色" min-width="150" align="center">
          <template #default>
            <el-tag effect="dark">{{ roleNameParam }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="用户名称" prop="userRoleName" min-width="150" align="center">
          <template #default="{ row }">
            <span style="font-weight: bold; color: #409eff;">{{ row.userRoleName }}</span>
          </template>
        </el-table-column>

        <el-table-column label="授权时间" prop="createTime" width="180" align="center" />
        <el-table-column label="更新时间" prop="updateTime" width="180" align="center" />

        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
                :icon="Delete"
                type="danger"
                link
                @click="deleteUsers(row)"
            >取消授权</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="该角色暂无授权用户" />
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
      <el-form :model="userRolesModel" label-width="90px" style="padding-right: 20px">
        <el-form-item label="角色编号">
          <el-input v-model="userRolesModel.roleId" disabled />
        </el-form-item>
        <el-form-item label="授权用户">
          <el-select placeholder="请选择要授权的用户" v-model="userRolesModel.userId" style="width: 100%">
            <el-option v-for="c in users" :key="c.id" :label="c.username" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleAddUserRoles">确认授权</el-button>
        </span>
      </template>
    </el-dialog>
  </el-card>
</template>

<style lang="scss" scoped>
/* 锁定全屏布局 */
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
      small { font-weight: normal; color: #909399; margin-left: 8px; }
    }
  }
}

/* 搜索区域 */
.search-area {
  flex-shrink: 0;
  margin-bottom: 12px;
}

/* 表格包装区域 */
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

/* 表格全局样式一致性补丁 */
:deep(.el-table__cell) {
  padding: 12px 0 !important;
}

:deep(.el-table__header th) {
  background-color: #fafafa;
  color: #333;
  font-weight: bold;
}
</style>