# ---------- Runtime only ----------
    FROM eclipse-temurin:17-jre-alpine
    WORKDIR /app
    
    # 시스템 패키지 업데이트 및 타임존 설정 (KST)
    # libexpat 취약점(CVE-2026-24515) 해결을 위해 모든 패키지 업그레이드
    RUN apk update && \
        apk upgrade --available && \
        apk add --no-cache tzdata && \
        cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
        echo "Asia/Seoul" > /etc/timezone && \
        apk del tzdata && \
        rm -rf /var/cache/apk/*
    
    # 타임존, JVM 옵션
    ENV TZ=Asia/Seoul
    ENV JAVA_OPTS=""
    
    # Host에서 미리 빌드된 JAR 복사 (bootJar + plain 둘 다 있으면 디렉터리로 복사)
    # (GitHub Actions에서 ./gradlew clean bootJar -x test 수행 후)
    COPY build/libs/*.jar /app/
    
    # non-root 사용자 생성(보안)
    RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
     && chown -R appuser:appgroup /app
    
    USER appuser
    
    EXPOSE 8080
    
    # -plain.jar 제외한 실행 가능 JAR(boot JAR) 실행
    ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar $(ls /app/*.jar | grep -v plain | head -1)"]