# ARCHITECTURE

## 헥사고날 아키텍처를 선택한 이유

이 프로젝트의 목적은 금융 백엔드의 신뢰성 메커니즘(분산 락, Saga, EOD 배치, 재시도/CDC 기반 이벤트 발행)을 검증하는 것입니다. 검증 대상인 이 메커니즘들은 인프라(DB, Kafka, Redis, K8s)와 강하게 얽히기 쉬운데, 도메인 로직(계좌 잔액 계산, Saga 상태 전이, 이자 계산)이 특정 인프라 구현에 오염되면 "무엇을 검증하는지"와 "어떻게 붙였는지"가 뒤섞여 버립니다. 헥사고날 아키텍처(Ports & Adapters)로 domain을 프레임워크 무의존 상태로 격리하면, 인프라 어댑터를 교체해도(예: MariaDB → PostgreSQL, Redisson → 다른 락 구현) 도메인 로직과 테스트는 그대로 유지됩니다.

**트레이드오프**

- 얻는 것: 도메인 순수성(`domain.model.Account`, `domain.model.Money`는 Spring/JPA 애노테이션이 전혀 없는 순수 Java), Port 인터페이스만 모킹하면 되는 낮은 테스트 비용, 어댑터 단위 교체 가능성(예: `feat/k8s-lease-election`에서 `LeaderElectionPort`를 K8s Java Client 타입 없이 Runnable/Consumer 콜백만으로 정의)
- 치르는 비용: 레이어마다 매핑 계층이 필요(`AccountMapper`, `SagaMapper`, `EodSnapshotMapper`가 도메인 ↔ JPA 엔티티를 매번 변환), 단순 CRUD 하나도 Port In → Service → Port Out → Adapter 4단으로 나뉘는 초기 보일러플레이트, package-private 리포지토리 규칙을 지키려면 Reader 등 일부 어댑터를 프레임워크 제공 구현체(`RepositoryItemReader`) 대신 직접 구현해야 하는 경우가 생김(`AccountItemReader`)

## 패키지 구조

```
com.fbrl
├── domain
│   ├── model       # Account, Money, TransferSaga, SagaStatus, OutboxEvent, EodSnapshot, InterestPolicy, ReconciliationDiscrepancy, ReconciliationStatus, LedgerBalanceDelta
│   ├── exception    # 도메인 전용 예외 (프레임워크 예외 번역 대상)
│   └── event        # TransferCompletedEvent
├── application
│   ├── port.in       # UseCase 인터페이스 (CreateAccountUseCase, TransferMoneyUseCase, VerifyAuditChainUseCase, ...)
│   ├── port.out       # Port 인터페이스 (AccountRepositoryPort, SaveOutboxEventPort, LoadAllOutboxEventsPort, ...)
│   └── service        # UseCase 구현체 (CreateAccountService, TransferSagaOrchestrator, VerifyAuditChainService, ...)
├── adapter
│   ├── in
│   │   ├── web         # AccountController, TransferMoneyController, TransferApprovalController, AuditController, GlobalExceptionHandler
│   │   ├── kafka        # TransferEventConsumer, KafkaRetryTopicConfig
│   │   ├── batch         # EodSettlementJobConfig, AccountItemReader/Processor/Writer, ReconciliationJobConfig/ItemWriter
│   │   └── scheduler      # EodSettlementScheduler, ReconciliationScheduler
│   └── out
│       ├── persistence   # JPA 엔티티/리포지토리/매퍼/영속성 어댑터
│       ├── messaging      # KafkaProducerConfig/TopicConfig (Retry Topic 전용, Port 미구현)
│       ├── participant     # WithdrawalParticipantAdapter, DepositParticipantAdapter (Saga 참여자)
│       ├── kubernetes       # KubernetesLeaderElectionAdapter
│       ├── fraud             # RuleBasedFraudCheckAdapter
│       └── serialization     # JacksonPayloadSerializerAdapter
└── global
    ├── common.annotation   # @DistributedLock, @CheckIdempotency
    ├── common.aop            # DistributedLockAspect, AopForTransaction, IdempotencyAspect
    └── config                 # RedissonConfig, ShedLockConfig, KubernetesApiClientConfig, LeaderElectionProperties
```

의존 방향은 항상 바깥(`adapter`)에서 안쪽(`domain`)입니다. `application`은 `domain`에 의존하고, `adapter`는 `application.port`와 `domain`에 의존하지만 그 역방향 의존은 없습니다.

## 레이어별 책임

- **`domain.model`**: 상태와 불변 규칙을 스스로 캡슐화하는 Rich Domain Model. 예: `Account`는 생성자를 `private`으로 막고 `create()`/`open()`/`reconstruct()` 정적 팩토리만 노출하며, `withdraw()` 내부에서 잔액 부족 검증을 수행. `SagaStatus.canTransitionTo()`는 상태 전이 규칙을 Switch Expression exhaustiveness로 캡슐화.
- **`domain.exception`**: 도메인 전용 예외. `adapter` 계층의 프레임워크 예외가 여기로 번역되어 올라옴.
- **`application.port.in` / `port.out`**: 유스케이스 계약(in)과 외부 의존 계약(out). `port.out` 인터페이스는 프레임워크 타입을 시그니처에 노출하지 않음(예: `LeaderElectionPort`는 K8s Java Client 타입이 등장하지 않음).
- **`application.service`**: Port In 구현체. 트랜잭션 경계와 오케스트레이션 로직이 위치. AOP self-invocation을 피해야 하는 로직(REQUIRES_NEW 등)은 별도 빈으로 분리(`AccountCreationExecutor`, `SagaStateWriter`).
- **`adapter.in`**: 외부 요청을 Port In 유스케이스 호출로 변환하는 진입점(HTTP, Kafka Consumer, Batch Job, Scheduler).
- **`adapter.out`**: Port Out을 구현하는 인프라 연동부. 프레임워크 예외를 도메인 예외로 번역하는 책임이 여기 있음.

