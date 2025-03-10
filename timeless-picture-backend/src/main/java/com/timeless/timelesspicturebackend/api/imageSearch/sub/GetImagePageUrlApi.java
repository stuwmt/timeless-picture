package com.timeless.timelesspicturebackend.api.imageSearch.sub;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.*;
import cn.hutool.json.JSONUtil;
import com.timeless.timelesspicturebackend.exception.BusinessException;
import com.timeless.timelesspicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取图片页面地址(step 1)
 */
@Slf4j
public class GetImagePageUrlApi {
    /**
     * 获取图片页面地址
     *
     * @param imageUrl 图片地址
     * @return 图片页面地址
     */
    public static String getImagePageUrl(String imageUrl) {
        // 1.构造请求
       /*  image: https%3A%2F%2Fwww.codefather.cn%2Flogo.png
        tn: pc
        from: pc
        image_source: PC_UPLOAD_URL
        sdkParams: undefined */
        Map<String, Object> formData = Map.of(
                "image", imageUrl,
                "tn", "pc",
                "from", "pc",
                "image_source", "PC_UPLOAD_URL"
        );
        // 获取当前时间戳
        long timestamp = System.currentTimeMillis();
        // 请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + timestamp;
        try {
            // 2.发送请求到百度接口
            HttpResponse httpResponse = HttpRequest.post(url)
                    .form(formData)
                    .timeout(5000)
                    .execute();
            // 判断响应状态码
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            // 3.解析响应
            String body = httpResponse.body();
        /* {
    "status": 0,
    "msg": "Success",
    "data": {
        "url": "https://graph.baidu.com/s?card_key=&entrance=GENERAL&extUiData%5BisLogoShow%5D=1&f=all&isLogoShow=1
        &session_id=4138484284919300736&sign=12665e97cd54acd88139901737896617&tpl_from=pc",
        "sign": "12665e97cd54acd88139901737896617"
    }
} */
            Map<String, Object> resMap = JSONUtil.toBean(body, Map.class);
            if (resMap == null || !Integer.valueOf(0).equals(resMap.get("status"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            // 4.返回结果
            Map<String, Object> data = (Map<String, Object>) resMap.get("data");
            String rawUrl = (String) data.get("url");
            String imageResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            if (imageResultUrl == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索Url结果失败");
            }
            return imageResultUrl;
        } catch (Exception e) {
            log.error("获取图片页面地址失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片页面地址失败");
        }
    }
}

