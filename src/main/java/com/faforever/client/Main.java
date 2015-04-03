package com.faforever.client;

import com.faforever.client.config.BaseConfig;
import com.faforever.client.config.UiConfig;
import com.faforever.client.login.LoginController;
import com.faforever.client.main.MainController;
import com.faforever.client.user.UserService;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main extends Application {

  public static final Integer VERSION = 123;

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(BaseConfig.class, UiConfig.class);

    stage.getIcons().add(new Image("/images/tray_icon.png"));
    stage.initStyle(StageStyle.TRANSPARENT);

    UserService userService = context.getBean(UserService.class);

    if (!userService.isLoggedIn()) {
      showLoginWindow(stage, context);
    } else {
      context.register(UiConfig.class);
      context.refresh();
      showMainWindow(stage, context);
    }
  }

  private void showMainWindow(Stage stage, ApplicationContext context) {
    MainController mainController = context.getBean(MainController.class);
    mainController.display(stage);
  }

  private void showLoginWindow(Stage stage, ApplicationContext context) {
    LoginController loginController = context.getBean(LoginController.class);
    loginController.display(stage);
  }
}
