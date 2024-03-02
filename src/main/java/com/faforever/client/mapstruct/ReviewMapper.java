package com.faforever.client.mapstruct;

import com.faforever.client.domain.api.MapVersionReview;
import com.faforever.client.domain.api.ModVersionReview;
import com.faforever.client.domain.api.ReplayReview;
import com.faforever.client.domain.api.ReviewBean;
import com.faforever.client.domain.api.ReviewsSummary;
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
  ReviewsSummary map(GameReviewsSummary dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "reviews", source = "numReviews")
  @Mapping(target = "game", ignore = true)
  GameReviewsSummary mapToGame(ReviewsSummary bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "numReviews", source = "reviews")
  ReviewsSummary map(com.faforever.commons.api.dto.MapReviewsSummary dto,
                        @Context CycleAvoidingMappingContext context);

  @Mapping(target = "reviews", source = "numReviews")
  @Mapping(target = "map", ignore = true)
  com.faforever.commons.api.dto.MapReviewsSummary mapToMap(ReviewsSummary bean,
                                                      @Context CycleAvoidingMappingContext context);

  @Mapping(target = "numReviews", source = "reviews")
  ReviewsSummary map(com.faforever.commons.api.dto.ModReviewsSummary dto,
                        @Context CycleAvoidingMappingContext context);

  @Mapping(target = "reviews", source = "numReviews")
  @Mapping(target = "mod", ignore = true)
  com.faforever.commons.api.dto.ModReviewsSummary mapToMod(ReviewsSummary bean,
                                                      @Context CycleAvoidingMappingContext context);
}
