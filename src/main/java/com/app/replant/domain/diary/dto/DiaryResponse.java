package com.app.replant.domain.diary.dto;

import com.app.replant.domain.diary.entity.Diary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class DiaryResponse {

    private Long id;
    private LocalDate date;
    private String emotion;
    private String content;
    private String weather;
    private String location;
    private List<String> imageUrls;
    private Boolean isPrivate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static DiaryResponse from(Diary diary) {
        List<String> images = Collections.emptyList();
        if (diary.getImageUrls() != null && !diary.getImageUrls().isEmpty()) {
            try {
                images = objectMapper.readValue(diary.getImageUrls(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            } catch (JsonProcessingException e) {
                images = Collections.emptyList();
            }
        }

        return DiaryResponse.builder()
                .id(diary.getId())
                .date(diary.getDate())
                .emotion(diary.getEmotion())
                .content(diary.getContent())
                .weather(diary.getWeather())
                .location(diary.getLocation())
                .imageUrls(images)
                .isPrivate(diary.getIsPrivate())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }
}
