package com.faforever.client.portcheck;

import com.faforever.client.task.PrioritizedTask;

public interface PortCheckTask extends PrioritizedTask<ConnectivityState> {

  void setPort(int port);
}
