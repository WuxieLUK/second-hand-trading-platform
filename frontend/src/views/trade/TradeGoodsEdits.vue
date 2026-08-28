<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElLoading } from 'element-plus'
import { Plus, Close } from '@element-plus/icons-vue'
import QuillEditor from '@/components/QuillEditor.vue'
import { goodsDetailService, goodsUpdateService } from '@/api/goods.js'
// ========== 核心修改：替换分类API ==========
import {
  shopCategoryTreeListService,
  shopCategoryChildListService
} from '@/api/shopcategory.js'
import { useTokenStore } from '@/stores/token.js'
import { useTagsViewStore } from '@/stores/tagsView.js'
import useUserInfoStore from '@/stores/userInfo.js'
import { getUploadUrl } from '@/utils/serviceUrls.js'

const userInfoStore = useUserInfoStore()
const router = useRouter()
const route = useRoute()
const tokenStore = useTokenStore()
const tagsViewStore = useTagsViewStore()
const uploadUrl = getUploadUrl()

// ========== 核心修改：分类数据结构调整 ==========
const parentCategorys = ref([]) // 一级分类列表
const childCategorys = ref([])  // 二级分类列表（根据选中的一级分类动态加载）
const selectedParentId = ref('') // 选中的一级分类ID
// 封面图文件（单文件）
const coverFile = ref(null)
// 封面图URL
const coverUrl = ref('')
// 详情图文件列表
const detailFileList = ref([])
// 详情图URL列表
const detailUrlList = ref([])

// 页面加载状态（仅保留页面级加载，移除图片加载）
const pageLoading = ref(true)
// 新增：loading实例引用
const loadingInstance = ref(null)

// 定义新旧程度选项（与后端GoodsIsNewEnum完全一致）
const newDegreeOptions = ref([
  { label: '二手', value: 0 },
  { label: '全新', value: 1 },
  { label: '9成新', value: 2 },
  { label: '8成新', value: 3 },
  { label: '7成新及以下', value: 4 }
])

// 商品表单模型 - 适配后端GoodsDTO
const goodsModel = ref({
  id: '',
  goodsName: '',
  parentCategoryId: '', // 新增：一级分类ID
  categoryId: '',       // 保留：二级分类ID
  originalPrice: 0,
  sellPrice: 0,
  isNew: 0,
  goodsStatus: 1,
  goodsDesc: '',
  goodsPic: '',
  stock: 1,
  sellerId: userInfoStore.info.id || null,
  imageList: []
})

// ========== 核心修改：重构分类获取逻辑 ==========
// 获取一级分类列表
const getParentCategoryList = async () => {
  try {
    const result = await shopCategoryTreeListService()
    if (result.code === 0) {
      parentCategorys.value = result.data.map(item => ({
        id: item.id,
        categoryName: item.categoryName
      }))
    }
  } catch (error) {
    ElMessage.error('获取商品分类失败')
  }
}

// 根据选中的一级分类加载二级分类
const loadChildCategories = async (parentId) => {
  if (!parentId) {
    childCategorys.value = []
    goodsModel.value.categoryId = '' // 清空二级分类选择
    return
  }
  try {
    const res = await shopCategoryChildListService(parentId)
    if (res.code === 0) {
      childCategorys.value = res.data
    }
  } catch (error) {
    ElMessage.error('获取子分类失败')
  }
}

