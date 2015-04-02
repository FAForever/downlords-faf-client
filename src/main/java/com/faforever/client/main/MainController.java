package com.faforever.client.main;

import com.faforever.client.chat.ChatController;
import com.faforever.client.fx.SceneFactory;
import com.faforever.client.games.GamesController;
import com.faforever.client.legacy.ServerAccessor;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.WindowPrefs;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.JavaFxUtil;
import com.faforever.client.whatsnew.WhatsNewController;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;

public class MainController {

  @FXML
  Button minimizeButton;

  @FXML
  Button maximizeButton;

  @FXML
  Button restoreButton;

  @FXML
  Button closeButton;

  @FXML
  TabPane mainTabPane;

  @FXML
  Parent mainRoot;

  @FXML
  WhatsNewController whatsNewController;

  @FXML
  ChatController chatController;

  @FXML
  GamesController gamesController;

  @FXML
  private Node titlePane;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  SceneFactory sceneFactory;

  @Autowired
  ServerAccessor serverAccessor;

  private double xOffset;
  private double yOffset;

  public void display(Stage stage) {
    final WindowPrefs mainWindowPrefs = preferencesService.getPreferences().getMainWindow();

    Scene scene = sceneFactory.createScene(mainRoot);

    stage.setScene(scene);
    stage.setTitle("FA Forever");
    stage.setResizable(true);
    restoreState(mainWindowPrefs, stage);
    configureWindowButtons(stage);
    configureWindowDragable(stage);
    stage.show();
    JavaFxUtil.centerOnScreen(stage);

    registerWindowPreferenceListeners(stage, mainWindowPrefs);
    registerSelectedTabListener(mainWindowPrefs);

    whatsNewController.load();
    chatController.load();

    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        while (isCancelled()) {
          serverAccessor.connect();
        }
        return null;
      }
    });
  }

  private void configureWindowDragable(Stage stage) {
    titlePane.setOnMousePressed(new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent event) {
        xOffset = stage.getX() - event.getScreenX();
        yOffset = stage.getY() - event.getScreenY();
      }
    });
    titlePane.setOnMouseDragged(new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent event) {
        stage.setX(event.getScreenX() + xOffset);
        stage.setY(event.getScreenY() + yOffset);
      }
    });
  }

  private void configureWindowButtons(Stage stage) {
    maximizeButton.managedProperty().bind(maximizeButton.visibleProperty());
    restoreButton.managedProperty().bind(restoreButton.visibleProperty());

    maximizeButton.setOnAction(event -> {
      stage.setMaximized(true);
      stage.setX(0);
      stage.setY(0);
      stage.setWidth(getVisualBounds(stage).getWidth());
      stage.setHeight(getVisualBounds(stage).getHeight());
      maximizeButton.setVisible(false);
      restoreButton.setVisible(true);
    });
    minimizeButton.setOnAction(event -> stage.setIconified(true));
    restoreButton.setOnAction(event -> {
      stage.setMaximized(false);
      maximizeButton.setVisible(true);
      restoreButton.setVisible(false);
    });
    closeButton.setOnAction(event -> stage.close());

    maximizeButton.setVisible(!stage.isMaximized());
    restoreButton.setVisible(stage.isMaximized());
  }

  public Rectangle2D getVisualBounds(Stage stage) {
    ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(
        stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()
    );
    return screensForRectangle.get(0).getVisualBounds();
  }

  private void restoreState(WindowPrefs mainWindowPrefs, Stage stage) {
    if (mainWindowPrefs.isMaximized()) {
      stage.setMaximized(true);
    } else {
      stage.setWidth(mainWindowPrefs.getWidth());
      stage.setHeight(mainWindowPrefs.getHeight());
    }

    if (mainWindowPrefs.getTab() != null) {
      mainTabPane.getTabs().stream()
          .filter(tab -> tab.getId() != null && tab.getId().equals(mainWindowPrefs.getTab()))
          .forEach(tab -> mainTabPane.getSelectionModel().select(tab));
    }
  }

  private void registerSelectedTabListener(final WindowPrefs mainWindowPrefs) {
    mainTabPane.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) -> {
          mainWindowPrefs.setTab(newValue.getId());
          preferencesService.storeInBackground();
        }
    );
  }

  private void registerWindowPreferenceListeners(final Stage stage, final WindowPrefs mainWindowPrefs) {
    stage.maximizedProperty().addListener((observable, oldValue, newValue) -> {
      mainWindowPrefs.setMaximized(newValue);
    });
    stage.heightProperty().addListener((observable, oldValue, newValue) -> {
      mainWindowPrefs.setHeight(newValue.intValue());
      preferencesService.storeInBackground();
    });
    stage.widthProperty().addListener((observable, oldValue, newValue) -> {
      mainWindowPrefs.setWidth(newValue.intValue());
      preferencesService.storeInBackground();
    });
  }

  public WhatsNewController getWhatsNewController() {
    return whatsNewController;
  }

  public ChatController getChatController() {
    return chatController;
  }

  public GamesController getGamesController() {
    return gamesController;
  }
}
