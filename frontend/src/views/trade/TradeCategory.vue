<script setup lang="js">
import { Edit, Delete, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ref, watch, onMounted} from 'vue'
// 替换API导入 - 适配新的层级分类接口
import {
  shopCategoryAddService,
  shopCategoryAdminListService, // 替换原list接口
  shopCategoryParentListService, // 新增：查询一级分类（父分类）
  shopCategoryUpdateService,
  shopCategoryDeleteService
} from '@/api/shopcategory.js'
import { ElMessage, ElMessageBox } from "element-plus"

// --- 数据定义 ---
const categorys = ref([]) // 全部分类列表
const filteredCategorys = ref([]) // 过滤后的分类列表
const parentCategorys = ref([]) // 一级分类列表（父分类选项）
const selectedIds = ref([])
const isAllSelected = ref(false)
const isIndeterminate = ref(false)
const tableKey = ref(0)
const loading = ref(false)
const searchKeyword = ref('')

// 格式化分类数据：给二级分类添加父分类名称，方便表格展示
const formatCategoryData = (list) => {
  return list.map(item => {
    // 找到父分类名称（parentId=0为一级分类，无父分类）
    const parent = parentCategorys.value.find(p => p.id === item.parentId)
    return {
      ...item,
      parentCategoryName: parent ? parent.categoryName : '无' // 父分类名称
    }
  })
}

// 获取全部分类列表（管理员）
const getShopCategoryList = async () => {
  loading.value = true
  try {
    // 1. 先获取一级分类（父分类选项）
    const parentRes = await shopCategoryParentListService()
    parentCategorys.value = parentRes.data || []

    // 2. 获取全部分类列表
    let result = await shopCategoryAdminListService()
    categorys.value = formatCategoryData(result.data || [])
    selectedIds.value = []
    updateSelectionStatus()
  } catch (error) {
    ElMessage.error('获取商品分类列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索过滤逻辑 - 新增父分类名称过滤
watch([searchKeyword, categorys], () => {
  if (!searchKeyword.value) {
    filteredCategorys.value = categorys.value
  } else {
    const keyword = searchKeyword.value.toLowerCase()
    filteredCategorys.value = categorys.value.filter(item =>
        item.categoryName?.toLowerCase().includes(keyword) ||
        item.categoryAlias?.toLowerCase().includes(keyword) ||
        item.parentCategoryName?.toLowerCase().includes(keyword)
    )
  }
}, { immediate: true })

onMounted(() => {
  getShopCategoryList()
})

// --- 弹窗逻辑 ---
const dialogVisible = ref(false)
const title = ref('')
const categoryFormRef = ref(null)
// 表单模型 - 新增parentId字段（父分类ID）
const categoryModel = ref({
  id: '',
  categoryName: '',
  categoryAlias: '',
  parentId: 0 // 默认0（一级分类）
})

// 表单验证规则 - 保持原有规则
const rules = {
  categoryName: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 1, max: 10, message: '1-10个字符', trigger: 'blur' }
  ],
  categoryAlias: [
    { required: true, message: '请输入分类别名', trigger: 'blur' },
    { min: 1, max: 15, message: '1-15个字符', trigger: 'blur' }
  ]
}

// 打开弹窗：区分添加/编辑，处理父分类回显
const showDialog = (row) => {
  if (row) {
    // 编辑模式：回显数据（含父分类ID）
    title.value = '修改商品分类'
    dialogVisible.value = true
    categoryModel.value = {
      id: row.id,
      categoryName: row.categoryName,
      categoryAlias: row.categoryAlias,
      parentId: row.parentId || 0
    }
  } else {
    // 添加模式：默认一级分类
    title.value = '添加商品分类'
    dialogVisible.value = true
    clearData()
  }
}

// 清空表单数据
const clearData = () => {
  categoryModel.value = {
    id: '',
    categoryName: '',
    categoryAlias: '',
    parentId: 0
  }
  if (categoryFormRef.value) categoryFormRef.value.resetFields()
}

// 保存/更新分类
const saveCategory = async () => {
  if (!categoryFormRef.value) return
  await categoryFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const service = title.value === '修改商品分类' ? shopCategoryUpdateService : shopCategoryAddService
        await service(categoryModel.value)
        ElMessage.success(`${title.value}成功`)
        await getShopCategoryList() // 重新加载列表
        dialogVisible.value = false
      } catch (error) {
        ElMessage.error(`${title.value}失败：${error.response?.data?.msg || '服务器错误'}`)
      } finally {
        loading.value = false
      }
    }
  })
}

