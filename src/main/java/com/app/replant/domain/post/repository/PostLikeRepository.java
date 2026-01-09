package com.app.replant.domain.post.repository;

import com.app.replant.domain.post.entity.Post;
import com.app.replant.domain.post.entity.PostLike;
import com.app.replant.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByPostAndUser(Post post, User user);

    boolean existsByPostAndUser(Post post, User user);

    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.post.id = :postId")
    long countByPostId(@Param("postId") Long postId);

    void deleteByPostAndUser(Post post, User user);
}
