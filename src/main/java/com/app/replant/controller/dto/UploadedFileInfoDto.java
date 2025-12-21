package com.app.replant.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "업로드된 파일 정보 DTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedFileInfoDto {

    @Schema(description = "파일 이름", example = "image.jpg")
    private String fileName;

    @Schema(description = "파일 URL", example = "https://...")
    private String fileUrl;

    @Schema(description = "파일 크기 (bytes)", example = "102400")
    private Long fileSize;

    @Schema(description = "파일 타입", example = "image/jpeg")
    private String contentType;
}
