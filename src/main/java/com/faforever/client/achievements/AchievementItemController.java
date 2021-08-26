package com.faforever.client.achievements;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.util.Assert;
import com.faforever.commons.api.dto.AchievementDefinition;
import com.faforever.commons.api.dto.AchievementState;
import com.faforever.commons.api.dto.AchievementType;
import com.faforever.commons.api.dto.PlayerAchievement;
import com.google.common.base.MoreObjects;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
// TODO this class should not use API objects
public class AchievementItemController implements Controller<Node> {

  private final I18n i18n;
  private final AchievementService achievementService;
  public GridPane achievementItemRoot;
  public Label nameLabel;
  public Label descriptionLabel;
  public Label pointsLabel;
  public ProgressBar progressBar;
  public Label progressLabel;
  public ImageView imageView;
  private AchievementDefinition achievementDefinition;

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
    pointsLabel.setText(i18n.number(achievementDefinition.getExperiencePoints()));
    imageView.setImage(achievementService.getImage(achievementDefinition, AchievementState.REVEALED));
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
    Assert.checkNullIllegalState(achievementDefinition, "achievementDefinition needs to be set first");
    Assert.checkObjectUnequalsIllegalState(achievementDefinition.getId(), playerAchievement.getAchievement().getId(),
      "Achievement ID does not match");

    if (AchievementState.UNLOCKED == AchievementState.valueOf(playerAchievement.getState().name())) {
      imageView.setImage(achievementService.getImage(achievementDefinition, AchievementState.UNLOCKED));
      imageView.setOpacity(1);
      imageView.setEffect(null);
    }

    if (AchievementType.INCREMENTAL == achievementDefinition.getType()) {
      Integer currentSteps = MoreObjects.firstNonNull(playerAchievement.getCurrentSteps(), 0);
      Integer totalSteps = achievementDefinition.getTotalSteps();
      progressBar.setProgress((double) currentSteps / totalSteps);
      JavaFxUtil.runLater(() -> progressLabel.setText(i18n.get("achievement.stepsFormat", currentSteps, totalSteps)));
    }
  }
}