## 핵심 기술적 의사결정

### 1. Choreography vs Orchestration Saga

**문제 상황**: 계좌 이체는 출금-입금 두 로컬 트랜잭션으로 나뉘는데, 2PC는 가용성 문제(참여자 중 하나라도 응답 지연 시 전체 락업)가 있어 Saga 패턴이 필요했음.

**대안 비교**: Choreography(각 참여자가 이벤트를 발행/구독하며 다음 단계를 스스로 트리거) vs Orchestration(중앙 오케스트레이터가 상태를 소유하고 각 단계를 지시).

**선택 이유**: 감사 추적성(어떤 이체가 어느 단계에서 실패했는지 한 곳에서 조회 가능) 때문에 Orchestration 채택. `TransferSaga`(`src/main/java/com/fbrl/domain/model/TransferSaga.java`)를 영속화된 상태 머신으로 설계하고, `TransferSagaOrchestrator`(`application.service`)가 `WithdrawalParticipantPort`/`DepositParticipantPort`(`adapter.out.participant`)를 순서대로 호출. 참여자 어댑터는 예외를 오케스트레이터로 전파하지 않고 항상 `Result(success, failureReason)`로 수렴시켜, 실패 처리 경로가 두 갈래로 갈라지는 것을 방지.

### 2. Enum 영속화 방식: `@Enumerated(STRING)` vs `ORDINAL`

**문제 상황**: `SagaStatus`를 JPA 엔티티(`TransferSagaJpaEntity`)에 저장해야 함.

**대안 비교**: `ORDINAL`(선언 순서의 정수 인덱스 저장, 저장 공간 절약) vs `STRING`(enum 이름 문자열 저장).

**선택 이유**: `ORDINAL`은 향후 enum 선언 순서가 바뀌면 기존에 저장된 데이터의 의미가 조용히 오염되는 위험이 있어 `STRING` 채택.

### 3. 분산 스케줄링 3종 병행: ShedLock vs Redisson 분산 락 vs K8s Lease 리더 선출

**문제 상황**: EOD 정산 Job은 다중 인스턴스 환경에서 중복 실행되면 안 됨. 이 프로젝트는 동시성 제어 메커니즘 자체가 검증 대상이므로 하나만 고르지 않고 세 가지 대안을 나란히 구현해 비교하는 것이 목표.

**대안 비교**: ShedLock(선점 후 즉시 skip, 상호 배제 아님) / Redisson(pub-sub 기반 블로킹 대기, 순서대로 다 처리) / K8s Lease API 리더 선출(지속 갱신 기반 단일 리더 고정, resourceVersion 낙관적 락).

**선택 이유**: 세 메커니즘은 대체 관계가 아니라 각기 다른 트레이드오프를 가진 병행 실험 대상. EOD 스케줄러는 `ShedLockConfig`(`global.config`) + `EodSettlementScheduler`(`adapter.in.scheduler`)의 `@SchedulerLock`으로 우선 구현되어 있고, `KubernetesLeaderElectionAdapter`(`adapter.out.kubernetes`)는 `k8s.leader-election.enabled` 플래그로 기본 비활성화된 채 별도 경로로 연동되어 있음(로컬/CI에서 kind 클러스터 없이도 기존 테스트가 깨지지 않도록 `@ConditionalOnProperty` 적용).

### 4. Kafka Non-blocking Retry Topic + DLT

**문제 상황**: `TransferEventConsumer`(`adapter.in.kafka`)에서 이벤트 처리가 실패했을 때, 같은 파티션 내에서 즉시 재시도(blocking retry)하면 실패한 계좌 하나 때문에 무관한 계좌 이벤트까지 전부 발이 묶여 Consumer Lag이 폭증함.

**대안 비교**: Blocking Retry(순서 보장 유지, 처리량 희생) vs Non-blocking Retry Topic(별도 토픽으로 위임, 순서 일부 희생 대신 처리량 유지).

**선택 이유**: "같은 계좌 내 이벤트 순서"보다 "다른 계좌들의 처리량"을 지키는 쪽을 선택. `KafkaRetryTopicConfig`(`src/main/java/com/fbrl/adapter/in/kafka/KafkaRetryTopicConfig.java`)에서 `maxAttempts(4)` + 지수 백오프(1s→2s→4s..., 최대 30s)로 재시도하고, `NonRetryableEventProcessingException`을 상속한 결정론적 실패(`notRetryOn` + `traversingCauses`)는 재시도 없이 즉시 DLT로 라우팅.

### 5. Kafka Retry Topic과 Resilience4j 서킷 브레이커의 역할 분리 — 이후 8번 결정에서 서킷 브레이커 제거됨

**문제 상황**: Kafka 발행(`OutboxPollingScheduler` → `KafkaEventPublisherAdapter`)은 DB 트랜잭션 커밋 이후에 실행되는 post-commit 비동기 경로라, 실패해도 되돌릴 트랜잭션 자체가 없음. Consumer 측 재시도(4번 결정)와는 별개로 "Kafka 자체가 죽었는지"를 판단할 방법이 필요했음.

**대안 비교**: 재시도만으로 대응 vs 서킷 브레이커로 장애 감지 시 시도 자체를 차단(Fail Fast).

**선택 이유(당시)**: 배타적이지 않고 함께 사용. Retry Topic은 "이벤트 단위 재시도", 서킷 브레이커는 "Kafka 생사 판단 후 호출 자체를 차단"으로 역할을 나눔. `KafkaEventPublisherAdapter#publish()`(`adapter.out.messaging`)에 `@CircuitBreaker(name="kafkaEventPublisher", fallbackMethod="publishFallback")`를 적용했고, `fallbackMethod`는 실패를 삼키지 않고 항상 `EventPublishException`을 재던져 `PublishPendingOutboxEventsService`의 "실패 시 `markAsSent()` 호출 안 함" 계약을 보존.

