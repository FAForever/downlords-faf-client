package com.faforever.client.remote.domain;

public interface ServerMessage {

  ServerMessageType getMessageType();

  MessageTarget getTarget();
}
