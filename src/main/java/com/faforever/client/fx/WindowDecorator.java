package com.faforever.client.fx;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.EnumSet;

public class WindowDecorator {

  private enum ResizeDirection {
    NORTH,
    EAST,
    SOUTH,
    WEST,
  }

  public static final int RESIZE_BORDER_WIDTH = 5;

  @FXML
  Pane resizePane;

  @FXML
  AnchorPane contentPane;

  @FXML
  Menu menuButton;

  @FXML
  Button minimizeButton;

  @FXML
  Button maximizeButton;

  @FXML
  Button restoreButton;

  @FXML
  Button closeButton;

  @FXML
  AnchorPane windowRoot;

  private Stage stage;
  private boolean resizable;
  private Point2D dragOffset;
  private EnumSet<ResizeDirection> resizeDirections;


  @FXML
  void onMinimizeButtonClicked(ActionEvent actionEvent) {
    stage.setIconified(true);
  }

  @FXML
  void onCloseButtonClicked(ActionEvent actionEvent) {
    stage.close();
  }

  @FXML
  void onRestoreButtonClicked(ActionEvent actionEvent) {
    stage.setMaximized(false);
    maximizeButton.setVisible(true);
    restoreButton.setVisible(false);
  }

  @FXML
  void onMaximizeButtonClicked(ActionEvent actionEvent) {
    stage.setMaximized(true);
    stage.setX(0);
    stage.setY(0);
    stage.setWidth(getVisualBounds(stage).getWidth());
    stage.setHeight(getVisualBounds(stage).getHeight());
    maximizeButton.setVisible(false);
    restoreButton.setVisible(true);
  }

  public Rectangle2D getVisualBounds(Stage stage) {
    ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(
        stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()
    );
    return screensForRectangle.get(0).getVisualBounds();
  }

  public Parent getWindowRoot() {
    return windowRoot;
  }

  public void configure(Stage stage, Parent content, boolean resizable, SceneFactoryImpl.WindowButtonType[] buttons) {
    this.stage = stage;
    this.resizable = resizable;

    configureResizability(stage);

    contentPane.getChildren().setAll(content);
    AnchorPane.setTopAnchor(content, 0d);
    AnchorPane.setRightAnchor(content, 0d);
    AnchorPane.setBottomAnchor(content, 0d);
    AnchorPane.setLeftAnchor(content, 0d);

    windowRoot.requestLayout();
  }

  private void configureResizability(Stage stage) {
    resizeDirections = EnumSet.noneOf(ResizeDirection.class);

    maximizeButton.managedProperty().bind(maximizeButton.visibleProperty());
    restoreButton.managedProperty().bind(restoreButton.visibleProperty());

    if (!resizable) {
      maximizeButton.setVisible(false);
      restoreButton.setVisible(false);
      return;
    }

    maximizeButton.setVisible(!stage.isMaximized());
    restoreButton.setVisible(stage.isMaximized());
  }

  public void updateResizeCursor(MouseEvent event) {
    if (event.getSource() != resizePane) {
      throw new IllegalStateException("Resize event must only be fired by element with ID resizePane");
    }
    if (!resizable) {
      return;
    }

    resizeDirections.clear();

    StringBuilder cursorName = new StringBuilder(9);

    boolean isSouth = event.getY() > stage.getHeight() - RESIZE_BORDER_WIDTH;
    boolean isNorth = !isSouth && event.getY() < RESIZE_BORDER_WIDTH;
    boolean isEast = event.getX() > stage.getWidth() - RESIZE_BORDER_WIDTH;
    boolean isWest = !isEast && event.getX() < RESIZE_BORDER_WIDTH;

    if (isSouth) {
      resizeDirections.add(ResizeDirection.SOUTH);
      cursorName.append('S');
    }
    if (isNorth) {
      resizeDirections.add(ResizeDirection.NORTH);
      cursorName.append('N');
    }
    if (isEast) {
      resizeDirections.add(ResizeDirection.EAST);
      cursorName.append('E');
    }
    if (isWest) {
      resizeDirections.add(ResizeDirection.WEST);
      cursorName.append('W');
    }

    resizePane.setCursor(Cursor.cursor(cursorName.append("_RESIZE").toString()));
  }

  public void onDragStarted(MouseEvent event) {
    dragOffset = new Point2D(event.getScreenX() - stage.getX(), event.getScreenY() - stage.getY());
    event.consume();
  }

  public void onWindowDrag(MouseEvent event) {
    if (event.getTarget() == resizePane) {
      return;
    }
    if (stage.isMaximized()) {
      return;
    }
    stage.setY(event.getScreenY() - dragOffset.getY());
    stage.setX(event.getScreenX() - dragOffset.getX());

    event.consume();
  }

  public void onWindowResize(MouseEvent event) {
    if (event.getTarget() != resizePane) {
      return;
    }
    if (!resizable) {
      return;
    }

    final double oldX = stage.getX();
    final double oldY = stage.getY();
    double newHeight = stage.getHeight();
    double newWidth = stage.getWidth();

    if (resizeDirections.contains(ResizeDirection.NORTH)) {
      double newY = event.getScreenY() - dragOffset.getY();
      stage.setY(newY);
      newHeight += oldY - newY;
    } else if (resizeDirections.contains(ResizeDirection.SOUTH)) {
      newHeight = event.getScreenY() - stage.getY();
    }

    if (resizeDirections.contains(ResizeDirection.WEST)) {
      double newX = event.getScreenX() - dragOffset.getX();
      stage.setX(newX);
      newWidth += oldX - newX;
    } else if (resizeDirections.contains(ResizeDirection.EAST)) {
      newWidth = event.getScreenX() - stage.getX();
    }

    stage.setHeight(newHeight);
    stage.setWidth(newWidth);

    event.consume();
  }

  public void onStartWindowResize(MouseEvent event) {
    if (event.getTarget() != resizePane) {
      return;
    }
    dragOffset = new Point2D(event.getScreenX() - stage.getX(), event.getScreenY() - stage.getY());
    event.consume();
  }

  public void onMouseClicked(MouseEvent event) {
    if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
      stage.setMaximized(!stage.isMaximized());
    }
  }
}

