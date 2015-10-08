package com.faforever.client.user;

import com.faforever.client.i18n.I18n;
import com.google.api.services.games.model.AchievementDefinition;
import com.google.api.services.games.model.PlayerAchievement;
import com.google.common.base.MoreObjects;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import javax.annotation.Resource;
import java.util.Locale;

public class AchievementItemController {

  private static final String STANDARD = "STANDARD";
  private static final String UNLOCKED = "UNLOCKED";
  private static final String INCREMENTAL = "INCREMENTAL";

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
    imageView.setImage(new Image(achievementDefinition.getRevealedIconUrl(), true));
    progressLabel.setText(i18n.get("achievement.stepsFormat", 0, achievementDefinition.getTotalSteps()));
    progressBar.setProgress(0);

    if (STANDARD.equals(achievementDefinition.getAchievementType())) {
      progressBar.setVisible(false);
      progressLabel.setVisible(false);
    }
  }

  public void setPlayerAchievement(PlayerAchievement playerAchievement) {
    if (achievementDefinition == null) {
      throw new IllegalStateException("achievementDefinition needs to be set first");
    }

    // TODO cache it?
    if (UNLOCKED.equals(playerAchievement.getAchievementState())) {
      imageView.setImage(new Image(achievementDefinition.getUnlockedIconUrl(), true));
    }

    if (INCREMENTAL.equals(achievementDefinition.getAchievementType())) {
      Integer currentSteps = MoreObjects.firstNonNull(playerAchievement.getCurrentSteps(), 0);
      Integer totalSteps = achievementDefinition.getTotalSteps();
      progressBar.setProgress((double) currentSteps / totalSteps);
      Platform.runLater(() -> progressLabel.setText(i18n.get("achievement.stepsFormat", currentSteps, totalSteps)));
    }
  }
}
