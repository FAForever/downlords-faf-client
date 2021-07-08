package com.faforever.client.api;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuthTokenInterceptor implements ClientHttpRequestInterceptor {
  private final TokenService tokenService;

  @SneakyThrows
  @NotNull
  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                      ClientHttpRequestExecution execution) {
    request.getHeaders().add("Authorization", "Bearer " + tokenService.getRefreshedTokenValue());
    return execution.execute(request, body);
  }
}
