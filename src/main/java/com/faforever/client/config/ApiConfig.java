package com.faforever.client.config;

import com.faforever.client.api.JsonApiMessageConverter;
import com.faforever.client.config.ClientProperties.Api;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiConfig {

  @Bean
  public RestTemplateBuilder restTemplateBuilder(JsonApiMessageConverter jsonApiMessageConverter, ClientProperties clientProperties) {
    Api api = clientProperties.getApi();
    return new RestTemplateBuilder()
        .messageConverters(jsonApiMessageConverter)
        .rootUri(api.getBaseUrl());
  }
}