> 8번 결정(PostgreSQL·Debezium CDC 전환)에서 Kafka 발행 자체가 애플리케이션 코드에서 사라지면서 이 문단이 설명하는 컴포넌트(`KafkaEventPublisherAdapter`, 서킷 브레이커 설정)는 모두 삭제됨. Retry Topic(4번 결정)은 Consumer 측 로직이라 그대로 유지.

### 6. 낙관적 락 예외 catch 순서

**문제 상황**: `SagaPersistenceAdapter.save()`에서 JPA `@Version` 충돌과 그 외 인프라 예외를 구분해서 각각 다른 도메인 예외(409/500)로 번역해야 함.

**대안 비교**: catch 순서를 신경 쓰지 않고 상위 타입부터 잡음 vs 하위 타입을 먼저 잡음.

**선택 이유**: `ObjectOptimisticLockingFailureException`은 `DataAccessException`의 하위 타입이므로, `DataAccessException`을 먼저 catch하면 낙관적 락 충돌 분기가 영원히 발동하지 않음. `SagaPersistenceAdapter`(`src/main/java/com/fbrl/adapter/out/persistence/SagaPersistenceAdapter.java`)는 반드시 `ObjectOptimisticLockingFailureException`을 먼저 catch하여 `ConcurrentSagaModificationException`(409)으로, 나머지를 `DataAccessException`으로 잡아 `SagaPersistenceException`(500)으로 번역.

### 7. AOP self-invocation 회피를 위한 빈 분리

**문제 상황**: `@Transactional(REQUIRES_NEW)`, `@DistributedLock`, `@CircuitBreaker`는 모두 Spring AOP 프록시 기반으로 동작하는데, 같은 클래스 내부에서 `this.xxx()`로 호출하면 프록시를 거치지 않아 애노테이션이 무시됨.

**대안 비교**: 하나의 서비스 클래스 안에서 메서드만 분리 vs 별도 스프링 빈으로 분리.

**선택 이유**: 별도 빈 분리만이 실제로 동작함. `AccountCreationExecutor`(`application.service`)는 `createInNewTransaction()`을 `@Transactional(REQUIRES_NEW)`로 별도 빈에 격리했고, `DistributedLockAspect`(`global.common.aop`)는 락 획득 후 실제 트랜잭션 실행을 별도 빈인 `AopForTransaction`에 위임. `@CircuitBreaker`도 한때 동일 원리로 `KafkaEventPublisherAdapter`에 적용해 검증했으나, 8번 결정(PostgreSQL·Debezium CDC 전환)에서 발행 주체 자체가 애플리케이션에서 사라지면서 함께 제거됨.

### 8. PostgreSQL 전환 및 Debezium CDC 도입 — 완료

**문제 상황**: Outbox 패턴(과제 4)을 폴링 방식(`OutboxPollingScheduler`)으로 구현했는데, MariaDB보다 PostgreSQL의 논리적 복제(WAL)를 활용한 Debezium CDC가 폴링 지연/부하 없이 더 실시간에 가까운 발행이 가능함.

**대안 비교**: 기존 MariaDB + 폴링 방식 유지 vs PostgreSQL 전환 + Debezium Outbox Event Router 도입.

**선택 이유**: 헥사고날 구조상 발행 책임을 애플리케이션에서 인프라(Kafka Connect)로 완전히 이전할 수 있어, 폴링 지연·발행 실패 재시도·서킷 브레이커 등 애플리케이션이 떠안던 복잡도를 통째로 제거할 수 있음.

**구현 내용**:
- `OutboxPollingScheduler`, `PublishPendingOutboxEventsUseCase`/구현체, `LoadPendingOutboxEventsPort`, `EventPublisherPort`/`KafkaEventPublisherAdapter`, `EventPublishException`, Resilience4j 서킷 브레이커 설정을 모두 삭제 — 발행 주체가 앱에서 Debezium으로 완전히 이전되어 더 이상 존재 이유가 없는 컴포넌트들.
- `OutboxEvent`는 발행 확인 상태(`Status` enum, `markAsSent()`/`markAsFailed()`)를 제거하고 append-only 로그로 전환 — 전환 후에는 이 상태를 바꾸는 코드가 하나도 남지 않기 때문.
- `outbox_event.payload` 컬럼은 `@Lob` 매핑 시 PostgreSQL에서 `oid`(Large Object) 타입으로 생성되어 논리적 복제로 본문을 캡처할 수 없었음(커넥터를 직접 붙여 검증하다 발견) — `@Column(columnDefinition = "text")`로 변경해 해결.
- Debezium Outbox Event Router는 `route.by.field`/`table.field.event.*` 커넥터 설정으로 `aggregate_type`/`aggregate_id`/`event_type`/`payload` 컬럼명을 그대로 매핑 — 엔티티/DB 스키마 변경 없이 인프라 설정만으로 해결(`debezium/outbox-connector.json`).
- `route.topic.replacement`를 고정값 `transfer-events`로 오버라이드해 기존 `TransferEventConsumer`/Retry Topic 설정을 전혀 건드리지 않음.
- `table.field.event.timestamp`는 사용하지 않음 — `created_at`이 `timestamptz`(Debezium 표현상 STRING)라 이 필드가 요구하는 INT64와 맞지 않아 커넥터가 즉시 실패하는 것을 직접 확인함. 대신 Debezium이 소스 커밋 시각을 자동으로 사용.
- REPLICA IDENTITY는 별도 설정 없이 기본값(PK 기반)으로 충분 — Event Router는 INSERT만 라우팅하므로 UPDATE/DELETE의 이전 값이 필요 없음.
- 커넥터 등록 후 실제 INSERT → `transfer-events` 토픽 수신까지 로컬에서 직접 검증 완료.

