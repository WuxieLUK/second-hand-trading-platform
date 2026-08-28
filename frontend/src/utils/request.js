//导入axios  npm install axios
import axios from 'axios';
import {ElMessage} from 'element-plus'
import useUserInfoStore from '@/stores/userInfo.js'
import { getGatewayBaseUrl } from '@/utils/serviceUrls.js'
//定义一个变量,记录公共的前缀  ,  baseURL
const baseURL = getGatewayBaseUrl();
//创建axios实例
const instance = axios.create({
    baseURL: baseURL,
    timeout: 10000, // 新增超时配置，避免请求一直挂起
})

import {useTokenStore} from '@/stores/token.js'
//添加请求拦截器
instance.interceptors.request.use(
    (config)=>{
        //请求前的回调
        //添加token
        const tokenStore = useTokenStore();
        //判断有没有token
        if(tokenStore.token){
            //在请求头中添加token返回给服务器后端，后端根据token进行身份验证
            config.headers.Authorization = tokenStore.token
        }
        return config;
    },
    (err)=>{
        //请求错误的回调
        ElMessage.error('请求发送失败，请检查网络');
        return Promise.reject(err);
    }
)

//添加响应拦截器对象
instance.interceptors.response.use(
    //请求响应成功
    result=>{
        //success
        if (result.data.code === 0){
            return result.data;
        }
        //error
        ElMessage.error(result.data.message?result.data.message:'服务异常');
        //返回被拒绝的原因
        return Promise.reject(result.data.message);
    },
    err=>{
        // 核心修复：先判断err.response是否存在，避免跨域/网络错误时的undefined报错
        if (!err.response) {
            ElMessage.error('跨域配置失败或后端服务未启动，请检查！');
            return Promise.reject(err);
        }
        //如果响应状态码是401，代表未登录/令牌过期
        if(err.response.status===401){
            const tokenStore = useTokenStore();
            tokenStore.removeToken(); // 清空token
            const userInfoStore = useUserInfoStore();
            userInfoStore.removeInfo();
            ElMessage.warning('请先登录！');
            // 可选：跳转到登录页（如果需要）
            // window.location.href = '/login';
        }else{
            ElMessage.error('服务异常，请联系管理员！');
        }
        return Promise.reject(err);//异步的状态转化成失败的状态
    }
)

export default instance;
