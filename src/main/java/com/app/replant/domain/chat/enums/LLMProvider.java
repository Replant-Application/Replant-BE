package com.app.replant.domain.chat.enums;

/**
 * LLM 제공자 열거형
 */
public enum LLMProvider {
    GEMINI("Google Gemini"),
    QWEN("Qwen (vLLM)"),
    AUTO("리앤트 자동 메시지");

    private final String displayName;

    LLMProvider(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
