package com.faforever.client.notification;

import com.faforever.client.util.Callback;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;

public class DirectoryChooserAction extends Action {

  private final String directoryChooserTitle;
  private final Callback<Path> callback;

  public DirectoryChooserAction(String notificationTitle, String directoryChooserTitle, Callback<Path> callback) {
    super(notificationTitle);
    this.directoryChooserTitle = directoryChooserTitle;
    this.callback = callback;
  }

  @Override
  public void call(ActionEvent event) {
    Window owner = ((Node) event.getSource()).getScene().getWindow();

    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle(directoryChooserTitle);
    File result = directoryChooser.showDialog(owner);

    if (result == null) {
      callback.success(null);
    } else {
      callback.success(result.toPath());
    }
  }
}
