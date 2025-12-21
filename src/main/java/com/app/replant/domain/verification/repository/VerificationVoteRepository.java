package com.app.replant.domain.verification.repository;

import com.app.replant.domain.verification.entity.VerificationVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VerificationVoteRepository extends JpaRepository<VerificationVote, Long> {

    boolean existsByVerificationPostIdAndVoterId(Long verificationPostId, Long voterId);

    @Query("SELECT vv FROM VerificationVote vv WHERE vv.verificationPost.id = :verificationId AND vv.voter.id = :voterId")
    Optional<VerificationVote> findByVerificationIdAndVoterId(@Param("verificationId") Long verificationId, @Param("voterId") Long voterId);

    @Query("SELECT vv FROM VerificationVote vv WHERE vv.verificationPost.id = :verificationPostId AND vv.voter.id = :voterId")
    Optional<VerificationVote> findByVerificationPostIdAndVoterId(@Param("verificationPostId") Long verificationPostId, @Param("voterId") Long voterId);
}
