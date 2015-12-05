package com.faforever.client.connectivity;

import com.faforever.client.task.PrioritizedTask;

public interface ConnectivityCheckTask extends PrioritizedTask<ConnectivityState> {

  void setPort(int port);
}
