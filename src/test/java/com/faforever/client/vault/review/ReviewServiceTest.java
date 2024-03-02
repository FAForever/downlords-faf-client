package com.faforever.client.vault.review;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.MapVersionReviewBean;
import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.domain.ModVersionReviewBean;
import com.faforever.client.domain.ReplayReviewBean;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mapstruct.ReviewMapper;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.elide.ElideEntity;
import com.faforever.commons.api.elide.ElideNavigatorOnId;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.instancio.Select.field;
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
    ReplayReviewBean reviewBean = Instancio.of(ReplayReviewBean.class).ignore(field(ReplayReviewBean::id)).create();
    Mono<ElideEntity> resultMono = Mono.just(reviewMapper.map(reviewBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.post(any(), any())).thenReturn(resultMono);

    StepVerifier.create(instance.saveReview(Instancio.of(ReplayReviewBean.class).ignore(field(ReplayReviewBean::id))
                                                     .create())).expectNextCount(1).verifyComplete();
    verify(fafApiAccessor).post(argThat(
        ElideMatchers.hasRelationship("reviews")
    ), any());
  }

  @Test
  public void saveNewMapVersionReview() throws Exception {
    MapVersionReviewBean reviewBean = Instancio.of(MapVersionReviewBean.class).ignore(field(MapVersionReviewBean::id))
                                               .create();
    Mono<ElideEntity> resultMono = Mono.just(reviewMapper.map(reviewBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.post(any(), any())).thenReturn(resultMono);

    StepVerifier.create(instance.saveReview(Instancio.of(MapVersionReviewBean.class)
                                                     .ignore(field(MapVersionReviewBean::id))
                                                     .set(field(MapVersionReviewBean::subject),
                                                          Instancio.create(MapVersionBean.class))
                                                     .create())).expectNextCount(1).verifyComplete();
    verify(fafApiAccessor).post(argThat(
        ElideMatchers.hasRelationship("reviews")
    ), any());
  }

  @Test
  public void saveNewModVersionReview() throws Exception {
    ModVersionReviewBean reviewBean = Instancio.create(ModVersionReviewBean.class);
    Mono<ElideEntity> resultMono = Mono.just(reviewMapper.map(reviewBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.post(any(), any())).thenReturn(resultMono);

    StepVerifier.create(instance.saveReview(Instancio.of(ModVersionReviewBean.class)
                                                     .ignore(field(ModVersionReviewBean::id))
                                                     .set(field(ModVersionReviewBean::subject),
                                                          Instancio.create(ModVersionBean.class))
                                                     .create())).expectNextCount(1).verifyComplete();
    verify(fafApiAccessor).post(argThat(
        ElideMatchers.hasRelationship("reviews")
    ), any());
  }

  @Test
  public void updateGameReview() throws Exception {
    when(fafApiAccessor.patch(any(), any())).thenReturn(Mono.empty());

    StepVerifier.create(instance.saveReview(Instancio.of(ReplayReviewBean.class)
                                                     .create())).expectNextCount(1).verifyComplete();
    verify(fafApiAccessor).patch(any(ElideNavigatorOnId.class), any());
  }

  @Test
  public void updateMapVersionReview() throws Exception {
    when(fafApiAccessor.patch(any(), any())).thenReturn(Mono.empty());

    StepVerifier.create(instance.saveReview(Instancio.create(MapVersionReviewBean.class)))
                .expectNextCount(1)
                .verifyComplete();
    verify(fafApiAccessor).patch(any(ElideNavigatorOnId.class), any());
  }

  @Test
  public void updateModVersionReview() throws Exception {
    when(fafApiAccessor.patch(any(), any())).thenReturn(Mono.empty());

    StepVerifier.create(instance.saveReview(Instancio.of(ModVersionReviewBean.class).create()))
                .expectNextCount(1)
                .verifyComplete();
    verify(fafApiAccessor).patch(any(ElideNavigatorOnId.class), any());
  }

}
