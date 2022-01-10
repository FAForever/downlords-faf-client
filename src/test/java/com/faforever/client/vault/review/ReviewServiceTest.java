package com.faforever.client.vault.review;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.MapVersionBeanBuilder;
import com.faforever.client.builders.MapVersionReviewBeanBuilder;
import com.faforever.client.builders.ModVersionBeanBuilder;
import com.faforever.client.builders.ModVersionReviewBeanBuilder;
import com.faforever.client.builders.ReplayBeanBuilder;
import com.faforever.client.builders.ReplayReviewBeanBuilder;
import com.faforever.client.domain.MapVersionReviewBean;
import com.faforever.client.domain.ModVersionReviewBean;
import com.faforever.client.domain.ReplayReviewBean;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mapstruct.ReviewMapper;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.elide.ElideEntity;
import com.faforever.commons.api.elide.ElideNavigatorOnId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReviewServiceTest extends ServiceTest {
  @InjectMocks
  private ReviewService instance;

  @Mock
  private FafApiAccessor fafApiAccessor;
  @Spy
  private final ReviewMapper reviewMapper = Mappers.getMapper(ReviewMapper.class);

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(reviewMapper);
  }

  @Test
  public void saveNewGameReview() throws Exception {
    ReplayReviewBean reviewBean = ReplayReviewBeanBuilder.create().defaultValues().get();
    Mono<ElideEntity> resultMono = Mono.just(reviewMapper.map(reviewBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.post(any(), any())).thenReturn(resultMono);

    instance.saveReplayReview(ReplayReviewBeanBuilder.create().defaultValues().replay(ReplayBeanBuilder.create().defaultValues().get()).get());
    verify(fafApiAccessor).post(argThat(
        ElideMatchers.hasRelationship("reviews")
    ), any());
  }

  @Test
  public void saveNewMapVersionReview() throws Exception {
    MapVersionReviewBean reviewBean = MapVersionReviewBeanBuilder.create().defaultValues().get();
    Mono<ElideEntity> resultMono = Mono.just(reviewMapper.map(reviewBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.post(any(), any())).thenReturn(resultMono);

    instance.saveMapVersionReview(MapVersionReviewBeanBuilder.create().defaultValues().mapVersion(MapVersionBeanBuilder.create().defaultValues().get()).get());
    verify(fafApiAccessor).post(argThat(
        ElideMatchers.hasRelationship("reviews")
    ), any());
  }

  @Test
  public void saveNewModVersionReview() throws Exception {
    ModVersionReviewBean reviewBean = ModVersionReviewBeanBuilder.create().defaultValues().get();
    Mono<ElideEntity> resultMono = Mono.just(reviewMapper.map(reviewBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.post(any(), any())).thenReturn(resultMono);

    instance.saveModVersionReview(ModVersionReviewBeanBuilder.create().defaultValues().modVersion(ModVersionBeanBuilder.create().defaultValues().get()).get());
    verify(fafApiAccessor).post(argThat(
        ElideMatchers.hasRelationship("reviews")
    ), any());
  }

  @Test
  public void updateGameReview() throws Exception {
    when(fafApiAccessor.patch(any(), any())).thenReturn(Mono.empty());

    instance.saveReplayReview(ReplayReviewBeanBuilder.create().defaultValues().replay(ReplayBeanBuilder.create().defaultValues().get()).id(0).get());
    verify(fafApiAccessor).patch(any(ElideNavigatorOnId.class), any());
  }

  @Test
  public void updateMapVersionReview() throws Exception {
    when(fafApiAccessor.patch(any(), any())).thenReturn(Mono.empty());

    instance.saveMapVersionReview(MapVersionReviewBeanBuilder.create().defaultValues().mapVersion(MapVersionBeanBuilder.create().defaultValues().get()).id(0).get());
    verify(fafApiAccessor).patch(any(ElideNavigatorOnId.class), any());
  }

  @Test
  public void updateModVersionReview() throws Exception {
    when(fafApiAccessor.patch(any(), any())).thenReturn(Mono.empty());

    instance.saveModVersionReview(ModVersionReviewBeanBuilder.create().defaultValues().modVersion(ModVersionBeanBuilder.create().defaultValues().get()).id(0).get());
    verify(fafApiAccessor).patch(any(ElideNavigatorOnId.class), any());
  }

}