### 9. Outbox 해시체인 기반 불변 감사로그

**문제 상황**: `OutboxEvent`는 과제 8에서 append-only 로그로 전환됐지만, "누구도 사후에 내용을 조용히 고칠 수 없다"는 보장까지는 없었음. 감사로그로서 신뢰받으려면 항목 하나라도 변조되면 반드시 감지할 수 있어야 함.

**동시성 제어 대안 비교**: 여러 트랜잭션이 동시에 `OutboxEvent`를 저장할 수 있는데(예: 서로 다른 계좌쌍이 동시에 송금), "직전 해시 조회 → 다음 해시 계산" 구간에 직렬화가 없으면 체인이 갈라지거나 두 항목이 같은 `previousHash`를 참조하며 동시에 커밋될 수 있음.

| 옵션 | 처리량 영향 | 구현 복잡도 | 장애 결합도 |
|---|---|---|---|
| a) Redisson 분산 락(전용 키) | 모든 저장이 이 락 하나에서 경합(기존 계좌별 락과 별개로 추가) | 중간 — 기존 `@DistributedLock`/`AopForTransaction` 패턴 재사용 가능 | Redis 장애 시 감사로그 기록 자체가 막힘 |
| **b) `outbox_chain_tail` 단일 행 + `SELECT ... FOR UPDATE`** | 마찬가지로 전역 경합이지만 Redis 왕복 없이 같은 트랜잭션 내에서 처리 | 낮음 — 이미 열려있는 REQUIRES_NEW 트랜잭션 안에 자연스럽게 포함 | 없음 — PostgreSQL 하나로 완결 |
| c) `aggregateType`별 체인 분리 | 이론상 유리하나 현재 `aggregateType`이 `"Account"` 하나뿐이라 오늘 기준 효과 없음 | 낮음 | 없음, 다만 "전역 무결성"이 "타입별 무결성"으로 약화됨 |

**선택 이유**: b) 채택(사용자 확인 후 진행). `outbox_event` 테이블의 "마지막 행"에 직접 `FOR UPDATE`를 거는 방식은 두 트랜잭션이 같은 "현재 최댓값" 행을 각각 조회해 잠그려는 팬텀 위험이 있어, 전용 단일 행 tail-pointer 테이블(`outbox_chain_tail`)이 필요함. 이미 outbox insert가 `AopForTransaction`이 연 REQUIRES_NEW 트랜잭션 안에서 일어나므로, 별도 시스템(Redis) 없이 같은 DB 트랜잭션에 원자적으로 편입 가능.

**구현 내용**:
- `OutboxEvent`(`domain.model`)에 `previousHash`/`entryHash` 필드 추가. 해시는 Jackson 직렬화 대신 `(aggregateType, aggregateId, eventType, payload, createdAt.toString(), previousHash)`를 `"|"`로 명시적으로 이어붙여 SHA-256으로 계산(`recomputeEntryHash()`) — 라이브러리 버전에 따라 직렬화 포맷이 달라지면 검증 재현성이 깨지기 때문. `id`는 DB IDENTITY라 해시 계산 시점(insert 이전)엔 알 수 없어 해시 입력에서 의도적으로 제외.
- 제네시스(체인 첫 항목)의 `previousHash`는 `null` 대신 64자리 `"0"` 문자열 상수(`OutboxEvent.GENESIS_PREVIOUS_HASH`)로 표현 — DB 컬럼을 NOT NULL로 유지하고, 검증 로직에서 "null이면 특별 취급"하는 분기를 없애기 위함.
- `OutboxPersistenceAdapter.save()`(`adapter.out.persistence`)가 저장 시 `outbox_chain_tail`을 `SELECT ... FOR UPDATE`로 잠그고 `previousHash`를 확정한 뒤 insert, 커밋 시점에 tail을 갱신. 최초 기동 시 tail 행이 없는 부트스트랩 경합은 `INSERT ... ON CONFLICT DO NOTHING`으로 제거(항상 로우가 있는 상태를 보장한 뒤 잠금).
- `VerifyAuditChainUseCase`/`VerifyAuditChainService`(`application`)와 `GET /api/v1/audit/verify`(`adapter.in.web.AuditController`) 추가 — 전체 체인을 id 오름차순으로 순회하며 `previousHash` 연결과 `entryHash` 재계산 일치 여부를 검증하고, 불일치 시 끊어진 `id`와 사유를 반환.
- `OutboxChainConcurrencyTest`로 서로 다른 계좌쌍 50건 동시 송금(기존 계좌별 Redisson 락으로는 서로 경합하지 않도록 의도적으로 분리) 시 체인이 갈라지지 않고 정확히 이어짐을 검증(entryHash/previousHash 각 50개 모두 유일).
- 실제로 앱을 띄워 송금 2건 후 검증 API가 `valid=true`를 반환하는 것과, DB에서 직접 `payload`를 변조한 뒤 `entryHash` 불일치로 정확한 `id`에서 감지되는 것까지 직접 확인함.
- Debezium 커넥터(`debezium/outbox-connector.json`)는 변경 없음 — 매핑 대상 컬럼(`aggregate_type`/`aggregate_id`/`event_type`/`payload`)이 그대로라 새 컬럼(`previous_hash`, `entry_hash`)은 CDC 라우팅에 영향 없음.

### 10. Account.balance를 LedgerEntry 기반 파생값으로 전환 (복식부기 원장) — 완료

**문제 상황**: `Account.balance`가 이체마다 직접 +/- 되는 저장 필드(read-modify-write)였음. 잔액 정합성을 "언제든 자체 검증 가능한" 형태로 만들려면 append-only INSERT만으로 잔액이 결정되어야 하는데, 저장 필드 방식은 그 자체로는 자기 정합성을 증명할 수 없음(값이 맞는지 별도 검증 로직 없이는 알 수 없음).

