package com.faforever.client.remote.domain;

public interface ServerMessageType {

  String getString();

  <T extends ServerMessage> Class<T> getType();
}
