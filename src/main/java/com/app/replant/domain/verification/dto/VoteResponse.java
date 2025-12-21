package com.app.replant.domain.verification.dto;

import com.app.replant.domain.verification.entity.VerificationVote;
import com.app.replant.domain.verification.enums.VerificationStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VoteResponse {

    private Long verificationId;
    private VerificationVote.VoteType vote;
    private Integer approveCount;
    private Integer rejectCount;
    private VerificationStatus status;
    private String message;
}