**대안 비교**: (1) 잔액 계산 시점 — 매 조회마다 전체 `LedgerEntry` 합산 vs 캐시 필드 유지 + 사후 검증 vs 앵커(EodSnapshot)+델타 하이브리드. (2) 잔액 부족 검증의 동시성 제어 — 기존 Redisson 분산 락 재사용 vs DB CHECK 제약 추가. (3) 기존 balance 마이그레이션 — 즉시 제거 vs opening-balance `LedgerEntry` 시딩 후 제거. (4) 대차평형 검증 시점 — 매 이체 후 즉시 전체 스캔 vs EOD 배치 vs 거래 단위 구조적 보장 + 시스템 전체는 배치.

**선택 이유**: 앵커+델타 하이브리드는 스캔 범위를 "최대 하루치"로 bounded시키면서도 캐시 이원화(SSOT 붕괴)를 피함. DB CHECK 제약은 잔액이 SUM 파생값이 되는 순간 집계 제약이라 순수 CHECK로 표현 불가(트리거로 우회하면 매 이체마다 전체 스캔이 재발해 append-only 목표와 정면 충돌)라 기각하고 기존 Redisson 락 안에서 애플리케이션 레벨로 검증. opening-balance 시딩은 상대계정(`SystemAccounts.OPENING_BALANCE_SOURCE`) 없이 단일 다리로 넣으면 시스템 전체 대차평형이 영구히 깨지므로, 기존 `LedgerEntry.transferPair`를 재사용해 페어로 시딩. 대차평형은 거래 단위(두 다리 합=0)는 `transferPair`의 시그니처 자체로 구조적 보장(invariant-by-construction)하고, 시스템 전체 합=0은 매 이체마다 전체 스캔할 필요 없이 EOD 배치에서 저빈도로 검증.

**구현 내용**:
- `LedgerEntry`(`domain.model`, record) — accountNumber/direction(DEBIT/CREDIT)/amount(`Money`)/transactionId/occurredAt, `@Version` 없음(불변·append-only). `transferPair(from, to, amount, txId, at)`가 두 다리에 동일 `amount` 인스턴스를 재사용해 합이 0이 아닌 쌍 자체를 생성 불가능하게 만듦.
- `Account.balance` 저장 필드/`deposit()`/`withdraw()` 제거 → 순수 함수 `calculateBalance(anchorBalance, entriesSinceAnchor)`로 전환(포트 의존 없이 도메인 순수성 유지). 앵커(`EodSnapshot.totalBalance()`) + 델타(`LedgerEntry` since 앵커 `computedAt`) 조회·조합은 application 계층의 `AccountBalanceCalculator`가 전담 — port.in으로 노출하지 않고 `AccountCreationExecutor`와 동일하게 여러 서비스가 재사용하는 내부 헬퍼로 위치.
- 시스템 전체 대차평형 검증은 `VerifyTrialBalanceUseCase`/`VerifyTrialBalanceService`(port.in에 검증 결과 record를 중첩시키는 컨벤션)로 구현, `EodSettlementJobConfig`의 `eodSettlementJob`에 `trialBalanceVerificationStep`(Tasklet)으로 연결 — 불일치 시 `TrialBalanceViolationException`으로 배치 스텝 실패.
- 예약 계좌 보호: 계좌 미존재로 "우연히" 막히던 `SystemAccounts.OPENING_BALANCE_SOURCE` 이체 시도를 `TransferMoneyService`에서 `assertNotReservedAccount` 가드로 명시화하고 `ReservedAccountException`을 던지도록 전환 — 나중에 그 계좌번호로 실제 Account row가 생기더라도 방어가 유지됨.
- `LockComparisonService`(비관적/낙관적/Redisson 3종 락 벤치마크, 과제 1-2)는 `balance` 컬럼 제거로 전제가 깨져 락 비교 대상을 `AccountLockAnchorJpaEntity`(도메인 매핑 없는 인프라 전용 엔티티)로 교체 — 벤치마크 목적은 유지, 실제 이체 경로 동시성 제어는 여전히 Redisson 분산 락 단독.
- Flyway/Liquibase 미사용(`ddl-auto: update`) 환경이라 `balance` 컬럼 제거는 애플리케이션 매핑 차원의 변경일 뿐 물리 컬럼은 자동 DROP되지 않음 — 실제 배포 시 별도 `ALTER TABLE ... DROP COLUMN` 마이그레이션 필요.

### 11. 분산 트레이싱(OpenTelemetry) 도입 — 완료

**문제 상황**: 이체 1건이 REQUIRES_NEW로 분리된 여러 스프링 빈(Saga 참여자), Outbox 저장, Debezium CDC, Kafka Consumer(재시도 토픽 포함)를 거치는 동안 인과관계를 추적할 방법이 없었음. 특히 Outbox → Debezium CDC → Kafka Consumer 구간은 애플리케이션 코드가 아니라 DB WAL을 거쳐가므로, 일반적인 Kafka producer의 trace context header 전파 방식이 그대로 통하지 않음.

**대안 비교**:

