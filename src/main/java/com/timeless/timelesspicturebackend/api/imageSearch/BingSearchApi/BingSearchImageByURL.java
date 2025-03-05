package com.timeless.timelesspicturebackend.api.imageSearch.BingSearchApi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

public class BingSearchImageByURL {

    private static final String subscriptionKey = "xxxxxxxx";
    private static final String endpoint = "https://api.bing.microsoft.com/v7.0/images/visualsearch";
    private static final Integer MAX_RETRY = 10;
    private static final long RETRY_DELAY = 1000;
    private static final int TIMEOUT = 30000;

    public static String execute(String imageUrl) {
        int retryCount = 0;
        while (retryCount < MAX_RETRY) {
            CloseableHttpClient httpClient = null;
            InputStream imageStream = null;
            try {
                httpClient = HttpClientBuilder.create()
                        .setDefaultRequestConfig(RequestConfig.custom()
                                .setConnectTimeout(TIMEOUT)
                                .setConnectionRequestTimeout(TIMEOUT)
                                .setSocketTimeout(TIMEOUT)
                                .build())
                        .build();

                if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                    URL url = new URL(imageUrl);
                    imageStream = url.openStream();
                } else {
                    imageStream = new FileInputStream(new File(imageUrl));
                }

                HttpEntity entity = MultipartEntityBuilder.create()
                        .addBinaryBody("image", imageStream, ContentType.DEFAULT_BINARY, "image.jpg")
                        .build();

                HttpPost httpPost = new HttpPost(endpoint);
                httpPost.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);
                httpPost.setEntity(entity);

                HttpResponse response = httpClient.execute(httpPost);
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode != 200) {
                    if (isRetryable(statusCode)) {
                        retryCount++;
                        closeResources(imageStream, httpClient);
                        Thread.sleep(RETRY_DELAY);
                        continue;
                    } else {
                        throw new RuntimeException("HTTP request failed with status code: " + statusCode);
                    }
                }

                HttpEntity responseEntity = response.getEntity();
                if (responseEntity == null) {
                    retryCount++;
                    closeResources(imageStream, httpClient);
                    Thread.sleep(RETRY_DELAY);
                    continue;
                }

                InputStream stream = responseEntity.getContent();
                String json = new Scanner(stream).useDelimiter("\\A").next();

                if (json == null || json.trim().isEmpty()) {
                    retryCount++;
                    closeResources(imageStream, httpClient);
                    Thread.sleep(RETRY_DELAY);
                    continue;
                }
                // System.out.println(prettify(json));
                return prettify(json);
            } catch (IOException e) {
                retryCount++;
                closeResources(imageStream, httpClient);
                if (retryCount >= MAX_RETRY) {
                    e.printStackTrace();
                    break;
                }
                try {
                    Thread.sleep(RETRY_DELAY);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            } finally {
                closeResources(imageStream, httpClient);
            }
        }
        return null;
    }

    private static boolean isRetryable(int statusCode) {
        return statusCode == 429 || (statusCode >= 500 && statusCode < 600);
    }

    private static void closeResources(InputStream imageStream, CloseableHttpClient httpClient) {
        try {
            if (imageStream != null) {
                imageStream.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            // Ignore
        }
    }

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