// 获取商品详情数据
const getGoodsDetail = async () => {
  try {
    // 核心修改：使用Loading服务替代标签式加载
    loadingInstance.value = ElLoading.service({
      fullscreen: true,
      lock: true,
      text: '正在加载商品信息...'
    })

    const goodsId = route.query.id || JSON.parse(sessionStorage.getItem('editGoods') || '{}').id
    if (!goodsId) {
      ElMessage.warning('商品ID不存在，无法编辑')
      await router.push('/trade/goods/list')
      return
    }

    // 获取商品详情
    const res = await goodsDetailService(goodsId)
    const goodsData = res.data

    // 基础信息映射
    goodsModel.value = {
      ...goodsData,
      parentCategoryId: goodsData.parentCategoryId || '', // 回显一级分类ID
      originalPrice: Number(goodsData.originalPrice) || 0,
      sellPrice: Number(goodsData.sellPrice) || 0,
      isNew: Number(goodsData.isNew) || 0,
      goodsStatus: Number(goodsData.goodsStatus) || 1,
      stock: Number(goodsData.stock) || 1,
      sellerId: userInfoStore.info.id || goodsData.sellerId
    }

    // ========== 核心修改：编辑回显分类逻辑 ==========
    // 回显一级分类并加载对应的二级分类
    if (goodsData.parentCategoryId) {
      selectedParentId.value = goodsData.parentCategoryId
      await loadChildCategories(goodsData.parentCategoryId)
    }

    // 封面图处理
    coverUrl.value = goodsData.goodsPic || ''
    goodsModel.value.goodsPic = coverUrl.value

    // 详情图处理
    if (goodsData.imageList && Array.isArray(goodsData.imageList)) {
      detailUrlList.value = goodsData.imageList.map(item => item.imageUrl)
      // 构建el-upload需要的fileList格式
      detailFileList.value = goodsData.imageList.map((item, index) => ({
        uid: index + '_' + Date.now(),
        name: `detail_${index + 1}.jpg`,
        url: item.imageUrl,
        status: 'success'
      }))
      // 同步到goodsModel的imageList
      goodsModel.value.imageList = goodsData.imageList.map((item, index) => ({
        ...item,
        sort: index + 1
      }))
    }
  } catch (error) {
    ElMessage.error('获取商品详情失败：' + error.message)
    await router.push('/trade/goods/list')
  } finally {
    pageLoading.value = false
    // 核心修改：关闭loading实例
    if (loadingInstance.value) {
      loadingInstance.value.close()
      loadingInstance.value = null
    }
  }
}

// 封面图选择回调（覆盖式）
const coverInput = ref(null)
const openCoverPicker = () => {
  coverInput.value?.click()
}
const handleCoverChange = async (file) => {
  if (!file) return
  const maxSize = 5 * 1024 * 1024 // 5MB
  if (file.size > maxSize) {
    ElMessage.warning('封面图大小不能超过5MB')
    return
  }
  coverFile.value = file

  // 模拟上传（实际替换为你的上传接口）
  try {
    const formData = new FormData()
    formData.append('file', file)
    const response = await fetch(uploadUrl, {
      method: 'POST',
      headers: {
        Authorization: tokenStore.token
      },
      body: formData
    })
    if (!response.ok) {
      throw new Error(`upload failed: ${response.status}`)
    }
    const res = await response.json()
    const url = res.data.url
    coverUrl.value = url
    goodsModel.value.goodsPic = url
    ElMessage.success('封面图上传成功')
  } catch (error) {
    ElMessage.error('封面图上传失败，请重试')
    coverFile.value = null
  }
}

// 移除封面图
const handleCoverRemove = () => {
  coverFile.value = null
  coverUrl.value = ''
  goodsModel.value.goodsPic = ''
}

// 详情图上传成功回调
const detailUploadSuccess = (response, uploadFile) => {
  const url = response.data.url
  uploadFile.url = url
  detailUrlList.value.push(url)
  goodsModel.value.imageList.push({
    goodsId: goodsModel.value.id || 0,
    imageUrl: url,
    sort: detailUrlList.value.length
  })
  ElMessage.success('详情图上传成功')
}

// 移除详情图
const handleDetailRemove = (uploadFile) => {
  const index = detailUrlList.value.indexOf(uploadFile.url)
  if (index !== -1) {
    detailUrlList.value.splice(index, 1)
    goodsModel.value.imageList.splice(index, 1)
    // 重新排序
    goodsModel.value.imageList.forEach((item, idx) => {
      item.sort = idx + 1
    })
  }
}

// 图片上传失败回调
const uploadError = () => {
  ElMessage.error('图片上传失败，请重试')
}

// 提交修改后的商品信息
const submitGoods = async () => {
  if (!goodsModel.value.goodsName) return ElMessage.warning('商品名称不能为空')
  if (!goodsModel.value.parentCategoryId) return ElMessage.warning('请选择一级分类') // 修改
  if (!goodsModel.value.categoryId) return ElMessage.warning('请选择二级分类') // 修改
  if (goodsModel.value.originalPrice < 0) return ElMessage.warning('原价不能为负数')
  if (!goodsModel.value.sellPrice || goodsModel.value.sellPrice <= 0) return ElMessage.warning('售卖价格必须大于0')
  if (goodsModel.value.isNew === null || goodsModel.value.isNew === undefined) return ElMessage.warning('请选择新旧程度')
  if (!goodsModel.value.sellerId) return ElMessage.warning('卖家ID不能为空，请先登录')
  if (goodsModel.value.stock < 1 || !goodsModel.value.goodsDesc) return ElMessage.warning('请完善库存和商品描述信息')
  if (!goodsModel.value.goodsPic) return ElMessage.warning('请上传商品封面图')

  try {
    const submitData = {
      ...goodsModel.value,
      originalPrice: Number(goodsModel.value.originalPrice),
      sellPrice: Number(goodsModel.value.sellPrice),
      parentCategoryId: Number(goodsModel.value.parentCategoryId), // 提交一级分类ID
      categoryId: Number(goodsModel.value.categoryId),
      isNew: Number(goodsModel.value.isNew),
      sellerId: Number(goodsModel.value.sellerId),
      stock: Number(goodsModel.value.stock)
    }

    await goodsUpdateService(submitData)
    ElMessage.success('商品修改成功')
    handleClose(router.currentRoute.value.path)
  } catch (error) {
    ElMessage.error('修改失败：' + (error.message || '服务器错误'))
  } finally {
    // 关闭提交加载
    submitLoading.close()
  }
}

