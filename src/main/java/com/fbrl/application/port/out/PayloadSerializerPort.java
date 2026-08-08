package com.fbrl.application.port.out;

public interface PayloadSerializerPort {
  String serialize(Object payload);
}
