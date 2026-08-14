# CLAUDE.md

이 문서는 이 리포지토리에서 작업하는 모든 AI 에이전트 세션이 자동으로 참조하는 가드레일입니다. 아키텍처 배경은 [`ARCHITECTURE.md`](./ARCHITECTURE.md), 작업 이력/트러블슈팅은 [`PROGRESS.md`](./PROGRESS.md)를 함께 참고하세요.

## 새 작업 시작 전 필수 절차

새 기능이나 리팩토링을 제안하기 전에 **`PROGRESS.md`를 먼저 검색**해서 이미 논의·결정된 사항과 충돌하지 않는지 확인할 것. 이 프로젝트는 특정 설계를 의도적으로 채택/보류한 경우가 많습니다(예: Testcontainers 도입 보류, ShedLock/Redisson/K8s Lease 3종 병행). PROGRESS.md 기록과 대조하지 않고 "더 나은 방법"을 제안하면 이미 검토 후 기각된 대안을 다시 꺼내는 것일 수 있습니다.

## 패키지 구조 규칙

새 클래스를 어느 레이어에 둘지 판단하는 기준:

- **`domain.model`**: 프레임워크(Spring, JPA) 의존 없이 순수 Java로 표현 가능한 상태 + 불변 규칙. Spring/JPA 애노테이션을 붙여야 한다면 이 패키지가 아님.
- **`domain.exception`**: 도메인 규칙 위반을 나타내는 예외. `domain.model`이 아니라 `domain.exception`에 위치(예: `InvalidSagaTransitionException`, `InsufficientBalanceException`).
- **`domain.event`**: 도메인에서 발생하는 사실을 표현하는 불변 이벤트(record).
- **`application.port.in`**: 유스케이스 인터페이스. 컨트롤러/컨슈머/배치가 호출하는 진입점 계약.
- **`application.port.out`**: 외부 의존(DB, Kafka, 다른 서비스)에 대한 계약. 시그니처에 프레임워크 타입이 등장하면 안 됨(예: `EntityManager`, K8s Java Client 타입 금지).
- **`application.service`**: Port In 구현체. 트랜잭션 경계, 오케스트레이션 로직. `REQUIRES_NEW`가 필요한 메서드는 이 패키지 내 **별도 빈**으로 분리(아래 안티패턴 참고).
- **`adapter.in.*`**: 외부 요청(HTTP/Kafka/Batch/Scheduler)을 Port In 호출로 변환하는 진입점. 업무 로직을 담지 않음.
- **`adapter.out.persistence`**: JPA 엔티티, `*JpaRepository`, `*Mapper`, 영속성 어댑터. JPA 전용 락(`@Lock(LockModeType...)`)을 다루는 클래스도 여기.
- **`adapter.out.*`(messaging/participant/kubernetes/serialization)**: 각 외부 시스템 연동 어댑터. Port Out을 구현하고, 프레임워크/라이브러리 예외를 도메인 예외로 번역하는 책임을 짐.
- **`global.config`**: `@Configuration` 빈 정의. **`global.common.aop`**: AOP 애스펙트. **`global.common.annotation`**: 커스텀 애노테이션.

## 절대 반복하면 안 되는 안티패턴 체크리스트

아래 항목은 이 문서 작성 시점에 코드베이스 전수 검증을 마쳤고 현재는 위반 사례가 없습니다. 재발 방지를 위해 계속 유지할 것 — 새 커밋에서 재검증 없이 신뢰하지 말고, 의심되면 아래 방식으로 직접 grep해서 확인하세요.