// 关闭编辑页面
const handleClose = (path) => {
  const index = tagsViewStore.tagsList.findIndex(tag => tag.path === path)
  if (index === -1) return

  // 清除sessionStorage中的编辑数据
  sessionStorage.removeItem('editGoods')

  if (tagsViewStore.tagsList.length <= 1) {
    router.push('/trade/goods/list')
    return
  }

  if (path === router.currentRoute.value.path) {
    const prevTag = tagsViewStore.tagsList[index - 1] || tagsViewStore.tagsList[index + 1]
    router.push(prevTag.path)
  }

  // 重置表单数据
  coverFile.value = null
  coverUrl.value = ''
  detailFileList.value = []
  detailUrlList.value = []
  selectedParentId.value = '' // 重置一级分类选择
  goodsModel.value = {
    id: '', goodsName: '', parentCategoryId: '', categoryId: '', originalPrice: 0, sellPrice: 0,
    isNew: 0, goodsStatus: 1, goodsDesc: '', goodsPic: '', stock: 1,
    sellerId: userInfoStore.info.id || null,
    imageList: []
  }
  tagsViewStore.delTagsItem(index)
}

// ========== 核心修改：监听一级分类变化 ==========
watch(() => selectedParentId.value, async (newVal) => {
  goodsModel.value.parentCategoryId = newVal
  await loadChildCategories(newVal)
}, { immediate: false })

// 初始化页面数据
onMounted(async () => {
  await getParentCategoryList() // 修改：加载一级分类
  await getGoodsDetail()
  pageLoading.value = false
})

// 监听路由参数变化（防止同页面刷新数据）
watch(() => route.query.id, async (newId) => {
  if (newId) {
    await getGoodsDetail()
  }
})
</script>

