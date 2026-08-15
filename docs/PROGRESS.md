# FBRL (Financial Backend Reliability Lab) 진행 상황

## 팀 구성

| 이름 | 역할 | 담당 업무 |
|---|---|---|
| 김주영 (본인) | Backend | 실시간 거래 파이프라인, Saga 오케스트레이션, Redisson 분산 락, Spring Batch 엔진 구축 |
| 김준희 | Infra / SRE | Kubernetes 클러스터, ArgoCD GitOps, Prometheus/Grafana 관측성 구축, Chaos Mesh 결함 주입 |

Chaos Mesh 결함 주입은 노션 "프로젝트 개요" 문서에 인프라(김준희) 담당으로 명시되어 있음. 백엔드는 "어떤 장애 시나리오로 무엇을 검증할지" 정의 + 장애 주입 후 애플리케이션(서킷 브레이커, 재시도 등)이 의도대로 반응하는지 검증하는 역할, 실제 CRD/클러스터 실행은 인프라와 협업.

이 프로젝트는 1인 개발이 아니라 Backend(본인)/Infra·SRE(김준희) 2인 협업 프로젝트임 — Chaos Mesh, K8s 클러스터 운영, GitOps/관측성 구축은 Infra 담당 영역이므로, 이런 영역을 "혼자 다 해야 하는지" 판단할 때는 먼저 노션 "프로젝트 개요"의 팀원 역할표를 확인할 것.

## 기술 스택

- Java 17 / Spring Boot 4.0.7 / 헥사고날 아키텍처 (Ports & Adapters)
- PostgreSQL 16 (wal_level=logical, Debezium CDC 기반)
- Redisson, Redis / Spring Batch 6.0.4 · ShedLock · Kafka · Kubernetes Lease API (client-java 27.0.0) · Resilience4j

## ✅ 완료된 작업

### 과제 1-2: 기본 구조 + 동시성 제어

- 헥사고날 기본 구조 (Account 도메인, In/Out Port, JPA 어댑터)
- Redisson 분산 락 (`@DistributedLock` AOP, REQUIRES_NEW)
- API 멱등성 (Redis SETNX, `@CheckIdempotency`)
- 락 성능 벤치마크 (분산락 vs 비관적락 vs 낙관적락, 100 스레드)

### 과제 3: 계좌 개설 & 잔액조회 API (완료, PR 리뷰 대기)

브랜치: `feat/account-api` → `develop`

- `Account.open()` 도메인 팩토리 (초기 잔액 0원 정책, SSOT)
- `CreateAccountUseCase` / `GetAccountUseCase` (Port In)
- `AccountNumberPolicy` (SecureRandom 채번, 약 36.5bit 엔트로피)
- `AccountCreationExecutor` (REQUIRES_NEW 트랜잭션, self-invocation 회피)
- 채번 충돌 시 최대 3회 재시도 (`CreateAccountService`)
- `AccountPersistenceAdapter`: `DataIntegrityViolationException` → `DuplicateAccountNumberException` 예외 번역
- `AccountController` (POST/GET), `AccountResponse` DTO, `GlobalExceptionHandler` 409 매핑
- 단위 테스트 3종 (`CreateAccountServiceTest`, `GetAccountServiceTest`, `AccountControllerTest`)
- 전체 테스트(`./gradlew test`) 통과 확인

### 과제 4: Transactional Outbox & 감사장부(Audit Log) 패턴 (완료)

브랜치: `feat/outbox-pattern` → `develop`

- `OutboxEvent` 도메인 모델 (PENDING/SENT/FAILED 상태 전이, Rich Domain Model)
- `TransferCompletedEvent` 도메인 이벤트 (record, 계좌 정보만 최소 선택하여 정보 노출 최소화)
- `SaveOutboxEventPort` / `PayloadSerializerPort` (Port Out)
- `OutboxEventJpaEntity` / `OutboxEventJpaRepository` / `OutboxPersistenceAdapter` (예외 번역 없이 전파 → 트랜잭션 롤백 보장)
- `JacksonPayloadSerializerAdapter` (Jackson 3 `JsonMapper`, 불변 객체로 스레드 세이프)
- `TransferMoneyService`의 `@DistributedLock` → REQUIRES_NEW 트랜잭션 경계 안에 Outbox insert 통합
- `TransferMoneyServiceTest` Mock 2개 추가 (`SaveOutboxEventPort`, `PayloadSerializerPort`)
- 리팩토링: `LockComparisonService`를 `application.service` → `adapter.out.persistence`로 이동 (JPA 락 실험 도구는 인프라 계층 소속)
- 리팩토링: `AccountJpaRepository` public → package-private 전환, 관련 테스트 3종(`LockComparisonTest`, `TransferConcurrencyTest`, `IdempotencyIntegrationTest`) 패키지 정리
- 전체 테스트(`./gradlew test`) 통과 확인
- (선택, 보류) Debezium CDC + PostgreSQL 전환 — 헥사고날 구조상 인프라 어댑터만 교체하면 되므로 확장 과제로 남김

### 과제 5: Outbox Polling Publisher (완료)

브랜치: `feat/outbox-polling-publisher` → `develop`

- `OutboxEventJpaRepository`: `findByStatusOrderByCreatedAtAsc(status, Pageable)` 커스텀 쿼리 추가
- `LoadPendingOutboxEventsPort` / `EventPublisherPort` (Port Out) 신규 추가
- `PublishPendingOutboxEventsUseCase` (Port In) — `PublishResult(publishedCount, failedCount)` 요약 반환
- `PublishPendingOutboxEventsService`: 이벤트 단위 즉시 커밋(오케스트레이션 메서드엔 `@Transactional` 미부여), "Kafka 발행 → 성공 확인 후 `markAsSent()`" 순서 고정, 한 건 실패해도 나머지 계속 처리(DLQ와 동일 철학)
- `OutboxPersistenceAdapter`: `LoadPendingOutboxEventsPort` 구현 추가 (Pageable 변환은 어댑터 내부로 캡슐화)
- `OutboxPollingScheduler` (`adapter.in.scheduler`): `@Scheduled(fixedDelay)` 트리거 전용, 배치크기/주기는 `outbox.polling.*` 설정으로 외부화
- `KafkaEventPublisherAdapter`: `key=aggregateId`로 파티션 고정(같은 계좌 이벤트 순서 보장), `send().get()`으로 동기 확인 후 성공/실패 판정
- `KafkaProducerConfig`: Boot 4.0 자동생성 `KafkaTemplate<Object,Object>` 타입 불일치 문제로 `KafkaTemplate<String,String>` 빈 직접 정의
- `KafkaTopicConfig`: `NewTopic` 빈으로 `transfer-events` 토픽 파티션/복제계수 명시 생성 (auto.create 기본값에 의존 금지)
- `build.gradle`: `spring-kafka` → `spring-boot-starter-kafka(-test)` 교체 (Boot 4.0 모듈 분리로 자동설정 클래스가 starter 없이는 클래스패스에 없음)
- `docker-compose.yml`: kafka 서비스 추가 (KRaft 단일 노드, `apache/kafka:3.9.0`)
- `PublishPendingOutboxEventsServiceTest`: PENDING 없음 / 발행 성공 시 순서(InOrder) 검증 / 부분 실패 시 나머지 계속 처리 3종
- 전체 테스트(`./gradlew test`) 통과 확인
- (보류) 실제 Kafka 브로커 E2E 수동 검증(IntelliJ Kafka 플러그인으로 콘솔 확인) — 로컬 계좌 잔액 시딩 방법 정리되면 재시도 예정

**리팩토링 메모 해소 (과제 4에서 이월된 항목)**

- `AccountPersistenceAdapter.findByAccountNumber()`: `DataAccessException` → `AccountPersistenceException`(신규 도메인 예외)로 번역, `GlobalExceptionHandler`에 500 매핑 추가 (기존 임시로 쓰였던 `IllegalStateException` 제거)
- `DistributedLockAspect`: `@Order(Ordered.HIGHEST_PRECEDENCE)` 클래스 레벨 적용 확인 완료

### 과제 6: Saga 패턴 (완료 — 트랙 1 "실시간 금융 트랜잭션 & 분산 동시성 제어" 마무리)

