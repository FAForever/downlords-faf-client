package com.faforever.client.tutorial;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.domain.api.Tutorial;
import com.faforever.client.domain.api.TutorialCategory;
import com.faforever.client.game.GameRunner;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.TutorialMapper;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutorialService {
  private final FafApiAccessor fafApiAccessor;
  private final GameRunner gameRunner;
  private final TutorialMapper tutorialMapper;

  public Flux<TutorialCategory> getTutorialCategories() {
    ElideNavigatorOnCollection<com.faforever.commons.api.dto.TutorialCategory> navigator = ElideNavigator.of(
        com.faforever.commons.api.dto.TutorialCategory.class).collection().pageSize(1000);
    return fafApiAccessor.getMany(navigator)
        .map(dto -> tutorialMapper.map(dto, new CycleAvoidingMappingContext())).cache();
  }

  public void launchTutorial(Tutorial tutorial) {
    gameRunner.launchTutorial(tutorial.mapVersion(), tutorial.technicalName());
  }
}
