package com.app.replant.domain.post.repository;

import com.app.replant.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p WHERE " +
           "(:missionId IS NULL OR p.mission.id = :missionId) " +
           "AND (:customMissionId IS NULL OR p.customMission.id = :customMissionId) " +
           "AND (:badgeOnly = false OR p.hasValidBadge = true)")
    Page<Post> findWithFilters(
        @Param("missionId") Long missionId,
        @Param("customMissionId") Long customMissionId,
        @Param("badgeOnly") boolean badgeOnly,
        Pageable pageable
    );

    @Query("SELECT p FROM Post p WHERE p.id = :postId AND p.user.id = :userId")
    Optional<Post> findByIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);
}
