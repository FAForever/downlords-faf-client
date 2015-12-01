package com.faforever.client.achievements;

import com.faforever.client.api.AchievementDefinition;
import com.faforever.client.api.AchievementState;
import com.faforever.client.api.AchievementType;
import com.faforever.client.api.PlayerAchievement;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.base.MoreObjects;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import javax.annotation.Resource;
import java.util.Locale;
import java.util.Objects;


public class AchievementItemController {

  @FXML
  GridPane achievementItemRoot;
  @FXML
  Label nameLabel;
  @FXML
  Label descriptionLabel;
  @FXML
  Label pointsLabel;
  @FXML
  ProgressBar progressBar;
  @FXML
  Label progressLabel;
  @FXML
  ImageView imageView;

  @Resource
  Locale locale;
  @Resource
  I18n i18n;
  @Resource
  PreferencesService preferencesService;
  @Resource
  AchievementService achievementService;

  private AchievementDefinition achievementDefinition;

  @FXML
  void initialize() {
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
    imageView.setImage(achievementService.getRevealedIcon(achievementDefinition));
    progressLabel.setText(i18n.get("achievement.stepsFormat", 0, achievementDefinition.getTotalSteps()));
    progressBar.setProgress(0);

    if (AchievementType.STANDARD.equals(achievementDefinition.getType())) {
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

    if (AchievementState.UNLOCKED.equals(playerAchievement.getState())) {
      imageView.setImage(achievementService.getUnlockedIcon(achievementDefinition));
      imageView.setOpacity(1);
      imageView.setEffect(null);
    }

    if (AchievementType.INCREMENTAL.equals(achievementDefinition.getType())) {
      Integer currentSteps = MoreObjects.firstNonNull(playerAchievement.getCurrentSteps(), 0);
      Integer totalSteps = achievementDefinition.getTotalSteps();
      progressBar.setProgress((double) currentSteps / totalSteps);
      Platform.runLater(() -> progressLabel.setText(i18n.get("achievement.stepsFormat", currentSteps, totalSteps)));
    }
  }
}