// --- 删除逻辑：新增子分类校验提示 ---
const deleteCategory = (row) => {
  // 检查当前分类是否有子分类
  const hasChildren = categorys.value.some(item => item.parentId === row.id)
  if (hasChildren) {
    ElMessage.warning(`无法删除「${row.categoryName}」：该分类下存在子分类，请先删除子分类`)
    return
  }

  ElMessageBox.confirm(`确定删除「${row.categoryName}」吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    loading.value = true
    try {
      await shopCategoryDeleteService(row.id)
      ElMessage.success('删除成功')
      await getShopCategoryList()
    } catch (error) {
      ElMessage.error('删除失败：' + (error.response?.data?.msg || '服务器错误'))
    } finally {
      loading.value = false
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

// 批量删除（新增子分类校验）
const batchDeleteCategories = async () => {
  // 检查选中的分类是否有子分类
  const hasChildrenCategory = selectedIds.value.some(id => {
    return categorys.value.some(item => item.parentId === id)
  })
  if (hasChildrenCategory) {
    ElMessage.warning('无法批量删除：部分选中的分类下存在子分类，请先删除子分类')
    return
  }

  ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 个商品分类？`, '批量删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'danger'
  }).then(async () => {
    loading.value = true
    try {
      for (const id of selectedIds.value) {
        await shopCategoryDeleteService(id)
      }
      ElMessage.success('批量删除成功')
      await getShopCategoryList()
    } catch (error) {
      ElMessage.error('批量删除失败：' + (error.response?.data?.msg || '服务器错误'))
    } finally {
      loading.value = false
    }
  }).catch(() => {
    ElMessage.info('已取消批量删除')
  })
}

// --- 自定义多选逻辑 ---
const updateSelectionStatus = () => {
  const total = filteredCategorys.value.length
  const selected = selectedIds.value.length
  if (total === 0) {
    isAllSelected.value = false
    isIndeterminate.value = false
    return
  }
  isAllSelected.value = selected === total
  isIndeterminate.value = selected > 0 && selected < total
}

const handleSelectionChange = (row, checked) => {
  if (checked) {
    if (!selectedIds.value.includes(row.id)) selectedIds.value.push(row.id)
  } else {
    selectedIds.value = selectedIds.value.filter(id => id !== row.id)
  }
  updateSelectionStatus()
}

const handleAllSelectionChange = (checked) => {
  selectedIds.value = checked ? filteredCategorys.value.map(item => item.id) : []
  tableKey.value++
  updateSelectionStatus()
}

// 快捷添加子分类：基于选中的一级分类快速创建子类
const addChildCategory = (parentRow) => {
  title.value = '添加子分类'
  dialogVisible.value = true
  clearData()
  categoryModel.value = {
    id: '',
    categoryName: '',
    categoryAlias: '',
    parentId: parentRow.id
  }
}
</script>

