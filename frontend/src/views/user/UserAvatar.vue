<script setup>
import { Plus, Upload } from '@element-plus/icons-vue'
import {ref} from 'vue'
import avatar from '@/assets/default.png'
const uploadRef = ref()

import useUserInfoStore from '@/stores/userInfo.js'
const userInfoStore = useUserInfoStore();

import {useTokenStore} from '@/stores/token.js'
const tokenStore = useTokenStore();
import { getUploadUrl } from '@/utils/serviceUrls.js'
//用户头像地址
const imgUrl = ref(userInfoStore.info.userPic)
const uploadUrl = getUploadUrl()

//图片上传成功地回调函数
const uploadSuccess = (result) => {
  imgUrl.value = result.data.url//base64
}

import { userAvatarUpdateService } from '@/api/user.js'
import { ElMessage } from 'element-plus'
//头像修改
const updateAvatar = async () => {
  //调用接口
  let result = await userAvatarUpdateService(imgUrl.value);
  ElMessage.success(result.data? result.data : '修改成功')

  //修改pinia中的数据
  userInfoStore.info.userPic = imgUrl.value
}
</script>

<template>
  <el-card class="page-container no-select">
    <template #header>
      <div class="header">
        <span>更换头像</span>
      </div>
    </template>
    <el-row>
      <el-col :span="30">
        <el-upload
            ref="uploadRef"
            class="avatar-uploader"
            :show-file-list="false"
            :auto-upload="true"
            :action="uploadUrl"
            :name="file"
            :headers="{
              Authorization: tokenStore.token
            }"
            :on-success="uploadSuccess"
        >
          <img v-if="imgUrl" :src="imgUrl" class="avatar" alt="" :style="{ objectFit: 'cover' }"/>
          <img v-else :src="avatar" width="278" alt=""/>
        </el-upload>
        <br />
        <el-button type="primary" :icon="Plus" size="large"  @click="uploadRef.$el.querySelector('input').click()">
          选择图片
        </el-button>
        <el-button type="success" :icon="Upload" size="large" @click="updateAvatar">
          上传头像
        </el-button>
      </el-col>
    </el-row>
  </el-card>
</template>

<style lang="scss" scoped>
.avatar-uploader {
  :deep() {
    .avatar {
      width: 278px;
      height: 278px;
      display: block;
    }

    .el-upload {
      border: 1px dashed var(--el-border-color);
      border-radius: 6px;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      transition: var(--el-transition-duration-fast);
    }

    .el-upload:hover {
      border-color: var(--el-color-primary);
    }

    .el-icon.avatar-uploader-icon {
      font-size: 28px;
      color: #8c939d;
      width: 278px;
      height: 278px;
      text-align: center;
    }
  }
}
</style>
