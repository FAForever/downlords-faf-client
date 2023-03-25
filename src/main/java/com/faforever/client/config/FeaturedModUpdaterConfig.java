package com.faforever.client.config;

import com.faforever.client.mod.ModService;
import com.faforever.client.patch.GameBinariesUpdateTask;
import com.faforever.client.patch.GameUpdater;
import com.faforever.client.patch.GameUpdaterImpl;
import com.faforever.client.patch.SimpleHttpFeaturedModUpdater;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.task.TaskService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class FeaturedModUpdaterConfig {

  private final ModService modService;
  private final ObjectFactory<GameBinariesUpdateTask> gameBinariesUpdateTaskFactory;
  private final TaskService taskService;
  private final SimpleHttpFeaturedModUpdater httpFeaturedModUpdater;
  private final DataPrefs dataPrefs;
  private final ForgedAlliancePrefs forgedAlliancePrefs;

  @Bean
  GameUpdater gameUpdater() {
    return new GameUpdaterImpl(modService, taskService, dataPrefs, forgedAlliancePrefs, gameBinariesUpdateTaskFactory)
        .addFeaturedModUpdater(httpFeaturedModUpdater);
  }
}
