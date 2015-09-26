package com.faforever.client.mod;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.commons.lang3.StringUtils;

public class ModTileController {

  @FXML
  Label commentsLabel;
  @FXML
  ImageView modImageView;
  @FXML
  Label nameLabel;
  @FXML
  Label authorLabel;
  @FXML
  Label likesLabel;
  @FXML
  Node modTileRoot;

  public void setMod(ModInfoBean mod) {
    if (StringUtils.isNotEmpty(mod.getThumbnailUrl())) {
      modImageView.setImage(new Image(mod.getThumbnailUrl()));
    }
    nameLabel.setText(mod.getName());
    authorLabel.setText(mod.getAuthor());
    likesLabel.setText(String.format("%d", mod.getLikes()));
    commentsLabel.setText(String.format("%d", mod.getComments().size()));
  }

  public Node getRoot() {
    return modTileRoot;
  }
}
