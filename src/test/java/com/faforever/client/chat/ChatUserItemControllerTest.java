package com.faforever.client.chat;

import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.fx.MouseEvents;
import com.faforever.client.game.GameBuilder;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.stage.Window;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatUserItemControllerTest extends AbstractPlainJavaFxTest {

  private ChatUserItemController instance;
  @Mock
  private AvatarService avatarService;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private ChatService chatService;
  @Mock
  private ReplayService replayService;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private JoinGameHelper joinGameHelper;
  @Mock
  private EventBus eventBus;

  @Before
  public void setUp() throws Exception {
    instance = new ChatUserItemController(preferencesService, avatarService, countryFlagService, chatService, replayService, i18n, uiService, notificationService, reportingService, joinGameHelper, eventBus);

    Preferences preferences = new Preferences();
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(chatService.getOrCreateChatUser("junit")).thenReturn(new ChatUser("junit", null));
    when(i18n.get(eq("user.status.hosting"), anyString())).thenReturn("Hosting");
    when(i18n.get(eq("user.status.waiting"), anyString())).thenReturn("Waiting");
    when(i18n.get(eq("user.status.playing"), anyString())).thenReturn("Playing");

    loadFxml("theme/chat/chat_user_item.fxml", param -> instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.chatUserItemRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testSetPlayer() throws Exception {
    instance.setPlayer(PlayerBuilder.create("junit").defaultValues().get());

    assertThat(instance.clanLabel.getText(), is("[e]"));
    assertThat(instance.countryImageView.isVisible(), is(true));
    verify(countryFlagService).loadCountryFlag("US");
  }

  @Test
  public void testGetPlayer() throws Exception {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    instance.setPlayer(player);

    assertThat(instance.getPlayer(), is(player));
  }

  @Test
  public void testOpenGameSetsStatusToWaiting() throws Exception {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    instance.setPlayer(player);

    assertThat(instance.statusLabel.getText(), is(""));

    player.setGame(GameBuilder.create().defaultValues().get());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.statusLabel.getText(), is("Waiting"));
  }

  @Test
  public void testHostedGameSetsStatusToHosting() throws Exception {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    instance.setPlayer(player);

    assertThat(instance.statusLabel.getText(), is(""));

    player.setGame(GameBuilder.create().defaultValues().host("junit").state(GameStatus.OPEN).get());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.statusLabel.getText(), is("Hosting"));
  }

  @Test
  public void testActiveGameSetsStatusToPlaying() throws Exception {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    instance.setPlayer(player);

    assertThat(instance.statusLabel.getText(), is(""));

    player.setGame(GameBuilder.create().defaultValues().host("junit").state(GameStatus.PLAYING).get());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.statusLabel.getText(), is("Playing"));
  }

  @Test
  public void testNullGameSetsStatusToNothing() throws Exception {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    instance.setPlayer(player);
    player.setGame(GameBuilder.create().defaultValues().host("junit").state(GameStatus.PLAYING).get());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.statusLabel.getText(), is("Playing"));
    player.setGame(null);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.statusLabel.getText(), is(""));
  }

  @Test
  public void testOnMouseEnterUsername() throws Exception {
    instance.setPlayer(PlayerBuilder.create("junit").defaultValues().get());
    WaitForAsyncUtils.asyncFx(() -> instance.onMouseEnterUsername());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.usernameLabel.getTooltip(), not(nullValue()));
    assertThat(instance.clanLabel.getTooltip(), not(nullValue()));
  }

  @Test
  public void testOnMouseEnterUsernameIfPlayerNull() throws Exception {
    WaitForAsyncUtils.asyncFx(() -> instance.onMouseEnterUsername());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.usernameLabel.getTooltip(), nullValue());
    assertThat(instance.clanLabel.getTooltip(), nullValue());
  }

  @Test
  public void testSingleClickDoesNotInitiatePrivateChat() throws Exception {
    instance.onUsernameClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 1));

    verify(eventBus, never()).post(CoreMatchers.any(InitiatePrivateChatEvent.class));
  }

  @Test
  public void testDoubleClickInitiatesPrivateChat() throws Exception {
    instance.setPlayer(PlayerBuilder.create("junit").defaultValues().get());
    WaitForAsyncUtils.waitForFxEvents();

    instance.onUsernameClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 2));

    ArgumentCaptor<InitiatePrivateChatEvent> captor = ArgumentCaptor.forClass(InitiatePrivateChatEvent.class);
    verify(eventBus).post(captor.capture());

    assertThat(captor.getValue().getUsername(), is("junit"));
  }

  @Test
  public void testOnContextMenuRequested() throws Exception {
    WaitForAsyncUtils.asyncFx(() -> getRoot().getChildren().setAll(instance.chatUserItemRoot));

    Player player = PlayerBuilder.create("junit").defaultValues().get();
    instance.setPlayer(player);
    WaitForAsyncUtils.waitForFxEvents();

    ChatUserContextMenuController contextMenuController = mock(ChatUserContextMenuController.class);
    ContextMenu contextMenu = mock(ContextMenu.class);
    when(contextMenuController.getContextMenu()).thenReturn(contextMenu);
    when(uiService.loadFxml("theme/chat/chat_user_context_menu.fxml")).thenReturn(contextMenuController);

    ContextMenuEvent event = mock(ContextMenuEvent.class);
    instance.onContextMenuRequested(event);

    verify(uiService).loadFxml("theme/chat/chat_user_context_menu.fxml");
    verify(contextMenuController).setPlayer(player);
    verify(contextMenu).show(any(Window.class), anyDouble(), anyDouble());
  }

  @Test
  public void testSetVisible() throws Exception {
    instance.setVisible(true);
    assertThat(instance.chatUserItemRoot.isVisible(), is(true));
    assertThat(instance.chatUserItemRoot.isManaged(), is(true));

    instance.setVisible(false);
    assertThat(instance.chatUserItemRoot.isVisible(), is(false));
    assertThat(instance.chatUserItemRoot.isManaged(), is(false));

    instance.setVisible(true);
    assertThat(instance.chatUserItemRoot.isVisible(), is(true));
    assertThat(instance.chatUserItemRoot.isManaged(), is(true));
  }
}
