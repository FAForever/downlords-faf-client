package com.faforever.client.exception;

import com.faforever.client.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.Lifecycle;
import org.springframework.context.Phased;
import org.springframework.stereotype.Component;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Method;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalExceptionHandler implements UncaughtExceptionHandler, AsyncUncaughtExceptionHandler, Lifecycle, Phased {
  private final NotificationService notificationService;

  private boolean shuttingDown;

  @Override
  public void uncaughtException(Thread t, Throwable ex) {
    if (shuttingDown) {
      return;
    }

    if (ex instanceof NotifiableException notifiableException) {
      log.error("Exception on Thread {}: ", t, ex);
      notificationService.addErrorNotification(notifiableException);
    } else {
      log.error("Uncaught exception on {}: ", t, ex);
    }
  }

  @Override
  public void handleUncaughtException(@NotNull Throwable ex, @NotNull Method method, Object @NotNull ... params) {
    if (ex instanceof NotifiableException notifiableException) {
      log.error("Exception on Method {} with parameters {}: ", method.getName(), params, ex);
      notificationService.addErrorNotification(notifiableException);
    } else {
      log.error("Uncaught Exception on Method {} with parameters {}: ", method.getName(), params, ex);
    }
  }

  @Override
  public void start() {
    shuttingDown = false;
  }

  @Override
  public void stop() {
    shuttingDown = true;
  }

  @Override
  public boolean isRunning() {
    return !shuttingDown;
  }

  @Override
  public int getPhase() {
    return Integer.MAX_VALUE;
  }
}
