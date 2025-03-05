package com.timeless.timelesspicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.timeless.timelesspicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.timeless.timelesspicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.timeless.timelesspicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.timeless.timelesspicturebackend.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 26315
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-02-01 16:49:56
*/
public interface SpaceUserService extends IService<SpaceUser> {

    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    void validSpaceUser(SpaceUser spaceUser, boolean add);

    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);
}
