<script setup lang="js">
import { Edit, Delete, Search, Refresh, Plus } from '@element-plus/icons-vue'
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from "element-plus"
import {
  userRolesListService,
  allRolesList,
  userRolesDelete,
  userRolesAdd
} from "@/api/roles.js"

// --- 路由与基础数据 ---
const route = useRoute()
const userId = ref(route.query.id)
const usernameParam = ref(route.query.username) // 从路由获取用户名
const roleId = ref()
const loading = ref(false)

// 列表数据模型
const userRoles = ref([])
const roles = ref([])

// 分页数据模型
const pageNum = ref(1)
const total = ref(0)
const pageSize = ref(10)

// --- 数据获取逻辑 ---

// 获取所有角色列表（用于下拉框）
const rolesList = async () => {
  let result = await allRolesList()
  roles.value = result.data
}

// 获取用户已分配的角色分页列表
const UserRolesList = async () => {
  loading.value = true
  try {
    let params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      userId: userId.value || null,
      roleId: roleId.value || null
    }

    // 确保角色基础数据已加载
    if (roles.value.length === 0) await rolesList()

    let result = await userRolesListService(params)
    let items = result.data.items

    // 映射角色名称
    userRoles.value = items.map(item => {
      const role = roles.value.find(r => String(item.roleId) === String(r.roleId))
      return {
        ...item,
        userRoleName: role ? role.roleName : '未知角色'
      }
    })
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

// 初始化加载
UserRolesList()

// 分页回调
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
    userId: userId.value,
    roleId: ''
  }
}

const handleAddUserRoles = async () => {
  if (!userRolesModel.value.roleId) {
    ElMessage.warning('请选择要分配的角色')
    return
  }

  await userRolesAdd(userRolesModel.value)
  ElMessage.success('角色分配成功')
  await UserRolesList()
  dialogVisible.value = false
}

// --- 删除（取消授权）逻辑 ---
const deleteRoles = (row) => {
  ElMessageBox.confirm('你确认要取消该用户的此角色授权吗?', '温馨提示', {
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
        <span class="title-text">用户管理 / <small>分配角色列表</small></span>
        <el-button type="primary" :icon="Plus" @click="dialogVisible = true; title='分配角色'; clearData()">分配角色</el-button>
      </div>
    </template>

    <div class="search-area">
      <el-form inline>
        <el-form-item label="角色筛选">
          <el-select placeholder="请选择角色" v-model="roleId" clearable style="width: 200px">
            <el-option
                v-for="c in roles"
                :key="c.roleId"
                :label="c.roleName"
                :value="c.roleId"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="UserRolesList">搜索</el-button>
          <el-button :icon="Refresh" @click="roleId=''; UserRolesList()">重置</el-button>
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

        <el-table-column label="用户名称" min-width="120" align="center">
          <template #default>
            <el-tag type="info" effect="plain">{{ usernameParam }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="角色名称" prop="userRoleName" min-width="150" align="center">
          <template #default="{ row }">
            <el-tag effect="dark">{{ row.userRoleName }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="分配时间" prop="createTime" width="180" align="center" />
        <el-table-column label="更新时间" prop="updateTime" width="180" align="center" />

        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
                :icon="Delete"
                type="danger"
                link
                @click="deleteRoles(row)"
            >取消授权</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="该用户尚未分配任何角色" />
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

    <el-dialog v-model="dialogVisible" :title="title" width="400px" destroy-on-close>
      <el-form :model="userRolesModel" label-width="90px" style="padding-right: 20px">
        <el-form-item label="用户编号">
          <el-input v-model="userRolesModel.userId" disabled />
        </el-form-item>
        <el-form-item label="分配角色">
          <el-select placeholder="请选择要授予的角色" v-model="userRolesModel.roleId" style="width: 100%">
            <el-option v-for="c in roles" :key="c.roleId" :label="c.roleName" :value="c.roleId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleAddUserRoles">确认分配</el-button>
        </span>
      </template>
    </el-dialog>
  </el-card>
</template>

<style lang="scss" scoped>
/* 锁定页面高度布局 */
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

/* 表格包装层 */
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

/* 单元格视觉微调 */
:deep(.el-table__cell) {
  padding: 12px 0 !important;
}

:deep(.el-table__header th) {
  background-color: #fafafa;
  color: #333;
  font-weight: bold;
}
</style>