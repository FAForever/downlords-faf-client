package com.faforever.client.chat;

import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.GameService;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerInfoBeanBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.user.UserService;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
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
  UserService userService;
  @Mock
  ChatService chatService;
  @Mock
  PreferencesService preferencesService;
  @Mock
  ApplicationContext applicationContext;
  @Mock
  PlayerService playerService;
  @Mock
  GameService gameService;
  @Mock
  ReplayService replayService;
  @Mock
  NotificationService notificationService;
  @Mock
  I18n i18n;
  @Mock
  EventBus eventBus;
  @Mock
  JoinGameHelper joinGameHelper;
  @Mock
  AvatarService avatarService;

  private ChatUserContextMenuController instance;
  private PlayerInfoBean playerInfoBean;

  @Before
  public void setUp() throws Exception {
    instance = loadController("chat_user_context_menu.fxml");

    instance.userService = userService;
    instance.chatService = chatService;
    instance.preferencesService = preferencesService;
    instance.applicationContext = applicationContext;
    instance.playerService = playerService;
    instance.gameService = gameService;
    instance.replayService = replayService;
    instance.notificationService = notificationService;
    instance.i18n = i18n;
    instance.eventBus = eventBus;
    instance.joinGameHelper = joinGameHelper;
    instance.avatarService = avatarService;

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

    playerInfoBean = PlayerInfoBeanBuilder.create(TEST_USER_NAME).socialStatus(SELF).avatar(null).get();
    instance.setPlayerInfoBean(playerInfoBean);
  }

  @Test
  public void testOnSendPrivateMessage() {
    instance.onSendPrivateMessage();

    verify(eventBus).post(any(InitiatePrivateChatEvent.class));
  }

  @Test
  public void testOnAddFriendWithFoe() {
    playerInfoBean.setSocialStatus(FOE);

    instance.onAddFriend();

    verify(playerService).removeFoe(playerInfoBean);
    verify(playerService).addFriend(playerInfoBean);
  }

  @Test
  public void testOnAddFriendWithNeutral() {
    playerInfoBean.setSocialStatus(OTHER);

    instance.onAddFriend();

    verify(playerService, never()).removeFoe(playerInfoBean);
    verify(playerService).addFriend(playerInfoBean);
  }

  @Test
  public void testOnRemoveFriend() {
    instance.onRemoveFriend();

    verify(playerService).removeFriend(playerInfoBean);
  }

  @Test
  public void testOnAddFoeWithFriend() {
    playerInfoBean.setSocialStatus(FRIEND);

    instance.onAddFoe();

    verify(playerService).removeFriend(playerInfoBean);
    verify(playerService).addFoe(playerInfoBean);
  }

  @Test
  public void testOnAddFoeWithNeutral() {
    playerInfoBean.setSocialStatus(OTHER);

    instance.onAddFoe();

    verify(playerService, never()).removeFriend(playerInfoBean);
    verify(playerService).addFoe(playerInfoBean);
  }

  @Test
  public void testOnRemoveFoe() {
    instance.onRemoveFoe();

    verify(playerService).removeFoe(playerInfoBean);
  }

  @Test
  public void testOnWatchGame() throws Exception {
    instance.onWatchGame();

    verify(replayService).runLiveReplay(playerInfoBean.getGameUid(), playerInfoBean.getId());
  }

  @Test
  public void testOnWatchGameThrowsIoExceptionTriggersNotification() throws Exception {
    doThrow(new IOException("Error in runLiveReplay"))
        .when(replayService).runLiveReplay(anyInt(), anyInt());

    instance.onWatchGame();

    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  @Test
  public void testOnJoinGame() {
    GameInfoBean gameInfoBean = mock(GameInfoBean.class);
    when(gameService.getByUid(anyInt())).thenReturn(gameInfoBean);

    instance.onJoinGame();

    verify(joinGameHelper).join(gameInfoBean);
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
