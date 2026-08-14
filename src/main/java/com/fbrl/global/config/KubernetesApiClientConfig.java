package com.fbrl.global.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "k8s.leader-election", name = "enabled", havingValue = "true")
public class KubernetesApiClientConfig {

  private static final Logger log = LoggerFactory.getLogger(KubernetesApiClientConfig.class);

  @Bean
  public ApiClient kubernetesApiClient() throws IOException {
    try {
      ApiClient client = ClientBuilder.cluster().build();
      log.info("Kubernetes ApiClient 초기화: 클러스터 내부(ServiceAccount 토큰) 인증 사용");
      return client;
    } catch (Exception e) {
      log.info("클러스터 내부 인증 실패({}), 로컬 kubeconfig로 폴백", e.getMessage());
      ApiClient client = Config.defaultClient();
      log.info("Kubernetes ApiClient 초기화: 로컬 kubeconfig 인증 사용");
      return client;
    }
  }
}
