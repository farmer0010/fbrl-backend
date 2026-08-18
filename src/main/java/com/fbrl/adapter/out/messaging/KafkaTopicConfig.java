package com.fbrl.adapter.out.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

  @Bean
  public NewTopic transferEventsTopic(
      @Value("${kafka.topic.transfer-events:transfer-events}") String transferEventsTopicName) {
    return TopicBuilder.name(transferEventsTopicName).partitions(3).replicas(1).build();
  }
}
