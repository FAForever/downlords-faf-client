package com.faforever.client.achievements;

import com.faforever.client.achievements.AchievementService.AchievementState;
import com.faforever.client.api.AchievementDefinition;
import com.faforever.client.api.AchievementType;
import com.faforever.client.api.PlayerAchievement;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.faforever.client.theme.UiService.DEFAULT_ACHIEVEMENT_IMAGE;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

public class AchievementItemControllerTest extends AbstractPlainJavaFxTest {

  private AchievementItemController instance;

  @Mock
  private I18n i18n;
  @Mock
  private AchievementService achievementService;

  @Before
  public void setUp() throws Exception {
    instance = new AchievementItemController(i18n, achievementService);
    when(i18n.number(anyInt())).thenAnswer(invocation -> String.format("%d", (int) invocation.getArgument(0)));

    loadFxml("theme/achievement_item.fxml", clazz -> instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.achievementItemRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testSetAchievementDefinition() throws Exception {
    AchievementDefinition achievementDefinition = AchievementDefinitionBuilder.create().defaultValues().get();
    when(achievementService.getImage(achievementDefinition, AchievementState.REVEALED)).thenReturn(new Image(getThemeFile(DEFAULT_ACHIEVEMENT_IMAGE)));

    instance.setAchievementDefinition(achievementDefinition);

    assertThat(instance.nameLabel.getText(), is(achievementDefinition.getName()));
    assertThat(instance.descriptionLabel.getText(), is(achievementDefinition.getDescription()));
    assertThat(instance.pointsLabel.getText(), is(String.format("%d", achievementDefinition.getExperiencePoints())));
    assertThat(instance.imageView.getImage(), notNullValue());
    assertThat(instance.imageView.getEffect(), is(instanceOf(ColorAdjust.class)));
    assertThat(instance.imageView.getOpacity(), is(0.5));
    assertThat(instance.progressBar.isVisible(), is(true));
  }

  @Test
  public void testSetAchievementDefinitionStandardHasNoProgress() throws Exception {
    instance.setAchievementDefinition(AchievementDefinitionBuilder.create().defaultValues()
        .type(AchievementType.STANDARD)
        .get());

    assertThat(instance.progressBar.isVisible(), is(false));
    assertThat(instance.progressLabel.isVisible(), is(false));
  }

  @Test
  public void testSetPlayerAchievementStandardDoesntUpdateProgress() throws Exception {
    instance.setAchievementDefinition(AchievementDefinitionBuilder.create().defaultValues()
        .type(AchievementType.STANDARD)
        .get());

    PlayerAchievement playerAchievement = PlayerAchievementBuilder.create().defaultValues()
        .state(com.faforever.client.api.AchievementState.UNLOCKED)
        .currentSteps(50)
        .get();

    instance.setPlayerAchievement(playerAchievement);

    assertThat(instance.progressBar.getProgress(), is(0.0));
  }

  @Test(expected = IllegalStateException.class)
  public void testSetPlayerAchievementWithUnsetAchievementThrowsIse() throws Exception {
    instance.setPlayerAchievement(new PlayerAchievement());
  }

  @Test(expected = IllegalStateException.class)
  public void testSetPlayerAchievementIdDoesntMatch() throws Exception {
    instance.setAchievementDefinition(AchievementDefinitionBuilder.create().defaultValues().get());

    PlayerAchievement playerAchievement = PlayerAchievementBuilder.create().defaultValues()
        .achievementId("foobar")
        .get();

    instance.setPlayerAchievement(playerAchievement);
  }

  @Test
  public void testSetPlayerAchievementRevealed() throws Exception {
    instance.setAchievementDefinition(AchievementDefinitionBuilder.create().defaultValues().get());

    PlayerAchievement playerAchievement = PlayerAchievementBuilder.create().defaultValues()
        .state(com.faforever.client.api.AchievementState.REVEALED)
        .get();

    instance.setPlayerAchievement(playerAchievement);
    assertThat(instance.imageView.getEffect(), is(instanceOf(ColorAdjust.class)));
    assertThat(instance.imageView.getOpacity(), is(0.5));
  }

  @Test
  public void testSetPlayerAchievementUnlocked() throws Exception {
    instance.setAchievementDefinition(AchievementDefinitionBuilder.create().defaultValues().get());

    PlayerAchievement playerAchievement = PlayerAchievementBuilder.create().defaultValues()
        .state(com.faforever.client.api.AchievementState.UNLOCKED)
        .currentSteps(50)
        .get();

    instance.setPlayerAchievement(playerAchievement);
    assertThat(instance.imageView.getEffect(), is(nullValue()));
    assertThat(instance.imageView.getOpacity(), is(1.0));
    assertThat(instance.progressBar.isVisible(), is(true));
    assertThat(instance.progressBar.getProgress(), is(0.5));
  }
}
