package com.app.replant.domain.reant.dto;

import com.app.replant.domain.reant.entity.Reant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InteractionResponse {
    private boolean success;
    private String message;
    private ReantStatusResponse reant;
    private Integer moodChange;
    private Integer healthChange;
    private Integer hungerChange;

    public static InteractionResponse of(Reant reant, String message, int moodChange, int healthChange, int hungerChange) {
        return InteractionResponse.builder()
                .success(true)
                .message(message)
                .reant(ReantStatusResponse.from(reant))
                .moodChange(moodChange)
                .healthChange(healthChange)
                .hungerChange(hungerChange)
                .build();
    }
}
