package com.faforever.client.config;

import com.faforever.client.reporting.ReportingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.beans.factory.DisposableBean;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@EnableAsync
@EnableScheduling
@Configuration
public class AsyncConfig implements AsyncConfigurer, SchedulingConfigurer, DisposableBean {

  private final ReportingService reportingService;

  @Inject
  public AsyncConfig(ReportingService reportingService) {
    this.reportingService = reportingService;
  }

  @Override
  public Executor getAsyncExecutor() {
    return taskExecutor();
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return (ex, method, params) -> {
      reportingService.silentlyReport(ex, false);
      log.error("Uncaught exception in async", ex);
    };
  }

  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    taskRegistrar.setTaskScheduler(taskScheduler());
  }

  @Bean
  public ExecutorService taskExecutor() {
    return Executors.newCachedThreadPool();
  }

  @Bean
  public TaskScheduler taskScheduler() {
    return new ThreadPoolTaskScheduler();
  }

  @Override
  public void destroy() {
    taskExecutor().shutdownNow();
  }
}
