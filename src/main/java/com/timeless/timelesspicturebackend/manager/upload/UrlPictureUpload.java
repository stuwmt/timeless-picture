package com.timeless.timelesspicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.timeless.timelesspicturebackend.exception.BusinessException;
import com.timeless.timelesspicturebackend.exception.ErrorCode;
import com.timeless.timelesspicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Service
public class UrlPictureUpload extends PictureUploadTemplate {
    /**
     * 处理文件
     *
     * @param inputSource 文件来源
     * @param file        文件
     */
    @Override
    protected void processFile(Object inputSource, File file) {
        String fileUrl = (String) inputSource;
        HttpUtil.downloadFile(fileUrl, file);
    }

    /**
     * 获取原始文件名
     *
     * @param inputSource 文件来源
     * @return 原始文件名
     */
    @Override
    protected String getOriginFilename(Object inputSource) {
       return FileUtil.mainName((String) inputSource);
    }

    /**
     * 校验图片
     *
     * @param inputSource 文件来源
     */
    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        if (fileUrl == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址不能为空");
        }
        // 1.验证url格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式错误");
        }
        // 2.校验url协议
        if (!fileUrl.startsWith("http") && !fileUrl.startsWith("https")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持http和https协议");
        }
        // 3.发送head请求，校验文件是否存在
        try (HttpResponse httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl).timeout(3000).execute()) {
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                // 未正常返回，无需执行其它判断
                return;
            }
            // 4.校验文件类型
            String contentType = httpResponse.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                // 允许的图片类型
                final List<String> ALLOW_CONTENT_TYPE = List.of("image/jpeg", "image/png", "image/gif", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPE.contains(contentType), ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            // 5.校验文件大小
            String contentLengthStr = httpResponse.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                long contentLength = 0;
                try {
                    contentLength = Long.parseLong(contentLengthStr);
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件大小解析失败");
                }
                final long ONE_M = 1024 * 1024L;
                ThrowUtils.throwIf(contentLength > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
            }
        }
    }
}
