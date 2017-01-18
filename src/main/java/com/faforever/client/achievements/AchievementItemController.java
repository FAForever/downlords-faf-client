package com.faforever.client.achievements;

import com.faforever.client.achievements.AchievementService.AchievementState;
import com.faforever.client.api.AchievementDefinition;
import com.faforever.client.api.AchievementType;
import com.faforever.client.api.PlayerAchievement;
import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.base.MoreObjects;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Objects;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
// TODO this class should not use API objects
public class AchievementItemController implements Controller<Node> {

  private final Locale locale;
  private final I18n i18n;
  private final PreferencesService preferencesService;
  private final AchievementService achievementService;
  public GridPane achievementItemRoot;
  public Label nameLabel;
  public Label descriptionLabel;
  public Label pointsLabel;
  public ProgressBar progressBar;
  public Label progressLabel;
  public ImageView imageView;
  private AchievementDefinition achievementDefinition;

  @Inject
  public AchievementItemController(Locale locale, I18n i18n, PreferencesService preferencesService, AchievementService achievementService) {
    this.locale = locale;
    this.i18n = i18n;
    this.preferencesService = preferencesService;
    this.achievementService = achievementService;
  }

  public void initialize() {
    progressBar.managedProperty().bind(progressBar.visibleProperty());
    progressLabel.managedProperty().bind(progressLabel.visibleProperty());
  }

  public Node getRoot() {
    return achievementItemRoot;
  }

  public void setAchievementDefinition(AchievementDefinition achievementDefinition) {
    this.achievementDefinition = achievementDefinition;

    nameLabel.setText(achievementDefinition.getName());
    descriptionLabel.setText(achievementDefinition.getDescription());
    pointsLabel.setText(String.format(locale, "%d", achievementDefinition.getExperiencePoints()));
    imageView.setImage(achievementService.getImage(achievementDefinition, AchievementService.AchievementState.REVEALED));
    progressLabel.setText(i18n.get("achievement.stepsFormat", 0, achievementDefinition.getTotalSteps()));
    progressBar.setProgress(0);

    if (AchievementType.STANDARD == achievementDefinition.getType()) {
      progressBar.setVisible(false);
      progressLabel.setVisible(false);
    }

    ColorAdjust colorAdjust = new ColorAdjust();
    colorAdjust.setSaturation(-1);
    imageView.setEffect(colorAdjust);
    imageView.setOpacity(0.5);
  }

  public void setPlayerAchievement(PlayerAchievement playerAchievement) {
    if (achievementDefinition == null) {
      throw new IllegalStateException("achievementDefinition needs to be set first");
    }
    if (!Objects.equals(achievementDefinition.getId(), playerAchievement.getAchievementId())) {
      throw new IllegalStateException("Achievement ID does not match");
    }

    if (AchievementState.UNLOCKED == AchievementState.valueOf(playerAchievement.getState().name())) {
      imageView.setImage(achievementService.getImage(achievementDefinition, AchievementState.UNLOCKED));
      imageView.setOpacity(1);
      imageView.setEffect(null);
    }

    if (AchievementType.INCREMENTAL == achievementDefinition.getType()) {
      Integer currentSteps = MoreObjects.firstNonNull(playerAchievement.getCurrentSteps(), 0);
      Integer totalSteps = achievementDefinition.getTotalSteps();
      progressBar.setProgress((double) currentSteps / totalSteps);
      Platform.runLater(() -> progressLabel.setText(i18n.get("achievement.stepsFormat", currentSteps, totalSteps)));
    }
  }
}
