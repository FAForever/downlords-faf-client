package com.faforever.client;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformServiceImpl;
import com.faforever.client.main.MainController;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@ComponentScan("com.faforever.client")
public class Main extends Application {

  private static final Logger logger;
  static {
    PreferencesService.configureLogging();
    logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  }

  @VisibleForTesting
  AnnotationConfigApplicationContext context;

  public Main() {
    context = new AnnotationConfigApplicationContext();
  }

  public static void main(String[] args) {
    PreferencesService.configureLogging();
    configureProfiles();

    launch(args);
  }

  private static void configureProfiles() {
    System.setProperty("spring.profiles.default", "production");
    List<String> profiles = new ArrayList<>();

    String activeProfiles = System.getProperty("spring.profiles.active");
    if (activeProfiles != null) {
      profiles.addAll(Arrays.asList(activeProfiles.split(",")));
    }
    if (org.bridj.Platform.isWindows()) {
      profiles.add("windows");
      if (org.bridj.Platform.isWindows7()) {
        profiles.add("windows7");
      }
    } else if (org.bridj.Platform.isLinux()) {
      profiles.add("linux");
    } else if (org.bridj.Platform.isMacOSX()) {
      profiles.add("mac");
    }
    System.setProperty("spring.profiles.active", Joiner.on(',').join(profiles));
  }

  @Override
  public void start(Stage stage) {
    JavaFxUtil.fixTooltipDuration();

    Font.loadFont(Main.class.getResourceAsStream("/font/dfc-icons.ttf"), 10);

    initApplicationContext(stage);
    initStage(stage, context.getBean(UiService.class));
    initMainWindow();
  }

  private void initApplicationContext(Stage stage) {
    Stopwatch stopwatch = Stopwatch.createStarted();

    context.getBeanFactory().registerSingleton("hostService", new PlatformServiceImpl(getHostServices()));
    context.getBeanFactory().registerSingleton("stage", stage);
    context.register(Main.class);
    context.registerShutdownHook();
    context.refresh();

    logger.debug("Loading application context took {}", stopwatch.stop());
  }

  private void initStage(Stage stage, UiService uiService) {
    stage.initStyle(StageStyle.UNDECORATED);
  }

  private void initMainWindow() {
    MainController controller = context.getBean(UiService.class).loadFxml("theme/main.fxml");
    controller.display();
  }

  @Override
  public void stop() throws Exception {
    context.close();
    super.stop();
  }
}
