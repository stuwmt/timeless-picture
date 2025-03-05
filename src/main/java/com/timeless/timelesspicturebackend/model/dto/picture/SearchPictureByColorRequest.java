package com.timeless.timelesspicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchPictureByColorRequest implements Serializable {
    /**
     * 空间 id
     */
    public Long spaceId;
    /**
     * 图片主色调
     */
    public String picColor;

    private static final long serialVersionUID = 1L;
}
