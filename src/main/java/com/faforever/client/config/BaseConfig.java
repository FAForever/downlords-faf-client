package com.faforever.client.config;

import com.google.common.eventbus.EventBus;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * This configuration has to be imported by other configurations and should only contain beans that are necessary to run
 * the application.
 */
@org.springframework.context.annotation.Configuration
public class BaseConfig {

  @Bean
  MessageSource messageSource() {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("i18n.messages");
    messageSource.setFallbackToSystemLocale(false);
    return messageSource;
  }

  @Bean
  EventBus eventBus() {
    return new EventBus((exception, context) -> exception.printStackTrace());
  }
}
