package com.app.replant.domain.missionset.repository;

import com.app.replant.domain.missionset.entity.MissionSet;
import com.app.replant.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MissionSetRepository extends JpaRepository<MissionSet, Long> {

    // 내가 만든 미션세트 목록
    List<MissionSet> findByCreatorAndIsActiveOrderByCreatedAtDesc(User creator, Boolean isActive);

    // 내가 만든 미션세트 목록 (페이징)
    Page<MissionSet> findByCreatorAndIsActive(User creator, Boolean isActive, Pageable pageable);

    // 공개된 미션세트 목록 (담은수 + 평점 순 정렬)
    @Query("SELECT ms FROM MissionSet ms WHERE ms.isPublic = true AND ms.isActive = true " +
           "ORDER BY ms.addedCount DESC, ms.averageRating DESC, ms.createdAt DESC")
    Page<MissionSet> findPublicMissionSetsOrderByPopularity(Pageable pageable);

    // 공개된 미션세트 검색
    @Query("SELECT ms FROM MissionSet ms WHERE ms.isPublic = true AND ms.isActive = true " +
           "AND (ms.title LIKE %:keyword% OR ms.description LIKE %:keyword%) " +
           "ORDER BY ms.addedCount DESC, ms.averageRating DESC")
    Page<MissionSet> searchPublicMissionSets(@Param("keyword") String keyword, Pageable pageable);

    // 특정 사용자의 공개 미션세트
    Page<MissionSet> findByCreatorAndIsPublicAndIsActive(User creator, Boolean isPublic, Boolean isActive, Pageable pageable);

    // 미션세트 상세 조회 (미션 목록 포함)
    @Query("SELECT DISTINCT ms FROM MissionSet ms " +
           "LEFT JOIN FETCH ms.missions msm " +
           "LEFT JOIN FETCH msm.mission " +
           "WHERE ms.id = :id AND ms.isActive = true")
    Optional<MissionSet> findByIdWithMissions(@Param("id") Long id);
}
