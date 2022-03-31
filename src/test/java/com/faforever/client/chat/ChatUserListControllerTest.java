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
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
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
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
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

  @InjectMocks
  private ChatUserListController instance;

  @BeforeEach
  public void setUp() throws Exception {
    chatChannel = new ChatChannel(CHANNEL_NAME);

    Preferences preferences = PreferencesBuilder.create().defaultValues().chatPrefs()
        .channelNameToHiddenCategories(FXCollections.observableHashMap()).then().get();
    chatPrefs = preferences.getChat();

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(uiService.loadFxml("theme/chat/user_filter.fxml")).thenReturn(chatUserFilterController);
    when(chatUserFilterController.getRoot()).thenReturn(new Pane());
    when(chatUserFilterController.filterAppliedProperty()).thenReturn(new SimpleBooleanProperty(false));
    when(chatService.connectionStateProperty()).thenReturn(new ReadOnlyObjectWrapper<>(ConnectionState.CONNECTED));

    loadFxml("theme/chat/user_list.fxml", clazz -> instance);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testOnOneUserJoinedForHiddenCategory(boolean isListViewInitialized) throws Exception {
    setHiddenCategoriesToChatPrefs(ChatUserCategory.OTHER);
    prepareDataAndSetChatChannel(isListViewInitialized);

    ChatChannelUser user = generateUser(SocialStatus.OTHER);
    assertNoUsersInCategory(ChatUserCategory.OTHER);
    addUsersToChannel(user);
    assertNotContainUsersInCategory(ChatUserCategory.OTHER, user);
    assertContainUsersInSource(user);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testOnManyUsersJoinedForHiddenCategory(boolean isListViewInitialized) throws Exception {
    setHiddenCategoriesToChatPrefs(ChatUserCategory.OTHER);
    prepareDataAndSetChatChannel(isListViewInitialized);

    List<ChatChannelUser> userList = generateUsers(SocialStatus.OTHER, 50);
    assertNoUsersInCategory(ChatUserCategory.OTHER);
    addUsersToChannel(userList);
    assertNotContainUsersInCategory(ChatUserCategory.OTHER, userList);
    assertContainUsersInSource(userList);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testOnOneUserLeftForHiddenCategory(boolean isListViewInitialized) throws Exception {
    setHiddenCategoriesToChatPrefs(ChatUserCategory.OTHER);

    ChatChannelUser user = generateUser(SocialStatus.OTHER);
    addUsersToChannel(user);

    prepareDataAndSetChatChannel(isListViewInitialized);

    assertNoUsersInCategory(ChatUserCategory.OTHER);
    assertContainUsersInSource(user);

    removeUsersFromChannel(user);

    assertNoUsersInCategory(ChatUserCategory.OTHER);
    assertNotContainUsersInSource(user);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testOnManyUsersLeftForHiddenCategory(boolean isListViewInitialized) throws Exception {
    setHiddenCategoriesToChatPrefs(ChatUserCategory.OTHER);

    List<ChatChannelUser> userList = generateUsers(SocialStatus.OTHER, 10);
    addUsersToChannel(userList);

    prepareDataAndSetChatChannel(isListViewInitialized);

    assertNoUsersInCategory(ChatUserCategory.OTHER);
    assertContainUsersInSource(userList);

    removeUsersFromChannel(userList);

    assertNoUsersInCategory(ChatUserCategory.OTHER);
    assertNotContainUsersInSource(userList);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testOnOneUserJoinedForVisibleCategory(boolean isListViewInitialized) throws Exception {
    prepareDataAndSetChatChannel(isListViewInitialized);

    ChatChannelUser user = generateUser(SocialStatus.OTHER);
    assertNoUsersInCategory(ChatUserCategory.OTHER);
    addUsersToChannel(user);
    assertContainUsersInCategory(ChatUserCategory.OTHER, user);
    assertContainUsersInSource(user);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testOnManyUsersJoinedForVisibleCategory(boolean isListViewInitialized) throws Exception {
    prepareDataAndSetChatChannel(isListViewInitialized);

    List<ChatChannelUser> userList = generateUsers(SocialStatus.OTHER, 50);
    assertNoUsersInCategory(ChatUserCategory.OTHER);
    addUsersToChannel(userList);
    assertContainUsersInCategory(ChatUserCategory.OTHER, userList);
    assertContainUsersInSource(userList);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testOnOneUserLeftForVisibleCategory(boolean isListViewInitialized) throws Exception {
    ChatChannelUser user = generateUser(SocialStatus.OTHER);
    addUsersToChannel(user);

    prepareDataAndSetChatChannel(isListViewInitialized);

    assertContainUsersInCategory(ChatUserCategory.OTHER, user);
    assertContainUsersInSource(user);

    removeUsersFromChannel(user);

    assertNotContainUsersInCategory(ChatUserCategory.OTHER, user);
    assertNotContainUsersInSource(user);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testOnManyUsersLeftForVisibleCategory(boolean isListViewInitialized) throws Exception {
    List<ChatChannelUser> userList = generateUsers(SocialStatus.OTHER, 50);
    addUsersToChannel(userList);

    prepareDataAndSetChatChannel(isListViewInitialized);

    assertContainUsersInCategory(ChatUserCategory.OTHER, userList);
    assertContainUsersInSource(userList);

    removeUsersFromChannel(userList);

    assertNotContainUsersInCategory(ChatUserCategory.OTHER, userList);
    assertNotContainUsersInSource(userList);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testOnChangeUserCategory(boolean isListViewInitialized) throws Exception {
    ChatChannelUser user = generateUser(SocialStatus.OTHER);
    ChatChannelUser updatedUser = ChatChannelUserBuilder.create(user.getUsername(), CHANNEL_NAME).socialStatus(SocialStatus.FRIEND).get();
    addUsersToChannel(user);

    prepareDataAndSetChatChannel(isListViewInitialized);

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
  public void testFriendlyUserIsModerator(boolean isListViewInitialized) throws Exception {
    ChatChannelUser user = generateUser(SocialStatus.FRIEND, true);
    addUsersToChannel(user);

    prepareDataAndSetChatChannel(isListViewInitialized);

    assertContainUsersInCategory(ChatUserCategory.FRIEND, user);
    assertContainUsersInCategory(ChatUserCategory.MODERATOR, user);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testEnemyUserIsModerator(boolean isListViewInitialized) throws Exception {
    ChatChannelUser user = generateUser(SocialStatus.FOE, true);
    addUsersToChannel(user);

    prepareDataAndSetChatChannel(isListViewInitialized);

    assertContainUsersInCategory(ChatUserCategory.FOE, user);
    assertContainUsersInCategory(ChatUserCategory.MODERATOR, user);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testChatOnlyUserIsModerator(boolean isListViewInitialized) throws Exception {
    ChatChannelUser user = generateUser(null, true);
    addUsersToChannel(user);

    prepareDataAndSetChatChannel(isListViewInitialized);

    assertContainUsersInCategory(ChatUserCategory.CHAT_ONLY, user);
    assertContainUsersInCategory(ChatUserCategory.MODERATOR, user);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testExistingChannelUsers(boolean isListViewInitialized) throws Exception {
    ChatChannelUser selfUser = generateUser(SocialStatus.SELF);
    ChatChannelUser moderatorUser = generateUser(SocialStatus.OTHER, true);
    ChatChannelUser friendUser = generateUser(SocialStatus.FRIEND);
    ChatChannelUser otherUser = generateUser(SocialStatus.OTHER);
    ChatChannelUser foeUser = generateUser(SocialStatus.FOE);
    ChatChannelUser chatOnlyUser = generateUser(null);

    addUsersToChannel(selfUser, moderatorUser, friendUser, otherUser, foeUser, chatOnlyUser);
    prepareDataAndSetChatChannel(isListViewInitialized);

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
    List<ChatChannelUser> userList = generateUsers(SocialStatus.OTHER, 10);
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
    List<ChatChannelUser> userList1 = generateUsers(SocialStatus.OTHER, 10);
    List<ChatChannelUser> userList2 = generateUsers(SocialStatus.FRIEND, 2);
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

    List<ChatChannelUser> userList = generateUsers(SocialStatus.OTHER, 10);
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

    List<ChatChannelUser> userList1 = generateUsers(SocialStatus.OTHER, 10);
    List<ChatChannelUser> userList2 = generateUsers(SocialStatus.FRIEND, 2);
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
  public void testGetRoot() {
    assertEquals(instance.root, instance.getRoot());
  }

  @SuppressWarnings("unchecked")
  private void prepareDataAndSetChatChannel(boolean isListViewInitialized) throws Exception {
    afterPropertiesSet();
    when(listInitializationFutureMock.isDone()).thenReturn(isListViewInitialized);

    doAnswer(invocation -> {
      chatChannel.addUsersListeners(invocation.getArgument(1, MapChangeListener.class));
      return invocation;
    }).when(chatService).addUsersListener(eq(CHANNEL_NAME), any(MapChangeListener.class));

    runOnFxThreadAndWait(() -> instance.setChatChannel(chatChannel, mock(Tab.class), mock(BooleanBinding.class)));
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
    users.forEach(user -> new Thread(() -> chatChannel.addUser(user)).start());
  }

  private void removeUsersFromChannel(ChatChannelUser... users) {
    removeUsersFromChannel(Arrays.stream(users).toList());
  }

  private void removeUsersFromChannel(Collection<ChatChannelUser> users) {
    users.forEach(user -> new Thread(() -> chatChannel.removeUser(user.getUsername())).start());
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
    while (count-- != 0) {
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