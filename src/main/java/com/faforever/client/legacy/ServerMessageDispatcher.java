package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.ServerMessageType;
import com.faforever.client.legacy.io.QDataReader;

public interface ServerMessageDispatcher {

  void dispatchServerMessage(QDataReader socketIn, ServerMessageType serverMessageType);
}
