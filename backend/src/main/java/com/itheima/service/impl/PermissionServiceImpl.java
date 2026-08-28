package com.itheima.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.mapper.PermissionsMapper;
import com.itheima.pojo.*;
import com.itheima.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionsMapper permissionsMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 缓存key前缀
    private static final String PERMISSION_KEY = "user:permission:%d";

    // ============== 【核心方法：查询用户权限（带缓存，适配切面鉴权） ==============
    @Transactional(readOnly = true)
    @Override
    public List<String> getUserPermissions(Integer userId) {
        // 1. 参数校验（避免无效查询）
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID无效！");
        }
        // 【补充】权限查询前，清理用户权限缓存
        clearUserPermissionCache();

        // 2. 构造缓存Key（格式：user:permission:1）
        String cacheKey = String.format(PERMISSION_KEY, userId);

        // 3. 先查Redis缓存，命中直接返回（杜绝重复数据库查询）
        List<String> permissions = (List<String>) redisTemplate.opsForValue().get(cacheKey);
        if (permissions != null && !permissions.isEmpty()) {
            return permissions;
        }

        // 4. 缓存未命中，查询数据库【关键修改：适配切面原有Mapper方法selectMenuPermsByUserId】
        // 替换为你切面中实际调用的Mapper方法，确保查询逻辑一致
        permissions = permissionsMapper.selectMenuPermsByUserId(userId);

        // 5. 空值防护：避免将null/空列表存入Redis（优化健壮性）
        if (permissions == null || permissions.isEmpty()) {
            return permissions;
        }

        // 6. 将查询结果存入Redis，设置30分钟过期（平衡性能与数据一致性）
        redisTemplate.opsForValue().set(cacheKey, permissions, 30, TimeUnit.MINUTES);

        return permissions;
    }

    //添加权限
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Permission permission) {
        permissionsMapper.add(permission);
        // 【补充】权限新增后，清理用户权限缓存（避免脏数据）
        clearUserPermissionCache();
    }

    //查询所有权限
    @Override
    @Transactional(readOnly = true)
    public PageBean<Permission> list(Integer pageNum, Integer pageSize,String permissionName, String permissionDescription) {
        PageBean<Permission> pb = new PageBean<>();
        PageHelper.startPage(pageNum,pageSize);
        List<Permission> as =permissionsMapper.list(permissionName,permissionDescription);
        //Page中提供了方法，可以获取PageHelper分页查询后，查询的总记录条数和当前页数据
        Page<Permission> p = (Page<Permission>) as;
        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());
        return pb;
    }

    @Override
    @Transactional(readOnly = true)
    public Permission findById(Integer id) {
        return permissionsMapper.findById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Permission permission) {
        permissionsMapper.update(permission);
        // 【补充】权限修改后，清理用户权限缓存
        clearUserPermissionCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        permissionsMapper.delete(id);
        //删除关联表信息
        // 【补充】权限删除后，清理用户权限缓存
        clearUserPermissionCache();
    }

    @Override
    @Transactional(readOnly = true)
    public PageBean<PermissionRoles> permissionRolesList(Integer pageNum, Integer pageSize, Integer permissionId, Integer roleId) {
        PageBean<PermissionRoles> pb = new PageBean<>();
        PageHelper.startPage(pageNum,pageSize);
        List<PermissionRoles> as =permissionsMapper.permissionRolesList(permissionId,roleId);
        //Page中提供了方法，可以获取PageHelper分页查询后，查询的总记录条数和当前页数据
        Page<PermissionRoles> p = (Page<PermissionRoles>) as;
        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());
        return pb;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void permissionRolesAdd(PermissionRoles permissionRoles) {
        permissionsMapper.permissionRolesAdd(permissionRoles);
        // 【补充】权限-角色关联新增后，清理用户权限缓存
        clearUserPermissionCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void permissionRolesDelete(Integer id) {
        permissionsMapper.permissionRolesDelete(id);
        // 【补充】权限-角色关联删除后，清理用户权限缓存
        clearUserPermissionCache();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> allPermissionList() {
        return permissionsMapper.allPermissionList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkPermissionExists(String permissionName) {
        return permissionsMapper.checkPermissionExists(permissionName);
    }

    // ============== 【私有方法】：批量清理用户权限缓存（不对外暴露，仅内部调用） ==============
    private void clearUserPermissionCache() {
        // 匹配所有 "user:permission:*" 前缀的缓存Key并批量删除，简单高效
        // 注意：keys命令在Redis大数据量下性能一般，中小型项目可放心使用
        redisTemplate.delete(redisTemplate.keys("user:permission:*"));
    }
}