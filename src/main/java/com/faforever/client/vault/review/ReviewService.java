package com.faforever.client.vault.review;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapVersionReviewBean;
import com.faforever.client.domain.ModBean;
import com.faforever.client.domain.ModVersionReviewBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.domain.ReplayReviewBean;
import com.faforever.client.domain.ReviewBean;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.ReviewMapper;
import com.faforever.commons.api.dto.Game;
import com.faforever.commons.api.dto.GameReview;
import com.faforever.commons.api.dto.Map;
import com.faforever.commons.api.dto.MapVersion;
import com.faforever.commons.api.dto.MapVersionReview;
import com.faforever.commons.api.dto.Mod;
import com.faforever.commons.api.dto.ModVersion;
import com.faforever.commons.api.dto.ModVersionReview;
import com.faforever.commons.api.dto.Review;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import com.faforever.commons.api.elide.ElideNavigatorOnId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ReviewService {

  private final FafApiAccessor fafApiAccessor;
  private final ReviewMapper reviewMapper;

  @SuppressWarnings("unchecked")
  public <R extends ReviewBean<R, ?>> Mono<R> saveReview(R review) {
    Assert.notNull(review.player(), "Player must be set");
    Assert.notNull(review.subject(), "Subject must be set");
    return switch (review) {
      case ReplayReviewBean replayReview -> (Mono<R>) saveReplayReview(replayReview);
      case MapVersionReviewBean mapReview -> (Mono<R>) saveMapVersionReview(mapReview);
      case ModVersionReviewBean modReview -> (Mono<R>) saveModVersionReview(modReview);
    };
  }

  private Mono<ReplayReviewBean> saveReplayReview(ReplayReviewBean review) {
    GameReview gameReview = reviewMapper.map(review, new CycleAvoidingMappingContext());
    if (gameReview.getId() == null) {
      ElideNavigatorOnCollection<GameReview> navigator = ElideNavigator.of(gameReview.getGame())
          .navigateRelationship(GameReview.class, "reviews")
          .collection();
      gameReview.setGame(null);
      return fafApiAccessor.post(navigator, gameReview)
          .map(dto -> reviewMapper.map(dto, new CycleAvoidingMappingContext()));
    } else {
      ElideNavigatorOnId<GameReview> endpointBuilder = ElideNavigator.of(gameReview);
      gameReview.setGame(null);
      return fafApiAccessor.patch(endpointBuilder, gameReview).thenReturn(review);
    }

  }

  private Mono<ModVersionReviewBean> saveModVersionReview(ModVersionReviewBean review) {
    ModVersionReview modVersionReview = reviewMapper.map(review, new CycleAvoidingMappingContext());
    if (modVersionReview.getId() == null) {
      ElideNavigatorOnCollection<ModVersionReview> navigator = ElideNavigator.of(modVersionReview.getModVersion())
          .navigateRelationship(ModVersionReview.class, "reviews")
          .collection();
      modVersionReview.setModVersion(null);
      return fafApiAccessor.post(navigator, modVersionReview)
          .map(dto -> reviewMapper.map(dto, new CycleAvoidingMappingContext()));
    } else {
      ElideNavigatorOnId<ModVersionReview> endpointBuilder = ElideNavigator.of(modVersionReview);
      modVersionReview.setModVersion(null);
      return fafApiAccessor.patch(endpointBuilder, modVersionReview).thenReturn(review);
    }
  }

  private Mono<MapVersionReviewBean> saveMapVersionReview(MapVersionReviewBean review) {
    MapVersionReview mapVersionReview = reviewMapper.map(review, new CycleAvoidingMappingContext());
    if (mapVersionReview.getId() == null) {
      ElideNavigatorOnCollection<MapVersionReview> navigator = ElideNavigator.of(mapVersionReview.getMapVersion())
          .navigateRelationship(MapVersionReview.class, "reviews")
          .collection();
      mapVersionReview.setMapVersion(null);
      return fafApiAccessor.post(navigator, mapVersionReview)
          .map(dto -> reviewMapper.map(dto, new CycleAvoidingMappingContext()));
    } else {
      mapVersionReview.setMapVersion(null);
      ElideNavigatorOnId<MapVersionReview> endpointBuilder = ElideNavigator.of(mapVersionReview);
      return fafApiAccessor.patch(endpointBuilder, mapVersionReview).thenReturn(review);
    }

  }

  public Mono<Void> deleteReview(ReviewBean<?, ?> review) {
    Review reviewDto = reviewMapper.map(review, new CycleAvoidingMappingContext());
    ElideNavigatorOnId<Review> endpointBuilder = ElideNavigator.of(reviewDto);
    return fafApiAccessor.delete(endpointBuilder);
  }

  public Flux<MapVersionReviewBean> getMapReviews(MapBean map) {
    ElideNavigatorOnCollection<MapVersion> versionsNavigator = ElideNavigator.of(Map.class)
        .id(String.valueOf(map.getId()))
        .navigateRelationship(MapVersion.class, "versions")
        .collection()
        .addInclude("reviews")
        .addInclude("reviews.player");

    return fafApiAccessor.getMany(versionsNavigator)
        .map(MapVersion::getReviews)
        .flatMap(Flux::fromIterable)
        .map(mapReview -> reviewMapper.map(mapReview, new CycleAvoidingMappingContext()));
  }

  public Flux<ModVersionReviewBean> getModReviews(ModBean mod) {
    ElideNavigatorOnCollection<ModVersion> versionsNavigator = ElideNavigator.of(Mod.class)
        .id(String.valueOf(mod.getId()))
        .navigateRelationship(ModVersion.class, "versions")
        .collection()
        .addInclude("reviews")
        .addInclude("reviews.player");

    return fafApiAccessor.getMany(versionsNavigator)
        .map(ModVersion::getReviews)
        .flatMap(Flux::fromIterable)
        .map(mapReview -> reviewMapper.map(mapReview, new CycleAvoidingMappingContext()));
  }

  public Flux<ReplayReviewBean> getReplayReviews(ReplayBean replay) {
    ElideNavigatorOnCollection<GameReview> versionsNavigator = ElideNavigator.of(Game.class)
        .id(String.valueOf(replay.getId()))
        .navigateRelationship(GameReview.class, "reviews")
        .collection()
        .addInclude("player");

    return fafApiAccessor.getMany(versionsNavigator)
        .map(gameReview -> reviewMapper.map(gameReview, new CycleAvoidingMappingContext()));
  }
}
