package com.faforever.client.legacy;

import java.io.IOException;

public interface ObjectSerializer {

  <T> byte[] serialize(T object) throws IOException;
}
