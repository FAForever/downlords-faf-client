package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.TimeService;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.MapChangeListener.Change;
import javafx.collections.ObservableMap;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Instant;
import java.util.Optional;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.theme.UiService.CHAT_CONTAINER;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChannelTabControllerTest extends AbstractPlainJavaFxTest {

  private static final String USER_NAME = "junit";
  private static final String CHANNEL_NAME = "#testChannel";

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private ChannelTabController instance;

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
  private UserFilterController userFilterController;
  @Mock
  private ChatUserItemController chatUserItemController;
  @Mock
  private ChatUserItemCategoryController chatUserItemCategoryController;
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
  private Preferences preferences;
  private Channel defaultChannel;

  @Before
  public void setUp() throws Exception {
    instance = new ChannelTabController(userService, chatService,
        preferencesService, playerService,
        audioService, timeService, i18n, imageUploadService,
        notificationService, reportingService,
        uiService, eventBus, webViewConfigurer, countryFlagService,
        platformService);

    defaultChannel = new Channel(CHANNEL_NAME);
    preferences = new Preferences();
    when(preferencesService.getPreferences()).thenReturn(this.preferences);
    when(userService.getUsername()).thenReturn(USER_NAME);
    when(uiService.loadFxml("theme/chat/user_filter.fxml")).thenReturn(userFilterController);
    when(uiService.loadFxml("theme/chat/chat_user_item.fxml")).thenReturn(chatUserItemController);
    when(uiService.loadFxml("theme/chat/chat_user_category.fxml")).thenReturn(chatUserItemCategoryController);
    when(userFilterController.getRoot()).thenReturn(new Pane());
    when(userFilterController.filterAppliedProperty()).thenReturn(new SimpleBooleanProperty(false));
    when(chatUserItemController.getRoot()).thenReturn(new Pane());
    when(uiService.getThemeFileUrl(CHAT_CONTAINER)).thenReturn(getClass().getResource("/theme/chat/chat_container.html"));

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
    instance.setChannel(defaultChannel);

    verify(chatService).addUsersListener(eq(CHANNEL_NAME), any());
  }

  @Test
  public void testSetChannelTopic() {
    Channel channel = new Channel("name");
    channel.setTopic("topic https://example.com/1");
    
    Platform.runLater(() -> instance.setChannel(channel));
    WaitForAsyncUtils.waitForFxEvents();

    verify(chatService).addUsersListener(eq("name"), any());
    assertThat(instance.topicText.getChildren().size(), is(2));
  }

  @Test
  public void testGetMessageCssClassModerator() {
    String playerName = "junit";
    ChatChannelUser chatUser = ChatChannelUserBuilder.create(playerName).defaultValues().moderator(true).get();

    when(playerService.getCurrentPlayer()).thenReturn(Optional.empty());
    when(chatService.getChatUser(playerName, defaultChannel.getName())).thenReturn(chatUser);

    Platform.runLater(() -> instance.setChannel(defaultChannel));
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(instance.getMessageCssClass(playerName), ChannelTabController.CSS_CLASS_MODERATOR);
  }

  @Test
  public void onSearchFieldCloseTest() {
    instance.onSearchFieldClose();
    assertTrue(!instance.searchField.isVisible());
    assertEquals("", instance.searchField.getText());
  }

  @Test
  public void onKeyReleasedTestEscape() {
    KeyEvent keyEvent = new KeyEvent(null, null, null, null, null, KeyCode.ESCAPE, false, false, false, false);

    assertTrue(!instance.searchField.isVisible());
    instance.onKeyReleased(keyEvent);
    assertTrue(!instance.searchField.isVisible());
    assertEquals("", instance.searchField.getText());
  }

  @Test
  public void onKeyReleasedTestCtrlF() {
    KeyEvent keyEvent = new KeyEvent(null, null, null, null, null, KeyCode.F, false, true, false, false);

    assertTrue(!instance.searchField.isVisible());
    instance.onKeyReleased(keyEvent);
    assertTrue(instance.searchField.isVisible());
    assertEquals("", instance.searchField.getText());
    instance.onKeyReleased(keyEvent);
    assertTrue(!instance.searchField.isVisible());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOnUserJoinsChannel() {
    instance.setChannel(defaultChannel);

    ArgumentCaptor<MapChangeListener<String, ChatChannelUser>> captor = ArgumentCaptor.forClass(MapChangeListener.class);
    verify(chatService).addUsersListener(anyString(), captor.capture());

    ChatChannelUser chatUser = new ChatChannelUser("junit", null, false);
    ObservableMap<String, ChatChannelUser> userMap = FXCollections.observableHashMap();
    userMap.put("junit", chatUser);

    Change<String, ChatChannelUser> change = mock(Change.class);
    when(change.wasAdded()).thenReturn(true);
    when(change.getValueAdded()).thenReturn(chatUser);
    when(change.getMap()).thenReturn(userMap);

    when(i18n.get("chat.userCount", 1)).thenReturn("1 Players");

    captor.getValue().onChanged(change);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.userSearchTextField.getPromptText(), is("1 Players"));
  }

  @Test
  public void testAtMentionTriggersNotification() {
    this.getRoot().setVisible(false);
    preferencesService.getPreferences().getNotification().notifyOnAtMentionOnlyEnabledProperty().setValue(false);
    instance.onMention(new ChatMessage("junit", Instant.now(), "junit", "hello @" + USER_NAME + "!!"));
    verify(audioService).playChatMentionSound();
  }

  @Test
  public void testAtMentionTriggersNotificationWhenFlagIsEnabled() {
    this.getRoot().setVisible(false);
    preferencesService.getPreferences().getNotification().notifyOnAtMentionOnlyEnabledProperty().setValue(true);
    instance.onMention(new ChatMessage("junit", Instant.now(), "junit", "hello @" + USER_NAME + "!!"));
    verify(audioService).playChatMentionSound();
  }

  @Test
  public void testNormalMentionTriggersNotification() {
    this.getRoot().setVisible(false);
    preferencesService.getPreferences().getNotification().notifyOnAtMentionOnlyEnabledProperty().setValue(false);
    instance.onMention(new ChatMessage("junit", Instant.now(), "junit", "hello " + USER_NAME + "!!"));
    verify(audioService).playChatMentionSound();
  }

  @Test
  public void testNormalMentionDoesNotTriggerNotificationWhenFlagIsEnabled() {
    this.getRoot().setVisible(false);
    preferencesService.getPreferences().getNotification().notifyOnAtMentionOnlyEnabledProperty().setValue(true);
    instance.onMention(new ChatMessage("junit", Instant.now(), "junit", "hello " + USER_NAME + "!!"));
    verify(audioService, never()).playChatMentionSound();
  }

  @Test
  public void getInlineStyleRandom() {
    String somePlayer = "somePlayer";
    Color color = ColorGeneratorUtil.generateRandomColor();
    ChatChannelUser chatUser = new ChatChannelUser(somePlayer, color, false);

    when(chatService.getChatUser(somePlayer, CHANNEL_NAME)).thenReturn(chatUser);
    Platform.runLater(() -> {
      instance.setChannel(defaultChannel);
      preferences.getChat().setChatColorMode(ChatColorMode.RANDOM);
      preferences.getChat().setHideFoeMessages(false);
    });
    WaitForAsyncUtils.waitForFxEvents();

    String expected = instance.createInlineStyleFromColor(color);
    String result = instance.getInlineStyle(somePlayer);
    assertEquals(expected, result);
  }

  @Test
  public void getInlineStyleCustom() {
    Color color = ColorGeneratorUtil.generateRandomColor();
    String colorStyle = instance.createInlineStyleFromColor(color);
    String username = "somePlayer";
    ChatChannelUser chatUser = new ChatChannelUser(username, color, false);

    when(chatService.getChatUser(username, CHANNEL_NAME)).thenReturn(chatUser);
    Platform.runLater(() -> {
      instance.setChannel(defaultChannel);
      preferences.getChat().setChatColorMode(ChatColorMode.CUSTOM);
      preferences.getChat().setHideFoeMessages(false);
    });
    WaitForAsyncUtils.waitForFxEvents();

    String expected = String.format("%s%s", colorStyle, "");
    String result = instance.getInlineStyle(username);
    assertEquals(expected, result);
  }

  @Test
  public void getInlineStyleRandomFoeHide() {
    String playerName = "playerName";
    ChatChannelUser chatUser = new ChatChannelUser(playerName, null, false);

    when(playerService.getPlayerForUsername(playerName)).thenReturn(Optional.of(PlayerBuilder.create(playerName).socialStatus(FOE).get()));
    when(chatService.getChatUser(playerName, CHANNEL_NAME)).thenReturn(chatUser);
    Platform.runLater(() -> {
      instance.setChannel(defaultChannel);
      preferences.getChat().setChatColorMode(ChatColorMode.RANDOM);
      preferences.getChat().setHideFoeMessages(true);
    });
    WaitForAsyncUtils.waitForFxEvents();

    String result = instance.getInlineStyle(playerName);
    assertEquals("display: none;", result);
  }

  @Test
  public void getInlineStyleRandomFoeShow() {
    String playerName = "somePlayer";
    ChatChannelUser chatUser = new ChatChannelUser(playerName, null, false);
    when(playerService.getPlayerForUsername(playerName)).thenReturn(Optional.of(PlayerBuilder.create(playerName).socialStatus(FOE).get()));

    when(chatService.getChatUser(playerName, CHANNEL_NAME)).thenReturn(chatUser);
    Platform.runLater(() -> {
      instance.setChannel(defaultChannel);
      preferences.getChat().setChatColorMode(ChatColorMode.RANDOM);
      preferences.getChat().setHideFoeMessages(false);
    });
    WaitForAsyncUtils.waitForFxEvents();

    String result = instance.getInlineStyle(playerName);
    assertEquals("", result);
  }

  @Test
  public void testUserIsRemovedFromCategoriesToUserListItems() {
    instance.setChannel(defaultChannel);

    ArgumentCaptor<MapChangeListener<String, ChatChannelUser>> captor = ArgumentCaptor.forClass(MapChangeListener.class);
    verify(chatService).addUsersListener(anyString(), captor.capture());

    ChatChannelUser chatUser = new ChatChannelUser("junit", null, false);
    ObservableMap<String, ChatChannelUser> userMap = FXCollections.observableHashMap();
    userMap.put("junit", chatUser);

    Change<String, ChatChannelUser> change = mock(Change.class);
    when(change.wasAdded()).thenReturn(true);
    when(change.getValueAdded()).thenReturn(chatUser);
    when(change.getMap()).thenReturn(userMap);

    when(i18n.get("chat.userCount", 1)).thenReturn("1 Players");

    captor.getValue().onChanged(change);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.userSearchTextField.getPromptText(), is("1 Players"));

    userMap.remove("junit");

    Change<String, ChatChannelUser> changeUserLeft = mock(Change.class);
    when(changeUserLeft.wasAdded()).thenReturn(false);
    when(changeUserLeft.wasRemoved()).thenReturn(true);
    when(changeUserLeft.getValueRemoved()).thenReturn(chatUser);
    when(changeUserLeft.getMap()).thenReturn(userMap);

    captor.getValue().onChanged(changeUserLeft);

    WaitForAsyncUtils.waitForFxEvents();

    boolean userStillListedInCategoryMap = instance.categoriesToUserListItems.entrySet().stream()
        .anyMatch(chatUserCategoryListEntry -> chatUserCategoryListEntry.getValue().stream()
            .anyMatch(categoryOrChatUserListItem -> categoryOrChatUserListItem.getUser().equals(chatUser))
        );

    assertFalse(userStillListedInCategoryMap);
  }

  @Test
  public void testChannelTopicUpdate() {
    defaultChannel.setTopic("topc1: https://faforever.com");
    Platform.runLater(() -> instance.setChannel(defaultChannel));
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(instance.topicText.getChildren().size(), 2);
    defaultChannel.setTopic("topic2: https://faforever.com topic3: https://faforever.com/example");
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(instance.topicText.getChildren().size(), 4);
  }
}
