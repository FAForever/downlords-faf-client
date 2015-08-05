package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.ServerMessageType;
import com.faforever.client.legacy.io.QDataInputStream;

public interface ServerMessageDispatcher {

  void dispatchServerMessage(QDataInputStream socketIn, ServerMessageType serverMessageType);
}
