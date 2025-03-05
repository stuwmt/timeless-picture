package com.timeless.timelesspicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.timeless.timelesspicturebackend.exception.BusinessException;
import com.timeless.timelesspicturebackend.exception.ErrorCode;
import com.timeless.timelesspicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 文件上传
 */
@Service
public class FilePictureUpload extends PictureUploadTemplate {
    /**
     * 处理文件
     *
     * @param inputSource 文件来源
     * @param file        文件
     */
    @Override
    protected void processFile(Object inputSource, File file) {
        MultipartFile multiPartFile = (MultipartFile) inputSource;
        try {
            multiPartFile.transferTo(file);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }
    }

    /**
     * 获取原始文件名
     *
     * @param inputSource 文件来源
     * @return 原始文件名
     */
    @Override
    protected String getOriginFilename(Object inputSource) {
        MultipartFile multiPartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multiPartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        return multiPartFile.getOriginalFilename();
    }

    /**
     * 校验图片
     *
     * @param inputSource 文件来源
     */
    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 1. 校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024L;
        ThrowUtils.throwIf(fileSize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
        // 2. 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的文件后缀
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
    }
}
