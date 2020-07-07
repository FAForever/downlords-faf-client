package com.faforever.client.api;


import com.faforever.client.api.dto.ApiException;
import com.github.jasminb.jsonapi.exceptions.ResourceParseException;
import com.github.jasminb.jsonapi.models.errors.Errors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;

@Component
@Slf4j
public class JsonApiErrorHandler extends DefaultResponseErrorHandler {
  private final JsonApiMessageConverter jsonApiMessageConverter;

  public JsonApiErrorHandler(JsonApiMessageConverter jsonApiMessageConverter) {
    this.jsonApiMessageConverter = jsonApiMessageConverter;
  }

  @Override
  public void handleError(ClientHttpResponse response) throws IOException {
    if (response.getStatusCode().is4xxClientError()) {
      try {
        jsonApiMessageConverter.readInternal(Errors.class, response);
      } catch (ResourceParseException e) {
        throw new ApiException(e.getErrors().getErrors());
      }
    }
    super.handleError(response);
  }
}
