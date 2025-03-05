package com.timeless.timelesspicturebackend.auth;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.SerializeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.timeless.timelesspicturebackend.auth.model.SpaceUserAuthConfig;
import com.timeless.timelesspicturebackend.auth.model.SpaceUserAuthContext;
import com.timeless.timelesspicturebackend.auth.model.SpaceUserRole;
import com.timeless.timelesspicturebackend.model.entity.Space;
import com.timeless.timelesspicturebackend.model.entity.SpaceUser;
import com.timeless.timelesspicturebackend.model.entity.User;
import com.timeless.timelesspicturebackend.model.enums.SpaceRoleEnum;
import com.timeless.timelesspicturebackend.model.enums.SpaceTypeEnum;
import com.timeless.timelesspicturebackend.service.SpaceUserService;
import com.timeless.timelesspicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SpaceUserAuthManager {
    @Resource
    private UserService userService;
    @Resource
    private SpaceUserService spaceUserService;


    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;


    static {
        String jsonStr = ResourceUtil.readUtf8Str("biz/SpaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(jsonStr, SpaceUserAuthConfig.class);
    }

    /**
     * 根据角色获取权限列表
     */
    public List<String> getPermissionsByRole(String roleKey) {
        if (StrUtil.isBlank(roleKey)) {
            return new ArrayList<>();
        }
        SpaceUserRole spaceUserRole = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(role -> role.getKey().equals(roleKey))
                .findFirst()
                .orElse(null);
        if (spaceUserRole == null) {
            return new ArrayList<>();
        }
        return spaceUserRole.getPermissions();
    }

    public List<String> getPermissionList(Space space, User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }
        // 管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库
        if (space == null) {
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
            return new ArrayList<>();
        }
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }
        // 根据空间获取对应的权限
        switch (spaceTypeEnum) {
            case PRIVATE:
                // 私有空间，仅本人或管理员有所有权限
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                // 团队空间，查询 SpaceUser 并获取角色和权限
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                } else {
                    return getPermissionsByRole(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }


}
