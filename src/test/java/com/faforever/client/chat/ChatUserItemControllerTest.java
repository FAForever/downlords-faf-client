package com.faforever.client.chat;

import com.faforever.client.builders.ChatChannelUserBuilder;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.MapVersionBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.MouseEvents;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.fx.contextmenu.helper.ContextMenuBuilderHelper;
import com.faforever.client.game.GameTooltipController;
import com.faforever.client.helper.TooltipHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import javafx.scene.control.ContextMenu;
import javafx.scene.image.Image;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatUserItemControllerTest extends UITest {

  private static final String CHANNEL_NAME = "#testChannel";
  private static final String USER_NAME = "junit";

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private EventBus eventBus;
  @Mock
  private ContextMenuBuilder contextMenuBuilder;
  @Mock
  private MapGeneratorService mapGeneratorService;
  @Mock
  private ImageViewHelper imageViewHelper;
  @Mock
  private MapService mapService;

  private ChatChannelUser defaultUser;
  private ChatPrefs chatPrefs;

  @InjectMocks
  private ChatUserItemController instance;

  @BeforeEach
  public void setUp() throws Exception {
    defaultUser = ChatChannelUserBuilder.create(USER_NAME, CHANNEL_NAME).defaultValues().get();

    Preferences preferences = PreferencesBuilder.create().defaultValues().chatPrefs().then().get();
    chatPrefs = preferences.getChat();
    when(preferencesService.getPreferences()).thenReturn(preferences);

    when(i18n.get(eq("clan.messageLeader"))).thenReturn("Message clan leader");
    when(i18n.get(eq("clan.visitPage"))).thenReturn("Visit clan website");

    loadFxml("theme/chat/chat_user_item.fxml", param -> instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertNotNull(instance.getRoot());
  }

  @Test
  public void testGetPlayer() {
    instance.setChatUser(defaultUser);
    assertEquals(defaultUser, instance.getChatUser());
  }

  @Test
  public void testSingleClickDoesNotInitiatePrivateChat() {
    runOnFxThreadAndWait(() -> instance.onItemClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 1)));
    verify(eventBus, never()).post(CoreMatchers.any(InitiatePrivateChatEvent.class));
  }

  @Test
  public void testDoubleClickInitiatesPrivateChat() {
    instance.setChatUser(defaultUser);
    runOnFxThreadAndWait(() -> instance.onItemClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 2)));

    ArgumentCaptor<InitiatePrivateChatEvent> captor = ArgumentCaptor.forClass(InitiatePrivateChatEvent.class);
    verify(eventBus, times(1)).post(captor.capture());
    assertEquals(USER_NAME, captor.getValue().getUsername());
  }

  @Test
  public void testOnContextMenuRequested() {
    runOnFxThreadAndWait(() -> getRoot().getChildren().add(instance.getRoot()));
    ContextMenu contextMenuMock = ContextMenuBuilderHelper.mockContextMenuBuilderAndGetContextMenuMock(contextMenuBuilder);

    instance.setChatUser(defaultUser);
    instance.onContextMenuRequested(mock(ContextMenuEvent.class));
    verify(contextMenuMock).show(eq(instance.getRoot().getScene().getWindow()), anyDouble(), anyDouble());
  }

  @Test
  public void testCheckShowMapNameListener() {
    PlayerBean player = PlayerBeanBuilder.create()
        .defaultValues()
        .game(GameBeanBuilder.create().defaultValues().get())
        .get();
    defaultUser.setPlayer(player);
    runOnFxThreadAndWait(() -> instance.setChatUser(defaultUser));
    assertEquals(instance.mapNameLabel.isVisible(), chatPrefs.isShowMapName());

    chatPrefs.setShowMapName(!chatPrefs.isShowMapName());
    assertEquals(instance.mapNameLabel.isVisible(), chatPrefs.isShowMapName());
  }

  @Test
  public void testInvisibleMapNameLabelWhenNoMapName() {
    runOnFxThreadAndWait(() -> {
      chatPrefs.setShowMapName(true);
      instance.mapNameLabel.setText("");
    });
    assertFalse(instance.mapNameLabel.isVisible());
  }

  @Test
  public void testCheckShowMapPreviewListener() {
    boolean visible = chatPrefs.isShowMapPreview();
    assertEquals(instance.mapImageView.isVisible(), visible);

    runOnFxThreadAndWait(() -> chatPrefs.setShowMapPreview(!visible));
    assertEquals(instance.mapImageView.isVisible(), !visible);
  }

  @Test
  public void testCheckChatUserGameListener() {
    PlayerBean player = PlayerBeanBuilder.create()
        .defaultValues()
        .game(GameBeanBuilder.create().defaultValues().get())
        .get();
    String mapFolderName = player.getGame().getMapFolderName();
    MapVersionBean mapVersion = MapVersionBeanBuilder.create().defaultValues().get();
    defaultUser.setMapImage(mock(Image.class));
    defaultUser.setGameStatusImage(mock(Image.class));
    defaultUser.setPlayer(player);

    when(mapService.getMapLocallyFromName(mapFolderName)).thenReturn(Optional.of(mapVersion));
    when(mapService.convertMapFolderNameToHumanNameIfPossible(mapFolderName)).thenReturn("map name");
    when(i18n.get(eq("game.onMapFormat"), anyString())).thenReturn(mapVersion.getMap().getDisplayName(), "map name", "Neroxis Generated Map");

    runOnFxThreadAndWait(() -> instance.setChatUser(defaultUser));
    assertNotNull(instance.gameStatusImageView.getImage());
    assertNotNull(instance.mapImageView.getImage());
    assertEquals(mapVersion.getMap().getDisplayName(), instance.mapNameLabel.getText());
  }

  @Test
  public void testAvatarImageViewHasTooltip() {
    assertNotNull(TooltipHelper.getTooltip(instance.avatarImageView));
  }

  @Test
  public void testOnMapImageViewMouseMovedAndExited() {
    defaultUser.setPlayer(PlayerBeanBuilder.create()
        .defaultValues()
        .game(GameBeanBuilder.create().defaultValues().get())
        .get());
    GameTooltipController controllerMock = mock(GameTooltipController.class);
    when(uiService.loadFxml("theme/play/game_tooltip.fxml")).thenReturn(controllerMock);
    when(controllerMock.getRoot()).thenReturn(new VBox());

    instance.setChatUser(defaultUser);
    runOnFxThreadAndWait(() -> instance.onMapImageViewMouseMoved());
    verify(controllerMock).displayGame();
    assertNotNull(TooltipHelper.getTooltip(instance.mapImageView));

    runOnFxThreadAndWait(() -> instance.onMapImageViewMouseExited());
    assertNull(TooltipHelper.getTooltip(instance.mapImageView));
  }

  @Test
  public void testPlayerNoteTooltip() {
    defaultUser.setPlayer(PlayerBeanBuilder.create()
        .defaultValues()
        .game(GameBeanBuilder.create().defaultValues().get())
        .note("Player 1")
        .get());
    runOnFxThreadAndWait(() -> instance.setChatUser(defaultUser));
    assertEquals("Player 1", TooltipHelper.getTooltipText(instance.userContainer));

    runOnFxThreadAndWait(() -> defaultUser.getPlayer().orElseThrow().setNote(""));
    assertNull(TooltipHelper.getTooltip(instance.userContainer));
  }
}
