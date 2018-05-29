package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.MapChangeListener.Change;
import javafx.collections.ObservableMap;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.scheduling.TaskScheduler;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Instant;
import java.util.concurrent.ThreadPoolExecutor;

import static com.faforever.client.theme.UiService.CHAT_CONTAINER;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
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

  public static final String USER_NAME = "junit";
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
  private PlatformService platformService;
  @Mock
  private TimeService timeService;
  @Mock
  private ImageUploadService imageUploadService;
  @Mock
  private I18n i18n;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ThreadPoolExecutor threadPoolExecutor;
  @Mock
  private TaskScheduler taskScheduler;
  @Mock
  private AutoCompletionHelper autoCompletionHelper;
  @Mock
  private UiService uiService;
  @Mock
  private UserFilterController userFilterController;
  @Mock
  private ChatUserItemController chatUserItemController;
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

  @Before
  public void setUp() throws Exception {
    instance = new ChannelTabController(userService, chatService,
        preferencesService, playerService,
        audioService, timeService, i18n, imageUploadService,
        notificationService, reportingService,
        uiService, autoCompletionHelper,
        eventBus, webViewConfigurer, threadPoolExecutor, taskScheduler,
        countryFlagService
    );

    when(preferencesService.getPreferences()).thenReturn(new Preferences());
    when(userService.getUsername()).thenReturn(USER_NAME);
    when(uiService.loadFxml("theme/chat/user_filter.fxml")).thenReturn(userFilterController);
    when(uiService.loadFxml("theme/chat/chat_user_item.fxml")).thenReturn(chatUserItemController);
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
  public void testGetMessagesWebView() throws Exception {
    assertNotNull(instance.getMessagesWebView());
  }

  @Test
  public void testGetMessageTextField() throws Exception {
    assertNotNull(instance.messageTextField());
  }

  @Test
  public void testSetChannelName() throws Exception {
    Channel channel = new Channel(CHANNEL_NAME);

    instance.setChannel(channel);

    verify(chatService).addUsersListener(eq(CHANNEL_NAME), any());
  }

  @Test
  public void testGetMessageCssClassModerator() throws Exception {
    Channel channel = new Channel(CHANNEL_NAME);
    String playerName = "junit";

    Player player = new Player(playerName);
    player.moderatorForChannelsProperty().set(FXCollections.observableSet(CHANNEL_NAME));
    instance.setChannel(channel);
    when(playerService.getPlayerForUsername(playerName)).thenReturn(player);
    assertEquals(instance.getMessageCssClass(playerName), ChannelTabController.CSS_CLASS_MODERATOR);
  }

  @Test
  public void onSearchFieldCloseTest() throws Exception {
    instance.onSearchFieldClose();
    assertTrue(!instance.searchField.isVisible());
    assertEquals("", instance.searchField.getText());
  }

  @Test
  public void onKeyReleasedTestEscape() throws Exception {
    KeyEvent keyEvent = new KeyEvent(null, null, null, null, null, KeyCode.ESCAPE, false, false, false, false);

    assertTrue(!instance.searchField.isVisible());
    instance.onKeyReleased(keyEvent);
    assertTrue(!instance.searchField.isVisible());
    assertEquals("", instance.searchField.getText());
  }

  @Test
  public void onKeyReleasedTestCtrlF() throws Exception {
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
  public void testOnUserJoinsChannel() throws Exception {
    Channel channel = new Channel(CHANNEL_NAME);
    instance.setChannel(channel);

    ArgumentCaptor<MapChangeListener<String, ChatUser>> captor = ArgumentCaptor.forClass(MapChangeListener.class);
    verify(chatService).addUsersListener(anyString(), captor.capture());

    ChatUser chatUser = new ChatUser("junit", null);
    ObservableMap<String, ChatUser> userMap = FXCollections.observableHashMap();
    userMap.put("junit", chatUser);

    Change<String, ChatUser> change = mock(Change.class);
    when(change.wasAdded()).thenReturn(true);
    when(change.getValueAdded()).thenReturn(chatUser);
    when(change.getMap()).thenReturn(userMap);

    Player player = PlayerBuilder.create("Hans").defaultValues().get();
    when(playerService.createAndGetPlayerForUsername("junit")).thenReturn(player);

    when(i18n.get("chat.userCount", 1)).thenReturn("1 Players");
    when(chatUserItemController.getPlayer()).thenReturn(player);

    // Actual test execution
    captor.getValue().onChanged(change);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(player.usernameProperty().isBound(), is(true));
    assertThat(player.getUsername(), is("junit"));
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
}
