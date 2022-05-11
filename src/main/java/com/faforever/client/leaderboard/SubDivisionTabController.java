package com.faforever.client.leaderboard;

import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringCell;
import com.faforever.client.fx.contextmenu.AddFoeMenuItem;
import com.faforever.client.fx.contextmenu.AddFriendMenuItem;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.fx.contextmenu.CopyUsernameMenuItem;
import com.faforever.client.fx.contextmenu.RemoveFoeMenuItem;
import com.faforever.client.fx.contextmenu.RemoveFriendMenuItem;
import com.faforever.client.fx.contextmenu.ShowPlayerInfoMenuItem;
import com.faforever.client.fx.contextmenu.ViewReplaysMenuItem;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.google.common.math.Quantiles.Scale;
import javafx.beans.binding.Bindings;
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

import java.text.DecimalFormat;
import java.util.function.Function;

import static javafx.collections.FXCollections.observableList;


@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class SubDivisionTabController implements Controller<Tab> {

  private final ContextMenuBuilder contextMenuBuilder;
  private final LeaderboardService leaderboardService;
  private final NotificationService notificationService;
  private final I18n i18n;
  public Tab subDivisionTab;
  public TableColumn<LeagueEntryBean, Number> rankColumn;
  public TableColumn<LeagueEntryBean, String> nameColumn;
  public TableColumn<LeagueEntryBean, Number> gamesPlayedColumn;
  public TableColumn<LeagueEntryBean, Number> winRateColumn;
  public TableColumn<LeagueEntryBean, Number> scoreColumn;
  public TableView<LeagueEntryBean> ratingTable;

  @Override
  public Tab getRoot() {
    return subDivisionTab;
  }

  @Override
  public void initialize() {
    ratingTable.setRowFactory(param -> entriesRowFactory());

    rankColumn.setCellValueFactory(param -> param.getValue().rankProperty());
    rankColumn.setCellFactory(param -> new StringCell<>(rank -> i18n.number(rank.intValue())));

    nameColumn.setCellValueFactory(param -> param.getValue().getPlayer().usernameProperty());
    nameColumn.setCellFactory(param -> new StringCell<>(name -> name));

    gamesPlayedColumn.setCellValueFactory(param -> param.getValue().gamesPlayedProperty());
    gamesPlayedColumn.setCellFactory(param -> new StringCell<>(count -> i18n.number(count.intValue())));

    winRateColumn.setCellValueFactory(param -> Bindings.createFloatBinding(() -> {
      LeagueEntryBean entry = param.getValue();
      float winRate = (float) entry.getScore() / entry.getGamesPlayed() * 100;
      return winRate > 100 ? 100 : winRate;
    }));
    winRateColumn.setCellFactory(param -> new StringCell<>(winRate -> i18n.rounded(winRate.doubleValue(), 1)));

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
      LeagueEntryBean entry = row.getItem();
      PlayerBean player = entry.getPlayer();
      contextMenuBuilder.newBuilder()
          .addItem(ShowPlayerInfoMenuItem.class, player)
          .addItem(CopyUsernameMenuItem.class, player.getUsername())
          .addSeparator()
          .addItem(AddFriendMenuItem.class, player)
          .addItem(RemoveFriendMenuItem.class, player)
          .addItem(AddFoeMenuItem.class, player)
          .addItem(RemoveFoeMenuItem.class, player)
          .addSeparator()
          .addItem(ViewReplaysMenuItem.class, player)
          .build()
          .show(subDivisionTab.getTabPane().getScene().getWindow(), event.getScreenX(), event.getScreenY());
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
      log.error("Error while loading leaderboard entries for subdivision `{}`", subdivision, throwable);
      notificationService.addImmediateErrorNotification(throwable, "leaderboard.failedToLoad");
      return null;
    });
  }
}
