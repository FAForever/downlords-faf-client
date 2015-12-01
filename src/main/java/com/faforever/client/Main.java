package com.faforever.client;

import com.faforever.client.config.BaseConfig;
import com.faforever.client.config.CacheConfig;
import com.faforever.client.config.LuceneConfig;
import com.faforever.client.config.ServiceConfig;
import com.faforever.client.config.TaskConfig;
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

  private AnnotationConfigApplicationContext context;

  @Override
  public void start(Stage stage) throws Exception {
    Font.loadFont(getClass().getResourceAsStream("/font/fontawesome-webfont.ttf"), 0);
    JavaFxUtil.fixTooltipDuration();

    context = new AnnotationConfigApplicationContext();
    context.getBeanFactory().registerSingleton("hostService", new JavaFxHostService(getHostServices()));
    context.getBeanFactory().registerSingleton("stage", stage);
    context.register(BaseConfig.class, UiConfig.class, ServiceConfig.class, TaskConfig.class, CacheConfig.class, LuceneConfig.class);
    context.registerShutdownHook();
    context.refresh();

    stage.getIcons().add(new Image("/images/tray_icon.png"));
    stage.initStyle(StageStyle.TRANSPARENT);

    showLoginWindow(context);
  }

  @Override
  public void stop() throws Exception {
    context.close();
    super.stop();
  }

  private void showLoginWindow(ApplicationContext context) {
    LoginController loginController = context.getBean(LoginController.class);
    loginController.display();
  }

  public static void main(String[] args) {
    PreferencesService.configureLogging();
    launch(args);
  }
}
