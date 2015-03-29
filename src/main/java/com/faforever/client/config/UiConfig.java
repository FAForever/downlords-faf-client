package com.faforever.client.config;

import com.faforever.client.chat.ChannelTabFactory;
import com.faforever.client.chat.ChatController;
import com.faforever.client.fx.SceneFactory;
import com.faforever.client.fx.SceneFactoryImpl;
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
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

@org.springframework.context.annotation.Configuration
@Import(BaseConfig.class)
public class UiConfig {

  @Autowired
  Environment environment;

  @Autowired
  BaseConfig baseConfig;

  @Bean
  SceneFactory sceneFactory() {
    return new SceneFactoryImpl();
  }

  @Bean
  LoginController loginController() {
    return loadController("login.fxml");
  }

  @Bean
  MainController mainController() {
    return loadController("main.fxml");
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
  ChannelTabFactory chatTabFactory() {
    return new ChannelTabFactoryImpl();
  }

  @Bean
  DocumentBuilder documentBuilder() throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    return documentBuilderFactory.newDocumentBuilder();
  }

  @Bean
  FxmlLoader fxmlLoader() {
    FxmlLoaderImpl fxmlLoader = new FxmlLoaderImpl(baseConfig.messageSource(), baseConfig.locale());
    fxmlLoader.setTheme(baseConfig.preferencesService().getPreferences().getTheme());
    return fxmlLoader;
  }

  private <T> T loadController(String fxml) {
    return fxmlLoader().loadAndGetController(fxml);
  }
}
