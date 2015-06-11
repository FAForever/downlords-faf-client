package com.faforever.client.main;

import com.faforever.client.chat.ChatController;
import com.faforever.client.chat.ChatService;
import com.faforever.client.fx.SceneFactory;
import com.faforever.client.fx.WindowDecorator;
import com.faforever.client.game.GamesController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LadderController;
import com.faforever.client.legacy.OnLobbyConnectedListener;
import com.faforever.client.legacy.OnLobbyConnectingListener;
import com.faforever.client.legacy.OnLobbyDisconnectedListener;
import com.faforever.client.lobby.LobbyService;
import com.faforever.client.network.PortCheckService;
import com.faforever.client.news.NewsController;
import com.faforever.client.patch.PatchService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.WindowPrefs;
import com.faforever.client.taskqueue.PrioritizedTask;
import com.faforever.client.taskqueue.TaskQueueService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Callback;
import com.faforever.client.util.JavaFxUtil;
import com.faforever.client.vault.VaultController;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.controlsfx.control.TaskProgressView;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static com.faforever.client.fx.WindowDecorator.WindowButtonType.CLOSE;
import static com.faforever.client.fx.WindowDecorator.WindowButtonType.MAXIMIZE_RESTORE;
import static com.faforever.client.fx.WindowDecorator.WindowButtonType.MINIMIZE;

public class MainController implements OnLobbyConnectedListener, OnLobbyConnectingListener, OnLobbyDisconnectedListener {

  @FXML
  Pane contentPane;

  @FXML
  ToggleGroup mainNavigationToggleGroup;

  @FXML
  ButtonBase newsButton;

  @FXML
  ButtonBase chatButton;

  @FXML
  ButtonBase gamesButton;

  @FXML
  ButtonBase vaultButton;

  @FXML
  ButtonBase ladderButton;

  @FXML
  ProgressBar progressBar;

  @FXML
  Region mainRoot;

  @FXML
  Label statusLabel;

  @FXML
  Label natStatusLabel;

  @FXML
  MenuButton usernameButton;

  @FXML
  Pane taskQueuePane;

  @Autowired
  NewsController newsController;

  @Autowired
  ChatController chatController;

  @Autowired
  GamesController gamesController;

  @Autowired
  VaultController vaultController;

  @Autowired
  LadderController ladderController;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  SceneFactory sceneFactory;

  @Autowired
  LobbyService lobbyService;

  @Autowired
  PortCheckService portCheckService;

  @Autowired
  PatchService patchService;

  @Autowired
  ChatService chatService;

  @Autowired
  I18n i18n;

  @Autowired
  UserService userService;

  @Autowired
  TaskQueueService taskQueueService;

  private Stage stage;

  @FXML
  void initialize() {
    contentPane.managedProperty().bind(contentPane.visibleProperty());

    taskQueueService.addChangeListener(change -> {
      if (change.wasAdded()) {
        addPrioritizedTaskToQueuePane(change.getAddedSubList());
      }
    });
  }

  private void addPrioritizedTaskToQueuePane(List<? extends PrioritizedTask<?>> tasks) {
    new TaskProgressView<>();

    List<Node> taskPanes = new ArrayList<>();

    for (Node taskPane : taskPanes) {
      taskPanes.add(taskPane);
    }

    taskQueuePane.getChildren().setAll(taskPanes);

    setCurrentTask(tasks.get(tasks.size() - 1));
  }

  private void setCurrentTask(PrioritizedTask<?> node) {
  }

  public void display(Stage stage) {
    this.stage = stage;

    lobbyService.setOnFafConnectedListener(this);
    lobbyService.setOnLobbyConnectingListener(this);
    lobbyService.setOnLobbyDisconnectedListener(this);

    chatService.connect();

    final WindowPrefs mainWindowPrefs = preferencesService.getPreferences().getMainWindow();

    sceneFactory.createScene(stage, mainRoot, true, MINIMIZE, MAXIMIZE_RESTORE, CLOSE);

    stage.setTitle("FA Forever");
    restoreState(mainWindowPrefs, stage);
    stage.show();
    JavaFxUtil.centerOnScreen(stage);

    registerWindowPreferenceListeners(stage, mainWindowPrefs);

    usernameButton.setText(userService.getUsername());

    checkUdpPort();
    checkForFafUpdate();
  }

  private void checkForFafUpdate() {
    patchService.needsPatching(new Callback<Boolean>() {
      @Override
      public void success(Boolean needsPatching) {
        if (!needsPatching) {
          return;
        }

        ButtonType updateNowButtonType = new ButtonType(i18n.get("patch.updateAvailable.updateNow"));
        ButtonType updateLaterButtonType = new ButtonType(i18n.get("patch.updateAvailable.updateLater"));

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(i18n.get("patch.updateAvailable.title"));
        alert.setHeaderText(i18n.get("patch.updateAvailable.header"));
        alert.setContentText(i18n.get("patch.updateAvailable.content"));
        alert.getButtonTypes().setAll(updateNowButtonType, updateLaterButtonType);

        alert.resultProperty().addListener((observable, oldValue, newValue) -> {
          if (newValue == updateNowButtonType) {
            Service<Void> patchTask = patchService.patchInBackground(new Callback<Void>() {
              @Override
              public void success(Void result) {

              }

              @Override
              public void error(Throwable e) {

              }
            });
            // TODO show update progress somewhere
          }
        });

        alert.show();
      }

      @Override
      public void error(Throwable e) {

      }
    });
  }

  private void checkUdpPort() {
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
    } else {
      stage.setWidth(mainWindowPrefs.getWidth());
      stage.setHeight(mainWindowPrefs.getHeight());
    }


    String lastView = mainWindowPrefs.getLastView();
    if (lastView != null) {
      mainNavigationToggleGroup.getToggles().stream()
          .filter(button -> button instanceof ToggleButton)
          .filter(navigationItem -> lastView.equals(((Node) navigationItem).getId()))
          .forEach(navigationItem -> {
            ((ToggleButton) navigationItem).fire();
          });
    } else {
      newsButton.fire();
    }
  }

  @FXML
  void onNavigationButton(ActionEvent event) {
    ToggleButton button = (ToggleButton) event.getSource();
    preferencesService.getPreferences().getMainWindow().setLastView(button.getId());
    preferencesService.storeInBackground();

    if (!button.isSelected()) {
      button.setSelected(true);
    }

    // TODO let the component initialize themselves instead of calling a setUp method
    if (button == newsButton) {
      newsController.setUpIfNecessary();
      setContent(newsController.getRoot());
    } else if (button == chatButton) {
      setContent(chatController.getRoot());
    } else if (button == gamesButton) {
      gamesController.setUpIfNecessary(stage);
      setContent(gamesController.getRoot());
    } else if (button == vaultButton) {
      vaultController.setUpIfNecessary();
      setContent(vaultController.getRoot());
    } else if (button == ladderButton) {
      ladderController.setUpIfNecessary();
      setContent(ladderController.getRoot());
    }
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
}
