package com.faforever.client.map;


import com.faforever.client.legacy.map.Comment;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class CommentCardController {

  public Label authorLabel;
  public Label dateLabel;
  public Label commentLabel;
  public GridPane root;

  public GridPane getRoot() {
    return root;
  }

  public void createComment(Comment comment) {
    authorLabel.setText(comment.getAuthor());
    dateLabel.setText(comment.getDate().format(DateTimeFormatter.ofPattern("MMMM dd yyyy", Locale.US)));
    commentLabel.setText(comment.getText());
  }
}
