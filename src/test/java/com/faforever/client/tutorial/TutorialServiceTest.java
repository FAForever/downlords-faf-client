package com.faforever.client.tutorial;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.game.GameService;
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
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());
    instance.getTutorialCategories().join();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasPageSize(1000)));
  }
}
