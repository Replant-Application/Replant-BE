## 📌 Summary
<!-- PR 요약을 써주세요. -->
GitHub Actions CI/CD 파이프라인을 Java/Spring Boot 프로젝트에 맞게 수정하고, Docker 컨테이너 타임존을 KST로 설정했습니다.

## ✍️ Description
<!-- PR에 대한 자세한 설명을 써주세요. -->
- close: 

### CI/CD 파이프라인 개선
- **Node.js → Java/Gradle 전환**: 기존 Node.js 기반 워크플로우를 Java 17 + Gradle 기반으로 변경
- **테스트 Job 추가**: 보안 스캔 전에 테스트를 실행하여 빌드 실패를 조기에 감지
- **보안 스캔 업데이트**:
  - Snyk: Node.js 액션 → Gradle 액션으로 변경하여 Java 의존성 취약점 스캔
  - SonarCloud: Java 소스(`src`) 및 바이너리(`build/classes`) 스캔 설정 추가
- **Docker 빌드 프로세스 개선**: Gradle로 JAR 파일을 먼저 빌드한 후 Docker 이미지 생성
- **배포 포트 수정**: Spring Boot 기본 포트에 맞춰 `80:80` → `8080:8080`으로 변경
- **환경 변수 참조 수정**: GitHub Actions 구문 오류 해결

### Docker 타임존 설정
- **KST(한국 표준시) 설정**: Docker 컨테이너의 타임존을 `Asia/Seoul`로 설정
- Alpine Linux에서 `tzdata` 패키지를 사용하여 타임존 데이터 설치 및 설정

### 주요 변경사항
- `.github/workflows/gh-deploy.yml`: 전체 워크플로우를 Java/Gradle 기반으로 재작성
- `Dockerfile`: 타임존 설정 추가 및 KST 적용

## 💡 PR Point
<!-- 코드를 작성할 때 고민했던 부분을 적어주세요 -->

### CI/CD 파이프라인 구조
- **Job 의존성 체인**: `test` → `snyk_scan` → `sonarcloud_scan` → `docker_build_and_push` → `trivy_scan` → `deploy` → `zap_scan` 순서로 실행하여 각 단계에서 문제를 조기에 발견
- **테스트 우선 실행**: 보안 스캔 전에 테스트를 실행하여 기본적인 빌드 오류를 먼저 확인

### 환경 변수 처리
- **GitHub Actions 제약사항**: Job-level `env`에서 top-level `env` 변수를 직접 참조할 수 없어 `$GITHUB_ENV`를 사용하여 step-level에서 설정
- **이미지 참조 일관성**: 모든 Job에서 동일한 이미지 참조를 사용하도록 환경 변수 설정

### Docker 타임존 설정
- **Alpine Linux 특성**: Alpine은 기본적으로 타임존 데이터가 없어 `tzdata` 패키지 설치 필요
- **이미지 크기 최적화**: 타임존 설정 후 `tzdata` 패키지를 삭제하여 이미지 크기 최소화
- **환경 변수와 파일 설정 병행**: `ENV TZ=Asia/Seoul`과 타임존 파일 복사를 모두 수행하여 안정성 확보

### 포트 매핑 수정
- **Spring Boot 기본 포트**: 애플리케이션이 8080 포트에서 실행되므로 Docker 포트 매핑도 8080으로 변경
- **기존 80 포트 사용 시**: 리버스 프록시(nginx 등)를 통해 80 → 8080으로 포워딩하는 구조를 고려할 수 있으나, 현재는 직접 8080 포트 사용

## 📚 Reference 
<!-- 참고할 만한 자료가 있다면 링크나 시각 자료를 달아주세요. -->
- GitHub Actions: https://docs.github.com/en/actions
- Snyk for Gradle: https://docs.snyk.io/products/snyk-open-source/language-and-package-manager-support/snyk-for-java-gradle
- SonarCloud: https://sonarcloud.io/documentation
- Docker Timezone: https://en.wikipedia.org/wiki/List_of_tz_database_time_zones

## 🔥 Test
<!-- Test -->

### CI/CD 파이프라인 테스트
1. **로컬 빌드 확인**:
   ```bash
   ./gradlew clean build
   ./gradlew bootJar
   ```

2. **Docker 빌드 확인**:
   ```bash
   docker build -t test-image .
   docker run -p 8080:8080 test-image
   ```

3. **타임존 확인**:
   ```bash
   docker run test-image date
   # KST 시간이 표시되어야 함
   ```

4. **GitHub Actions 실행**:
   - `main` 브랜치에 push하여 전체 파이프라인 실행 확인
   - 각 Job이 순차적으로 성공하는지 확인
   - Docker 이미지가 정상적으로 빌드되고 푸시되는지 확인

### 배포 테스트
1. **EC2 배포 확인**:
   - 배포 후 컨테이너가 정상적으로 실행되는지 확인
   - `http://EC2_IP:8080`으로 접속하여 애플리케이션 응답 확인
   - 컨테이너 로그에서 타임존이 KST로 설정되었는지 확인

2. **ZAP 스캔 확인**:
   - 배포된 애플리케이션에 대한 보안 스캔이 정상적으로 실행되는지 확인
   - 스캔 리포트가 Artifact로 업로드되는지 확인

## 📝 PR Comment
<!-- Comment에 대한 주의 사항입니다. -->

```
P1: 꼭 반영해주세요 (Request changes)
리뷰어는 PR의 내용이 서비스에 중대한 오류를 발생할 수 있는 가능성을 잠재하고 있는 등 중대한 코드 수정이 반드시 필요하다고 판단되는 경우, P1 태그를 통해 리뷰 요청자에게 수정을 요청합니다.

P2: 적극적으로 고려해주세요 (Request changes)
작성자는 P2에 대해 수용하거나 만약 수용할 수 없는 상황이라면 적합한 의견을 들어 토론할 것을 권장합니다.

P3: 웬만하면 반영해 주세요 (Comment)
작성자는 P3에 대해 수용하거나 만약 수용할 수 없는 상황이라면 반영할 수 없는 이유를 들어 설명하거나 다음에 반영할 계획을 명시적으로(JIRA 티켓 등으로) 표현할 것을 권장합니다.

P4: 반영해도 좋고 넘어가도 좋습니다 (Approve)
작성자는 P4에 대해서는 아무런 의견을 달지 않고 무시해도 괜찮습니다. 해당 의견을 반영하는 게 좋을지 고민해 보는 정도면 충분합니다.

P5: 그냥 사소한 의견입니다 (Approve)
작성자는 P5에 대해 아무런 의견을 달지 않고 무시해도 괜찮습니다!
```
