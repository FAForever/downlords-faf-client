package com.faforever.client.api;

import com.faforever.client.api.ErrorResponse.Error;

public class ApiException extends RuntimeException {
  private final ErrorResponse errorResponse;

  public ApiException(ErrorResponse errorResponse) {
    this.errorResponse = errorResponse;
  }

  @Override
  public String getLocalizedMessage() {
    // TODO localize
    StringBuilder errorMessage = new StringBuilder();
    for (Error error : errorResponse.getErrors()) {
      errorMessage.append(error.getDetail()).append("\n");
    }
    return errorMessage.toString();
  }
}
