package com.faforever.client.login;

import com.faforever.client.i18n.I18n;
import com.faforever.client.main.MainController;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Callback;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class LoginController {

  @FXML
  private CheckBox rememberLoginCheckBox;

  @FXML
  private CheckBox autoLoginCheckBox;

  @FXML
  private TextField usernameInput;

  @FXML
  private TextField passwordInput;

  @FXML
  private Button loginButton;

  @FXML
  private Button quitButton;

  @FXML
  private Parent loginRoot;

  @Autowired
  MainController mainController;

  @Autowired
  private I18n i18n;

  @Autowired
  private Environment environment;

  @Autowired
  private UserService userService;

  private Stage stage;

  public void display(Stage stage) {
    this.stage = stage;

    Scene scene = new Scene(loginRoot);
    scene.getStylesheets().add(environment.getProperty("style"));

    stage.setScene(scene);
    stage.setTitle(i18n.get("login.title"));
    stage.setResizable(false);
    stage.show();
  }

  @FXML
  private void login(ActionEvent actionEvent) {
    String username = usernameInput.getCharacters().toString();
    String password = passwordInput.getCharacters().toString();
    boolean autoLogin = autoLoginCheckBox.isSelected();

    userService.login(username, password, autoLogin, new Callback<Void>() {
      @Override
      public void success(Void result) {
        mainController.display(stage);
      }

      @Override
      public void error(Throwable e) {
        Dialog<Void> loginFailedDialog = new Dialog<>();
        loginFailedDialog.setTitle(i18n.get("login.failed.title"));
        loginFailedDialog.setContentText(i18n.get("login.failed.message"));
        loginFailedDialog.show();
      }
    });
  }

  @FXML
  private void cancel(ActionEvent actionEvent) {
    stage.close();
  }
}
