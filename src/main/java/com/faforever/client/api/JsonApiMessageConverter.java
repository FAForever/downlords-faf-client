package com.faforever.client.api;

import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.ReflectionUtils;
import com.github.jasminb.jsonapi.ResourceConverter;
import lombok.SneakyThrows;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collection;

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
    return Collection.class.isAssignableFrom(clazz)
        || ReflectionUtils.getTypeName(clazz) != null
        || clazz.equals(JSONAPIDocument.class);
  }

  @Override
  @SneakyThrows
  protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) {
    try (InputStream inputStream = inputMessage.getBody()) {
      JSONAPIDocument<?> document;
      if (Iterable.class.isAssignableFrom(clazz)) {
        document = resourceConverter.readDocumentCollection(inputStream, Object.class);
      } else if (clazz.equals(JSONAPIDocument.class)) {
        return resourceConverter.readDocumentCollection(inputStream, Object.class);
      } else {
        document = resourceConverter.readDocument(inputMessage.getBody(), Object.class);
      }

      return document.get();
    }
  }

  @Override
  @SneakyThrows
  protected void writeInternal(Object o, HttpOutputMessage outputMessage) {
    byte[] serializedObject;
    if (o instanceof Iterable) {
      serializedObject = resourceConverter.writeDocumentCollection(new JSONAPIDocument<Iterable<?>>((Iterable<?>) o));
    } else {
      serializedObject = resourceConverter.writeDocument(new JSONAPIDocument<>(o));
    }
    logger.trace(MessageFormat.format("Serialized ''{0}'' as ''{1}''", o, new String(serializedObject)));
    outputMessage.getBody().write(serializedObject);
  }
}
