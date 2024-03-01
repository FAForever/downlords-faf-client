package com.faforever.client.mapstruct;

import com.faforever.client.domain.MapReviewsSummaryBean;
import com.faforever.client.domain.MapVersionReviewBean;
import com.faforever.client.domain.MapVersionReviewsSummaryBean;
import com.faforever.client.domain.ModReviewsSummaryBean;
import com.faforever.client.domain.ModVersionReviewBean;
import com.faforever.client.domain.ModVersionReviewsSummaryBean;
import com.faforever.client.domain.ReplayReviewBean;
import com.faforever.client.domain.ReplayReviewsSummaryBean;
import com.faforever.client.domain.ReviewBean;
import com.faforever.commons.api.dto.GameReview;
import com.faforever.commons.api.dto.GameReviewsSummary;
import com.faforever.commons.api.dto.MapReviewsSummary;
import com.faforever.commons.api.dto.MapVersionReview;
import com.faforever.commons.api.dto.MapVersionReviewsSummary;
import com.faforever.commons.api.dto.ModReviewsSummary;
import com.faforever.commons.api.dto.ModVersionReview;
import com.faforever.commons.api.dto.ModVersionReviewsSummary;
import com.faforever.commons.api.dto.Review;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = {ReplayMapper.class, ModMapper.class, MapMapper.class, PlayerMapper.class}, config = MapperConfiguration.class)
public interface ReviewMapper {

  default ReviewBean<?> map(Review dto, @Context CycleAvoidingMappingContext context) {
    return switch (dto) {
      case GameReview replayReview -> map(replayReview, context);
      case MapVersionReview mapReview -> map(mapReview, context);
      case ModVersionReview modReview -> map(modReview, context);
      default -> throw new UnsupportedOperationException("Cannot map reviews of type: " + dto.getClass());
    };
  }

  default Review map(ReviewBean<?> bean, @Context CycleAvoidingMappingContext context) {
    return switch (bean) {
      case ReplayReviewBean replayReview -> map(replayReview, context);
      case MapVersionReviewBean mapReview -> map(mapReview, context);
      case ModVersionReviewBean modReview -> map(modReview, context);
    };
  }

  @Mapping(target = "subject", source = "game")
  ReplayReviewBean map(GameReview dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "game", source = "subject")
  GameReview map(ReplayReviewBean bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "subject", source = "mapVersion")
  MapVersionReviewBean map(MapVersionReview dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "mapVersion", source = "subject")
  MapVersionReview map(MapVersionReviewBean bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "subject", source = "modVersion")
  ModVersionReviewBean map(ModVersionReview dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "modVersion", source = "subject")
  ModVersionReview map(ModVersionReviewBean bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "numReviews", source = "reviews")
  @Mapping(target = "subject", source = "game")
  ReplayReviewsSummaryBean map(GameReviewsSummary dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "reviews", source = "numReviews")
  @Mapping(target = "game", source = "subject")
  GameReviewsSummary map(ReplayReviewsSummaryBean bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "numReviews", source = "reviews")
  @Mapping(target = "subject", source = "mapVersion")
  MapVersionReviewsSummaryBean map(MapVersionReviewsSummary dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "reviews", source = "numReviews")
  @Mapping(target = "mapVersion", source = "subject")
  MapVersionReviewsSummary map(MapVersionReviewsSummaryBean bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "numReviews", source = "reviews")
  @Mapping(target = "subject", source = "modVersion")
  ModVersionReviewsSummaryBean map(ModVersionReviewsSummary dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "reviews", source = "numReviews")
  @Mapping(target = "modVersion", source = "subject")
  ModVersionReviewsSummary map(ModVersionReviewsSummaryBean bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "numReviews", source = "reviews")
  @Mapping(target = "subject", source = "map")
  MapReviewsSummaryBean map(MapReviewsSummary dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "reviews", source = "numReviews")
  @Mapping(target = "map", source = "subject")
  MapReviewsSummary map(MapReviewsSummaryBean bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "numReviews", source = "reviews")
  @Mapping(target = "subject", source = "mod")
  ModReviewsSummaryBean map(ModReviewsSummary dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "reviews", source = "numReviews")
  @Mapping(target = "mod", source = "subject")
  ModReviewsSummary map(ModReviewsSummaryBean bean, @Context CycleAvoidingMappingContext context);
}
