package com.app.replant.domain.version.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 버전 체크 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionCheckRequest {
    @NotBlank(message = "현재 앱 버전은 필수입니다.")
    private String currentVersion;
}
