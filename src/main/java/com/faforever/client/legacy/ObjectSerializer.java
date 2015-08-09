package com.faforever.client.legacy;

public interface ObjectSerializer {

  <T> byte[] serialize(T object);
}