| 항목 | 옵션 | 선택 |
|---|---|---|
| 계측 방식 | (a) Micrometer Tracing(ObservationRegistry/`@Observed`) (b) OTel Java Agent(바이트코드 자동계측) (c) 순수 OTel SDK 수동계측 | **(a)** — Boot 4.0.7 네이티브 스택과 정합적. 단, `DepositParticipantPort.deposit()`이 실제 입금·보상 트랜잭션 두 곳에서 호출되어 `@Observed`(정적 애노테이션)로는 두 호출을 구분할 수 없어, 실제 구현은 `Tracer` API 수동 계측으로 전환(부수효과로 AOP 프록시를 타지 않아 self-invocation 리스크 카테고리 자체가 사라짐) |
| Outbox→Kafka context 전파 | (a) traceparent를 payload JSON 필드에 포함 (b) `trace_id`/`span_id` 전용 컬럼 (c) 트레이스 단절 + Span Link만 연결 | **(b)** — `outbox_event`에 전용 컬럼 추가 → Debezium Outbox EventRouter SMT의 `table.fields.additional.placement`로 Kafka 헤더 라우팅 → Consumer가 W3C traceparent(`00-{traceId}-{spanId}-01`)로 재구성해 `Propagator.extract()`로 부모 span 복원. (a)는 payload가 entryHash 계산 입력이라 자동으로 "trace_id를 해시에 포함"을 강제하게 되어 기각 |
| entryHash 계산에 trace_id 포함 여부 | (a) 포함 (b) 제외 | **(b)** — 감사로그(entryHash)는 업무적 사실 변조 여부를 증명하는 무결성 대상이고, trace_id는 샘플링/인프라 설정에 따라 달라지는 관측성 메타데이터라 목적이 다름. `OutboxEvent.withTraceContext()`는 entryHash 계산 이후에만 적용 |
| Exporter 목적지 | (a) 콘솔 로깅 (b) 로컬 Jaeger(docker-compose) (c) 배포 환경 OTLP Collector 연동 | **(b)** 우선 적용, (c)는 Infra(김준희) 협의 필요 항목으로 별도 관리 — 배포 환경 Prometheus/Grafana 스택과 연동할 OTLP Collector 엔드포인트는 아직 미정 |

**구현 내용**:
- `TransferSagaOrchestrator`(출금/입금/보상 각 단계), `OutboxPersistenceAdapter.save()`(`outbox.save` span), `TransferEventConsumer.consume()`(재시도 토픽 리스너 포함, 같은 리스너 메서드를 공유)에 `io.micrometer.tracing.Tracer`를 직접 주입해 span 생성 — AOP 미사용으로 self-invocation 위험 자체를 회피.
- `OutboxEvent`(`domain.model`)에 `traceId`/`spanId` 필드 추가(순수 `String`, 프레임워크 의존 없음) — 도메인 계층에 트레이싱 관련 코드가 유입되지 않도록 값 자체만 보유.
- `HTTP` 진입점(`AuditController` 등)은 `spring-boot-starter-opentelemetry` 추가만으로 Spring MVC 자동계측 대상에 이미 포함됨을 실측 확인(별도 코드 변경 불필요).
- 로컬에 Kafka Connect + Debezium 커넥터를 실제로 등록하고 이체를 실행해, `outbox_event.trace_id`/`span_id`가 실제 `transfer-events` 토픽 메시지 헤더와 정확히 일치하고, Jaeger UI에서 `http post /api/v1/transfers` → `outbox.save` → `transfer-event.consume` 3-span이 동일 trace_id로 이어짐을 확인. 다만 이 검증은 수동이며, 자동화된 통합 테스트(`TransferTraceContinuityIntegrationTest`)는 `TransferEventConsumer.consume()`을 직접 호출하는 방식이라 Debezium 라우팅 자체는 커버하지 않음(기존에 이미 보류 처리된 "실제 Kafka 브로커 기반 통합 테스트"와 같은 종류의 갭).

### 12. 룰 기반 이상거래 탐지 — 판정 로직을 도메인/어댑터 어디에 둘지

**문제 상황**: `TransferMoneyController → TransferMoneyService`(직접 이체)와 `ApproveTransferService → TransferMoneyService`(Maker-Checker 승인 후 트리거) 두 경로가 모두 실제 자금 이동을 발생시키는데, Maker-Checker 승인 게이트(`TransferMoneyController.assertApprovalNotRequired()`)는 컨트롤러에만 있어 승인 경로가 이를 구조적으로 우회함(이 게이트는 우회돼도 문제 없음 — 이미 승인된 이체를 재차 막으면 안 되므로 의도된 설계). 이상거래 탐지는 반대로 두 경로 모두에서 빠짐없이 적용돼야 하므로, 같은 실수(게이트를 한쪽 진입점에만 두는 것)를 반복하지 않는 것이 설계 목표였음.

**위치 대안 비교**: (a) `TransferMoneyController`(승인 게이트와 동일 위치, 승인 경로 우회됨) vs (b) `TransferMoneyService.transfer()` 내부(두 진입점의 유일한 합류점) vs (c) `@FraudCheck` 커스텀 애노테이션 + AOP(관심사 분리는 되지만 `@Order` 설정을 잘못하면 기존 `@DistributedLock` 바깥에서 돌아 카운팅 룰 도입 시 동시성 경합 재노출 위험).

**선택 이유**: (b) 채택. `TransferMoneyService.transfer()`는 이미 `@DistributedLock(key = "#command.senderAccountNumber")`로 발신 계좌 단위 상호배제가 걸려 있어, 판정 로직을 이 메서드 초입(`assertNotReservedAccount`와 같은 위치대)에 두면 신규 락 없이 기존 락에 자연히 편승한다는 이점도 있음.

**판정 로직의 계층 배치 — 리뷰 지적 사항**: 최초 구현은 임계치 비교(`amount.isGreaterThanOrEqual(threshold)`)를 `RuleBasedFraudCheckAdapter`(`adapter.out.fraud`, 인프라 계층) 안에 직접 작성했음. "임계치/규칙 비교" 성격의 로직을 `ApprovalPolicy`처럼 항상 프레임워크 무의존 `domain.model`에 둬온 이 프로젝트의 컨벤션과 다른 배치였고, Port(`FraudCheckPort`)로 감싼 의도(향후 어댑터를 외부 룰엔진/ML 기반으로 교체 가능하게)와도 맞지 않음 — "무엇이 의심거래인가"라는 업무 규칙이 어댑터 소유가 되면 어댑터 교체 시 그 규칙까지 함께 사라짐. 리뷰 지적 후 `domain.model.FraudPolicy(Money threshold)`(record, `isSuspicious(Money amount)`)를 신설해 `ApprovalPolicy`와 동형으로 맞추고, `RuleBasedFraudCheckAdapter`는 이 정책 객체에 위임만 하도록 축소. `FraudConfig`도 `Money` 타입 빈을 직접 노출하던 것(향후 다른 `Money` 빈과 타입 충돌 위험)을 `FraudPolicy` 빈으로 교체.

