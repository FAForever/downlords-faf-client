package com.faforever.client.task;

public enum TaskGroup {
  /**
   * Tasks within this group heavily operate on the network, like downloading many/big files.
   */
  NET_HEAVY,

  /**
   * Tasks within this group operate on the network for only a short while, like doing a REST request.
   */
  NET_LIGHT,

  /**
   * Tasks within this group operate on the disk.
   */
  DISK,

  /**
   * Tasks within this group operate on the network for uploading data.
   */
  NET_UPLOAD,

  /**
   * Tasks within this group are not queued but executed immediately. This should only be used for tasks that spend most
   * of their time idling instead of doing anything resource-intensive.
   */
  IMMEDIATE
}
