package com.timeless.timelesspicturebackend.api.imageSearch;

import com.timeless.timelesspicturebackend.api.imageSearch.model.ImageSearchResult;
import com.timeless.timelesspicturebackend.api.imageSearch.sub.GetImageFirstUrlApi;
import com.timeless.timelesspicturebackend.api.imageSearch.sub.GetImageListApi;
import com.timeless.timelesspicturebackend.api.imageSearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImageSearchApiFacade {
    /**
     * 搜索图片
     * @param imageUrl 图片地址
     * @return 图片搜索结果列表
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String firstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        return GetImageListApi.getImageList(firstUrl);
    }
}
