package com.timeless.timelesspicturebackend.api.imageSearch.BingSearchApi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.timeless.timelesspicturebackend.exception.BusinessException;
import com.timeless.timelesspicturebackend.exception.ErrorCode;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/**
 * 使用图片URL作为输入，不需要先将图片下载到本地。
 * 请求体构造为JSON格式，包含 imageInfo 以及 knowledgeRequest 对象。
 *
 * 请求JSON示例:
 * {
 *   "imageInfo": {
 *     "url": "https://example.com/path/to/image.jpg"
 *   },
 *   "knowledgeRequest": {
 *     "invokedSkills": ["SimilarImages"]
 *   }
 * }
 */
public class BingSearchImageResponse {

    // 替换为你的Bing API密钥
    private static final String subscriptionKey = "xxxxxxx";
    private static final String endpoint = "https://api.bing.microsoft.com/v7.0/images/visualsearch";
    private static final Integer MAX_RETRY = 10;
    private static final long RETRY_DELAY = 1000; // 重试间隔时间（毫秒）
    private static final int TIMEOUT = 30000; // 请求超时时间 ms

    /**
     * 根据传入的图片 URL 搜索相似图片。
     *
     * @param imageUrl 图片的公开 URL（必须使用 HTTPS）
     * @return 格式化后的 JSON 响应字符串
     */
    public static String execute(String imageUrl) {
        // 配置请求超时
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(TIMEOUT)
                .setConnectionRequestTimeout(TIMEOUT)
                .setSocketTimeout(TIMEOUT)
                .build();

        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .build()) {

            int retryCount = 0;

            while (retryCount < MAX_RETRY) {

                try {
                    // 构建 JSON 请求体
                    JsonObject imageInfo = new JsonObject();
                    imageInfo.addProperty("url", imageUrl);

                    JsonObject knowledgeRequest = new JsonObject();
                    knowledgeRequest.add("invokedSkills", new Gson().toJsonTree(new String[]{"SimilarImages"}));

                    JsonObject requestJson = new JsonObject();
                    requestJson.add("imageInfo", imageInfo);
                    requestJson.add("knowledgeRequest", knowledgeRequest);

                    String jsonBody = new GsonBuilder().create().toJson(requestJson);

                    // 创建 HTTP POST 请求，使用 application/json 作为 Content-Type
                    HttpPost httpPost = new HttpPost(endpoint);
                    httpPost.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);
                    httpPost.setHeader("Content-Type", "application/json");
                    httpPost.setEntity(new StringEntity(jsonBody));

                    System.out.println("发送搜索请求... 尝试次数: " + (retryCount + 1));
                    System.out.println("请求体: " + jsonBody);

                    // 执行请求
                    HttpResponse response = httpClient.execute(httpPost);
                    int statusCode = response.getStatusLine().getStatusCode();

                    // 读取响应内容
                    String responseContent;
                    try (InputStream stream = response.getEntity().getContent()) {
                        responseContent = new Scanner(stream).useDelimiter("\\A").next();
                    }

                    if (statusCode != 200) {
                        System.err.println("API请求失败，状态码: " + statusCode + "，响应: " + responseContent);
                        retryCount++;
                        if (retryCount < MAX_RETRY) {
                            Thread.sleep(RETRY_DELAY * retryCount);
                            continue;
                        }
                        throw new BusinessException(ErrorCode.OPERATION_ERROR,
                                String.format("API请求失败 (状态码: %d): %s", statusCode, responseContent));
                    }

                    String prettyJson = prettify(responseContent);
                    if (isEmptyResponse(prettyJson)) {
                        System.out.println("收到空响应，准备重试...");
                        retryCount++;
                        if (retryCount < MAX_RETRY) {
                            Thread.sleep(RETRY_DELAY * retryCount);
                            continue;
                        }
                    } else {
                        System.out.println("\nJSON Response:\n");
                        System.out.println(prettyJson);
                        return prettyJson;
                    }
                } catch (Exception e) {
                    System.err.println("请求失败，尝试重试... 错误: " + e.getMessage());
                    retryCount++;
                    if (retryCount < MAX_RETRY) {
                        Thread.sleep(RETRY_DELAY * retryCount);
                    } else {
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "达到最大重试次数: " + e.getMessage());
                    }
                }
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "达到最大重试次数，搜索失败");
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索操作失败: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "线程中断: " + e.getMessage());
        }
    }

    /**
     * 判断 JSON 响应是否为空或无效结果
     */
    private static boolean isEmptyResponse(String jsonResponse) {
        try {
            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();

            // 如果没有tags字段或者其数组为空，则视为空响应
            if (!json.has("tags") || json.getAsJsonArray("tags").size() == 0) {
                return true;
            }

            // 若返回结果仅包含基本结构而无实际内容，也认为是空响应
            return json.size() <= 4 &&
                    json.has("_type") &&
                    json.has("instrumentation") &&
                    json.has("debugInfo");
        } catch (Exception e) {
            System.err.println("解析JSON响应时出错: " + e.getMessage());
            return true;
        }
    }

    /**
     * 格式化 JSON 输出为漂亮的格式
     */
    public static String prettify(String jsonText) {
        try {
            JsonParser parser = new JsonParser();
            JsonObject json = (JsonObject) parser.parse(jsonText);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(json);
        } catch (Exception e) {
            System.err.println("JSON格式化失败: " + e.getMessage());
            return jsonText;
        }
    }
}