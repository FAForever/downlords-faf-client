package com.faforever.client.main;

import com.faforever.client.chat.ChatController;
import com.faforever.client.fx.SceneFactory;
import com.faforever.client.fx.WindowDecorator;
import com.faforever.client.game.GamesController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.OnLobbyConnectedListener;
import com.faforever.client.legacy.OnLobbyConnectingListener;
import com.faforever.client.legacy.OnLobbyDisconnectedListener;
import com.faforever.client.lobby.LobbyService;
import com.faforever.client.network.PortCheckService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.WindowPrefs;
import com.faforever.client.util.Callback;
import com.faforever.client.util.JavaFxUtil;
import com.faforever.client.news.NewsController;
import com.faforever.client.vault.VaultController;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;

import static com.faforever.client.fx.WindowDecorator.WindowButtonType.CLOSE;
import static com.faforever.client.fx.WindowDecorator.WindowButtonType.MAXIMIZE_RESTORE;
import static com.faforever.client.fx.WindowDecorator.WindowButtonType.MINIMIZE;

public class MainController implements OnLobbyConnectedListener, OnLobbyConnectingListener, OnLobbyDisconnectedListener {

  @FXML
  Pane contentPane;

  @FXML
  ButtonBase newsButton;

  @FXML
  Parent mainRoot;

  @FXML
  Label statusLabel;

  @FXML
  Label natStatusLabel;

  @Autowired
  NewsController newsController;

  @Autowired
  ChatController chatController;

  @Autowired
  GamesController gamesController;

  @Autowired
  VaultController vaultController;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  SceneFactory sceneFactory;

  @Autowired
  LobbyService lobbyService;

  @Autowired
  PortCheckService portCheckService;

  @Autowired
  private I18n i18n;

  public void display(Stage stage) {
    lobbyService.setOnFafConnectedListener(this);
    lobbyService.setOnLobbyConnectingListener(this);
    lobbyService.setOnLobbyDisconnectedListener(this);

    final WindowPrefs mainWindowPrefs = preferencesService.getPreferences().getMainWindow();

    sceneFactory.createScene(stage, mainRoot, true, MINIMIZE, MAXIMIZE_RESTORE, CLOSE);

    stage.setTitle("FA Forever");
    restoreState(mainWindowPrefs, stage);
    stage.show();
    JavaFxUtil.centerOnScreen(stage);

    registerWindowPreferenceListeners(stage, mainWindowPrefs);

    newsController.setUp();
    chatController.setUp();
    gamesController.setUp(stage);

    // FIXME i18n/icons
    natStatusLabel.setText("Checking NAT");
    portCheckService.checkUdpPortInBackground(preferencesService.getPreferences().getForgedAlliance().getPort(), new Callback<Boolean>() {
      @Override
      public void success(Boolean result) {
        // FIXME add iamge
        if (result) {
          natStatusLabel.setText("NAT: OK");
        } else {
          natStatusLabel.setText("NAT: Closed");
        }
      }

      @Override
      public void error(Throwable e) {
        // FIXME add image
        natStatusLabel.setText("NAT: Error");
      }
    });
  }

  private void restoreState(WindowPrefs mainWindowPrefs, Stage stage) {
    if (mainWindowPrefs.isMaximized()) {
      WindowDecorator.maximize(stage);
    }

    String lastView = mainWindowPrefs.getLastView();
    if (lastView != null) {
      contentPane.getChildren().stream()
          .filter(navigationItem -> navigationItem.getId() != null && navigationItem.getId().equals(lastView))
          .filter(button -> button instanceof ToggleButton)
          .forEach(navigationItem -> {
            ToggleButton item = (ToggleButton) navigationItem;
            item.setSelected(true);
            item.fire();
          });
    } else {
      newsButton.fire();
    }
  }

  @FXML
  void onNewsButton(ActionEvent event) {
    setContent(newsController.getRoot());
  }

  @FXML
  void onChatButton(ActionEvent event) {
    setContent(chatController.getRoot());
  }

  @FXML
  void onGamesButton(ActionEvent event) {
    setContent(gamesController.getRoot());
  }

  @FXML
  void onVaultButton(ActionEvent event) {
    setContent(vaultController.getRoot());
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

  @Override
  public void onFaConnected() {
    statusLabel.setText(i18n.get("statusbar.connected"));
  }

  @Override
  public void onFaConnecting() {
    statusLabel.setText(i18n.get("statusbar.connecting"));
  }

  @Override
  public void onFaDisconnected() {
    statusLabel.setText(i18n.get("statusbar.disconnected"));
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

  public void onLeaderboardButton(ActionEvent event) {

  }
}
