package com.app.replant.domain.reant.repository;

import com.app.replant.domain.reant.entity.Reant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReantRepository extends JpaRepository<Reant, Long>, ReantRepositoryCustom {
    // JPA 자동 생성 메서드 제거 - 순환 참조 방지
    // findByUser, findByUserId는 ReantRepositoryCustom의 findByUserIdWithUser를 사용하세요
}
