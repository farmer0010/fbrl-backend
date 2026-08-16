# DEPLOYMENT

이 문서(특히 아래 표)는 배포 담당자에게 그대로 전달 가능한 계약입니다. 여기 없는 값은 애플리케이션 기본값(로컬 개발 기준)이 그대로 쓰인다는 뜻이며, 프로덕션에서 그게 맞는지는 이 표를 기준으로 판단하면 됩니다.

배포 대상 클라우드(Azure)의 구체적인 서비스 구성은 인프라 팀과 아직 미정이지만, 애플리케이션 쪽은 어떤 배포 방식이든 **환경변수 주입만으로 동작**하도록 맞춰져 있습니다. 이 문서는 배포 준비도 감사(설정 외부화 전수 조사, loud/silent 판정 기준 명시본)의 카테고리 1-4("체크리스트 문서 부재")를 채우기 위해 작성됐습니다.

## 환경변수 표기 규칙 확인

Spring Boot의 환경변수 relaxed binding은 `.`과 `-` 둘 다 단어 경계로 보고 `_`로 치환합니다. 예: `k8s.leader-election.namespace` → `K8S_LEADER_ELECTION_NAMESPACE`. 이 문서 작성 중 이 매핑 규칙을 실제로 스파이크 테스트로 검증했습니다(`K8S_LEADER_ELECTION_NAMESPACE`, `K8S_LEADERELECTION_NAMESPACE` 둘 다 정상 매핑되는 것까지 확인 — Boot가 두 형태 모두 별칭으로 인식). 아래 표는 Spring 공식 문서가 권장하는 표준 표기(하이픈→언더스코어)를 사용합니다.

## "loud" / "silent" 판정 기준

배포 준비도 감사 2차본에서 정의한 기준을 그대로 씁니다 — 이 값을 프로덕션에서 안 건드리고 배포했을 때:

- **loud**: 앱이 기동에 실패해 그 자리에서 드러남(배포가 막힘, 알아채기 쉬움)
- **silent**: 앱은 정상 기동하고, 잘못된 동작이 조용히 누적됨(알아채기 어려움 — 더 위험)
- **silent-breach**: 앱은 정상 기동하고 겉보기엔 정상 동작하지만, 보안 경계 자체가 뚫려 있는 상태 — silent보다 한 단계 더 위험함. silent는 "잘못된 결과가 쌓이는 것"이지만 silent-breach는 "인증/인가 자체가 무력화된 채로 정상처럼 보이는 것"이라, 로그나 모니터링에 이상 신호가 아예 안 남을 수 있음(침해가 일어나도 알아챌 단서 자체가 없음)
- **N/A**: 값이 틀려도 앱 동작 자체엔 영향 없음(업무 정책값이거나, 조건부로 비활성화된 기능)

## 필수 환경변수 체크리스트

