package com.app.replant.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.api.GoogleGenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Google GenAI (Gemini) 설정
 * API Key만 사용하여 Gemini Developer API로 연결
 * Spring AI 자동 설정을 비활성화하고 수동으로 Bean 생성
 */
@Slf4j
@Configuration
public class GoogleGenAiConfig {

    @Value("${env.GEMINI_API_KEY}")
    private String apiKey;

    @Value("${env.GEMINI_MODEL:gemini-3-flash-preview}")
    private String model;

    @Value("${env.GEMINI_TEMPERATURE:0.7}")
    private Double temperature;

    @Bean
    public GoogleGenAiChatModel googleGenAiChatModel() {
        log.info("Google GenAI ChatModel 생성 (API Key 모드) - Model: {}, Temperature: {}", model, temperature);
        
        // API Key만으로 GoogleGenAiApi 생성
        GoogleGenAiApi api = new GoogleGenAiApi(apiKey);
        
        // GoogleGenAiChatModel 생성
        GoogleGenAiChatModel chatModel = new GoogleGenAiChatModel(
            api,
            GoogleGenAiApi.ChatOptions.builder()
                .withModel(model)
                .withTemperature(temperature.floatValue())
                .build()
        );
        
        return chatModel;
    }
}
