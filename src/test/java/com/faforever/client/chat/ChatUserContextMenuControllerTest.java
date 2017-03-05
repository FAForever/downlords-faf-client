package com.faforever.client.chat;

import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameService;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserService;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.chat.SocialStatus.FOE;
import static com.faforever.client.chat.SocialStatus.FRIEND;
import static com.faforever.client.chat.SocialStatus.OTHER;
import static com.faforever.client.chat.SocialStatus.SELF;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatUserContextMenuControllerTest extends AbstractPlainJavaFxTest {
  public static final String TEST_USER_NAME = "junit";

  @Mock
  private UserService userService;
  @Mock
  private ChatService chatService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private UiService uiService;
  @Mock
  private PlayerService playerService;
  @Mock
  private GameService gameService;
  @Mock
  private ReplayService replayService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private EventBus eventBus;
  @Mock
  private JoinGameHelper joinGameHelper;
  @Mock
  private AvatarService avatarService;

  private ChatUserContextMenuController instance;
  private Player player;

  @Before
  public void setUp() throws Exception {
    instance = new ChatUserContextMenuController(userService, chatService, preferencesService, playerService, gameService,
        replayService, notificationService, i18n, eventBus, joinGameHelper, avatarService, uiService);

    Preferences preferences = mock(Preferences.class);
    ChatPrefs chatPrefs = mock(ChatPrefs.class);
    ObjectProperty<ChatColorMode> chatColorModeObjectProperty = new SimpleObjectProperty<>();

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getChat()).thenReturn(chatPrefs);
    when(chatPrefs.getUserToColor()).thenReturn(mock(ObservableMap.class));
    when(chatPrefs.chatColorModeProperty()).thenReturn(chatColorModeObjectProperty);
    when(avatarService.getAvailableAvatars()).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
        new AvatarBean(new URL("http://www.example.com/avatar1.png"), "Avatar Number #1"),
        new AvatarBean(new URL("http://www.example.com/avatar2.png"), "Avatar Number #2"),
        new AvatarBean(new URL("http://www.example.com/avatar3.png"), "Avatar Number #3")
    )));


    loadFxml("theme/chat/chat_user_context_menu.fxml", clazz -> instance);

    player = PlayerBuilder.create(TEST_USER_NAME).socialStatus(SELF).avatar(null).game(new Game()).get();
    instance.setPlayer(player);
  }

  @Test
  public void testOnSendPrivateMessage() {
    instance.onSendPrivateMessage();

    verify(eventBus).post(any(InitiatePrivateChatEvent.class));
  }

  @Test
  public void testOnAddFriendWithFoe() {
    player.setSocialStatus(FOE);

    instance.onAddFriend();

    verify(playerService).removeFoe(player);
    verify(playerService).addFriend(player);
  }

  @Test
  public void testOnAddFriendWithNeutral() {
    player.setSocialStatus(OTHER);

    instance.onAddFriend();

    verify(playerService, never()).removeFoe(player);
    verify(playerService).addFriend(player);
  }

  @Test
  public void testOnRemoveFriend() {
    instance.onRemoveFriend();

    verify(playerService).removeFriend(player);
  }

  @Test
  public void testOnAddFoeWithFriend() {
    player.setSocialStatus(FRIEND);

    instance.onAddFoe();

    verify(playerService).removeFriend(player);
    verify(playerService).addFoe(player);
  }

  @Test
  public void testOnAddFoeWithNeutral() {
    player.setSocialStatus(OTHER);

    instance.onAddFoe();

    verify(playerService, never()).removeFriend(player);
    verify(playerService).addFoe(player);
  }

  @Test
  public void testOnRemoveFoe() {
    instance.onRemoveFoe();

    verify(playerService).removeFoe(player);
  }

  @Test
  public void testOnWatchGame() throws Exception {
    instance.onWatchGame();

    verify(replayService).runLiveReplay(player.getGame().getId(), player.getId());
  }

  @Test
  public void testOnWatchGameThrowsIoExceptionTriggersNotification() throws Exception {
    doThrow(new RuntimeException("Error in runLiveReplay")).when(replayService).runLiveReplay(anyInt(), anyInt());

    instance.onWatchGame();

    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  @Test
  public void testOnJoinGame() {
    instance.onJoinGame();

    verify(joinGameHelper).join(any());
  }

  @Test
  public void onSelectAvatar() throws Exception {
    instance.avatarComboBox.show();

    WaitForAsyncUtils.waitForAsyncFx(100000, () -> instance.avatarComboBox.getSelectionModel().select(2));

    ArgumentCaptor<AvatarBean> captor = ArgumentCaptor.forClass(AvatarBean.class);
    verify(avatarService).changeAvatar(captor.capture());

    AvatarBean avatarBean = captor.getValue();
    assertThat(avatarBean.getUrl(), equalTo(new URL("http://www.example.com/avatar2.png")));
    assertThat(avatarBean.getDescription(), is("Avatar Number #2"));
  }
}