| 환경변수명 | 대응 Spring 프로퍼티 | 기본값(로컬) | 필수 여부(프로덕션) | 실패 시 동작 | 설명 |
|---|---|---|---|---|---|
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | `jdbc:postgresql://localhost:5432/fbrl_db` | **필수** | loud | HikariCP가 기동 시 커넥션 풀 초기화 과정에서 실제 연결을 시도 — 실패하면 애플리케이션 컨텍스트 자체가 뜨지 않음 |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` | `fbrl_user` | **필수** | loud | 상동 |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` | `fbrlpassword` | **필수** | loud | 로컬 기본값은 docker-compose 시드값과 동일한 더미 — 프로덕션에서 반드시 실제 값으로 교체. 안 바꾸면 인증 실패로 기동 자체가 안 됨(loud) |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `spring.jpa.hibernate.ddl-auto` | `validate`(2026-08-16 이번 변경으로 기본값 자체가 안전해짐) | 프로덕션 필수 아님 | (이번 수정 전) silent → (이번 수정 후) 안전 | 예전엔 기본값이 `update`라 안 건드려도 매 기동마다 조용히 스키마를 변경하는 것이 Critical 리스크였음. 기본값을 `validate`로 바꿔 프로덕션에서 이 값을 아예 신경 쓰지 않아도 스키마를 건드리지 않도록 함. **`update`로 절대 덮어쓰지 말 것**(마이그레이션 도구 도입 전까지) |
| `SPRING_DATA_REDIS_HOST` | `spring.data.redis.host` | `localhost` | **필수** | loud로 추정(미검증) | Redisson이 기동 시 실제 연결을 시도하는 것으로 일반적으로 알려져 있음. 이 코드베이스에서 직접 재현 검증한 것은 아님 |
| `SPRING_DATA_REDIS_PORT` | `spring.data.redis.port` | `6379` | **필수** | loud로 추정(미검증) | 상동 |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `spring.kafka.bootstrap-servers` | `localhost:9092` | **필수** | **미확정(loud/silent 둘 다 가능성 있음)** | Kafka Producer는 일반적으로 lazy 연결이라, 이 값이 틀리거나 없어도 앱이 정상 기동하고 이벤트 발행만 조용히 실패할 가능성이 있음(미검증). **배포 후 반드시 실제 이체 1건을 발행해 `transfer-events` 토픽 수신을 직접 확인할 것.** |
| `MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_ENDPOINT` | `management.opentelemetry.tracing.export.otlp.endpoint` | `http://localhost:4318/v1/traces`(로컬 Jaeger) | 인프라 협의 대기 중 | silent | 배포 환경 OTLP Collector 엔드포인트가 아직 미정(PROGRESS.md에 이미 기록된 상태 그대로) — 안 바꾸면 트레이스 export만 조용히 실패, 앱 기능에는 영향 없음 |
| `CORS_ALLOWED_ORIGINS` | `cors.allowed-origins` | `http://localhost:5173`, `http://localhost:3000` | **필수** | silent | 관리자 프론트엔드가 호출을 허용받을 오리진 목록. 실제 관리자 프론트엔드 배포 도메인으로 교체 필요 — 안 바꾸면 앱은 정상 기동하지만, 등록 안 된 오리진에서의 요청은 서버 로그 없이 브라우저 단에서 CORS 에러로만 조용히 막힘. 리스트형 값이라 콤마 구분 문자열(`CORS_ALLOWED_ORIGINS=https://admin.example.com,https://admin2.example.com`)로 오버라이드 가능 — relaxed binding이 `List<String>`으로 정상 바인딩되는지 실제 프리플라이트 요청으로 검증 완료(env var에만 있는 오리진은 허용되고, yaml 기본값에만 있던 오리진은 env var가 있으면 사라짐 — merge가 아니라 override) |
| `JWT_SECRET` | `jwt.secret` | `local-dev-only-jwt-signing-secret-change-in-production-32bytes-min`(66바이트, 528비트 — HS256 최소 요구치인 256비트/32바이트는 충족하지만 이름 그대로 로컬 전용 더미값) | **필수** | **silent-breach** | 안 바꾸면 앱은 정상 기동하고 로그인도 정상 동작하는 것처럼 보이지만, 이 문자열이 공개 저장소에 커밋되어 있는 값이라 **누구나 같은 시크릿으로 유효한 관리자 JWT를 직접 위조해 서명할 수 있음** — 인증 자체가 사실상 없는 것과 동일한 상태가 됨. loud도 silent도 아닌 이유: silent는 "틀린 값 때문에 기능이 저하"되는 것이지만 이건 "값이 새어나가 있어서 인증 경계 자체가 무의미"해지는 것 — 겉보기엔 완벽하게 정상 동작해서 침해 여부를 앱 로그만 봐서는 절대 알 수 없음. **배포 전 반드시 별도로 생성한 고엔트로피 시크릿으로 교체할 것**(예: `openssl rand -base64 48`) |
| `ADMIN_INITIAL_USERNAME` / `ADMIN_INITIAL_PASSWORD` | `admin.initial.username` / `admin.initial.password` | 없음(yaml 기본값 미설정 — 둘 다 비어있으면 `AdminUserSeeder`가 계정 생성 자체를 스킵) | **필수(최초 배포 1회만)** | **silent-breach** | 로컬 개발 문서/README 등에 예시로 적어둔 값을 그대로 프로덕션에 써서 배포하면, 그 값이 곧 "알려진 관리자 계정"이 되어 누구나 로그인 가능 — 이것도 앱은 정상 기동/정상 동작하므로 겉보기엔 문제가 없어 보임. 최초 1회 생성 이후에는 이 값을 바꿔도 이미 만들어진 계정 자체는 안 바뀜(`AdminUserSeeder`는 idempotent — username이 이미 존재하면 skip)이므로, 초기 배포 시점에만 강한 값을 넣는 것으로 충분하지만 그 순간이 가장 중요함 |
| `APPROVAL_THRESHOLD` | `approval.threshold` | `10000000` | 필수 아님(업무 정책값) | N/A | Maker-Checker 승인이 필요해지는 금액 기준. 값이 틀려도 앱은 정상 동작, 업무 정책만 달라짐 |
| `FRAUD_THRESHOLD` | `fraud.threshold` | `50000000` | 필수 아님(업무 정책값) | N/A | 이상거래 탐지 임계치. 상동 |
| `EOD_BATCH_CRON` | `eod.batch.cron` | `"0 0 2 * * *"` | 필수 아님 | N/A | EOD 정산 배치 트리거 시각. 스테이징/프로덕션에서 다른 시각이 필요하면 이 값만 바꾸면 됨 |
| `RECONCILIATION_BATCH_CRON` | `reconciliation.batch.cron` | `"0 0 3 * * *"` | 필수 아님 | N/A | 정산 대사 배치 트리거 시각. EOD 이후 시각으로 유지할 것 |
| `K8S_LEADER_ELECTION_ENABLED` | `k8s.leader-election.enabled` | `false` | 필수 아님 | N/A | **Azure로 갈 경우 기본값 `false` 유지 권장** — K8s Lease API 기반 리더 선출은 실제 K8s 클러스터 환경(kind/AKS 등)이 전제. Azure 배포 대상이 확정되지 않은 현재는 건드리지 말 것 |
| `K8S_LEADER_ELECTION_NAMESPACE` | `k8s.leader-election.namespace` | `default` | 필수 아님 | N/A(`enabled=false`면 미사용) | `ENABLED=true`로 켤 때만 의미 있음 |
| `K8S_LEADER_ELECTION_LEASE_NAME` | `k8s.leader-election.lease-name` | `eod-settlement-leader` | 필수 아님 | N/A(`enabled=false`면 미사용) | 상동 |
| `K8S_LEADER_ELECTION_LEASE_DURATION_SECONDS` | `k8s.leader-election.lease-duration-seconds` | `15` | 필수 아님 | N/A(`enabled=false`면 미사용) | 상동 |
| `K8S_LEADER_ELECTION_RENEW_DEADLINE_SECONDS` | `k8s.leader-election.renew-deadline-seconds` | `10` | 필수 아님 | N/A(`enabled=false`면 미사용) | 상동 |
| `K8S_LEADER_ELECTION_RETRY_PERIOD_SECONDS` | `k8s.leader-election.retry-period-seconds` | `2` | 필수 아님 | N/A(`enabled=false`면 미사용) | 상동 |

