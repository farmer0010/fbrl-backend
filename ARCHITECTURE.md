# ARCHITECTURE

## 헥사고날 아키텍처를 선택한 이유

이 프로젝트의 목적은 금융 백엔드의 신뢰성 메커니즘(분산 락, Saga, EOD 배치, 재시도/서킷 브레이커)을 검증하는 것입니다. 검증 대상인 이 메커니즘들은 인프라(DB, Kafka, Redis, K8s)와 강하게 얽히기 쉬운데, 도메인 로직(계좌 잔액 계산, Saga 상태 전이, 이자 계산)이 특정 인프라 구현에 오염되면 "무엇을 검증하는지"와 "어떻게 붙였는지"가 뒤섞여 버립니다. 헥사고날 아키텍처(Ports & Adapters)로 domain을 프레임워크 무의존 상태로 격리하면, 인프라 어댑터를 교체해도(예: MariaDB → PostgreSQL, Redisson → 다른 락 구현) 도메인 로직과 테스트는 그대로 유지됩니다.

**트레이드오프**

- 얻는 것: 도메인 순수성(`domain.model.Account`, `domain.model.Money`는 Spring/JPA 애노테이션이 전혀 없는 순수 Java), Port 인터페이스만 모킹하면 되는 낮은 테스트 비용, 어댑터 단위 교체 가능성(예: `feat/k8s-lease-election`에서 `LeaderElectionPort`를 K8s Java Client 타입 없이 Runnable/Consumer 콜백만으로 정의)
- 치르는 비용: 레이어마다 매핑 계층이 필요(`AccountMapper`, `SagaMapper`, `EodSnapshotMapper`가 도메인 ↔ JPA 엔티티를 매번 변환), 단순 CRUD 하나도 Port In → Service → Port Out → Adapter 4단으로 나뉘는 초기 보일러플레이트, package-private 리포지토리 규칙을 지키려면 Reader 등 일부 어댑터를 프레임워크 제공 구현체(`RepositoryItemReader`) 대신 직접 구현해야 하는 경우가 생김(`AccountItemReader`)

## 패키지 구조

```
com.fbrl
├── domain
│   ├── model       # Account, Money, TransferSaga, SagaStatus, OutboxEvent, EodSnapshot, InterestPolicy
│   ├── exception    # 도메인 전용 예외 (프레임워크 예외 번역 대상)
│   └── event        # TransferCompletedEvent
├── application
│   ├── port.in       # UseCase 인터페이스 (CreateAccountUseCase, TransferMoneyUseCase, ...)
│   ├── port.out       # Port 인터페이스 (AccountRepositoryPort, EventPublisherPort, ...)
│   └── service        # UseCase 구현체 (CreateAccountService, TransferSagaOrchestrator, ...)
├── adapter
│   ├── in
│   │   ├── web         # AccountController, TransferMoneyController, GlobalExceptionHandler
│   │   ├── kafka        # TransferEventConsumer, KafkaRetryTopicConfig
│   │   ├── batch         # EodSettlementJobConfig, AccountItemReader/Processor/Writer
│   │   └── scheduler      # EodSettlementScheduler, OutboxPollingScheduler
│   └── out
│       ├── persistence   # JPA 엔티티/리포지토리/매퍼/영속성 어댑터
│       ├── messaging      # KafkaEventPublisherAdapter, KafkaProducerConfig/TopicConfig
│       ├── participant     # WithdrawalParticipantAdapter, DepositParticipantAdapter (Saga 참여자)
│       ├── kubernetes       # KubernetesLeaderElectionAdapter
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

### 5. Kafka Retry Topic과 Resilience4j 서킷 브레이커의 역할 분리

**문제 상황**: Kafka 발행(`OutboxPollingScheduler` → `KafkaEventPublisherAdapter`)은 DB 트랜잭션 커밋 이후에 실행되는 post-commit 비동기 경로라, 실패해도 되돌릴 트랜잭션 자체가 없음. Consumer 측 재시도(4번 결정)와는 별개로 "Kafka 자체가 죽었는지"를 판단할 방법이 필요했음.

**대안 비교**: 재시도만으로 대응 vs 서킷 브레이커로 장애 감지 시 시도 자체를 차단(Fail Fast).

**선택 이유**: 배타적이지 않고 함께 사용. Retry Topic은 "이벤트 단위 재시도", 서킷 브레이커는 "Kafka 생사 판단 후 호출 자체를 차단"으로 역할을 나눔. `KafkaEventPublisherAdapter#publish()`(`adapter.out.messaging`)에 `@CircuitBreaker(name="kafkaEventPublisher", fallbackMethod="publishFallback")`를 적용했고, `fallbackMethod`는 실패를 삼키지 않고 항상 `EventPublishException`을 재던져 `PublishPendingOutboxEventsService`의 "실패 시 `markAsSent()` 호출 안 함" 계약을 보존.

