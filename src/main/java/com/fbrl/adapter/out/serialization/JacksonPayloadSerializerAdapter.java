package com.fbrl.adapter.out.serialization;

import com.fbrl.application.port.out.PayloadSerializerPort;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class JacksonPayloadSerializerAdapter implements PayloadSerializerPort {
  private final JsonMapper jsonMapper;

  public JacksonPayloadSerializerAdapter(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  @Override
  public String serialize(Object payload) {
    return jsonMapper.writeValueAsString(payload);
  }
}
