package com.faforever.client.vault.review;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.ReviewBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ReviewController<R extends ReviewBean<R, ?>> extends NodeController<Pane> {
  private static final String[] STARS_TIP_KEYS = {"review.starsTip.one", "review.starsTip.two", "review.starsTip.three", "review.starsTip.four", "review.starsTip.five"};

  private final I18n i18n;
  private final PlayerService playerService;

  public Pane ownReviewRoot;
  public Pane displayReviewPane;
  public Pane editReviewPane;
  public TextArea reviewTextArea;
  public Label starsTipLabel;
  public Label usernameLabel;
  public Label versionLabel;
  public HBox selectionStars;
  public HBox displayStars;
  public StarsController selectionStarsController;
  public StarsController displayStarsController;
  public Label reviewTextLabel;
  public Button deleteButton;
  public Button editButton;
  public Button sendButton;

  private final ObjectProperty<R> review = new SimpleObjectProperty<>();
  private final BooleanProperty editing = new SimpleBooleanProperty();

  private Consumer<R> onSendReviewListener;
  private Consumer<R> onDeleteReviewListener;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(displayReviewPane, editReviewPane, editButton, deleteButton);

    starsTipLabel.textProperty()
        .bind(selectionStarsController.valueProperty()
            .map(val -> val.intValue() - 1)
            .map(i -> i >= 0 && i < STARS_TIP_KEYS.length ? STARS_TIP_KEYS[i] : null)
            .map(i18n::get));

    selectionStarsController.setSelectable(true);

    editReviewPane.visibleProperty().bind(editing.and(review.isNotNull()));
    displayReviewPane.visibleProperty().bind(editing.not().and(review.isNotNull()));

    BooleanExpression reviewCreated = BooleanExpression.booleanExpression(review.map(ReviewBean::id)
        .map(Objects::nonNull));
    ObservableValue<PlayerBean> reviewerObservable = review.map(ReviewBean::player);
    BooleanExpression ownedByPlayer = BooleanExpression.booleanExpression(playerService.currentPlayerProperty()
                                                                                       .flatMap(
                                                                                           player -> reviewerObservable.map(
                                                                                               reviewer -> Objects.equals(
                                                                                                   reviewer, player)))
                                                                                       .when(showing));
    displayStars.visibleProperty().bind(reviewCreated.when(showing));
    usernameLabel.visibleProperty().bind(reviewCreated.when(showing));
    versionLabel.visibleProperty().bind(reviewCreated.when(showing));
    reviewTextLabel.visibleProperty().bind(reviewCreated.when(showing));
    editButton.textProperty()
        .bind(reviewCreated.map(created -> created ? "" : i18n.get("reviews.create")).when(showing));
    editButton.visibleProperty().bind(ownedByPlayer.when(showing));
    deleteButton.visibleProperty().bind(Bindings.and(reviewCreated, ownedByPlayer).when(showing));

    ObservableValue<Number> scoreObservable = review.map(ReviewBean::score);
    sendButton.disableProperty()
        .bind(selectionStarsController.valueProperty().map(score -> score.intValue() < 1).when(showing));
    displayStarsController.valueProperty().bind(scoreObservable.when(showing));
    usernameLabel.textProperty()
        .bind(reviewerObservable.flatMap(PlayerBean::usernameProperty).when(showing));
    reviewTextLabel.textProperty().bind(review.map(ReviewBean::text).when(showing));
    versionLabel.textProperty().bind(review.map(ReviewBean::version).map(version -> {
      if (version == null || version.toString().isBlank()) {
        return null;
      }

      return i18n.get("review.version", version.toString());
    }).when(showing));
  }

  public void setReview(R review) {
    this.review.set(review);
  }

  public R getReview() {
    return review.get();
  }

  public ObjectProperty<R> reviewProperty() {
    return review;
  }

  @Override
  public Pane getRoot() {
    return ownReviewRoot;
  }

  public void onDeleteButtonClicked() {
    R reviewValue = review.getValue();
    Assert.notNull(reviewValue, "No review has been set");

    if (onDeleteReviewListener != null) {
      onDeleteReviewListener.accept(reviewValue);
    }

    editing.set(false);
  }

  public void onEditButtonClicked() {
    R reviewValue = review.get();
    Assert.notNull(reviewValue, "No review has been set");

    selectionStarsController.setValue(reviewValue.score());
    reviewTextArea.setText(reviewValue.text());
    editing.set(true);
  }

  public void onCancelButtonClicked() {
    editing.set(false);
  }

  public void onSendReview() {
    R newReviewValue = review.get()
                             .withScoreAndText(Math.round(selectionStarsController.getValue()),
                                               reviewTextArea.getText());

    if (onSendReviewListener != null) {
      onSendReviewListener.accept(newReviewValue);
    }

    editing.set(false);
  }

  public void setOnSendReviewListener(Consumer<R> onSendReviewListener) {
    this.onSendReviewListener = onSendReviewListener;
  }

  public void setOnDeleteReviewListener(Consumer<R> onDeleteReviewListener) {
    this.onDeleteReviewListener = onDeleteReviewListener;
  }
}
