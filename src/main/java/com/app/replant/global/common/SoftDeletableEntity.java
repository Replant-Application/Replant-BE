package com.app.replant.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Soft Delete 지원 기본 엔티티
 * 모든 DELETE 요청은 실제 삭제 대신 deletedAt 필드를 설정
 */
@Getter
@MappedSuperclass
public abstract class SoftDeletableEntity extends BaseEntity {

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Soft Delete 처리
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 삭제 복구
     */
    public void restore() {
        this.deletedAt = null;
    }

    /**
     * 삭제 여부 확인
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
