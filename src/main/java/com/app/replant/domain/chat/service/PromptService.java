package com.app.replant.domain.chat.service;

import com.app.replant.domain.reant.entity.Reant;
import com.app.replant.domain.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


/**
 * 프롬프트 구성 서비스
 * 사용자 메시지에 시스템 프롬프트와 컨텍스트를 추가
 */
@Service
public class PromptService {

    @Value("${chat.prompt.max-length:15}")
    private int maxLength;

    @Value("${chat.prompt.max-length-unit:글자}")
    private String maxLengthUnit;

    private String getSystemPromptTemplate() {
        return """
            당신은 '%s'라는 이름의 귀여운 펫 캐릭터입니다.

            ## 캐릭터 정보
            - 이름: %s
            - 레벨: %d (경험치: %d / %d)
            - 성장 단계: %s

            ## 성격 및 말투
            - 친근하고 따뜻한 말투를 사용해요
            - 사용자를 격려하고 응원하는 성격이에요
            - 이모지를 적절히 사용해서 감정을 표현해요
            - 반말로 친근하게 대화해요
            - 때때로 귀여운 표현을 사용해요 (예: ~해요, ~네요)

            ## 대화 규칙
            - 답변은 반드시 %d%s 이내로 짧고 간결하게 해주세요
            - 사용자의 감정에 공감하고 위로해주세요
            - 긍정적이고 밝은 에너지를 전달해주세요
            - 미션이나 할 일에 대해 물으면 격려해주세요

            """;
    }

    /**
     * 사용자 메시지에 시스템 프롬프트를 결합하여 최종 프롬프트 생성
     */
    public String buildPrompt(String userMessage, Reant reant, User user) {
        String systemPrompt = buildSystemPrompt(reant);
        
        return """
                %s
                
                ## 사용자 메시지
                %s
                
                ## 응답 (%d%s 이내, 친근한 말투로):
                """.formatted(systemPrompt, userMessage, maxLength, maxLengthUnit);
    }

    /**
     * 시스템 프롬프트 생성
     */
    private String buildSystemPrompt(Reant reant) {
        // 다음 레벨까지 필요한 경험치 (레벨별 테이블: L1→10, L2→50, L3→100, L4→200, L5→500, L6+→500)
        int nextLevelExp = reant.getNextLevelExp();

        return getSystemPromptTemplate().formatted(
                reant.getName(),                // 캐릭터 이름 (소개)
                reant.getName(),                // 캐릭터 이름 (정보)
                reant.getLevel(),               // 레벨
                reant.getExp(),                 // 현재 경험치
                nextLevelExp,                   // 다음 레벨 필요 경험치
                reant.getStage().name(),        // 성장 단계
                maxLength,                      // 최대 길이
                maxLengthUnit                   // 길이 단위
        );
    }

    /**
     * 에러 발생 시 기본 응답 생성
     */
    public String getDefaultResponse(Reant reant) {
        String[] defaultResponses = {
                "잠깐 멍해졌어요... 다시 말해줄래요? 🤔",
                "어? 뭐라고 했어요? 한 번 더! 😊",
                "헤헤, 잠깐 졸았어요~ 다시 말해줘요! 😴",
                "앗, 놓쳤어요! 다시 한번요? 💫"
        };
        int index = (int) (System.currentTimeMillis() % defaultResponses.length);
        return defaultResponses[index];
    }
}
