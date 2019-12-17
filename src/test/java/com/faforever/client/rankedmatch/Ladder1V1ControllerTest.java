package com.faforever.client.rankedmatch;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.game.Faction;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardEntry;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Ladder1v1Prefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.event.MissingGamePathEvent;
import com.faforever.client.rankedmatch.MatchmakerMessage.MatchmakerQueue.QueueName;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ToggleButton;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Ladder1V1ControllerTest extends AbstractPlainJavaFxTest {

  private static final String USERNAME = "junit";
  private static final int PLAYER_ID = 123;
  @Mock
  private Ladder1v1Controller instance;
  @Mock
  private GameService gameService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private PlayerService playerService;
  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private I18n i18n;
  @Mock
  private BooleanProperty searching1v1Property;
  @Mock
  private Preferences preferences;
  @Mock
  private Ladder1v1Prefs ladder1V1Prefs;
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;

  @Mock
  private EventBus eventBus;

  private ObjectProperty<Player> currentPlayerProperty;
  private ObservableList<Faction> factionList;

  @Before
  public void setUp() throws Exception {
    instance = new Ladder1v1Controller(gameService, preferencesService, playerService, leaderboardService, i18n,
        new ClientProperties(), eventBus);

    Player player = new Player(USERNAME);
    player.setId(PLAYER_ID);
    currentPlayerProperty = new SimpleObjectProperty<>(player);
    factionList = FXCollections.observableArrayList();
    LeaderboardEntry leaderboardEntry = new LeaderboardEntry();
    leaderboardEntry.setRating(500);
    leaderboardEntry.setWinLossRatio(12.23f);
    leaderboardEntry.setRank(100);
    leaderboardEntry.setGamesPlayed(412);
    leaderboardEntry.setUsername(USERNAME);

    when(leaderboardService.getLadder1v1Stats()).thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
    when(leaderboardService.getEntryForPlayer(PLAYER_ID)).thenReturn(CompletableFuture.completedFuture(leaderboardEntry));
    when(gameService.searching1v1Property()).thenReturn(searching1v1Property);
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getLadder1v1Prefs()).thenReturn(ladder1V1Prefs);
    when(ladder1V1Prefs.getFactions()).thenReturn(factionList);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(playerService.getCurrentPlayer()).thenReturn(Optional.ofNullable(currentPlayerProperty.get()));
    when(playerService.currentPlayerProperty()).thenReturn(currentPlayerProperty);

    loadFxml("theme/play/ranked_1v1.fxml", clazz -> instance);
  }

  @After
  public void tearDown() throws Exception {
    instance.destroy();
  }

  @Test
  public void testPostConstructSelectsPreviousFactions() throws Exception {
    factionList.setAll(Faction.SERAPHIM, Faction.AEON);

    instance.initialize();

    assertThat(instance.aeonButton.isSelected(), is(true));
    assertThat(instance.seraphimButton.isSelected(), is(true));
    assertThat(instance.uefButton.isSelected(), is(false));
    assertThat(instance.cybranButton.isSelected(), is(false));
  }

  @Test
  public void testAllFactionButtonsMapped() throws Exception {
    assertThat(instance.factionsToButtons.keySet(), containsInAnyOrder(Faction.AEON, Faction.CYBRAN, Faction.UEF, Faction.SERAPHIM));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.ladder1v1Root));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testOnCancelButtonClicked() throws Exception {
    instance.onCancelButtonClicked();

    verify(gameService).stopSearchLadder1v1();
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
    when(forgedAlliancePrefs.getInstallationPath()).thenReturn(Paths.get("."));

    instance.aeonButton.setSelected(true);
    instance.onFactionButtonClicked();

    instance.onPlayButtonClicked();
    instance.setSearching(true);

    verify(gameService).startSearchLadder1v1(any());
    assertThat(instance.cancelButton.isVisible(), is(true));
    assertThat(instance.playButton.isVisible(), is(false));
    assertThat(instance.searchProgressIndicator.isVisible(), is(true));
    assertThat(instance.searchingForOpponentLabel.isVisible(), is(true));
    for (ToggleButton button : instance.factionsToButtons.values()) {
      assertThat(button.isDisable(), is(true));
    }
  }

  @Test
  public void testOnPlayButtonClickedWithNoGamePath() throws Exception {
    when(forgedAlliancePrefs.getInstallationPath()).thenReturn(null);

    instance.onPlayButtonClicked();

    verify(gameService, never()).startSearchLadder1v1(any());
    verify(eventBus).post(new MissingGamePathEvent(true));

    assertThat(instance.cancelButton.isVisible(), is(false));
    assertThat(instance.playButton.isVisible(), is(true));
    assertThat(instance.searchProgressIndicator.isVisible(), is(false));
    assertThat(instance.searchingForOpponentLabel.isVisible(), is(false));
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
  public void testQueuePopTime() throws TimeoutException {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Consumer<MatchmakerMessage>> listenerCaptor = ArgumentCaptor.forClass(Consumer.class);
    verify(gameService).addOnRankedMatchNotificationListener(listenerCaptor.capture());
    
    MatchmakerMessage message = new MatchmakerMessage();
    String timeString = DateTimeFormatter.ISO_INSTANT.format(Instant.now().plusSeconds(65));
    message.setQueues(List.of(new MatchmakerMessage.MatchmakerQueue(QueueName.LADDER_1V1, timeString, null, null)));
    
    listenerCaptor.getValue().accept(message);
    WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS, () -> instance.timeUntilQueuePopLabel.isVisible());
    verify(i18n, atLeast(1)).get(any(), eq(1L), anyInt());
    WaitForAsyncUtils.waitForFxEvents();
  }
}
