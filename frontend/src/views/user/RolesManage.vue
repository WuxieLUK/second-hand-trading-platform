<script setup lang="js">
import { Edit, Delete, Search, Refresh, Stamp } from '@element-plus/icons-vue'
import { ref } from 'vue'
import { rolesAddService, rolesDeleteService, rolesListService, rolesUpdateInfoServices } from "@/api/roles.js";
import { ElMessage, ElMessageBox } from "element-plus";
import router from "@/router/index.js";

// --- 数据模型 ---
const roleName = ref("")
const roleDescription = ref('')
const roles = ref([])
const loading = ref(false)

// 分页条数据模型
const pageNum = ref(1)
const total = ref(0)
const pageSize = ref(10)

// 获取分页列表数据
const RolesList = async () => {
  loading.value = true
  try {
    let params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      roleName: roleName.value || null,
      roleDescription: roleDescription.value || null
    }
    let result = await rolesListService(params)
    roles.value = result.data.items;
    total.value = result.data.total;
  } catch (error) {
    ElMessage.error('获取角色列表失败')
  } finally {
    loading.value = false
  }
}

RolesList()

// 分页逻辑
const onSizeChange = (size) => {
  pageSize.value = size
  RolesList()
}
const onCurrentChange = (num) => {
  pageNum.value = num
  RolesList()
}

// --- 弹窗逻辑 ---
const dialogVisible = ref(false)
const title = ref('')
const rolesModel = ref({
  roleName: '',
  roleDescription: '',
  roleId: ''
})

const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleDescription: [{ required: true, message: '请输入角色权能', trigger: 'blur' }]
}

const clearData = () => {
  rolesModel.value = { roleName: '', roleDescription: '', roleId: '' }
}

const showDialog = (row) => {
  title.value = '修改角色'
  dialogVisible.value = true
  rolesModel.value = { ...row }
}

// 保存逻辑（新增/修改）
const handleConfirm = async () => {
  if (!rolesModel.value.roleName || !rolesModel.value.roleDescription) {
    ElMessage.warning('请填写完整信息')
    return
  }

  try {
    if (title.value === '新增角色') {
      await rolesAddService(rolesModel.value)
      ElMessage.success('添加成功')
    } else {
      await rolesUpdateInfoServices(rolesModel.value)
      ElMessage.success('修改成功')
    }
    await RolesList()
    dialogVisible.value = false
  } catch (error) {
    // 错误由拦截器统一处理，或在此单独提示
  }
}

// 删除角色
const deleteRoles = (row) => {
  ElMessageBox.confirm('确认删除该角色信息吗?', '温馨提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await rolesDeleteService(row.roleId)
    ElMessage.success('删除成功')
    await RolesList();
  }).catch(() => {})
}

const UserGrant = (row) => {
  router.push({
    path: '/user/userGrant',
    query: { roleId: row.roleId, roleName: row.roleName }
  })
}
</script>

<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <span class="title-text">角色管理</span>
        <el-button type="primary" :icon="Edit" @click="dialogVisible = true; title='新增角色'; clearData()">新增角色</el-button>
      </div>
    </template>

    <div class="search-area">
      <el-form inline>
        <el-form-item label="角色名称">
          <el-input v-model="roleName" placeholder="输入名称搜索" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="角色权能">
          <el-input v-model="roleDescription" placeholder="输入权能关键字" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="RolesList">搜索</el-button>
          <el-button :icon="Refresh" @click="roleName=''; roleDescription=''; RolesList()">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-wrapper">
      <el-table
          :data="roles"
          height="100%"
          style="width: 100%"
          v-loading="loading"
          border
          stripe
      >
        <el-table-column label="编号" prop="roleId" width="90" align="center" />

        <el-table-column label="角色名称" prop="roleName" min-width="150" align="center" show-overflow-tooltip />

        <el-table-column label="角色权能描述" prop="roleDescription" min-width="220" align="center" show-overflow-tooltip />

        <el-table-column label="创建时间" prop="createTime" width="180" align="center" />

        <el-table-column label="最近修改时间" prop="updateTime" width="180" align="center" />

        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button :icon="Edit" type="primary" link @click="showDialog(row)">编辑</el-button>
            <el-button :icon="Delete" type="danger" link @click="deleteRoles(row)">删除</el-button>
            <el-button :icon="Stamp" type="warning" link @click="UserGrant(row)">成员授权</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="未检索到相关角色信息" />
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

    <el-dialog v-model="dialogVisible" :title="title" width="440px" destroy-on-close>
      <el-form :model="rolesModel" :rules="rules" label-width="80px" style="padding-right: 20px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="rolesModel.roleName" maxlength="20" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="权能描述" prop="roleDescription">
          <el-input v-model="rolesModel.roleDescription" maxlength="50" type="textarea" :rows="2" placeholder="简述该角色的权限边界" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确认保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style lang="scss" scoped>
/* 整个卡片容器锁定页面高度，防止产生双滚动条 */
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

/* 搜索栏布局 */
.search-area {
  flex-shrink: 0;
  margin-bottom: 12px;
}

/* 表格区域：必须设为 flex: 1 且 overflow: hidden，确保内部滚动有效 */
.table-wrapper {
  flex: 1;
  overflow: hidden;
  margin-bottom: 12px;
}

/* 分页条吸底样式 */
.pagination-area {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
}

/* 表格视觉微调 */
:deep(.el-table__cell) {
  padding: 12px 0 !important; // 增加行高，视觉更通透
}

:deep(.el-table__header th) {
  background-color: #fafafa;
  color: #333;
  font-weight: bold;
}
</style>