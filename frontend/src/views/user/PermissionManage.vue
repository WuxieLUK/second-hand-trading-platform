<script setup lang="js">
import { Edit, Delete, Search, Refresh, Connection } from '@element-plus/icons-vue'
import { ref } from 'vue'
import {
  permissionAddService,
  permissionDeleteService,
  permissionListService,
  permissionUpdateInfoServices
} from "@/api/permission.js";
import { ElMessage, ElMessageBox } from "element-plus";
import router from "@/router/index.js";

// --- 数据模型 ---
const permissionName = ref("")
const permissionDescription = ref('')
const permissions = ref([])
const loading = ref(false)

// 分页数据模型
const pageNum = ref(1)
const total = ref(0)
const pageSize = ref(10)

// 获取权限分页列表数据
const PermissionList = async () => {
  loading.value = true
  try {
    let params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      permissionName: permissionName.value || null,
      permissionDescription: permissionDescription.value || null
    }
    let result = await permissionListService(params)
    permissions.value = result.data.items;
    total.value = result.data.total;
  } finally {
    loading.value = false
  }
}

PermissionList()

// 分页逻辑
const onSizeChange = (size) => {
  pageSize.value = size
  PermissionList()
}
const onCurrentChange = (num) => {
  pageNum.value = num
  PermissionList()
}

// --- 弹窗逻辑 ---
const dialogVisible = ref(false)
const title = ref('')
const permissionModel = ref({
  permissionName: '',
  permissionDescription: '',
  permissionId: ''
})

const rules = {
  permissionName: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  permissionDescription: [{ required: true, message: '请输入权限说明', trigger: 'blur' }]
}

const clearData = () => {
  permissionModel.value = { permissionName: '', permissionDescription: '', permissionId: '' }
}

const showDialog = (row) => {
  dialogVisible.value = true
  title.value = '修改权限'
  permissionModel.value = { ...row } // 对象拷贝
}

// 保存（新增/修改）
const handleConfirm = async () => {
  if (!permissionModel.value.permissionName || !permissionModel.value.permissionDescription) {
    ElMessage.warning('内容不能为空')
    return
  }

  if (title.value === '新增权限') {
    await permissionAddService(permissionModel.value)
    ElMessage.success('添加成功')
  } else {
    await permissionUpdateInfoServices(permissionModel.value)
    ElMessage.success('修改成功')
  }

  await PermissionList()
  dialogVisible.value = false
}

// 删除逻辑
const deletePermission = (row) => {
  ElMessageBox.confirm('确认要删除该权限信息吗?', '温馨提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await permissionDeleteService(row.permissionId)
    ElMessage.success('删除成功')
    await PermissionList()
  })
}

// 角色授权跳转
const permissionGrant = (row) => {
  router.push({
    path: '/permission/permissionGrant',
    query: {
      permissionId: row.permissionId,
      permissionName: row.permissionName,
      permissionDescription: row.permissionDescription
    }
  })
}
</script>

<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <span class="title-text">权限管理</span>
        <el-button type="primary" :icon="Edit" @click="dialogVisible = true; title='新增权限'; clearData()">新增权限</el-button>
      </div>
    </template>

    <div class="search-area">
      <el-form inline>
        <el-form-item label="权限字符">
          <el-input v-model="permissionName" placeholder="输入权限名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="权限说明">
          <el-input v-model="permissionDescription" placeholder="输入说明关键字" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="PermissionList">搜索</el-button>
          <el-button :icon="Refresh" @click="permissionName=''; permissionDescription=''; PermissionList()">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-wrapper">
      <el-table
          :data="permissions"
          height="100%"
          style="width: 100%"
          v-loading="loading"
          border
          stripe
      >
        <el-table-column label="编号" prop="permissionId" width="100" align="center" />

        <el-table-column label="权限字符" prop="permissionName" min-width="150" align="center" show-overflow-tooltip />

        <el-table-column label="权限说明" prop="permissionDescription" min-width="200" align="center" show-overflow-tooltip />

        <el-table-column label="创建时间" prop="createTime" width="180" align="center" />

        <el-table-column label="修改时间" prop="updateTime" width="180" align="center" />

        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button :icon="Edit" type="primary" link @click="showDialog(row)">编辑</el-button>
            <el-button :icon="Delete" type="danger" link @click="deletePermission(row)">删除</el-button>
            <el-button :icon="Connection" type="warning" link @click="permissionGrant(row)">授权角色</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无相关权限数据" />
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
      <el-form :model="permissionModel" :rules="rules" label-width="100px" style="padding-right: 20px">
        <el-form-item label="权限字符" prop="permissionName">
          <el-input v-model="permissionModel.permissionName" maxlength="50" placeholder="例如: user:list" />
        </el-form-item>
        <el-form-item label="权限说明" prop="permissionDescription">
          <el-input v-model="permissionModel.permissionDescription" maxlength="50" placeholder="简述权限作用" />
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
/* 容器全屏化适配 */
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

/* 搜索栏高度固定 */
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

/* 单元格视觉居中优化 */
:deep(.el-table__cell) {
  padding: 12px 0 !important;
}

:deep(.el-table__header th) {
  background-color: #fafafa;
  color: #333;
  font-weight: bold;
}
</style>