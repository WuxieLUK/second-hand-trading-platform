<script setup>
import { ref } from 'vue'
import { userPasswordUpdateService } from '@/api/user'
import { ElMessage ,ElMessageBox} from 'element-plus'

import { useRouter } from 'vue-router';
const router = useRouter();
import {useTokenStore} from '@/stores/token.js'
const tokenStore = useTokenStore();
const passwordInfo = ref({
  oldPwd: '',
  newPwd: '',
  rePwd: ''
})

const checkRePassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('确认密码不能为空'))
  } else if (value !== passwordInfo.value.newPwd) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  oldPwd: [
    { required: true, message: '请输入原密码', trigger: 'blur' },
    {
      pattern: /^\S{1,16}$/,
      message: '原密码必须是1-16位的非空字符串',
      trigger: 'blur'
    }
  ],
  newPwd: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      pattern: /^\S{1,16}$/,
      message: '新密码必须是1-16位的非空字符串',
      trigger: 'blur'
    }
  ],
  rePwd: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: checkRePassword, trigger: 'blur' },
    {
      pattern: /^\S{1,16}$/,
      message: '新密码必须是1-16位的非空字符串',
      trigger: 'blur'
    }
  ]
}

const loading = ref(false)

const updatePassword = async () => {
  ElMessageBox.confirm(
      '确认修改密码？',
      '警告',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
      }
  )
      .then(async () => {
        const formattedData = {
          old_pwd: passwordInfo.value.oldPwd,
          new_pwd: passwordInfo.value.newPwd,
          re_pwd: passwordInfo.value.rePwd
        }
        loading.value = true;
        try {
          let result = await userPasswordUpdateService(formattedData)
          if (result.code === 0) {
            ElMessage.success(result.data ? result.data : '密码修改成功')
            ElMessage.warning(result.data ? result.data : '请重新登录')
            tokenStore.removeToken();
            passwordInfo.value = { oldPwd: '', newPwd: '', rePwd: '' }
            await router.push('/homes/login')
          } else {
            ElMessage.error(result.message ? result.message : '修改失败')
          }
        } catch (error) {
          ElMessage.error('修改失败')
        } finally {
          loading.value = false
        }
      })
}

</script>

<template>
  <el-card class="page-container no-select">
    <template #header>
      <div class="header">
        <span><strong>密码修改</strong></span>
      </div>
    </template>
    <el-row>
      <el-col :span="20">
        <el-form :model="passwordInfo" :rules="rules" label-width="100px" size="large">
          <el-form-item label="原密码" prop="oldPwd">
            <el-input v-model="passwordInfo.oldPwd" type="password"></el-input>
          </el-form-item>
          <el-form-item label="新密码" prop="newPwd">
            <el-input v-model="passwordInfo.newPwd" type="password"></el-input>
          </el-form-item>
          <el-form-item label="确认密码" prop="rePwd">
            <el-input v-model="passwordInfo.rePwd" type="password"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="updatePassword" :loading="loading">确认修改</el-button>
          </el-form-item>
        </el-form>
      </el-col>
    </el-row>
  </el-card>
</template>

<style lang="scss" scoped>
</style>