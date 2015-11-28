package com.faforever.client.chat;

import com.faforever.client.achievements.AchievementDefinitionBuilder;
import com.faforever.client.achievements.AchievementItemController;
import com.faforever.client.achievements.AchievementService;
import com.faforever.client.achievements.PlayerAchievementBuilder;
import com.faforever.client.api.AchievementState;
import com.faforever.client.events.EventService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.player.PlayerInfoBeanBuilder;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.stats.PlayerStatisticsMessageLobby;
import com.faforever.client.stats.RatingInfo;
import com.faforever.client.stats.StatisticsService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.collections.FXCollections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserInfoWindowControllerTest extends AbstractPlainJavaFxTest {

  private static final String PLAYER_NAME = "junit";
  private UserInfoWindowController instance;
  private Locale locale = Locale.US;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private I18n i18n;
  @Mock
  private StatisticsService statisticsService;
  @Mock
  private AchievementService achievementService;
  @Mock
  private EventService eventService;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private Preferences preferences;
  @Mock
  private AchievementItemController achievementItemController;

  @Before
  public void setUp() throws Exception {
    instance = loadController("user_info_window.fxml");
    instance.countryFlagService = countryFlagService;
    instance.locale = locale;
    instance.i18n = i18n;
    instance.statisticsService = statisticsService;
    instance.achievementService = achievementService;
    instance.eventService = eventService;
    instance.applicationContext = applicationContext;
    instance.preferencesService = preferencesService;

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getTheme()).thenReturn("default");

    PlayerStatisticsMessageLobby playerStatisticsMessage = new PlayerStatisticsMessageLobby();
    playerStatisticsMessage.setPlayer(PLAYER_NAME);
    playerStatisticsMessage.setValues(asList(
        new RatingInfo(LocalDate.now(), 1500, 200, LocalTime.now()),
        new RatingInfo(LocalDate.now(), 1500, 200, LocalTime.now()))
    );
    when(statisticsService.getStatisticsForPlayer(StatisticsType.GLOBAL_90_DAYS, PLAYER_NAME))
        .thenReturn(CompletableFuture.completedFuture(playerStatisticsMessage));
    when(statisticsService.getStatisticsForPlayer(StatisticsType.GLOBAL_365_DAYS, PLAYER_NAME))
        .thenReturn(CompletableFuture.completedFuture(playerStatisticsMessage));
  }

  @Test
  public void testSetPlayerInfoBeanNoAchievementUnlocked() throws Exception {
    when(achievementService.getAchievementDefinitions()).thenReturn(CompletableFuture.completedFuture(asList(
        AchievementDefinitionBuilder.create().defaultValues().get()
    )));
    when(applicationContext.getBean(AchievementItemController.class)).thenReturn(achievementItemController);
    when(achievementService.getPlayerAchievements(PLAYER_NAME)).thenReturn(FXCollections.observableArrayList(
        PlayerAchievementBuilder.create().defaultValues().get()
    ));
    when(eventService.getPlayerEvents(PLAYER_NAME)).thenReturn(CompletableFuture.completedFuture(new HashMap<>()));

    instance.setPlayerInfoBean(PlayerInfoBeanBuilder.create(PLAYER_NAME).get());

    verify(achievementService).getAchievementDefinitions();
    verify(achievementService).getPlayerAchievements(PLAYER_NAME);
    verify(eventService).getPlayerEvents(PLAYER_NAME);

    assertThat(instance.mostRecentAchievementPane.isVisible(), is(false));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.userInfoRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testOnRatingOver90DaysButtonClicked() throws Exception {
    testSetPlayerInfoBean();
    instance.onRatingOver90DaysButtonClicked();
    verify(statisticsService, times(2)).getStatisticsForPlayer(StatisticsType.GLOBAL_90_DAYS, PLAYER_NAME);
  }

  @Test
  public void testSetPlayerInfoBean() throws Exception {
    when(achievementService.getAchievementDefinitions()).thenReturn(CompletableFuture.completedFuture(asList(
        AchievementDefinitionBuilder.create().id("foo-bar").get(),
        AchievementDefinitionBuilder.create().defaultValues().get()
    )));
    when(applicationContext.getBean(AchievementItemController.class)).thenReturn(achievementItemController);
    when(achievementService.getPlayerAchievements(PLAYER_NAME)).thenReturn(FXCollections.observableArrayList(
        PlayerAchievementBuilder.create().defaultValues().achievementId("foo-bar").state(AchievementState.UNLOCKED).get(),
        PlayerAchievementBuilder.create().defaultValues().get()
    ));
    when(eventService.getPlayerEvents(PLAYER_NAME)).thenReturn(CompletableFuture.completedFuture(new HashMap<>()));

    instance.setPlayerInfoBean(PlayerInfoBeanBuilder.create(PLAYER_NAME).get());

    verify(achievementService).getAchievementDefinitions();
    verify(achievementService).getPlayerAchievements(PLAYER_NAME);
    verify(eventService).getPlayerEvents(PLAYER_NAME);

    assertThat(instance.mostRecentAchievementPane.isVisible(), is(true));
  }

  @Test
  public void testOnRatingOver365DaysButtonClicked() throws Exception {
    testSetPlayerInfoBean();
    instance.onRatingOver365DaysButtonClicked();
    verify(statisticsService).getStatisticsForPlayer(StatisticsType.GLOBAL_365_DAYS, PLAYER_NAME);
  }
}
