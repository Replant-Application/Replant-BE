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

        // NPE 방어: answerer null 체크 추가
        public static AnswerInfo from(MissionQnAAnswer answer) {
            Long answererId = answer.getAnswerer() != null ? answer.getAnswerer().getId() : null;
            String answererNickname = answer.getAnswerer() != null ? answer.getAnswerer().getNickname() : "알 수 없음";
            
            return AnswerInfo.builder()
                    .id(answer.getId())
                    .answererId(answererId)
                    .answererNickname(answererNickname)
                    .content(answer.getContent())
                    .isAccepted(answer.getIsAccepted())
                    .createdAt(answer.getCreatedAt())
                    .build();
        }
    }

    // NPE 방어: questioner null 체크 추가
    public static MissionQnAResponse from(MissionQnA qna) {
        Long questionerId = qna.getQuestioner() != null ? qna.getQuestioner().getId() : null;
        String questionerNickname = qna.getQuestioner() != null ? qna.getQuestioner().getNickname() : "알 수 없음";
        
        return MissionQnAResponse.builder()
                .id(qna.getId())
                .questionerId(questionerId)
                .questionerNickname(questionerNickname)
                .question(qna.getQuestion())
                .isResolved(qna.getIsResolved())
                .answerCount(qna.getAnswers() != null ? qna.getAnswers().size() : 0)
                .createdAt(qna.getCreatedAt())
                .build();
    }

    // NPE 방어: questioner null 체크 추가
    public static MissionQnAResponse fromWithAnswers(MissionQnA qna) {
        Long questionerId = qna.getQuestioner() != null ? qna.getQuestioner().getId() : null;
        String questionerNickname = qna.getQuestioner() != null ? qna.getQuestioner().getNickname() : "알 수 없음";
        
        List<AnswerInfo> answerInfos = qna.getAnswers() != null ?
                qna.getAnswers().stream().map(AnswerInfo::from).toList() : List.of();

        return MissionQnAResponse.builder()
                .id(qna.getId())
                .questionerId(questionerId)
                .questionerNickname(questionerNickname)
                .question(qna.getQuestion())
                .isResolved(qna.getIsResolved())
                .createdAt(qna.getCreatedAt())
                .answers(answerInfos)
                .build();
    }
}
