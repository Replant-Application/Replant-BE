package com.app.replant.domain.rag.enums;

public enum UserMemoryCategory {
    DIARY("diary"),
    MISSION("mission");

    private final String value;

    UserMemoryCategory(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
