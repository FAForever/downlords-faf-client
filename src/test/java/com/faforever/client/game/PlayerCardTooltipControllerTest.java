package com.faforever.client.game;

import com.faforever.client.chat.InitiatePrivateChatEvent;
import com.faforever.client.fx.MouseEvents;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserContextMenuController;
import com.faforever.commons.api.dto.Faction;
import com.google.common.eventbus.EventBus;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerCardTooltipControllerTest extends UITest {
  @Mock
  private I18n i18n;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private EventBus eventBus;
  @Mock
  private UiService uiService;

  private PlayerCardTooltipController instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new PlayerCardTooltipController(countryFlagService, uiService,eventBus, i18n);
    loadFxml("theme/player_card_tooltip.fxml", clazz -> instance);
  }

  @Test
  public void testSetFoe() {
    when(i18n.get("userInfo.tooltipFormat.withRating", "foe", 1000)).thenReturn("foe(1000)");

    PlayerBuilder playerBuilder = PlayerBuilder.create("foe")
        .defaultValues()
        .socialStatus(SocialStatus.FOE);
    Player player = playerBuilder.get();
    instance.setPlayer(player, 1000, Faction.CYBRAN);

    assertThat(instance.factionIcon.getStyleClass(), hasItem(UiService.CYBRAN_STYLE_CLASS));
    assertThat(instance.factionIcon.isVisible(), is(true));
    assertThat(instance.factionImage.isVisible(), is(false));
    assertThat(instance.foeIconText.isVisible(), is(true));
    assertThat(instance.friendIconText.isVisible(), is(false));
    assertThat(instance.playerInfo.getText(), is("foe(1000)"));
  }

  @Test
  public void testSetFriend() {
    when(i18n.get("userInfo.tooltipFormat.withRating", "user", 1000)).thenReturn("user(1000)");

    PlayerBuilder playerBuilder = PlayerBuilder.create("user")
        .defaultValues()
        .socialStatus(SocialStatus.FRIEND);
    Player player = playerBuilder.get();
    instance.setPlayer(player, 1000, Faction.SERAPHIM);

    assertThat(instance.factionIcon.getStyleClass(), hasItem(UiService.SERAPHIM_STYLE_CLASS));
    assertThat(instance.factionIcon.isVisible(), is(true));
    assertThat(instance.factionImage.isVisible(), is(false));
    assertThat(instance.foeIconText.isVisible(), is(false));
    assertThat(instance.friendIconText.isVisible(), is(true));
    assertThat(instance.playerInfo.getText(), is("user(1000)"));
  }

  @Test
  public void testSetOther() {
    when(i18n.get("userInfo.tooltipFormat.withRating", "user", 1000)).thenReturn("user(1000)");

    PlayerBuilder playerBuilder = PlayerBuilder.create("user")
        .defaultValues()
        .socialStatus(SocialStatus.OTHER);
    Player player = playerBuilder.get();
    instance.setPlayer(player, 1000, Faction.RANDOM);

    assertThat(instance.factionIcon.isVisible(), is(false));
    assertThat(instance.factionImage.getImage().getUrl(), is(PlayerCardTooltipController.RANDOM_IMAGE.getUrl()));
    assertThat(instance.factionImage.isVisible(), is(true));
    assertThat(instance.foeIconText.isVisible(), is(false));
    assertThat(instance.friendIconText.isVisible(), is(false));
    assertThat(instance.playerInfo.getText(), is("user(1000)"));
  }

  @Test
  public void testInitiatePrivateChatByDoubleClickingOnRoot() {
    Player player = PlayerBuilder.create("junit").socialStatus(SocialStatus.OTHER).get();
    runOnFxThreadAndWait(() -> {
      instance.setPlayer(player, 1000, Faction.RANDOM);
      instance.getRoot().getOnMouseClicked().handle(createMouseDoubleClickEvent());
    });

    verify(eventBus).post(any(InitiatePrivateChatEvent.class));
  }

  @Test
  public void testNoMouseEventForRootIfPlayerIsOwner() {
    Player player = PlayerBuilder.create("junit").socialStatus(SocialStatus.SELF).get();
    runOnFxThreadAndWait(() -> instance.setPlayer(player, 1000, Faction.RANDOM));

    assertThat(instance.getRoot().getOnMouseClicked(), nullValue());
  }

  @Test
  public void testShowContextMenuOfPlayerByRightMouseClicking() {
    UserContextMenuController userContextMenuController = mock(UserContextMenuController.class);
    ContextMenu contextMenu = mock(ContextMenu.class);
    when(uiService.loadFxml("theme/user/user_context_menu.fxml")).thenReturn(userContextMenuController);
    when(userContextMenuController.getRoot()).thenReturn(contextMenu);
    Player player = PlayerBuilder.create("junit").socialStatus(SocialStatus.OTHER).get();
    runOnFxThreadAndWait(() -> {
      getRoot().getChildren().add(instance.getRoot()); // for avoid NPE when invoke getScene()
      instance.setPlayer(player, 1000, Faction.RANDOM);
      instance.getRoot().getOnMouseClicked().handle(MouseEvents.generateClick(MouseButton.SECONDARY, 1));
    });

    verify(contextMenu).show(getStage(), 0.0d, 0.0d);
  }

  private MouseEvent createMouseDoubleClickEvent() {
    return MouseEvents.generateClick(MouseButton.PRIMARY, 2);
  }
}
