package com.app.replant.global.config;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

@TestConfiguration
public class TestVectorStoreConfig {

    @Bean
    @Primary
    public VectorStore vectorStore() {
        return (VectorStore) Proxy.newProxyInstance(
                VectorStore.class.getClassLoader(),
                new Class[]{VectorStore.class},
                (proxy, method, args) -> {
                    Class<?> returnType = method.getReturnType();
                    if (returnType.equals(List.class)) {
                        return List.of();
                    }
                    if (returnType.equals(Optional.class)) {
                        return Optional.of(Boolean.TRUE);
                    }
                    if (returnType.equals(boolean.class) || returnType.equals(Boolean.class)) {
                        return true;
                    }
                    return null;
                }
        );
    }
}
