package com.timeless.timelesspicturebackend.controller;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.timeless.timelesspicturebackend.annotation.AuthCheck;
import com.timeless.timelesspicturebackend.common.BaseResponse;
import com.timeless.timelesspicturebackend.common.ResultUtils;
import com.timeless.timelesspicturebackend.constant.UserConstant;
import com.timeless.timelesspicturebackend.exception.BusinessException;
import com.timeless.timelesspicturebackend.exception.ErrorCode;
import com.timeless.timelesspicturebackend.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {
    @Resource
    private CosManager cosManager;

    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> upload(@RequestPart("file") MultipartFile file) {
        //文件目录
        String fileName = file.getOriginalFilename();
        String filePath = String.format("/test/%s", fileName);
        //上传文件
        File upFile = null;
        try {
            upFile =  File.createTempFile(filePath, null);
            file.transferTo(upFile); //转存文件
            cosManager.putObject(filePath, upFile);
            //返回文件路径
            return ResultUtils.success(filePath);
        } catch (Exception e) {
            log.error("上传文件失败,path" + filePath, e);
            throw  new BusinessException(ErrorCode.SYSTEM_ERROR,"上传文件失败");
        }finally {
            if (upFile != null) {
                boolean delete = upFile.delete();
                if (!delete) {
                    log.error("删除临时文件失败,path" + filePath);
                }
            }
        }
    }

    /**
     * 下载文件
     * @param filePath 文件路径
     * @param response 响应
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download")
    public void download(String filePath, HttpServletResponse response) {
        COSObjectInputStream cosObjectInputStream = null;
        try {
            COSObject cosObject = cosManager.getObject(filePath);
             cosObjectInputStream = cosObject.getObjectContent();
            //处理下载到的流
            byte[] bytes = IOUtils.toByteArray(cosObjectInputStream);
            //设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + filePath);
            //写出
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("下载文件失败,path" + filePath, e);
            throw  new BusinessException(ErrorCode.SYSTEM_ERROR,"下载文件失败");
        }finally {
            if (cosObjectInputStream != null) {
                try {
                    cosObjectInputStream.close();
                } catch (IOException e) {
                    log.error("关闭cosObjectInputStream失败", e);
                }
            }
        }
    }
}
