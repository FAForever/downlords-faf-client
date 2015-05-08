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
import com.faforever.client.whatsnew.WhatsNewController;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;

import static com.faforever.client.fx.WindowDecorator.WindowButtonType.CLOSE;
import static com.faforever.client.fx.WindowDecorator.WindowButtonType.MAXIMIZE_RESTORE;
import static com.faforever.client.fx.WindowDecorator.WindowButtonType.MINIMIZE;

public class MainController implements OnLobbyConnectedListener, OnLobbyConnectingListener, OnLobbyDisconnectedListener {

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
  Label statusLabel;

  @FXML
  Label natStatusLabel;

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
    registerSelectedTabListener(mainWindowPrefs);

    whatsNewController.configure();
    chatController.configure();
    gamesController.configure(stage);

    // FIXME i18n
    natStatusLabel.setText("Checking NAT");
    portCheckService.checkUdpPortInBackground(preferencesService.getPreferences().getForgedAlliance().getPort(), new Callback<Boolean>() {
      @Override
      public void success(Boolean result) {
        // FIXME add iamge
        if(result)
        natStatusLabel.setText("NAT: OK"); else
          natStatusLabel.setText("NAT: Closed");
      }

      @Override
      public void error(Throwable e) {
        // FIXME add iamge
        natStatusLabel.setText("NAT: Error");
      }
    });
  }

  private void restoreState(WindowPrefs mainWindowPrefs, Stage stage) {
    if(mainWindowPrefs.isMaximized()) {
      WindowDecorator.maximize(stage);
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
}
