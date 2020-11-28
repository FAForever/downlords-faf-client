package com.faforever.client.vault.review;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Optional;
import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReviewController implements Controller<Pane> {
  private static final String[] STARS_TIP_KEYS = {
      "review.starsTip.one",
      "review.starsTip.two",
      "review.starsTip.three",
      "review.starsTip.four",
      "review.starsTip.five"
  };

  private final I18n i18n;
  private final PlayerService playerService;

  public Pane ownReviewRoot;
  public Pane displayReviewPane;
  public Pane editReviewPane;
  public TextArea reviewTextArea;
  public Label starsTipLabel;
  public Label usernameLabel;
  public Label versionLabel;
  public StarsController selectionStarsController;
  public StarsController displayStarsController;
  public ImageView avatarImageView;
  public Label reviewTextLabel;
  public Button deleteButton;
  public Button editButton;

  private Consumer<Review> onSendReviewListener;
  private Consumer<Review> onDeleteReviewListener;
  private Optional<Review> review;
  private Runnable onCancelReviewListener;

  public ReviewController(I18n i18n, PlayerService playerService) {
    this.i18n = i18n;
    this.playerService = playerService;
    review = Optional.empty();
  }

  public void initialize() {
    selectionStarsController.valueProperty().addListener((observable, oldValue, newValue)
        -> starsTipLabel.setText(i18n.get(STARS_TIP_KEYS[newValue.intValue() - 1])));
    selectionStarsController.setSelectable(true);
    selectionStarsController.setValue(4);

    displayReviewPane.managedProperty().bind(displayReviewPane.visibleProperty());
    editReviewPane.managedProperty().bind(editReviewPane.visibleProperty());
    editButton.managedProperty().bind(editButton.visibleProperty());
    deleteButton.managedProperty().bind(deleteButton.visibleProperty());

    displayReviewPane.setVisible(false);
    editButton.setVisible(false);
    deleteButton.setVisible(false);
  }

  public void setReview(Optional<Review> optionalReview) {
    JavaFxUtil.assertApplicationThread();
    this.review = optionalReview;
    if (!optionalReview.isPresent()) {
      editReviewPane.setVisible(true);
      displayReviewPane.setVisible(false);
      return;
    }

    Player currentPlayer = playerService.getCurrentPlayer()
        .orElseThrow(() -> new IllegalStateException("No player is available"));
    Review definiteReview = optionalReview.get();

    boolean isReviewOwnedByCurrentUser = currentPlayer.equals(definiteReview.getPlayer());

    int rating = definiteReview.getScore();
    selectionStarsController.setValue(rating);
    displayStarsController.setValue(rating);
    usernameLabel.setText(definiteReview.getPlayer().getUsername());
    reviewTextLabel.setText(definiteReview.getText());
    if (!definiteReview.getVersion().toString().isBlank()) {
      versionLabel.setText(i18n.get("review.version", definiteReview.getVersion().toString()));
    }
    displayReviewPane.setVisible(true);
    editReviewPane.setVisible(false);
    editButton.setVisible(isReviewOwnedByCurrentUser);
    deleteButton.setVisible(isReviewOwnedByCurrentUser);
  }

  @Override
  public Pane getRoot() {
    return ownReviewRoot;
  }

  public void onDeleteButtonClicked() {
    Optional.ofNullable(onDeleteReviewListener).ifPresent(listener -> {
      Assert.state(review.isPresent(), "No review has been set");
      listener.accept(review.get());
    });
  }

  public void onEditButtonClicked() {
    Assert.state(review.isPresent(), "No review has been set");

    reviewTextArea.setText(review.get().getText());
    displayReviewPane.setVisible(false);
    editReviewPane.setVisible(true);
  }

  public void onSendReview() {
    Review review = this.review.orElse(new Review());
    review.setScore(Math.round(selectionStarsController.getValue()));
    review.setText(reviewTextArea.getText());
    this.onSendReviewListener.accept(review);
  }

  void setOnSendReviewListener(Consumer<Review> onSendReviewListener) {
    this.onSendReviewListener = onSendReviewListener;
  }

  public void onCancelButtonClicked() {
    boolean reviewPresent = review.isPresent();
    displayReviewPane.setVisible(reviewPresent);
    editReviewPane.setVisible(false);

    Optional.ofNullable(onCancelReviewListener).ifPresent(Runnable::run);
  }

  void setOnCancelListener(Runnable onCancelReviewListener) {
    this.onCancelReviewListener = onCancelReviewListener;
  }

  void setOnDeleteReviewListener(Consumer<Review> onDeleteReviewListener) {
    this.onDeleteReviewListener = onDeleteReviewListener;
  }
}
