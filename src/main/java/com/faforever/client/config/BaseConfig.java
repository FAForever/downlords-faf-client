package com.faforever.client.config;

import com.faforever.client.i18n.I18n;
import com.faforever.client.i18n.I18nImpl;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.common.eventbus.EventBus;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * This configuration has to be imported by other configurations and should only contain beans that are necessary to run
 * the application.
 */
@org.springframework.context.annotation.Configuration
@PropertySource("classpath:/application.properties")
@EnableAsync
public class BaseConfig {
  Preferences preferences;

  @Resource
  ScheduledExecutorService scheduledExecutorService;

  @Bean
  static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  @Bean
  Locale locale() {
     return Locale.getDefault();

  }
  @Bean
  HashMap<String,String> countryCodes(){
    HashMap<String,String> countries =  new HashMap<String,String>();
    countries.put("de","DE");
    countries.put("en","UK");
    countries.put("ru","RU");
    return countries;
  }
  @Bean
  String[] languageCodes()
  {
    String[] template= {null,"en","de","ru"};
    return template;
  }

  @Bean
  MessageSource messageSource() {

    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("i18n.Messages");
    messageSource.setFallbackToSystemLocale(false);

    return messageSource;
  }

  @Bean
  I18n i18n() {
    return new I18nImpl();
  }

  @Bean
  ThreadPoolExecutor threadPoolExecutor() {
    return (ThreadPoolExecutor) Executors.newCachedThreadPool();
  }

  @Bean
  ScheduledExecutorService scheduledExecutorService() {
    return Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
  }

  @Bean
  HttpTransport httpTransport() {
    return new NetHttpTransport.Builder().build();
  }

  @Bean
  JsonFactory jsonFactory() {
    return GsonFactory.getDefaultInstance();
  }

  @PreDestroy
  void shutdown() {
    scheduledExecutorService.shutdown();
  }

  @Bean
  ClientHttpRequestFactory clientHttpRequestFactory() {
    return new SimpleClientHttpRequestFactory();
  }

  @Bean
  EventBus eventBus() {
    return new EventBus();
  }
}
