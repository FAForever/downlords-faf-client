package com.faforever.client.config;

import com.faforever.client.chat.ChatController;
import com.faforever.client.chat.ChatTabFactory;
import com.faforever.client.chat.ChatUserControlFactory;
import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.PlayerInfoTooltipController;
import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.fxml.FxmlLoaderImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.mockito.Mockito.mock;

@Configuration
@Import(BaseConfig.class)
public class TestUiConfiguration {

  @Bean
  FxmlLoader fxmlLoader() {
    return new FxmlLoaderImpl();
  }

  @Bean
  PlayerInfoTooltipController playerInfoTooltipController() {
    return mock(PlayerInfoTooltipController.class);
  }

  @Bean
  CountryFlagService countryFlagService() {
    return mock(CountryFlagService.class);
  }

  @Bean
  ChatController chatController() {
    return mock(ChatController.class);
  }

  @Bean
  ChatTabFactory chatTabFactory() {
    return mock(ChatTabFactory.class);
  }

  @Bean
  ChatUserControlFactory chatUserControlFactory() {
    return mock(ChatUserControlFactory.class);
  }
}
