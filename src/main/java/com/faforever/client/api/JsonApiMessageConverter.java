package com.faforever.client.api;

import com.faforever.client.api.dto.ApiException;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.ResourceConverter;
import lombok.SneakyThrows;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class JsonApiMessageConverter extends AbstractHttpMessageConverter<Object> {
  private final ResourceConverter resourceConverter;

  @Inject
  public JsonApiMessageConverter(ResourceConverter resourceConverter) {
    super(MediaType.parseMediaType("application/vnd.api+json"));
    this.resourceConverter = resourceConverter;
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    return true;
  }

  @Override
  @SneakyThrows
  protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) {
    try (InputStream inputStream = inputMessage.getBody()) {
      JSONAPIDocument<?> document;
      if (Iterable.class.isAssignableFrom(clazz)) {
        document = resourceConverter.readDocumentCollection(inputStream, Object.class);
      } else {
        document = resourceConverter.readDocument(inputMessage.getBody(), Object.class);
      }

      Optional.ofNullable(document.getErrors())
          .map(Iterable::spliterator)
          .map(spliterator -> StreamSupport.stream(spliterator, false).collect(Collectors.toList()))
          .ifPresent(errors -> {
            throw new ApiException(errors);
          });

      return document.get();
    }
  }

  @Override
  @SneakyThrows
  protected void writeInternal(Object o, HttpOutputMessage outputMessage) {
    if (o instanceof Iterable) {
      resourceConverter.writeDocumentCollection(new JSONAPIDocument<Iterable<?>>((Iterable<?>) o));
    } else {
      resourceConverter.writeDocument(new JSONAPIDocument<>(o));
    }
  }
}
