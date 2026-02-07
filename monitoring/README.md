# Replant Backend 모니터링 설정

Prometheus와 Grafana를 사용한 모니터링 시스템입니다.

## 사전 요구사항

1. **Spring Boot Actuator Prometheus 의존성 추가**

   `build.gradle`에 다음 의존성을 추가하세요:

   ```gradle
   dependencies {
       // Prometheus 메트릭
       implementation 'io.micrometer:micrometer-registry-prometheus'
   }
   ```

2. **애플리케이션 설정**

   `application.yml`에 Prometheus 엔드포인트를 추가하세요:

   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,info,metrics,prometheus
       base-path: /actuator
     metrics:
       export:
         prometheus:
           enabled: true
   ```

## 실행 방법

### 1. Docker Compose로 모니터링 시스템 시작

```bash
docker-compose -f docker-compose.monitoring.yml up -d
```

### 2. 접속

- **Grafana**: http://localhost:3000
  - 기본 계정: `admin` / `admin`
  - 첫 로그인 시 비밀번호 변경 요청

- **Prometheus**: http://localhost:9090

### 3. 애플리케이션 연결

애플리케이션이 호스트에서 실행 중인 경우:
- `prometheus.yml`의 `host.docker.internal:8080`이 기본값입니다.
- 애플리케이션이 다른 포트에서 실행 중이면 포트를 변경하세요.

애플리케이션이 Docker 컨테이너에서 실행 중인 경우:
- `prometheus.yml`의 `targets`를 컨테이너 이름이나 네트워크 IP로 변경하세요.
- 예: `replant-backend:8080` (같은 Docker 네트워크에 있는 경우)

## Grafana 대시보드

### 기본 대시보드

Grafana에 로그인한 후:
1. 좌측 메뉴에서 "Dashboards" → "Import" 클릭
2. Dashboard ID `4701` (JVM Micrometer) 또는 `11378` (Spring Boot 2.1 Statistics) 입력
3. Prometheus 데이터소스 선택 후 Import

### 커스텀 대시보드

`monitoring/grafana/dashboards/` 디렉토리에 JSON 파일을 추가하면 자동으로 로드됩니다.

## 중지

```bash
docker-compose -f docker-compose.monitoring.yml down
```

데이터를 유지하려면:

```bash
docker-compose -f docker-compose.monitoring.yml down
```

데이터를 삭제하려면:

```bash
docker-compose -f docker-compose.monitoring.yml down -v
```

## 네트워크 설정

애플리케이션이 Docker 컨테이너에서 실행 중인 경우, 같은 네트워크에 연결하세요:

```bash
# 애플리케이션 컨테이너를 monitoring 네트워크에 연결
docker network connect monitoring replant-backend
```

또는 `docker-compose.monitoring.yml`의 `networks` 섹션을 수정하여 기존 네트워크를 사용할 수 있습니다.

## 보안 설정

프로덕션 환경에서는:
1. Grafana 기본 비밀번호 변경
2. Prometheus와 Grafana에 인증 추가
3. 방화벽 규칙 설정
4. HTTPS 사용 고려
