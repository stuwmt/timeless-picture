package com.timeless.timelesspicturebackend.api.imageSearch;

import com.timeless.timelesspicturebackend.api.imageSearch.BingSearchApi.BingSearchImageByURL;
import com.timeless.timelesspicturebackend.api.imageSearch.model.BingImageSearchResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class BingImageSearchApiFacade {

    public static List<BingImageSearchResult> searchImage(String imageUrl) {
        String execute = BingSearchImageByURL.execute(imageUrl);
        return BingImageSearchResult.extractUrls(execute);
    }
}
