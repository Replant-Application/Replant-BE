package com.app.replant.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이지네이션 응답 DTO
 *
 * Spring Data의 Page 객체를 표준화된 형식으로 변환합니다.
 *
 * @param <T> 컨텐츠 타입
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /**
     * 페이지 컨텐츠
     */
    private List<T> content;

    /**
     * 페이지 메타데이터
     */
    private PageMetadata metadata;

    /**
     * Spring Data Page 객체로부터 PageResponse 생성
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .metadata(PageMetadata.builder()
                        .page(page.getNumber())
                        .size(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }

    /**
     * 페이지 메타데이터
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageMetadata {
        /**
         * 현재 페이지 번호 (0부터 시작)
         */
        private int page;

        /**
         * 페이지 크기
         */
        private int size;

        /**
         * 전체 요소 개수
         */
        private long totalElements;

        /**
         * 전체 페이지 개수
         */
        private int totalPages;
    }
}
