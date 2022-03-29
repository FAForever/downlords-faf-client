package com.faforever.client.player;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@RequiredArgsConstructor
public class UserLeaderboardInfoController implements Controller<Node> {
  public Label leaderboardNameLabel;
  public Label gamesPlayedLabel;
  public ImageView divisionImage;
  public Label ratingLabel;
  public VBox root;

  @Override
  public VBox getRoot() {
    return root;
  }

  public void setLeaderboardInfo(String leaderboardName, String gameNumber, String ratingNumber, Image image) {
    JavaFxUtil.runLater(() -> {
      leaderboardNameLabel.setText(leaderboardName);
      gamesPlayedLabel.setText(gameNumber);
      ratingLabel.setText(ratingNumber);
      if (image != null) {
        divisionImage.setImage(image);
      }
    });
  }
}
