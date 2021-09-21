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
import com.faforever.client.util.Validator;
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

import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.github.nocatch.NoCatch.noCatch;


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
  private LeagueSeasonBean season;

  @VisibleForTesting
  protected AutoCompletionBinding<String> usernamesAutoCompletion;

  @Override
  public void initialize() {
    scoreLabel.setText(i18n.get("leaderboard.score").toUpperCase());
    searchTextField.setPromptText(i18n.get("leaderboard.searchPrompt").toUpperCase());

    contentPane.managedProperty().bind(contentPane.visibleProperty());
    connectionProgressPane.managedProperty().bind(connectionProgressPane.visibleProperty());
    connectionProgressPane.visibleProperty().bind(contentPane.visibleProperty().not());

    majorDivisionPicker.setConverter(divisionStringConverter());
    yAxis.setTickLabelFormatter(integerStringConverter());

    searchTextField.textProperty().addListener((observable, oldValue, newValue) ->
        processSearchInput(newValue));
  }

  private void processSearchInput(String searchText) {
    TableView<LeagueEntryBean> ratingTable = (TableView<LeagueEntryBean>) subDivisionTabPane.getSelectionModel().getSelectedItem().getContent();
    if (Validator.isInt(searchText)) {
      ratingTable.scrollTo(Integer.parseInt(searchText) - 1);
    } else {
      LeagueEntryBean foundPlayer = null;
      for (LeagueEntryBean leagueEntry : ratingTable.getItems()) {
        if (leagueEntry.getUsername().toLowerCase().startsWith(searchText.toLowerCase())) {
          foundPlayer = leagueEntry;
          break;
        }
      }
      if (foundPlayer == null) {
        for (LeagueEntryBean leagueEntry : ratingTable.getItems()) {
          if (leagueEntry.getUsername().toLowerCase().contains(searchText.toLowerCase())) {
            foundPlayer = leagueEntry;
            break;
          }
        }
      }
      if (foundPlayer != null) {
        ratingTable.scrollTo(foundPlayer);
        ratingTable.getSelectionModel().select(foundPlayer);
      } else {
        ratingTable.getSelectionModel().select(null);
        searchInAllDivisions(searchText);
      }
    }
  }

  private void searchInAllDivisions(String searchText) {
    playerService.getPlayerByName(searchText).thenAccept(playerBeanOptional -> playerBeanOptional.ifPresent(player ->
      leaderboardService.getLeagueEntryForPlayer(player, season.getId()).thenAccept(leagueEntry -> JavaFxUtil.runLater(() -> {
        if (leagueEntry == null) {
          return;
        }
        majorDivisionPicker.getItems().stream()
            .filter(item -> item.getDivision().getIndex() == leagueEntry.getSubdivision().getDivision().getIndex())
            .findFirst().ifPresent(item -> majorDivisionPicker.getSelectionModel().select(item));
        subDivisionTabPane.getTabs().stream()
            .filter(tab -> tab.getUserData().equals(leagueEntry.getSubdivision().getIndex()))
            .findFirst().ifPresent(tab -> {
              subDivisionTabPane.getSelectionModel().select(tab);
              TableView<LeagueEntryBean> newTable = (TableView<LeagueEntryBean>) tab.getContent();
              newTable.scrollTo(leagueEntry);
              newTable.getSelectionModel().select(leagueEntry);
            });
      }))));
  }

  public void setSeason(LeagueSeasonBean season) {
    this.season = season;

    seasonLabel.setText(i18n.getOrDefault(season.getNameKey(), String.format("leaderboard.season.%s", season.getNameKey())).toUpperCase());
    contentPane.setVisible(false);
    searchTextField.clear();
    if (usernamesAutoCompletion != null) {
      usernamesAutoCompletion.dispose();
    }
    leaderboardService.getAllSubdivisions(season.getId()).thenAccept(subdivisions -> JavaFxUtil.runLater(() -> {
      majorDivisionPicker.getItems().clear();
      majorDivisionPicker.getItems().addAll(
          subdivisions.stream().filter(subdivision -> subdivision.getIndex() == 1).collect(Collectors.toList()));

      List<LeagueEntryBean> leagueEntries = new ArrayList<>();
      List<CompletableFuture<?>> futures = new ArrayList<>();
      subdivisions.forEach(subdivision -> {
        CompletableFuture<List<LeagueEntryBean>> future = leaderboardService.getEntries(subdivision);
        futures.add(future.thenAccept(leagueEntries::addAll));
      });
      CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).thenRun(() -> {
        usernamesAutoCompletion = TextFields.bindAutoCompletion(searchTextField,
            leagueEntries.stream().map(LeagueEntryBean::getUsername).collect(Collectors.toList()));
        usernamesAutoCompletion.setDelay(0);
      });
      contentPane.setVisible(true);
    })).exceptionally(throwable -> {
      contentPane.setVisible(false);
      log.warn("Error while loading division list", throwable);
      notificationService.addImmediateErrorNotification(throwable, "leaderboard.failedToLoad");
      return null;
    });
    setCurrentPlayer(playerService.getCurrentPlayer());
  }

  @Override
  public Tab getRoot() {
    return leaderboardRoot;
  }

  private void setCurrentPlayer(PlayerBean player) {
    InvalidationListener playerLeagueScoreListener = leagueObservable -> JavaFxUtil.runLater(() -> updateDisplayedPlayerStats(player));

    JavaFxUtil.addListener(player.getLeaderboardRatings(), new WeakInvalidationListener(playerLeagueScoreListener));
    updateDisplayedPlayerStats(player);
  }

  private void updateDisplayedPlayerStats(PlayerBean player) {
    leaderboardService.getAllSubdivisions(season.getId()).thenAccept(divisions ->
      leaderboardService.getLeagueEntryForPlayer(player, season.getId()).thenAccept(leagueEntry -> {
        if (leagueEntry == null) {
          selectHighestDivision();
        } else {
          if (leagueEntry.getSubdivision() == null) {
            playerDivisionNameLabel.setText(i18n.get("leaderboard.placement", leagueEntry.getGamesPlayed()));
            selectHighestDivision();
          } else {
            JavaFxUtil.runLater(() -> {
              playerDivisionImageView.setImage(assetService.loadAndCacheImage(
                  noCatch(() -> new URL(leagueEntry.getSubdivision().getImageKey())), Paths.get("divisions"), null));
              playerDivisionNameLabel.setText(i18n.get("leaderboard.divisionName",
                  i18n.get(leagueEntry.getSubdivision().getDivisionI18nKey()),
                  i18n.get(leagueEntry.getSubdivision().getNameKey())));
              scoreArc.setLength(-360.0 * leagueEntry.getScore() / leagueEntry.getSubdivision().getHighestScore());
              playerScoreLabel.setText(i18n.number(leagueEntry.getScore()));
            });
            selectOwnLeagueEntry(leagueEntry);
          }
        }
        plotDivisionDistributions(divisions, leagueEntry);
      }).exceptionally(throwable -> {
        log.warn("Error while fetching leagueEntry", throwable);
        return null;
      })
    ).exceptionally(throwable -> {
      log.warn("Error while loading division list", throwable);
      return null;
    });
  }

  private void selectOwnLeagueEntry(LeagueEntryBean leagueEntry) {
    JavaFxUtil.runLater(() -> {
      majorDivisionPicker.getItems().stream()
          .filter(item -> item.getDivision().getIndex() == leagueEntry.getSubdivision().getDivision().getIndex())
          .findFirst().ifPresent(item -> majorDivisionPicker.getSelectionModel().select(item));
      onMajorDivisionPicked();
      subDivisionTabPane.getTabs().stream()
          .filter(tab -> tab.getUserData().equals(leagueEntry.getSubdivision().getIndex()))
          .findFirst().ifPresent(tab -> {
            subDivisionTabPane.getSelectionModel().select(tab);
            // Need to test this once the api is up
            TableView<LeagueEntryBean> newTable = (TableView<LeagueEntryBean>) tab.getContent();
            newTable.scrollTo(leagueEntry);
            newTable.getSelectionModel().select(leagueEntry);
            // Alternatively:
//          for (LeagueEntryBean tableEntry : newTable.getItems()) {
//            if (tableEntry.getUsername().equals(leagueEntry.getUsername())) {
//              newTable.scrollTo(tableEntry);
//              newTable.getSelectionModel().select(tableEntry);
//              break;
//            }
//          }
      });
    });
  }

  private void selectHighestDivision() {
    JavaFxUtil.runLater(() -> {
      majorDivisionPicker.getSelectionModel().selectLast();
      onMajorDivisionPicked();
      subDivisionTabPane.getSelectionModel().selectLast();
    });
  }

  public void onMajorDivisionPicked() {
    leaderboardService.getAllSubdivisions(season.getId()).thenAccept(subdivisions ->
        JavaFxUtil.runLater(() -> {
          subDivisionTabPane.getTabs().clear();
          subdivisions.stream()
              .filter(subdivision -> subdivision.getDivision().getIndex() == majorDivisionPicker.getValue().getDivision().getIndex())
              .forEach(subdivision -> {
                SubDivisionTabController controller = uiService.loadFxml("theme/leaderboard/subDivisionTab.fxml");
                controller.getRoot().setUserData(subdivision.getIndex());
                controller.populate(subdivision);
                subDivisionTabPane.getTabs().add(controller.getRoot());
                subDivisionTabPane.getSelectionModel().selectLast();
              });
        }));
    // The tabs always appear 40px wider than they should for whatever reason. We add a little more to prevent horizontal scrolling
    JavaFxUtil.runLater(() -> subDivisionTabPane.setTabMinWidth(subDivisionTabPane.getWidth() / subDivisionTabPane.getTabs().size() - 45.0));
    JavaFxUtil.runLater(() -> subDivisionTabPane.setTabMaxWidth(subDivisionTabPane.getWidth() / subDivisionTabPane.getTabs().size() - 45.0));
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
              if (leagueEntry != null && subdivision == leagueEntry.getSubdivision()) {
                newValue.pseudoClassStateChanged(NOTIFICATION_HIGHLIGHTED_PSEUDO_CLASS, true);
              }
              addNodeOnTopOfBar(data, label);
            });
            JavaFxUtil.runLater(() -> series.getData().add(data));
          }));
      JavaFxUtil.runLater(() -> ratingDistributionChart.getData().add(series));
    });
    setXAxisLabel(leagueEntry);
  }

  private void setXAxisLabel(LeagueEntryBean leagueEntry) {
    leaderboardService.getTotalPlayers(season.getId()).thenAccept(totalPlayers -> {
      if (leagueEntry == null) {
        JavaFxUtil.runLater(() -> xAxis.labelProperty().setValue(i18n.get("leaderboard.totalPlayers", totalPlayers)));
      } else {
        leaderboardService.getAccumulatedRank(leagueEntry)
            .thenAccept(rank -> xAxis.labelProperty().setValue(i18n.get("leaderboard.rank", rank, totalPlayers)))
            .exceptionally(throwable -> {
              log.info("Could not get player rank", throwable);
              return null;
            });
      }
    });
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
