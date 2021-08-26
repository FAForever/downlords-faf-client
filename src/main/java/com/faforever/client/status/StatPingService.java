package com.faforever.client.status;

import com.faforever.client.config.ClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** REST client for the statping-ng API. */
@org.springframework.stereotype.Service
@Lazy
@Slf4j
public class StatPingService {

  private final WebClient webClient;
  private final String apiRoot;

  public StatPingService(ClientProperties clientProperties, Builder webClientBuilder) {
    apiRoot = clientProperties.getStatping().getApiRoot();
    webClient = webClientBuilder
        .baseUrl(apiRoot)
        .build();
  }

  public Flux<Service> getServices() {
    return getMany("/services", Service.class);
  }

  public Flux<Message> getMessages() {
    return getMany("/messages", Message.class);
  }

  private <T> Flux<T> getMany(String path, Class<T> type) {
    return webClient.get().uri(path).retrieve().bodyToFlux(type)
        .onErrorResume(t -> {
          log.warn("Could not read StatPing from: {}", apiRoot, t);
          return Mono.empty();
        });
  }
}
