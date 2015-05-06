package com.faforever.client.fx;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static com.faforever.client.fx.WindowDecorator.WindowButtonType.CLOSE;
import static com.faforever.client.fx.WindowDecorator.WindowButtonType.MAXIMIZE_RESTORE;
import static com.faforever.client.fx.WindowDecorator.WindowButtonType.MINIMIZE;

public class WindowDecorator {

  public enum WindowButtonType {
    MINIMIZE,
    MAXIMIZE_RESTORE,
    CLOSE;
  }

  private enum ResizeDirection {
    NORTH,
    EAST,
    SOUTH,
    WEST,
  }

  public static final double RESIZE_BORDER_WIDTH = 7d;
  public static final String PROPERTY_WINDOW_DECORATOR = "windowDecorator";

  @FXML
  AnchorPane contentPane;

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

  @FXML
  Pane windowButtons;

  private Stage stage;
  private boolean resizable;
  private Point2D dragOffset;
  private EnumSet<ResizeDirection> resizeDirections;
  private boolean isResizing;

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
    restore();
  }

  @FXML
  void onMaximizeButtonClicked(ActionEvent actionEvent) {
    maximize();
  }

  @FXML
  void initialize() {
    minimizeButton.managedProperty().bind(minimizeButton.visibleProperty());
    maximizeButton.managedProperty().bind(maximizeButton.visibleProperty());
    restoreButton.managedProperty().bind(restoreButton.visibleProperty());
    closeButton.managedProperty().bind(closeButton.visibleProperty());
  }

  public void configure(Stage stage, Parent content, boolean resizable, WindowButtonType... buttons) {
    this.stage = stage;
    this.resizable = resizable;

    stage.getProperties().put(PROPERTY_WINDOW_DECORATOR, this);

    List<WindowButtonType> buttonList = Arrays.asList(buttons);

    minimizeButton.setVisible(buttonList.contains(MINIMIZE));
    maximizeButton.setVisible(buttonList.contains(MAXIMIZE_RESTORE));
    restoreButton.setVisible(buttonList.contains(MAXIMIZE_RESTORE));
    closeButton.setVisible(buttonList.contains(CLOSE));

    configureResizability(stage);

    contentPane.getChildren().setAll(content);
    AnchorPane.setTopAnchor(content, 0d);
    AnchorPane.setRightAnchor(content, 0d);
    AnchorPane.setBottomAnchor(content, 0d);
    AnchorPane.setLeftAnchor(content, 0d);

    windowRoot.requestLayout();
  }

  private void restore() {
    maximizeButton.setVisible(true);
    restoreButton.setVisible(false);

    stage.setMaximized(false);
    AnchorPane.setTopAnchor(contentPane, RESIZE_BORDER_WIDTH);
    AnchorPane.setRightAnchor(contentPane, RESIZE_BORDER_WIDTH);
    AnchorPane.setBottomAnchor(contentPane, RESIZE_BORDER_WIDTH);
    AnchorPane.setLeftAnchor(contentPane, RESIZE_BORDER_WIDTH);

    AnchorPane.setRightAnchor(windowButtons, RESIZE_BORDER_WIDTH);
  }

  public void maximize() {
    Rectangle2D visualBounds = getVisualBounds(stage);

    stage.setWidth(visualBounds.getWidth());
    stage.setHeight(visualBounds.getHeight());
    stage.setX(visualBounds.getMinX());
    stage.setY(visualBounds.getMinY());

    maximizeButton.setVisible(false);
    restoreButton.setVisible(true);

    stage.setMaximized(true);

    AnchorPane.setTopAnchor(contentPane, 0d);
    AnchorPane.setRightAnchor(contentPane, 0d);
    AnchorPane.setBottomAnchor(contentPane, 0d);
    AnchorPane.setLeftAnchor(contentPane, 0d);

    AnchorPane.setRightAnchor(windowButtons, 0d);
  }

  public Parent getWindowRoot() {
    return windowRoot;
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

    if (stage.isMaximized()) {
      maximize();
    } else {
      restore();
    }
  }

  public void onMouseMoved(MouseEvent event) {
    if (!resizable) {
      return;
    }
    if (stage.isMaximized()) {
      windowRoot.setCursor(Cursor.DEFAULT);
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
    if (cursorName.length() == 0) {
      windowRoot.setCursor(Cursor.DEFAULT);
      return;
    }

    windowRoot.setCursor(Cursor.cursor(cursorName.append("_RESIZE").toString()));
  }

  public void onMousePressed(MouseEvent event) {
    if (isOnResizeBorder(event)) {
      isResizing = true;
    }

    dragOffset = new Point2D(event.getScreenX() - stage.getX(), event.getScreenY() - stage.getY());
    event.consume();
  }

  private boolean isOnResizeBorder(MouseEvent event) {
    return event.getY() > stage.getHeight() - RESIZE_BORDER_WIDTH
        || event.getY() < RESIZE_BORDER_WIDTH
        || event.getX() > stage.getWidth() - RESIZE_BORDER_WIDTH
        || event.getX() < RESIZE_BORDER_WIDTH;
  }

  public void onMouseDragged(MouseEvent event) {
    if (isResizing) {
      onWindowResize(event);
    } else {
      onWindowMove(event);
    }
  }

  private void onWindowMove(MouseEvent event) {
    if (event.getTarget() == windowRoot) {
      return;
    }
    if (stage.isMaximized()) {
      return;
    }
    double newY = event.getScreenY() - dragOffset.getY();
    double newX = event.getScreenX() - dragOffset.getX();

    stage.setY(newY);
    stage.setX(newX);

    event.consume();
  }

  private void onWindowResize(MouseEvent event) {
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

  public void onMouseClicked(MouseEvent event) {
    if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2 && resizable) {
      if (stage.isMaximized()) {
        restore();
      } else {
        maximize();
      }
    }
  }

  public void onMouseReleased(Event event) {
    isResizing = false;
  }

  public static Rectangle2D getVisualBounds(Stage stage) {
    double x1 = stage.getX() + (stage.getWidth() / 2);
    double y1 = stage.getY() + (stage.getHeight() / 2);

    Rectangle2D windowCenter = new Rectangle2D(x1, y1, x1, y1);
    ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(windowCenter);
    return screensForRectangle.get(0).getVisualBounds();
  }

  public static void maximize(Stage stage) {
    ((WindowDecorator) stage.getProperties().get("windowDecorator")).maximize();
  }
}

