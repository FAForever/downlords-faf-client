package com.faforever.client.chat;

import com.faforever.client.filter.ChatUserFilterController;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.game.GameTooltipController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.PopupUtil;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.WeakListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.PopupWindow.AnchorLocation;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualFlow.Gravity;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ChatUserListController extends NodeController<VBox> {

  private static final Comparator<ChatListItem> CHAT_LIST_ITEM_COMPARATOR = Comparator.comparing(ChatListItem::category)
                                                                                      .thenComparing(ChatListItem::user,
                                                                                                     Comparator.nullsFirst(
                                                                                                         Comparator.comparing(
                                                                                                             ChatChannelUser::getUsername)));

  private final UiService uiService;
  private final I18n i18n;
  private final ChatPrefs chatPrefs;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final ObjectFactory<ChatListItemCell> chatListItemCellFactory;

  public VBox root;
  public HBox userListTools;
  public ToggleButton filterButton;
  public TextField searchUsernameTextField;
  public Button listCustomizationButton;
  public VBox userListContainer;
  private VirtualFlow<ChatListItem, Cell<ChatListItem, Node>> chatItemListView;
  private GameTooltipController gameInfoController;
  private Tooltip gameTooltip;

  private Popup filterPopup;

  private final ObjectProperty<ChatChannel> chatChannel = new SimpleObjectProperty<>();
  private final ObservableValue<ObservableList<ChatChannelUser>> users = chatChannel.map(ChatChannel::getUsers);
  private final ObservableValue<String> channelName = chatChannel.map(ChatChannel::getName);
  private final ObservableList<ChatListItem> unfilteredSource = FXCollections.synchronizedObservableList(
      FXCollections.observableArrayList());
  private final FilteredList<ChatListItem> items = new FilteredList<>(
      new SortedList<>(unfilteredSource, CHAT_LIST_ITEM_COMPARATOR));
  private final ObjectProperty<ObservableSet<ChatUserCategory>> hiddenCategories = new SimpleObjectProperty<>(
      FXCollections.emptyObservableSet());
  private final Map<ChatChannelUser, Set<ChatListItem>> userChatListItemMap = new ConcurrentHashMap<>();
  private final ObservableValue<Predicate<ChatListItem>> hiddenCategoryPredicate = hiddenCategories.flatMap(
                                                                                                       categories -> Bindings.createObjectBinding(() -> categories.stream()
                                                                                                                                                                  .map(
                                                                                                                                                                      category -> (Predicate<ChatListItem>) item -> item.user() == null || item.category() != category)
                                                                                                                                                                  .reduce(item -> true, Predicate::and), categories))
                                                                                                   .orElse(
                                                                                                       item -> true);

  private final ListChangeListener<ChatChannelUser> channelUserListListener = this::onUserChange;
  private final WeakListChangeListener<ChatChannelUser> weakUserListChangeListener = new WeakListChangeListener<>(
      channelUserListListener);

  private ChatUserFilterController chatUserFilterController;

  @Override
  protected void onInitialize() {
    hiddenCategories.bind(
        Bindings.valueAt(chatPrefs.getChannelNameToHiddenCategories(), chatChannel.map(ChatChannel::getName))
                .orElse(FXCollections.observableSet())
                .when(showing));

    searchUsernameTextField.promptTextProperty()
                           .bind(users.flatMap(Bindings::size)
                                      .map(size -> i18n.get("chat.userCount", size))
                                      .when(showing));

    users.when(showing).subscribe((oldValue, newValue) -> {
      unfilteredSource.removeIf(item -> item.user() != null);

      if (oldValue != null) {
        oldValue.removeListener(weakUserListChangeListener);
      }

      if (newValue != null) {
        newValue.addListener(weakUserListChangeListener);
        List.copyOf(newValue).forEach(this::onUserJoined);
      }

      chatItemListView.showAsFirst(0);
    });

    initializeFilter();
    initializeList();
    initializeGameTooltip();

    for (ChatUserCategory category : ChatUserCategory.values()) {
      FilteredList<ChatListItem> categoryFilteredList = new FilteredList<>(unfilteredSource);
      categoryFilteredList.predicateProperty()
                          .bind(chatUserFilterController.predicateProperty()
                                                        .map(filterPredicate -> filterPredicate.and(
                                                            item -> item.user() != null && item.category() == category)));
      ChatListItem item = new ChatListItem(null, category, channelName, Bindings.size(categoryFilteredList).asObject());
      fxApplicationThreadExecutor.execute(() -> unfilteredSource.add(item));
    }
  }

  public void setChatChannel(ChatChannel chatChannel) {
    this.chatChannel.set(chatChannel);
  }

  public ChatChannel getChatChannel() {
    return chatChannel.get();
  }

  public ObjectProperty<ChatChannel> chatChannelProperty() {
    return chatChannel;
  }

  private ChatListItemCell createCellWithItem(ChatListItem item) {
    ChatListItemCell cell = chatListItemCellFactory.getObject();
    cell.updateItem(item);
    cell.installGameTooltip(gameInfoController, gameTooltip);
    return cell;
  }

  private void initializeGameTooltip() {
    gameInfoController = uiService.loadFxml("theme/play/game_tooltip.fxml");
    gameInfoController.setShowMods(false);

    gameTooltip = JavaFxUtil.createCustomTooltip(gameInfoController.getRoot());
    gameTooltip.setShowDelay(Duration.ZERO);
    gameTooltip.setShowDuration(Duration.seconds(30));
  }

  private void initializeList() {
    chatItemListView = VirtualFlow.createVertical(items, this::createCellWithItem, Gravity.FRONT);
    VirtualizedScrollPane<VirtualFlow<ChatListItem, Cell<ChatListItem, Node>>> scrollPane = new VirtualizedScrollPane<>(
        chatItemListView);

    scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
    VBox.setVgrow(scrollPane, Priority.ALWAYS);

    userListContainer.getChildren().add(scrollPane);
    userListTools.setDisable(false);

    items.predicateProperty()
         .bind(chatUserFilterController.predicateProperty()
                                       .flatMap(filterPredicate -> hiddenCategoryPredicate.map(filterPredicate::and)));
  }

  private void onUserJoined(ChatChannelUser user) {
    for (ChatUserCategory category : user.getCategories()) {
      ChatListItem item = new ChatListItem(user, category, null, null);
      userChatListItemMap.computeIfAbsent(user, newUser -> ConcurrentHashMap.newKeySet()).add(item);
      fxApplicationThreadExecutor.execute(() -> unfilteredSource.add(item));
    }
  }

  private void onUserLeft(ChatChannelUser user) {
    Set<ChatListItem> userItems = userChatListItemMap.remove(user);
    if (userItems != null) {
      fxApplicationThreadExecutor.execute(() -> userItems.forEach(unfilteredSource::remove));
    }
  }

  private void onUserChange(Change<? extends ChatChannelUser> change) {
    while (change.next()) {
      if (change.wasAdded()) {
        List.copyOf(change.getAddedSubList()).forEach(this::onUserJoined);
      } else if (change.wasRemoved()) {
        change.getRemoved().forEach(this::onUserLeft);
      } else if (change.wasUpdated()) {
        List<ChatChannelUser> changedUsers = List.copyOf(change.getList().subList(change.getFrom(), change.getTo()));
        changedUsers.forEach(this::onUserLeft);
        changedUsers.forEach(this::onUserJoined);
      }
    }
  }

  private void initializeFilter() {
    chatUserFilterController = uiService.loadFxml("theme/filter/filter.fxml", ChatUserFilterController.class);
    chatUserFilterController.addExternalFilter(searchUsernameTextField.textProperty().when(showing),
                                               (text, item) -> text.isEmpty() || item.user() == null || StringUtils.containsIgnoreCase(
                                                   item.user().getUsername(), text));
    chatUserFilterController.completeSetting();

    filterPopup = PopupUtil.createPopup(PopupWindow.AnchorLocation.CONTENT_TOP_RIGHT,
                                        chatUserFilterController.getRoot());

    chatUserFilterController.filterActiveProperty().when(showing).subscribe(filterButton::setSelected);
    filterButton.selectedProperty()
                .when(showing)
                .subscribe(() -> filterButton.setSelected(chatUserFilterController.getFilterActive()));
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
    return unfilteredSource.stream()
                           .filter(item -> item.category() == category)
                           .map(ChatListItem::user)
                           .filter(Objects::nonNull)
                           .collect(Collectors.toList());
  }

  @VisibleForTesting
  List<ChatChannelUser> getFilteredUserListByCategory(ChatUserCategory category) {
    return items.stream()
                .filter(item -> item.category() == category)
                .map(ChatListItem::user)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
  }

  @VisibleForTesting
  List<ChatChannelUser> getUserList() {
    return unfilteredSource.stream().map(ChatListItem::user).filter(Objects::nonNull).collect(Collectors.toList());
  }
}
