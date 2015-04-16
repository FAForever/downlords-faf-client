package com.faforever.client.network;

import com.faforever.client.util.Callback;

import java.io.IOException;
import java.net.SocketException;

public interface PortCheckService {

  void checkUdpPortInBackground(int port, Callback<Boolean> callback);
}
