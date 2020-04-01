package com.faforever.client.chat;

import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.clan.Clan;
import com.faforever.client.clan.ClanService;
import com.faforever.client.clan.ClanTooltipController;
import com.faforever.client.fx.MouseEvents;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameBuilder;
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
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.stage.Window;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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
  private I18n i18n;
  @Mock
  private UiService uiService;
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
  private ClanTooltipController clanTooltipControllerMock;
  private Clan testClan;

  @Before
  public void setUp() throws Exception {
    Preferences preferences = new Preferences();
    when(preferencesService.getPreferences()).thenReturn(preferences);

    when(i18n.get(eq("clan.messageLeader"))).thenReturn("Message clan leader");
    when(i18n.get(eq("clan.visitPage"))).thenReturn("Visit clan website");
    testClan = new Clan();
    testClan.setTag("e");
    testClan.setLeader(PlayerBuilder.create("test_player").defaultValues().id(2).get());
    when(clanService.getClanByTag(anyString())).thenReturn(CompletableFuture.completedFuture(Optional.of(testClan)));
    when(countryFlagService.loadCountryFlag("US")).thenReturn(Optional.of(mock(Image.class)));
    when(playerService.isOnline(eq(2))).thenReturn(true);
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(new Player("junit")));
    clanTooltipControllerMock = mock(ClanTooltipController.class);
    when(uiService.loadFxml("theme/chat/clan_tooltip.fxml")).thenReturn(clanTooltipControllerMock);
    when(clanTooltipControllerMock.getRoot()).thenReturn(new Pane());
    when(uiService.getThemeImage(UiService.CHAT_LIST_STATUS_HOSTING)).thenReturn(new Image(UiService.CHAT_LIST_STATUS_HOSTING));
    when(uiService.getThemeImage(UiService.CHAT_LIST_STATUS_LOBBYING)).thenReturn(new Image(UiService.CHAT_LIST_STATUS_LOBBYING));
    when(uiService.getThemeImage(UiService.CHAT_LIST_STATUS_PLAYING)).thenReturn(new Image(UiService.CHAT_LIST_STATUS_PLAYING));

    instance = new ChatUserItemController(preferencesService, avatarService, countryFlagService, i18n, uiService, eventBus, clanService, playerService, platformService, timeService);
    loadFxml("theme/chat/chat_user_item.fxml", param -> instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.chatUserItemRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testSetChatUser() throws Exception {
    Player player = PlayerBuilder.create("junit")
        .defaultValues()
        .clan("e")
        .avatar(new AvatarBean(new URL("http://example.com/avatar.png"), "dog"))
        .get();
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(player));
    instance.setChatUser(ChatChannelUserBuilder.create("junit").defaultValues().setPlayer(player).get());
    WaitForAsyncUtils.waitForFxEvents();
    instance.clanMenu.getOnMouseClicked().handle(null);

    assertThat(instance.clanMenu.getText(), is("[e]"));
    assertThat(instance.clanMenu.isVisible(), is(true));
    verify(countryFlagService).loadCountryFlag("US");
    assertThat(instance.countryTooltip, CoreMatchers.notNullValue());
    assertThat(instance.avatarTooltip, CoreMatchers.notNullValue());
    assertThat(instance.userTooltip, CoreMatchers.notNullValue());
    instance.clanMenu.getOnMouseEntered().handle(null);
    verify(clanTooltipControllerMock).setClan(testClan);
  }

  @Test
  public void testGetPlayer() {
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit").defaultValues().get();
    instance.setChatUser(chatUser);

    assertThat(instance.getChatUser(), is(chatUser));
  }

  @Test
  public void testOpenGameSetsStatusToWaiting() {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .setPlayer(player)
        .get();
    instance.setChatUser(chatUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.playerStatusIndicator.isVisible());

    player.setGame(GameBuilder.create().defaultValues().get());
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.playerStatusIndicator.isVisible());
    assertThat(instance.playerStatusIndicator.getImage().getUrl(), endsWith(Paths.get(UiService.CHAT_LIST_STATUS_LOBBYING).getFileName().toString()));
  }

  @Test
  public void testHostedGameSetsStatusToHosting() {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .setPlayer(player)
        .get();

    instance.setChatUser(chatUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.playerStatusIndicator.isVisible());

    player.setGame(GameBuilder.create().defaultValues().host("junit").state(GameStatus.OPEN).get());
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.playerStatusIndicator.isVisible());
    assertThat(instance.playerStatusIndicator.getImage().getUrl(), endsWith(Paths.get(UiService.CHAT_LIST_STATUS_HOSTING).getFileName().toString()));
  }

  @Test
  public void testActiveGameSetsStatusToPlaying() {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .setPlayer(player)
        .get();
    instance.setChatUser(chatUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.playerStatusIndicator.isVisible());

    player.setGame(GameBuilder.create().defaultValues().host("junit").state(GameStatus.PLAYING).get());
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.playerStatusIndicator.isVisible());
    assertThat(instance.playerStatusIndicator.getImage().getUrl(), endsWith(Paths.get(UiService.CHAT_LIST_STATUS_PLAYING).getFileName().toString()));
  }

  @Test
  public void testNullGameSetsStatusToNothing() {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .setPlayer(player)
        .get();
    instance.setChatUser(chatUser);
    player.setGame(GameBuilder.create().defaultValues().host("junit").state(GameStatus.PLAYING).get());
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.playerStatusIndicator.isVisible());
    assertThat(instance.playerStatusIndicator.getImage().getUrl(), endsWith(Paths.get(UiService.CHAT_LIST_STATUS_PLAYING).getFileName().toString()));
    player.setGame(null);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.playerStatusIndicator.isVisible());
  }

  @Test
  public void testSingleClickDoesNotInitiatePrivateChat() {
    instance.onItemClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 1));

    verify(eventBus, never()).post(CoreMatchers.any(InitiatePrivateChatEvent.class));
  }

  @Test
  public void testDoubleClickInitiatesPrivateChat() {
    instance.setChatUser(ChatChannelUserBuilder.create("junit").defaultValues().get());
    WaitForAsyncUtils.waitForFxEvents();

    instance.onItemClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 2));

    ArgumentCaptor<InitiatePrivateChatEvent> captor = ArgumentCaptor.forClass(InitiatePrivateChatEvent.class);
    verify(eventBus).post(captor.capture());

    assertThat(captor.getValue().getUsername(), is("junit"));
  }

  @Test
  public void testOnContextMenuRequested() {
    ChatUserContextMenuController contextMenuController = mock(ChatUserContextMenuController.class);
    ContextMenu contextMenu = mock(ContextMenu.class);
    when(contextMenuController.getContextMenu()).thenReturn(contextMenu);
    when(uiService.loadFxml("theme/chat/chat_user_context_menu.fxml")).thenReturn(contextMenuController);

    WaitForAsyncUtils.asyncFx(() -> getRoot().getChildren().setAll(instance.chatUserItemRoot));

    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit").defaultValues().get();
    instance.setChatUser(chatUser);
    WaitForAsyncUtils.waitForFxEvents();

    ContextMenuEvent event = mock(ContextMenuEvent.class);
    instance.onContextMenuRequested(event);

    verify(uiService).loadFxml("theme/chat/chat_user_context_menu.fxml");
    verify(contextMenuController).setChatUser(chatUser);
    verify(contextMenu).show(any(Window.class), anyDouble(), anyDouble());
  }

  @Test
  public void testContactClanLeaderNotShowing() throws Exception {
    Player player = PlayerBuilder.create("junit")
        .defaultValues()
        .id(2)
        .clan("e")
        .avatar(new AvatarBean(new URL("http://example.com/avatar.png"), "dog"))
        .get();
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(player));
    instance.setChatUser(ChatChannelUserBuilder.create("junit").defaultValues().setPlayer(player).get());
    WaitForAsyncUtils.waitForFxEvents();

    instance.clanMenu.getOnMouseClicked().handle(null);

    ObservableList<MenuItem> items = instance.clanMenu.getItems();
    assertThat(items.size(), is(1));
    boolean containsMessageItem = items.stream().anyMatch((item) -> "Message clan leader".equals(item.getText()));
    assertThat(containsMessageItem, is(false));
  }

  @Test
  public void testContactClanLeaderShowingSameClan() throws Exception {
    Player player = PlayerBuilder.create("junit")
        .defaultValues()
        .clan("e")
        .avatar(new AvatarBean(new URL("http://example.com/avatar.png"), "dog"))
        .get();
    Player otherClanLeader = PlayerBuilder.create("test_player")
        .id(2)
        .defaultValues()
        .clan("e")
        .avatar(new AvatarBean(new URL("http://example.com/avatar.png"), "dog"))
        .get();
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(otherClanLeader));
    instance.setChatUser(ChatChannelUserBuilder.create("junit").defaultValues().setPlayer(player).get());
    WaitForAsyncUtils.waitForFxEvents();

    instance.clanMenu.getOnMouseClicked().handle(null);

    ObservableList<MenuItem> items = instance.clanMenu.getItems();
    assertThat(items.size(), is(2));
    boolean containsMessageItem = items.stream().anyMatch((item) -> "Message clan leader".equals(item.getText()));
    assertThat(containsMessageItem, is(true));
  }

  @Test
  public void testContactClanLeaderShowingOtherClan() throws Exception {
    Player player = PlayerBuilder.create("junit")
        .defaultValues()
        .clan("e")
        .avatar(new AvatarBean(new URL("http://example.com/avatar.png"), "dog"))
        .get();
    Player otherClanLeader = PlayerBuilder.create("test_player")
        .defaultValues()
        .clan("not")
        .avatar(new AvatarBean(new URL("http://example.com/avatar.png"), "dog"))
        .get();
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(otherClanLeader));
    instance.setChatUser(ChatChannelUserBuilder.create("junit").defaultValues().setPlayer(player).get());
    WaitForAsyncUtils.waitForFxEvents();

    instance.clanMenu.getOnMouseClicked().handle(null);

    ObservableList<MenuItem> items = instance.clanMenu.getItems();
    assertThat(items.size(), is(2));
    boolean containsMessageItem = items.stream().anyMatch((item) -> "Message clan leader".equals(item.getText()));
    assertThat(containsMessageItem, is(true));
  }


  @Test
  public void testSetVisible() {
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
