package com.timeless.timelessspacebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.timeless.timelesspicturebackend.model.dto.space.SpaceAddRequest;
import com.timeless.timelesspicturebackend.model.dto.space.SpaceQueryRequest;
import com.timeless.timelesspicturebackend.model.entity.Space;
import com.timeless.timelesspicturebackend.model.entity.User;
import com.timeless.timelesspicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author undefined
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-01-23 13:57:05
 */
public interface SpaceService extends IService<Space> {
    /**
     * 构造查询条件
     *
     * @param spaceQueryRequest 空间查询请求
     * @return 查询条件
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 获取空间VO
     *
     * @param space   空间
     * @param request 请求
     * @return 空间VO
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间VO分页
     *
     * @param spacePage 空间分页
     * @param request   请求
     * @return 空间VO分页
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 校验空间
     *
     * @param space 空间
     * @param add   是否新增
     */
    void validSpace(Space space, boolean add);

    /**
     * 根据空间级别填充信息
     *
     * @param space 空间
     */

    void fillSpaceBySpaceEnum(Space space);

    /**
     * 用户创建空间，返回空间 id
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    Space getSpaceByUserId(long userId);

    /**
     * 检查私有空间权限
     * @param space
     * @param loginUser
     */
    void checkSpaceAuth(Space space, User loginUser);

}
