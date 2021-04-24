package com.faforever.client.api;


import com.faforever.commons.api.dto.ApiException;
import com.github.jasminb.jsonapi.exceptions.ResourceParseException;
import com.github.jasminb.jsonapi.models.errors.Errors;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;

@Component
public class JsonApiErrorHandler extends DefaultResponseErrorHandler {
  private final JsonApiMessageConverter jsonApiMessageConverter;

  public JsonApiErrorHandler(JsonApiMessageConverter jsonApiMessageConverter) {
    this.jsonApiMessageConverter = jsonApiMessageConverter;
  }

  @Override
  public void handleError(ClientHttpResponse response) throws IOException {
    if (response.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
      try {
        jsonApiMessageConverter.readInternal(Errors.class, response);
      } catch (ResourceParseException e) {
        throw new ApiException(e.getErrors().getErrors());
      }
    }
    super.handleError(response);
  }
}
