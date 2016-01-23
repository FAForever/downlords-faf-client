package com.faforever.client.legacy.domain;

public interface ServerMessage {

  ServerMessageType getMessageType();

  MessageTarget getTarget();
}