<template>
  <div class="goods-edit-container">
    <!-- 核心修改：移除标签式el-loading，改用服务式加载 -->
    <div v-if="pageLoading" class="page-loading">
      <!-- 此处不再需要el-loading标签，加载状态由ElLoading.service控制 -->
    </div>

    <div v-else class="main-wrapper">
      <div class="main-layout">
        <div class="left-panel">
          <div class="form-block">
            <div class="block-title">商品名</div>
            <el-input
                v-model="goodsModel.goodsName"
                placeholder="请输入商品名称"
                class="goods-name-input"
            />
          </div>

          <div class="form-block">
            <div class="block-title">新旧程度</div>
            <el-select
                v-model="goodsModel.isNew"
                placeholder="请选择商品新旧程度"
                class="new-degree-select"
            >
              <el-option
                  v-for="item in newDegreeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
              />
            </el-select>
          </div>

          <div class="form-block">
            <div class="block-title">价格</div>
            <div class="price-group">
              <div class="price-item">
                <label class="price-label">售卖价格</label>
                <el-input-number
                    v-model="goodsModel.sellPrice"
                    :min="0.01"
                    :step="0.01"
                    :controls="false"
                    placeholder="请输入售卖价格"
                    class="price-input"
                />
              </div>
              <div class="price-item">
                <label class="price-label">商品原价</label>
                <el-input-number
                    v-model="goodsModel.originalPrice"
                    :min="0"
                    :step="0.01"
                    :controls="false"
                    placeholder="请输入商品原价"
                    class="original-price-input"
                />
              </div>
            </div>
          </div>

          <div class="form-block">
            <el-button
                type="primary"
                class="publish-btn"
                @click="submitGoods"
            >
              保存修改
            </el-button>
          </div>
        </div>

        <div class="right-panel">
          <div class="form-block">
            <div class="block-title">产品图</div>
            <div class="images-inline-container">
              <div class="img-group">
                <div class="section-title">封面图</div>
                <div class="cover-img-box" @click="openCoverPicker">
                  <img
                      v-if="coverUrl"
                      :src="coverUrl"
                      alt="封面图"
                      class="cover-img"
                  >
                  <div v-else class="cover-placeholder">
                    <el-icon><Plus /></el-icon>
                  </div>
                  <input
                      ref="coverInput"
                      type="file"
                      accept="image/*"
                      class="cover-file-input"
                      @change="handleCoverChange($event.target.files[0])"
                  >
                  <el-icon
                      v-if="coverUrl"
                      class="cover-remove-icon"
                      @click.stop="handleCoverRemove"
                  >
                    <Close />
                  </el-icon>
                </div>
              </div>

              <div class="img-group detail-group">
                <div class="section-title">详情图 (最多5张)</div>
                <el-upload
                    v-model:file-list="detailFileList"
                    class="detail-img-upload"
                    :action="uploadUrl"
                    name="file"
                    :headers="{ Authorization: tokenStore.token }"
                    list-type="picture-card"
                    :on-success="detailUploadSuccess"
                    :on-remove="handleDetailRemove"
                    :on-error="uploadError"
                    multiple
                    :limit="5"
                >
                  <div class="detail-placeholder">
                    <el-icon><Plus /></el-icon>
                  </div>

                  <!-- 移除自定义file模板，使用默认样式 -->
                </el-upload>
              </div>
            </div>
          </div>

          <!-- ========== 核心修改：分类选择区域 ========== -->
          <div class="form-block">
            <div class="block-title">所属商品类别</div>
            <div class="category-select-container">
              <!-- 一级分类选择 -->
              <div class="category-level-wrapper">
                <div class="level-title">一级分类</div>
                <div class="category-tags-container">
                  <div
                      v-for="c in parentCategorys"
                      :key="c.id"
                      class="tag-item"
                      :class="{ 'active': selectedParentId === c.id }"
                      @click="selectedParentId = c.id"
                  >
                    {{ c.categoryName }}
                  </div>
                </div>
              </div>

              <!-- 二级分类选择（有数据时显示） -->
              <div class="category-level-wrapper" v-if="childCategorys.length > 0">
                <div class="level-title">二级分类 <span class="required-tag">*</span></div>
                <div class="category-tags-container">
                  <div
                      v-for="c in childCategorys"
                      :key="c.id"
                      class="tag-item"
                      :class="{ 'active': goodsModel.categoryId === c.id }"
                      @click="goodsModel.categoryId = c.id"
                  >
                    {{ c.categoryName }}
                  </div>
                </div>
              </div>

              <!-- 无二级分类提示 -->
              <div class="no-child-tips" v-if="selectedParentId && childCategorys.length === 0">
                该分类下暂无二级分类，请选择其他一级分类
              </div>
            </div>
          </div>

          <div class="form-block">
            <div class="block-title">库存</div>
            <el-input-number
                v-model="goodsModel.stock"
                :min="1"
                :step="1"
                class="stock-input"
            />
          </div>

          <div class="form-block editor-block">
            <div class="block-title">商品描述</div>
            <div class="editor-container">
              <QuillEditor
                  v-model="goodsModel.goodsDesc"
                  previewTheme="vuepress"
                  codeTheme="atom"
                  placeholder="请输入商品详细描述..."
                  :height="350"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.goods-edit-container {
  padding: 16px;
  background-color: #fff;
  min-height: 100vh;
  box-sizing: border-box;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.05);  //边框阴影
}

.page-loading {
  width: 100%;
  height: 100vh;
  position: fixed;
  top: 0;
  left: 0;
  z-index: 9999;
}

