package com.upc.face.controller;

import com.upc.common.result.Result;
import com.upc.face.service.FaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/face")
@RequiredArgsConstructor
@Tag(name = "人脸识别", description = "人脸特征提取、人脸比对接口")
public class FaceController {

    private final FaceService faceService;

    @Operation(summary = "提取人脸特征")
    @PostMapping("/extract")
    public Result<String> extractFaceFeature(@RequestParam("file") MultipartFile file) {
        String feature = faceService.extractFaceFeature(file);
        if (feature != null) {
            return Result.success(feature);
        } else {
            return Result.fail(500, "人脸特征提取失败");
        }
    }

    @Operation(summary = "人脸比对")
    @PostMapping("/compare")
    public Result<Double> compareFace(
            @RequestParam("file1") MultipartFile file1,
            @RequestParam("file2") MultipartFile file2) {
        Double similarity = faceService.compareFace(file1, file2);
        if (similarity != null) {
            return Result.success(similarity);
        } else {
            return Result.fail(500, "人脸比对失败");
        }
    }

    @Operation(summary = "人脸登录验证")
    @PostMapping("/verify/{userId}")
    public Result<Boolean> verifyFace(
            @PathVariable Integer userId,
            @RequestParam("file") MultipartFile file) {
        Boolean verified = faceService.verifyFace(userId, file);
        if (verified != null && verified) {
            return Result.success(true);
        } else {
            return Result.fail(401, "人脸验证失败");
        }
    }

    @Operation(summary = "检测人脸")
    @PostMapping("/detect")
    public Result<Boolean> detectFace(@RequestParam("file") MultipartFile file) {
        Boolean detected = faceService.detectFace(file);
        if (detected != null && detected) {
            return Result.success(true);
        } else {
            return Result.fail(400, "未检测到人脸");
        }
    }
}
