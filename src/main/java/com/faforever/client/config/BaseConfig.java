package com.faforever.client.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * This configuration has to be imported by other configurations and should only contain beans that are necessary to run
 * the application.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({ClientProperties.class})
@EnableScheduling
public class BaseConfig {

  @Bean
  ReloadableResourceBundleMessageSource messageSource() {
    ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
    messageSource.setDefaultEncoding("utf-8");
    messageSource.setBasename("classpath:i18n/messages");
    messageSource.setFallbackToSystemLocale(false);
    return messageSource;
  }
}
