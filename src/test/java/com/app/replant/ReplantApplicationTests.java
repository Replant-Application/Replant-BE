package com.app.replant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.app.replant.global.config.TestVectorStoreConfig;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import com.app.replant.domain.rag.service.UserMemoryVectorService;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestVectorStoreConfig.class)
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

    @MockBean
    private UserMemoryVectorService userMemoryVectorService;

    @Test
    void contextLoads() {
    }

}
