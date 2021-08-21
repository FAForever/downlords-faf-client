package com.faforever.client.tutorial;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.domain.TutorialBean;
import com.faforever.client.domain.TutorialCategoryBean;
import com.faforever.client.game.GameService;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.TutorialMapper;
import com.faforever.commons.api.dto.TutorialCategory;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutorialService {
  private final FafApiAccessor fafApiAccessor;
  private final GameService gameService;
  private final TutorialMapper tutorialMapper;

  public CompletableFuture<List<TutorialCategoryBean>> getTutorialCategories() {
    ElideNavigatorOnCollection<TutorialCategory> navigator = ElideNavigator.of(TutorialCategory.class).collection()
        .pageSize(1000);
    return fafApiAccessor.getMany(navigator)
        .map(dto -> tutorialMapper.map(dto, new CycleAvoidingMappingContext()))
        .collectList()
        .toFuture();
  }

  public void launchTutorial(TutorialBean tutorial) {
    gameService.launchTutorial(tutorial.getMapVersion(), tutorial.getTechnicalName());
  }
}
