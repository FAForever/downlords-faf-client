package com.faforever.client.play;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.game.Faction;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.parsecom.CloudAccessor;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.user.UserService;
import com.google.api.client.json.JsonFactory;
import javafx.collections.ObservableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PlayServicesImplTest extends AbstractPlainJavaFxTest {

  private static final int PLAYER_ID = 123;
  private static final String USERNAME = "junit";
  @Rule
  public TemporaryFolder preferencesDirectory = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Mock
  private PlayServicesImpl instance;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private UserService userService;
  @Mock
  private CloudAccessor cloudAccessor;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private JsonFactory jsonFactory;
  @Mock
  private FafApiAccessor fafApiAccessor;
  @Captor
  private ArgumentCaptor<AchievementUpdatesRequest> achievementUpdatesRequestCaptor;
  @Captor
  private ArgumentCaptor<EventUpdatesRequest> eventUpdatesRequestCaptor;

  @Before
  public void setUp() throws Exception {
    instance = new PlayServicesImpl();
    instance.preferencesService = preferencesService;
    instance.userService = userService;
    instance.cloudAccessor = cloudAccessor;
    instance.notificationService = notificationService;
    instance.i18n = i18n;
    instance.fafApiAccessor = fafApiAccessor;

    when(userService.getUid()).thenReturn(PLAYER_ID);
    when(userService.getUsername()).thenReturn(USERNAME);

    doAnswer(invocation -> {
      EventUpdatesRequest request = invocation.getArgumentAt(0, EventUpdatesRequest.class);
      return Collections.nCopies(request.getUpdates().size(), new UpdatedEvent());
    }).when(fafApiAccessor).recordEvents(any(), anyInt());

    doAnswer(invocation -> {
      AchievementUpdatesRequest request = invocation.getArgumentAt(0, AchievementUpdatesRequest.class);
      return Collections.nCopies(request.getUpdates().size(), new UpdatedAchievement());
    }).when(fafApiAccessor).executeAchievementUpdates(any(), anyInt());
  }

  @Test
  public void testAuthorize() throws Exception {
    instance.authorize();
    verify(fafApiAccessor).authorize(PLAYER_ID);
  }

  @Test
  public void testStartBatchUpdate() throws Exception {
    // Does it smoke?
    instance.startBatchUpdate();
    instance.startBatchUpdate();
  }

  @Test
  public void testExecuteBatchUpdateNotStartedThrowsException() throws Exception {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Batch update has not been started");
    instance.executeBatchUpdate();
  }

  @Test
  public void testCustomGamePlayed() throws Exception {
    executeInBatchUpdate(instance::customGamePlayed);
    assertEventRecorded(PlayServicesImpl.EVENT_CUSTOM_GAMES_PLAYED, 1);
  }

  private void executeInBatchUpdate(Runnable runnable) throws Exception {
    instance.startBatchUpdate();
    runnable.run();
    instance.executeBatchUpdate();
  }

  private void assertEventRecorded(String eventId, long count) {
    Collection<EventUpdate> updates = getEventUpdates();

    EventUpdate eventUpdate = null;
    for (EventUpdate update : updates) {
      if (eventId.equals(update.getEventId())) {
        eventUpdate = update;
        break;
      }
    }
    assertNotNull("Event " + eventId + " has not been recorded", eventUpdate);
    assertThat(eventUpdate.getEventId(), is(eventId));
    assertThat(eventUpdate.getCount(), is(count));
    verify(fafApiAccessor).recordEvents(any(), eq(PLAYER_ID));
  }

  private Collection<EventUpdate> getEventUpdates() {
    verify(fafApiAccessor).recordEvents(eventUpdatesRequestCaptor.capture(), eq(PLAYER_ID));
    return eventUpdatesRequestCaptor.getValue().getUpdates();
  }

  @Test
  public void testRanked1v1GamePlayed() throws Exception {
    executeInBatchUpdate(instance::ranked1v1GamePlayed);
    assertEventRecorded(PlayServicesImpl.EVENT_RANKED_1V1_GAMES_PLAYED, 1);
  }

  @Test
  public void testRanked1v1GameWon() throws Exception {
    executeInBatchUpdate(instance::ranked1v1GameWon);
    assertAchievementUnlocked(PlayServicesImpl.ACH_FIRST_SUCCESS);
  }

  private void assertAchievementUnlocked(String achievementId) {
    Collection<AchievementUpdate> updates = getAchievementUpdates();

    AchievementUpdate achievementUpdate = null;
    for (AchievementUpdate update : updates) {
      if (achievementId.equals(update.getAchievementId())) {
        achievementUpdate = update;
        break;
      }
    }
    assertNotNull("Achievement " + achievementId + " has not been recorded", achievementUpdate);
    assertThat(achievementUpdate.getAchievementId(), is(achievementId));
    assertThat(achievementUpdate.getSteps(), nullValue());
    verifyAchievementsUpdated();
  }

  private Collection<AchievementUpdate> getAchievementUpdates() {
    verify(fafApiAccessor).executeAchievementUpdates(achievementUpdatesRequestCaptor.capture(), eq(PLAYER_ID));
    return achievementUpdatesRequestCaptor.getValue().getUpdates();
  }

  private void verifyAchievementsUpdated() {
    verify(fafApiAccessor).executeAchievementUpdates(any(), eq(PLAYER_ID));
    verify(fafApiAccessor).getPlayerAchievements(PLAYER_ID);
  }

  @Test
  public void testKilledCommandersIncrementsDontMessWithMe() throws Exception {
    executeInBatchUpdate(() -> instance.killedCommanders(2, true));
    assertAchievementIncremented(PlayServicesImpl.ACH_DONT_MESS_WITH_ME, 2);
  }

  private void assertAchievementIncremented(String achievementId, int steps) {
    Collection<AchievementUpdate> updates = getAchievementUpdates();

    AchievementUpdate achievementUpdate = null;
    for (AchievementUpdate update : updates) {
      if (achievementId.equals(update.getAchievementId())) {
        achievementUpdate = update;
        break;
      }
    }
    assertNotNull("Achievement " + achievementId + " has not been recorded", achievementUpdate);
    assertThat(achievementUpdate.getUpdateType(), is(AchievementUpdateType.INCREMENT));
    assertThat(achievementUpdate.getAchievementId(), is(achievementId));
    assertThat(achievementUpdate.getSteps(), is(steps));
    verifyAchievementsUpdated();
  }

  @Test
  public void testKilledCommandersThreeUnlocksHattrick() throws Exception {
    executeInBatchUpdate(() -> instance.killedCommanders(3, true));
    assertAchievementUnlocked(PlayServicesImpl.ACH_HATTRICK);
  }

  @Test
  public void testKilledCommandersDiedRecordsFallenAcus() throws Exception {
    executeInBatchUpdate(() -> instance.killedCommanders(2, false));
    assertEventRecorded(PlayServicesImpl.EVENT_FALLEN_ACUS, 1L);
    assertAchievementIncremented(PlayServicesImpl.ACH_DONT_MESS_WITH_ME, 2);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testAcuDamageReceivedDoesntUnlockBelow12000() throws Exception {
    executeInBatchUpdate(() -> instance.acuDamageReceived(11_999, true));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testAcuDamageReceivedUnlocksAt12000() throws Exception {
    executeInBatchUpdate(() -> instance.acuDamageReceived(12_000, true));
    assertAchievementUnlocked(PlayServicesImpl.ACH_THAT_WAS_CLOSE);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testAcuDamageReceivedUnlocksAbove12000() throws Exception {
    executeInBatchUpdate(() -> instance.acuDamageReceived(12_001, true));
    assertAchievementUnlocked(PlayServicesImpl.ACH_THAT_WAS_CLOSE);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testTopScoringPlayerDoesNothingWhenBelow8Players() throws Exception {
    executeInBatchUpdate(() -> instance.topScoringPlayer(7));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testTopScoringPlayerUnlocksAndIncrementsWhen8Players() throws Exception {
    executeInBatchUpdate(() -> instance.topScoringPlayer(8));
    assertAchievementUnlocked(PlayServicesImpl.ACH_TOP_SCORE);
    assertAchievementIncremented(PlayServicesImpl.ACH_UNBEATABLE, 1);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testTopScoringPlayerUnlocksAndIncrementsWhenMoreThan8Players() throws Exception {
    executeInBatchUpdate(() -> instance.topScoringPlayer(9));
    assertAchievementUnlocked(PlayServicesImpl.ACH_TOP_SCORE);
    assertAchievementIncremented(PlayServicesImpl.ACH_UNBEATABLE, 1);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testGetPlayerAchievementsForCurrentUser() throws Exception {
    when(cloudAccessor.getPlayerIdForUsername(USERNAME)).thenReturn(completedFuture(String.valueOf(PLAYER_ID)));
    instance.getPlayerAchievements(USERNAME);
    verifyZeroInteractions(cloudAccessor);
  }

  @Test
  public void testGetPlayerAchievementsForAnotherUser() throws Exception {
    List<PlayerAchievement> achievements = Arrays.asList(new PlayerAchievement(), new PlayerAchievement());
    when(cloudAccessor.getPlayerIdForUsername("foobar")).thenReturn(completedFuture(String.valueOf(123)));
    when(fafApiAccessor.getPlayerAchievements(123)).thenReturn(achievements);

    ObservableList<PlayerAchievement> result = instance.getPlayerAchievements("foobar");

    assertThat(result, hasSize(2));
    assertThat(result, is(achievements));
    verify(cloudAccessor).getPlayerIdForUsername("foobar");
    verify(fafApiAccessor).getPlayerAchievements(123);
  }

  @Test
  public void testGetAchievementDefinitions() throws Exception {
    instance.getAchievementDefinitions();
    verify(fafApiAccessor).getAchievementDefinitions();
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testPlayerRating1v1() throws Exception {
    executeInBatchUpdate(() -> instance.playerRating1v1(1234));
    assertAchievementSetStepsAtLeast(PlayServicesImpl.ACH_PRIVATE, 1234);
    assertAchievementSetStepsAtLeast(PlayServicesImpl.ACH_CORPORAL, 1234);
    assertAchievementSetStepsAtLeast(PlayServicesImpl.ACH_SERGEANT_MAJOR, 1234);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  private void assertAchievementSetStepsAtLeast(String achievementId, int steps) {
    Collection<AchievementUpdate> updates = getAchievementUpdates();

    AchievementUpdate achievementUpdate = null;
    for (AchievementUpdate update : updates) {
      if (achievementId.equals(update.getAchievementId())) {
        achievementUpdate = update;
        break;
      }
    }
    assertNotNull("Achievement " + achievementId + " has not been recorded", achievementUpdate);
    assertThat(achievementUpdate.getUpdateType(), is(AchievementUpdateType.SET_STEPS_AT_LEAST));
    assertThat(achievementUpdate.getAchievementId(), is(achievementId));
    assertThat(achievementUpdate.getSteps(), is(steps));
    verifyAchievementsUpdated();
  }

  @Test
  public void testPlayerRatingGlobalOf499DoesntDoAnything() throws Exception {
    executeInBatchUpdate(() -> instance.playerRatingGlobal(1234));
    assertAchievementSetStepsAtLeast(PlayServicesImpl.ACH_GETTING_STARTED, 1234);
    assertAchievementSetStepsAtLeast(PlayServicesImpl.ACH_GETTING_BETTER, 1234);
    assertAchievementSetStepsAtLeast(PlayServicesImpl.ACH_GETTING_PRO, 1234);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testWonWithinDuration15MinutesTooSlow() throws Exception {
    executeInBatchUpdate(() -> instance.wonWithinDuration(Duration.ofMinutes(15)));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testWonWithinDuration15MinutesFastEnough() throws Exception {
    executeInBatchUpdate(() -> instance.wonWithinDuration(Duration.ofMinutes(15).minusSeconds(1)));
    assertAchievementUnlocked(PlayServicesImpl.ACH_RUSHER);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testFactionPlayedAeonDied() throws Exception {
    executeInBatchUpdate(() -> instance.factionPlayed(Faction.AEON, false));
    assertEventRecorded(PlayServicesImpl.EVENT_AEON_PLAYS, 1);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testFactionPlayedCybranDied() throws Exception {
    executeInBatchUpdate(() -> instance.factionPlayed(Faction.CYBRAN, false));
    assertEventRecorded(PlayServicesImpl.EVENT_CYBRAN_PLAYS, 1);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testFactionPlayedUefDied() throws Exception {
    executeInBatchUpdate(() -> instance.factionPlayed(Faction.UEF, false));
    assertEventRecorded(PlayServicesImpl.EVENT_UEF_PLAYS, 1);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testFactionPlayedSeraphimDied() throws Exception {
    executeInBatchUpdate(() -> instance.factionPlayed(Faction.SERAPHIM, false));
    assertEventRecorded(PlayServicesImpl.EVENT_SERAPHIM_PLAYS, 1);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testFactionPlayedAeonSurvived() throws Exception {
    executeInBatchUpdate(() -> instance.factionPlayed(Faction.AEON, true));
    assertEventRecorded(PlayServicesImpl.EVENT_AEON_PLAYS, 1);
    assertEventRecorded(PlayServicesImpl.EVENT_AEON_WINS, 1);
    verifyAchievementsUpdated();
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testFactionPlayedCybranSurvived() throws Exception {
    executeInBatchUpdate(() -> instance.factionPlayed(Faction.CYBRAN, true));
    assertEventRecorded(PlayServicesImpl.EVENT_CYBRAN_PLAYS, 1);
    assertEventRecorded(PlayServicesImpl.EVENT_CYBRAN_WINS, 1);
    verifyAchievementsUpdated();
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testFactionPlayedUefSurvived() throws Exception {
    executeInBatchUpdate(() -> instance.factionPlayed(Faction.UEF, true));
    assertEventRecorded(PlayServicesImpl.EVENT_UEF_PLAYS, 1);
    assertEventRecorded(PlayServicesImpl.EVENT_UEF_WINS, 1);
    verifyAchievementsUpdated();
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testFactionPlayedSeraphimSurvived() throws Exception {
    executeInBatchUpdate(() -> instance.factionPlayed(Faction.SERAPHIM, true));
    assertEventRecorded(PlayServicesImpl.EVENT_SERAPHIM_PLAYS, 1);
    assertEventRecorded(PlayServicesImpl.EVENT_SERAPHIM_WINS, 1);
    verifyAchievementsUpdated();
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testUnitStatsDiedRecordsAllEventsButNoMore() throws Exception {
    executeInBatchUpdate(() -> instance.unitStats(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, false));
    assertEventRecorded(PlayServicesImpl.EVENT_BUILT_AIR_UNITS, 1);
    assertEventRecorded(PlayServicesImpl.EVENT_FALLEN_AIR_UNITS, 2);
    assertEventRecorded(PlayServicesImpl.EVENT_BUILT_LAND_UNITS, 3);
    assertEventRecorded(PlayServicesImpl.EVENT_FALLEN_LAND_UNITS, 4);
    assertEventRecorded(PlayServicesImpl.EVENT_BUILT_NAVAL_UNITS, 5);
    assertEventRecorded(PlayServicesImpl.EVENT_FALLEN_NAVAL_UNITS, 6);
    assertEventRecorded(PlayServicesImpl.EVENT_BUILT_TECH_1_UNITS, 7);
    assertEventRecorded(PlayServicesImpl.EVENT_FALLEN_TECH_1_UNITS, 8);
    assertEventRecorded(PlayServicesImpl.EVENT_BUILT_TECH_2_UNITS, 9);
    assertEventRecorded(PlayServicesImpl.EVENT_FALLEN_TECH_2_UNITS, 10);
    assertEventRecorded(PlayServicesImpl.EVENT_BUILT_TECH_3_UNITS, 11);
    assertEventRecorded(PlayServicesImpl.EVENT_FALLEN_TECH_3_UNITS, 12);
    assertEventRecorded(PlayServicesImpl.EVENT_BUILT_EXPERIMENTALS, 13);
    assertEventRecorded(PlayServicesImpl.EVENT_FALLEN_EXPERIMENTALS, 14);
    assertEventRecorded(PlayServicesImpl.EVENT_BUILT_ENGINEERS, 15);
    assertEventRecorded(PlayServicesImpl.EVENT_FALLEN_ENGINEERS, 16);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testUnitStatsSurvivedRecordsAllEvents() throws Exception {
    executeInBatchUpdate(() -> instance.unitStats(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, true));
    assertEventRecorded(PlayServicesImpl.EVENT_BUILT_AIR_UNITS, 1);
    assertEventRecorded(PlayServicesImpl.EVENT_FALLEN_AIR_UNITS, 2);
    assertEventRecorded(PlayServicesImpl.EVENT_BUILT_LAND_UNITS, 3);
    assertEventRecorded(PlayServicesImpl.EVENT_FALLEN_LAND_UNITS, 4);
    assertEventRecorded(PlayServicesImpl.EVENT_BUILT_NAVAL_UNITS, 5);
    assertEventRecorded(PlayServicesImpl.EVENT_FALLEN_NAVAL_UNITS, 6);
    assertEventRecorded(PlayServicesImpl.EVENT_BUILT_TECH_1_UNITS, 7);
    assertEventRecorded(PlayServicesImpl.EVENT_FALLEN_TECH_1_UNITS, 8);
    assertEventRecorded(PlayServicesImpl.EVENT_BUILT_TECH_2_UNITS, 9);
    assertEventRecorded(PlayServicesImpl.EVENT_FALLEN_TECH_2_UNITS, 10);
    assertEventRecorded(PlayServicesImpl.EVENT_BUILT_TECH_3_UNITS, 11);
    assertEventRecorded(PlayServicesImpl.EVENT_FALLEN_TECH_3_UNITS, 12);
    assertEventRecorded(PlayServicesImpl.EVENT_BUILT_EXPERIMENTALS, 13);
    assertEventRecorded(PlayServicesImpl.EVENT_FALLEN_EXPERIMENTALS, 14);
    assertEventRecorded(PlayServicesImpl.EVENT_BUILT_ENGINEERS, 15);
    assertEventRecorded(PlayServicesImpl.EVENT_FALLEN_ENGINEERS, 16);
  }

  @Test
  public void testUnitStatsSurvivedAirDominant() throws Exception {
    executeInBatchUpdate(() -> instance.unitStats(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, true));
    assertAchievementIncremented(PlayServicesImpl.ACH_WRIGHT_BROTHER, 1);
    assertAchievementIncremented(PlayServicesImpl.ACH_WINGMAN, 1);
    assertAchievementIncremented(PlayServicesImpl.ACH_KING_OF_THE_SKIES, 1);
  }

  @Test
  public void testUnitStatsSurvivedLandDominant() throws Exception {
    executeInBatchUpdate(() -> instance.unitStats(0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, true));
    assertAchievementIncremented(PlayServicesImpl.ACH_MILITIAMAN, 1);
    assertAchievementIncremented(PlayServicesImpl.ACH_GRENADIER, 1);
    assertAchievementIncremented(PlayServicesImpl.ACH_FIELD_MARSHAL, 1);
  }

  @Test
  public void testUnitStatsSurvivedNavalDominant() throws Exception {
    executeInBatchUpdate(() -> instance.unitStats(0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, true));
    assertAchievementIncremented(PlayServicesImpl.ACH_LANDLUBBER, 1);
    assertAchievementIncremented(PlayServicesImpl.ACH_SEAMAN, 1);
    assertAchievementIncremented(PlayServicesImpl.ACH_ADMIRAL_OF_THE_FLEET, 1);
  }

  @Test
  public void testUnitStatsSurvivedWithManyExperimentals() throws Exception {
    executeInBatchUpdate(() -> instance.unitStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, true));
    assertAchievementIncremented(PlayServicesImpl.ACH_TECHIE, 1);
    assertAchievementIncremented(PlayServicesImpl.ACH_I_LOVE_BIG_TOYS, 1);
    assertAchievementIncremented(PlayServicesImpl.ACH_EXPERIMENTALIST, 1);
    assertAchievementIncremented(PlayServicesImpl.ACH_DR_EVIL, 3);
  }

  @Test
  public void testUnitStatsSurvivedWithFewExperimentals() throws Exception {
    executeInBatchUpdate(() -> instance.unitStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, true));
    assertAchievementIncremented(PlayServicesImpl.ACH_DR_EVIL, 2);
  }

  @Test
  public void testTimePlayed() throws Exception {
    executeInBatchUpdate(() -> instance.timePlayed(Duration.ofMinutes(5), true));
    assertEventRecorded(PlayServicesImpl.EVENT_SECONDS_PLAYED, 300);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testAsfBuiltLessThan150DoesNotUnlockAchievement() throws Exception {
    executeInBatchUpdate(() -> instance.asfBuilt(149));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testAsfBuiltMore150UnlocksAchievement() throws Exception {
    executeInBatchUpdate(() -> instance.asfBuilt(150));
    assertAchievementUnlocked(PlayServicesImpl.ACH_WHAT_A_SWARM);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testAsfBuiltMoreThan150UnlocksAchievement() throws Exception {
    executeInBatchUpdate(() -> instance.asfBuilt(151));
    assertAchievementUnlocked(PlayServicesImpl.ACH_WHAT_A_SWARM);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltTransportsNone() throws Exception {
    executeInBatchUpdate(() -> instance.builtTransports(0));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltTransportsSome() throws Exception {
    executeInBatchUpdate(() -> instance.builtTransports(5));
    assertAchievementIncremented(PlayServicesImpl.ACH_THE_TRANSPORTER, 5);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltParagonsNoneButSurvived() throws Exception {
    executeInBatchUpdate(() -> instance.builtParagons(0, true));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltParagonsNoneAndDied() throws Exception {
    executeInBatchUpdate(() -> instance.builtParagons(0, false));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltParagonsOneButDied() throws Exception {
    executeInBatchUpdate(() -> instance.builtParagons(1, false));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltParagonsOneAndSurvived() throws Exception {
    executeInBatchUpdate(() -> instance.builtParagons(1, true));
    assertAchievementUnlocked(PlayServicesImpl.ACH_SO_MUCH_RESOURCES);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltYolonaOssNoneButSurvived() throws Exception {
    executeInBatchUpdate(() -> instance.builtYolonaOss(0, true));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltYolonaOssNoneAndDied() throws Exception {
    executeInBatchUpdate(() -> instance.builtYolonaOss(0, false));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltYolonaOssOneButDied() throws Exception {
    executeInBatchUpdate(() -> instance.builtYolonaOss(1, false));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltYolonaOssOneAndSurvived() throws Exception {
    executeInBatchUpdate(() -> instance.builtYolonaOss(1, true));
    assertAchievementUnlocked(PlayServicesImpl.ACH_NUCLEAR_WAR);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltScathisNoneButSurvived() throws Exception {
    executeInBatchUpdate(() -> instance.builtScathis(0, true));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltScathisNoneAndDied() throws Exception {
    executeInBatchUpdate(() -> instance.builtScathis(0, false));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltScathisOneButDied() throws Exception {
    executeInBatchUpdate(() -> instance.builtScathis(1, false));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltScathisOneAndSurvived() throws Exception {
    executeInBatchUpdate(() -> instance.builtScathis(1, true));
    assertAchievementUnlocked(PlayServicesImpl.ACH_MAKE_IT_HAIL);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltSalvationsNoneButSurvived() throws Exception {
    executeInBatchUpdate(() -> instance.builtSalvations(0, true));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltSalvationsNoneAndDied() throws Exception {
    executeInBatchUpdate(() -> instance.builtSalvations(0, false));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltSalvationsOneButDied() throws Exception {
    executeInBatchUpdate(() -> instance.builtSalvations(1, false));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltSalvationsOneAndSurvived() throws Exception {
    executeInBatchUpdate(() -> instance.builtSalvations(1, true));
    assertAchievementUnlocked(PlayServicesImpl.ACH_RAINMAKER);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltMavorsNoneButSurvived() throws Exception {
    executeInBatchUpdate(() -> instance.builtMavors(0, true));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltMavorsNoneAndDied() throws Exception {
    executeInBatchUpdate(() -> instance.builtMavors(0, false));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltMavorsOneButDied() throws Exception {
    executeInBatchUpdate(() -> instance.builtMavors(1, false));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltMavorsOneAndSurvived() throws Exception {
    executeInBatchUpdate(() -> instance.builtMavors(1, true));
    assertAchievementUnlocked(PlayServicesImpl.ACH_I_HAVE_A_CANON);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltAtlantis() throws Exception {
    executeInBatchUpdate(() -> instance.builtAtlantis(2));
    assertAchievementIncremented(PlayServicesImpl.ACH_IT_AINT_A_CITY, 2);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltTempests() throws Exception {
    executeInBatchUpdate(() -> instance.builtTempests(2));
    assertAchievementIncremented(PlayServicesImpl.ACH_STORMY_SEA, 2);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltCzars() throws Exception {
    executeInBatchUpdate(() -> instance.builtCzars(2));
    assertAchievementIncremented(PlayServicesImpl.ACH_DEATH_FROM_ABOVE, 2);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltAhwasshas() throws Exception {
    executeInBatchUpdate(() -> instance.builtAhwasshas(2));
    assertAchievementIncremented(PlayServicesImpl.ACH_ASS_WASHER, 2);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltYthothas() throws Exception {
    executeInBatchUpdate(() -> instance.builtYthothas(2));
    assertAchievementIncremented(PlayServicesImpl.ACH_ALIEN_INVASION, 2);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltFatboys() throws Exception {
    executeInBatchUpdate(() -> instance.builtFatboys(2));
    assertAchievementIncremented(PlayServicesImpl.ACH_FATTER_IS_BETTER, 2);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltMonkeylords() throws Exception {
    executeInBatchUpdate(() -> instance.builtMonkeylords(2));
    assertAchievementIncremented(PlayServicesImpl.ACH_ARACHNOLOGIST, 2);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltGalacticColossus() throws Exception {
    executeInBatchUpdate(() -> instance.builtGalacticColossus(2));
    assertAchievementIncremented(PlayServicesImpl.ACH_INCOMING_ROBOTS, 2);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltSoulRippers() throws Exception {
    executeInBatchUpdate(() -> instance.builtSoulRippers(2));
    assertAchievementIncremented(PlayServicesImpl.ACH_FLYING_DEATH, 2);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltMercies() throws Exception {
    executeInBatchUpdate(() -> instance.builtMercies(2));
    assertAchievementIncremented(PlayServicesImpl.ACH_NO_MERCY, 2);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltFireBeetles() throws Exception {
    executeInBatchUpdate(() -> instance.builtFireBeetles(2));
    assertAchievementIncremented(PlayServicesImpl.ACH_DEADLY_BUGS, 2);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltSupportCommandersLessThan10DoesNotUnlockAchievement() throws Exception {
    executeInBatchUpdate(() -> instance.builtSupportCommanders(9));
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltSupportCommandersUnlocksAchievement() throws Exception {
    executeInBatchUpdate(() -> instance.builtSupportCommanders(10));
    assertAchievementUnlocked(PlayServicesImpl.ACH_WHO_NEEDS_SUPPORT);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testBuiltMegaliths() throws Exception {
    executeInBatchUpdate(() -> instance.builtMegaliths(2));
    assertAchievementIncremented(PlayServicesImpl.ACH_HOLY_CRAB, 2);
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test
  public void testNumberOfGamesPlayed() throws Exception {
    executeInBatchUpdate(() -> instance.numberOfGamesPlayed(2));
    assertAchievementSetStepsAtLeast(PlayServicesImpl.ACH_NOVICE, 2);
    assertAchievementSetStepsAtLeast(PlayServicesImpl.ACH_JUNIOR, 2);
    assertAchievementSetStepsAtLeast(PlayServicesImpl.ACH_SENIOR, 2);
    assertAchievementSetStepsAtLeast(PlayServicesImpl.ACH_VETERAN, 2);
    assertAchievementSetStepsAtLeast(PlayServicesImpl.ACH_ADDICT, 2);
    verifyNoMoreInteractions(fafApiAccessor);
  }
}
