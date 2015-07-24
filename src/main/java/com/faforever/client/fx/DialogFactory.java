package com.faforever.client.fx;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public interface DialogFactory {

  Alert createAlert(Alert.AlertType alertType, String text, ButtonType... buttons);
}
