package com.faforever.client.vault.review;


import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReviewsController implements Controller<Pane> {
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
  public ReviewController ownReviewPaneController;
  public Label ownReviewLabel;
  public Pane otherReviewsContainer;
  public Pane reviewsPagination;
  public Button pageLeftButton;
  public Button pageRightButton;

  private Consumer<Review> onSendReviewListener;
  private Pane ownReviewRoot;
  private Consumer<Review> onDeleteReviewListener;
  private ObservableList<Review> reviews;
  private InvalidationListener onReviewsChangedListener;
  private Optional<Review> ownReview;
  private List<List<Review>> reviewPages;
  private int currentReviewPage;

  public ReviewsController(I18n i18n, UiService uiService, PlayerService playerService) {
    this.i18n = i18n;
    this.uiService = uiService;
    this.playerService = playerService;
    onReviewsChangedListener = observable -> Platform.runLater(this::onReviewsChanged);
    ownReview = Optional.empty();
  }

  public void initialize() {
    ownReviewRoot = ownReviewPaneController.getRoot();
    JavaFxUtil.setAnchors(ownReviewRoot, 0d);
    ownReviewRoot.managedProperty().bind(ownReviewRoot.visibleProperty());
    ownReviewRoot.setVisible(false);
    ownReviewPaneController.setOnDeleteReviewListener(this::onDeleteReview);
    ownReviewPaneController.setOnCancelListener(this::onCancelReview);
    ownReviewPaneController.setOnSendReviewListener(review -> onSendReviewListener.accept(review));

    // Prevent flickering
    setReviews(FXCollections.emptyObservableList());
    createReviewButton.managedProperty().bind(createReviewButton.visibleProperty());
    createReviewButton.setVisible(false);

    ownReviewLabel.managedProperty().bind(ownReviewLabel.visibleProperty());
    ownReviewLabel.setVisible(false);

    pageLeftButton.managedProperty().bind(pageLeftButton.visibleProperty());
    reviewsPagination.managedProperty().bind(reviewsPagination.visibleProperty());
    pageRightButton.managedProperty().bind(pageRightButton.visibleProperty());
  }

  private void onDeleteReview(Review review) {
    Optional.ofNullable(this.onDeleteReviewListener).ifPresent(listener -> listener.accept(review));
  }

  @Override
  public Pane getRoot() {
    return reviewsRoot;
  }

  public void onCreateReviewButtonClicked() {
    ownReviewRoot.setVisible(true);
    createReviewButton.setVisible(false);
    ownReviewPaneController.setReview(Optional.empty());
  }

  private void onCancelReview() {
    setOwnReview(this.ownReview);
  }

  public void setOnSendReviewListener(Consumer<Review> onSendReviewListener) {
    this.onSendReviewListener = onSendReviewListener;
  }

  public void setReviews(ObservableList<Review> reviews) {
    this.reviews = reviews;

    Player currentPlayer = playerService.getCurrentPlayer()
        .orElseThrow(() -> new IllegalStateException("No current player available"));

    JavaFxUtil.addListener(reviews, onReviewsChangedListener);
    FilteredList<Review> onlyOtherNonEmptyReviews = reviews
        .filtered(review -> review.getPlayer().getId() != currentPlayer.getId() && !Strings.isNullOrEmpty(review.getText()));

    reviewPages = Lists.newArrayList(Iterables.partition(onlyOtherNonEmptyReviews, REVIEWS_PER_PAGE));
    currentReviewPage = Math.max(0, Math.min(0, reviewPages.size() - 1));
    reviewsPagination.setVisible(!reviewPages.isEmpty());
    displayReviewsPage(0);

    // Prevent flickering
    if (Platform.isFxApplicationThread()) {
      onReviewsChanged();
    } else {
      Platform.runLater(this::onReviewsChanged);
    }
  }

  public void setCanWriteReview(boolean canWriteReview) {
    createReviewButton.setVisible(canWriteReview);
  }

  private void displayReviewsPage(int page) {
    if (page >= reviewPages.size()) {
      return;
    }
    pageLeftButton.setVisible(page > 0);
    pageRightButton.setVisible(page < reviewPages.size() - 1);

    List<Review> reviewsPage = reviewPages.get(currentReviewPage);
    List<Pane> reviewNodes = reviewsPage.stream()
        .map(review -> {
          ReviewController controller = uiService.loadFxml("theme/vault/review/review.fxml");
          controller.setReview(Optional.of(review));
          return controller.getRoot();
        })
        .collect(Collectors.toList());

    Platform.runLater(() -> otherReviewsContainer.getChildren().setAll(reviewNodes));
  }

  private void onReviewsChanged() {
    JavaFxUtil.assertApplicationThread();

    Map<Integer, Long> ratingOccurrences = reviews.stream()
        .collect(Collectors.groupingBy(Review::getScore, Collectors.counting()));

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

    IntSummaryStatistics statistics = reviews.stream().mapToInt(Review::getScore).summaryStatistics();
    float average = (float) statistics.getAverage();
    scoreLabel.setText(i18n.rounded(average, 1));
    averageStarsController.setValue(average);
  }

  public void setOwnReview(Optional<Review> ownReview) {
    this.ownReview = ownReview;
    Platform.runLater(() -> {
      if (ownReview.isPresent()) {
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

  public void setOnDeleteReviewListener(Consumer<Review> onDeleteReviewListener) {
    this.onDeleteReviewListener = onDeleteReviewListener;
  }

  public void onPageLeftButtonClicked() {
    displayReviewsPage(--currentReviewPage);
  }

  public void onPageRightButtonClicked() {
    displayReviewsPage(++currentReviewPage);
  }
}