## 스키마 변경이 포함된 배포

이 프로젝트는 Flyway/Liquibase 같은 마이그레이션 도구를 쓰지 않고, `ddl-auto: validate` 기본값을 전제로 스키마는 배포 담당자가 수동으로 맞춰야 합니다(로컬 `test`/`bootRun` 태스크만 `ddl-auto=update`로 오버라이드되어 있어 로컬에서는 자동으로 맞춰지지만, 이 오버라이드는 프로덕션에는 적용되지 않습니다).

- **`fix/decouple-approval-status-from-execution-result`(승인 상태와 집행 결과 분리)** — `transfer_approval_requests` 테이블에 컬럼 2개 추가 포함:
  - `execution_status VARCHAR(255) NOT NULL` — 기존 행이 있는 테이블에 `NOT NULL` 컬럼을 한 번에 추가하면 실패하므로, 배포 시 아래 순서로 적용할 것:
    ```sql
    ALTER TABLE transfer_approval_requests ADD COLUMN execution_status VARCHAR(255) NOT NULL DEFAULT 'NOT_APPLICABLE';
    ALTER TABLE transfer_approval_requests ALTER COLUMN execution_status DROP DEFAULT;
    ```
  - `execution_failure_reason VARCHAR(255)` — nullable이라 단순 `ADD COLUMN`으로 충분:
    ```sql
    ALTER TABLE transfer_approval_requests ADD COLUMN execution_failure_reason VARCHAR(255);
    ```
  - 배포 전 이 DDL을 프로덕션 DB에 먼저 적용하지 않으면, 새 애플리케이션 버전은 `ddl-auto: validate`가 스키마 불일치를 즉시 감지해 기동 자체가 실패합니다(loud) — 데이터 정합성보다는 기동 실패로 먼저 드러나는 종류의 변경.

## 인증 실패 시 HTTP 상태 코드 (프론트엔드 참고)

- **401 Unauthorized** — 요청에 유효한 인증 정보가 아예 없는 경우. `Authorization` 헤더 자체가 없거나, 토큰이 만료/위조/형식 오류로 `TokenPort.validateToken()`이 실패한 경우 전부 여기에 해당. 응답 바디는 `{"code":"UNAUTHORIZED", "message":"...", "timestamp":"..."}`.
- **403 Forbidden** — 인증은 됐지만(유효한 토큰을 갖고 있지만) 해당 작업을 수행할 권한이 없는 경우. 응답 바디는 `{"code":"FORBIDDEN", "message":"...", "timestamp":"..."}`.
- 현재 스코프(단일 `ADMIN` 역할)에서는 403이 실제로 발생할 경로가 없습니다 — 역할이 하나뿐이라 "인증은 됐는데 권한이 부족한" 상황 자체가 없기 때문입니다. 그래도 401/403을 처음부터 분리해둔 이유: 나중에 역할이 늘어나면(예: 조회 전용 역할 추가) 403 경로가 바로 의미를 갖게 되고, 프론트엔드도 그때 가서 에러 처리 로직을 새로 만들 필요 없이 지금부터 "401=로그인 필요, 403=권한 부족"으로 분기해두면 됩니다.

## 인프라 팀과 협의 필요한 별도 항목

아래는 코드 수정 없이, 배포 준비도 감사에서 확인된 사실만 그대로 옮긴 목록입니다.

- **Redis/Kafka 인증 경로 부재** — `RedissonConfig`/`KafkaProducerConfig`에 password/SASL 설정 필드 자체가 없음. Azure Cache for Redis, Event Hubs(또는 Azure 상의 Kafka 호환 서비스) 등 실제 대상이 정해지면 인증 설정 코드를 추가해야 함.
- **Kafka 재시도/DLT 토픽 replication factor=1** — 단일 장애점. 실제 브로커 구성(파티션/복제본 수)이 정해지면 그에 맞게 조정 필요.
- **ShedLock 네임스페이스 하드코딩** — `ENVIRONMENT="fbrl-backend"`로 고정돼 있어, staging/prod가 같은 Redis를 공유하면 분산 락 키가 충돌할 수 있음. 환경별로 분리 필요.
