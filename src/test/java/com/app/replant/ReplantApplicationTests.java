package com.app.replant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "FRONTEND_URL=http://localhost:8081",
    "GOOGLE_MAIL=test@test.com",
    "REPLANT_URL=http://localhost:8080",
    "firebase.config.path=firebase/replant-admin.json",
    "app.version.min=0.0.0",
    "app.version.latest=0.0.0",
    "app.version.store-url=https://play.google.com/store/apps/details?id=com.anonymous.replantmobileapp",
    "app.version.message="
})
class ReplantApplicationTests {

    @Test
    void contextLoads() {
    }

}
