package com.faforever.client.vault.review;


import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.ReviewBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReviewsController<T extends ReviewBean> implements Controller<Pane> {
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
  public Button createReviewButton;
  public StarsController averageStarsController;
  /**
   * Not named `ownReviewController` because `ownReview` clashes with {@link #ownReview}.
   */
  public ReviewController<T> ownReviewPaneController;
  public Label ownReviewLabel;
  public Pane otherReviewsContainer;
  public Pane reviewsPagination;
  public Button pageLeftButton;
  public Button pageRightButton;

  private Consumer<T> onSendReviewListener;
  private Pane ownReviewRoot;
  private Consumer<T> onDeleteReviewListener;
  private ObservableList<T> reviews;
  private final InvalidationListener onReviewsChangedListener;
  private T ownReview;
  private List<List<T>> reviewPages;
  private int currentReviewPage;

  public ReviewsController(I18n i18n, UiService uiService, PlayerService playerService) {
    this.i18n = i18n;
    this.uiService = uiService;
    this.playerService = playerService;
    onReviewsChangedListener = observable -> JavaFxUtil.runLater(this::onReviewsChanged);
  }

  public void initialize() {
    ownReviewRoot = ownReviewPaneController.getRoot();
    JavaFxUtil.setAnchors(ownReviewRoot, 0d);

    ownReviewRoot.setVisible(false);
    ownReviewPaneController.setOnDeleteReviewListener(this::onDeleteReview);
    ownReviewPaneController.setOnCancelListener(this::onCancelReview);
    ownReviewPaneController.setOnSendReviewListener(review -> onSendReviewListener.accept(review));

    // Prevent flickering
    setReviews(FXCollections.emptyObservableList());
    createReviewButton.setVisible(false);
    ownReviewLabel.setVisible(false);

    JavaFxUtil.bindManagedToVisible(createReviewButton, ownReviewLabel, ownReviewRoot, pageLeftButton,
        reviewsPagination, pageRightButton);
  }

  private void onDeleteReview(T review) {
    Optional.ofNullable(this.onDeleteReviewListener).ifPresent(listener -> listener.accept(review));
  }

  @Override
  public Pane getRoot() {
    return reviewsRoot;
  }

  public void onCreateReviewButtonClicked() {
    ownReviewRoot.setVisible(true);
    createReviewButton.setVisible(false);
    ownReviewPaneController.setReview(null);
  }

  private void onCancelReview() {
    setOwnReview(this.ownReview);
  }

  public void setOnSendReviewListener(Consumer<T> onSendReviewListener) {
    this.onSendReviewListener = onSendReviewListener;
  }

  public void setReviews(ObservableList<T> reviews) {
    this.reviews = reviews.sorted(Comparator.<T, ComparableVersion>comparing(review -> Optional.ofNullable(review.getVersion()).orElse(new ComparableVersion("0"))).reversed());

    PlayerBean currentPlayer = playerService.getCurrentPlayer();

    FilteredList<T> onlyOtherNonEmptyReviews = this.reviews
        .filtered(review -> review.getPlayer().getId() != currentPlayer.getId() && !Strings.isNullOrEmpty(review.getText()));

    reviewPages = Lists.newArrayList(Iterables.partition(onlyOtherNonEmptyReviews, REVIEWS_PER_PAGE));
    currentReviewPage = Math.max(0, Math.min(0, reviewPages.size() - 1));
    reviewsPagination.setVisible(!reviewPages.isEmpty());
    displayReviewsPage(0);
    JavaFxUtil.addAndTriggerListener(this.reviews, onReviewsChangedListener);
  }

  public void setCanWriteReview(boolean canWriteReview) {
    createReviewButton.setDisable(!canWriteReview);
  }

  private void displayReviewsPage(int page) {
    if (page >= reviewPages.size()) {
      return;
    }
    pageLeftButton.setVisible(page > 0);
    pageRightButton.setVisible(page < reviewPages.size() - 1);

    List<? extends ReviewBean> reviewsPage = reviewPages.get(currentReviewPage);
    List<Pane> reviewNodes = reviewsPage.stream()
        .map(review -> {
          ReviewController<T> controller = uiService.loadFxml("theme/vault/review/review.fxml");
          controller.setReview((T) review);
          return controller.getRoot();
        })
        .collect(Collectors.toList());

    JavaFxUtil.runLater(() -> otherReviewsContainer.getChildren().setAll(reviewNodes));
  }

  private void onReviewsChanged() {
    JavaFxUtil.assertApplicationThread();

    Map<Integer, Long> ratingOccurrences = reviews.stream()
        .collect(Collectors.groupingBy(ReviewBean::getScore, Collectors.counting()));

    Long fiveStars = ratingOccurrences.getOrDefault(5, 0L);
    Long fourStars = ratingOccurrences.getOrDefault(4, 0L);
    Long threeStars = ratingOccurrences.getOrDefault(3, 0L);
    Long twoStars = ratingOccurrences.getOrDefault(2, 0L);
    Long oneStars = ratingOccurrences.getOrDefault(1, 0L);

    int totalReviews = Math.max(reviews.size(), 1);
    float fiveStarsPercentage = (float) fiveStars / totalReviews;
    float fourStarsPercentage = (float) fourStars / totalReviews;
    float threeStarsPercentage = (float) threeStars / totalReviews;
    float twoStarsPercentage = (float) twoStars / totalReviews;
    float oneStarPercentage = (float) oneStars / totalReviews;

    // So that the bars' parents have their sizes
    reviewsRoot.applyCss();
    reviewsRoot.layout();

    totalReviewsLabel.setText(i18n.get("reviews.totalReviewers", reviews.size()));
    fiveStarsBar.prefWidthProperty().bind(((Pane) fiveStarsBar.getParent()).widthProperty().multiply(fiveStarsPercentage));
    fourStarsBar.prefWidthProperty().bind(((Pane) fourStarsBar.getParent()).widthProperty().multiply(fourStarsPercentage));
    threeStarsBar.prefWidthProperty().bind(((Pane) threeStarsBar.getParent()).widthProperty().multiply(threeStarsPercentage));
    twoStarsBar.prefWidthProperty().bind(((Pane) twoStarsBar.getParent()).widthProperty().multiply(twoStarsPercentage));
    oneStarBar.prefWidthProperty().bind(((Pane) oneStarBar.getParent()).widthProperty().multiply(oneStarPercentage));

    IntSummaryStatistics statistics = reviews.stream().mapToInt(ReviewBean::getScore).summaryStatistics();
    float average = (float) statistics.getAverage();
    scoreLabel.setText(i18n.rounded(average, 1));
    averageStarsController.setValue(average);
  }

  public void setReviewSupplier(Supplier<T> reviewSupplier) {
    this.ownReviewPaneController.setReviewSupplier(reviewSupplier);
  }

  public void setOwnReview(T ownReview) {
    this.ownReview = ownReview;
    JavaFxUtil.runLater(() -> {
      if (ownReview != null) {
        ownReviewPaneController.setReview(ownReview);
        ownReviewRoot.setVisible(true);
        createReviewButton.setVisible(false);
        ownReviewLabel.setVisible(true);
      } else {
        ownReviewRoot.setVisible(false);
        createReviewButton.setVisible(true);
        ownReviewLabel.setVisible(false);
      }
    });
  }

  public void setOnDeleteReviewListener(Consumer<T> onDeleteReviewListener) {
    this.onDeleteReviewListener = onDeleteReviewListener;
  }

  public void onPageLeftButtonClicked() {
    displayReviewsPage(--currentReviewPage);
  }

  public void onPageRightButtonClicked() {
    displayReviewsPage(++currentReviewPage);
  }
}
