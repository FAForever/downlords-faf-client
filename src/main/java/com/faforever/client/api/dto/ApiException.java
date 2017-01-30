package com.faforever.client.api.dto;


import com.github.jasminb.jsonapi.models.errors.Error;

import java.util.List;
import java.util.stream.Collectors;

public class ApiException extends RuntimeException {

  private final List<? extends Error> errors;

  public ApiException(List<? extends Error> errors) {
    this.errors = errors;
  }

  @Override
  public String getLocalizedMessage() {
    // TODO localize
    return errors.stream()
        .map(Error::getDetail)
        .collect(Collectors.joining("\n"));
  }
}
