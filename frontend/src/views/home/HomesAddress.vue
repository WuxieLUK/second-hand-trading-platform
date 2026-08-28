<script setup lang="js">
import { Edit, Delete, Plus, Search, Refresh} from '@element-plus/icons-vue'
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from "element-plus";
import {
  addressAddService,
  addressDetailService,
  addressUpdateService,
  addressDeleteService,
  addressListService,
  // 新增：导入默认地址相关API
  addressSetDefaultService,
  addressGetDefaultService
} from "@/api/address.js";
import Footer from "@/components/footer.vue";

// --- 核心新增：表格强制刷新Key ---
const tableKey = ref(0)

// --- 数据模型 ---
const addressName = ref('') // 地址名称筛选
const addressType = ref() // 地址类型筛选（默认/非默认）
const addressList = ref([])
const loading = ref(false)

// 分页数据
const pageNum = ref(1)
const total = ref(0)
const pageSize = ref(10)

// 获取地址分页列表
const getAddressList = async () => {
  loading.value = true
  try {
    let params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      addressName: addressName.value || null,
      addressType: Number(addressType.value) || null
    }
    let result = await addressListService(params)
    console.log('获取地址列表成功：', result)
    addressList.value = result.data.items
    total.value = result.data?.total || 0;
  } catch (error) {
    console.error('获取地址列表失败：', error)
    ElMessage.error('获取地址列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  getAddressList()
})

// 分页回调
const onSizeChange = (size) => {
  pageSize.value = size
  getAddressList()
}
const onCurrentChange = (num) => {
  pageNum.value = num
  getAddressList()
}

// --- 新增：设置默认地址逻辑 ---
const setDefaultAddress = async (row) => {
  try {
    await ElMessageBox.confirm(
        '确认要将该地址设为默认地址吗？',
        '温馨提示',
        {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )

    // 调用设置默认地址API
    await addressSetDefaultService(row.id)
    ElMessage.success('默认地址设置成功')

    // 刷新列表
    pageNum.value = 1
    tableKey.value++
    await nextTick()
    await getAddressList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('设置默认地址失败：', error)
      ElMessage.error('设置默认地址失败')
    }
  }
}

// --- 弹窗与表单 ---
const dialogVisible = ref(false)
const title = ref('')
const addressFormRef = ref(null)
// 地址表单模型（对应后端AddressDTO）
const addressModel = ref({
  id: '',
  addressName: '',
  province: '',
  city: '',
  district: '',
  detailAddr: '',
  zipCode: '',
  addressType: 0, // 默认非默认地址
  userId: '',
  isDefault: 0 // 新增：是否默认地址字段
})