<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <span class="title-text">商品分类管理</span>
        <div class="header-actions">
          <el-input v-model="searchKeyword" placeholder="搜索名称/别名/父分类..." :prefix-icon="Search" clearable style="width: 220px" />
          <el-button type="danger" :icon="Delete" @click="batchDeleteCategories" :disabled="selectedIds.length === 0">
            批量删除 ({{ selectedIds.length }})
          </el-button>
          <el-button :icon="Refresh" @click="getShopCategoryList" :loading="loading" circle />
          <el-button type="primary" :icon="Plus" @click="showDialog()">添加商品分类</el-button>
        </div>
      </div>
    </template>

    <div class="table-wrapper">
      <el-table
          :data="filteredCategorys"
          height="100%"
          style="width: 100%"
          :key="tableKey"
          v-loading="loading"
          border
          stripe
      >
        <el-table-column width="55" align="center">
          <template #header>
            <el-checkbox v-model="isAllSelected" :indeterminate="isIndeterminate" @change="handleAllSelectionChange" />
          </template>
          <template #default="{ row }">
            <el-checkbox :checked="selectedIds.includes(row.id)" @change="(val) => handleSelectionChange(row, val)" />
          </template>
        </el-table-column>

        <el-table-column label="序号" width="80" prop="id" align="center" />

        <!-- 分类名称：区分一级/二级，添加层级标识 -->
        <el-table-column label="分类名称" prop="categoryName" min-width="180" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="category-name-wrapper">
              <!-- 二级分类添加缩进和标识 -->
              <span v-if="row.parentId !== 0" class="level-tag">└─ 子分类：</span>
              {{ row.categoryName }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="分类别名" prop="categoryAlias" min-width="150" align="center" show-overflow-tooltip />

        <!-- 新增：父分类列 -->
        <el-table-column label="父分类" prop="parentCategoryName" min-width="150" align="center" show-overflow-tooltip />

        <el-table-column label="创建时间" prop="createTime" width="200" align="center">
          <template #default="{ row }">
            {{ row.createTime || '-' }}
          </template>
        </el-table-column>

        <!-- 操作列：调整宽度确保按钮同行显示 -->
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <div class="operation-btn-group">
              <el-button :icon="Edit" type="primary" link @click="showDialog(row)">编辑</el-button>
              <!-- 仅一级分类显示“添加子分类” -->
              <el-button v-if="row.parentId === 0" type="success" link @click="addChildCategory(row)">添加子分类</el-button>
              <el-button :icon="Delete" type="danger" link @click="deleteCategory(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无商品分类数据" />
        </template>
      </el-table>
    </div>

    <div class="table-footer">
      <div class="footer-info">
        <span>共 {{ categorys.length }} 条记录</span>
        <span v-if="searchKeyword" class="filter-tag"> (过滤出 {{ filteredCategorys.length }} 条)</span>
      </div>
    </div>

    <!-- 弹窗：新增父分类选择器 -->
    <el-dialog v-model="dialogVisible" :title="title" width="420px" destroy-on-close>
      <el-form ref="categoryFormRef" :model="categoryModel" :rules="rules" label-width="80px" style="padding-right: 20px">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="categoryModel.categoryName" maxlength="10" show-word-limit placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="分类别名" prop="categoryAlias">
          <el-input v-model="categoryModel.categoryAlias" maxlength="15" show-word-limit placeholder="请输入分类别名" />
        </el-form-item>
        <!-- 新增：父分类选择器 -->
        <el-form-item label="父分类">
          <el-select v-model="categoryModel.parentId" placeholder="请选择父分类（默认一级分类）">
            <el-option label="无（一级分类）" value="0" />
            <el-option
                v-for="parent in parentCategorys"
                :key="parent.id"
                :label="parent.categoryName"
                :value="parent.id"
            />
          </el-select>
          <div class="form-tip">选择父分类则为二级分类，不选则为一级分类</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCategory" :loading="loading">确认</el-button>
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
    .header-actions { display: flex; gap: 12px; align-items: center; }
  }
}

/* 表格包装层：负责内部滚动 */
.table-wrapper {
  flex: 1;
  overflow: hidden;
  margin-bottom: 12px;
}

/* 模拟分页条的底部固定栏 */
.table-footer {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;

  .footer-info {
    color: #909399;
    font-size: 13px;
    .filter-tag {
      margin-left: 8px;
      color: #409EFF;
      font-weight: 500;
    }
  }
}

/* 单元格内容垂直居中微调 */
:deep(.el-table__cell) {
  padding: 10px 0 !important;
}

:deep(.el-table__header th) {
  background-color: #fafafa;
  color: #333;
  font-weight: bold;
}

/* 分类名称层级样式 - 新增水平居中 */
.category-name-wrapper {
  display: flex;
  align-items: center;
  justify-content: center; /* 新增：让分类名称水平居中 */
  .level-tag {
    color: #909399;
    margin-right: 4px;
    font-size: 12px;
  }
}

/* 操作按钮组样式 - 确保按钮同行显示 */
.operation-btn-group {
  display: flex;
  justify-content: center; /* 按钮组整体居中 */
  gap: 8px; /* 按钮之间的间距 */
  flex-wrap: nowrap; /* 禁止换行 */

  :deep(.el-button) {
    white-space: nowrap; /* 按钮文字不换行 */
    padding: 0 8px; /* 微调按钮内边距，节省空间 */
  }
}

/* 表单提示文字 */
.form-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
</style>