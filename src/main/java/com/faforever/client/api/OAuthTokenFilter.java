package com.faforever.client.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuthTokenFilter implements ExchangeFilterFunction {
  private final TokenService tokenService;

  @Override
  public @NotNull Mono<ClientResponse> filter(@NotNull ClientRequest request, @NotNull ExchangeFunction next) {
    return tokenService.getRefreshedTokenValue().flatMap(token -> next.exchange(ClientRequest.from(request)
        .headers(headers -> headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .build()));
  }
}
