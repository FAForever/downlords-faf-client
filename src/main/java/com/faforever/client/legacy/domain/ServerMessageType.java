package com.faforever.client.legacy.domain;

public interface ServerMessageType {

  String getString();

  <T extends ServerMessage> Class<T> getType();
}
