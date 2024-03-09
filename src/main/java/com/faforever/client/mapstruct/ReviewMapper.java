package com.faforever.client.mapstruct;

import com.faforever.client.domain.api.MapVersionReview;
import com.faforever.client.domain.api.ModVersionReview;
import com.faforever.client.domain.api.ReplayReview;
import com.faforever.client.domain.api.ReviewBean;
import com.faforever.client.domain.api.ReviewsSummary;
import com.faforever.commons.api.dto.GameReview;
import com.faforever.commons.api.dto.GameReviewsSummary;
import com.faforever.commons.api.dto.Review;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = {ReplayMapper.class, ModMapper.class, MapMapper.class, PlayerMapper.class}, config = MapperConfiguration.class)
public interface ReviewMapper {

  default ReviewBean<?> map(Review dto) {
    return switch (dto) {
      case GameReview replayReview -> map(replayReview);
      case com.faforever.commons.api.dto.MapVersionReview mapReview -> map(mapReview);
      case com.faforever.commons.api.dto.ModVersionReview modReview -> map(modReview);
      default -> throw new UnsupportedOperationException("Cannot map reviews of type: " + dto.getClass());
    };
  }

  default Review map(ReviewBean<?> bean) {
    return switch (bean) {
      case ReplayReview replayReview -> map(replayReview);
      case MapVersionReview mapReview -> map(mapReview);
      case ModVersionReview modReview -> map(modReview);
    };
  }

  @Mapping(target = "subject", source = "game")
  ReplayReview map(GameReview dto);

  @InheritInverseConfiguration
  GameReview map(ReplayReview bean);

  @Mapping(target = "subject", source = "mapVersion")
  MapVersionReview map(com.faforever.commons.api.dto.MapVersionReview dto);

  @InheritInverseConfiguration
  com.faforever.commons.api.dto.MapVersionReview map(MapVersionReview bean);

  @Mapping(target = "subject", source = "modVersion")
  ModVersionReview map(com.faforever.commons.api.dto.ModVersionReview dto);

  @InheritInverseConfiguration
  com.faforever.commons.api.dto.ModVersionReview map(ModVersionReview bean);

  @Mapping(target = "numReviews", source = "reviews")
  ReviewsSummary map(GameReviewsSummary dto);

  @Mapping(target = "game", ignore = true)
  @InheritInverseConfiguration
  GameReviewsSummary mapToGame(ReviewsSummary bean);

  @Mapping(target = "numReviews", source = "reviews")
  ReviewsSummary map(com.faforever.commons.api.dto.MapReviewsSummary dto);

  @InheritInverseConfiguration
  @Mapping(target = "map", ignore = true)
  com.faforever.commons.api.dto.MapReviewsSummary mapToMap(ReviewsSummary bean);

  @Mapping(target = "numReviews", source = "reviews")
  ReviewsSummary map(com.faforever.commons.api.dto.ModReviewsSummary dto);

  @InheritInverseConfiguration
  @Mapping(target = "mod", ignore = true)
  com.faforever.commons.api.dto.ModReviewsSummary mapToMod(ReviewsSummary bean);
}
