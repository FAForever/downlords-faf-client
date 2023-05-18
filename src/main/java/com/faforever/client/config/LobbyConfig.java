package com.faforever.client.config;

import com.faforever.commons.lobby.FafLobbyClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LobbyConfig {

  @Bean
  public FafLobbyClient lobbyClient(ObjectMapper objectMapper) {
    return new FafLobbyClient(objectMapper);
  }

}
