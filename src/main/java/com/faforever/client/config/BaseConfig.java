package com.faforever.client.config;

import com.faforever.client.i18n.I18n;
import com.faforever.client.i18n.I18nImpl;
import com.faforever.client.legacy.ServerAccessor;
import com.faforever.client.user.UserService;
import com.faforever.client.user.UserServiceImpl;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;

import java.util.Locale;

/**
 * This configuration has to be imported by other configurations and should only contain beans that are necessary to run
 * the application, whether the user is logged in or not.
 */
@org.springframework.context.annotation.Configuration
@PropertySource("classpath:/faf_client.properties")
public class BaseConfig {

  private static final java.lang.String PROPERTY_LOCALE = "locale";

  @Autowired
  Environment environment;

  @Bean
  Locale locale() {
    return new Locale(environment.getProperty(PROPERTY_LOCALE));
  }

  @Bean
  ResourceBundleMessageSource messageSource() {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("i18n.Messages");
    return messageSource;
  }

  @Bean
  I18n i18n() {
    return new I18nImpl(messageSource(), locale());
  }

  @Bean
  UserService userService() {
    return new UserServiceImpl();
  }

  @Bean
  Gson gson() {
    return new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();
  }

  @Bean
  ServerAccessor serverAccessor() {
    return new ServerAccessor();
  }

}
