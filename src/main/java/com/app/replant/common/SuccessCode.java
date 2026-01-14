package com.app.replant.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 성공 응답 코드 Enum
 * 
 * @author : lee
 * @since : 2/21/24
 */
@Getter
@AllArgsConstructor
public enum SuccessCode {
    
    /**
     * 조회 성공
     */
    SELECT_SUCCESS("200", "조회 성공"),
    
    /**
     * 생성 성공
     */
    INSERT_SUCCESS("201", "생성 성공"),
    
    /**
     * 수정 성공
     */
    UPDATE_SUCCESS("200", "수정 성공"),
    
    /**
     * 삭제 성공
     */
    DELETE_SUCCESS("200", "삭제 성공"),
    
    /**
     * 전송 성공
     */
    SEND_SUCCESS("200", "전송 성공");
    
    /**
     * 상태 코드
     */
    private final String status;
    
    /**
     * 메시지
     */
    private final String message;
}
