package com.faforever.client.relay;

import java.net.DatagramPacket;
import java.util.function.Consumer;

/**
 * Classes implementing this interface allow registration of listeners for received packages.
 */
public interface PackageReceiver {

  /**
   * Adds a listener to be called for any received package.
   */
  void setOnPackageListener(Consumer<DatagramPacket> consumer);
}