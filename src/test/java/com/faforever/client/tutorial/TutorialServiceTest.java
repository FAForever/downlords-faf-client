package com.faforever.client.tutorial;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.TutorialCategoryBeanBuilder;
import com.faforever.client.domain.TutorialCategoryBean;
import com.faforever.client.game.GameService;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mapstruct.TutorialMapper;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.elide.ElideEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TutorialServiceTest extends ServiceTest {

  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private GameService gameService;
  @Spy
  private final TutorialMapper tutorialMapper = Mappers.getMapper(TutorialMapper.class);
  @InjectMocks
  private TutorialService instance;

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(tutorialMapper);
  }

  @Test
  public void testGetTutorialCategories() throws Exception {
    TutorialCategoryBean tutorialCategoryBean = TutorialCategoryBeanBuilder.create().defaultValues().get();
    Flux<ElideEntity> resultFlux = Flux.just(tutorialMapper.map(tutorialCategoryBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    StepVerifier.create(instance.getTutorialCategories()).expectNext(tutorialCategoryBean).verifyComplete();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasPageSize(1000)));
  }
}
