package com.faforever.client;

import com.faforever.client.config.BaseConfig;
import com.faforever.client.config.EhCacheConfig;
import com.faforever.client.config.ServiceConfig;
import com.faforever.client.config.UiConfig;
import com.faforever.client.fx.JavaFxHostService;
import com.faforever.client.login.LoginController;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.JavaFxUtil;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main extends Application {

  @Override
  public void start(Stage stage) throws Exception {
    Font.loadFont(Main.class.getResource("/font/fontawesome-webfont.ttf").toExternalForm(), 10);
    JavaFxUtil.fixTooltipDuration();

    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.getBeanFactory().registerSingleton("hostService", new JavaFxHostService(getHostServices()));
    context.register(BaseConfig.class, UiConfig.class, ServiceConfig.class, EhCacheConfig.class);
    context.refresh();

    stage.getIcons().add(new Image("/images/tray_icon.png"));
    stage.initStyle(StageStyle.TRANSPARENT);

    showLoginWindow(stage, context);
  }

  private void showLoginWindow(Stage stage, ApplicationContext context) {
    LoginController loginController = context.getBean(LoginController.class);
    loginController.display(stage);
  }

  public static void main(String[] args) {
    PreferencesService.configureLogging();
    launch(args);
  }
}
