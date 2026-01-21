# ---------- Runtime only ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 타임존 설정 (KST)
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apk del tzdata

# 타임존, JVM 옵션
ENV TZ=Asia/Seoul
ENV JAVA_OPTS=""

# Host에서 미리 빌드된 JAR 복사
# (GitHub Actions에서 ./gradlew clean bootJar -x test 수행 후)
COPY build/libs/*.jar app.jar

# non-root 사용자 생성(보안)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
 && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
