package com.faforever.client.config;

import com.faforever.client.chat.ChannelTabController;
import com.faforever.client.chat.ChatController;
import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.PlayerInfoTooltipController;
import com.faforever.client.chat.PrivateChatTabController;
import com.faforever.client.chat.UrlPreviewResolver;
import com.faforever.client.fx.FxmlLoader;
import com.faforever.client.fx.FxmlLoaderImpl;
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
  ChannelTabController channelTab() {
    return mock(ChannelTabController.class);
  }

  @Bean
  PrivateChatTabController privateChatTab() {
    return mock(PrivateChatTabController.class);
  }

  @Bean
  UrlPreviewResolver urlPreviewResolver() {
    return mock(UrlPreviewResolver.class);
  }
}
