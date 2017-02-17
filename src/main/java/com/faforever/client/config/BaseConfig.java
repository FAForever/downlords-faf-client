package com.faforever.client.config;

import com.google.common.eventbus.EventBus;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ResourceBundleMessageSource;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * This configuration has to be imported by other configurations and should only contain beans that are necessary to run
 * the application.
 */
@org.springframework.context.annotation.Configuration
public class BaseConfig {

  private ScheduledExecutorService scheduledExecutorService;

  @Bean
  static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  @Inject
  public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
    this.scheduledExecutorService = scheduledExecutorService;
  }

  @Bean
  MessageSource messageSource() {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("i18n.messages");
    messageSource.setFallbackToSystemLocale(false);
    return messageSource;
  }

  @PreDestroy
  void shutdown() {
    scheduledExecutorService.shutdown();
  }

  @Bean
  EventBus eventBus() {
    return new EventBus((exception, context) -> exception.printStackTrace());
  }
}
