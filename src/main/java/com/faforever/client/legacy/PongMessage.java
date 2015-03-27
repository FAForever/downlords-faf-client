package com.faforever.client.legacy;

import java.io.IOException;

public enum PongMessage implements Serializable {
  INSTANCE;

  @Override
  public void serialize(QStreamWriter writer, String username, String session) throws IOException {
    writer.append("PONG");
    writer.append(username);
    writer.append(session);
  }
}
