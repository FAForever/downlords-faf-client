package com.faforever.client.ui.dialog;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/** Ported from JFoenix since we wanted to get rid of the JFoenix dependency */
public class DialogLayout extends VBox {
  private static final String DEFAULT_STYLE_CLASS = "dialog-layout";

  private final StackPane heading = new StackPane();
  private final StackPane body = new StackPane();
  private final FlowPane actions = new FlowPane();

  public DialogLayout() {
    initialize();
    heading.getStyleClass().addAll("layout-heading", "title");
    body.getStyleClass().add("layout-body");
    VBox.setVgrow(body, Priority.ALWAYS);
    actions.getStyleClass().add("layout-actions");
    getChildren().setAll(heading, body, actions);
  }

  public ObservableList<Node> getHeading() {
    return heading.getChildren();
  }

  public void setHeading(Node... titleContent) {
    this.heading.getChildren().setAll(titleContent);
  }

  public ObservableList<Node> getBody() {
    return body.getChildren();
  }

  public void setBody(Node... body) {
    this.body.getChildren().setAll(body);
  }

  public ObservableList<Node> getActions() {
    return actions.getChildren();
  }

  public void setActions(Node... actions) {
    this.actions.getChildren().setAll(actions);
  }

  public void setActions(List<? extends Node> actions) {
    this.actions.getChildren().setAll(actions);
  }

  @Override
  public String getUserAgentStylesheet() {
    return getClass().getResource("/css/controls/dialog-layout.css").toExternalForm();
  }

  private void initialize() {
    this.getStyleClass().add(DEFAULT_STYLE_CLASS);
  }
}
