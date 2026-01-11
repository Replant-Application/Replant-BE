package com.app.replant.domain.diary.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class DiaryStatsResponse {
    private long totalCount;
    private Map<String, Long> emotionStats;
}
