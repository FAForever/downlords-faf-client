package com.faforever.client.game;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;

public class EnterPasswordController {

  public interface OnPasswordEnteredListener {

    void onPasswordEntered(GameInfoBean gameInfoBean, String password, double screenX, double screenY);
  }

  private OnPasswordEnteredListener listener;

  @FXML
  Node enterPasswordRoot;

  @FXML
  TextField passwordTextField;

  private GameInfoBean gameInfoBean;

  public Node getRoot() {
    return enterPasswordRoot;
  }

  public void setOnPasswordEnteredListener(OnPasswordEnteredListener listener) {
    if (this.listener != null) {
      throw new IllegalStateException("Listener has already been set");
    }
    this.listener = listener;
  }

  @FXML
  void onPasswordEntered() {
    if (listener == null) {
      throw new IllegalStateException("No listener has been set");
    }
    listener.onPasswordEntered(gameInfoBean, passwordTextField.getText(), 0 ,0);
  }

  public void setGameInfoBean(GameInfoBean gameInfoBean) {
    this.gameInfoBean = gameInfoBean;
  }
}
