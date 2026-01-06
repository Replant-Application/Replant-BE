package com.app.replant.domain.diary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class DiaryRequest {

    @NotNull(message = "날짜를 입력해주세요")
    private LocalDate date;

    @NotBlank(message = "감정을 선택해주세요")
    @Size(max = 50, message = "감정은 50자 이하로 입력해주세요")
    private String emotion;

    @NotBlank(message = "내용을 입력해주세요")
    @Size(max = 1000, message = "내용은 1000자 이하로 입력해주세요")
    private String content;

    @Size(max = 50, message = "날씨는 50자 이하로 입력해주세요")
    private String weather;

    @Size(max = 100, message = "위치는 100자 이하로 입력해주세요")
    private String location;

    private List<String> imageUrls;

    private Boolean isPrivate;
}