- **AOP self-invocation 금지** — `@Transactional`, `@DistributedLock`, `@CircuitBreaker` 등 AOP로 동작하는 메서드를 같은 클래스 내부에서 `this.xxx()`로 직접 호출하지 말 것(프록시를 거치지 않아 애노테이션이 무시됨). `REQUIRES_NEW`가 필요한 로직은 반드시 별도 스프링 빈으로 분리(참고: `AccountCreationExecutor`, `AopForTransaction`). 검증: `grep -rEn "this\.[a-zA-Z]+\(" src/main/java`로 같은 클래스 내 애노테이션 붙은 메서드를 self-invocation하지 않는지 확인.
- **테스트 초기화 시 `deleteAll()` 대신 `deleteAllInBatch()` 사용** — JPA 1차 캐시 찌꺼기 방지. 검증: `grep -rn "\.deleteAll(" src/test/java`에 매치가 없어야 함(현재 4개 테스트 파일이 `deleteAllInBatch()`를 사용 중).
- **낙관적 락 사용 시 엔티티 `@Version` 값이 도메인 매퍼를 통과할 때 누락되지 않도록 주의** — 도메인 모델의 `id`/`version` 필드는 nullable로 소유하고, 매퍼가 매번 완전한 엔티티를 재구성해 `save()` 단일 호출로 insert/update를 통일하는 컨벤션(참고: `Account`, `TransferSaga`).
- **Adapter 계층의 프레임워크 전용 예외를 Application 계층으로 그대로 흘려보내지 말 것** — `DataIntegrityViolationException`, `DataAccessException` 등은 영속성 어댑터 안에서 도메인 예외로 번역(참고: `AccountPersistenceAdapter`, `SagaPersistenceAdapter`, `EodSnapshotPersistenceAdapter`). **catch 순서 주의**: `ObjectOptimisticLockingFailureException`은 `DataAccessException`의 하위 타입이므로 반드시 하위 타입을 먼저 catch할 것 — 순서가 바뀌면 낙관적 락 충돌 분기가 영원히 발동하지 않음.
- **Spring Boot 4.x 호환**: Jackson 3 패키지 경로는 `tools.jackson`(`com.fasterxml.jackson` 아님). `@AutoConfigureMockMvc`는 `org.springframework.boot.webmvc.test.autoconfigure` 소속. `@MockBean`/`@SpyBean`은 Boot 4.0에서 완전히 제거되었으므로 `org.springframework.test.context.bean.override.mockito.MockitoBean`/`MockitoSpyBean` 사용.

## 코드 컨벤션

- **`*JpaRepository`는 package-private, `*Mapper`는 public** — JPA 구현 세부사항(리포지토리)은 어댑터 패키지 밖으로 노출하지 않고, 변환 로직(매퍼)만 공개. AI 에이전트가 신규 리포지토리 생성 시 관성적으로 `public interface`로 만드는 경우가 있으니 diff에서 반드시 확인.
- **금액은 `BigDecimal`을 직접 쓰지 말고 `Money` VO로 통일** — 도메인/포트 시그니처에서 금액을 표현할 때는 항상 `Money`(`domain.model.Money`). `BigDecimal`은 `Money` 내부 구현이나 이자율처럼 "금액이 아닌 비율" 값에 한해 허용(예: `InterestPolicy.annualRate`).
- **도메인 전용 Exception은 `domain.exception` 소속** — `domain.model`에 예외 클래스를 두지 말 것.
- **package-private으로 좁힌 interface는 이를 참조하던 테스트 파일도 같은 패키지로 함께 이동**.

## 작업 검증 워크플로 (AI 에이전트에게 위임 시)

1. 작업지시서에 판단 기준과 금지 규칙을 명시.
2. `git diff`/파일 트리를 직접 검증(터미널 pager로 잘릴 수 있으니 `--stat`의 파일 수와 실제 나열된 파일 수가 일치하는지 대조, 의심되면 GitHub 웹 file tree로 재확인).
3. 기존 컨벤션과 다른 결과물(예: 상태 정리를 `@AfterEach`로 작성)은 근거를 들어 반려하고 재작성 요청 — 기능적으로 동작해도 컨벤션 불일치는 반려 사유.
