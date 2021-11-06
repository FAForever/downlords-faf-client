package com.faforever.client.exception;

import com.faforever.client.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.stereotype.Component;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Method;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalExceptionHandler implements UncaughtExceptionHandler, AsyncUncaughtExceptionHandler {
  private final NotificationService notificationService;

  @Override
  public void uncaughtException(Thread t, Throwable ex) {
    if (ex instanceof NotifiableException) {
      log.error("Exception on Thread {}: ", t, ex);
      notificationService.addErrorNotification((NotifiableException) ex);
    } else {
      log.warn("Uncaught exception on {}: ", t, ex);
    }
  }

  @Override
  public void handleUncaughtException(@NotNull Throwable ex, @NotNull Method method, Object @NotNull ... params) {
    if (ex instanceof NotifiableException) {
      log.error("Exception on Method {} with parameters {}: ", method.getName(), params, ex);
      notificationService.addErrorNotification((NotifiableException) ex);
    } else {
      log.error("Uncaught Exception on Method {} with parameters {}: ", method.getName(), params, ex);
    }
  }
}
