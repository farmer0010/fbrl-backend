package com.fbrl.adapter.out.kubernetes;

import com.fbrl.application.port.out.LeaderElectionPort;
import com.fbrl.global.config.LeaderElectionProperties;
import io.kubernetes.client.extended.leaderelection.LeaderElectionConfig;
import io.kubernetes.client.extended.leaderelection.LeaderElector;
import io.kubernetes.client.extended.leaderelection.resourcelock.LeaseLock;
import io.kubernetes.client.openapi.ApiClient;
import jakarta.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "k8s.leader-election", name = "enabled", havingValue = "true")
public class KubernetesLeaderElectionAdapter implements LeaderElectionPort {

  private static final Logger log = LoggerFactory.getLogger(KubernetesLeaderElectionAdapter.class);

  private final ApiClient apiClient;
  private final LeaderElectionProperties properties;
  private final String identity;

  private ExecutorService executorService;
  private LeaderElector leaderElector;

  public KubernetesLeaderElectionAdapter(ApiClient apiClient, LeaderElectionProperties properties) {
    this.apiClient = apiClient;
    this.properties = properties;
    this.identity = resolveIdentity();
  }

  @Override
  public void participateInElection(
      Runnable onStartLeading, Runnable onStopLeading, Consumer<String> onNewLeader) {

    LeaseLock leaseLock =
        new LeaseLock(properties.namespace(), properties.leaseName(), identity, apiClient);

    LeaderElectionConfig config =
        new LeaderElectionConfig(
            leaseLock,
            Duration.ofSeconds(properties.leaseDurationSeconds()),
            Duration.ofSeconds(properties.renewDeadlineSeconds()),
            Duration.ofSeconds(properties.retryPeriodSeconds()));

    this.leaderElector = new LeaderElector(config);
    this.executorService = Executors.newSingleThreadExecutor(this::newDaemonThread);

    log.info("Kubernetes 리더 선출 참여 시작: identity={}, lease={}", identity, properties.leaseName());

    executorService.submit(() -> leaderElector.run(onStartLeading, onStopLeading, onNewLeader));
  }

  @PreDestroy
  public void shutdown() {
    if (leaderElector != null) {
      log.info("Kubernetes 리더 선출 종료 요청: identity={}", identity);
      leaderElector.close();
    }
    if (executorService != null) {
      executorService.shutdown();
    }
  }

  private Thread newDaemonThread(Runnable runnable) {
    Thread thread = new Thread(runnable, "k8s-leader-election");
    thread.setDaemon(true);
    return thread;
  }

  private String resolveIdentity() {
    String podName = System.getenv("POD_NAME");
    if (podName != null && !podName.isBlank()) {
      return podName;
    }
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      log.warn("hostname 조회 실패, 임의 identity 사용", e);
      return "unknown-" + System.currentTimeMillis();
    }
  }
}
