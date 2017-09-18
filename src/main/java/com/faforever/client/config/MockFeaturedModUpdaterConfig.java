package com.faforever.client.config;

import com.faforever.client.FafClientApplication;
import com.faforever.client.game.FaInitGenerator;
import com.faforever.client.mod.ModService;
import com.faforever.client.patch.FeaturedModUpdater;
import com.faforever.client.patch.GameUpdater;
import com.faforever.client.patch.GameUpdaterImpl;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.TaskService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.inject.Inject;

@Configuration
@Profile(FafClientApplication.PROFILE_OFFLINE)
public class MockFeaturedModUpdaterConfig {

  @Inject
  private ModService modService;
  @Inject
  private ApplicationContext applicationContext;
  @Inject
  private TaskService taskService;
  @Inject
  private FafService fafService;
  @Inject
  private FaInitGenerator faInitGenerator;
  @Inject
  private FeaturedModUpdater featuredModUpdater;

  @Bean
  GameUpdater gameUpdater() {
    return new GameUpdaterImpl(modService, applicationContext, taskService, fafService, faInitGenerator)
        .addFeaturedModUpdater(featuredModUpdater);
  }
}
