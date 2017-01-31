package com.faforever.client;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.PlatformServiceImpl;
import com.faforever.client.main.MainController;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@EnableConfigurationProperties({ClientProperties.class})
public class FafClientApplication extends Application {

  private static String[] args;

  private ConfigurableApplicationContext applicationContext;

  public static void main(String[] args) {
    FafClientApplication.args = args;
    launch(args);
  }

  private static String[] getAdditionalProfiles() {
    List<String> additionalProfiles = new ArrayList<>();

    if (org.bridj.Platform.isWindows()) {
      additionalProfiles.add("windows");
      if (org.bridj.Platform.isWindows7()) {
        additionalProfiles.add("windows7");
      }
    } else if (org.bridj.Platform.isLinux()) {
      additionalProfiles.add("linux");
    } else if (org.bridj.Platform.isMacOSX()) {
      additionalProfiles.add("mac");
    }
    return additionalProfiles.toArray(new String[additionalProfiles.size()]);
  }

  @Override
  public void init() throws Exception {
    Font.loadFont(FafClientApplication.class.getResourceAsStream("/font/dfc-icons.ttf"), 10);
    JavaFxUtil.fixTooltipDuration();

    applicationContext = new SpringApplicationBuilder(FafClientApplication.class)
        .profiles(getAdditionalProfiles())
        .bannerMode(Mode.OFF)
        .run(args);
  }

  @Override
  public void start(Stage stage) {
    StageHolder.setStage(stage);
    initStage(stage, applicationContext.getBean(UiService.class));
    showMainWindow();
  }

  @Bean
  public PlatformService platformService() {
    return new PlatformServiceImpl(getHostServices());
  }

  private void initStage(Stage stage, UiService uiService) {
    stage.initStyle(StageStyle.UNDECORATED);
  }

  private void showMainWindow() {
    MainController controller = applicationContext.getBean(UiService.class).loadFxml("theme/main.fxml");
    controller.display();
  }

  @Override
  public void stop() throws Exception {
    applicationContext.close();
    super.stop();
  }
}
