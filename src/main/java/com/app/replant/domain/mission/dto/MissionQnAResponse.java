package com.app.replant.domain.mission.dto;

import com.app.replant.domain.qna.entity.MissionQnA;
import com.app.replant.domain.qna.entity.MissionQnAAnswer;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MissionQnAResponse {
    private Long id;
    private Long questionerId;
    private String questionerNickname;
    private String question;
    private Boolean isResolved;
    private Integer answerCount;
    private LocalDateTime createdAt;
    private List<AnswerInfo> answers;

    @Getter
    @Builder
    public static class AnswerInfo {
        private Long id;
        private Long answererId;
        private String answererNickname;
        private String content;
        private Boolean isAccepted;
        private LocalDateTime createdAt;

        public static AnswerInfo from(MissionQnAAnswer answer) {
            return AnswerInfo.builder()
                    .id(answer.getId())
                    .answererId(answer.getAnswerer().getId())
                    .answererNickname(answer.getAnswerer().getNickname())
                    .content(answer.getContent())
                    .isAccepted(answer.getIsAccepted())
                    .createdAt(answer.getCreatedAt())
                    .build();
        }
    }

    public static MissionQnAResponse from(MissionQnA qna) {
        return MissionQnAResponse.builder()
                .id(qna.getId())
                .questionerId(qna.getQuestioner().getId())
                .questionerNickname(qna.getQuestioner().getNickname())
                .question(qna.getQuestion())
                .isResolved(qna.getIsResolved())
                .answerCount(qna.getAnswers() != null ? qna.getAnswers().size() : 0)
                .createdAt(qna.getCreatedAt())
                .build();
    }

    public static MissionQnAResponse fromWithAnswers(MissionQnA qna) {
        List<AnswerInfo> answerInfos = qna.getAnswers() != null ?
                qna.getAnswers().stream().map(AnswerInfo::from).toList() : List.of();

        return MissionQnAResponse.builder()
                .id(qna.getId())
                .questionerId(qna.getQuestioner().getId())
                .questionerNickname(qna.getQuestioner().getNickname())
                .question(qna.getQuestion())
                .isResolved(qna.getIsResolved())
                .createdAt(qna.getCreatedAt())
                .answers(answerInfos)
                .build();
    }
}
