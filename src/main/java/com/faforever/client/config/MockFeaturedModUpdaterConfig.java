package com.faforever.client.config;

import com.faforever.client.patch.GameUpdater;
import com.faforever.client.patch.GameUpdaterImpl;
import com.faforever.client.update.MockFeaturedModUpdater;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.inject.Inject;

@Configuration
@Profile("local")
public class MockFeaturedModUpdaterConfig {

  @Inject
  private MockFeaturedModUpdater mockFeaturedModUpdater;

  @Bean
  GameUpdater gameUpdater() {
    return new GameUpdaterImpl()
        .addFeaturedModUpdater(mockFeaturedModUpdater);
  }
}
