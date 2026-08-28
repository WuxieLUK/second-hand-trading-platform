import request from "@/utils/request.js";

//权限分类列表查询
export const permissionListService=(params)=>{
    return request.get('/api/backAll/permission/list',{params:params});
}

//新增权限
export const permissionAddService = (permissionData)=>{
    return request.post('/api/backAll/permission/add',permissionData);
}

//编辑权限
export const permissionUpdateInfoServices = (permissionData)=>{
    return request.put('/api/backAll/permission/update',permissionData);
}

//删除权限
export const permissionDeleteService = (id)=>{
    return request.delete(`/api/backAll/permission/delete/${id}`);
}

//获取权限角色列表
export const permissionRolesListService=(params)=>{
    return request.get('/api/backAll/permission/permissionRolesList',{params:params});
}

//新增权限角色表
export const permissionRolesAdd = (permissionRolesData)=>{
    return request.post('/api/backAll/permission/permissionRolesAdd',permissionRolesData);
}

//删除权限角色表
export const permissionRolesDelete = (id)=>{
    return request.delete(`/api/backAll/permission/permissionRolesDelete/${id}`);
}

//获取所有权限列表
export const allPermissionList = () => {
    return request.get('/api/backAll/permission/allPermissionList');
}


