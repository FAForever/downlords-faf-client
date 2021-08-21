package com.faforever.client.chat;

import com.faforever.client.avatar.AvatarBean;
import com.faforever.client.clan.Clan;
import com.faforever.client.fx.MouseEvents;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesBuilder;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.google.common.eventbus.EventBus;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.stage.Window;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.net.URL;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatUserItemControllerTest extends UITest {

  private ChatUserItemController instance;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private EventBus eventBus;
  @Mock
  private PlayerService playerService;
  @Mock
  private PlatformService platformService;
  @Mock
  private TimeService timeService;

  private Clan testClan;

  @BeforeEach
  public void setUp() throws Exception {
    Preferences preferences = PreferencesBuilder.create().defaultValues().get();
    when(preferencesService.getPreferences()).thenReturn(preferences);

    when(i18n.get(eq("clan.messageLeader"))).thenReturn("Message clan leader");
    when(i18n.get(eq("clan.visitPage"))).thenReturn("Visit clan website");
    testClan = new Clan();
    testClan.setTag("e");
    testClan.setLeader(PlayerBuilder.create("test_player").defaultValues().id(2).get());
    when(playerService.isOnline(eq(2))).thenReturn(true);
    when(playerService.getCurrentPlayer()).thenReturn(PlayerBuilder.create("junit").defaultValues().get());

    instance = new ChatUserItemController(
        preferencesService,
        i18n,
        uiService,
        eventBus,
        playerService,
        platformService
    );
    loadFxml("theme/chat/chat_user_item.fxml", param -> instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.chatUserItemRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testGetPlayer() {
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit").defaultValues().get();
    instance.setChatUser(chatUser);

    assertThat(instance.getChatUser(), is(chatUser));
  }

  @Test
  public void testNullValuesHidesNodes() {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .player(player)
        .get();
    instance.setChatUser(chatUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.avatarImageView.isVisible());
    assertFalse(instance.playerStatusIndicator.isVisible());
    assertFalse(instance.playerMapImage.isVisible());
    assertFalse(instance.countryImageView.isVisible());
  }

  @Test
  public void testNotNullValuesShowsNodes() {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit")
        .defaultValues()
        .player(player)
        .avatar(new Image(UiService.UNKNOWN_MAP_IMAGE))
        .countryFlag(new Image(UiService.UNKNOWN_MAP_IMAGE))
        .mapImage(new Image(UiService.UNKNOWN_MAP_IMAGE))
        .statusImage(new Image(UiService.UNKNOWN_MAP_IMAGE))
        .get();
    instance.setChatUser(chatUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.avatarImageView.isVisible());
    assertTrue(instance.playerStatusIndicator.isVisible());
    assertTrue(instance.playerMapImage.isVisible());
    assertTrue(instance.countryImageView.isVisible());
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
    verify(eventBus, times(1)).post(captor.capture());

    assertThat(captor.getValue().getUsername(), is("junit"));
  }

  @Test
  public void testOnContextMenuRequested() {
    UserContextMenuController contextMenuController = mock(UserContextMenuController.class);
    ContextMenu contextMenu = mock(ContextMenu.class);
    when(contextMenuController.getContextMenu()).thenReturn(contextMenu);
    when(uiService.loadFxml("theme/chat/user_context_menu.fxml")).thenReturn(contextMenuController);

    WaitForAsyncUtils.asyncFx(() -> getRoot().getChildren().setAll(instance.chatUserItemRoot));

    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit").defaultValues().get();
    instance.setChatUser(chatUser);
    WaitForAsyncUtils.waitForFxEvents();

    ContextMenuEvent event = mock(ContextMenuEvent.class);
    instance.onContextMenuRequested(event);

    verify(uiService).loadFxml("theme/chat/user_context_menu.fxml");
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
    when(playerService.getCurrentPlayer()).thenReturn(player);
    instance.setChatUser(ChatChannelUserBuilder.create("junit").defaultValues().player(player).clan(testClan).get());
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
    when(playerService.getCurrentPlayer()).thenReturn(otherClanLeader);
    instance.setChatUser(ChatChannelUserBuilder.create("junit").defaultValues().player(player).clan(testClan).get());
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
        .get();
    Player otherClanLeader = PlayerBuilder.create("test_player")
        .defaultValues()
        .clan("not")
        .get();
    when(playerService.getCurrentPlayer()).thenReturn(otherClanLeader);
    instance.setChatUser(ChatChannelUserBuilder.create("junit").defaultValues().player(player).clan(testClan).get());
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
