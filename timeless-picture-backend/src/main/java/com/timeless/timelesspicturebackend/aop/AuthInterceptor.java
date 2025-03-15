package com.timeless.timelesspicturebackend.aop;

import com.timeless.timelesspicturebackend.annotation.AuthCheck;
import com.timeless.timelesspicturebackend.exception.ErrorCode;
import com.timeless.timelesspicturebackend.exception.ThrowUtils;
import com.timeless.timelesspicturebackend.model.entity.User;
import com.timeless.timelesspicturebackend.model.enums.UserRoleEnum;
import com.timeless.timelesspicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect // 切面
@Component
public class AuthInterceptor {
    @Resource
    private UserService userService;

    /**
     * 执行拦截器
     *
     * @param joinPoint 切点
     * @param authCheck 注解
     * @return 放行
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 不需要权限，放行
        if (mustRoleEnum == null) {
            joinPoint.proceed();
        }
        // 需要权限，判断是否有权限
        // 获取当前用户的权限
        UserRoleEnum userRole = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 没有权限,拒绝
        ThrowUtils.throwIf(userRole == null, ErrorCode.NO_AUTH_ERROR);
        // 要求用户有管理员权限，但用户没有管理员权限，拒绝
        ThrowUtils.throwIf(UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRole),
                ErrorCode.NO_AUTH_ERROR);
        // 放行
        return joinPoint.proceed();
    }
}
