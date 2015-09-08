package com.faforever.client.hub;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

public class CommunityHubController {

  @FXML
  Node communityHubRoot;

  @FXML
  Pane flowPane;

  @Autowired
  LastNewsController lastNewsController;
  @Autowired
  ConcurrentUsersController concurrentUsersController;
  @Autowired
  LastCastController lastCastController;
  @Autowired
  UpcomingEventsController upcomingEventsController;
  @Autowired
  MapOfTheDayController mapOfTheDayController;
  @Autowired
  TopPlayersController topPlayersController;
  @Autowired
  DonationWallController donationWallController;
  @Autowired
  RecentForumPostsController recentForumPostsController;
  @Autowired
  MostActivePlayersController mostActivePlayersController;

  @PostConstruct
  void postConstruct() {
    ObservableList<Node> children = flowPane.getChildren();

    children.addAll(
        lastNewsController.getRoot(),
        concurrentUsersController.getRoot(),
        lastCastController.getRoot(),
        upcomingEventsController.getRoot(),
        mapOfTheDayController.getRoot(),
        topPlayersController.getRoot(),
        donationWallController.getRoot(),
        recentForumPostsController.getRoot(),
        mostActivePlayersController.getRoot()
    );
  }

  public Node getRoot() {
    return communityHubRoot;
  }
}
