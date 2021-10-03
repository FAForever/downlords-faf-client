package com.faforever.client.chat;

import com.faforever.client.builders.AvatarBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.domain.ClanBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.MouseEvents;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
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

  private ClanBean testClan;

  @BeforeEach
  public void setUp() throws Exception {
    Preferences preferences = PreferencesBuilder.create().defaultValues().get();
    when(preferencesService.getPreferences()).thenReturn(preferences);

    when(i18n.get(eq("clan.messageLeader"))).thenReturn("Message clan leader");
    when(i18n.get(eq("clan.visitPage"))).thenReturn("Visit clan website");
    testClan = new ClanBean();
    testClan.setTag("e");
    testClan.setLeader(PlayerBeanBuilder.create().defaultValues().username("test_player").id(2).get());
    when(playerService.isOnline(eq(2))).thenReturn(true);
    when(playerService.getCurrentPlayer()).thenReturn(PlayerBeanBuilder.create().defaultValues().get());

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
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create(player.getUsername())
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
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create(player.getUsername())
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
    ChatUserContextMenuController contextMenuController = mock(ChatUserContextMenuController.class);
    ContextMenu contextMenu = mock(ContextMenu.class);
    when(contextMenuController.getContextMenu()).thenReturn(contextMenu);
    when(uiService.loadFxml("theme/player_context_menu.fxml", ChatUserContextMenuController.class)).thenReturn(contextMenuController);

    WaitForAsyncUtils.asyncFx(() -> getRoot().getChildren().setAll(instance.chatUserItemRoot));

    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit").defaultValues().get();
    instance.setChatUser(chatUser);
    WaitForAsyncUtils.waitForFxEvents();

    ContextMenuEvent event = mock(ContextMenuEvent.class);
    instance.onContextMenuRequested(event);

    verify(contextMenuController).setChatUser(chatUser);
    verify(contextMenu).show(any(Window.class), anyDouble(), anyDouble());
  }

  @Test
  public void testContactClanLeaderNotShowing() throws Exception {
    PlayerBean player = PlayerBeanBuilder.create()
        .defaultValues()
        .id(2)
        .clan("e")
        .avatar(AvatarBeanBuilder.create().defaultValues().url(new URL("http://example.com/avatar.png")).description("dog").get())
        .get();
    when(playerService.getCurrentPlayer()).thenReturn(player);
    instance.setChatUser(ChatChannelUserBuilder.create(player.getUsername()).defaultValues().player(player).clan(testClan).get());
    WaitForAsyncUtils.waitForFxEvents();

    instance.clanMenu.getOnMouseClicked().handle(null);

    ObservableList<MenuItem> items = instance.clanMenu.getItems();
    assertThat(items.size(), is(1));
    boolean containsMessageItem = items.stream().anyMatch((item) -> "Message clan leader".equals(item.getText()));
    assertThat(containsMessageItem, is(false));
  }

  @Test
  public void testContactClanLeaderShowingSameClan() throws Exception {
    PlayerBean player = PlayerBeanBuilder.create()
        .defaultValues()
        .id(1)
        .clan("e")
        .get();
    when(playerService.getCurrentPlayer()).thenReturn(player);
    instance.setChatUser(ChatChannelUserBuilder.create(player.getUsername()).defaultValues().player(player).clan(testClan).get());
    WaitForAsyncUtils.waitForFxEvents();

    instance.clanMenu.getOnMouseClicked().handle(null);

    ObservableList<MenuItem> items = instance.clanMenu.getItems();
    assertThat(items.size(), is(2));
    boolean containsMessageItem = items.stream().anyMatch((item) -> "Message clan leader".equals(item.getText()));
    assertThat(containsMessageItem, is(true));
  }

  @Test
  public void testContactClanLeaderShowingOtherClan() throws Exception {
    PlayerBean player = PlayerBeanBuilder.create()
        .defaultValues()
        .clan("e")
        .get();
    PlayerBean otherClanLeader = PlayerBeanBuilder.create()
        .defaultValues()
        .username("test_player")
        .clan("not")
        .get();
    when(playerService.getCurrentPlayer()).thenReturn(otherClanLeader);
    instance.setChatUser(ChatChannelUserBuilder.create(player.getUsername()).defaultValues().player(player).clan(testClan).get());
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
