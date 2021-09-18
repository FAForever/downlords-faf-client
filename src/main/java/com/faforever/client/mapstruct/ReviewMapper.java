package com.faforever.client.mapstruct;

import com.faforever.client.domain.MapReviewsSummaryBean;
import com.faforever.client.domain.MapVersionReviewBean;
import com.faforever.client.domain.MapVersionReviewsSummaryBean;
import com.faforever.client.domain.ModReviewsSummaryBean;
import com.faforever.client.domain.ModVersionReviewBean;
import com.faforever.client.domain.ModVersionReviewsSummaryBean;
import com.faforever.client.domain.ReplayReviewBean;
import com.faforever.client.domain.ReplayReviewsSummaryBean;
import com.faforever.commons.api.dto.GameReview;
import com.faforever.commons.api.dto.GameReviewsSummary;
import com.faforever.commons.api.dto.MapReviewsSummary;
import com.faforever.commons.api.dto.MapVersionReview;
import com.faforever.commons.api.dto.MapVersionReviewsSummary;
import com.faforever.commons.api.dto.ModReviewsSummary;
import com.faforever.commons.api.dto.ModVersionReview;
import com.faforever.commons.api.dto.ModVersionReviewsSummary;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ReplayMapper.class, ModMapper.class, MapMapper.class, PlayerMapper.class}, config = MapperConfiguration.class)
public interface ReviewMapper {

  @Mapping(target = "replay", source = "game")
  ReplayReviewBean map(GameReview dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "game", source = "replay")
  GameReview map(ReplayReviewBean bean, @Context CycleAvoidingMappingContext context);

  MapVersionReviewBean map(MapVersionReview dto, @Context CycleAvoidingMappingContext context);

  MapVersionReview map(MapVersionReviewBean bean, @Context CycleAvoidingMappingContext context);

  ModVersionReviewBean map(ModVersionReview dto, @Context CycleAvoidingMappingContext context);

  ModVersionReview map(ModVersionReviewBean bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "replay", source = "game")
  ReplayReviewsSummaryBean map(GameReviewsSummary dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "game", source = "replay")
  GameReviewsSummary map(ReplayReviewsSummaryBean bean, @Context CycleAvoidingMappingContext context);

  MapVersionReviewsSummaryBean map(MapVersionReviewsSummary dto, @Context CycleAvoidingMappingContext context);

  MapVersionReviewsSummary map(MapVersionReviewsSummaryBean bean, @Context CycleAvoidingMappingContext context);

  ModVersionReviewsSummaryBean map(ModVersionReviewsSummary dto, @Context CycleAvoidingMappingContext context);

  ModVersionReviewsSummary map(ModVersionReviewsSummaryBean bean, @Context CycleAvoidingMappingContext context);

  MapReviewsSummaryBean map(MapReviewsSummary dto, @Context CycleAvoidingMappingContext context);

  MapReviewsSummary map(MapReviewsSummaryBean bean, @Context CycleAvoidingMappingContext context);

  ModReviewsSummaryBean map(ModReviewsSummary dto, @Context CycleAvoidingMappingContext context);

  ModReviewsSummary map(ModReviewsSummaryBean bean, @Context CycleAvoidingMappingContext context);
}
