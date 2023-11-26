package com.faforever.client.game;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.builders.AvatarBeanBuilder;
import com.faforever.client.builders.FeaturedModBeanBuilder;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.MouseEvents;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.ModService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.social.SocialService;
import com.faforever.client.test.PlatformTest;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameTileControllerTest extends PlatformTest {

  @Mock
  private ModService modService;
  @Mock
  private JoinGameHelper joinGameHelper;
  @Mock
  private ImageViewHelper imageViewHelper;
  @Mock
  private I18n i18n;
  @Mock
  private MapService mapService;
  @Mock
  private AvatarService avatarService;
  @Mock
  private PlayerService playerService;
  @Mock
  private SocialService socialService;

  private GameTileController instance;

  private GameBean game;

  @Mock
  private Consumer<GameBean> onSelectedConsumer;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new GameTileController(mapService, i18n, joinGameHelper, modService, playerService, avatarService,
                                      socialService, imageViewHelper, fxApplicationThreadExecutor);

    game = GameBeanBuilder.create().defaultValues().get();

    when(i18n.get(anyString())).thenReturn("test");
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(
        Mono.just(FeaturedModBeanBuilder.create().defaultValues().get()));
    when(fxApplicationThreadExecutor.asScheduler()).thenReturn(Schedulers.immediate());
    when(mapService.isInstalledBinding(anyString())).thenReturn(new SimpleBooleanProperty());
    when(imageViewHelper.createPlaceholderImageOnErrorObservable(any())).thenAnswer(
        invocation -> new SimpleObjectProperty<>(invocation.getArgument(0)));

    loadFxml("theme/play/game_card.fxml", clazz -> instance);
    instance.setOnSelectedListener(onSelectedConsumer);
  }

  @Test
  public void testOnLeftDoubleClick() {
    runOnFxThreadAndWait(() -> instance.setGame(game));
    runOnFxThreadAndWait(() -> instance.onClick(MouseEvents.generateClick(MouseButton.PRIMARY, 2)));
    verify(joinGameHelper).join(any());
    verify(onSelectedConsumer).accept(game);
  }

  @Test
  public void testOnLeftSingleClick() {
    runOnFxThreadAndWait(() -> instance.setGame(game));
    runOnFxThreadAndWait(() -> instance.onClick(MouseEvents.generateClick(MouseButton.PRIMARY, 1)));
    verify(joinGameHelper, never()).join(any());
    verify(onSelectedConsumer).accept(game);
  }

  @Test
  public void testSimModeLabel4Mods() {
    HashMap<String, String> simMods = new HashMap<>();
    simMods.put("test1", "test1");
    simMods.put("test2", "test2");
    simMods.put("test3", "test3");
    simMods.put("test4", "test4");
    game.setSimMods(FXCollections.observableMap(simMods));
    runOnFxThreadAndWait(() -> instance.setGame(game));
    WaitForAsyncUtils.waitForFxEvents();

    verify(i18n).get(eq("game.mods.twoAndMore"), contains("test"), eq(3));
  }

  @Test
  public void testSimModeLabel2Mods() {
    HashMap<String, String> simMods = new HashMap<>();
    simMods.put("test1", "test1");
    simMods.put("test2", "test2");
    game.setSimMods(FXCollections.observableMap(simMods));
    runOnFxThreadAndWait(() -> instance.setGame(game));
    WaitForAsyncUtils.waitForFxEvents();

    verify(i18n).get("textSeparator");
  }

  @Test
  public void testFriendInGameHighlighting() {
    when(socialService.areFriendsInGame(game)).thenReturn(true);

    runOnFxThreadAndWait(() -> instance.setGame(game));

    assertTrue(instance.getRoot().getPseudoClassStates().contains(GameTileController.FRIEND_IN_GAME_PSEUDO_CLASS));
  }

  @Test
  public void testShowAvatarInsteadOfDefaultHostIcon() {
    when(playerService.getPlayerByNameIfOnline(anyString())).thenReturn(
        Optional.of(PlayerBeanBuilder.create().avatar(AvatarBeanBuilder.create().get()).get()));
    when(avatarService.loadAvatar(any())).thenReturn(new Image(InputStream.nullInputStream()));

    runOnFxThreadAndWait(() -> instance.setGame(game));

    assertFalse(instance.defaultHostIcon.isVisible());
    assertTrue(instance.avatarImageView.isVisible());
  }

  @Test
  public void testShowDefaultHostIconIfNoAvatar() {
    when(playerService.getPlayerByNameIfOnline(anyString())).thenReturn(Optional.of(PlayerBeanBuilder.create().get()));

    runOnFxThreadAndWait(() -> instance.setGame(game));

    assertTrue(instance.defaultHostIcon.isVisible());
    assertFalse(instance.avatarImageView.isVisible());
  }
}
