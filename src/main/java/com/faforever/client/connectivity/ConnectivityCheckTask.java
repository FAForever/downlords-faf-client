package com.faforever.client.connectivity;

import com.faforever.client.relay.ConnectivityStateMessage;
import com.faforever.client.task.PrioritizedTask;

public interface ConnectivityCheckTask extends PrioritizedTask<ConnectivityStateMessage> {

  int getPublicPort();

  void setPublicPort(int publicPort);
}
