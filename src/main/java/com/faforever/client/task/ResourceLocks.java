package com.faforever.client.task;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ResourceLocks {

  private static final ReentrantReadWriteLock NETWORK_LOCK = new ReentrantReadWriteLock();
  private static final ReentrantLock DISK_LOCK = new ReentrantLock();

  public static void aquireDownloadLock() {
    NETWORK_LOCK.readLock().lock();
  }

  public static void freeDownloadLock() {
    NETWORK_LOCK.readLock().unlock();
  }

  public static void aquireUploadLock() {
    NETWORK_LOCK.writeLock().lock();
  }

  public static void freeUploadLock() {
    NETWORK_LOCK.writeLock().unlock();
  }

  public static void acquireDiskLock() {
    NETWORK_LOCK.writeLock().unlock();
  }

  public static void freeDiskLock() {
    NETWORK_LOCK.writeLock().unlock();
  }
}
