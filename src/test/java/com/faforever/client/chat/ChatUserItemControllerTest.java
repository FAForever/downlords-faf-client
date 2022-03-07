package com.faforever.client.chat;

import com.faforever.client.builders.ChatChannelUserBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.MouseEvents;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.fx.contextmenu.helper.ContextMenuBuilderHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import javafx.scene.control.ContextMenu;
import javafx.scene.image.Image;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatUserItemControllerTest extends UITest {
  private static final String CHANNEL_NAME = "testChannel";

  @InjectMocks
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
  private ContextMenuBuilder contextMenuBuilder;

  @BeforeEach
  public void setUp() throws Exception {
    Preferences preferences = PreferencesBuilder.create().defaultValues().get();
    when(preferencesService.getPreferences()).thenReturn(preferences);

    when(i18n.get(eq("clan.messageLeader"))).thenReturn("Message clan leader");
    when(i18n.get(eq("clan.visitPage"))).thenReturn("Visit clan website");
    when(playerService.isOnline(eq(2))).thenReturn(true);
    when(playerService.getCurrentPlayer()).thenReturn(PlayerBeanBuilder.create().defaultValues().get());

    loadFxml("theme/chat/chat_user_item.fxml", param -> instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.root));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testGetPlayer() {
    ChatChannelUser chatUser = ChatChannelUserBuilder.create("junit", CHANNEL_NAME).defaultValues().get();
    instance.setChatUser(chatUser);

    assertThat(instance.getChatUser(), is(chatUser));
  }

  @Test
  public void testSingleClickDoesNotInitiatePrivateChat() {
    instance.onItemClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 1));

    verify(eventBus, never()).post(CoreMatchers.any(InitiatePrivateChatEvent.class));
  }

  @Test
  public void testDoubleClickInitiatesPrivateChat() {
    instance.setChatUser(ChatChannelUserBuilder.create("junit", CHANNEL_NAME).defaultValues().get());
    WaitForAsyncUtils.waitForFxEvents();

    instance.onItemClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 2));

    ArgumentCaptor<InitiatePrivateChatEvent> captor = ArgumentCaptor.forClass(InitiatePrivateChatEvent.class);
    verify(eventBus, times(1)).post(captor.capture());

    assertThat(captor.getValue().getUsername(), is("junit"));
  }

  @Test
  public void testOnContextMenuRequested() {
    runOnFxThreadAndWait(() -> getRoot().getChildren().add(instance.getRoot()));
    ChatChannelUser user = ChatChannelUserBuilder.create("junit", CHANNEL_NAME).defaultValues().get();
    ContextMenu contextMenuMock = ContextMenuBuilderHelper.mockContextMenuBuilderAndGetContextMenuMock(contextMenuBuilder);

    instance.setChatUser(user);
    instance.onContextMenuRequested(mock(ContextMenuEvent.class));
    verify(contextMenuMock).show(eq(instance.getRoot().getScene().getWindow()), anyDouble(), anyDouble());
  }
}
