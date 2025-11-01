package com.faforever.client.serialization;

import com.faforever.client.preferences.VetoKey;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdKeySerializer;

import java.io.IOException;

public class VetoKeySerializer extends StdKeySerializer {

  @Override
  public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    VetoKey vetoKey = (VetoKey) value;
    gen.writeFieldName(vetoKey.matchmakerQueueMapPoolId() + ":" + vetoKey.mapPoolMapVersionId());
  }
}