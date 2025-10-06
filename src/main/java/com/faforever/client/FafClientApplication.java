package com.faforever.client;

import ch.micheljung.fxwindow.FxStage;
import com.faforever.client.exception.GlobalExceptionHandler;
import com.faforever.client.game.GameRunner;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.MainController;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.svg.SvgImageLoaderFactory;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class FafClientApplication extends Application {
  /**
   * Does always reload root tabs in the MainController. This is useful if you do hot swap and you want to see your
   * changes.
   */
  public static final String PROFILE_WINDOWS = "windows";
  public static final String PROFILE_LINUX = "linux";
  public static final String PROFILE_MAC = "mac";

  private ConfigurableApplicationContext applicationContext;

  public static void applicationMain(String[] args) {
    launch(args);
  }

  @Override
  public void init() throws IOException {
    SvgImageLoaderFactory.install();
    try (InputStream inputStream = FafClientApplication.class.getResourceAsStream("/font/dfc-icons.ttf")) {
      Font.loadFont(inputStream, 10);
    }

    applicationContext = new SpringApplicationBuilder(Main.class)
        .run(getParameters().getRaw().toArray(new String[0]));

    Thread.setDefaultUncaughtExceptionHandler(applicationContext.getBean(GlobalExceptionHandler.class));
  }

  @Override
  public void start(Stage stage) {
    try {
      StageHolder.setStage(stage);
      FxStage fxStage = FxStage.configure(stage)
                               .withSceneFactory(
                                   parent -> applicationContext.getBean(ThemeService.class).createScene(parent))
          .apply();

      fxStage.getStage().setOnCloseRequest(this::closeMainWindow);

      showMainWindow(fxStage);
    } catch (Exception e) {
      log.error("Unable to start", e);
      throw e;
    }
  }

  private void showMainWindow(FxStage fxStage) {
    UiService uiService = applicationContext.getBean(UiService.class);

    MainController controller = uiService.loadFxml("theme/main.fxml");
    controller.setFxStage(fxStage);
    controller.display();
  }

  private void closeMainWindow(WindowEvent event) {
    if (applicationContext.getBean(GameRunner.class).isRunning()) {
      I18n i18n = applicationContext.getBean(I18n.class);
      NotificationService notificationService = applicationContext.getBean(NotificationService.class);
      notificationService.addNotification(new ImmediateNotification(i18n.get("exitWarning.title"),
          i18n.get("exitWarning.message"),
          Severity.WARN,
          List.of(new Action(i18n.get("yes"), Platform::exit), new Action(i18n.get("no"), () -> {})
          )));
      event.consume();
    } else {
      Platform.exit();
    }
  }

  @Override
  public void stop() throws Exception {
    log.info("Stopping application");
    applicationContext.close();
    super.stop();

    Thread timeoutThread = new Thread(() -> {
      log.info("Starting non-daemon detector thread");
      try {
        Thread.sleep(Duration.ofSeconds(10).toMillis());
      } catch (InterruptedException _) {
      }

      Set<Entry<Thread, StackTraceElement[]>> threads = Thread.getAllStackTraces().entrySet();

      if (threads.stream().allMatch(t -> t.getKey().isDaemon())) {
        log.info("No non-daemon threads started");
        return;
      }

      threads.stream()
          .filter(threadEntry -> !threadEntry.getKey().isDaemon())
          .forEach(threadEntry -> {
            Thread thread = threadEntry.getKey();
            log.error("Non daemon Thread \"{}\" (id: {}) still active in state: {}", thread.getName(), thread.getId(), thread.getState());
            log.error("Stacktrace of thread {}:\n{}", thread.getName(), Arrays.stream(threadEntry.getValue()).map(Object::toString).collect(Collectors.joining("\n")));
          });

      try {
        Thread.sleep(Duration.ofSeconds(1).toMillis());
      } catch (InterruptedException _) {
      }

      System.exit(-1);
    });
    timeoutThread.setDaemon(true);
    timeoutThread.start();
  }
}
