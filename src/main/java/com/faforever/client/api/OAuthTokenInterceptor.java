package com.faforever.client.api;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
@Component
public class OAuthTokenInterceptor implements ClientHttpRequestInterceptor {
  private final TokenService tokenService;

  @SneakyThrows
  @NotNull
  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                      ClientHttpRequestExecution execution) throws IOException {
    OAuth2AccessToken refreshedToken = tokenService.getRefreshedToken();
    request.getHeaders().add("Authorization", "Bearer " + refreshedToken.getValue());
    return execution.execute(request, body);
  }
}