package com.faforever.client.leaderboard;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.TableCell;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.annotation.Autowired;


public class LeaderboardController {

  @FXML
  Node leaderboardRoot;

  @FXML
  Pane contentPane;

  @FXML
  ButtonBase byLadderRatingButton;

  @FXML
  ButtonBase byMapButton;

  @FXML
  ButtonBase byRankButton;

  @Autowired
  LadderRatingController ladderRatingController;

  @Autowired
  MapController mapController;

  @Autowired
  RankController rankController;


  @FXML
  void onLeaderboardButton(ActionEvent event) {
    ToggleButton button = (ToggleButton) event.getSource();

    if (!button.isSelected()) {
      button.setSelected(true);
    }

    if (button == byLadderRatingButton) {
      setContent(byLadderRatingController.getRoot());
    } else if (button == byMapButton) {
      setContent(byMapController.getRoot());
    } else if (button == byRankButton) {
      setContent(byRankController.getRoot());
    }
  }



  @Autowired
  LeaderboardService leaderboardService;

  public Node getRoot() {
    return leaderboardRoot;
  }

  public void onOneOneLadderButton(ActionEvent actionEvent) {
    leaderboardService.getLadderInfo();
    newsController.setUp();
    chatController.setUp();
    gamesController.setUp();
  }

  private void setContent(Node node) {
    ObservableList<Node> children = contentPane.getChildren();

    if (!children.contains(node)) {
      children.add(node);

      AnchorPane.setTopAnchor(node, 0d);
      AnchorPane.setRightAnchor(node, 0d);
      AnchorPane.setBottomAnchor(node, 0d);
      AnchorPane.setLeftAnchor(node, 0d);
    }

    for (Node child : children) {
      child.setVisible(child == node);
    }
  }
}
