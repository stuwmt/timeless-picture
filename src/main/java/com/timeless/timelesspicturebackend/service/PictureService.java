package com.timeless.timelesspicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.timeless.timelesspicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.timeless.timelesspicturebackend.model.dto.picture.*;
import com.timeless.timelesspicturebackend.model.entity.Picture;
import com.timeless.timelesspicturebackend.model.entity.User;
import com.timeless.timelesspicturebackend.model.vo.PictureVO;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author undefined
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-01-19 17:01:44
 */
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     *
     * @param inputSource          文件来源
     * @param pictureUploadRequest 上传图片请求
     * @param loginUser            当前登录用户
     * @return 图片信息
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    void validPicture(Picture picture);

    /**
     * 审核图片
     *
     * @param pictureReviewRequest 审核请求
     * @param loginUser            当前登录用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);


    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );

    /**
     * 清理图片
     *
     * @param oldPicture 旧图片
     */
    void clearPicture(Picture oldPicture);

    /**
     * 检查图片权限
     *
     * @param loginUser 当前登录用户
     * @param picture   图片
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 删除图片
     *
     * @param pictureId 图片 id
     * @param loginUser 当前登录用户
     */
    void deletePicture(long pictureId, User loginUser);

    void editPicture(PictureEditRequest pictureEditRequest,
                     User loginUser);

    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    //批量更新图片
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    // 创建任务
    CreateOutPaintingTaskResponse createOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);
}
