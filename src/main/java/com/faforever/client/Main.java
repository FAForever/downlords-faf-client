package com.faforever.client;

import com.faforever.client.config.BaseConfig;
import com.faforever.client.config.CacheConfig;
import com.faforever.client.config.LuceneConfig;
import com.faforever.client.config.ServiceConfig;
import com.faforever.client.config.TaskConfig;
import com.faforever.client.config.UiConfig;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformServiceImpl;
import com.faforever.client.main.MainController;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.ThemeService;
import com.google.common.annotations.VisibleForTesting;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main extends Application {

  @VisibleForTesting
  AnnotationConfigApplicationContext context;

  public Main() {
    context = new AnnotationConfigApplicationContext();
  }

  public static void main(String[] args) {
    PreferencesService.configureLogging();
    launch(args);
  }

  @Override
  public void start(Stage stage) {
    Font.loadFont(getClass().getResourceAsStream("/font/fontawesome-webfont.ttf"), 0);
    JavaFxUtil.fixTooltipDuration();

    initApplicationContext(stage);
    initStage(stage, context.getBean(ThemeService.class));
    initMainWindow(stage);
  }

  private void initApplicationContext(Stage stage) {
    context.getBeanFactory().registerSingleton("hostService", new PlatformServiceImpl(getHostServices()));
    context.getBeanFactory().registerSingleton("stage", stage);
    context.register(BaseConfig.class, UiConfig.class, ServiceConfig.class, TaskConfig.class, CacheConfig.class, LuceneConfig.class);
    context.registerShutdownHook();
    context.refresh();
  }

  private void initStage(Stage stage, ThemeService themeService) {
    stage.getIcons().add(new Image(themeService.getThemeFile(ThemeService.TRAY_ICON)));
    stage.initStyle(StageStyle.TRANSPARENT);
  }

  private void initMainWindow(Stage stage) {
    MainController mainController = context.getBean(MainController.class);
    mainController.display();
  }

  @Override
  public void stop() throws Exception {
    context.close();
    super.stop();
  }
}
