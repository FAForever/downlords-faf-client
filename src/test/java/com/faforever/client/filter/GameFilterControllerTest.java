package com.faforever.client.filter;

import com.faforever.client.builders.FeaturedModBeanBuilder;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.ModService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.UITest;
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
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import static com.faforever.client.builders.GameBeanBuilder.create;
import static com.faforever.client.filter.FilterName.MAP_WIDTH;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class GameFilterControllerTest extends UITest {

  @Mock
  private UiService uiService;
  @Mock
  private I18n i18n;
  @Mock
  private ModService modService;
  @Mock
  private PlayerService playerService;

  @InjectMocks
  private AbstractGameFilterController instance;

  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/filter/filter.fxml", clazz -> instance, instance);
  }

  @Test
  public void testGameTypeFilter() {
    FilterMultiCheckboxController<GameType, GameBean> controller = FilterTestUtil.mockFilter(FilterMultiCheckboxController.class, uiService);

    instance.setFollowingFilters(FilterName.GAME_TYPE);

    ArgumentCaptor<BiFunction<List<GameType>, GameBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(controller).registerListener(argumentCaptor.capture());

    BiFunction<List<GameType>, GameBean, Boolean> filter = argumentCaptor.getValue();

    List<GameType> emptyList = Collections.emptyList();
    assertTrue(filter.apply(emptyList, create().defaultValues().gameType(GameType.CUSTOM).get()));
    assertTrue(filter.apply(emptyList, create().defaultValues().gameType(GameType.MATCHMAKER).get()));

    List<GameType> matchmaker = List.of(GameType.MATCHMAKER);
    assertFalse(filter.apply(matchmaker, create().defaultValues().gameType(GameType.CUSTOM).get()));
    assertTrue(filter.apply(matchmaker, create().defaultValues().gameType(GameType.MATCHMAKER).get()));
  }

  @Test
  public void testSimModsFilter() {
    FilterCheckboxController<GameBean> controller = FilterTestUtil.mockFilter(FilterCheckboxController.class, uiService);

    instance.setFollowingFilters(FilterName.SIM_MODS);

    ArgumentCaptor<BiFunction<Boolean, GameBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(controller).registerListener(argumentCaptor.capture());

    BiFunction<Boolean, GameBean, Boolean> filter = argumentCaptor.getValue();

    assertTrue(filter.apply(false, create().defaultValues().simMods(Collections.EMPTY_MAP).get()));
    assertTrue(filter.apply(false, create().defaultValues().simMods(Map.of("1", "2")).get()));
    assertTrue(filter.apply(true, create().defaultValues().simMods(Collections.EMPTY_MAP).get()));
    assertFalse(filter.apply(true, create().defaultValues().simMods(Map.of("1", "2")).get()));
  }

  @Test
  public void testPrivateGameFilter() {
    FilterCheckboxController<GameBean> controller = FilterTestUtil.mockFilter(FilterCheckboxController.class, uiService);

    instance.setFollowingFilters(FilterName.PRIVATE_GAME);

    ArgumentCaptor<BiFunction<Boolean, GameBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(controller).registerListener(argumentCaptor.capture());

    BiFunction<Boolean, GameBean, Boolean> filter = argumentCaptor.getValue();

    assertTrue(filter.apply(false, create().defaultValues().passwordProtected(false).get()));
    assertTrue(filter.apply(false, create().defaultValues().passwordProtected(true).get()));
    assertTrue(filter.apply(true, create().defaultValues().passwordProtected(false).get()));
    assertFalse(filter.apply(true, create().defaultValues().passwordProtected(true).get()));
  }

  @Test
  public void testPlayerNameFilter() {
    FilterTextFieldController<GameBean> controller = FilterTestUtil.mockFilter(FilterTextFieldController.class, uiService);

    instance.setFollowingFilters(FilterName.PLAYER_NAME);

    ArgumentCaptor<BiFunction<String, GameBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(controller).registerListener(argumentCaptor.capture());

    GameBean game = create().defaultValues()
        .teams(Map.of("1", List.of("player1", "player2"), "2", List.of("Enemy1", "ENEmy2")))
        .get();
    BiFunction<String, GameBean, Boolean> filter = argumentCaptor.getValue();
    assertTrue(filter.apply("", game));
    assertTrue(filter.apply("player", game));
    assertTrue(filter.apply("enemy", game));
    assertFalse(filter.apply("god", game));
  }

  @Test
  public void testFeatureModFilter() {
    FilterMultiCheckboxController<FeaturedModBean, GameBean> controller = FilterTestUtil.mockFilter(FilterMultiCheckboxController.class, uiService);
    when(modService.getFeaturedMods()).thenReturn(CompletableFuture.completedFuture(FXCollections.observableArrayList()));

    instance.setFollowingFilters(FilterName.FEATURE_MOD);

    ArgumentCaptor<BiFunction<List<FeaturedModBean>, GameBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(controller).registerListener(argumentCaptor.capture());

    FeaturedModBean featuredMod1 = FeaturedModBeanBuilder.create()
        .defaultValues()
        .technicalName("fafbeta")
        .get();
    FeaturedModBean featuredMod2 = FeaturedModBeanBuilder.create()
        .defaultValues()
        .technicalName("faf")
        .get();

    BiFunction<List<FeaturedModBean>, GameBean, Boolean> filter = argumentCaptor.getValue();

    List<FeaturedModBean> emptyList = Collections.emptyList();
    assertTrue(filter.apply(emptyList, create().defaultValues().featuredMod("faf").get()));
    assertTrue(filter.apply(List.of(featuredMod1), create().defaultValues().featuredMod("fafbeta").get()));
    assertFalse(filter.apply(List.of(featuredMod1, featuredMod2), create().defaultValues()
        .featuredMod("fafdevelop")
        .get()));
    assertTrue(filter.apply(List.of(featuredMod1, featuredMod2), create().defaultValues()
        .featuredMod("fafbeta")
        .get()));
  }

  @Test
  public void testMapFolderNameBlackListFilter() {
    MutableListFilterController<GameBean> controller = FilterTestUtil.mockFilter(MutableListFilterController.class, uiService);

    instance.setFollowingFilters(FilterName.MAP_FOLDER_NAME_BLACKLIST);

    ArgumentCaptor<BiFunction<List<String>, GameBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(controller).registerListener(argumentCaptor.capture());

    GameBean game = create().defaultValues().mapFolderName("test_map.v011").get();
    BiFunction<List<String>, GameBean, Boolean> filter = argumentCaptor.getValue();

    assertTrue(filter.apply(Collections.emptyList(), game));
    assertFalse(filter.apply(List.of("test_"), game));
    assertFalse(filter.apply(List.of(".v011"), game));
    assertFalse(filter.apply(List.of("lenta", "test_map.v011"), game));
    assertTrue(filter.apply(List.of("lenta"), game));
  }

  @Test
  public void testOnePlayerFilter() {
    FilterCheckboxController<GameBean> controller = FilterTestUtil.mockFilter(FilterCheckboxController.class, uiService);

    instance.setFollowingFilters(FilterName.ONE_PLAYER);

    ArgumentCaptor<BiFunction<Boolean, GameBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(controller).registerListener(argumentCaptor.capture());

    BiFunction<Boolean, GameBean, Boolean> filter = argumentCaptor.getValue();

    assertTrue(filter.apply(false, create().defaultValues().numPlayers(1).get()));
    assertTrue(filter.apply(false, create().defaultValues().numPlayers(8).get()));
    assertTrue(filter.apply(true, create().defaultValues().numPlayers(10).get()));
    assertFalse(filter.apply(true, create().defaultValues().numPlayers(1).get()));
  }

  @Test
  public void testGameWithFriendsFilter() {
    FilterCheckboxController<GameBean> controller = FilterTestUtil.mockFilter(FilterCheckboxController.class, uiService);

    instance.setFollowingFilters(FilterName.GAME_WITH_FRIENDS);

    ArgumentCaptor<BiFunction<Boolean, GameBean, Boolean>> argumentCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(controller).registerListener(argumentCaptor.capture());

    GameBean game = create().defaultValues().get();

    BiFunction<Boolean, GameBean, Boolean> filter = argumentCaptor.getValue();

    assertTrue(filter.apply(false, game));

    when(playerService.areFriendsInGame(game)).thenReturn(false, true);
    assertFalse(filter.apply(true, game));
    assertTrue(filter.apply(true, game));
  }

  @Test
  public void testThrowExceptionWhenFilterNotExist() {
    assertThrows(IllegalArgumentException.class, () -> instance.setFollowingFilters(MAP_WIDTH));
  }
}