브랜치: `feat/saga-orchestration` → `develop` (PR #16, 커밋 75c062b) → `develop` → `main` (PR #17, 사용자 직접 머지)

**도메인/개념 학습**

- 2PC의 가용성 문제 → Saga(로컬 트랜잭션 체이닝 + 보상 트랜잭션) → Choreography vs Orchestration(감사 추적성 때문에 Orchestration 채택) → Orchestrator = 영속화된 상태 머신
- `SagaStatus` 도메인 모델 (`domain.model`, `canTransitionTo()`로 상태 전이 규칙을 Switch Expression exhaustiveness로 캡슐화)
- `@Enumerated(EnumType.STRING)` vs `ORDINAL`: enum 선언 순서 변경 시 기존 저장 데이터 의미가 조용히 오염되는 문제 → STRING 채택 (`TransferSagaJpaEntity`에 적용)
- `InvalidSagaTransitionException` / `InvalidTransferAmountException` (`domain.exception`)
- `TransferSaga` 도메인 모델 (`domain.model`, `start()`/`reconstruct()` 이원화)
- 버그 발견 및 수정: `SagaStatus.canTransitionTo()`에 `STARTED -> FAILED` 전이가 누락되어, 출금 자체가 실패한 케이스(`saga.fail()`)에서 `InvalidSagaTransitionException`이 잘못 터지는 문제 확인 → 전이 규칙에 `STARTED -> FAILED` 추가하여 해결

**Account 패턴과의 정합성 맞춤 (SSOT)**

- `TransferSaga`에 기술적 PK `id`(Long, nullable) 필드 추가, `version` 타입 `long` → `Long`(nullable)로 변경 — `Account.id`/`Account.version`과 동일한 "nullable 소유 + 매퍼가 매번 완전한 엔티티를 재구성해 `save()` 단일 호출로 insert/update 통일" 컨벤션에 맞춤
- `reconstruct()` 시그니처에 `id` 파라미터 추가

**영속성 어댑터 4종 (`adapter.out.persistence`)**

- `TransferSagaJpaEntity`: `AccountEntity`와 동일하게 Money는 BigDecimal로 풀어서 저장(도메인 프레임워크 무의존 원칙 유지), `SagaStatus`는 enum 타입 그대로 필드에 두고 어댑터 계층 어노테이션(`@Enumerated`)만 부여(도메인 클래스 자체는 오염 안 됨)
- `SagaMapper`: `AccountMapper`와 동일 패턴 (`toDomain()` / `toEntity()` 단일 메서드, null 방어)
- `TransferSagaJpaRepository`: package-private JpaRepository, YAGNI 원칙에 따라 커스텀 쿼리 없이 기본 `save()`만 사용
- `SagaPersistenceAdapter`: `ObjectOptimisticLockingFailureException`(하위 타입) → `DataAccessException`(상위 타입) 순서로 catch(순서 뒤바뀌면 낙관적 락 충돌이 영원히 안 잡힘)하여 각각 `ConcurrentSagaModificationException`(409) / `SagaPersistenceException`(500)으로 번역

**참여자 어댑터 2종 (`adapter.out.participant`, 신규 패키지 — 향후 MSA 분리 시 이 안의 구현체만 HTTP/gRPC 클라이언트로 교체)**

- `WithdrawalParticipantAdapter` / `DepositParticipantAdapter`: 예외를 오케스트레이터로 절대 전파하지 않고 `Result(boolean success, String failureReason)`로 항상 수렴시킴 — 알려진 도메인 예외(`AccountNotFoundException`, `InsufficientBalanceException`)뿐 아니라 예기치 못한 `RuntimeException`까지 잡아서 `Result(false, ...)`로 변환(안 하면 실패 처리 경로가 두 갈래로 나뉘어 정합성 깨짐), 원인은 `log.error`로 서버 로그에 남김
- 계좌 하나 단위로 `@DistributedLock(key = "#accountNumber")` 적용

**예외 2종 + GlobalExceptionHandler**

- `SagaPersistenceException`(500, `AccountPersistenceException`과 동일 패턴 — 인프라 예외 메시지 비노출)
- `ConcurrentSagaModificationException`(409, "재시도 안내" 형태의 actionable 메시지로 고정 — 프레임워크 원문 메시지 비노출)

**AI 에이전트 위임 작업 검증 (이번 세션에서 확립한 워크플로)**

- package-private 접근제어자 위반 전수 수정을 AI 에이전트(Claude Code)에 위임 — 판단 기준(cleanup만 필요하면 어댑터 위임 / 락 세부구현 검증이면 패키지 이동)과 "리포지토리를 다시 public으로 되돌리지 말 것" 금지 규칙을 명시한 작업 지시서로 위임
- 에이전트가 만든 `TransferSagaJpaRepository`가 실수로 public으로 생성된 것을 git diff 리뷰로 발견 → package-private으로 재수정 지시
- git diff/status 결과가 터미널에서 반복적으로 잘려서(--stat, pager) 안 보이는 문제 발생 → 최종적으로 GitHub 웹에서 직접 커밋(75c062b)의 file tree diff를 열람하여 26개 파일 전체를 실제로 검증 완료 (untracked 신규 파일 누락 여부, 캡슐화 회귀 여부 등)
- 감사(audit) 결과 미해결 2건 모두 최종 오탐으로 확인: `LockComparisonService`는 의도된 인프라 계층 벤치마크 도구, `AccountJpaRepository`는 실제로 package-private 유지 중이었음(사용자가 직접 코드 확인)

### 과제 7: Spring Batch 6.0.4 인프라 검증 (완료 — 트랙 2 "EOD 대규모 금융 배치 플랫폼" 착수)

브랜치: `feat/spring-batch-foundation` → `develop`

- `build.gradle`: `spring-boot-starter-batch`(→ `spring-batch-core` 6.0.4 자동 포함), `spring-batch-test` 추가
- `application.yml`: `spring.batch.job.enabled: false`(파드 재시작마다 Job 중복 자동실행되는 사고 예방), `spring.batch.jdbc.initialize-schema: always`
- `EodInfraCheckJobConfig` (`adapter.in.batch`): JobRepository ↔ MariaDB 배선만 검증하는 최소 Job/Step/Tasklet (업무 로직 없음, 과제 8에서 실제 EOD 로직으로 교체 예정)
- `EodInfraCheckJobConfigTest`: `JobOperatorTestUtils.startJob()`으로 명시적 실행 → `BatchStatus.COMPLETED` 검증
- 전체 테스트(`./gradlew test`) 통과 확인
- 노션 "프로젝트 개요" 기술 스택 최신화 (Spring Boot 3.x / Spring Batch 5.x → 4.0.7 / 6.x)

### 과제 8: EOD 정산 Job 실전 구현 (완료 — Chunk-oriented, 이자 계산/마감 스냅샷)

브랜치: `feat/eod-settlement-job` → `develop`

**도메인 계층**

- `EodSnapshot` 도메인 모델(record) — 마감 스냅샷은 한 번 기록되면 절대 변경되지 않는 불변 사실이라 Account/TransferSaga(class)와 다르게 record로 설계, `totalBalance()`는 저장 필드가 아닌 계산 메서드(SSOT)
- `InterestPolicy`(record) — 단리(simple interest) 일할 이자 계산, 이자 계산 규칙을 EodSnapshot과 별도 타입으로 분리(SRP/OCP, Saga 참여자 어댑터 분리와 동일한 논리). 연이자율÷365 중간 계산은 scale 10 유지 후 최종만 원단위 반올림(오차 누적 방지)
- `InvalidInterestRateException`, `DuplicateEodSnapshotException` (`domain.exception`)
- `InterestPolicyTest` — 깨끗하게 나눠떨어지는 이자율/순환소수 반올림/0원 경계값 등 5종

**포트 & 영속성 계층**

- `LoadAllAccountsPort` / `SaveEodSnapshotPort` (Port Out) — page/size 원시값만 받고 Pageable 변환은 어댑터 내부로 캡슐화(Outbox 폴링 컨벤션과 동일), Writer가 청크 단위로 받으므로 `saveAll(List)`만 정의(YAGNI)
- `EodSnapshotJpaEntity` — `@Version` 없음(INSERT 후 UPDATE 없는 불변 레코드), `(account_number, settlement_date)` 복합 유니크 제약으로 재시작 시 중복 저장 방지, setter 없음
- `EodSnapshotMapper`, `EodSnapshotJpaRepository`(package-private, YAGNI)
- `EodSnapshotPersistenceAdapter` — `DataIntegrityViolationException` → `DuplicateEodSnapshotException` 예외 번역
- `AccountPersistenceAdapter`에 `LoadAllAccountsPort` 구현 추가 — id 기준 정렬 명시(정렬 없는 OFFSET 페이징은 페이지 간 순서 일관성 미보장)

**배치 계층 (`adapter.in.batch`)**

- `AccountItemReader`(커스텀 `ItemStreamReader`) — `RepositoryItemReader` 대신 직접 구현(`AccountJpaRepository`가 package-private이라 헥사고날 경계상 접근 불가), `ExecutionContext`에 readCount 저장해 재시작 시 페이지/skip 위치 정확히 복원
- `AccountInterestItemProcessor` — Account+InterestPolicy+settlementDate 조립만 하는 얇은 배선 코드, `@StepScope` + `@Value("#{jobParameters['settlementDate']}")`
- `EodSnapshotItemWriter` — Chunk를 `SaveEodSnapshotPort.saveAll()`에 위임
- `EodSettlementJobConfig` — `StepBuilder(name, jobRepository).chunk(1000).transactionManager(tm)...`(Spring Batch 6.0.4 GA 방식)
- `EodSettlementJobConfigTest` — 종단 간 테스트, `JobOperatorTestUtils`가 상속한 `launchJob(JobParameters)`로 실행, `BatchStatus.COMPLETED` + 실제 이자 계산값(연 3.65%, 100만원→100원) 검증
- 전체 테스트(`./gradlew test`) 통과, 빌드 성공 확인

**핵심 개념**

- Chunk-oriented Processing: 커밋 단위 = 청크 단위(재시작 시 청크 시작점부터 재개, 개별 아이템 단위 아님)
- JobParameters = Job의 정체성: JobRepository가 "Job 이름 + JobParameters" 조합으로 JobInstance를 식별 → settlementDate를 `LocalDate.now()` 대신 명시적 JobParameter로 넘겨야 재시작 시 같은 JobInstance로 인식되어 이어서 처리됨
- `@StepScope` 지연 바인딩(Late Binding): JobParameters는 Job 실행 시점에야 결정되는데 스프링 빈은 기동 시 미리 생성됨 → `@StepScope`로 빈 생성 자체를 Step 시작 시점까지 미룸

**트러블슈팅 (Spring Batch 6.0.4 GA API, 공식 문서로 3회 재확인하며 정정)**

- `ChunkOrientedStepBuilder`를 `new`로 직접 생성하는 코드는 6.0.0-M2(마일스톤) 문서 기준이었고, 실제 6.0.4 GA는 `new StepBuilder(name, jobRepository).chunk(size).transactionManager(tm)...` 방식(공식 마이그레이션 가이드로 재확인)
- `JobOperatorTestUtils extends JobLauncherTestUtils` — `startJob(JobParameters)`가 `@StepScope` 빈에 파라미터를 전달 못하는 버그(spring-batch#5216, 6.0.x, "Closed as not planned"로 미해결) → 상속받은 `launchJob(JobParameters)`(6.2+ 제거 예정 구버전 API)를 대신 사용
- `JobParameters`/`JobParametersBuilder`가 `org.springframework.batch.core.job.parameters` 패키지로 이동(6.0 패키지 재구성 목록에 추가 확인)
- `Account` 생성자가 private으로 강화됨 — `new Account(...)` 대신 `Account.create()`/`reconstruct()` 정적 팩토리 사용 필요(SSOT 정책 강화 반영)

### 과제 9: ShedLock 분산 스케줄 락 (완료 — 다중 인스턴스 중복 실행 방지)

브랜치: `feat/shedlock-scheduler` → `develop`

- `build.gradle`: `shedlock-spring:7.7.0`, `shedlock-provider-redis-spring:7.7.0`(Spring Boot 4.x 호환 계열은 7.x.x, 공식 호환성 표 기준)
- `ShedLockConfig`(`global.config`) — `@EnableScheduling` + `@EnableSchedulerLock(defaultLockAtMostFor="10m")`, `RedisConnectionFactory` 기반 `RedisLockProvider`(spring-boot-starter-data-redis가 자동 구성해준 빈 재사용 — ShedLock 공식 프로바이더 중 Redisson 전용은 없음)
- `EodSettlementScheduler`(`adapter.in.scheduler`) — `@Scheduled(cron="${eod.batch.cron:0 0 2 * * *}")` + `@SchedulerLock(lockAtLeastFor="5m")`, `JobOperator.start(Job, JobParameters)` 사용(JobLauncher는 6.0부터 deprecated)
- `JobInstanceAlreadyCompleteException`은 별도 catch하여 INFO 로그(정상 시나리오로 취급), 그 외는 ERROR
- `EodSettlementSchedulerTest` — `JobOperator`를 `@MockitoBean`으로 대체, `CountDownLatch`로 5개 스레드 동시 트리거 → `jobOperator.start()` 호출이 정확히 1회인지 검증
- 전체 테스트(`./gradlew test`) 통과, 빌드 성공 확인

**핵심 개념**

- ShedLock은 상호 배제(기다림)가 아니라 선점 후 스킵 패턴 — "이미 실행 중이면 기다리지 않고 그냥 건너뜀"(README: "execution on other nodes does not wait, it is simply skipped"). Redisson 분산 락(상호 배제, 순서대로 다 처리)과 근본적으로 다른 용도
- `lockAtMostFor`: 인스턴스가 락을 쥔 채로 죽어도 무조건 이 시간 후 락이 풀리는 안전장치, 정상 실행 시간보다 넉넉히 길게 설정
- `lockAtLeastFor`: Job이 실제로는 순식간에 끝나더라도, 최소 이 시간 동안은 락을 강제로 유지시켜 "찰나의 틈"으로 다른 인스턴스가 끼어드는 것을 방지하는 안전장치. 다만 이 값이 클수록 같은 작업을 짧은 간격으로 재실행/재트리거할 때 "이미 락이 걸려있다"고 판단되는 구간도 함께 길어짐(→ 과제 11 트러블슈팅 참고)
- `ShedLockConfig`(인프라 설정, 앱 전체 1개)와 `@SchedulerLock`(개별 작업 설정, 작업마다 1개)은 별개 — 스케줄러가 늘어나도 LockProvider는 안 건드림

**트러블슈팅**

- `JobOperator`(비-deprecated) vs `JobLauncher`(6.0부터 deprecated, 6.2+ 제거 예정) — 프로덕션 코드는 `JobOperator.start(Job, JobParameters)` 사용, 관련 예외(`JobInstanceAlreadyCompleteException` 등)는 `org.springframework.batch.core.launch` 패키지(JobOperator와 동일 패키지라 별도 import 불필요)
- Redisson을 쓰고 있어도 ShedLock 공식 프로바이더 중 Redisson 전용은 없음(README 확인) — spring-boot-starter-data-redis가 자동 구성해주는 `RedisConnectionFactory` 기반 `shedlock-provider-redis-spring`을 대신 사용
- Mockito 가짜 객체 메서드 호출 시에도 원본 메서드의 checked exception 선언이 그대로 적용됨 — `verify(mock).checkedMethod()` 호출부도 그 예외를 처리해야 함(테스트 메서드에 `throws Exception`으로 포괄 선언)

### 과제 10: Kubernetes Lease API 기반 리더 선출 연동 (완료 — 트랙 2 "EOD 대규모 금융 배치 플랫폼" 마무리)

브랜치: `feat/k8s-lease-election` → `develop`

**선행 학습 세션 (착수 전, 코드 작성 없이 개념만 정리)**

- API Server = 클러스터의 단일 진입점 — 모든 요청(Lease 조회/갱신 포함)이 반드시 거쳐감. 인증(401, "누구냐") → 인가(403, "권한 있냐") 순서로 관문 통과. `AccountController`가 클라이언트-DB 사이의 유일한 관문 역할을 하는 것과 동일한 논리.
- ServiceAccount/RBAC = Pod의 신원 + 최소 권한 — ServiceAccount는 Pod용 신원(JWT의 sub와 유사). Role은 "무엇을 할 수 있는가"(get/create/update만 명시, delete나 `*`는 배제 — `LoadPendingOutboxEventsPort`가 필요한 범위만 노출한 것과 동일 사고방식). RoleBinding은 ServiceAccount ↔ Role 연결.
- Lease 리더 선출 = 낙관적 락 기반 경합 — holderIdentity + renewTime + resourceVersion(JPA `@Version`과 동일 원리)
- Split-Brain 위험 및 EOD 정산 Job에 대입한 실제 리스크(중복 수행 시 자원 낭비 2배, DB 유니크 제약은 저장 시점의 최후 방어선일 뿐 그 전 읽기/계산 낭비는 못 막음)
- 로컬 검증 환경: minikube/kind/Testcontainers K3s 비교 후 kind로 결정(리소스 가볍고, `kubectl get lease`로 resourceVersion 변화를 눈으로 직접 관찰하는 것을 우선순위로 둠)

**수동 실습: resourceVersion 낙관적 락 직접 재현**

- kind 클러스터(`kind create cluster --name fbrl-lease-lab`)에 Lease 오브젝트를 `kubectl create`로 직접 생성
- 동일 resourceVersion을 가진 두 YAML 사본을 만들어 순서대로 `kubectl replace` → 먼저 적용한 쪽은 성공(resourceVersion 자동 증가), 나중 쪽은 `409 Conflict: the object has been modified` 확인 — DB `@Version` 충돌과 동일한 메커니즘을 API Server 레벨에서 직접 재현

**RBAC (`k8s/rbac/`)**

- ServiceAccount(`eod-settlement-leader-election`) — Pod용 신원
- Role — `apiGroups: coordination.k8s.io`, `resources: leases`, `verbs: get/list/watch/create/update`만 부여(delete·`*` 배제, 최소 권한 원칙)
- RoleBinding — 위 ServiceAccount ↔ Role 연결. `roleRef.apiGroup`은 Lease가 아니라 "Role이라는 오브젝트 자체"의 소속 그룹(`rbac.authorization.k8s.io`)이라는 점에 주의(다루는 대상의 소속과 자기 자신의 소속은 별개)
- `kubectl auth can-i create/delete leases --as=system:serviceaccount:...`로 최종 권한 시뮬레이션 검증 완료

**애플리케이션 계층**

- `build.gradle`: `io.kubernetes:client-java:27.0.0`, `client-java-extended:27.0.0` 추가(LeaderElector/LeaseLock은 extended 모듈 소속, starter 없이 순수 라이브러리라 자동설정 없음 — 배선은 전부 수동)
- `LeaderElectionPort`(`application.port.out`) — Runnable(onStartLeading/onStopLeading) + Consumer\<String\>(onNewLeader) 콜백 시그니처만 노출, K8s Java Client 타입은 시그니처에 절대 등장하지 않음(도메인 순수성 유지, `LoadAccountPort`에 EntityManager가 없는 것과 동일 원칙)
- `LeaderElectionProperties`(`global.config`, record + `@ConfigurationProperties`) — enabled/namespace/leaseName/leaseDurationSeconds/renewDeadlineSeconds/retryPeriodSeconds. `@ConfigurationPropertiesScan`을 메인 클래스에 추가(향후 설정 클래스가 늘어나도 메인 클래스를 계속 안 건드려도 되도록)
- `KubernetesApiClientConfig`(`global.config`) — `ClientBuilder.cluster()`(클러스터 내부 ServiceAccount 토큰 인증)를 우선 시도, 실패 시 `Config.defaultClient()`(로컬 kubeconfig)로 폴백하여 배포 환경/로컬 개발 환경 모두 동일 코드로 동작
- `KubernetesLeaderElectionAdapter`(`adapter.out.kubernetes`) — `LeaderElectionPort` 구현체. `LeaseLock(namespace, leaseName, identity, apiClient)` + `LeaderElectionConfig(lock, leaseDuration, renewDeadline, retryPeriod)`로 LeaderElector 구성. `LeaderElector.run()`은 블로킹 호출이므로 daemon `ExecutorService`(단일 스레드)에서 실행(non-daemon으로 두면 리더 선출 루프가 절대 스스로 안 끝나 graceful shutdown이 SIGKILL로 강제 종료될 위험). identity는 `POD_NAME` 환경변수(K8s Downward API) 우선, 없으면 hostname으로 폴백
- `@PreDestroy`로 `leaderElector.close()`(AutoCloseable) 호출 — graceful shutdown 시 리더 자격을 능동적으로 반납하여, leaseDuration(15초)을 다 기다리지 않고 팔로워가 더 빨리 리더를 이어받도록 함
- 두 `@Configuration`/`@Component`(`KubernetesApiClientConfig`, `KubernetesLeaderElectionAdapter`) 모두에 `@ConditionalOnProperty(k8s.leader-election.enabled=true)` 적용 — kind/kubeconfig가 없는 환경(CI, 다른 개발자 PC)에서 기존 `@SpringBootTest` 전체가 ApplicationContext 로딩 실패로 깨지는 것을 방지(기본값 false)

**수동 검증 (kind 클러스터 대상)**

- `kubectl get lease -o yaml -w`로 실시간 관찰하며 애플리케이션 기동
- 리더 획득: holderIdentity가 애플리케이션 hostname으로 설정됨, 콘솔에 `onStartLeading` 로그 확인
- 지속 갱신: resourceVersion이 retryPeriodSeconds(2초) 간격으로 계속 증가
- Graceful shutdown: Ctrl+C 시 `@PreDestroy` 로그 확인 및 resourceVersion 갱신 즉시 중단 확인
- 검증에 사용한 임시 ApplicationRunner(`LeaderElectionVerificationRunner`)는 검증 완료 후 삭제(재사용되지 않는 코드는 남기지 않음, YAGNI)

**핵심 개념**

- resourceVersion은 Lease뿐 아니라 모든 K8s 오브젝트의 공통 메타데이터(`metadata.resourceVersion`) — Service 전용 개념이 아니라 오브젝트 전체에 걸친 낙관적 락 메커니즘
- leaseDuration(외부/팔로워 관점의 만료 판단 기준) vs renewDeadline(리더 본인의 자기 검열 기준, 항상 leaseDuration보다 짧게 잡아 GC pause 등으로 갱신 실패 시 API가 강제로 뺏기 전에 스스로 물러남) — 이 여유 구간이 없으면 리더가 자신의 갱신 실패를 스스로 의심할 기준이 없어져 Split-Brain 겹침 구간이 길어짐
- ShedLock(즉시 skip, 상호 배제 아님) / Redisson(pub·sub 기반 블로킹 대기) / K8s Lease 리더 선출(지속 갱신 기반 단일 리더 고정) — 프로젝트 기획 문서 기준으로 이 프로젝트는 세 가지 분산 스케줄링 대안을 나란히 구현해 비교 실험하는 것이 목적이며, K8s 리더 선출이 ShedLock을 대체하는 관계가 아님(둘 다 유지)
- LeaderElector는 AutoCloseable — CancellationToken 등을 직접 구현할 필요 없이 `close()` 한 줄로 정리 가능

**트러블슈팅**

- `ClientBuilder.cluster()`의 실패는 `IOException`이 아니라 `IllegalStateException`(내부적으로 `NumberFormatException` 래핑) — 로컬 환경엔 `KUBERNETES_SERVICE_HOST`/`PORT` 환경변수가 없어 발생. catch 타입을 `IOException`에서 `Exception`으로 넓혀야 로컬 kubeconfig 폴백이 실제로 동작함(라이브러리 내부 구현이 어떤 unchecked exception을 던질지는 실제로 돌려보기 전엔 확신할 수 없음)
- 손으로 `kubectl create`한 불완전한 Lease(leaseTransitions 필드 누락 상태)를 LeaderElector가 읽으려 하면 `NullPointerException`(`getLeaseTransitions().intValue()`의 auto-unboxing) — `kubectl delete` 후 LeaderElector가 처음부터 새로 생성하게 하면 해결(라이브러리가 스스로 만드는 Lease는 모든 필드가 채워져 있음)
- `LeaseLock` 생성자 시그니처: `(namespace, name, identity, ApiClient)` / `LeaderElectionConfig` 생성자 시그니처: `(lock, leaseDuration, renewDeadline, retryPeriod)` — client-java-extended 27.0.0 기준(버전마다 시그니처가 다를 수 있어 공식 소스/javadoc으로 재확인 필요)
- `@Bean` 메서드에서 예외가 나면 그 빈 하나만 실패하는 게 아니라 ApplicationContext 전체 로딩이 실패 — K8s 관련 설정처럼 "항상 뜬다고 보장 못 하는 외부 인프라"에 의존하는 빈은 반드시 `@ConditionalOnProperty` 등으로 기본 비활성화해 무관한 기존 테스트까지 연쇄로 깨지지 않게 방어할 것

### 과제 11: Kafka Consumer Non-blocking Retry Topic & DLT (완료 — 트랙 3 "장애 복구 & 카오스 엔지니어링" 착수)

브랜치: `feat/kafka-dlq-retry-topic` → `develop` (PR 예정)

**개념 학습**

- Non-blocking Retry Topic: 실패한 이벤트를 별도 토픽으로 위임하고 메인 컨슈머는 즉시 다음 이벤트로 넘어가는 패턴 — "같은 계좌 내 이벤트 순서"를 일부러 희생하고 "다른 계좌들의 처리량"을 지키는 트레이드오프임을 확인(blocking retry 시 무관한 계좌 이벤트까지 전부 발이 묶이는 Consumer Lag 폭증 문제 방지)
- DLQ(범용 메시징 용어) vs DLT(Kafka 전용 용어, Dead Letter Topic) 구분 — Kafka는 큐가 아닌 토픽 기반이라 Spring Kafka API 전체가 DLT로 통일
- 파티션 수 일치의 의미: 재시도/DLT 토픽 파티션 수를 원본과 동일하게 맞추면 key(계좌번호) 보존과 결합되어 동일 key가 항상 동일 파티션으로 해시됨(`hash(key) % 파티션수`) → 특별한 설정이 아니라 자연스러운 결과물이며, 장애 시 복구 처리량을 원본과 동등하게 유지하는 목적

**포트 계층 (ISP 적용)**

- `PayloadDeserializerPort`(`application.port.out`) 신규 — 기존 `PayloadSerializerPort`(직렬화 전용)와 별도 인터페이스로 분리(클라이언트별 필요 메서드만 노출, ISP), 구현체는 `JacksonPayloadSerializerAdapter` 하나로 통합(ISP는 인터페이스 표면의 문제이지 구현체 개수의 문제가 아님)
- `ProcessTransferEventUseCase`(`application.port.in`) 신규 — `TransferCompletedEvent`를 그대로 파라미터로 받음, 1:1 Command 래핑 생략(이미 번역이 끝난 순수 도메인 타입을 또 감싸는 것은 Premature Abstraction)

**도메인 예외 계층**

- `NonRetryableEventProcessingException`(`domain.exception`, 추상 상위 타입) 신규 — "재시도해도 결과가 달라지지 않는 결정론적 실패"의 공통 상위 타입으로 설계, 향후 새 결정론적 실패 예외가 생겨도 이 타입만 상속하면 `KafkaRetryTopicConfig` 재수정 없이 자동으로 재시도 제외 대상에 포함됨(OCP)
- `PayloadDeserializationException`이 위 상위 타입을 상속하도록 구성, cause(원인 예외) 보존 필수화

**어댑터 계층**

- `JacksonPayloadSerializerAdapter` — `PayloadSerializerPort` + `PayloadDeserializerPort` 동시 구현, `deserialize()`에서 Jackson 3의 unchecked `JacksonException`을 의도적으로 catch하여 `PayloadDeserializationException`으로 번역(인프라 예외 노출 금지 원칙 적용)
- `KafkaRetryTopicConfig`(`adapter.in.kafka`) — maxAttempts(4)(최초 1회+재시도 3회) + 지수 백오프(1s→2s→4s..., 최대 30s), `notRetryOn(NonRetryableEventProcessingException.class)` + `traversingCauses()`, `includeTopic("transfer-events")`로 적용 대상 명시, `autoCreateTopics(true, 3, 1)`로 재시도/DLT 토픽까지 브로커 auto-create에 의존하지 않고 명시적 생성(기존 `KafkaTopicConfig` 원칙의 확장)
- `TransferEventConsumer`(`adapter.in.kafka`) — `@KafkaListener`에서 역직렬화 실패 시 예외를 절대 삼키지 않고 그대로 전파(재시도/DLT 판단은 `RetryTopicConfiguration`에 위임, 관심사 분리), `@DltHandler`는 실패 없는 로깅만 수행(최후 보루이므로 여기서 또 실패하면 메시지 완전 유실)
- `ProcessTransferEventService`(`application.service`) — 현재는 로깅 수준 최소 구현(배선 검증 단계, 실제 알림/감사로그 로직은 후속 과제)

**테스트**

- `JacksonPayloadSerializerAdapterTest` — 직렬화↔역직렬화 라운드트립, 깨진 JSON/스키마 불일치 시 `PayloadDeserializationException` + cause 보존 검증
- `TransferEventConsumerTest` — 정상 처리 시 UseCase 호출 검증, 역직렬화 실패 시 예외가 삼켜지지 않고 그대로 전파되는지 + 부작용(UseCase 미호출) 검증
- 전체 테스트(`./gradlew test`) 통과 확인
- (보류) 실제 Kafka 브로커로 재시도 토픽 → DLT 라우팅까지 흘러가는 통합 테스트는 범위를 분리하여 다음 세션에서 진행

**부수 발견 및 별도 수정: ShedLock 테스트 격리 결함 (과제 9 후속)**

브랜치: `fix/shedlock-test-isolation` → `develop` (별도 PR, Kafka 작업과 분리 — Atomic PR 원칙)

- 오늘 작업 중 `./gradlew test`에서 `EodSettlementSchedulerTest`가 `WantedButNotInvoked`(`jobOperator.start()` 0회 호출)로 실패하는 것을 발견
- 원인 규명: `EodSettlementScheduler`의 `@SchedulerLock(lockAtLeastFor = "5m")`로 Job이 끝나도 Redis 락(`job-lock:fbrl-backend:eodSettlementJob`)이 최소 5분간 유지됨 — 테스트가 락 키를 정리하지 않아 5분 내 재실행 시 5개 스레드 전부가 "이미 걸린 락"을 만나 아무도 실행되지 못함(ShedLock 자체는 의도대로 정상 동작, 테스트 격리 미흡이 원인)
- 수정: `EodSettlementSchedulerTest`에 `@BeforeEach`로 락 키 삭제 로직 추가(`@AfterEach`가 아닌 `@BeforeEach`를 택한 이유: 이전 실행이 비정상 종료됐을 때도 다음 실행이 무조건 깨끗한 상태에서 시작하도록 보장하기 위함 — 기존 `deleteAllInBatch()` 컨벤션과 동일한 방어적 설계 원칙)
- AI 에이전트(Claude Code)에게 진단 및 1차 수정 위임 → `@AfterEach`로 작성된 초안을 지침 컨벤션 근거로 반려하고 `@BeforeEach`로 재작성 요청 → 연속 재실행 검증 + 전체 테스트 스위트 통과 확인
- 세션 중 두 작업의 브랜치가 뒤섞이는 사고 발생(Kafka WIP가 fix 브랜치 워킹 디렉토리에 얹힘) → `git log`/`git status --short`/`git diff`로 커밋 경계 및 파일별 변경 내용을 직접 검증 후 `git stash`로 안전하게 분리 이동

### 과제 12: Resilience4j 서킷 브레이커 (완료 — 트랙 3 "장애 복구 & 카오스 엔지니어링" 두 번째 과제)

브랜치: `feat/resilience4j-circuit-breaker` → `develop` (PR 예정)

**개념 학습**

- 서킷 브레이커 3상태(CLOSED/OPEN/HALF_OPEN) — Kafka 발행은 post-commit 비동기 경로(`OutboxPollingScheduler`에서 호출)라 DB 호출과 달리 롤백 안전망이 없고, 실패해도 되돌릴 트랜잭션 자체가 없다는 점이 우선순위 판단 근거
- Retry Topic(과제 11)과의 역할 분리: Retry는 "이벤트 단위 재시도", 서킷 브레이커는 "Kafka 자체 생사 판단 후 시도 자체를 차단" — 배타적이지 않고 함께 사용
- 실패 판정 방식 두 갈래: 예외 기반(`recordExceptions`) vs 반환값 기반(`recordResult` 커스텀 Predicate) — 어댑터가 예외를 던지는 컨벤션이면 전자만으로 충분, boolean 등으로 성공/실패를 번역해 반환하는 컨벤션이면 후자로 보강 필요(우리 프로젝트는 이미 예외를 던지는 컨벤션이라 전자만 사용)

**의존성/설정**

- `resilience4j-spring-boot4:2.4.0` 버전 명시 고정 (BOM 미반영 버그, resilience4j/resilience4j#2427 아직 open)
- `spring-boot-starter-aop` → `spring-boot-starter-aspectj`로 아티팩트명 변경 확인 후 반영 (Boot 4.0 리네임, spring-projects/spring-boot#42948)
- `application.yml`: `kafkaEventPublisher` 인스턴스 — COUNT_BASED, slidingWindowSize 10, minimumNumberOfCalls 10, failureRateThreshold 50%, waitDurationInOpenState 30s, permittedNumberOfCallsInHalfOpenState 5, automaticTransitionFromOpenToHalfOpenEnabled true

**어댑터 계층**

- `KafkaEventPublisherAdapter#publish()`에 `@CircuitBreaker(name="kafkaEventPublisher", fallbackMethod="publishFallback")` 적용
- 기존 "예외를 `EventPublishException`으로 번역해 던지는" 컨벤션 그대로 유지 — fallback도 `CallNotPermittedException` 포함 모든 실패를 `EventPublishException`으로 재변환해 `PublishPendingOutboxEventsService`의 "실패 시 `markAsSent()` 호출 안 함" 계약 보존
- fallback 메서드는 실패를 무마시키는 곳이 아님: 예외를 삼키고 조용히 리턴하면 호출부가 성공으로 오인해 `markAsSent()`를 잘못 호출할 위험 → 항상 `EventPublishException`을 재던짐

**리팩토링**

- `EventPublishException`을 `adapter.out.messaging` → `domain.exception`으로 이동 (포트 대칭성 위반 + application 계층이 adapter 패키지를 import해야 하는 의존성 역전 문제 발견 후 수정)

**테스트**

- `KafkaEventPublisherAdapterCircuitBreakerTest` — `@SpringBootTest` + `@MockitoBean(KafkaTemplate)` 조합으로 실제 AOP 프록시를 통과시켜 검증 (`new`로 직접 생성 시 self-invocation과 동일한 이유로 서킷 브레이커가 전혀 개입하지 않는 함정 확인 — `@CircuitBreaker`도 결국 Spring AOP 프록시 기반)
- 실패 10회 누적 시 OPEN 전환 검증
- OPEN 상태에서 `kafkaTemplate.send()` 자체가 호출되지 않음(Fail Fast) 검증
- 전체 테스트(`./gradlew test`) 통과 확인

**트러블슈팅**

- Boot 4.0: `spring-boot-starter-aop`가 `spring-boot-starter-aspectj`로 리네임됨(공식 이슈 #42948). 기존 이름으로 의존성을 추가하면 "버전을 찾을 수 없음" 형태의 에러가 나서 마치 버전 문제처럼 보이지만 실제로는 그 이름의 아티팩트 자체가 더 이상 없는 것 — Kafka starter, webmvc-test 패키지 이동과 동일 계열의 함정
- Boot 4.0: `@MockBean`/`@SpyBean` 완전 제거(3.4부터 deprecated, 4.0에서 삭제), `org.springframework.test.context.bean.override.mockito.MockitoBean`/`MockitoSpyBean`으로 교체 필요

### 과제 13: 복식부기 원장(Double-entry Ledger) 도입 (완료)

브랜치: `feat/double-entry-ledger` → `develop`

**배경**

- 기존 `Account.balance`는 이체마다 직접 +/- 되는 저장 필드(read-modify-write)였음 — 이를 "해당 계좌 `LedgerEntry`의 합"으로 계산되는 파생값(SSOT)으로 전환. 목표는 append-only INSERT만으로 잔액 정합성을 자체 검증 가능하게 만드는 것(대차평형/trial balance 원칙).
- 코드 작성 전에 4가지 설계 결정을 옵션(a/b/c) + 트레이드오프로 먼저 보고하고 사용자 확정을 받은 뒤 구현 — 이번 세션에서 처음으로 "설계 승인 → 구현" 2단계 워크플로를 적용.

**설계 결정**

1. **잔액 계산 시점 — 앵커+델타 하이브리드**: 가장 최근 `EodSnapshot.totalBalance()`를 앵커로 삼고, 그 이후 발생한 `LedgerEntry` 합을 델타로 더함(`AccountBalanceCalculator`). 매일 EOD가 지날 때마다 델타 구간이 리셋되어 스캔 범위가 항상 "최대 하루치"로 bounded됨 — 순수 실시간 전체 합산(스캔 비용 무제한 증가) vs 캐시 필드(SSOT 이원화) 사이 절충안으로 채택.
2. **동시성 제어 — 기존 Redisson 분산 락 유지, DB CHECK 제약 미도입**: 잔액이 LedgerEntry SUM 파생값이 되면 "잔액 음수 금지"는 집계 제약이라 PostgreSQL `CHECK`로 직접 표현 불가(다른 행을 참조/집계 불가) — 트리거로 우회하면 매 이체마다 전체 스캔이 재발해 애초 목표(append-only)와 역행하므로 기각. 락 보유 구간 안에서 애플리케이션 레벨로 계산한 잔액이 요청 금액 이상인지 확인 후 커밋.
3. **마이그레이션 — opening-balance 시딩 후 balance 필드 제거, 순서 강제**: `OpeningBalanceMigrationService`가 기존 balance 값을 보존하는 `OPENING_BALANCE` `LedgerEntry` 쌍(상대계정: `SystemAccounts.OPENING_BALANCE_SOURCE`, accounts 테이블에 실제 row 없는 sentinel)으로 시딩. `LedgerEntry.transferPair`를 재사용해 상대계정 없는 단일 다리 시딩(대차평형 깨짐)을 원천적으로 배제 — 지시서 문구("1건씩")보다 대차평형 원칙을 우선함을 명시적으로 근거 들어 반영.
4. **대차평형 검증 — 거래 단위 즉시(구조적 강제) + 시스템 전체는 EOD 배치**: `LedgerEntry.transferPair(from, to, amount, txId, at)`가 두 다리에 동일한 `amount` 인스턴스를 재사용하는 시그니처라서 합이 0이 아닌 쌍 자체를 만들 수 없음(validate-after가 아닌 invariant-by-construction). 시스템 전체 SUM=0 검증은 `VerifyTrialBalanceUseCase`/`VerifyTrialBalanceService`로 구현해 `EodSettlementJobConfig`의 `eodSettlementJob`에 `trialBalanceVerificationStep`으로 추가 — 불일치 시 `TrialBalanceViolationException`으로 배치 스텝이 실패해 알림.

**추가 반영 (설계 확정 후 리뷰에서 지적됨)**

- `SystemAccounts.OPENING_BALANCE_SOURCE`를 `domain.model`의 SSOT 상수로 추출(`OpeningBalanceMigrationService`/`TransferMoneyService` 공용 참조). 처음엔 accounts 테이블에 해당 row가 "없어서" 조회 실패로 우연히 이체가 막히는 구조였는데, 나중에 어떤 경로로든 이 계좌번호로 실제 Account row가 생기면 방어가 조용히 사라지는 문제가 있어 `TransferMoneyService`에 명시적 가드 클로즈(`assertNotReservedAccount`)를 추가하고 `ReservedAccountException`(`domain.exception`)을 던지도록 변경.

**리팩토링**

- `Account.balance` 저장 필드/`deposit()`/`withdraw()` 제거, 순수 함수 `calculateBalance(anchorBalance, entriesSinceAnchor)`로 전환(포트 의존 없이 도메인 순수성 유지, 앵커·델타 조회는 application 계층의 `AccountBalanceCalculator`가 담당).
- `LockComparisonService`(과제 1-2, 비관적/낙관적/Redisson 3종 락 벤치마크 — 감사에서도 "의도된 인프라 벤치마크 도구"로 재확인된 이력)는 `balance` 컬럼이 사라지면서 전제가 깨져, 락 비교 대상을 신규 `AccountLockAnchorJpaEntity`(`@Version` 보유, 도메인 모델 매핑 없이 인프라 계층에서만 쓰는 전용 엔티티)로 교체해 벤치마크 목적 자체는 그대로 보존. 실제 이체 경로(`TransferMoneyService`, `Withdrawal·DepositParticipantAdapter`)는 여전히 Redisson 분산 락만 사용.

**트러블슈팅**

- 이 프로젝트는 Flyway/Liquibase 없이 `ddl-auto: update`만 사용 — `balance` 필드를 Java 엔티티에서 지워도 Hibernate가 기존 물리 컬럼을 DROP하지 않아, 로컬 Postgres에 남아있던 `NOT NULL balance` 컬럼 때문에 모든 계좌 INSERT가 깨짐(`DataIntegrityViolationException` → 엉뚱하게 `DuplicateAccountNumberException`으로 오역). 컬럼이 비어있음을 확인 후 `ALTER TABLE accounts DROP COLUMN balance`로 직접 정리 — 실제 배포 환경에서는 별도 마이그레이션으로 처리 필요.
- 같은 원리로, 로컬 Postgres에 (당시 develop에는 merge되지 않은) 다른 브랜치의 해시체인 감사로그 스키마 잔재(`outbox_event.entry_hash`/`previous_hash` NOT NULL, `outbox_chain_tail` 테이블)가 남아있어 100스레드 동시성 테스트(`TransferConcurrencyTest`)가 매번 실패 — Redisson 락 문제로 오인하기 쉬운 증상이었으나 원인은 순수 로컬 스키마 drift였음. 동일하게 `ALTER TABLE ... DROP COLUMN` / 잔재 테이블 DROP으로 해결.

**테스트**

- 신규 8개(`LedgerEntryTest`, `AccountBalanceCalculatorTest`, `VerifyTrialBalanceServiceTest`, `TrialBalanceVerificationTaskletTest`, `OpeningBalanceMigrationServiceTest`, `TransferMoneyServiceTest`의 예약 계좌 가드 테스트 2종 포함) + 기존 `balance` API 변경에 따른 11개 파일 수정.
- 전체 테스트(`./gradlew test`) 61개 통과, `./gradlew spotlessCheck` 통과.

### 과제 14: 분산 트레이싱(OpenTelemetry) 도입 (완료)

브랜치: `feat/opentelemetry-tracing` → `develop` (PR #35)

**배경**

- Saga(REQUIRES_NEW로 분리된 여러 빈) + Outbox 저장 + Debezium CDC + Kafka Consumer(재시도 토픽 포함)를 거치는 하나의 이체 요청에서 장애 발생 지점을 추적할 방법이 없었음. Micrometer Tracing(OTel bridge)으로 요청 시작부터 Consumer 처리까지 하나의 trace로 연결하는 것이 목표.
- 코드 작성 전 4가지 설계 결정(계측 방식 / Outbox→Kafka context 전파 방법 / trace_id를 entryHash에 포함할지 / Exporter 목적지)을 옵션+트레이드오프로 먼저 보고하고 사용자 확정 후 구현.

**설계 결정**

1. **계측 방식 — Micrometer Tracing (OTel bridge)**: Boot 4.0.7 네이티브 스택(`spring-boot-starter-opentelemetry`)과 정합적이고, `spring-boot-starter-aspectj`와도 자연스럽게 결합. 다만 실제 구현 단계에서 애초 근거로 들었던 `@Observed` 선언적 계측 대신 `Tracer` API로 span을 수동 생성하는 쪽으로 방향을 바꿈 — 이유는 (a) `DepositParticipantPort.deposit()`이 실제 입금과 보상 트랜잭션 두 지점에서 호출되는데 `@Observed`는 정적 애노테이션이라 같은 메서드의 두 호출을 다른 span 이름으로 구분할 수 없었고, (b) `Tracer`로 직접 감싸면 AOP 프록시 자체를 타지 않아 self-invocation 리스크 카테고리가 통째로 사라지는 부수효과가 있었기 때문. `@Observed`의 선언적 간결함은 포기한 트레이드오프.
2. **Outbox → Debezium CDC → Kafka Consumer 구간 trace context 전파 — 전용 컬럼**: `outbox_event`에 `trace_id`/`span_id` 컬럼을 추가하고 Debezium Outbox EventRouter SMT의 `table.fields.additional.placement`로 Kafka 헤더에 실어 전달, Consumer가 W3C traceparent 형식(`00-{traceId}-{spanId}-01`)으로 재구성해 `Propagator.extract()`로 부모 span을 복원. payload JSON 필드에 넣는 방식(옵션 a)은 entryHash 계산 입력에 payload가 포함되므로 자동으로 옵션 3을 "포함"으로 강제하게 되는 문제가 있어 기각.
3. **entryHash 계산에 trace_id 제외**: 감사로그(entryHash)는 업무적 사실 변조 여부를 증명하는 무결성 대상이고, 트레이스 ID는 샘플링/인프라 설정에 따라 달라질 수 있는 관측성 메타데이터라 목적이 다름 — `OutboxEvent.withTraceContext()`는 entryHash 계산 이후에만 적용해 분리.
4. **Exporter 목적지 — 로컬은 Jaeger, 배포 환경은 Infra 협의 필요**: docker-compose에 Jaeger all-in-one(OTLP 수신) 추가해 즉시 로컬 검증 가능하게 구성. 실제 배포 환경 Prometheus/Grafana 스택과 연동할 OTLP Collector 엔드포인트는 Infra 담당(김준희)과 별도 협의 필요 — 아직 미정.

**검증 (리뷰에서 지적받아 추가로 확인)**

- 자동 리뷰에서 "Debezium이 실제로 DB 컬럼을 Kafka 헤더로 옮겨주는지는 `outbox-connector.json` 설정 파일 하나만 믿고 있는 상태"라는 지적을 받아, 로컬에 Jaeger + Kafka Connect를 직접 띄우고 커넥터를 등록한 뒤 실제 이체 1건을 실행해 확인함:
  - `outbox_event` 테이블에 저장된 `trace_id`/`span_id`가 `kafka-console-consumer --property print.headers=true`로 읽은 실제 `transfer-events` 메시지 헤더 값과 정확히 일치.
  - Jaeger UI(`http://localhost:16686`)에서 `http post /api/v1/transfers` → `outbox.save` → `transfer-event.consume` 3개 span이 동일 trace_id로 연결됨을 실물로 확인. `transfer-event.consume` span은 이후 발견된 `Money` VO Jackson 역직렬화 실패(아래 참고)로 `otel.status_code=ERROR` + 예외 스택트레이스가 함께 기록됨 — 트레이싱이 실제 장애 지점을 정확히 짚어주는 것도 같이 확인됨.
  - 다만 이 확인은 수동 검증이며, 자동화된 통합 테스트(`TransferTraceContinuityIntegrationTest`)는 여전히 `TransferEventConsumer.consume()`을 직접 호출하는 방식이라 Debezium 라우팅 자체는 커버하지 않음 — 기존에 이미 보류 처리된 "실제 Kafka 브로커 기반 통합 테스트" 항목과 같은 종류의 갭이라 다음 작업 backlog에 병기.

**부수 발견 (이번 작업 범위 밖, 미수정)**

- `Money` VO(`domain.model`)에 Jackson creator가 없어 `TransferCompletedEvent`(payload에 `Money` 포함) 실제 역직렬화가 항상 실패함. 기존 테스트가 전부 Mock 기반(`PayloadDeserializerPort`를 목으로 대체)이라 지금까지 드러나지 않았던 것으로 보임 — 실제 배포 환경이라면 `transfer-events` 토픽 메시지가 전부 재시도 후 DLT로 빠지고 있었을 가능성. 별도 이슈로 다뤄야 함.

**테스트**

- 신규 3개(`OutboxPersistenceAdapterTest`, `TransferSagaOrchestratorTest`, `TransferTraceContinuityIntegrationTest`) + 기존 2개(`OutboxEventTest`, `TransferEventConsumerTest`) 확장.
- 전체 테스트(`./gradlew test`) 78개 통과, `./gradlew spotlessCheck` 통과.

### 과제 15: Money VO Jackson 역직렬화 버그 수정 (완료)

브랜치: `fix/money-vo-jackson-deserialization` → `develop`

**근본 원인**

- `Money`(domain.model)는 `private` 생성자만 가진 불변 VO라 Jackson이 기본 전략(무인자 생성자 + setter)으로 역직렬화할 방법이 없었음. `TransferCompletedEvent`가 `Money` 필드를 포함하므로, 이 이벤트를 담은 페이로드는 직렬화(쓰기)는 성공하지만 역직렬화(읽기)는 항상 실패하는 비대칭 구조였음.

**왜 지금까지 안 드러났는지**

- `PayloadDeserializerPort`를 호출하는 쪽(`TransferEventConsumer`)의 기존 테스트가 전부 `PayloadDeserializerPort`를 Mock으로 대체하고 있어서, 실제 `JsonMapper`가 `Money`를 역직렬화하는 경로 자체가 테스트에서 한 번도 실행되지 않았음. Mock 테스트 통과가 "실제 역직렬화가 된다"는 증명이 아니었음.

**영향 범위 추정**

- serialize(쓰기)는 getter만 있으면 되므로 실패하지 않음 — `TransferCompletedEvent`가 도입된 2026-08-05(과제 4, Outbox 통합) 시점부터도 이 부분은 문제없었음.
- 실제로 deserialize(읽기)가 호출되는 지점은 `TransferEventConsumer`뿐이고, 이 컨슈머가 도입된 시점이 2026-08-14(과제 11, Kafka Consumer Retry/DLT 도입). 따라서 실제 배포 환경이었다면 **2026-08-14부터** `transfer-events` 토픽 메시지가 전부 재시도 후 DLT로 빠졌을 것으로 추정 — 과제 14(분산 트레이싱, 2026-08-15) 작업 중 Jaeger 트레이스 실물 확인 과정에서 처음 발견됨(과제 14 "부수 발견" 참고).

**수정 내용**

- 기존 정적 팩토리 `Money.of(BigDecimal)`에 `@JsonCreator`/`@JsonProperty("amount")`(`com.fasterxml.jackson.annotation` — Jackson 3 `tools.jackson.databind`에서도 annotations 모듈은 이 구 패키지를 그대로 씀) 추가. `Money.of()` → private 생성자 → `validate()` 경로를 그대로 타므로 검증 로직(음수/null 금액 방지)을 우회하지 않음.
- 전수조사 결과 `Money`를 필드로 가진 Jackson 직렬화 대상은 `TransferCompletedEvent`가 유일. 웹 DTO(`AccountResponse`, `TransferMoneyRequest`)는 애초에 경계에서 `BigDecimal`로 변환하는 컨벤션이라 동일 버그 클래스에서 벗어나 있음.

**재발 방지책 (테스트)**

- `JacksonPayloadSerializerAdapterTest`에 Mock 없이 실제 `JsonMapper`로 `TransferCompletedEvent`(Money 포함)를 직렬화→역직렬화하는 왕복 테스트 추가 — 이번 버그가 안 잡혔던 이유가 Mock 기반 테스트뿐이었기 때문이므로, 재발 방지의 핵심은 이 실제-왕복 테스트임.
- 음수 금액이 담긴 JSON을 역직렬화했을 때 `InvalidMoneyException`이 원인 체인에 그대로 보존되는지 확인하는 테스트 추가 — creator 애노테이션이 검증 로직을 우회하는 새 생성 경로를 만들지 않았음을 실측으로 확인.

**테스트**

- 신규 2개(`serializeThenDeserialize_transferCompletedEventWithMoney_roundTrip`, `serializeThenDeserialize_moneyWithNegativeAmount_preservesDomainValidationException`).
- 전체 테스트(`./gradlew test`) 80개 통과, `./gradlew spotlessCheck` 통과.

## 🚧 다음 작업

- 트랙 3(장애 복구 & 카오스 엔지니어링) 두 과제(Kafka DLQ, Resilience4j) 완료 — 3개 트랙(실시간 트랜잭션/EOD 배치/장애복구) 모두 핵심 구현 최소 1개 이상 완료.
- (협업 필요) Chaos Mesh 인프라 결함 주입 — 노션 "프로젝트 개요"상 Infra(김준희) 담당 업무. 백엔드가 처음부터 CRD/클러스터까지 다 짜는 게 아니라, "어떤 장애 시나리오로 무엇(서킷 브레이커/재시도 등)을 검증할지"를 먼저 정의해 인프라 담당자와 공유하고, 실제 장애 주입 후 애플리케이션 반응을 검증하는 역할 분담으로 진행할 것
- (보류) 실제 Kafka 브로커 기반 재시도 토픽 → DLT 라우팅 통합 테스트 (과제 11에서 범위 분리)
- (보류) 실제 Kafka 브로커 E2E 수동 검증
- (보류) Debezium EventRouter SMT의 `table.fields.additional.placement`(trace_id/span_id 헤더 라우팅)를 커버하는 자동화된 통합 테스트 — 위 두 항목과 같은 이유(실제 Kafka 브로커 필요)로 보류, 현재는 로컬 수동 검증으로만 확인됨(과제 14 참고)
- (보류) Testcontainers 기반 통합 테스트 재검증 — 프로젝트 전체가 docker-compose 기반 통합 테스트 컨벤션을 일관되게 쓰고 있어 현재는 도입 보류로 결정(Testcontainers는 이 컨벤션과 공존 시 일관성이 깨짐, YAGNI)
- (권장) 실제 배포 대상 Postgres에 `accounts.balance` 컬럼 등 orphan 컬럼이 남아있다면 `ALTER TABLE ... DROP COLUMN`으로 별도 정리 필요(과제 13 참고, 이 프로젝트는 Flyway/Liquibase 미사용)

## 🤖 AI 에이전트(Claude Code) 활용 방침

- 현재는 학습 목적상 위임 범위를 반복적·정형화된 작업(예: package-private 접근제어자 전수 수정, 테스트 코드의 정형화된 반복 패턴 작성)으로 의도적으로 제한 중
- 3개 트랙의 핵심 개념을 충분히 체득한 이후에는, 위임 범위를 넓혀 기능 리팩토링 및 신규 기능 추가까지 AI 에이전트에게 맡길 계획
- 다만 위임 범위가 넓어져도 이번 세션까지 확립된 검증 워크플로는 계속 유지: (1) 작업지시서에 판단 기준과 금지 규칙을 명시 → (2) git diff/파일 트리를 직접 검증 → (3) 기존 컨벤션과 다른 결과물은 근거를 들어 반려 후 재작성 요청

## 📌 트러블슈팅 / 지켜야 할 원칙

- Jackson 3 패키지 경로: `tools.jackson.databind.ObjectMapper`
- `@AutoConfigureMockMvc` 패키지 경로: `org.springframework.boot.webmvc.test.autoconfigure`
- AOP self-invocation 금지 → REQUIRES_NEW는 반드시 별도 스프링 빈으로 분리 (동일 원리가 `@CircuitBreaker`에도 적용됨 — 프록시로 감싸인 빈을 거쳐야만 동작)
- 멀티스레드 테스트 초기화 시 `deleteAllInBatch()` 사용 (`deleteAll()` 금지)
- ShedLock(Redis) 상태도 테스트 간 격리 대상 — `lockAtLeastFor`로 인해 Job 완료 후에도 락이 일정 시간 유지되므로, 해당 스케줄러를 검증하는 테스트는 `@BeforeEach`에서 락 키를 선제적으로 삭제할 것 (JPA의 `deleteAllInBatch()`와 동일한 논리를 Redis 상태에도 적용)
- 낙관적 락 사용 시 `@Version` 값이 도메인 매퍼에서 누락되지 않도록 주의
- 통합 테스트(`@SpringBootTest`)는 Docker(MariaDB/Redis/Kafka)가 떠 있어야 함 → `docker compose up -d` 먼저 확인
- Jackson 3: `ObjectMapper` 대신 불변(immutable) `JsonMapper`가 권장 진입점, Spring Boot가 자동 빈 등록
- `private` 생성자만 가진 불변 VO(예: `Money`)를 Jackson (역)직렬화 대상으로 쓰려면 정적 팩토리에 `@JsonCreator`/`@JsonProperty`를 반드시 지정할 것 — 안 붙이면 serialize(쓰기, getter만 필요)는 성공하고 deserialize(읽기)만 조용히 실패하는 비대칭 버그가 생기고, Mock으로 `PayloadDeserializerPort`를 대체한 테스트로는 절대 못 잡음. Jackson 3에서도 `@JsonCreator`/`@JsonProperty`는 `com.fasterxml.jackson.annotation`(구 패키지) 그대로 사용(과제 15 참고)
- Mockito `@InjectMocks`는 `@Mock` 안 된 생성자 파라미터에 null을 채워 넣으므로, 서비스 생성자 파라미터가 늘어나면 관련 단위 테스트의 `@Mock` 필드도 반드시 같이 추가
- JPA 전용 락(`@Lock(LockModeType...)`)을 다루는 클래스는 `adapter.out.persistence`에 위치
- package-private으로 좁힌 interface는 이를 참조하던 테스트 파일도 같은 패키지로 함께 이동
- Spring Boot 4.0 Kafka 모듈 분리: `KafkaAutoConfiguration`, `KafkaProperties`가 `org.springframework.boot.kafka.autoconfigure` 패키지로 이동. 순수 spring-kafka만 추가하면 이 자동설정 모듈이 클래스패스에 없어 오토와이어링 실패 → 반드시 `spring-boot-starter-kafka(-test)` 사용
- `KafkaProperties.buildProducerProperties()`는 Boot 3.4~3.x대에 SslBundles 인자를 받다가 Boot 4.0에서 다시 무인자로 환원됨 (버전별 시그니처 변경 주의, 도입 시 공식 API 문서 재확인 필수)
- Boot 4.0이 자동생성하는 `KafkaTemplate`은 `<Object,Object>` 타입 고정 — 원하는 제네릭 타입(`<String,String>`)을 쓰려면 `@ConditionalOnMissingBean`을 활용해 직접 빈 정의 필요
- Kafka Producer는 key가 파티셔닝 기준이 되므로, 순서 보장이 필요한 단위(계좌 등 aggregateId)를 key로 반드시 지정
- Kafka 토픽은 브로커 auto-create 기본값에 맡기지 말고 `NewTopic` 빈으로 파티션 수/복제 계수를 명시적으로 생성 (재시도/DLT 토픽도 동일 원칙 — `RetryTopicConfiguration`의 `autoCreateTopics()`로 명시적 생성)
- Kafka Consumer 재시도 예외 분류: 재시도해도 결과가 달라지지 않는 결정론적 실패는 반드시 `NonRetryableEventProcessingException`을 상속해야 하며, 그래야 `KafkaRetryTopicConfig` 재수정 없이 자동으로 즉시 DLT 라우팅 대상에 포함됨(OCP). 상속하지 않은 일반 `RuntimeException`은 기본적으로 재시도 대상으로 간주됨
- Kafka Consumer에서 역직렬화/처리 실패 시 예외를 컨슈머 메서드 내부에서 절대 삼키지 말 것(try-catch로 흡수 금지) — `RetryTopicConfiguration`이 재시도/DLT 여부를 판단하는 유일한 근거가 "리스너 메서드가 던진 예외 타입"이므로, 삼키면 메시지가 정상 처리로 오인되어 에러 로그 없이 조용히 유실됨
- 금액을 다루는 모든 도메인/포트 시그니처는 `BigDecimal`이 아닌 `Money` VO로 통일 (Saga 작업 중 위반 발견 후 전체 전환)
- 도메인 전용 Exception은 `domain.model`이 아닌 `domain.exception` 패키지 소속
- 감사(리뷰) AI가 PROGRESS.md 등 프로젝트 히스토리를 모르는 상태로 점검하면 오탐이 섞일 수 있음 → 리포트는 항상 과거 기록과 대조해서 검증할 것
- package-private 인터페이스 컨벤션: `*JpaRepository`(순수 JPA 구현 세부사항)는 package-private, `*Mapper`(변환 로직)는 public. 신규 리포지토리 생성 시 AI 에이전트가 관성적으로 public interface를 만드는 경우가 있으니 git diff에서 반드시 확인
- 낙관적 락 예외 catch 순서: `ObjectOptimisticLockingFailureException`은 `DataAccessException`의 하위 타입이므로, 어댑터에서 반드시 하위 타입을 먼저 catch할 것 — 순서가 바뀌면 더 구체적인 예외 분기가 영원히 발동하지 않음
- Saga 참여자 어댑터 실패 처리 원칙: 오케스트레이터로는 예외를 절대 전파하지 않고 항상 Result 객체로 수렴 — 알려진 도메인 예외뿐 아니라 예기치 못한 `RuntimeException`까지 잡지 않으면 실패 처리 경로가 두 갈래로 갈라져 saga 정합성이 깨짐
- 터미널 diff 확인 시 잘림 주의: `git diff`/`git status`가 pager나 터미널 버퍼로 인해 일부만 보이는 경우가 있음 — 전체 파일 수(`--stat` 마지막 줄의 "N files changed")와 실제 나열된 파일 수가 일치하는지 항상 대조하고, 의심되면 GitHub 웹의 커밋/PR file tree에서 최종 확인할 것. 페이저(less)에 멈췄을 때는 `git --no-pager diff`로 우회
- Spring Batch 6.0 패키지 재구성: `Job`→`core.job.Job`, `Step`→`core.step.Step`, `JobExecution`→`core.job.JobExecution`로 이동. `JobParameters`/`JobParametersBuilder`→`core.job.parameters`로 이동. `RepeatStatus`는 core 밑도 아닌 완전히 새 모듈 `org.springframework.batch.infrastructure.repeat.RepeatStatus`로 이동. `ItemReader`/`ItemProcessor`/`ItemWriter`/`ExecutionContext` 등 구 spring-batch-infrastructure 모듈 전체가 `org.springframework.batch.infrastructure.*`로 이동. 반대로 `BatchStatus`/`JobRepository`/`JobBuilder`/`StepBuilder`(단, `chunk(size, tm)` 오버로드는 deprecated → `chunk(size).transactionManager(tm)` 사용)/`Tasklet`/`JobOperator`는 패키지 그대로 — 클래스마다 다르므로 6.x 공식 문서(그것도 마일스톤이 아닌 GA 버전 문서인지 확인)로 매번 재확인 필요
- 테스트 유틸 세대교체: `JobLauncherTestUtils`(6.0 deprecated, 6.2+ 제거 예정) → `JobOperatorTestUtils`, `launchJob()` → `startJob()` 원칙이나, `startJob(JobParameters)`에 `@StepScope` 파라미터 전달 버그가 있어(spring-batch#5216) 그 경우엔 예외적으로 상속받은 `launchJob(JobParameters)` 사용
- IntelliJ가 `@SpringBatchTest`로 런타임에 동적 등록되는 빈(`JobOperatorTestUtils` 등)을 정적 분석으로 못 쫓아가 "오토와이어링 불가" 오탐을 낼 수 있음 — 에디터 빨간줄보다 실제 테스트 실행 결과를 우선 신뢰할 것
- 프로덕션 코드는 `JobOperator.start(Job, JobParameters)` 사용(JobLauncher는 6.0부터 deprecated, 6.2+ 제거 예정). 관련 예외(`JobInstanceAlreadyCompleteException` 등)는 `org.springframework.batch.core.launch` 패키지(JobOperator와 동일 패키지)
- ShedLock 공식 프로바이더 중 Redisson 전용은 없음 — Redisson을 쓰는 프로젝트라도 spring-boot-starter-data-redis가 자동 구성해주는 `RedisConnectionFactory` 기반 `shedlock-provider-redis-spring`을 사용
- `TransferEventConsumer`가 Kafka 헤더의 `trace_id`/`span_id`로 W3C traceparent를 조립할 때 sampled flag를 `"01"`(항상 샘플됨)로 하드코딩(`SAMPLED_TRACE_FLAGS`) — 현재 `management.tracing.sampling.probability: 1.0`(100% 샘플링)이라 드러나지 않지만, 나중에 샘플링 확률을 낮추면 producer 쪽에서 "샘플링 안 함"으로 결정한 trace도 consumer가 무조건 "샘플됨"으로 강제 복원하게 됨. 샘플링 확률을 조정할 때는 이 하드코딩도 함께 수정 필요(sampled 여부를 별도 컬럼/헤더로 전달하거나 span context의 실제 sampled 상태를 반영하도록 변경)
- Mockito 가짜 객체도 원본 인터페이스의 checked exception 시그니처를 그대로 물려받음 — `verify(mock).method()`에서도 그 예외 처리가 필요할 수 있음
- K8s Lease API의 리더 선출도 낙관적 락(resourceVersion) 기반 — JPA `@Version` 충돌 처리와 동일한 사고방식으로 접근 가능. 다만 Split-Brain(옛 리더가 죽은 게 아니라 응답만 지연된 경우 짧게 리더가 둘로 보이는 상황) 위험은 DB 유니크 제약 같은 최후 방어선과는 별개로 반드시 고려해야 함 — 유니크 제약은 저장 단계의 중복만 막을 뿐, 그 전 단계(읽기/계산)의 자원 낭비는 못 막음
- `@Bean` 팩토리 메서드가 unchecked exception(`IllegalStateException` 등)을 던질 수 있음 — 특정 checked exception(`IOException` 등)만 잡도록 catch를 좁게 설계하면 실제 라이브러리가 던지는 예외를 못 잡을 수 있으니, "외부 인프라 감지 후 폴백"처럼 의도가 명확한 방어 로직에서는 `Exception`으로 넓게 잡는 것이 오히려 올바른 설계일 수 있음(무분별한 예외 은폐와는 구분할 것 — 로그를 남기고 명시적으로 대체 경로로 전환하는 경우에 한함)
- AI 에이전트(Claude Code)에게 diff를 승인하기 전, 이 프로젝트의 기존 컨벤션(예: 상태 정리는 `@BeforeEach`)과 다른 방식을 제안하면 근거를 들어 반려하고 재작성을 요청할 것 — 에이전트의 해결책이 "동작은 하지만 컨벤션과 다른" 경우가 있으므로 기능적 정합성뿐 아니라 컨벤션 일치 여부도 함께 검증
- Resilience4j 실패 판정: 어댑터가 예외를 던지는 컨벤션이면 `recordExceptions`만으로 충분하지만, boolean 등 반환값으로 성공/실패를 번역하는 컨벤션이면 `CircuitBreakerConfig.Builder.recordResult(Predicate)`로 별도 보강 필요 — 감싸인(wrapped) 예외 타입만 `recordExceptions`에 등록해야 함(원인 예외 타입을 나열해봤자 밖으로 실제로 던져지는 타입이 아니면 무의미)
- Boot 4.0: `spring-boot-starter-aop` → `spring-boot-starter-aspectj`로 리네임(#42948), `@MockBean`/`@SpyBean` 완전 제거 → `@MockitoBean`/`@MockitoSpyBean`(`org.springframework.test.context.bean.override.mockito`) 사용
- 이 프로젝트는 1인 개발이 아니라 Backend(본인)/Infra·SRE(김준희) 2인 협업 프로젝트임 — Chaos Mesh, K8s 클러스터 운영, GitOps/관측성 구축은 Infra 담당 영역이므로, 이런 영역을 "혼자 다 해야 하는지" 판단할 때는 먼저 노션 "프로젝트 개요"의 팀원 역할표를 확인할 것
- Flyway/Liquibase 없이 `ddl-auto: update`만 쓰는 프로젝트에서 엔티티 필드를 제거해도 물리 컬럼은 DROP되지 않고 NOT NULL 제약만 orphan으로 남아 INSERT가 깨질 수 있음 — 로컬 DB에 이전 브랜치/이전 스키마 잔재가 없는지 항상 의심할 것(과제 13)
- 두 값(예: DEBIT/CREDIT 쌍)의 불변식을 지키려면 "따로 만들고 나중에 검증"(validate-after)보다 "애초에 어긋난 값을 만들 수 없는 시그니처"(invariant-by-construction, 예: `LedgerEntry.transferPair`가 두 다리에 동일 `Money` 인스턴스를 강제)가 더 신뢰도 높음
- "존재하지 않아서 우연히 막히는" 방어(예: sentinel 계좌번호가 실제 row가 없어서 조회 실패로 차단됨)는 나중에 그 전제가 깨지면 조용히 무력화되므로, 알아챈 즉시 명시적 가드 클로즈 + 전용 도메인 예외로 전환할 것(과제 13, `ReservedAccountException`)

---

마지막 업데이트: 2026-08-15
