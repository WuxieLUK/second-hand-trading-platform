package com.itheima.validation;

import com.itheima.anno.PreAuthorize;
import com.itheima.pojo.Result;
import com.itheima.service.PermissionService;
import com.itheima.util.ThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Aspect
public class PreAuthorizeAspect {

    // 【修改1：注入PermissionService（带缓存），替换直接注入PermissionsMapper】
    @Autowired
    private PermissionService permissionService;

    //配置切入点，@annotation
    @Pointcut("@annotation(com.itheima.anno.PreAuthorize)")
    public void authorizePointCut() {
    }

    /*
     * 后端接口资源鉴权：
     *     1. 查询用户的所有接口资源权限列表（从缓存优先查询）
     *     2. 获取当前接口资源的访问权限标识符
     *     3. 比较
     *  @return
     */
    @Around("authorizePointCut()")//前置通知，主要控制目标方法是否执行
    public Object handle(ProceedingJoinPoint pjp) throws Throwable {
        //1. 查询用户的所有接口资源权限列表（【修改2：调用Service带缓存的方法，拦截重复查询】）
        // 请求头中模拟用户信息: user_id
        Map<String,Object> map = ThreadLocalUtil.get();
        if (map == null) {
            return Result.error("用户未登录，请先登录");
        }
        Integer id = (Integer)map.get("id");
        // 检查id是否为空
        if (id == null) {
            return Result.error("用户信息不完整");
        }

        // 【核心优化：不再直接调用Mapper，而是调用Service的缓存方法】
        List<String> perms = permissionService.getUserPermissions(id);
        // 补充：若权限列表为空，直接返回无权限
        if (perms == null || perms.isEmpty()) {
            return Result.error("达咩，你无权访问！");
        }

        //2. 获取当前接口资源的访问权限标识符（原有逻辑不变）
        MethodSignature signature = (MethodSignature) pjp.getSignature();//获取方法签名对象
        Method method = signature.getMethod();//获取目标方法对象（后端接口资源）
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assert annotation != null;

        //不为空
        String methodPermission = annotation.value();

        //3. 比较（原有逻辑不变）
        boolean result = perms.contains(methodPermission);

        //如果当前用户角色没有当前资源接口的权限：403/无礼之徒，你无权访问！
        if(!result){
            return Result.error("达咩，你无权访问！");
        }
        //5.如果具有当前资源的访问权限，正常执行目标方法
        return pjp.proceed();
    }

}