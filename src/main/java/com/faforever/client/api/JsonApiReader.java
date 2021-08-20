package com.faforever.client.api;

import com.faforever.commons.api.dto.CoopMission;
import com.faforever.commons.api.dto.CoopResult;
import com.faforever.commons.api.dto.FeaturedModFile;
import com.faforever.commons.api.dto.PlayerEvent;
import com.faforever.commons.api.elide.ElideEntity;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.ResourceConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.SequenceInputStream;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JsonApiReader implements HttpMessageReader<Object> {
  private final ResourceConverter resourceConverter;

  @Override
  public List<MediaType> getReadableMediaTypes() {
    return List.of(MediaType.APPLICATION_JSON, MediaType.parseMediaType("application/vnd.api+json;charset=utf-8"));
  }

  @Override
  public boolean canRead(ResolvableType elementType, MediaType mediaType) {
    Class<?> clazz = elementType.toClass();
    return (clazz.equals(JSONAPIDocument.class)
        || ElideEntity.class.isAssignableFrom(clazz)
        //TODO: Remove once data classes properly extend ElideEntity
        || clazz.equals(CoopMission.class)
        || clazz.equals(CoopResult.class)
        || clazz.equals(FeaturedModFile.class)
        || clazz.equals(PlayerEvent.class))
        && (mediaType == null
        || getReadableMediaTypes().stream().anyMatch(readerMediaType -> readerMediaType.isCompatibleWith(mediaType)));
  }

  @Override
  public Mono<Object> readMono(ResolvableType elementType, ReactiveHttpInputMessage message, Map hints) {
    Class<?> clazz = elementType.toClass();
    if (clazz.equals(JSONAPIDocument.class)) {
      return message.getBody()
          .map(dataBuffer -> dataBuffer.asInputStream(true))
          .reduce((SequenceInputStream::new))
          .map(completeStream -> resourceConverter.readDocumentCollection(completeStream, Object.class));
    } else {
      return message.getBody()
          .map(dataBuffer -> dataBuffer.asInputStream(true))
          .reduce((SequenceInputStream::new))
          .flatMap(completeStream -> Mono.fromCallable(() -> resourceConverter.readDocument(completeStream, Object.class).get()));
    }
  }

  @Override
  public Flux<Object> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map hints) {
    return message.getBody()
        .map(dataBuffer -> dataBuffer.asInputStream(true))
        .reduce((SequenceInputStream::new))
        .flatMap(completeStream -> Mono.fromCallable(() -> resourceConverter.readDocumentCollection(completeStream, Object.class).get()))
        .flatMapMany(Flux::fromIterable);
  }
}