### 6. 낙관적 락 예외 catch 순서

**문제 상황**: `SagaPersistenceAdapter.save()`에서 JPA `@Version` 충돌과 그 외 인프라 예외를 구분해서 각각 다른 도메인 예외(409/500)로 번역해야 함.

**대안 비교**: catch 순서를 신경 쓰지 않고 상위 타입부터 잡음 vs 하위 타입을 먼저 잡음.

**선택 이유**: `ObjectOptimisticLockingFailureException`은 `DataAccessException`의 하위 타입이므로, `DataAccessException`을 먼저 catch하면 낙관적 락 충돌 분기가 영원히 발동하지 않음. `SagaPersistenceAdapter`(`src/main/java/com/fbrl/adapter/out/persistence/SagaPersistenceAdapter.java`)는 반드시 `ObjectOptimisticLockingFailureException`을 먼저 catch하여 `ConcurrentSagaModificationException`(409)으로, 나머지를 `DataAccessException`으로 잡아 `SagaPersistenceException`(500)으로 번역.

### 7. AOP self-invocation 회피를 위한 빈 분리

**문제 상황**: `@Transactional(REQUIRES_NEW)`, `@DistributedLock`, `@CircuitBreaker`는 모두 Spring AOP 프록시 기반으로 동작하는데, 같은 클래스 내부에서 `this.xxx()`로 호출하면 프록시를 거치지 않아 애노테이션이 무시됨.

**대안 비교**: 하나의 서비스 클래스 안에서 메서드만 분리 vs 별도 스프링 빈으로 분리.

**선택 이유**: 별도 빈 분리만이 실제로 동작함. `AccountCreationExecutor`(`application.service`)는 `createInNewTransaction()`을 `@Transactional(REQUIRES_NEW)`로 별도 빈에 격리했고, `DistributedLockAspect`(`global.common.aop`)는 락 획득 후 실제 트랜잭션 실행을 별도 빈인 `AopForTransaction`에 위임. `@CircuitBreaker`도 동일 원리로, `KafkaEventPublisherAdapterCircuitBreakerTest`는 어댑터를 `new`로 직접 생성하면 프록시를 거치지 않아 서킷 브레이커가 전혀 개입하지 않는다는 점을 `@SpringBootTest` + `@MockitoBean`으로 실제 프록시를 통과시켜 검증.

### 8. PostgreSQL·Debezium CDC 전환 — 현재는 보류

**문제 상황**: Outbox 패턴(과제 4)을 폴링 방식(`OutboxPollingScheduler`)으로 구현했는데, MariaDB보다 PostgreSQL의 논리적 복제(WAL)를 활용한 Debezium CDC가 폴링 지연/부하 없이 더 실시간에 가까운 발행이 가능함.

**대안 비교**: 현재의 MariaDB + 폴링 방식 유지 vs PostgreSQL 전환 + Debezium CDC 도입.

**선택 이유(현재 상태)**: 헥사고날 구조상 `SaveOutboxEventPort`/`LoadPendingOutboxEventsPort`의 구현체(인프라 어댑터)만 교체하면 되므로 확장 과제로 남기고 보류. 현재 코드베이스는 MariaDB + 폴링 방식만 구현되어 있음.

> TODO: PostgreSQL/Debezium 전환은 실제 착수 시점에 이 문서를 갱신할 것.

---

각 결정의 배경/트러블슈팅 전체 기록은 [`PROGRESS.md`](./PROGRESS.md)를 참고하세요.