**구현 내용**:
- `application.port.out.FraudCheckPort` — `boolean isSuspicious(String accountNumber, Money amount)`. `DepositParticipantPort`/`WithdrawalParticipantPort`처럼 도메인 엔티티 전체가 아닌 최소 파라미터만 받음. 이 두 참여자 포트가 `Result(boolean, failureReason)`를 쓰는 것과 달리 `boolean` 단독 반환을 택한 이유: Result 패턴은 "예외를 오케스트레이터로 전파하지 않는" Saga 참여자 특유의 제약 때문인데, 이상거래 탐지는 판정 실패 시 즉시 예외를 던지는 설계라 그 전제가 다름.
- `domain.exception.SuspiciousTransferException` — 판정 실패 시 던져지는 도메인 예외, `GlobalExceptionHandler`가 400으로 매핑.
- `global.config.FraudPolicyProperties`(`@ConfigurationProperties(prefix="fraud")`) / `FraudConfig` — `ApprovalPolicyProperties`/`ApprovalConfig`와 동형으로 `application.yaml`의 `fraud.threshold`를 `FraudPolicy` 빈으로 조립.
- `ApproveTransferTriggersFraudCheckIntegrationTest`(`@SpringBootTest`)로 Mock 없이 승인 경로에서도 threshold 이상 금액이 실제로 차단되고 잔액이 이동하지 않는지 검증 — Maker-Checker 승인 게이트가 겪었던 우회 사고의 재발 방지 확인.

> PROGRESS.md 기준으로 이 결정은 과제 21에 해당하며, 그 사이 과제 13~15(PostgreSQL 전환, Debezium CDC 전환, 해시체인 감사로그)와 과제 20(거절 사유 조회 API)도 함께 복원·기록되었습니다. 자세한 내용은 [`PROGRESS.md`](./PROGRESS.md)를 참고하세요.

### 13. EOD 정산 대사(Reconciliation) 엔진 도입 — 완료

**문제 상황**: 과제 10(복식부기 원장)에서 이미 `VerifyTrialBalanceService`/`TrialBalanceVerificationTasklet`으로 "시스템 전체 `LedgerEntry` 차변합=대변합" 검증이 `eodSettlementJob` 안에 존재해, 신규 대사(Reconciliation) 기능이 이것과 중복인지부터 확인이 필요했음. 조사 결과 trial balance는 시스템 전체 스칼라 2개(전체 차변/대변 합)만 비교하고 `EodSnapshot`을 전혀 참조하지 않는 반면, 대사 엔진이 검증해야 할 대상은 "`AccountBalanceCalculator`가 상시 사용하는 앵커(`EodSnapshot`) 캐시가 원장(`LedgerEntry`) 원본과 실제로 일치하는가"라는 계좌 단위의 다른 질문이라 중복이 아님으로 판단.

**대사 대상 옵션 비교**: (a) 계좌별 `EodSnapshot`(앵커) vs 해당 계좌의 genesis부터 `LedgerEntry` 전량 재계산 — 앵커+델타 최적화 경로가 실제로 원장과 어긋나지 않았는지 검증하는 유일한 방법 (b) 계좌별 잔액 합계 vs 시스템 전체 trial balance 재검증 — 수학적으로 `transferPair`의 구조적 보장과 거의 항상 같은 값이라 새로운 결함을 잡을 확률이 낮음 (c) 외부/레거시 코어뱅킹 산출물 vs 원장 — 이 프로젝트엔 그런 라이브 외부 소스가 현재 존재하지 않아(과제 10에서 `accounts.balance` 컬럼 자체가 제거됨) 이번 스코프에서 인프라가 없음. **(a) 채택**.

**계좌별 EodSnapshot 특정 방식 옵션 비교**: (1) 정확히 오늘 날짜(`settlementDate`) 스냅샷만 대상, 없으면 "스냅샷 없음"으로 명시 분류(불일치 아님) (2) 계좌별 "가장 최근" 스냅샷을 날짜 무관하게 사용. **(1) 채택** — (2)는 EOD가 며칠간 실패해도 예전 스냅샷이 "존재한다"는 우연한 사실만으로 계속 초록불을 켜주는, 과제 10의 `ReservedAccountException`(존재하지 않아서 우연히 막히는 방어) 사례와 대칭되는 "존재해서 우연히 통과하는" 문제가 있어 기각. `LoadEodSnapshotByDatePort`로 날짜 조건을 명시적으로 강제.

**불일치 처리 및 배치 위치**: trial balance(시스템 전체 불변식 위반=심각한 버그이므로 배치 즉시 실패가 타당)와 달리, Reconciliation은 계좌 단위 부분 실패라 같은 패턴(예외로 배치 실패)을 그대로 적용하면 계좌 1건 때문에 EOD 정산 전체가 막힘 — 대신 `ReconciliationDiscrepancy`(write-once, `@Version` 없음, `EodSnapshot`과 동일한 append-only 산출물 성격) 별도 테이블에 `MISMATCH`/`NO_SNAPSHOT`만 기록(`MATCH`는 저장하지 않음 — 전수 검사 건수는 Spring Batch `StepExecution`의 read/write count로 충분). Job도 `eodSettlementJob`과 완전히 분리한 별도 Job(`reconciliationJob`)으로 두어, 대사 비용이 EOD 크리티컬 패스(이자 계산·스냅샷 저장)를 지연시키지 않도록 함. 운영자 "확인 처리(resolved)" 워크플로는 현재 이 프로젝트에 그런 기능/화면이 전혀 없어 YAGNI로 제외 — 필요해지면 `EodSnapshot`처럼 정정을 새 레코드 추가로 표현.

