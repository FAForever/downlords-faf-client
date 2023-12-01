package com.faforever.client.leaderboard;

import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.LeagueSeasonBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.SimpleInvalidationListener;
import com.faforever.client.fx.ToStringOnlyConverter;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.text.Text;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class LeaderboardController extends NodeController<StackPane> {

  private static final PseudoClass NOTIFICATION_HIGHLIGHTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("highlighted-bar");

  private final I18n i18n;
  private final LeaderboardService leaderboardService;
  private final NotificationService notificationService;
  private final PlayerService playerService;
  private final TimeService timeService;
  private final UiService uiService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public StackPane leaderboardRoot;
  public TextField searchTextField;
  public Pane connectionProgressPane;
  public Pane contentPane;
  public BarChart<String, Integer> ratingDistributionChart;
  public Label playerDivisionNameLabel;
  public Label playerScoreLabel;
  public Label scoreLabel;
  public Label seasonLabel;
  public ComboBox<SubdivisionBean> majorDivisionPicker;
  public Arc scoreArc;
  public TabPane subDivisionTabPane;
  public ImageView playerDivisionImageView;
  public CategoryAxis xAxis;
  public NumberAxis yAxis;
  public Label placementLabel;
  public Label seasonDateLabel;
  private LeagueSeasonBean season;
  private LeagueEntryBean entryToSelect;
  private final SimpleInvalidationListener playerRatingListener = this::updateDisplayedPlayerStats;

  @VisibleForTesting
  protected AutoCompletionBinding<String> usernamesAutoCompletion;

  @Override
  protected void onInitialize() {
    scoreLabel.setText(i18n.get("leaderboard.score").toUpperCase());
    searchTextField.setPromptText(i18n.get("leaderboard.searchPrompt").toUpperCase());

    JavaFxUtil.bindManagedToVisible(contentPane, connectionProgressPane);
    connectionProgressPane.visibleProperty().bind(contentPane.visibleProperty().not());
    contentPane.setVisible(false);

    majorDivisionPicker.setConverter(
        new ToStringOnlyConverter<>(subdivision -> i18n.get(subdivision.getDivisionI18nKey()).toUpperCase()));
    yAxis.setTickLabelFormatter(new ToStringOnlyConverter<>(number -> String.valueOf(number.intValue())));
    yAxis.setTickUnit(10d);

    subDivisionTabPane.widthProperty().when(showing).subscribe(newValue ->
        setTabWidth((double) newValue, subDivisionTabPane.getTabs().size()));
  }

  private void setTabWidth(double tabPaneWidth, int tabNumber) {
    // The tabs always appear 40px wider, maybe because of some obscure padding rule.
    // We also need to subtract the tab area padding to prevent horizontal scrolling.
    double tabWidth = Math.floor((tabPaneWidth - 20.0) / tabNumber) - 40.0;
    fxApplicationThreadExecutor.execute(() -> {
      subDivisionTabPane.setTabMinWidth(tabWidth);
      subDivisionTabPane.setTabMaxWidth(tabWidth);
    });
  }

  public void processSearchInput() {
    String searchText = searchTextField.getText();
    if (StringUtils.isBlank(searchText)) {
      return;
    }

    playerService.getPlayerByName(searchText).thenAccept(playerBeanOptional -> playerBeanOptional.ifPresent(player ->
      leaderboardService.getLeagueEntryForPlayer(player, season).thenAccept(leagueEntry -> {
        if (leagueEntry == null) {
          return;
        }
        selectLeagueEntry(leagueEntry);
      })));
  }

  public void setSeason(LeagueSeasonBean season) {
    this.season = season;

    seasonLabel.setText(i18n.getOrDefault(
        season.getNameKey(), String.format("leaderboard.season.%s", season.getNameKey()), season.getSeasonNumber()
    ).toUpperCase());
    String startDate = timeService.asDate(season.getStartDate(), FormatStyle.MEDIUM);
    String endDate = timeService.asDate(season.getEndDate(), FormatStyle.MEDIUM);
    seasonDateLabel.setText(i18n.get("leaderboard.seasonDate", startDate, endDate));
    contentPane.setVisible(false);
    searchTextField.clear();
    if (usernamesAutoCompletion != null) {
      usernamesAutoCompletion.dispose();
    }
    leaderboardService.getAllSubdivisions(season).thenAccept(this::setUsernamesAutoCompletion)
        .exceptionally(throwable -> {
          contentPane.setVisible(false);
          log.error("Error while loading division list", throwable);
          notificationService.addImmediateErrorNotification(throwable, "leaderboard.failedToLoadDivisions");
          return null;
        });

    JavaFxUtil.addAndTriggerListener(playerService.getCurrentPlayer().getLeaderboardRatings(), new WeakInvalidationListener(playerRatingListener));
  }

  private void setUsernamesAutoCompletion(List<SubdivisionBean> subdivisions) {
    fxApplicationThreadExecutor.execute(() -> {
      majorDivisionPicker.getItems().clear();
      List<SubdivisionBean> majorDivisions = subdivisions
          .stream()
          .filter(subdivision -> subdivision.getIndex() == 1)
          .collect(Collectors.toList());
      Collections.reverse(majorDivisions);
      majorDivisionPicker.getItems().addAll(majorDivisions);
    });
    Set<String> leagueEntryNames = new HashSet<>();
    List<CompletableFuture<?>> futures = new ArrayList<>();
    subdivisions.forEach(subdivision -> {
      CompletableFuture<Void> future = leaderboardService.getEntries(subdivision).thenAccept(entries ->
          entries.stream()
              .map(leagueEntryBean -> leagueEntryBean.getPlayer().getUsername())
              .forEach(leagueEntryNames::add));
      futures.add(future);
    });
    CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).thenRun(() -> {
      usernamesAutoCompletion = TextFields.bindAutoCompletion(searchTextField,
          leagueEntryNames);
      usernamesAutoCompletion.setDelay(0);
      usernamesAutoCompletion.setOnAutoCompleted(event -> processSearchInput());
      fxApplicationThreadExecutor.execute(() -> contentPane.setVisible(true));
    });
  }

  @Override
  public StackPane getRoot() {
    return leaderboardRoot;
  }

  @VisibleForTesting
  protected void updateDisplayedPlayerStats() {
    leaderboardService.getAllSubdivisions(season).thenAccept(divisions ->
      leaderboardService.getLeagueEntryForPlayer(playerService.getCurrentPlayer(), season).thenAccept(leagueEntry -> {
        if (leagueEntry == null) {
          selectHighestDivision();
        } else {
          if (leagueEntry.getSubdivision() == null) {
            fxApplicationThreadExecutor.execute(() -> {
              playerDivisionNameLabel.setVisible(false);
              placementLabel.setVisible(true);
              int gamesNeeded = leagueEntry.getReturningPlayer() ? season.getPlacementGamesReturningPlayer() : season.getPlacementGames();
              placementLabel.setText(i18n.get("leaderboard.placement", leagueEntry.getGamesPlayed(), gamesNeeded));
            });
            selectHighestDivision();
          } else {
            fxApplicationThreadExecutor.execute(() -> {
              playerDivisionNameLabel.setVisible(true);
              placementLabel.setVisible(false);
              playerDivisionImageView.setImage(
                  leaderboardService.loadDivisionImage(leagueEntry.getSubdivision().getImageUrl()));
              playerDivisionNameLabel.setText(i18n.get("leaderboard.divisionName",
                  i18n.get(leagueEntry.getSubdivision().getDivisionI18nKey()),
                  leagueEntry.getSubdivision().getNameKey()).toUpperCase());
              scoreArc.setLength(-360.0 * leagueEntry.getScore() / leagueEntry.getSubdivision().getHighestScore());
              playerScoreLabel.setText(i18n.number(leagueEntry.getScore()));
            });
            selectLeagueEntry(leagueEntry);
          }
        }
        plotDivisionDistributions(divisions, leagueEntry);
      }).exceptionally(throwable -> {
        log.error("Error while fetching leagueEntry", throwable);
        notificationService.addImmediateErrorNotification(throwable, "leaderboard.failedToLoadEntry");
        return null;
      })
    ).exceptionally(throwable -> {
      log.error("Error while loading division list", throwable);
      notificationService.addImmediateErrorNotification(throwable, "leaderboard.failedToLoadDivisions");
      return null;
    });
  }

  private void selectHighestDivision() {
    entryToSelect = null;
    fxApplicationThreadExecutor.execute(() -> majorDivisionPicker.getSelectionModel().selectFirst());
  }

  private void selectLeagueEntry(LeagueEntryBean leagueEntry) {
    entryToSelect = leagueEntry;
    // When selecting an item from majorDivisionPicker, onMajorDivisionPicked gets called automatically,
    // but only if the selection actually changes
    if (majorDivisionPicker.getValue() != null && hasSameDivisionIndex(entryToSelect, majorDivisionPicker.getValue())) {
      selectAssociatedTab();
    } else {
      majorDivisionPicker.getItems()
          .stream()
          .filter(item -> hasSameDivisionIndex(entryToSelect, item))
          .findFirst()
          .ifPresent(item -> fxApplicationThreadExecutor.execute(() -> majorDivisionPicker.getSelectionModel()
              .select(item)));
    }
  }

  private boolean hasSameDivisionIndex(LeagueEntryBean entry, SubdivisionBean subdivision) {
    return entry.getSubdivision().getDivision().getIndex() == subdivision.getDivision().getIndex();
  }

  public void onMajorDivisionPicked() {
    if (majorDivisionPicker.getValue() == null) {
      throw new IllegalStateException("majorDivisionPicker has no item selected");
    }
    leaderboardService.getAllSubdivisions(season).thenAccept(subdivisions -> {
      fxApplicationThreadExecutor.execute(() -> subDivisionTabPane.getTabs().clear());
      subdivisions.stream()
          .filter(subdivision -> subdivision.getDivision().getIndex() == majorDivisionPicker.getValue()
              .getDivision()
              .getIndex())
          .forEach(subdivision -> {
            SubDivisionTabController controller = uiService.loadFxml("theme/leaderboard/sub_division_tab.fxml");
            controller.getRoot().setUserData(subdivision.getIndex());
            controller.populate(subdivision);
            fxApplicationThreadExecutor.execute(() -> {
              subDivisionTabPane.getTabs().add(controller.getRoot());
              subDivisionTabPane.getSelectionModel().selectLast();
            });
          });
      setTabWidth(subDivisionTabPane.getWidth(), subDivisionTabPane.getTabs().size());
      if (entryToSelect != null && hasSameDivisionIndex(entryToSelect, majorDivisionPicker.getValue())) {
        selectAssociatedTab();
      }
    });
  }

  private void selectAssociatedTab() {
    subDivisionTabPane.getTabs().stream()
        .filter(tab -> tab.getUserData().equals(entryToSelect.getSubdivision().getIndex()))
        .findFirst().ifPresent(tab -> fxApplicationThreadExecutor.execute(() -> {
          subDivisionTabPane.getSelectionModel().select(tab);
          TableView<LeagueEntryBean> newTable = (TableView<LeagueEntryBean>) tab.getContent();
          newTable.scrollTo(entryToSelect);
          newTable.getSelectionModel().select(entryToSelect);
        }));
  }

  private void plotDivisionDistributions(List<SubdivisionBean> subdivisions, LeagueEntryBean leagueEntry) {
    fxApplicationThreadExecutor.execute(() -> ratingDistributionChart.getData().clear());
    // We need to set the categories first, to ensure they have the right order.
    ObservableList<String> categories = FXCollections.observableArrayList();
    subdivisions.stream()
        .filter(subdivision -> subdivision.getIndex() == 1)
        .map(subdivision -> i18n.get(subdivision.getDivisionI18nKey()))
        .forEach(categories::add);
    xAxis.setCategories(categories);

    subdivisions.stream().filter(subdivision -> subdivision.getDivision().getIndex() == 1).forEach(firstTierSubDivision -> {
      XYChart.Series<String, Integer> series = new XYChart.Series<>();
      series.setName(firstTierSubDivision.getNameKey());
      subdivisions.stream()
          .filter(subdivision -> subdivision.getIndex() == firstTierSubDivision.getIndex())
          .forEach(subdivision -> leaderboardService.getSizeOfDivision(subdivision).thenAccept(size -> {
            XYChart.Data<String, Integer> data = new XYChart.Data<>(i18n.get(subdivision.getDivisionI18nKey()), size);
            Text label = new Text();
            label.setText(subdivision.getNameKey());
            label.setFill(Color.WHITE);
            data.nodeProperty().addListener((observable, oldValue, newValue) -> {
              if (leagueEntry != null && subdivision.equals(leagueEntry.getSubdivision())) {
                newValue.pseudoClassStateChanged(NOTIFICATION_HIGHLIGHTED_PSEUDO_CLASS, true);
              }
              addNodeOnTopOfBar(data, label);
            });
            fxApplicationThreadExecutor.execute(() -> series.getData().add(data));
          }));
      fxApplicationThreadExecutor.execute(() -> ratingDistributionChart.getData().add(series));
    });
    leaderboardService.getTotalPlayers(season).thenAccept(totalPlayers ->
        fxApplicationThreadExecutor.execute(() -> xAxis.labelProperty()
            .setValue(i18n.get("leaderboard.totalPlayers", totalPlayers))));
  }

  private void addNodeOnTopOfBar(XYChart.Data<String, Integer> data, Node nodeToAdd) {
    final Node node = data.getNode();
    node.parentProperty().addListener((ov, oldParent, parent) -> {
      if (parent == null) {
        return;
      }
      Group parentGroup = (Group) parent;
      ObservableList<Node> children = parentGroup.getChildren();
      if (!children.contains(nodeToAdd)) {
        children.add(nodeToAdd);
        nodeToAdd.setViewOrder(-0.5);
      }
    });

    JavaFxUtil.addListener(node.boundsInParentProperty(), (ov, oldBounds, bounds) -> {
      nodeToAdd.setLayoutX(Math.round(bounds.getMinX() + bounds.getWidth() / 2 - nodeToAdd.prefWidth(-1) / 2));
      nodeToAdd.setLayoutY(Math.round(bounds.getMaxY() - nodeToAdd.prefHeight(-1) * 0.5));
    });
  }
}
