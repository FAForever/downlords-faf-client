package com.faforever.client;

import com.faforever.client.config.BaseConfig;
import com.faforever.client.config.UiConfig;
import com.faforever.client.login.LoginController;
import com.faforever.client.main.MainController;
import com.faforever.client.util.JavaFxUtil;
import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main extends Application {

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) throws Exception {
    JavaFxUtil.fixTooltipDuration();
    configureLogging();

    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.getBeanFactory().registerSingleton("hostServices", getHostServices());
    context.register(BaseConfig.class, UiConfig.class);
    context.refresh();

    stage.getIcons().add(new Image("/images/tray_icon.png"));
    stage.initStyle(StageStyle.TRANSPARENT);

    showLoginWindow(stage, context);
  }

  private void showMainWindow(Stage stage, ApplicationContext context) {
    MainController mainController = context.getBean(MainController.class);
    mainController.display(stage);
  }

  private void showLoginWindow(Stage stage, ApplicationContext context) {
    LoginController loginController = context.getBean(LoginController.class);
    loginController.display(stage);
  }

  private void configureLogging() {
    System.setProperty("logDirectory", Shell32Util.getFolderPath(ShlObj.CSIDL_COMMON_APPDATA) + "\\FAForever\\logs");
  }
}
