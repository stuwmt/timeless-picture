package com.timeless.timelesspicturebackend.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import cn.hutool.json.JSONUtil;
import com.timeless.timelesspicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.timeless.timelesspicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.timeless.timelesspicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.timeless.timelesspicturebackend.exception.BusinessException;
import com.timeless.timelesspicturebackend.exception.ErrorCode;
import com.timeless.timelesspicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunApi {
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    // 创建任务地址
    private static final String CREATE_TASK_URL = "https://dashscope.aliyuncs" +
            ".com/api/v1/services/aigc/image2image/out-painting";
    // 查询任务地址
    private static final String QUERY_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    // 创建任务
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        ThrowUtils.throwIf(createOutPaintingTaskRequest == null, ErrorCode.PARAMS_ERROR, "扩图参数不能为空");
        // 发送请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_TASK_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));
        // 处理响应
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("创建任务失败: {}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建任务失败");
            }
            CreateOutPaintingTaskResponse createOutPaintingTaskResponse = JSONUtil.toBean(httpResponse.body(),
                    CreateOutPaintingTaskResponse.class);
            String errorCode = createOutPaintingTaskResponse.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                String message = createOutPaintingTaskResponse.getMessage();
                log.error("AI扩图接口异常: {}", message);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI扩图接口异常");
            }
            return createOutPaintingTaskResponse;
        }
    }

    // 查询任务
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务 ID 不能为空");
        }
        // 发送请求
        HttpRequest httpRequest = HttpRequest.get(String.format(QUERY_TASK_URL, taskId))
                .header("Authorization", "Bearer " + apiKey);
        // 处理响应
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("查询任务失败: {}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "查询任务失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }
    }
}
