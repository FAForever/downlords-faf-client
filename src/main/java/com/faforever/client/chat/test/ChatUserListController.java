package com.faforever.client.chat.test;

import com.faforever.client.chat.ChatChannel;
import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ChatService;
import com.faforever.client.chat.ChatUserCategory;
import com.faforever.client.chat.ChatUserService;
import com.faforever.client.chat.event.ChatUserCategoryChangeEvent;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.property.MapProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.WeakListChangeListener;
import javafx.collections.WeakMapChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

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
  public TextField searchTextField;
  public Button listCustomizationButton;
  public VBox userListContainer;

  private final Map<ChatUserCategory, List<ChatUserItem>> categoriesToUsers = new HashMap<>();
  private final Map<String, List<ChatUserItem>> usernameToChatUserList = Collections.synchronizedMap(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
  private final ObservableList<ListItem> source = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
  private final Object monitor = new Object();

  private MapProperty<String, ObservableList<ChatUserCategory>> channelNameToHiddenCategories;
  private MapChangeListener<String, ObservableList<ChatUserCategory>> channelNameToHiddenCategoriesListener;
  private ObservableList<ChatUserCategory> hiddenCategories;
  private ListChangeListener<ChatUserCategory> hiddenCategoriesListener;
  private ObservableMap<ChatUserCategory, Boolean> visibleCategories = FXCollections.observableHashMap();
  private MapChangeListener<ChatUserCategory, Boolean> visibleCategoriesListener;
  private MapChangeListener<String, ChatChannelUser> channelUserListListener;
  private String channelName;
  private VirtualFlow<ListItem, Cell<ListItem, Node>> listView;
  private FilteredList<ListItem> items;

  @Override
  public void afterPropertiesSet() throws Exception {
    eventBus.register(this);
  }

  public void setChatChannel(ChatChannel chatChannel) {
    this.channelName = chatChannel.getName();
    this.channelNameToHiddenCategories = preferencesService.getPreferences().getChat().getChannelNameToHiddenCategories();
    this.hiddenCategories = channelNameToHiddenCategories.get(channelName);

    prepareData();
    initializeListeners();
    updateUserCount(chatChannel.getUsers().size());
    chatChannel.getUsers().forEach(this::onUserJoined);
    initializeList();
  }

  private void prepareData() {
    Arrays.stream(ChatUserCategory.values()).forEach(category -> {
      visibleCategories.put(category, true);
      categoriesToUsers.put(category, FXCollections.synchronizedObservableList(FXCollections.observableArrayList()));
      source.add(new ChatUserCategoryItem(category, channelName));
    });
  }

  private void initializeListeners() {
    hiddenCategoriesListener = change -> {
      while (change.next()) {
        if (change.wasAdded()) {
          change.getAddedSubList().forEach(category -> visibleCategories.put(category, false));
        } else if (change.wasRemoved()) {
          change.getRemoved().forEach(category -> visibleCategories.put(category, true));
        }
      }
    };

    visibleCategoriesListener = change -> onVisibleCategoryChanged(change.getKey(), change.getValueAdded());

    channelNameToHiddenCategoriesListener = change -> {
      if (change.getKey().equals(channelName)) {
        if (change.wasAdded()) {
          change.getValueAdded().forEach(hiddenCategory -> visibleCategories.put(hiddenCategory, false));
          JavaFxUtil.addListener(change.getValueAdded(), new WeakListChangeListener<>(hiddenCategoriesListener));
        }
        if (change.wasRemoved()) {
          change.getValueRemoved().forEach(removedHiddenCategory -> visibleCategories.put(removedHiddenCategory, true));
          JavaFxUtil.removeListener(change.getValueRemoved(), hiddenCategoriesListener);
        }
        preferencesService.storeInBackground();
      }
    };

    channelUserListListener = change -> {
      if (change.wasAdded()) {
        onUserJoined(change.getValueAdded());
      } else {
        onUserLeft(change.getValueRemoved());
      }
      updateUserCount(change.getMap().size());
    };

    chatService.addUsersListener(channelName, channelUserListListener);
    JavaFxUtil.addListener(visibleCategories, new WeakMapChangeListener<>(visibleCategoriesListener));
    JavaFxUtil.addListener((ObservableMap<String, ObservableList<ChatUserCategory>>) channelNameToHiddenCategories,
        new WeakMapChangeListener<>(channelNameToHiddenCategoriesListener));
    if (hiddenCategories != null) {
      hiddenCategories.forEach(hiddenCategory -> visibleCategories.put(hiddenCategory, false));
      JavaFxUtil.addListener(hiddenCategories, new WeakListChangeListener<>(hiddenCategoriesListener));
    }
  }

  public void onTabClosed() {
    chatService.removeUsersListener(channelName, channelUserListListener);
  }

  private void initializeList() {
    taskScheduler.schedule(() -> {
      items = new FilteredList<>(new SortedList<>(source, ListItem.getComparator()));
      listView = VirtualFlow.createVertical(items, item -> item.createCell(uiService));
      VirtualizedScrollPane<VirtualFlow<ListItem, Cell<ListItem, Node>>> scrollPane = new VirtualizedScrollPane<>(listView);
      VBox.setVgrow(scrollPane, Priority.ALWAYS);
      JavaFxUtil.runLater(() -> userListContainer.getChildren().add(scrollPane));
    }, Instant.now().plus(3000, ChronoUnit.MILLIS));
  }

  private void onVisibleCategoryChanged(ChatUserCategory category, boolean visible) {
    synchronized (monitor) {
      if (visible) {
        source.addAll(categoriesToUsers.get(category));
      } else {
        // TODO: Uncomment when the bug will be fixed
        // TODO: https://bugs.openjdk.java.net/browse/JDK-8195750
        //source.removeAll(categoriesToUsers.get(category));
        categoriesToUsers.get(category).forEach(source::remove);
      }
    }
  }

  private void onUserJoined(ChatChannelUser user) {
    synchronized (monitor) {
      playerService.getPlayerByNameIfOnline(user.getUsername()).ifPresent(player -> chatUserService.associatePlayerToChatUser(user, player));
      List<ChatUserItem> chatUserItems = usernameToChatUserList.computeIfAbsent(user.getUsername(), name -> Collections.synchronizedList(new ArrayList<>()));
      if (chatUserItems.isEmpty()) {
        user.getChatUserCategories().forEach(category -> {
          ChatUserItem item = new ChatUserItem(user, category);
          categoriesToUsers.get(category).add(item);
          chatUserItems.add(item);
          if (visibleCategories.get(category)) {
            JavaFxUtil.runLater(() -> source.add(item));
          }
        });
        log.info("User {} was added", user.getUsername());
      } else {
        log.warn("Unregistered user {} for adding", user.getUsername());
      }
    }
  }

  private void onUserLeft(ChatChannelUser user) {
    synchronized (monitor) {
      List<ChatUserItem> chatUserItems = usernameToChatUserList.get(user.getUsername());
      if (chatUserItems != null && !chatUserItems.isEmpty()) {
        categoriesToUsers.values().forEach(items -> items.removeAll(chatUserItems));
        JavaFxUtil.runLater(() -> {
          source.removeAll(chatUserItems);
          chatUserItems.clear();
          usernameToChatUserList.remove(user.getUsername());
        });
        log.info("User {} was removed from list", user.getUsername());
      } else {
        log.warn("User {} was no registered for removal", user.getUsername());
      }
    }
  }

  private void onUserUpdated(ChatChannelUser user) {
      List<ChatUserItem> chatUserItems = usernameToChatUserList.get(user.getUsername());
      if (chatUserItems == null || chatUserItems.isEmpty()) {
        log.warn("No user {} in list", user.getUsername());
      } else {
        onUserLeft(user);
        onUserJoined(user);
      }
  }

  @Subscribe
  public void onChatUserCategoryChange(ChatUserCategoryChangeEvent event) {
    onUserUpdated(event.getChatUser());
  }

  private void updateUserCount(int count) {
    JavaFxUtil.runLater(() -> searchTextField.setPromptText(i18n.get("chat.userCount", count)));
  }

  private ChatChannelUser createRandomUser() {
    String username = RandomStringUtils.randomAlphabetic(10);
    ChatChannelUser user = new ChatChannelUser(username, "#test", RandomUtils.nextBoolean());
    SocialStatus status = SocialStatus.values()[RandomUtils.nextInt(0, 4)];
    user.setSocialStatus(status == SocialStatus.SELF ? null : status);
    log.info("Created user: {}", user);
    return user;
  }

  public void onSettingsButtonClicked() {
    ChatChannelUser user = createRandomUser();
    onUserJoined(user);
  }

  public void onFilterButtonClicked() {

  }

  @Override
  public VBox getRoot() {
    return root;
  }
}
