package com.faforever.client.task;

public enum TaskGroup {
  /**
   * Tasks within this group heavily operate on the network, like downloading many/big files.
   */
  NET_HEAVY,

  /**
   * Tasks within this group operate on the network for only a short while, like doing a REST request.
   */
  NET_LIGHT
}
