package com.faforever.client.mapstruct;

import com.faforever.client.domain.api.MapReviewsSummary;
import com.faforever.client.domain.api.MapVersionReview;
import com.faforever.client.domain.api.MapVersionReviewsSummary;
import com.faforever.client.domain.api.ModReviewsSummary;
import com.faforever.client.domain.api.ModVersionReview;
import com.faforever.client.domain.api.ModVersionReviewsSummary;
import com.faforever.client.domain.api.ReplayReview;
import com.faforever.client.domain.api.ReplayReviewsSummary;
import com.faforever.client.domain.api.ReviewBean;
import com.faforever.commons.api.dto.GameReview;
import com.faforever.commons.api.dto.GameReviewsSummary;
import com.faforever.commons.api.dto.Review;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = {ReplayMapper.class, ModMapper.class, MapMapper.class, PlayerMapper.class}, config = MapperConfiguration.class)
public interface ReviewMapper {

  default ReviewBean<?> map(Review dto, @Context CycleAvoidingMappingContext context) {
    return switch (dto) {
      case GameReview replayReview -> map(replayReview, context);
      case com.faforever.commons.api.dto.MapVersionReview mapReview -> map(mapReview, context);
      case com.faforever.commons.api.dto.ModVersionReview modReview -> map(modReview, context);
      default -> throw new UnsupportedOperationException("Cannot map reviews of type: " + dto.getClass());
    };
  }

  default Review map(ReviewBean<?> bean, @Context CycleAvoidingMappingContext context) {
    return switch (bean) {
      case ReplayReview replayReview -> map(replayReview, context);
      case MapVersionReview mapReview -> map(mapReview, context);
      case ModVersionReview modReview -> map(modReview, context);
    };
  }

  @Mapping(target = "subject", source = "game")
  ReplayReview map(GameReview dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "game", source = "subject")
  GameReview map(ReplayReview bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "subject", source = "mapVersion")
  MapVersionReview map(com.faforever.commons.api.dto.MapVersionReview dto,
                       @Context CycleAvoidingMappingContext context);

  @Mapping(target = "mapVersion", source = "subject")
  com.faforever.commons.api.dto.MapVersionReview map(MapVersionReview bean,
                                                     @Context CycleAvoidingMappingContext context);

  @Mapping(target = "subject", source = "modVersion")
  ModVersionReview map(com.faforever.commons.api.dto.ModVersionReview dto,
                       @Context CycleAvoidingMappingContext context);

  @Mapping(target = "modVersion", source = "subject")
  com.faforever.commons.api.dto.ModVersionReview map(ModVersionReview bean,
                                                     @Context CycleAvoidingMappingContext context);

  @Mapping(target = "numReviews", source = "reviews")
  @Mapping(target = "subject", source = "game")
  ReplayReviewsSummary map(GameReviewsSummary dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "reviews", source = "numReviews")
  @Mapping(target = "game", source = "subject")
  GameReviewsSummary map(ReplayReviewsSummary bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "numReviews", source = "reviews")
  @Mapping(target = "subject", source = "mapVersion")
  MapVersionReviewsSummary map(com.faforever.commons.api.dto.MapVersionReviewsSummary dto,
                               @Context CycleAvoidingMappingContext context);

  @Mapping(target = "reviews", source = "numReviews")
  @Mapping(target = "mapVersion", source = "subject")
  com.faforever.commons.api.dto.MapVersionReviewsSummary map(MapVersionReviewsSummary bean,
                                                             @Context CycleAvoidingMappingContext context);

  @Mapping(target = "numReviews", source = "reviews")
  @Mapping(target = "subject", source = "modVersion")
  ModVersionReviewsSummary map(com.faforever.commons.api.dto.ModVersionReviewsSummary dto,
                               @Context CycleAvoidingMappingContext context);

  @Mapping(target = "reviews", source = "numReviews")
  @Mapping(target = "modVersion", source = "subject")
  com.faforever.commons.api.dto.ModVersionReviewsSummary map(ModVersionReviewsSummary bean,
                                                             @Context CycleAvoidingMappingContext context);

  @Mapping(target = "numReviews", source = "reviews")
  @Mapping(target = "subject", source = "map")
  MapReviewsSummary map(com.faforever.commons.api.dto.MapReviewsSummary dto,
                        @Context CycleAvoidingMappingContext context);

  @Mapping(target = "reviews", source = "numReviews")
  @Mapping(target = "map", source = "subject")
  com.faforever.commons.api.dto.MapReviewsSummary map(MapReviewsSummary bean,
                                                      @Context CycleAvoidingMappingContext context);

  @Mapping(target = "numReviews", source = "reviews")
  @Mapping(target = "subject", source = "mod")
  ModReviewsSummary map(com.faforever.commons.api.dto.ModReviewsSummary dto,
                        @Context CycleAvoidingMappingContext context);

  @Mapping(target = "reviews", source = "numReviews")
  @Mapping(target = "mod", source = "subject")
  com.faforever.commons.api.dto.ModReviewsSummary map(ModReviewsSummary bean,
                                                      @Context CycleAvoidingMappingContext context);
}
