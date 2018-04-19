package com.faforever.client.fx;

import com.faforever.client.theme.UiService;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static com.faforever.client.fx.WindowController.WindowButtonType.CLOSE;
import static com.faforever.client.fx.WindowController.WindowButtonType.MAXIMIZE_RESTORE;
import static com.faforever.client.fx.WindowController.WindowButtonType.MINIMIZE;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class WindowController implements Controller<Node> {

  private static final double RESIZE_BORDER_WIDTH = 4d;
  private static final String PROPERTY_WINDOW_DECORATOR = "windowDecorator";
  private static final PseudoClass MAXIMIZED_PSEUDO_STATE = PseudoClass.getPseudoClass("maximized");
  private final UiService uiService;
  public AnchorPane contentPane;
  public Button minimizeButton;
  public Button maximizeButton;
  public Button restoreButton;
  public Button closeButton;
  public AnchorPane windowRoot;
  public Pane windowButtons;
  private Stage stage;
  private boolean resizable;
  private Point2D dragOffset;
  private EnumSet<ResizeDirection> resizeDirections;
  private boolean isResizing;

  @Inject
  public WindowController(UiService uiService) {
    this.uiService = uiService;
  }

  public static Rectangle2D getVisualBounds(Stage stage) {
    double x1 = stage.getX() + (stage.getWidth() / 2);
    double y1 = stage.getY() + (stage.getHeight() / 2);

    Rectangle2D windowCenter = new Rectangle2D(x1, y1, 1, 1);
    ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(windowCenter);
    Screen targetScreen = screensForRectangle.stream().findFirst().orElse(Screen.getPrimary());
    return targetScreen.getVisualBounds();
  }

  public static void maximize(Stage stage) {
    ((WindowController) stage.getProperties().get(PROPERTY_WINDOW_DECORATOR)).maximize();
  }

  @Override
  public Node getRoot() {
    return windowRoot;
  }

  public void onMinimizeButtonClicked() {
    stage.setIconified(true);
  }

  public void onCloseButtonClicked() {
    stage.close();
  }

  public void onRestoreButtonClicked() {
    restore();
  }

  public void setOnHiding(EventHandler<WindowEvent> eventEventHandler) {
    stage.setOnHiding(eventEventHandler);
  }

  private void restore() {
    windowRoot.pseudoClassStateChanged(MAXIMIZED_PSEUDO_STATE, false);

    stage.setMaximized(false);
    AnchorPane.setTopAnchor(contentPane, 0d);
    AnchorPane.setRightAnchor(contentPane, 0d);
    AnchorPane.setBottomAnchor(contentPane, 0d);
    AnchorPane.setLeftAnchor(contentPane, 0d);

    AnchorPane.setRightAnchor(windowButtons, RESIZE_BORDER_WIDTH);
  }

  public void onMaximizeButtonClicked() {
    maximize();
  }

  public void maximize() {
    windowRoot.pseudoClassStateChanged(MAXIMIZED_PSEUDO_STATE, true);

    Rectangle2D visualBounds = getVisualBounds(stage);

    stage.setMaximized(true);

    stage.setWidth(visualBounds.getWidth());
    stage.setHeight(visualBounds.getHeight());
    stage.setX(visualBounds.getMinX());
    stage.setY(visualBounds.getMinY());

    AnchorPane.setTopAnchor(contentPane, 0d);
    AnchorPane.setRightAnchor(contentPane, 0d);
    AnchorPane.setBottomAnchor(contentPane, 0d);
    AnchorPane.setLeftAnchor(contentPane, 0d);

    AnchorPane.setRightAnchor(windowButtons, 0d);
  }

  public void initialize() {
    minimizeButton.managedProperty().bind(minimizeButton.visibleProperty());
    maximizeButton.managedProperty().bind(maximizeButton.visibleProperty());
    restoreButton.managedProperty().bind(restoreButton.visibleProperty());
    closeButton.managedProperty().bind(closeButton.visibleProperty());
  }

  public void configure(Stage stage, Region content, boolean resizable, WindowButtonType... buttons) {
    if (this.stage != null) {
      throw new IllegalStateException("Already configured");
    }

    this.stage = stage;
    this.resizable = resizable;

    Scene scene = new Scene(windowRoot);
    stage.setScene(scene);
    uiService.registerScene(scene);

    // Configure these only once per stage
    if (!stage.getProperties().containsKey(PROPERTY_WINDOW_DECORATOR)) {
      stage.getProperties().put(PROPERTY_WINDOW_DECORATOR, this);
      JavaFxUtil.addListener(stage.iconifiedProperty(), (observable, oldValue, newValue) -> {
        if (!newValue && stage.isMaximized()) {
          maximize();
        }
      });
    }

    maximizeButton.managedProperty().bind(maximizeButton.visibleProperty());
    restoreButton.managedProperty().bind(restoreButton.visibleProperty());

    List<WindowButtonType> buttonList = Arrays.asList(buttons);

    if (!resizable) {
      maximizeButton.setVisible(false);
      restoreButton.setVisible(false);
    } else if (buttonList.contains(MAXIMIZE_RESTORE)) {
      maximizeButton.visibleProperty().bind(stage.maximizedProperty().not());
      restoreButton.visibleProperty().bind(stage.maximizedProperty());
    } else {
      maximizeButton.setVisible(false);
      restoreButton.setVisible(false);
    }

    minimizeButton.setVisible(buttonList.contains(MINIMIZE));
    closeButton.setVisible(buttonList.contains(CLOSE));
    resizeDirections = EnumSet.noneOf(ResizeDirection.class);

    if (stage.isMaximized()) {
      maximize();
    } else {
      restore();
    }

    setContent(content);
  }

  public void setContent(Region content) {
    contentPane.getChildren().setAll(content);
    AnchorPane.setTopAnchor(content, 0d);
    AnchorPane.setRightAnchor(content, 0d);
    AnchorPane.setBottomAnchor(content, 0d);
    AnchorPane.setLeftAnchor(content, 0d);

    if (content.getMinWidth() > 0) {
      stage.minWidthProperty().bind(content.minWidthProperty());
    }

    if (content.getMinHeight() > 0) {
      stage.minHeightProperty().bind(content.minHeightProperty());
    }

    windowRoot.requestLayout();

  }

  public Parent getWindowRoot() {
    return windowRoot;
  }

  public void onMouseMoved(MouseEvent event) {
    if (!resizable || stage.isMaximized()) {
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

  public void onMouseDragged(MouseEvent event) {
    if (dragOffset == null) {
      // Somehow the drag event occurred without an initial press event
      onMousePressed(event);
    }

    if (isResizing) {
      onWindowResize(event);
    } else {
      onWindowMove(event);
    }
  }

  public void onMousePressed(MouseEvent event) {
    if (isOnResizeBorder(event)) {
      isResizing = true;
    }

    dragOffset = new Point2D(event.getScreenX() - stage.getX(), event.getScreenY() - stage.getY());
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

    if (newHeight < stage.getMinHeight()) {
      newHeight = stage.getMinHeight();
    }
    if (newWidth < stage.getMinWidth()) {
      newWidth = stage.getMinWidth();
    }

    stage.setHeight(newHeight);
    stage.setWidth(newWidth);

    event.consume();
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

  private boolean isOnResizeBorder(MouseEvent event) {
    return event.getY() > stage.getHeight() - RESIZE_BORDER_WIDTH
        || event.getY() < RESIZE_BORDER_WIDTH
        || event.getX() > stage.getWidth() - RESIZE_BORDER_WIDTH
        || event.getX() < RESIZE_BORDER_WIDTH;
  }

  public void onMouseClicked(MouseEvent event) {
    if (event.getTarget() instanceof Pane
        && event.getButton() == MouseButton.PRIMARY
        && event.getClickCount() == 2 && resizable) {
      if (stage.isMaximized()) {
        restore();
      } else {
        maximize();
      }
    }
  }

  public void onMouseReleased() {
    isResizing = false;
    dragOffset = null;
  }

  public void onMouseExited() {
    windowRoot.setCursor(Cursor.DEFAULT);
  }

  public enum WindowButtonType {
    MINIMIZE,
    MAXIMIZE_RESTORE,
    CLOSE
  }

  private enum ResizeDirection {
    NORTH,
    EAST,
    SOUTH,
    WEST
  }
}