// 表单校验规则
const rules = {
  province: [{ required: true, message: '请选择省份', trigger: 'blur' }],
  city: [{ required: true, message: '请选择城市', trigger: 'blur' }],
  district: [{ required: true, message: '请选择区县', trigger: 'blur' }],
  detailAddr: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

// 清空表单数据
const clearData = () => {
  addressModel.value = {
    id: '',
    addressName: '',
    province: '',
    city: '',
    district: '',
    detailAddr: '',
    zipCode: '',
    addressType: 0,
    userId: '',
    isDefault: 0 // 新增：重置默认地址字段
  }
  if (addressFormRef.value) addressFormRef.value.resetFields()
}

// 打开弹窗（新增/编辑）
const showDialog = async (row) => {
  dialogVisible.value = true
  if (row) {
    // 编辑模式：查询详情并赋值
    title.value = '修改地址'
    try {
      let result = await addressDetailService(row.id)
      addressModel.value = { ...result.data }
      // 兼容后端字段：如果返回的是addressType，同步到isDefault
      if (result.data.addressType !== undefined && result.data.isDefault === undefined) {
        addressModel.value.isDefault = result.data.addressType
      }
    } catch (error) {
      console.error('获取地址详情失败：', error)
      ElMessage.error('获取地址详情失败')
      dialogVisible.value = false
    }
  } else {
    // 新增模式
    title.value = '新增地址'
    clearData()
  }
}

// 保存地址（新增/修改）
const submitAddress = async () => {
  // 表单校验
  try {
    await addressFormRef.value.validate()
  } catch (error) {
    ElMessage.warning('请完善必填字段')
    return
  }

  const isEdit = title.value === '修改地址'
  const service = isEdit ? addressUpdateService : addressAddService

  try {
    await service(addressModel.value)
    ElMessage.success(isEdit ? '地址修改成功' : '地址新增成功')

    // 重置分页+强制表格刷新
    pageNum.value = 1
    tableKey.value++
    await nextTick()
    await getAddressList()

    dialogVisible.value = false
  } catch (error) {
    console.error('保存地址失败：', error)
    ElMessage.error(isEdit ? '地址修改失败' : '地址新增失败')
  }
}

// --- 删除地址逻辑 ---
const deleteAddress = async (row) => {
  try {
    // 新增：如果是默认地址，提示不能删除
    if (row.isDefault === 1 || row.addressType === 1) {
      ElMessage.warning('默认地址不能直接删除，请先取消默认状态！')
      return
    }

    await ElMessageBox.confirm(
        '确认要删除该地址吗？删除后不可恢复！',
        '温馨提示',
        {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )

    await addressDeleteService(row.id)
    ElMessage.success('地址删除成功')

    // 重置分页+强制表格刷新
    pageNum.value = 1
    tableKey.value++
    await nextTick()
    await getAddressList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除地址失败：', error)
      ElMessage.error('地址删除失败')
    }
  }
}
</script>

<template>
  <!-- 最外层：100vh 撑满全屏 -->
  <div class="app-wrapper">
    <!-- 内容卡片：上下左右 100px 边距 -->
    <el-card class="page-container">
      <template #header>
        <div class="header">
          <span class="title-text">地址管理</span>
          <el-button type="primary" :icon="Plus" @click="showDialog(); title='新增地址'; clearData()">新增地址</el-button>
        </div>
      </template>

      <!-- 搜索区域 -->
      <div class="search-area">
        <el-form inline>
          <el-form-item label="地址名称">
            <el-input v-model="addressName" placeholder="输入地址名称" clearable style="width: 200px" />
          </el-form-item>
          <!-- 新增：默认地址筛选 -->
          <el-form-item label="地址类型">
            <el-select v-model="addressType" placeholder="全部类型" clearable style="width: 150px">
              <el-option label="默认地址" :value="1" />
              <el-option label="非默认地址" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="getAddressList">搜索</el-button>
            <el-button :icon="Refresh" @click="addressName=''; addressType=''; pageNum.value=1; tableKey.value++; getAddressList()">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 表格区域：正常撑满高度 -->
      <div class="table-wrapper">
        <el-table
            :data="addressList"
            v-loading="loading"
            border
            stripe
            :key="tableKey"
            height="100%"
            style="width: 100%"
        >
          <el-table-column label="地址ID" width="80" prop="id" align="center" />
          <el-table-column label="地址名称" prop="addressName" width="150" align="center" show-overflow-tooltip />
          <el-table-column label="完整地址" prop="fullAddress" min-width="300" align="center" show-overflow-tooltip />
          <!-- 新增：默认地址标识列 -->
          <el-table-column label="地址类型" width="120" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.isDefault === 1" type="success">默认地址</el-tag>
              <el-tag v-else type="info">普通地址</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" prop="createTime" width="180" align="center" />
          <el-table-column label="更新时间" prop="updateTime" width="180" align="center" />
          <el-table-column label="操作" width="380" fixed="right" align="center">
            <template #default="{ row }">
              <el-button :icon="Edit" type="primary" link @click="showDialog(row)">编辑</el-button>
              <!-- 新增：设为默认按钮 -->
              <el-button
                  type="success"
                  link
                  :icon="Refresh"
                  @click="setDefaultAddress(row)"
                  :disabled="row.isDefault === 1 || row.addressType === 1"
              >
                {{ (row.isDefault === 1 || row.addressType === 1) ? '默认地址' : '设为默认' }}
              </el-button>
              <el-button :icon="Delete" type="danger" link @click="deleteAddress(row)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无地址记录" />
          </template>
        </el-table>
      </div>

      <!-- 分页 -->
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

      <!-- 弹窗 -->
      <el-dialog v-model="dialogVisible" :title="title" width="600px" destroy-on-close>
        <el-form ref="addressFormRef" :model="addressModel" :rules="rules" label-width="90px" style="padding-right: 20px">
          <el-form-item label="地址名称" prop="addressName">
            <el-input v-model="addressModel.addressName" placeholder="如：家里地址/宿舍地址" clearable />
          </el-form-item>
          <el-form-item label="省份" prop="province">
            <el-input v-model="addressModel.province" placeholder="请输入省份" clearable />
          </el-form-item>
          <el-form-item label="城市" prop="city">
            <el-input v-model="addressModel.city" placeholder="请输入城市" clearable />
          </el-form-item>
          <el-form-item label="区县" prop="district">
            <el-input v-model="addressModel.district" placeholder="请输入区县" clearable />
          </el-form-item>
          <el-form-item label="详细地址" prop="detailAddr">
            <el-input v-model="addressModel.detailAddr" placeholder="如：XX街道XX小区X栋X单元X室" clearable />
          </el-form-item>
          <el-form-item label="邮政编码" prop="zipCode">
            <el-input v-model="addressModel.zipCode" placeholder="选填" clearable maxlength="6" />
          </el-form-item>
          <!-- 新增：默认地址选择 -->
          <el-form-item label="是否默认">
            <el-radio-group v-model="addressModel.isDefault">
              <el-radio :label="1">是</el-radio>
              <el-radio :label="0">否</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAddress">确认保存</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
  <Footer />
</template>

<style lang="scss" scoped>
// 最外层：100vh 全屏布局
.app-wrapper {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  overflow: hidden;
}

// 内容卡片：上下左右 100px 边距，占满剩余空间
.page-container {
  flex: 1;
  margin: 100px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
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
    }
  }
}

