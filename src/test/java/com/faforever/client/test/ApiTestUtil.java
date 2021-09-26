package com.faforever.client.test;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;

public class ApiTestUtil {

  public static <T> Mono<Tuple2<List<T>, Integer>> apiPageOf(List<T> objects, int pageNumber) {
    return Mono.zip(Mono.just(objects), Mono.just(pageNumber));
  }
}
