package com.app.replant.domain.diary.repository;

import com.app.replant.domain.diary.entity.Diary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    @Query("SELECT d FROM Diary d WHERE d.user.id = :userId")
    Page<Diary> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT d FROM Diary d WHERE d.user.id = :userId AND d.date = :date")
    Optional<Diary> findByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT d FROM Diary d WHERE d.id = :diaryId AND d.user.id = :userId")
    Optional<Diary> findByIdAndUserId(@Param("diaryId") Long diaryId, @Param("userId") Long userId);

    boolean existsByUserIdAndDate(Long userId, LocalDate date);
}
