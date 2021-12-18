package com.faforever.client;

import ch.micheljung.fxwindow.FxStage;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.exception.GlobalExceptionHandler;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.logging.LoggingService;
import com.faforever.client.main.MainController;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.ui.taskbar.WindowsTaskbarProgressUpdater;
import com.faforever.client.util.WindowsUtil;
import de.codecentric.centerdevice.javafxsvg.SvgImageLoaderFactory;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

@Configuration
@ComponentScan
@EnableConfigurationProperties({ClientProperties.class})
@Slf4j
public class FafClientApplication extends Application {
  public static final String PROFILE_PROD = "prod";
  public static final String PROFILE_TEST = "test";
  /**
   * Does always reload root tabs in the MainController. This is useful if you do hot swap and you want to see your
   * changes.
   */
  public static final String PROFILE_RELOAD = "reload";
  public static final String PROFILE_LOCAL = "local";
  public static final String PROFILE_WINDOWS = "windows";
  public static final String PROFILE_LINUX = "linux";
  public static final String PROFILE_MAC = "mac";
  public static final int EXIT_STATUS_RAN_AS_ADMIN = 3;

  private ConfigurableApplicationContext applicationContext;

  public static void applicationMain(String[] args) {
    LoggingService.configureLogging();
    launch(args);
  }

  private static String[] getAdditionalProfiles() {
    List<String> additionalProfiles = new ArrayList<>();

    if (org.bridj.Platform.isWindows()) {
      additionalProfiles.add(PROFILE_WINDOWS);
    } else if (org.bridj.Platform.isLinux()) {
      additionalProfiles.add(PROFILE_LINUX);
    } else if (org.bridj.Platform.isMacOSX()) {
      additionalProfiles.add(PROFILE_MAC);
    }
    return additionalProfiles.toArray(new String[0]);
  }

  @Override
  public void init() throws InterruptedException, NoSuchFieldException, IllegalAccessException {
    if (org.bridj.Platform.isWindows() && WindowsUtil.isAdmin()) {
      CountDownLatch waitForUserInput = new CountDownLatch(1);
      JavaFxUtil.runLater(() -> {
        Alert alert = new Alert(AlertType.WARNING, "Please don't run the client as admin. Because if you do you might need to delete C:\\ProgramData\\FAForever to be able to run it as a normal user again. Do you want to ignore the warning and continue?", ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> buttonType = alert.showAndWait();
        if (buttonType.filter(button -> button == ButtonType.NO).isPresent()) {
          System.exit(EXIT_STATUS_RAN_AS_ADMIN);
        }
        waitForUserInput.countDown();
      });
      waitForUserInput.await();
    }

    SvgImageLoaderFactory.install();
    Font.loadFont(FafClientApplication.class.getResourceAsStream("/font/dfc-icons.ttf"), 10);
    JavaFxUtil.fixTooltipDuration();

    applicationContext = new SpringApplicationBuilder(FafClientApplication.class)
        .profiles(getAdditionalProfiles())
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

  @Bean
  public PlatformService platformService() {
    return new PlatformService(getHostServices());
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
          asList(
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
          .filter(e -> !e.getKey().isDaemon())
          .forEach(e -> {
            log.error("Non daemon Thread \"{}\" (id: {}) still active in state: {}", e.getKey().getName(), e.getKey().getId(), e.getKey().getState());
            log.error("Stacktrace of thread {}:\n{}", e.getKey().getName(), Arrays.stream(e.getValue()).map(Object::toString).collect(Collectors.joining("\n")));
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
