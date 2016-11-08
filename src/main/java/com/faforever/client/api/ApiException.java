package com.faforever.client.api;

import com.faforever.client.api.ErrorResponse.Error;

import java.util.stream.Collectors;

public class ApiException extends RuntimeException {
  private final ErrorResponse errorResponse;

  public ApiException(ErrorResponse errorResponse) {
    this.errorResponse = errorResponse;
  }

  @Override
  public String getLocalizedMessage() {
    // TODO localize
    return errorResponse.getErrors().stream()
        .map(Error::getDetail)
        .collect(Collectors.joining("\n"));
  }
}
