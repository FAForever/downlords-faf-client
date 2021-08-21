package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.chat.event.ChatUserCategoryChangeEvent;
import com.faforever.client.chat.event.ChatUserColorChangeEvent;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.TimeService;
import com.google.common.eventbus.EventBus;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.MapChangeListener.Change;
import javafx.collections.ObservableMap;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.FRIEND;
import static com.faforever.client.player.SocialStatus.OTHER;
import static com.faforever.client.player.SocialStatus.SELF;
import static com.faforever.client.theme.UiService.CHAT_CONTAINER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatChannelTabControllerTest extends UITest {

  private static final String USER_NAME = "junit";
  private static final String CHANNEL_NAME = "#testChannel";

  private ChannelTabController instance;
  private ChatUserFilterController chatUserFilterController;

  @Mock
  private ChatService chatService;
  @Mock
  private UserService userService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private PlayerService playerService;
  @Mock
  private TimeService timeService;
  @Mock
  private ImageUploadService imageUploadService;
  @Mock
  private I18n i18n;
  @Mock
  private NotificationService notificationService;
  @Mock
  private UiService uiService;
  @Mock
  private ChatCategoryItemController chatCategoryItemController;
  @Mock
  private WebViewConfigurer webViewConfigurer;
  @Mock
  private AudioService audioService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private EventBus eventBus;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private PlatformService platformService;
  @Mock
  private ChatUserService chatUserService;

  private Preferences preferences;
  private ChatChannel defaultChatChannel;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new ChannelTabController(userService, chatService,
        preferencesService, playerService,
        audioService, timeService, i18n, imageUploadService,
        notificationService, reportingService,
        uiService, eventBus, webViewConfigurer, countryFlagService,
        platformService, chatUserService);
    chatUserFilterController = new ChatUserFilterController(i18n, countryFlagService);

    defaultChatChannel = new ChatChannel(CHANNEL_NAME);
    preferences = PreferencesBuilder.create().defaultValues().get();
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(userService.getUsername()).thenReturn(USER_NAME);
    when(uiService.loadFxml("theme/chat/user_filter.fxml")).thenReturn(chatUserFilterController);
    when(uiService.loadFxml("theme/chat/chat_user_category.fxml")).thenReturn(chatCategoryItemController);
    when(uiService.getThemeFileUrl(CHAT_CONTAINER)).thenReturn(getClass().getResource("/theme/chat/chat_container.html"));

    loadFxml("theme/chat/user_filter.fxml", clazz -> chatUserFilterController);
    loadFxml("theme/chat/channel_tab.fxml", clazz -> instance);

    TabPane tabPane = new TabPane();
    tabPane.getTabs().add(instance.getRoot());
    WaitForAsyncUtils.waitForAsyncFx(5000, () -> getRoot().getChildren().add(tabPane));
  }


  @Test
  public void testGetMessagesWebView() {
    assertNotNull(instance.getMessagesWebView());
  }

  @Test
  public void testGetMessageTextField() {
    assertNotNull(instance.messageTextField());
  }

  @Test
  public void testSetChannelName() {
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));
    verify(chatService).addUsersListener(eq(CHANNEL_NAME), any());
  }

  @Test
  public void testSetChannelTopic() {
    ChatChannel chatChannel = new ChatChannel("name");
    chatChannel.setTopic("topic https://example.com/1");

    runOnFxThreadAndWait(() -> instance.setChatChannel(chatChannel));

    verify(chatService).addUsersListener(eq("name"), any());
    assertEquals(2, instance.topicText.getChildren().size());
  }

  @Test
  public void testGetMessageCssClassModerator() {
    String playerName = "junit";
    ChatChannelUser chatUser = ChatChannelUserBuilder.create(playerName).defaultValues().moderator(true).get();

    when(chatService.getOrCreateChatUser(playerName, defaultChatChannel.getName())).thenReturn(chatUser);

    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    assertEquals(ChannelTabController.CSS_CLASS_MODERATOR, instance.getMessageCssClass(playerName));
  }

  @Test
  public void onSearchFieldCloseTest() {
    instance.onSearchFieldClose();
    assertFalse(instance.searchField.isVisible());
    assertEquals("", instance.searchField.getText());
  }

  @Test
  public void onKeyReleasedTestEscape() {
    KeyEvent keyEvent = new KeyEvent(null, null, null, null, null, KeyCode.ESCAPE, false, false, false, false);

    assertFalse(instance.searchField.isVisible());
    runOnFxThreadAndWait(() -> instance.onKeyReleased(keyEvent));
    assertFalse(instance.searchField.isVisible());
    assertEquals("", instance.searchField.getText());
  }

  @Test
  public void onKeyReleasedTestCtrlF() {
    KeyEvent keyEvent = new KeyEvent(null, null, null, null, null, KeyCode.F, false, true, false, false);

    assertFalse(instance.searchField.isVisible());
    runOnFxThreadAndWait(() -> instance.onKeyReleased(keyEvent));
    assertTrue(instance.searchField.isVisible());
    assertEquals("", instance.searchField.getText());
    runOnFxThreadAndWait(() -> instance.onKeyReleased(keyEvent));
    assertFalse(instance.searchField.isVisible());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOnUserJoinsChannel() {
    String username1 = "player";
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().username(username1).socialStatus(OTHER).get();
    ChatChannelUser userInList = ChatChannelUserBuilder.create(username1).socialStatus(OTHER).get();
    when(playerService.getPlayerByNameIfOnline(username1)).thenReturn(Optional.of(player));

    String username2 = "player joins";
    PlayerBean newPlayer = PlayerBeanBuilder.create().defaultValues().username(username2).socialStatus(OTHER).get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create(username2).socialStatus(OTHER).get();
    ObservableMap<String, ChatChannelUser> userMap = FXCollections.observableHashMap();
    userMap.put(username2, chatUser);

    Change<String, ChatChannelUser> change = mock(Change.class);
    when(change.wasAdded()).thenReturn(true);
    when(change.getValueAdded()).thenReturn(chatUser);
    when(change.getMap()).thenReturn(userMap);
    when(playerService.getPlayerByNameIfOnline(username2)).thenReturn(Optional.of(newPlayer));
    when(i18n.get("chat.userCount", 1)).thenReturn("2 Players");

    defaultChatChannel.addUser(userInList);
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    ArgumentCaptor<MapChangeListener<String, ChatChannelUser>> captor = ArgumentCaptor.forClass(MapChangeListener.class);
    verify(chatService).addUsersListener(anyString(), captor.capture());

    assertEquals(1, instance.getChatUserItemsByCategory(ChatUserCategory.OTHER).size());

    runOnFxThreadAndWait(() -> captor.getValue().onChanged(change));

    assertEquals("2 Players", instance.userSearchTextField.getPromptText());
    assertEquals(2, instance.getChatUserItemsByCategory(ChatUserCategory.OTHER).size());
  }

  @Test
  public void testOnUserLeaveFromChannel() {
    String username1 = "player";
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().username(username1).socialStatus(OTHER).get();
    ChatChannelUser userInList = ChatChannelUserBuilder.create(username1).socialStatus(OTHER).get();
    when(playerService.getPlayerByNameIfOnline(username1)).thenReturn(Optional.of(player));

    String username2 = "leaving player";
    PlayerBean leavingPlayer = PlayerBeanBuilder.create().defaultValues().username(username2).socialStatus(OTHER).get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create(username2).socialStatus(OTHER).get();
    ObservableMap<String, ChatChannelUser> userMap = FXCollections.observableHashMap();
    userMap.put(username2, chatUser);

    Change<String, ChatChannelUser> change = mock(Change.class);
    when(change.wasRemoved()).thenReturn(true);
    when(change.getValueRemoved()).thenReturn(chatUser);
    when(change.getMap()).thenReturn(userMap);
    when(playerService.getPlayerByNameIfOnline(username2)).thenReturn(Optional.of(leavingPlayer));
    when(i18n.get("chat.userCount", 1)).thenReturn("1 Players");

    defaultChatChannel.addUsers(Arrays.asList(userInList, chatUser));
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    ArgumentCaptor<MapChangeListener<String, ChatChannelUser>> captor = ArgumentCaptor.forClass(MapChangeListener.class);
    verify(chatService).addUsersListener(anyString(), captor.capture());

    assertEquals(2, instance.getChatUserItemsByCategory(ChatUserCategory.OTHER).size());

    runOnFxThreadAndWait(() -> captor.getValue().onChanged(change));

    assertEquals("1 Players", instance.userSearchTextField.getPromptText());
    assertEquals(1, instance.getChatUserItemsByCategory(ChatUserCategory.OTHER).size());
  }

  @Test
  public void testAtMentionTriggersNotification() {
    this.getRoot().setVisible(false);
    preferences.getNotification().notifyOnAtMentionOnlyEnabledProperty().setValue(false);
    instance.onMention(new ChatMessage("junit", Instant.now(), "junit", "hello @" + USER_NAME + "!!"));
    verify(audioService).playChatMentionSound();
  }

  @Test
  public void testAtMentionTriggersNotificationWhenFlagIsEnabled() {
    this.getRoot().setVisible(false);
    preferences.getNotification().notifyOnAtMentionOnlyEnabledProperty().setValue(true);
    instance.onMention(new ChatMessage("junit", Instant.now(), "junit", "hello @" + USER_NAME + "!!"));
    verify(audioService).playChatMentionSound();
  }

  @Test
  public void testNormalMentionTriggersNotification() {
    this.getRoot().setVisible(false);
    preferences.getNotification().notifyOnAtMentionOnlyEnabledProperty().setValue(false);
    instance.onMention(new ChatMessage("junit", Instant.now(), "junit", "hello " + USER_NAME + "!!"));
    verify(audioService).playChatMentionSound();
  }

  @Test
  public void testNormalMentionDoesNotTriggerNotificationWhenFlagIsEnabled() {
    this.getRoot().setVisible(false);
    preferences.getNotification().notifyOnAtMentionOnlyEnabledProperty().setValue(true);
    instance.onMention(new ChatMessage("junit", Instant.now(), "junit", "hello " + USER_NAME + "!!"));
    verify(audioService, never()).playChatMentionSound();
  }

  @Test
  public void testNormalMentionDoesNotTriggerNotificationFromFoe() {
    this.getRoot().setVisible(false);
    preferences.getNotification().notifyOnAtMentionOnlyEnabledProperty().setValue(false);
    when(playerService.getPlayerByNameIfOnline("junit")).thenReturn(Optional.ofNullable(PlayerBeanBuilder.create().defaultValues().username("junit").socialStatus(FOE).get()));
    instance.onMention(new ChatMessage("junit", Instant.now(), "junit", "hello " + USER_NAME + "!!"));
    verify(audioService, never()).playChatMentionSound();
  }

  @Test
  public void getInlineStyleChangeToRandom() {
    String somePlayer = "somePlayer";

    ChatChannelUser chatUser = new ChatChannelUser(somePlayer, false);

    when(chatService.getOrCreateChatUser(somePlayer, CHANNEL_NAME)).thenReturn(chatUser);
    runOnFxThreadAndWait(() -> {
      instance.setChatChannel(defaultChatChannel);
      preferences.getChat().setChatColorMode(ChatColorMode.RANDOM);
      preferences.getChat().setHideFoeMessages(false);
    });

    Color color = ColorGeneratorUtil.generateRandomColor();
    chatUser.setColor(color);
    WaitForAsyncUtils.waitForFxEvents();

    String expected = instance.createInlineStyleFromColor(color);
    String result = instance.getInlineStyle(somePlayer);
    assertEquals(expected, result);
  }

  @Test
  public void getInlineStyleRandom() {
    String somePlayer = "somePlayer";
    Color color = ColorGeneratorUtil.generateRandomColor();
    ChatChannelUser chatUser = new ChatChannelUser(somePlayer, false);
    chatUser.setColor(color);

    when(chatService.getOrCreateChatUser(somePlayer, CHANNEL_NAME)).thenReturn(chatUser);
    runOnFxThreadAndWait(() -> {
      instance.setChatChannel(defaultChatChannel);
      preferences.getChat().setChatColorMode(ChatColorMode.RANDOM);
      preferences.getChat().setHideFoeMessages(false);
    });

    String expected = instance.createInlineStyleFromColor(color);
    String result = instance.getInlineStyle(somePlayer);
    assertEquals(expected, result);
  }

  @Test
  public void getInlineStyleRandomFoeHide() {
    String playerName = "playerName";
    ChatChannelUser chatUser = new ChatChannelUser(playerName, false);

    when(playerService.getPlayerByNameIfOnline(playerName)).thenReturn(Optional.of(PlayerBeanBuilder.create().defaultValues().username(playerName).socialStatus(FOE).get()));
    when(chatService.getOrCreateChatUser(playerName, CHANNEL_NAME)).thenReturn(chatUser);
    runOnFxThreadAndWait(() -> {
      instance.setChatChannel(defaultChatChannel);
      preferences.getChat().setChatColorMode(ChatColorMode.RANDOM);
      preferences.getChat().setHideFoeMessages(true);
    });

    String result = instance.getInlineStyle(playerName);
    assertEquals("display: none;", result);
  }

  @Test
  public void getInlineStyleRandomFoeShow() {
    String playerName = "somePlayer";
    ChatChannelUser chatUser = new ChatChannelUser(playerName, false);
    when(playerService.getPlayerByNameIfOnline(playerName)).thenReturn(Optional.of(PlayerBeanBuilder.create().defaultValues().username(playerName).socialStatus(FOE).get()));

    when(chatService.getOrCreateChatUser(playerName, CHANNEL_NAME)).thenReturn(chatUser);
    runOnFxThreadAndWait(() -> {
      instance.setChatChannel(defaultChatChannel);
      preferences.getChat().setChatColorMode(ChatColorMode.RANDOM);
      preferences.getChat().setHideFoeMessages(false);
    });

    String result = instance.getInlineStyle(playerName);
    assertEquals("", result);
  }

  @Test
  public void userColorChangeTest() {
    ChatChannelUser chatUser = new ChatChannelUser("test", false);
    chatUser.setColor(Color.AQUA);

    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    instance.onChatUserColorChange(new ChatUserColorChangeEvent(chatUser));

    assertEquals(Optional.of(Color.AQUA), chatUser.getColor());
  }

  @Test
  public void testUserIsRemovedFromCategoriesToUserListItems() {
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    ArgumentCaptor<MapChangeListener<String, ChatChannelUser>> captor = ArgumentCaptor.forClass(MapChangeListener.class);
    verify(chatService).addUsersListener(anyString(), captor.capture());

    ChatChannelUser chatUser = new ChatChannelUser("junit", false);
    ObservableMap<String, ChatChannelUser> userMap = FXCollections.observableHashMap();
    userMap.put("junit", chatUser);

    Change<String, ChatChannelUser> change = mock(Change.class);
    when(change.wasAdded()).thenReturn(true);
    when(change.getValueAdded()).thenReturn(chatUser);
    when(change.getMap()).thenReturn(userMap);

    when(i18n.get("chat.userCount", 1)).thenReturn("1 Players");

    runOnFxThreadAndWait(() -> captor.getValue().onChanged(change));

    assertEquals("1 Players", instance.userSearchTextField.getPromptText());

    userMap.remove("junit");

    Change<String, ChatChannelUser> changeUserLeft = mock(Change.class);
    when(changeUserLeft.wasAdded()).thenReturn(false);
    when(changeUserLeft.wasRemoved()).thenReturn(true);
    when(changeUserLeft.getValueRemoved()).thenReturn(chatUser);
    when(changeUserLeft.getMap()).thenReturn(userMap);

    runOnFxThreadAndWait(() -> captor.getValue().onChanged(changeUserLeft));

    boolean userStillListedInCategoryMap = instance.categoriesToUserListItems.entrySet().stream()
        .anyMatch(chatUserCategoryListEntry -> chatUserCategoryListEntry.getValue().stream()
            .anyMatch(categoryOrChatUserListItem -> categoryOrChatUserListItem.getUser().equals(chatUser))
        );

    assertFalse(userStillListedInCategoryMap);
  }

  @Test
  public void testChannelTopicUpdate() {
    defaultChatChannel.setTopic("topc1: https://faforever.com");
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    assertEquals(2, instance.topicText.getChildren().size());
    runOnFxThreadAndWait(() -> defaultChatChannel.setTopic("topic2: https://faforever.com topic3: https://faforever.com/example"));
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(4, instance.topicText.getChildren().size());
  }

  @Test
  public void testChannelListHide() {
    runOnFxThreadAndWait(() -> instance.toggleSidePaneButton.fire());

    assertFalse(instance.channelTabScrollPaneVBox.isManaged());
    assertFalse(instance.toggleSidePaneButton.isSelected());
  }

  @Test
  public void testHideSidePane() {
    runOnFxThreadAndWait(() -> instance.toggleSidePaneButton.fire());

    assertFalse(preferences.getChat().isPlayerListShown());
    verify(preferencesService, atLeast(1)).storeInBackground();

    assertFalse(instance.channelTabScrollPaneVBox.isManaged());
    assertFalse(instance.channelTabScrollPaneVBox.isVisible());
  }

  @Test
  public void testModeratorIsInModeratorsList() {
    String username = "moderator";
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().username(username).socialStatus(OTHER).get();
    ChatChannelUser moderator = ChatChannelUserBuilder.create(username).moderator(true).get();
    defaultChatChannel.addUser(moderator);

    when(playerService.getPlayerByNameIfOnline(username)).thenReturn(Optional.of(player));
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    List<CategoryOrChatUserListItem> users = instance.getChatUserItemsByCategory(ChatUserCategory.MODERATOR);
    assertTrue(users.stream().anyMatch(userItem -> userItem.getUser().equals(moderator)));
    assertEquals(1, users.size());
  }

  @Test
  public void testFriendlyPlayerIsInFriendList() {
    String username = "friend";
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().username(username).socialStatus(FRIEND).get();
    ChatChannelUser friendUser = ChatChannelUserBuilder.create(username).socialStatus(FRIEND).get();
    defaultChatChannel.addUser(friendUser);

    when(playerService.getPlayerByNameIfOnline(username)).thenReturn(Optional.of(player));
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    List<CategoryOrChatUserListItem> users = instance.getChatUserItemsByCategory(ChatUserCategory.FRIEND);
    assertTrue(users.stream().anyMatch(userItem -> userItem.getUser().equals(friendUser)));
    assertEquals(1, users.size());
  }

  @Test
  public void testEnemyPlayerIsInFoeList() {
    String username = "foe";
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().username(username).socialStatus(FOE).get();
    ChatChannelUser enemyUser = ChatChannelUserBuilder.create(username).socialStatus(FOE).get();
    defaultChatChannel.addUser(enemyUser);

    when(playerService.getPlayerByNameIfOnline(username)).thenReturn(Optional.of(player));
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    List<CategoryOrChatUserListItem> users = instance.getChatUserItemsByCategory(ChatUserCategory.FOE);
    assertTrue(users.stream().anyMatch(userItem -> userItem.getUser().equals(enemyUser)));
    assertEquals(1, users.size());
  }

  @Test
  public void testOtherPlayersIsInOtherList() {
    String username1 = "player1";
    PlayerBean player1 = PlayerBeanBuilder.create().defaultValues().username(username1).socialStatus(OTHER).get();
    ChatChannelUser user1 = ChatChannelUserBuilder.create(username1).socialStatus(OTHER).get();
    String username2 = "player2";
    PlayerBean player2 = PlayerBeanBuilder.create().defaultValues().username(username2).socialStatus(SELF).get();
    ChatChannelUser user2 = ChatChannelUserBuilder.create(username2).socialStatus(SELF).get();
    defaultChatChannel.addUsers(Arrays.asList(user1, user2));

    when(playerService.getPlayerByNameIfOnline(username1)).thenReturn(Optional.of(player1));
    when(playerService.getPlayerByNameIfOnline(username2)).thenReturn(Optional.of(player2));
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    List<CategoryOrChatUserListItem> users = instance.getChatUserItemsByCategory(ChatUserCategory.OTHER);
    assertTrue(users.stream().anyMatch(userItem -> userItem.getUser().equals(user1)));
    assertTrue(users.stream().anyMatch(userItem -> userItem.getUser().equals(user2)));
    assertEquals(2, users.size());
  }

  @Test
  public void testPlayerIsInOnlyChatList() {
    ChatChannelUser user = ChatChannelUserBuilder.create("player").player(null).get();
    defaultChatChannel.addUser(user);

    when(playerService.getPlayerByNameIfOnline(user.getUsername())).thenReturn(Optional.empty());
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    List<CategoryOrChatUserListItem> users = instance.getChatUserItemsByCategory(ChatUserCategory.CHAT_ONLY);
    assertTrue(users.stream().anyMatch(userItem -> userItem.getUser().equals(user)));
    assertEquals(1, users.size());
  }

  @Test
  public void testPlayerBecomesFriendly() {
    String username1 = "player1";
    PlayerBean player1 = PlayerBeanBuilder.create().defaultValues().username(username1).socialStatus(OTHER).get();
    ChatChannelUser user1 = ChatChannelUserBuilder.create(username1).socialStatus(OTHER).get();
    String username2 = "player2";
    PlayerBean player2 = PlayerBeanBuilder.create().defaultValues().username(username2).socialStatus(OTHER).get();
    ChatChannelUser user2 = ChatChannelUserBuilder.create(username2).socialStatus(OTHER).get();
    defaultChatChannel.addUsers(Arrays.asList(user1, user2));

    when(playerService.getPlayerByNameIfOnline(username1)).thenReturn(Optional.of(player1));
    when(playerService.getPlayerByNameIfOnline(username2)).thenReturn(Optional.of(player2));
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    user1.setSocialStatus(FRIEND);
    runOnFxThreadAndWait(() -> instance.onChatUserCategoryChange(new ChatUserCategoryChangeEvent(user1)));

    List<CategoryOrChatUserListItem> friends = instance.getChatUserItemsByCategory(ChatUserCategory.FRIEND);
    List<CategoryOrChatUserListItem> otherUsers = instance.getChatUserItemsByCategory(ChatUserCategory.OTHER);
    assertTrue(friends.stream().anyMatch(userItem -> userItem.getUser().equals(user1)));
    assertTrue(otherUsers.stream().anyMatch(userItem -> userItem.getUser().equals(user2)));
    assertEquals(1, friends.size());
    assertEquals(1, otherUsers.size());
  }

  @Test
  public void testPlayerBecomesEnemy() {
    String username1 = "player1";
    PlayerBean player1 = PlayerBeanBuilder.create().defaultValues().username(username1).socialStatus(OTHER).get();
    ChatChannelUser user1 = ChatChannelUserBuilder.create(username1).socialStatus(OTHER).get();
    String username2 = "player2";
    PlayerBean player2 = PlayerBeanBuilder.create().defaultValues().username(username2).socialStatus(OTHER).get();
    ChatChannelUser user2 = ChatChannelUserBuilder.create(username2).socialStatus(OTHER).get();
    defaultChatChannel.addUsers(Arrays.asList(user1, user2));

    when(playerService.getPlayerByNameIfOnline(username1)).thenReturn(Optional.of(player1));
    when(playerService.getPlayerByNameIfOnline(username2)).thenReturn(Optional.of(player2));
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    user1.setSocialStatus(FOE);
    runOnFxThreadAndWait(() -> instance.onChatUserCategoryChange(new ChatUserCategoryChangeEvent(user1)));

    List<CategoryOrChatUserListItem> enemies = instance.getChatUserItemsByCategory(ChatUserCategory.FOE);
    List<CategoryOrChatUserListItem> otherUsers = instance.getChatUserItemsByCategory(ChatUserCategory.OTHER);
    assertTrue(enemies.stream().anyMatch(userItem -> userItem.getUser().equals(user1)));
    assertTrue(otherUsers.stream().anyMatch(userItem -> userItem.getUser().equals(user2)));
    assertEquals(1, enemies.size());
    assertEquals(1, otherUsers.size());
  }

  @Test
  public void testFriendlyPlayerBecomesOther() {
    String username1 = "player1";
    PlayerBean player1 = PlayerBeanBuilder.create().defaultValues().username(username1).socialStatus(FRIEND).get();
    ChatChannelUser user1 = ChatChannelUserBuilder.create(username1).socialStatus(FRIEND).get();
    String username2 = "player2";
    PlayerBean player2 = PlayerBeanBuilder.create().defaultValues().username(username2).socialStatus(OTHER).get();
    ChatChannelUser user2 = ChatChannelUserBuilder.create(username2).socialStatus(OTHER).get();
    defaultChatChannel.addUsers(Arrays.asList(user1, user2));

    when(playerService.getPlayerByNameIfOnline(username1)).thenReturn(Optional.of(player1));
    when(playerService.getPlayerByNameIfOnline(username2)).thenReturn(Optional.of(player2));
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    user1.setSocialStatus(OTHER);
    runOnFxThreadAndWait(() -> instance.onChatUserCategoryChange(new ChatUserCategoryChangeEvent(user1)));

    List<CategoryOrChatUserListItem> friends = instance.getChatUserItemsByCategory(ChatUserCategory.FRIEND);
    List<CategoryOrChatUserListItem> otherUsers = instance.getChatUserItemsByCategory(ChatUserCategory.OTHER);
    assertTrue(otherUsers.stream().anyMatch(userItem -> userItem.getUser().equals(user1)));
    assertEquals(0, friends.size());
    assertEquals(2, otherUsers.size());
  }

  @Test
  public void testEnemyPlayerBecomesOther() {
    String username1 = "player1";
    PlayerBean player1 = PlayerBeanBuilder.create().defaultValues().username(username1).socialStatus(FOE).get();
    ChatChannelUser user1 = ChatChannelUserBuilder.create(username1).socialStatus(FOE).get();
    String username2 = "player2";
    PlayerBean player2 = PlayerBeanBuilder.create().defaultValues().username(username2).socialStatus(OTHER).get();
    ChatChannelUser user2 = ChatChannelUserBuilder.create(username2).socialStatus(OTHER).get();
    defaultChatChannel.addUsers(Arrays.asList(user1, user2));

    when(playerService.getPlayerByNameIfOnline(username1)).thenReturn(Optional.of(player1));
    when(playerService.getPlayerByNameIfOnline(username2)).thenReturn(Optional.of(player2));
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    user1.setSocialStatus(OTHER);
    runOnFxThreadAndWait(() -> instance.onChatUserCategoryChange(new ChatUserCategoryChangeEvent(user1)));

    List<CategoryOrChatUserListItem> enemies = instance.getChatUserItemsByCategory(ChatUserCategory.FOE);
    List<CategoryOrChatUserListItem> otherUsers = instance.getChatUserItemsByCategory(ChatUserCategory.OTHER);
    assertTrue(otherUsers.stream().anyMatch(userItem -> userItem.getUser().equals(user1)));
    assertEquals(0, enemies.size());
    assertEquals(2, otherUsers.size());
  }

  @Test
  public void testWhenUserAddPlayerToFriendlyListByAccidentallyAndCancelsActionImmediately() {
    String username1 = "player1";
    PlayerBean player1 = PlayerBeanBuilder.create().defaultValues().username(username1).socialStatus(OTHER).get();
    ChatChannelUser user1 = ChatChannelUserBuilder.create(username1).socialStatus(OTHER).get();
    String username2 = "player2";
    PlayerBean player2 = PlayerBeanBuilder.create().defaultValues().username(username2).socialStatus(OTHER).get();
    ChatChannelUser user2 = ChatChannelUserBuilder.create(username2).socialStatus(OTHER).get();
    defaultChatChannel.addUsers(Arrays.asList(user1, user2));

    when(playerService.getPlayerByNameIfOnline(username1)).thenReturn(Optional.of(player1));
    when(playerService.getPlayerByNameIfOnline(username2)).thenReturn(Optional.of(player2));
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    runOnFxThreadAndWait(() -> user1.setSocialStatus(FRIEND));
    runOnFxThreadAndWait(() -> user1.setSocialStatus(OTHER));

    List<CategoryOrChatUserListItem> friends = instance.getChatUserItemsByCategory(ChatUserCategory.FRIEND);
    List<CategoryOrChatUserListItem> otherUsers = instance.getChatUserItemsByCategory(ChatUserCategory.OTHER);
    assertTrue(otherUsers.stream().anyMatch(userItem -> userItem.getUser().equals(user1)));
    assertEquals(0, friends.size());
    assertEquals(2, otherUsers.size());
  }

  @Test
  public void testWhenUserAddPlayerToFoeListByAccidentallyAndCancelsActionImmediately() {
    String username1 = "player1";
    PlayerBean player1 = PlayerBeanBuilder.create().defaultValues().username(username1).socialStatus(OTHER).get();
    ChatChannelUser user1 = ChatChannelUserBuilder.create(username1).socialStatus(OTHER).get();
    String username2 = "player2";
    PlayerBean player2 = PlayerBeanBuilder.create().defaultValues().username(username2).socialStatus(OTHER).get();
    ChatChannelUser user2 = ChatChannelUserBuilder.create(username2).socialStatus(OTHER).get();
    defaultChatChannel.addUsers(Arrays.asList(user1, user2));

    when(playerService.getPlayerByNameIfOnline(username1)).thenReturn(Optional.of(player1));
    when(playerService.getPlayerByNameIfOnline(username2)).thenReturn(Optional.of(player2));
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    runOnFxThreadAndWait(() -> user1.setSocialStatus(FOE));
    runOnFxThreadAndWait(() -> user1.setSocialStatus(OTHER));

    List<CategoryOrChatUserListItem> enemies = instance.getChatUserItemsByCategory(ChatUserCategory.FOE);
    List<CategoryOrChatUserListItem> otherUsers = instance.getChatUserItemsByCategory(ChatUserCategory.OTHER);
    assertTrue(otherUsers.stream().anyMatch(userItem -> userItem.getUser().equals(user1)));
    assertEquals(0, enemies.size());
    assertEquals(2, otherUsers.size());
  }

  @Test
  public void testFindModeratorByName() {
    defaultChatChannel.addUsers(createUserList(Map.of(
        ChatUserCategory.MODERATOR, Arrays.asList("1Moderator", "12Moderator", "123Moderator")
    )));

    runOnFxThreadAndWait(() -> instance.initialize());
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    runOnFxThreadAndWait(() -> instance.userSearchTextField.setText("12"));
    assertTrue(instance.checkUsersAreInList(ChatUserCategory.MODERATOR, "12Moderator", "123Moderator"));

    runOnFxThreadAndWait(() -> instance.userSearchTextField.setText("123"));
    assertTrue(instance.checkUsersAreInList(ChatUserCategory.MODERATOR, "123Moderator"));

    runOnFxThreadAndWait(() -> instance.userSearchTextField.setText(""));
    assertTrue(instance.checkUsersAreInList(ChatUserCategory.MODERATOR, "1Moderator", "12Moderator", "123Moderator"));
  }

  @Test
  public void testFindOtherUserByName() {
    defaultChatChannel.addUsers(createUserList(Map.of(
        ChatUserCategory.OTHER, Arrays.asList("MarcSpector", "Lenkin")
    )));

    runOnFxThreadAndWait(() -> instance.initialize());
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    runOnFxThreadAndWait(() -> instance.userSearchTextField.setText("Marc"));
    assertTrue(instance.checkUsersAreInList(ChatUserCategory.OTHER, "MarcSpector"));

    runOnFxThreadAndWait(() -> instance.userSearchTextField.setText("Lenkin"));
    assertTrue(instance.checkUsersAreInList(ChatUserCategory.OTHER, "Lenkin"));

    runOnFxThreadAndWait(() -> instance.userSearchTextField.setText(""));
    assertTrue(instance.checkUsersAreInList(ChatUserCategory.OTHER, "MarcSpector", "Lenkin"));
  }

  @Test
  public void testFindEnemyUserByName() {
    defaultChatChannel.addUsers(createUserList(Map.of(
        ChatUserCategory.FOE, Arrays.asList("EnemyPlayer", "RandomFoePlayer")
    )));

    runOnFxThreadAndWait(() -> instance.initialize());
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    runOnFxThreadAndWait(() -> instance.userSearchTextField.setText("Random"));
    assertTrue(instance.checkUsersAreInList(ChatUserCategory.FOE, "RandomFoePlayer"));

    runOnFxThreadAndWait(() -> instance.userSearchTextField.setText(""));
    assertTrue(instance.checkUsersAreInList(ChatUserCategory.FOE, "EnemyPlayer", "RandomFoePlayer"));
  }

  @Test
  public void testFindChatOnlyUserByName() {
    defaultChatChannel.addUsers(createUserList(Map.of(
        ChatUserCategory.CHAT_ONLY, Arrays.asList("ChatOnlyPlayer", "ExamplePlayer")
    )));

    runOnFxThreadAndWait(() -> instance.initialize());
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    runOnFxThreadAndWait(() -> instance.userSearchTextField.setText("Only"));
    assertTrue(instance.checkUsersAreInList(ChatUserCategory.CHAT_ONLY, "ChatOnlyPlayer"));

    runOnFxThreadAndWait(() -> instance.userSearchTextField.setText(""));
    assertTrue(instance.checkUsersAreInList(ChatUserCategory.CHAT_ONLY, "ChatOnlyPlayer", "ExamplePlayer"));
  }

  @Test
  public void testFindSimilarNamesOfUsersInList() {
    defaultChatChannel.addUsers(createUserList(Map.of(
        ChatUserCategory.MODERATOR, Arrays.asList("1_NAme_1", "Moderator"),
        ChatUserCategory.CHAT_ONLY, Arrays.asList("ChatOnlyNameOlolo", "Bingo"),
        ChatUserCategory.OTHER, Arrays.asList("Player", "PlayerNamEEE"),
        ChatUserCategory.FRIEND, Arrays.asList("Friend", "NaMeFriend"),
        ChatUserCategory.FOE, Arrays.asList("FOE", "For______NAME")
    )));

    runOnFxThreadAndWait(() -> instance.initialize());
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));

    runOnFxThreadAndWait(() -> instance.userSearchTextField.setText("name"));
    assertTrue(instance.checkUsersAreInList(ChatUserCategory.MODERATOR, "1_NAme_1"));
    assertTrue(instance.checkUsersAreInList(ChatUserCategory.CHAT_ONLY, "ChatOnlyNameOlolo"));
    assertTrue(instance.checkUsersAreInList(ChatUserCategory.OTHER, "PlayerNamEEE"));
    assertTrue(instance.checkUsersAreInList(ChatUserCategory.FRIEND, "NaMeFriend"));
    assertTrue(instance.checkUsersAreInList(ChatUserCategory.FOE, "For______NAME"));
  }

  private List<ChatChannelUser> createUserList(Map<ChatUserCategory, List<String>> map) {
    List<ChatChannelUser> list = new ArrayList<>();
    map.forEach((category, usernames) -> list.addAll(prepareUserList(category, usernames)));
    return list;
  }

  private List<ChatChannelUser> prepareUserList(ChatUserCategory category, List<String> usernames) {
    return usernames.stream().map(name -> {
      ChatChannelUserBuilder builder = ChatChannelUserBuilder.create(name);
      PlayerBeanBuilder playerBeanBuilder = PlayerBeanBuilder.create().defaultValues().username(name);
      switch (category) {
        case OTHER -> builder.player(playerBeanBuilder.socialStatus(OTHER).get());
        case FRIEND -> builder.player(playerBeanBuilder.socialStatus(FRIEND).get());
        case FOE -> builder.player(playerBeanBuilder.socialStatus(FOE).get());
        case MODERATOR -> builder.player(playerBeanBuilder.socialStatus(OTHER).get()).moderator(true);
        case CHAT_ONLY -> builder.player(null);
      }
      return builder.get();
    }).peek(user -> {
      if (user.getPlayer().isPresent()) {
        when(playerService.getPlayerByNameIfOnline(user.getUsername())).thenReturn(user.getPlayer());
      }
    }).collect(Collectors.toList());
  }
}
