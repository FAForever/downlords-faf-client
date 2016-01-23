package com.faforever.client.connectivity;

import com.faforever.client.relay.ConnectivityStateMessage;
import com.faforever.client.task.PrioritizedTask;

public interface ConnectivityCheckTask extends PrioritizedTask<ConnectivityStateMessage> {

  void setDatagramGateway(DatagramGateway datagramGateway);

  int getPublicPort();

  void setPublicPort(int publicPort);
}
