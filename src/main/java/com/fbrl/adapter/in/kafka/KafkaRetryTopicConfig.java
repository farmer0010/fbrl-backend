package com.fbrl.adapter.in.kafka;

import com.fbrl.domain.exception.NonRetryableEventProcessingException;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;

@Configuration
public class KafkaRetryTopicConfig {
  private static final int MAX_ATTEMPTS = 4;
  private static final long INITIAL_INTERVAL_MS = Duration.ofSeconds(1).toMillis();
  private static final double BACKOFF_MULTIPLIER = 2.0;
  private static final long MAX_INTERVAL_MS = Duration.ofSeconds(30).toMillis();
  private static final int RETRY_TOPIC_PARTITIONS = 3;
  private static final short RETRY_TOPIC_REPLICATION_FACTOR = 1;
  private static final String TRANSFER_EVENTS_TOPIC = "transfer-events";

  @Bean
  public RetryTopicConfiguration transferEventsRetryTopic(
      KafkaTemplate<String, String> kafkaTemplate) {
    return RetryTopicConfigurationBuilder.newInstance()
        .includeTopic(TRANSFER_EVENTS_TOPIC)
        .maxAttempts(MAX_ATTEMPTS)
        .exponentialBackoff(INITIAL_INTERVAL_MS, BACKOFF_MULTIPLIER, MAX_INTERVAL_MS)
        .notRetryOn(NonRetryableEventProcessingException.class)
        .traversingCauses()
        .autoCreateTopics(true, RETRY_TOPIC_PARTITIONS, RETRY_TOPIC_REPLICATION_FACTOR)
        .create(kafkaTemplate);
  }
}
