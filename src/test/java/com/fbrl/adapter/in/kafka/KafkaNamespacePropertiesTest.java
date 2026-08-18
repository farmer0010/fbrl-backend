package com.fbrl.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;

@DisplayName("Kafka 토픽명/컨슈머 group-id 바인딩 테스트")
class KafkaNamespacePropertiesTest {

  @Nested
  @SpringBootTest
  @DisplayName("환경변수 미설정 시")
  class Default {
    @Autowired private NewTopic transferEventsTopic;
    @Autowired private KafkaListenerEndpointRegistry listenerEndpointRegistry;

    @Test
    @DisplayName("토픽명이 기본값 transfer-events로 바인딩된다")
    void bindsDefaultTopicName() {
      assertThat(transferEventsTopic.name()).isEqualTo("transfer-events");
    }

    @Test
    @DisplayName("컨슈머 group-id가 기본값 transfer-event-processor로 바인딩된다")
    void bindsDefaultGroupId() {
      assertThat(mainListenerContainer(listenerEndpointRegistry).getGroupId())
          .isEqualTo("transfer-event-processor");
    }
  }

  @Nested
  @SpringBootTest(
      properties = {
        "KAFKA_TOPIC_TRANSFER_EVENTS=demo-transfer-events",
        "KAFKA_CONSUMER_GROUP_ID=demo-transfer-event-processor"
      })
  @DisplayName("환경변수 오버라이드 시")
  class Overridden {
    @Autowired private NewTopic transferEventsTopic;
    @Autowired private KafkaListenerEndpointRegistry listenerEndpointRegistry;

    @Test
    @DisplayName("토픽명이 오버라이드 값으로 바인딩된다")
    void bindsOverriddenTopicName() {
      assertThat(transferEventsTopic.name()).isEqualTo("demo-transfer-events");
    }

    @Test
    @DisplayName("컨슈머 group-id가 오버라이드 값으로 바인딩된다")
    void bindsOverriddenGroupId() {
      assertThat(mainListenerContainer(listenerEndpointRegistry).getGroupId())
          .isEqualTo("demo-transfer-event-processor");
    }
  }

  // RetryTopicConfiguration이 재시도/DLT용으로 group-id에 -retry-*, -dlt가 붙은 컨테이너를
  // 추가 등록하므로, 그 접미사가 없는 메인 리스너 컨테이너만 골라 group-id를 검증한다.
  private static MessageListenerContainer mainListenerContainer(
      KafkaListenerEndpointRegistry registry) {
    Collection<MessageListenerContainer> mainContainers =
        registry.getListenerContainersMatching(
            groupId -> !groupId.contains("-retry-") && !groupId.endsWith("-dlt"));
    assertThat(mainContainers).hasSize(1);
    return mainContainers.iterator().next();
  }
}
