package com.app.replant.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * API 응답 래퍼 클래스
 * 
 * @author : lee
 * @since : 2/21/24
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponseWrapper<T> {
    
    /**
     * 응답 결과 데이터
     */
    private T result;
    
    /**
     * 결과 코드 (상태 코드)
     */
    private String resultCode;
    
    /**
     * 결과 메시지
     */
    private String resultMsg;
}
