package com.app.replant.domain.qna.repository;

import com.app.replant.domain.qna.entity.MissionQnAAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MissionQnAAnswerRepository extends JpaRepository<MissionQnAAnswer, Long> {

    @Query("SELECT ma FROM MissionQnAAnswer ma WHERE ma.id = :answerId AND ma.qna.id = :qnaId")
    Optional<MissionQnAAnswer> findByIdAndQnaId(@Param("answerId") Long answerId, @Param("qnaId") Long qnaId);
}
