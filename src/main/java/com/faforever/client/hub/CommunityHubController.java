package com.faforever.client.hub;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;

public class CommunityHubController {

  @FXML
  Pane communityHubRoot;

  @Autowired
  ApplicationContext applicationContext;

  @PostConstruct
  void postConstruct() {
    ConcurrentUsersController concurrentUsersController = applicationContext.getBean(ConcurrentUsersController.class);
    communityHubRoot.getChildren().add(concurrentUsersController.getRoot());
  }

  public Node getRoot() {
    return communityHubRoot;
  }

  public void setUpIfNecessary() {

  }
}
