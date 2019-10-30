package com.faforever.client.tutorial;

import com.faforever.client.game.GameService;
import com.faforever.client.remote.FafService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class TutorialService {
  private final FafService fafService;
  private final GameService gameService;

  @Inject
  public TutorialService(FafService fafService, GameService gameService) {
    this.fafService = fafService;
    this.gameService = gameService;
  }

  public CompletableFuture<List<TutorialCategory>> getTutorialCategories() {
    return fafService.getTutorialCategories();
  }

  public void launchTutorial(Tutorial tutorial) {
    gameService.launchTutorial(tutorial.getMapVersion(), tutorial.getTechnicalName());
  }
}
