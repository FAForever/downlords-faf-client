package com.faforever.client.api;

import com.faforever.commons.api.elide.ElideEntity;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.ResourceConverter;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JsonApiWriter implements HttpMessageWriter<Object> {
  private final ResourceConverter resourceConverter;

  @Override
  public List<MediaType> getWritableMediaTypes() {
    return List.of(MediaType.parseMediaType("application/vnd.api+json;charset=utf-8"));
  }

  @Override
  public boolean canWrite(ResolvableType elementType, MediaType mediaType) {
    Class<?> clazz = elementType.toClass();
    return ElideEntity.class.isAssignableFrom(clazz) && resourceConverter.isRegisteredType(clazz)
        && getWritableMediaTypes().stream().anyMatch(readerMediaType -> readerMediaType.isCompatibleWith(mediaType));
  }

  @Override
  public Mono<Void> write(Publisher<?> inputStream, ResolvableType elementType, MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints) {
    return message.writeWith(
        Mono.from(inputStream).flatMap(objects ->
            Mono.fromCallable(() -> {
              byte[] serializedObject;
              if (objects instanceof Iterable) {
                serializedObject = resourceConverter.writeDocumentCollection(new JSONAPIDocument<Iterable<?>>((Iterable<?>) objects));
              } else if (objects instanceof JSONAPIDocument) {
                serializedObject = resourceConverter.writeDocument((JSONAPIDocument<?>) objects);
              } else {
                serializedObject = resourceConverter.writeDocument(new JSONAPIDocument<>(objects));
              }
              return serializedObject;
            })
        ).map(bytes -> message.bufferFactory().wrap(bytes)));
  }

  @Override
  public Mono<Void> write(Publisher<?> inputStream, ResolvableType actualType, ResolvableType elementType, MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints) {
    return HttpMessageWriter.super.write(inputStream, actualType, elementType, mediaType, request, response, hints);
  }
}
