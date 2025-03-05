package com.timeless.timelesspicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.timeless.timelesspicturebackend.exception.BusinessException;
import com.timeless.timelesspicturebackend.exception.ErrorCode;
import com.timeless.timelesspicturebackend.exception.ThrowUtils;
import com.timeless.timelesspicturebackend.manager.sharding.DynamicShardingManager;
import com.timeless.timelesspicturebackend.mapper.SpaceMapper;
import com.timeless.timelesspicturebackend.model.dto.space.SpaceAddRequest;
import com.timeless.timelesspicturebackend.model.dto.space.SpaceQueryRequest;
import com.timeless.timelesspicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.timeless.timelesspicturebackend.model.entity.Picture;
import com.timeless.timelesspicturebackend.model.entity.Space;
import com.timeless.timelesspicturebackend.model.entity.SpaceUser;
import com.timeless.timelesspicturebackend.model.entity.User;
import com.timeless.timelesspicturebackend.model.enums.SpaceLevelEnum;
import com.timeless.timelesspicturebackend.model.enums.SpaceRoleEnum;
import com.timeless.timelesspicturebackend.model.enums.SpaceTypeEnum;
import com.timeless.timelesspicturebackend.model.vo.PictureVO;
import com.timeless.timelesspicturebackend.model.vo.SpaceVO;
import com.timeless.timelesspicturebackend.model.vo.UserVO;
import com.timeless.timelesspicturebackend.service.SpaceUserService;
import com.timeless.timelesspicturebackend.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author undefined
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-01-23 13:57:05
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements com.timeless.timelessspacebackend.service.SpaceService {
    @Resource
    private UserService userService;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private SpaceUserService spaceUserService;
    // @Resource
    // private DynamicShardingManager dynamicShardingManager;

    private final Map<Long, Object> lockMap = new ConcurrentHashMap<>();

    /**
     * 构造查询条件
     *
     * @param spaceQueryRequest 空间查询请求
     * @return 查询条件
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取空间VO
     *
     * @param space   空间
     * @param request 请求
     * @return 空间VO
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    /**
     * 获取空间VO分页
     *
     * @param spacePage 空间分页
     * @param request   请求
     * @return 空间VO分页
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(),
                spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    /**
     * 校验空间
     *
     * @param space 空间
     * @param add   是否新增
     */
    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 创建
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            ThrowUtils.throwIf(ObjUtil.isNull(spaceLevel), ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            ThrowUtils.throwIf(spaceType == null, ErrorCode.PARAMS_ERROR, "空间类型不能为空");
        }
        // 修改数据,如果要改空间级别
        if (spaceLevel != null) {
            ThrowUtils.throwIf(ObjUtil.isNull(spaceLevelEnum), ErrorCode.PARAMS_ERROR, "空间级别不合法");
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称长度不能超过30");
        }
        if (spaceType != null && ObjUtil.isNull(spaceTypeEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不合法");
        }
    }

    /**
     * 根据空间自动填充参数
     *
     * @param space 空间
     */
    @Override
    public void fillSpaceBySpaceEnum(Space space) {
        // 根据空间级别自动填充空间容量
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        ThrowUtils.throwIf(ObjUtil.isNull(spaceLevelEnum), ErrorCode.PARAMS_ERROR, "空间级别不合法");
        long maxCount = spaceLevelEnum.getMaxCount();
        if (space.getMaxCount() == null) {
            space.setMaxCount(maxCount);
        }
        long maxSize = spaceLevelEnum.getMaxSize();
        if (space.getMaxSize() == null) {
            space.setMaxSize(maxSize);
        }
    }

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 转换为实体类
        Space space = new Space();
        BeanUtil.copyProperties(spaceAddRequest, space);
        // 默认值
        if (spaceAddRequest.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (spaceAddRequest.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 自动填充数据
        fillSpaceBySpaceEnum(space);
        // 数据校验
        validSpace(space, true);
        Long userId = loginUser.getId();
        space.setUserId(userId);
        // 权限校验
        if (SpaceLevelEnum.COMMON.getValue() != spaceAddRequest.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别空间");
        }
        // 针对用户id进行加锁
        Object lock = lockMap.computeIfAbsent(userId, k -> new Object());
        synchronized (lock) {
            try {
                Long newSpaceId = transactionTemplate.execute(status -> {
                    // 判断是否存在
                    boolean exists = this.lambdaQuery().eq(Space::getUserId, userId)
                            .eq(Space::getSpaceType, spaceAddRequest.getSpaceType())
                            .exists();
                    ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户只能有一个私有空间");
                    // 操作数据库
                    boolean result = save(space);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
                    // 如果是团队空间，新增团队成员记录
                    if (SpaceTypeEnum.TEAM.getValue() == spaceAddRequest.getSpaceType()) {
                        SpaceUser spaceUser = new SpaceUser();
                        spaceUser.setSpaceId(space.getId());
                        spaceUser.setUserId(userId);
                        spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                        boolean save = spaceUserService.save(spaceUser);
                        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "团队成员创建失败");
                    }
                    // 创建分表
                    // dynamicShardingManager.createSpacePictureTable(space);
                    // 返回写入的id
                    return space.getId();
                });
                // 返回结果
                return Optional.ofNullable(newSpaceId).orElse(-1L);
            } finally {
                // 释放锁，防止内存泄漏
                lockMap.remove(userId);
            }
        }
    }

    @Override
    public Space getSpaceByUserId(long userId) {
        return this.lambdaQuery().eq(Space::getUserId, userId).one();
    }

    @Override
    public void checkSpaceAuth(Space space, User loginUser) {
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        Long userId = space.getUserId();
        // 空间创建者或者管理员
        ThrowUtils.throwIf(!userId.equals(loginUser.getId()) && !userService.isAdmin(loginUser),
                ErrorCode.NO_AUTH_ERROR);
    }
}




