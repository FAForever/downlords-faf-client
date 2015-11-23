package com.faforever.client.map;


import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.map.Comment;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import javax.annotation.Resource;
import java.time.format.DateTimeFormatter;

public class CommentCardController {

  @FXML
  Label authorLabel;
  @FXML
  Label dateLabel;
  @FXML
  Label commentLabel;
  @FXML
  GridPane root;

  @Resource
  I18n i18n;

  public GridPane getRoot() {
    return root;
  }

  public void addComment(Comment comment) {
    authorLabel.setText(comment.getAuthor());
    String formattedDate = DateTimeFormatter.ofPattern(i18n.get("datePattern")).format(comment.getDate());
    dateLabel.setText(formattedDate);
    commentLabel.setText(comment.getText());
  }
}
