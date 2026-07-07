package com.scaffold.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.scaffold.common.result.Result;
import com.scaffold.modules.system.service.MinioStorageService;
import com.scaffold.modules.system.vo.FileUploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文件上传")
@RestController
@RequestMapping("/admin/file")
@RequiredArgsConstructor
public class SysFileController {

    private final MinioStorageService minioStorageService;

    @Operation(summary = "上传菜品图片")
    @SaCheckPermission("dish:list")
    @PostMapping(value = "/upload/dish-image", consumes = "multipart/form-data")
    public Result<FileUploadVO> uploadDishImage(@RequestPart("file") MultipartFile file) {
        return Result.success(minioStorageService.uploadDishImage(file));
    }

    /**
     * 上传首页轮播图图片
     *
     * @param file 图片文件
     * @return 上传结果
     * @author Henfon
     * @date 2026-06-26
     * @description 提供给轮播图管理页面上传图片使用
     */
    @Operation(summary = "上传首页轮播图图片")
    @SaCheckPermission("banner:list")
    @PostMapping(value = "/upload/banner-image", consumes = "multipart/form-data")
    public Result<FileUploadVO> uploadBannerImage(@RequestPart("file") MultipartFile file) {
        return Result.success(minioStorageService.uploadDishImage(file));
    }
}
