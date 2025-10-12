package com.faforever.client.serialization;

import com.faforever.client.preferences.VetoKey;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import java.io.IOException;

public class VetoKeyDeserializer extends KeyDeserializer {

  @Override
  public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
    return VetoKey.fromString(key);
  }
}