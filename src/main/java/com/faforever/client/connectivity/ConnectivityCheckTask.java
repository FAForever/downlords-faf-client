package com.faforever.client.connectivity;

import com.faforever.client.task.PrioritizedTask;

import java.net.DatagramSocket;

public interface ConnectivityCheckTask extends PrioritizedTask<ConnectivityState> {

  DatagramSocket getPublicSocket();

  void setPublicSocket(DatagramSocket publicSocket);
}
