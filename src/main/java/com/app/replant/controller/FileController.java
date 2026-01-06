package com.app.replant.controller;

import com.app.replant.common.ApiResponse;
import com.app.replant.controller.dto.UploadedFileInfoDto;
import com.app.replant.service.s3FileService.S3FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "File", description = "파일 업로드/삭제 API")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "JWT Token")
public class FileController {

    private final S3FileService s3FileService;

    @Operation(summary = "파일 업로드", description = "이미지 파일을 S3에 업로드합니다")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UploadedFileInfoDto> uploadFile(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "업로드할 파일", required = true)
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        UploadedFileInfoDto uploadedFile = s3FileService.uploadImage(file);
        log.info("User {} uploaded file: {}", userId, uploadedFile.getFileName());

        return ApiResponse.success(uploadedFile);
    }

    @Operation(summary = "미션 인증 사진 업로드", description = "미션 인증 사진을 S3의 mission_verify 폴더에 업로드합니다")
    @PostMapping(value = "/upload/mission-verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UploadedFileInfoDto> uploadMissionVerifyImage(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "업로드할 파일", required = true)
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        UploadedFileInfoDto uploadedFile = s3FileService.uploadImageToFolder(file, "mission_verify");
        log.info("User {} uploaded mission verify image: {}", userId, uploadedFile.getFileName());

        return ApiResponse.success(uploadedFile);
    }

    @Operation(summary = "폴더에 파일 업로드", description = "이미지 파일을 지정된 폴더에 업로드합니다")
    @PostMapping(value = "/upload/{folder}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UploadedFileInfoDto> uploadFileToFolder(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "업로드할 폴더", required = true)
            @PathVariable String folder,
            @Parameter(description = "업로드할 파일", required = true)
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        UploadedFileInfoDto uploadedFile = s3FileService.uploadImageToFolder(file, folder);
        log.info("User {} uploaded file to folder {}: {}", userId, folder, uploadedFile.getFileName());

        return ApiResponse.success(uploadedFile);
    }

    @Operation(summary = "파일 삭제", description = "S3에서 파일을 삭제합니다")
    @DeleteMapping("/{fileName}")
    public ApiResponse<Map<String, String>> deleteFile(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "삭제할 파일 이름", required = true)
            @PathVariable String fileName) {

        boolean deleted = s3FileService.deleteImage(fileName);

        Map<String, String> result = new HashMap<>();
        if (deleted) {
            log.info("User {} deleted file: {}", userId, fileName);
            result.put("message", "파일이 삭제되었습니다.");
        } else {
            result.put("message", "파일 삭제에 실패했습니다.");
        }

        return ApiResponse.success(result);
    }
}
