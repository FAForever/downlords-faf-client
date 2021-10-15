package com.faforever.client.exception;

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

  @Override
  public void uncaughtException(Thread t, Throwable ex) {
    log.warn("Uncaught exception on {}: ", t, ex);
  }

  @Override
  public void handleUncaughtException(@NotNull Throwable ex, @NotNull Method method, Object @NotNull ... params) {
    log.error("Uncaught Exception on Method {} with parameters {}: ", method.getName(), params, ex);
  }
}
