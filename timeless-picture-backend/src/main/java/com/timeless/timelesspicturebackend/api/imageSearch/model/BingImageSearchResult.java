package com.timeless.timelesspicturebackend.api.imageSearch.model;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BingImageSearchResult {
    private String thumbnailUrl;
    private String contentUrl;
    private String hostPageUrl;

    public static List<BingImageSearchResult> extractUrls(String jsonStr) {
        List<BingImageSearchResult> resultList = new ArrayList<>();

        try {
            // 解析JSON
            JSONObject root = JSONUtil.parseObj(jsonStr);

            // 获取tags数组
            JSONArray tags = root.getJSONArray("tags");
            if (tags == null) return resultList;

            // 遍历tags数组寻找VisualSearch action
            for (int i = 0; i < tags.size(); i++) {
                JSONObject tag = tags.getJSONObject(i);
                JSONArray actions = tag.getJSONArray("actions");
                if (actions == null) continue;

                for (int j = 0; j < actions.size(); j++) {
                    JSONObject action = actions.getJSONObject(j);

                    // 只处理VisualSearch类型的action
                    if ("VisualSearch".equals(action.getStr("actionType"))) {
                        JSONObject data = action.getJSONObject("data");
                        if (data == null) continue;

                        JSONArray values = data.getJSONArray("value");
                        if (values == null) continue;

                        // 提取每个图片的URL信息
                        for (int k = 0; k < values.size(); k++) {
                            JSONObject value = values.getJSONObject(k);

                            BingImageSearchResult urls = new BingImageSearchResult();
                            urls.setThumbnailUrl(value.getStr("thumbnailUrl"));
                            urls.setContentUrl(value.getStr("contentUrl"));
                            urls.setHostPageUrl(value.getStr("hostPageUrl"));

                            resultList.add(urls);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("解析JSON时发生错误: " + e.getMessage());
        }

        return resultList;
    }

}
