package com.faforever.client.config;

import com.faforever.client.exception.GlobalExceptionHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

@EnableAsync
@EnableScheduling
@Configuration
@Slf4j
@AllArgsConstructor
public class AsyncConfig implements AsyncConfigurer, SchedulingConfigurer {

  private final GlobalExceptionHandler globalExceptionHandler;
  private final ExecutorService taskExecutor;
  private final TaskScheduler taskScheduler;

  @Override
  public Executor getAsyncExecutor() {
    return taskExecutor;
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return globalExceptionHandler;
  }

  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    taskRegistrar.setTaskScheduler(taskScheduler);
  }
}
