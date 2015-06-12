package com.faforever.client.notification;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

public class DirectoryChooserAction extends Action {

  private final String directoryChooserTitle;

  public DirectoryChooserAction(String notificationTitle, String directoryChooserTitle) {
    super(notificationTitle);
    this.directoryChooserTitle = directoryChooserTitle;
  }

  @Override
  public void call(ActionEvent event) {
    Window owner = ((Node) event.getSource()).getScene().getWindow();

    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle(directoryChooserTitle);
    directoryChooser.showDialog(owner);
  }

}
