package com.faforever.client.chat.test;

import com.faforever.client.chat.ChatChannel;
import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ChatService;
import com.faforever.client.chat.ChatUserCategory;
import com.faforever.client.chat.ChatUserService;
import com.faforever.client.chat.UserListCustomizationController;
import com.faforever.client.chat.event.ChatUserCategoryChangeEvent;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.MapProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.collections.WeakMapChangeListener;
import javafx.collections.WeakSetChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ChatUserListController implements Controller<VBox>, InitializingBean {

  private final PreferencesService preferencesService;
  private final UiService uiService;
  private final TaskScheduler taskScheduler;
  private final ChatService chatService;
  private final I18n i18n;
  private final EventBus eventBus;
  private final PlayerService playerService;
  private final ChatUserService chatUserService;

  public VBox root;
  public ToggleButton advancedFiltersToggleButton;
  public TextField searchUsernameTextField;
  public Button listCustomizationButton;
  public VBox userListContainer;

  private final Map<ChatUserCategory, List<ChatUserItem>> categoriesToUsers = new HashMap<>();
  private final Map<String, List<ChatUserItem>> usernameToChatUserList = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  private final ObservableList<ListItem> source = FXCollections.observableArrayList();

  private MapProperty<String, ObservableList<ChatUserCategory>> channelNameToHiddenCategories;
  private ObservableList<ChatUserCategory> hiddenCategories;
  private final ObservableSet<ChatUserCategory> visibleCategories = FXCollections.observableSet();

  private String channelName;
  private ChatChannel chatChannel;

  private VirtualFlow<ListItem, Cell<ListItem, Node>> listView;
  private FilteredList<ListItem> items;
  private final ExecutorService usersEventQueueExecutor = Executors.newSingleThreadExecutor();

  private Future<?> listInitializationFuture;

  /* ----- Listeners ----- */
  private final ListChangeListener<ChatUserCategory> hiddenCategoriesListener = change -> {
    while (change.next()) {
      if (change.wasAdded()) {
        change.getAddedSubList().forEach(visibleCategories::remove);
      } else if (change.wasRemoved()) {
        visibleCategories.addAll(change.getRemoved());
      }
    }
  };

  private final SetChangeListener<ChatUserCategory> visibleCategoriesListener = change -> {
    if (change.wasAdded()) {
      onVisibleCategoryChanged(change.getElementAdded(), true);
    } else {
      onVisibleCategoryChanged(change.getElementRemoved(), false);
    }
  };

  private final MapChangeListener<String, ChatChannelUser> channelUserListListener = change -> {
    usersEventQueueExecutor.execute(() -> {
      if (change.wasAdded()) {
        onUserJoined(change.getValueAdded());
      } else {
        onUserLeft(change.getValueRemoved());
      }
      updateUserCount();
    });
  };

  @SuppressWarnings("FieldCanBeLocal")
  private MapChangeListener<String, ObservableList<ChatUserCategory>> channelNameToHiddenCategoriesListener;

  @SuppressWarnings("FieldCanBeLocal")
  private InvalidationListener chatConnectionStateListener;

  @Override
  public void afterPropertiesSet() throws Exception {
    eventBus.register(this);
    listInitializationFuture = taskScheduler.schedule(this::initializeList, Instant.now().plus(3000, ChronoUnit.MILLIS));
  }

  public void setChatChannel(ChatChannel chatChannel) {
    this.chatChannel = chatChannel;
    this.channelName = chatChannel.getName();
    this.channelNameToHiddenCategories = preferencesService.getPreferences().getChat().getChannelNameToHiddenCategories();
    this.hiddenCategories = channelNameToHiddenCategories.get(channelName);

    prepareData();
    initializeListeners();
    updateUserCount();
    usersEventQueueExecutor.execute(() -> chatChannel.getUsers().forEach(this::onUserJoined));
  }

  private void prepareData() {
    Arrays.stream(ChatUserCategory.values()).forEach(category -> {
      visibleCategories.add(category);
      categoriesToUsers.put(category, FXCollections.observableArrayList());
      source.add(new ChatUserCategoryItem(category, channelName));
    });
    if (hiddenCategories != null) {
      hiddenCategories.forEach(visibleCategories::remove);
    }
  }

  private void initializeListeners() {
    channelNameToHiddenCategoriesListener = change -> {
      if (change.getKey().equals(channelName)) {
        if (change.wasAdded()) {
          change.getValueAdded().forEach(visibleCategories::remove);
          JavaFxUtil.addListener(change.getValueAdded(), new WeakListChangeListener<>(hiddenCategoriesListener));
        }
        if (change.wasRemoved()) {
          change.getValueRemoved().forEach(visibleCategories::add);
          JavaFxUtil.removeListener(change.getValueRemoved(), hiddenCategoriesListener);
        }
        preferencesService.storeInBackground();
      }
    };

    chatConnectionStateListener = observable -> {
      if (chatService.getConnectionState() == ConnectionState.DISCONNECTED) {
        dispose();
      }
    };

    chatService.addUsersListener(channelName, channelUserListListener);
    JavaFxUtil.addListener(chatService.connectionStateProperty(), new WeakInvalidationListener(chatConnectionStateListener));
    JavaFxUtil.addListener(visibleCategories, new WeakSetChangeListener<>(visibleCategoriesListener));
    JavaFxUtil.addListener((ObservableMap<String, ObservableList<ChatUserCategory>>) channelNameToHiddenCategories,
        new WeakMapChangeListener<>(channelNameToHiddenCategoriesListener));
    if (hiddenCategories != null) {
      JavaFxUtil.addListener(hiddenCategories, new WeakListChangeListener<>(hiddenCategoriesListener));
    }
  }

  private void initializeList() {
    usersEventQueueExecutor.execute(() -> {
      items = new FilteredList<>(new SortedList<>(source, ListItem.getComparator()));
      listView = VirtualFlow.createVertical(items, item -> item.createCell(uiService));
      VirtualizedScrollPane<VirtualFlow<ListItem, Cell<ListItem, Node>>> scrollPane = new VirtualizedScrollPane<>(listView);
      scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
      VBox.setVgrow(scrollPane, Priority.ALWAYS);
      JavaFxUtil.runLater(() -> userListContainer.getChildren().add(scrollPane));
    });
  }

  public void onTabClosed() {
    dispose();
  }

  private void dispose() {
    if (!usersEventQueueExecutor.isShutdown()) {
      usersEventQueueExecutor.execute(() -> {
        eventBus.unregister(this);
        chatService.removeUsersListener(channelName, channelUserListListener);
        usersEventQueueExecutor.shutdownNow();
      });
    }
  }

  private void onVisibleCategoryChanged(ChatUserCategory category, boolean visible) {
    JavaFxUtil.assertApplicationThread();
    if (visible) {
      source.addAll(categoriesToUsers.get(category));
    } else {
      // TODO: Uncomment when the bug will be fixed
      // TODO: https://bugs.openjdk.java.net/browse/JDK-8195750
      // source.removeAll(categoriesToUsers.get(category));
      categoriesToUsers.get(category).forEach(source::remove);
    }
  }

  private void onUserJoined(ChatChannelUser user) {
    playerService.getPlayerByNameIfOnline(user.getUsername()).ifPresent(player -> chatUserService.associatePlayerToChatUser(user, player));
    List<ChatUserItem> chatUserItems = usernameToChatUserList.computeIfAbsent(user.getUsername(), name -> new ArrayList<>());
    if (chatUserItems.isEmpty()) {
      user.getChatUserCategories().forEach(category -> {
        ChatUserItem item = new ChatUserItem(user, category);
        chatUserItems.add(item);
        addUserToList(item);
      });
    }
  }

  private void onUserLeft(ChatChannelUser user) {
    String username = user.getUsername();
    List<ChatUserItem> chatUserItems = usernameToChatUserList.get(username);
    if (chatUserItems != null && !chatUserItems.isEmpty()) {
      usernameToChatUserList.remove(username);
      removeUserItemsFromList(chatUserItems);
    }
  }

  private void addUserToList(ChatUserItem item) {
    Runnable addAction = () -> {
      ChatUserCategory category = item.getCategory();
      categoriesToUsers.get(category).add(item);
      if (visibleCategories.contains(category)) {
        source.add(item);
      }
    };

    if (listInitializationFuture.isDone()) {
      JavaFxUtil.runLater(addAction);
    } else {
      addAction.run();
    }
  }

  private void removeUserItemsFromList(List<ChatUserItem> itemsForRemove) {
    Runnable removeAction = () -> {
      categoriesToUsers.values().forEach(items -> items.removeAll(itemsForRemove));
      // TODO: Uncomment when the bug will be fixed
      // TODO: https://bugs.openjdk.java.net/browse/JDK-8195750
      // source.removeAll(chatUserItems);
      itemsForRemove.forEach(source::remove);
      itemsForRemove.clear();
    };

    if (listInitializationFuture.isDone()) {
      JavaFxUtil.runLater(removeAction);
    } else {
      removeAction.run();
    }
  }

  @Subscribe
  public void onChatUserCategoryChange(ChatUserCategoryChangeEvent event) {
    usersEventQueueExecutor.execute(() -> {
      ChatChannelUser user = event.getChatUser();
      if (chatChannel.containsUser(user)) {
        onUserLeft(user);
        onUserJoined(user);
      } else {
        onUserLeft(user);
      }
    });
  }

  private void updateUserCount() {
    JavaFxUtil.runLater(() -> searchUsernameTextField.setPromptText(i18n.get("chat.userCount", chatChannel.getUserCount())));
  }

  public void onListCustomizationButtonClicked() {
    UserListCustomizationController controller = uiService.loadFxml("theme/chat/user_list_customization.fxml");
    Popup popup = new Popup();
    popup.getContent().add(controller.getRoot());
    popup.setAutoFix(true);
    popup.setAutoHide(true);
    popup.setAnchorLocation(AnchorLocation.WINDOW_TOP_RIGHT);
    Bounds bounds = listCustomizationButton.localToScreen(listCustomizationButton.getBoundsInLocal());
    popup.show(listCustomizationButton.getScene().getWindow(), bounds.getMaxX(), bounds.getMaxY() + 5);
  }

  public void onAdvancedFiltersToggleButtonClicked() {

  }

  @Override
  public VBox getRoot() {
    return root;
  }
}
