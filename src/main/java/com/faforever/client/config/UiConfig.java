package com.faforever.client.config;

import com.faforever.client.chat.ChatController;
import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.fxml.FxmlLoaderImpl;
import com.faforever.client.irc.IrcService;
import com.faforever.client.irc.PircBotXIrcService;
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
    return mainController().getWhatsNewController();
  }

  @Bean
  ChatController chatController() {
    return mainController().getChatController();
  }

  @Bean
  IrcService ircClient() {
    return new PircBotXIrcService();
  }

  @Bean
  FxmlLoader fxmlLoader() {
    return new FxmlLoaderImpl(baseConfig.messageSource(), baseConfig.locale());
  }

  private <T> T loadController(String fxml) {
    return fxmlLoader().loadAndGetController(fxml);
  }
}
