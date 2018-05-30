package com.faforever.client.chat;

import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.clan.Clan;
import com.faforever.client.clan.ClanService;
import com.faforever.client.fx.MouseEvents;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameBuilder;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.google.common.eventbus.EventBus;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatChannelUserItemControllerTest extends AbstractPlainJavaFxTest {

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
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private JoinGameHelper joinGameHelper;
  @Mock
  private EventBus eventBus;
  @Mock
  private ClanService clanService;
  @Mock
  private PlayerService playerService;
  @Mock
  private PlatformService platformService;
  @Mock
  private TimeService timeService;

  @Before
  public void setUp() throws Exception {
    instance = new ChatUserItemController(preferencesService, avatarService, countryFlagService, i18n, uiService, joinGameHelper, eventBus, clanService, playerService, platformService, timeService);

    Preferences preferences = new Preferences();
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(chatService.getOrCreateChatUser("junit", )).thenReturn(new ChatChannelUser("junit", null));
    when(i18n.get(eq("user.status.hosting"), anyString())).thenReturn("Hosting");
    when(i18n.get(eq("user.status.waiting"), anyString())).thenReturn("Waiting");
    when(i18n.get(eq("user.status.playing"), anyString())).thenReturn("Playing");
    when(clanService.getClanByTag(anyString())).thenReturn(CompletableFuture.completedFuture(Optional.of(new Clan())));

    loadFxml("theme/chat/chat_user_item.fxml", param -> instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.chatUserItemRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testSetPlayer() throws Exception {
    instance.setChatUser(PlayerBuilder.create("junit").defaultValues().get());

    assertThat(instance.clanMenu.getText(), is("[e]"));
    assertThat(instance.countryImageView.isVisible(), is(true));
    verify(countryFlagService).loadCountryFlag("US");
  }

  @Test
  public void testGetPlayer() throws Exception {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    instance.setChatUser(player);

    assertThat(instance.getChatUser(), is(player));
  }

  @Test
  public void testOpenGameSetsStatusToWaiting() throws Exception {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    instance.setChatUser(player);

    assertThat(instance.statusLabel.getText(), is(""));

    player.setGame(GameBuilder.create().defaultValues().get());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.statusLabel.getText(), is("Waiting"));
  }

  @Test
  public void testHostedGameSetsStatusToHosting() throws Exception {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    instance.setChatUser(player);

    assertThat(instance.statusLabel.getText(), is(""));

    player.setGame(GameBuilder.create().defaultValues().host("junit").state(GameStatus.OPEN).get());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.statusLabel.getText(), is("Hosting"));
  }

  @Test
  public void testActiveGameSetsStatusToPlaying() throws Exception {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    instance.setChatUser(player);

    assertThat(instance.statusLabel.getText(), is(""));

    player.setGame(GameBuilder.create().defaultValues().host("junit").state(GameStatus.PLAYING).get());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.statusLabel.getText(), is("Playing"));
  }

  @Test
  public void testNullGameSetsStatusToNothing() throws Exception {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    instance.setChatUser(player);
    player.setGame(GameBuilder.create().defaultValues().host("junit").state(GameStatus.PLAYING).get());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.statusLabel.getText(), is("Playing"));
    player.setGame(null);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.statusLabel.getText(), is(""));
  }

  @Test
  public void testOnMouseEnterUsername() throws Exception {
    instance.setChatUser(PlayerBuilder.create("junit").defaultValues().get());
    WaitForAsyncUtils.asyncFx(() -> instance.onMouseEnteredUsername());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.usernameLabel.getTooltip(), not(nullValue()));
    assertThat(instance.clanMenu.getTooltip(), nullValue());
  }

  @Test
  public void testOnMouseEnterUsernameIfPlayerNull() throws Exception {
    WaitForAsyncUtils.asyncFx(() -> instance.onMouseEnteredUsername());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.usernameLabel.getTooltip(), nullValue());
    assertThat(instance.clanMenu.getTooltip(), nullValue());
  }

  @Test
  public void testOnMouseEnterUsernameIfClanNull() throws Exception {
    instance.setChatUser(PlayerBuilder.create("junit").defaultValues().get());
    instance.getChatUser().setClan("");

    WaitForAsyncUtils.asyncFx(() -> instance.onMouseEnteredUsername());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.usernameLabel.getTooltip(), not(nullValue()));
    assertThat(instance.clanMenu.getTooltip(), nullValue());
  }

  @Test
  public void testSingleClickDoesNotInitiatePrivateChat() throws Exception {
    instance.onItemClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 1));

    verify(eventBus, never()).post(CoreMatchers.any(InitiatePrivateChatEvent.class));
  }

  @Test
  public void testDoubleClickInitiatesPrivateChat() throws Exception {
    instance.setChatUser(PlayerBuilder.create("junit").defaultValues().get());
    WaitForAsyncUtils.waitForFxEvents();

    instance.onItemClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 2));

    ArgumentCaptor<InitiatePrivateChatEvent> captor = ArgumentCaptor.forClass(InitiatePrivateChatEvent.class);
    verify(eventBus).post(captor.capture());

    assertThat(captor.getValue().getUsername(), is("junit"));
  }

  @Test
  public void testOnContextMenuRequested() throws Exception {
    WaitForAsyncUtils.asyncFx(() -> getRoot().getChildren().setAll(instance.chatUserItemRoot));

    Player player = PlayerBuilder.create("junit").defaultValues().get();
    instance.setChatUser(player);
    WaitForAsyncUtils.waitForFxEvents();

    ChatUserContextMenuController contextMenuController = mock(ChatUserContextMenuController.class);
    ContextMenu contextMenu = mock(ContextMenu.class);
    when(contextMenuController.getContextMenu()).thenReturn(contextMenu);
    when(uiService.loadFxml("theme/chat/chat_user_context_menu.fxml")).thenReturn(contextMenuController);

    ContextMenuEvent event = mock(ContextMenuEvent.class);
    instance.onContextMenuRequested(event);

    verify(uiService).loadFxml("theme/chat/chat_user_context_menu.fxml");
    verify(contextMenuController).setChatUser(player);
    verify(contextMenu).show(any(Pane.class), anyDouble(), anyDouble());
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

  @Test
  public void testOnMouseEnteredTag() throws Exception {
    instance.clanMenu.setVisible(false);
    instance.setChatUser(PlayerBuilder.create("junit").defaultValues().get());
    instance.onMouseEnteredClanTag();
    assertThat(instance.clanMenu.getPrefWidth(), not(0.0));
    assertThat(instance.clanMenu.isVisible(), is(true));
  }
}
