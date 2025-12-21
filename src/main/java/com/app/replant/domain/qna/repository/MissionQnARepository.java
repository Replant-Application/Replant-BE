package com.app.replant.domain.qna.repository;

import com.app.replant.domain.qna.entity.MissionQnA;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MissionQnARepository extends JpaRepository<MissionQnA, Long> {

    @Query("SELECT mq FROM MissionQnA mq WHERE mq.mission.id = :missionId")
    Page<MissionQnA> findByMissionId(@Param("missionId") Long missionId, Pageable pageable);

    @Query("SELECT mq FROM MissionQnA mq LEFT JOIN FETCH mq.answers WHERE mq.id = :qnaId AND mq.mission.id = :missionId")
    Optional<MissionQnA> findByIdAndMissionIdWithAnswers(@Param("qnaId") Long qnaId, @Param("missionId") Long missionId);

    @Query("SELECT COUNT(mq) FROM MissionQnA mq WHERE mq.mission.id = :missionId")
    long countByMissionId(@Param("missionId") Long missionId);
}
