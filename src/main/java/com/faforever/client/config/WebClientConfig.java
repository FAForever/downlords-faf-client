package com.faforever.client.config;

import com.faforever.client.api.HmacTokenFilter;
import com.faforever.client.api.OAuthTokenFilter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  @Bean
  public WebClient defaultWebClient(WebClient.Builder webClientBuilder) {
    return webClientBuilder.build();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public WebClient apiWebClient(WebClient.Builder webClientBuilder, OAuthTokenFilter oAuthTokenFilter,
                                HmacTokenFilter hmacTokenFilter, ClientProperties clientProperties) {
    return webClientBuilder.baseUrl(clientProperties.getApi().getBaseUrl())
                           .filter(oAuthTokenFilter)
                           .filter(hmacTokenFilter)
                           .build();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public WebClient userWebClient(WebClient.Builder webClientBuilder, OAuthTokenFilter oAuthTokenFilter,
                                 HmacTokenFilter hmacTokenFilter, ClientProperties clientProperties) {
    return webClientBuilder.baseUrl(clientProperties.getUser().getBaseUrl())
                           .filter(oAuthTokenFilter)
                           .filter(hmacTokenFilter)
                           .build();
  }

}