.search-area {
  flex-shrink: 0;
  margin-bottom: 12px;
}

// 关键：让表格容器撑满中间所有空间
.table-wrapper {
  flex: 1;
  overflow: hidden;
  min-height: 400px; /* 空数据时也不会塌陷 */
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

:deep(.el-dialog__body) {
  padding: 20px;
}

// ========== 新增：移动端适配样式 ==========
// 平板设备（768px以下）
@media (max-width: 768px) {
  .page-container {
    margin: 10px; // 大幅减小边距
  }

  .search-area {
    :deep(.el-form-item) {
      margin-bottom: 10px;
      width: 100%; // 搜索项纵向排列
    }

    :deep(.el-input) {
      width: 100% !important; // 输入框占满宽度
    }

    :deep(.el-select) {
      width: 100% !important; // 下拉框占满宽度
    }

    :deep(.el-form-item__label) {
      width: 80px !important;
    }
  }

  .table-wrapper {
    min-height: 200px; // 减小最小高度
    overflow-x: auto; // 允许表格横向滚动

    :deep(.el-table) {
      width: 100%;

      // 调整表格列宽度
      :deep(.el-table-column--width-80) {
        width: 60px !important;
      }
      :deep(.el-table-column--width-150) {
        width: 100px !important;
      }
      :deep(.el-table-column--width-120) {
        width: 90px !important;
      }
      :deep(.el-table-column--width-180) {
        width: 120px !important;
      }
      :deep(.el-table-column--width-380) {
        width: 240px !important;
      }

      // 操作列按钮自适应
      :deep(.el-button) {
        padding: 0 5px;
        font-size: 12px;
      }
    }
  }

  .pagination-area {
    :deep(.el-pagination) {
      font-size: 12px;

      :deep(.el-pagination__sizes) {
        display: none; // 隐藏每页条数选择
      }

      :deep(.el-pagination__total) {
        display: none; // 隐藏总数显示
      }
    }
  }

  // 弹窗适配
  :deep(.el-dialog) {
    width: 90% !important; // 弹窗占屏幕90%宽度
    margin: 0 auto;

    :deep(.el-dialog__body) {
      padding: 10px;
    }

    :deep(.el-form-item__label) {
      width: 70px !important;
      font-size: 13px;
    }

    :deep(.el-input) {
      font-size: 13px;
    }
  }
}

// 手机设备（480px以下）
@media (max-width: 480px) {
  .page-container .header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;

    .title-text {
      font-size: 14px;
    }

    :deep(.el-button) {
      width: 100%;
    }
  }

  .table-wrapper {
    :deep(.el-table) {
      // 进一步减小列宽
      :deep(.el-table-column--width-60) {
        width: 50px !important;
      }
      :deep(.el-table-column--width-100) {
        width: 80px !important;
      }
      :deep(.el-table-column--width-90) {
        width: 70px !important;
      }
      :deep(.el-table-column--width-120) {
        display: none !important; // 隐藏更新时间列
      }

      // 操作列按钮换行显示
      :deep(.el-table-column--width-240) {
        width: 180px !important;

        :deep(.el-button) {
          display: inline-block;
          margin-bottom: 5px;
          width: calc(33.33% - 4px);
        }
      }
    }
  }

  // 表单适配
  :deep(.el-form-item) {
    margin-bottom: 15px;
  }

  :deep(.el-radio) {
    margin-right: 5px;
  }
}
</style>