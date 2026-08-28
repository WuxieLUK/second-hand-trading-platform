<script setup lang="js">
import { Edit, Delete, Search, Refresh, Plus } from '@element-plus/icons-vue'
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from "element-plus"
import { allRolesList } from "@/api/roles.js"
import {
  allPermissionList,
  permissionRolesAdd,
  permissionRolesDelete,
  permissionRolesListService
} from "@/api/permission.js"

// --- 路由与基础数据 ---
const route = useRoute()
const permissionId = ref(route.query.permissionId)
const permissionIdDefault = ref(route.query.permissionId)
const roleId = ref()
const loading = ref(false)

// 列表数据模型
const permissionRoles = ref([])
const roles = ref([])
const permissions = ref([])

// 分页数据模型
const pageNum = ref(1)
const total = ref(0)
const pageSize = ref(10)

// --- 数据获取与处理逻辑 ---

// 获取所有角色
const rolesList = async () => {
  let result = await allRolesList()
  roles.value = result.data
}

// 获取所有权限
const permissionList = async () => {
  let result = await allPermissionList()
  permissions.value = result.data
}

// 获取分页列表数据
const PermissionRolesList = async () => {
  loading.value = true
  try {
    let params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      permissionId: permissionId.value || null,
      roleId: roleId.value || null
    }

    // 初始化基础数据
    await rolesList()
    await permissionList()

    let result = await permissionRolesListService(params)
    let items = result.data.items

    // 数据聚合映射 (将 ID 转换为名称显示)
    permissionRoles.value = items.map(item => {
      const role = roles.value.find(r => String(r.roleId) === String(item.roleId))
      const perm = permissions.value.find(p => String(p.permissionId) === String(item.permissionId))
      return {
        ...item,
        permissionRoleName: role ? role.roleName : '未知角色',
        permissionName: perm ? perm.permissionName : '未知权限',
        permissionDescription: perm ? perm.permissionDescription : '-'
      }
    })
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

PermissionRolesList()

// 分页回调
const onSizeChange = (size) => {
  pageSize.value = size
  PermissionRolesList()
}
const onCurrentChange = (num) => {
  pageNum.value = num
  PermissionRolesList()
}

// --- 弹窗逻辑 ---
const dialogVisible = ref(false)
const title = ref('')
const permissionRolesModel = ref({
  permissionId: '',
  roleId: ''
})

const clearData = () => {
  permissionRolesModel.value = {
    permissionId: permissionIdDefault.value,
    roleId: ''
  }
}

const addPermissionRoles = async () => {
  if (!permissionRolesModel.value.permissionId || !permissionRolesModel.value.roleId) {
    ElMessage.warning('请确保权限编号和角色已选择')
    return
  }

  await permissionRolesAdd(permissionRolesModel.value)
  ElMessage.success('角色授权成功')
  await PermissionRolesList()
  dialogVisible.value = false
}

// --- 删除逻辑 ---
const deleteRoles = (row) => {
  ElMessageBox.confirm('你确认要为该权限取消此角色授权吗?', '温馨提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await permissionRolesDelete(row.permissionRoleId)
    ElMessage.success('已取消授权')
    await PermissionRolesList()
  }).catch(() => {})
}
</script>

<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <span class="title-text">权限管理 / <small>授权角色列表</small></span>
        <el-button type="primary" :icon="Plus" @click="dialogVisible = true; title='授权新角色'; clearData()">授权角色</el-button>
      </div>
    </template>

    <div class="search-area">
      <el-form inline>
        <el-form-item label="权限编号">
          <el-input v-model="permissionId" placeholder="输入编号" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="角色名称">
          <el-select placeholder="请选择角色进行筛选" v-model="roleId" clearable style="width: 200px">
            <el-option v-for="c in roles" :key="c.roleId" :label="c.roleName" :value="c.roleId" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="PermissionRolesList">搜索</el-button>
          <el-button :icon="Refresh" @click="permissionId=permissionIdDefault; roleId=''; PermissionRolesList()">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-wrapper">
      <el-table
          :data="permissionRoles"
          height="100%"
          style="width: 100%"
          v-loading="loading"
          border
          stripe
      >
        <el-table-column label="编号" width="90" prop="permissionRoleId" align="center" />
        <el-table-column label="权限编号" prop="permissionId" width="120" align="center" />
        <el-table-column label="权限名称" prop="permissionName" min-width="150" align="center" show-overflow-tooltip />
        <el-table-column label="权限说明" prop="permissionDescription" min-width="200" align="center" show-overflow-tooltip />
        <el-table-column label="角色名称" prop="permissionRoleName" min-width="150" align="center">
          <template #default="{ row }">
            <el-tag>{{ row.permissionRoleName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="授权时间" prop="createTime" width="180" align="center" />
        <el-table-column label="修改时间" prop="updateTime" width="180" align="center" />

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
          <el-empty description="暂无关联角色数据" />
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
      <el-form :model="permissionRolesModel" label-width="90px" style="padding-right: 20px">
        <el-form-item label="权限编号">
          <el-input v-model="permissionRolesModel.permissionId" disabled />
        </el-form-item>
        <el-form-item label="选择角色">
          <el-select placeholder="请选择要授权的角色" v-model="permissionRolesModel.roleId" style="width: 100%">
            <el-option v-for="c in roles" :key="c.roleId" :label="c.roleName" :value="c.roleId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="addPermissionRoles">确认授权</el-button>
        </span>
      </template>
    </el-dialog>
  </el-card>
</template>

<style lang="scss" scoped>
/* 全屏高度锁定方案 */
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

/* 分页条 */
.pagination-area {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
}

/* 单元格微调 */
:deep(.el-table__cell) {
  padding: 12px 0 !important;
}

:deep(.el-table__header th) {
  background-color: #fafafa;
  color: #333;
  font-weight: bold;
}
</style>