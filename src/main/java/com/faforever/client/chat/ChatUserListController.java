package com.faforever.client.chat;

import com.faforever.client.chat.event.ChatUserCategoryChangeEvent;
import com.faforever.client.filter.ChatUserFilterController;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.PopupUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.PopupWindow.AnchorLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ChatUserListController implements Controller<VBox>, InitializingBean {

  private static final Comparator<ChatListItem> CHAT_LIST_ITEM_COMPARATOR = Comparator.comparing(ChatListItem::getCategory)
      .thenComparing(item -> item.getUser().map(user -> user.getUsername().toLowerCase(Locale.ROOT)).orElse(""));

  private final PreferencesService preferencesService;
  private final UiService uiService;
  private final I18n i18n;
  private final EventBus eventBus;

  public VBox root;
  public HBox userListTools;
  public ToggleButton filterButton;
  public TextField searchUsernameTextField;
  public Button listCustomizationButton;
  public VBox userListContainer;

  private Popup filterPopup;

  private final ObservableList<ChatListItem> unfilteredSource = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
  private final FilteredList<ChatListItem> items = new FilteredList<>(new SortedList<>(unfilteredSource, CHAT_LIST_ITEM_COMPARATOR));
  private final SetProperty<ChatUserCategory> hiddenCategories = new SimpleSetProperty<>();

  private String channelName;
  private ObservableList<ChatChannelUser> chatUsers;

  private final ListChangeListener<ChatChannelUser> channelUserListListener = change -> {
    while (change.next()) {
      if (change.wasAdded()) {
        change.getAddedSubList().forEach(this::onUserJoined);
      } else if (change.wasRemoved()) {
        change.getRemoved().forEach(this::onUserLeft);
      }
    }
  };
  private ChatUserFilterController chatUserFilterController;
  @SuppressWarnings("FieldCanBeLocal")
  private InvalidationListener predicateListener;

  @Override
  public void afterPropertiesSet() throws Exception {
    eventBus.register(this);
  }

  @Override
  public void initialize() {
    initializeFilter();
    initializeList();

    predicateListener = observable -> {
      Predicate<ChatListItem> filterControllerPredicate = chatUserFilterController.getPredicate();
      Predicate<ChatListItem> categoryPredicate = hiddenCategories.stream()
          .map(category -> (Predicate<ChatListItem>) item -> item.getUser()
              .isPresent() && item.getCategory() != category)
          .reduce(Predicate::and)
          .orElse(item -> true);
      JavaFxUtil.runLater(() -> items.setPredicate(filterControllerPredicate.and(categoryPredicate)));
    };
    WeakInvalidationListener weakInvalidationListener = new WeakInvalidationListener(predicateListener);
    JavaFxUtil.addListener(chatUserFilterController.predicateProperty(), weakInvalidationListener);
    JavaFxUtil.addAndTriggerListener(hiddenCategories, weakInvalidationListener);
  }

  public void setChatChannel(String channelName, ObservableList<ChatChannelUser> chatUsers) {
    this.chatUsers = chatUsers;
    this.channelName = channelName;
    this.hiddenCategories.bindContent(preferencesService.getPreferences()
        .getChat()
        .getChannelNameToHiddenCategories()
        .computeIfAbsent(this.channelName, p_name -> FXCollections.observableSet()));

    searchUsernameTextField.promptTextProperty()
        .bind(Bindings.size(this.chatUsers).map(size -> i18n.get("chat.userCount", size)));

    List.copyOf(this.chatUsers).forEach(this::onUserJoined);
    JavaFxUtil.addListener(this.chatUsers, new WeakListChangeListener<>(channelUserListListener));
  }

  private void initializeList() {
    VirtualFlow<ChatListItem, Cell<ChatListItem, Node>> listView = VirtualFlow.createVertical(items, item -> item.createCell(uiService));
    VirtualizedScrollPane<VirtualFlow<ChatListItem, Cell<ChatListItem, Node>>> scrollPane = new VirtualizedScrollPane<>(listView);
    scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
    VBox.setVgrow(scrollPane, Priority.ALWAYS);

    userListContainer.getChildren().add(scrollPane);
    userListTools.setDisable(false);

    Arrays.stream(ChatUserCategory.values())
        .forEach(category -> JavaFxUtil.runLater(() -> unfilteredSource.add(new ChatUserCategoryItem(category))));
  }

  private void onUserJoined(ChatChannelUser user) {
    user.getChatUserCategories()
        .stream()
        .map(category -> new ChatUserItem(user, category))
        .forEach(item -> JavaFxUtil.runLater(() -> unfilteredSource.add(item)));
  }

  private void onUserLeft(ChatChannelUser user) {
    user.getChatUserCategories()
        .stream()
        .map(category -> new ChatUserItem(user, category))
        .forEach(item -> JavaFxUtil.runLater(() -> unfilteredSource.remove(item)));
  }

  @Subscribe
  public void onChatUserCategoryChange(ChatUserCategoryChangeEvent event) {
    ChatChannelUser user = event.chatUser();
    if (chatUsers.contains(user) && user.getChannel().equals(channelName)) {
      onUserLeft(user);
      onUserJoined(user);
    }
  }

  private void initializeFilter() {
    chatUserFilterController = uiService.loadFxml("theme/filter/filter.fxml", ChatUserFilterController.class);
    chatUserFilterController.bindExternalFilter(searchUsernameTextField.textProperty(), (text, item) -> item.isCategory() || text.isEmpty() || item.getUser()
        .stream()
        .anyMatch(user -> StringUtils.containsIgnoreCase(user.getUsername(), text)));
    chatUserFilterController.completeSetting();

    filterPopup = PopupUtil.createPopup(PopupWindow.AnchorLocation.CONTENT_TOP_RIGHT, chatUserFilterController.getRoot());

    filterButton.selectedProperty().bindBidirectional(chatUserFilterController.getFilterStateProperty());
  }

  public void onListCustomizationButtonClicked() {
    UserListCustomizationController controller = uiService.loadFxml("theme/chat/user_list_customization.fxml");
    Popup popup = PopupUtil.createPopup(AnchorLocation.WINDOW_TOP_RIGHT, controller.getRoot());
    Bounds bounds = listCustomizationButton.localToScreen(listCustomizationButton.getBoundsInLocal());
    popup.show(listCustomizationButton.getScene().getWindow(), bounds.getMaxX(), bounds.getMaxY() + 5);
  }

  public void onFilterButtonClicked() {
    if (filterPopup.isShowing()) {
      filterPopup.hide();
    } else {
      Bounds screenBounds = filterButton.localToScreen(filterButton.getBoundsInLocal());
      filterPopup.show(filterButton.getScene().getWindow(), screenBounds.getMinX() - 10, screenBounds.getMinY());
    }
  }

  @Override
  public VBox getRoot() {
    return root;
  }

  @VisibleForTesting
  List<ChatChannelUser> getUserListByCategory(ChatUserCategory category) {
    return List.copyOf(unfilteredSource).stream()
        .filter(item -> item.getCategory() == category)
        .map(ChatListItem::getUser)
        .flatMap(Optional::stream)
        .collect(Collectors.toList());
  }

  @VisibleForTesting
  List<ChatChannelUser> getUserList() {
    return List.copyOf(unfilteredSource)
        .stream()
        .map(ChatListItem::getUser)
        .flatMap(Optional::stream)
        .collect(Collectors.toList());
  }
}
