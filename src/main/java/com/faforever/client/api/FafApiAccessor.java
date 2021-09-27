package com.faforever.client.api;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Api;
import com.faforever.client.io.CountingFileSystemResource;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.commons.api.dto.ApiException;
import com.faforever.commons.api.dto.Clan;
import com.faforever.commons.api.dto.CoopResult;
import com.faforever.commons.api.dto.Game;
import com.faforever.commons.api.dto.GameReviewsSummary;
import com.faforever.commons.api.dto.LeaderboardEntry;
import com.faforever.commons.api.dto.LeaderboardRatingJournal;
import com.faforever.commons.api.dto.Map;
import com.faforever.commons.api.dto.MapPoolAssignment;
import com.faforever.commons.api.dto.MapReviewsSummary;
import com.faforever.commons.api.dto.MapVersion;
import com.faforever.commons.api.dto.MatchmakerQueue;
import com.faforever.commons.api.dto.MeResult;
import com.faforever.commons.api.dto.Mod;
import com.faforever.commons.api.dto.ModReviewsSummary;
import com.faforever.commons.api.dto.ModVersion;
import com.faforever.commons.api.dto.ModerationReport;
import com.faforever.commons.api.dto.Player;
import com.faforever.commons.api.dto.TutorialCategory;
import com.faforever.commons.api.elide.ElideEndpointBuilder;
import com.faforever.commons.api.elide.ElideEntity;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import com.faforever.commons.api.elide.ElideNavigatorOnId;
import com.faforever.commons.io.ByteCountListener;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.exceptions.ResourceParseException;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.retry.Retry;

import java.io.Serializable;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;

@SuppressWarnings("unchecked")
@Lazy
@Slf4j
@Component
@Profile("!offline")
@RequiredArgsConstructor
public class FafApiAccessor implements InitializingBean {

  @VisibleForTesting
  static final java.util.Map<Class<? extends ElideEntity>, List<String>> INCLUDES = java.util.Map.ofEntries(
      java.util.Map.entry(CoopResult.class, List.of("game.playerStats.player")),
      java.util.Map.entry(Clan.class, List.of("leader", "founder", "memberships", "memberships.player")),
      java.util.Map.entry(LeaderboardEntry.class, List.of("player", "leaderboard")),
      java.util.Map.entry(LeaderboardRatingJournal.class, List.of("gamePlayerStats")),
      java.util.Map.entry(GameReviewsSummary.class,
          List.of("game", "game.featuredMod", "game.playerStats", "game.playerStats.player", "game.playerStats.ratingChanges",
              "game.reviews", "game.reviews.player", "game.mapVersion", "game.mapVersion.map", "game.mapVersion.map")),
      java.util.Map.entry(Game.class,
          List.of("featuredMod", "playerStats", "playerStats.player", "playerStats.ratingChanges", "reviews", "reviews.player",
              "mapVersion", "mapVersion.map", "mapVersion.map.versions", "reviewsSummary")),
      java.util.Map.entry(MapVersion.class,
          List.of("map", "map.latestVersion", "map.versions",
              "map.versions.reviews", "map.versions.reviews.player", "map.reviewsSummary", "map.author")),
      java.util.Map.entry(MapReviewsSummary.class,
          List.of("map.latestVersion", "map.author", "map.versions", "map.versions.reviews", "map.versions.reviews.player", "map.reviewsSummary")),
      java.util.Map.entry(Map.class,
          List.of("latestVersion", "author", "versions", "versions.reviews", "versions.reviews.player", "reviewsSummary")),
      java.util.Map.entry(MapPoolAssignment.class,
          List.of("mapVersion", "mapVersion.map", "mapVersion.map.latestVersion", "mapVersion.map.author",
              "mapVersion.map.reviewsSummary", "mapVersion.map.versions.reviews", "mapVersion.map.versions.reviews.player")),
      java.util.Map.entry(ModVersion.class,
          List.of("mod", "mod.latestVersion", "mod.versions",
              "mod.versions.reviews", "mod.versions.reviews.player", "mod.reviewsSummary", "mod.uploader")),
      java.util.Map.entry(ModReviewsSummary.class,
          List.of("mod.latestVersion", "mod.versions", "mod.versions.reviews", "mod.versions.reviews.player", "mod.reviewsSummary", "mod.uploader")),
      java.util.Map.entry(Mod.class,
          List.of("latestVersion", "versions", "versions.reviews", "versions.reviews.player", "reviewsSummary", "uploader")),
      java.util.Map.entry(Player.class, List.of("names")),
      java.util.Map.entry(ModerationReport.class, List.of("reporter", "lastModerator", "reportedUsers", "game", "game.playerStats", "game.playerStats.player")),
      java.util.Map.entry(MatchmakerQueue.class, List.of("leaderboard")),
      java.util.Map.entry(TutorialCategory.class, List.of("tutorials", "tutorials.mapVersion.map", "tutorials.mapVersion.map.latestVersion",
          "tutorials.mapVersion.map.author"))
  );

