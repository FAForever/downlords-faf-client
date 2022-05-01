package com.faforever.client.chat;

import com.faforever.client.builders.ChatChannelUserBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.chat.event.ChatUserCategoryChangeEvent;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import javafx.beans.binding.Bindings;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

@Slf4j
public class ChatUserListControllerTest extends UITest {

  private static final String CHANNEL_NAME = "#testChannel";

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private UiService uiService;
  @Mock
  private TaskScheduler taskScheduler;
  @Mock
  private ChatService chatService;
  @Mock
  private I18n i18n;
  @Mock
  private EventBus eventBus;
  @Mock
  private PlayerService playerService;
  @Mock
  private ChatUserService chatUserService;
  @Mock
  private ChatUserFilterController chatUserFilterController;

  private ChatChannel chatChannel;
  private ChatPrefs chatPrefs;

  private ScheduledFuture<?> listInitializationFutureMock;
  private ObjectProperty<ConnectionState> connectionState;

  @InjectMocks
  private ChatUserListController instance;

  @BeforeEach
  public void setUp() throws Exception {
    chatChannel = new ChatChannel(CHANNEL_NAME);
    connectionState = new ReadOnlyObjectWrapper<>(ConnectionState.CONNECTED);

    Preferences preferences = PreferencesBuilder.create().defaultValues().chatPrefs()
        .channelNameToHiddenCategories(FXCollections.observableHashMap()).then().get();
    chatPrefs = preferences.getChat();

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(uiService.loadFxml("theme/chat/user_filter.fxml")).thenReturn(chatUserFilterController);
    when(chatUserFilterController.getRoot()).thenReturn(new Pane());
    when(chatUserFilterController.filterAppliedProperty()).thenReturn(new SimpleBooleanProperty(false));
    when(chatService.connectionStateProperty()).thenReturn(connectionState);

    loadFxml("theme/chat/user_list.fxml", clazz -> instance);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testOnOneUserJoinedForHiddenCategory(boolean shouldInitializeList) throws Exception {
    setHiddenCategoriesToChatPrefs(ChatUserCategory.OTHER);
    prepareDataAndSetChatChannel(shouldInitializeList);

    ChatChannelUser user = generateUser(SocialStatus.OTHER);
    assertNoUsersInCategory(ChatUserCategory.OTHER);
    addUsersToChannel(user);
    assertNotContainUsersInCategory(ChatUserCategory.OTHER, user);
    assertContainUsersInSource(user);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testOnManyUsersJoinedForHiddenCategory(boolean shouldInitializeList) throws Exception {
    setHiddenCategoriesToChatPrefs(ChatUserCategory.OTHER);
    prepareDataAndSetChatChannel(shouldInitializeList);

    List<ChatChannelUser> userList = generateUsers(SocialStatus.OTHER, 1000);
    assertNoUsersInCategory(ChatUserCategory.OTHER);
    addUsersToChannel(userList);
    assertNotContainUsersInCategory(ChatUserCategory.OTHER, userList);
    assertContainUsersInSource(userList);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testOnOneUserLeftForHiddenCategory(boolean shouldInitializeList) throws Exception {
    setHiddenCategoriesToChatPrefs(ChatUserCategory.OTHER);

    ChatChannelUser user = generateUser(SocialStatus.OTHER);
    addUsersToChannel(user);

    prepareDataAndSetChatChannel(shouldInitializeList);

    assertNoUsersInCategory(ChatUserCategory.OTHER);
    assertContainUsersInSource(user);

    removeUsersFromChannel(user);

    assertNoUsersInCategory(ChatUserCategory.OTHER);
    assertNotContainUsersInSource(user);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testOnManyUsersLeftForHiddenCategory(boolean shouldInitializeList) throws Exception {
    setHiddenCategoriesToChatPrefs(ChatUserCategory.OTHER);

    List<ChatChannelUser> userList = generateUsers(SocialStatus.OTHER, 1000);
    addUsersToChannel(userList);

    prepareDataAndSetChatChannel(shouldInitializeList);

    assertNoUsersInCategory(ChatUserCategory.OTHER);
    assertContainUsersInSource(userList);

    removeUsersFromChannel(userList);

    assertNoUsersInCategory(ChatUserCategory.OTHER);
    assertNotContainUsersInSource(userList);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testOnOneUserJoinedForVisibleCategory(boolean shouldInitializeList) throws Exception {
    prepareDataAndSetChatChannel(shouldInitializeList);

    ChatChannelUser user = generateUser(SocialStatus.OTHER);
    assertNoUsersInCategory(ChatUserCategory.OTHER);
    addUsersToChannel(user);
    assertContainUsersInCategory(ChatUserCategory.OTHER, user);
    assertContainUsersInSource(user);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testOnManyUsersJoinedForVisibleCategory(boolean shouldInitializeList) throws Exception {
    prepareDataAndSetChatChannel(shouldInitializeList);

    List<ChatChannelUser> userList = generateUsers(SocialStatus.OTHER, 1000);
    assertNoUsersInCategory(ChatUserCategory.OTHER);
    addUsersToChannel(userList);
    assertContainUsersInCategory(ChatUserCategory.OTHER, userList);
    assertContainUsersInSource(userList);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testOnOneUserLeftForVisibleCategory(boolean shouldInitializeList) throws Exception {
    ChatChannelUser user = generateUser(SocialStatus.OTHER);
    addUsersToChannel(user);

    prepareDataAndSetChatChannel(shouldInitializeList);

    assertContainUsersInCategory(ChatUserCategory.OTHER, user);
    assertContainUsersInSource(user);

    removeUsersFromChannel(user);

    assertNotContainUsersInCategory(ChatUserCategory.OTHER, user);
    assertNotContainUsersInSource(user);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testOnManyUsersLeftForVisibleCategory(boolean shouldInitializeList) throws Exception {
    List<ChatChannelUser> userList = generateUsers(SocialStatus.OTHER, 1000);
    addUsersToChannel(userList);

    prepareDataAndSetChatChannel(shouldInitializeList);

    assertContainUsersInCategory(ChatUserCategory.OTHER, userList);
    assertContainUsersInSource(userList);

    removeUsersFromChannel(userList);

    assertNotContainUsersInCategory(ChatUserCategory.OTHER, userList);
    assertNotContainUsersInSource(userList);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testOnChangeUserCategory(boolean shouldInitializeList) throws Exception {
    ChatChannelUser user = generateUser(SocialStatus.OTHER);
    ChatChannelUser updatedUser = ChatChannelUserBuilder.create(user.getUsername(), CHANNEL_NAME).socialStatus(SocialStatus.FRIEND).get();
    addUsersToChannel(user);

    prepareDataAndSetChatChannel(shouldInitializeList);

    assertContainUsersInCategory(ChatUserCategory.OTHER, user);
    assertNotContainUsersInCategory(ChatUserCategory.FRIEND, updatedUser);
    assertContainUsersInSource(user);

    instance.onChatUserCategoryChange(new ChatUserCategoryChangeEvent(updatedUser));

    assertNotContainUsersInCategory(ChatUserCategory.OTHER, user);
    assertContainUsersInCategory(ChatUserCategory.FRIEND, updatedUser);
    assertContainUsersInSource(updatedUser);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testFriendlyUserIsModerator(boolean shouldInitializeList) throws Exception {
    ChatChannelUser user = generateUser(SocialStatus.FRIEND, true);
    addUsersToChannel(user);

    prepareDataAndSetChatChannel(shouldInitializeList);

    assertContainUsersInCategory(ChatUserCategory.FRIEND, user);
    assertContainUsersInCategory(ChatUserCategory.MODERATOR, user);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testEnemyUserIsModerator(boolean shouldInitializeList) throws Exception {
    ChatChannelUser user = generateUser(SocialStatus.FOE, true);
    addUsersToChannel(user);

    prepareDataAndSetChatChannel(shouldInitializeList);

    assertContainUsersInCategory(ChatUserCategory.FOE, user);
    assertContainUsersInCategory(ChatUserCategory.MODERATOR, user);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testChatOnlyUserIsModerator(boolean shouldInitializeList) throws Exception {
    ChatChannelUser user = generateUser(null, true);
    addUsersToChannel(user);

    prepareDataAndSetChatChannel(shouldInitializeList);

    assertContainUsersInCategory(ChatUserCategory.CHAT_ONLY, user);
    assertContainUsersInCategory(ChatUserCategory.MODERATOR, user);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testExistingChannelUsers(boolean shouldInitializeList) throws Exception {
    ChatChannelUser selfUser = generateUser(SocialStatus.SELF);
    ChatChannelUser moderatorUser = generateUser(SocialStatus.OTHER, true);
    ChatChannelUser friendUser = generateUser(SocialStatus.FRIEND);
    ChatChannelUser otherUser = generateUser(SocialStatus.OTHER);
    ChatChannelUser foeUser = generateUser(SocialStatus.FOE);
    ChatChannelUser chatOnlyUser = generateUser(null);

    addUsersToChannel(selfUser, moderatorUser, friendUser, otherUser, foeUser, chatOnlyUser);
    prepareDataAndSetChatChannel(shouldInitializeList);

    assertContainUsersInCategory(ChatUserCategory.SELF, selfUser);
    assertContainUsersInCategory(ChatUserCategory.MODERATOR, moderatorUser);
    assertContainUsersInCategory(ChatUserCategory.FRIEND, friendUser);
    assertContainUsersInCategory(ChatUserCategory.OTHER, moderatorUser);
    assertContainUsersInCategory(ChatUserCategory.OTHER, otherUser);
    assertContainUsersInCategory(ChatUserCategory.FOE, foeUser);
    assertContainUsersInCategory(ChatUserCategory.CHAT_ONLY, chatOnlyUser);

    assertContainUsersInSource(selfUser, moderatorUser, friendUser, otherUser, foeUser, chatOnlyUser);
  }

  @Test
  public void testHideCategory() throws Exception {
    List<ChatChannelUser> userList = generateUsers(SocialStatus.OTHER, 1000);
    addUsersToChannel(userList);
    prepareDataAndSetChatChannel(true);

    assertContainUsersInCategory(ChatUserCategory.OTHER, userList);
    assertContainUsersInSource(userList);

    hideCategory(ChatUserCategory.OTHER);

    assertNoUsersInCategory(ChatUserCategory.OTHER);
    assertContainUsersInSource(userList);
  }

  @Test
  public void testHideCategories() throws Exception {
    List<ChatChannelUser> userList1 = generateUsers(SocialStatus.OTHER, 1000);
    List<ChatChannelUser> userList2 = generateUsers(SocialStatus.FRIEND, 500);
    addUsersToChannel(userList1);
    addUsersToChannel(userList2);
    prepareDataAndSetChatChannel(true);

    assertContainUsersInCategory(ChatUserCategory.OTHER, userList1);
    assertContainUsersInCategory(ChatUserCategory.FRIEND, userList2);
    assertContainUsersInSource(userList1);
    assertContainUsersInSource(userList2);

    hideCategory(ChatUserCategory.OTHER);
    hideCategory(ChatUserCategory.FRIEND);

    assertNoUsersInCategory(ChatUserCategory.OTHER);
    assertNoUsersInCategory(ChatUserCategory.FRIEND);
    assertContainUsersInSource(userList1);
    assertContainUsersInSource(userList2);
  }

  @Test
  public void testShowCategory() throws Exception {
    setHiddenCategoriesToChatPrefs(ChatUserCategory.OTHER);

    List<ChatChannelUser> userList = generateUsers(SocialStatus.OTHER, 1000);
    addUsersToChannel(userList);
    prepareDataAndSetChatChannel(true);

    assertNoUsersInCategory(ChatUserCategory.OTHER);
    assertContainUsersInSource(userList);

    showCategory(ChatUserCategory.OTHER);

    assertContainUsersInCategory(ChatUserCategory.OTHER, userList);
    assertContainUsersInSource(userList);
  }

  @Test
  public void testShowCategories() throws Exception {
    setHiddenCategoriesToChatPrefs(ChatUserCategory.OTHER, ChatUserCategory.FRIEND);

    List<ChatChannelUser> userList1 = generateUsers(SocialStatus.OTHER, 1000);
    List<ChatChannelUser> userList2 = generateUsers(SocialStatus.FRIEND, 1000);
    addUsersToChannel(userList1);
    addUsersToChannel(userList2);
    prepareDataAndSetChatChannel(true);

    assertNoUsersInCategory(ChatUserCategory.OTHER);
    assertNoUsersInCategory(ChatUserCategory.FRIEND);
    assertContainUsersInSource(userList1);
    assertContainUsersInSource(userList2);

    showCategory(ChatUserCategory.OTHER);
    showCategory(ChatUserCategory.FRIEND);

    assertContainUsersInCategory(ChatUserCategory.OTHER, userList1);
    assertContainUsersInCategory(ChatUserCategory.FRIEND, userList2);
    assertContainUsersInSource(userList1);
    assertContainUsersInSource(userList2);
  }

  @Test
  public void testOnListCustomizationButtonClicked() {
    runOnFxThreadAndWait(() -> getRoot().getChildren().add(instance.getRoot()));

    VBox popupContent = new VBox();
    UserListCustomizationController controllerMock = mock(UserListCustomizationController.class);
    when(controllerMock.getRoot()).thenReturn(popupContent);
    when(uiService.loadFxml("theme/chat/user_list_customization.fxml")).thenReturn(controllerMock);

    runOnFxThreadAndWait(() -> instance.onListCustomizationButtonClicked());
    Window window = popupContent.getParent().getScene().getWindow();
    assertTrue(window.getClass().isAssignableFrom(Popup.class));
    assertTrue(window.isShowing());
  }

  @Test
  public void testOnClearSearchUsernameButtonClicked() throws Exception {
    prepareDataAndSetChatChannel(true);

    assertFalse(instance.clearSearchUsernameButton.isVisible());
    runOnFxThreadAndWait(() -> instance.searchUsernameTextField.setText("junit"));
    assertTrue(instance.clearSearchUsernameButton.isVisible());

    runOnFxThreadAndWait(() -> instance.onClearSearchUsernameButtonClicked());
    assertFalse(instance.clearSearchUsernameButton.isVisible());
    assertTrue(instance.searchUsernameTextField.getText().isEmpty());
  }

  @Test
  public void testOnAdvancedFiltersToggleButtonClickedAndHidingPopup() {
    runOnFxThreadAndWait(() -> getRoot().getChildren().add(instance.getRoot()));
    runOnFxThreadAndWait(() -> instance.onAdvancedFiltersToggleButtonClicked());
    Window window = chatUserFilterController.getRoot().getParent().getScene().getWindow();
    assertTrue(window.getClass().isAssignableFrom(Popup.class));
    assertTrue(window.isShowing());

    runOnFxThreadAndWait(() -> instance.onAdvancedFiltersToggleButtonClicked());
    assertFalse(window.isShowing());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testOnTabClosed(boolean shouldInitializeList) throws Exception {
    prepareDataAndSetChatChannel(shouldInitializeList);
    runOnFxThreadAndWait(() -> instance.onTabClosed());

    Thread.sleep(100);
    verify(eventBus).unregister(any());
    verify(chatService).removeUsersListener(eq(CHANNEL_NAME), any());
    assertThrows(RejectedExecutionException.class, () -> instance.waitForUsersEvent());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testCheckConnectionStateListener(boolean shouldInitializeList) throws Exception {
    when(chatService.getConnectionState()).thenReturn(ConnectionState.DISCONNECTED);

    prepareDataAndSetChatChannel(shouldInitializeList);
    connectionState.set(ConnectionState.DISCONNECTED);

    Thread.sleep(100);
    verify(eventBus).unregister(any());
    verify(chatService).removeUsersListener(eq(CHANNEL_NAME), any());
    assertThrows(RejectedExecutionException.class, () -> instance.waitForUsersEvent());
  }

  @Test
  public void testInitializeListView() throws Exception {
    addUsersToChannel(generateUsers(SocialStatus.OTHER, 1000));
    prepareDataAndSetChatChannel(true);
    waitForAllEvents();

    assertTrue(instance.userListContainer.getChildren().stream()
        .anyMatch(view -> view.getClass().isAssignableFrom(VirtualizedScrollPane.class)));
    assertFalse(instance.userListTools.isDisable());
  }

  @Test
  public void testGetAutoCompletionHelper() {
    assertNotNull(instance.getAutoCompletionHelper());
  }

  @Test
  public void testGetRoot() {
    assertEquals(instance.root, instance.getRoot());
  }

  @SuppressWarnings("unchecked")
  private void prepareDataAndSetChatChannel(boolean shouldInitializeList) throws Exception {
    afterPropertiesSet();
    when(listInitializationFutureMock.isDone()).thenReturn(shouldInitializeList);

    doAnswer(invocation -> {
      chatChannel.addUsersListeners(invocation.getArgument(1, MapChangeListener.class));
      return invocation;
    }).when(chatService).addUsersListener(eq(CHANNEL_NAME), any(MapChangeListener.class));

    Tab channelTab = new Tab(CHANNEL_NAME);
    runOnFxThreadAndWait(() -> {
      instance.setChatChannel(chatChannel, channelTab, Bindings.createBooleanBinding(() -> shouldInitializeList));
      if (shouldInitializeList) {
        TabPane tabPane = new TabPane(channelTab);
        getRoot().getChildren().add(tabPane);
        tabPane.getSelectionModel().select(channelTab);
      }
    });
  }

  private void setHiddenCategoriesToChatPrefs(ChatUserCategory... hiddenCategories) {
    chatPrefs.getChannelNameToHiddenCategories().put(CHANNEL_NAME, FXCollections.observableArrayList(hiddenCategories));
  }

  private void hideCategory(ChatUserCategory category) throws Exception {
    waitForAllEvents();
    MapProperty<String, ObservableList<ChatUserCategory>> channelNameToHiddenCategories = chatPrefs.getChannelNameToHiddenCategories();
    if (channelNameToHiddenCategories.containsKey(CHANNEL_NAME)) {
      JavaFxUtil.runLater(() -> channelNameToHiddenCategories.get(CHANNEL_NAME).add(category));
    } else {
      JavaFxUtil.runLater(() -> chatPrefs.getChannelNameToHiddenCategories()
          .put(CHANNEL_NAME, FXCollections.observableArrayList(category)));
    }
  }

  private void showCategory(ChatUserCategory category) {
    JavaFxUtil.runLater(() -> chatPrefs.getChannelNameToHiddenCategories().get(CHANNEL_NAME).remove(category));
  }

  private void addUsersToChannel(ChatChannelUser... users) {
    addUsersToChannel(Arrays.stream(users).toList());
  }

  private void addUsersToChannel(Collection<ChatChannelUser> users) {
    new Thread(() -> users.forEach(user -> chatChannel.addUser(user))).start();
  }

  private void removeUsersFromChannel(ChatChannelUser... users) {
    removeUsersFromChannel(Arrays.stream(users).toList());
  }

  private void removeUsersFromChannel(Collection<ChatChannelUser> users) {
    new Thread(() -> users.forEach(user -> chatChannel.removeUser(user.getUsername()))).start();
  }

  private void assertNotContainUsersInSource(ChatChannelUser... users) throws Exception {
    assertNotContainUsersInSource(Arrays.stream(users).toList());
  }

  private void assertNotContainUsersInSource(Collection<ChatChannelUser> users) throws Exception {
    waitForAllEvents();
    List<ChatChannelUser> userList = instance.getUserList();
    users.forEach(user -> assertFalse(userList.contains(user)));
  }

  private void assertContainUsersInSource(ChatChannelUser... users) throws Exception {
    assertContainUsersInSource(Arrays.stream(users).toList());
  }

  private void assertContainUsersInSource(Collection<ChatChannelUser> users) throws Exception {
    waitForAllEvents();
    assertTrue(instance.getUserList().containsAll(users));
  }

  private void assertNoUsersInCategory(ChatUserCategory category) throws Exception {
    waitForAllEvents();
    assertTrue(instance.getUserListByCategory(category).isEmpty());
  }

  private void assertNotContainUsersInCategory(ChatUserCategory category, ChatChannelUser... users) throws Exception {
    assertNotContainUsersInCategory(category, Arrays.stream(users).toList());
  }

  private void assertNotContainUsersInCategory(ChatUserCategory category, Collection<ChatChannelUser> users) throws Exception {
    waitForAllEvents();
    List<ChatChannelUser> userList = instance.getUserListByCategory(category);
    for (ChatChannelUser user : users) {
      assertFalse(userList.contains(user));
    }
  }

  private void assertContainUsersInCategory(ChatUserCategory category, ChatChannelUser... users) throws Exception {
    assertContainUsersInCategory(category, Arrays.stream(users).toList());
  }

  private void assertContainUsersInCategory(ChatUserCategory category, Collection<ChatChannelUser> users) throws Exception {
    waitForAllEvents();
    assertTrue(instance.getUserListByCategory(category).containsAll(users));
  }

  private void afterPropertiesSet() throws Exception {
    doAnswer(invocation -> {
      listInitializationFutureMock = mock(ScheduledFuture.class);
      return listInitializationFutureMock;
    }).when(taskScheduler).schedule(any(), any(Instant.class));
    instance.afterPropertiesSet();
  }

  private List<ChatChannelUser> generateUsers(SocialStatus status, int count) {
    ArrayList<ChatChannelUser> userList = new ArrayList<>();
    while (userList.size() != count) {
      userList.add(generateUser(status));
    }
    return userList;
  }

  private ChatChannelUser generateUser(SocialStatus socialStatus) {
    return generateUser(socialStatus, false);
  }

  private ChatChannelUser generateUser(SocialStatus socialStatus, boolean isModerator) {
    return ChatChannelUserBuilder.create(RandomStringUtils.randomAlphanumeric(15), CHANNEL_NAME)
        .moderator(isModerator).socialStatus(socialStatus).get();
  }

  private void waitForAllEvents() throws Exception {
    instance.waitForUsersEvent();
    waitForFxEvents();
  }
}