package com.faforever.client.stats;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.events.AchievementDefinition;
import com.faforever.client.events.PlayerAchievement;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.UpdatedAchievement;
import com.faforever.client.legacy.UpdatedAchievementsInfo;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class AchievementServiceImplTest extends AbstractPlainJavaFxTest {

  private static final int PLAYER_ID = 123;
  private static final String USERNAME = "junit";
  @Rule
  public TemporaryFolder preferencesDirectory = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Mock
  private AchievementServiceImpl instance;
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
  @Mock
  private ExecutorService executorService;
  @Mock
  private LobbyServerAccessor lobbyServerAccessor;

  @Captor
  private ArgumentCaptor<Consumer<UpdatedAchievementsInfo>> onUpdatedAchievementsCaptor;

  @Before
  public void setUp() throws Exception {
    instance = new AchievementServiceImpl();
    instance.userService = userService;
    instance.cloudAccessor = cloudAccessor;
    instance.notificationService = notificationService;
    instance.i18n = i18n;
    instance.fafApiAccessor = fafApiAccessor;
    instance.lobbyServerAccessor = lobbyServerAccessor;

    when(userService.getUid()).thenReturn(PLAYER_ID);
    when(userService.getUsername()).thenReturn(USERNAME);

    doAnswer(invocation -> {
      invocation.getArgumentAt(0, Runnable.class).run();
      return null;
    }).when(executorService).execute(any(Runnable.class));

    instance.postConstruct();
  }

  @Test
  public void testOnUpdatedAchievementsNewlyUnlockedTriggersNotification() {
    verify(lobbyServerAccessor).addOnUpdatedAchievementsInfoListener(onUpdatedAchievementsCaptor.capture());
    Consumer<UpdatedAchievementsInfo> listener = onUpdatedAchievementsCaptor.getValue();

    AchievementDefinition achievementDefinition = new AchievementDefinition();
    achievementDefinition.setUnlockedIconUrl(getClass().getResource("/images/tray_icon.png").toExternalForm());
    when(fafApiAccessor.getAchievementDefinition("123")).thenReturn(achievementDefinition);

    UpdatedAchievementsInfo updatedAchievementsInfo = new UpdatedAchievementsInfo();
    UpdatedAchievement updatedAchievement = new UpdatedAchievement();
    updatedAchievement.setAchievementId("123");
    updatedAchievement.setNewlyUnlocked(true);

    updatedAchievementsInfo.setUpdatedAchievements(Collections.singletonList(updatedAchievement));
    listener.accept(updatedAchievementsInfo);

    verify(notificationService).addNotification(any(TransientNotification.class));
  }

  @Test
  public void testOnUpdatedAchievementsAlreadyUnlockedDoesntTriggerNotification() {
    verify(lobbyServerAccessor).addOnUpdatedAchievementsInfoListener(onUpdatedAchievementsCaptor.capture());
    Consumer<UpdatedAchievementsInfo> listener = onUpdatedAchievementsCaptor.getValue();

    UpdatedAchievementsInfo updatedAchievementsInfo = new UpdatedAchievementsInfo();
    updatedAchievementsInfo.setUpdatedAchievements(Collections.singletonList(new UpdatedAchievement()));
    listener.accept(updatedAchievementsInfo);

    verifyZeroInteractions(notificationService);
    verifyZeroInteractions(fafApiAccessor);
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
}
