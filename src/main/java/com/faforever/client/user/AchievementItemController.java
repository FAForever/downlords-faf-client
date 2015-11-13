package com.faforever.client.user;

import com.faforever.client.events.AchievementDefinition;
import com.faforever.client.events.PlayerAchievement;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.AchievementUtil;
import com.google.common.base.MoreObjects;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import javax.annotation.Resource;
import java.util.Locale;

import static com.faforever.client.events.AchievementState.UNLOCKED;
import static com.faforever.client.events.AchievementType.INCREMENTAL;
import static com.faforever.client.events.AchievementType.STANDARD;


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

    String imageUrl = AchievementUtil.defaultIcon(preferencesService.getPreferences().getTheme(),
        achievementDefinition.getRevealedIconUrl());

    nameLabel.setText(achievementDefinition.getName());
    descriptionLabel.setText(achievementDefinition.getDescription());
    pointsLabel.setText(String.format(locale, "%d", achievementDefinition.getExperiencePoints()));
    imageView.setImage(new Image(imageUrl, true));
    progressLabel.setText(i18n.get("achievement.stepsFormat", 0, achievementDefinition.getTotalSteps()));
    progressBar.setProgress(0);

    if (STANDARD.equals(achievementDefinition.getType())) {
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

    // TODO cache it?
    if (UNLOCKED.equals(playerAchievement.getState())) {
      String imageUrl = AchievementUtil.defaultIcon(preferencesService.getPreferences().getTheme(),
          achievementDefinition.getRevealedIconUrl());
      imageView.setImage(new Image(imageUrl, true));
      imageView.setOpacity(1);
      imageView.setEffect(null);
    }

    if (INCREMENTAL.equals(achievementDefinition.getType())) {
      Integer currentSteps = MoreObjects.firstNonNull(playerAchievement.getCurrentSteps(), 0);
      Integer totalSteps = achievementDefinition.getTotalSteps();
      progressBar.setProgress((double) currentSteps / totalSteps);
      Platform.runLater(() -> progressLabel.setText(i18n.get("achievement.stepsFormat", currentSteps, totalSteps)));
    }
  }
}
