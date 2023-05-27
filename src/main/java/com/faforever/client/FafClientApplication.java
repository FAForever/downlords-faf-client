package com.faforever.client;

import ch.micheljung.fxwindow.FxStage;
import com.faforever.client.exception.GlobalExceptionHandler;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.MainController;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.svg.SvgImageLoaderFactory;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.ui.taskbar.WindowsTaskbarProgressUpdater;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

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
  public static final String PROFILE_RELOAD = "reload";
  public static final String PROFILE_WINDOWS = "windows";
  public static final String PROFILE_LINUX = "linux";
  public static final String PROFILE_MAC = "mac";

  private ConfigurableApplicationContext applicationContext;

  public static void applicationMain(String[] args) {
    launch(args);
  }

  @Override
  public void init() throws InterruptedException, NoSuchFieldException, IllegalAccessException {
    SvgImageLoaderFactory.install();
    Font.loadFont(FafClientApplication.class.getResourceAsStream("/font/dfc-icons.ttf"), 10);

    applicationContext = new SpringApplicationBuilder(Main.class)
        .bannerMode(Mode.OFF)
        .run(getParameters().getRaw().toArray(new String[0]));

    Thread.setDefaultUncaughtExceptionHandler(applicationContext.getBean(GlobalExceptionHandler.class));
  }

  @Override
  public void start(Stage stage) {
    StageHolder.setStage(stage);
    FxStage fxStage = FxStage.configure(stage)
        .withSceneFactory(parent -> applicationContext.getBean(UiService.class).createScene(parent))
        .apply();

    fxStage.getStage().setOnCloseRequest(this::closeMainWindow);

    showMainWindow(fxStage);

    // TODO publish event instead
    if (!applicationContext.getBeansOfType(WindowsTaskbarProgressUpdater.class).isEmpty()) {
      applicationContext.getBean(WindowsTaskbarProgressUpdater.class).initTaskBar();
    }
  }

  private void showMainWindow(FxStage fxStage) {
    UiService uiService = applicationContext.getBean(UiService.class);

    MainController controller = uiService.loadFxml("theme/main.fxml");
    controller.setFxStage(fxStage);
    controller.display();
  }

  private void closeMainWindow(WindowEvent event) {
    if (applicationContext.getBean(GameService.class).isGameRunning()) {
      I18n i18n = applicationContext.getBean(I18n.class);
      NotificationService notificationService = applicationContext.getBean(NotificationService.class);
      notificationService.addNotification(new ImmediateNotification(i18n.get("exitWarning.title"),
          i18n.get("exitWarning.message"),
          Severity.WARN,
          List.of(
              new Action(i18n.get("yes"), ev -> {
                Platform.exit();
              }),
              new Action(i18n.get("no"), ev -> {
              })
          )));
      event.consume();
    } else {
      Platform.exit();
    }
  }

  @Override
  public void stop() throws Exception {
    applicationContext.getBean(GlobalExceptionHandler.class).setShuttingDown(true);
    applicationContext.close();
    super.stop();

    Thread timeoutThread = new Thread(() -> {
      try {
        Thread.sleep(Duration.ofSeconds(10).toMillis());
      } catch (InterruptedException ignored) {
      }

      Set<Entry<Thread, StackTraceElement[]>> threads = Thread.getAllStackTraces().entrySet();

      if (threads.stream().allMatch(t -> t.getKey().isDaemon())) {
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
      } catch (InterruptedException ignored) {
      }

      System.exit(-1);
    });
    timeoutThread.setDaemon(true);
    timeoutThread.start();
  }
}
