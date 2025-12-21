package com.app.replant.domain.verification.dto;

import com.app.replant.domain.verification.entity.VerificationVote;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VoteRequest {

    @NotNull(message = "투표는 필수입니다.")
    private VerificationVote.VoteType vote;
}
