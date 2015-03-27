package com.faforever.client.login;

import com.faforever.client.i18n.I18n;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

public class LoginDialog extends Dialog {

  private final ButtonType cancelButton;
  private final ButtonType okButton;

  public LoginDialog(Window window, I18n i18n) {
    setTitle(i18n.get("login.title"));
    setHeaderText(i18n.get("login.header"));

    cancelButton = new ButtonType(i18n.get("login.button.quit"), ButtonBar.ButtonData.CANCEL_CLOSE);
    okButton = new ButtonType(i18n.get("login.button.login"), ButtonBar.ButtonData.OK_DONE);

    getDialogPane().getButtonTypes().addAll(
        okButton,
        this.cancelButton
    );

    ((Stage) getDialogPane().getScene().getWindow()).getIcons().add(new Image("/images/tray_icon.png"));
    initOwner(window);
  }

  public ButtonType getCancelButton() {
    return cancelButton;
  }

  public ButtonType getOkButton() {
    return okButton;
  }
}
