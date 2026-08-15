package com.fbrl.adapter.out.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fbrl.domain.event.TransferCompletedEvent;
import com.fbrl.domain.exception.PayloadDeserializationException;
import com.fbrl.domain.model.Money;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("JacksonPayloadSerializerAdapter 단위 테스트")
class JacksonPayloadSerializerAdapterTest {

  private JacksonPayloadSerializerAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new JacksonPayloadSerializerAdapter(JsonMapper.builder().build());
  }

  record SamplePayload(String name, int value) {}

  @Test
  @DisplayName("직렬화 후 역직렬화하면 원본과 동일한 객체로 복원된다.")
  void serializeThenDeserialize_roundTrip() {
    SamplePayload original = new SamplePayload("test", 42);

    String json = adapter.serialize(original);
    SamplePayload restored = adapter.deserialize(json, SamplePayload.class);

    assertThat(restored).isEqualTo(original);
  }

  @Test
  @DisplayName(
      "Mock 없이 실제 JsonMapper로 Money를 포함한 TransferCompletedEvent를 직렬화-역직렬화하면 원본과 동일하게 복원된다.")
  void serializeThenDeserialize_transferCompletedEventWithMoney_roundTrip() {
    TransferCompletedEvent original =
        new TransferCompletedEvent(
            "1000-01", "1000-02", Money.wons(50_000), Instant.parse("2026-08-15T00:00:00Z"));

    String json = adapter.serialize(original);
    TransferCompletedEvent restored = adapter.deserialize(json, TransferCompletedEvent.class);

    assertThat(restored).isEqualTo(original);
  }

  @Test
  @DisplayName("깨진 JSON을 역직렬화하면 PayloadDeserializationException을 던지고, 원인 예외를 보존한다.")
  void deserialize_malformedJson_throwsPayloadDeserializationException() {
    String malformedJson = "{ \"name\": \"test\", "; // 닫히지 않은 JSON

    assertThatThrownBy(() -> adapter.deserialize(malformedJson, SamplePayload.class))
        .isInstanceOf(PayloadDeserializationException.class)
        .hasCauseInstanceOf(tools.jackson.core.JacksonException.class);
  }

  @Test
  @DisplayName("필드 타입이 스키마와 맞지 않는 JSON을 역직렬화해도 PayloadDeserializationException을 던진다.")
  void deserialize_schemaMismatch_throwsPayloadDeserializationException() {
    String mismatchedJson = "{ \"name\": \"test\", \"value\": \"not-a-number\" }";

    assertThatThrownBy(() -> adapter.deserialize(mismatchedJson, SamplePayload.class))
        .isInstanceOf(PayloadDeserializationException.class);
  }
}