  @VisibleForTesting
  static final java.util.Map<Class<? extends ElideEntity>, List<Condition<?>>> FILTERS = java.util.Map.ofEntries(
      java.util.Map.entry(ModVersion.class, List.of(qBuilder().bool("hidden").isFalse())),
      java.util.Map.entry(Mod.class, List.of(qBuilder().bool("latestVersion.hidden").isFalse())),
      java.util.Map.entry(ModReviewsSummary.class, List.of(qBuilder().bool("mod.latestVersion.hidden").isFalse())),
      java.util.Map.entry(MapVersion.class, List.of(qBuilder().bool("hidden").isFalse())),
      java.util.Map.entry(Map.class, List.of(qBuilder().bool("latestVersion.hidden").isFalse())),
      java.util.Map.entry(MapReviewsSummary.class, List.of(qBuilder().bool("map.latestVersion.hidden").isFalse()))
  );

  private static final String JSONAPI_MEDIA_TYPE = "application/vnd.api+json;charset=utf-8";

  private final EventBus eventBus;
  private final ClientProperties clientProperties;
  private final OAuthTokenFilter oAuthTokenFilter;
  private final WebClient.Builder webClientBuilder;

  private CountDownLatch authorizedLatch = new CountDownLatch(1);
  private WebClient webClient;
  @Getter
  private int maxPageSize;
  private Retry apiRetrySpec;

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
    Api api = clientProperties.getApi();
    apiRetrySpec = Retry.backoff(api.getRetryAttempts(), Duration.ofSeconds(api.getRetryBackoffSeconds()))
        .jitter(api.getRetryJitter())
        .filter(error -> error instanceof UnreachableApiException)
        .doBeforeRetry(retry -> log.info("Could not retrieve value from api retrying: Attempt #{} of {}", retry.totalRetries(), retry.totalRetriesInARow()))
        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
            new UnreachableApiException("API is unreachable after max retries", retrySignal.failure()));
  }

  public void authorize() {
    Api apiProperties = clientProperties.getApi();
    maxPageSize = apiProperties.getMaxPageSize();

    webClient = webClientBuilder
        .baseUrl(apiProperties.getBaseUrl())
        .filter(oAuthTokenFilter)
        .build();

    authorizedLatch.countDown();
  }

  @Subscribe
  public void onLoggedOutEvent(LoggedOutEvent event) {
    authorizedLatch = new CountDownLatch(1);
  }

  @Subscribe
  public void onSessionExpiredEvent(SessionExpiredEvent event) {
    authorizedLatch = new CountDownLatch(1);
  }

  public Mono<MeResult> getMe() {
    return retrieveMonoWithErrorHandling(MeResult.class, webClient.get().uri("/me"))
        .cache()
        .doOnNext(object -> log.debug("Retrieved {} from {} with type {}", object, "/me", MeResult.class));
  }

  public Mono<Void> uploadFile(String endpoint, Path file, ByteCountListener listener, java.util.Map<String, java.util.Map<String, ?>> params) {
    MultiValueMap<String, Object> multipartContent = createFileMultipart(file, listener);
    params.forEach(multipartContent::add);
    return postMultipartForm(endpoint, multipartContent);
  }

  @NotNull
  private MultiValueMap<String, Object> createFileMultipart(Path file, ByteCountListener listener) {
    MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
    form.add("file", new CountingFileSystemResource(file, listener));
    return form;
  }

  public Mono<Void> postMultipartForm(String endpointPath, MultiValueMap<String, Object> request) {
    return retrieveMonoWithErrorHandling(Void.class, webClient.post().uri(endpointPath)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .bodyValue(request))
        .doOnSuccess(aVoid -> log.debug("Posted {} to {}", request, endpointPath));
  }

  public <T extends ElideEntity> Mono<T> post(ElideNavigatorOnCollection<T> navigator, T request) {
    Class<T> type = navigator.getDtoClass();
    String endpointPath = navigator.build();
    return retrieveMonoWithErrorHandling(type, webClient.post().uri(endpointPath)
        .contentType(MediaType.parseMediaType(JSONAPI_MEDIA_TYPE))
        .bodyValue(request))
        .doOnNext(object -> log.debug("Posted {} to {} with type {}", object, endpointPath, type));
  }

  public <T extends ElideEntity> Mono<Void> patch(ElideNavigatorOnId<T> navigator, T request) {
    String endpointPath = navigator.build();
    return retrieveMonoWithErrorHandling(Void.class, webClient.patch().uri(endpointPath)
        .contentType(MediaType.parseMediaType(JSONAPI_MEDIA_TYPE))
        .bodyValue(request))
        .doOnSuccess(aVoid -> log.debug("Patched {} at {}", request, endpointPath));
  }

  public Mono<Void> delete(ElideNavigatorOnId<?> navigator) {
    String endpointPath = navigator.build();
    return retrieveMonoWithErrorHandling(Void.class, webClient.delete().uri(endpointPath))
        .doOnSuccess(aVoid -> log.debug("Deleted {}", endpointPath));
  }

  public <T extends ElideEntity> Mono<T> getOne(ElideNavigatorOnId<T> navigator) {
    enrichBuilder(navigator);

    Class<T> type = navigator.getDtoClass();
    String endpointPath = navigator.build();
    return retrieveMonoWithErrorHandling(type, webClient.get().uri(endpointPath))
        .cache()
        .doOnNext(object -> log.debug("Retrieved {} from {} with type {}", object, endpointPath, type));
  }

  public <T> Flux<T> getMany(Class<T> type, String endpointPath, int count, java.util.Map<String, Serializable> params) {
    java.util.Map<String, List<String>> multiValues = params.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, entry -> List.of(String.valueOf(entry.getValue()))));

    UriComponents uriComponents = UriComponentsBuilder.fromPath(endpointPath)
        .queryParams(CollectionUtils.toMultiValueMap(multiValues))
        .replaceQueryParam("page[size]", count)
        .replaceQueryParam("page[number]", 1)
        .build();


    String url = uriComponents.toUriString();

    return retrieveFluxWithErrorHandling(type, webClient.get().uri(url))
        .cache()
        .doOnNext(list -> log.debug("Retrieved {} from {}", list, url));
  }

  public <T extends ElideEntity> Flux<T> getMany(ElideNavigatorOnCollection<T> navigator) {
    return getMany(navigator, "");
  }

  public <T extends ElideEntity> Flux<T> getMany(ElideNavigatorOnCollection<T> navigator, String customFilter) {
    enrichBuilder(navigator);
    enrichCollectionFilter(navigator);
    String endpointPath;
    if (!customFilter.isEmpty()) {
      endpointPath = enrichWithCustomFilter(navigator.build(), customFilter);
    } else {
      endpointPath = navigator.build();
    }

    return retrieveFluxWithErrorHandling(navigator.getDtoClass(), webClient.get().uri(endpointPath))
        .cache()
        .doOnNext(object -> log.debug("Retrieved {} from {}", object, endpointPath));
  }

  public <T extends ElideEntity> Mono<Tuple2<List<T>, Integer>> getManyWithPageCount(ElideNavigatorOnCollection<T> navigator) {
    return getManyWithPageCount(navigator, "");
  }

  public <T extends ElideEntity> Mono<Tuple2<List<T>, Integer>> getManyWithPageCount(ElideNavigatorOnCollection<T> navigator, String customFilter) {
    navigator.pageTotals(true);
    enrichCollectionFilter(navigator);
    enrichBuilder(navigator);
    String endpointPath = navigator.build();
    if (!customFilter.isEmpty()) {
      endpointPath = enrichWithCustomFilter(endpointPath, customFilter);
    }

    return getFromEndpointWithPageCount(endpointPath);
  }

  @NotNull
  private <T extends ElideEntity> Mono<Tuple2<List<T>, Integer>> getFromEndpointWithPageCount(String endpointPath) {
    return retrieveMonoWithErrorHandling(JSONAPIDocument.class, webClient.get().uri(endpointPath))
        .map(jsonapiDocument -> (JSONAPIDocument<List<T>>) jsonapiDocument)
        .flatMap(document -> Mono.zip(
            Mono.fromCallable(document::get),
            Mono.fromCallable(document::getMeta)
                .map(meta -> ((java.util.Map<String, Integer>) meta.get("page")).get("totalPages"))))
        .switchIfEmpty(Mono.zip(Mono.just(List.of()), Mono.just(0)))
        .cache()
        .doOnNext(tuple -> log.debug("Retrieved {} from {}", tuple.getT1(), endpointPath));
  }

  private <T> Mono<T> retrieveMonoWithErrorHandling(Class<T> type, WebClient.RequestHeadersSpec<?> requestSpec) {
    return retrieveWithErrorHandling(requestSpec)
        .bodyToMono(type)
        .retryWhen(apiRetrySpec);
  }

  private <T> Flux<T> retrieveFluxWithErrorHandling(Class<T> type, WebClient.RequestHeadersSpec<?> requestSpec) {
    return retrieveWithErrorHandling(requestSpec)
        .bodyToFlux(type)
        .retryWhen(apiRetrySpec);
  }

  private WebClient.ResponseSpec retrieveWithErrorHandling(WebClient.RequestHeadersSpec<?> requestSpec) {
    try {
      authorizedLatch.await();
    } catch (InterruptedException e) {
      log.warn("Api thread interrupted while waiting for authorization, will retry", e);
      return retrieveWithErrorHandling(requestSpec);
    }
    return requestSpec
        .retrieve()
        .onStatus(HttpStatus::isError, response -> {
          HttpStatus httpStatus = response.statusCode();
          if (httpStatus.equals(HttpStatus.BAD_REQUEST) || httpStatus.equals(HttpStatus.UNPROCESSABLE_ENTITY)) {
            /* onStatus expects a mono which emits an exception so here we map it to an Exception, however
              this map is never executed since bodyToMono will throw its own ResourceParseException if there are
              any errors in the JSONAPIDocument which we expect with a BAD REQUEST and UNPROCESSABLE response so this
              mapping only exists to satisfy the typing of onStatus*/
            return response.bodyToMono(JSONAPIDocument.class)
                .flatMap(jsonapiDocument -> response.createException())
                .onErrorMap(ResourceParseException.class, exception -> new ApiException(exception.getErrors().getErrors()));
          } else if (httpStatus.equals(HttpStatus.SERVICE_UNAVAILABLE)) {
            return response.createException().map(error -> new UnreachableApiException("API is unreachable", error));
          } else {
            return response.createException();
          }
        });
  }

  private void enrichBuilder(ElideEndpointBuilder<?> endpointBuilder) {
    for (String include : INCLUDES.getOrDefault(endpointBuilder.getDtoClass(), List.of())) {
      endpointBuilder.addInclude(include);
    }
  }

  private void enrichCollectionFilter(ElideNavigatorOnCollection<?> navigator) {
    List<Condition<?>> additionalConditions = FILTERS.getOrDefault(navigator.getDtoClass(), List.of());
    if (!additionalConditions.isEmpty()) {
      Optional<Condition<?>> currentFilter = navigator.getFilter();
      currentFilter.ifPresentOrElse(condition -> navigator.setFilter(new QBuilder().and(additionalConditions).and().and(List.of(condition))),
          () -> navigator.setFilter(new QBuilder().and(additionalConditions))
      );
    }
  }

  private String enrichWithCustomFilter(String endpoint, String customFilter) {
    String filterHeader = "filter=";
    int startIndex = endpoint.indexOf(filterHeader);
    if (startIndex == -1) {
      return endpoint + "&" + filterHeader + customFilter;
    }

    int endIndex = endpoint.indexOf("&", startIndex);
    if (endIndex == -1) {
      endIndex = endpoint.length();
    }

    String currentFilter = endpoint.substring(startIndex + filterHeader.length(), endIndex);
    String enrichedFilter = String.format("(%s);%s", currentFilter, customFilter);
    return endpoint.replace(currentFilter, enrichedFilter);
  }
}
