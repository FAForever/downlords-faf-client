package com.faforever.client.leaderboard;

import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringCell;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.theme.UiService;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static javafx.collections.FXCollections.observableList;


@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class SubDivisionTabController implements Controller<Tab> {

  private final LeaderboardService leaderboardService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final UiService uiService;
  public Tab subDivisionTab;
  public TableColumn<LeagueEntryBean, Number> rankColumn;
  public TableColumn<LeagueEntryBean, String> nameColumn;
  public TableColumn<LeagueEntryBean, Number> gamesPlayedColumn;
  public TableColumn<LeagueEntryBean, Number> scoreColumn;
  public TableView<LeagueEntryBean> ratingTable;

  private LeaderboardContextMenuController contextMenuController;

  @Override
  public Tab getRoot() {
    return subDivisionTab;
  }

  @Override
  public void initialize() {
    contextMenuController = uiService.loadFxml("theme/player_context_menu.fxml", LeaderboardContextMenuController.class);
    ratingTable.setRowFactory(param -> entriesRowFactory());

    rankColumn.setCellValueFactory(param -> param.getValue().rankProperty());
    rankColumn.setCellFactory(param -> new StringCell<>(rank -> i18n.number(rank.intValue())));

    nameColumn.setCellValueFactory(param -> param.getValue().getPlayer().usernameProperty());
    nameColumn.setCellFactory(param -> new StringCell<>(name -> name));

    gamesPlayedColumn.setCellValueFactory(param -> param.getValue().gamesPlayedProperty());
    gamesPlayedColumn.setCellFactory(param -> new StringCell<>(count -> i18n.number(count.intValue())));

    scoreColumn.setCellValueFactory(param -> param.getValue().scoreProperty());
    scoreColumn.setCellFactory(param -> new StringCell<>(score -> i18n.number(score.intValue())));
  }

  @NotNull
  private TableRow<LeagueEntryBean> entriesRowFactory() {
    TableRow<LeagueEntryBean> row = new TableRow<>();
    row.setOnContextMenuRequested(event -> {
      if (row.getItem() == null) {
        return;
      }
      LeagueEntryBean  entry = row.getItem();
      contextMenuController.setPlayer(entry.getPlayer());
      contextMenuController.getContextMenu().show(subDivisionTab.getTabPane().getScene().getWindow(), event.getScreenX(), event.getScreenY());
    });

    return row;
  }

  public void populate(SubdivisionBean subdivision) {
    JavaFxUtil.runLater(() -> subDivisionTab.setText(subdivision.getNameKey()));

    leaderboardService.getEntries(subdivision).thenAccept(leagueEntryBeans -> {
      leaderboardService.getPlayerNumberInHigherDivisions(subdivision).thenAccept(count ->
          leagueEntryBeans.forEach(entry -> entry.setRank(count + 1 + leagueEntryBeans.indexOf(entry))));
      ratingTable.setItems(observableList(leagueEntryBeans));
    }).exceptionally(throwable -> {
      log.warn("Error while loading leaderboard entries for division " + subdivision, throwable);
      notificationService.addImmediateErrorNotification(throwable, "leaderboard.failedToLoad");
      return null;
    });
  }
}
