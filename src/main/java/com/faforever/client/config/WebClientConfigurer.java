package com.faforever.client.config;

import com.faforever.client.api.JsonApiReader;
import com.faforever.client.api.JsonApiWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Component
@RequiredArgsConstructor
public class WebClientConfigurer implements WebClientCustomizer {
  private final JsonApiWriter jsonApiWriter;
  private final JsonApiReader jsonApiReader;
  private final ClientProperties clientProperties;

  @Override
  public void customize(WebClient.Builder webClientBuilder) {
    webClientBuilder.defaultHeader("User-Agent", clientProperties.getUserAgent())
        .clientConnector(new ReactorClientHttpConnector(HttpClient.newConnection()))
        .codecs(clientCodecConfigurer -> {
          clientCodecConfigurer.customCodecs().register(jsonApiReader);
          clientCodecConfigurer.customCodecs().register(jsonApiWriter);
        });
  }
}