// 主包装器：限制最大宽度，居中显示
.main-wrapper {
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

// 主布局：响应式适配，小屏幕自动堆叠
.main-layout {
  display: flex;
  gap: 24px;
  width: 100%;
  flex-wrap: wrap;
  box-sizing: border-box;

  .left-panel {
    flex: 0 0 280px;
    min-width: 280px;
    box-sizing: border-box;
  }

  .right-panel {
    flex: 1;
    min-width: 300px;
    box-sizing: border-box;
  }

  @media (max-width: 768px) {
    flex-direction: column;
    .left-panel, .right-panel {
      width: 100%;
      flex: none;
    }
  }
}

/* 通用区块标题 */
.block-title {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  margin-bottom: 12px;
}

.form-block {
  margin-bottom: 24px;
  width: 100%;
  box-sizing: border-box;
}

/* 左侧面板样式 */
.goods-name-input {
  width: 100%;
  font-size: 16px;
  box-sizing: border-box;

  :deep(.el-input__wrapper) {
    border-radius: 4px;
    width: 100%;
    box-sizing: border-box;
  }
}

.new-degree-select {
  width: 100%;
  box-sizing: border-box;

  :deep(.el-select__wrapper) {
    border-radius: 4px;
    width: 100%;
    box-sizing: border-box;
  }
}

/* 价格组：原价 + 现价 */
.price-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  box-sizing: border-box;

  .price-item {
    display: flex;
    flex-direction: column;
    gap: 6px;
    width: 100%;
    box-sizing: border-box;
  }

  .price-label {
    font-size: 13px;
    color: #6b7280;
    font-weight: 500;
  }

  :deep(.price-input) {
    width: 100%;
    box-sizing: border-box;

    .el-input__inner {
      font-size: 28px;
      font-weight: 600;
      color: #111827;
      text-align: left;
    }
  }

  :deep(.original-price-input) {
    width: 100%;
    box-sizing: border-box;

    .el-input__inner {
      font-size: 16px;
      text-align: left;
      text-decoration: line-through;
      color: #9ca3af;
    }
  }
}

.publish-btn {
  width: 100%;
  height: 40px;
  background-color: #303133;
  border-color: #303133;
  border-radius: 4px;
  box-sizing: border-box;

  &:hover {
    background-color: #444547;
    border-color: #444547;
  }
}

/* 右侧面板 - 产品图同行排版样式 */
.images-inline-container {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  align-items: flex-start;
  width: 100%;
  box-sizing: border-box;
}

.img-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 100%;
}

.detail-group {
  flex: 1;
  min-width: 0;
}

.section-title {
  font-size: 13px;
  color: #666;
}

/* 封面图上传框 */
.cover-img-box {
  width: 120px;
  height: 120px;
  border: 1px dashed #e5e5e5;
  border-radius: 4px;
  position: relative;
  cursor: pointer;
  overflow: hidden;
  box-sizing: border-box;
  flex-shrink: 0;

  &:hover {
    border-color: #409eff;
  }
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
  font-size: 24px;
}

.cover-file-input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
  z-index: 1;
}

.cover-remove-icon {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 20px;
  height: 20px;
  background-color: rgba(0, 0, 0, 0.5);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  z-index: 2;
  cursor: pointer;

  &:hover {
    background-color: #f56c6c;
  }
}

/* 详情图上传 */
.detail-img-upload {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  width: 100%;
  box-sizing: border-box;

  :deep(.el-upload--picture-card) {
    width: 120px;
    height: 120px;
    border-radius: 4px;
    margin: 0;
    flex-shrink: 0;
  }

  :deep(.el-upload-list) {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    width: 100%;
    margin: 0;

    .el-upload-list__item {
      width: 120px;
      height: 120px;
      margin: 0;
      border-radius: 4px;
      flex-shrink: 0;
    }
  }
}

.detail-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
  font-size: 24px;
}

/* ========== 核心修改：分类选择样式 ========== */
.category-select-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  box-sizing: border-box;
}

.category-level-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  box-sizing: border-box;
}

.level-title {
  font-size: 13px;
  color: #666;
  font-weight: 500;

  .required-tag {
    color: #f56c6c;
    margin-left: 4px;
  }
}

.category-tags-container {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  width: 100%;
  box-sizing: border-box;
}

.tag-item {
  padding: 6px 16px;
  border: 1px solid #e5e5e5;
  border-radius: 16px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s ease;

  &.active {
    background-color: #409eff;
    color: #fff;
    border-color: #409eff;
  }

  &:hover:not(.active) {
    border-color: #409eff;
    color: #409eff;
  }
}

.no-child-tips {
  font-size: 12px;
  color: #909399;
  padding: 8px 0;
  box-sizing: border-box;
}

/* 库存输入 */
.stock-input {
  width: 200px;
  max-width: 100%;
  box-sizing: border-box;

  :deep(.el-input-number__wrapper) {
    border-radius: 4px;
    width: 100%;
    box-sizing: border-box;
  }
}

/* 富文本编辑器容器 */
.editor-container {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.editor-block {
  margin-top: 30px;
  width: 100%;
  box-sizing: border-box;

  :deep(.ql-toolbar) {
    border-radius: 4px 4px 0 0;
    border: 1px solid #e5e5e5;
    width: 100%;
    box-sizing: border-box;
  }

  :deep(.ql-container) {
    border-radius: 0 0 4px 4px;
    border: 1px solid #e5e5e5;
    border-top: none;
    width: 100%;
    box-sizing: border-box;
  }
}
</style>
