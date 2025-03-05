package com.timeless.timelesspicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片标签分类
 */
@Data
public class PictureTagCategory implements Serializable {

    private static final long serialVersionUID = 6945463562525049429L;

    public List<String> tagList;
    public List<String> categoryList;
}
