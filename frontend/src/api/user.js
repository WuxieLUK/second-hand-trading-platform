//导入request.js请求工具
import request from '@/utils/request'

//提供调用注册接口的函数
export const userRegisterServices=(registerData)=>{
    //借助于URLSeacherParams完成传递
    const params=new URLSearchParams()
    params.append('username',registerData.username)
    params.append('password',registerData.password)
    return request.post('/api/user/user/register',params)
    //先传到服务器之后再通过request返回服务器响应的结果
}

//提供调用登录接口的函数
export const userLoginServices=(loginData)=>{
    //借助于URLSeacherParams完成传递
    const params=new URLSearchParams()
    /*for (let key in loginData){
        params.append(key,loginData[key])
    }*/
    params.append('username',loginData.username)
    params.append('password',loginData.password)
    return request.post('/api/user/user/login',params)
}

//获取用户详细信息
export const userInfoServices=()=>{
    return request.get('/api/user/user/info')
}

//修改个人信息
export const userUpdateInfoServices=(updateInfoData)=>{
    return request.put('/api/user/user/updates',updateInfoData)
}

//修改头像
export const userAvatarUpdateService = (avatarUrl) => {
    const params = new URLSearchParams();
    params.append('avatarUrl', avatarUrl)
    return request.post('/api/user/user/updateAvatar', params)
}

//修改密码
export const userPasswordUpdateService = (updatePasswordData) => {
    return request.post('/api/user/user/updatePwd', updatePasswordData)
}

//用户分页列表查询
export const userListService = (params)=>{
    return request.get('/api/user/user/userList',{params:params})
}

//删除用户
export const userDeleteService = (id)=>{
    return request.delete(`/api/user/user/${id}`)
}

//查询所有用户
export const allUserList = ()=>{
    return request.get('/api/user/user/allUserList')
}