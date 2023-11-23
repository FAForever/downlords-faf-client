package com.faforever.client.vault.review;


import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.ReviewBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.NumberBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ReviewsController<T extends ReviewBean> extends NodeController<Pane> {
  private static final int REVIEWS_PER_PAGE = 4;
  private final I18n i18n;
  private final UiService uiService;
  private final PlayerService playerService;

  public Pane reviewsRoot;
  public Label scoreLabel;
  public Pane fiveStarsBar;
  public Pane fourStarsBar;
  public Pane threeStarsBar;
  public Pane twoStarsBar;
  public Pane oneStarBar;
  public GridPane ratingsGrid;
  public Label totalReviewsLabel;
  public StarsController averageStarsController;
  public Pane ownReview;
  public ReviewController<T> ownReviewController;
  public Label ownReviewLabel;
  public Pane otherReviewsContainer;
  public Pane reviewsPagination;
  public Button pageLeftButton;
  public Button pageRightButton;

  private final BooleanProperty canWriteReview = new SimpleBooleanProperty();
  private final IntegerProperty currentPage = new SimpleIntegerProperty(0);
  private final ObjectProperty<Supplier<T>> reviewSupplier = new SimpleObjectProperty<>();
  private final ObservableList<T> reviews = FXCollections.observableArrayList(
      review -> new Observable[]{review.scoreProperty()});

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(ownReviewLabel, ownReview, pageLeftButton, reviewsPagination, pageRightButton);
    JavaFxUtil.setAnchors(ownReview, 0d);

    FilteredList<T> otherNonEmptyReviews = new FilteredList<>(
        new SortedList<>(reviews, Comparator.comparing(ReviewBean::getVersion).reversed()));
    IntegerBinding numOtherNonEmptyReviews = Bindings.size(otherNonEmptyReviews);
    NumberBinding totalReviews = Bindings.max(Bindings.size(reviews), 1d);
    NumberBinding fiveStarPercentage = Bindings.size(new FilteredList<>(reviews, review -> review.getScore() == 5))
                                               .divide(totalReviews);
    NumberBinding fourStarPercentage = Bindings.size(new FilteredList<>(reviews, review -> review.getScore() == 4))
                                               .divide(totalReviews);
    NumberBinding threeStarPercentage = Bindings.size(new FilteredList<>(reviews, review -> review.getScore() == 3))
                                                .divide(totalReviews);
    NumberBinding twoStarPercentage = Bindings.size(new FilteredList<>(reviews, review -> review.getScore() == 2))
                                              .divide(totalReviews);
    NumberBinding oneStarPercentage = Bindings.size(new FilteredList<>(reviews, review -> review.getScore() == 1))
                                              .divide(totalReviews);

    totalReviewsLabel.textProperty()
                     .bind(totalReviews.map(numReviews -> i18n.get("reviews.totalReviewers", numReviews)));
    DoubleBinding average = Bindings.createDoubleBinding(
        () -> reviews.stream().mapToInt(ReviewBean::getScore).average().orElse(0d), reviews);
    scoreLabel.textProperty().bind(average.map(averageValue -> i18n.rounded(averageValue, 1)));
    averageStarsController.valueProperty().bind(average);

    fiveStarsBar.prefWidthProperty()
                .bind(((Pane) fiveStarsBar.getParent()).widthProperty().multiply(fiveStarPercentage).when(showing));
    fourStarsBar.prefWidthProperty()
                .bind(((Pane) fourStarsBar.getParent()).widthProperty().multiply(fourStarPercentage).when(showing));
    threeStarsBar.prefWidthProperty()
                 .bind(((Pane) threeStarsBar.getParent()).widthProperty().multiply(threeStarPercentage).when(showing));
    twoStarsBar.prefWidthProperty()
               .bind(((Pane) twoStarsBar.getParent()).widthProperty().multiply(twoStarPercentage).when(showing));
    oneStarBar.prefWidthProperty()
              .bind(((Pane) oneStarBar.getParent()).widthProperty().multiply(oneStarPercentage).when(showing));

    IntegerBinding maxPages = numOtherNonEmptyReviews.divide(REVIEWS_PER_PAGE);
    maxPages.when(showing).subscribe(newValue -> {
      int maxPage = newValue.intValue();
      if (currentPage.get() > maxPage) {
        currentPage.set(Math.max(0, maxPage - 1));
      }
    });

    pageLeftButton.visibleProperty().bind(currentPage.greaterThan(0).when(showing));
    pageRightButton.visibleProperty().bind(currentPage.lessThan(maxPages).when(showing));

    otherNonEmptyReviews.predicateProperty()
                        .bind(playerService.currentPlayerProperty()
                                           .map(player -> (Predicate<? super ReviewBean>) review -> !Objects.equals(
                                               review.getPlayer().getId(), player.getId()) && !StringUtils.isBlank(
                                               review.getText()))
                                           .when(showing));

    ObjectBinding<T> ownReviewBinding = Bindings.createObjectBinding(() -> {
      PlayerBean currentPlayer = playerService.getCurrentPlayer();
      return reviews.stream()
                    .filter(review -> Objects.equals(currentPlayer, review.getPlayer()))
                    .max(Comparator.comparing(ReviewBean::getVersion))
                    .orElseGet(() -> {
                      if (!canWriteReview.get() || reviewSupplier.get() == null) {
                        return null;
                      }

                      return reviewSupplier.get().get();
                    });
    }, playerService.currentPlayerProperty(), reviews, canWriteReview, reviewSupplier);
    ownReviewController.reviewProperty().bind(ownReviewBinding.when(showing));

    BooleanExpression reviewCreated = BooleanExpression.booleanExpression(
        ownReviewBinding.flatMap(ReviewBean::idProperty).map(Objects::nonNull));

    BooleanBinding noOwnReview = ownReviewController.reviewProperty().isNotNull();
    ownReview.visibleProperty().bind(noOwnReview.when(showing));
    ownReviewLabel.visibleProperty().bind(Bindings.and(noOwnReview, reviewCreated).when(showing));
    reviewsPagination.visibleProperty().bind(numOtherNonEmptyReviews.greaterThan(REVIEWS_PER_PAGE).when(showing));

    for (int i = 0; i < 4; i++) {
      ReviewController<T> controller = uiService.loadFxml("theme/vault/review/review.fxml");
      controller.reviewProperty()
                .bind(Bindings.valueAt(otherNonEmptyReviews, currentPage.multiply(REVIEWS_PER_PAGE).add(i))
                              .when(showing));
      Pane controllerRoot = controller.getRoot();
      controllerRoot.visibleProperty().bind(controller.reviewProperty().isNotNull().when(showing));
      otherReviewsContainer.getChildren().add(controllerRoot);
    }
  }

  @Override
  public Pane getRoot() {
    return reviewsRoot;
  }

  public void setOnSendReviewListener(Consumer<T> onSendReviewListener) {
    ownReviewController.setOnSendReviewListener(onSendReviewListener);
  }

  public void bindReviews(ObservableList<T> reviews) {
    Bindings.bindContent(this.reviews, reviews);
  }

  public void setCanWriteReview(boolean canWriteReview) {
    this.canWriteReview.set(canWriteReview);
  }

  public void setReviewSupplier(Supplier<T> reviewSupplier) {
    this.reviewSupplier.set(reviewSupplier);
  }

  public void setOnDeleteReviewListener(Consumer<T> onDeleteReviewListener) {
    ownReviewController.setOnDeleteReviewListener(onDeleteReviewListener);
  }

  public void onPageLeftButtonClicked() {
    currentPage.set(currentPage.get() - 1);
  }

  public void onPageRightButtonClicked() {
    currentPage.set(currentPage.get() + 1);
  }
}
