<!--登录组件-->
<script setup lang="js">
import { User, Lock } from '@element-plus/icons-vue'
import { ref, onMounted } from 'vue' // 新增onMounted钩子
import { ElMessage } from 'element-plus'
import { useTokenStore } from '@/stores/token.js'
import { userRegisterServices, userLoginServices, userInfoServices } from "@/api/user.js";
import { useRouter } from "vue-router";

// 控制注册与登录表单的显示,默认显示登录
const isRegister = ref(false)

// 记住我复选框状态
const rememberMe = ref(false)

// 定义数据模型
const registerData = ref({
  username: '',
  password: '',
  rePassword: ''
})

// 校验密码的函数
const checkPassword = (rules, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerData.value.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const tokenStore = useTokenStore()
const router = useRouter()

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 1, max: 16, message: '长度在 1 到 16 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 1, max: 16, message: '长度在 1 到 16 个字符', trigger: 'blur' }
  ],
  rePassword: [
    { validator: checkPassword, trigger: 'blur' },
  ]
}

// 新增：从本地存储加载记住的用户信息
const loadRememberedUser = () => {
  const rememberedUser = localStorage.getItem('rememberedUser')
  if (rememberedUser) {
    try {
      const { username, password } = JSON.parse(rememberedUser)
      registerData.value.username = username
      registerData.value.password = password
      rememberMe.value = true // 自动勾选复选框
    } catch (e) {
      localStorage.removeItem('rememberedUser') // 缓存损坏则清除
    }
  }
}

// 新增：保存用户信息到本地存储（记住我勾选时）
const saveRememberedUser = (username, password) => {
  localStorage.setItem('rememberedUser', JSON.stringify({ username, password }))
}

// 新增：清除本地存储的用户信息
const clearRememberedUser = () => {
  localStorage.removeItem('rememberedUser')
}

// 调用后台接口，完成注册
const register = async () => {
  if (!registerData.value.username || !registerData.value.password) {
    ElMessage.warning('请填写用户名和密码');
    return;
  }
  let result = await userRegisterServices(registerData.value)
  ElMessage.success(result.data ? result.data : '注册成功');
  // 注册成功则跳转登录
  isRegister.value = false;
  clearRegisterData();
}

// 登录逻辑改造（增加记住我处理）
const login = async () => {
  if (!registerData.value.username || !registerData.value.password) {
    ElMessage.warning('请填写用户名和密码');
    return;
  }

  try {
    let result = await userLoginServices(registerData.value)
    ElMessage.success('登录成功');

    // 把得到的token保存到pinia中
    tokenStore.setToken(result.data);

    // 处理记住我逻辑
    if (rememberMe.value) {
      // 勾选状态：保存用户名密码到本地存储
      saveRememberedUser(registerData.value.username, registerData.value.password)
    } else {
      // 未勾选：清除本地存储的用户信息
      clearRememberedUser()
    }

    // 获取用户信息并跳转
    let userInfo = await userInfoServices()
    if (userInfo.data.id === 1) {
      ElMessage.success('欢迎您，尊贵的管理员大人！');
      await router.push('/about/system');
    } else {
      await router.push('/homes/home');
    }
  } catch (error) {
    ElMessage.error('登录失败：' + (error.message || '网络异常'))
  }
}

// 定义函数，清空数据模型的数据
const clearRegisterData = () => {
  registerData.value = {
    username: '',
    password: '',
    rePassword: ''
  }
  // 清空数据时，若未勾选记住我则取消复选框状态（可选）
  if (!rememberMe.value) {
    rememberMe.value = false
  }
}

// 页面挂载时加载记住的用户信息
onMounted(() => {
  loadRememberedUser()
})
</script>

<template>
  <el-row class="login-page">
    <el-col class="form">
      <!-- 注册表单 -->
      <el-form ref="form" size="large" autocomplete="off" v-if="isRegister" :model="registerData" :rules="rules">
        <el-form-item>
          <h1 class="title">注册新用户</h1>
        </el-form-item>
        <el-form-item prop="username">
          <el-input :prefix-icon="User" placeholder="请输入用户名" v-model="registerData.username"></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input :prefix-icon="Lock" type="password" placeholder="请输入密码" v-model="registerData.password"></el-input>
        </el-form-item>
        <el-form-item prop="rePassword">
          <el-input :prefix-icon="Lock" type="password" placeholder="请输入再次密码" v-model="registerData.rePassword"></el-input>
        </el-form-item>
        <!-- 注册按钮 -->
        <el-form-item>
          <el-button class="button no-select" type="warning" auto-insert-space @click="register">
            立即注册
          </el-button>
        </el-form-item>
        <el-form-item class="flex">
          <p style="color: #787575">已有帐号？</p>
          <el-link :underline="false" @click="isRegister = false;clearRegisterData()">
            立即登录
          </el-link>
        </el-form-item>
      </el-form>

      <!-- 登录表单 -->
      <el-form ref="form" size="large" autocomplete="off" v-else :model="registerData" :rules="rules">
        <el-form-item>
          <h1 class="title">登录</h1>
        </el-form-item>
        <el-form-item prop="username">
          <el-input :prefix-icon="User" placeholder="请输入用户名" v-model="registerData.username"></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input name="password" :prefix-icon="Lock" type="password" placeholder="请输入密码" v-model="registerData.password"></el-input>
        </el-form-item>
        <el-form-item class="flex">
          <div class="flex">
            <!-- 绑定复选框状态到rememberMe -->
            <el-checkbox class="no-select" v-model="rememberMe">记住我</el-checkbox>
            <el-link type="primary" :underline="false">忘记密码？</el-link>
          </div>
        </el-form-item>
        <!-- 登录按钮 -->
        <el-form-item>
          <el-button class="button no-select" type="success" auto-insert-space @click="login">立即登录</el-button>
        </el-form-item>
        <!--注册按钮-->
        <el-divider>没有账号</el-divider>
        <el-form-item>
          <el-button class="button no-select" type="danger" auto-insert-space @click="isRegister = true;clearRegisterData()">
            立即注册
          </el-button>
        </el-form-item>
        <el-divider>其它方式</el-divider>
      </el-form>
    </el-col>
  </el-row>
</template>

<style lang="scss" scoped>
.login-page {
  background-color: var(--bg);
  padding-top: 200px;
  height: 100%;
  width: 100%;
  overflow: hidden;

  .form {
    display: flex;
    flex-direction: column;
    justify-content: center;
    user-select: none;
    padding: 30px; // 增加内边距
    margin-top: -250px;

    .title {
      margin: 0 auto;
      color: var(--text);
      font-size: 40px;
    }

    .button {
      width: 100%;
    }

    .flex {
      width: 100%;
      display: flex;
      justify-content: space-between;
    }
  }
}

.welcome-title {
  position: absolute;
  bottom: 30px;
  left: 30px;
  color: white;
  text-shadow: 0 0 10px white;
}

.welcome-title div:first-child {
  font-size: 30px;
  font-weight: bold;
}
</style>