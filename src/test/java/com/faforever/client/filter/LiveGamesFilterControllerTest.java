package com.faforever.client.filter;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.mod.ModService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.social.SocialService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.commons.lobby.GameType;
import javafx.collections.FXCollections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import static com.faforever.client.builders.GameBeanBuilder.create;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class LiveGamesFilterControllerTest extends PlatformTest {

  @Mock
  private UiService uiService;
  @Mock
  private I18n i18n;
  @Mock
  private ModService modService;
  @Mock
  private PlayerService playerService;
  @Mock
  private SocialService socialService;
  @Mock
  private MapGeneratorService mapGeneratorService;

  @Mock
  private FilterCheckboxController<GameBean> singleGamesController;
  @Mock
  private FilterCheckboxController<GameBean> gamesWithFriendsController;
  @Mock
  private FilterMultiCheckboxController<GameType, GameBean> gameTypeController;
  @Mock
  private FilterTextFieldController<GameBean> playerNameController;
  @Mock
  private FilterCheckboxController<GameBean> generatedMapsController;

  @InjectMocks
  private LiveGamesFilterController instance;

  @BeforeEach
  public void setUp() throws Exception {
    // Order is important
    when(uiService.loadFxml(anyString())).thenReturn(
        mock(FilterCheckboxController.class), // Sim mods
        singleGamesController,
        gamesWithFriendsController,
        generatedMapsController,
        gameTypeController,
        mock(FilterMultiCheckboxController.class), // Featured mods
        playerNameController
    );
    when(modService.getFeaturedMods()).thenReturn(CompletableFuture.completedFuture(FXCollections.observableArrayList()));

    loadFxml("theme/filter/filter.fxml", clazz -> instance, instance);
  }

  @Test
  public void testGameTypeFilter() {
    ArgumentCaptor<BiFunction<List<GameType>, GameBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(gameTypeController).registerListener(argumentCaptor.capture());

    BiFunction<List<GameType>, GameBean, Boolean> filter = argumentCaptor.getValue();

    List<GameType> emptyList = Collections.emptyList();
    assertTrue(filter.apply(emptyList, create().defaultValues().gameType(GameType.CUSTOM).get()));
    assertTrue(filter.apply(emptyList, create().defaultValues().gameType(GameType.MATCHMAKER).get()));

    List<GameType> matchmaker = List.of(GameType.MATCHMAKER);
    assertFalse(filter.apply(matchmaker, create().defaultValues().gameType(GameType.CUSTOM).get()));
    assertTrue(filter.apply(matchmaker, create().defaultValues().gameType(GameType.MATCHMAKER).get()));
  }

  @Test
  public void testPlayerNameFilter() {
    ArgumentCaptor<BiFunction<String, GameBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(playerNameController).registerListener(argumentCaptor.capture());

    PlayerBean player1 = PlayerBeanBuilder.create().defaultValues().id(1).username("player1").get();
    PlayerBean player2 = PlayerBeanBuilder.create().defaultValues().id(2).username("player2").get();
    PlayerBean enemy1 = PlayerBeanBuilder.create().defaultValues().id(3).username("enemy1").get();
    PlayerBean enemy2 = PlayerBeanBuilder.create().defaultValues().id(4).username("enemy2").get();

    when(playerService.getPlayerByIdIfOnline(1)).thenReturn(Optional.of(player1));
    when(playerService.getPlayerByIdIfOnline(2)).thenReturn(Optional.of(player2));
    when(playerService.getPlayerByIdIfOnline(3)).thenReturn(Optional.of(enemy1));
    when(playerService.getPlayerByIdIfOnline(4)).thenReturn(Optional.of(enemy2));

    GameBean game = create().defaultValues()
        .teams(Map.of(1, List.of(1, 2), 2, List.of(3, 4)))
        .get();
    BiFunction<String, GameBean, Boolean> filter = argumentCaptor.getValue();
    assertTrue(filter.apply("", game));
    assertTrue(filter.apply("player", game));
    assertTrue(filter.apply("enemy", game));
    assertFalse(filter.apply("god", game));
  }

  @Test
  public void testSingleGamesFilter() {
    ArgumentCaptor<BiFunction<Boolean, GameBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(singleGamesController).registerListener(argumentCaptor.capture());

    BiFunction<Boolean, GameBean, Boolean> filter = argumentCaptor.getValue();

    assertTrue(filter.apply(false, create().defaultValues().teams(Map.of(1, List.of(1))).get()));
    assertTrue(filter.apply(false, create().defaultValues().teams(Map.of(1, List.of(1, 2))).get()));
    assertTrue(filter.apply(true, create().defaultValues().teams(Map.of(1, List.of(1), 2, List.of(2))).get()));
    assertFalse(filter.apply(true, create().defaultValues().teams(Map.of(1, List.of(1))).get()));
  }

  @Test
  public void testGameWithFriendsFilter() {
    ArgumentCaptor<BiFunction<Boolean, GameBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(gamesWithFriendsController).registerListener(argumentCaptor.capture());

    GameBean game = create().defaultValues().get();

    BiFunction<Boolean, GameBean, Boolean> filter = argumentCaptor.getValue();

    assertTrue(filter.apply(false, game));

    when(socialService.areFriendsInGame(game)).thenReturn(false, true);
    assertFalse(filter.apply(true, game));
    assertTrue(filter.apply(true, game));
  }

  @Test
  public void testGeneratedMapsFilter() {
    ArgumentCaptor<BiFunction<Boolean, GameBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(generatedMapsController).registerListener(argumentCaptor.capture());

    GameBean game = create().defaultValues().get();

    BiFunction<Boolean, GameBean, Boolean> filter = argumentCaptor.getValue();

    assertTrue(filter.apply(false, game));

    when(mapGeneratorService.isGeneratedMap(game.getMapFolderName())).thenReturn(false, true);
    assertFalse(filter.apply(true, game));
    assertTrue(filter.apply(true, game));
  }
}