**청크 단위 배치 조회 설계**: `AccountItemReader`(과제 10에서 이미 `LoadAllAccountsPort.loadAccounts(page, size)`로 `findAll()` 없이 페이징하던 것) 그대로 재사용하되, 초기 설계는 `ItemProcessor`가 계좌 1건마다 스냅샷/원장 델타를 개별 쿼리하는 구조였음 — 청크 크기(1000)만큼 청크당 N번 왕복이 발생하는 문제를 사용자가 지적해, 무거운 조회를 `ItemProcessor`가 아니라 청크 전체(`List<Account>`)를 한 번에 받는 `ItemWriter`(`EodSnapshotItemWriter`가 이미 쓰는 것과 동일한 위치)로 이동. `LoadEodSnapshotByDatePort.loadByAccountNumbersAndDate(List, LocalDate)`와 `LoadLedgerBalanceDeltasPort.loadBalanceDeltasUntil(List, Instant)` 모두 청크당 1쿼리로 수렴.

원장 델타 조회는 애초에 `List<LedgerEntry>` 원본 행을 그대로 배치 `IN` 조회하는 형태로 설계했으나, 계좌당 거래 건수가 무제한이라 거래량 많은 계좌가 청크에 섞이면 결과 크기가 다시 unbounded해지는(`findAll()` 금지와 같은 유형의) 문제가 있어, SQL `GROUP BY account_number`로 계좌별 신용/차변 합계까지 미리 집계해 반환하도록 재설계(`LedgerBalanceDelta(creditTotal, debitTotal)`, 결과 크기가 청크당 계좌 수로 bounded). 순델타(credit-debit)를 SQL에서 미리 빼서 단일 `Money`로 반환하지 않고 신용/차변을 분리 반환한 이유: `Money`는 음수를 금지하는데(`Money.validate()`), 순델타는 특정 계좌에서 차변이 신용보다 커지는 상황(정상적으로는 발생하지 않지만 실제 원장 불변식이 깨졌을 때는 발생 가능)에 음수가 될 수 있어 그대로 `Money`로 감싸면 생성자가 예외를 던져 대사 배치 자체가 죽음.

**Job 파라미터 설계**: Reconciliation은 EOD와 달리 원장 재계산의 상한 시각(`asOf`)이 필요 — 하한 없이(genesis부터) 상한만 두면 EOD 스냅샷 계산 시점과 Reconciliation 실행 시점 사이에 발생한 정상 거래까지 재계산에 포함되어 거짓 `MISMATCH`가 발생하기 때문. `ReconciliationScheduler`가 트리거 시각을 `asOf` 파라미터로 넘기는데, 이 값은 매 실행마다 달라지므로 `JobParametersBuilder.addString(key, value, false)`로 **non-identifying**으로 지정 — 그러지 않으면 Job 인스턴스 식별이 매번 달라져 EOD와 동일하게 갖고 있어야 할 "당일 재실행 스킵" 보호(`JobInstanceAlreadyCompleteException`)가 조용히 무력화됨.

**expectedBalance 계산 버그 및 수정 (사용자 확인 중 발견)**: 최초 구현은 `expectedBalance`로 `EodSnapshot.totalBalance()`(`closingBalance + interestAmount`)를 사용했음. `AccountInterestItemProcessor`가 계산하는 `interestAmount`는 스냅샷 필드에만 기록될 뿐 `LedgerEntry`로 전혀 적립되지 않는데(grep으로 확인, `transferPair`/`SaveLedgerEntryPort` 사용처 어디에도 이자 적립 로직 없음), `actualBalance`는 원장만 재계산한 값이라 이자를 포함하지 않음 — 결과적으로 이자가 0이 아닌 사실상 모든 계좌가 매일 `MISMATCH`로 오탐되는 버그였음. 이게 안 걸린 이유는 초기 단위 테스트들이 이자를 전부 `Money.ZERO`로 고정해 시나리오 자체를 가렸기 때문. `expectedBalance`를 이자 제외 `EodSnapshot.closingBalance()`로 교체하고, 이자가 0이 아닌 케이스로 테스트를 재작성해 회귀 방지.

**구현 내용**:
- Port 3종(`application.port.out`): `LoadEodSnapshotByDatePort`, `LoadLedgerBalanceDeltasPort`, `SaveReconciliationDiscrepancyPort`.
- `domain.model.ReconciliationDiscrepancy`(record, write-once) — `expectedBalance`(`Money`, nullable)/`actualBalance`(`Money`, nullable — `MISMATCH`만 채워짐)/`status`(`ReconciliationStatus`: `MISMATCH`/`NO_SNAPSHOT`), `domain.model.LedgerBalanceDelta`(record, `creditTotal`/`debitTotal` 둘 다 `Money`).
- `adapter.out.persistence.ReconciliationDiscrepancyJpaEntity` — `(account_number, settlement_date)` 유니크 제약, `@Version` 없음(write-once). `*JpaRepository`는 package-private, `*Mapper`는 public 컨벤션 유지. `DataIntegrityViolationException`은 `DuplicateReconciliationDiscrepancyException`으로 번역.
- `adapter.in.batch.ReconciliationJobConfig`/`ReconciliationItemWriter` — 별도 `reconciliationJob`, reader는 기존 `AccountItemReader` 재사용(새 `@StepScope` 빈으로만 재선언).
- `adapter.in.scheduler.ReconciliationScheduler` — `${reconciliation.batch.cron:0 0 3 * * *}`(EOD 이후 시각), ShedLock `@SchedulerLock(name = "reconciliationJob")`.

---

각 결정의 배경/트러블슈팅 전체 기록은 [`PROGRESS.md`](./PROGRESS.md)를 참고하세요.
