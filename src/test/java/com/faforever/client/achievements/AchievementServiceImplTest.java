package com.faforever.client.achievements;

import com.faforever.client.achievements.AchievementService.AchievementState;
import com.faforever.client.api.AchievementDefinition;
import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.PlayerAchievement;
import com.faforever.client.i18n.I18n;
import com.faforever.client.io.ByteCopier;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerInfoBeanBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.UpdatedAchievementsMessage;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.user.UserService;
import com.google.api.client.json.JsonFactory;
import javafx.scene.image.Image;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.BufferedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.faforever.client.achievements.AchievementService.AchievementState.HIDDEN;
import static com.faforever.client.achievements.AchievementService.AchievementState.REVEALED;
import static com.faforever.client.achievements.AchievementService.AchievementState.UNLOCKED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
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
  public TemporaryFolder cacheDirectory = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Mock
  PlayerService playerService;
  @Mock
  private AchievementServiceImpl instance;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private UserService userService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private JsonFactory jsonFactory;
  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private ThemeService themeService;
  @Mock
  private FafService fafService;
  @Mock
  private ThreadPoolExecutor threadPoolExecutor;
  @Captor
  private ArgumentCaptor<Consumer<UpdatedAchievementsMessage>> onUpdatedAchievementsCaptor;

  @Before
  public void setUp() throws Exception {
    instance = new AchievementServiceImpl();
    instance.userService = userService;
    instance.notificationService = notificationService;
    instance.i18n = i18n;
    instance.fafApiAccessor = fafApiAccessor;
    instance.fafService = fafService;
    instance.playerService = playerService;
    instance.threadPoolExecutor = threadPoolExecutor;
    instance.themeService = themeService;
    instance.preferencesService = preferencesService;

    when(userService.getUid()).thenReturn(PLAYER_ID);
    when(userService.getUsername()).thenReturn(USERNAME);
    when(preferencesService.getCacheDirectory()).thenReturn(cacheDirectory.getRoot().toPath());

    doAnswer(invocation -> {
      invocation.getArgumentAt(0, Runnable.class).run();
      return null;
    }).when(threadPoolExecutor).execute(any(Runnable.class));

    instance.postConstruct();
  }

  @Test
  public void testGetPlayerAchievementsForCurrentUser() throws Exception {
    instance.getPlayerAchievements(USERNAME);
    verifyZeroInteractions(playerService);
  }

  @Test
  public void testGetPlayerAchievementsForAnotherUser() throws Exception {
    List<PlayerAchievement> achievements = Arrays.asList(new PlayerAchievement(), new PlayerAchievement());
    when(playerService.getPlayerForUsername("foobar")).thenReturn(PlayerInfoBeanBuilder.create("foobar").id(PLAYER_ID).get());
    when(fafApiAccessor.getPlayerAchievements(PLAYER_ID)).thenReturn(achievements);

    List<PlayerAchievement> playerAchievements = instance.getPlayerAchievements("foobar").toCompletableFuture().get(5, TimeUnit.SECONDS);

    assertThat(playerAchievements, hasSize(2));
    assertThat(playerAchievements, is(achievements));
    verify(playerService).getPlayerForUsername("foobar");
    verify(fafApiAccessor).getPlayerAchievements(PLAYER_ID);
  }

  @Test
  public void testGetAchievementDefinitions() throws Exception {
    instance.getAchievementDefinitions();
    verify(fafApiAccessor).getAchievementDefinitions();
    verifyNoMoreInteractions(fafApiAccessor);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetHiddenThrowsUnsupportedOperationException() throws Exception {
    instance.getImage(null, HIDDEN);
  }

  @Test
  public void testGetPreviewNotExistsReturnsNull() throws Exception {
    AchievementDefinition achievementDefinition = AchievementDefinitionBuilder.create().defaultValues().get();
    for (AchievementState achievementState : EnumSet.of(REVEALED, UNLOCKED)) {
      Image image = instance.getImage(achievementDefinition, achievementState);
      assertNotNull(image);
      // Let's use impl_getUrl as long as it's there, later use reflection
      assertThat(image.impl_getUrl(), startsWith("http://127.0.0.1"));
    }
  }

  @Test
  public void testGetSmallPreviewCached() throws Exception {
    AchievementDefinition achievementDefinition = AchievementDefinitionBuilder.create().defaultValues().get();
    for (AchievementState achievementState : EnumSet.of(REVEALED, UNLOCKED)) {
      Path previewPath = cacheDirectory.getRoot().toPath()
          .resolve("achievements")
          .resolve(achievementState.name().toLowerCase())
          .resolve(achievementDefinition.getId() + ".png");
      Files.createDirectories(previewPath.getParent());
      try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(previewPath))) {
        ByteCopier.from(getClass().getResourceAsStream("/images/logo_transparent.png")).to(outputStream).copy();
      }
      Image image = instance.getImage(achievementDefinition, achievementState);
      assertNotNull(image);
      // Let's use impl_getUrl as long as it's there, later use reflection
      assertThat(image.impl_getUrl(), is(previewPath.toUri().toURL().toExternalForm()));
    }
  }
}
