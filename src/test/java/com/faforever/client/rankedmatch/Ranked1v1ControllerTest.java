package com.faforever.client.rankedmatch;

import com.faforever.client.api.Ranked1v1Stats;
import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.game.Faction;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.leaderboard.Ranked1v1EntryBean;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.Ranked1v1Prefs;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ToggleButton;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.core.env.Environment;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class Ranked1v1ControllerTest extends AbstractPlainJavaFxTest {

  private static final String USERNAME = "junit";
  private static final int PLAYER_ID = 123;
  @Mock
  private Ranked1v1Controller instance;
  @Mock
  private GameService gameService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private PlayerService playerService;
  @Mock
  private Environment environment;
  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private I18n i18n;
  @Mock
  private BooleanProperty searching1v1Property;
  @Mock
  private Preferences preferences;
  @Mock
  private Ranked1v1Prefs ranked1v1Prefs;
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;

  private ObjectProperty<PlayerInfoBean> currentPlayerProperty;
  private ObservableList<Faction> factionList;

  @Before
  public void setUp() throws Exception {
    instance = loadController("ranked_1v1.fxml");
    instance.gameService = gameService;
    instance.preferencesService = preferencesService;
    instance.playerService = playerService;
    instance.environment = environment;
    instance.leaderboardService = leaderboardService;
    instance.i18n = i18n;

    PlayerInfoBean playerInfoBean = new PlayerInfoBean(USERNAME);
    playerInfoBean.setId(PLAYER_ID);
    currentPlayerProperty = new SimpleObjectProperty<>(playerInfoBean);
    factionList = FXCollections.observableArrayList();
    Ranked1v1EntryBean ranked1v1EntryBean = new Ranked1v1EntryBean();
    ranked1v1EntryBean.setRating(500);
    ranked1v1EntryBean.setWinLossRatio(12.23f);
    ranked1v1EntryBean.setRank(100);
    ranked1v1EntryBean.setGamesPlayed(412);
    ranked1v1EntryBean.setUsername(USERNAME);

    Ranked1v1Stats ranked1v1Stats = new Ranked1v1Stats();
    ranked1v1Stats.setRatingDistribution(new HashMap<>());

    when(leaderboardService.getRanked1v1Stats()).thenReturn(CompletableFuture.completedFuture(ranked1v1Stats));
    when(leaderboardService.getEntryForPlayer(PLAYER_ID)).thenReturn(CompletableFuture.completedFuture(ranked1v1EntryBean));
    when(gameService.searching1v1Property()).thenReturn(searching1v1Property);
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getRanked1v1()).thenReturn(ranked1v1Prefs);
    when(ranked1v1Prefs.getFactions()).thenReturn(factionList);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(playerService.getCurrentPlayer()).thenReturn(currentPlayerProperty.get());
    when(playerService.currentPlayerProperty()).thenReturn(currentPlayerProperty);
    when(environment.getProperty("rating.low", int.class)).thenReturn(100);
    when(environment.getProperty("rating.moderate", int.class)).thenReturn(200);
    when(environment.getProperty("rating.good", int.class)).thenReturn(300);
    when(environment.getProperty("rating.high", int.class)).thenReturn(400);
    when(environment.getProperty("rating.top", int.class)).thenReturn(500);
    when(environment.getProperty("rating.beta", int.class)).thenReturn(10);

    instance.postConstruct();
  }

  @Test
  public void testPostConstructSelectsPreviousFactions() throws Exception {
    factionList.setAll(Faction.SERAPHIM, Faction.AEON);

    instance.postConstruct();

    assertThat(instance.aeonButton.isSelected(), is(true));
    assertThat(instance.seraphimButton.isSelected(), is(true));
    assertThat(instance.uefButton.isSelected(), is(false));
    assertThat(instance.cybranButton.isSelected(), is(false));
  }

  @Test
  public void testAllFactionButtonsMapped() throws Exception {
    assertThat(instance.factionsToButtons.keySet(), containsInAnyOrder(Faction.values()));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.ranked1v1Root));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testOnCancelButtonClicked() throws Exception {
    instance.onCancelButtonClicked();

    verify(gameService).stopSearchRanked1v1();
    assertThat(instance.cancelButton.isVisible(), is(false));
    assertThat(instance.playButton.isVisible(), is(true));
    assertThat(instance.searchProgressIndicator.isVisible(), is(false));
    assertThat(instance.searchingForOpponentLabel.isVisible(), is(false));
    for (ToggleButton button : instance.factionsToButtons.values()) {
      assertThat(button.isDisable(), is(false));
    }
  }

  @Test
  public void testOnPlayButtonClicked() throws Exception {
    when(forgedAlliancePrefs.getPath()).thenReturn(Paths.get("."));

    instance.aeonButton.setSelected(true);
    instance.onFactionButtonClicked();

    instance.onPlayButtonClicked();

    verify(gameService).startSearchRanked1v1(any());
    assertThat(instance.cancelButton.isVisible(), is(true));
    assertThat(instance.playButton.isVisible(), is(false));
    assertThat(instance.searchProgressIndicator.isVisible(), is(true));
    assertThat(instance.searchingForOpponentLabel.isVisible(), is(true));
    for (ToggleButton button : instance.factionsToButtons.values()) {
      assertThat(button.isDisable(), is(true));
    }
  }

  @Test
  public void testOnFactionButtonClicked() throws Exception {
    instance.aeonButton.setSelected(true);
    instance.uefButton.setSelected(false);
    instance.cybranButton.setSelected(true);
    instance.seraphimButton.setSelected(false);

    instance.onFactionButtonClicked();

    verify(preferencesService).storeInBackground();
    assertThat(factionList, containsInAnyOrder(Faction.AEON, Faction.CYBRAN));
  }

  @Test
  public void testUpdateRatingLow() throws Exception {
    testUpdateRating(10, "ranked1v1.ratingHint.low");
  }

  private void testUpdateRating(int leaderboardRatingMean, String key) {
    reset(i18n);

    PlayerInfoBean currentPlayer = currentPlayerProperty.get();
    currentPlayer.setLeaderboardRatingDeviation(1);
    currentPlayer.setLeaderboardRatingMean(leaderboardRatingMean);

    instance.setUpIfNecessary();

    verify(playerService).getCurrentPlayer();
    verify(i18n).get(key);
  }

  @Test
  public void testUpdateRatingModerate() throws Exception {
    testUpdateRating(150, "ranked1v1.ratingHint.moderate");
  }

  @Test
  public void testUpdateRatingGood() throws Exception {
    testUpdateRating(250, "ranked1v1.ratingHint.good");
  }

  @Test
  public void testUpdateRatingHigh() throws Exception {
    testUpdateRating(350, "ranked1v1.ratingHint.high");
  }

  @Test
  public void testUpdateRatingTop() throws Exception {
    testUpdateRating(450, "ranked1v1.ratingHint.top");
  }

  @Test
  public void testUpdateRatingInProgress() throws Exception {
    when(environment.getProperty("rating.beta", int.class)).thenReturn(10);
    when(environment.getProperty("rating.initialStandardDeviation", int.class)).thenReturn(50);

    PlayerInfoBean currentPlayer = currentPlayerProperty.get();
    currentPlayer.setLeaderboardRatingDeviation(45);
    currentPlayer.setLeaderboardRatingMean(100);

    instance.setUpIfNecessary();

    assertThat(instance.ratingProgressIndicator.isVisible(), is(true));
    assertThat(instance.ratingProgressIndicator.getProgress(), is(5d / 10d));
  }

  @Test
  public void testSetUpIfNecessaryFiresOnlyOnce() throws Exception {
    verifyZeroInteractions(playerService);
    instance.setUpIfNecessary();
    verify(playerService).currentPlayerProperty();
    verify(playerService).getCurrentPlayer();
    verify(playerService).currentPlayerProperty();
    instance.setUpIfNecessary();
    verifyNoMoreInteractions(playerService);
  }
}
