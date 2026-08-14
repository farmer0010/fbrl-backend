package com.fbrl.adapter.out.serialization;

import com.fbrl.application.port.out.PayloadDeserializerPort;
import com.fbrl.application.port.out.PayloadSerializerPort;
import com.fbrl.domain.exception.PayloadDeserializationException;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class JacksonPayloadSerializerAdapter
    implements PayloadSerializerPort, PayloadDeserializerPort {
  private final JsonMapper jsonMapper;

  public JacksonPayloadSerializerAdapter(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  @Override
  public String serialize(Object payload) {
    return jsonMapper.writeValueAsString(payload);
  }

  @Override
  public <T> T deserialize(String payload, Class<T> targetType) {
    try {
      return jsonMapper.readValue(payload, targetType);
    } catch (JacksonException e) {
      throw new PayloadDeserializationException(
          "페이로드를 %s 타입으로 역직렬화하는 데 실패했습니다.".formatted(targetType.getSimpleName()), e);
    }
  }
}
