package com.faforever.client.leaderboard;

import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.LeagueSeasonBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.theme.UiService;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.InvalidationListener;
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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.text.Text;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class LeaderboardController implements Controller<Tab> {

  private static final PseudoClass NOTIFICATION_HIGHLIGHTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("highlighted-bar");

  private final AssetService assetService;
  private final I18n i18n;
  private final LeaderboardService leaderboardService;
  private final NotificationService notificationService;
  private final PlayerService playerService;
  private final UiService uiService;
  public Tab leaderboardRoot;
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
  private InvalidationListener playerRatingListener;

  @VisibleForTesting
  protected AutoCompletionBinding<String> usernamesAutoCompletion;

  @Override
  public void initialize() {
    scoreLabel.setText(i18n.get("leaderboard.score").toUpperCase());
    searchTextField.setPromptText(i18n.get("leaderboard.searchPrompt").toUpperCase());

    JavaFxUtil.bindManagedToVisible(contentPane);
    JavaFxUtil.bindManagedToVisible(connectionProgressPane);
    connectionProgressPane.visibleProperty().bind(contentPane.visibleProperty().not());

    majorDivisionPicker.setConverter(divisionStringConverter());
    yAxis.setTickLabelFormatter(integerStringConverter());

    searchTextField.textProperty().addListener((observable, oldValue, newValue) ->
        processSearchInput(newValue));

    subDivisionTabPane.widthProperty().addListener((observable, oldValue, newValue) ->
        setTabWidth((double) newValue, subDivisionTabPane.getTabs().size()));
  }

  private void setTabWidth(double tabPaneWidth, int tabNumber) {
    // The tabs always appear 40px wider, maybe because of some obscure padding rule.
    // We also need to subtract the tab area padding to prevent horizontal scrolling.
    double tabWidth = Math.floor((tabPaneWidth - 20.0) / tabNumber) - 40.0;
    JavaFxUtil.runLater(() -> {
      subDivisionTabPane.setTabMinWidth(tabWidth);
      subDivisionTabPane.setTabMaxWidth(tabWidth);
    });
  }

  private void processSearchInput(String searchText) {
    playerService.getPlayerByName(searchText).thenAccept(playerBeanOptional -> playerBeanOptional.ifPresent(player ->
      leaderboardService.getLeagueEntryForPlayer(player, season.getId()).thenAccept(leagueEntry -> {
        if (leagueEntry == null) {
          return;
        }
        selectLeagueEntry(leagueEntry);
      })));
  }

  public void setSeason(LeagueSeasonBean season) {
    this.season = season;

    seasonLabel.setText(i18n.getOrDefault(season.getNameKey(), String.format("leaderboard.season.%s", season.getNameKey())).toUpperCase());
    String startDate = season.getStartDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM));
    String endDate = season.getEndDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM));
    seasonDateLabel.setText(i18n.get("leaderboard.seasonDate", startDate, endDate));
    contentPane.setVisible(false);
    searchTextField.clear();
    if (usernamesAutoCompletion != null) {
      usernamesAutoCompletion.dispose();
    }
    leaderboardService.getAllSubdivisions(season.getId()).thenAccept(this::setUsernamesAutoCompletion)
        .exceptionally(throwable -> {
          contentPane.setVisible(false);
          log.warn("Error while loading division list", throwable);
          notificationService.addImmediateErrorNotification(throwable, "leaderboard.failedToLoad");
          return null;
        });
    setListener(playerService.getCurrentPlayer());
  }

  private void setUsernamesAutoCompletion(List<SubdivisionBean> subdivisions) {
    JavaFxUtil.runLater(() -> {
      majorDivisionPicker.getItems().clear();
      majorDivisionPicker.getItems().addAll(
          subdivisions.stream().filter(subdivision -> subdivision.getIndex() == 1).collect(Collectors.toList()));
    });
    List<LeagueEntryBean> leagueEntries = new ArrayList<>();
    List<CompletableFuture<?>> futures = new ArrayList<>();
    subdivisions.forEach(subdivision -> {
      CompletableFuture<List<LeagueEntryBean>> future = leaderboardService.getEntries(subdivision);
      futures.add(future.thenAccept(leagueEntries::addAll));
    });
    CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).thenRun(() -> {
      usernamesAutoCompletion = TextFields.bindAutoCompletion(searchTextField,
          leagueEntries.stream().map(leagueEntryBean -> leagueEntryBean.getPlayer().getUsername()).collect(Collectors.toList()));
      usernamesAutoCompletion.setDelay(0);
      JavaFxUtil.runLater(() -> contentPane.setVisible(true));
    });
  }

  @Override
  public Tab getRoot() {
    return leaderboardRoot;
  }

  private void setListener(PlayerBean player) {
    playerRatingListener = observable -> updateDisplayedPlayerStats(player);
    JavaFxUtil.addAndTriggerListener(player.getLeaderboardRatings(), new WeakInvalidationListener(playerRatingListener));
  }

  @VisibleForTesting
  protected void updateDisplayedPlayerStats(PlayerBean player) {
    leaderboardService.getAllSubdivisions(season.getId()).thenAccept(divisions ->
      leaderboardService.getLeagueEntryForPlayer(player, season.getId()).thenAccept(leagueEntry -> {
        if (leagueEntry == null) {
          selectHighestDivision();
        } else {
          if (leagueEntry.getSubdivision() == null) {
            JavaFxUtil.runLater(() -> {
              playerDivisionNameLabel.setVisible(false);
              placementLabel.setVisible(true);
              placementLabel.setText(i18n.get("leaderboard.placement", leagueEntry.getGamesPlayed(), season.getPlacementGames()));
            });
            selectHighestDivision();
          } else {
            Image divisionImage = assetService.loadAndCacheImage(
                leagueEntry.getSubdivision().getImageUrl(), Path.of("divisions"), null);
            JavaFxUtil.runLater(() -> {
              playerDivisionNameLabel.setVisible(true);
              placementLabel.setVisible(false);
              playerDivisionImageView.setImage(divisionImage);
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
        log.warn("Error while fetching leagueEntry", throwable);
        notificationService.addImmediateErrorNotification(throwable, "leaderboard.failedToLoadEntry");
        return null;
      })
    ).exceptionally(throwable -> {
      log.warn("Error while loading division list", throwable);
      notificationService.addImmediateErrorNotification(throwable, "leaderboard.failedToLoadDivisions");
      return null;
    });
  }

  private void selectHighestDivision() {
    entryToSelect = null;
    JavaFxUtil.runLater(() -> majorDivisionPicker.getSelectionModel().selectLast());
  }

  private void selectLeagueEntry(LeagueEntryBean leagueEntry) {
    entryToSelect = leagueEntry;
    // When selecting an item from majorDivisionPicker, onMajorDivisionPicked gets called automatically,
    // but only if the selection actually changes
    if (majorDivisionPicker.getValue() != null && correctDivisionSelected()) {
      selectAssociatedTab();
    } else {
      majorDivisionPicker.getItems().stream()
          .filter(item -> item.getDivision().getIndex() == entryToSelect.getSubdivision().getDivision().getIndex())
          .findFirst().ifPresent(item -> JavaFxUtil.runLater(() -> majorDivisionPicker.getSelectionModel().select(item)));
    }
  }

  private boolean correctDivisionSelected() {
    return entryToSelect.getSubdivision().getDivision().getIndex() == majorDivisionPicker.getValue().getDivision().getIndex();
  }

  public void onMajorDivisionPicked() {
    leaderboardService.getAllSubdivisions(season.getId()).thenAccept(subdivisions -> {
      JavaFxUtil.runLater(() -> subDivisionTabPane.getTabs().clear());
      subdivisions.stream()
          .filter(subdivision -> subdivision.getDivision().getIndex() == majorDivisionPicker.getValue().getDivision().getIndex())
          .forEach(subdivision -> {
            SubDivisionTabController controller = uiService.loadFxml("theme/leaderboard/subDivisionTab.fxml");
            controller.getRoot().setUserData(subdivision.getIndex());
            controller.populate(subdivision);
            JavaFxUtil.runLater(() -> {
              subDivisionTabPane.getTabs().add(controller.getRoot());
              subDivisionTabPane.getSelectionModel().selectLast();
            });
          });
      setTabWidth(subDivisionTabPane.getWidth(), subDivisionTabPane.getTabs().size());
      if (entryToSelect != null && correctDivisionSelected()) {
        selectAssociatedTab();
      }
    });
  }

  private void selectAssociatedTab() {
    subDivisionTabPane.getTabs().stream()
        .filter(tab -> tab.getUserData().equals(entryToSelect.getSubdivision().getIndex()))
        .findFirst().ifPresent(tab -> JavaFxUtil.runLater(() -> {
          subDivisionTabPane.getSelectionModel().select(tab);
          TableView<LeagueEntryBean> newTable = (TableView<LeagueEntryBean>) tab.getContent();
          newTable.scrollTo(entryToSelect);
          newTable.getSelectionModel().select(entryToSelect);
        }));
  }

  private void plotDivisionDistributions(List<SubdivisionBean> subdivisions, LeagueEntryBean leagueEntry) {
    JavaFxUtil.runLater(() -> ratingDistributionChart.getData().clear());
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
            JavaFxUtil.runLater(() -> series.getData().add(data));
          }));
      JavaFxUtil.runLater(() -> ratingDistributionChart.getData().add(series));
    });
    leaderboardService.getTotalPlayers(season.getId()).thenAccept(totalPlayers ->
        JavaFxUtil.runLater(() -> xAxis.labelProperty().setValue(i18n.get("leaderboard.totalPlayers", totalPlayers))));
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

  @NotNull
  private StringConverter<SubdivisionBean> divisionStringConverter() {
    return new StringConverter<>() {
      @Override
      public String toString(SubdivisionBean subdivision) {
        return i18n.get(subdivision.getDivisionI18nKey()).toUpperCase();
      }

      @Override
      public SubdivisionBean fromString(String string) {
        return null;
      }
    };
  }

  @NotNull
  private StringConverter<Number> integerStringConverter() {
    return new StringConverter<>() {
      @Override
      public String toString(Number object) {
        if(object.intValue()!=object.doubleValue())
          return "";
        return String.valueOf(object.intValue());
      }

      @Override
      public Number fromString(String string) {
        Number val = Double.parseDouble(string);
        return val.intValue();
      }
    };
  }
}
