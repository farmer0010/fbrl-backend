package com.fbrl.application.port.out;

public interface PayloadDeserializerPort {
  <T> T deserialize(String payload, Class<T> targetType);
}
