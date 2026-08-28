<script setup>
import { ref } from 'vue'
import useUserInfoStore from '@/stores/userInfo.js'
import { userUpdateInfoServices } from '@/api/user.js'
import { ElMessage } from "element-plus";

const userInfoStore = useUserInfoStore();
// 浅拷贝当前 store 信息，新增三个字段的兜底处理
const userInfo = ref({
  ...userInfoStore.info,
  phone: userInfoStore.info.phone || '',
  // ========== 新增字段 ==========
  major: userInfoStore.info.major || '',       // 专业
  grade: userInfoStore.info.grade || '',       // 年级
  campusScene: userInfoStore.info.campusScene || '', // 校园场景
  tags: userInfoStore.info.tags || '' // 用户标签（新增）
})

// 【核心修改】年级选项改为大一/大二/大三/大四，补充研究生/博士生
const gradeOptions = [
  { label: '大一', value: '大一' },
  { label: '大二', value: '大二' },
  { label: '大三', value: '大三' },
  { label: '大四', value: '大四' },
  { label: '研究生', value: '研究生' },
  { label: '博士生', value: '博士生' }
]

const campusSceneOptions = [
  { label: '图书馆', value: '图书馆' },
  { label: '教学楼', value: '教学楼' },
  { label: '宿舍区', value: '宿舍区' },
  { label: '食堂', value: '食堂' },
  { label: '快递站', value: '快递站' },
  { label: '操场/体育馆', value: '操场/体育馆' },
  { label: '校内商业街', value: '校内商业街' }
]

// 优化校验规则：调整标签规则（支持逗号分隔多标签）
const rules = {
  nickname: [
    { required: false, message: '请输入用户昵称', trigger: 'blur' },
    { pattern: /^\S{2,10}$/, message: '昵称必须是2-10位的非空字符串', trigger: 'blur' }
  ],
  email: [
    { required: false, message: '请输入用户邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  phone: [
    { required: false, message: '请输入手机号码', trigger: 'blur' },
    // 中国大陆手机号正则（支持13/14/15/16/17/18/19开头）
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号码', trigger: 'blur' }
  ],
  // ========== 优化字段校验规则 ==========
  major: [
    { required: false, message: '请输入您的专业', trigger: 'blur' },
    { pattern: /^\S{2,20}$/, message: '专业必须是2-20位的非空字符串', trigger: 'blur' }
  ],
  grade: [
    { required: false, message: '请选择您的年级', trigger: 'change' }
  ],
  campusScene: [
    { required: false, message: '请选择常用校园场景', trigger: 'change' }
  ],
  tags: [ // 优化：支持逗号分隔多标签，允许中文/英文逗号
    { required: false, message: '请输入用户标签', trigger: 'blur' },
    { pattern: /^[\u4e00-\u9fa5a-zA-Z0-9,，]{2,50}$/, message: '标签为2-50位，可输入多个标签用逗号分隔', trigger: 'blur' }
  ]
}

// 优化：添加表单校验（避免空数据提交）
const formRef = ref(null)
const updateUserInfoStore = async () => {
  // 先进行表单校验
  try {
    await formRef.value.validate()
  } catch (error) {
    // 校验失败时终止提交
    ElMessage.error('表单填写有误，请检查后重新提交')
    return
  }

  // 调用接口更新后台
  try {
    let result = await userUpdateInfoServices(userInfo.value)
    ElMessage.success(result.data ? result.data : '修改成功')

    // 合并数据，保留原有的 userPic 等其他字段，新增所有字段
    const updatedInfo = {
      ...userInfoStore.info,
      nickname: userInfo.value.nickname,
      email: userInfo.value.email,
      phone: userInfo.value.phone,
      // ========== 新增字段 ==========
      major: userInfo.value.major,
      grade: userInfo.value.grade,
      campusScene: userInfo.value.campusScene,
      tags: userInfo.value.tags // 用户标签（新增）
    }

    // 更新 Pinia 状态
    userInfoStore.setInfo(updatedInfo)
  } catch (error) {
    // 接口调用失败时提示
    ElMessage.error('修改失败，请稍后重试')
    console.error('用户信息修改失败：', error)
  }
}
</script>

<template>
  <el-card class="page-container no-select">
    <template #header>
      <div class="header"><span>基本资料</span></div>
    </template>
    <el-row>
      <el-col :span="20">
        <!-- 新增表单引用，用于校验 -->
        <el-form
            ref="formRef"
            :model="userInfo"
            :rules="rules"
            label-width="100px"
            size="large"
        >
          <el-form-item label="登录名称">
            <el-input v-model="userInfo.username" disabled></el-input>
          </el-form-item>
          <el-form-item label="用户昵称" prop="nickname">
            <el-input v-model="userInfo.nickname"></el-input>
          </el-form-item>
          <el-form-item label="用户邮箱" prop="email">
            <el-input v-model="userInfo.email"></el-input>
          </el-form-item>
          <!-- 新增手机号表单项 -->
          <el-form-item label="手机号码" prop="phone">
            <el-input
                v-model="userInfo.phone"
                placeholder="请输入11位手机号码"
                maxlength="11"
                show-word-limit
            ></el-input>
          </el-form-item>
          <!-- ========== 优化后的字段表单项 ========== -->
          <el-form-item label="所学专业" prop="major">
            <el-input
                v-model="userInfo.major"
                placeholder="请输入您的专业（如：计算机科学与技术）"
                maxlength="20"
                show-word-limit
            ></el-input>
          </el-form-item>
          <el-form-item label="所在年级" prop="grade">
            <el-select
                v-model="userInfo.grade"
                placeholder="请选择您的年级"
                style="width: 100%;"
            >
              <el-option
                  v-for="item in gradeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
              ></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="常用场景" prop="campusScene">
            <el-select
                v-model="userInfo.campusScene"
                placeholder="请选择常用校园场景"
                style="width: 100%;"
            >
              <el-option
                  v-for="item in campusSceneOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
              ></el-option>
            </el-select>
          </el-form-item>
          <!-- 用户标签表单项（优化提示） -->
          <el-form-item label="个人标签" prop="tags">
            <el-input
                v-model="userInfo.tags"
                placeholder="请输入标签（如：考研,二手书,电子产品，多个标签用逗号分隔）"
                maxlength="50"
                show-word-limit
            >
              <template #suffix>
                <span style="font-size: 12px; color: #999;">例：考研,二手书,电子产品</span>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="updateUserInfoStore">提交修改</el-button>
          </el-form-item>
        </el-form>
      </el-col>
    </el-row>
  </el-card>
</template>

<style scoped>
.page-container { border: none; box-shadow: none; }
/* 可选：优化输入框样式 */
:deep(.el-input__inner) {
  padding: 0 15px;
}
/* 优化下拉选择框样式 */
:deep(.el-select .el-input__inner) {
  padding: 0 15px;
}
/* 标签输入框后缀文字样式 */
:deep(.el-input__suffix) {
  right: 10px;
}
</style>