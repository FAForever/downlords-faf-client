package com.faforever.client.mod;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;

public class ModTileController {

  @FXML
  Label nameLabel;
  @FXML
  Label authorLabel;
  @FXML
  Label likesLabel;
  @FXML
  Node modTileRoot;

  public void setMod(ModInfoBean mod) {
    nameLabel.setText(mod.getName());
    authorLabel.setText(mod.getAuthor());
    likesLabel.setText(String.format("%d", mod.getLikes()));
  }

  public Node getRoot() {
    return modTileRoot;
  }
}
