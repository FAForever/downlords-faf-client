package com.faforever.client.vault.review;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.domain.api.MapVersionReview;
import com.faforever.client.domain.api.ModVersion;
import com.faforever.client.domain.api.ModVersionReview;
import com.faforever.client.domain.api.ReplayReview;
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
    ReplayReview reviewBean = Instancio.of(ReplayReview.class).ignore(field(ReplayReview::id)).create();
    Mono<ElideEntity> resultMono = Mono.just(reviewMapper.map(reviewBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.post(any(), any())).thenReturn(resultMono);

    StepVerifier.create(instance.saveReview(Instancio.of(ReplayReview.class).ignore(field(ReplayReview::id))
                                                     .create())).expectNextCount(1).verifyComplete();
    verify(fafApiAccessor).post(argThat(
        ElideMatchers.hasRelationship("reviews")
    ), any());
  }

  @Test
  public void saveNewMapVersionReview() throws Exception {
    MapVersionReview reviewBean = Instancio.of(MapVersionReview.class).ignore(field(MapVersionReview::id)).create();
    Mono<ElideEntity> resultMono = Mono.just(reviewMapper.map(reviewBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.post(any(), any())).thenReturn(resultMono);

    StepVerifier.create(instance.saveReview(Instancio.of(MapVersionReview.class)
                                                     .ignore(field(MapVersionReview::id))
                                                     .set(field(MapVersionReview::subject),
                                                          Instancio.create(MapVersion.class))
                                                     .create())).expectNextCount(1).verifyComplete();
    verify(fafApiAccessor).post(argThat(
        ElideMatchers.hasRelationship("reviews")
    ), any());
  }

  @Test
  public void saveNewModVersionReview() throws Exception {
    ModVersionReview reviewBean = Instancio.create(ModVersionReview.class);
    Mono<ElideEntity> resultMono = Mono.just(reviewMapper.map(reviewBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.post(any(), any())).thenReturn(resultMono);

    StepVerifier.create(instance.saveReview(Instancio.of(ModVersionReview.class)
                                                     .ignore(field(ModVersionReview::id))
                                                     .set(field(ModVersionReview::subject),
                                                          Instancio.create(ModVersion.class))
                                                     .create())).expectNextCount(1).verifyComplete();
    verify(fafApiAccessor).post(argThat(
        ElideMatchers.hasRelationship("reviews")
    ), any());
  }

  @Test
  public void updateGameReview() throws Exception {
    when(fafApiAccessor.patch(any(), any())).thenReturn(Mono.empty());

    StepVerifier.create(instance.saveReview(Instancio.of(ReplayReview.class)
                                                     .create())).expectNextCount(1).verifyComplete();
    verify(fafApiAccessor).patch(any(ElideNavigatorOnId.class), any());
  }

  @Test
  public void updateMapVersionReview() throws Exception {
    when(fafApiAccessor.patch(any(), any())).thenReturn(Mono.empty());

    StepVerifier.create(instance.saveReview(Instancio.create(MapVersionReview.class)))
                .expectNextCount(1)
                .verifyComplete();
    verify(fafApiAccessor).patch(any(ElideNavigatorOnId.class), any());
  }

  @Test
  public void updateModVersionReview() throws Exception {
    when(fafApiAccessor.patch(any(), any())).thenReturn(Mono.empty());

    StepVerifier.create(instance.saveReview(Instancio.of(ModVersionReview.class).create()))
                .expectNextCount(1)
                .verifyComplete();
    verify(fafApiAccessor).patch(any(ElideNavigatorOnId.class), any());
  }

}
