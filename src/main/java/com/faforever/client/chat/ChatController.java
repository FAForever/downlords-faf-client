package com.faforever.client.chat;

import com.faforever.client.chat.event.ChatMessageEvent;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.WindowController;
import com.faforever.client.fx.WindowController.WindowButtonType;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserService;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.util.ProgrammingError;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sun.glass.ui.Robot;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Transform;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChatController extends AbstractViewController<Node> {

  @VisibleForTesting
  protected final Map<String, AbstractChatTabController> nameToChatTabController;
  private final ChatService chatService;
  private final UiService uiService;
  private final UserService userService;
  private final EventBus eventBus;
  private final I18n i18n;
  public Node chatRoot;
  public TabPane tabPane;
  public Pane connectingProgressPane;
  public VBox openNewChannelContainer;
  public TextField channelNameTextField;
  public ArrayList<String> outSourcedChannels;
  private Robot robot;
  private Tab addChannelTab;

  @Inject
  public ChatController(ChatService chatService, UiService uiService, UserService userService, EventBus eventBus, I18n i18n) {
    this.chatService = chatService;
    this.uiService = uiService;
    this.i18n = i18n;
    this.userService = userService;
    this.eventBus = eventBus;

    Platform.runLater(() -> robot = com.sun.glass.ui.Application.GetApplication().createRobot());

    nameToChatTabController = new HashMap<>();
  }

  private void onChannelLeft(Channel channel) {
    removeTab(channel.getName());
  }

  private void onChannelJoined(Channel channel) {
    Platform.runLater(() -> getOrCreateChannelTab(channel.getName()));
  }

  private void onDisconnected() {
    connectingProgressPane.setVisible(true);
    tabPane.setVisible(false);
  }

  private void onConnected() {
    connectingProgressPane.setVisible(false);
    tabPane.setVisible(true);
  }

  private void onConnecting() {
    connectingProgressPane.setVisible(true);
    tabPane.setVisible(false);
  }

  private void onLoggedOut() {
    Platform.runLater(() -> tabPane.getTabs().clear());
  }

  private void removeTab(String playerOrChannelName) {
    nameToChatTabController.remove(playerOrChannelName);

    if (nameToChatTabController.containsKey(playerOrChannelName)) {
      tabPane.getTabs().remove(nameToChatTabController.remove(playerOrChannelName).getRoot());
    }
  }

  private AbstractChatTabController getOrCreateChannelTab(String channelName) {
    JavaFxUtil.assertApplicationThread();
    if (!nameToChatTabController.containsKey(channelName)) {
      ChannelTabController tab = uiService.loadFxml("theme/chat/channel_tab.fxml");
      tab.setChannel(chatService.getOrCreateChannel(channelName));
      addTab(channelName, tab);
    }
    return nameToChatTabController.get(channelName);
  }

  @VisibleForTesting
  protected void addTab(String playerOrChannelName, AbstractChatTabController tabController) {
    JavaFxUtil.assertApplicationThread();
    nameToChatTabController.put(playerOrChannelName, tabController);
    Tab newTab = tabController.getRoot();
    MenuItem menuItem = new MenuItem(i18n.get("chat.popOutTab"));
    ContextMenu menu = new ContextMenu(menuItem);
    menu.setOnAction(event -> showSeperateWindow(newTab));
    newTab.setContextMenu(menu);
    tabPane.getTabs().add(tabPane.getTabs().size() - 1, newTab);
    tabPane.getSelectionModel().select(newTab);
  }

  private void showSeperateWindow(Tab newTab) {
    Stage stage = new Stage(StageStyle.UNDECORATED);
    outSourcedChannels.add(newTab.getId());
    Pane content = (Pane) newTab.getContent();
    tabPane.getTabs().remove(newTab);
    newTab.setContent(null);
    final Scene scene = new Scene(new Pane(content), content.getWidth(), content.getHeight());
    stage.setScene(scene);
    stage.setTitle(newTab.getText());

    Screen screen = Screen.getScreensForRectangle(robot.getMouseX(), robot.getMouseY(), 1, 1).get(0);
    stage.setX(screen.getVisualBounds().getMinX());
    stage.setY(screen.getVisualBounds().getMinY());

    assureCorrectLayout(content, scene);

    WindowController windowController = uiService.loadFxml("theme/window.fxml");
    windowController.configure(scene, stage, true, WindowButtonType.CLOSE, WindowButtonType.MAXIMIZE_RESTORE, WindowButtonType.MINIMIZE);
    windowController.setOnClose(this.new closureHandler(newTab, content, stage));

    stage.show();
  }

  private void detectDrages() {
    tabPane.setOnDragDetected(
        event -> {
          Tab selectedTab = ((TabPane) event.getSource()).getSelectionModel().getSelectedItem();
          if (event.getSource().equals(tabPane) && !selectedTab.equals(addChannelTab)) {
            tabPane.setCursor(Cursor.MOVE);
            Pane root = (Pane) tabPane.getScene().getRoot();
            root.setOnDragOver((DragEvent event1) -> {
              event1.acceptTransferModes(TransferMode.ANY);
              event1.consume();
            });
            SnapshotParameters snapshotParams = new SnapshotParameters();
            snapshotParams.setTransform(Transform.scale(0.4, 0.4));
            WritableImage snapshot = selectedTab.getContent().snapshot(snapshotParams, null);
            Dragboard db = tabPane.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.put(DataFormat.PLAIN_TEXT, "");
            db.setDragView(snapshot, 40, 40);
            db.setContent(clipboardContent);
          }
          event.consume();
        }
    );

    tabPane.setOnDragDone(
        (DragEvent event) -> {
          showSeperateWindow(((TabPane) event.getSource()).getSelectionModel().getSelectedItem());
          tabPane.setCursor(Cursor.DEFAULT);
          event.consume();
        }
    );
  }

  private void assureCorrectLayout(Region content, Scene scene) {
    content.setPadding(new Insets(20));
    content.setPrefWidth(scene.getWidth());
    content.setPrefHeight(scene.getHeight());
    scene.widthProperty().addListener((observable, oldValue, newValue) -> content.setPrefWidth((Double) newValue));
    scene.heightProperty().addListener((observable, oldValue, newValue) -> content.setPrefHeight((Double) newValue));
  }

  @Override
  public void initialize() {
    super.initialize();
    eventBus.register(this);
    outSourcedChannels = new ArrayList<>();
    tabPane.getTabs().addListener((InvalidationListener) observable ->

    chatService.addChannelsListener(change -> {
      if (change.wasRemoved()) {
        onChannelLeft(change.getValueRemoved());
      }
      if (change.wasAdded()) {
        onChannelJoined(change.getValueAdded());
      }
    }));
    addChannelTab = new Tab("+");
    tabPane.getTabs().add(tabPane.getTabs().size(), addChannelTab);
    addChannelTab.setContent(openNewChannelContainer);
    addChannelTab.setClosable(false);


    chatService.connectionStateProperty().addListener((observable, oldValue, newValue) -> onConnectionStateChange(newValue));
    onConnectionStateChange(chatService.connectionStateProperty().get());

    tabPane.getTabs().addListener((ListChangeListener<Tab>) change -> {
      while (change.next()) {
        change.getRemoved().forEach(tab -> {
          if (!outSourcedChannels.contains(tab.getId())) {
            nameToChatTabController.remove(tab.getId());
          }
        });
      }
    });
    detectDrages();
  }

  @Subscribe
  public void onLoggedOutEvent(LoggedOutEvent event) {
    onLoggedOut();
  }

  private void onConnectionStateChange(ConnectionState newValue) {
    switch (newValue) {
      case DISCONNECTED:
        onDisconnected();
        break;
      case CONNECTED:
        onConnected();
        break;
      case CONNECTING:
        onConnecting();
        break;
      default:
        throw new ProgrammingError("Uncovered connection state: " + newValue);
    }
  }

  @Subscribe
  public void onChatMessage(ChatMessageEvent event) {
    Platform.runLater(() -> {
      ChatMessage message = event.getMessage();
      if (Objects.toString(message.getSource(), "").startsWith("#")) {
        getOrCreateChannelTab(message.getSource()).onChatMessage(message);
      } else {
        addAndGetPrivateMessageTab(message.getSource()).onChatMessage(message);
      }
    });
  }

  private AbstractChatTabController addAndGetPrivateMessageTab(String username) {
    JavaFxUtil.assertApplicationThread();
    if (!nameToChatTabController.containsKey(username)) {
      PrivateChatTabController tab = uiService.loadFxml("theme/chat/private_chat_tab.fxml");
      tab.setReceiver(username);
      addTab(username, tab);
    }

    return nameToChatTabController.get(username);
  }

  public Node getRoot() {
    return chatRoot;
  }

  @Subscribe
  public void onInitiatePrivateChatEvent(InitiatePrivateChatEvent event) {
    Platform.runLater(() -> openPrivateMessageTabForUser(event.getUsername()));
  }

  private void openPrivateMessageTabForUser(String username) {
    if (username.equalsIgnoreCase(userService.getUsername())) {
      return;
    }
    AbstractChatTabController controller = addAndGetPrivateMessageTab(username);
    tabPane.getSelectionModel().select(controller.getRoot());
  }

  public void onJoinChannelButtonClicked() {
    String channelName = channelNameTextField.getText();
    channelNameTextField.clear();

    joinChannel(channelName);
  }

  private void joinChannel(String channelName) {
    chatService.addUsersListener(channelName, change -> {
      if (change.wasRemoved()) {
        onChatUserLeftChannel(channelName, change.getValueRemoved().getUsername());
      }
      if (change.wasAdded()) {
        onUserJoinedChannel(channelName, change.getValueAdded());
      }
    });
    chatService.joinChannel(channelName);
  }

  private void onChatUserLeftChannel(String channelName, String username) {
    if (!username.equalsIgnoreCase(userService.getUsername())) {
      return;
    }
    AbstractChatTabController chatTab = nameToChatTabController.get(channelName);
    if (chatTab != null) {
      tabPane.getTabs().remove(chatTab.getRoot());
    }
  }

  private void onUserJoinedChannel(String channelName, ChatUser chatUser) {
    Platform.runLater(() -> {
      if (isCurrentUser(chatUser)) {
        onConnected();
      }
    });
  }

  private boolean isCurrentUser(ChatUser chatUser) {
    return chatUser.getUsername().equalsIgnoreCase(userService.getUsername());
  }

  @Override
  protected void onDisplay() {
    Tab tab = tabPane.getSelectionModel().getSelectedItem();
    super.onDisplay();
    if ((tabPane.getTabs().size() > 1) && !addChannelTab.equals(tab)) {
      nameToChatTabController.get(tab.getId()).onDisplay();
    }
  }

  @Override
  protected void onHide() {
    super.onHide();
    if (!tabPane.getTabs().isEmpty()) {
      Tab tab = tabPane.getSelectionModel().getSelectedItem();
      Optional.ofNullable(nameToChatTabController.get(tab.getId())).ifPresent(AbstractChatTabController::onHide);
    }
  }

  @VisibleForTesting
  protected int getIndexOfTab(Tab tab) {
    int i = 0;
    for (String tabId : nameToChatTabController.keySet()) {
      if (tab.getId() == (tabId)) {
        return i;
      } else {
        i++;
      }
    }
    return tabPane.getTabs().size() - 1;
  }

  private class closureHandler implements Runnable {
    private Stage stage;
    private Pane content;
    private Tab newTab;

    private closureHandler(Tab newTab, Pane content, Stage stage) {
      this.newTab = newTab;
      this.content = content;
      this.stage = stage;
    }

    @Override
    public void run() {
      content.setPadding(Insets.EMPTY);
      newTab.setContent(content);
      tabPane.getTabs().add(getIndexOfTab(newTab), newTab);
      tabPane.getSelectionModel().select(newTab);
      outSourcedChannels.remove(newTab.getId());
      stage.close();
    }
  }
}
