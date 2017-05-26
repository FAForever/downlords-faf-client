package com.faforever.client.config;

import org.ice4j.stack.StunStack;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StunConfig {

  @Bean
  public StunStack stunStack() {
    return new StunStack();
  }
}
