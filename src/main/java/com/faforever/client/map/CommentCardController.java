package com.faforever.client.map;


import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.map.Comment;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class CommentCardController {

  @FXML
  Label authorLabel;
  @FXML
  Label dateLabel;
  @FXML
  Label commentLabel;
  @FXML
  GridPane root;

  @Autowired
  I18n i18n;

  @Autowired
  Locale locale;

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
