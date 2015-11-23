package com.faforever.client.hub;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

public class CommunityHubController {

  @FXML
  Node communityHubRoot;
  @FXML
  Pane flowPane;

  @Resource
  LastNewsController lastNewsController;
  @Resource
  ConcurrentUsersController concurrentUsersController;
  @Resource
  LastCastController lastCastController;
  @Resource
  UpcomingEventsController upcomingEventsController;
  @Resource
  MapOfTheDayController mapOfTheDayController;
  @Resource
  TopPlayersController topPlayersController;
  @Resource
  DonationWallController donationWallController;
  @Resource
  RecentForumPostsController recentForumPostsController;
  @Resource
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
