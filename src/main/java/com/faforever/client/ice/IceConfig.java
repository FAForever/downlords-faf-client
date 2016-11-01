package com.faforever.client.ice;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

@Lazy
public class IceConfig {

  @Bean
  public IceAdapter iceAdapter() {
    return new IceAdapterImpl();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  IceAdapterCallbacks iceAdapterCallbacks() {
    return new IceAdapterCallbacks();
  }
}
