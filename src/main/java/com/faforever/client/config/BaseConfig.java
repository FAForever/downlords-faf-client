package com.faforever.client.config;

import com.faforever.client.i18n.I18n;
import com.faforever.client.i18n.I18nImpl;
import com.faforever.client.stats.domain.GameStats;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * This configuration has to be imported by other configurations and should only contain beans that are necessary to run
 * the application.
 */
@org.springframework.context.annotation.Configuration
@PropertySource("classpath:/faf_client.properties")
@EnableAsync
public class BaseConfig {

  @Autowired
  Environment environment;

  @Autowired
  ScheduledExecutorService scheduledExecutorService;

  @Bean
  Locale locale() {
    return Locale.getDefault();
  }

  @Bean
  MessageSource messageSource() {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("i18n.Messages");
    return messageSource;
  }

  @Bean
  I18n i18n() {
    return new I18nImpl();
  }

  @Bean
  ScheduledExecutorService scheduledExecutorService() {
    return Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
  }

  @Bean
  public Unmarshaller unmarshaller() {
    Jaxb2Marshaller jaxbMarshaller = new Jaxb2Marshaller();
    jaxbMarshaller.setPackagesToScan(GameStats.class.getPackage().getName());
    return jaxbMarshaller;
  }

  @Bean
  HttpTransport httpTransport() {
    return new NetHttpTransport();
  }

  @Bean
  JsonFactory jsonFactory() {
    return GsonFactory.getDefaultInstance();
  }

  @Bean
  RestTemplate restTemplate() {
    Gson gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) -> new Date(json.getAsLong()))
        .create();

    GsonHttpMessageConverter gsonHttpMessageConverter = new GsonHttpMessageConverter();
    gsonHttpMessageConverter.setGson(gson);

    return new RestTemplate(Collections.singletonList(gsonHttpMessageConverter));
  }

  @Bean
  PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  @PreDestroy
  void shutdown() {
    scheduledExecutorService.shutdown();
  }
}
