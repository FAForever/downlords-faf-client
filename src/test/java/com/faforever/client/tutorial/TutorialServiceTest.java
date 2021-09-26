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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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

  private final TutorialMapper tutorialMapper = Mappers.getMapper(TutorialMapper.class);
  private TutorialService instance;
  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(tutorialMapper);
    instance = new TutorialService(fafApiAccessor, gameService, tutorialMapper);
  }

  @Test
  public void testGetTutorialCategories() throws Exception {
    TutorialCategoryBean tutorialCategoryBean = TutorialCategoryBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(tutorialMapper.map(tutorialCategoryBean, new CycleAvoidingMappingContext())));
    List<TutorialCategoryBean> results = instance.getTutorialCategories().join();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasPageSize(1000)));
    assertThat(results, contains(tutorialCategoryBean));
  }
}
