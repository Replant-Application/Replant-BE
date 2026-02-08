package com.app.replant.global.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UuidV5Test {

    @Test
    void generatesDeterministicUuidFromCategoryAndOrigin() {
        assertThat(UuidV5.fromCategoryAndOrigin("diary", 101L).toString())
                .isEqualTo("3c30e347-8935-537d-ac17-380ece99f225");
        assertThat(UuidV5.fromCategoryAndOrigin("mission", 55L).toString())
                .isEqualTo("aed05958-d163-5e17-9261-7dab662bd159");
    }
}
