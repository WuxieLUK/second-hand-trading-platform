package com.itheima.controller;

import org.example.common.Result;
import com.itheima.service.impl.ImageUploadService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FileUploadController {

    @Resource
    private ImageUploadService imageUploadService;

    @PostMapping("/upload")
    public Result uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            ImageUploadService.ImageUploadResponse response = imageUploadService.upload(file);
            return Result.success(response);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("图片上传失败");
        }
    }
}