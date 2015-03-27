package com.faforever.client.config;

import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.fxml.FxmlLoaderImpl;
import com.faforever.client.irc.IrcClient;
import com.faforever.client.irc.PircBotXIrcClient;
import com.faforever.client.login.LoginController;
import com.faforever.client.main.MainController;
import com.faforever.client.whatsnew.WhatsNewController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

@org.springframework.context.annotation.Configuration
@Import(BaseConfig.class)
public class UiConfig {

  @Autowired
  Environment environment;

  @Autowired
  BaseConfig baseConfig;

  @Bean
  LoginController loginController() {
    return loadController("/fxml/login.fxml");
  }

  @Bean
  MainController mainController() {
    return loadController("/fxml/main.fxml");
  }

  @Bean
  WhatsNewController whatsNewController() {
    return loadController("/fxml/whats_new.fxml");
  }

  @Bean
  IrcClient ircClient() {
    return new PircBotXIrcClient();
  }

  @Bean
  FxmlLoader fxmlLoader() {
    return new FxmlLoaderImpl(baseConfig.messageSource(), baseConfig.locale());
  }

  private <T> T loadController(String fxml) {
    return fxmlLoader().load(fxml);
  }
}
